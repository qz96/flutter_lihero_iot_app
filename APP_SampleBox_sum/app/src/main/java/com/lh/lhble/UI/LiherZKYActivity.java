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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DlgTime;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.UI.View.LiquidRecordDailog;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.UI.View.SelecterObjectDailog;
import com.lh.lhble.google.zxing.activity.CaptureActivity;
import com.lh.lhble.listener.CommonItemOnClickListener3;
import com.lh.lhble.network.RetrofitUtils;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseFragmentActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.lh.lhble.google.zxing.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;

/**
 * 质控仪
 */
public class LiherZKYActivity extends BaseFragmentActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherZKYActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    // 是否支持配置
    public static final String EXTRAS_DEVICE_IS_CONFIG = "DEVICE_IS_CONFIG";

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean isConfig;
    private WiseBluetoothLe mble;
    private Context context;
    private Handler mHandler;
    private static final int MSG_DATA_CHANGE = 0x11;
    private String sampleId = "zhikongyi";
    private ScrollView sclUIMode;
    private LinearLayout llUIMode;
    private TextView tvRefresh, tvDevVersion, tvDevAddress, tvTemperature, tvWorkStatus, tvMacAddress, tvSetMacAddress, tvStdVol, tvSampleVol, tvSurplusVol, tvPumpRatio, tvStandardSet, tvStandardAdd, tvPumpExtract, tvPumpPut,
            tvResetLiquid, tvSurplusDensity, tvDevReset, tvDevOpen, tvWashNo,tvCCSTime,tvRXTime,tvChTime;
    private ImageView iv0, iv1, iv2, iv3, iv4, iv5, ivStandardBottle, ivSampleBottle, ivTempCom;
    private ScrollView sclOrders;
    private LinearLayout llOrderMode, llOrders;
    private ImageView iv7, iv8, iv9, iv10, ivBtn0, ivBtn1, ivBtn2, ivBtn3, ivBtn4, ivBtn5;
    private TextView tvZb0, tvZb1, tvZb2, tvZb3, tvZb4, tvYw0, tvYw1, tvYw2, tvYw3, tvParamsSet, tvLiquidRecord;
    private EditText sendData;
    private TextView send;
    private TextView tvYWRefresh, tvYW1Info, tvYW2ADInfo, tvYW2Info, tvYW4Info;
    private LoadingDialog loadingDialog = null;

    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;

    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private int currentSendType, currentReceiveType;

    private String tempBottleMac;
    public String version;
    private int devAdd;
    private int devStatus;
    private int temperature;
    private double standerVol;
    private double sampleVol;
    private int rubbishVol;
    private int surplusVol;
    private int pumpRatio;
    private boolean pumpStatus, armStatus, buttonStatus, doorStatus;
    private int hardVersion;
    private long liquidRefreshTime, liquidOutTime, tempResetTime, tempOutTime;
    private int yw1th, yw2th, yw4th, yw2adth;//液位阈值
    private int zb1Time, zb2Time, zbDuty, vMax, pumpDuty, liquidStep;
    private int ywTime = -1, pyTime;//液位间隔时间，排样时间
    private int surplusDensity100;//母液浓度*100的整数
    private int tempcom = -1;//柱塞泵温补开关。0为关，1为开 -1为未升级
    private int washno = -1;//配标润洗次数，要么为1，要么为2 -1为未升级
    private int ccstime=-1;//抽纯水时间,默认单位0.01s
    private int rxtime=-1,chtime=-1;//配标润洗时间，配标侧吸定容时间，默认单位0.01s
    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_zky;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);

        topLeftBtn.setVisibility(View.VISIBLE);
        context = this;
        if (HJBleApplication.getUser().getStrPermissions().contains("PRO_Qualification") || HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {
            setRightText("交互模式");
            rightTitleTxt.setVisibility(View.VISIBLE);
            rightTitleTxt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSelectUIModeMenu();
                }
            });
        } else {
            rightTitleTxt.setVisibility(View.GONE);
        }


        mble = WiseBluetoothLe.getInstance(getApplicationContext());    //获取蓝牙实例
        mble.mBleCallBack = bleCallBack;

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        String strData = (String) msg.obj;
                        analysisMsg(msg.arg1, msg.arg2, msg.obj);

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
                sendLHBleData("Read Detail", Constant.Param_Read_Detail);
                showMsg(2,"连接成功，正在初始化...");
            }
        });

    }


    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherZKYActivity.class);
        intent.putExtra(LiherZKYActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherZKYActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherZKYActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();
        sclUIMode = findViewById(R.id.scl_ui_mode);
        llUIMode = findViewById(R.id.ll_ui_mode);
        tvRefresh = findViewById(R.id.tv_refresh);
        tvDevAddress = findViewById(R.id.tv_dev_address);
        tvStdVol = findViewById(R.id.tv_std_vol);
        tvDevVersion = findViewById(R.id.tv_dev_version);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvSampleVol = findViewById(R.id.tv_sample_vol);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);
        tvWashNo = findViewById(R.id.tv_wash_no);
        tvCCSTime=findViewById(R.id.tv_ccs_time);
        tvRXTime=findViewById(R.id.tv_rx_time);
        tvChTime=findViewById(R.id.tv_ch_time);
        tvSurplusVol = findViewById(R.id.tv_surplus_vol);
        tvPumpRatio = findViewById(R.id.tv_pump_ratio);
        tvStandardSet = findViewById(R.id.tv_standard_set);
        tvStandardAdd = findViewById(R.id.tv_standard_add);
        tvPumpExtract = findViewById(R.id.tv_pump_extract);
        tvWorkStatus = findViewById(R.id.tv_work_status);
        tvPumpPut = findViewById(R.id.tv_pump_put);
        tvResetLiquid = findViewById(R.id.tv_reset_liquid);
        tvSurplusDensity = findViewById(R.id.tv_surplus_density);
        tvDevReset = findViewById(R.id.tv_dev_reset);
        tvDevOpen = findViewById(R.id.tv_dev_open);
        ivTempCom = findViewById(R.id.iv_temp_com);
        iv0 = findViewById(R.id.iv_0);
        iv1 = findViewById(R.id.iv_1);
        iv2 = findViewById(R.id.iv_2);
        iv3 = findViewById(R.id.iv_3);
        iv4 = findViewById(R.id.iv_4);
        iv5 = findViewById(R.id.iv_5);
        ivStandardBottle = findViewById(R.id.iv_standard_bottle);
        ivSampleBottle = findViewById(R.id.iv_sample_bottle);

        llOrderMode = findViewById(R.id.ll_order_mode);
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        sendData = findViewById(R.id.sendData);
        send = findViewById(R.id.send);
        tvYWRefresh = findViewById(R.id.tv_yw_refresh);
        tvYW1Info = findViewById(R.id.tv_yw1_info);
        tvYW2ADInfo = findViewById(R.id.tv_yw2ad_info);
        tvYW2Info = findViewById(R.id.tv_yw2_info);
        tvYW4Info = findViewById(R.id.tv_yw4_info);
        iv7 = findViewById(R.id.iv_7);
        iv8 = findViewById(R.id.iv_8);
        iv9 = findViewById(R.id.iv_9);
        iv10 = findViewById(R.id.iv_10);
        ivBtn0 = findViewById(R.id.iv_btn_0);
        ivBtn1 = findViewById(R.id.iv_btn_1);
        ivBtn2 = findViewById(R.id.iv_btn_2);
        ivBtn3 = findViewById(R.id.iv_btn_3);
        ivBtn4 = findViewById(R.id.iv_btn_4);
        ivBtn5 = findViewById(R.id.iv_btn_5);
        tvZb0 = findViewById(R.id.tv_zb_0);
        tvZb1 = findViewById(R.id.tv_zb_1);
        tvZb2 = findViewById(R.id.tv_zb_2);
        tvZb3 = findViewById(R.id.tv_zb_3);
        tvZb4 = findViewById(R.id.tv_zb_4);
        tvYw0 = findViewById(R.id.tv_yw_0);
        tvYw1 = findViewById(R.id.tv_yw_1);
        tvYw2 = findViewById(R.id.tv_yw_2);
        tvYw3 = findViewById(R.id.tv_yw_3);
        tvParamsSet = findViewById(R.id.tv_params_set);
        tvLiquidRecord = findViewById(R.id.tv_liquid_record);
        tvDevAddress.setOnClickListener(LiherZKYActivity.this);
        tvRefresh.setOnClickListener(LiherZKYActivity.this);
        tvSetMacAddress.setOnClickListener(LiherZKYActivity.this);
        tvStdVol.setOnClickListener(LiherZKYActivity.this);
        tvSampleVol.setOnClickListener(LiherZKYActivity.this);
        tvDevReset.setOnClickListener(LiherZKYActivity.this);
        tvWashNo.setOnClickListener(LiherZKYActivity.this);
        tvDevOpen.setOnClickListener(LiherZKYActivity.this);
        tvRXTime.setOnClickListener(LiherZKYActivity.this);
        tvChTime.setOnClickListener(LiherZKYActivity.this);
        send.setOnClickListener(LiherZKYActivity.this);
        tvYWRefresh.setOnClickListener(LiherZKYActivity.this);
        tvStandardSet.setOnClickListener(LiherZKYActivity.this);
        tvStandardAdd.setOnClickListener(LiherZKYActivity.this);
        tvPumpExtract.setOnClickListener(LiherZKYActivity.this);
        tvPumpPut.setOnClickListener(LiherZKYActivity.this);
        tvResetLiquid.setOnClickListener(LiherZKYActivity.this);
        tvSurplusDensity.setOnClickListener(LiherZKYActivity.this);
        ivTempCom.setOnClickListener(LiherZKYActivity.this);
        ivBtn0.setOnClickListener(LiherZKYActivity.this);
        ivBtn1.setOnClickListener(LiherZKYActivity.this);
        ivBtn2.setOnClickListener(LiherZKYActivity.this);
        ivBtn3.setOnClickListener(LiherZKYActivity.this);
        ivBtn4.setOnClickListener(LiherZKYActivity.this);
        ivBtn5.setOnClickListener(LiherZKYActivity.this);
        tvZb0.setOnClickListener(LiherZKYActivity.this);
        tvZb1.setOnClickListener(LiherZKYActivity.this);
        tvZb2.setOnClickListener(LiherZKYActivity.this);
        tvZb3.setOnClickListener(LiherZKYActivity.this);
        tvZb4.setOnClickListener(LiherZKYActivity.this);
        tvYw0.setOnClickListener(LiherZKYActivity.this);
        tvYw1.setOnClickListener(LiherZKYActivity.this);
        tvYw2.setOnClickListener(LiherZKYActivity.this);
        tvYw3.setOnClickListener(LiherZKYActivity.this);
        tvParamsSet.setOnClickListener(LiherZKYActivity.this);
        tvLiquidRecord.setOnClickListener(LiherZKYActivity.this);
        LoadingDialog.Builder loadBuilder = new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage("正在加载...")
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

        } else {
            // 发送服务
            mSendCharact = Constant.Ble_Data_Send_Service;
            // 接收服务
            mReceiveCharact = Constant.Ble_Data_Receive_Service;

            setTitle(mDeviceName + "-数据");
        }


        if (!StringUtils.isEmpty(mDeviceAddress)) tvMacAddress.setText(mDeviceAddress);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_set_mac_address:
                startActivityForResult(new Intent(LiherZKYActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_Box_MAC_Address);
                break;
            case R.id.tv_refresh:
            case R.id.tv_std_vol:
            case R.id.tv_sample_vol:
                refreshDetails();
                break;
            case R.id.tv_dev_address:
                new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_DevAddress, "设备地址", "地址值(0~255):", "", LiherZKYActivity.this);
                break;
            case R.id.tv_standard_set:
                new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Standard_Set, "配标操作", "配标量(uL):", "", "设置配标量，0~2000之间", LiherZKYActivity.this);
                break;
            case R.id.tv_standard_add:
                new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Standard_Add, "加标操作", "加标量(uL):", "", "设置加标量，1~1000之间", LiherZKYActivity.this);
                break;
            case R.id.tv_dev_reset:
                new HitiStringDailog(LiherZKYActivity.this, Constant.HitiStrTargetDailog_ZKY_Reset, "", "复位确认", "发送设备复位指令将退出当前页并设备重启,是否确认？", LiherZKYActivity.this);
                break;
            case R.id.tv_dev_open:
                sendLHBleData("Set lock", Constant.Param_OPEN_LOCK);
                break;
            case R.id.tv_pump_extract:
                new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Pump_Extract, "柱塞泵抽取", "抽取量(uL):", "", "柱塞泵抽取量，0~13000之间", LiherZKYActivity.this);
                break;
            case R.id.tv_pump_put:
                new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Pump_Put, "柱塞泵前推", "前推量(uL):", "", "柱塞泵前推量，0~13000之间", LiherZKYActivity.this);
                break;
            case R.id.send:
                if (StringUtils.isEmpty(sendData.getText().toString())) {
                    showMsg(2,"请先输入指令内容");
                    return;
                }
                sendLHBleData(sendData.getText().toString().trim(), Constant.PARAM_LIHER_ORDER);
                sendData.setText("");
                break;
            case R.id.tv_reset_liquid:
                if (devStatus == 0x32 || devStatus == 0x33 || devStatus == 0x34 || devStatus == 0x42 || devStatus == 0x43 || devStatus == 0x51) {
                    new HitiStringDailog(LiherZKYActivity.this, Constant.HitiStrTargetDialog_ZKY_ErrCode, null, "故障报警", "设备故障：" + StringUtils.getZKYDevStatus(devStatus) + "！\n请修好后点击'设备复位'，才可以使用", LiherZKYActivity.this);
                } else {
                    new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid0, "", "更换母液", "确认开始更换母液操作？", LiherZKYActivity.this);

                }

                break;
            case R.id.iv_btn_0:
                if (ivBtn0.isSelected()) {
                    sendLHBleData("Set off=1", Constant.Param_Write_ZKY_Btn_Close);
                } else {
                    sendLHBleData("Set on=1", Constant.Param_Write_ZKY_Btn_Open);
                }
                break;
            case R.id.iv_btn_1:
                if (ivBtn1.isSelected()) {
                    sendLHBleData("Set off=2", Constant.Param_Write_ZKY_Btn_Close);
                } else {
                    sendLHBleData("Set on=2", Constant.Param_Write_ZKY_Btn_Open);
                }
                break;
            case R.id.iv_btn_2:
                if (ivBtn2.isSelected()) {
                    sendLHBleData("Set off=3", Constant.Param_Write_ZKY_Btn_Close);
                } else {
                    sendLHBleData("Set on=3", Constant.Param_Write_ZKY_Btn_Open);
                }
                break;
            case R.id.iv_btn_3:
                if (ivBtn3.isSelected()) {
                    sendLHBleData("Set off=4", Constant.Param_Write_ZKY_Btn_Close);
                } else {
                    sendLHBleData("Set on=4", Constant.Param_Write_ZKY_Btn_Open);
                }
                break;
            case R.id.iv_btn_4:
                if (ivBtn4.isSelected()) {
                    sendLHBleData("Set off=5", Constant.Param_Write_ZKY_Btn_Close);
                } else {
                    sendLHBleData("Set on=5", Constant.Param_Write_ZKY_Btn_Open);
                }
                break;
            case R.id.iv_btn_5:
                if (ivBtn5.isSelected()) {
                    sendLHBleData("Set off=6", Constant.Param_Write_ZKY_Btn_Close);
                } else {
                    sendLHBleData("Set on=6", Constant.Param_Write_ZKY_Btn_Open);
                }
                break;
            case R.id.tv_zb_0:
                sendLHBleData("Set zb=0", Constant.Param_Write_ZKY_ZB_0);
                break;
            case R.id.tv_zb_1:
                sendLHBleData("Set zb=1", Constant.Param_Write_ZKY_ZB_1);
                break;
            case R.id.tv_zb_2:
                sendLHBleData("Set zb=2", Constant.Param_Write_ZKY_ZB_2);
                break;
            case R.id.tv_zb_3:
                sendLHBleData("Set zb=3", Constant.Param_Write_ZKY_ZB_3);
                break;
            case R.id.tv_zb_4:
                sendLHBleData("Set zb=4", Constant.Param_Write_ZKY_ZB_4);
                break;
            case R.id.tv_yw_0:
                sendLHBleData("Set yw=1", Constant.Param_Write_ZKY_YW_0);
                break;
            case R.id.tv_yw_1:
                sendLHBleData("Set yw=2", Constant.Param_Write_ZKY_YW_1);
                break;
            case R.id.tv_yw_2:
                sendLHBleData("Set yw=3", Constant.Param_Write_ZKY_YW_2);
                break;
            case R.id.tv_yw_3:
                sendLHBleData("Set yw=4", Constant.Param_Write_ZKY_YW_3);
                break;
            case R.id.tv_yw_refresh:
                sendLHBleData("Read ywadc", Constant.Param_ZKY_YWADC);
                break;
            case R.id.tv_params_set:
                List<JSONObject> jat = new ArrayList<>();
                JSONObject jo0 = new JSONObject();
                jo0.put("MyName", "转臂时间1");
                jo0.put("MyObject", Constant.Dailog_Arm_Time1_Set);
                jat.add(jo0);
                JSONObject jo1 = new JSONObject();
                jo1.put("MyName", "转臂时间2");
                jo1.put("MyObject", Constant.Dailog_Arm_Time2_Set);
                jat.add(jo1);
                JSONObject jo2 = new JSONObject();
                jo2.put("MyName", "转臂速度");
                jo2.put("MyObject", Constant.Dailog_Arm_Speed_Set);
                jat.add(jo2);
                JSONObject jo3 = new JSONObject();
                jo3.put("MyName", "滴定速度");
                jo3.put("MyObject", Constant.Dialog_Titration_Speed_Set);
                jat.add(jo3);
                JSONObject jo4 = new JSONObject();
                jo4.put("MyName", "柱塞泵比率");
                jo4.put("MyObject", Constant.Dialog_Titration_Standard_Set);
                jat.add(jo4);
                JSONObject jo5 = new JSONObject();
                jo5.put("MyName", "废液体积");
                jo5.put("MyObject", Constant.Dialog_Liquid_Vol_Set);
                jat.add(jo5);
                JSONObject jo6 = new JSONObject();
                jo6.put("MyName", "配标水泵速度");
                jo6.put("MyObject", Constant.Dialog_Pump_Speed_Set);
                jat.add(jo6);
                JSONObject jo7 = new JSONObject();
                jo7.put("MyName", "标样定容");
                jo7.put("MyObject", Constant.Dialog_Standard_Vol_Set);
                jat.add(jo7);
                JSONObject jo8 = new JSONObject();
                jo8.put("MyName", "水样定容");
                jo8.put("MyObject", Constant.Dialog_Sample_Vol_Set);
                jat.add(jo8);
                JSONObject jo9 = new JSONObject();
                jo9.put("MyName", "排样时间");
                jo9.put("MyObject", Constant.Dialog_CYX_PYTime_Set);
                jat.add(jo9);
                JSONObject jo10 = new JSONObject();
                jo10.put("MyName", "液位电源间隔");
                jo10.put("MyObject", Constant.Dialog_CYX_YWTime_Set);
                jat.add(jo10);
                JSONObject jo11 = new JSONObject();
                jo11.put("MyName", "液位1判断阈值");
                jo11.put("MyObject", Constant.Dialog_CYX_YW1_Set);
                jat.add(jo11);
                JSONObject jo12 = new JSONObject();
                jo12.put("MyName", "液位2判断阈值");
                jo12.put("MyObject", Constant.Dialog_CYX_YW2AD_Set);
                jat.add(jo12);
                JSONObject jo13 = new JSONObject();
                jo13.put("MyName", "液位4判断阈值");
                jo13.put("MyObject", Constant.Dialog_CYX_YW4_Set);
                jat.add(jo13);
                JSONObject jo14 = new JSONObject();
                jo14.put("MyName", "液位2DDL判断阈值");
                jo14.put("MyObject", Constant.Dialog_CYX_YW2_Set);
                jat.add(jo14);
                JSONObject jo15 = new JSONObject();
                jo15.put("MyName", "母液余量");
                jo15.put("MyObject", Constant.Dialog_ZKY_Surplus_Vol);
                jat.add(jo15);
                JSONObject jo16 = new JSONObject();
                jo16.put("MyName", "母液量程步数");
                jo16.put("MyObject", Constant.Dialog_ZKY_Surplus_Step);
                jat.add(jo16);
                new SelecterObjectDailog(LiherZKYActivity.this, "选择参数", jat, Constant.Dailog_Select_ZKY_Param, LiherZKYActivity.this);

                break;
            case R.id.tv_liquid_record:
                new LiquidRecordDailog(LiherZKYActivity.this, "母液信息记录", Constant.Dialog_Liquid_Record, liquidRefreshTime, liquidOutTime, LiherZKYActivity.this);
                break;
            case R.id.tv_surplus_density:

                new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_ZKY_Surplus_Density, "设置母液浓度", "母液浓度(mg/L):", surplusDensity100 > 0 ? (DoubleUtils.div(surplusDensity100, 100, 2) + "") : "", "母液浓度值，<=1000000", LiherZKYActivity.this);

                break;
            case R.id.iv_temp_com:
                if (tempcom < 0) {
                    showMsg(2,"当前设备程序版本不支持设置柱塞泵温补,请尽快升级设备程序");
                    refreshDetails();
                } else if (tempcom == 1) {
                    sendLHBleData("Write tempcom=0", Constant.Param_Write_ZKY_TEMP_COM);
                } else {
                    sendLHBleData("Write tempcom=1", Constant.Param_Write_ZKY_TEMP_COM);
                }
                break;
            case R.id.tv_wash_no:
                if (washno < 0) {
                    showMsg(2,"当前设备程序版本不支持设置配标润洗次数,请尽快升级设备程序");
                    refreshDetails();
                } else {
                    new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_ZKY_Wash_No, "设置配标润洗次数", "次数值:", washno > 0 ? (washno + "") : "", "配标润洗次数值，请输入1或者2", LiherZKYActivity.this);

                }
                break;
            case R.id.tv_rx_time:
                if (rxtime < 0) {
                    showMsg(2,"当前设备程序版本不支持设置配标润洗时间,请尽快升级设备程序");
                    refreshDetails();
                } else {
                    new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_ZKY_RX_Time, "设置配标润洗时间", "配标润洗时间（s）:", rxtime > 0 ? (DoubleUtils.div(rxtime,100,1) + "") : "", "配标润洗时间,设值范围1~30s", LiherZKYActivity.this);

                }
                break;
            case R.id.tv_ch_time:
                if (chtime < 0) {
                    showMsg(2,"当前设备程序版本不支持设置配标侧吸定容时间,请尽快升级设备程序");
                    refreshDetails();
                } else {
                    String chTips="配标侧吸定容时间,设值范围5~200s";
                    if (rxtime>0){
                        chTips="配标侧吸定容时间,设值范围"+DoubleUtils.div(rxtime+500,100,1)+"~200s";
                    }
                    new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_ZKY_CH_Time, "设置配标侧吸定容时间", "设置时间（s）:", chtime > 0 ? (DoubleUtils.div(chtime,100,1) + "") : "", chTips, LiherZKYActivity.this);

                }
                break;
        }
    }

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.Dialog_Liquid_Reset_Time:
                LiquidRecordDailog liquidRecordDailog = (LiquidRecordDailog) target;
                Calendar defaultCalendar = Calendar.getInstance();
                Calendar startCalendar = Calendar.getInstance();
                if (liquidRecordDailog.getTempResetTime() > 0) {
                    defaultCalendar = DateUtil.getStringFormat2Calendar(DateUtil.long2time(liquidRecordDailog.getTempResetTime()), "yyyy-MM-dd HH:mm:ss");
                    startCalendar = DateUtil.getStringFormat2Calendar(DateUtil.long2time(liquidRecordDailog.getTempResetTime() - 90 * 24 * 60 * 60 * 1000L), "yyyy-MM-dd HH:mm:ss");

                }
                DlgTime.showYMDDialog(LiherZKYActivity.this, defaultCalendar, startCalendar, Calendar.getInstance(),
                        true, true, true, true, true, true,
                        new DlgTime.OnDateSelectedFinishCallback() {
                            @Override
                            public void onDateSelected(Date date) {
                                liquidRecordDailog.notifyDataSetChanged(date.getTime(), 0);
                            }
                        });
                break;
            case Constant.Dialog_Liquid_Out_Time:
                LiquidRecordDailog liquidRecordDailog1 = (LiquidRecordDailog) target;
                Calendar defaultCalendar1 = Calendar.getInstance();

                Calendar endCalendar1 = Calendar.getInstance();
                if (liquidRecordDailog1.getTempOutTime() > 0) {
                    defaultCalendar1 = DateUtil.getStringFormat2Calendar(DateUtil.long2time(liquidRecordDailog1.getTempOutTime()), "yyyy-MM-dd HH:mm:ss");
                    endCalendar1 = DateUtil.getStringFormat2Calendar(DateUtil.long2time(liquidRecordDailog1.getTempOutTime() + 365 * 24 * 60 * 60 * 1000L), "yyyy-MM-dd HH:mm:ss");

                }
                Calendar startCalendar1 = null;
                if (liquidRecordDailog1.getTempResetTime() > 0) {
                    startCalendar1 = DateUtil.getStringFormat2Calendar(DateUtil.long2time(liquidRecordDailog1.getTempResetTime()), "yyyy-MM-dd HH:mm:ss");
                }
                DlgTime.showYMDDialog(this, defaultCalendar1, startCalendar1, endCalendar1,
                        true, true, true, true, true, true,
                        new DlgTime.OnDateSelectedFinishCallback() {
                            @Override
                            public void onDateSelected(Date date) {
                                liquidRecordDailog1.notifyDataSetChanged(0, date.getTime());
                            }
                        });
                break;
            case Constant.Dialog_Liquid_Record:
                String strTarget = (String) target;
                if (strTarget.contains("#")) {
                    tempResetTime = Long.parseLong(strTarget.split("#")[0]);
                    tempOutTime = Long.parseLong(strTarget.split("#")[1]);
                    if (tempResetTime > 0) {

                        sendLHBleData("Write changetime=" + DateUtil.long2time10(tempResetTime).substring(2), Constant.Param_Write_ZKY_Liquid_ResetTime);
                    } else if (tempOutTime > 0) {
                        sendLHBleData("Write expiration=" + DateUtil.long2time10(tempOutTime).substring(2), Constant.Param_Write_ZKY_Liquid_OutTime);

                    }
                }


                break;
            case Constant.Dailog_ZKY_Reset_Liquid0:
                sendLHBleData("Set cmd=4", Constant.Param_Write_ZKY_Reset_Liquid0);
                break;
            case Constant.Dailog_ZKY_Reset_Liquid1:
                sendLHBleData("Set next", Constant.Param_Write_ZKY_Reset_Liquid1);
                break;
            case Constant.Dailog_ZKY_Reset_Liquid2:
                sendLHBleData("Set next", Constant.Param_Write_ZKY_Reset_Liquid2);
                break;
            case Constant.Dailog_ZKY_Reset_Liquid3:
                sendLHBleData("Set next", Constant.Param_Write_ZKY_Reset_Liquid3);
                break;
            case Constant.Dailog_ZKY_Reset_Liquid4:
                sendLHBleData("Set next", Constant.Param_Write_ZKY_Reset_Liquid4);
                break;
            case Constant.Dailog_ZKY_Reset_Liquid5:
                sendLHBleData("Set next", Constant.Param_Write_ZKY_Reset_Liquid5);
                break;
            case Constant.HitiStrTargetDialog_ZKY_Reset_Liquid_Record:
                onViewClick(Constant.Dialog_Liquid_Record, System.currentTimeMillis() + "#" + (System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L), tvResetLiquid);
                break;
            case Constant.Dailog_ZKY_Pump_Extract:
                String strPumpExtract = (String) target;
                if (StringUtils.isDouble(strPumpExtract)) {
                    if (strPumpExtract.contains(".")) {
                        strPumpExtract = strPumpExtract.substring(0, strPumpExtract.indexOf(".")) + strPumpExtract.substring(strPumpExtract.indexOf(".") + 1, strPumpExtract.indexOf(".") + 2);
                    } else {
                        strPumpExtract = strPumpExtract + "0";
                    }
                    int pumpExtract = Integer.parseInt(strPumpExtract);
                    if (pumpExtract > 0 && pumpExtract <= 130000) {
                        sendLHBleData("Set pull=" + pumpExtract, Constant.Param_Write_ZKY_Pump_Extract);
                        return;
                    }
                }
                showMsg(2,"请输入正确的柱塞泵抽取量，0~13000之间");
                break;
            case Constant.Dailog_ZKY_Pump_Put:
                String strPumpPut = (String) target;
                if (StringUtils.isDouble(strPumpPut)) {
                    if (strPumpPut.contains(".")) {
                        strPumpPut = strPumpPut.substring(0, strPumpPut.indexOf(".")) + strPumpPut.substring(strPumpPut.indexOf(".") + 1, strPumpPut.indexOf(".") + 2);
                    } else {
                        strPumpPut = strPumpPut + "0";
                    }
                    int pumpPut = Integer.parseInt(strPumpPut);
                    if (pumpPut > 0 && pumpPut <= 130000) {
                        sendLHBleData("Set push=" + pumpPut, Constant.Param_Write_ZKY_Pump_Put);
                        return;
                    }
                }
                showMsg(2,"请输入正确的柱塞泵前推量，0~13000之间");
                break;
            case Constant.Dailog_ZKY_Standard_Set:
                String strStandardSet = (String) target;
                if (StringUtils.isDouble(strStandardSet)) {
                    if (strStandardSet.contains(".")) {
                        strStandardSet = strStandardSet.substring(0, strStandardSet.indexOf(".")) + strStandardSet.substring(strStandardSet.indexOf(".") + 1, strStandardSet.indexOf(".") + 2);
                    } else {
                        strStandardSet = strStandardSet + "0";
                    }
                    int standardSet = Integer.parseInt(strStandardSet);
                    if (standardSet >= 0 && standardSet <= 20000) {
                        sendLHBleData("Set pb=" + standardSet, Constant.Param_Write_ZKY_Standard_Set);
                        return;
                    }
                }
                showMsg(2,"请输入正确的配标量，0~2000之间");
                break;
            case Constant.Dailog_ZKY_Standard_Add:
                String strStandardAdd = (String) target;
                if (StringUtils.isDouble(strStandardAdd)) {
                    if (strStandardAdd.contains(".")) {
                        strStandardAdd = strStandardAdd.substring(0, strStandardAdd.indexOf(".")) + strStandardAdd.substring(strStandardAdd.indexOf(".") + 1, strStandardAdd.indexOf(".") + 2);
                    } else {
                        strStandardAdd = strStandardAdd + "0";
                    }
                    int standardAdd = Integer.parseInt(strStandardAdd);
                    if (standardAdd >= 10 && standardAdd <= 10000) {
                        sendLHBleData("Set jb=" + standardAdd, Constant.Param_Write_ZKY_Standard_Add);
                        return;
                    }
                }
                showMsg(2,"请输入正确的加标量，1~1000之间");
                break;
            case Constant.Dailog_ZKY_DevAddress:
                String strDevAdd = (String) target;
                if (StringUtils.isInt(strDevAdd)) {
                    int devAdd = Integer.parseInt(strDevAdd);
                    if (devAdd > 255 || devAdd < 0) {
                        showMsg(2,"请输入正确的设备地址值，0~255之间");
                    } else {
                        sendLHBleData("Write Devaddr=" + devAdd, Constant.Param_Wirte_ZKY_DevAddress);

                    }
                }

                break;
            case Constant.Dialog_ZKY_Surplus_Vol:
                String strSurplusVol = (String) target;
                if (StringUtils.isInt(strSurplusVol)) {
                    int sVol = Integer.parseInt(strSurplusVol);
                    if (sVol > 15000 || sVol < 0) {
                        showMsg(2,"请输入正确的母液余量，0~15000的整数");
                    } else {
                        sendLHBleData("Write vsu=" + sVol, Constant.Param_Wirte_ZKY_SurplusVol);

                    }
                } else {
                    showMsg(2,"请输入正确的母液余量，0~15000的整数");
                }

                break;
            case Constant.Dialog_ZKY_Surplus_Step:
                String strSurplusStep = (String) target;
                if (StringUtils.isInt(strSurplusStep)) {
                    int sStep = Integer.parseInt(strSurplusStep);
                    if (sStep > 28000 || sStep < 0) {
                        showMsg(2,"请输入正确的母液量程步数，0~28000的整数");
                    } else {
                        sendLHBleData("Write StepMax=" + sStep, Constant.Param_Wirte_ZKY_SurplusStep);

                    }
                } else {
                    showMsg(2,"请输入正确的母液量程步数，0~28000的整数");
                }

                break;


            case Constant.Dailog_BLE_MACAddress:
                String strT = (String) target;

                sendLHBleData("Write BLEaddr=" + strT, Constant.Param_Write_ZKY_Mac_Address);
                break;
            case Constant.HitiStrTargetDailog_ZKY_Reset:
                sendLHBleData("Set rst", Constant.Param_DEV_Reset);
                RxTimerUtil.cancel();
                RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                    @Override
                    public void doNext(long number) {
                        showMsg(2,"设备复位，退出当前界面");
                        finish();
                    }
                });
                break;
            case Constant.Dailog_Arm_Time1_Set:
                String strArmTime1 = (String) target;
                if (StringUtils.isInt(strArmTime1)) {
                    int armTime1 = Integer.parseInt(strArmTime1);
                    if (armTime1 >= 1500 && armTime1 <= 4000) {
                        sendLHBleData("Write ZT1=" + armTime1, Constant.Param_Write_ZKY_Arm_Time1);
                        return;
                    }
                }
                showMsg(2,"请输入正确的转臂时间1，1500~4000之间");
                break;
            case Constant.Dailog_Arm_Time2_Set:
                String strArmTime2 = (String) target;
                if (StringUtils.isInt(strArmTime2)) {
                    int armTime2 = Integer.parseInt(strArmTime2);
                    if (armTime2 > 2500 && armTime2 <= 6000) {
                        sendLHBleData("Write ZT2=" + armTime2, Constant.Param_Write_ZKY_Arm_Time2);
                        return;
                    }
                }
                showMsg(2,"请输入正确的转臂时间2，2500~6000之间");
                break;
            case Constant.Dailog_Arm_Speed_Set:
                String strArmSpeed = (String) target;
                if (StringUtils.isInt(strArmSpeed)) {
                    int armSpeed = Integer.parseInt(strArmSpeed);
                    if (armSpeed >= 20 && armSpeed <= 100) {
                        sendLHBleData("Write ZDuty=" + armSpeed, Constant.Param_Write_ZKY_Arm_Speed);
                        return;
                    }
                }
                showMsg(2,"请输入正确的转臂速度，20~100之间");
                break;
            case Constant.Dialog_Titration_Speed_Set:
                String strTitrationSpeed = (String) target;
                if (StringUtils.isInt(strTitrationSpeed)) {
                    int titrationSpeed = Integer.parseInt(strTitrationSpeed);
                    if (titrationSpeed >= 50 && titrationSpeed <= 3000) {
                        sendLHBleData("Write Vmax=" + titrationSpeed, Constant.Param_Write_ZKY_Titration_Speed);
                        return;
                    }
                }
                showMsg(2,"请输入正确的滴定速度，50~3000之间");
                break;
            case Constant.Dialog_Titration_Standard_Set:
                String strTitrationStandard = (String) target;
                if (StringUtils.isInt(strTitrationStandard)) {
                    int titrationStandard = Integer.parseInt(strTitrationStandard);
                    if (titrationStandard >= 1000 && titrationStandard <= 3000) {
                        sendLHBleData("Write Calib=" + titrationStandard, Constant.Param_Write_ZKY_Titration_Standard);
                        return;
                    }
                }
                showMsg(2,"请输入正确的柱塞泵比率，1000~3000之间");
                break;
            case Constant.Dialog_Liquid_Vol_Set:
                String strLiquidVol = (String) target;
                if (StringUtils.isInt(strLiquidVol)) {
                    int liquidVol = Integer.parseInt(strLiquidVol);
                    if (liquidVol >= 10 && liquidVol <= 1000) {
                        sendLHBleData("Write Cdrop=" + liquidVol * 10, Constant.Param_Write_ZKY_Liquid_Vol);
                        return;
                    }
                }
                showMsg(2,"请输入正确的废液体积，10~1000之间");
                break;
            case Constant.Dialog_Pump_Speed_Set:
                String strPumpSpeed = (String) target;
                if (StringUtils.isInt(strPumpSpeed)) {
                    int pumpSpeed = Integer.parseInt(strPumpSpeed);
                    if (pumpSpeed >= 5 && pumpSpeed <= 100) {
                        sendLHBleData("Write PDuty=" + pumpSpeed, Constant.Param_Write_ZKY_Pump_Speed);
                        return;
                    }
                }
                showMsg(2,"请输入正确的配标水泵速度，5~100之间");
                break;
            case Constant.Dialog_Standard_Vol_Set:
                String strStandardVol = (String) target;
                if (StringUtils.isDouble(strStandardVol)) {
                    int standardVol = (int) DoubleUtils.mul(DoubleUtils.round(Double.parseDouble(strStandardVol), 1), 10d);
                    if (standardVol >= 500 && standardVol <= 700) {
                        sendLHBleData("Write vby=" + standardVol, Constant.Param_Write_ZKY_Standard_Vol);
                        return;
                    }
                }
                showMsg(2,"请输入正确的标样定容值，50~70之间");
                break;
            case Constant.Dialog_Sample_Vol_Set:
                String strSampleVol = (String) target;
                if (StringUtils.isDouble(strSampleVol)) {
                    int sampleVol = (int) DoubleUtils.mul(DoubleUtils.round(Double.parseDouble(strSampleVol), 1), 10d);
                    if (sampleVol >= 500 && sampleVol <=1200) {
                        sendLHBleData("Write vsy=" + sampleVol, Constant.Param_Write_ZKY_Sample_Vol);
                        return;
                    }
                }
                showMsg(2,"请输入正确的水样定容值，50~120之间");
                break;
            case Constant.Dialog_ZKY_Surplus_Density:
                String strSampleDensity = (String) target;
                if (StringUtils.isDouble(strSampleDensity)) {
                    int sampleDensity = (int) DoubleUtils.mul(DoubleUtils.round(Double.parseDouble(strSampleDensity), 2), 100d);
                    if (sampleDensity > 0 && sampleDensity <= 100000000) {
                        sendLHBleData("Write nongdu=" + sampleDensity, Constant.Param_Write_ZKY_SurplusDensity);
                        return;
                    }
                }
                showMsg(2,"请输入正确的母液浓度值，0~1000000");
                break;
            case Constant.Dialog_CYX_PYTime_Set:
                String strPYTime = (String) target;
                if (StringUtils.isInt(strPYTime)) {
                    int pyTime = Integer.parseInt(strPYTime);
                    if (pyTime >= 10000 && pyTime <= 65000) {
                        sendLHBleData("Write pytime=" + pyTime, Constant.Param_Write_ZKY_PYTime);
                        return;
                    }
                }
                showMsg(2,"请输入正确的排样时间，10000~65000之间");
                break;
            case Constant.Dialog_CYX_YWTime_Set:
                String strYWTime = (String) target;
                if (StringUtils.isDouble(strYWTime)) {
                    if (strYWTime.contains(".")) {
                        strYWTime = strYWTime.substring(0, strYWTime.indexOf(".")) + strYWTime.substring(strYWTime.indexOf(".") + 1, strYWTime.indexOf(".") + 2);
                    } else {
                        strYWTime = strYWTime + "0";
                    }
                    int ywTime = Integer.parseInt(strYWTime);
                    if (ywTime >= 4 && ywTime <= 120) {
                        sendLHBleData("Write ywtime=" + ywTime, Constant.Param_Write_ZKY_YWTime);
                        return;
                    }
                }
                showMsg(2,"请输入正确的液位电源间隔，0.4~12之间");
                break;
            case Constant.Dialog_CYX_YW1_Set:
                String strYW1 = (String) target;
                if (StringUtils.isInt(strYW1)) {
                    int yw1 = Integer.parseInt(strYW1);
                    if (yw1 >= 100 && yw1 <= 8150) {
                        sendLHBleData("Write yw1th=" + yw1, Constant.Param_Write_ZKY_YW1);
                        return;
                    }
                }
                showMsg(2,"请输入正确的液位1判断阈值，100~8150之间");
                break;
            case Constant.Dialog_CYX_YW2AD_Set:
                String strYWAD2 = (String) target;
                if (StringUtils.isInt(strYWAD2)) {
                    int yw2ad = Integer.parseInt(strYWAD2);
                    if (yw2ad >= 100 && yw2ad <= 8150) {
                        sendLHBleData("Write yw2adth=" + yw2ad, Constant.Param_Write_ZKY_YW2AD);
                        return;
                    }
                }
                showMsg(2,"请输入正确的液位2判断阈值，100~8150之间");
                break;
            case Constant.Dialog_CYX_YW4_Set:
                String strYW4 = (String) target;
                if (StringUtils.isInt(strYW4)) {
                    int yw4 = Integer.parseInt(strYW4);
                    if (yw4 >= 100 && yw4 <= 8150) {
                        sendLHBleData("Write yw4th=" + yw4, Constant.Param_Write_ZKY_YW4);
                        return;
                    }
                }
                showMsg(2,"请输入正确的液位4判断阈值，100~8150之间");
                break;
            case Constant.Dialog_CYX_YW2_Set:
                String strYW2 = (String) target;
                if (StringUtils.isInt(strYW2)) {
                    int yw2 = Integer.parseInt(strYW2);
                    if (yw2 >= 10 && yw2 <= 500) {
                        sendLHBleData("Write yw2th=" + yw2, Constant.Param_Write_ZKY_YW2);
                        return;
                    }
                }
                showMsg(2,"请输入正确的液位2的DDL判断阈值，10~500之间");
                break;
            case Constant.Dialog_ZKY_Wash_No:
                String strWashNo = (String) target;
                if (StringUtils.isInt(strWashNo)) {
                    int washNo = Integer.parseInt(strWashNo);
                    if (washNo == 1 ||washNo==2) {
                        sendLHBleData("Write washno=" + washNo, Constant.Param_Write_ZKY_Wash_No);
                        return;
                    }
                }
                showMsg(2,"请输入正确的配标润洗次数值，1或者2");
                break;
            case Constant.Dialog_ZKY_RX_Time:
                String strRxTime = (String) target;
                if (StringUtils.isDouble(strRxTime)) {
                    int rxTime = (int) (DoubleUtils.mul(Double.parseDouble(strRxTime),100));
                    if (rxTime >=100 &&rxTime<=3000) {
                        sendLHBleData("Write rxtime=" + rxTime, Constant.Param_Write_ZKY_RX_Time);
                        return;
                    }
                }
                showMsg(2,"请输入正确的配标润洗时间,设值范围1~30s");
                break;
            case Constant.Dialog_ZKY_CH_Time:
                String strChTime = (String) target;
                if (StringUtils.isDouble(strChTime)) {
                    int chTime = (int) (DoubleUtils.mul(Double.parseDouble(strChTime),100));
                    if (chTime >=500 &&chTime<=20000) {
                        if (chTime-rxtime<500){
                            showMsg(2,"侧吸定容时间，需要比‘配标润洗时间’长5秒以上");
                            return;
                        }else {
                            sendLHBleData("Write chtime=" + chTime, Constant.Param_Write_ZKY_Ch_Time);
                        }

                        return;
                    }
                }
                showMsg(2,"请输入正确的配标侧吸定容时间,设值范围5~200s");
                break;
            case Constant.HitiStrTargetDialog_ZKY_ErrCode:

                break;

        }
    }

    @Override
    public void onItemClick3(View v, int position, Object object, int type) {
        switch (type) {
            case Constant.Dailog_Select_ZKY_Param:
                JSONObject jo = (JSONObject) object;
                switch (jo.getIntValue("MyObject")) {
                    case Constant.Dailog_Arm_Time1_Set:
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dailog_Arm_Time1_Set, "参数设定", "转臂时间1(ms):", zb1Time > 0 ? (zb1Time + "") : "", "设置转臂时间1，1500~4000之间", LiherZKYActivity.this);
                        break;
                    case Constant.Dailog_Arm_Time2_Set:
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dailog_Arm_Time2_Set, "参数设定", "转臂时间2(ms):", zb2Time > 0 ? (zb2Time + "") : "", "设置转臂时间2，2500~6000之间", LiherZKYActivity.this);
                        break;
                    case Constant.Dailog_Arm_Speed_Set:
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dailog_Arm_Speed_Set, "参数设定", "转臂速度:", zbDuty > 0 ? (zbDuty + "") : "", "设置转臂速度，20~100之间", LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_Titration_Speed_Set:
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_Titration_Speed_Set, "参数设定", "滴定速度:", vMax > 0 ? (vMax + "") : "", "设置滴定速度，50~3000之间", LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_Titration_Standard_Set:
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_Titration_Standard_Set, "参数设定", "柱塞泵比率(步/mL):", pumpRatio > 0 ? (pumpRatio + "") : "", "设置柱塞泵比率，1000~3000之间", LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_Liquid_Vol_Set:
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_Liquid_Vol_Set, "参数设定", "废液体积(ul):", rubbishVol > 0 ? (rubbishVol + "") : "", "设置废液体积，10~1000之间", LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_Pump_Speed_Set:
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_Pump_Speed_Set, "参数设定", "配标水泵速度:", pumpDuty > 0 ? (pumpDuty + "") : "", "设置水泵速度，5~100之间", LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_Standard_Vol_Set:
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_Standard_Vol_Set, "参数设定", "标样定容值(mL):", standerVol > 0 ? (standerVol + "") : "", "设置标样定容值，50~70之间", LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_Sample_Vol_Set:
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_Sample_Vol_Set, "参数设定", "水样定容值(mL):", sampleVol > 0 ? (sampleVol + "") : "", "设置水样定容值，50~120之间", LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_CYX_PYTime_Set:
                        String pyTimeTips = "可设范围10000-65000";
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_CYX_PYTime_Set, "参数设定", "排样时间(mS):", pyTime > 0 ? (pyTime + "") : "", pyTimeTips, LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_CYX_YWTime_Set:
                        String ywTimeTips = "可设范围0.4~12";
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_CYX_YWTime_Set, "参数设定", "液位电源间隔(S):", ywTime >= 0 ? (DoubleUtils.div(ywTime, 10d, 1) + "") : "", ywTimeTips, LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_CYX_YW1_Set:
                        String yw1ThTips = "实时值低于阈值则已检测到液位,可设范围100~8150";
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_CYX_YW1_Set, "参数设定", "液位1判断阈值:", yw1th > 0 ? (yw1th + "") : "", yw1ThTips, LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_CYX_YW2AD_Set:
                        String yw2ADThTips = "实时值高于阈值则已检测到液位,可设范围100~8150";
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_CYX_YW2AD_Set, "参数设定", "液位2判断阈值:", yw2adth > 0 ? (yw2adth + "") : "", yw2ADThTips, LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_CYX_YW4_Set:
                        String yw4ThTips = "实时值低于阈值则已检测到液位,可设范围100~8150";
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_CYX_YW4_Set, "参数设定", "液位4判断阈值:", yw4th > 0 ? (yw4th + "") : "", yw4ThTips, LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_CYX_YW2_Set:
                        String yw2ThTips = "实时值高于阈值则已检测到液位,可设范围10~500";
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_CYX_YW2_Set, "参数设定", "液位2的DDL判断阈值:", yw2th > 0 ? (yw2th + "") : "", yw2ThTips, LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_ZKY_Surplus_Vol:
                        String surplusVolTips = "设置母液余量，<=15000";
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_ZKY_Surplus_Vol, "参数设定", "剩余母液(uL):", surplusVol + "", surplusVolTips, LiherZKYActivity.this);
                        break;
                    case Constant.Dialog_ZKY_Surplus_Step:

                        String surplusStepTips = "设置母液量程步数，<=28000";
                        new HitiInputStrDailog(LiherZKYActivity.this, Constant.Dialog_ZKY_Surplus_Step, "参数设定", "母液量程步数:", liquidStep + "", surplusStepTips, LiherZKYActivity.this);

                        break;
                }
                break;
        }
    }


    private void showSelectUIModeMenu() {
        PopupMenu popupMenu = new PopupMenu(this, rightTitleTxt);
        popupMenu.inflate(R.menu.menu_select_ui_mode);
        popupMenu.getMenu().findItem(R.id.version_upgrade).setVisible(false);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.ui_mode:
                        sclUIMode.setVisibility(View.VISIBLE);
                        llOrderMode.setVisibility(View.GONE);
                        rightTitleTxt.setText("交互模式");
                        refreshDetails();
                        return true;
                    case R.id.order_mode:
                        sclUIMode.setVisibility(View.GONE);
                        llOrderMode.setVisibility(View.VISIBLE);
                        rightTitleTxt.setText("窗口模式");
                        sendLHBleData("Read ywadc", Constant.Param_ZKY_YWADC);
                        return true;
                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void refreshDetails() {
        RxTimerUtil.cancel();
        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read Detail", Constant.Param_Read_Detail);
            }
        });

    }


    //发送数据
    public void sendLHBleData(String sendString, int sendType) {
        byte[] tmpBytes;
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2,"发送数据内容不能为空");
            return;
        }

        tmpBytes = ProtocolUtil.getBlueToothMsgForSend(HJBleApplication.shareInstance().getDesID(), sendString);

        if (tmpBytes == null) {
            return;
        } else if (mble == null || !mble.isConnect()) {
            showMsg(1,"设备蓝牙连接已中断，请扫描连接后重试");
            finish();
            return;
        }
        if (LiherZKYActivity.this.isFinishing()) {
            return;
        }
        loadingDialog.show();
        addLogText(sendString, 0, sendType);
        insertLog(mDeviceAddress, sampleId, sendString);
        final byte[] bytes = tmpBytes;
        currentSendType = sendType;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mSendCharact, bytes)) {
                    addLogText((mble!=null?mble.getSendDataLenMax()+"MTU,":"")+"发送失败！",0, Constant.Return_BLE_False);
                    currentSendType = 0;
                }else if (!mble.waitReturn(mReceiveCharact,15*1000)){
                    addLogText(getResources().getString(R.string.ble_rtn_timeout),0, Constant.Return_BLE_False);
                    currentSendType = 0;
                }
            }
        }).start();

    }

    WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
        @Override
        public void OnWiseBluetoothState(int state) {

            if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED) {
                if (loadingDialog != null) loadingDialog.dismiss();
                addLogText("蓝牙已断开", Color.BLACK, 0);
               // finish();
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
                    switch (currentSendType) {
                        case Constant.PARAM_LIHER_ORDER:
                            currentReceiveType = Constant.RETURN_LIHER_OREDER;
                            break;
                        case Constant.Param_Read_Detail:
                            currentReceiveType = Constant.Return_Read_Detail;
                            break;
                        case Constant.Param_Wirte_ZKY_DevAddress:
                            currentReceiveType = Constant.Return_Wirte_ZKY_DevAddress;
                            break;
                        case Constant.Param_Write_ZKY_Mac_Address:
                            currentReceiveType = Constant.Return_Write_ZKY_Mac_Address;
                            break;
                        case Constant.Param_DEV_Reset:
                            currentReceiveType = Constant.Return_DEV_Reset;
                            break;
                        case Constant.Param_OPEN_LOCK:
                            currentReceiveType = Constant.Return_OPEN_LOCK;
                            break;
                        case Constant.Param_Write_ZKY_Standard_Set:
                            currentReceiveType = Constant.Return_Write_ZKY_Standard_Set;
                            break;
                        case Constant.Param_Write_ZKY_Standard_Add:
                            currentReceiveType = Constant.Return_Write_ZKY_Standard_Add;
                            break;
                        case Constant.Param_Write_ZKY_Pump_Extract:
                            currentReceiveType = Constant.Return_Write_ZKY_Pump_Extract;
                            break;
                        case Constant.Param_Write_ZKY_Pump_Put:
                            currentReceiveType = Constant.Return_Write_ZKY_Pump_Put;
                            break;
                        case Constant.Param_Write_ZKY_Reset_Liquid0:
                            currentReceiveType = Constant.Return_Write_ZKY_Reset_Liquid0;
                            break;
                        case Constant.Param_Write_ZKY_Reset_Liquid1:
                            currentReceiveType = Constant.Return_Write_ZKY_Reset_Liquid1;
                            break;
                        case Constant.Param_Write_ZKY_Reset_Liquid2:
                            currentReceiveType = Constant.Return_Write_ZKY_Reset_Liquid2;
                            break;
                        case Constant.Param_Write_ZKY_Reset_Liquid3:
                            currentReceiveType = Constant.Return_Write_ZKY_Reset_Liquid3;
                            break;
                        case Constant.Param_Write_ZKY_Reset_Liquid4:
                            currentReceiveType = Constant.Return_Write_ZKY_Reset_Liquid4;
                            break;
                        case Constant.Param_Write_ZKY_Reset_Liquid5:
                            currentReceiveType = Constant.Return_Write_ZKY_Reset_Liquid5;
                            break;
                        case Constant.Param_Write_ZKY_Btn_Close:
                            currentReceiveType = Constant.Return_Write_ZKY_Btn_Close;
                            break;
                        case Constant.Param_Write_ZKY_Btn_Open:
                            currentReceiveType = Constant.Return_Write_ZKY_Btn_Open;
                            break;
                        case Constant.Param_Write_ZKY_ZB_0:
                            currentReceiveType = Constant.Return_Write_ZKY_ZB_0;
                            break;
                        case Constant.Param_Write_ZKY_ZB_1:
                            currentReceiveType = Constant.Return_Write_ZKY_ZB_1;
                            break;
                        case Constant.Param_Write_ZKY_ZB_2:
                            currentReceiveType = Constant.Return_Write_ZKY_ZB_2;
                            break;
                        case Constant.Param_Write_ZKY_ZB_3:
                            currentReceiveType = Constant.Return_Write_ZKY_ZB_3;
                            break;
                        case Constant.Param_Write_ZKY_ZB_4:
                            currentReceiveType = Constant.Return_Write_ZKY_ZB_4;
                            break;
                        case Constant.Param_Write_ZKY_YW_0:
                            currentReceiveType = Constant.Return_Write_ZKY_YW_0;
                            break;
                        case Constant.Param_Write_ZKY_YW_1:
                            currentReceiveType = Constant.Return_Write_ZKY_YW_1;
                            break;
                        case Constant.Param_Write_ZKY_YW_2:
                            currentReceiveType = Constant.Return_Write_ZKY_YW_2;
                            break;
                        case Constant.Param_Write_ZKY_YW_3:
                            currentReceiveType = Constant.Return_Write_ZKY_YW_3;
                            break;
                        case Constant.Param_Write_ZKY_Arm_Time1:
                            currentReceiveType = Constant.Return_Write_ZKY_Arm_Time1;
                            break;
                        case Constant.Param_Write_ZKY_Arm_Time2:
                            currentReceiveType = Constant.Return_Write_ZKY_Arm_Time2;
                            break;
                        case Constant.Param_Write_ZKY_Arm_Speed:
                            currentReceiveType = Constant.Return_Write_ZKY_Arm_Speed;
                            break;
                        case Constant.Param_Write_ZKY_Titration_Speed:
                            currentReceiveType = Constant.Return_Write_ZKY_Titration_Speed;
                            break;
                        case Constant.Param_Write_ZKY_Titration_Standard:
                            currentReceiveType = Constant.Return_Write_ZKY_Titration_Standard;
                            break;
                        case Constant.Param_Write_ZKY_Liquid_Vol:
                            currentReceiveType = Constant.Return_Write_ZKY_Liquid_Vol;
                            break;
                        case Constant.Param_Write_ZKY_Pump_Speed:
                            currentReceiveType = Constant.Return_Write_ZKY_Pump_Speed;
                            break;
                        case Constant.Param_Write_ZKY_Standard_Vol:
                            currentReceiveType = Constant.Return_Write_ZKY_Standard_Vol;
                            break;
                        case Constant.Param_Write_ZKY_Sample_Vol:
                            currentReceiveType = Constant.Return_Write_ZKY_Sample_Vol;
                            break;
                        case Constant.Param_Write_ZKY_Liquid_ResetTime:
                            currentReceiveType = Constant.Return_Write_ZKY_Liquid_ResetTime;
                            break;
                        case Constant.Param_Write_ZKY_Liquid_OutTime:
                            currentReceiveType = Constant.Return_Write_ZKY_Liquid_OutTime;
                            break;
                        case Constant.Param_Write_ZKY_PYTime:
                            currentReceiveType = Constant.Return_Write_ZKY_PYTime;
                            break;
                        case Constant.Param_Write_ZKY_YWTime:
                            currentReceiveType = Constant.Return_Write_ZKY_YWTime;
                            break;
                        case Constant.Param_Write_ZKY_YW1:
                            currentReceiveType = Constant.Return_Write_ZKY_YW1;
                            break;
                        case Constant.Param_Write_ZKY_YW2AD:
                            currentReceiveType = Constant.Return_Write_ZKY_YW2AD;
                            break;
                        case Constant.Param_Write_ZKY_YW4:
                            currentReceiveType = Constant.Return_Write_ZKY_YW4;
                            break;
                        case Constant.Param_ZKY_YWADC:
                            currentReceiveType = Constant.Return_ZKY_YWADC;
                            break;
                        case Constant.Param_Write_ZKY_YW2:
                            currentReceiveType = Constant.Return_Write_ZKY_YW2;
                            break;
                        case Constant.Param_Wirte_ZKY_SurplusVol:
                            currentReceiveType = Constant.Return_Wirte_ZKY_SurplusVol;
                            break;
                        case Constant.Param_Wirte_ZKY_SurplusStep:
                            currentReceiveType = Constant.Return_Wirte_ZKY_SurplusStep;
                            break;
                        case Constant.Param_Write_ZKY_SurplusDensity:
                            currentReceiveType = Constant.Return_Write_ZKY_SurplusDensity;
                            break;
                        case Constant.Param_Write_ZKY_TEMP_COM:
                            currentReceiveType = Constant.Return_Write_ZKY_TEMP_COM;
                            break;
                        case Constant.Param_Write_ZKY_Wash_No:
                            currentReceiveType = Constant.Return_Write_ZKY_Wash_No;
                            break;
                        case Constant.Param_Write_ZKY_Ch_Time:
                            currentReceiveType = Constant.Return_Write_ZKY_Ch_Time;
                            break;
                        case Constant.Param_Write_ZKY_RX_Time:
                            currentReceiveType = Constant.Return_Write_ZKY_RX_Time;
                            break;
                        default:
                            currentReceiveType = 0;
                            return;
                    }
                    addLogText(str, 1, currentReceiveType);

                }

            } else if (characteristic.equals(mLogCharact)) {
                String logStr = ConvertData.bytesToHexString(recvData, false);
                addLogText("日志：" + logStr, Color.rgb(139, 0, 255), logStr.length());
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

    /**
     * @param log      内容
     * @param type     0-发送 1-接收 2-日志
     * @param dataType 数据类型
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
     * 解析消息内容
     *
     * @param arg1
     * @param arg2
     * @param obj
     */
    private void analysisMsg(int arg1, int arg2, Object obj) {
        if (arg1 == 0) {//发送的消息
            String msgContext = (String) obj;
            if (arg2==Constant.Return_BLE_False){
                showMsg(1,msgContext);
                if (loadingDialog != null&&!LiherZKYActivity.this.isFinishing()){
                    loadingDialog.dismiss();
                    SpannableStringBuilder builder = new SpannableStringBuilder( msgContext);
                    //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
                    builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    TextView tView = new TextView(context);
                    tView.setText(builder);
                    llOrders.addView(tView);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                }
            }else if (arg2 == Constant.PARAM_LIHER_ORDER || llOrderMode.getVisibility() == View.VISIBLE) {
                SpannableStringBuilder builder = new SpannableStringBuilder("发送:" + msgContext);
                //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                    SpannableStringBuilder builder = new SpannableStringBuilder("接收:" + msgContext);
                    //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(139, 0, 255));
                    builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    TextView tView = new TextView(context);
                    tView.setText(builder);
                    llOrders.addView(tView);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                    break;
                case Constant.Return_Read_Detail://220120#52#0#24#0#0#570#585#1000#0#1750
                    try {
                        String[] details = msgContext.split("#", -1);
                        version = details[0];
                        devAdd = Integer.parseInt(details[1]);
                        devStatus = Integer.parseInt(details[2]);
                        temperature = Integer.parseInt((details[3]));
                        long value4 = Long.parseLong(details[4]);
                        iv0.setSelected(((value4 & 0xff)) % 2 == 1);
                        ivBtn0.setSelected(((value4 & 0xff)) % 2 == 1);
                        iv1.setSelected(((value4 >> 1 & 0xff) % 2) == 1);
                        ivBtn1.setSelected(((value4 >> 1 & 0xff) % 2) == 1);
                        iv2.setSelected(((value4 >> 2 & 0xff) % 2) == 1);
                        ivBtn2.setSelected(((value4 >> 2 & 0xff) % 2) == 1);
                        iv3.setSelected(((value4 >> 3 & 0xff) % 2) == 1);
                        ivBtn3.setSelected(((value4 >> 3 & 0xff) % 2) == 1);
                        iv4.setSelected(((value4 >> 4 & 0xff) % 2) == 1);
                        ivBtn4.setSelected(((value4 >> 4 & 0xff) % 2) == 1);
                        iv5.setSelected(((value4 >> 5 & 0xff) % 2) == 1);
                        ivBtn5.setSelected(((value4 >> 5 & 0xff) % 2) == 1);
                        byte v4b6 = (byte) ((value4 >> 6 & 0xff) % 2);
                        byte v4b7 = (byte) ((value4 >> 7 & 0xff) % 2);
                        long value5 = Long.parseLong(details[5]);
                        if (value5 % 2 == 1) {
                            ivSampleBottle.setImageResource(R.mipmap.icon_yq_state_100);
                        } else if ((value5 >> 1 & 0xff) % 2 == 1) {
                            ivSampleBottle.setImageResource(R.mipmap.icon_yq_state_50);
                        } else {
                            ivSampleBottle.setImageResource(R.mipmap.icon_yq_state_0);
                        }
                        if ((value5 >> 2 & 0xff) % 2 == 1) {
                            ivStandardBottle.setImageResource(R.mipmap.icon_yq_state_100);
                        } else if ((value5 >> 3 & 0xff) % 2 == 1) {
                            ivStandardBottle.setImageResource(R.mipmap.icon_yq_state_50);
                        } else {
                            ivStandardBottle.setImageResource(R.mipmap.icon_yq_state_0);
                        }
                        pumpStatus = (value5 >> 4 & 0xff) % 2 == 1;
                        iv7.setSelected(pumpStatus);
                        armStatus = (value5 >> 5 & 0xff) % 2 == 1;
                        iv8.setSelected(armStatus);
                        buttonStatus = (value5 >> 6 & 0xff) % 2 == 1;
                        iv9.setSelected(buttonStatus);
                        doorStatus = (value5 >> 7 & 0xff) % 2 == 1;
                        iv10.setSelected(doorStatus);
                        sampleVol = DoubleUtils.div(Double.parseDouble(details[6]), 10, 1);
                        standerVol = DoubleUtils.div(Double.parseDouble(details[7]), 10, 1);
                        rubbishVol = Integer.parseInt(details[8]) / 10;
                        surplusVol = Integer.parseInt(details[9]);
                        pumpRatio = Integer.parseInt(details[10]);
                        tvDevVersion.setText(version);
                        tvDevAddress.setText(devAdd + "");
                        tvTemperature.setText(temperature + "℃");
                        tvWorkStatus.setText(StringUtils.getZKYDevStatus(devStatus));
                        tvStdVol.setText(standerVol + "mL");
                        tvSampleVol.setText(sampleVol + "mL");
                        tvSurplusVol.setText(surplusVol + "uL");
                        tvPumpRatio.setText(pumpRatio + "步/mL");
                        if (devStatus == 0x32 || devStatus == 0x33 || devStatus == 0x34 || devStatus == 0x42 || devStatus == 0x43 || devStatus == 0x51) {
                            new HitiStringDailog(LiherZKYActivity.this, Constant.HitiStrTargetDialog_ZKY_ErrCode, null, "故障报警", "设备故障：" + StringUtils.getZKYDevStatus(devStatus) + "！\n请修好后点击'设备复位'，才可以使用", LiherZKYActivity.this);
                        } else if (devStatus > 1 && devStatus != 0x31) {
                            showMsg(1,"设备报警：" + StringUtils.getZKYDevStatus(devStatus));
                        }
                        if (details.length >= 25) {
                            if (StringUtils.isInt(details[11])) {
                                hardVersion = Integer.parseInt(details[11]);
                                tvDevVersion.setText("程序V" + version + " 硬件V" + details[11].substring(0, details[11].length() - 1) + "." + details[11].substring(details[11].length() - 1));

                            }
                            if (!StringUtils.isEmpty(details[12]) && details[12].startsWith("2") && details[12].split("-").length == 6) {
                                liquidRefreshTime = DateUtil.format2miao("20" + details[12], "yyyy-MM-dd-HH-mm-ss");
                            }
                            if (!StringUtils.isEmpty(details[13]) && details[13].startsWith("2") && details[13].split("-").length == 6) {
                                liquidOutTime = DateUtil.format2miao("20" + details[13], "yyyy-MM-dd-HH-mm-ss");
                            }
                            tvLiquidRecord.setVisibility(View.VISIBLE);
                            yw1th = Integer.parseInt(details[14]);
                            yw2th = Integer.parseInt(details[15]);
                            yw4th = Integer.parseInt(details[16]);
                            pyTime = Integer.parseInt(details[17]);
                            ywTime = Integer.parseInt(details[18]);
                            yw2adth = Integer.parseInt(details[19]);
                            zb1Time = Integer.parseInt(details[20]);
                            zb2Time = Integer.parseInt(details[21]);
                            zbDuty = Integer.parseInt(details[22]);
                            vMax = Integer.parseInt(details[23]);
                            pumpDuty = Integer.parseInt(details[24]);
                        }
                        if (details.length >= 29) {
                            liquidStep = Integer.parseInt(details[25]);
                            surplusDensity100 = Integer.parseInt(details[26]);
                            tvSurplusDensity.setText(DoubleUtils.div(surplusDensity100, 100, 2) + "mg/L");
                            tempcom = Integer.parseInt(details[27]);
                            washno = Integer.parseInt(details[28]);
                            tvWashNo.setText(washno+"次");
                            ivTempCom.setSelected(tempcom==1?true:false);
                        }
                        if (details.length>=32){
                            ccstime=StringUtils.isEmpty(details[29])?0:Integer.parseInt(details[29]);
                            rxtime=StringUtils.isEmpty(details[30])?0:Integer.parseInt(details[30]);
                            chtime=StringUtils.isEmpty(details[31])?0:Integer.parseInt(details[31]);
                        }
                        tvCCSTime.setText(ccstime>0?(ccstime/100+"s"):"");
                        tvRXTime.setText(rxtime>0?(DoubleUtils.div(rxtime,100,1)+"s"):"");
                        tvChTime.setText(chtime>0?(DoubleUtils.div(chtime,100,1)+"s"):"");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case Constant.Return_Write_ZKY_Mac_Address:
                    if (msgContext != null && msgContext.contains("Write BLEaddr=")) {
                        showMsg(0,"设置成功,正在重启...");
                    } else {
                        showMsg(1,"设置蓝牙地址失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_ZKY_DevAddress:
                    if (msgContext != null && msgContext.contains("Write Devaddr=")) {
                        showMsg(0,"设备地址提交成功");
                    } else {
                        showMsg(1,"设置失败," + msgContext);
                    }
                    refreshDetails();
                    break;
                case Constant.Return_Wirte_ZKY_SurplusVol:
                    if (msgContext != null && msgContext.contains("Write vsu=")) {
                        showMsg(0,"剩余母液提交成功");
                        refreshDetails();
                    } else {
                        showMsg(2,"您的设备版本不支持设置剩余母液，请尽快升级设备程序");
                    }

                    break;
                case Constant.Return_Wirte_ZKY_SurplusStep:
                    if (msgContext != null && msgContext.contains("Write StepMax=")) {
                        showMsg(0,"设置母液量程步数成功");
                        refreshDetails();
                    } else {

                        showMsg(1,"设置母液量程步数失败\n" + msgContext);
                    }

                    break;
                case Constant.Return_Write_ZKY_SurplusDensity:
                    if (msgContext != null && msgContext.contains("Write nongdu=")) {
                        showMsg(0,"设置母液浓度成功");
                        refreshDetails();
                    } else {
                        showMsg(2,"您的设备版本不支持设置母液浓度，请尽快升级设备程序\n" + msgContext);
                    }

                    break;
                case Constant.Return_DEV_Reset:
                    if (msgContext != null && msgContext.equals("Set rst")) {
                        showMsg(0,"复位指令下发成功~");
                        finish();
                    } else {
                        showMsg(1,"复位指令返回失败~");
                        refreshDetails();
                    }


                    break;
                case Constant.Return_OPEN_LOCK:
                    if (msgContext != null && msgContext.equals("Set lock")) showMsg(0,"开锁命令下发成功~");
                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Standard_Set:
                    if (msgContext != null && msgContext.contains("Set pb=")) {
                        showMsg(0,"配标操作下发成功~");
                    } else {
                        showMsg(1,"配标操作下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Standard_Add:
                    if (msgContext != null && msgContext.contains("Set jb=")) {
                        showMsg(0,"加标操作下发成功~");
                    } else {
                        showMsg(1,"加标操作下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Pump_Extract:
                    if (msgContext != null && msgContext.contains("Set pull=")) {
                        showMsg(0,"柱塞泵抽取下发成功~");
                    } else {
                        showMsg(1,"柱塞泵抽取下发失败\n" + msgContext);
                    }

                    break;
                case Constant.Return_Write_ZKY_Pump_Put:
                    if (msgContext != null && msgContext.contains("Set push=")) {
                        showMsg(0,"柱塞泵前推下发成功~");
                    } else {
                        showMsg(1,"柱塞泵前推下发失败\n" + msgContext);
                    }

                    break;
                case Constant.Return_Write_ZKY_Reset_Liquid0:
                    if (msgContext != null && msgContext.equals("Set cmd=4")) {
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid1, 1, "", "第二步操作", "待排空柱塞泵后，请将接头接入母液杯,\n确认完成？", LiherZKYActivity.this);
                    } else {
                        showMsg(1,"开启更换母液失败~\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_ZKY_Reset_Liquid1:
                    if (msgContext != null && msgContext.equals("Set next")) {
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid2, 1, "", "第三步操作", "待抽取润洗母液后，请将接头接入废液杯,\n确认完成？", LiherZKYActivity.this);
                    } else if (msgContext != null && msgContext.contains("Error2")) {
                        showMsg(2,"还未排空柱塞泵，请稍后再试~");
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid1, 1, "", "第二步操作", "待排空柱塞泵后，请将接头接入母液杯,\n确认完成？", LiherZKYActivity.this);

                    } else {
                        showMsg(1,"更换母液第二步返回失败");
                        refreshDetails();
                    }
                    break;
                case Constant.Return_Write_ZKY_Reset_Liquid2:
                    if (msgContext != null && msgContext.equals("Set next")) {
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid3, 1, "", "第四步操作", "待再次排空后，请将接头接入母液杯,\n确认完成？", LiherZKYActivity.this);
                    } else if (msgContext != null && msgContext.contains("Error2")) {
                        showMsg(2,"未完成抽取润洗母液，请稍后再试~");
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid2, 1, "", "第三步操作", "待抽取润洗母液后，请将接头接入废液杯,\n确认完成？", LiherZKYActivity.this);
                    } else {
                        showMsg(1,"更换母液第三步返回失败");
                        refreshDetails();
                    }
                    break;
                case Constant.Return_Write_ZKY_Reset_Liquid3:
                    if (msgContext != null && msgContext.equals("Set next")) {
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid4, 1, "", "第五步操作", "待抽取母液，请确认针筒内无气泡？", LiherZKYActivity.this);
                    } else if (msgContext != null && msgContext.contains("Error2")) {
                        showMsg(2,"还未再次排空，请稍后再试~");
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid3, 1, "", "第四步操作", "待再次排空后，请将接头接入母液杯,\n确认完成？", LiherZKYActivity.this);

                    } else {
                        showMsg(1,"更换母液第四步返回失败");
                        refreshDetails();
                    }
                    break;
                case Constant.Return_Write_ZKY_Reset_Liquid4:
                    if (msgContext != null && msgContext.equals("Set next")) {
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid5, 1, "", "最后一步", "请将接头接入质控仪转臂，然后确认完成", LiherZKYActivity.this);
                    } else if (msgContext != null && msgContext.contains("Error2")) {
                        showMsg(2,"未完成抽取母液，请稍后再试~");
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid4, 1, "", "第五步操作", "待抽取母液，请确认针筒内无气泡？", LiherZKYActivity.this);
                    } else {
                        showMsg(1,"更换母液第五步返回失败");
                        refreshDetails();
                    }
                    break;
                case Constant.Return_Write_ZKY_Reset_Liquid5:
                    if (msgContext != null && msgContext.equals("Set next")) {

                        if (tvLiquidRecord.getVisibility() == View.VISIBLE) {
                            showMsg(0,"更换母液成功~");
                            new HitiStringDailog(LiherZKYActivity.this, Constant.HitiStrTargetDialog_ZKY_Reset_Liquid_Record, 1, "", "母液更换记录", "是否以当前时间，自动填报母液信息记录?", LiherZKYActivity.this);

                        } else {
                            showMsg(0,"更换母液成功~");
                            refreshDetails();
                        }

                    } else if (msgContext != null && msgContext.contains("Error2")) {
                        showMsg(2,"未将接头接入质控仪转臂，请稍后再试~" + msgContext);
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_ZKY_Reset_Liquid5, 1, "", "最后一步", "请将接头接入质控仪转臂，然后确认完成", LiherZKYActivity.this);

                    } else {
                        showMsg(1,"未将接头接入质控仪转臂,更换母液失败~\n" + msgContext);
                        refreshDetails();
                    }

                    break;
                case Constant.Return_Write_ZKY_Btn_Close:
                    if (msgContext != null && msgContext.contains("Set off=")) {
                        showMsg(0,"关闭指令下发成功~");
                        RxTimerUtil.timer(1000, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1,"关闭失败~");
                    }

                    break;
                case Constant.Return_Write_ZKY_Btn_Open:
                    if (msgContext != null && msgContext.contains("Set on=")) {
                        showMsg(0,"打开指令下发成功~");
                        RxTimerUtil.timer(1000, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1,"打开失败~");
                    }


                    break;
                case Constant.Return_Write_ZKY_ZB_0:
                    if (msgContext != null && msgContext.equals("Set zb=0")) {
                        showMsg(0,"转臂控制标样位下发成功~");
                    } else {
                        showMsg(1,"转臂控制标样位下发失败\n" + msgContext);
                    }

                    break;
                case Constant.Return_Write_ZKY_ZB_1:
                    if (msgContext != null && msgContext.equals("Set zb=1")) {
                        showMsg(0,"转臂控制溢流位下发成功~");
                    } else {
                        showMsg(1,"转臂控制溢流位下发失败\n" + msgContext);
                    }

                    break;
                case Constant.Return_Write_ZKY_ZB_2:
                    if (msgContext != null && msgContext.equals("Set zb=2")) {
                        showMsg(0,"转臂控制水样位下发成功~");
                    } else {
                        showMsg(1,"转臂控制水样位下发失败\n" + msgContext);
                    }

                    break;
                case Constant.Return_Write_ZKY_ZB_3:
                    if (msgContext != null && msgContext.equals("Set zb=3")) {
                        showMsg(0,"转臂控制左下发成功~");
                    } else {
                        showMsg(1,"转臂控制左下发失败\n" + msgContext);
                    }

                    break;
                case Constant.Return_Write_ZKY_ZB_4:
                    if (msgContext != null && msgContext.equals("Set zb=4"))
                    showMsg(0,"转臂控制右下发成功~");
                    break;
                case Constant.Return_Write_ZKY_YW_0:
                    if (msgContext != null && msgContext.equals("Set yw=1")) {
                        showMsg(0,"设置水样高液位下发成功~");
                    } else {
                        showMsg(1,"设置水样高液位下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_YW_1:
                    if (msgContext != null && msgContext.equals("Set yw=2")) {
                        showMsg(0,"设置水样低液位下发成功~");
                    } else {
                        showMsg(1,"设置水样低液位下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_YW_2:
                    if (msgContext != null && msgContext.equals("Set yw=3")) {
                        showMsg(0,"设置标样高液位下发成功~");
                    } else {
                        showMsg(1,"设置标样高液位下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_YW_3:
                    if (msgContext != null && msgContext.equals("Set yw=4")) {
                        showMsg(0,"设置标样低液位下发成功~");
                    } else {
                        showMsg(1,"设置标样低液位下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Arm_Time1:
                    if (msgContext != null && msgContext.contains("Write ZT1=")) {
                        showMsg(0,"设置转臂时间1下发成功~");
                    } else {
                        showMsg(1,"设置转臂时间1下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Arm_Time2:
                    if (msgContext != null && msgContext.contains("Write ZT2=")) {
                        showMsg(0,"设置转臂时间2下发成功~");
                    } else {
                        showMsg(1,"设置转臂时间2下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Arm_Speed:
                    if (msgContext != null && msgContext.contains("Write ZDuty=")) {
                        showMsg(0,"设置转臂速度下发成功~");
                    } else {
                        showMsg(1,"设置转臂速度下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Titration_Speed:
                    if (msgContext != null && msgContext.contains("Write Vmax=")) {
                        showMsg(0,"设置滴定速度下发成功~");
                    } else {
                        showMsg(1,"设置滴定速度下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Titration_Standard:
                    if (msgContext != null && msgContext.contains("Write Calib=")) {
                        showMsg(0,"设置滴定校准下发成功~");
                    } else {
                        showMsg(1,"设置滴定校准下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Liquid_Vol:
                    if (msgContext != null && msgContext.contains("Write Cdrop=")) {
                        showMsg(0,"设置废液体积下发成功~");
                    } else {
                        showMsg(1,"设置废液体积下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Pump_Speed:
                    if (msgContext != null && msgContext.contains("Write PDuty=")) {
                        showMsg(0,"设置配标水泵速度下发成功~");
                    } else {
                        showMsg(1,"设置配标水泵速度下发失败\n" + msgContext);
                    }
                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Standard_Vol:
                    if (msgContext != null && msgContext.contains("Write vby=")) {
                        showMsg(0,"设置标样定容下发成功~");
                    } else {
                        showMsg(1,"设置标样定容下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_Sample_Vol:
                    if (msgContext != null && msgContext.contains("Write vsy=")) {
                        showMsg(0,"设置水样定容下发成功~");
                    } else {
                        showMsg(1,"设置水样定容下发失败\n" + msgContext);
                    }

                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_PYTime:
                    if (msgContext != null && msgContext.contains("Write pytime=")) {
                        showMsg(0,"设置排样时间" + msgContext.substring(13) + "下发成功~");
                    } else if (msgContext != null) {
                        showMsg(1,"当前质控仪程序版本不支持设置排样时间," + msgContext);
                    }
                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_YWTime:
                    if (msgContext != null && msgContext.contains("Write ywtime=")) {
                        showMsg(0,"设置液位电源间隔" + msgContext.substring(13) + "下发成功~");
                    } else if (msgContext != null) {
                        showMsg(2,"当前质控仪程序版本不支持设置液位电源间隔," + msgContext);
                    }
                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_YW1:
                    if (msgContext != null && msgContext.contains("Write yw1th=")) {
                        showMsg(0,"设置液位1判断阈值" + msgContext.substring(12) + "下发成功~");
                    } else if (msgContext != null) {
                        showMsg(2,"当前质控仪程序版本不支持设置液位1判断阈值," + msgContext);
                    }
                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_YW2AD:
                    if (msgContext != null && msgContext.contains("Write yw2adth=")) {
                        showMsg(0,"设置液位2判断阈值" + msgContext.substring(12) + "下发成功~");
                    } else if (msgContext != null) {
                        showMsg(1,"当前质控仪程序版本不支持设置液位2判断阈值," + msgContext);
                    }
                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_YW4:
                    if (msgContext != null && msgContext.contains("Write yw4th=")) {
                        showMsg(0,"设置液位4判断阈值" + msgContext.substring(12) + "下发成功~");
                    } else if (msgContext != null) {
                        showMsg(2,"当前质控仪程序版本不支持设置液位4判断阈值," + msgContext);
                    }
                    refreshDetails();
                    break;
                case Constant.Return_Write_ZKY_YW2:
                    if (msgContext != null && msgContext.contains("Write yw2th=")) {
                        showMsg(0,"设置液位2DDL判断阈值" + msgContext.substring(12) + "下发成功~");
                    } else if (msgContext != null) {
                        showMsg(2,"当前质控仪程序版本不支持设置液位2DDL判断阈值," + msgContext);
                    }
                    refreshDetails();
                    break;
                case Constant.Return_ZKY_YWADC:
                    if (!StringUtils.isEmpty(msgContext) && msgContext.contains("\n")) {
                        String[] ywadcs = msgContext.trim().split("\n", -1);
                        if (ywadcs != null && ywadcs.length >= 4) {
                            tvYW1Info.setText(ywadcs[0].substring(4));
                            tvYW2Info.setText(ywadcs[1].substring(4));
                            tvYW4Info.setText(ywadcs[2].substring(4));
                            tvYW2ADInfo.setText(ywadcs[3].substring(6));
                            return;
                        }

                    } else if (!StringUtils.isEmpty(msgContext) && msgContext.contains("#")) {
                        String[] ywadcs = msgContext.trim().split("#", -1);
                        if (ywadcs != null && ywadcs.length >= 4) {
                            tvYW1Info.setText(ywadcs[0].substring(3));
                            tvYW2Info.setText(ywadcs[1].substring(3));
                            tvYW4Info.setText(ywadcs[2].substring(3));
                            tvYW2ADInfo.setText(ywadcs[3].substring(6));
                            return;
                        }
                    }

                     showMsg(2,"当前质控仪程序版本不支持读取液位1、2、4阈值,请尽快升级设备程序\n" + msgContext);

                    break;
                case Constant.Return_Write_ZKY_Liquid_ResetTime:
                    if (msgContext != null && msgContext.contains("Write changetime=")) {
                        if (tempOutTime > 0) {
                            RxTimerUtil.cancel();
                            RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                                @Override
                                public void doNext(long number) {
                                    sendLHBleData("Write expiration=" + DateUtil.long2time10(tempOutTime).substring(2), Constant.Param_Write_ZKY_Liquid_OutTime);

                                }
                            });

                        } else {
                            showMsg(0,"登记母液更换日期成功！");
                            refreshDetails();
                        }
                    } else {
                        showMsg(1,"登记母液更换日期失败！");
                    }
                    tempResetTime = 0;

                    break;
                case Constant.Return_Write_ZKY_Liquid_OutTime:
                    if (msgContext != null && msgContext.contains("Write expiration=")) {
                        showMsg(0,"登记母液信息成功！");
                        refreshDetails();
                    } else {
                        showMsg(1,"登记母液过期日期失败！");
                    }
                    tempOutTime = 0;
                    break;
                case Constant.Return_Write_ZKY_TEMP_COM:
                    if (msgContext != null && msgContext.contains("Write tempcom=1")) {
                        showMsg(0,"开启成功");
                        tempcom=1;
                        ivTempCom.setSelected(true);
                    } else if (msgContext != null && msgContext.contains("Write tempcom=0")) {
                        showMsg(1,"关闭成功");
                        tempcom=0;
                        ivTempCom.setSelected(false);
                    } else {
                        showMsg(1,"操作柱塞泵温补开关失败！");
                    }
                    break;

                case Constant.Return_Write_ZKY_Wash_No:
                    if (msgContext != null && msgContext.contains("Write washno")) {
                        showMsg(0,"设置成功！");
                        refreshDetails();
                    } else {
                        showMsg(1,"设置配标润洗次数" +"失败！");
                    }
                    break;
                case Constant.Return_Write_ZKY_RX_Time:
                    if (msgContext != null && msgContext.contains("Write rxtime")) {
                        showMsg(0,"设置成功！");
                        refreshDetails();
                    } else {
                        showMsg(1,"设置配标润洗时间" + "失败！");
                    }
                    break;
                case Constant.Return_Write_ZKY_Ch_Time:
                    if (msgContext != null && msgContext.contains("Write chtime")) {
                        showMsg(0,"设置成功！");
                        refreshDetails();
                    } else {
                        showMsg(1,"设置配标侧吸定容时间" +"失败！");
                    }
                    break;

                case Constant.Return_BLE_False:
                    showMsg(1,msgContext);
                    if (loadingDialog != null) loadingDialog.dismiss();
                    break;
            }


        } else if (arg1 == 2) {//日志信息

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
    public void onBackPressed() {
        if (llOrderMode != null && llOrderMode.getVisibility() == View.VISIBLE) {
            sclUIMode.setVisibility(View.VISIBLE);
            llOrderMode.setVisibility(View.GONE);
            rightTitleTxt.setText("交互模式");
            refreshDetails();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void finish() {
        if (llOrderMode != null && llOrderMode.getVisibility() == View.VISIBLE) {
            sclUIMode.setVisibility(View.VISIBLE);
            llOrderMode.setVisibility(View.GONE);
            rightTitleTxt.setText("交互模式");
            refreshDetails();
            return;
        }
        super.finish();
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

                case Constant.RequestCode_Scan_Box_MAC_Address:
                    String boxQcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(boxQcode)) {
                        showMsg(2,"未读取到二维码信息");
                    } else if (boxQcode.length() != 12) {
                        showMsg(2,"二维码，非力合蓝牙标准码");
                    } else if (boxQcode.startsWith("0E")) {
                        String macAds = boxQcode.trim();
                        new HitiStringDailog(LiherZKYActivity.this, Constant.Dailog_BLE_MACAddress, macAds, "Mac地址确认", "确认设置Mac地址为：" + StringUtils.getNewBlueToothMacAddress(macAds) + "?\n蓝牙将重启后生效", LiherZKYActivity.this);
                    } else {
                        showMsg(2,boxQcode + "二维码，非力合质控仪编码规则0E");
                    }
                    break;
            }
        }
    }
}
