package com.lh.lhble.UI;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lh.lhble.Entity.LHADCPSysInfo;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * LHADCP实时数据面板页面
 */
public class LiherADCPRealtimeDataActivity extends BaseActivity implements View.OnClickListener {
    private final static String TAG = LiherADCPRealtimeDataActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_SYS_INFO = "SYS_INFO";

    private String mDeviceName;
    private String mDeviceAddress;
    private LHADCPSysInfo sysInfo;

    private WiseBluetoothLe mble;
    private Handler mHandler;
    private static final int MSG_DATA_CHANGE = 0x11;

    private LoadingDialog loadingDialog = null;

    // BLE服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;
    private int currentSendType, currentReceiveType;

    // UI控件
    private TextView tvMeasureTime, tvSectionMN, tvSectionName;
    private TextView tvFlowVelocity, tvRawFlowVelocity, tvInstantFlow, tvCrossSectionArea;
    private TextView tvCumulativeFlow, tvWaterDepth, tvWaterTemp;
    private TextView tvWaterLevel, tvRollAngle, tvPitchAngle, tvIntervalFlow;
    private LinearLayout llCumulativeFlow;
    private TextView tvOperatorName;

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_lhadcp_realtime_data;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        sysInfo = (LHADCPSysInfo) intent.getParcelableExtra(EXTRAS_SYS_INFO);

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

        // 自动读取实时数据
        RxTimerUtil.cancel();
        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                refreshRealtimeData();
            }
        });
    }

    public static Intent getIntent(Context context, String name, String mac, LHADCPSysInfo info) {
        Intent intent = new Intent(context, LiherADCPRealtimeDataActivity.class);
        intent.putExtra(LiherADCPRealtimeDataActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherADCPRealtimeDataActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherADCPRealtimeDataActivity.EXTRAS_SYS_INFO, info);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();

        // 初始化UI控件
        tvMeasureTime = findViewById(R.id.tv_measure_time);
        tvSectionMN = findViewById(R.id.tv_section_mn);
        tvSectionName = findViewById(R.id.tv_section_name);
        tvFlowVelocity = findViewById(R.id.tv_flow_velocity);
        tvRawFlowVelocity = findViewById(R.id.tv_raw_flow_velocity);
        tvInstantFlow = findViewById(R.id.tv_instant_flow);
        tvCrossSectionArea = findViewById(R.id.tv_cross_section_area);
        tvCumulativeFlow = findViewById(R.id.tv_cumulative_flow);
        tvWaterDepth = findViewById(R.id.tv_water_depth);
        tvWaterTemp = findViewById(R.id.tv_water_temp);
        tvWaterLevel = findViewById(R.id.tv_water_level);
        tvRollAngle = findViewById(R.id.tv_roll_angle);
        tvPitchAngle = findViewById(R.id.tv_pitch_angle);
        tvIntervalFlow = findViewById(R.id.tv_interval_flow);
        llCumulativeFlow = findViewById(R.id.ll_cumulative_flow);
        tvOperatorName = findViewById(R.id.tv_operator_name);

        // 设置点击事件（BaseActivity已提供左上角返回按钮）
        rightTitleTxt.setText("刷新");
        rightTitleTxt.setOnClickListener(this);

        // 初始化Loading对话框
        LoadingDialog.Builder loadBuilder = new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage(HJBleApplication.getAppContext().getString(R.string.loading_normal))
                .setCancelOutside(false);
        loadingDialog = loadBuilder.create();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 设置标题栏（使用 BaseActivity 的标题栏）
        setTitle("实时数据面板");
        setRightText("刷新");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.right_txt:  // 右上角刷新按钮
                refreshRealtimeData();
                break;
        }
    }

    /**
     * 刷新实时数据
     */
    private void refreshRealtimeData() {
        sendLHBleData("Read Detail", Constant.Param_Read_Detail);
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

        loadingDialog.show();

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
                            case Constant.Param_Read_Detail:
                                currentReceiveType = Constant.Return_Read_Detail;
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
        if (arg1 == 1) {
            // 接收的消息
            String msgContext = (String) obj;
            
            switch (arg2) {
                case Constant.Return_Read_Detail:
                    parseRealtimeData(msgContext);
                    break;
                    
                case Constant.Return_BLE_False:
                    showMsg(2, msgContext);
                    if (loadingDialog != null) loadingDialog.dismiss();
                    break;
            }
        }
    }

    /**
     * 解析实时数据
     */
    private void parseRealtimeData(String msgContext) {
        try {
            String[] details = msgContext.split("#", -1);
            //000111054722#260527#ADCP00000001#          #ADCP20250001#036137024154#30000#162600000001#0#陈翔#11.78
            // #24#0#59#0#5#5#60#260711174200#0#0#898604091825D0348837#460042962010424#99#000000000000#0.000#0.000
            // #0.000#0.000#0.030#0.000#25.000#15.600#-2.486#0.0000#0.0000#0#0.0000#0#0.0000#0#0.0000#0#0#0#1#0.05
            // #0.06#0.07#1.00#0.00#0.00#
            if (details.length < 45) {
                showMsg(2, "数据格式异常");
                return;
            }

            // 创建新的系统信息对象
            LHADCPSysInfo info = new LHADCPSysInfo();

            // 解析所有字段（与主页面相同）
            info.setSysTime(details[0]);
            info.setFirmwareVersion(details[1]);
            info.setDeviceName(details[2]);
            info.setDeviceId(details[3]);
            info.setDeviceMN(details[4]);
            info.setDeviceIp(details[5]);
            info.setDevicePort(details[6]);
            info.setBleAddress(details[7]);
            
            if (StringUtils.isLong(details[8])) {
                info.setDeviceStatus(Long.parseLong(details[8]));
            }
            
            info.setOperatorName(details[9]);
            
            if (StringUtils.isInt(details[10])) {
                info.setBatteryVoltage(Integer.parseInt(details[10]));
            }
            
            if (StringUtils.isInt(details[11])) {
                info.setMemoryNumber(Integer.parseInt(details[11]));
            }
            
            if (StringUtils.isInt(details[12])) {
                info.setRunMode(Integer.parseInt(details[12]));
            }
            
            if (StringUtils.isInt(details[13])) {
                info.setAutoTestDuration(Integer.parseInt(details[13]));
            }
            
            if (StringUtils.isInt(details[14])) {
                info.setManualTestDuration(Integer.parseInt(details[14]));
            }
            
            if (StringUtils.isInt(details[15])) {
                info.setOnlineSampleInterval(Integer.parseInt(details[15]));
            }
            
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

            // 更新sysInfo
            this.sysInfo = info;

            // 更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateRealtimeUI();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showMsg(2, "数据解析异常：" + e.getMessage());
        }
    }

    /**
     * 更新实时数据UI
     */
    private void updateRealtimeUI() {
        if (sysInfo == null) return;

        // 测量时间
        String sysTime = sysInfo.getSysTime();
        if (!StringUtils.isEmpty(sysTime) && sysTime.length() >= 12) {
            // 格式：yyMMddHHmmss -> yyyy-MM-dd HH:mm:ss
            try {
                String year = "20" + sysTime.substring(0, 2);
                String month = sysTime.substring(2, 4);
                String day = sysTime.substring(4, 6);
                String hour = sysTime.substring(6, 8);
                String minute = sysTime.substring(8, 10);
                String second = sysTime.substring(10, 12);
                tvMeasureTime.setText(year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second);
            } catch (Exception e) {
                tvMeasureTime.setText("--");
            }
        } else {
            tvMeasureTime.setText("--");
        }

        // 断面MN
        tvSectionMN.setText(StringUtils.isEmpty(sysInfo.getDeviceMN()) ? "--" : sysInfo.getDeviceMN());

        // 操作人
        tvOperatorName.setText(StringUtils.isEmpty(sysInfo.getOperatorName()) ? "--" : sysInfo.getOperatorName());
        
        // 断面名称（预留）
        tvSectionName.setText("--（预留）");

        // 流速（3位小数）
        if (sysInfo.getFlowVelocity() != -1) {
            tvFlowVelocity.setText(String.format(Locale.getDefault(), "%.3f", sysInfo.getFlowVelocity()));
        } else {
            tvFlowVelocity.setText("--");
        }

        // 原始流速（3位小数）
        if (sysInfo.getRawFlowVelocity() != -1) {
            tvRawFlowVelocity.setText(String.format(Locale.getDefault(), "%.3f", sysInfo.getRawFlowVelocity()));
        } else {
            tvRawFlowVelocity.setText("--");
        }

        // 瞬时流量（3位小数）
        if (sysInfo.getInstantFlow() != -1) {
            tvInstantFlow.setText(String.format(Locale.getDefault(), "%.3f", sysInfo.getInstantFlow()));
        } else {
            tvInstantFlow.setText("--");
        }

        // 过水面积（3位小数）
        if (sysInfo.getCrossSectionArea() != -1) {
            tvCrossSectionArea.setText(String.format(Locale.getDefault(), "%.3f", sysInfo.getCrossSectionArea()));
        } else {
            tvCrossSectionArea.setText("--");
        }

        // 累计流量（仅自动模式显示，3位小数）
        if (sysInfo.getRunMode() == 0) {
            llCumulativeFlow.setVisibility(View.VISIBLE);
            if (sysInfo.getPositiveCumulativeFlow() != -1) {
                tvCumulativeFlow.setText(String.format(Locale.getDefault(), "%.3f", sysInfo.getPositiveCumulativeFlow()));
            } else {
                tvCumulativeFlow.setText("--");
            }
        } else {
            llCumulativeFlow.setVisibility(View.GONE);
        }

        // 水深（3位小数）
        if (sysInfo.getWaterDepth() != -1) {
            tvWaterDepth.setText(String.format(Locale.getDefault(), "%.3f", sysInfo.getWaterDepth()));
        } else {
            tvWaterDepth.setText("--");
        }

        // 水温（1位小数）
        if (sysInfo.getWaterTemp() != -1) {
            tvWaterTemp.setText(String.format(Locale.getDefault(), "%.1f", sysInfo.getWaterTemp()));
        } else {
            tvWaterTemp.setText("--");
        }

        // 水位（3位小数）
        if (sysInfo.getWaterLevel() != -1) {
            tvWaterLevel.setText(String.format(Locale.getDefault(), "%.3f", sysInfo.getWaterLevel()));
        } else {
            tvWaterLevel.setText("--");
        }

        // 横滚角
        if (sysInfo.getRollAngle() != -1) {
            tvRollAngle.setText(String.format(Locale.getDefault(), "%.2f", sysInfo.getRollAngle()));
        } else {
            tvRollAngle.setText("--");
        }

        // 俯仰角
        if (sysInfo.getPitchAngle() != -1) {
            tvPitchAngle.setText(String.format(Locale.getDefault(), "%.2f", sysInfo.getPitchAngle()));
        } else {
            tvPitchAngle.setText("--");
        }

        // 间隔流量
        if (sysInfo.getIntervalFlow() != -1) {
            tvIntervalFlow.setText(String.format(Locale.getDefault(), "%.3f", sysInfo.getIntervalFlow()));
        } else {
            tvIntervalFlow.setText("--");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxTimerUtil.cancel();

    }
}
