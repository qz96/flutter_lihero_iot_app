package com.lh.lhble.UI;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
import com.lh.lhble.UI.View.SelecterObjectDailog;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommonItemOnClickListener3;
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
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.lh.lhble.google.zxing.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;


public class LiherActivity extends BaseActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherActivity.class.getSimpleName();

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
    private TextView tvBlueName, tvMacAddress;
    private TextView tvSetBlueName, tvSetMacAddress;
    private TextView tvReadInfo,tvDiyOrder;
    private TextView tvResetTemperature,tvOpenTime,tvCLoseDelay;
    private String sampleId;
    private String bleVesion;
    private String lastCloseTime;
    private double currentTemperature=100;
    private Uri binUri;//目标升级文件的Uri
    private String fileName;
    private int fileCode;

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);

        topLeftBtn.setVisibility(View.VISIBLE);
        if (HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
            setRightText("设置");
            rightTitleTxt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent(LiherActivity.this, SetActivity.class);

                    intent.putExtra(SetActivity.EXTRAS_SET_IS_CONFIG, isConfig);

                    LiherActivity.this.startActivityForResult(intent, RequestCode_Bin_Version);
                }
            });
        }
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
                        if (msg.arg2==12&&StringUtils.getNewBlueToothMacAddress(strData.substring(strData.length()-12)).equals(mDeviceAddress)){
                            return false;
                        }else if (msg.arg2 == 1 && "O".equals(strData.substring(strData.length() - 1))) {
                            tvBottlestate.setText("瓶盖未关闭");
                        } else if (msg.arg2 == 1 && "C".equals(strData.substring(strData.length() - 1))) {
                            tvBottlestate.setText("瓶盖已关闭");
                        } else if (msg.arg2 == 1 && "1".equals(strData.substring(strData.length() - 1))) {
                            tvTapIsclose.setText("阀门已打开");
                        } else if (msg.arg2 == 1 && "2".equals(strData.substring(strData.length() - 1))) {
                            tvTapIsclose.setText("阀门已关闭");
                        }else if (msg.arg2 == 2 &&StringUtils.isInt(strData.substring(strData.length() - 2))) {
                            int lz=Integer.parseInt(strData.substring(strData.length() - 2));
                            String strLz=StringUtils.getBottleLZ(lz);
                            addLogText(strLz,lz>1?Color.RED:Color.BLACK, strLz.length());
                        } else if (msg.arg2 == 3 && "olo".equals(strData.substring(strData.length() - 3))) {
                            sendLHBleData("ls");
                        } else if (msg.arg2 == 3 && "olc".equals(strData.substring(strData.length() - 3))) {
                            sendLHBleData("ls");
                        } else if (msg.arg2 > 42 && strData.contains("#") && strData.split("#", -1).length >= 7) {
                            setReadDetailData(strData);
                        } else if (msg.arg2 == 16 && strData.contains("接收") && strData.contains("Write CapState")) {
                            sendLHBleData("ls");
                        }else if (msg.arg2==21&&strData.contains("接收")&&strData.substring(strData.length()-21).split(":").length==2){//240827150926:Clos  00
                            String formatTime = DateUtils.formatTime("20" + strData.substring(strData.length()-21).split(":")[0].trim(), DateUtils.METHOD_DATE_TIME_PATTERN, DateUtils.DATE_TIME_PATTERN);
                            String devKeyMsg="";
                            if (strData.contains("Open")){
                                devKeyMsg="开盖"+strData.substring(strData.length()-2)+"次，最近开盖时间:"+(formatTime.length() == 19 ? formatTime : "错误" + formatTime);
                                addLogText(devKeyMsg, Color.RED, devKeyMsg.length());
                            }else {
                                devKeyMsg="无开盖记录";
                                addLogText(devKeyMsg, Color.BLACK, devKeyMsg.length());
                            }

                        } else if (msg.arg2==18&&strData.contains("接收")&&strData.substring(strData.length()-18).split(":").length==2){//接收：220908180437:00002
                            String formatTime = DateUtils.formatTime("20" + strData.substring(strData.length()-18).split(":")[0].trim(), DateUtils.METHOD_DATE_TIME_PATTERN, DateUtils.DATE_TIME_PATTERN);
                            lastCloseTime = "最近关阀时间：" + (formatTime.length() == 19 ? formatTime : "错误" + formatTime);
                            addLogText(lastCloseTime, Color.BLACK, lastCloseTime.length());
                        } else if (msg.arg2>15&&strData!=null&&strData.contains("接收")&&strData.contains("Write paratemp=")){
                           refreshDetail();
                        }else if (msg.arg2>=26&&strData!=null&&strData.contains("接收")&&strData.contains("Write BLEaddr=")){
                            showMsg(0,"MAC地址设置成功，请退出重连");
                        }else if (msg.arg2>=26&&strData!=null&&strData.contains("接收")&&strData.contains("Write DevName=")){
                            showMsg(0,"蓝牙名称设置成功，请退出重连");
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
        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read Detail");
            }
        });
    }

    private void refreshDetail() {
        RxTimerUtil.cancel();
        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read Detail");
            }
        });
    }

    private void setReadDetailData(String strData) {
        String[] details = strData.split("#", -1);
        String s0 = details[0] + "°C水样温度 ";
        if (details[0].length()>7&&StringUtils.isDouble(details[0].substring(details[0].length()-6))){
            currentTemperature = Double.parseDouble(details[0].substring(details[0].length()-6));
        }

        String s1 = details[1] + "V电池电压 ";
        sampleId = details[4];
        bleVesion = details[5];
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
        String bleVesion = details[5];
        String formatTime = DateUtils.formatTime("20" + details[6].trim(), DateUtils.METHOD_DATE_TIME_PATTERN, DateUtils.DATE_TIME_PATTERN);
        String s6 = "\r\n瓶内时间" + (formatTime.length() == 19 ? formatTime : "错误" + formatTime);
        String s7 = "";
        if (details.length > 7&& mDeviceAddress!=null&&(mDeviceAddress.startsWith("0B")||mDeviceAddress.startsWith("0A"))) {
            s7 = "\r\n瓶阀已操作" + details[7].trim() + "次 蓝牙版本" + bleVesion;

        }
        if (details[1]!=null&&StringUtils.isDouble(details[1].trim())&&DoubleUtils.round(Double.parseDouble(details[1].trim()),1)<3.5){

            showMsg(2,"电池电压低于3.5V，请尽快返厂更换。");
        }
        addLogText("瓶子信息" + s0 + s1 + s6 + s7, Color.BLACK, s0.length() + s1.length() + s7.length() + s6.length());
        if (formatTime.length() != 19 || Math.abs(System.currentTimeMillis() - DateUtils.getTimeMillis(formatTime, DateUtils.DATE_TIME_PATTERN)) > 1 * 60 * 1000) {
            RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                @Override
                public void doNext(long number) {
                    new HitiInputStrDailog(LiherActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置蓝牙瓶系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherActivity.this);
                }
            });
        }else if (lastCloseTime==null){
            sendLHBleData("Read Times");
        }
    }

    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherActivity.class);
        intent.putExtra(LiherActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
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
        tvReadInfo = findViewById(R.id.tv_read_info);
        tvDiyOrder=findViewById(R.id.tv_diy_order);
        tvBottlestate = findViewById(R.id.tv_bottle_state);
        tvTapIsclose = findViewById(R.id.tv_tap_isclose);
        tvOpenTap = findViewById(R.id.tv_open_tap);
        tvSetTap = findViewById(R.id.tv_set_tap);
        tvCloseTap = findViewById(R.id.tv_close_tap);
        tvBlueName = findViewById(R.id.tv_blue_name);
        tvSetBlueName = findViewById(R.id.tv_set_blue_name);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);
        tvResetTemperature=findViewById(R.id.tv_reset_temperature);
        tvOpenTime=findViewById(R.id.tv_open_time);
        tvCLoseDelay=findViewById(R.id.tv_close_delay);
        tvSetMacAddress.setOnClickListener(this);
        tvSetBlueName.setOnClickListener(this);
        tvBottlestate.setOnClickListener(this);
        tvTapIsclose.setOnClickListener(this);
        tvSetTap.setOnClickListener(this);
        tvOpenTap.setOnClickListener(this);
        tvCloseTap.setOnClickListener(this);
        tvReadInfo.setOnClickListener(this);
        tvResetTemperature.setOnClickListener(this);
        tvOpenTime.setOnClickListener(this);
        tvCLoseDelay.setOnClickListener(this);
        tvDiyOrder.setOnClickListener(this);
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
        RxTimerUtil.cancel();
        if (!StringUtils.isEmpty(sendString)&&!sendString.contains("Write DevName=")&&!sendString.contains("Write BLEaddr=")&&!sendString.startsWith("Read")&&!sendString.contains("Write SysTime=")&&!HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
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
            showMsg(2,"蓝牙连接已中断，请扫描连接后重试");
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
                    addLogText((mble!=null?mble.getSendDataLenMax()+"MTU,":"")+"发送失败！", Color.RED, 0);
                }else if (!mble.waitReturn(mReceiveCharact,15*1000)){

                    addLogText(getResources().getString(R.string.ble_rtn_timeout), Color.RED, 0);
                }


            }
        }).start();
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
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close)+","+stateMsg, Color.RED, 6);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMsg(1,stateMsg);
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
            case Constant.Dailog_BLE_SET_TAP:
                Integer i = (Integer) target;
                if (i == 1) {
                    sendLHBleData("Write CapState=" + 1);
                } else {
                    sendLHBleData("Write CapState=" + 2);
                }
                break;
            case Constant.Dialog_Reset_Temperature:
                if (target != null && target instanceof String) {
                    try {
                        double temperature = Double.parseDouble((String) target);
                        if (temperature<100 && temperature>-100) {
                            double d=DoubleUtils.sub(temperature,currentTemperature);
                            double d1000=DoubleUtils.mul(d,1000);
                            sendLHBleData("Write paratemp="+((int)d1000));
                        }else{

                            showMsg(2,"请录入正确温度值，" + temperature);
                        }
                    }catch (NumberFormatException ex){
                        showMsg(2,"请录入正确温度值" );
                    }

                }
                break;
            case Constant.Dialog_BLE_OpenDateTime:
                String openTime = (String) target;
                if (openTime != null && openTime.length() == 19) {
                    sendLHBleData("Write nexttime=" + DateUtils.formatTime(openTime, DateUtils.DATE_TIME_PATTERN, DateUtils.METHOD_DATE_TIME_PATTERN).substring(2));
                } else {

                    showMsg(2,"请录入正确时间，" + openTime);
                }
                break;
            case Constant.Dialog_BLE_CloseDelay:
                if (target != null && target instanceof String) {
                    try {
                        int min = Integer.parseInt((String) target);
                        if (min<100 && min>0) {
                            sendLHBleData("Write worktime="+min);
                        }else {

                            showMsg(2,"请录入正确采样时长，" + min);
                        }
                    }catch (NumberFormatException ex){
                        showMsg(2,"请录入正确采样时长，整数分钟" );
                    }
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_read_info:
                List<JSONObject> jatRead = new ArrayList<>();
                JSONObject jor0 = new JSONObject();
                jor0.put("MyName", "读取温度(°C)");
                jor0.put("MyObject", "Read DevTemp");
                jatRead.add(jor0);
                JSONObject jor1 = new JSONObject();
                jor1.put("MyName", "读取电压（V）");
                jor1.put("MyObject", "Read Vvbat");
                jatRead.add(jor1);
                JSONObject jor2 = new JSONObject();
                jor2.put("MyName", "读取电机状态");
                jor2.put("MyObject", "lz");
                jatRead.add(jor2);
                new SelecterObjectDailog(LiherActivity.this, "请选择读取信息", jatRead, Constant.Dialog_Select_Bottle_Read_Info, LiherActivity.this);

                break;
            case R.id.tv_diy_order:
                List<JSONObject> jat = new ArrayList<>();
                JSONObject jo0 = new JSONObject();
                jo0.put("MyName", "开启防伪(公安厅)");
                jo0.put("MyObject", "Write caplock=1");
                jat.add(jo0);
                JSONObject jo1 = new JSONObject();
                jo1.put("MyName", "读取防伪(公安厅)");
                jo1.put("MyObject", "Read decap");
                jat.add(jo1);
                JSONObject jo2 = new JSONObject();
                jo2.put("MyName", "读取防伪");
                jo2.put("MyObject", "Read DevKey");
                jat.add(jo2);
                new SelecterObjectDailog(LiherActivity.this, "防伪功能指令", jat, Constant.Dialog_Select_FW_Action, LiherActivity.this);

                break;

            case R.id.tv_set_blue_name:
                startActivityForResult(new Intent(LiherActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_BLE_Name);
                break;
            case R.id.tv_set_mac_address:
                startActivityForResult(new Intent(LiherActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);
                break;
            case R.id.tv_bottle_state:
                sendLHBleData("Read RealKey");
                break;
            case R.id.tv_tap_isclose:
                sendLHBleData("ls");
                break;
            case R.id.tv_set_tap:
                new SetBLETapDailog(LiherActivity.this, Constant.Dailog_BLE_SET_TAP, "设置阀门开关状态", LiherActivity.this);
                showMsg(2, "手动设置阀门开关状态,仅用于校正，请谨慎设置！");

                break;
            case R.id.tv_open_tap:
                if (tvTapIsclose.getText().toString().equals("阀门已打开")) {
                    showMsg(2, "阀门已打开,请勿重复开启");
                    sendLHBleData("ls");
                    return;
                }
                sendLHBleData("lo");
                break;
            case R.id.tv_close_tap:
                if (tvTapIsclose.getText().toString().equals("阀门已关闭")) {
                    showMsg(2, "阀门已关闭,请勿重复关闭");
                    sendLHBleData("ls");
                    return;
                }
                sendLHBleData("lc");
                break;
            case R.id.tv_reset_temperature:
                if (currentTemperature>=100d){
                    showMsg(2, "未获取到瓶内温度信息");
                }else {
                    showMsg(2, "请输入正确温度值°C，保留两位小数");
                    new HitiInputStrDailog(LiherActivity.this,Constant.Dialog_Reset_Temperature,"温度校准","正确温度°C：", DoubleUtils.round(currentTemperature,2)+"",LiherActivity.this);
                }
                break;
            case R.id.tv_open_time:
                showMsg(2, "请输入下次发起采样时间");
                new HitiInputStrDailog(LiherActivity.this, Constant.Dialog_BLE_OpenDateTime, "设置下次发起采样时间", "下次采样时间：", DateUtils.long2time(System.currentTimeMillis()+15*60*1000L), LiherActivity.this);

                break;
            case R.id.tv_close_delay:
                showMsg(2, "输入多久采样时长后，自动关阀");
                new HitiInputStrDailog(LiherActivity.this, Constant.Dialog_BLE_CloseDelay, "设置采样时长", "采样时长（分钟）：", 5+"", LiherActivity.this);

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
                            e.printStackTrace();
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
                            showMsg(2, "未获取到当前瓶版本信息，请稍后重试");
                            sendLHBleData("Read Detail");
                        }else {
                            ArrayList<String> macList=new ArrayList<>();
                            macList.add(mDeviceAddress);
                            startActivity(BlueOTAActivity.getIntent(this,macList,bleVesion));
                            finish();
                        }

                    }else {
                        ArrayList<String> macList=new ArrayList<>();
                        macList.add(mDeviceAddress);
                        startActivity(LhBleListOrderActivity.getIntent(this,macList,bleVesion));
                        finish();
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
                            new HitiStringDailog(LiherActivity.this, Constant.Dailog_BLE_NAME, bleName, "蓝牙名称确认", "确认设置蓝牙名称为：" + bleName + "?\n蓝牙将重启后生效", LiherActivity.this);

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
                            String  macAds = qcode;
                            new HitiStringDailog(LiherActivity.this, Constant.Dailog_BLE_MACAddress, macAds, "Mac地址确认", "确认设置Mac地址为：" + StringUtils.getNewBlueToothMacAddress(qcode) + "?\n蓝牙将重启后生效", LiherActivity.this);

                        }
                    }
                    break;
            }
        }
    }


    @Override
    public void onItemClick3(View v, int position, Object object, int type) {
        switch (type){
            case Constant.Dialog_Select_FW_Action:
                if (position==0){
                    sendLHBleData("Write caplock=1");
                }else if (position==1){
                    sendLHBleData("Read decap");
                }else if (position==2){
                    sendLHBleData("Read DevKey");
                }
                break;
            case Constant.Dialog_Select_Bottle_Read_Info:
                if (position==0){
                    sendLHBleData("Read DevTemp");
                }else if (position==1){
                    sendLHBleData("Read Vvbat");
                }else if (position==2){
                    sendLHBleData("lz");
                }

                break;
        }
    }
}
