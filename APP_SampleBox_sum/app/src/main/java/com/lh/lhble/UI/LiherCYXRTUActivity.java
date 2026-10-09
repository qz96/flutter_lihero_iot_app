package com.lh.lhble.UI;

import static com.lh.lhble.google.zxing.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
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

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.NetInfo;
import com.lh.lhble.Entity.RTUInfo;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.Entity.RtuLljInfo;
import com.lh.lhble.Entity.RtuSWJInfo;
import com.lh.lhble.R;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.UI.View.NetInfoInputDailog;
import com.lh.lhble.UI.View.SelecterObjectDailog;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.google.zxing.activity.CaptureActivity;
import com.lh.lhble.listener.CommonItemOnClickListener3;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.network.RetrofitUtils;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;
import com.wise.wisekit.utils.DateUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 力合采样设备内置RTU功能页面
 */
public class LiherCYXRTUActivity extends BaseActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherCYXRTUActivity.class.getSimpleName();

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
    private String sampleId = "LhCYXRTU";
    private RTUInfo mRtuInfo;

    private ScrollView sclOrders;
    private LinearLayout llOrderMode, llOrders;
    private EditText sendData;
    private TextView send;
    private LinearLayout llUiMode, llDevInfo, llWorkModels, llSWJModel, llLLJModel, llSETModel;

    private TextView tvSWJInput, tvLLJInput, tvSETInput;
    // RTU基础信息UI控件
    private LinearLayout llDevInfoExpand, llDevInfoMore, llNet0, llNet1, llNet2, llNet3, llNet4;
    private ImageView ivDevInfoExpand,ivDevOnOff,ivPwrEn;;
    private TextView tvLotType, tvSimImsi, tvLotErr, tvSampleInterval, tvUpdateInterval, tvDataCount, tvUpdateEndTime,
            tvDevIp0, tvNetPort0,tvProtocolType0,tvNetIsEn0, tvDevIp1, tvNetPort1,tvProtocolType1,tvNetIsEn1, tvDevIp2, tvNetPort2, tvProtocolType2,tvNetIsEn2,
            tvDevIp3, tvNetPort3,tvProtocolType3,tvNetIsEn3, tvDevIp4, tvNetPort4,tvProtocolType4,tvNetIsEn4, tvNetAdd;
    // RTU设置信息UI控件
    private TextView tvDevWorkState, tvDevEv, tvRefresh, tvDevName, tvDevVersion, tvDevNum, tvDevMN, tvLotStep, tvSYSTime, tvSYSST, tvMacAddress,
            tvSetMacAddress, tvSIMCard, tvLotEV,tvDevSamdevtype,tvRtuLLJ485Set;
    //RTU流量计信息UI控件
    private TextView tvLLJSamWait,tvLljRoll, tvLljPitch, tvLljWaterLev, tvLljHeight, tvLljDepth, tvLljDirection, tvLljSurfaceV, tvLljSectionV, tvLljArea, tvLljThinkNess,
            tvLljRateFlow, tvLljIntervalFlow, tvLljCumulativeFlow, tvLljScumuFlow, tvLljNcumuFlow, tvLljAbscumuFlow, tvLljResetFlow, tvLljTimeReads, tvLljParamsUp;
    //RTU水位计信息UI控件
    private TextView tvSWJSamWait, tvSWJPitch, tvSWJWaterLev, tvSWJHeight, tvSWJDepth,
            tvSWJRateFlow, tvSWJIntervalFlow, tvSWJCumulativeFlow,tvSWJTimeReads, tvSWJParamsUp;


    private LoadingDialog loadingDialog = null;

    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;

    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private int currentSendType, currentReceiveType;
    private RtuLljInfo rtuLljInfo;
    private boolean lljIsMeasure, lljIsUpdate;
    private NetInfo addNetInfo,currentNetInfo;
    private RtuSWJInfo rtuSWJInfo;
    private boolean swjIsMeasure, swjIsUpdate;
    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_cyx_rtu;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);
        setRightText("选择协议文件");
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkNewBlueBin();

            }
        });
        topLeftBtn.setVisibility(View.VISIBLE);
        context = this;

        mble = WiseBluetoothLe.getInstance(getApplicationContext());    //获取蓝牙实例
        mble.mBleCallBack = bleCallBack;

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
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
                sendLHBleData("Read RtuInf", Constant.Param_Read_RTUInfo);
                showMsg(2, HJBleApplication.getAppContext().getString(R.string.init_data));
            }
        });
    }


    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherCYXRTUActivity.class);
        intent.putExtra(LiherCYXRTUActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherCYXRTUActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherCYXRTUActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();
        llOrderMode = findViewById(R.id.ll_order_mode);
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        sendData = findViewById(R.id.sendData);
        send = findViewById(R.id.send);
        send.setOnClickListener(LiherCYXRTUActivity.this);
        llUiMode = findViewById(R.id.ll_ui_mode);
        llWorkModels = findViewById(R.id.ll_work_models);
        llDevInfo = findViewById(R.id.ll_dev_info);
        llSWJModel = findViewById(R.id.ll_swj_model);
        llLLJModel = findViewById(R.id.ll_llj_model);
        llSETModel = findViewById(R.id.ll_set_model);
        tvSWJInput = findViewById(R.id.tv_swj_input);
        tvLLJInput = findViewById(R.id.tv_llj_input);
        tvSETInput = findViewById(R.id.tv_set_input);
        tvDevWorkState = findViewById(R.id.tv_dev_work_state);
        tvDevEv = findViewById(R.id.tv_dev_ev);
        tvRefresh = findViewById(R.id.tv_refresh);
        llDevInfoExpand = findViewById(R.id.ll_dev_info_expand);
        ivDevInfoExpand = findViewById(R.id.iv_dev_info_expand);
        ivDevOnOff=findViewById(R.id.iv_dev_on_off);
        ivPwrEn=findViewById(R.id.iv_pwr_en);
        llDevInfoMore = findViewById(R.id.ll_dev_info_more);
        tvDevName = findViewById(R.id.tv_dev_name);
        tvDevVersion = findViewById(R.id.tv_dev_version);
        tvDevNum = findViewById(R.id.tv_dev_num);
        tvDevMN = findViewById(R.id.tv_dev_mn);
        tvLotType = findViewById(R.id.tv_lot_type);
        tvLotStep = findViewById(R.id.tv_lot_step);
        tvSYSTime = findViewById(R.id.tv_sys_time);
        tvSYSST = findViewById(R.id.tv_sys_st);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);
        tvSIMCard = findViewById(R.id.tv_sim_card);
        tvLotEV = findViewById(R.id.tv_lot_lev);
        tvSimImsi = findViewById(R.id.tv_sim_imsi);
        tvLotErr = findViewById(R.id.tv_lot_err);
        tvSampleInterval = findViewById(R.id.tv_sample_interval);
        tvUpdateInterval = findViewById(R.id.tv_update_interval);
        tvDevSamdevtype=findViewById(R.id.tv_dev_samdevtype);
        tvRtuLLJ485Set=findViewById(R.id.tv_rtu_llj_485_set);
        tvDataCount = findViewById(R.id.tv_data_count);
        tvUpdateEndTime = findViewById(R.id.tv_update_end_time);
        llNet0 = findViewById(R.id.ll_net_0);
        tvDevIp0 = findViewById(R.id.tv_dev_ip0);
        tvNetPort0 = findViewById(R.id.tv_net_port0);
        tvProtocolType0 = findViewById(R.id.tv_protocol_type0);
        tvNetIsEn0 = findViewById(R.id.tv_net_is_en0);
        llNet1 = findViewById(R.id.ll_net_1);
        tvDevIp1 = findViewById(R.id.tv_dev_ip1);
        tvNetPort1 = findViewById(R.id.tv_net_port1);
        tvProtocolType1 = findViewById(R.id.tv_protocol_type1);
        tvNetIsEn1 = findViewById(R.id.tv_net_is_en1);
        llNet2 = findViewById(R.id.ll_net_2);
        tvDevIp2 = findViewById(R.id.tv_dev_ip2);
        tvNetPort2 = findViewById(R.id.tv_net_port2);
        tvProtocolType2 = findViewById(R.id.tv_protocol_type2);
        tvNetIsEn2 = findViewById(R.id.tv_net_is_en2);
        llNet3 = findViewById(R.id.ll_net_3);
        tvDevIp3 = findViewById(R.id.tv_dev_ip3);
        tvNetPort3 = findViewById(R.id.tv_net_port3);
        tvProtocolType3 = findViewById(R.id.tv_protocol_type3);
        tvNetIsEn3 = findViewById(R.id.tv_net_is_en3);
        llNet4 = findViewById(R.id.ll_net_4);
        tvDevIp4 = findViewById(R.id.tv_dev_ip4);
        tvNetPort4 = findViewById(R.id.tv_net_port4);
        tvProtocolType4 = findViewById(R.id.tv_protocol_type4);
        tvNetIsEn4= findViewById(R.id.tv_net_is_en4);
        tvNetAdd = findViewById(R.id.tv_net_add);
        //RTU流量计信息UI控件
        tvLLJSamWait = findViewById(R.id.tv_llj_sam_wait);
        tvLljRoll = findViewById(R.id.tv_llj_roll);
        tvLljPitch = findViewById(R.id.tv_llj_pitch);
        tvLljWaterLev = findViewById(R.id.tv_llj_water_lev);
        tvLljHeight = findViewById(R.id.tv_llj_height);
        tvLljDepth = findViewById(R.id.tv_llj_depth);
        tvLljDirection = findViewById(R.id.tv_llj_direction);
        tvLljSurfaceV = findViewById(R.id.tv_llj_surface_v);
        tvLljSectionV = findViewById(R.id.tv_llj_section_v);
        tvLljArea = findViewById(R.id.tv_llj_area);
        tvLljThinkNess = findViewById(R.id.tv_llj_think_ness);
        tvLljRateFlow = findViewById(R.id.tv_llj_rate_flow);
        tvLljIntervalFlow = findViewById(R.id.tv_llj_interval_flow);
        tvLljCumulativeFlow = findViewById(R.id.tv_llj_cumulative_flow);
        tvLljScumuFlow = findViewById(R.id.tv_llj_scumu_flow);
        tvLljNcumuFlow = findViewById(R.id.tv_llj_ncumu_flow);
        tvLljAbscumuFlow = findViewById(R.id.tv_llj_abscumu_flow);
        tvLljResetFlow = findViewById(R.id.tv_llj_reset_flow);
        tvLljTimeReads = findViewById(R.id.tv_llj_time_reads);
        tvLljParamsUp = findViewById(R.id.tv_llj_params_up);
        //RTU水位计信息UI控件
        tvSWJSamWait=findViewById(R.id.tv_swj_sam_wait);
        tvSWJPitch=findViewById(R.id.tv_swj_pitch);
        tvSWJWaterLev=findViewById(R.id.tv_swj_water_lev);
        tvSWJHeight=findViewById(R.id.tv_swj_height);
        tvSWJDepth=findViewById(R.id.tv_swj_depth);
        tvSWJRateFlow=findViewById(R.id.tv_swj_rate_flow);
        tvSWJIntervalFlow=findViewById(R.id.tv_swj_interval_flow);
        tvSWJCumulativeFlow=findViewById(R.id.tv_swj_cumulative_flow);
        tvSWJTimeReads=findViewById(R.id.tv_swj_time_reads);
        tvSWJParamsUp=findViewById(R.id.tv_swj_params_up);

        tvSETInput.setOnClickListener(this);
        tvSWJInput.setOnClickListener(this);
        tvLLJInput.setOnClickListener(this);
        llDevInfoExpand.setOnClickListener(this);
        tvRefresh.setOnClickListener(this);
        tvLljTimeReads.setOnClickListener(this);
        tvLljParamsUp.setOnClickListener(this);
        tvSWJTimeReads.setOnClickListener(this);
        tvSWJParamsUp.setOnClickListener(this);
        tvLljResetFlow.setOnClickListener(this);
        tvSampleInterval.setOnClickListener(this);
        tvUpdateInterval.setOnClickListener(this);
        tvNetAdd.setOnClickListener(this);
        tvSetMacAddress.setOnClickListener(this);
        tvSYSTime.setOnClickListener(this);
        tvDevMN.setOnClickListener(this);
        tvSYSST.setOnClickListener(this);
        tvLotType.setOnClickListener(this);
        tvDevIp0.setOnClickListener(this);
        tvNetPort0.setOnClickListener(this);
        tvProtocolType0.setOnClickListener(this);
        tvNetIsEn0.setOnClickListener(this);
        tvDevIp1.setOnClickListener(this);
        tvNetPort1.setOnClickListener(this);
        tvProtocolType1.setOnClickListener(this);
        tvNetIsEn1.setOnClickListener(this);
        tvDevIp2.setOnClickListener(this);
        tvNetPort2.setOnClickListener(this);
        tvProtocolType2.setOnClickListener(this);
        tvNetIsEn2.setOnClickListener(this);
        tvDevIp3.setOnClickListener(this);
        tvNetPort3.setOnClickListener(this);
        tvProtocolType3.setOnClickListener(this);
        tvNetIsEn3.setOnClickListener(this);
        tvDevIp4.setOnClickListener(this);
        tvNetPort4.setOnClickListener(this);
        tvProtocolType4.setOnClickListener(this);
        tvNetIsEn4.setOnClickListener(this);
        tvDevSamdevtype.setOnClickListener(this);
        tvRtuLLJ485Set.setOnClickListener(this);
        tvLLJSamWait.setOnClickListener(this);
        tvSWJSamWait.setOnClickListener(this);
        ivDevOnOff.setOnClickListener(this);
        ivPwrEn.setOnClickListener(this);
        if (HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
            tvSetMacAddress.setVisibility(View.VISIBLE);
        }else {
            tvSetMacAddress.setVisibility(View.INVISIBLE);
        }

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
        if (bleCallBack != null && mble.mBleCallBack != bleCallBack) {
            mble.mBleCallBack = bleCallBack;
        }
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

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
       /* if (((byte) ((moduleSTA16>>2 & 0xff) % 2))==1&&v.getId()!=R.id.tv_refresh){
            showMsg(2, "设备正在上传数据，请刷新后重试");
            return;
        }*/

        switch (v.getId()) {
            case R.id.send:
                if (StringUtils.isEmpty(sendData.getText().toString())) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_order));
                    return;
                }
                sendLHBleData(sendData.getText().toString().trim(), Constant.PARAM_LIHER_ORDER);

                sendData.setText("");
                break;
            case R.id.tv_set_mac_address:
                startActivityForResult(new Intent(LiherCYXRTUActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_Box_MAC_Address);
                break;
            case R.id.tv_sys_time:
                new HitiInputStrDailog(LiherCYXRTUActivity.this, Constant.Dailog_BLE_SYSDateTime, HJBleApplication.getAppContext().getString(R.string.set_systime), HJBleApplication.getAppContext().getString(R.string.current_time)+"：", DateUtils.long2time(System.currentTimeMillis()), LiherCYXRTUActivity.this);
                break;
            case R.id.tv_dev_mn:
                new HitiInputStrDailog(LiherCYXRTUActivity.this, Constant.Dailog_BLE_DEVMN, "编辑设备MN号",  "MN号：",StringUtils.isEmpty(mRtuInfo.getDevMN())?"":mRtuInfo.getDevMN(), LiherCYXRTUActivity.this);

                break;
            case R.id.tv_refresh:
                if (llLLJModel.getVisibility()==View.VISIBLE){
                    sendLHBleData("Read SamInf", Constant.Param_Read_LLJInfo);
                    llDevInfoMore.setVisibility(View.GONE);
                    ivDevInfoExpand.setSelected(false);
                }else if (llSWJModel.getVisibility()==View.VISIBLE){

                    llDevInfoMore.setVisibility(View.GONE);
                    ivDevInfoExpand.setSelected(false);
                }else if (llSETModel.getVisibility()==View.VISIBLE){
                    sendLHBleData("Read RtuInf", Constant.Param_Read_RTUInfo);
                    llDevInfoMore.setVisibility(View.GONE);
                    ivDevInfoExpand.setSelected(false);
                }
                break;
            case R.id.tv_set_input:
                llSETModel.setVisibility(View.VISIBLE);
                llSWJModel.setVisibility(View.GONE);
                llLLJModel.setVisibility(View.GONE);
                ivDevInfoExpand.setSelected(true);
                llDevInfoMore.setVisibility(View.VISIBLE);
                refresh(1);
                break;
            case R.id.tv_swj_input:
                llSETModel.setVisibility(View.GONE);
                llSWJModel.setVisibility(View.VISIBLE);
                llLLJModel.setVisibility(View.GONE);
                sendLHBleData("Read SamInf", Constant.Param_Read_SWJInfo);
                break;
            case R.id.tv_llj_input:
                llSETModel.setVisibility(View.GONE);
                llSWJModel.setVisibility(View.GONE);
                llLLJModel.setVisibility(View.VISIBLE);
                sendLHBleData("Read SamInf", Constant.Param_Read_LLJInfo);
                break;
            case R.id.ll_dev_info_expand:
                if (ivDevInfoExpand.isSelected()) {
                    llDevInfoMore.setVisibility(View.GONE);
                    ivDevInfoExpand.setSelected(false);
                } else {
                    ivDevInfoExpand.setSelected(true);
                    llDevInfoMore.setVisibility(View.VISIBLE);
                    refresh(1);
                }

                break;
            case R.id.tv_llj_reset_flow:
                sendLHBleData("Set ZeroInit", Constant.Param_LLJ_SetZero);
                break;
            case R.id.tv_llj_time_reads:
                if (lljIsMeasure) {
                    RxTimerUtil.cancel();
                    sendLHBleData("Set Stop", Constant.Param_LLJ_SetStop);
                } else {
                    RxTimerUtil.cancel();
                    sendLHBleData("Set Start", Constant.Param_LLJ_SetStart);
                }

                break;
            case R.id.tv_llj_params_up:
                if (lljIsUpdate) {
                    RxTimerUtil.cancel();
                    sendLHBleData("Read LotInf",Constant.Param_RTU_UpdateInfo);
                } else {
                    sendLHBleData("Set DataUp", Constant.Param_LLJ_Update);
                }
                break;
            case R.id.tv_swj_time_reads:
                if (swjIsMeasure) {
                    RxTimerUtil.cancel();
                    sendLHBleData("Set Stop", Constant.Param_SWJ_SetStop);
                } else {
                    RxTimerUtil.cancel();
                    sendLHBleData("Set Start", Constant.Param_SWJ_SetStart);
                }

                break;
            case R.id.tv_swj_params_up:
                if (swjIsUpdate) {
                    RxTimerUtil.cancel();
                    sendLHBleData("Read LotInf",Constant.Param_RTU_UpdateInfo);
                } else {
                    sendLHBleData("Set DataUp", Constant.Param_SWJ_Update);
                }
                break;
            case R.id.tv_sample_interval:
                new HitiInputStrDailog(LiherCYXRTUActivity.this,Constant.Dialog_RTU_Sample_Interval,"设置采样间隔时间","间隔时间（分钟）:"
                        ,(mRtuInfo!=null&&mRtuInfo.getSapInterval()>=0)?mRtuInfo.getSapInterval()+"":"","请录入整数，采样间隔时间分钟数", LiherCYXRTUActivity.this);
                break;
            case R.id.tv_update_interval:
                new HitiInputStrDailog(LiherCYXRTUActivity.this,Constant.Dialog_RTU_Update_Interval,"设置上传间隔时间","间隔时间（分钟）:"
                        ,(mRtuInfo!=null&&mRtuInfo.getUpInterval()>=0)?mRtuInfo.getUpInterval()+"":"","请录入整数，上传间隔时间分钟数", LiherCYXRTUActivity.this);
                break;
            case R.id.tv_net_add:
                addNetInfo=null;
                NetInfo tempNetInfo ;
                if (mRtuInfo!=null&&mRtuInfo.getNetInfoList()!=null){
                    for (NetInfo netInfo:mRtuInfo.getNetInfoList()){
                        if (StringUtils.isEmpty(netInfo.getStrIp12())||netInfo.getStrIp12().length()!=12||"000000000000".equals(netInfo.getStrIp12())||!StringUtils.isLong(netInfo.getStrIp12())){
                            tempNetInfo=netInfo;
                            new NetInfoInputDailog(LiherCYXRTUActivity.this,Constant.Dialog_RTU_NetInfo_Add,"新增通信服务",tempNetInfo, LiherCYXRTUActivity.this);
                            return;
                        }
                    }
                    showMsg(2," 当前RTU的通信服务已达到上限5个，请清空现有通信服务再新增");
                }else {
                   showMsg(2,"未读取到当前RTU的通信服务配置信息,请刷新后重试");
                   refresh(1);
                   return;
                }

                break;
            case R.id.tv_sys_st:
                new HitiInputStrDailog(LiherCYXRTUActivity.this,Constant.Dialog_RTU_DEVST,"设置系统编码","系统编码：",StringUtils.isEmpty(mRtuInfo.getDevST())?"":mRtuInfo.getDevST().trim(),"录入上传数据的系统编码,例如：21", LiherCYXRTUActivity.this);
                break;
            case R.id.tv_lot_type:
                List<JSONObject> jat = new ArrayList<>();
                JSONObject jo0 = new JSONObject();
                jo0.put("MyName", "NB模组");
                jo0.put("MyObject", "1");
                jat.add(jo0);
                JSONObject jo1 = new JSONObject();
                jo1.put("MyName", "4G模组");
                jo1.put("MyObject", "2");
                jat.add(jo1);
                new SelecterObjectDailog(LiherCYXRTUActivity.this,"设置通信模组",jat,Constant.Dialog_Select_LotType, LiherCYXRTUActivity.this);
                break;
            case R.id.tv_dev_ip0:
            case R.id.tv_dev_ip1:
            case R.id.tv_dev_ip2:
            case R.id.tv_dev_ip3:
            case R.id.tv_dev_ip4:
                int ipNo=0;
                switch (v.getId()){
                    case R.id.tv_dev_ip0:
                        ipNo=0;
                        break;
                    case R.id.tv_dev_ip1:
                        ipNo=1;
                        break;
                    case R.id.tv_dev_ip2:
                        ipNo=2;
                        break;
                    case R.id.tv_dev_ip3:
                        ipNo=3;
                        break;
                    case R.id.tv_dev_ip4:
                        ipNo=4;
                        break;
                }
                if (mRtuInfo==null||mRtuInfo.getNetInfoList()==null||mRtuInfo.getNetInfoList().size()>ipNo){
                    currentNetInfo=mRtuInfo.getNetInfoList().get(ipNo);
                    new HitiInputStrDailog(LiherCYXRTUActivity.this,Constant.Dialog_Set_DevIP,"通信服务"+(ipNo+1)+"配置","IP地址：",
                            "","请录入正确的IP地址，例如36.137.24.154", LiherCYXRTUActivity.this);
                }else {
                    showMsg(0,"未获取到RTU通讯服务信息");
                    refresh(1);
                }
                break;
            case R.id.tv_net_port0:
            case R.id.tv_net_port1:
            case R.id.tv_net_port2:
            case R.id.tv_net_port3:
            case R.id.tv_net_port4:
                int portNo=0;
                switch (v.getId()){
                    case R.id.tv_net_port0:
                        portNo=0;
                        break;
                    case R.id.tv_net_port1:
                        portNo=1;
                        break;
                    case R.id.tv_net_port2:
                        portNo=2;
                        break;
                    case R.id.tv_net_port3:
                        portNo=3;
                        break;
                    case R.id.tv_net_port4:
                        portNo=4;
                        break;
                }
                if (mRtuInfo==null||mRtuInfo.getNetInfoList()==null||mRtuInfo.getNetInfoList().size()>portNo){
                    currentNetInfo=mRtuInfo.getNetInfoList().get(portNo);
                    new HitiInputStrDailog(LiherCYXRTUActivity.this,Constant.Dialog_Set_DevPort,"通信服务"+(portNo+1)+"配置","端口号：",
                            "","请录入正确的通信服务端口号", LiherCYXRTUActivity.this);
                }else {
                    showMsg(0,"未获取到RTU通讯服务信息");
                    refresh(1);
                }
                break;
            case R.id.tv_protocol_type0:
            case R.id.tv_protocol_type1:
            case R.id.tv_protocol_type2:
            case R.id.tv_protocol_type3:
            case R.id.tv_protocol_type4:
                int protocolNo=0;
                switch (v.getId()){
                    case R.id.tv_protocol_type0:
                        protocolNo=0;
                        break;
                    case R.id.tv_protocol_type1:
                        protocolNo=1;
                        break;
                    case R.id.tv_protocol_type2:
                        protocolNo=2;
                        break;
                    case R.id.tv_protocol_type3:
                        protocolNo=3;
                        break;
                    case R.id.tv_protocol_type4:
                        protocolNo=4;
                        break;
                }
                if (mRtuInfo==null||mRtuInfo.getNetInfoList()==null||mRtuInfo.getNetInfoList().size()>protocolNo){
                    currentNetInfo=mRtuInfo.getNetInfoList().get(protocolNo);
                    List<JSONObject> jatProtocol = new ArrayList<>();
                    JSONObject joProtocol0 = new JSONObject();
                    joProtocol0.put("MyName", "212协议");
                    joProtocol0.put("MyObject", "0");
                    jatProtocol.add(joProtocol0);
                    JSONObject joProtocol1 = new JSONObject();
                    joProtocol1.put("MyName", "Mqtt协议");
                    joProtocol1.put("MyObject", "1");
                    jatProtocol.add(joProtocol1);
                    new SelecterObjectDailog(LiherCYXRTUActivity.this,"通信服务"+(protocolNo+1)+"上传协议",jatProtocol,Constant.Dialog_Set_DevProtocol, LiherCYXRTUActivity.this);

                }else {
                    showMsg(0,"未获取到RTU通讯服务信息");
                    refresh(1);
                }
                break;
            case R.id.tv_net_is_en0:
            case R.id.tv_net_is_en1:
            case R.id.tv_net_is_en2:
            case R.id.tv_net_is_en3:
            case R.id.tv_net_is_en4:
                int isEnNo=0;
                switch (v.getId()){
                    case R.id.tv_net_is_en0:
                        isEnNo=0;
                        break;
                    case R.id.tv_net_is_en1:
                        isEnNo=1;
                        break;
                    case R.id.tv_net_is_en2:
                        isEnNo=2;
                        break;
                    case R.id.tv_net_is_en3:
                        isEnNo=3;
                        break;
                    case R.id.tv_net_is_en4:
                        isEnNo=4;
                        break;
                }
                if (mRtuInfo==null||mRtuInfo.getNetInfoList()==null||mRtuInfo.getNetInfoList().size()>isEnNo){
                    currentNetInfo=mRtuInfo.getNetInfoList().get(isEnNo);
                    List<JSONObject> jatIsEn = new ArrayList<>();
                    JSONObject joIsEn0 = new JSONObject();
                    joIsEn0.put("MyName", "设为停用状态");
                    joIsEn0.put("MyObject", "0");
                    jatIsEn.add(joIsEn0);
                    JSONObject joIsEn1 = new JSONObject();
                    joIsEn1.put("MyName", "设为启用状态");
                    joIsEn1.put("MyObject", "1");
                    jatIsEn.add(joIsEn1);
                    JSONObject joIsEn2 = new JSONObject();
                    joIsEn2.put("MyName", "删除本通信服务");
                    joIsEn2.put("MyObject", "2");
                    jatIsEn.add(joIsEn2);

                    new SelecterObjectDailog(LiherCYXRTUActivity.this,"通信服务"+(isEnNo+1)+"状态配置",jatIsEn,Constant.Dialog_Set_DevIsEn, LiherCYXRTUActivity.this);

                }else {
                    showMsg(0,"未获取到RTU通讯服务信息");
                    refresh(1);
                }
                break;
            case R.id.tv_dev_samdevtype:
                if (mRtuInfo==null||StringUtils.isEmpty(mRtuInfo.getVersion())){
                    showMsg(2,"请先刷新获取‘RTU设置‘基础信息");
                    return;
                }
                List<JSONObject> jatSamdevtype = new ArrayList<>();
                JSONObject joSamdevtype0 = new JSONObject();
                joSamdevtype0.put("MyName", "雷达水位计");
                joSamdevtype0.put("MyObject", "0");
                jatSamdevtype.add(joSamdevtype0);
                JSONObject joSamdevtype1 = new JSONObject();
                joSamdevtype1.put("MyName", "雷达流量计");
                joSamdevtype1.put("MyObject", "1");
                jatSamdevtype.add(joSamdevtype1);
                JSONObject joSamdevtype2 = new JSONObject();
                joSamdevtype2.put("MyName", "未连接设备");
                joSamdevtype2.put("MyObject", "2");
                jatSamdevtype.add(joSamdevtype2);

                new SelecterObjectDailog(LiherCYXRTUActivity.this,"设置RTU当前连接设备",jatSamdevtype,Constant.Dialog_Set_DevSamdevType, LiherCYXRTUActivity.this);
                showMsg(2,"务必和RTU实际连接设备一致，否则无法正常采集测量！");
                break;
            case R.id.tv_llj_sam_wait:
                new HitiInputStrDailog(LiherCYXRTUActivity.this,Constant.Dialog_RTU_LLJSamWait,"流量计上电延时",
                        "上电延时(s):",rtuLljInfo!=null&&rtuLljInfo.samWait>0?(rtuLljInfo.samWait+""):"", LiherCYXRTUActivity.this);
                break;
            case R.id.tv_swj_sam_wait:
                new HitiInputStrDailog(LiherCYXRTUActivity.this,Constant.Dialog_RTU_SWJSamWait,"水位计上电延时",
                        "上电延时(s):",rtuSWJInfo!=null&&rtuSWJInfo.samWait>0?(rtuSWJInfo.samWait+""):"", LiherCYXRTUActivity.this);
                break;
            case R.id.iv_dev_on_off:
                if (ivDevOnOff.isSelected()){
                    sendLHBleData("Set Stop",Constant.Param_RTU_Set_Stop);
                }else {
                    sendLHBleData("Set Start",Constant.Param_RTU_Set_Start);

                }
                break;
            case R.id.iv_pwr_en:
                if (ivPwrEn.isSelected()){
                    sendLHBleData("Write PwrEn=0",Constant.Param_RTU_PwrEn);
                }else {
                    sendLHBleData("Write PwrEn=1",Constant.Param_RTU_PwrEn);
                }
                break;
            case R.id.tv_rtu_llj_485_set:
                startActivity(LiherRTULLJ485SetActivity.getIntent(LiherCYXRTUActivity.this, mDeviceName, mDeviceAddress, isConfig));
                break;
        }
    }

    private void checkNewBlueBin() {
        showMsg(2,"请选择您的协议目标文件(.bin)");
        Intent intent =new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*") ;
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        LiherCYXRTUActivity.this.startActivityForResult(Intent.createChooser(intent, "选择蓝牙升级文件"), Constant.RequestCode_RTU_Bin);
    }


    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.Dialog_RTU_Sample_Interval:
                String strSampleInterval = (String) target;
                if (StringUtils.isInt(strSampleInterval)&&Integer.parseInt(strSampleInterval)>=0){
                    sendLHBleData("Write SapInterval="+StringUtils.formatString(Integer.parseInt(strSampleInterval),5),Constant.Param_RTU_SampleInterval);
                }else {
                    showMsg(2,"请录入正确格式的分钟数，整数");
                }
                break;

            case Constant.Dialog_RTU_Update_Interval:
                String strUpdateInterval = (String) target;
                if (StringUtils.isInt(strUpdateInterval)&&Integer.parseInt(strUpdateInterval)>=0){
                    sendLHBleData("Write UpInterval="+StringUtils.formatString(Integer.parseInt(strUpdateInterval),5),Constant.Param_RTU_UpdateInterval);
                }else {
                    showMsg(2,"请录入正确格式的分钟数，整数");
                }
                break;
            case Constant.Dialog_RTU_DEVST:
                String devST= (String) target;
                if (StringUtils.isInt(devST)){
                    sendLHBleData("Write DevST="+devST.trim(),Constant.Param_RTU_ST);
                }else {
                    showMsg(2,"请录入正确格式的系统编码，整数");
                }
                break;
            case Constant.Dialog_RTU_NetInfo_Add:
                NetInfo netInfo= (NetInfo) target;
                if (netInfo==null ||netInfo.getNo()<0){
                    showMsg(2,"未获取到正确的通信服务序号0~4");
                }else {
                    addNetInfo=netInfo;
                    sendLHBleData("Write DevIp="+StringUtils.formatString(addNetInfo.getNo(),2)+"#"+addNetInfo.getStrIp12(),Constant.Param_RTU_Set_IP);
                    addNetInfo.setStrIp12("");
                }
                break;
            case Constant.Dailog_BLE_MACAddress:
                String strTarget = (String) target;
                sendLHBleData("Write BleAddr=" + strTarget, Constant.Param_Write_DEV_Mac_Address);
                break;
            case Constant.Dailog_BLE_SYSDateTime:
                String sysTime = (String) target;
                if (sysTime != null && sysTime.length() == 19) {
                    sendLHBleData("Write SysTime=" + DateUtils.formatTime(sysTime, DateUtils.DATE_TIME_PATTERN, DateUtils.METHOD_DATE_TIME_PATTERN).substring(2),Constant.Param_Wirte_SysTime);
                } else {
                    showMsg(2,HJBleApplication.getAppContext().getString(R.string.input_right_systime)+"，" + sysTime);
                }
                break;
            case Constant.Dailog_BLE_DEVMN:
                String MN= (String) target;
                if (StringUtils.isEmpty(MN)){
                    showMsg(2,"请录入正确MN号");
                }else {
                    sendLHBleData("Write DevMN="+MN.trim(),Constant.Param_Write_RTU_DEVMN);
                }
                break;
            case Constant.Dialog_Set_DevIP:
                String ip= (String) target;
                if (!StringUtils.isIPAddress(ip)){
                    showMsg(0,"请设置正确IP地址，例如192.107.8.26");
                }else {
                    String[]ips=ip.trim().split("\\.");
                    String devIP=StringUtils.formatString(Integer.parseInt(ips[0]),3)+StringUtils.formatString(Integer.parseInt(ips[1]),3)+StringUtils.formatString(Integer.parseInt(ips[2]),3)+StringUtils.formatString(Integer.parseInt(ips[3]),3);
                    sendLHBleData("Write DevIp="+StringUtils.formatString(currentNetInfo.getNo(),2)+"#"+devIP,Constant.Param_RTU_Set_IP);
                    currentNetInfo=null;
                }
                break;
            case Constant.Dialog_Set_DevPort:
                String port=(String) target;
                if (port!=null&&port.length()<=5&&StringUtils.isInt(port)){
                    String devPort=StringUtils.formatString(Integer.parseInt(port),5);
                    sendLHBleData("Write DevPort="+StringUtils.formatString(currentNetInfo.getNo(),2)+"#"+devPort,Constant.Param_RTU_Set_Port);
                    currentNetInfo=null;
                }else {
                    showMsg(0,"请设置正确端口号，整数");
                }
                break;
            case Constant.Dialog_RTU_LLJSamWait:
                String lljWait= (String) target;
                if (lljWait!=null&&StringUtils.isInt(lljWait)&&Integer.parseInt(lljWait)>0){
                    sendLHBleData("Write SamWait="+StringUtils.formatString(Integer.parseInt(lljWait),5),Constant.Param_RTU_LLJ_SamWait);

                }else {
                    showMsg(2,"请录入正确格式的上电延时时间秒数,>0的整数");
                }
                break;
            case Constant.Dialog_RTU_SWJSamWait:
                String swjWait= (String) target;
                if (swjWait!=null&&StringUtils.isInt(swjWait)&&Integer.parseInt(swjWait)>0){
                    sendLHBleData("Write SamWait="+StringUtils.formatString(Integer.parseInt(swjWait),5),Constant.Param_RTU_SWJ_SamWait);

                }else {
                    showMsg(2,"请录入正确格式的上电延时时间秒数,>0的整数");
                }
                break;
        }
    }
    @Override
    public void onItemClick3(View v, int position, Object object, int type) {
        switch (type){
            case Constant.Dialog_Select_LotType:
                    sendLHBleData("Write LotType="+position,Constant.Param_RTU_Set_LotType);
                    break;
            case Constant.Dialog_Set_DevProtocol:
                sendLHBleData("Write Protocol="+StringUtils.formatString(currentNetInfo.getNo(),2)+"#"+position,Constant.Param_RTU_Set_Protocol);
                currentNetInfo=null;
                break;
            case Constant.Dialog_Set_DevIsEn:
                if (mRtuInfo==null){
                    showMsg(1,"未启用通信服务"+(addNetInfo.getNo()+1));
                    refresh(1);
                    return;
                }
                int ipEns=mRtuInfo.getIpEns();
                byte[] bytes=StringUtils.getDateBit16(ipEns);
                if (position==0||position==1){
                    bytes[currentNetInfo.getNo()]= (byte) position;
                    ipEns=StringUtils.bit162Int(bytes);
                    sendLHBleData("Write LotIpEn="+StringUtils.formatString(ipEns,3),Constant.Param_RTU_Set_LotIpEn);
                }else if (position==2){
                    bytes[currentNetInfo.getNo()]= 0;
                    ipEns=StringUtils.bit162Int(bytes);
                    sendLHBleData("Write LotIpEn="+StringUtils.formatString(ipEns,3),Constant.Param_RTU_Clean_NetInfo);
                }


                break;
            case Constant.Dialog_Set_DevSamdevType:
                int i=mRtuInfo.getSamDevType();
                byte[] ib =StringUtils.getDateBit16(i);
                if (position==0){
                    ib[0]=1;
                    ib[1]=0;
                    sendLHBleData("Write SamdevType="+StringUtils.formatString(StringUtils.bit162Int(ib),5),Constant.Param_RTU_Set_SamDevType);
                }else if (position==1){
                    ib[0]=0;
                    ib[1]=1;
                    sendLHBleData("Write SamdevType="+StringUtils.formatString(StringUtils.bit162Int(ib),5),Constant.Param_RTU_Set_SamDevType);
                }else if (position==2){
                    ib[0]=0;
                    ib[1]=0;
                    sendLHBleData("Write SamdevType="+StringUtils.formatString(StringUtils.bit162Int(ib),5),Constant.Param_RTU_Set_SamDevType);
                }


                break;
        }
    }

    //发送数据
    public void sendLHBleData(String sendString, int sendType) {

        byte[] tmpBytes;
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2, HJBleApplication.getAppContext().getString(R.string.empty_send_msg));
            return;
        }

        tmpBytes = ProtocolUtil.getBlueToothMsgForSend(HJBleApplication.shareInstance().getDesID(), sendString);

        if (tmpBytes == null) {
            return;
        } else if (mble == null || !mble.isConnect()) {

            showMsg(2, HJBleApplication.getAppContext().getString(R.string.ble_close_connect_again));
            finish();
            return;
        }
        if (LiherCYXRTUActivity.this.isFinishing()) {
            return;
        }

        loadingDialog.show();
        addLogText(sendString, 0, sendType);
        insertLog(mDeviceAddress, sampleId, sendString);
        final byte[] bytes = tmpBytes;
        currentSendType = sendType;
        if (sendString.length()>50){
            mble.sendLhOrderDataList(mSendCharact,mReceiveCharact,sendString,currentSendType);
        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!mble.sendData(mSendCharact, bytes)) {
                        addLogText(HJBleApplication.getAppContext().getString(R.string.send_fail) + "！", 0, Constant.Return_Ble_SendFail);
                        currentSendType = 0;

                    }else if (!mble.waitReturn(mReceiveCharact,15*1000)){
                        addLogText(getResources().getString(R.string.ble_rtn_timeout),1, Constant.Return_BLE_False);
                        currentSendType = 0;
                    }

                }
            }).start();
        }



    }

    WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
        @Override
        public void OnWiseBluetoothState(int state) {

            if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED) {
                if (loadingDialog != null) loadingDialog.dismiss();
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close), 2, Constant.Return_Ble_ConnectStop);
             //   finish();
            }

        }

        @Override
        public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {

            if (characteristic.equals(mReceiveCharact)) {
                if (loadingDialog != null) loadingDialog.dismiss();
                String str = "";
                if (recvData==null||recvData.length<=0){
                    str="";
                }else if (HJBleApplication.shareInstance().isBleHex()) {
                    str = ConvertData.bytesToHexString(recvData, false);
                } else if (HJBleApplication.shareInstance().isBleConfig() == 0) {
                    str = ProtocolUtil.getReturnMsgForReceive(recvData);
                } else {
                    str = new String(recvData);
                }
                if (str != null) {
                    if (Constant.ReceiveFalse.equals(str)) {
                        addLogText("蓝牙返回数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                    } else if (Constant.ReceiveDataListFalse.equals(str)) {
                        addLogText("蓝牙返回分包数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                    } else if (Constant.ReceiveContinue.equals(str)) {
                        addLogText("蓝牙消息返回中", 1, Constant.Return_BLE_Continue);
                    } else if (str.startsWith(Constant.SendDataListFalse)) {
                        addLogText(str.substring(Constant.SendDataListFalse.length()), 1, Constant.Return_Ble_ReceiveFalse);
                    } else {
                        switch (currentSendType) {
                            case Constant.PARAM_LIHER_ORDER:
                                currentReceiveType = Constant.RETURN_LIHER_OREDER;
                                break;
                            case Constant.Param_Wirte_SysTime:
                                currentReceiveType = Constant.Return_Wirte_SysTime;
                                break;
                            case Constant.Param_Read_RTUInfo:
                                currentReceiveType = Constant.Return_Read_RTUInfo;
                                break;
                            case Constant.Param_Read_RTULot:
                                currentReceiveType = Constant.Return_Read_RTULot;
                                break;
                            case Constant.Param_Read_LLJInfo:
                                currentReceiveType = Constant.Return_Read_LLJInfo;
                                break;
                            case Constant.Param_RTU_UpdateInfo:
                                currentReceiveType = Constant.Return_RTU_UpdateInfo;
                                break;
                            case Constant.Param_LLJ_SetZero:
                                currentReceiveType = Constant.Return_LLJ_SetZero;
                                break;
                            case Constant.Param_LLJ_SetStart:
                                currentReceiveType = Constant.Return_LLJ_SetStart;
                                break;
                            case Constant.Param_LLJ_SetStop:
                                currentReceiveType = Constant.Return_LLJ_SetStop;
                                break;
                            case Constant.Param_LLJ_Update:
                                currentReceiveType = Constant.Return_LLJ_Update;
                                break;
                            case Constant.Param_SWJ_SetStart:
                                currentReceiveType = Constant.Return_SWJ_SetStart;
                                break;
                            case Constant.Param_SWJ_SetStop:
                                currentReceiveType = Constant.Return_SWJ_SetStop;
                                break;
                            case Constant.Param_SWJ_Update:
                                currentReceiveType = Constant.Return_SWJ_Update;
                                break;
                            case Constant.Param_RTU_SampleInterval:
                                currentReceiveType = Constant.Return_RTU_SampleInterval;
                                break;
                            case Constant.Param_RTU_UpdateInterval:
                                currentReceiveType = Constant.Return_RTU_UpdateInterval;
                                break;
                            case Constant.Param_RTU_Set_IP:
                                currentReceiveType=Constant.Return_RTU_Set_IP;
                                break;
                            case Constant.Param_RTU_Set_Port:
                                currentReceiveType=Constant.Return_RTU_Set_Port;
                                break;
                            case Constant.Param_RTU_Set_Protocol:
                                currentReceiveType=Constant.Return_RTU_Set_Protocol;
                                break;
                            case Constant.Param_RTU_Set_LotIpEn:
                                currentReceiveType=Constant.Return_RTU_Set_LotIpEn;
                                break;
                            case Constant.Param_Write_DEV_Mac_Address:
                                currentReceiveType = Constant.Return_Write_DEV_Mac_Address;
                                break;
                            case Constant.Param_Write_RTU_DEVMN:
                                currentReceiveType = Constant.Return_Write_RTU_DEVMN;
                                break;
                            case Constant.Param_RTU_ST:
                                currentReceiveType = Constant.Return_RTU_ST;
                                break;
                            case Constant.Param_RTU_Set_LotType:
                                currentReceiveType = Constant.Return_RTU_Set_LotType;
                                break;
                            case Constant.Param_RTU_Clean_NetInfo:
                                currentReceiveType = Constant.Return_RTU_Clean_NetInfo;
                                break;
                            case Constant.Param_RTU_Set_SamDevType:
                                currentReceiveType = Constant.Return_RTU_Set_SamDevType;
                                break;
                            case Constant.Param_Read_SWJInfo:
                                currentReceiveType = Constant.Return_Read_SWJInfo;
                                break;
                            case Constant.Param_RTU_LLJ_SamWait:
                                currentReceiveType = Constant.Return_RTU_LLJ_SamWait;
                                break;
                            case Constant.Param_RTU_SWJ_SamWait:
                                currentReceiveType = Constant.Return_RTU_SWJ_SamWait;
                                break;
                            case Constant.Param_RTU_Set_Start:
                                currentReceiveType=Constant.Return_RTU_Set_Start;
                                break;
                            case Constant.Param_RTU_Set_Stop:
                                currentReceiveType=Constant.Return_RTU_Set_Stop;
                                break;
                            case Constant.Param_RTU_PwrEn:
                                currentReceiveType=Constant.Return_RTU_PwrEn;
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
                addLogText(HJBleApplication.getAppContext().getString(R.string.note) + logStr, 2, 0);
            }
        }

        @Override
        public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {
            if (!StringUtils.isEmpty(stateMsg)) {
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close) + "," + stateMsg, 2, Constant.Return_Ble_ConnectState);
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
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            } else if (arg2 == Constant.Return_Ble_SendFail) {
                SpannableStringBuilder builder = new SpannableStringBuilder(msgContext);
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
                builder.setSpan(colorSpan, 0, msgContext.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                if (loadingDialog != null) loadingDialog.dismiss();
                showMsg(1, msgContext);
            }
            System.out.println("SendString:" + msgContext);
        } else if (arg1 == 1) {//接收到的消息
            String msgContext = (String) obj;
            SpannableStringBuilder builder = new SpannableStringBuilder(HJBleApplication.getAppContext().getString(R.string.receive) + msgContext);
            //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(139, 0, 255));
            builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView tView = new TextView(context);
            tView.setText(builder);
            llOrders.addView(tView);
            sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            switch (arg2) {
                case Constant.RETURN_LIHER_OREDER:
                    break;
                case Constant.Return_Wirte_SysTime:
                    if (msgContext != null && msgContext.contains("Write SysTime=")) {
                        mRtuInfo.setSysTime(DateUtil.format2miao("20" + msgContext.substring(14).trim(), "yyyyMMddHHmmss"));
                        tvSYSTime.setText(mRtuInfo.getSysTime()>0?DateUtil.long2time4(mRtuInfo.getSysTime()):"");
                    } else {
                        showMsg(1,HJBleApplication.getAppContext().getString(R.string.set_systime_fail)+",\n" + msgContext);
                    }
                    break;
                case Constant.Return_Read_RTUInfo://返回蓝牙RTU基本信息
                    try {
                        String[] rtuInfos = msgContext.split("#", -1);
                        if (mRtuInfo == null) mRtuInfo = new RTUInfo();
                        if (rtuInfos.length >= 8) {
                            mRtuInfo.setVersion(rtuInfos[0]);
                            if (rtuInfos[1] != null && rtuInfos[1].length() == 12 && rtuInfos[1].startsWith("2")) {
                                mRtuInfo.setSysTime(DateUtil.format2miao("20" + rtuInfos[1], "yyyyMMddHHmmss"));
                            }
                            mRtuInfo.setDevName(rtuInfos[2].trim());
                            mRtuInfo.setBleMac(StringUtils.getNewBlueToothMacAddress(rtuInfos[3].trim()));
                            mRtuInfo.setDevMN(rtuInfos[4].trim());
                            mRtuInfo.setBatV(StringUtils.isDouble(rtuInfos[5]) ? Double.parseDouble(rtuInfos[5]) : 0d);
                            mRtuInfo.setDevNum(rtuInfos[6]);
                            mRtuInfo.setDevState(StringUtils.isInt(rtuInfos[7]) ? Integer.parseInt(rtuInfos[7]) : -1);
                        }
                        if (rtuInfos.length >= 9) {
                            mRtuInfo.setSamDevType(StringUtils.isInt(rtuInfos[8]) ? Integer.parseInt(rtuInfos[8]) : -1);
                        }
                        setRTUInfoData(0);
                        if (Math.abs(mRtuInfo.getSysTime()-System.currentTimeMillis())>1*60*1000L){
                            RxTimerUtil.cancel();
                            RxTimerUtil.timer(3000L, new RxTimerUtil.IRxNext() {
                                @Override
                                public void doNext(long number) {
                                    new HitiInputStrDailog(LiherCYXRTUActivity.this, Constant.Dailog_BLE_SYSDateTime, HJBleApplication.getAppContext().getString(R.string.set_systime), HJBleApplication.getAppContext().getString(R.string.current_time)+"：", DateUtils.long2time(System.currentTimeMillis()), LiherCYXRTUActivity.this);
                                }
                            });
                        }
                    } catch (Exception e) {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        showMsg(1, "RtuInfo解析失败，\n" + msgContext);
                        e.printStackTrace();
                    }

                    break;
                case Constant.Return_Read_RTULot://返回蓝牙RTU的Lot网络数据信息
                    try {
                        String[] rtuLots = msgContext.split("#", -1);
                        if (mRtuInfo == null) mRtuInfo = new RTUInfo();
                        if (rtuLots.length >= 30) {
                            mRtuInfo.setLotType(StringUtils.isInt(rtuLots[0]) ? Integer.parseInt(rtuLots[0]) : Constant.DefaultValue);
                            mRtuInfo.setLocalIP(rtuLots[1]);
                            mRtuInfo.setNCCID(rtuLots[2]);
                            mRtuInfo.setIMSI(rtuLots[3]);
                            mRtuInfo.setLotCsq(StringUtils.isInt(rtuLots[4]) ? Integer.parseInt(rtuLots[4]) : Constant.DefaultValue);
                            mRtuInfo.setSaveNum(StringUtils.isInt(rtuLots[5]) ? Integer.parseInt(rtuLots[5]) : Constant.DefaultValue);
                            mRtuInfo.setLotState(StringUtils.isInt(rtuLots[6]) ? Integer.parseInt(rtuLots[6]) : Constant.DefaultValue);
                            mRtuInfo.setLotError(StringUtils.isInt(rtuLots[7]) ? Integer.parseInt(rtuLots[7]) : Constant.DefaultValue);
                            if (rtuLots[8] != null && rtuLots[8].length() == 12 && rtuLots[8].startsWith("2")) {
                                mRtuInfo.setUpdateEndTime(DateUtil.format2miao("20" + rtuLots[8], "yyyyMMddHHmmss"));
                            }
                            mRtuInfo.setLotStep(StringUtils.isInt(rtuLots[9]) ? Integer.parseInt(rtuLots[9]) : Constant.DefaultValue);
                            mRtuInfo.setSapInterval(StringUtils.isInt(rtuLots[10]) ? Integer.parseInt(rtuLots[10]) : Constant.DefaultValue);
                            mRtuInfo.setUpInterval(StringUtils.isInt(rtuLots[11]) ? Integer.parseInt(rtuLots[11]) : Constant.DefaultValue);
                            List<NetInfo> netInfos=new ArrayList<>();
                            int ipEns=StringUtils.isInt(rtuLots[27])?Integer.parseInt(rtuLots[27]):0;
                            mRtuInfo.setIpEns(ipEns);
                            NetInfo netInfo0=new NetInfo(0,rtuLots[12],rtuLots[13],(StringUtils.isInt(rtuLots[14])?Integer.parseInt(rtuLots[14]):Constant.DefaultValue),ipEns%2);
                            netInfo0.checkIp2En();//检查通信IP配置被清空或不正常状态
                            NetInfo netInfo1=new NetInfo(1,rtuLots[15],rtuLots[16],(StringUtils.isInt(rtuLots[17])?Integer.parseInt(rtuLots[17]):Constant.DefaultValue),ipEns>>1%2);
                            netInfo1.checkIp2En();//检查通信IP配置被清空或不正常状态
                            NetInfo netInfo2=new NetInfo(2,rtuLots[18],rtuLots[19],(StringUtils.isInt(rtuLots[20])?Integer.parseInt(rtuLots[20]):Constant.DefaultValue),ipEns>>2%2);
                            netInfo2.checkIp2En();//检查通信IP配置被清空或不正常状态
                            NetInfo netInfo3=new NetInfo(3,rtuLots[21],rtuLots[22],(StringUtils.isInt(rtuLots[23])?Integer.parseInt(rtuLots[23]):Constant.DefaultValue),ipEns>>3%2);
                            netInfo3.checkIp2En();//检查通信IP配置被清空或不正常状态
                            NetInfo netInfo4=new NetInfo(4,rtuLots[24],rtuLots[25],(StringUtils.isInt(rtuLots[26])?Integer.parseInt(rtuLots[26]):Constant.DefaultValue),ipEns>>4%2);
                            netInfo4.checkIp2En();//检查通信IP配置被清空或不正常状态
                            netInfos.add(netInfo0);
                            netInfos.add(netInfo1);
                            netInfos.add(netInfo2);
                            netInfos.add(netInfo3);
                            netInfos.add(netInfo4);
                            mRtuInfo.setNetInfoList(netInfos);
                            mRtuInfo.setDevST(StringUtils.isEmpty(rtuLots[28])?"":rtuLots[28]);
                            mRtuInfo.setPwrEn(StringUtils.isInt(rtuLots[29])?Integer.parseInt(rtuLots[29]):Constant.DefaultValue);
                        }
                        setRTUInfoData(1);
                    } catch (Exception e) {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        showMsg(1, "LotInf解析失败，\n" + msgContext);
                        e.printStackTrace();
                    }

                    break;
                case Constant.Return_Read_LLJInfo:
                    String[] lljInfos = msgContext.split("#", -1);
                    rtuLljInfo = new RtuLljInfo();
                    if (lljInfos.length >= 22) {
                        rtuLljInfo.samWait = StringUtils.isInt(lljInfos[0]) ? Integer.parseInt(lljInfos[0].trim()) : (lljInfos[0].endsWith("N")? Integer.parseInt(lljInfos[0].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.waterLev = StringUtils.isDouble(lljInfos[1]) ? Double.parseDouble(lljInfos[1].trim()) : (lljInfos[1].endsWith("N") ? Double.parseDouble(lljInfos[1].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.depth = StringUtils.isDouble(lljInfos[2]) ? Double.parseDouble(lljInfos[2].trim()) :  ( lljInfos[2].endsWith("N") ? Double.parseDouble(lljInfos[2].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.height = StringUtils.isDouble(lljInfos[3]) ? Double.parseDouble(lljInfos[3].trim()) : (lljInfos[3].endsWith("N") ? Double.parseDouble(lljInfos[3].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.direction = StringUtils.isInt(lljInfos[4]) ? Integer.parseInt(lljInfos[4].trim()) : ( lljInfos[4].endsWith("N") ? Integer.parseInt(lljInfos[4].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.surfaceV = StringUtils.isDouble(lljInfos[5]) ? Double.parseDouble(lljInfos[5].trim()) : (lljInfos[5].endsWith("N") ? Double.parseDouble(lljInfos[5].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.sectionV = StringUtils.isDouble(lljInfos[6]) ? Double.parseDouble(lljInfos[6].trim()) : (lljInfos[6].endsWith("N") ? Double.parseDouble(lljInfos[6].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.rateFlow = StringUtils.isDouble(lljInfos[7]) ? Double.parseDouble(lljInfos[7].trim()) : (lljInfos[7].endsWith("N") ? Double.parseDouble(lljInfos[7].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.intervalFlow = StringUtils.isDouble(lljInfos[8]) ? Double.parseDouble(lljInfos[8].trim()) : (lljInfos[8].endsWith("N") ? Double.parseDouble(lljInfos[8].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.cumulativeFlow = StringUtils.isDouble(lljInfos[9]) ? Double.parseDouble(lljInfos[9].trim()) : (lljInfos[9].endsWith("N") ? Double.parseDouble(lljInfos[9].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.cumuFlowNum = StringUtils.isInt(lljInfos[10]) ? Integer.parseInt(lljInfos[10].trim()) : (lljInfos[10].endsWith("N") ? Integer.parseInt(lljInfos[10].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.scumuFlow = StringUtils.isDouble(lljInfos[11]) ? Double.parseDouble(lljInfos[11].trim()) : (lljInfos[11].endsWith("N") ? Double.parseDouble(lljInfos[11].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.scumuFlowNum = StringUtils.isInt(lljInfos[12]) ? Integer.parseInt(lljInfos[12].trim()) : (lljInfos[12].endsWith("N") ? Integer.parseInt(lljInfos[12].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.ncumuFlow = StringUtils.isDouble(lljInfos[13]) ? Double.parseDouble(lljInfos[13].trim()) : (lljInfos[13].endsWith("N") ? Double.parseDouble(lljInfos[13].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.ncumuFlowNum = StringUtils.isInt(lljInfos[14]) ? Integer.parseInt(lljInfos[14].trim()) : (lljInfos[14].endsWith("N") ? Integer.parseInt(lljInfos[14].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.abscumuFlow = StringUtils.isDouble(lljInfos[15]) ? Double.parseDouble(lljInfos[15].trim()) : (lljInfos[15].endsWith("N") ? Double.parseDouble(lljInfos[15].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.abscumuFlowNum = StringUtils.isInt(lljInfos[16]) ? Integer.parseInt(lljInfos[16].trim()) : (lljInfos[16].endsWith("N") ? Integer.parseInt(lljInfos[16].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.pitch = StringUtils.isDouble(lljInfos[17]) ? Double.parseDouble(lljInfos[17].trim()) : (lljInfos[17].endsWith("N")? Double.parseDouble(lljInfos[17].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.roll = StringUtils.isDouble(lljInfos[18]) ? Double.parseDouble(lljInfos[18].trim()) : (lljInfos[18].endsWith("N") ? Double.parseDouble(lljInfos[18].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.batV = StringUtils.isDouble(lljInfos[19]) ? Double.parseDouble(lljInfos[19].trim()) : (lljInfos[19].endsWith("N") ? Double.parseDouble(lljInfos[19].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.thickness = StringUtils.isDouble(lljInfos[20]) ? Double.parseDouble(lljInfos[20].trim()) : (lljInfos[20].endsWith("N") ? Double.parseDouble(lljInfos[20].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuLljInfo.area = StringUtils.isDouble(lljInfos[21]) ? Double.parseDouble(lljInfos[21].trim()) : (lljInfos[21].endsWith("N") ? Double.parseDouble(lljInfos[21].replaceAll(".$","")) : Constant.DefaultBigValue);
                    }
                    setRtuLljData(rtuLljInfo);
                    break;
                case Constant.Return_Read_SWJInfo:
                    String[] swjInfos = msgContext.split("#", -1);
                    rtuSWJInfo = new RtuSWJInfo();
                    if (swjInfos.length >= 22) {
                        rtuSWJInfo.samWait = StringUtils.isInt(swjInfos[0]) ? Integer.parseInt(swjInfos[0].trim()) : (swjInfos[0].endsWith("N")? Integer.parseInt(swjInfos[0].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.waterLev = StringUtils.isDouble(swjInfos[1]) ?  Double.parseDouble(swjInfos[1].trim()) : (swjInfos[1].endsWith("N") ? Double.parseDouble(swjInfos[1].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.depth = StringUtils.isDouble(swjInfos[2]) ?  Double.parseDouble(swjInfos[2].trim()) :  ( swjInfos[2].endsWith("N") ? Double.parseDouble(swjInfos[2].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.height = StringUtils.isDouble(swjInfos[3]) ? Double.parseDouble(swjInfos[3].trim()) : (swjInfos[3].endsWith("N") ? Double.parseDouble(swjInfos[3].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.direction = StringUtils.isInt(swjInfos[4]) ?  Integer.parseInt(swjInfos[4].trim()) : ( swjInfos[4].endsWith("N") ? Integer.parseInt(swjInfos[4].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.surfaceV = StringUtils.isDouble(swjInfos[5]) ?Double.parseDouble(swjInfos[5].trim()) : (swjInfos[5].endsWith("N") ? Double.parseDouble(swjInfos[5].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.sectionV = StringUtils.isDouble(swjInfos[6]) ?Double.parseDouble(swjInfos[6].trim()) : (swjInfos[6].endsWith("N") ? Double.parseDouble(swjInfos[6].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.rateFlow = StringUtils.isDouble(swjInfos[7]) ? Double.parseDouble(swjInfos[7].trim()) : (swjInfos[7].endsWith("N") ? Double.parseDouble(swjInfos[7].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.intervalFlow = StringUtils.isDouble(swjInfos[8]) ?  Double.parseDouble(swjInfos[8].trim()) : (swjInfos[8].endsWith("N") ? Double.parseDouble(swjInfos[8].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.cumulativeFlow = StringUtils.isDouble(swjInfos[9]) ? Double.parseDouble(swjInfos[9].trim()) : (swjInfos[9].endsWith("N") ? Double.parseDouble(swjInfos[9].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.cumuFlowNum = StringUtils.isInt(swjInfos[10]) ?Integer.parseInt(swjInfos[10].trim()) : (swjInfos[10].endsWith("N") ? Integer.parseInt(swjInfos[10].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.scumuFlow = StringUtils.isDouble(swjInfos[11]) ? Double.parseDouble(swjInfos[11].trim()) : (swjInfos[11].endsWith("N") ? Double.parseDouble(swjInfos[11].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.scumuFlowNum = StringUtils.isInt(swjInfos[12]) ? Integer.parseInt(swjInfos[12].trim()) : (swjInfos[12].endsWith("N") ? Integer.parseInt(swjInfos[12].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.ncumuFlow = StringUtils.isDouble(swjInfos[13]) ? Double.parseDouble(swjInfos[13].trim()) : (swjInfos[13].endsWith("N") ? Double.parseDouble(swjInfos[13].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.ncumuFlowNum = StringUtils.isInt(swjInfos[14]) ?  Integer.parseInt(swjInfos[14].trim()) : (swjInfos[14].endsWith("N") ? Integer.parseInt(swjInfos[14].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.abscumuFlow = StringUtils.isDouble(swjInfos[15]) ? Double.parseDouble(swjInfos[15].trim()) : (swjInfos[15].endsWith("N") ? Double.parseDouble(swjInfos[15].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.abscumuFlowNum = StringUtils.isInt(swjInfos[16]) ? Integer.parseInt(swjInfos[16].trim()) : (swjInfos[16].endsWith("N") ? Integer.parseInt(swjInfos[16].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.pitch = StringUtils.isDouble(swjInfos[17]) ? Double.parseDouble(swjInfos[17].trim()) : (swjInfos[17].endsWith("N")? Double.parseDouble(swjInfos[17].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.roll = StringUtils.isDouble(swjInfos[18]) ? Double.parseDouble(swjInfos[18].trim()) : (swjInfos[18].endsWith("N") ? Double.parseDouble(swjInfos[18].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.batV = StringUtils.isDouble(swjInfos[19]) ? Double.parseDouble(swjInfos[19].trim()) : (swjInfos[19].endsWith("N") ? Double.parseDouble(swjInfos[19].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.thickness = StringUtils.isDouble(swjInfos[20]) ? Double.parseDouble(swjInfos[20].trim()) : (swjInfos[20].endsWith("N") ? Double.parseDouble(swjInfos[20].replaceAll(".$","")) : Constant.DefaultBigValue);
                        rtuSWJInfo.area = StringUtils.isDouble(swjInfos[21]) ? Double.parseDouble(swjInfos[21].trim()) : (swjInfos[21].endsWith("N") ? Double.parseDouble(swjInfos[21].replaceAll(".$","")) : Constant.DefaultBigValue);

                    }
                    setRtuSWJData(rtuSWJInfo);
                    break;

                case Constant.Return_LLJ_SetZero:
                    if (msgContext != null && msgContext.contains("Set ZeroInit")) {
                        showMsg(0, " 累计流量已清空");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                sendLHBleData("Read SamInf", Constant.Param_Read_LLJInfo);
                            }
                        });

                    } else {
                        showMsg(1, "清空累计流量失败，" + msgContext);
                    }
                    break;
                case Constant.Return_LLJ_SetStart:
                    if (msgContext != null && msgContext.contains("Set Start")) {
                        showMsg(0, "调试测量开始");
                        lljIsMeasure = true;
                        tvLljTimeReads.setText("关闭调试模式");
                        RxTimerUtil.cancel();
                        RxTimerUtil.interval(3000, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                sendLHBleData("Read SamInf", Constant.Param_Read_LLJInfo);
                            }
                        });
                    } else {
                        showMsg(1, "打开调试模式失败，" + msgContext);
                    }
                    break;
                case Constant.Return_LLJ_SetStop:
                    if (msgContext != null && msgContext.contains("Set Stop")) {
                        showMsg(0, "调试模式已退出");
                        lljIsMeasure = false;
                        tvLljTimeReads.setText("打开调试模式");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(1000, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                sendLHBleData("Read SamInf", Constant.Param_Read_LLJInfo);
                            }
                        });
                    } else {
                        showMsg(1, "关闭调试模式失败，" + msgContext);
                    }
                    break;
                case Constant.Return_LLJ_Update:
                    if (msgContext != null && msgContext.contains("Set DataUp")) {
                        showMsg(0, "开始上报");
                        lljIsUpdate = true;
                        tvLljParamsUp.setText("获取上报结果");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(1000 * 10, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                sendLHBleData("Read LotInf",Constant.Param_RTU_UpdateInfo);
                            }
                        });
                    } else {
                        showMsg(1, "开始上报失败，" + msgContext);
                    }
                    break;
                case Constant.Return_RTU_UpdateInfo:
                    try {
                        String[] rtuLots = msgContext.split("#", -1);
                        if (mRtuInfo == null) mRtuInfo = new RTUInfo();
                        if (rtuLots.length >= 29) {
                            mRtuInfo.setLotType(StringUtils.isInt(rtuLots[0]) ? Integer.parseInt(rtuLots[0]) : Constant.DefaultValue);
                            mRtuInfo.setLocalIP(rtuLots[1]);
                            mRtuInfo.setNCCID(rtuLots[2]);
                            mRtuInfo.setIMSI(rtuLots[3]);
                            mRtuInfo.setLotCsq(StringUtils.isInt(rtuLots[4]) ? Integer.parseInt(rtuLots[4]) : Constant.DefaultValue);
                            mRtuInfo.setSaveNum(StringUtils.isInt(rtuLots[5]) ? Integer.parseInt(rtuLots[5]) : Constant.DefaultValue);
                            mRtuInfo.setLotState(StringUtils.isInt(rtuLots[6]) ? Integer.parseInt(rtuLots[6]) : Constant.DefaultValue);
                            mRtuInfo.setLotError(StringUtils.isInt(rtuLots[7]) ? Integer.parseInt(rtuLots[7]) : Constant.DefaultValue);
                            if (rtuLots[8] != null && rtuLots[8].length() == 12 && rtuLots[8].startsWith("2")) {
                                mRtuInfo.setUpdateEndTime(DateUtil.format2miao("20" + rtuLots[8], "yyyyMMddHHmmss"));
                            }
                            mRtuInfo.setLotStep(StringUtils.isInt(rtuLots[9]) ? Integer.parseInt(rtuLots[9]) : Constant.DefaultValue);
                            mRtuInfo.setSapInterval(StringUtils.isInt(rtuLots[10]) ? Integer.parseInt(rtuLots[10]) : Constant.DefaultValue);
                            mRtuInfo.setUpInterval(StringUtils.isInt(rtuLots[11]) ? Integer.parseInt(rtuLots[11]) : Constant.DefaultValue);
                            List<NetInfo> netInfos=new ArrayList<>();
                            int ipEns=StringUtils.isInt(rtuLots[27])?Integer.parseInt(rtuLots[27]):0;
                            mRtuInfo.setIpEns(ipEns);
                            NetInfo netInfo0=new NetInfo(0,rtuLots[12],rtuLots[13],(StringUtils.isInt(rtuLots[14])?Integer.parseInt(rtuLots[14]):Constant.DefaultValue),ipEns%2);
                            netInfo0.checkIp2En();//检查通信IP配置被清空或不正常状态
                            NetInfo netInfo1=new NetInfo(1,rtuLots[15],rtuLots[16],(StringUtils.isInt(rtuLots[17])?Integer.parseInt(rtuLots[17]):Constant.DefaultValue),ipEns>>1%2);
                            netInfo1.checkIp2En();//检查通信IP配置被清空或不正常状态
                            NetInfo netInfo2=new NetInfo(2,rtuLots[18],rtuLots[19],(StringUtils.isInt(rtuLots[20])?Integer.parseInt(rtuLots[20]):Constant.DefaultValue),ipEns>>2%2);
                            netInfo2.checkIp2En();//检查通信IP配置被清空或不正常状态
                            NetInfo netInfo3=new NetInfo(3,rtuLots[21],rtuLots[22],(StringUtils.isInt(rtuLots[23])?Integer.parseInt(rtuLots[23]):Constant.DefaultValue),ipEns>>3%2);
                            netInfo3.checkIp2En();//检查通信IP配置被清空或不正常状态
                            NetInfo netInfo4=new NetInfo(4,rtuLots[24],rtuLots[25],(StringUtils.isInt(rtuLots[26])?Integer.parseInt(rtuLots[26]):Constant.DefaultValue),ipEns>>4%2);
                            netInfo4.checkIp2En();//检查通信IP配置被清空或不正常状态
                            netInfos.add(netInfo0);
                            netInfos.add(netInfo1);
                            netInfos.add(netInfo2);
                            netInfos.add(netInfo3);
                            netInfos.add(netInfo4);
                            mRtuInfo.setNetInfoList(netInfos);
                            mRtuInfo.setDevST(StringUtils.isEmpty(rtuLots[28])?"":rtuLots[28]);
                        }
                        if (mRtuInfo.getLotError()>10){
                         /*   tvLotErr.setText(StringUtils.getLotStep(mRtuInfo.getLotError())+"故障");
                            showMsg(1,"上报失败，"+StringUtils.getLotStep(mRtuInfo.getLotError())+"故障");*/
                            if (llSWJModel.getVisibility()==View.VISIBLE){
                                swjIsUpdate = false;
                                tvSWJParamsUp.setText("采集上报");
                            }else if (llLLJModel.getVisibility()==View.VISIBLE){
                                lljIsUpdate = false;
                                tvLljParamsUp.setText("采集上报");
                            }

                            return;
                        }else if (mRtuInfo.getLotStep()>10&&mRtuInfo.getLotStep()<50){
                            if (llSWJModel.getVisibility()==View.VISIBLE){
                                swjIsUpdate = true;
                                tvSWJParamsUp.setText("获取上报结果");
                            }else if (llLLJModel.getVisibility()==View.VISIBLE){
                                lljIsUpdate = true;
                                tvLljParamsUp.setText("获取上报结果");
                            }

                            showMsg(0,"正在上报中" );
                        }else if (Math.abs(mRtuInfo.getUpdateEndTime()-System.currentTimeMillis())<5*60*1000L){
                            showMsg(0,"上报完毕，"+DateUtil.long2time4(mRtuInfo.getUpdateEndTime()) );
                            if (llSWJModel.getVisibility()==View.VISIBLE){
                                swjIsUpdate = false;
                                tvSWJParamsUp.setText("采集上报");
                            }else if  (llLLJModel.getVisibility()==View.VISIBLE){
                                lljIsUpdate = false;
                                tvLljParamsUp.setText("采集上报");
                            }

                        }
                        setRTUInfoData(1);
                    } catch (Exception e) {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        showMsg(1, "LotInf解析失败，\n" + msgContext);
                        if (llSWJModel.getVisibility()==View.VISIBLE){
                            swjIsUpdate = false;
                            tvSWJParamsUp.setText("采集上报");
                        }else if  (llLLJModel.getVisibility()==View.VISIBLE){
                            lljIsUpdate = false;
                            tvLljParamsUp.setText("采集上报");
                        }
                        e.printStackTrace();
                    }
                    break;
                case Constant.Return_SWJ_SetStart:
                    if (msgContext != null && msgContext.contains("Set Start")) {
                        showMsg(0, "测量开始");
                        swjIsMeasure = true;
                        tvSWJTimeReads.setText("停止测量");
                        RxTimerUtil.cancel();
                        RxTimerUtil.interval(3000, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                sendLHBleData("Read SamInf", Constant.Param_Read_SWJInfo);
                            }
                        });
                    } else {
                        showMsg(1, "开始测量失败，" + msgContext);
                    }
                    break;
                case Constant.Return_SWJ_SetStop:
                    if (msgContext != null && msgContext.contains("Set Stop")) {
                        showMsg(0, "测量已停止");
                        swjIsMeasure = false;
                        tvSWJTimeReads.setText("开始测量");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(1000, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                sendLHBleData("Read SamInf", Constant.Param_Read_SWJInfo);
                            }
                        });
                    } else {
                        showMsg(1, "停止测量失败，" + msgContext);
                    }
                    break;
                case Constant.Return_SWJ_Update:
                    if (msgContext != null && msgContext.contains("Set DataUp")) {
                        showMsg(0, "开始上报");
                        swjIsUpdate = true;
                        tvSWJParamsUp.setText("获取上报结果");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(1000 * 10, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                sendLHBleData("Read LotInf",Constant.Param_RTU_UpdateInfo);
                            }
                        });
                    } else {
                        showMsg(1, "开始上报失败，" + msgContext);
                    }
                    break;


                case Constant.Return_RTU_SampleInterval:
                    if (msgContext != null && msgContext.startsWith("Write SapInterval=")) {
                        showMsg(0, "设置成功");
                        tvSampleInterval.setText(Integer.parseInt(msgContext.substring(18))+"");
                    } else {
                        showMsg(1, "设置采样间隔时间失败，" + msgContext);
                    }
                    break;
                case Constant.Return_RTU_UpdateInterval:
                    if (msgContext != null && msgContext.startsWith("Write UpInterval=")) {
                        showMsg(0, "设置成功");
                        tvUpdateInterval.setText(Integer.parseInt(msgContext.substring(17))+"");
                    } else {
                        showMsg(1, "设置上传间隔时间失败，" + msgContext);
                    }
                    break;
                case Constant.Return_RTU_Set_IP:
                    if (msgContext!=null&& msgContext.startsWith("Write DevIp=")){
                       if (addNetInfo!=null&&addNetInfo.getStrPort5()!=null&&addNetInfo.getStrPort5().length()==5){
                           sendLHBleData("Write DevPort="+StringUtils.formatString(addNetInfo.getNo(),2)+"#"+addNetInfo.getStrPort5(),Constant.Param_RTU_Set_Port);
                           addNetInfo.setStrPort5("");
                       }else if ("000000000000".equals(msgContext.substring(12))){
                           showMsg(0,"通信服务删除完成");
                           refresh(1);
                       }else {
                           showMsg(0,"通信服务IP地址配置成功");
                           refresh(1);
                       }
                    }else {
                        showMsg(1,"设置通信服务IP地址失败");
                        refresh(1);
                    }
                    break;
                case Constant.Return_RTU_Set_Port:
                    if (msgContext!=null&& msgContext.startsWith("Write DevPort=")){
                        if (addNetInfo!=null&&addNetInfo.getProtocolType()>=0){
                            sendLHBleData("Write Protocol="+StringUtils.formatString(addNetInfo.getNo(),2)+"#"+addNetInfo.getProtocolType(),Constant.Param_RTU_Set_Protocol);
                            addNetInfo.setProtocolType(Constant.DefaultValue);
                        }else {
                            showMsg(0,"通信服务端口配置成功");
                            refresh(1);
                        }
                    }else {
                        showMsg(1,"设置通信服务端口失败");
                    }
                    break;
                case Constant.Return_RTU_Set_Protocol:
                    if (msgContext!=null&& msgContext.startsWith("Write Protocol=")){
                        if (addNetInfo!=null&&addNetInfo.getEn()>=0){
                            if (mRtuInfo==null){
                                showMsg(1,"未启用通信服务"+(addNetInfo.getNo()+1));
                                refresh(1);
                                return;
                            }
                            int ipEns=mRtuInfo.getIpEns();
                            byte[] bytes=StringUtils.getDateBit16(ipEns);
                            if (addNetInfo.getEn()==0){
                                bytes[addNetInfo.getNo()]=0;
                            }else if (addNetInfo.getEn()==1){
                                bytes[addNetInfo.getNo()]=1;
                            }
                            ipEns=StringUtils.bit162Int(bytes);
                            sendLHBleData("Write LotIpEn="+StringUtils.formatString(ipEns,3),Constant.Param_RTU_Set_LotIpEn);
                            addNetInfo.setEn(Constant.DefaultValue);
                        }else {
                            showMsg(0,"通信服务端口配置成功");
                            refresh(1);
                        }
                    }else {
                        showMsg(1,"设置通信服务端口失败");
                    }
                    break;
                case Constant.Return_RTU_Set_LotIpEn:
                    if (msgContext!=null&& msgContext.startsWith("Write LotIpEn=")){
                        if (addNetInfo!=null ){
                            showMsg(0,"通信服务"+(addNetInfo.getNo()+1)+"配置成功");
                            refresh(1);
                        }else {
                            showMsg(0,"通信服务端口配置成功");
                            refresh(1);
                        }
                    }else {
                        showMsg(1,"设置通信服务端口失败");
                    }
                    break;
                case Constant.Return_Write_DEV_Mac_Address:
                    if (msgContext != null && msgContext.contains("Write BleAddr=")) {
                        showMsg(0,HJBleApplication.getAppContext().getString(R.string.set_ble_mac_success));
                    } else {
                        showMsg(1,HJBleApplication.getAppContext().getString(R.string.set_fail)+",\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_RTU_DEVMN:
                    if (msgContext!=null&& msgContext.startsWith("Write DevMN=")){
                        showMsg(0,"设备MN号配置成功");
                        tvDevMN.setText(msgContext.substring(12));
                    }else {
                        showMsg(1,"设备MN号配置失败");
                    }
                    break;
                case Constant.Return_RTU_ST:
                    if (msgContext!=null&& msgContext.startsWith("Write DevST=")){
                        showMsg(0,"系统编码配置成功");
                        tvSYSST.setText(msgContext.substring(12));
                    }else {
                        showMsg(1,"系统编码配置失败");
                    }
                    break;
                case Constant.Return_RTU_Set_LotType:
                    if (msgContext!=null&& msgContext.startsWith("Write LotType=")){
                        showMsg(0,"通信模组配置成功");
                        tvLotType.setText(StringUtils.getRtuLotType(Integer.parseInt(msgContext.substring(14,15))));
                    }else {
                        showMsg(1,"通信模组配置失败");
                    }
                    break;
                case Constant.Return_RTU_Clean_NetInfo:
                    if (msgContext!=null&& msgContext.startsWith("Write LotIpEn=")){
                        showMsg(0,"正在关闭目标通信服务");
                        sendLHBleData("Write DevIp="+StringUtils.formatString(currentNetInfo.getNo(),2)+"#000000000000",Constant.Param_RTU_Set_IP);
                        currentNetInfo=null;
                    }else {
                        showMsg(1,"通信服务删除失败");
                        refresh(1);
                    }
                    break;
                case Constant.Return_RTU_Set_SamDevType:
                    if (msgContext!=null&& msgContext.startsWith("Write SamdevType=")){
                        showMsg(0,"设置连接设备成功");
                        refresh(0);
                    }else {
                        showMsg(1,"设置连接设备失败，"+msgContext);
                        refresh(0);
                    }
                    break;

                case Constant.Return_RTU_LLJ_SamWait:
                    if (msgContext!=null&&msgContext.startsWith("Write SamWait=")){
                        showMsg(0,"上电延时设置成功");
                    }else {
                        showMsg(1,"设置连接设备失败，"+msgContext);
                    }
                    RxTimerUtil.cancel();
                    RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                        @Override
                        public void doNext(long number) {
                            sendLHBleData("Read SamInf", Constant.Param_Read_LLJInfo);
                        }
                    });
                    break;
                case Constant.Return_RTU_SWJ_SamWait:
                    if (msgContext!=null&&msgContext.startsWith("Write SamWait=")){
                        showMsg(0,"上电延时设置成功");
                    }else {
                        showMsg(1,"设置连接设备失败，"+msgContext);
                    }
                    RxTimerUtil.cancel();
                    RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                        @Override
                        public void doNext(long number) {
                            sendLHBleData("Read SamInf", Constant.Param_Read_SWJInfo);
                        }
                    });
                    break;

                case Constant.Return_RTU_Set_Start:
                    if (msgContext!=null&&msgContext.equals("Set Start")){
                        showMsg(0,"开启成功");
                    }else {
                        showMsg(1,"开启失败，"+msgContext);
                    }
                    refresh(0);
                    break;
                case Constant.Return_RTU_Set_Stop:
                    if (msgContext!=null&&msgContext.equals("Set Stop")){
                        showMsg(0,"关闭成功");
                    }else {
                        showMsg(1,"关闭失败，"+msgContext);
                    }
                    refresh(0);
                    break;
                case Constant.Return_RTU_PwrEn:
                    if (msgContext!=null&&msgContext.startsWith("Write PwrEn=1")){
                        showMsg(0,"长供电模式开启成功");
                        ivPwrEn.setSelected(true);
                    }else if (msgContext!=null&&msgContext.startsWith("Write PwrEn=0")){
                        showMsg(0,"长供电模式关闭成功");
                        ivPwrEn.setSelected(false);
                    }else {
                        showMsg(1,"设置失败，"+msgContext);
                    }
                    refresh(0);
                    break;

                case Constant.Return_Ble_ReceiveFalse:
                    if (loadingDialog != null) loadingDialog.dismiss();
                    showMsg(1, msgContext);
                    break;
                case Constant.Return_BLE_Continue:
                    break;
                case Constant.Return_BLE_False:
                    if (loadingDialog != null) loadingDialog.dismiss();
                    showMsg(msgContext.contains("成功") ? 0 : msgContext.contains("失败") ? 1 : 2, msgContext);
                    break;
            }


        } else if (arg1 == 2) {//日志信息
            String msgContext = (String) obj;
            if (arg2 == Constant.Return_Ble_ConnectStop) {
                SpannableStringBuilder builder = new SpannableStringBuilder(msgContext);
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            } else if (arg2 == Constant.Return_Ble_ConnectState) {
                SpannableStringBuilder builder = new SpannableStringBuilder(msgContext);
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                if (loadingDialog != null) loadingDialog.dismiss();
                showMsg(1, msgContext);
            } else {
                SpannableStringBuilder builder = new SpannableStringBuilder(msgContext);
                //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLACK);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }
    }

    /**
     * 延时500毫秒后刷新数据
     * @param type 0-读取RTU设备基本信息 1-读取RTU网络相关配置
     */
    private void refresh(int type) {
      if (type==0){
        RxTimerUtil.cancel();
        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                sendLHBleData("Read RtuInf", Constant.Param_Read_RTUInfo);
            }
        });
      }else if (type==1){
          RxTimerUtil.cancel();
          RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
              @Override
              public void doNext(long number) {
                  sendLHBleData("Read LotInf", Constant.Param_Read_RTULot);
              }
          });

      }
    }

    private void setRtuLljData(RtuLljInfo rtuLljInfo) {
        tvLLJSamWait.setText(rtuLljInfo.samWait > Constant.DefaultValue ? rtuLljInfo.samWait + "秒" : "");
        tvLljWaterLev.setText(rtuLljInfo.waterLev > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.waterLev, 3) + "" : "");
        tvLljPitch.setText(rtuLljInfo.pitch > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.pitch, 3) + "°" : "");
        tvLljRoll.setText(rtuLljInfo.roll > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.roll, 3) + "°" : "");
        tvLljHeight.setText(rtuLljInfo.height > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.height, 3) + "" : "");
        tvLljDepth.setText(rtuLljInfo.depth > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.depth, 3) + "" : "");
        tvLljDirection.setText(StringUtils.getRtuLljDirection(rtuLljInfo.direction));
        tvLljSurfaceV.setText(rtuLljInfo.surfaceV > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.surfaceV, 3) + "" : "");
        tvLljSectionV.setText(rtuLljInfo.sectionV > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.sectionV, 3) + "" : "");
        tvLljArea.setText(rtuLljInfo.area > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.area, 3) + "" : "");
        tvLljRateFlow.setText(rtuLljInfo.rateFlow > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.rateFlow, 3) + "" : "");
        tvLljThinkNess.setText(rtuLljInfo.thickness > Constant.DefaultValue ? DoubleUtils.round(rtuLljInfo.thickness, 3) + "" : "");
        tvLljIntervalFlow.setText(rtuLljInfo.intervalFlow > Constant.DefaultValue ? String.format("%.3f",DoubleUtils.round(rtuLljInfo.intervalFlow, 3)) : "");
        String cumulativeFlow = (rtuLljInfo.cumuFlowNum > 0 ? rtuLljInfo.cumuFlowNum + "*10^7+" : "") + (rtuLljInfo.cumulativeFlow > Constant.DefaultValue ? String.format("%.3f",DoubleUtils.round(rtuLljInfo.cumulativeFlow, 3)) : "");
        tvLljCumulativeFlow.setText(cumulativeFlow);
        String scumuFlow = (rtuLljInfo.scumuFlowNum > 0 ? rtuLljInfo.scumuFlowNum + "*10^7+" : "") + (rtuLljInfo.scumuFlow > Constant.DefaultValue ? String.format("%.3f",DoubleUtils.round(rtuLljInfo.scumuFlow, 3)) : "");
        tvLljScumuFlow.setText(scumuFlow);
        String ncumuFlow = (rtuLljInfo.ncumuFlowNum > 0 ? rtuLljInfo.ncumuFlowNum + "*10^7+" : "") + (rtuLljInfo.ncumuFlow > Constant.DefaultValue ? String.format("%.3f",DoubleUtils.round(rtuLljInfo.ncumuFlow, 3)): "");
        tvLljNcumuFlow.setText(ncumuFlow);
        String abscumuFlow = (rtuLljInfo.abscumuFlowNum > 0 ? rtuLljInfo.abscumuFlowNum + "*10^7+" : "") + (rtuLljInfo.abscumuFlow > Constant.DefaultValue ? String.format("%.3f",DoubleUtils.round(rtuLljInfo.abscumuFlow, 3)) : "");
        tvLljAbscumuFlow.setText(abscumuFlow);
    }

    private void setRtuSWJData(RtuSWJInfo rtuSWJInfo) {
        tvSWJSamWait.setText(rtuSWJInfo.samWait > Constant.DefaultValue ? rtuSWJInfo.samWait + "秒" : "");
        tvSWJWaterLev.setText(rtuSWJInfo.waterLev > Constant.DefaultValue ? DoubleUtils.round(rtuSWJInfo.waterLev, 3) + "" : "");
        tvSWJPitch.setText(rtuSWJInfo.pitch > Constant.DefaultValue ? DoubleUtils.round(rtuSWJInfo.pitch, 3) + "°" : "");
        tvSWJHeight.setText(rtuSWJInfo.height > Constant.DefaultValue ? DoubleUtils.round(rtuSWJInfo.height, 3) + "" : "");
        tvSWJDepth.setText(rtuSWJInfo.depth > Constant.DefaultValue ? DoubleUtils.round(rtuSWJInfo.depth, 3) + "" : "");
        tvSWJRateFlow.setText(rtuSWJInfo.rateFlow > Constant.DefaultValue ? DoubleUtils.round(rtuSWJInfo.rateFlow, 3) + "" : "");
        tvSWJIntervalFlow.setText(rtuSWJInfo.intervalFlow > Constant.DefaultValue ? DoubleUtils.round(rtuSWJInfo.intervalFlow, 3) + "" : "");
        String cumulativeFlow = (rtuSWJInfo.cumuFlowNum > 0 ? rtuSWJInfo.cumuFlowNum + "*10^7+" : "") + (rtuSWJInfo.cumulativeFlow > Constant.DefaultValue ? DoubleUtils.round(rtuSWJInfo.cumulativeFlow, 3) + "" : "");
        tvSWJCumulativeFlow.setText(cumulativeFlow);
    }

    /**
     * 设置RTU显示信息
     *
     * @param resourceType 0-只有RTU设备信息 Read RtuInf数据源 1-只有RTU网络信息 Read LotInf
     */
    private void setRTUInfoData(int resourceType) {
        if (mRtuInfo == null) {
            return;
        } else if (resourceType == 0) {
            if (mRtuInfo.getSamDevType() == -1) {
                showMsg(2, "未读取到当前连接设备信息");
                tvDevSamdevtype.setText("");
            } else {
                tvSWJInput.setBackgroundResource(StringUtils.getDateBit16(mRtuInfo.getSamDevType())[0] == 1 ? R.drawable.shape_background_orange_color_radiu_15dp: R.drawable.shape_background_gray_color_radiu_15dp);
                tvLLJInput.setBackgroundResource(StringUtils.getDateBit16(mRtuInfo.getSamDevType())[1] == 1 ? R.drawable.shape_background_app_color_radiu_15dp : R.drawable.shape_background_gray_color_radiu_15dp);
                tvDevSamdevtype.setText(StringUtils.getDevSamdevtype(mRtuInfo.getSamDevType()));
            }
            if (mRtuInfo.getDevState() == -1) {
                showMsg(2, "未读取到当前RTU工作状态");
            } else if (StringUtils.getDateBit16(mRtuInfo.getDevState())[0] == 0) {
                tvDevWorkState.setText("空闲");
                ivDevOnOff.setSelected(false);
            } else if (StringUtils.getDateBit16(mRtuInfo.getDevState())[1] == 1) {
                tvDevWorkState.setText("正在采样");
                ivDevOnOff.setSelected(true);
            } else if (StringUtils.getDateBit16(mRtuInfo.getDevState())[2] == 1) {
                tvDevWorkState.setText("正在上传");
                ivDevOnOff.setSelected(true);
            } else if (StringUtils.getDateBit16(mRtuInfo.getDevState())[0] == 1) {
                tvDevWorkState.setText("定时采样");
                ivDevOnOff.setSelected(true);
            } else {
                tvDevWorkState.setText("未知状态" + mRtuInfo.getDevState());
                ivDevOnOff.setSelected(true);
            }
            tvDevVersion.setText(mRtuInfo.getVersion());
            tvSYSTime.setText(DateUtil.long2time4(mRtuInfo.getSysTime()));
            tvDevName.setText(mRtuInfo.getDevName());
            tvDevMN.setText(mRtuInfo.getDevMN());
            tvDevNum.setText(StringUtils.isEmpty(mRtuInfo.getDevNum())?"":mRtuInfo.getDevNum());
            tvMacAddress.setText(mRtuInfo.getBleMac());
            if (mRtuInfo.getBatV() <= 9.8d) {
                tvDevEv.setText(DoubleUtils.round(mRtuInfo.getBatV(), 2) + "V 0%");
            } else if (mRtuInfo.getBatV() >= 10.6d) {
                tvDevEv.setText(DoubleUtils.round(mRtuInfo.getBatV(), 2) + "V100%");
            } else {
                int precentBat = (int) DoubleUtils.div((mRtuInfo.getBatV() - 9.8d), 0.008d, 0);
                tvDevEv.setText(DoubleUtils.round(mRtuInfo.getBatV(), 2) + "V " + precentBat + "%");
            }
        }else if (resourceType==1){
            if (!StringUtils.isEmpty(mRtuInfo.getDevST())&&!"00".equals(mRtuInfo.getDevST())){
                tvSYSST.setText(mRtuInfo.getDevST());
            }else {
                tvSYSST.setText("");
            }
            tvLotType.setText(StringUtils.getRtuLotType(mRtuInfo.getLotType()));
            tvSIMCard.setText(mRtuInfo.getNCCID());
            tvSimImsi.setText(mRtuInfo.getIMSI());
            tvLotEV.setText(StringUtils.getLotLex(mRtuInfo.getLotCsq()));
            if (mRtuInfo.getLotError()>10){
            /*    tvLotErr.setText(StringUtils.getLotStep(mRtuInfo.getLotError())+"故障");
                showMsg(1,StringUtils.getLotStep(mRtuInfo.getLotError())+"故障");*/
            }
            tvSampleInterval.setText(mRtuInfo.getSapInterval()>=0?mRtuInfo.getSapInterval()+"分钟":"");
            tvUpdateInterval.setText(mRtuInfo.getUpInterval()>=0?mRtuInfo.getUpInterval()+"分钟":"");
            tvLotStep.setText(StringUtils.getLotStep(mRtuInfo.getLotStep()));
            tvDataCount.setText(mRtuInfo.getSaveNum()>0?mRtuInfo.getSaveNum()+"条":"");
            tvUpdateEndTime.setText(mRtuInfo.getUpdateEndTime()>0?DateUtil.long2time4(mRtuInfo.getUpdateEndTime()):"");
            llNet0.setVisibility(View.VISIBLE);
            llNet1.setVisibility(View.GONE);
            llNet2.setVisibility(View.GONE);
            llNet3.setVisibility(View.GONE);
            llNet4.setVisibility(View.GONE);
            if (mRtuInfo.getNetInfoList().size()>=5){
                Collections.sort(mRtuInfo.getNetInfoList());
                if (mRtuInfo.getNetInfoList().get(0).getEn()>=0){
                    NetInfo netInfo0=mRtuInfo.getNetInfoList().get(0);
                    tvDevIp0.setText(StringUtils.getIPAddress(netInfo0.getStrIp12()));
                    tvNetPort0.setText(StringUtils.isInt(netInfo0.getStrPort5())?Integer.parseInt(netInfo0.getStrPort5())+"":"");
                    tvProtocolType0.setText(StringUtils.getRtuProtocolType(netInfo0.getProtocolType()));
                    tvNetIsEn0.setText(netInfo0.getEn()==1?"已启用":"未启用");
                }else {
                    tvDevIp0.setText("");
                    tvNetPort0.setText("");
                    tvProtocolType0.setText("");
                    tvNetIsEn0.setText("");
                }
                if (mRtuInfo.getNetInfoList().get(1).getEn()>=0){
                    NetInfo netInfo=mRtuInfo.getNetInfoList().get(1);
                    llNet1.setVisibility(View.VISIBLE);
                    tvDevIp1.setText(StringUtils.getIPAddress(netInfo.getStrIp12()));
                    tvNetPort1.setText(StringUtils.isInt(netInfo.getStrPort5())?Integer.parseInt(netInfo.getStrPort5())+"":"");
                    tvProtocolType1.setText(StringUtils.getRtuProtocolType(netInfo.getProtocolType()));
                    tvNetIsEn1.setText(netInfo.getEn()==1?"已启用":"未启用");
                }
                if (mRtuInfo.getNetInfoList().get(2).getEn()>=0){
                    NetInfo netInfo=mRtuInfo.getNetInfoList().get(2);
                    llNet2.setVisibility(View.VISIBLE);
                    tvDevIp2.setText(StringUtils.getIPAddress(netInfo.getStrIp12()));
                    tvNetPort2.setText(StringUtils.isInt(netInfo.getStrPort5())?Integer.parseInt(netInfo.getStrPort5())+"":"");
                    tvProtocolType2.setText(StringUtils.getRtuProtocolType(netInfo.getProtocolType()));
                    tvNetIsEn2.setText(netInfo.getEn()==1?"已启用":"未启用");
                }
                if (mRtuInfo.getNetInfoList().get(3).getEn()>=0){
                    NetInfo netInfo=mRtuInfo.getNetInfoList().get(3);
                    llNet3.setVisibility(View.VISIBLE);
                    tvDevIp3.setText(StringUtils.getIPAddress(netInfo.getStrIp12()));
                    tvNetPort3.setText(StringUtils.isInt(netInfo.getStrPort5())?Integer.parseInt(netInfo.getStrPort5())+"":"");
                    tvProtocolType3.setText(StringUtils.getRtuProtocolType(netInfo.getProtocolType()));
                    tvNetIsEn3.setText(netInfo.getEn()==1?"已启用":"未启用");
                }
                if (mRtuInfo.getNetInfoList().get(4).getEn()>=0){
                    NetInfo netInfo=mRtuInfo.getNetInfoList().get(4);
                    llNet4.setVisibility(View.VISIBLE);
                    tvDevIp4.setText(StringUtils.getIPAddress(netInfo.getStrIp12()));
                    tvNetPort4.setText(StringUtils.isInt(netInfo.getStrPort5())?Integer.parseInt(netInfo.getStrPort5())+"":"");
                    tvProtocolType4.setText(StringUtils.getRtuProtocolType(netInfo.getProtocolType()));
                    tvNetIsEn4.setText(netInfo.getEn()==1?"已启用":"未启用");
                }
            }
            if (llNet1.getVisibility()==View.VISIBLE&&llNet2.getVisibility()==View.VISIBLE&&llNet3.getVisibility()==View.VISIBLE&&llNet3.getVisibility()==View.VISIBLE){
                tvNetAdd.setVisibility(View.GONE);
            }else {
                tvNetAdd.setVisibility(View.VISIBLE);
            }
            ivPwrEn.setSelected(mRtuInfo.getPwrEn()==1);
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
      //  mble.disconnectDevice();
        Log.d(TAG, "destroy");
      //  mble.disconnectLocalDevice();
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
                    } else if (boxQcode.startsWith("0D07")) {
                        String macAds = boxQcode.trim();
                        new HitiStringDailog(LiherCYXRTUActivity.this, Constant.Dailog_BLE_MACAddress, macAds, HJBleApplication.getAppContext().getString(R.string.hiti_macaddress_title), HJBleApplication.getAppContext().getString(R.string.hiti_macaddress_set_0) + StringUtils.getNewBlueToothMacAddress(macAds) + HJBleApplication.getAppContext().getString(R.string.hiti_macaddress_set_1), LiherCYXRTUActivity.this);
                    } else {
                        showMsg(2, boxQcode + "二维码，非力合采样箱RTU编码规则0D07");
                    }
                    break;
                case Constant.RequestCode_RTU_Bin:
                    if (data != null && data.getData() != null) {
                        Uri fileUri = data.getData();

                        if (fileUri != null) {
                            ContentResolver resolver = LiherCYXRTUActivity.this.getContentResolver();
                            if (fileUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {

                                ContentResolver contentResolver = LiherCYXRTUActivity.this.getContentResolver();
                                Cursor cursor = contentResolver.query(fileUri, null, null, null, null);
                                if (cursor.moveToFirst()) {
                                    String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                    if (displayName==null){
                                        displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE));
                                    }
                                    if (displayName==null){
                                        showMsg(2,"未获取到您选的文件名");
                                        return;
                                    }else if (!displayName.endsWith(".bin")){
                                        showMsg(1,displayName+",请选择bin文件");
                                        return;
                                    }else {
                                        showMsg(0,"选中文件："+displayName);
                                        InputStream r=null;
                                        try {
                                            r = resolver.openInputStream(fileUri);
                                            // 读取二进制文件内容
                                            byte[] buffer = new byte[r.available()];
                                            r.read(buffer);
                                            // 将二进制数据转换为十六进制字符串
                                            String hexStr =new String(buffer);
                                            sendLHBleData(hexStr, Constant.PARAM_LIHER_ORDER);
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                            showMsg(2,"文件未找到");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            showMsg(2,"文件读取失败");
                                        } finally {
                                            if (r != null) {
                                                try {
                                                    r.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                }
                            }


                            return;
                        } else {
                                showMsg(2,"未识别出文件名称！");
                                return;
                            }
                    } else {
                        showMsg(2,"请选择设备RTU协议文件(.bin)");
                    }
                    break;

            }
        }
    }


}
