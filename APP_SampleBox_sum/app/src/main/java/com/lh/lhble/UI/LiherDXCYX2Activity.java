package com.lh.lhble.UI;

import static com.lh.lhble.base.Constant.Return_DXCYX2_DBWCS;
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
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.BoxSamplePlan;
import com.lh.lhble.Entity.DX2SampleBox;
import com.lh.lhble.Entity.DbParamData;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.Entity.SampleBottle;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DlgTime;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.UI.View.BoxSamplePlanDailog;
import com.lh.lhble.UI.View.Dxcyx2DBDatasDailog;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.UI.View.SelecterObjectDailog;
import com.lh.lhble.UI.View.TimeModeDelayDailog;
import com.lh.lhble.adapter.SampleBottleAdapter;
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
import com.wise.wisekit.activity.BaseFragmentActivity;
import com.wise.wisekit.dialog.LoadingDialog;
import com.wise.wisekit.utils.DateUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 0D02 天津地下水采样箱
 */
public class LiherDXCYX2Activity extends BaseFragmentActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherDXCYX2Activity.class.getSimpleName();

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
    private String sampleId = "";
    private LinearLayout llUIMode;
    private TextView tvRefresh;
    private TextView tvDevName, tvDevNum, tvDevVersion, tvSysTime, tvDevIP, tvMacAddress, tvSetMacAddress;
    private TextView tvSampleMode, tvWorkStep, tvWorkPlan, tvRequestMsgCycle, tvWorkStatus, tvSampleNextTime, tvDevEV, tvJyWarningSec, tvJyThreshold, tvWashTime, tvPulseSpeed, tvWaterPressure,
            tvSampleStatue, tvParamsData, tvParamsDBData, tvDevLog, tvSampleStart, tvSampleEnd, tvPyTime, tvDevRiss, tvInternetCID,tvBaseWaterPressure,tvWaterStatus,tvPulseStatus,tvWaterLLJ;
    private ImageView ivParamsDataFresh, ivParamsDBDataFresh;
    private Switch swAlarmState, swGPSSearch, swJudgeSearch;
    private TextView tvDevReset, tvPulseOpen, tvPulseClose;
    private LinearLayout llOrderMode, llWorkPlan, llRequestMsgCycle, llSampleNextTime, llWorkStep;
    private ScrollView srlUIMode, sclOrders;
    private LinearLayout llOrders;
    private EditText sendData;
    private TextView send;

    private SwipeRefreshLayout srlSampleBottles;
    private ListView lvSampleBottles;
    private SampleBottleAdapter mAdapter;
    private LoadingDialog loadingDialog = null;
    private DX2SampleBox sampleBox;
    private List<SampleBottle> sampleBottleList;
    private SampleBottle currentSampleBottle;
    private BoxSamplePlanDailog boxSamplePlanDailog;
    private List<BoxSamplePlan> samplePlanList;
    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;

    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private int currentSendType, currentReceiveType;
    private boolean alarmState, gpsSearch, judgeSearch;
    private String tempBottleMac;
    private HitiInputStrDailog sampleNoDialog;
    private ArrayList<String> devLogs;
    private List<DbParamData> dbParamDataList;
    private int isBack;//是否因蓝牙中断回退页面 1-回退
    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_dxcyx2;
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
        setRightText("交互模式");
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectUIModeMenu();
            }
        });

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
                sendLHBleData("Read Detail", Constant.Param_Read_Detail);
                showMsg(2, "正在初始化...");
            }
        });
        //addLogText("SW1=00,Temp1=26.2,Turb1=58.7,pH1=3.51,EC1=7,DO1=359.51,SW2=00,Temp2=26.1,Turb2=58.9,pH2=3.51,EC2=7,DO2=359.89,SW3=00,Temp3=26.1,Turb3=59.1,pH3=3.51,EC3=7,DO3=360.17,SJ1=240824102513,SJ2=240824103043,SJ3=240824103613,DBflag=011111,ZT=3", 1, Return_DXCYX2_DBWCS);
    }

    private void refreshDetails() {
        showMsg(2, "正在重新加载采样箱信息...");
        sendLHBleData("Read Detail", Constant.Param_Read_Detail);
    }

    private void refreshBottleList() {
        showMsg(2, "正在加载瓶位信息...");
        sampleBottleList = new ArrayList<>();
        sendLHBleData("Read Subaddr", Constant.Param_Read_Subaddr);
    }


    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherDXCYX2Activity.class);
        intent.putExtra(LiherDXCYX2Activity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherDXCYX2Activity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherDXCYX2Activity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();
        llUIMode = findViewById(R.id.ll_ui_mode);
        tvRefresh = findViewById(R.id.tv_refresh);
        tvDevName = findViewById(R.id.tv_dev_name);
        tvDevNum = findViewById(R.id.tv_dev_num);
        tvDevVersion = findViewById(R.id.tv_dev_version);
        tvSysTime = findViewById(R.id.tv_sys_time);
        tvDevIP = findViewById(R.id.tv_dev_ip);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);
        tvSampleMode = findViewById(R.id.tv_sample_mode);
        tvWorkStep = findViewById(R.id.tv_work_step);
        tvWorkPlan = findViewById(R.id.tv_work_plan);

        tvRequestMsgCycle = findViewById(R.id.tv_request_msg_cycle);
        tvWorkStatus = findViewById(R.id.tv_work_status);
        tvSampleNextTime = findViewById(R.id.tv_sample_next_time);
        tvDevEV = findViewById(R.id.tv_dev_ev);
        tvJyWarningSec = findViewById(R.id.tv_jy_warning_sec);
        tvWashTime = findViewById(R.id.tv_wash_time);
        tvPulseSpeed = findViewById(R.id.tv_pulse_speed);
        tvWaterPressure = findViewById(R.id.tv_water_pressure);
        tvBaseWaterPressure=findViewById(R.id.tv_base_water_pressure);
        tvWaterStatus=findViewById(R.id.tv_water_status);
        tvPulseStatus=findViewById(R.id.tv_pulse_status);
        tvJyThreshold = findViewById(R.id.tv_jy_threshold);
        tvPyTime = findViewById(R.id.tv_py_time);
        tvDevRiss = findViewById(R.id.tv_dev_riss);
        tvInternetCID = findViewById(R.id.tv_internet_cid);
        tvWaterLLJ=findViewById(R.id.tv_water_llj);
        tvSampleStatue = findViewById(R.id.tv_sample_statue);
        tvParamsData = findViewById(R.id.tv_params_data);
        tvParamsDBData = findViewById(R.id.tv_params_db_data);
        tvDevLog = findViewById(R.id.tv_dev_log);
        tvSampleStart = findViewById(R.id.tv_sample_start);
        tvSampleEnd = findViewById(R.id.tv_sample_end);
        ivParamsDataFresh = findViewById(R.id.iv_params_data_refresh);
        ivParamsDBDataFresh = findViewById(R.id.iv_params_db_data_refresh);
        swAlarmState = findViewById(R.id.sw_alarm_state);
        swGPSSearch = findViewById(R.id.sw_gps_search);
        swJudgeSearch = findViewById(R.id.sw_pressure_search);
        tvDevReset = findViewById(R.id.tv_dev_reset);

        tvPulseOpen = findViewById(R.id.tv_pulse_open);
        tvPulseClose = findViewById(R.id.tv_pulse_close);
        llOrderMode = findViewById(R.id.ll_order_mode);

        llWorkPlan = findViewById(R.id.ll_work_plan);

        llRequestMsgCycle = findViewById(R.id.ll_request_msg_cycle);
        llSampleNextTime = findViewById(R.id.ll_sample_next_time);
        llWorkStep = findViewById(R.id.ll_work_step);
        srlUIMode = findViewById(R.id.srl_ui_mode);
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        sendData = findViewById(R.id.sendData);
        send = findViewById(R.id.send);
        srlSampleBottles = findViewById(R.id.srl_sample_bottles);
        lvSampleBottles = findViewById(R.id.lv_sample_bottles);
        sampleBottleList = new ArrayList<>();
        mAdapter = new SampleBottleAdapter(LiherDXCYX2Activity.this);
        lvSampleBottles.setAdapter(mAdapter);
        tvRefresh.setOnClickListener(LiherDXCYX2Activity.this);
        tvSysTime.setOnClickListener(LiherDXCYX2Activity.this);
        tvSetMacAddress.setOnClickListener(LiherDXCYX2Activity.this);
        tvSampleMode.setOnClickListener(LiherDXCYX2Activity.this);
        tvWorkPlan.setOnClickListener(LiherDXCYX2Activity.this);

        tvRequestMsgCycle.setOnClickListener(LiherDXCYX2Activity.this);
        tvJyWarningSec.setOnClickListener(LiherDXCYX2Activity.this);
        tvWashTime.setOnClickListener(LiherDXCYX2Activity.this);
        tvPulseSpeed.setOnClickListener(LiherDXCYX2Activity.this);
        tvJyThreshold.setOnClickListener(LiherDXCYX2Activity.this);
        tvPyTime.setOnClickListener(LiherDXCYX2Activity.this);
        tvInternetCID.setOnClickListener(LiherDXCYX2Activity.this);
        ivParamsDataFresh.setOnClickListener(LiherDXCYX2Activity.this);
        ivParamsDBDataFresh.setOnClickListener(LiherDXCYX2Activity.this);
        tvSampleStatue.setOnClickListener(LiherDXCYX2Activity.this);
        tvDevLog.setOnClickListener(LiherDXCYX2Activity.this);
        tvSampleStart.setOnClickListener(LiherDXCYX2Activity.this);
        tvSampleEnd.setOnClickListener(LiherDXCYX2Activity.this);
        tvDevReset.setOnClickListener(LiherDXCYX2Activity.this);

        tvPulseOpen.setOnClickListener(LiherDXCYX2Activity.this);
        tvPulseClose.setOnClickListener(LiherDXCYX2Activity.this);
        send.setOnClickListener(LiherDXCYX2Activity.this);
        swAlarmState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked != alarmState) {
                    sendLHBleData("Write Alarm=" + (isChecked ? 1 : 0), Constant.Param_Wirte_Alarm);
                }
            }
        });
        swGPSSearch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked != gpsSearch) {
                    sendLHBleData("Write gnss=" + (isChecked ? 1 : 0), Constant.Param_Wirte_Gnss);
                }
            }
        });
        swJudgeSearch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
               if (isChecked != judgeSearch) {
                   if (sampleBox != null && (sampleBox.getStartFlag() == 1 || sampleBox.getSampleStatus() >0)) {
                       showMsg(1, "地下采样箱正在流程工作中，请勿修改水位控制状态！");
                       refreshDetails();
                       return;
                   }else{
                       sendLHBleData("Write judge=" + (isChecked ? 1 : 0), Constant.Param_Write_Judge);
                   }

                }
            }
        });
        sampleBox = new DX2SampleBox();
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
        lvSampleBottles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (sampleBottleList != null && sampleBottleList.size() > position) {
                    SampleBottle sampleBottle = sampleBottleList.get(position);
                    List<JSONObject> jat = new ArrayList<>();
                    if (sampleBottle.getBlueMac() == null || "000000000000".equals(sampleBottle.getBlueMac().trim()) || sampleBottle.getBlueMac().trim().length() != 12) {
                      /*  JSONObject jo0 = new JSONObject();
                        jo0.put("MyName", "装瓶");
                        jo0.put("MyObject", sampleBottle);
                        jat.add(jo0);*/
                        currentSampleBottle =sampleBottle;
                        if (currentSampleBottle.getBottleNo() < 1) {
                            refreshBottleList();
                            return;
                        }
                        startActivityForResult(new Intent(LiherDXCYX2Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);
                        return;
                    } else {
                        JSONObject jo1 = new JSONObject();
                        jo1.put("MyName", "卸瓶");
                        jo1.put("MyObject", sampleBottle);
                        jat.add(jo1);
                        JSONObject jo3 = new JSONObject();
                        jo3.put("MyName", "采样");
                        jo3.put("MyObject", sampleBottle);
                        jat.add(jo3);
                    }
                    if (sampleBottle.getIsLock() > 0) {
                        JSONObject jo2 = new JSONObject();
                        jo2.put("MyName", "启用");
                        jo2.put("MyObject", sampleBottle);
                        jat.add(jo2);
                    } else {
                        JSONObject jo2 = new JSONObject();
                        jo2.put("MyName", "禁用");
                        jo2.put("MyObject", sampleBottle);
                        jat.add(jo2);
                     /*   JSONObject jo4 = new JSONObject();
                        jo4.put("MyName", "留样");
                        jo4.put("MyObject", sampleBottle);
                        jat.add(jo4);*/
                    }
                    new SelecterObjectDailog(LiherDXCYX2Activity.this, sampleBottle.getBottleNo() + "号瓶操作指令", jat, Constant.Dailog_Select_SampleBottle_Action, LiherDXCYX2Activity.this);

                }
            }
        });
        srlSampleBottles.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                srlSampleBottles.setRefreshing(false);
                refreshBottleList();
            }
        });
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


        if (!StringUtils.isEmpty(mDeviceName)) tvDevName.setText(mDeviceName);
        if (!StringUtils.isEmpty(mDeviceAddress)) tvMacAddress.setText(mDeviceAddress);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    //发送数据
    public void sendLHBleData(String sendString, int sendType) {
        byte[] tmpBytes;
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2, "发送数据内容不能为空");
            return;
        }

        tmpBytes = ProtocolUtil.getBlueToothMsgForSend(HJBleApplication.shareInstance().getDesID(), sendString);

        if (tmpBytes == null) {
            return;
        } else if (mble == null || !mble.isConnect()) {
            showMsg(1, "蓝牙连接已中断，请返回扫描连接后重试");
            if (isBack<1){
                isBack++;
                return;
            }else {
                finish();
            }
        }
        isBack=0;
        addLogText(sendString, 0, sendType);
        insertLog(mDeviceAddress, sampleId, sendString);
        final byte[] bytes = tmpBytes;
        currentSendType = sendType;
        if (LiherDXCYX2Activity.this.isFinishing()) {
            return;
        }
        loadingDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mSendCharact, bytes)) {
                    addLogText("发送失败！", 0,Constant.Return_BLE_False);
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
              //  if (StringUtils.isEmpty(tempBottleMac)) finish();
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
                        case Constant.Param_Read_Subaddr:
                            currentReceiveType = Constant.Return_Read_Subaddr;
                            break;
                        case Constant.Param_Wirte_Alarm:
                            currentReceiveType = Constant.Return_Wirte_Alarm;
                            break;
                        case Constant.Param_Wirte_Mode:
                            currentReceiveType = Constant.Return_Wirte_Mode;
                            break;
                        case Constant.Param_Wirte_SamPeriod:
                            currentReceiveType = Constant.Return_Wirte_SamPeriod;
                            break;
                        case Constant.Param_Write_Subaddr:
                            currentReceiveType = Constant.Return_Write_Subaddr;
                            break;
                        case Constant.Param_Write_Sampling_Bottle:
                            currentReceiveType = Constant.Return_Write_Sampling_Bottle;
                            break;
                        case Constant.Param_Write_Banned:
                            currentReceiveType = Constant.Return_Write_Banned;
                            break;
                        case Constant.Param_DEV_Reset:
                            currentReceiveType = Constant.Return_DEV_Reset;
                            break;

                        case Constant.Param_Pulse_Open:
                            currentReceiveType = Constant.Return_Pulse_Open;
                            break;
                        case Constant.Param_Pulse_Close:
                            currentReceiveType = Constant.Return_Pulse_Close;
                            break;
                        case Constant.PARAM_Work_Plan:
                            currentReceiveType = Constant.RETURN_Work_Plan;
                            break;
                        case Constant.PARAM_Write_Box_Sample_Plan:
                            currentReceiveType = Constant.RETURN_Write_Box_Sample_Plan;
                            break;
                        case Constant.Param_Write_JyThreshold:
                            currentReceiveType = Constant.Return_Write_JyThreshold;
                            break;
                        case Constant.Param_Write_PyTime:
                            currentReceiveType = Constant.Return_Write_PyTIme;
                            break;
                        case Constant.Param_Write_DXBox_WashTime:
                            currentReceiveType = Constant.Return_Write_DXBox_WashTime;
                            break;
                        case Constant.Param_Write_DXBox_PulseSpeed:
                            currentReceiveType = Constant.Return_Write_DXBox_PulseSpeed;
                            break;
                        case Constant.Param_Write_DXBox_WarningSec:
                            currentReceiveType = Constant.Return_Write_DXBox_WarningSec;
                            break;
                        case Constant.Param_Write_TimeMode_Delay:
                            currentReceiveType = Constant.Return_Write_TimeMode_Delay;
                            break;
                        case Constant.Param_Wirte_SysTime:
                            currentReceiveType = Constant.Return_Wirte_SysTime;
                            break;
                        case Constant.Param_Write_Box_Mac_Address:
                            currentReceiveType = Constant.Return_Write_Box_Mac_Address;
                            break;
                        case Constant.Param_Write_RequestMsg_Cycle:
                            currentReceiveType = Constant.Return_Write_RequestMsg_Cycle;
                            break;
                        case Constant.Param_Write_Update_Type:
                            currentReceiveType = Constant.Return_Write_Update_Type;
                            break;
                        case Constant.Param_Write_Update_Cycle:
                            currentReceiveType = Constant.Return_Write_Update_Cycle;
                            break;
                        case Constant.Param_Write_SamplingBottle:
                            currentReceiveType = Constant.Return_Write_SamplingBottle;
                            break;
                        case Constant.Param_Wirte_CyNum:
                            currentReceiveType = Constant.Return_Wirte_CyNum;
                            break;
                        case Constant.Param_Read_LS:
                            currentReceiveType = Constant.Return_Read_LS;
                            break;
                        case Constant.PARAM_BLE_CloseCap:
                            currentReceiveType = Constant.Return_BLE_CloseCap;
                            break;
                        case Constant.Param_CYX_ICCID:
                            currentReceiveType = Constant.Return_CYX_ICCID;
                            break;
                        case Constant.Param_Wirte_Gnss:
                            currentReceiveType = Constant.Return_Wirte_Gnss;
                            break;
                        case Constant.Param_DXCYX2_WCS:
                            currentReceiveType = Constant.Return_DXCYX2_WCS;
                            break;
                        case Constant.Param_DXCYX2_DBWCS:
                            currentReceiveType = Return_DXCYX2_DBWCS;
                            break;
                        case Constant.Param_DXCYX2_LOG:
                            currentReceiveType = Constant.Return_DXCYX2_LOG;
                            break;
                        case Constant.Param_Write_Judge:
                            currentReceiveType = Constant.Return_Write_Judge;
                            break;
                        case Constant.PARAM_DX2_SampleStart:
                            currentReceiveType = Constant.Return_DX2_SampleStart;
                            break;
                        case Constant.PARAM_DX2_SampleStop:
                            currentReceiveType = Constant.Return_DX2_SampleStop;
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
                        showMsg(1, stateMsg);
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_set_mac_address:
                startActivityForResult(new Intent(LiherDXCYX2Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_Box_MAC_Address);
                break;
            case R.id.tv_refresh:
                refreshDetails();
                break;
            case R.id.tv_sample_mode:
                showSelectSampleModeMenu();
                break;
            case R.id.tv_sys_time:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherDXCYX2Activity.this);

                break;
            case R.id.tv_work_times:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_BLE_CycTime, "等时采样周期", "等时周期（分钟）:", "", LiherDXCYX2Activity.this);
                break;
            case R.id.tv_dev_reset:
                sendLHBleData("ln", Constant.Param_DEV_Reset);
                break;
            case R.id.tv_pulse_open:
                if (sampleBox != null && (sampleBox.getStartFlag() == 1 || sampleBox.getSampleStatus() >0)) {
                    showMsg(1, "地下采样箱正在流程工作中，请勿控制气囊泵！");
                    refreshDetails();
                    return;
                }
                sendLHBleData("lk", Constant.Param_Pulse_Open);
                break;
            case R.id.tv_pulse_close:
                if (sampleBox != null && (sampleBox.getStartFlag() == 1 || sampleBox.getSampleStatus()>0)) {
                    showMsg(2, "地下采样箱正在流程工作中，请勿控制气囊泵！");
                    refreshDetails();
                    return;
                }
                sendLHBleData("lb", Constant.Param_Pulse_Close);
                break;

            case R.id.tv_wash_time:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dialog_DXBox_WashTime, "设置洗井时长", "洗井时长(秒):", "", LiherDXCYX2Activity.this);

                break;
            case R.id.tv_pulse_speed:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dialog_DXBox_PulseSpeed, "气囊泵启停间隔时间", "间隔时间(秒):", "", LiherDXCYX2Activity.this);

                break;
            case R.id.tv_jy_warning_sec:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dialog_DXBox_WarningSec, "设置洗进超时时长", "洗井超时(分钟):", "", LiherDXCYX2Activity.this);

                break;

            case R.id.tv_jy_threshold:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_Box_JyThreshold, "每次采样的进样阀值", "进样阀值(脉冲):", "", LiherDXCYX2Activity.this);
                break;
            case R.id.tv_py_time:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_Box_PyTime, "每次排空水样的时长", "排样时长(秒):", "", LiherDXCYX2Activity.this);
                break;
            case R.id.tv_internet_cid:
                sendLHBleData("Read CID", Constant.Param_CYX_ICCID);
                break;
            case R.id.iv_params_data_refresh:
                sendLHBleData("Read wcs", Constant.Param_DXCYX2_WCS);
                break;
            case R.id.iv_params_db_data_refresh:
                sendLHBleData("Read dbw", Constant.Param_DXCYX2_DBWCS);
                break;
            case R.id.tv_sample_statue:
                refreshDetails();
                break;
            case R.id.tv_dev_log:
                devLogs = null;
                sendLHBleData("Read log", Constant.Param_DXCYX2_LOG);
                break;
            case R.id.tv_sample_start:
                new HitiStringDailog(LiherDXCYX2Activity.this, Constant.HitiStrTargetDailog_DX2_SampleStart, null, "采样开始", "确认开始采样前的润洗管路、洗井、检测参数等流程", LiherDXCYX2Activity.this);
                break;
            case R.id.tv_sample_end:
                String stopTips="确认停止进行中的流程？";
                if (sampleBox!=null&&sampleBox.getIsLoad()==0){
                    stopTips="参数达标数据还未下载！！！\n是否确认要停止进行中的流程？";
                }
                new HitiStringDailog(LiherDXCYX2Activity.this, Constant.HitiStrTargetDailog_DX2_SampleStop, null, "停止采样", stopTips, LiherDXCYX2Activity.this);

                break;
            case R.id.tv_work_rain:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_Box_RainValue, "触发采样雨量值", "触发雨量(毫米):", "", LiherDXCYX2Activity.this);
                break;
            case R.id.tv_work_water_height:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_Box_Water_HeightValue, "触发采样液位值", "液位阀值(厘米):", "", LiherDXCYX2Activity.this);
                break;
            case R.id.tv_request_msg_cycle:
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_Box_RequestMsg_Cycle, "同步平台指令周期", "同步周期(分钟):", "", LiherDXCYX2Activity.this);
                break;


            case R.id.send:
                if (StringUtils.isEmpty(sendData.getText().toString())) {
                    showMsg(2, "请先输入指令内容");
                    return;
                }
                sendLHBleData(sendData.getText().toString().trim(), Constant.PARAM_LIHER_ORDER);
                sendData.setText("");
                break;
            case R.id.tv_work_plan:
                sendLHBleData("Read SamPoint", Constant.PARAM_Work_Plan);
                break;
        }
    }

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.HitiStrTargetDialog_DX2_DBDataList:
                sampleBox.setIsLoad(1);
                sendLHBleData("Read wcs", Constant.Param_DXCYX2_WCS);
                break;
            case Constant.Dailog_Input_CyNum:
                String strNum = (String) target;
                if (StringUtils.isInt(strNum)) {
                    int scale = Integer.parseInt(strNum);
                    if (scale <= 24) {
                        sendLHBleData("Write scale=" + scale, Constant.Param_Wirte_CyNum);
                        return;
                    }
                }
                showMsg(2, "请输入正确等比例采样次数,1~24");
                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_Input_CyNum, "设置等比例采样", "等比例采样次数", "", LiherDXCYX2Activity.this);

                break;
            case Constant.Dailog_BLE_CycTime:
                String str = (String) target;
                if (StringUtils.isInt(str)) {
                    int cycTime = Integer.parseInt(str);
                    sendLHBleData("Write SamPd=" + cycTime, Constant.Param_Wirte_SamPeriod);
                }

                break;
            case Constant.Dailog_Input_SampleNo:
                if (target != null && target instanceof String) {
                    String sampleNo = (String) target;
                    if (sampleNo.length() > 2 && sampleNo.length() < 16) {
                        String strMsg = "0" + currentSampleBottle.getBottleNo() + tempBottleMac + "-" + StringUtils.addCharForNum(sampleNo.trim(), 16, '_');
                        if (strMsg.length() == 32) strMsg = strMsg.substring(1);
                        sendLHBleData("Write Subaddr=" + strMsg, Constant.Param_Write_Subaddr);
                    }
                }
                break;


            case Constant.Dailog_Box_PyTime:
                String pyTimeStr = (String) target;
                if (pyTimeStr != null && StringUtils.isInt(pyTimeStr)) {
                    int pyTime = Integer.parseInt(pyTimeStr);
                    if (pyTime >= 10 && pyTime <= 300) {
                        sendLHBleData("Write PyTime=" + pyTime, Constant.Param_Write_PyTime);
                        return;
                    }
                }
                showMsg(2, "请输入正确的排样时长，10~300秒之间");

                break;
            case Constant.Dialog_DXBox_WashTime:
                String washTimeStr = (String) target;
                if (washTimeStr != null && StringUtils.isInt(washTimeStr)) {
                    int washTime = Integer.parseInt(washTimeStr);
                    if (washTime > 0) {
                        sendLHBleData("Write WashTime=" + washTime, Constant.Param_Write_DXBox_WashTime);
                        return;
                    }
                }
                showMsg(2, "请输入正确的洗井时长，整数秒");
                break;
            case Constant.Dialog_DXBox_PulseSpeed:
                String pulseSpeedStr = (String) target;
                if (pulseSpeedStr != null && StringUtils.isInt(pulseSpeedStr)) {
                    int pulseSpeed = Integer.parseInt(pulseSpeedStr);
                    if (pulseSpeed > 0) {
                        sendLHBleData("Write speed=" + pulseSpeed, Constant.Param_Write_DXBox_PulseSpeed);
                        return;
                    }
                }
                showMsg(2, "请输入正确的气囊泵启停间隔时间，整数秒");
                break;
            case Constant.Dialog_DXBox_WarningSec:
                String warningSecStr = (String) target;
                if (warningSecStr != null && StringUtils.isInt(warningSecStr)) {
                    int warningSec = Integer.parseInt(warningSecStr);
                    if (warningSec > 20) {
                        sendLHBleData("Write jybj=" + warningSec, Constant.Param_Write_DXBox_WarningSec);
                        return;
                    }
                }
                showMsg(2, "请输入正确的洗井超时时间，整数分钟>20");
                break;
            case Constant.Dailog_Box_JyThreshold:
                String jyThresholdStr = (String) target;
                if (jyThresholdStr != null && StringUtils.isInt(jyThresholdStr)) {
                    int jyThreshold = Integer.parseInt(jyThresholdStr);
                    if (jyThreshold > 300 && jyThreshold <= 6000) {
                        sendLHBleData("Write JyTime=" + jyThreshold, Constant.Param_Write_JyThreshold);
                        return;
                    }
                }
                showMsg(2, "请输入正确的进样阀值，301~6000之间");

                break;

            case Constant.Dailog_Box_TimeMode_Delay:
                int delay = (int) target;
                if (delay > 0 && delay <= 800) {
                    sendLHBleData("Write delay=" + delay * 60, Constant.Param_Write_TimeMode_Delay);
                    return;
                }
                showMsg(2, "请输入正确的延后生效时刻，13个小时以内");

                break;

            case Constant.Dailog_Box_RequestMsg_Cycle:
                String cycleTime = (String) target;
                if (StringUtils.isInt(cycleTime)) {
                    int cycleValue = Integer.parseInt(cycleTime);

                    if (cycleValue > 10080 || cycleValue < 0) {
                        showMsg(2, "请输入正确同步周期，1~10080分钟的整数");
                    } else {
                        sendLHBleData("Write revpd=" + cycleValue, Constant.Param_Write_RequestMsg_Cycle);

                    }
                } else {
                    showMsg(2, "请输入正确同步周期，1~10080分钟的整数");

                }
                break;
            case Constant.Dailog_Box_UpdateMsg_Cycle:
                String updateTime = (String) target;
                if (StringUtils.isInt(updateTime)) {
                    int updateValue = Integer.parseInt(updateTime);

                    if (updateValue > 10080 || updateValue < 0) {
                        showMsg(2, "请输入正确上传周期，1~10080分钟的整数");
                    } else {
                        sendLHBleData("Write reppd=" + updateValue, Constant.Param_Write_Update_Cycle);

                    }
                } else {
                    showMsg(2, "请输入正确上传周期，1~10080分钟的整数");

                }
                break;
            case Constant.Dailog_BLE_SYSDateTime:
                String sysTime = (String) target;
                if (sysTime != null && sysTime.length() == 19) {
                    sendLHBleData("Write SysTime=" + DateUtils.formatTime(sysTime, DateUtils.DATE_TIME_PATTERN, DateUtils.METHOD_DATE_TIME_PATTERN).substring(2), Constant.Param_Wirte_SysTime);
                } else {
                    showMsg(2, "请录入正确时间，" + sysTime);
                }
                break;
            case Constant.Dailog_BLE_MACAddress:
                String strTarget = (String) target;

                sendLHBleData("Write BLEaddr=" + strTarget, Constant.Param_Write_Box_Mac_Address);
                break;
            case Constant.RequestCode_Scan_Add_SampleNo:
                showMsg(2, "请扫描录入样品编号");
                startActivityForResult(new Intent(LiherDXCYX2Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_Add_SampleNo);
                break;
            case Constant.Dialog_Box_Sample_Bottle:
                String bottleNumStr = (String) target;
                if (bottleNumStr != null && StringUtils.isInt(bottleNumStr)) {
                    int bottleNum = Integer.parseInt(bottleNumStr);
                    if (bottleNum >= 1 && bottleNum <= 6) {
                        sendLHBleData("Write samdev=" + bottleNum, Constant.Param_Write_SamplingBottle);
                        return;
                    }
                }
                showMsg(2, "请输入正确的瓶号，1~6之间");

                break;
            case Constant.HitiStrTargetDialog_Bottle_CloseCap:

                sendLHBleData("lc", Constant.PARAM_BLE_CloseCap);
                break;
            case Constant.HitiStrTargetDailog_DX2_SampleStart:
                sendLHBleData("Write start=1", Constant.PARAM_DX2_SampleStart);
                break;
            case Constant.HitiStrTargetDailog_DX2_SampleStop:
                sendLHBleData("Write start=0", Constant.PARAM_DX2_SampleStop);
                break;
            case Constant.HitiStrTargetDailog_DX2_SamplePrepare:

                break;
            case Constant.HitiStrTargetDialog_DX2_DevLog:

                break;

        }
    }

    @Override
    public void onItemClick3(View v, int position, Object object, int type) {
        switch (type) {
            case Constant.Dailog_Select_SampleBottle_Action:
                JSONObject jo = (JSONObject) object;
                String orderName = jo.getString("MyName");
                currentSampleBottle = jo.getObject("MyObject", SampleBottle.class);
                if (currentSampleBottle.getBottleNo() < 1) {
                    refreshBottleList();
                    return;
                }
                switch (orderName) {
                    case "装瓶":
                        startActivityForResult(new Intent(LiherDXCYX2Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);

                        break;
                    case "卸瓶":
                        String str = "0" + currentSampleBottle.getBottleNo();
                        if (str.length() == 3) str = str.substring(1);
                        sendLHBleData("Write Subaddr=" + str + "000000000000-0000000000000000", Constant.Param_Write_Subaddr);
                        break;
                    case "采样":
                        showMsg(2, "发起" + currentSampleBottle.getBottleNo() + "号瓶采样");
                        sendLHBleData("Write samnum=" + currentSampleBottle.getBottleNo(), Constant.Param_Write_Sampling_Bottle);
                        break;
                    case "禁用":
                        if (sampleBox.getLockBottles() == null || sampleBox.getLockBottles().length() != 6) {
                            refreshDetails();
                        } else {
                            StringBuffer lockBottles = new StringBuffer(sampleBox.getLockBottles());
                            sendLHBleData("Write Banned=" + lockBottles.replace(currentSampleBottle.getBottleNo() - 1, currentSampleBottle.getBottleNo(), "1").toString(), Constant.Param_Write_Banned);

                        }
                        break;
                    case "启用":
                        if (sampleBox.getLockBottles() == null || sampleBox.getLockBottles().length() != 6) {
                            refreshDetails();
                        } else {
                            StringBuffer lockBottles = new StringBuffer(sampleBox.getLockBottles());
                            sendLHBleData("Write Banned=" + lockBottles.replace(currentSampleBottle.getBottleNo() - 1, currentSampleBottle.getBottleNo(), "0").toString(), Constant.Param_Write_Banned);

                        }
                        break;
                    case "留样":
                        if (sampleBox.getLockBottles() == null || sampleBox.getLockBottles().length() != 6) {
                            refreshDetails();
                        } else {
                            StringBuffer lockBottles = new StringBuffer(sampleBox.getLockBottles());
                            sendLHBleData("Write Banned=" + lockBottles.replace(currentSampleBottle.getBottleNo() - 1, currentSampleBottle.getBottleNo(), "2").toString(), Constant.Param_Write_Banned);

                        }
                        break;

                }
                break;
            case Constant.Dailog_Box_SamplePlan:
                List<BoxSamplePlan> boxSamplePlans = (List<BoxSamplePlan>) object;
                if (boxSamplePlans != null && boxSamplePlans.size() == 6) {
                    updateBoxSamplePlans(boxSamplePlans);
                }
                break;

        }
    }

    /**
     * 提交新的定时采样计划
     *
     * @param boxSamplePlans
     */
    private void updateBoxSamplePlans(List<BoxSamplePlan> boxSamplePlans) {
        StringBuffer sb = new StringBuffer();
        sb.append("Write SamPoint=");
        for (BoxSamplePlan plan : boxSamplePlans) {
            sb.append(plan.getStatus() + plan.getHHmm() + "#");
        }
        showMsg(2, "正在更新定时采样计划信息...");

        sendLHBleData(sb.toString(), Constant.PARAM_Write_Box_Sample_Plan);

    }

    private void showSelectSampleModeMenu() {
        PopupMenu popupMenu = new PopupMenu(this, tvSampleMode);
        popupMenu.inflate(R.menu.menu_select_sample_mode);
        popupMenu.getMenu().findItem(R.id.sample_rain_mode).setVisible(false);
        popupMenu.getMenu().findItem(R.id.sample_mix_mode).setVisible(false);
        popupMenu.getMenu().findItem(R.id.sample_water_mode).setVisible(false);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.sample_485_mode:
                        sendLHBleData("Write Mode=0", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_time_mode:
                        sendLHBleData("Write Mode=1", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_rain_mode:
                        sendLHBleData("Write Mode=2", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_mix_mode:
                        sendLHBleData("Write Mode=3", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_plan_mode:
                        sendLHBleData("Write Mode=4", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_water_mode:
                        sendLHBleData("Write Mode=5", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_test_mode:
                        sendLHBleData("Write Mode=7", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_cy_num_mode:
                        sendLHBleData("Write Mode=8", Constant.Param_Wirte_Mode);
                        return true;
                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }


    private void showSelectUIModeMenu() {
        PopupMenu popupMenu = new PopupMenu(this, rightTitleTxt);
        popupMenu.inflate(R.menu.menu_select_ui_mode);
        if (!HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {//无生成维修权限的用户无版本更新功能
            popupMenu.getMenu().findItem(R.id.version_upgrade).setVisible(false);
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.ui_mode:

                        srlUIMode.setVisibility(View.VISIBLE);
                        llOrderMode.setVisibility(View.GONE);
                        rightTitleTxt.setText("交互模式");
                        refreshDetails();
                        return true;
                    case R.id.order_mode:
                        srlUIMode.setVisibility(View.GONE);
                        llOrderMode.setVisibility(View.VISIBLE);
                        rightTitleTxt.setText("窗口模式");
                        return true;
                    case R.id.version_upgrade:
                        showMsg(2, "功能正在开发完善中，请先用GRToolbox软件升级");


                     /*   if (sampleBox!=null&&!StringUtils.isEmpty(sampleBox.getVersion())&&sampleBox.getVersion().length()==6){
                            ArrayList<String> macList=new ArrayList<>();
                            macList.add(mDeviceAddress);
                            startActivity(BlueOTAActivity.getIntent(LiherCYXActivity.this,macList,sampleBox.getVersion()));
                            finish();
                        }else {
                            refreshDetails();
                        }*/
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
            if (arg2 == Constant.PARAM_LIHER_ORDER) {
                SpannableStringBuilder builder = new SpannableStringBuilder("发送:" + msgContext);
                //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            } else if (arg2 == Constant.Return_BLE_False) {
                showMsg(2, msgContext);
                if (loadingDialog != null) loadingDialog.dismiss();
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
                case Constant.Return_BLE_False:
                    showMsg(2, msgContext);
                    if (loadingDialog != null) loadingDialog.dismiss();
                    break;
                case Constant.Return_Read_Detail:
                    //00#01#00/13#00060#003600#007#000000#240613#240702144703#0#0D0224070001#2.7#                 0#001#100#1500#15#5#120#  #0#0	#                    #0#10#600#900#0#0#0#0
                    try {
                        String[] details = msgContext.split("#", -1);
                        if (sampleBox == null) sampleBox = new DX2SampleBox();
                        if (details.length >= 18) {
                            sampleBox.setSampleMode(details[0]);
                            sampleBox.setCurrentSampleBottleNo(details[1]);
                            if (!StringUtils.isEmpty(details[2]) && details[2].split("/").length == 2) {
                                int currentStep = Integer.parseInt(details[2].split("/")[0]);
                                int allStep = Integer.parseInt(details[2].split("/")[1]);
                                if (currentStep == 0) {
                                    sampleBox.setWorkStep("空闲");
                                }else if (currentStep>allStep){
                                    sampleBox.setWorkStep("100%");
                                }else {
                                    sampleBox.setWorkStep(currentStep * 100 / allStep + "%");
                                }

                            } else {
                                sampleBox.setWorkStep(details[2]);
                            }

                            sampleBox.setCycleTime(Integer.parseInt(details[3]));
                            if (!"_".equals(details[4].trim()))
                                sampleBox.setNextSampleTime(Integer.parseInt(details[4]));
                            sampleBox.setDevStatus(details[5]);
                            sampleBox.setLockBottles(details[6]);
                            sampleBox.setVersion(details[7]);
                            sampleBox.setSysTime(details[8]);
                            sampleBox.setAlarmState(Integer.parseInt(details[9]));
                            sampleBox.setMN(details[10]);
                            sampleBox.setDevEV(details[11].trim());
                            sampleBox.setSamplePointID(details[12].trim());
                            if (details[13].trim().length() <= 3)
                                sampleBox.setRainNum(Integer.parseInt(details[13].trim()));
                            sampleBox.setWaterHeight(Integer.parseInt(details[14].trim()));
                            sampleBox.setJyTime(Integer.parseInt(details[15]));
                            sampleBox.setUpdateCycle(Integer.parseInt(details[16].trim()));
                            sampleBox.setRequestCycle(Integer.parseInt(details[17].trim()));
                            if (details.length >= 21) {
                                sampleBox.setPyTime(Integer.parseInt(details[18].trim()));
                                sampleBox.setRiss(details[19]);
                                sampleBox.setCyNum(Integer.parseInt(details[20].trim()));
                            }

                            if (details.length >= 23) {
                                String icc = details[22];
                                if (StringUtils.isEmpty(icc) || "00000000000000000000".equals(icc.trim())) {
                                    sampleBox.setIccID("");
                                    tvInternetCID.setText("物联网卡号ICCID值为空");
                                } else {
                                    sampleBox.setIccID(icc.trim());
                                    tvInternetCID.setText(icc.trim());
                                }
                            }
                            if (details.length >= 28) {
                                sampleBox.setGnns(Integer.parseInt(details[23].trim()));
                                gpsSearch = sampleBox.getGnns() == 1;
                                swGPSSearch.setChecked(gpsSearch);
                                sampleBox.setPulseValvePeriod(StringUtils.isEmpty(details[24]) ? 0 : Integer.parseInt(details[24].trim()));
                                sampleBox.setJyWarningSec(StringUtils.isEmpty(details[25]) ? 0 : Integer.parseInt(details[25].trim()));
                                sampleBox.setWashTime(StringUtils.isEmpty(details[26]) ? 0 : Integer.parseInt(details[26].trim()));
                                sampleBox.setWaterPressure(StringUtils.isEmpty(details[27]) ? 0 : Integer.parseInt(details[27].trim()));
                            }
                            if (details.length >= 31) {
                                sampleBox.setJudge(StringUtils.isEmpty(details[28]) ? 0 : Integer.parseInt(details[28].trim()));
                                sampleBox.setStartFlag(StringUtils.isEmpty(details[29]) ? 0 : Integer.parseInt(details[29].trim()));
                                sampleBox.setSampleStatus(StringUtils.isEmpty(details[30]) ? 0 : Integer.parseInt(details[30].trim()));
                                judgeSearch = sampleBox.getJudge() == 1;
                                swJudgeSearch.setChecked(judgeSearch);
                                if (sampleBox.getStartFlag() == 1) {
                                    tvSampleStart.setVisibility(View.GONE);
                                    tvSampleEnd.setVisibility(View.VISIBLE);
                                } else {
                                    tvSampleStart.setVisibility(View.VISIBLE);
                                    tvSampleEnd.setVisibility(View.GONE);
                                }
                                tvSampleStatue.setText(StringUtils.getDX2SampleStatus(sampleBox.getSampleStatus()));
                                if (sampleBox.getSampleStatus() == 3 || sampleBox.getSampleStatus() == 4) {
                                    new HitiStringDailog(LiherDXCYX2Activity.this, Constant.HitiStrTargetDailog_DX2_SamplePrepare, null, "采样提示", StringUtils.getDX2SampleStatus(sampleBox.getSampleStatus()), LiherDXCYX2Activity.this);
                                }
                            }
                            if (details.length>=34){
                                sampleBox.setBaseWaterPressure(StringUtils.isEmpty(details[31]) ? 0 : Integer.parseInt(details[31].trim()));
                                sampleBox.setWaterStatus(StringUtils.isEmpty(details[32]) ? 0 : Integer.parseInt(details[32].trim()));
                                sampleBox.setPulseValveFlag(StringUtils.isEmpty(details[33]) ? 0 : Integer.parseInt(details[33].trim()));
                                tvBaseWaterPressure.setText(sampleBox.getBaseWaterPressure() == 0 ? "" : (DoubleUtils.div(sampleBox.getBaseWaterPressure(), 100d, 2) + "米"));
                                tvWaterStatus.setText(sampleBox.getWaterStatus()==1?"水位波动>10cm":"正常");
                                tvPulseStatus.setText(sampleBox.getPulseValveFlag()==1?"开启":"关闭");
                            }
                            if (details.length>=35){
                                sampleBox.setLlj(Integer.parseInt(details[34].trim()));
                                if (sampleBox.getLlj()==1){
                                    tvWaterLLJ.setText("无水");
                                    tvWaterLLJ.setTextColor(getResources().getColor(R.color.color_black_999999));
                                }else if (sampleBox.getLlj()==0){
                                    tvWaterLLJ.setText("有水");
                                    tvWaterLLJ.setTextColor(getResources().getColor(R.color.color_app_normal));
                                }else {
                                    tvWaterLLJ.setText("未知"+sampleBox.getLlj());
                                    tvWaterLLJ.setTextColor(getResources().getColor(R.color.color_black_999999));
                                }
                            }
                            if ("08".equals(sampleBox.getSampleMode()) && sampleBox.getCyNum() >= 0) {
                                tvSampleMode.setText("等比例采样" + sampleBox.getCyNum() + "次");
                            } else {
                                tvSampleMode.setText(StringUtils.getSampleMode(sampleBox.getSampleMode()));
                            }

                            llWorkPlan.setVisibility(View.GONE);

                            llSampleNextTime.setVisibility(View.GONE);
                            llWorkStep.setVisibility(View.GONE);
                            switch (sampleBox.getSampleMode()) {
                                case "00":
                                    llWorkStep.setVisibility(View.GONE);//20240705 陈备隐藏当前进度字段
                                    break;
                                case "01":
                                    llSampleNextTime.setVisibility(View.VISIBLE);
                                    break;
                                case "02":
                                    llWorkStep.setVisibility(View.GONE);//20240705 陈备隐藏当前进度字段
                                    break;
                                case "03":
                                    llWorkStep.setVisibility(View.GONE);//20240705 陈备隐藏当前进度字段
                                    break;
                                case "04":
                                    llWorkPlan.setVisibility(View.VISIBLE);
                                    llWorkStep.setVisibility(View.GONE);//20240705 陈备隐藏当前进度字段
                                    break;
                                case "05":
                                    llWorkStep.setVisibility(View.GONE);//20240705 陈备隐藏当前进度字段
                                    break;
                                case "06":
                                    llWorkStep.setVisibility(View.GONE);//20240705 陈备隐藏当前进度字段
                                    break;
                                case "07":
                                    llWorkStep.setVisibility(View.GONE);//20240705 陈备隐藏当前进度字段
                                    break;
                                case "08":
                                    llSampleNextTime.setVisibility(View.VISIBLE);
                                    break;
                            }
                            if (!StringUtils.isEmpty(sampleBox.getSysTime()) && sampleBox.getSysTime().startsWith("2")) {

                                tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" + sampleBox.getSysTime(), "yyyyMMddHHmmss")));
                                if (Math.abs(System.currentTimeMillis() - DateUtil.format2miao("20" + sampleBox.getSysTime(), "yyyyMMddHHmmss")) > 1 * 60 * 1000) {
                                    new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherDXCYX2Activity.this);
                                }
                            } else {
                                new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherDXCYX2Activity.this);

                            }

                            tvDevVersion.setText(sampleBox.getVersion());
                            tvWorkStep.setText(sampleBox.getWorkStep());
                            String devStatus = StringUtils.getDevStatus(sampleBox.getDevStatus());
                            tvWorkStatus.setText(devStatus);
                            if (devStatus != null && devStatus.length() > 3) {
                                tvWorkStatus.setTextColor(getResources().getColor(R.color.color_red));
                                playAlarmSoundAndVibrate();
                                showMsg(1, "警告：" + devStatus);
                            }else {
                                tvWorkStatus.setTextColor(getResources().getColor(R.color.color_black_333333));
                            }

                            tvSampleNextTime.setText(sampleBox.getNextSampleTime() / 60 + "分钟");
                            tvDevEV.setText(" " + sampleBox.getDevEV() + "V");
                            alarmState = sampleBox.getAlarmState() == 1;
                            swAlarmState.setChecked(alarmState);

                            if (sampleBox.getJyTime() <= 300) {
                                tvJyThreshold.setText("");
                            } else {
                                tvJyThreshold.setText(sampleBox.getJyTime() + "脉冲");
                            }
                            if (sampleBox.getPyTime() > 0) {
                                tvPyTime.setText(sampleBox.getPyTime() + "秒");
                            }

                            if (sampleBox.getPyTime() > 0 && (StringUtils.isEmpty(sampleBox.getRiss()) || "0".equals(sampleBox.getRiss()))) {
                                tvDevRiss.setText(" 离线");
                            } else {
                                tvDevRiss.setText(StringUtils.isEmpty(sampleBox.getRiss()) ? " " : " " + sampleBox.getRiss());
                            }
                            tvRequestMsgCycle.setText(sampleBox.getRequestCycle() + "分钟");
                            tvJyWarningSec.setText(sampleBox.getJyWarningSec() == 0 ? "" : (sampleBox.getJyWarningSec() + "分钟"));
                            tvWashTime.setText(sampleBox.getWashTime() == 0 ? "" : (sampleBox.getWashTime() + "秒"));
                            tvPulseSpeed.setText(sampleBox.getPulseValvePeriod() == 0 ? "" : (sampleBox.getPulseValvePeriod() + "秒"));
                            tvWaterPressure.setText(sampleBox.getWaterPressure() == 0 ? "" : (DoubleUtils.div(sampleBox.getWaterPressure(), 100d, 2) + "米"));


                            sampleBottleList = new ArrayList<>();
                            //20240702 陈备 新类型地下水采样器没有采样瓶
                            // sendLHBleData("Read Subaddr", Constant.Param_Read_Subaddr);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case Constant.Return_Read_Subaddr://1-736F6C430000-210930235959-0000000000000000
                    if (sampleBottleList == null) sampleBottleList = new ArrayList<>();
                    if (loadingDialog != null) loadingDialog.dismiss();
                    SampleBottle sampleBottle = new SampleBottle();
                    String[] subaddrs = msgContext.split("-", -1);
                    if (subaddrs != null && subaddrs.length >= 4) {
                        sampleBottle.setBottleNo(Integer.parseInt(subaddrs[0]));
                        if (sampleBottle.getBottleNo() > 0 && sampleBottle.getBottleNo() <= 6 && sampleBox != null && sampleBox.getLockBottles() != null && sampleBox.getLockBottles().length() == 6) {
                            sampleBottle.setIsLock(Integer.parseInt(String.valueOf(sampleBox.getLockBottles().toCharArray()[sampleBottle.getBottleNo() - 1])));
                        } else {
                            sampleBottle.setIsLock(0);
                        }
                        sampleBottle.setBlueMac(subaddrs[1]);
                        sampleBottle.setDateTime(subaddrs[2]);
                        sampleBottle.setSampleID(subaddrs[3]);
                        sampleBottleList.add(sampleBottle);
                        mAdapter.setDatas(sampleBottleList);
                    }
                    break;
                case Constant.RETURN_Work_Plan:
                    samplePlanList = new ArrayList<>();
                    String[] cbPlans = msgContext.split("#", 0);//limit >0为限制切割后份数，1则不分割原字符串返回；0为不限制切割份数但去除最后一段如果是空字符串的情况；<0为不限制切割份数也不去除最后一段如果是空字符串的情况；
                    if (cbPlans != null && cbPlans.length >= 6) {
                        for (int i = 0; i < 6; i++) {
                            BoxSamplePlan plan = new BoxSamplePlan();
                            if (cbPlans[i] != null && cbPlans[i].length() == 6) {

                                plan.setStatus(cbPlans[i].substring(0, 2));
                                plan.setHHmm(cbPlans[i].substring(2));

                            } else {
                                plan.setStatus("00");
                                plan.setHHmm("2359");
                            }
                            samplePlanList.add(plan);
                        }
                        Collections.sort(samplePlanList);
                        if (boxSamplePlanDailog != null && boxSamplePlanDailog.isShow()) {
                            boxSamplePlanDailog.notifyDataSetChanged(samplePlanList);
                        } else {
                            boxSamplePlanDailog = new BoxSamplePlanDailog(LiherDXCYX2Activity.this, "定时采样计划", samplePlanList,null, Constant.Dailog_Box_SamplePlan, LiherDXCYX2Activity.this);

                        }
                    }
                    break;
                case Constant.Return_Wirte_Alarm:
                    if (msgContext != null && msgContext.contains("Write Alarm=")) {
                        alarmState = !alarmState;
                        sampleBox.setAlarmState(alarmState ? 1 : 0);
                        swAlarmState.setChecked(alarmState);
                    }
                    break;
                case Constant.Return_Wirte_Gnss:
                    if (msgContext != null && msgContext.contains("Write gnss=")) {
                        gpsSearch = !gpsSearch;
                        sampleBox.setGnns(gpsSearch ? 1 : 0);
                        swGPSSearch.setChecked(gpsSearch);
                    }
                    break;
                case Constant.Return_Wirte_Mode:
                    if (msgContext != null && msgContext.contains("Write Mode=")) {
                        if ("Write Mode=1".equals(msgContext)) {
                            DlgTime.showYMDDialog(LiherDXCYX2Activity.this, Calendar.getInstance(), Calendar.getInstance(), null,
                                    true, true, true, true, true, true,
                                    new DlgTime.OnDateSelectedFinishCallback() {
                                        @Override
                                        public void onDateSelected(Date date) {
                                            showMsg(2, "设置等时模式采样生效时间" + DateUtil.long2time10(date.getTime()).substring(2));
                                            sendLHBleData("Write Effective=" + DateUtil.long2time10(date.getTime()).substring(2), Constant.Param_Write_TimeMode_Delay);

                                        }
                                    });
                          sampleBox.setSampleMode("01");
                            tvSampleMode.setText("等时模式");
                        } else if ("Write Mode=8".equals(msgContext)) {
                            showMsg(2, "设置等比例采样次数,1~24次");
                            new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_Input_CyNum, "设置等比例采样", "等比例采样次数", "", LiherDXCYX2Activity.this);
                            sampleBox.setSampleMode("08");
                            tvSampleMode.setText("等比例采样");
                        } else {
                            RxTimerUtil.cancel();
                            RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                                @Override
                                public void doNext(long number) {
                                    refreshDetails();
                                }
                            });
                        }

                    } else {
                        showMsg(1, "设置采样模式失败,\n" + msgContext);

                    }
                    break;
                case Constant.Return_Wirte_SamPeriod:
                    if (msgContext != null && msgContext.startsWith("Write SamPd=")) {
                        int cycTime = Integer.parseInt(msgContext.substring(12).trim());
                        if (sampleBox != null) sampleBox.setCycleTime(cycTime);
                    }
                    break;
                case Constant.Return_Write_Subaddr:
                    if (msgContext != null && msgContext.startsWith("Write Subaddr=")) {
                        tempBottleMac = null;
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    }
                    break;
                case Constant.Return_Write_Sampling_Bottle:
                    if (msgContext != null && msgContext.startsWith("Write samnum=")) {
                        showMsg(0, "采样命令下发成功~");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });

                    }
                    break;
                case Constant.Return_Write_Banned:
                    if (msgContext != null && msgContext.startsWith("Write Banned=")) {
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    }
                    break;
                case Constant.Return_DEV_Reset:
                    if (msgContext != null && msgContext.equals("ln"))
                        showMsg(0, "复位指令下发成功~");
                    break;

                case Constant.Return_Pulse_Open:
                    if (msgContext != null && msgContext.equals("lk"))
                        showMsg(0, "打开气囊泵命令下发成功~");

                    break;
                case Constant.Return_Pulse_Close:
                    if (msgContext != null && msgContext.equals("lb"))
                        showMsg(0, "关闭气囊泵命令下发成功~");

                    break;
                case Constant.RETURN_Write_Box_Sample_Plan:
                    if (msgContext != null && msgContext.contains("Write SamPoint=")) {
                        sendLHBleData("Read SamPoint", Constant.PARAM_Work_Plan);
                    } else {
                        showMsg(1, "定时采样计划提交失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_JyThreshold:
                    if (msgContext != null && msgContext.contains("Write JyTime=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int jyThreshold = Integer.parseInt(msgContext.substring(13).trim());
                        if (sampleBox != null) sampleBox.setJyTime(jyThreshold);
                        tvJyThreshold.setText(jyThreshold + "脉冲");

                    } else {
                        showMsg(1, "设置进样阀值失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_PyTIme:
                    if (msgContext != null && msgContext.contains("Write PyTime=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int pyTime = Integer.parseInt(msgContext.substring(13).trim());
                        if (sampleBox != null) sampleBox.setPyTime(pyTime);
                        tvPyTime.setText(pyTime + "秒");
                    } else {
                        showMsg(1, "设置排样时长失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_DXBox_WashTime:
                    if (msgContext != null && msgContext.contains("Write WashTime=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int washTime = Integer.parseInt(msgContext.substring(15).trim());
                        if (sampleBox != null) sampleBox.setWashTime(washTime);
                        tvWashTime.setText(washTime + "秒");
                    } else {
                        showMsg(1, "设置洗井时长失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_DXBox_PulseSpeed:
                    if (msgContext != null && msgContext.contains("Write speed=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int pulseSpeed = Integer.parseInt(msgContext.substring(12).trim());
                        if (sampleBox != null) sampleBox.setPulseValvePeriod(pulseSpeed);
                        tvPulseSpeed.setText(pulseSpeed + "秒");
                    } else {
                        showMsg(1, "设置气囊泵启停间隔时间失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_DXBox_WarningSec:
                    if (msgContext != null && msgContext.contains("Write jybj=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int warningSec = Integer.parseInt(msgContext.substring(11).trim());
                        if (sampleBox != null) sampleBox.setJyWarningSec(warningSec);
                        tvJyWarningSec.setText(warningSec + "分钟");
                    } else {
                        showMsg(1, "设置洗井超时时间失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_CyNum:
                    if (msgContext != null && msgContext.contains("Write scale=")) {
                        DlgTime.showYMDDialog(LiherDXCYX2Activity.this, Calendar.getInstance(), Calendar.getInstance(), null,
                                true, true, true, true, true, true,
                                new DlgTime.OnDateSelectedFinishCallback() {
                                    @Override
                                    public void onDateSelected(Date date) {
                                        showMsg(2, "设置等比例采样生效时间" + DateUtil.long2time10(date.getTime()).substring(2));
                                        sendLHBleData("Write Effective=" + DateUtil.long2time10(date.getTime()).substring(2), Constant.Param_Write_TimeMode_Delay);

                                    }
                                });
                    } else {
                        showMsg(1, "设置等比例采样次数失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Read_LS:
                    if (msgContext != null && msgContext.contains("2")) {
                        mble.disconnectDevice();
                        mble.disconnectLocalDevice();
                        showMsg(0, "读取瓶信息成功...");
                        connectBleMac(mDeviceAddress, 255, false);
                    } else if (msgContext != null && msgContext.contains("1")) {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        new HitiStringDailog(LiherDXCYX2Activity.this, Constant.HitiStrTargetDialog_Bottle_CloseCap, "", "关闭瓶阀", "样瓶阀门未关，请确认关闭瓶阀后才可装瓶", LiherDXCYX2Activity.this);

                    } else {
                        showMsg(1, msgContext + "样瓶阀门状态异常无法装瓶！");

                    }
                    break;
                case Constant.Return_BLE_CloseCap:
                    if (msgContext != null && msgContext.trim().equals("olc")) {
                        mble.disconnectDevice();
                        mble.disconnectLocalDevice();
                        showMsg(0, "阀门关闭成功");
                        connectBleMac(mDeviceAddress, 255, false);


                    } else if (msgContext != null && !msgContext.trim().equals("lc")) {
                        showMsg(1, msgContext + "样瓶关阀失败，无法装瓶！");

                    } else if (msgContext != null && msgContext.trim().equals("lc")) {
                        showMsg(2, "正在关闭瓶阀，请稍后...");
                    }
                    break;

                case Constant.Return_Write_TimeMode_Delay:
                    if (msgContext != null && msgContext.startsWith("Write delay=")) {
                        int delay = Integer.parseInt(msgContext.substring(12).trim());
                        showMsg(0, delay / 60 + "分钟后生效");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "设置延迟生效失败,\n" + msgContext);
                    }
                    break;

                case Constant.Return_Write_RequestMsg_Cycle:
                    if (msgContext != null && msgContext.contains("Write revpd=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int cycleValue = Integer.parseInt(msgContext.substring(12).trim());
                        if (sampleBox != null) sampleBox.setRequestCycle(cycleValue);
                        if (cycleValue == 0) {
                            tvRequestMsgCycle.setText("同步关闭");
                        } else {
                            tvRequestMsgCycle.setText(cycleValue + "分钟");
                        }

                    } else {
                        showMsg(1, "设置同步周期失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_Update_Type:
                    if (msgContext != null && msgContext.contains("Write Report=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int updateType = Integer.parseInt(msgContext.substring(13).trim());
                        if (sampleBox != null) sampleBox.setUpdateType(updateType);

                     } else {
                        showMsg(1, "设置上传方式失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_Update_Cycle:
                    if (msgContext != null && msgContext.contains("Write reppd=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int updateValue = Integer.parseInt(msgContext.substring(12).trim());
                        if (sampleBox != null) sampleBox.setUpdateCycle(updateValue);

                    } else {
                        showMsg(1, "设置上传周期失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_SamplingBottle:
                    if (msgContext != null && msgContext.contains("Write samdev=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        if (sampleBox != null)
                            sampleBox.setCurrentSampleBottleNo(msgContext.substring(13).trim());

                    } else {
                        showMsg(1, "设置当前采样瓶失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_SysTime:
                    if (msgContext != null && msgContext.contains("Write SysTime=")) {
                        if (sampleBox != null)
                            sampleBox.setSysTime(msgContext.substring(14).trim());
                        tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" + sampleBox.getSysTime(), "yyyyMMddHHmmss")));
                    } else {
                        showMsg(1, "设置系统校时失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_Box_Mac_Address:
                    if (msgContext != null && msgContext.contains("Write BLEaddr=")) {
                        showMsg(0, "设置成功,正在重启...");

                    } else {
                        showMsg(1, "设置蓝牙地址失败,\n" + msgContext);

                    }
                    break;
                case Constant.Return_CYX_ICCID:
                    if ("00000000000000000000".equals(msgContext)) {
                        tvInternetCID.setText("物联网卡号ICCID值为空");
                    } else if (msgContext != null && msgContext.length() == 20) {
                        tvInternetCID.setText(msgContext);
                    }
                    showMsg(0, "读取物联网卡号成功~");

                    break;
                case Constant.Return_DXCYX2_WCS:
                    String[] wcsDetails = msgContext.split(",", -1);
                    int sw = 0, ec = 0;
                    double temp = 0, turb = 0, ph = 0, Do = 0,ORP=0;

                    for (String detail : wcsDetails) {
                        if (detail.startsWith("SW=")) {
                            sw = Integer.parseInt(detail.substring(3));
                        } else if (detail.startsWith("Temp=")) {
                            temp = Double.parseDouble(detail.substring(5));
                        } else if (detail.startsWith("Turb=")) {
                            turb = Double.parseDouble(detail.substring(5));
                        } else if (detail.startsWith("pH=")) {
                            ph = Double.parseDouble(detail.substring(3));
                        } else if (detail.startsWith("DO=")) {
                            Do = Double.parseDouble(detail.substring(3));
                        } else if (detail.startsWith("EC=")) {
                            ec = Integer.parseInt(detail.substring(3));
                        } else if (detail.startsWith("ORP=")) {
                            ORP =  Double.parseDouble(detail.substring(4));
                        }
                    }

                    tvParamsData.setText("水位:" + sw + "cm" + " pH:" + DoubleUtils.round(ph, 2) + " 电导率:" + ec + "uS/cm"+ " 浊度:" + DoubleUtils.round(turb, 1) + "NTU\n"
                             + "温度:" + DoubleUtils.round(temp, 1) + "℃" + " 溶解氧:" + DoubleUtils.round(Do, 2) + "mg/L"+" ORP:" + DoubleUtils.round(ORP, 1) + "mV");
                    break;
                case Return_DXCYX2_DBWCS:
                    String[] dbwDetails = msgContext.split(",", -1);
                    String sj1="000000000000",sj2="000000000000",sj3="000000000000",DBflag="000000";
                    int sw1 = 0, ec1 = 0, sw2 = 0, ec2 = 0, sw3 = 0, ec3 = 0,zt=0;
                    double temp1 = 0, turb1 = 0, ph1 = 0, Do1 = 0,orp1=0, temp2 = 0, turb2 = 0, ph2 = 0, Do2 = 0,orp2=0, temp3 = 0, turb3 = 0, ph3 = 0, Do3 = 0,orp3=0;
                    DbParamData sjData=new DbParamData(0,"sj","日期\n时间",0);
                    DbParamData pHData=new DbParamData(1,"pH","pH\n",0);
                    DbParamData tempData=new DbParamData(2,"temp","温度\n(℃)",0);
                    DbParamData turbData=new DbParamData(3,"turb","浊度\n(NTU)",0);
                    DbParamData ecData=new DbParamData(4,"ec","电导率\n(uS/cm)",0);
                    DbParamData doData=new DbParamData(5,"Do","溶解氧\n(mg/L)",0);
                    DbParamData orpData=new DbParamData(6,"ORP","ORP\n(mV)",0);
                    DbParamData flowData=new DbParamData(7,"flow","流量\n(mL/min)",0);
                    DbParamData swData=new DbParamData(8,"sw","水位\n(cm)",0);

                    for (String detail : dbwDetails) {
                        if (detail.startsWith("A=")) {
                            sw1 = Integer.parseInt(detail.substring(2));
                            swData.setData1Value(sw1+"");
                        } else if (detail.startsWith("B=")) {
                            temp1 = Double.parseDouble(detail.substring(2));
                            tempData.setData1Value(DoubleUtils.round(temp1, 1)+"");
                        } else if (detail.startsWith("C=")) {
                            turb1 = Double.parseDouble(detail.substring(2));
                            turbData.setData1Value(DoubleUtils.round(turb1, 1)+"");
                        } else if (detail.startsWith("D=")) {
                            ph1 = Double.parseDouble(detail.substring(2));
                            pHData.setData1Value(DoubleUtils.round(ph1, 2)+"");
                        } else if (detail.startsWith("F=")) {
                            Do1 = Double.parseDouble(detail.substring(2));
                            doData.setData1Value(DoubleUtils.round(Do1, 2)+"");
                        } else if (detail.startsWith("E=")) {
                            ec1 = Integer.parseInt(detail.substring(2));
                            ecData.setData1Value(ec1+"");
                        }else if (detail.startsWith("G=")) {
                            orp1 =Double.parseDouble(detail.substring(2));
                            orpData.setData1Value(DoubleUtils.round(orp1, 1)+"");
                        } else if (detail.startsWith("V=")) {
                            sj1 =detail.substring(2).startsWith("2")?(detail.trim().substring(2)):"000000000000";
                            sjData.setData1Value(DateUtil.long2time9(DateUtil.format2miao("20"+sj1,"yyyyMMddHHmmss")));
                        } else if (detail.startsWith("H=")) {
                            sw2 = Integer.parseInt(detail.substring(2));
                            swData.setData2Value(sw2+"");
                        } else if (detail.startsWith("I=")) {
                            temp2 = Double.parseDouble(detail.substring(2));
                            tempData.setData2Value(DoubleUtils.round(temp2, 1)+"");
                        } else if (detail.startsWith("J=")) {
                            turb2 = Double.parseDouble(detail.substring(2));
                            turbData.setData2Value(DoubleUtils.round(turb2, 1)+"");
                        } else if (detail.startsWith("K=")) {
                            ph2 = Double.parseDouble(detail.substring(2));
                            pHData.setData2Value(DoubleUtils.round(ph2, 2)+"");
                        } else if (detail.startsWith("M=")) {
                            Do2 = Double.parseDouble(detail.substring(2));
                            doData.setData2Value(DoubleUtils.round(Do2, 2)+"");
                        } else if (detail.startsWith("L=")) {
                            ec2 = Integer.parseInt(detail.substring(2));
                            ecData.setData2Value(ec2+"");
                        }else if (detail.startsWith("N=")) {
                            orp2 =Double.parseDouble(detail.substring(2));
                            orpData.setData2Value(DoubleUtils.round(orp2, 1)+"");
                        } else if (detail.startsWith("W=")) {
                            sj2 =detail.substring(2).startsWith("2")?(detail.trim().substring(2)):"000000000000";
                            sjData.setData2Value(DateUtil.long2time9(DateUtil.format2miao("20"+sj2,"yyyyMMddHHmmss")));
                        } else if (detail.startsWith("O=")) {
                            sw3 = Integer.parseInt(detail.substring(2));
                            swData.setData3Value(sw3+"");
                        } else if (detail.startsWith("P=")) {
                            temp3 = Double.parseDouble(detail.substring(2));
                            tempData.setData3Value(DoubleUtils.round(temp3, 1)+"");
                        } else if (detail.startsWith("Q=")) {
                            turb3 = Double.parseDouble(detail.substring(2));
                            turbData.setData3Value(DoubleUtils.round(turb3, 1)+"");
                        } else if (detail.startsWith("R=")) {
                            ph3 = Double.parseDouble(detail.substring(2));
                            pHData.setData3Value(DoubleUtils.round(ph3, 2)+"");
                        } else if (detail.startsWith("T=")) {
                            Do3 = Double.parseDouble(detail.substring(2));
                            doData.setData3Value(DoubleUtils.round(Do3, 2)+"");
                        } else if (detail.startsWith("S=")) {
                            ec3 = Integer.parseInt(detail.substring(2));
                            ecData.setData3Value(ec3+"");
                        }else if (detail.startsWith("U=")) {
                            orp3 =Double.parseDouble(detail.substring(2));
                            orpData.setData3Value(DoubleUtils.round(orp3, 1)+"");
                        } else if (detail.startsWith("X=")) {
                            sj3 =detail.substring(2).startsWith("2")?(detail.trim().substring(2)):"000000000000";
                            sjData.setData3Value(DateUtil.long2time9(DateUtil.format2miao("20"+sj3,"yyyyMMddHHmmss")));
                        } else if (detail.startsWith("Y=")){
                            DBflag=detail.substring(2).length()==7?detail.substring(2):"0000000";
                        }else if (detail.startsWith("Z=")){
                            zt= Integer.parseInt(detail.substring(2));
                        }
                    }
                   StringBuffer sbDBFlag=new StringBuffer();
                    if (DBflag!=null&&DBflag.length()==7){
                        if (DBflag.charAt(0)=='1'){
                            sbDBFlag.append("水位达标\t");
                        }else if (sampleBox!=null&&sampleBox.getSampleStatus()==3){
                            sw1=-999;
                            sw2=-999;
                            sw3=-999;
                        }
                        if (DBflag.charAt(1)=='1'){
                            sbDBFlag.append("温度达标\t");
                            tempData.setIsDb(1);
                        }else if (sampleBox!=null&&sampleBox.getSampleStatus()==3){
                            tempData.setIsDb(2);
                            temp1=-999d;
                            temp2=-999d;
                            temp3=-999d;
                        }
                        if (DBflag.charAt(2)=='1'){
                            sbDBFlag.append("浊度达标\t");
                            turbData.setIsDb(1);
                        }else if (sampleBox!=null&&sampleBox.getSampleStatus()==3){
                            turbData.setIsDb(2);
                            turb1=-999d;
                            turb2=-999d;
                            turb3=-999d;
                        }
                        if (DBflag.charAt(3)=='1'){
                            sbDBFlag.append("pH达标\t");
                            pHData.setIsDb(1);
                        }else if (sampleBox!=null&&sampleBox.getSampleStatus()==3){
                            pHData.setIsDb(2);
                            ph1=-999d;
                            ph2=-999d;
                            ph3=-999d;
                        }
                        if (DBflag.charAt(4)=='1'){
                            sbDBFlag.append("电导率达标\t");
                            ecData.setIsDb(1);
                        }else if (sampleBox!=null&&sampleBox.getSampleStatus()==3){
                            ecData.setIsDb(2);
                            ec1=-999;
                            ec2=-999;
                            ec3=-999;
                        }
                        if (DBflag.charAt(5)=='1'){
                            sbDBFlag.append("溶解氧达标\t");
                            doData.setIsDb(1);
                        }else if (sampleBox!=null&&sampleBox.getSampleStatus()==3){
                            doData.setIsDb(2);
                            Do1=-999d;
                            Do2=-999d;
                            Do3=-999d;
                        }
                        if (DBflag.charAt(6)=='1'){
                            sbDBFlag.append("ORP达标\t");
                            orpData.setIsDb(1);
                        }else if (sampleBox!=null&&sampleBox.getSampleStatus()==3){
                            orpData.setIsDb(2);
                            orp1=-999d;
                            orp2=-999d;
                            orp3=-999d;
                        }
                    }

                  /*  String dbw1="000000000000".equals(sj1)?"":("时间1："+DateUtil.long2time(DateUtil.format2miao("20"+sj1,"yyyyMMddHHmmss"))+"\n"+(sw1>-999?("水位1:" + sw1 + "cm "):"" )+(ph1>-999? ("pH1:" + DoubleUtils.round(ph1, 2)):"") + (ec1>-999?(" 电导率1:" + ec1 + "uS/cm\n"):"\n")
                            +(turb1>-999?("浊度1:" + DoubleUtils.round(turb1, 1) + "NTU "):" ") + (temp1>-999?("温度1:" + DoubleUtils.round(temp1, 1) + "℃ "):" ") + (Do1>-999?("溶解氧1:" + DoubleUtils.round(Do1, 2) + "mg/L\n\n"):"\n\n"));
                    String dbw2="000000000000".equals(sj2)?"":("时间2："+DateUtil.long2time(DateUtil.format2miao("20"+sj2,"yyyyMMddHHmmss"))+"\n"+(sw2>-999?("水位2:" + sw2 + "cm "):" ")+ (ph2>-999?("pH2:" + DoubleUtils.round(ph2, 2)):" ") + (ec2>-999?("电导率2:" + ec2 + "uS/cm\n"):"\n")
                            +(turb2>-999?("浊度2:" + DoubleUtils.round(turb2, 1) + "NTU "):" ") + (temp2>-999?("温度2:" + DoubleUtils.round(temp2, 1) + "℃ "):" ") + (Do2>-999?("溶解氧2:" + DoubleUtils.round(Do2, 2) + "mg/L\n\n"):"\n\n"));
                    String dbw3="000000000000".equals(sj3)?"":("时间3："+DateUtil.long2time(DateUtil.format2miao("20"+sj3,"yyyyMMddHHmmss"))+"\n"+(sw3>-999?("水位3:" + sw3 + "cm "):" ")+ (ph3>-999?("pH3:" + DoubleUtils.round(ph3, 2)):" ") + (ec3>-999?("电导率3:" + ec3 + "uS/cm\n"):"\n")
                            +(turb3>-999?("浊度3:" + DoubleUtils.round(turb3, 1) + "NTU "):" ") + (temp3>-999?("温度3:" + DoubleUtils.round(temp3, 1) + "℃ "):" ") + (Do3>-999?("溶解氧3:" + DoubleUtils.round(Do3, 2) + "mg/L\n\n"):"\n\n"));
*/
                    if (zt<2){
                        showMsg(2, "设备还未到洗井流程，暂无达标参数");
                        tvParamsDBData.setText("");
                        refreshDetails();
                        return;
                    }else if (zt==2){
                        if ("000000000000".equals(sj2)){
                            sjData.setData2Value("-");
                            pHData.setData2Value("-");
                            tempData.setData2Value("-");
                            turbData.setData2Value("-");
                            ecData.setData2Value("-");
                            doData.setData2Value("-");
                            swData.setData2Value("-");
                            flowData.setData2Value("-");
                            orpData.setData2Value("-");
                        }
                        sjData.setData3Value("-");
                        pHData.setIsDb(0);
                        pHData.setData3Value("-");
                        tempData.setIsDb(0);
                        tempData.setData3Value("-");
                        turbData.setIsDb(0);
                        turbData.setData3Value("-");
                        ecData.setIsDb(0);
                        ecData.setData3Value("-");
                        doData.setIsDb(0);
                        doData.setData3Value("-");
                        swData.setIsDb(0);
                        swData.setData3Value("-");
                        flowData.setIsDb(0);
                        flowData.setData3Value("-");
                        orpData.setIsDb(0);
                        orpData.setData3Value("-");
                      //  tvParamsDBData.setText(dbw1+dbw2);
                        dbParamDataList = new ArrayList<>();
                        dbParamDataList.add(sjData);
                        dbParamDataList.add(pHData);
                        dbParamDataList.add(tempData);
                        dbParamDataList.add(turbData);
                        dbParamDataList.add(ecData);
                        dbParamDataList.add(doData);
                        dbParamDataList.add(orpData);
                       // dbParamDataList.add(flowData);//20240918 陈备说先去掉流量信息
                        dbParamDataList.add(swData);

                        new Dxcyx2DBDatasDailog(LiherDXCYX2Activity.this,"部分参数信息，洗井尚未完成",dbParamDataList,Constant.HitiStrTargetDialog_DX2_DBDataList,LiherDXCYX2Activity.this);
                    }else if (zt==4){
                        dbParamDataList = new ArrayList<>();
                        dbParamDataList.add(sjData);
                        dbParamDataList.add(pHData);
                        dbParamDataList.add(tempData);
                        dbParamDataList.add(turbData);
                        dbParamDataList.add(ecData);
                        dbParamDataList.add(doData);
                        dbParamDataList.add(orpData);
                     //   dbParamDataList.add(flowData);//20240918 陈备说先去掉流量信息
                        dbParamDataList.add(swData);
                        new Dxcyx2DBDatasDailog(LiherDXCYX2Activity.this,"洗井超时，参数未达标",dbParamDataList,Constant.HitiStrTargetDialog_DX2_DBDataList,LiherDXCYX2Activity.this);

                      //  tvParamsDBData.setText(dbw1+dbw2+dbw3+"洗井超时，参数未达标");
                    }else {
                        dbParamDataList = new ArrayList<>();
                        dbParamDataList.add(sjData);
                        dbParamDataList.add(pHData);
                        dbParamDataList.add(tempData);
                        dbParamDataList.add(turbData);
                        dbParamDataList.add(ecData);
                        dbParamDataList.add(doData);
                        dbParamDataList.add(orpData);
                      //  dbParamDataList.add(flowData);//20240918 陈备说先去掉流量信息
                        dbParamDataList.add(swData);
                        new Dxcyx2DBDatasDailog(LiherDXCYX2Activity.this,"参数达标数据",dbParamDataList,Constant.HitiStrTargetDialog_DX2_DBDataList,LiherDXCYX2Activity.this);
                       // tvParamsDBData.setText(dbw1+dbw2+dbw3+sbDBFlag.toString());
                    }
                    if (sampleBox==null&&sampleBox.getSampleStatus()!=zt){
                        refreshDetails();
                    }
                    break;
                case Constant.Return_DXCYX2_LOG:
                    if (devLogs == null)
                        devLogs = new ArrayList<String>(Arrays.asList("未润洗管路", "未开始洗井","未洗井完成"));
                    if (StringUtils.isEmpty(msgContext)) {
                        return;
                    } else if (msgContext.startsWith("1")) {
                        if (!msgContext.startsWith("1-0")) {
                            devLogs.remove(0);
                            devLogs.add(0,  DateUtil.long2time(DateUtil.format2miao("20" + msgContext.trim().substring(2), "yyyyMMddHHmmss"))+" 开始润洗管路" );

                        }
                    } else if (msgContext.startsWith("2")) {
                        if (!msgContext.startsWith("2-0")){
                            devLogs.remove(1);
                            devLogs.add(1,  DateUtil.long2time(DateUtil.format2miao("20" + msgContext.trim().substring(2), "yyyyMMddHHmmss"))+" 开始洗井流程" );

                        }
                    } else if (msgContext.startsWith("3") ) {
                        if (!msgContext.startsWith("3-0")){
                            devLogs.remove(2);
                            devLogs.add(2,  DateUtil.long2time(DateUtil.format2miao("20" + msgContext.trim().substring(2), "yyyyMMddHHmmss"))+" 水质检测达标" );
                        }
                    } else if (msgContext.startsWith("4")) {
                        if ( !msgContext.startsWith("4-0")){
                            devLogs.remove(2);
                            devLogs.add(2, DateUtil.long2time(DateUtil.format2miao("20" + msgContext.trim().substring(2), "yyyyMMddHHmmss"))+" 洗井超时结束" );

                        }
                        new HitiStringDailog(LiherDXCYX2Activity.this, Constant.HitiStrTargetDialog_DX2_DevLog, devLogs, "流程日志", devLogs.get(0) + "\n" + devLogs.get(1) + "\n" + devLogs.get(2) , LiherDXCYX2Activity.this);
                    }
                    break;
                case Constant.Return_Write_Judge:
                    if (msgContext != null && msgContext.contains("Write judge=")) {
                        judgeSearch = !judgeSearch;
                        sampleBox.setJudge(judgeSearch ? 1 : 0);
                        swJudgeSearch.setChecked(judgeSearch);
                    }
                    break;
                case Constant.Return_DX2_SampleStart:
                    if (msgContext != null && msgContext.contains("Write start=")) {
                        sampleBox.setStartFlag(1);
                        sampleBox.setIsLoad(0);
                        tvSampleStart.setVisibility(View.GONE);
                        tvSampleEnd.setVisibility(View.VISIBLE);
                        showMsg(0, "下发成功，开始流程~");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(3000, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                        if (loadingDialog != null) loadingDialog.show();
                    }
                    break;
                case Constant.Return_DX2_SampleStop:
                    if (msgContext != null && msgContext.contains("Write start=")) {
                        sampleBox.setStartFlag(0);
                        tvSampleStart.setVisibility(View.VISIBLE);
                        tvSampleEnd.setVisibility(View.GONE);
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    }
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
    protected void onDestroy() {
        super.onDestroy();
        mble.disconnectDevice();
        Log.d(TAG, "destroy");
        mble.disconnectLocalDevice();
        Log.d(TAG, "销毁");
    }

    // 连接蓝牙
    private void connectBleMac(final String macAddress, final int sendDataLenMax, final boolean isConfig) {
        if ("00:00:00:00:00:00".equals(macAddress)) {
            showMsg(1, "设备蓝牙Mac为初始化地址，连接后请尽快扫码设置！");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++) {
                    //String ads="19:98:03:17:03:20";
                    if (mble.connectDevice(macAddress,bleCallBack))    //连接蓝牙设备
                        break;

                    mble.disconnectDevice();
                    try {
                        Thread.sleep(500, 0);//200ms
                    } catch (Exception e) {

                    }
                }
                if (i == 5) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMsg(1, "连接失败");
                            loadingDialog.dismiss();
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

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMsg(1, "连接失败1");
                            if (loadingDialog!=null&&!LiherDXCYX2Activity.this.isFinishing()){
                                loadingDialog.dismiss();
                            }

                        }
                    });
                    return;
                }

                if (mble.openNotify(Constant.Ble_Data_Receive_Service)) {
                    if (mble.requestMtu(255)) {
                        mble.setSendDataLenMax(sendDataLenMax);
                        mble.mBleCallBack = bleCallBack;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                loadingDialog.dismiss();
                                HJBleApplication.shareInstance().setDesID("12345678");
                                //  ScanBleActivity.this.startActivity(LiherActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                if (macAddress.startsWith("0B") || macAddress.startsWith("0C")) {
                                    RxTimerUtil.cancel();
                                    RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                                        @Override
                                        public void doNext(long number) {
                                            sendLHBleData("ls", Constant.Param_Read_LS);
                                        }
                                    });
                                } else if (macAddress.startsWith("0D")) {
                                    sampleNoDialog = new HitiInputStrDailog(LiherDXCYX2Activity.this, Constant.Dailog_Input_SampleNo, "装瓶样品信息", "样品编号", "", LiherDXCYX2Activity.this);
                                }
                            }
                        });
                    } else {
                        if (mble.getGatt()!=null){
                            mble.getGatt().disconnect();
                            mble.getGatt().close();
                        }
                        mble.disconnectDevice();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showMsg(1, "连接失败3,设置MTU失败");
                                loadingDialog.dismiss();

                            }
                        });
                    }

                    return;
                } else {
                    mble.disconnectDevice();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMsg(1, "连接失败2");
                            loadingDialog.dismiss();
                        }
                    });
                    return;
                }

            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Constant.RequestCode_Scan_MAC_Address:
                    String qcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(qcode)) {
                        showMsg(1, "未读取到二维码信息");
                    } else if (qcode.length() != 12 || !(qcode.startsWith("0B") || qcode.startsWith("0C"))) {
                        showMsg(1, "二维码，非力合蓝牙瓶标准码");
                    } else {
                        if (currentSampleBottle == null || currentSampleBottle.getBottleNo() == 0) {
                            refreshBottleList();
                        } else {
                            tempBottleMac = qcode;
                            if (mble == null) {
                                showMsg(1, "蓝牙连接丢失，请返回重试");
                                finish();
                            } else {
                                mble.disconnectDevice();
                                mble.disconnectLocalDevice();
                                showMsg(2, "正在连接采样瓶...");

                                connectBleMac(StringUtils.getNewBlueToothMacAddress(tempBottleMac), 255, false);

                            }

                        }

                    }
                    break;
                case Constant.RequestCode_Scan_Box_MAC_Address:
                    String boxQcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(boxQcode)) {
                        showMsg(1, "未读取到二维码信息");
                    } else if (boxQcode.length() != 12) {
                        showMsg(1, "二维码，非力合蓝牙标准码");
                    } else if (boxQcode.startsWith("0D")) {
                        String macAds = boxQcode.trim();
                        new HitiStringDailog(LiherDXCYX2Activity.this, Constant.Dailog_BLE_MACAddress, macAds, "Mac地址确认", "确认设置Mac地址为：" + StringUtils.getNewBlueToothMacAddress(macAds) + "?\n蓝牙将重启后生效", LiherDXCYX2Activity.this);
                    } else {
                        showMsg(1, boxQcode + "二维码，非力合采样箱编码规则0D");
                    }
                    break;
                case Constant.RequestCode_Scan_Add_SampleNo:
                    String sampleNo = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (!StringUtils.isEmpty(sampleNo)) {
                        if (sampleNo.length() > 2 && sampleNo.length() <= 16) {
                            if (sampleNoDialog != null) sampleNoDialog.setContent(sampleNo.trim());
                            return;
                        }
                    }
                    showMsg(1, "请输入正确的样品编号");
                    break;
            }
        }
    }
}
