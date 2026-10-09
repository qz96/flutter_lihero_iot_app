package com.lh.lhble.UI;

import static com.lh.lhble.google.zxing.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;

import android.content.Context;
import android.content.DialogInterface;
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
import com.lh.lhble.Entity.BinData;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.R;
import com.lh.lhble.Utils.AESUtil;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommonItemOnClickListener3;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.network.RetrofitUtils;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;
import com.wise.wisekit.utils.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class DYBleActivity extends BaseActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = DYBleActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
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


    private WiseCharacteristic mLogCharact = Constant.DY_Ble_Data_Service;
    public static final int RequestCode_Bin_Version = 1001;
    private BinData binData;
    private Boolean isTest = false;
    private TextView tvBottlestate, tvTapIsclose;
    private TextView tvSetTap, tvOpenTap, tvCloseTap;
    private TextView tvBlueName, tvMacAddress;
    private TextView tvSetBlueName, tvSetMacAddress;
    private TextView tvResetTemperature,tvOpenTime,tvCLoseDelay;
    private String sampleId;
    private String bleVesion;
    private String lastCloseTime;
    private double currentTemperature=100;
    private Uri binUri;//目标升级文件的Uri
    private String fileName;
    private int fileCode;
    byte[] mKey;//大域的通信加密KEY
    private LoadingDialog loadingDialog = null;
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


        topLeftBtn.setVisibility(View.VISIBLE);

        context = this;

        setButtonsState(true);



        mble = WiseBluetoothLe.getInstance(getApplicationContext());    //获取蓝牙实例

        LoadingDialog.Builder loadBuilder=new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage(HJBleApplication.getAppContext().getString(R.string.connecting))
                .setCancelOutside(false)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if (loadingDialog!=null){
                            if (loadingDialog.cancelTimes>1){
                                mble.disconnectDevice();
                            }else {
                                loadingDialog.cancelTimes++;
                            }
                        }
                    }
                });
        loadingDialog = loadBuilder.create();
        connectDYBle(mDeviceAddress);
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        int color = msg.arg1;
                        String strData = (String) msg.obj;

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
      /*  RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read Detail");
            }
        });*/
    }
    // 连接蓝牙
    void connectDYBle(final String  macAddress) {
        loadingDialog.show();
        if ("00:00:00:00:00:00".equals(macAddress)){
            showMsg(2,HJBleApplication.getAppContext().getString(R.string.initial_ble_tips));
        }
        new Thread(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++)
                {
                    //String ads="19:98:03:17:03:20";
                    if(mble.connectDevice(macAddress,bleCallBack))	//连接蓝牙设备
                        break;

                    mble.disconnectDevice();
                    try {
                        Thread.sleep(500,0);//200ms
                    }
                    catch (Exception e){

                    }
                }
                if(i == 5)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (!DYBleActivity.this.isFinishing()){
                                showMsg(2,HJBleApplication.getAppContext().getString(R.string.connect_err));
                                loadingDialog.dismiss();
                            }
                        }
                    });
                    return ;
                }

                try {
                    Thread.sleep(200,0);//200ms
                }
                catch (Exception e){

                }


                // if(mble.openNotify(BleConfig.Ble_Data_Receive_Service)&&mble.openNotify(BleConfig.Ble_LOG_Receive_Service))
                if(mble.openNotify(Constant.DY_Ble_Data_Service))
                {
                    if (mble.requestMtu(255)){


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (DYBleActivity.this.isFinishing())return;
                                loadingDialog.dismiss();
                                HJBleApplication.shareInstance().setDesID("12345678");
                                mble.mBleCallBack = bleCallBack;
                            }
                        });
                    }else {
                        if (mble.getGatt()!=null){
                            mble.getGatt().disconnect();
                            mble.getGatt().close();
                        }
                        mble.disconnectDevice();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!DYBleActivity.this.isFinishing()){
                                    showMsg(2,HJBleApplication.getAppContext().getString(R.string.connect_err)+"3,"+HJBleApplication.getAppContext().getString(R.string.mtu_err));
                                    loadingDialog.dismiss();
                                }

                            }
                        });
                    }

                    return ;
                } else {
                    mble.disconnectDevice();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!DYBleActivity.this.isFinishing()) {
                                showMsg(2, HJBleApplication.getAppContext().getString(R.string.connect_err)+"2");
                                loadingDialog.dismiss();

                            }
                        }
                    });
                    return ;
                }

            }
        }).start();
    }
    private void setReadDetailData(String strData) {
        String[] details = strData.split("#", -1);
        String s0 = details[0] + "°C水样温度 ";
        if (details[0].length()>7){
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
        addLogText("瓶子信息" + s0 + s1 + s6 + s7, Color.rgb(139, 0, 255), s0.length() + s1.length() + s7.length() + s6.length());
        if (formatTime.length() != 19 || Math.abs(System.currentTimeMillis() - DateUtils.getTimeMillis(formatTime, DateUtils.DATE_TIME_PATTERN)) > 1 * 60 * 1000) {
            RxTimerUtil.cancel();
            RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                @Override
                public void doNext(long number) {
                    new HitiInputStrDailog(DYBleActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置蓝牙瓶系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), DYBleActivity.this);
                }
            });
        }else if (lastCloseTime==null){
            sendLHBleData("Read Times");
        }
    }

    public static Intent getIntent(Context context, String name, String mac) {
        Intent intent = new Intent(context, DYBleActivity.class);
        intent.putExtra(DYBleActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(DYBleActivity.EXTRAS_DEVICE_ADDRESS, mac);
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
                sendDYBleData(sendString.trim());

            }
        });
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

        tvResetTemperature.setOnClickListener(this);
        tvOpenTime.setOnClickListener(this);
        tvCLoseDelay.setOnClickListener(this);
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

    @Override
    protected void onResume() {
        super.onResume();

        if (!StringUtils.isEmpty(mDeviceName)) tvBlueName.setText(mDeviceName);
        if (!StringUtils.isEmpty(mDeviceAddress)) tvMacAddress.setText(mDeviceAddress);
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    void setButtonsState(boolean enable) {
        sendBt.setEnabled(enable);
    }

    //发送数据
    void sendBleData(String sendString) {
        byte[] tmpBytes;
        if (HJBleApplication.shareInstance().isBleHex()) {

            if (sendString.length() % 2 != 0) {
                showMsg(2, "Hex长度应为2的倍数");
                return;
            }

            String pattern = "^[A-Fa-f0-9]+$";

            if (!Pattern.matches(pattern, sendString)) {
                showMsg(2, "输入格式错误");
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
                if (!mble.sendData(mLogCharact, bytes))
                    addLogText("发送失败！", Color.RED, 0);

            }
        }).start();

    }


    //发送数据
    void sendDYBleData(String sendString) {
        byte[] tmpBytes=AESUtil.encrypt128NoPadding(sendString,mKey,Constant.DY_AES_IV);
        addLogText("发送：\r\n      " + sendString, Color.BLUE, sendString.length());
        //insertLog(mDeviceAddress, sampleId, sendString);
        final byte[] bytes = tmpBytes;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mLogCharact, bytes))
                    addLogText("发送失败！", Color.RED, 0);

            }
        }).start();
    }
    //发送数据
    void sendLHBleData(String sendString) {
        if (!StringUtils.isEmpty(sendString)&&!"Read Detail".equals(sendString)&&!"Read Times".equals(sendString)&&!sendString.contains("Write SysTime=")&&!HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
            showMsg(2, "您的账号无生产维修权限，无法操作蓝牙采样瓶");
            return;
        }
        byte[] tmpBytes;
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2, "发送数据内容不能为空");
            return;
        }

        tmpBytes = ProtocolUtil.getBlueToothMsgForSend(HJBleApplication.shareInstance().getDesID(), sendString);

        if (tmpBytes == null) {
            return;
        }else  if (mble == null || !mble.isConnect()) {

            showMsg(2,"设备蓝牙连接已中断，请扫描连接后重试");
            finish();
            return;
        }

        addLogText("发送：\r\n      " + sendString, Color.BLUE, sendString.length());
        insertLog(mDeviceAddress, sampleId, sendString);
        final byte[] bytes = tmpBytes;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mLogCharact, bytes))
                    addLogText("发送失败！", Color.RED, 0);

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

          if (characteristic.equals(mLogCharact)) {
                String receiveStr=ConvertData.bytesToUtf8(recvData);
                if (receiveStr.startsWith("KEY")&&receiveStr.length()>3&&receiveStr.length()<=8){
                    byte[] mRandrom=new byte[recvData.length-3];
                    System.arraycopy(recvData,3,mRandrom,0,recvData.length-3);
                     mKey="0123456789ABCDEF".getBytes();
                    System.arraycopy(mRandrom,0,mKey,0,mRandrom.length);


                    addLogText("接收：\r\n      " + receiveStr, Color.rgb(139, 0, 255), receiveStr.length());
                }else {
                    addLogText("接收：\r\n      " + receiveStr, Color.rgb(139, 0, 255), receiveStr.length());

                }
			/*	if (HJBleApplication.shareInstance().isBleHex()) {
					logStr = ConvertData.bytesToHexString(recvData, false);
				} else if (HJBleApplication.shareInstance().isBleConfig()==0){
					logStr=ProtocolUtil.getReturnMsgForReceive(recvData);
				}else {
					logStr =new String(recvData);
				}*/
          //      addLogText("日志：\r\n      " + logStr, Color.rgb(139, 0, 255), logStr.length());
            }
        }

        @Override
        public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {
            if (!StringUtils.isEmpty(stateMsg)){
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close)+","+stateMsg, Color.BLACK, 0);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingDialog != null) loadingDialog.dismiss();
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

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

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
                            new HitiStringDailog(DYBleActivity.this, Constant.Dailog_BLE_NAME, bleName, "蓝牙名称确认", "确认设置蓝牙名称为：" + bleName + "?\n蓝牙将重启后生效", DYBleActivity.this);

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
                            new HitiStringDailog(DYBleActivity.this, Constant.Dailog_BLE_MACAddress, macAds, "Mac地址确认", "确认设置Mac地址为：" + StringUtils.getNewBlueToothMacAddress(qcode) + "?\n蓝牙将重启后生效", DYBleActivity.this);

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
                }
                break;
        }
    }
}
