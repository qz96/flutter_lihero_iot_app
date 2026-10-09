package com.lh.lhble.UI;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.BinData;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.Utils.Utilty;
import com.lh.lhble.UI.View.BottleSampleInfoDailog;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.UI.View.SetBLETapDailog;
import com.lh.lhble.google.zxing.activity.CaptureActivity;
import com.lh.lhble.network.RetrofitUtils;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.utils.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.lh.lhble.google.zxing.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;


public class LiherAdvancedBottleActivity extends BaseActivity implements CommontClickListener, View.OnClickListener {
    private final static String TAG = LiherAdvancedBottleActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    // 是否支持配置
    public static final String EXTRAS_DEVICE_IS_CONFIG = "DEVICE_IS_CONFIG";

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean isConfig;
    private WiseBluetoothLe mble;
    private Context context;
    private EditText sendEdit;
    private ScrollView scrollView;
    private TextView sendBt;
    private Handler mHandler;
    private static final int MSG_DATA_CHANGE = 0x11;

    // 更新速率
    private static final int MSG_UPDATE_RATE = 0x12;

    private RelativeLayout rateLayout;

    // 发送字节数text view
    private TextView sendByteCountTV;

    // 发送字节数
    private int sendByteCount = 0;

    // 接收字节数text view
    private TextView receiveByteCountTV;

    // 接收字节数
    private int receiveByteCount = 0;

    // 接收速率
    private TextView receiveRateTV;

    private int recCountBySecond = 0;

    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;

    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private Timer timer;

    private TimerTask timerTask;
    public static final int RequestCode_Bin_Version = 1001;
    private BinData binData;
    private Boolean isTest = false;
    private TextView tvBottlestate, tvTapIsclose;
    private TextView tvSetTap, tvOpenTap, tvCloseTap;
    private TextView tvSampleNo,tvBlueName, tvMacAddress;
    private TextView tvSetBlueName, tvSetMacAddress;
    private TextView tvTemperature, tvElectricV;
    private TextView tvSetSampleNo,tvSetSample,tvReadSensor,tvSetLj,tvReadTempArray,tvUpdateCycle,tvRequestCycle;
    private String sampleId;
    private String bleVesion;
    private String samplePointID;
    private String samplePlan;
    private int sampleWater;
    private int sampleDDL;
    private String tempSamplePlan;
    private Integer tempSampleWater;
    private Integer tempSampleDDL;

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_advanced_bottle;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);

        topLeftBtn.setVisibility(View.VISIBLE);

        setRightText("设置");
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(LiherAdvancedBottleActivity.this, SetActivity.class);

                intent.putExtra(SetActivity.EXTRAS_SET_IS_CONFIG, isConfig);

                LiherAdvancedBottleActivity.this.startActivityForResult(intent, RequestCode_Bin_Version);
            }
        });

        context = this;

        setButtonsState(true);

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = MSG_UPDATE_RATE;
                message.arg1 = recCountBySecond;
                recCountBySecond = 0;
                mHandler.sendMessage(message);
            }
        };

        mble = WiseBluetoothLe.getInstance(getApplicationContext());    //获取蓝牙实例
        mble.mBleCallBack = bleCallBack;

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        int color = msg.arg1;
                        String strData = (String) msg.obj;
                        if (msg.arg2 == 1 && "O".equals(strData.substring(strData.length() - 1))) {
                            tvBottlestate.setText("瓶盖未关闭");
                        } else if (msg.arg2 == 1 && "C".equals(strData.substring(strData.length() - 1))) {
                            tvBottlestate.setText("瓶盖已关闭");
                        } else if (msg.arg2 == 1 && "1".equals(strData.substring(strData.length() - 1))) {
                            tvTapIsclose.setText("阀门已打开");
                        } else if (msg.arg2 == 1 && "2".equals(strData.substring(strData.length() - 1))) {
                            tvTapIsclose.setText("阀门已关闭");
                        } else if (msg.arg2 == 3 && "olo".equals(strData.substring(strData.length() - 3))) {
                            sendLHBleData("ls");
                        } else if (msg.arg2 == 3 && "olc".equals(strData.substring(strData.length() - 3))) {
                            sendLHBleData("ls");
                        } else if (msg.arg2 > 42 && strData.contains("#") && strData.split("#", -1).length >= 7) {
                            setReadDetailData(strData);
                        } else if (msg.arg2 == 16 && strData.contains("接收") && strData.contains("Write CapState")) {
                            sendLHBleData("ls");
                        }else if (msg.arg2==31 &&strData.contains("接收")&&strData.contains("Write WaterNum=")){
                            sendLHBleData("Read Detail");
                            showMsg(0,"设置样品编号成功");
                        }else if (msg.arg2==2&&strData.contains("接收")&&"lj".equals(strData.substring(strData.length()-2))){
                            showMsg(0,"交样运输发送成功，开始温度记录~");
                        }else if (msg.arg2>12&&strData.contains("接收")&&strData.contains("Write reppd=")){
                            sendLHBleData("Read Detail");

                            showMsg(0,"设置上传周期成功");
                        }else if (msg.arg2>12&&strData.contains("接收")&&strData.contains("Write revpd=")){
                            sendLHBleData("Read Detail");
                            showMsg(0,"设置同步周期成功");
                        }else if (msg.arg2==21&&strData.contains("接收")&&strData.contains("Write SamClock=")){
                            tempSamplePlan=null;
                            sendSampleSetting();
                        }else if (msg.arg2>13&&strData.contains("接收")&&strData.contains("Write SamDDL=")){
                            tempSampleDDL=null;
                            sendSampleSetting();
                        }else if (msg.arg2>12&&strData.contains("接收")&&strData.contains("Write SamYW=")){
                            tempSampleWater=null;
                            sendSampleSetting();
                        }
                        SpannableStringBuilder builder = new SpannableStringBuilder(strData);

                        //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);

                        switch (color) {
                            case Color.BLUE: //send
                                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.RED:    //error
                                builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.BLACK: //tips
                                builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            default: //receive
                                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                        }
                        TextView tView = new TextView(context);

                        tView.setText(builder);

                        LinearLayout layout = findViewById(R.id.scroll_layout);
                        layout.addView(tView);
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        break;

                    // 更新速率
                    case MSG_UPDATE_RATE:
                        int rate = msg.arg1;
                        showRecCountBySecond(rate);
                        break;

                    default:
                        break;
                }

                return false;
            }
        });
        RxTimerUtil.cancel();
        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read Detail");
            }
        });
    }

    private void setReadDetailData(String strData) {
        //接收：
        //      25.94#3.65#O#2#001#220303#220311163950#12336#0#0#959548#10#60#123456789012345678
        String[] details = strData.split("#", -1);
        String s0 = details[0]+ "°C水样温度 ";
        String s1 = details[1] + "V电池电压 ";
        sampleId = details[4];
        bleVesion = details[5];
        tvSampleNo.setText(sampleId.replaceAll("_",""));
        if ("O".equals(details[2])) {
            tvBottlestate.setText("瓶盖未关闭");
        } else if ("C".equals(details[2])) {
            tvBottlestate.setText("瓶盖已关闭");
        }
        if ("1".equals(details[3])) {
            tvTapIsclose.setText("阀门已打开");
        } else if ("2".equals(details[3])) {
            tvTapIsclose.setText("阀门已关闭");
        }

        String formatTime = DateUtils.formatTime("20" + details[6].trim(), DateUtils.METHOD_DATE_TIME_PATTERN, DateUtils.DATE_TIME_PATTERN);
        String s6 = "\r\n瓶内时间" + (formatTime.length() == 19 ? formatTime : "错误" + formatTime);
        String s7 = "";
        if (details.length > 7) {
            s7 = "\r\n瓶阀已操作" + details[7].trim() + "次 蓝牙版本" + bleVesion;
        }
        if (details[1]!=null&&StringUtils.isDouble(details[1].trim())&& DoubleUtils.round(Double.parseDouble(details[1].trim()),1)<3.5){
            showMsg(2,"电池电压低于3.5V，请尽快返厂更换。");
        }
        String s812="";
        if (details.length>=14){
            String s10="";
            if (details[10].length()==6&&(Integer.parseInt(details[10].substring(0,2))<=23)&&(Integer.parseInt(details[10].substring(2,4))<=59)&&(Integer.parseInt(details[10].substring(4,6))<=1)){
                s10="\r\n定时采样时间："+("01".equals(details[10].substring(4,6))?(details[10].substring(0,2)+":"+details[10].substring(2,4)):"已关闭");
            }
            samplePlan = details[10];
            sampleWater = Integer.parseInt(details[8]);
            sampleDDL = Integer.parseInt(details[9]);
            samplePointID = details[13];
            s812="\r\n"+(sampleWater==0?"":("水位阀值："+sampleWater)+"cm ")+(sampleDDL==0?"":("电导阀值："+sampleDDL+"uS "))+s10+"\r\n上传周期："+("0".equals(details[11])?"关闭":details[11]+"分钟")+" 同步周期："+("0".equals(details[12])?"关闭":details[12]+"分钟");
            tvUpdateCycle.setText("0".equals(details[11])?"关闭":details[11]+"分钟");
            tvRequestCycle.setText("0".equals(details[12])?"关闭":details[12]+"分钟");
        }
        addLogText("瓶子信息" + s0 + s1 + s6 + s7+s812, Color.rgb(139, 0, 255), s0.length() + s1.length() + s7.length() + s6.length()+s812.length());
        if (formatTime.length() != 19 || Math.abs(System.currentTimeMillis() - DateUtils.getTimeMillis(formatTime, DateUtils.DATE_TIME_PATTERN)) > 1 * 60 * 1000) {
            RxTimerUtil.cancel();
            RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                @Override
                public void doNext(long number) {
                    new HitiInputStrDailog(LiherAdvancedBottleActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置蓝牙瓶系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherAdvancedBottleActivity.this);
                }
            });
        }
    }

    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherAdvancedBottleActivity.class);
        intent.putExtra(LiherAdvancedBottleActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherAdvancedBottleActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherAdvancedBottleActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();

        rateLayout = findViewById(R.id.rate_layout);
        sendByteCountTV = findViewById(R.id.send_byte_count_tv);
        receiveByteCountTV = findViewById(R.id.receive_byte_count_tv);
        receiveRateTV = findViewById(R.id.receive_rate_tv);

        scrollView = findViewById(R.id.scroll);
        sendBt = findViewById(R.id.send);
        sendEdit = findViewById(R.id.sendData);
        findViewById(R.id.start_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTest == false) {
                    isTest = true;
                } else {
                    addLogText("请先停止正在运行中的测试~", Color.RED, 0);
                    return;
                }
                sendTestData();
            }
        });

        findViewById(R.id.stop_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isTest) {
                    addLogText("测试停止，请清屏后再发起新的测试~", Color.GREEN, 0);
                }
                isTest = false;

            }
        });
        // 清屏
        findViewById(R.id.clear_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout layout = findViewById(R.id.scroll_layout);
                layout.removeAllViews();

                setReceiveByteCount(0);
                setSendByteCount(0);
            }
        });

        sendBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String sendString = sendEdit.getText().toString();
                if (TextUtils.isEmpty(sendString))
                    return;
                sendEdit.setText("");
                if (HJBleApplication.shareInstance().isBleConfig() == 0) {
                    sendLHBleData(sendString);
                } else {
                    sendBleData(sendString);
                }
                //

            }
        });
        tvTemperature = findViewById(R.id.tv_temperature);
        tvElectricV = findViewById(R.id.tv_electric_v);
        tvBottlestate = findViewById(R.id.tv_bottle_state);
        tvTapIsclose = findViewById(R.id.tv_tap_isclose);
        tvOpenTap = findViewById(R.id.tv_open_tap);
        tvSetTap = findViewById(R.id.tv_set_tap);
        tvCloseTap = findViewById(R.id.tv_close_tap);
        tvBlueName = findViewById(R.id.tv_blue_name);
        tvSampleNo=findViewById(R.id.tv_sample_no);
        tvSetBlueName = findViewById(R.id.tv_set_blue_name);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);
        tvSetSampleNo=findViewById(R.id.tv_set_sample_no);
        tvSetSample=findViewById(R.id.tv_set_sample);
        tvReadSensor=findViewById(R.id.tv_read_sensor);
        tvSetLj=findViewById(R.id.tv_set_lj);
        tvReadTempArray=findViewById(R.id.tv_read_temp_array);
        tvUpdateCycle=findViewById(R.id.tv_update_cycle);
        tvRequestCycle=findViewById(R.id.tv_request_cycle);
        tvSetSampleNo.setOnClickListener(this);
        tvSetSample.setOnClickListener(this);
        tvReadSensor.setOnClickListener(this);
        tvSetLj.setOnClickListener(this);
        tvReadTempArray.setOnClickListener(this);
        tvUpdateCycle.setOnClickListener(this);
        tvRequestCycle.setOnClickListener(this);
        tvSetMacAddress.setOnClickListener(this);
        tvSetBlueName.setOnClickListener(this);
        tvBottlestate.setOnClickListener(this);
        tvTapIsclose.setOnClickListener(this);
        tvSetTap.setOnClickListener(this);
        tvOpenTap.setOnClickListener(this);
        tvCloseTap.setOnClickListener(this);
        tvTemperature.setOnClickListener(this);
        tvElectricV.setOnClickListener(this);
    }

    private void setSendByteCount(int count) {
        sendByteCount = count;
        sendByteCountTV.setText(String.format("发送字节数：%d Byte", count));
    }

    private void addSendByteCount(int count) {
        setSendByteCount(sendByteCount + count);
    }

    private void setReceiveByteCount(int count) {
        receiveByteCount = count;
        receiveByteCountTV.setText(String.format("接收字节数：%d Byte", count));
    }

    private void addReceiveByteCount(int count) {
        setReceiveByteCount(receiveByteCount + count);
    }

    // 显示速率
    private void showRecCountBySecond(int count) {
        receiveRateTV.setText(String.format("实时速率：%d B/s", count));
    }

    // 开始定时器
    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
        }

        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = MSG_UPDATE_RATE;
                    message.arg1 = recCountBySecond;
                    recCountBySecond = 0;
                    mHandler.sendMessage(message);
                }
            };
        }

        if (timer != null && timerTask != null) {
            timer.schedule(timerTask, 10, 1000);
        }
    }

    // 停止定时器
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (HJBleApplication.shareInstance().isBleConfig() == 2) {
            // 发送服务
            mSendCharact = Constant.Ble_Config_Send_Service;
            // 接收服务
            mReceiveCharact = Constant.Ble_Config_Receive_Service;

            setTitle(mDeviceName + "-配置");

            rateLayout.setVisibility(View.GONE);

            stopTimer();
        } else {
            // 发送服务
            mSendCharact = Constant.Ble_Data_Send_Service;
            // 接收服务
            mReceiveCharact = Constant.Ble_Data_Receive_Service;

            setTitle(mDeviceName + "-数据");

            rateLayout.setVisibility(View.VISIBLE);

            startTimer();
        }

        if (HJBleApplication.shareInstance().isBleHex()) {
            sendEdit.setText("");
            sendEdit.setHint("请输入十六进制数据");
        } else {
            sendEdit.setText("");
            sendEdit.setHint("请输入字符数据");
        }
        if (!StringUtils.isEmpty(mDeviceName)) tvBlueName.setText(mDeviceName);
        if (!StringUtils.isEmpty(mDeviceAddress)) tvMacAddress.setText(mDeviceAddress);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopTimer();
    }

    void setButtonsState(boolean enable) {
        sendBt.setEnabled(enable);
    }

    //发送数据
    void sendBleData(String sendString) {
        byte[] tmpBytes;
        if (HJBleApplication.shareInstance().isBleHex()) {

            if (sendString.length() % 2 != 0) {
                showMsg(1,"Hex长度应为2的倍数");
                return;
            }

            String pattern = "^[A-Fa-f0-9]+$";

            if (!Pattern.matches(pattern, sendString)) {
                showMsg(1,"输入格式错误");
                return;
            }

            // 添加回车
            if (HJBleApplication.shareInstance().isAddReturn()) {
                sendString += "0D0A";
            }

            tmpBytes = ConvertData.hexStringToBytes(sendString);
        } else {
            // 添加回车
            if (HJBleApplication.shareInstance().isAddReturn()) {
                sendString += "\r\n";
            }
            tmpBytes = ConvertData.utf8ToBytes(sendString);
        }

        if (tmpBytes == null) {
            return;
        }

        addLogText("发送：\r\n      " + sendString, Color.BLUE, sendString.length());

        if (HJBleApplication.shareInstance().isBleConfig() != 2) {
            addSendByteCount(tmpBytes.length);
        }

        final byte[] bytes = tmpBytes;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mSendCharact, bytes))
                    addLogText("发送失败！", Color.RED, 0);

            }
        }).start();

    }


    //发送数据
    void sendLHBleData(String sendString) {
        if (!StringUtils.isEmpty(sendString)&&!"Read Detail".equals(sendString)&&!"Read Times".equals(sendString)&&!sendString.contains("Write SysTime=")&&!HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
            showMsg(2,"您的账号无生产维修权限，无法操作蓝牙采样瓶");
            return;
        }
        byte[] tmpBytes;
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2,"发送数据内容不能为空");
            return;
        }

        tmpBytes = ProtocolUtil.getBlueToothMsgForSend(HJBleApplication.shareInstance().getDesID(), sendString);

        if (tmpBytes == null) {
            return;
        }else  if (mble == null || !mble.isConnect()) {
            showMsg(2,"样瓶蓝牙连接已中断，请扫描连接后重试");
            finish();
            return;
        }

        addLogText("发送：\r\n      " + sendString, Color.BLUE, sendString.length());
        insertLog(mDeviceAddress, sampleId, sendString);
        final byte[] bytes = tmpBytes;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mSendCharact, bytes)){
                    addLogText("发送失败！", Color.RED, 0);
                }else if (!mble.waitReturn(mReceiveCharact,15*1000)){

                    addLogText(getResources().getString(R.string.connect_err_0), Color.RED, 0);
                }


            }
        }).start();

    }

    /**
     * 延迟发送数据
     * @param sendString
     * @param millisecond
     */
    void sendLHBleData(String sendString,long millisecond) {
        byte[] tmpBytes;
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2,"发送数据内容不能为空");
            return;
        }

        tmpBytes = ProtocolUtil.getBlueToothMsgForSend(HJBleApplication.shareInstance().getDesID(), sendString);

        if (tmpBytes == null) {
            return;
        }
        RxTimerUtil.cancel();
        RxTimerUtil.timer(millisecond, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                addLogText("发送：\r\n      " + sendString, Color.BLUE, sendString.length());
                insertLog(mDeviceAddress, sampleId, sendString);
                final byte[] bytes = tmpBytes;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!mble.sendData(mSendCharact, bytes)){
                            addLogText("发送失败！", Color.RED, 0);

                        }else if (!mble.waitReturn(mReceiveCharact,15*1000)){

                            addLogText(getResources().getString(R.string.connect_err_0), Color.RED, 0);
                        }

                    }
                }).start();
            }
        });
    }

    //发送数据
    void sendBinData() {
        new Thread(new Runnable() {
            int trys = 0;

            @Override
            public void run() {
                while (binData != null && binData.getBagNo() < binData.getBagNum()) {
                    byte[] bytes = BagFormBinData(binData);
                    //System.out.print("sendStr-->"+ConvertData.bytesToUtf8(bytes));
                    byte[] recvData = mble.sendReceive(mSendCharact, mReceiveCharact, bytes, 6000);
                    if (recvData != null && ConvertData.bytesToUtf8(recvData).contains(">ok<\r\n")) {
                        addLogText(binData.getBagNo() + "号升级分包文件，发送成功！", Color.GREEN, 0);
                        binData.setBagNo(binData.getBagNo() + 1);
                        trys = 0;
                    } else if (trys < 3) {
                        trys++;
                        if (recvData == null) {
                            synchronized (Thread.currentThread()) {
                                try {
                                    Thread.currentThread().wait(3000);
                                    continue;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    addLogText("蓝牙通信不稳定，升级分包文件发送失败！请稍后重试", Color.RED, 0);
                                    break;
                                }
                            }

                        }
                        continue;
                    } else {
                        addLogText(recvData == null ? "" : ConvertData.bytesToUtf8(recvData) + "升级分包文件发送失败！", Color.RED, 0);
                        break;
                    }
                }

                if (binData != null && binData.getBagNo() == binData.getBagNum()) {
                    addLogText("升级文件发送完毕，请输入'read basic'查看版本信息", Color.GREEN, 0);
                }
            }
        }).start();

    }

    //发送数据
    void sendTestData() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (isTest) {

                    try {
                        synchronized (Thread.currentThread()) {
                            sendLHBleData("Read Vvbat");
                            Thread.currentThread().wait(100);
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    private byte[] BagFormBinData(BinData binData) {
        byte[] bagBytes = new byte[binData.getBagNo() < binData.getBagNum() - 1 ? BinData.bagSize : binData.getBinBytes().length - binData.getBagNo() * BinData.bagSize];
        System.arraycopy(binData.getBinBytes(), binData.getBagNo() * BinData.bagSize, bagBytes, 0, bagBytes.length);
        return ConvertData.utf8ToBytes("set upgrade " + binData.getVersionCode() + ":" + binData.getCrcNum() + ":" + binData.getBagNum() + ":" + binData.getBagNo() + ":" + bagBytes.length + ":" + Utilty.parseByte2HexStr(bagBytes) + "\r\n");
    }

    void addLogText(final String log, final int color, int byteLen) {
        Message message = new Message();
        message.what = MSG_DATA_CHANGE;
        message.arg1 = color;
        message.arg2 = byteLen;
        message.obj = log;
        mHandler.sendMessage(message);
    }

    WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
        @Override
        public void OnWiseBluetoothState(int state) {

            if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED) {
                addLogText("蓝牙已断开", Color.BLACK, 0);
            }

        }

        @Override
        public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {

            if (characteristic.equals(mReceiveCharact)) {

                // 不是配置模式
                if (HJBleApplication.shareInstance().isBleConfig() != 2) {
                    final int len = recvData.length;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addReceiveByteCount(len);
                            recCountBySecond += len;
                        }
                    });
                }
                String str = "";
                if (HJBleApplication.shareInstance().isBleHex()) {
                    str = ConvertData.bytesToHexString(recvData, false);
                } else if (HJBleApplication.shareInstance().isBleConfig() == 0) {
                    str = ProtocolUtil.getReturnMsgForReceive(recvData);
                } else {
                    str = new String(recvData);
                }
                addLogText("接收：\r\n      " + str, Color.rgb(139, 0, 255), str.length());
            } else if (characteristic.equals(mLogCharact)) {
                String logStr = ConvertData.bytesToHexString(recvData, false);
			/*	if (HJBleApplication.shareInstance().isBleHex()) {
					logStr = ConvertData.bytesToHexString(recvData, false);
				} else if (HJBleApplication.shareInstance().isBleConfig()==0){
					logStr=ProtocolUtil.getReturnMsgForReceive(recvData);
				}else {
					logStr =new String(recvData);
				}*/
                addLogText("日志：\r\n      " + logStr, Color.rgb(139, 0, 255), logStr.length());
            }
        }

        @Override
        public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {
            if (!StringUtils.isEmpty(stateMsg)){
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close)+","+stateMsg, Color.BLACK, 0);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMsg(2,stateMsg);
                    }
                });
            }
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.Dailog_Bottle_Sample_INFO:
                JSONObject jo = (JSONObject) target;
                tempSamplePlan = jo.getString("SamplePlan");
                tempSampleWater = jo.getInteger("SampleWater");
                tempSampleDDL = jo.getInteger("SampleDDL");
                if (tempSamplePlan!=null||tempSampleWater!=null||tempSampleDDL!=null){
                    sendSampleSetting();
                }

                break;
            case Constant.Dailog_BIN_UPDATE_Hiti:
                if (binData != null) {
                    sendBinData();
                }
                break;
            case Constant.Dailog_BLE_MACAddress:
                String strTarget = (String) target;

                sendLHBleData("Write BLEaddr=" + strTarget);
                break;
            case Constant.Dailog_BLE_NAME:
                String bleName = (String) target;
                sendLHBleData("Write DevName=" + bleName);
                break;
            case Constant.Dailog_BLE_SYSDateTime:
                String sysTime = (String) target;
                if (sysTime != null && sysTime.length() == 19) {
                    sendLHBleData("Write SysTime=" + DateUtils.formatTime(sysTime, DateUtils.DATE_TIME_PATTERN, DateUtils.METHOD_DATE_TIME_PATTERN).substring(2));
                } else {
                    showMsg(2,"请录入正确时间，" + sysTime);
                }
                break;
            case Constant.Dailog_Box_UpdateMsg_Cycle:
                String updateTime= (String) target;
                if (StringUtils.isInt(updateTime)){
                    int updateValue = Integer.parseInt(updateTime);

                    if (updateValue > 10080 || updateValue < 0) {
                        showMsg(2,"请输入正确上传周期，1~10080分钟的整数");
                    } else {
                        sendLHBleData("Write reppd=" +updateValue);

                    }
                }else {
                    showMsg(2,"请输入正确上传周期，1~10080分钟的整数");
                }
                break;
            case Constant.Dailog_Box_RequestMsg_Cycle:
                String cycleTime= (String) target;
                if (StringUtils.isInt(cycleTime)){
                    int cycleValue = Integer.parseInt(cycleTime);
                    if (cycleValue > 10080 || cycleValue < 0) {
                        showMsg(2,"请输入正确同步周期，1~10080分钟的整数");
                    } else {
                        sendLHBleData("Write revpd=" +cycleValue);
                    }
                }else {
                    showMsg(2,"请输入正确同步周期，1~10080分钟的整数");
                }
                break;

            case Constant.Dailog_BLE_SET_TAP:
                Integer i = (Integer) target;
                if (i == 1) {
                    sendLHBleData("Write CapState=" + 1);
                } else {
                    sendLHBleData("Write CapState=" + 2);
                }
                break;

        }
    }

    private void sendSampleSetting() {
        if (!StringUtils.isEmpty(tempSamplePlan)&&tempSamplePlan.length()==6){
            sendLHBleData("Write SamClock="+tempSamplePlan,300);
        }else if (tempSampleWater!=null){
            sendLHBleData("Write SamYW="+tempSampleWater,300);
        }else if (tempSampleDDL!=null){
            sendLHBleData("Write SamDDL="+tempSampleDDL,300);
        }else {
            sendLHBleData("Read Detail",500);
            showMsg(2,"连接成功，读取当前采样配置信息...");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_temperature:
                sendLHBleData("Read DevTemp");
                break;
            case R.id.tv_electric_v:
                sendLHBleData("Read Vvbat");
                break;
            case R.id.tv_set_blue_name:
                startActivityForResult(new Intent(LiherAdvancedBottleActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_BLE_Name);
                break;
            case R.id.tv_set_mac_address:
                startActivityForResult(new Intent(LiherAdvancedBottleActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);
                break;
            case R.id.tv_bottle_state:
                sendLHBleData("Read RealKey");
                break;
            case R.id.tv_tap_isclose:
                sendLHBleData("ls");
                break;
            case R.id.tv_set_tap:
                new SetBLETapDailog(LiherAdvancedBottleActivity.this, Constant.Dailog_BLE_SET_TAP, "设置阀门开关状态", LiherAdvancedBottleActivity.this);
                 showMsg(2,"手动设置阀门开关状态,仅用于校正，请谨慎设置！");
                break;
            case R.id.tv_open_tap:
                if (tvTapIsclose.getText().toString().equals("阀门已打开")) {
                    showMsg(2,"阀门已打开,请勿重复开启");
                    sendLHBleData("ls");
                    return;
                }
                sendLHBleData("lo");
                break;
            case R.id.tv_close_tap:
                if (tvTapIsclose.getText().toString().equals("阀门已关闭")) {
                    showMsg(2,"阀门已关闭,请勿重复关闭");
                    sendLHBleData("ls");
                    return;
                }
                sendLHBleData("lc");
                break;
            case R.id.tv_set_sample:
                if (StringUtils.isEmpty(samplePlan)){
                    sendLHBleData("Read Detail");
                }else {
                    new BottleSampleInfoDailog(LiherAdvancedBottleActivity.this,Constant.Dailog_Bottle_Sample_INFO,"采样配置",samplePlan,sampleWater,sampleDDL,LiherAdvancedBottleActivity.this);

                }
                break;
            case R.id.tv_set_sample_no:
                startActivityForResult(new Intent(LiherAdvancedBottleActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_SampleNo);
                break;
            case R.id.tv_read_sensor:
                sendLHBleData("Read Sensor");
                break;
            case R.id.tv_set_lj:
                sendLHBleData("lj");
                break;
            case R.id.tv_read_temp_array:
                sendLHBleData("Read TempArray");
                break;
            case R.id.tv_update_cycle:
                new HitiInputStrDailog(LiherAdvancedBottleActivity.this,Constant.Dailog_Box_UpdateMsg_Cycle,"上传数据间隔周期","上传周期(分钟)","",LiherAdvancedBottleActivity.this);
                break;
            case R.id.tv_request_cycle:
                new HitiInputStrDailog(LiherAdvancedBottleActivity.this,Constant.Dailog_Box_RequestMsg_Cycle,"同步平台指令周期","同步周期(分钟)","",LiherAdvancedBottleActivity.this);
                break;
        }
    }

    private void insertLog(String blueMac, String sampleId, String logContent) {

        if (StringUtils.isEmpty(blueMac)) {
            return;
        } else {
            Map<String, Object> params = new HashMap<>();
            params.put("blueMac", blueMac);
            params.put("sampleId", sampleId);
            params.put("operator", HJBleApplication.getUser().getUserId());
            params.put("logContent", logContent);
            RetrofitUtils.getApiService().insertLog(JSON.toJSON(params))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Result>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Result result) {
                            result.toString();

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mble.disconnectDevice();
        Log.d(TAG, "destroy");
        mble.disconnectLocalDevice();
        Log.d(TAG, "销毁");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RequestCode_Bin_Version:
                    if (data==null){
                        if (StringUtils.isEmpty(bleVesion)){
                            showMsg(2,"未获取到当前瓶版本信息，请稍后重试");
                            sendLHBleData("Read Detail");
                        }else {
                            ArrayList<String> macList=new ArrayList<>();
                            macList.add(mDeviceAddress);
                            startActivity(BlueOTAActivity.getIntent(this,macList,bleVesion));
                            finish();
                        }

                    }
                    break;
                case Constant.RequestCode_Scan_SampleNo:
                    String qcodeSampleNo= data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(qcodeSampleNo)){
                        showMsg(2, "未读取到二维码信息");
                    }else if (qcodeSampleNo.length()>16){
                        showMsg(2, "请录入正确16位样品编号，"+qcodeSampleNo);
                    }else {
                        String code=StringUtils.addCharForNum(qcodeSampleNo.trim(),16,'_');
                        sendLHBleData("Write WaterNum="+code);
                    }
                    break;
                case Constant.RequestCode_Scan_BLE_Name:
                    if (resultCode == RESULT_OK) {
                        String qcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                        if (StringUtils.isEmpty(qcode)) {
                            showMsg(2, "未读取到二维码信息");
                        } else if (qcode.length() != 12) {
                            showMsg(2, qcode + "\n，非力合蓝牙标准码");
                        } else {
                            String bleName = StringUtils.getNewBlueName(qcode);
                            new HitiStringDailog(LiherAdvancedBottleActivity.this, Constant.Dailog_BLE_NAME, bleName, "蓝牙名称确认", "确认设置蓝牙名称为：" + bleName + "?\n蓝牙将重启后生效", LiherAdvancedBottleActivity.this);

                        }
                    }
                    break;
                case Constant.RequestCode_Scan_MAC_Address:
                    if (resultCode == RESULT_OK) {
                        String qcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                        if (StringUtils.isEmpty(qcode)) {
                            showMsg(2, "未读取到二维码信息");
                        } else if (qcode.length() != 12) {
                            showMsg(2, "二维码，非力合蓝牙标准码");
                        } else {
                            String macAds = qcode;

                            new HitiStringDailog(LiherAdvancedBottleActivity.this, Constant.Dailog_BLE_MACAddress, macAds, "Mac地址确认", "确认设置Mac地址为：" + StringUtils.getNewBlueToothMacAddress(qcode) + "?\n蓝牙将重启后生效", LiherAdvancedBottleActivity.this);

                        }
                    }
                    break;
            }
        }
    }
}
