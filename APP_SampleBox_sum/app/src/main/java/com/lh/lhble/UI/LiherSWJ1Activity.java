package com.lh.lhble.UI;

import static com.lh.lhble.google.zxing.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;

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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;

import com.alibaba.fastjson.JSON;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.UI.View.RangeCurveEditDailog;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.google.zxing.activity.CaptureActivity;
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
import java.util.List;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 雷达水位计
 */
public class LiherSWJ1Activity extends BaseActivity implements CommontClickListener, View.OnClickListener {
    private final static String TAG = LiherSWJ1Activity.class.getSimpleName();

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
    private String sampleId = "ShuiWeiJi";
    private LinearLayout llUIMode;
    private TextView tvRefresh,tvWorkState, tvDevEV,tvSysTime,tvDevNum,tvDevVersion,tvDevMN,tvMacAddress,tvSetMacAddress,tvSimCard,tvNetPort,tvDevIp,tvLotLev,tvDevDistance,tvInstallHigh,
            tvHuangHai,tvSampleInterval,tvUpdateInterval,tvRangeCurve,tvRangeCurve1,tvRangeCurve2,tvRangeCurve3,tvRangeCurve4,tvSurplusVol,tvAdsLevel,tvRxLev,tvRange,tvCalibration,tvBlindSet;
    private ScrollView sclOrders;
    private LinearLayout llOrderMode, llOrders;
    private EditText sendData;
    private TextView send;
    private LinearLayout llParamsBtn,llWorkBtn;
    private TextView tvParamsRead, tvParamsUp, tvResetNet, tvResetFlow, tvResetRx,tvSaveNum, tvTimeReads,tvWorkStart,tvAngle,tvUploadType,tvRTUType,tvModbusMode;


    private LoadingDialog loadingDialog = null;

    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;

    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private int currentSendType, currentReceiveType;

    private String tempBottleMac;
    public String version;
    private long sysTime;
    private String devNum,devMN,devIP,simCard;
    private int devEv,devInstallHigh,devRange,devCalibration,devBlindSet,waterLev,waterAbsLev,devDistance,huangHai,upInterval,sapInterval,saveNum,protocol,rtuType,modbusMode,lotStep;
    private int devRxLev=-1;
    private int devLotLev=-1;
    private int currentFlow,intervaFlow,netPort;
    private long totalFlow;
    private int moduleSTA;
    private long moduleSTA16=-1;
    double curveA= Constant.DefaultValue;
    double curveB=Constant.DefaultValue;
    double curveC=Constant.DefaultValue;
    int curveSWYZ= Constant.DefaultValue;
    double curve1A=Constant.DefaultValue;
    double curve1B=Constant.DefaultValue;
    double curve1C=Constant.DefaultValue;
    int curve1SWYZ= Constant.DefaultValue;
    double curve2A=Constant.DefaultValue;
    double curve2B=Constant.DefaultValue;
    double curve2C=Constant.DefaultValue;
    int curve2SWYZ= Constant.DefaultValue;
    double curve3A=Constant.DefaultValue;
    double curve3B=Constant.DefaultValue;
    double curve3C=Constant.DefaultValue;
    int curve3SWYZ= Constant.DefaultValue;
    double curve4A=Constant.DefaultValue;
    double curve4B=Constant.DefaultValue;
    double curve4C=Constant.DefaultValue;
    int curve4SWYZ= Constant.DefaultValue;
    int angle;
    int isMeaSure=0;
    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_swj_1;
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
        if (HJBleApplication.getUser().getStrPermissions().contains("PRO_Qualification")||HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
            setRightText(HJBleApplication.getAppContext().getString(R.string.ui_mode));
            rightTitleTxt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSelectUIModeMenu();
                }
            });
            rightTitleTxt.setVisibility(View.VISIBLE);
            tvSetMacAddress.setVisibility(View.VISIBLE);
        }else {
            rightTitleTxt.setVisibility(View.GONE);
        }
        if (HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
            tvSetMacAddress.setVisibility(View.VISIBLE);
        }else {
            tvSetMacAddress.setVisibility(View.INVISIBLE);
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
                showMsg(2,HJBleApplication.getAppContext().getString(R.string.init_data));
            }
        });
    }


    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherSWJ1Activity.class);
        intent.putExtra(LiherSWJ1Activity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherSWJ1Activity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherSWJ1Activity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();
        llUIMode = findViewById(R.id.ll_ui_mode);
        tvRefresh = findViewById(R.id.tv_refresh);
        tvWorkState=findViewById(R.id.tv_dev_work_state);
        tvDevEV = findViewById(R.id.tv_dev_ev);
        tvSysTime = findViewById(R.id.tv_sys_time);
        tvDevVersion = findViewById(R.id.tv_dev_version);
        tvDevNum = findViewById(R.id.tv_dev_num);
        tvDevMN = findViewById(R.id.tv_dev_mn);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);
        tvSurplusVol = findViewById(R.id.tv_surplus_vol);
        tvAdsLevel=findViewById(R.id.tv_ads_level);
        tvSimCard=findViewById(R.id.tv_sim_card);
        tvDevIp = findViewById(R.id.tv_dev_ip);
        tvNetPort=findViewById(R.id.tv_net_port);
        tvLotLev=findViewById(R.id.tv_lot_lev);
        tvDevDistance = findViewById(R.id.tv_dev_distance);
        tvInstallHigh = findViewById(R.id.tv_install_high);
        tvRxLev = findViewById(R.id.tv_rx_lev);
        tvRange = findViewById(R.id.tv_range);
        tvCalibration = findViewById(R.id.tv_calibration);
        tvBlindSet = findViewById(R.id.tv_blind_set);
        tvHuangHai=findViewById(R.id.tv_huang_hai);
        tvSampleInterval=findViewById(R.id.tv_sample_interval);
        tvUpdateInterval=findViewById(R.id.tv_update_interval);
        tvRangeCurve=findViewById(R.id.tv_range_curve);
        tvRangeCurve1=findViewById(R.id.tv_range_curve1);
        tvRangeCurve2=findViewById(R.id.tv_range_curve2);
        tvRangeCurve3=findViewById(R.id.tv_range_curve3);
        tvRangeCurve4=findViewById(R.id.tv_range_curve4);
        llOrderMode = findViewById(R.id.ll_order_mode);
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        sendData = findViewById(R.id.sendData);
        send = findViewById(R.id.send);
        llParamsBtn=findViewById(R.id.ll_params_btn);
        tvParamsRead = findViewById(R.id.tv_params_read);
        tvResetNet = findViewById(R.id.tv_reset_net);
        tvResetFlow=findViewById(R.id.tv_reset_flow);
        tvResetRx=findViewById(R.id.tv_reset_rx);
        tvSaveNum=findViewById(R.id.tv_save_num);
        tvTimeReads = findViewById(R.id.tv_time_reads);
        tvParamsUp = findViewById(R.id.tv_params_up);
        llWorkBtn=findViewById(R.id.ll_work_btn);
        tvWorkStart=findViewById(R.id.tv_start_work);
        tvAngle=findViewById(R.id.tv_angle);
        tvUploadType=findViewById(R.id.tv_upload_type);
        tvRTUType=findViewById(R.id.tv_rtu_type);
        tvModbusMode=findViewById(R.id.tv_modbus_mode);
        tvDevNum.setOnClickListener(LiherSWJ1Activity.this);
        tvDevMN.setOnClickListener(LiherSWJ1Activity.this);
        tvRefresh.setOnClickListener(LiherSWJ1Activity.this);
        tvRxLev.setOnClickListener(LiherSWJ1Activity.this);
        tvSetMacAddress.setOnClickListener(LiherSWJ1Activity.this);
        tvDevIp.setOnClickListener(LiherSWJ1Activity.this);
        tvLotLev.setOnClickListener(LiherSWJ1Activity.this);
        tvSampleInterval.setOnClickListener(LiherSWJ1Activity.this);
        tvUpdateInterval.setOnClickListener(LiherSWJ1Activity.this);
        tvNetPort.setOnClickListener(LiherSWJ1Activity.this);
        tvHuangHai.setOnClickListener(LiherSWJ1Activity.this);
        tvRangeCurve.setOnClickListener(LiherSWJ1Activity.this);
        tvRangeCurve1.setOnClickListener(LiherSWJ1Activity.this);
        tvRangeCurve2.setOnClickListener(LiherSWJ1Activity.this);
        tvRangeCurve3.setOnClickListener(LiherSWJ1Activity.this);
        tvRangeCurve4.setOnClickListener(LiherSWJ1Activity.this);
        tvInstallHigh.setOnClickListener(LiherSWJ1Activity.this);
        send.setOnClickListener(LiherSWJ1Activity.this);
        tvWorkStart.setOnClickListener(LiherSWJ1Activity.this);
       // tvAngle.setOnClickListener(LiherSWJ1Activity.this);
        tvUploadType.setOnClickListener(LiherSWJ1Activity.this);
       // tvRTUType.setOnClickListener(LiherSWJ1Activity.this);
        tvModbusMode.setOnClickListener(LiherSWJ1Activity.this);
        tvRange.setOnClickListener(LiherSWJ1Activity.this);
        tvCalibration.setOnClickListener(LiherSWJ1Activity.this);
        tvBlindSet.setOnClickListener(LiherSWJ1Activity.this);
        tvParamsRead.setOnClickListener(LiherSWJ1Activity.this);
        tvParamsUp.setOnClickListener(LiherSWJ1Activity.this);
        tvResetNet.setOnClickListener(LiherSWJ1Activity.this);
        tvResetFlow.setOnClickListener(LiherSWJ1Activity.this);
        tvResetRx.setOnClickListener(LiherSWJ1Activity.this);
        tvTimeReads.setOnClickListener(LiherSWJ1Activity.this);
        LoadingDialog.Builder loadBuilder = new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage(HJBleApplication.getAppContext().getString(R.string.loading_normal))
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

            setTitle(mDeviceName);

        } else {
            // 发送服务
            mSendCharact = Constant.Ble_Data_Send_Service;
            // 接收服务
            mReceiveCharact = Constant.Ble_Data_Receive_Service;

            setTitle(mDeviceName);
        }


        if (!StringUtils.isEmpty(mDeviceAddress)) tvMacAddress.setText(mDeviceAddress);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        if (((byte) ((moduleSTA16>>2 & 0xff) % 2))==1&&v.getId()!=R.id.tv_refresh){ 
            showMsg(2,"设备正在上传数据，请刷新后重试");
            return;
        }

        switch (v.getId()) {

            case R.id.tv_time_reads:
                RxTimerUtil.cancel();
                if (isMeaSure==0){
                    isMeaSure=1;
                    tvTimeReads.setText(HJBleApplication.getAppContext().getString(R.string.stop_circle));
                    RxTimerUtil.interval(10000L, new RxTimerUtil.IRxNext() {
                        @Override
                        public void doNext(long number) {
                            sendLHBleData("Read TimeData", Constant.Param_SWJ_Measure);
                        }
                    });
                }else {
                    isMeaSure=0;
                    tvTimeReads.setText(HJBleApplication.getAppContext().getString(R.string.circle_read));
                }
                break;
            case R.id.tv_dev_ip:
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_DevIP, HJBleApplication.getAppContext().getString(R.string.reset_ip_address), HJBleApplication.getAppContext().getString(R.string.ip_address), "", LiherSWJ1Activity.this);
                break;
            case R.id.tv_net_port:
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_NetPort, HJBleApplication.getAppContext().getString(R.string.set_net_port), HJBleApplication.getAppContext().getString(R.string.net_port), "", LiherSWJ1Activity.this);
                break;
            case R.id.tv_huang_hai:
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_HuangHai,  HJBleApplication.getAppContext().getString(R.string.set_huanghai), HJBleApplication.getAppContext().getString(R.string.huanghai), "", LiherSWJ1Activity.this);
                break;
            case R.id.tv_sample_interval:
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_SampleInterval, HJBleApplication.getAppContext().getString(R.string.set_sample_interval_time), HJBleApplication.getAppContext().getString(R.string.sample_interval_time), "", LiherSWJ1Activity.this);
                break;
            case R.id.tv_update_interval:
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_UpdateInterval, HJBleApplication.getAppContext().getString(R.string.set_update_interval_time), HJBleApplication.getAppContext().getString(R.string.update_interval_time), "", LiherSWJ1Activity.this);
                break;

            case R.id.tv_range:
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_Range, HJBleApplication.getAppContext().getString(R.string.set_range), HJBleApplication.getAppContext().getString(R.string.swj_range), "", LiherSWJ1Activity.this);

                break;
            case R.id.tv_params_read:
                sendLHBleData("Read TimeData", Constant.Param_SWJ_TimeData);
                break;
            case R.id.tv_lot_lev:
                sendLHBleData("Read LotLev",Constant.Param_SWJ_LotLev);
                break;
            case R.id.tv_rx_lev:
                sendLHBleData("Read LdLev", Constant.Param_Rx_Lex);
                break;
            case R.id.tv_range_curve:
                new RangeCurveEditDailog(LiherSWJ1Activity.this,Constant.Dialog_SWJ_RangeCurve,0,curveA,curveB,curveC,curveSWYZ, LiherSWJ1Activity.this);
                break;
            case R.id.tv_range_curve1:
                new RangeCurveEditDailog(LiherSWJ1Activity.this,Constant.Dialog_SWJ_RangeCurve,1,curve1A,curve1B,curve1C,curve1SWYZ, LiherSWJ1Activity.this);
                break;
            case R.id.tv_range_curve2:
                new RangeCurveEditDailog(LiherSWJ1Activity.this,Constant.Dialog_SWJ_RangeCurve,2,curve2A,curve2B,curve2C,curve2SWYZ, LiherSWJ1Activity.this);
                break;
            case R.id.tv_range_curve3:
                new RangeCurveEditDailog(LiherSWJ1Activity.this,Constant.Dialog_SWJ_RangeCurve,3,curve3A,curve3B,curve3C,curve3SWYZ, LiherSWJ1Activity.this);
                break;
            case R.id.tv_range_curve4:
                new RangeCurveEditDailog(LiherSWJ1Activity.this,Constant.Dialog_SWJ_RangeCurve,4,curve4A,curve4B,curve4C,curve4SWYZ, LiherSWJ1Activity.this);
                break;
            case R.id.tv_calibration:
                 new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_Calibration, HJBleApplication.getAppContext().getString(R.string.swj_set_calibration), HJBleApplication.getAppContext().getString(R.string.swj_calibration), "",HJBleApplication.getAppContext().getString(R.string.swj_set_calibration_tips),
                        LiherSWJ1Activity.this);

                break;
            case R.id.tv_blind_set:
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_BlindSet, HJBleApplication.getAppContext().getString(R.string.swj_set_blind), HJBleApplication.getAppContext().getString(R.string.swj_blind), "", LiherSWJ1Activity.this);

                break;
            case R.id.tv_set_mac_address:
                startActivityForResult(new Intent(LiherSWJ1Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_Box_MAC_Address);
                break;
            case R.id.tv_refresh:
                refreshDetails();
                break;
            case R.id.tv_dev_num:
                if (HJBleApplication.getUser().getStrPermissions().contains("PRO_Qualification")||HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
                    new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_DevNum, HJBleApplication.getAppContext().getString(R.string.swj_set_devnum), HJBleApplication.getAppContext().getString(R.string.swj_devnum), StringUtils.isEmpty(devNum)?"":devNum, LiherSWJ1Activity.this);

                }else {
                     showMsg(2,HJBleApplication.getAppContext().getString(R.string.swj_not_pro_fix_premission));
                }
                 break;
            case R.id.tv_dev_mn:
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_DevMN, HJBleApplication.getAppContext().getString(R.string.swj_set_mn), HJBleApplication.getAppContext().getString(R.string.swj_mn), StringUtils.isEmpty(devMN)?"":devMN, LiherSWJ1Activity.this);
                break;

            case R.id.tv_dev_reset:
                new HitiStringDailog(LiherSWJ1Activity.this, Constant.HitiStrTargetDailog_ZKY_Reset, "", HJBleApplication.getAppContext().getString(R.string.reset_dlg_title), HJBleApplication.getAppContext().getString(R.string.reset_dlg_context), LiherSWJ1Activity.this);
                break;

            case R.id.send:
                if (StringUtils.isEmpty(sendData.getText().toString())) {
                   showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_order));
                    return;
                }
                sendLHBleData(sendData.getText().toString().trim(), Constant.PARAM_LIHER_ORDER);
                sendData.setText("");
                break;
            case R.id.tv_install_high:
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_Dev_InstallHigh, HJBleApplication.getAppContext().getString(R.string.swj_set_install_high), HJBleApplication.getAppContext().getString(R.string.swj_install_high), devInstallHigh>0?(DoubleUtils.div(devInstallHigh,1000,3)+""):"", LiherSWJ1Activity.this);
                break;
            case R.id.tv_angle:
                showMsg(2,HJBleApplication.getAppContext().getString(R.string.input_right_angle));
                new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dialog_SWJ_Dev_Angle, HJBleApplication.getAppContext().getString(R.string.swj_set_angle), HJBleApplication.getAppContext().getString(R.string.angle), angle>0?(DoubleUtils.div(angle,10d,1)+""):"", LiherSWJ1Activity.this);
                break;
            case R.id.tv_upload_type:
                showSelectUploadTypeMenu();

                break;
          /*  case R.id.tv_rtu_type:
                showSelectRTUTypeMenu();
                break;*/
            case R.id.tv_modbus_mode:
                showSelectModbusModeMenu();
                break;
            case R.id.tv_params_up:
                sendLHBleData("Set DataUp", Constant.Param_Write_SWJ_ParamUp);
                break;
            case R.id.tv_reset_net:
                new HitiStringDailog(LiherSWJ1Activity.this,Constant.Dialog_SWJ_ResetNet,null,HJBleApplication.getAppContext().getString(R.string.init_net_dlg_title),HJBleApplication.getAppContext().getString(R.string.init_net_dlg_context), LiherSWJ1Activity.this);
                break;
            case R.id.tv_reset_flow:
                new HitiStringDailog(LiherSWJ1Activity.this,Constant.Dialog_SWJ_ResetFlow,null,HJBleApplication.getAppContext().getString(R.string.reset_flow_dlg_title),HJBleApplication.getAppContext().getString(R.string.reset_flow_dlg_context), LiherSWJ1Activity.this);
                break;
            case R.id.tv_reset_rx:
                new HitiStringDailog(LiherSWJ1Activity.this,Constant.Dialog_SWJ_ResetRx,null,HJBleApplication.getAppContext().getString(R.string.reset_rx_dlg_title),HJBleApplication.getAppContext().getString(R.string.reset_rx_dlg_context), LiherSWJ1Activity.this);
                break;
            case R.id.tv_start_work:
                if (sapInterval<=0){
                    showMsg(2,HJBleApplication.getAppContext().getString(R.string.input_right_sample_interval));
                 }else if (upInterval<=0){
                    showMsg(2,HJBleApplication.getAppContext().getString(R.string.input_right_update_interval));
                  }else if (StringUtils.isEmpty(devIP)||devIP.length()!=12){
                    showMsg(2,HJBleApplication.getAppContext().getString(R.string.input_right_ip));
                   }else if (StringUtils.isEmpty(devMN)){
                    showMsg(2,HJBleApplication.getAppContext().getString(R.string.input_right_mn)); 
                }else {
                    sendLHBleData("Set Start", Constant.Param_Write_SWJ_Start);
                }

                 break;
        }
    }

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.HitiStrTargetDialog_SWJ_TimeData:

                break;
            case Constant.Dailog_BLE_SYSDateTime:
            String sysTime = (String) target;
            if (sysTime != null && sysTime.length() == 19) {
                sendLHBleData("Write SysTime=" + DateUtils.formatTime(sysTime, DateUtils.DATE_TIME_PATTERN, DateUtils.METHOD_DATE_TIME_PATTERN).substring(2),Constant.Param_Wirte_SysTime);
            } else { 
                showMsg(2,HJBleApplication.getAppContext().getString(R.string.input_right_systime)+"，" + sysTime);
            }
            break;
            case Constant.Dialog_SWJ_DevMN:
                String MN = (String) target;
                if (StringUtils.isEmpty(MN)||MN.trim().length()!=12) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_mn));
                } else {
                    sendLHBleData("Write DevMN=" + MN.trim(), Constant.Param_Write_SWJ_DEVMN);
                }
                break;
            case Constant.Dialog_SWJ_DevIP:
                String strIP = (String) target;
                if (StringUtils.isIPAddress(strIP)){
                    String[]ips=strIP.split("\\.");
                    String devIP=StringUtils.formatString(Integer.parseInt(ips[0]),3)+StringUtils.formatString(Integer.parseInt(ips[1]),3)+StringUtils.formatString(Integer.parseInt(ips[2]),3)+StringUtils.formatString(Integer.parseInt(ips[3]),3);
                    sendLHBleData("Write DevIP=" +devIP, Constant.Param_Write_SWJ_DEVIP);
                }else {
                    showMsg(2,HJBleApplication.getAppContext().getString(R.string.input_right_ip));
                }

                break;
            case Constant.Dialog_SWJ_NetPort:
                String strPort = (String) target;
                if (StringUtils.isInt(strPort)){

                    String port=StringUtils.formatString(Integer.parseInt(strPort),5);
                    sendLHBleData("Write NetPort=" +port, Constant.Param_Write_SWJ_NetPort);
                }else {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_port));
                }

                break;
            case Constant.Dialog_SWJ_HuangHai:
                String strHuangHai = (String) target;
                if (StringUtils.isDouble(strHuangHai)){
                    if (Double.parseDouble(strHuangHai)>99999.999){
                        showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_huanhai));
                    }else {

                        String huanghai=StringUtils.formatString((int) (DoubleUtils.round(Double.parseDouble(strHuangHai),3)*1000),8);
                        sendLHBleData("Write HuangHai=" +huanghai, Constant.Param_Write_SWJ_HuangHai);
                    }


                }else {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_huanhai));
                }

                break;

            case Constant.Dialog_SWJ_SampleInterval:
                String stSampleInterval = (String) target;
                if (StringUtils.isInt(stSampleInterval)){

                    String port=StringUtils.formatString(Integer.parseInt(stSampleInterval),4);
                    sendLHBleData("Write SapInterval=" +port, Constant.Param_Write_SWJ_SampleInterval);
                }else {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_sample_interval));
                }

                break;
            case Constant.Dialog_SWJ_UpdateInterval:
                String strUpdateInterval = (String) target;
                if (StringUtils.isInt(strUpdateInterval)){

                    String updateInterval=StringUtils.formatString(Integer.parseInt(strUpdateInterval),4);
                    sendLHBleData("Write UpInterval=" +updateInterval, Constant.Param_Write_SWJ_UpdateInterval);
                }else {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_update_interval));
                }

                break;
            case Constant.Dialog_SWJ_DevNum:
                String num = (String) target;
                if (StringUtils.isEmpty(num)||num.trim().length()!=10) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_devnum));
                } else {
                    sendLHBleData("Write DevNum=" + num.trim(), Constant.Param_Write_SWJ_DEVNum);
                }
                break;
            case Constant.Dialog_SWJ_Dev_InstallHigh:
                String strIH = (String) target;
                if (StringUtils.isEmpty(strIH)||!StringUtils.isDouble(strIH.trim())||Double.parseDouble(strIH.trim())>40||Double.parseDouble(strIH.trim())<0) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_install_high));
                } else {
                    sendLHBleData("Write Installhigh=" + StringUtils.formatString((int) DoubleUtils.mul(Double.parseDouble(strIH.trim()),1000d),5), Constant.Param_Write_SWJ_InstallHigh);
                }
                break;
            case Constant.Dialog_SWJ_Dev_Angle:
                String strAngle = (String) target;
                if (StringUtils.isEmpty(strAngle)||!StringUtils.isDouble(strAngle)||Double.parseDouble(strAngle)>90||Double.parseDouble(strAngle)<0) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_angle));
                } else {
                    double angleValue=Double.parseDouble(strAngle);
                    int angle10= (int) DoubleUtils.mul(angleValue,10d);
                    sendLHBleData("Write Angle=" + StringUtils.formatString(angle10,3), Constant.Param_Write_SWJ_Angle);
                }
                break;
            case Constant.Dialog_SWJ_Range:
                String strRange = (String) target;
                if (StringUtils.isEmpty(strRange)||!StringUtils.isInt(strRange)||Integer.parseInt(strRange)>40||Integer.parseInt(strRange)<1) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_range));
                } else {
                    sendLHBleData("Write Range=" + StringUtils.formatString(Integer.parseInt(strRange),2), Constant.Param_Write_SWJ_RANGE);
                }
                break;
            case Constant.Dialog_SWJ_RangeCurve:

                String strCurveABCSW = (String) target;

                int curveNo=Integer.parseInt(strCurveABCSW.substring(0,1));
                strCurveABCSW=strCurveABCSW.substring(2);
                String[] curveInfos=new String[3];
                curveInfos[0]=(curveSWYZ>0?(StringUtils.formatString(((int)(DoubleUtils.round(curveA,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curveB,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curveC,2)*100)),5)
                        +"#"+StringUtils.formatString(curveSWYZ,5)):"00000#00000#00000#00000");
                curveInfos[1]=(curve1SWYZ>0?(StringUtils.formatString(((int)(DoubleUtils.round(curve1A,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curve1B,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curve1C,2)*100)),5)
                        +"#"+StringUtils.formatString(curve1SWYZ,5)):"00000#00000#00000#00000");
                curveInfos[2]=(curve2SWYZ>0?(StringUtils.formatString(((int)(DoubleUtils.round(curve2A,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curve2B,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curve2C,2)*100)),5)
                        +"#"+StringUtils.formatString(curve2SWYZ,5)):"00000#00000#00000#00000");
        /*        curveInfos[3]=(curve3SWYZ>0?(StringUtils.formatString(((int)(DoubleUtils.round(curve3A,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curve3B,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curve3C,2)*100)),5)
                        +"#"+StringUtils.formatString(curve3SWYZ,5)):"00000#00000#00000#00000");
                curveInfos[4]=(curve3SWYZ>0?(StringUtils.formatString(((int)(DoubleUtils.round(curve3A,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curve3B,2)*100)),5)
                        +"#"+StringUtils.formatString(((int)(DoubleUtils.round(curve3C,2)*100)),5)
                        +"#"+StringUtils.formatString(curve3SWYZ,5)):"00000#00000#00000#00000");*/
                if (curveNo>=0&&curveNo<=3){
                    curveInfos[curveNo]=strCurveABCSW;
                    StringBuffer sbCurveInfos=new StringBuffer();
                    for (int i=0;i<curveInfos.length;i++){
                        sbCurveInfos.append(curveInfos[i]+"#");
                    }

                    sendLHBleData("Write Curve=" +sbCurveInfos.substring(0,sbCurveInfos.length()-1), Constant.Param_Write_SWJ_RangeCurve);
                }else {
                    showMsg(2,"曲线序号异常，"+curveNo);
                }

                break;
            case Constant.Dialog_SWJ_Calibration:
                String strCal = (String) target;
                if (StringUtils.isEmpty(strCal)||!StringUtils.isInt(strCal)||Integer.parseInt(strCal)>127||Integer.parseInt(strCal)<-127) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.swj_set_calibration_tips));
                } else {
                    int cal=Integer.parseInt(strCal);
                    if (cal<0)cal=256+cal;
                    sendLHBleData("Write Calibration=" + StringUtils.formatString(cal,3), Constant.Param_Write_SWJ_Calibration);
                }
                break;
            case Constant.Dialog_SWJ_BlindSet:
                String strBlind = (String) target;
                if (StringUtils.isEmpty(strBlind)||!StringUtils.isInt(strBlind)||Integer.parseInt(strBlind)>255||Integer.parseInt(strBlind)<0) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_blind));
                } else {
                    sendLHBleData("Write BlindSet=" + StringUtils.formatString(Integer.parseInt(strBlind),3), Constant.Param_Write_SWJ_BlindSet);
                }
                break;
            case Constant.Dialog_SWJ_ResetNet:
                sendLHBleData("Set NetReset", Constant.Param_Write_SWJ_ResetNet);
                break;
            case Constant.Dialog_SWJ_ResetFlow:
                sendLHBleData("Set ZeroInit", Constant.Param_Write_SWJ_ResetFlow);
                break;
            case Constant.Dialog_SWJ_ResetRx:
                sendLHBleData("Set LdReset", Constant.Param_Write_SWJ_ResetRx);
                break;
            case Constant.Dailog_BLE_MACAddress:
                String strTarget = (String) target;

                sendLHBleData("Write BleAddr=" + strTarget, Constant.Param_Write_DEV_Mac_Address);
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
                        llUIMode.setVisibility(View.VISIBLE);
                        llOrderMode.setVisibility(View.GONE);
                        rightTitleTxt.setText(HJBleApplication.getAppContext().getString(R.string.ui_mode));
                        refreshDetails();
                        return true;
                    case R.id.order_mode:
                        llUIMode.setVisibility(View.VISIBLE);
                        llOrderMode.setVisibility(View.VISIBLE);
                        rightTitleTxt.setText(HJBleApplication.getAppContext().getString(R.string.order_mode));
                        return true;
                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }
    private void showSelectUploadTypeMenu() {
        PopupMenu popupMenu = new PopupMenu(this, tvUploadType);
        popupMenu.inflate(R.menu.menu_select_upload_type);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.type_212:
                        sendLHBleData("Write Protocol=0",Constant.Param_Write_SWJ_UploadType);
                        return true;
                    case R.id.type_mgtt:
                        sendLHBleData("Write Protocol=1",Constant.Param_Write_SWJ_UploadType);
                        return true;
                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void showSelectRTUTypeMenu() {
        PopupMenu popupMenu = new PopupMenu(this, tvRTUType);
        popupMenu.inflate(R.menu.menu_select_rtu_type);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.type_4g:
                        sendLHBleData("Write RTUType=0",Constant.Param_Write_SWJ_RTUType);
                        return true;
                    case R.id.type_nb:
                        sendLHBleData("Write RTUType=1",Constant.Param_Write_SWJ_RTUType);
                        return true;
                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }


    private void showSelectModbusModeMenu() {
        PopupMenu popupMenu = new PopupMenu(this, tvModbusMode);
        popupMenu.inflate(R.menu.menu_select_modbus_mode);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.mode_rtu:
                        sendLHBleData("Write EnModBus=0",Constant.Param_Write_SWJ_ModbusMode);
                        return true;
                    case R.id.mode_485:
                        sendLHBleData("Write EnModBus=1",Constant.Param_Write_SWJ_ModbusMode);
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
    private void refreshLdData() {
        RxTimerUtil.cancel();
        RxTimerUtil.timer(2000L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read LdData", Constant.Param_SWJ_LdData);
            }
        });

    }

    //发送数据
    public void sendLHBleData(String sendString, int sendType) {
        if (isMeaSure!=0&&sendType!=Constant.Param_SWJ_Measure){
            RxTimerUtil.cancel();
            isMeaSure=0;
            tvTimeReads.setText(HJBleApplication.getAppContext().getString(R.string.circle_read));
        }
        byte[] tmpBytes;
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.empty_send_msg));
            return;
        }

        tmpBytes = ProtocolUtil.getBlueToothMsgForSend(HJBleApplication.shareInstance().getDesID(), sendString);

        if (tmpBytes == null) {
            return;
        }else  if (mble == null || !mble.isConnect()) {

            showMsg(2, HJBleApplication.getAppContext().getString(R.string.ble_close_connect_again));
            finish();
            return;
        }
        if (LiherSWJ1Activity.this.isFinishing()){
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
                    addLogText(HJBleApplication.getAppContext().getString(R.string.send_fail)+"！", 0,Constant.Return_Ble_SendFail);
                    currentSendType = 0;
                }else if (!mble.waitReturn(mReceiveCharact,15*1000)){
                    addLogText(getResources().getString(R.string.ble_rtn_timeout),1, Constant.Return_BLE_False);
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
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close), Color.BLACK, 0);
              //  finish();
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
                if (str!=null) {
                    switch (currentSendType) {
                        case Constant.PARAM_LIHER_ORDER:
                            currentReceiveType = Constant.RETURN_LIHER_OREDER;
                            break;
                        case Constant.Param_Wirte_SysTime:
                            currentReceiveType = Constant.Return_Wirte_SysTime;
                            break;
                        case Constant.Param_Read_Detail:
                            currentReceiveType = Constant.Return_Read_Detail;
                            break;
                        case Constant.Param_Write_SWJ_DEVMN:
                            currentReceiveType = Constant.Return_Write_SWJ_DEVMN;
                            break;
                        case Constant.Param_Write_SWJ_InstallHigh:
                            currentReceiveType = Constant.Return_Write_SWJ_InstallHigh;
                            break;
                        case Constant.Param_Write_SWJ_Angle:
                            currentReceiveType = Constant.Return_Write_SWJ_Angle;
                            break;
                        case Constant.Param_Write_SWJ_UploadType:
                            currentReceiveType = Constant.Return_Write_SWJ_UploadType;
                            break;
                        case Constant.Param_Write_SWJ_RTUType:
                            currentReceiveType = Constant.Return_Write_SWJ_RTUType;
                            break;
                        case Constant.Param_Write_SWJ_ModbusMode:
                            currentReceiveType = Constant.Return_Write_SWJ_ModbusMode;
                            break;
                        case Constant.Param_Write_DEV_Mac_Address:
                            currentReceiveType = Constant.Return_Write_DEV_Mac_Address;
                            break;
                        case Constant.Param_Write_SWJ_DEVNum:
                            currentReceiveType = Constant.Return_Write_SWJ_DEVNum;
                            break;
                        case Constant.Param_Write_SWJ_DEVIP:
                            currentReceiveType = Constant.Return_Write_SWJ_DEVIP;
                            break;
                        case Constant.Param_Write_SWJ_RANGE:
                            currentReceiveType = Constant.Return_Write_SWJ_RANGE;
                            break;
                        case Constant.Param_Write_SWJ_Calibration:
                            currentReceiveType = Constant.Return_Write_SWJ_Calibration;
                            break;
                        case Constant.Param_Write_SWJ_BlindSet:
                            currentReceiveType = Constant.Return_Write_SWJ_BlindSet;
                            break;
                        case Constant.Param_Write_SWJ_ParamUp:
                            currentReceiveType = Constant.Return_Write_SWJ_ParamUp;
                            break;
                        case Constant.Param_Write_SWJ_ResetNet:
                            currentReceiveType = Constant.Return_Write_SWJ_ResetNet;
                            break;
                        case Constant.Param_Write_SWJ_ResetFlow:
                            currentReceiveType = Constant.Return_Write_SWJ_ResetFlow;
                            break;
                        case Constant.Param_Write_SWJ_ResetRx:
                            currentReceiveType = Constant.Return_Write_SWJ_ResetRx;
                            break;
                        case Constant.Param_Write_SWJ_NetPort:
                            currentReceiveType = Constant.Return_Write_SWJ_NetPort;
                            break;
                        case Constant.Param_Write_SWJ_HuangHai:
                            currentReceiveType = Constant.Return_Write_SWJ_HuangHai;
                            break;
                        case Constant.Param_Write_SWJ_SampleInterval:
                            currentReceiveType = Constant.Return_Write_SWJ_SampleInterval;
                            break;
                        case Constant.Param_Write_SWJ_UpdateInterval:
                            currentReceiveType = Constant.Return_Write_SWJ_UpdateInterval;
                            break;
                        case Constant.Param_Write_SWJ_RangeCurve:
                            currentReceiveType = Constant.Return_Write_SWJ_RangeCurve;
                            break;
                        case Constant.Param_SWJ_TimeData:
                            currentReceiveType = Constant.Return_SWJ_TimeData;
                            break;
                        case Constant.Param_SWJ_Measure:
                            currentReceiveType = Constant.Return_SWJ_Measure;
                            break;
                        case Constant.Param_SWJ_LdData:
                            currentReceiveType = Constant.Return_SWJ_LdData;
                            break;
                        case Constant.Param_SWJ_LotLev:
                            currentReceiveType = Constant.Return_SWJ_LotLev;
                            break;
                        case Constant.Param_Write_SWJ_Start:
                            currentReceiveType = Constant.Return_Write_SWJ_Start;
                            break;
                        case Constant.Param_Rx_Lex:
                            currentReceiveType=Constant.Return_Rx_Lex;
                            break;
                        default:
                            currentReceiveType = 0;
                            return;
                    }
                    addLogText(str, 1, currentReceiveType);

                }

            } else if (characteristic.equals(mLogCharact)) {
                String logStr = ConvertData.bytesToHexString(recvData, false);
                addLogText(HJBleApplication.getAppContext().getString(R.string.note) + logStr, Color.rgb(139, 0, 255), logStr.length());
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
                        // 根据消息内容设置type参数：成功用0，失败用1，其他用2
                        int type = 2; // 默认类型
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
            if (arg2 == Constant.PARAM_LIHER_ORDER || llOrderMode.getVisibility() == View.VISIBLE) {
                SpannableStringBuilder builder = new SpannableStringBuilder(HJBleApplication.getAppContext().getString(R.string.send) + msgContext);
                //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            }else if (arg2==Constant.Return_Ble_SendFail){
                showMsg(1, msgContext);
                if (loadingDialog != null) loadingDialog.dismiss();
            }
            System.out.println("SendString:" + msgContext);
        } else if (arg1 == 1) {//接收到的消息
            String msgContext = (String) obj;
            SpannableStringBuilder builder = new SpannableStringBuilder(HJBleApplication.getAppContext().getString(R.string.receive) + msgContext);
            //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(139, 0, 255));
            builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            switch (arg2) {
                case Constant.RETURN_LIHER_OREDER:

                    TextView tView = new TextView(context);
                    tView.setText(builder);
                    llOrders.addView(tView);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                    break;
                case Constant.Return_SWJ_TimeData:

                    TextView tViewTimeData = new TextView(context);
                    tViewTimeData.setText(builder);
                    llOrders.addView(tViewTimeData);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                    String[] timeData = msgContext.split("#", -1);
                    if (timeData!=null&&timeData.length>=6){
                        if (timeData[0]!=null&&StringUtils.isInt(timeData[0]))waterLev=Integer.parseInt(timeData[0]);
                        if (timeData[1]!=null&&StringUtils.isInt((timeData[1])))devDistance=Integer.parseInt(timeData[1]);
                        if (timeData[2]!=null&&StringUtils.isInt(timeData[2]))devRxLev=Integer.parseInt(timeData[2]);
                        if (timeData[3]!=null&&StringUtils.isInt(timeData[3]))currentFlow=Integer.parseInt(timeData[3]);
                        if (timeData[4]!=null&&StringUtils.isInt(timeData[4]))intervaFlow=Integer.parseInt(timeData[4]);
                        if (timeData[5]!=null&&StringUtils.isInt(timeData[5]))totalFlow=Long.parseLong(timeData[5]);
                        if (timeData.length>=7&&timeData[6]!=null&&StringUtils.isInt(timeData[6]))waterAbsLev=Integer.parseInt(timeData[6]);
                        if (timeData.length>=8&&timeData[7]!=null&&StringUtils.isInt(timeData[7]))angle=Integer.parseInt(timeData[7]);
                        tvSurplusVol.setText(DoubleUtils.div(waterLev,1000d,3)+"M");
                        tvAdsLevel.setText(DoubleUtils.div(waterAbsLev,1000d,3)+"M");
                        tvDevDistance.setText(DoubleUtils.div(devDistance,1000d,3)+"M");
                        tvRxLev.setText(StringUtils.getRxLex(devRxLev));
                        tvAngle.setText(DoubleUtils.div(angle,10d,1)+"°");
                        String msg=HJBleApplication.getAppContext().getString(R.string.current_flow)+DoubleUtils.div(currentFlow,100d,2)+"m³/s\n"+
                                HJBleApplication.getAppContext().getString(R.string.interval_time)+DoubleUtils.div(intervaFlow,100d,2)+"m³\n"+
                                HJBleApplication.getAppContext().getString(R.string.total_flow)+DoubleUtils.div(totalFlow,100d,2)+"m³\n"+
                                HJBleApplication.getAppContext().getString(R.string.water_level)+DoubleUtils.div(waterLev,1000d,3)+"M "+HJBleApplication.getAppContext().getString(R.string.water_abs_level)+DoubleUtils.div(waterAbsLev,1000d,3)+"M\n"+HJBleApplication.getAppContext().getString(R.string.height)+DoubleUtils.div(devDistance,1000d,3)+"M "+HJBleApplication.getAppContext().getString(R.string.rx_level)+devRxLev+HJBleApplication.getAppContext().getString(R.string.angle)+DoubleUtils.div(angle,10d,1);
                        new HitiStringDailog(LiherSWJ1Activity.this,Constant.HitiStrTargetDialog_SWJ_TimeData,null,HJBleApplication.getAppContext().getString(R.string.params_info),msg, LiherSWJ1Activity.this);

                    }else {
                        showMsg(2, getString(R.string.swj_version_too_low));
                    }
                      break;
                case Constant.Return_SWJ_Measure:

                    TextView tViewMeasure = new TextView(context);
                    tViewMeasure.setText(builder);
                    llOrders.addView(tViewMeasure);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                    String[] measureData = msgContext.split("#", -1);
                    if (measureData!=null&&measureData.length>=7){
                        if (measureData[0]!=null&&StringUtils.isInt(measureData[0]))waterLev=Integer.parseInt(measureData[0]);
                        if (measureData[1]!=null&&StringUtils.isInt((measureData[1])))devDistance=Integer.parseInt(measureData[1]);
                        if (measureData[2]!=null&&StringUtils.isInt(measureData[2]))devRxLev=Integer.parseInt(measureData[2]);
                        if (measureData[3]!=null&&StringUtils.isInt(measureData[3]))currentFlow=Integer.parseInt(measureData[3]);
                        if (measureData[4]!=null&&StringUtils.isInt(measureData[4]))intervaFlow=Integer.parseInt(measureData[4]);
                        if (measureData[5]!=null&&StringUtils.isInt(measureData[5]))totalFlow=Long.parseLong(measureData[5]);
                        if (measureData.length>=7&&measureData[6]!=null&&StringUtils.isInt(measureData[6]))waterAbsLev=Integer.parseInt(measureData[6]);
                        if (measureData.length>=8&&measureData[7]!=null&&StringUtils.isInt(measureData[7]))angle=Integer.parseInt(measureData[7]);
                        tvSurplusVol.setText(DoubleUtils.div(waterLev,1000d,3)+"M");
                        tvAdsLevel.setText(DoubleUtils.div(waterAbsLev,1000d,3)+"M");
                        tvDevDistance.setText(DoubleUtils.div(devDistance,1000d,3)+"M");
                        tvRxLev.setText(StringUtils.getRxLex(devRxLev));
                        tvAngle.setText(DoubleUtils.div(angle,10d,1)+"°");
                        String measureMsg=HJBleApplication.getAppContext().getString(R.string.water_level)+DoubleUtils.div(waterLev,1000d,3)+"M "+HJBleApplication.getAppContext().getString(R.string.water_abs_level)+DoubleUtils.div(waterAbsLev,1000d,3)+"M "+HJBleApplication.getAppContext().getString(R.string.height)+DoubleUtils.div(devDistance,1000d,3)+"M\n"
                                +HJBleApplication.getAppContext().getString(R.string.current_flow)+DoubleUtils.div(currentFlow,100d,2)+"m³/s "+ HJBleApplication.getAppContext().getString(R.string.interval_time)+DoubleUtils.div(intervaFlow,100d,2)+"m³ "+HJBleApplication.getAppContext().getString(R.string.total_flow)+DoubleUtils.div(totalFlow,100d,2)+"m³";
                        showMsg(2, measureMsg);

                    }else {
                        showMsg(2, getString(R.string.swj_version_too_low));
                    }
                       break;
                case Constant.Return_Wirte_SysTime:
                    if (msgContext != null && msgContext.contains("Write SysTime=")) {
                        sysTime=DateUtil.format2miao("20" + msgContext.substring(14).trim(), "yyyyMMddHHmmss");

                        tvSysTime.setText(sysTime>0?DateUtil.long2time4(sysTime):"");
                    } else {
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_systime_fail)+",\n" + msgContext);

                    }
                    break;
                case Constant.Return_Read_Detail:
                    //220215#230815150220#            #2307310001#307000001230#7310001__030#192168001012#2895#0300#05#   #   #96#66#65535#00000#     #     # 0   #0   #    #                    #     
                   try {

                       String[] details = msgContext.split("#", -1);

                       version = details[0];
                       if (StringUtils.isInt(version)&&Integer.parseInt(version)<260423){
                           showMsg(1,"请尽快将水位计设备蓝牙程序升级到260423及其以上版本，否则安装高度无法设置和正常显示。");
                       }
                       if (details[1]!=null&&details[1].length()==12&&details[1].startsWith("2")){
                           sysTime= DateUtil.format2miao("20" + details[1], "yyyyMMddHHmmss");
                       }
                       if (details.length>=23){//2023年8月份以后的雷达水位计展示全部信息 废弃
                           devNum=details[3];
                           devMN=details[5];
                           devIP=details[6];
                           if (details[7]!=null&&StringUtils.isInt(details[7]))devEv=Integer.parseInt(details[7]);
                           if (details[8]!=null&&StringUtils.isInt(details[8]))devInstallHigh=Integer.parseInt(details[8]);
                           if (details[9]!=null&&StringUtils.isInt(details[9]))devRange=Integer.parseInt(details[9]);
                           if (details[10]!=null&&StringUtils.isInt(details[10]))devCalibration=Integer.parseInt(details[10])<128?Integer.parseInt(details[10]):(Integer.parseInt(details[10])-256);
                           if (details[11]!=null&&StringUtils.isInt(details[11]))devBlindSet=Integer.parseInt(details[11]);
                           if (details[12]!=null&&StringUtils.isInt(details[12]))devRxLev=Integer.parseInt(details[12]);
                           if (details[13]!=null&&StringUtils.isInt(details[13]))devLotLev=Integer.parseInt(details[13]);
                           if (details[14]!=null&&StringUtils.isInt(details[14]))waterLev=Integer.parseInt(details[14]);
                           if (details[15]!=null&&StringUtils.isInt(details[15]))devDistance=Integer.parseInt(details[15]);
                           if (details.length>=25){
                               if (details[16]!=null&&StringUtils.isInt(details[16]))huangHai=Integer.parseInt(details[16]);
                               if (details[17]!=null&&StringUtils.isInt(details[17]))curveA=DoubleUtils.div(Integer.parseInt(details[17]),100d,2);
                               if (details[18]!=null&&StringUtils.isInt(details[18]))curveB=DoubleUtils.div(Integer.parseInt(details[18]),100d,2);
                               if (details[19]!=null&&StringUtils.isInt(details[19]))curveC=DoubleUtils.div(Integer.parseInt(details[19]),100d,2);
                               if (details[20]!=null&&StringUtils.isInt(details[20]))moduleSTA=Integer.parseInt(details[20]);
                               if (details[21]!=null&&StringUtils.isInt(details[21]))upInterval=Integer.parseInt(details[21]);
                               if (details[22]!=null&&StringUtils.isInt(details[22]))sapInterval=Integer.parseInt(details[22]);
                               simCard=details[23];
                               if (details[24]!=null&&StringUtils.isInt(details[24]))netPort=Integer.parseInt(details[24]);
                           }else if (details.length==23){
                               if (details[16]!=null&&StringUtils.isInt(details[16]))huangHai=Integer.parseInt(details[16]);
                               if (details[18]!=null&&StringUtils.isInt(details[18]))moduleSTA=Integer.parseInt(details[18]);
                               if (details[19]!=null&&StringUtils.isInt(details[19]))upInterval=Integer.parseInt(details[19]);
                               if (details[20]!=null&&StringUtils.isInt(details[20]))sapInterval=Integer.parseInt(details[20]);
                               simCard=details[21];
                               if (details[22]!=null&&StringUtils.isInt(details[22]))netPort=Integer.parseInt(details[22]);
                           }
                       }else {
                           devNum=details[2];
                           devMN=details[4];
                           devIP=details[5];
                           if (details[6]!=null&&StringUtils.isInt(details[6]))netPort=Integer.parseInt(details[6]);
                           if (details[7]!=null&&StringUtils.isInt(details[7]))devEv=Integer.parseInt(details[7]);
                           if (details[8]!=null&&StringUtils.isLong(details[8])){
                               moduleSTA16=Long.parseLong(details[8]);
                           }
                           if (details[9]!=null&&StringUtils.isInt(details[9]))upInterval=Integer.parseInt(details[9]);
                           if (details[10]!=null&&StringUtils.isInt(details[10]))sapInterval=Integer.parseInt(details[10]);
                           simCard=details[11];
                           if (details[12]!=null&&StringUtils.isInt(details[12]))saveNum=Integer.parseInt(details[12]);
                           if (details.length>=15){
                               if (details[13]!=null&&StringUtils.isInt(details[13]))protocol=Integer.parseInt(details[13]);
                               if (details[14]!=null&&StringUtils.isInt(details[14]))rtuType=Integer.parseInt(details[14]);
                           }
                           if (details.length>=16){
                               if (details[15]!=null&&StringUtils.isInt(details[15]))modbusMode=Integer.parseInt(details[15]);
                           }
                           if (details.length>=17){//当前上传步骤
                               if (details[16]!=null&&StringUtils.isInt(details[16]))lotStep=Integer.parseInt(details[16]);
                           }
                       }




                       if (!StringUtils.isEmpty(devNum)){
                           tvDevNum.setText(devNum.trim());
                           setTitle(devNum.trim());
                       }
                       if (!StringUtils.isEmpty(devMN))tvDevMN.setText(devMN.trim());
                       if (!StringUtils.isEmpty(devIP)&&devIP.length()==12&&StringUtils.isLong(devIP))tvDevIp.setText(Integer.parseInt(devIP.substring(0,3))+"."+Integer.parseInt(devIP.substring(3,6))+"."+Integer.parseInt(devIP.substring(6,9))+"."+Integer.parseInt(devIP.substring(9,12)));

                       tvUpdateInterval.setText(upInterval+HJBleApplication.getAppContext().getString(R.string.minute));
                       tvSampleInterval.setText(sapInterval+HJBleApplication.getAppContext().getString(R.string.minute));
                       tvSimCard.setText(simCard);
                       tvNetPort.setText(netPort>0?(netPort+""):"");
                       tvDevVersion.setText(version);
                       tvSysTime.setText(sysTime>0?DateUtil.long2time4(sysTime):"");

                       if (devEv>0){
                            if (devEv<=9800){
                                tvDevEV.setText("0%\t"+DoubleUtils.div(devEv,1000d,2)+"V");
                            }else if (devEv>=10800){
                                tvDevEV.setText("100%\t"+DoubleUtils.div(devEv,1000d,2)+"V");
                            }else {
                                tvDevEV.setText( ((devEv-9800)/10)+"%\t"+DoubleUtils.div(devEv,1000d,2)+"V");
                            }
                       }
                       if (moduleSTA16==-1){//2023年8月份雷达水位计展示全部信息
                           tvInstallHigh.setText(devInstallHigh+"cm");
                           if (devRange>0)tvRange.setText(devRange+"M");
                           tvCalibration.setText(devCalibration+"mm");
                           tvBlindSet.setText(devBlindSet+"cm");
                           tvRxLev.setText(StringUtils.getRxLex(devRxLev));
                           tvLotLev.setText(StringUtils.getLotLex(devLotLev));
                           tvSurplusVol.setText(DoubleUtils.div(waterLev,1000d,3)+"M");
                           tvDevDistance.setText(DoubleUtils.div(devDistance,1000d,3)+"M");
                           tvWorkState.setText(StringUtils.getSWJWorkStatue(moduleSTA));
                           if (moduleSTA<3){
                               tvWorkState.setTextColor(getResources().getColor(R.color.color_green_24ccaa));
                           }else {
                               tvWorkState.setTextColor(getResources().getColor(R.color.color_red));
                           }
                           tvHuangHai.setText(DoubleUtils.div(huangHai,1000d,3)+"M");
                           tvRangeCurve.setText((curveA>=0?DoubleUtils.round(curveA,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curveB>=0?DoubleUtils.round(curveB,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curveC>=0?DoubleUtils.round(curveC,2):HJBleApplication.getAppContext().getString(R.string.no_data)));
                           tvWorkStart.setVisibility(View.GONE);
                           if (Math.abs(sysTime-System.currentTimeMillis())>1*60*1000L){
                               RxTimerUtil.cancel();
                               RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                                   @Override
                                   public void doNext(long number) {
                                       new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dailog_BLE_SYSDateTime, HJBleApplication.getAppContext().getString(R.string.set_systime), HJBleApplication.getAppContext().getString(R.string.current_time)+"：", DateUtils.long2time(System.currentTimeMillis()), LiherSWJ1Activity.this);
                                   }
                               });
                           }
                       }else {//2023年10月份后雷达水位计展示部分信息
                           if (((byte) ((moduleSTA16 & 0xff) % 2))==1){
                               tvWorkState.setTextColor(getResources().getColor(R.color.color_red));
                           }else {
                               tvWorkState.setTextColor(getResources().getColor(R.color.color_green_24ccaa));
                           }
                           if (((byte) ((moduleSTA16>>8 & 0xff) % 2))==1){
                               tvWorkStart.setVisibility(View.GONE);
                           }else {
                               tvWorkStart.setVisibility(View.VISIBLE);
                           }
                           tvWorkState.setText(StringUtils.getSWJWorkStatue16(moduleSTA16));
                           tvSaveNum.setText(saveNum<=0?"无":(saveNum+"条"));
                           tvUploadType.setText(StringUtils.getSWJUploadType(protocol));
                           tvRTUType.setText(StringUtils.getSWJRTUType(rtuType));
                           tvModbusMode.setText(StringUtils.getSWJModbusMode(modbusMode));
                           if (((byte) ((moduleSTA16>>2 & 0xff) % 2))==1){
                             showMsg(2, "设备正在上传数据，进行到‘"+StringUtils.getLotStep(lotStep)+"'");
                             playAlarmSoundAndVibrate();
                             RxTimerUtil.cancel();
                             RxTimerUtil.timer(3000L, new RxTimerUtil.IRxNext() {
                                 @Override
                                 public void doNext(long number) {
                                     sendLHBleData("Read Detail", Constant.Param_Read_Detail);
                                 }
                             });
                             return;
                           }

                           sendLHBleData("Read LotLev",Constant.Param_SWJ_LotLev);

                           if (Math.abs(sysTime-System.currentTimeMillis())>1*60*1000L){
                               RxTimerUtil.cancel();
                               RxTimerUtil.timer(3000L, new RxTimerUtil.IRxNext() {
                                   @Override
                                   public void doNext(long number) {
                                       new HitiInputStrDailog(LiherSWJ1Activity.this, Constant.Dailog_BLE_SYSDateTime, HJBleApplication.getAppContext().getString(R.string.set_systime), HJBleApplication.getAppContext().getString(R.string.current_time)+"：", DateUtils.long2time(System.currentTimeMillis()), LiherSWJ1Activity.this);
                                   }
                               });
                           }
                       }

                   }catch (Exception e){
                       e.printStackTrace();
                   }

                    break;
                case Constant.Return_SWJ_LdData:
                    String[] details = msgContext.split("#", -1);
                    if (details.length<=8){
                        showMsg(2, HJBleApplication.getAppContext().getString(R.string.unknown_error)+"\n"+msgContext);
                        return;
                    }
                     if (details.length==9){
                         if (details[0]!=null&&StringUtils.isInt(details[0]))devInstallHigh=Integer.parseInt(details[0]);
                         if (details[1]!=null&&StringUtils.isInt(details[1]))devRange=Integer.parseInt(details[1]);
                         if (details[2]!=null&&StringUtils.isInt(details[2]))devCalibration=Integer.parseInt(details[2])<128?Integer.parseInt(details[2]):(Integer.parseInt(details[2])-256);
                         if (details[3]!=null&&StringUtils.isInt(details[3]))devBlindSet=Integer.parseInt(details[3]);
                         if (details[4]!=null&&StringUtils.isInt(details[4]))huangHai=Integer.parseInt(details[4]);
                         if (details[5]!=null&&StringUtils.isInt(details[5]))curveA=DoubleUtils.div(Integer.parseInt(details[5]),100d,2);
                         if (details[6]!=null&&StringUtils.isInt(details[6]))curveB=DoubleUtils.div(Integer.parseInt(details[6]),100d,2);
                         if (details[7]!=null&&StringUtils.isInt(details[7]))curveC=DoubleUtils.div(Integer.parseInt(details[7]),100d,2);
                         if (details[8]!=null&&StringUtils.isInt(details[8]))angle=Integer.parseInt(details[8]);
                    }else if (details.length>=26){
                         if (details[0]!=null&&StringUtils.isInt(details[0]))devInstallHigh=Integer.parseInt(details[0]);
                         if (details[1]!=null&&StringUtils.isInt(details[1]))devRange=Integer.parseInt(details[1]);
                         if (details[2]!=null&&StringUtils.isInt(details[2]))devCalibration=Integer.parseInt(details[2])<128?Integer.parseInt(details[2]):(Integer.parseInt(details[2])-256);
                         if (details[3]!=null&&StringUtils.isInt(details[3]))devBlindSet=Integer.parseInt(details[3]);
                         if (details[4]!=null&&StringUtils.isInt(details[4]))huangHai=Integer.parseInt(details[4]);
                         if (details[5]!=null&&StringUtils.isInt(details[5]))curveA=DoubleUtils.div(Integer.parseInt(details[5]),100d,2);
                         if (details[6]!=null&&StringUtils.isInt(details[6]))curveB=DoubleUtils.div(Integer.parseInt(details[6]),100d,2);
                         if (details[7]!=null&&StringUtils.isInt(details[7]))curveC=DoubleUtils.div(Integer.parseInt(details[7]),100d,2);
                         if (details[8]!=null&&StringUtils.isInt(details[8]))curveSWYZ=Integer.parseInt(details[8]);
                         if (details[9]!=null&&StringUtils.isInt(details[9]))curve1A=DoubleUtils.div(Integer.parseInt(details[9]),100d,2);
                         if (details[10]!=null&&StringUtils.isInt(details[10]))curve1B=DoubleUtils.div(Integer.parseInt(details[10]),100d,2);
                         if (details[11]!=null&&StringUtils.isInt(details[11]))curve1C=DoubleUtils.div(Integer.parseInt(details[11]),100d,2);
                         if (details[12]!=null&&StringUtils.isInt(details[12]))curve1SWYZ=Integer.parseInt(details[12]);
                         if (details[13]!=null&&StringUtils.isInt(details[13]))curve2A=DoubleUtils.div(Integer.parseInt(details[13]),100d,2);
                         if (details[14]!=null&&StringUtils.isInt(details[14]))curve2B=DoubleUtils.div(Integer.parseInt(details[14]),100d,2);
                         if (details[15]!=null&&StringUtils.isInt(details[15]))curve2C=DoubleUtils.div(Integer.parseInt(details[15]),100d,2);
                         if (details[16]!=null&&StringUtils.isInt(details[16]))curve2SWYZ=Integer.parseInt(details[16]);
                         if (details[17]!=null&&StringUtils.isInt(details[17]))curve3A=DoubleUtils.div(Integer.parseInt(details[17]),100d,2);
                         if (details[18]!=null&&StringUtils.isInt(details[18]))curve3B=DoubleUtils.div(Integer.parseInt(details[18]),100d,2);
                         if (details[19]!=null&&StringUtils.isInt(details[19]))curve3C=DoubleUtils.div(Integer.parseInt(details[19]),100d,2);
                         if (details[20]!=null&&StringUtils.isInt(details[20]))curve3SWYZ=Integer.parseInt(details[20]);
                         if (details[21]!=null&&StringUtils.isInt(details[21]))curve4A=DoubleUtils.div(Integer.parseInt(details[21]),100d,2);
                         if (details[22]!=null&&StringUtils.isInt(details[22]))curve4B=DoubleUtils.div(Integer.parseInt(details[22]),100d,2);
                         if (details[23]!=null&&StringUtils.isInt(details[23]))curve4C=DoubleUtils.div(Integer.parseInt(details[23]),100d,2);
                         if (details[24]!=null&&StringUtils.isInt(details[24]))curve4SWYZ=Integer.parseInt(details[24]);
                         if (details[25]!=null&&StringUtils.isInt(details[25]))angle=Integer.parseInt(details[25]);

                     }else {
                         showMsg(2,"Read LdData返回异常，字段个数:"+details.length);
                     }
                    tvInstallHigh.setText(DoubleUtils.div(devInstallHigh,1000,3)+"M");
                    if (devRange>0)tvRange.setText(devRange+"M");
                    tvCalibration.setText(devCalibration+"mm");
                    tvBlindSet.setText(devBlindSet+"cm");
                    tvHuangHai.setText(DoubleUtils.div(huangHai,1000d,3)+"M");
                    tvRangeCurve.setText((curveA!=Constant.DefaultValue?DoubleUtils.round(curveA,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curveB!=Constant.DefaultValue?DoubleUtils.round(curveB,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curveC!=Constant.DefaultValue?DoubleUtils.round(curveC,2):HJBleApplication.getAppContext().getString(R.string.no_data))+(curveSWYZ!=Constant.DefaultValue?(" 水位阈值"+curveSWYZ+"mm"):""));
                    if (curve1SWYZ!=Constant.DefaultValue||curveSWYZ!=Constant.DefaultValue){
                        tvRangeCurve1.setVisibility(View.VISIBLE);
                        tvRangeCurve1.setText((curve1A!=Constant.DefaultValue?DoubleUtils.round(curve1A,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curve1B!=Constant.DefaultValue?DoubleUtils.round(curve1B,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curve1C!=Constant.DefaultValue?DoubleUtils.round(curve1C,2):HJBleApplication.getAppContext().getString(R.string.no_data))+(curve1SWYZ!=Constant.DefaultValue?(" 水位阈值"+curve1SWYZ+"mm"):""));

                    }else {
                        tvRangeCurve1.setVisibility(View.GONE);
                    }
                    if (curve2SWYZ!=Constant.DefaultValue||curve1SWYZ!=Constant.DefaultValue){
                        tvRangeCurve2.setVisibility(View.VISIBLE);
                        tvRangeCurve2.setText((curve2A!=Constant.DefaultValue?DoubleUtils.round(curve2A,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curve2B!=Constant.DefaultValue?DoubleUtils.round(curve2B,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curve2C!=Constant.DefaultValue?DoubleUtils.round(curve2C,2):HJBleApplication.getAppContext().getString(R.string.no_data))+(curve2SWYZ!=Constant.DefaultValue?(" 水位阈值"+curve2SWYZ+"mm"):""));

                    }else {
                        tvRangeCurve2.setVisibility(View.GONE);
                    }
                    if (curve3SWYZ!=Constant.DefaultValue||curve2SWYZ!=Constant.DefaultValue){
                        tvRangeCurve3.setVisibility(View.GONE);
                        tvRangeCurve3.setText((curve3A!=Constant.DefaultValue?DoubleUtils.round(curve3A,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curve3B!=Constant.DefaultValue?DoubleUtils.round(curve3B,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curve3C!=Constant.DefaultValue?DoubleUtils.round(curve3C,2):HJBleApplication.getAppContext().getString(R.string.no_data))+(curve3SWYZ!=Constant.DefaultValue?(" 水位阈值"+curve3SWYZ+"mm"):""));
                    }else {
                        tvRangeCurve3.setVisibility(View.GONE);
                    }
                    if (curve4SWYZ>0||curve3SWYZ>0){
                        tvRangeCurve4.setVisibility(View.GONE);
                        tvRangeCurve4.setText((curve3A!=Constant.DefaultValue?DoubleUtils.round(curve3A,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curve3B!=Constant.DefaultValue?DoubleUtils.round(curve3B,2):HJBleApplication.getAppContext().getString(R.string.no_data))+" : "+(curve3C!=Constant.DefaultValue?DoubleUtils.round(curve3C,2):HJBleApplication.getAppContext().getString(R.string.no_data))+(curve3SWYZ>0?(" 水位阈值"+curve3SWYZ+"mm"):""));
                    }else {
                        tvRangeCurve4.setVisibility(View.GONE);
                    }
                    tvAngle.setText(DoubleUtils.div(angle,10d,1)+"°");
                    if (devRxLev<0) sendLHBleData("Read TimeData", Constant.Param_SWJ_Measure);
                   break;
                case Constant.Return_SWJ_LotLev:
                    if (msgContext!=null&&StringUtils.isInt(msgContext.trim()))devLotLev=Integer.parseInt(msgContext.trim());
                    tvLotLev.setText(StringUtils.getLotLex(devLotLev));
                    if (devRange<=0){
                        sendLHBleData("Read LdData",Constant.Param_SWJ_LdData);
                    }
                    break;
                case Constant.Return_Rx_Lex:
                    if (msgContext!=null&&StringUtils.isInt(msgContext.trim())){
                        devRxLev=Integer.parseInt(msgContext.trim());
                        tvRxLev.setText(StringUtils.getRxLex(devRxLev));
                    }
                    break;
                case Constant.Return_Write_SWJ_DEVMN:
                    if (msgContext != null && msgContext.contains("Write DevMN="))
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.swj_set_mn_success));
                    refreshDetails();
                    break;
                case Constant.Return_Write_SWJ_InstallHigh:
                    if (msgContext != null && msgContext.contains("Write Installhigh="))
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.swj_set_install_high_success));
                    if (moduleSTA16==-1){
                        refreshDetails();
                    }else {
                        refreshLdData();
                    }
                    break;
                case Constant.Return_Write_SWJ_Angle:
                    if (msgContext == null || !msgContext.contains("Write Angle="))
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail)+","+msgContext);
                    if (moduleSTA16==-1){
                        refreshDetails();
                    }else {
                        refreshLdData();
                    }
                    break;
                case Constant.Return_Write_SWJ_RANGE:
                    if (msgContext != null && msgContext.contains("Write Range="))
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_range_success));
                    if (moduleSTA16==-1){
                        refreshDetails();
                    }else {
                        refreshLdData();
                    }
                    break;
                case Constant.Return_Write_SWJ_UploadType:
                    if (msgContext == null || !msgContext.contains("Write Protocol=")){
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail)+","+msgContext);
                    }else if ("Write Protocol=0".equals(msgContext)){
                        protocol=0;
                        tvUploadType.setText(StringUtils.getSWJUploadType(protocol));
                    }else if ("Write Protocol=1".equals(msgContext)){
                        protocol=1;
                        tvUploadType.setText(StringUtils.getSWJUploadType(protocol));
                    }

                    break;
                case Constant.Return_Write_SWJ_RTUType:
                    if (msgContext == null || !msgContext.contains("Write RTUType=")){
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail)+","+msgContext);
                    }else if ("Write RTUType=0".equals(msgContext)){
                        rtuType=0;
                        tvRTUType.setText(StringUtils.getSWJRTUType(rtuType));
                    }else if ("Write RTUType=1".equals(msgContext)){
                        rtuType=1;
                        tvRTUType.setText(StringUtils.getSWJRTUType(rtuType));
                    }

                    break;
                case Constant.Return_Write_SWJ_ModbusMode:
                    if (msgContext == null || !msgContext.contains("Write EnModBus=")){
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail)+","+msgContext);
                    }else if ("Write EnModBus=0".equals(msgContext)){
                        modbusMode=0;
                        tvModbusMode.setText(StringUtils.getSWJModbusMode(modbusMode));
                    }else if ("Write EnModBus=1".equals(msgContext)){
                        modbusMode=1;
                        tvModbusMode.setText(StringUtils.getSWJModbusMode(modbusMode));
                    }

                    break;
                case Constant.Return_Write_SWJ_RangeCurve:
                    if (msgContext != null && msgContext.contains("Write Curve="))
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_range_params_success));
                    if (moduleSTA16==-1){
                        refreshDetails();
                    }else {
                        refreshLdData();
                    }
                    break;
                case Constant.Return_Write_SWJ_Calibration:
                    if (msgContext != null && msgContext.contains("Write Calibration="))
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_init_value_success));
                    if (moduleSTA16==-1){
                        refreshDetails();
                    }else {
                        refreshLdData();
                    }
                    break;
                case Constant.Return_Write_SWJ_BlindSet:
                    if (msgContext != null && msgContext.contains("Write BlindSet="))
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.swj_set_blind_success));
                    if (moduleSTA16==-1){
                        refreshDetails();
                    }else {
                        refreshLdData();
                    }
                    break;
                case Constant.Return_Write_SWJ_ParamUp:
                    if (msgContext != null && "Set DataUp".equals(msgContext)){
                          showMsg(0, HJBleApplication.getAppContext().getString(R.string.param_update_order_sent));
                    }
                    RxTimerUtil.cancel();
                    RxTimerUtil.timer(15000L, new RxTimerUtil.IRxNext() {
                        @Override
                        public void doNext(long number) {
                            refreshDetails();
                        }
                    });


                    break;
                case Constant.Return_Write_SWJ_ResetNet:
                    if (msgContext != null && "Set NetReset".equals(msgContext)) showMsg(2, HJBleApplication.getAppContext().getString(R.string.init_net_success));
                    refreshDetails();
                    break;
                case Constant.Return_Write_SWJ_ResetFlow:
                    if (msgContext != null && "Set ZeroInit".equals(msgContext)) showMsg(2, HJBleApplication.getAppContext().getString(R.string.reset_flow_success));

                    break;
                case Constant.Return_Write_SWJ_ResetRx:
                    if (msgContext != null && "Set LdReset".equals(msgContext)) showMsg(2, HJBleApplication.getAppContext().getString(R.string.reset_rx_success));
                    if (moduleSTA16==-1){
                        refreshDetails();
                    }else {
                        devRange=0;
                        refreshDetails();
                    }
                    break;
                case Constant.Return_Write_SWJ_NetPort:
                    if (msgContext != null && msgContext.contains("Write NetPort="))
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_net_port_success));
                    refreshDetails();
                    break;
                case Constant.Return_Write_SWJ_HuangHai:
                    if (msgContext != null && msgContext.contains("Write HuangHai="))
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_huanghai_success));
                    if (moduleSTA16==-1){
                        refreshDetails();
                    }else {
                        refreshLdData();
                    }
                    break;
                case Constant.Return_Write_SWJ_SampleInterval:
                    if (msgContext != null && msgContext.contains("Write SapInterval=")){
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_sample_interval_time_success));
                        sapInterval=Integer.parseInt(msgContext.substring(18));
                        tvSampleInterval.setText(sapInterval+"分钟");
                    }else {
                        refreshDetails();
                    }


                    break;
                case Constant.Return_Write_SWJ_UpdateInterval:
                    if (msgContext != null && msgContext.contains("Write UpInterval=")){
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_update_interval_time_success));
                        upInterval=Integer.parseInt(msgContext.substring(17));
                        tvUpdateInterval.setText(upInterval+HJBleApplication.getAppContext().getString(R.string.minute));
                    }else {
                        refreshDetails();
                    }


                    break;
                case Constant.Return_Write_SWJ_DEVNum:
                    if (msgContext != null && msgContext.contains("Write DevNum=")){
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_devnum_success));
                        devNum=msgContext.substring(msgContext.length()-10);
                        tvDevNum.setText(devNum);
                        setTitle(devNum);
                    }else {
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail)+",\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_DEV_Mac_Address:
                    if (msgContext != null && msgContext.contains("Write BleAddr=")) {
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_ble_mac_success));
                    } else {
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail)+",\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_SWJ_DEVIP:
                    if (msgContext != null && msgContext.contains("Write DevIP=")){
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_ip_address_success));
                        refreshDetails();
                    }else {
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail)+",\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_SWJ_Start:
                    if (msgContext != null && "Set Start".equals(msgContext)){
                         showMsg(0, HJBleApplication.getAppContext().getString(R.string.swj_start_work_success));
                         tvWorkStart.setVisibility(View.GONE);
                    }

                    refreshDetails();
                    break;

                case Constant.Return_BLE_False:
                    showMsg(2, msgContext);
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
                case Constant.RequestCode_Scan_MAC_Address:
                    String qcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(qcode)) {
                        showMsg(2, HJBleApplication.getAppContext().getString(R.string.not_qcode_info));
                    } else if (qcode.length() != 12) {
                        showMsg(2, HJBleApplication.getAppContext().getString(R.string.not_lh_qcode));
                    } else {


                    }
                    break;
                case Constant.RequestCode_Scan_Box_MAC_Address:
                    String boxQcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(boxQcode)) {
                        showMsg(2, HJBleApplication.getAppContext().getString(R.string.not_qcode_info));
                    } else if (boxQcode.length() != 12) {
                        showMsg(2, HJBleApplication.getAppContext().getString(R.string.not_lh_qcode));
                    } else if (boxQcode.startsWith("12")) {
                        String macAds = boxQcode.trim();
                        new HitiStringDailog(LiherSWJ1Activity.this, Constant.Dailog_BLE_MACAddress, macAds, HJBleApplication.getAppContext().getString(R.string.hiti_macaddress_title), HJBleApplication.getAppContext().getString(R.string.hiti_macaddress_set_0) + StringUtils.getNewBlueToothMacAddress(macAds) + HJBleApplication.getAppContext().getString(R.string.hiti_macaddress_set_1), LiherSWJ1Activity.this);
                    } else {
                        showMsg(2, boxQcode + HJBleApplication.getAppContext().getString(R.string.not_lh_swj_qcode));
                    }
                    break;
            }
        }
    }
}
