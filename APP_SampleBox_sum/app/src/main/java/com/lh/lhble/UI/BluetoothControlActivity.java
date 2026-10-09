package com.lh.lhble.UI;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.lh.lhble.Entity.BinData;
import com.lh.lhble.Entity.LogMsg;
import com.lh.lhble.R;
import com.lh.lhble.Utils.FileUtil;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.Utils.Utilty;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.utils.DateUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;


public class BluetoothControlActivity extends BaseActivity implements CommontClickListener {
    private final static String TAG = BluetoothControlActivity.class.getSimpleName();

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
    private TextView tvLogCount;
    private ScrollView scrollView;
    private Button sendBt;
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
    private WiseCharacteristic mEVCharact = Constant.Ble_EV_Receive_Service;
    private Timer timer;

    private TimerTask timerTask;
    public static final int RequestCode_Bin_Version = 1001;
    private BinData binData;
    private Boolean isTest = false;

    public int logCount;
    public int bottleOrderNo = -1;
    private List<String> bottleBleAddress = new ArrayList<>();
    private int testTotalCount = 0;
    private LogMsg logMsg;

    @Override
    protected int getPageLayoutId() {
        return R.layout.bluetooth_control;
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
                final Intent intent = new Intent(BluetoothControlActivity.this, SetActivity.class);

                intent.putExtra(SetActivity.EXTRAS_SET_IS_CONFIG, isConfig);

                BluetoothControlActivity.this.startActivityForResult(intent, RequestCode_Bin_Version);
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
		/*if (!mble.openNotify(BleConfig.Ble_EV_Receive_Service)){
			Toast.makeText(BluetoothControlActivity.this,"电压通知通道连接失败",Toast.LENGTH_SHORT).show();

		};*/
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
                        logMsg = new LogMsg();
                        logMsg.setContent(strData);
                        logMsg.setDateTime(DateUtils.long2time(System.currentTimeMillis()));
                        switch (color) {
                            case Color.BLUE: //send
                                logMsg.setType(1);
                                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.RED:    //error
                                logMsg.setType(2);
                                builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.BLACK: //tips
                                logMsg.setType(3);
                                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
							case Color.GREEN://receive
                                logMsg.setType(4);
								builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            default:
                                logMsg.setType(5);
                                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                        }
                        TextView tView = new TextView(context);
                        tView.setText(builder);
                        LinearLayout layout = findViewById(R.id.scroll_layout);
                        layout.addView(tView);
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        logCount++;
                        tvLogCount.setText("日志数：" + logCount);
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
        tvLogCount = findViewById(R.id.tv_log_count);
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
        findViewById(R.id.bottle_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (testTotalCount > 0) {
                    testTotalCount = 0;
                } else {
                    testTotalCount = 999;
                    if (bottleBleAddress == null || bottleBleAddress.size() <= 0) {
                        bottleBleAddress = new ArrayList<>();
                        bottleBleAddress.add("0B:21:05:00:00:40");
                        bottleBleAddress.add("0B:21:05:00:00:54");
                        bottleBleAddress.add("0B:21:05:00:00:91");
                        bottleBleAddress.add("0B:21:05:00:00:38");
                        bottleBleAddress.add("0B:21:05:00:01:08");
                        bottleBleAddress.add("0B:21:05:00:00:50");
                        bottleBleAddress.add("0B:21:05:00:01:06");
                        bottleBleAddress.add("0B:21:05:00:01:09");
                        bottleBleAddress.add("0B:21:05:00:00:86");
                        bottleBleAddress.add("0B:21:05:00:00:42");
                        bottleBleAddress.add("0B:21:05:00:00:58");
                        bottleBleAddress.add("0B:21:05:00:00:46");
                    }
                    sendBottleOrders();
                }

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
        byte[] tmpBytes;
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2,"发送数据内容不能为空");
            return;
        }

        tmpBytes = ProtocolUtil.getBlueToothMsgForSend(HJBleApplication.shareInstance().getDesID(), sendString);

        if (tmpBytes == null) {
            return;
        }

        addLogText("发送：\r\n      " + sendString, Color.BLUE, sendString.length());

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
    void sendBinData() {
        new Thread(new Runnable() {
            int trys = 0;

            @Override
            public void run() {
                while (binData != null && binData.getBagNo() < binData.getBagNum()) {
                    byte[] bytes = BagFormBinData(binData);
                    System.out.print("sendStr-->"+ConvertData.bytesToUtf8(bytes));
                    byte[] recvData = mble.sendReceive(mSendCharact, mReceiveCharact, bytes, 6000);
                    String sr= ConvertData.bytesToUtf8(recvData);
                    if (recvData != null &&sr.contains(">ok<\r\n")) {
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
                    addLogText("升级文件发送完毕，请输入'read basic'查看版本信息", Color.BLACK, 0);
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

                    byte[] testBytes = (System.currentTimeMillis() + "6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666").getBytes();
                    addLogText("测试发送值" + testBytes.length + "->" + new String(testBytes), Color.BLUE, 0);
                    byte[] recvData = mble.sendReceive(mSendCharact, mReceiveCharact, testBytes, 6000);
                    if (recvData != null && recvData.length > 0) {
                        addLogText("测试返回值" + recvData.length + "->" + new String(recvData), Color.GREEN, 0);

                    } else {
                        addLogText(new String(testBytes) + "的测试返回null", Color.RED, 0);
                    }

                }
            }
        }).start();
    }

    //发送数据
    void sendBottleOrders() {

        bottleOrderNo = 0;
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (bottleOrderNo < bottleBleAddress.size() * 4 * testTotalCount) {
                    if (bottleOrderNo % 4 == 0) {
                        if (logMsg!=null&&logMsg.getType()==1){
                            addLogText("错误： " +logMsg.getContent().substring(5)+"，未收到lc", Color.RED, mble.getGatt().getDevice().getName().length());
                        }else if (logMsg!=null&&logMsg.getType()==2&&!logMsg.getContent().contains("olc")&&!logMsg.getContent().contains("err")){
                            addLogText("错误： " +logMsg.getContent().substring(5)+"，未收到olc", Color.RED, mble.getGatt().getDevice().getName().length());
                        }
                        addLogText("提示：\r\n      " + "第" + bottleOrderNo / 4 + "次瓶子蓝牙开关瓶阀测试完成，" + mble.getGatt().getDevice().getName(), Color.BLACK, mble.getGatt().getDevice().getName().length());
                        if (mble.getGatt()!=null){
                            mble.getGatt().disconnect();
                            mble.getGatt().close();
                        }
                        mble.disconnectDevice();
                    } else if (bottleOrderNo % 4 == 1) {
                        final String bleAddress = bottleBleAddress.get(bottleOrderNo / 4 % bottleBleAddress.size());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectBle(bleAddress, 255, false);
                            }
                        });
                    } else if (bottleOrderNo % 4 == 2) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //sendLHBleData("Set OpenCap");
                                sendLHBleData("lo");
                            }
                        });

                    } else if (bottleOrderNo % 4 == 3) {
                        if (logMsg!=null&&logMsg.getType()==1){
                            addLogText("错误： " +logMsg.getContent().substring(5)+"，未收到lo", Color.RED, mble.getGatt().getDevice().getName().length());
                        }else if (logMsg!=null&&logMsg.getType()==2&&!logMsg.getContent().contains("olo")&&!logMsg.getContent().contains("err")){
                            addLogText("错误： " +logMsg.getContent().substring(5)+"，未收到olo", Color.RED, mble.getGatt().getDevice().getName().length());
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //sendLHBleData("Set CloseCap");
                                sendLHBleData("lc");
                            }
                        });

                    }
                    bottleOrderNo++;
                    try {
                        Thread.sleep(60 * 1000, 0);//200ms
                    } catch (Exception e) {

                    }
                }
                addLogText("提示：\r\n      " +"测试结束" + DateUtils.long2time(System.currentTimeMillis()), Color.BLACK, mble.getGatt().getDevice().getName().length());

            }

        }).start();
    }

    // 连接蓝牙
    void connectBle(final String bleAddress, final int sendDataLenMax, final boolean isConfig) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 10; i++) {
                    if (mble.connectDevice(bleAddress,bleCallBack))    //连接蓝牙设备
                        break;

                    mble.disconnectDevice();
                    try {
                        Thread.sleep(1000, 0);//200ms
                    } catch (Exception e) {

                    }
                }
                if (i == 5) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addLogText("连接失败" + bleAddress, Color.RED, 0);
                            showMsg(1,"连接失败");
                        }
                    });
                    return;
                }

                try {
                    Thread.sleep(1000, 0);//200ms
                } catch (Exception e) {

                }


                // 打开配置通知
                if (isConfig && !mble.openNotify(Constant.Ble_Config_Receive_Service)) {
                    mble.disconnectDevice();
                    addLogText("连接失败" + bleAddress, Color.RED, 0);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            showMsg(1,"连接失败");

                        }
                    });
                    return;
                }



                if (mble.openNotify(Constant.Ble_Data_Receive_Service)) {
                    if (mble.requestMtu(255)) {
                        mble.setSendDataLenMax(sendDataLenMax);
                        mble.mBleCallBack = bleCallBack;
                        addLogText("连接：\r\n      " + DateUtils.long2time(System.currentTimeMillis()) + " " + mble.getGatt().getDevice().getName(), Color.GREEN, mble.getGatt().getDevice().getName().length());
                        return;
                    } else {
                        if (mble.getGatt()!=null){
                            mble.getGatt().disconnect();
                            mble.getGatt().close();
                        }
                        mble.disconnectDevice();
						addLogText("连接失败3，" + bleAddress, Color.RED, 0);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showMsg(1,"连接失败3,设置MTU失败");

                            }
                        });
                    }
                } else {
                    mble.disconnectDevice();
                    addLogText("连接失败" + bleAddress, Color.RED, 0);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMsg(1,"连接失败");
                        }
                    });
                    return;
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
                addLogText("蓝牙已断开", Color.RED, 0);
                if (bottleOrderNo == -1) finish();
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
                addLogText("接收：\r\n      " + str, Color.GREEN, str.length());
            }
          /*  else if (characteristic.equals(mEVCharact)) {
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
                addLogText("接收：\r\n      " + str, Color.GREEN, str.length());
            }*/
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
/*
    private void checkNewApk() {
        byte[] binBytes = Utilty.getAssetBinData(BluetoothControlActivity.this, "NbiotRtuEP-V16_4_0_0-20201212.bin");
        if (binBytes != null && binBytes.length > 0) {
            int crcNum = Utilty.getItuCrc16(binBytes);
            int bagNum = binBytes.length / BinData.bagSize;
            if (binBytes.length % BinData.bagSize > 0) bagNum++;
            new HitiStringDailog(BluetoothControlActivity.this, Constant.Dailog_BIN_UPDATE_Hiti, bagNum, "Bin升级文件信息确认", "Bin文件共计" + binBytes.length + "个字节，CRC校验值：" + crcNum +
                    ",发送不超过" + BinData.bagSize + "个字节数据包需要" + bagNum + "个数据包", BluetoothControlActivity.this);
            binData = new BinData("16.4.0.0", binBytes, crcNum, bagNum);
        } else {
            Toast.makeText(BluetoothControlActivity.this, "未获取到用于蓝牙设备升级的Bin文件~", Toast.LENGTH_SHORT).show();
        }
    }*/

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.Dailog_BIN_UPDATE_Hiti:
                if (binData != null) {
                    sendBinData();
                }
                break;


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
                    //checkNewApk();
                    File file;
                    String binFileVision;
                    if (Build.VERSION.SDK_INT >= 29) {
                        file = FileUtil.uriToFileApiQ(BluetoothControlActivity.this, data.getData());
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < 29) {
                        String strPath = FileUtil.getFileAbsolutePath(BluetoothControlActivity.this, data.getData());
                        file = new File(strPath);
                    } else {
                        file = new File(data.getData().getPath());
                    }
                    if (file != null && file.getName() != null) {
                        binFileVision = FileUtil.binVisionNum(file.getName());
                        if (binFileVision.isEmpty()) {
                            showMsg(2,"请选择本设备可识别的Bin升级文件！");
                            return;
                        }

                    } else {
                        showMsg(2,"未识别出文件名称！");
                        return;
                    }
                    byte[] binBytes = FileUtil.readFile(file);
                    showMsg(2,"Bin文件字节数" + binBytes.length);
                    if (binBytes != null && binBytes.length > 0) {
                        int crcNum = Utilty.getItuCrc16(binBytes);
                        int bagNum = binBytes.length / BinData.bagSize;
                        if (binBytes.length % BinData.bagSize > 0) bagNum++;
                        new HitiStringDailog(BluetoothControlActivity.this, Constant.Dailog_BIN_UPDATE_Hiti, bagNum, "Bin升级文件" + binFileVision + "信息确认", "Bin文件共计" + binBytes.length + "个字节，CRC校验值：" + crcNum +
                                ",发送不超过" + BinData.bagSize + "个字节数据包需要" + bagNum + "个数据包", BluetoothControlActivity.this);
                        binData = new BinData(binFileVision, binBytes, crcNum, bagNum);
                    } else {
                        showMsg(2,"未获取到用于蓝牙设备升级的Bin文件~");
                    }
                    break;
            }
        }
    }
}
