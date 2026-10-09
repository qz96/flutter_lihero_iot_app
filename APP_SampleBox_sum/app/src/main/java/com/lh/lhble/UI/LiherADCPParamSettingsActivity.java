package com.lh.lhble.UI;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import com.lh.lhble.Entity.LHADCPSysInfo;
import com.lh.lhble.R;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 参数设置与校准页面
 * 实现8项功能需求：
 * 1. 压力水位计零点校准
 * 2. 曲线系数ABC配置
 * 3. 恢复出厂设置
 * 4. 系统时间校准
 * 5. 流量单位选择
 * 6. 河底高程设置
 * 7. 安装高度设置
 * 8. 安装方向设置
 */
public class LiherADCPParamSettingsActivity extends BaseActivity implements View.OnClickListener {
    private final static String TAG = LiherADCPParamSettingsActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_SYS_INFO = "SYS_INFO";

    private String mDeviceName;
    private String mDeviceAddress;
    private LHADCPSysInfo sysInfo;
    private Context context;
    private Handler mHandler;
    private static final int MSG_DATA_CHANGE = 0x11;

    private LoadingDialog loadingDialog = null;

    // BLE服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;
    private int currentSendType, currentReceiveType;

    // UI控件
    private Button btnPressureCalibration, btnSaveCoeff, btnFactoryReset;
    private Button btnSetPhoneTime, btnSaveSysTime, btnSaveElevation;
    private Button btnSaveInstallHeight, btnSaveDirection;
    private EditText etCoeffA, etCoeffB, etCoeffC, etSysTime;
    private EditText etElevation, etInstallHeight;
    private RadioGroup rgFlowUnit, rgDirection;
    private RadioButton rbM3s, rbLs, rbForward, rbReverse;

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_lhadcp_param_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        sysInfo = (LHADCPSysInfo) intent.getParcelableExtra(EXTRAS_SYS_INFO);

        context = this;

        // 初始化BLE
        WiseBluetoothLe mble = WiseBluetoothLe.getInstance(getApplicationContext());
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

        // 加载设备参数到UI
        if (sysInfo != null) {
            loadSysInfoToUI();
        }
    }

    public static Intent getIntent(Context context, String name, String mac, LHADCPSysInfo info) {
        Intent intent = new Intent(context, LiherADCPParamSettingsActivity.class);
        intent.putExtra(LiherADCPParamSettingsActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherADCPParamSettingsActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherADCPParamSettingsActivity.EXTRAS_SYS_INFO, info);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();

        // 初始化UI控件
        btnPressureCalibration = findViewById(R.id.btn_pressure_calibration);
        btnSaveCoeff = findViewById(R.id.btn_save_coeff);
        btnFactoryReset = findViewById(R.id.btn_factory_reset);
        btnSetPhoneTime = findViewById(R.id.btn_set_phone_time);
        btnSaveSysTime = findViewById(R.id.btn_save_sys_time);
        btnSaveElevation = findViewById(R.id.btn_save_elevation);
        btnSaveInstallHeight = findViewById(R.id.btn_save_install_height);
        btnSaveDirection = findViewById(R.id.btn_save_direction);
        
        etCoeffA = findViewById(R.id.et_coeff_a);
        etCoeffB = findViewById(R.id.et_coeff_b);
        etCoeffC = findViewById(R.id.et_coeff_c);
        etSysTime = findViewById(R.id.et_sys_time);
        etElevation = findViewById(R.id.et_elevation);
        etInstallHeight = findViewById(R.id.et_install_height);
        
        rgFlowUnit = findViewById(R.id.rg_flow_unit);
        rgDirection = findViewById(R.id.rg_direction);
        rbM3s = findViewById(R.id.rb_m3s);
        rbLs = findViewById(R.id.rb_ls);
        rbForward = findViewById(R.id.rb_forward);
        rbReverse = findViewById(R.id.rb_reverse);

        // 设置点击事件（BaseActivity已提供左上角返回按钮）
        btnPressureCalibration.setOnClickListener(this);
        btnSaveCoeff.setOnClickListener(this);
        btnFactoryReset.setOnClickListener(this);
        btnSetPhoneTime.setOnClickListener(this);
        btnSaveSysTime.setOnClickListener(this);
        btnSaveElevation.setOnClickListener(this);
        btnSaveInstallHeight.setOnClickListener(this);
        btnSaveDirection.setOnClickListener(this);

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
                                // mble.disconnectDevice();
                            } else {
                                loadingDialog.cancelTimes++;
                            }
                        }
                    }
                });
        loadingDialog = loadBuilder.create();
    }

    /**
     * 加载系统信息到UI
     */
    private void loadSysInfoToUI() {
        // 曲线系数
        if (sysInfo.getCurveCoeffA() != -1) {
            etCoeffA.setText(String.valueOf(sysInfo.getCurveCoeffA()));
        }
        if (sysInfo.getCurveCoeffB() != -1) {
            etCoeffB.setText(String.valueOf(sysInfo.getCurveCoeffB()));
        }
        if (sysInfo.getCurveCoeffC() != -1) {
            etCoeffC.setText(String.valueOf(sysInfo.getCurveCoeffC()));
        }
        
        // 河底高程（mm转m）
        if (sysInfo.getRiverbedElevation() != -1) {
            double elevationInMeters = sysInfo.getRiverbedElevation() / 1000.0;
            etElevation.setText(String.format(Locale.getDefault(), "%.3f", elevationInMeters));
        }
        
        // 安装高度
        if (sysInfo.getInstallHeight() != -1) {
            etInstallHeight.setText(String.format(Locale.getDefault(), "%.3f", sysInfo.getInstallHeight()));
        }
        
        // 安装方向
        // TODO: 需要从协议中获取安装方向字段，当前默认为正向
        rbForward.setChecked(true);
        
        // 设置默认系统时间为手机当前时间
        setDefaultSystemTime();
    }

    /**
     * 设置默认系统时间为手机当前时间
     */
    private void setDefaultSystemTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        etSysTime.setText(currentTime);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 设置标题栏（使用 BaseActivity 的标题栏）
        setTitle("参数设置与校准");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pressure_calibration:
                pressureCalibration();
                break;
            case R.id.btn_save_coeff:
                saveCurveCoefficients();
                break;
            case R.id.btn_factory_reset:
                factoryReset();
                break;
            case R.id.btn_set_phone_time:
                setPhoneTime();
                break;
            case R.id.btn_save_sys_time:
                saveSystemTime();
                break;
            case R.id.btn_save_elevation:
                saveElevation();
                break;
            case R.id.btn_save_install_height:
                saveInstallHeight();
                break;
            case R.id.btn_save_direction:
                saveDirection();
                break;
        }
    }

    /**
     * 1. 压力水位计零点校准
     */
    private void pressureCalibration() {
        new AlertDialog.Builder(this)
            .setTitle("确认校准")
            .setMessage("请将仪器放置在空气中，进行压力水位计零点校准")
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sendLHBleData("Set Pressure", Constant.Param_Set_Pressure);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 2. 保存曲线系数ABC
     */
    private void saveCurveCoefficients() {
        String coeffA = etCoeffA.getText().toString().trim();
        String coeffB = etCoeffB.getText().toString().trim();
        String coeffC = etCoeffC.getText().toString().trim();
        
        if (TextUtils.isEmpty(coeffA) || TextUtils.isEmpty(coeffB) || TextUtils.isEmpty(coeffC)) {
            showMsg(2, "请填写完整的曲线系数A、B、C");
            return;
        }
        
        if (!StringUtils.isDouble(coeffA) || !StringUtils.isDouble(coeffB) || !StringUtils.isDouble(coeffC)) {
            showMsg(2, "曲线系数必须为数字");
            return;
        }
        
        String coeffStr = coeffA + "-" + coeffB + "-" + coeffC;
        sendLHBleData("Write Coeff=" + coeffStr, Constant.Param_Write_Coeff);
    }

    /**
     * 3. 恢复出厂设置
     */
    private void factoryReset() {
        new AlertDialog.Builder(this)
            .setTitle("警告")
            .setMessage("确定要恢复出厂默认参数吗？此操作不可撤销！")
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sendLHBleData("Set SysInit", Constant.Param_Set_SysInit);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 4. 使用手机当前时间
     */
    private void setPhoneTime() {
        setDefaultSystemTime();
        showMsg(0, "已设置为手机当前时间");
    }

    /**
     * 4. 保存系统时间
     */
    private void saveSystemTime() {
        String sysTime = etSysTime.getText().toString().trim();
        
        if (TextUtils.isEmpty(sysTime)) {
            showMsg(2, "请输入系统时间");
            return;
        }
        
        if (sysTime.length() != 12) {
            showMsg(2, "系统时间格式错误，应为12位（yyMMddHHmmss）");
            return;
        }
        
        sendLHBleData("Write SysTime=" + sysTime, Constant.Param_Write_SysTime);
    }

    /**
     * 5. 流量单位选择（仅本地存储，不发送到设备）
     */
    // 注：流量单位由APP本地控制，不需要发送到设备

    /**
     * 6. 保存河底高程
     */
    private void saveElevation() {
        String elevation = etElevation.getText().toString().trim();
        
        if (TextUtils.isEmpty(elevation)) {
            showMsg(2, "请输入河底高程");
            return;
        }
        
        if (!StringUtils.isDouble(elevation)) {
            showMsg(2, "河底高程必须为数字");
            return;
        }
        
        // 米转毫米
        double elevationInMeters = Double.parseDouble(elevation);
        long elevationInMM = (long) (elevationInMeters * 1000);
        
        sendLHBleData("Write Elevation=" + elevationInMM, Constant.Param_Write_Elevation);
    }

    /**
     * 7. 保存安装高度
     */
    private void saveInstallHeight() {
        String installHeight = etInstallHeight.getText().toString().trim();
        
        if (TextUtils.isEmpty(installHeight)) {
            showMsg(2, "请输入安装高度");
            return;
        }
        
        if (!StringUtils.isDouble(installHeight)) {
            showMsg(2, "安装高度必须为数字");
            return;
        }
        
        // 米转毫米
        double heightInMeters = Double.parseDouble(installHeight);
        long heightInMM = (long) (heightInMeters * 1000);
        
        sendLHBleData("Write Install=" + heightInMM, Constant.Param_Write_Install);
    }

    /**
     * 8. 保存安装方向
     */
    private void saveDirection() {
        int direction = rbForward.isChecked() ? 1 : -1;
        sendLHBleData("Write Direction=" + direction, Constant.Param_Write_Direction);
    }

    /**
     * 发送BLE数据
     */
    public void sendLHBleData(String sendString, int sendType) {
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.empty_send_msg));
            return;
        }

        WiseBluetoothLe mble = WiseBluetoothLe.getInstance(getApplicationContext());
        if (mble == null || !mble.isConnect()) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.ble_close_connect_again));
            finish();
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
                            case Constant.Param_Set_Pressure:
                            case Constant.Param_Set_SysInit:
                            case Constant.Param_Write_Coeff:
                            case Constant.Param_Write_SysTime:
                            case Constant.Param_Write_Elevation:
                            case Constant.Param_Write_Install:
                            case Constant.Param_Write_Direction:
                                currentReceiveType = currentSendType+1; // Write/Set指令返回相同内容表示成功
                                break;
                            default:
                                currentReceiveType = 0;
                                return;
                        }
                        addLogText(str, 1, currentReceiveType);
                    }
                }
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
        if (arg1 == 0) {
            // 发送的消息
            String msgContext = (String) obj;
            System.out.println("SendString:" + msgContext);
            
        } else if (arg1 == 1) {
            // 接收的消息
            String msgContext = (String) obj;
            
            // 判断是否成功（Write/Set指令返回相同内容表示成功）
            boolean isSuccess = msgContext.contains("Set ") || msgContext.contains("Write ");
            
            if (isSuccess) {
                showMsg(0, "操作成功");
            } else {
                showMsg(2, "操作失败：" + msgContext);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
