package com.lh.lhble.UI;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;

import com.lh.lhble.Entity.LHADCPSysInfo;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommontClickListener;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;
import com.wise.wisekit.utils.DateUtils;

/**
 * 手持式多普勒流量计(LHADCP)主界面
 * 蓝牙地址16开头的设备
 */
public class LiherADCPActivity extends BaseActivity implements CommontClickListener, View.OnClickListener {
    private final static String TAG = LiherADCPActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_IS_CONFIG = "DEVICE_IS_CONFIG";

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean isConfig;
    private WiseBluetoothLe mble;
    private Context context;
    private Handler mHandler;
    private static final int MSG_DATA_CHANGE = 0x11;

    private LoadingDialog loadingDialog = null;

    // BLE服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;
    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private int currentSendType, currentReceiveType;

    // UI控件 - 设备信息
    private TextView tvBatteryInfo, tvFirmwareVersion, tvSignalStrength, tvWaterTemp;
    private TextView tvStorageInfo, tvDeviceName, tvDeviceMN, tvDeviceStatus;
    private TextView tvWorkMode, tvRunMode, tvMacAddress;
    private TextView tvRefresh;
    private TextView tvSysTime;
    
    // UI模式切换
    private LinearLayout llUIMode, llOrderMode;
    private ScrollView sclOrders;
    private LinearLayout llOrders;
    private EditText sendData;
    private TextView send, tvClearScreen;

    // UI控件 - 常用功能
    private Button btnSingleMeasure, btnHistoryData, btnCalibration;
    private Button btnUploadSettings, btnMultiMeasure;

    // 设备数据
    private LHADCPSysInfo sysInfo = new LHADCPSysInfo();

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_lhadcp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);

        context = this;
        setRightText("交互模式");
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectUIModeMenu();
            }
        });

        // 初始化BLE
        mble = WiseBluetoothLe.getInstance(getApplicationContext());
        mble.mBleCallBack = bleCallBack;

        // 初始化Handler
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        analysisMsg(msg.arg1, msg.arg2, (String) msg.obj);
                        break;
                }
                return false;
            }
        });

        // 延迟读取设备详情
        RxTimerUtil.cancel();
        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read Detail", Constant.Param_Read_Detail);
                showMsg(2, HJBleApplication.getAppContext().getString(R.string.init_data));
            }
        });
    }

    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherADCPActivity.class);
        intent.putExtra(LiherADCPActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherADCPActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherADCPActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }


    @Override
    protected void initView() {
        super.initView();

        // 初始化设备信息控件
        tvBatteryInfo = findViewById(R.id.tv_battery_info);
        tvFirmwareVersion = findViewById(R.id.tv_firmware_version);
        tvSignalStrength = findViewById(R.id.tv_signal_strength);
        tvWaterTemp = findViewById(R.id.tv_water_temp);
        tvStorageInfo = findViewById(R.id.tv_storage_info);
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvDeviceMN = findViewById(R.id.tv_device_mn);
        tvDeviceStatus = findViewById(R.id.tv_device_status);
        tvWorkMode = findViewById(R.id.tv_work_mode);
        tvRunMode = findViewById(R.id.tv_run_mode);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvRefresh = findViewById(R.id.tv_refresh);
        tvSysTime = findViewById(R.id.tv_sys_time);

        // 初始化功能按钮
        btnSingleMeasure = findViewById(R.id.btn_single_measure);
        btnHistoryData = findViewById(R.id.btn_history_data);
        btnCalibration = findViewById(R.id.btn_calibration);
        btnUploadSettings = findViewById(R.id.btn_upload_settings);
        btnMultiMeasure = findViewById(R.id.btn_multi_measure);

        // 设置点击事件
        tvRefresh.setOnClickListener(this);
        tvSysTime.setOnClickListener(this);
        btnSingleMeasure.setOnClickListener(this);
        btnHistoryData.setOnClickListener(this);
        btnCalibration.setOnClickListener(this);
        btnUploadSettings.setOnClickListener(this);
        btnMultiMeasure.setOnClickListener(this);
        
        // 初始化UI模式容器
        llUIMode = findViewById(R.id.ll_ui_mode);
        llOrderMode = findViewById(R.id.ll_order_mode);
        
        // 初始化窗口模式控件
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        sendData = findViewById(R.id.sendData);
        send = findViewById(R.id.send);
        tvClearScreen = findViewById(R.id.tv_clear_screen);
        send.setOnClickListener(this);
        tvClearScreen.setOnClickListener(this);

        // 初始化Loading对话框
        LoadingDialog.Builder loadBuilder = new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage(HJBleApplication.getAppContext().getString(R.string.loading_normal))
                .setCancelOutside(false)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if (loadingDialog != null) {
                            if (loadingDialog.cancelTimes > 1) {
                                mble.disconnectDevice();
                            } else {
                                loadingDialog.cancelTimes++;
                            }
                        }
                    }
                });
        loadingDialog = loadBuilder.create();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 根据配置模式选择服务通道
        if (HJBleApplication.shareInstance().isBleConfig() == 2) {
            mSendCharact = Constant.Ble_Config_Send_Service;
            mReceiveCharact = Constant.Ble_Config_Receive_Service;
        } else {
            mSendCharact = Constant.Ble_Data_Send_Service;
            mReceiveCharact = Constant.Ble_Data_Receive_Service;
        }

        setTitle(mDeviceName);

        if (!StringUtils.isEmpty(mDeviceAddress)) {
            tvMacAddress.setText(mDeviceAddress);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_refresh:
                refreshDetails();
                break;
            case R.id.tv_sys_time:
                // 用户手动点击修改系统时间
                new com.lh.lhble.UI.View.HitiInputStrDailog(LiherADCPActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherADCPActivity.this);
                break;
            case R.id.btn_single_measure:
                // 进入单点测量页面
                Intent singleMeasureIntent = LiherADCPSingleMeasureActivity.getIntent(
                    this, mDeviceName, mDeviceAddress, sysInfo);
                startActivity(singleMeasureIntent);
                break;
            case R.id.btn_history_data:
                // 进入历史数据页面
                Intent historyIntent = LiherADCPHistoryActivity.getIntent(
                    this, mDeviceName, mDeviceAddress, sysInfo);
                startActivity(historyIntent);
                break;
            case R.id.btn_calibration:
                // 进入参数设置与校准页面
                Intent paramSettingsIntent = LiherADCPParamSettingsActivity.getIntent(
                    this, mDeviceName, mDeviceAddress, sysInfo);
                startActivity(paramSettingsIntent);
                break;
            case R.id.btn_upload_settings:
                // TODO: 进入上传设置页面
                showMsg(2, "上传设置功能开发中");
                break;
            case R.id.btn_multi_measure:
                // TODO: 进入多线多点测量页面
                showMsg(2, "多线多点测量功能开发中");
                break;
            case R.id.send:
                // 窗口模式发送指令
                if (StringUtils.isEmpty(sendData.getText().toString())) {
                    showMsg(2, "请先输入指令内容");
                    return;
                }
                sendLHBleData(sendData.getText().toString().trim(), Constant.PARAM_LIHER_ORDER);
                sendData.setText("");
                break;
            case R.id.tv_clear_screen:
                // 清屏：清除窗口中的所有日志记录
                llOrders.removeAllViews();
                showMsg(0, "已清屏");
                break;
        }
    }

    /**
     * 显示UI模式选择菜单
     */
    private void showSelectUIModeMenu() {
        PopupMenu popupMenu = new PopupMenu(this, rightTitleTxt);
        popupMenu.inflate(R.menu.menu_select_ui_mode);
        popupMenu.getMenu().findItem(R.id.version_upgrade).setVisible(false);
        if (!HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {//无生产维修权限的用户无窗口模式功能
            popupMenu.getMenu().findItem(R.id.order_mode).setVisible(false);
        }

        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.ui_mode:
                        llUIMode.setVisibility(View.VISIBLE);
                        llOrderMode.setVisibility(View.GONE);
                        rightTitleTxt.setText("交互模式");
                        refreshDetails();
                        return true;
                    case R.id.order_mode:
                        android.util.Log.d(TAG, "Switching to order mode");
                        llUIMode.setVisibility(View.GONE);
                        llOrderMode.setVisibility(View.VISIBLE);
                        rightTitleTxt.setText("窗口模式");
                        android.util.Log.d(TAG, "llOrderMode visibility: " + llOrderMode.getVisibility());
                        return true;
                    default:
                        //do nothing
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * 刷新设备详情
     */
    private void refreshDetails() {
        RxTimerUtil.cancel();
        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read Detail", Constant.Param_Read_Detail);
            }
        });
    }

    /**
     * 发送BLE数据
     */
    public void sendLHBleData(String sendString, int sendType) {
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.empty_send_msg));
            return;
        }

        if (mble == null || !mble.isConnect()) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.ble_close_connect_again));
            finish();
            return;
        }

        if (LiherADCPActivity.this.isFinishing()) {
            return;
        }

        loadingDialog.show();
        addLogText(sendString, 0, sendType);

        byte[] bytes = ProtocolUtil.getBlueToothMsgForSend(
            HJBleApplication.shareInstance().getDesID(), 
            sendString
        );

        if (bytes == null) {
            return;
        }

        currentSendType = sendType;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mSendCharact, bytes)) {
                    addLogText(HJBleApplication.getAppContext().getString(R.string.send_fail) + "！", 
                        0, Constant.Return_Ble_SendFail);
                    currentSendType = 0;
                } else if (!mble.waitReturn(mReceiveCharact, 15 * 1000)) {
                    addLogText(getResources().getString(R.string.ble_rtn_timeout), 
                        1, Constant.Return_BLE_False);
                    currentSendType = 0;
                }
            }
        }).start();
    }

    /**
     * BLE回调
     */
    WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
        @Override
        public void OnWiseBluetoothState(int state) {
            if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED) {
                if (loadingDialog != null) loadingDialog.dismiss();
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close), Color.BLACK, 0);
                finish();
            }
        }

        @Override
        public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {
            if (characteristic.equals(mReceiveCharact)) {
                if (loadingDialog != null) loadingDialog.dismiss();

                String str = "";
                if (HJBleApplication.shareInstance().isBleHex()) {
                    str = ConvertData.bytesToHexString(recvData, false);
                } else if (HJBleApplication.shareInstance().isBleConfig() == 0) {
                    str = ProtocolUtil.getReturnMsgForReceive(recvData);
                } else {
                    str = new String(recvData);
                }

                if (!StringUtils.isEmpty(str)) {
                    // ========== 关键步骤：处理非正常返回数据 ==========
                    if (Constant.ReceiveFalse.equals(str)) {
                        // 情况1：蓝牙返回数据不符合力合协议格式
                        addLogText("蓝牙返回数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                        
                    } else if (Constant.ReceiveDataListFalse.equals(str)) {
                        // 情况2：蓝牙返回的分包数据不符合力合协议格式
                        addLogText("蓝牙返回分包数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                        
                    } else if (Constant.ReceiveContinue.equals(str)) {
                        // 情况3：设备正在返回数据，需要等待后续数据包
                        addLogText("蓝牙消息返回中", 1, Constant.Return_BLE_Continue);
                        
                    } else if (str.startsWith(Constant.SendDataListFalse)) {
                        // 情况4：发送的分包数据有误，提取错误信息
                        String errorMsg = str.substring(Constant.SendDataListFalse.length());
                        addLogText(errorMsg, 1, Constant.Return_Ble_ReceiveFalse);
                        
                    } else {
                        // ========== 正常数据：根据发送类型确定接收类型 ==========
                        switch (currentSendType) {
                            case Constant.PARAM_LIHER_ORDER:
                                currentReceiveType = Constant.RETURN_LIHER_OREDER;
                                break;
                            case Constant.Param_Read_Detail:
                                currentReceiveType = Constant.Return_Read_Detail;
                                break;
                            case Constant.Param_Wirte_SysTime:
                                currentReceiveType = Constant.Return_Wirte_SysTime;
                                break;
                            default:
                                currentReceiveType = 0;
                                return;
                        }
                        addLogText(str, 1, currentReceiveType);
                    }
                }

            } else if (characteristic.equals(mLogCharact)) {
                String logStr = ConvertData.bytesToHexString(recvData, false);
                addLogText(HJBleApplication.getAppContext().getString(R.string.note) + logStr, 
                    Color.rgb(139, 0, 255), logStr.length());
            }
        }

        @Override
        public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {
            if (!StringUtils.isEmpty(stateMsg)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        
                        int type = 2;
                        if (stateMsg.contains("成功")) {
                            type = 0;
                        } else if (stateMsg.contains("失败")) {
                            type = 1;
                        }
                        showMsg(type, stateMsg);
                    }
                });
            }
        }
    };

    /**
     * 添加日志
     */
    void addLogText(final String log, int type, int dataType) {
        Message message = new Message();
        message.what = MSG_DATA_CHANGE;
        message.arg1 = type;
        message.arg2 = dataType;
        message.obj = log;
        mHandler.sendMessage(message);
    }

    /**
     * 解析消息
     */
    private void analysisMsg(int arg1, int arg2, Object obj) {
        if (arg1 == 0) {//发送的消息
            String msgContext = (String) obj;
            
            // 所有发送的指令都显示在窗口日志中
            if (arg2 == Constant.PARAM_LIHER_ORDER || arg2 == Constant.Param_Read_Detail) {
                SpannableStringBuilder builder = new SpannableStringBuilder("发送:" + msgContext);
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            }
            
            if (arg2 == Constant.Return_BLE_False) {
                showMsg(2, msgContext);
                if (loadingDialog != null) loadingDialog.dismiss();
                // 错误信息显示在窗口日志中
                SpannableStringBuilder builder = new SpannableStringBuilder(msgContext);
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
                builder.setSpan(colorSpan, 0, msgContext.length() > 3 ? 3 : msgContext.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            }
            System.out.println("SendString:" + msgContext);
        } else if (arg1 == 1) {//接收到的消息
            String msgContext = (String) obj;
            switch (arg2) {
                case Constant.RETURN_LIHER_OREDER:
                    // 所有接收的数据都显示在窗口日志中
                    SpannableStringBuilder builder = new SpannableStringBuilder("接收:" + msgContext);
                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(139, 0, 255));
                    builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    TextView tView = new TextView(context);
                    tView.setText(builder);
                    llOrders.addView(tView);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                    break;
                    
                case Constant.Return_Read_Detail:
                    // 先显示在窗口日志中
                    SpannableStringBuilder builder2 = new SpannableStringBuilder("接收:" + msgContext);
                    ForegroundColorSpan colorSpan2 = new ForegroundColorSpan(Color.rgb(139, 0, 255));
                    builder2.setSpan(colorSpan2, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    TextView tView2 = new TextView(context);
                    tView2.setText(builder2);
                    llOrders.addView(tView2);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                    // 再解析数据更新UI
                    parseDetailData(msgContext);
                    break;
                    
                case Constant.Return_BLE_False:
                    showMsg(2, msgContext);
                    if (loadingDialog != null) loadingDialog.dismiss();
                    break;
                case Constant.Return_Wirte_SysTime:
                    if (msgContext != null && msgContext.contains("Write SysTime=")) {
                        if (sysInfo != null) {
                            sysInfo.setSysTime(msgContext.substring(14).trim());
                        }
                        tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" + sysInfo.getSysTime(), "yyyyMMddHHmmss")));
                        showMsg(0, "系统时间设置成功");
                    } else {
                        showMsg(1, "设置系统时间失败,\n" + msgContext);
                    }
                    break;
            }
        }
    }

    /**
     * 解析Read Detail返回数据
     * 严格按照《ADCP表头蓝牙通信协议.md》中的Read Detail指令格式解析
     * 共52个字段，以"#"分隔
     */
    private void parseDetailData(String msgContext) {
        try {
            String[] details = msgContext.split("#", -1);
            /*示例数据：
            260625141841#260527#ADCP00000001#000000000000#ADCP20250001
            #036137024154#30000#162600000001#0##24#0#60#0#5#5#0#000000000000#0#0
            #898604091825D0348837#460042962010424#0#000000000000
            #0.000#0.000#0.000#0.000#0.000#0.000#0.000#0.000#0.000
            #0.0000#0.0000#0#0.0000#0#0.0000#0#0.0000#0
            #0#0#0#0.00#0.00#0.00#0.00#0.00#0.00#
            */
            
            // 协议规定52个字段，至少需要52个字段
            if (details.length < 52) {
                showMsg(2, "数据格式异常，期望52个字段，实际：" + details.length);
                return;
            }

            // 创建新的系统信息对象
            LHADCPSysInfo info = new LHADCPSysInfo();

            // ========== 基础信息字段 (1-25) ==========
            
            // (1) 系统时间（12位）yyMMddhhmmss
            info.setSysTime(details[0]);
            
            // (2) 固件版本（6位）例如：230706
            info.setFirmwareVersion(details[1]);
            
            // (3) 设备名（12位）
            info.setDeviceName(details[2]);
            
            // (4) 设备唯一标识（12位）
            info.setDeviceId(details[3]);
            
            // (5) 设备所在断面MN号（12位）
            info.setDeviceMN(details[4]);
            
            // (6) 设备IP（12位）例：036137024154 表示36.137.24.154
            info.setDeviceIp(details[5]);
            
            // (7) 设备端口（5位）
            info.setDevicePort(details[6]);
            
            // (8) 蓝牙地址（12位）
            info.setBleAddress(details[7]);
            
            // (9) 设备状态（5位）见附录二
            if (StringUtils.isLong(details[8])) {
                info.setDeviceStatus(Long.parseLong(details[8]));
            }
            
            // (10) 操作账号名（24位）
            info.setOperatorName(details[9]);
            
            // (11) 设备电池电压（5位，小数点后两位）电压（9.8 ~ 10.6）对应电量0~100%
            if (StringUtils.isDouble(details[10])) {
                info.setBatteryVoltage(Double.parseDouble(details[10]));
            }
            
            // (12) 内存编号（3位）
            if (StringUtils.isInt(details[11])) {
                info.setMemoryNumber(Integer.parseInt(details[11]));
            }
            
            // (13) 运行模式（3位）0：自动模式 1：手动模式 2：在线模式 3：维护模式
            if (StringUtils.isInt(details[12])) {
                info.setRunMode(Integer.parseInt(details[12]));
            }
            
            // (14) 自动测试模式持续时间（5位，单位秒）
            if (StringUtils.isInt(details[13])) {
                info.setAutoTestDuration(Integer.parseInt(details[13]));
            }
            
            // (15) 手动模式持续时间（5位，单位秒）
            if (StringUtils.isInt(details[14])) {
                info.setManualTestDuration(Integer.parseInt(details[14]));
            }
            
            // (16) 在线模式采样间隔时间（5位，单位秒）
            if (StringUtils.isInt(details[15])) {
                info.setOnlineSampleInterval(Integer.parseInt(details[15]));
            }
            
            // (17) 在线模式上传间隔时间（5位，单位秒）
            if (StringUtils.isInt(details[16])) {
                info.setOnlineUploadInterval(Integer.parseInt(details[16]));
            }
            
            // (18) 在线模式下供电等待时间（5位，单位秒）SamWait
            if (StringUtils.isInt(details[17])) {
                info.setSamplingWaitTime(Integer.parseInt(details[17]));
            }
            
            // (19) 在线模式首次上传时间（12位）yyMMddhhmmss
            info.setStartTime(details[18]);
            
            // (20) 212协议ST编号
            info.setDevST(details[19]);
            
            // (21) 上传协议（0：212，1：MQTT）
            if (StringUtils.isInt(details[20])) {
                info.setUploadProtocol(Integer.parseInt(details[20]));
            }
            
            // (22) SIM卡NCCID号（20位）
            info.setSimNccid(details[21]);
            
            // (23) SIM卡IMSI号（15位）
            info.setSimImsi(details[22]);
            
            // (24) 网络信号强度（2位）
            if (StringUtils.isInt(details[23])) {
                info.setSignalStrength(Integer.parseInt(details[23]));
            }
            
            // (25) 本地IP（12位）
            info.setLocalIp(details[24]);
            
            // ========== 测量数据字段 (26-43) ==========
            
            // (26) 流速（小数，位数不定，m/s）
            if (!StringUtils.isEmpty(details[25]) && StringUtils.isDouble(details[25])) {
                info.setFlowVelocity(Double.parseDouble(details[25]));
            }
            
            // (27) 原始流速（小数，位数不定，m/s）
            if (!StringUtils.isEmpty(details[26]) && StringUtils.isDouble(details[26])) {
                info.setRawFlowVelocity(Double.parseDouble(details[26]));
            }
            
            // (28) 瞬时流量（小数，位数不定，m³/s）
            if (!StringUtils.isEmpty(details[27]) && StringUtils.isDouble(details[27])) {
                info.setInstantFlow(Double.parseDouble(details[27]));
            }
            
            // (29) 水深（小数，位数不定，m）
            if (!StringUtils.isEmpty(details[28]) && StringUtils.isDouble(details[28])) {
                info.setWaterDepth(Double.parseDouble(details[28]));
            }
            
            // (30) 水位（小数，位数不定，m）
            if (!StringUtils.isEmpty(details[29]) && StringUtils.isDouble(details[29])) {
                info.setWaterLevel(Double.parseDouble(details[29]));
            }
            
            // (31) 断面面积（小数，位数不定，m²）
            if (!StringUtils.isEmpty(details[30]) && StringUtils.isDouble(details[30])) {
                info.setCrossSectionArea(Double.parseDouble(details[30]));
            }
            
            // (32) 水温（小数，位数不定，摄氏度）
            if (!StringUtils.isEmpty(details[31]) && StringUtils.isDouble(details[31])) {
                info.setWaterTemp(Double.parseDouble(details[31]));
            }
            
            // (33) 横滚角（小数，位数不定，°）
            if (!StringUtils.isEmpty(details[32]) && StringUtils.isDouble(details[32])) {
                info.setRollAngle(Double.parseDouble(details[32]));
            }
            
            // (34) 俯仰角（小数，位数不定，°）
            if (!StringUtils.isEmpty(details[33]) && StringUtils.isDouble(details[33])) {
                info.setPitchAngle(Double.parseDouble(details[33]));
            }
            
            // (35) 间隔流量
            if (!StringUtils.isEmpty(details[34]) && StringUtils.isDouble(details[34])) {
                info.setIntervalFlow(Double.parseDouble(details[34]));
            }
            
            // (36) 净累计流量
            if (!StringUtils.isEmpty(details[35]) && StringUtils.isDouble(details[35])) {
                info.setNetCumulativeFlow(Double.parseDouble(details[35]));
            }
            
            // (37) 净累计流量进位次数
            if (StringUtils.isInt(details[36])) {
                info.setNetCumulativeCarry(Integer.parseInt(details[36]));
            }
            
            // (38) 正向累计流量
            if (!StringUtils.isEmpty(details[37]) && StringUtils.isDouble(details[37])) {
                info.setPositiveCumulativeFlow(Double.parseDouble(details[37]));
            }
            
            // (39) 正向累计流量进位次数
            if (StringUtils.isInt(details[38])) {
                info.setPositiveCumulativeCarry(Integer.parseInt(details[38]));
            }
            
            // (40) 反向累计流量
            if (!StringUtils.isEmpty(details[39]) && StringUtils.isDouble(details[39])) {
                info.setReverseCumulativeFlow(Double.parseDouble(details[39]));
            }
            
            // (41) 反向累计流量进位次数
            if (StringUtils.isInt(details[40])) {
                info.setReverseCumulativeCarry(Integer.parseInt(details[40]));
            }
            
            // (42) 绝对值累计流量
            if (!StringUtils.isEmpty(details[41]) && StringUtils.isDouble(details[41])) {
                info.setAbsCumulativeFlow(Double.parseDouble(details[41]));
            }
            
            // (43) 绝对值累计流量进位次数
            if (StringUtils.isInt(details[42])) {
                info.setAbsCumulativeCarry(Integer.parseInt(details[42]));
            }
            
            // ========== 配置参数字段 (44-52) ==========
            
            // (44) 河底高程（位数不定，mm）
            if (!StringUtils.isEmpty(details[43]) && StringUtils.isLong(details[43])) {
                info.setRiverbedElevation(Long.parseLong(details[43]));
            }
            
            // (45) 安装高度（小数，位数不定，m）
            if (!StringUtils.isEmpty(details[44]) && StringUtils.isDouble(details[44])) {
                info.setInstallHeight(Double.parseDouble(details[44]));
            }
            
            // (46) 断面形状（0:矩形 1：梯形 2：V形 3：U形 4：圆形）
            if (StringUtils.isInt(details[45])) {
                info.setCrossSectionShape(Integer.parseInt(details[45]));
            }
            
            // (47) 断面参数1（小数，位数不定，单位m）详见附录一
            if (!StringUtils.isEmpty(details[46]) && StringUtils.isDouble(details[46])) {
                info.setSectionParam1(Double.parseDouble(details[46]));
            }
            
            // (48) 断面参数2（小数，位数不定，单位m）详见附录一
            if (!StringUtils.isEmpty(details[47]) && StringUtils.isDouble(details[47])) {
                info.setSectionParam2(Double.parseDouble(details[47]));
            }
            
            // (49) 断面参数3（小数，位数不定，单位m）详见附录一
            if (!StringUtils.isEmpty(details[48]) && StringUtils.isDouble(details[48])) {
                info.setSectionParam3(Double.parseDouble(details[48]));
            }
            
            // (50) 曲线系数A（小数，位数不定）
            if (!StringUtils.isEmpty(details[49]) && StringUtils.isDouble(details[49])) {
                info.setCurveCoeffA(Double.parseDouble(details[49]));
            }
            
            // (51) 曲线系数B（小数，位数不定）
            if (!StringUtils.isEmpty(details[50]) && StringUtils.isDouble(details[50])) {
                info.setCurveCoeffB(Double.parseDouble(details[50]));
            }
            
            // (52) 曲线系数C（小数，位数不定）
            if (!StringUtils.isEmpty(details[51]) && StringUtils.isDouble(details[51])) {
                info.setCurveCoeffC(Double.parseDouble(details[51]));
            }

            // 保存到成员变量
            this.sysInfo = info;

            // 更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                    // 校验系统时间，与手机时间差异超过1分钟则弹窗提示用户校准
                    checkAndCorrectSysTime();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showMsg(2, "数据解析异常：" + e.getMessage());
        }
    }

    /**
     * 更新UI显示
     */
    private void updateUI() {
        // 电池电量（百分比 + 电压）
        int batteryPercent = calculateBatteryPercent(sysInfo.getBatteryVoltage());
        String batteryText = batteryPercent + "%\t" + String.format("%.2f",DoubleUtils.round(sysInfo.getBatteryVoltage(), 2)) + "V";
        tvBatteryInfo.setText(batteryText);
        
        // 低电告警（60%以下红色）
        if (batteryPercent < 60) {
            tvBatteryInfo.setTextColor(Color.RED);
        } else {
            tvBatteryInfo.setTextColor(Color.parseColor("#333333"));
        }
        
        // 固件版本
        tvFirmwareVersion.setText(StringUtils.isEmpty(sysInfo.getFirmwareVersion()) ? "--" : sysInfo.getFirmwareVersion());
        
        // 系统时间
        if (!StringUtils.isEmpty(sysInfo.getSysTime()) && sysInfo.getSysTime().startsWith("2")) {
            tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" + sysInfo.getSysTime(), "yyyyMMddHHmmss")));
        } else {
            tvSysTime.setText("--");
        }
        
        // 网络信号强度
        tvSignalStrength.setText(sysInfo.getSignalStrength() + " dBm");
        
        // 探头水温
        if (sysInfo.getWaterTemp() != -1) {
            tvWaterTemp.setText(String.format("%.2f ℃", sysInfo.getWaterTemp()));
        } else {
            tvWaterTemp.setText("--");
        }
        
        // 存储空间（TODO: 需要从协议获取具体数值）
        tvStorageInfo.setText("可用：-- GB / 已用：-- GB");
        
        // 设备名称
        String devName = StringUtils.isEmpty(sysInfo.getDeviceName()) ? "--" : sysInfo.getDeviceName().trim();
        tvDeviceName.setText(devName);
        if (!StringUtils.isEmpty(sysInfo.getDeviceName())) {
            setTitle(sysInfo.getDeviceName().trim());
        }
        
        // 设备MN号
        tvDeviceMN.setText(StringUtils.isEmpty(sysInfo.getDeviceMN()) ? "--" : sysInfo.getDeviceMN().trim());
        
        // 设备状态
        String statusText = StringUtils.getLHADCPDeviceStatusText(sysInfo.getDeviceStatus());
        tvDeviceStatus.setText(statusText);
        if (sysInfo.getDeviceStatus() == 0) {
            tvDeviceStatus.setTextColor(getResources().getColor(R.color.color_green_24ccaa));
        } else {
            tvDeviceStatus.setTextColor(getResources().getColor(R.color.color_red));
        }
        
        // 工作模式（TODO: 需要根据协议确认字段）
        tvWorkMode.setText(StringUtils.getLHADCPRunModeText(sysInfo.getRunMode()));
        
        // 运行模式
        tvRunMode.setText(StringUtils.getLHADCPRunModeText(sysInfo.getRunMode()));
        
        // MAC地址
        if (!StringUtils.isEmpty(mDeviceAddress)) {
            tvMacAddress.setText(mDeviceAddress);
        }
    }

    /**
     * 计算电池百分比
     * 电压（9.8 ~ 10.6）对应电量0%~100%
     */
    private int calculateBatteryPercent(double voltageInCentiVolt) {
        double voltage = voltageInCentiVolt;
        
        if (voltage <= 9.8) {
            return 0;
        } else if (voltage >= 10.6) {
            return 100;
        } else {
            return (int) ((voltage - 9.8) / (10.6 - 9.8) * 100);
        }
    }

    /**
     * 处理返回键
     * 窗口模式下返回交互模式，交互模式下退出页面
     */
    @Override
    public void onBackPressed() {
        // 调试日志
        android.util.Log.d(TAG, "onBackPressed called, llOrderMode=" + llOrderMode + ", visibility=" + (llOrderMode != null ? llOrderMode.getVisibility() : "null"));
        
        if (llOrderMode != null && llOrderMode.getVisibility() == View.VISIBLE) {
            // 当前是窗口模式，切换回交互模式
            android.util.Log.d(TAG, "Switching to UI mode");
            llUIMode.setVisibility(View.VISIBLE);
            llOrderMode.setVisibility(View.GONE);
            rightTitleTxt.setText("交互模式");
            showMsg(0, "已切换到交互模式");
            // 不调用 super.onBackPressed()，阻止退出
        } else {
            // 当前是交互模式，正常退出
            android.util.Log.d(TAG, "Exiting activity");
            super.onBackPressed();
        }
    }

    /**
     * 校验设备系统时间，与手机时间差异超过1分钟则弹窗提示用户校准
     */
    private void checkAndCorrectSysTime() {
        String deviceSysTime = sysInfo.getSysTime();
        
        if (!StringUtils.isEmpty(deviceSysTime) && deviceSysTime.startsWith("2")) {
            // 系统时间格式正常，比较与手机时间的差异
            long deviceTimeMillis = DateUtil.format2miao("20" + deviceSysTime, "yyyyMMddHHmmss");
            if (Math.abs(System.currentTimeMillis() - deviceTimeMillis) > 1 * 60 * 1000) {
                new com.lh.lhble.UI.View.HitiInputStrDailog(LiherADCPActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherADCPActivity.this);
            }
        } else {
            // 系统时间不合法，直接弹窗要求设置
            new com.lh.lhble.UI.View.HitiInputStrDailog(LiherADCPActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherADCPActivity.this);
        }
    }

    /**
     * CommontClickListener回调 - 处理HitiInputStrDailog对话框的确认操作
     */
    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.Dailog_BLE_SYSDateTime:
                String sysTime = (String) target;
                if (sysTime != null && sysTime.length() == 19) {
                    // 用户输入格式：yyyy-MM-dd HH:mm:ss (19位)
                    // 转换为 yyMMddHHmmss (12位) 后发送
                    String formattedTime = DateUtils.formatTime(sysTime, DateUtils.DATE_TIME_PATTERN, DateUtils.METHOD_DATE_TIME_PATTERN).substring(2);
                    sendLHBleData("Write SysTime=" + formattedTime, Constant.Param_Wirte_SysTime);
                } else {
                    showMsg(2, "请录入正确时间，" + sysTime);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxTimerUtil.cancel();
        mble.disconnectDevice();
        mble.disconnectLocalDevice();
    }
}
