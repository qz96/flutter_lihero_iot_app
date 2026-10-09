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
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.BoxSamplePlan;
import com.lh.lhble.Entity.DX3SWJInfo;
import com.lh.lhble.Entity.DX3WcsInfo;
import com.lh.lhble.Entity.NetInfo;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.Entity.RtuLljInfo;
import com.lh.lhble.Entity.SampleBottle;
import com.lh.lhble.Entity.SampleBox;
import com.lh.lhble.R;
import com.lh.lhble.UI.View.BoxSamplePlanDailog;
import com.lh.lhble.UI.View.CYX2SWJDataDailog;
import com.lh.lhble.UI.View.CYX2WCSDataDailog;
import com.lh.lhble.UI.View.CYX7PHThInputDailog;
import com.lh.lhble.UI.View.CYXBumpDialog;
import com.lh.lhble.UI.View.DXCYX3JYTimeInputDailog;
import com.lh.lhble.UI.View.HitiInputCoefficientDailog;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.UI.View.NetInfoInputDailog;
import com.lh.lhble.UI.View.RTU2SamInfDailog;
import com.lh.lhble.UI.View.SelecterObjectDailog;
import com.lh.lhble.UI.View.SelecterObjectListDailog;
import com.lh.lhble.UI.View.TimeModeDelayDailog;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DlgTime;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
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
 * 采样箱0D07
 */
public class LiherCYX7Activity extends BaseFragmentActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherCYX7Activity.class.getSimpleName();

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
    private TextView tvDevName, tvDevNum, tvDevVersion, tvSysTime, tvDevIP, tvNetPort, tvCypIP, tvCypNetPort, tvMacAddress, tvSetMacAddress, tvReadData;
    private TextView tvSampleMode, tvWorkStep, tvWorkTimes, tvWorkPlan, tvWorkRain, tvWorkRainTotal, tvWorkWaterHeight, tvRequestMsgCycle, tvWorkStatus, tvSampleNextTitle, tvSampleNextTime, tvDevEV, tvUpdateMsgCycle, tvJyTime, tvJyThreshold, tvSampleBottleNum,
            tvJYHeight, tvPyTime, tvWashTime, tvDevRiss, tvInternetCID, tvDatLow, tvDevBump, tvDevStop, tvSampleLLJ, tvTriggerMode, tvWorkFlow, tvWorkPH, tvWorkCond, tvWorkTurb, tvWorkOxygen, tvWorkTemp,tvJyPipeLen,tvJyCapacity,tvTopCoefficient,tvBottomCoefficient;
    private Switch swAlarmState, swGPSSearch;
    private TextView tvRtuDev, tvDevReset, tvDevOpen;
    private LinearLayout llOrderMode, llWorkTimes, llWorkPlan, llWorkRain, llWorkRainTotal, llWorkWaterHeight, llRequestMsgCycle, llSampleNextTime, llWorkStep, llSettingInfo, llWorkTrigger, llWorkFlow, llWorkPH, llWorkCond, llWorkTurb, llWorkOxygen, llWorkTemp,llSampleSetOld,llSampleSetNew;
    private ScrollView sclOrders;
    private LinearLayout llOrders;
    private EditText sendData;
    private TextView send;
    private ImageView ivExpandContract;
    private SwipeRefreshLayout srlSampleBottles;
    private ListView lvSampleBottles;
    private SampleBottleAdapter mAdapter;
    private LoadingDialog loadingDialog = null;
    private SampleBox sampleBox;
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
    private boolean alarmState, gpsSearch;
    private String tempBottleMac;
    private HitiInputStrDailog sampleNoDialog;
    private int isBack;//是否因蓝牙中断回退页面 1-回退
    private int version;//硬件程序版本号 eg:20260501

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_cyx7;
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
                sendLHBleData("Read All", Constant.Param_Read_Detail);
                showMsg(2, "正在初始化...");
            }
        });
    }

    private void refreshDetails() {
        showMsg(2, "正在重新加载采样箱信息...");
        sendLHBleData("Read All", Constant.Param_Read_Detail);
    }

    private void refreshBottleList() {
        showMsg(2, "正在加载瓶位列表信息...");
        sampleBottleList = new ArrayList<>();
        sendLHBleData("Read ProInfo", Constant.Param_Read_ProInfo);
    }


    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherCYX7Activity.class);
        intent.putExtra(LiherCYX7Activity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherCYX7Activity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherCYX7Activity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
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
        tvNetPort = findViewById(R.id.tv_net_port);
        tvCypIP = findViewById(R.id.tv_cyp_ip);
        tvCypNetPort = findViewById(R.id.tv_cyp_net_port);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);
        tvReadData = findViewById(R.id.tv_read_data);
        tvSampleMode = findViewById(R.id.tv_sample_mode);
        tvWorkStep = findViewById(R.id.tv_work_step);
        tvWorkTimes = findViewById(R.id.tv_work_times);
        tvWorkPlan = findViewById(R.id.tv_work_plan);
        tvWorkRain = findViewById(R.id.tv_work_rain);
        tvWorkRainTotal = findViewById(R.id.tv_work_rain_total);
        tvWorkWaterHeight = findViewById(R.id.tv_work_water_height);
        tvRequestMsgCycle = findViewById(R.id.tv_request_msg_cycle);
        tvUpdateMsgCycle = findViewById(R.id.tv_update_msg_cycle);
        tvWorkStatus = findViewById(R.id.tv_work_status);
        tvSampleNextTitle = findViewById(R.id.tv_sample_next_title);
        tvSampleNextTime = findViewById(R.id.tv_sample_next_time);
        tvDevEV = findViewById(R.id.tv_dev_ev);
        tvJyTime = findViewById(R.id.tv_jy_time);
        tvJYHeight = findViewById(R.id.tv_jy_height);
        tvJyThreshold = findViewById(R.id.tv_jy_threshold);
        tvPyTime = findViewById(R.id.tv_py_time);
        tvWashTime = findViewById(R.id.tv_wash_time);
        tvDevRiss = findViewById(R.id.tv_dev_riss);
        tvInternetCID = findViewById(R.id.tv_internet_cid);
        tvDatLow = findViewById(R.id.tv_bat_low);
        tvDevBump = findViewById(R.id.tv_dev_bump);
        tvDevStop = findViewById(R.id.tv_dev_stop);

        tvSampleLLJ = findViewById(R.id.tv_sample_llj);
        tvSampleBottleNum = findViewById(R.id.tv_sample_bottle_num);
        swAlarmState = findViewById(R.id.sw_alarm_state);
        swGPSSearch = findViewById(R.id.sw_gps_search);
        tvRtuDev = findViewById(R.id.tv_rtu_dev);
        tvDevReset = findViewById(R.id.tv_dev_reset);
        tvDevOpen = findViewById(R.id.tv_dev_open);
        tvTriggerMode = findViewById(R.id.tv_trigger_mode);
        tvWorkFlow = findViewById(R.id.tv_work_flow);
        tvWorkPH = findViewById(R.id.tv_work_ph);
        tvWorkCond = findViewById(R.id.tv_work_cond);
        tvWorkTurb = findViewById(R.id.tv_work_turb);
        tvWorkOxygen = findViewById(R.id.tv_work_oxygen);
        tvWorkTemp = findViewById(R.id.tv_work_temp);
        tvJyPipeLen=findViewById(R.id.tv_jy_pipeLen);
        tvJyCapacity=findViewById(R.id.tv_jy_capacity);
        tvTopCoefficient=findViewById(R.id.tv_top_coefficient);
        tvBottomCoefficient=findViewById(R.id.tv_bottom_coefficient);
        llOrderMode = findViewById(R.id.ll_order_mode);
        llWorkTimes = findViewById(R.id.ll_work_times);
        llWorkPlan = findViewById(R.id.ll_work_plan);
        llWorkRain = findViewById(R.id.ll_work_rain);
        llWorkRainTotal = findViewById(R.id.ll_work_rain_total);
        llWorkWaterHeight = findViewById(R.id.ll_work_water_height);
        llRequestMsgCycle = findViewById(R.id.ll_request_msg_cycle);
        llSettingInfo = findViewById(R.id.ll_setting_info);
        llWorkTrigger = findViewById(R.id.ll_work_trigger);
        ivExpandContract = findViewById(R.id.iv_expand_contract);
        llSampleNextTime = findViewById(R.id.ll_sample_next_time);

        llWorkStep = findViewById(R.id.ll_work_step);
        llWorkFlow = findViewById(R.id.ll_work_flow);
        llWorkPH = findViewById(R.id.ll_work_ph);
        llWorkCond = findViewById(R.id.ll_work_cond);
        llWorkTurb = findViewById(R.id.ll_work_turb);
        llWorkOxygen = findViewById(R.id.ll_work_oxygen);
        llWorkTemp = findViewById(R.id.ll_work_temp);
        llSampleSetOld=findViewById(R.id.ll_sample_set_old);
        llSampleSetNew=findViewById(R.id.ll_sample_set_new);
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        sendData = findViewById(R.id.sendData);
        send = findViewById(R.id.send);
        srlSampleBottles = findViewById(R.id.srl_sample_bottles);
        lvSampleBottles = findViewById(R.id.lv_sample_bottles);
        sampleBottleList = new ArrayList<>();
        mAdapter = new SampleBottleAdapter(LiherCYX7Activity.this);
        lvSampleBottles.setAdapter(mAdapter);
        ivExpandContract.setOnClickListener(LiherCYX7Activity.this);
        tvRefresh.setOnClickListener(LiherCYX7Activity.this);
        tvSysTime.setOnClickListener(LiherCYX7Activity.this);
        tvDevIP.setOnClickListener(LiherCYX7Activity.this);
        tvNetPort.setOnClickListener(LiherCYX7Activity.this);
        tvCypIP.setOnClickListener(LiherCYX7Activity.this);
        tvCypNetPort.setOnClickListener(LiherCYX7Activity.this);
        tvSetMacAddress.setOnClickListener(LiherCYX7Activity.this);
        tvReadData.setOnClickListener(LiherCYX7Activity.this);
        tvSampleMode.setOnClickListener(LiherCYX7Activity.this);
        tvWorkTimes.setOnClickListener(LiherCYX7Activity.this);
        tvWorkPlan.setOnClickListener(LiherCYX7Activity.this);
        tvWorkRain.setOnClickListener(LiherCYX7Activity.this);
        tvWorkWaterHeight.setOnClickListener(LiherCYX7Activity.this);
        tvWorkPH.setOnClickListener(LiherCYX7Activity.this);
        tvWorkCond.setOnClickListener(LiherCYX7Activity.this);
        tvWorkTurb.setOnClickListener(LiherCYX7Activity.this);
        tvWorkOxygen.setOnClickListener(LiherCYX7Activity.this);
        tvWorkTemp.setOnClickListener(LiherCYX7Activity.this);
        tvWorkFlow.setOnClickListener(LiherCYX7Activity.this);
        tvTriggerMode.setOnClickListener(LiherCYX7Activity.this);
        tvRequestMsgCycle.setOnClickListener(LiherCYX7Activity.this);
        tvJYHeight.setOnClickListener(LiherCYX7Activity.this);
        tvJyTime.setOnClickListener(LiherCYX7Activity.this);
        tvJyThreshold.setOnClickListener(LiherCYX7Activity.this);
        tvDevEV.setOnClickListener(LiherCYX7Activity.this);
        tvPyTime.setOnClickListener(LiherCYX7Activity.this);
        tvWashTime.setOnClickListener(LiherCYX7Activity.this);
        tvInternetCID.setOnClickListener(LiherCYX7Activity.this);
        tvDatLow.setOnClickListener(LiherCYX7Activity.this);
        tvUpdateMsgCycle.setOnClickListener(LiherCYX7Activity.this);
        tvSampleBottleNum.setOnClickListener(LiherCYX7Activity.this);
        tvRtuDev.setOnClickListener(LiherCYX7Activity.this);
        tvDevReset.setOnClickListener(LiherCYX7Activity.this);
        tvDevOpen.setOnClickListener(LiherCYX7Activity.this);
        send.setOnClickListener(LiherCYX7Activity.this);
        tvDevBump.setOnClickListener(LiherCYX7Activity.this);
        tvDevStop.setOnClickListener(LiherCYX7Activity.this);
        tvJyPipeLen.setOnClickListener(LiherCYX7Activity.this);

        tvTopCoefficient.setOnClickListener(LiherCYX7Activity.this);
        tvBottomCoefficient.setOnClickListener(LiherCYX7Activity.this);
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

        sampleBox = new SampleBox();
        LoadingDialog.Builder loadBuilder = new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage("正在加载...")
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
        lvSampleBottles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (sampleBottleList != null && sampleBottleList.size() > position) {
                    SampleBottle sampleBottle = sampleBottleList.get(position);
                    List<JSONObject> jat = new ArrayList<>();
                    if (sampleBottle.getBlueMac() == null || "000000000000".equals(sampleBottle.getBlueMac().trim()) || sampleBottle.getBlueMac().trim().length() != 12) {
                     /*   JSONObject jo0 = new JSONObject();
                        jo0.put("MyName", "装瓶");
                        jo0.put("MyObject", sampleBottle);
                        jat.add(jo0);*/
                        currentSampleBottle = sampleBottle;
                        if (currentSampleBottle.getBottleNo() < 1) {
                            refreshBottleList();
                            return;
                        }
                        startActivityForResult(new Intent(LiherCYX7Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);
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
                    if (sampleBottle.getIsLock() == 1) {
                        JSONObject jo2 = new JSONObject();
                        jo2.put("MyName", "启用");
                        jo2.put("MyObject", sampleBottle);
                        jat.add(jo2);
                    } else {
                        JSONObject jo2 = new JSONObject();
                        jo2.put("MyName", "禁用");
                        jo2.put("MyObject", sampleBottle);
                        jat.add(jo2);
                       /* JSONObject jo4 = new JSONObject();
                        jo4.put("MyName", "留样");
                        jo4.put("MyObject", sampleBottle);
                        jat.add(jo4);*/
                    }
                    new SelecterObjectDailog(LiherCYX7Activity.this, sampleBottle.getBottleNo() + "号瓶操作指令", jat, Constant.Dailog_Select_SampleBottle_Action, LiherCYX7Activity.this);

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
        if (bleCallBack != null && mble.mBleCallBack != bleCallBack) {
            mble.mBleCallBack = bleCallBack;
        }
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
            showMsg(2, "蓝牙连接已中断，请返回扫描连接后重试");
            if (isBack < 1) {
                isBack++;
                return;
            } else {
                finish();
            }
        }
        isBack = 0;
        addLogText(sendString, 0, sendType);
        insertLog(mDeviceAddress, sampleId, sendType == Constant.Param_Write_PutDown_Bottle ? ("确认强制卸瓶," + sendString) : sendString);
        final byte[] bytes = tmpBytes;
        currentSendType = sendType;
        if (LiherCYX7Activity.this.isFinishing()) {
            return;
        }
        loadingDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mSendCharact, bytes)) {
                    addLogText((mble != null ? mble.getSendDataLenMax() + "MTU," : "") + "发送失败！", 0, Constant.Return_BLE_False);
                    currentSendType = 0;

                } else if (!mble.waitReturn(mReceiveCharact, 15 * 1000)) {
                    addLogText(getResources().getString(R.string.ble_rtn_timeout), 0, Constant.Return_BLE_False);
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
                addLogText("蓝牙已断开", 2, Constant.Return_Ble_ConnectStop);
                // if (StringUtils.isEmpty(tempBottleMac))finish();
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
                            case Constant.Param_Read_ProInfo:
                                currentReceiveType = Constant.Return_Read_ProInfo;
                                break;
                            case Constant.Param_Read_LLJInfo:
                                currentReceiveType = Constant.Return_Read_LLJInfo;
                                break;
                            case Constant.Param_Read_Detail:
                                currentReceiveType = Constant.Return_Read_Detail;
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
                            case Constant.Param_OPEN_LOCK:
                                currentReceiveType = Constant.Return_OPEN_LOCK;
                                break;
                            case Constant.PARAM_DX3_SWJ_Data:
                                currentReceiveType = Constant.Return_DX3_SWJ_Data;
                                break;
                            case Constant.Param_DXCYX3_WCS:
                                currentReceiveType = Constant.Return_DXCYX3_WCS;
                                break;
                            case Constant.PARAM_Work_Plan:
                                currentReceiveType = Constant.RETURN_Work_Plan;
                                break;
                            case Constant.PARAM_Write_Box_Sample_Plan:
                                currentReceiveType = Constant.RETURN_Write_Box_Sample_Plan;
                                break;
                            case Constant.Param_Wirte_Rainth:
                                currentReceiveType = Constant.Return_Wirte_Rainth;
                                break;
                            case Constant.Param_Write_JyTime:
                                currentReceiveType = Constant.Return_Write_JyTIme;
                                break;
                            case Constant.Param_Write_JyThreshold:
                                currentReceiveType = Constant.Return_Write_JyThreshold;
                                break;
                            case Constant.Param_Write_PyTime:
                                currentReceiveType = Constant.Return_Write_PyTIme;
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
                            case Constant.Param_Write_SWJ_DEVIP:
                                currentReceiveType = Constant.Return_Write_SWJ_DEVIP;
                                break;
                            case Constant.Param_Write_SWJ_NetPort:
                                currentReceiveType = Constant.Return_Write_SWJ_NetPort;
                                break;
                            case Constant.Param_Write_Water_Height:
                                currentReceiveType = Constant.Return_Write_Water_Height;
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
                            case Constant.Param_Write_BatLow:
                                currentReceiveType = Constant.Return_Write_BatLow;
                                break;
                            case Constant.PARAM_DX3_Bump_Stop:
                                currentReceiveType = Constant.Return_DX3_Bump_Stop;
                                break;
                            case Constant.PARAM_DX3_Bump1:
                                currentReceiveType = Constant.Return_DX3_Bump1;
                                break;
                            case Constant.PARAM_Dev_Restart:
                                currentReceiveType = Constant.Return_Dev_Restart;
                                break;
                            case Constant.PARAM_DX3_Bump2:
                                currentReceiveType = Constant.Return_DX3_Bump2;
                                break;
                            case Constant.Param_Write_BatEV:
                                currentReceiveType = Constant.Return_Write_BatEV;
                                break;
                            case Constant.Param_Write_WashTime:
                                currentReceiveType = Constant.Return_Write_WashTime;
                                break;
                            case Constant.PARAM_Clean_Box_Sample_Plan:
                                currentReceiveType = Constant.RETURN_Clean_Box_Sample_Plan;
                                break;
                            case Constant.Param_Write_PutDown_Bottle:
                                currentReceiveType = Constant.Return_Write_PutDown_Bottle;
                                break;
                            case Constant.Param_Write_Trigger_Mode:
                                currentReceiveType = Constant.Return_Write_Trigger_Mode;
                                break;
                            case Constant.Param_Wirte_PHTH:
                                currentReceiveType = Constant.Return_Wirte_PHTH;
                                break;
                            case Constant.Param_Wirte_CondTH:
                                currentReceiveType = Constant.Return_Wirte_CondTH;
                                break;
                            case Constant.Param_Wirte_TurbTH:
                                currentReceiveType = Constant.Return_Wirte_TurbTH;
                                break;
                            case Constant.Param_Wirte_OxygenTH:
                                currentReceiveType = Constant.Return_Wirte_OxygenTH;
                                break;
                            case Constant.Param_Wirte_WaterTempTH:
                                currentReceiveType = Constant.Return_Wirte_WaterTempTH;
                                break;
                            case Constant.Param_Wirte_FlowTH:
                                currentReceiveType = Constant.Return_Wirte_FlowTH;
                                break;
                            case Constant.PARAM_CYX_CYP_NetInfo:
                                currentReceiveType = Constant.RETURN_CYX_CYP_NetInfo;
                                break;
                            case Constant.PARAM_PipeLen_Write:
                                currentReceiveType = Constant.RETURN_PipeLen_Write;
                                break;
                            case Constant.PARAM_Capacity_Write:
                                currentReceiveType = Constant.RETURN_Capacity_Write;
                                break;
                            case Constant.PARAM_Samflow_Write:
                                currentReceiveType = Constant.RETURN_Samflow_Write;
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
                addLogText("日志：" + logStr, 2, logStr.length());
            }
        }

        @Override
        public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {
            if (!StringUtils.isEmpty(stateMsg)) {
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close) + "," + stateMsg, 2, Constant.Return_Ble_ConnectStop);

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
                startActivityForResult(new Intent(LiherCYX7Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_Box_MAC_Address);
                break;
            case R.id.iv_expand_contract:
                if (llSettingInfo.getVisibility() == View.VISIBLE) {
                    ivExpandContract.setSelected(false);
                    llSettingInfo.setVisibility(View.GONE);
                } else {
                    ivExpandContract.setSelected(true);
                    llSettingInfo.setVisibility(View.VISIBLE);
                }
                break;

            case R.id.tv_refresh:
                refreshDetails();
                break;
            case R.id.tv_sample_mode:
                showSelectSampleModeMenu();
                break;
            case R.id.tv_sys_time:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherCYX7Activity.this);

                break;
            case R.id.tv_dev_ip:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_SWJ_DevIP, "设置212平台地址", "平台地址：", "", "请输入设备212数据上传的平台端IP地址", LiherCYX7Activity.this);
                break;
            case R.id.tv_net_port:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_SWJ_NetPort, HJBleApplication.getAppContext().getString(R.string.set_net_port), HJBleApplication.getAppContext().getString(R.string.net_port), "", "请输入设备212数据上传的平台端口号", LiherCYX7Activity.this);
                break;
            case R.id.tv_cyp_ip:
            case R.id.tv_cyp_net_port:
                NetInfo netInfo = new NetInfo();
                netInfo.setProtocolType(-1);
                netInfo.setNo(0);
                new NetInfoInputDailog(LiherCYX7Activity.this, Constant.Dialog_CYP_NetInfo_Input, "重置采样通道的通信地址", netInfo, LiherCYX7Activity.this);
                break;
            case R.id.tv_work_times:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_BLE_CycTime, "等时采样周期", "等时周期（分钟）:", "", LiherCYX7Activity.this);
                break;
            case R.id.tv_dev_reset:
                new HitiStringDailog(LiherCYX7Activity.this, Constant.Dialog_Dev_Reset, null, "设备复位", "请确认是否进行设备复位？", LiherCYX7Activity.this);
                break;
            case R.id.tv_read_data:
                List<JSONObject> jatParams = new ArrayList<>();
                JSONObject jo0 = new JSONObject();
                jo0.put("MyName", "读取参数数据");
                jo0.put("MyObject", "Read SamInf");
                jatParams.add(jo0);
                JSONObject jo1 = new JSONObject();
                jo1.put("MyName", "参数校准配置");
                jo1.put("MyObject", "");
                jatParams.add(jo1);

                new SelecterObjectDailog(LiherCYX7Activity.this, "操作选择", jatParams, Constant.Dialog_Select_CYX_Read_Data, LiherCYX7Activity.this);
                //   sendLHBleData("Read SamInf", Constant.Param_Read_LLJInfo);
                break;
            case R.id.tv_rtu_dev:

                startActivity(LiherCYXRTUActivity.getIntent(LiherCYX7Activity.this, mDeviceName, mDeviceAddress, isConfig));
                break;
            case R.id.tv_dev_open:
                sendLHBleData("ll", Constant.Param_OPEN_LOCK);
                break;
            case R.id.tv_jy_time:
                showMsg(2, "请通过设置采样高程自动计算进样时长！");
                break;
            case R.id.tv_jy_height:
                new DXCYX3JYTimeInputDailog(LiherCYX7Activity.this, Constant.Dailog_Box_JyTime, "通过采样高程调整进样时长", sampleBox.getJyTimeD() > 0 ? (sampleBox.getJyTimeD() + "") : "", StringUtils.isEmpty(tvJYHeight.getText().toString()) ? "" : tvJYHeight.getText().toString().trim(), LiherCYX7Activity.this);
                break;
            case R.id.tv_jy_threshold:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Box_JyThreshold, "每次采样的进样阀值", "进样阀值(脉冲):", "", LiherCYX7Activity.this);
                break;
            case R.id.tv_py_time:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Box_PyTime, "每次排空水样的时长", "排样时长(秒):", "", LiherCYX7Activity.this);
                break;
            case R.id.tv_wash_time:
                //  new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_DX3_WashTime, "每次润洗时长", "润洗时长(秒):", "", LiherCYX7Activity.this);

                if (sampleBox.getWashTimeD() > 0) {
                    new HitiStringDailog(LiherCYX7Activity.this, Constant.Dailog_DX3_WashTime, 0, "关闭确认", "确认关闭润洗流程吗？", LiherCYX7Activity.this);
                } else {
                    if (!StringUtils.isEmpty(tvJyTime.getText().toString()) && StringUtils.isDouble(tvJyTime.getText().toString().substring(0, tvJyTime.getText().toString().length() - 1))) {
                        double t = Double.parseDouble(tvJyTime.getText().toString().substring(0, tvJyTime.getText().toString().length() - 1));
                        sendLHBleData("Write RxTime=" + (int) DoubleUtils.round(t, 0), Constant.Param_Write_WashTime);
                    } else {
                        showMsg(2, "请先录入采样高程，才可以录入润洗时长");
                    }
                }


                break;
            case R.id.tv_internet_cid:
                sendLHBleData("Read CID", Constant.Param_CYX_ICCID);
                break;
            case R.id.tv_bat_low:
                if (sampleBox == null || sampleBox.getBatLow() <= 0) {
                    showMsg(2, "当前采样箱版本不支持设置低压阈值，请尽快升级设备程序");

                } else {
                    new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Input_BatLow, "修改低压阈值", "低压阈值(V):", sampleBox.getBatLow() > 0 ? (DoubleUtils.div(sampleBox.getBatLow(), 100d, 2) + "") : "", "请输入正确的低压阈值(V)，10.00~24.00", LiherCYX7Activity.this);

                }

                break;
            case R.id.tv_sample_bottle_num:
                //20260108 陈备提出因为硬件程序还不够完善，暂时隐藏该设置功能
              /*  if (sampleBox != null && !"13".equals(sampleBox.getSampleMode())) {
                    new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_Box_Sample_Bottle, "修改当前采样瓶号", "采样瓶号:", "", LiherCYX7Activity.this);
                }*/
                break;
            case R.id.tv_work_rain:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Box_RainValue, "触发采样雨量值", "触发雨量(毫米):", "", LiherCYX7Activity.this);
                break;
            case R.id.tv_work_water_height:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Box_Water_HeightValue, "触发采样液位值", "液位阀值(米):", "", "20米以内，保留3位小数", LiherCYX7Activity.this);
                break;
            case R.id.tv_request_msg_cycle:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Box_RequestMsg_Cycle, "同步平台指令周期", "同步周期(分钟):", "", LiherCYX7Activity.this);
                break;

            case R.id.tv_update_msg_cycle:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Box_UpdateMsg_Cycle, "上传数据间隔周期", "上传周期(分钟):", "", LiherCYX7Activity.this);
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
            case R.id.tv_dev_bump:
                new CYXBumpDialog(LiherCYX7Activity.this, Constant.Dialog_CYX_Bump, LiherCYX7Activity.this);
                break;

            case R.id.tv_dev_stop:
                new HitiStringDailog(LiherCYX7Activity.this, Constant.Dialog_Dev_Restart, null, "设备重启(急停)", "确认让设备程序重启，流程急停吗？", LiherCYX7Activity.this);

                break;
            case R.id.tv_dev_ev:
                if (!HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {//无生成维修权限的用户无版本更新功能
                    showMsg(2, "您的账号无生产维修权限，无法校准电压值");
                } else {
                    new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Input_BatEV, "校正电压值", "电压值(V)：", "", "请输入采样箱当前正确电压数值", LiherCYX7Activity.this);
                }
                break;
            case R.id.tv_trigger_mode:
                List<JSONObject> jat = new ArrayList<>();
                JSONObject joWaterHeight = new JSONObject();
                joWaterHeight.put("MyName", "水深触发");
                joWaterHeight.put("MyObject", 1);
                JSONObject joRain = new JSONObject();
                joRain.put("MyName", "雨量触发");
                joRain.put("MyObject", 2);
                JSONObject joPH = new JSONObject();
                joPH.put("MyName", "pH触发");
                joPH.put("MyObject", 3);
                JSONObject joCond = new JSONObject();
                joCond.put("MyName", "电导率触发");
                joCond.put("MyObject", 4);
                JSONObject joTurb = new JSONObject();
                joTurb.put("MyName", "浊度触发");
                joTurb.put("MyObject", 5);
                JSONObject joOxygen = new JSONObject();
                joOxygen.put("MyName", "溶解氧触发");
                joOxygen.put("MyObject", 6);
                JSONObject joWaterTemp = new JSONObject();
                joWaterTemp.put("MyName", "水温触发");
                joWaterTemp.put("MyObject", 7);
                JSONObject joFlow = new JSONObject();
                joFlow.put("MyName", "瞬时流量触发");
                joFlow.put("MyObject", 8);
                JSONObject joYW = new JSONObject();
                joYW.put("MyName", "液位触发");
                joYW.put("MyObject", 9);
                if (sampleBox.getTrigger() > 0) {
                    byte[] triggers = StringUtils.getDateBit16(sampleBox.getTrigger());
                    joWaterHeight.put("isChecked", triggers[0] == 1);
                    joRain.put("isChecked", triggers[1] == 1);
                    joPH.put("isChecked", triggers[2] == 1);
                    joCond.put("isChecked", triggers[3] == 1);
                    joTurb.put("isChecked", triggers[4] == 1);
                    joOxygen.put("isChecked", triggers[5] == 1);
                    joWaterTemp.put("isChecked", triggers[6] == 1);
                    joFlow.put("isChecked", triggers[7] == 1);
                    joYW.put("isChecked", triggers[8] == 1);
                }

                jat.add(joWaterHeight);
                jat.add(joRain);
                jat.add(joPH);
                jat.add(joCond);
                jat.add(joTurb);
                jat.add(joOxygen);
                jat.add(joWaterTemp);
                jat.add(joFlow);
                jat.add(joYW);
                new SelecterObjectListDailog(LiherCYX7Activity.this, "选择触发条件", jat, Constant.Dialog_Select_TriggerMode, LiherCYX7Activity.this);
                break;
            case R.id.tv_work_ph:
                new CYX7PHThInputDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Work_PH, "pH阈值配置", sampleBox.getPhThMax() > 0 ? DoubleUtils.div(sampleBox.getPhThMax(), 100d, 2) : 0,
                        sampleBox.getPhThMin() > 0 ? DoubleUtils.div(sampleBox.getPhThMin(), 100d, 2) : 0, LiherCYX7Activity.this);
                break;
            case R.id.tv_work_cond:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Work_Cond, "阈值配置", "电导率(us/cm)触发阈值",
                        sampleBox.getCondTh() > Constant.DefaultBigValue ? DoubleUtils.div(sampleBox.getCondTh(), 100d, 2) + "" : "", "阈值配置，保留两位小数", LiherCYX7Activity.this);
                break;
            case R.id.tv_work_turb:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Work_Turb, "阈值配置", "浊度(NTU)触发阈值",
                        sampleBox.getTurbTh() > 0 ? DoubleUtils.div(sampleBox.getTurbTh(), 100d, 2) + "" : "", "阈值配置，保留两位小数", LiherCYX7Activity.this);
                break;
            case R.id.tv_work_oxygen:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Work_Oxygen, "阈值配置", "溶解氧(mg/L)触发阈值",
                        sampleBox.getOxygenTh() > 0 ? DoubleUtils.div(sampleBox.getOxygenTh(), 100d, 2) + "" : "", "阈值配置，保留两位小数", LiherCYX7Activity.this);
                break;
            case R.id.tv_work_temp:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Work_WaterTemp, "阈值配置", "水温(°C)触发阈值",
                        sampleBox.getWaterTempTh() > 0 ? DoubleUtils.div(sampleBox.getWaterTempTh(), 100d, 2) + "" : "", "阈值配置，保留两位小数", LiherCYX7Activity.this);
                break;
            case R.id.tv_work_flow:
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Work_Flow, "阈值配置", "瞬时流量(m³/s)触发阈值",
                        sampleBox.getFlowTh() > 0 ? DoubleUtils.div(sampleBox.getFlowTh(), 100d, 2) + "" : "", "阈值配置，保留两位小数", LiherCYX7Activity.this);
                break;
            case R.id.tv_jy_pipeLen:
                showMsg(2, "设置管长越长，进样时间越短！");
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_JY_Pipelen, "采样管长配置", "采样管长(M)",
                        sampleBox.getPipelen()>0?(DoubleUtils.div(sampleBox.getPipelen(),1000d,3)+""):"", "管长越长，进样时间越短，保留3位小数", LiherCYX7Activity.this);

                break;
            case R.id.tv_jy_capacity:
             /*   new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dialog_JY_Capacity, "采样管容量配置", "采样管容量(mL)",
                        sampleBox.getCapacity()>0?(sampleBox.getCapacity()+""):"", "管容量，整数毫升值", LiherCYX7Activity.this);*/
                break;
            case R.id.tv_top_coefficient:
            case R.id.tv_bottom_coefficient:
                new HitiInputCoefficientDailog(LiherCYX7Activity.this,Constant.Dialog_JY_Coefficient,sampleBox.getTopCoefficient100(),sampleBox.getBottomCoefficient100(),null,LiherCYX7Activity.this);

                break;
        }
    }

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.Dialog_CYX_Bump:
                 int bump= (int) target;
                 switch ( bump){
                     case 0:
                         new HitiStringDailog(LiherCYX7Activity.this, Constant.HitiStrTargetDialog_DX3_Bump, 0, "水泵停止控制", "确认停止水泵转动吗？", LiherCYX7Activity.this);

                         break;
                     case 1:
                         if ("01".equals(sampleBox.getDevStatus())||"001".equals(sampleBox.getDevStatus())){
                             showMsg(2, "设备正在工作中，无法单点控制水泵！");
                         }else {
                             new HitiStringDailog(LiherCYX7Activity.this,Constant.HitiStrTargetDialog_DX3_Bump,1,"水泵正转确认","提示:请先确认蓝牙瓶是开阀状态或瓶位上没有瓶子堵住进样嘴，再水泵正转抽水，否则会蹦管!",R.color.color_red, LiherCYX7Activity.this);
                             showMsg(2,"请先确认蓝牙瓶是开阀状态或瓶位上没有瓶子堵住进样嘴");
                         }
                         break;
                     case 2:
                         if ("01".equals(sampleBox.getDevStatus()) || "001".equals(sampleBox.getDevStatus())) {
                             showMsg(2, "设备正在工作中，无法单点控制水泵！");
                         } else {
                             new HitiStringDailog(LiherCYX7Activity.this, Constant.HitiStrTargetDialog_DX3_Bump, 2, "水泵反转控制", "确认进行水泵反转排水吗？", LiherCYX7Activity.this);

                         }
                         break;


                 }
                 break;
            case Constant.Dialog_JY_Pipelen:
                String sp= (String) target;
                if (StringUtils.isDouble(sp)&&Double.parseDouble(sp)>0){
                    sendLHBleData("Write PipeLen="+(int)Math.round(Double.parseDouble(sp)*1000),Constant.PARAM_PipeLen_Write);
                }else {
                    showMsg(2,"请输入正确采样管长（M），大于0的值");
                }
                break;
            case Constant.Dialog_JY_Capacity:
                String sc= (String) target;
                if (StringUtils.isInt(sc)&&Integer.parseInt(sc)>0){
                    sendLHBleData("Write Capacity="+Integer.parseInt(sc),Constant.PARAM_Capacity_Write);
                }else {
                    showMsg(2,"请输入正确采样管容量（mL），大于0的正整数");
                }
                break;
            case Constant.Dialog_JY_Coefficient:
                String content= (String) target;
                sendLHBleData("Write Samflow="+content,Constant.PARAM_Samflow_Write);
                break;
            case Constant.Dialog_Select_TriggerMode:
                List<JSONObject> mDatas = (List<JSONObject>) target;
                byte[] bytes = StringUtils.getDateBit16(0);
                if (mDatas != null && mDatas.size() > 0) {
                    for (JSONObject jo : mDatas) {
                        int i = jo.getInteger("MyObject");
                        if (i > 0) {
                            bytes[i - 1] = 1;
                        }
                    }
                }
                int triggerValue = StringUtils.bit162Int(bytes);
                sendLHBleData("Write Trigger=" + triggerValue, Constant.Param_Write_Trigger_Mode);
                break;
            case Constant.HitiStrTargetDialog_PutDown_Bottle:
                String strBottleNo = "00" + currentSampleBottle.getBottleNo();
                if (strBottleNo.length() == 4) strBottleNo = strBottleNo.substring(1);
                sendLHBleData("Write Subaddr=" + strBottleNo + "000000000000-0000000000000000", Constant.Param_Write_PutDown_Bottle);
                break;
            case Constant.Dailog_Input_CyNum:
                String strNum = (String) target;
                if (StringUtils.isInt(strNum)) {
                    int scale = Integer.parseInt(strNum);
                    if (scale <= 24) {
                        sendLHBleData("Write cyNum=" + scale, Constant.Param_Wirte_CyNum);
                        return;
                    }
                }
                showMsg(2, "请输入正确等比例采样次数,1~24");
                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Input_CyNum, "设置等比例采样", "等比例采样次数", "", LiherCYX7Activity.this);

                break;
            case Constant.Dialog_CYX_Work_PH:
                String strPH = (String) target;
                if (strPH.contains("#") && strPH.split("#").length == 2) {
                    sendLHBleData("Write PHTh=" + strPH.trim(), Constant.Param_Wirte_PHTH);
                }
                showMsg(2, strPH + ",请输入正确pH触发阈值上下限");

                break;
            case Constant.Dialog_CYX_Work_Cond:
                String strCond = (String) target;
                if (StringUtils.isDouble(strCond)) {
                    int condTh = (int) (DoubleUtils.round(Double.parseDouble(strCond.trim()), 2) * 100);
                    if (condTh > Constant.DefaultBigValue) {
                        sendLHBleData("Write CondTh=" + condTh, Constant.Param_Wirte_CondTH);
                        return;
                    }
                }
                showMsg(2, "请输入正确触发阈值,保留两位小数");

                break;
            case Constant.Dialog_CYX_Work_Turb:
                String strTurb = (String) target;
                if (StringUtils.isDouble(strTurb)) {
                    int turbTh = (int) (DoubleUtils.round(Double.parseDouble(strTurb.trim()), 2) * 100);
                    if (turbTh > Constant.DefaultBigValue) {
                        sendLHBleData("Write TurbTh=" + turbTh, Constant.Param_Wirte_TurbTH);
                        return;
                    }
                }
                showMsg(2, "请输入正确触发阈值,保留两位小数");

                break;
            case Constant.Dialog_CYX_Work_Oxygen:
                String strOxygen = (String) target;
                if (StringUtils.isDouble(strOxygen)) {
                    int oxygenTh = (int) (DoubleUtils.round(Double.parseDouble(strOxygen.trim()), 2) * 100);
                    if (oxygenTh > Constant.DefaultBigValue) {
                        sendLHBleData("Write OxygenTh=" + oxygenTh, Constant.Param_Wirte_OxygenTH);
                        return;
                    }
                }
                showMsg(2, "请输入正确触发阈值,保留两位小数");

                break;
            case Constant.Dialog_CYX_Work_WaterTemp:
                String strWaterTemp = (String) target;
                if (StringUtils.isDouble(strWaterTemp)) {
                    int waterTempTh = (int) (DoubleUtils.round(Double.parseDouble(strWaterTemp.trim()), 2) * 100);
                    if (waterTempTh > Constant.DefaultBigValue) {
                        sendLHBleData("Write WaterTempTh=" + waterTempTh, Constant.Param_Wirte_WaterTempTH);
                        return;
                    }
                }
                showMsg(2, "请输入正确触发阈值,>0");

                break;
            case Constant.Dialog_CYX_Work_Flow:
                String strFlow = (String) target;
                if (StringUtils.isDouble(strFlow)) {
                    int flowTh = (int) (DoubleUtils.round(Double.parseDouble(strFlow.trim()), 2) * 100);
                    if (flowTh > Constant.DefaultBigValue) {
                        sendLHBleData("Write FlowTh=" + flowTh, Constant.Param_Wirte_FlowTH);
                        return;
                    }
                }
                showMsg(2, "请输入正确触发阈值,保留两位小数");

                break;
            case Constant.Dailog_BLE_CycTime:
                String str = (String) target;
                if (StringUtils.isInt(str)) {
                    int cycTime = Integer.parseInt(str);
                    sendLHBleData("Write cycleTime=" + cycTime, Constant.Param_Wirte_SamPeriod);
                }

                break;
            case Constant.Dailog_Input_SampleNo:

                if (target != null && target instanceof String) {
                    String sampleNo = (String) target;
                    if (sampleNo.length() > 2 && sampleNo.length() < 16) {
                        String strMsg = "00" + currentSampleBottle.getBottleNo() + tempBottleMac + "-" + StringUtils.addCharForNum(sampleNo.trim(), 16, '_');
                        if (strMsg.length() == 33) strMsg = strMsg.substring(1);
                        sendLHBleData("Write Subaddr=" + strMsg, Constant.Param_Write_Subaddr);
                    }
                }
                break;
            case Constant.Dailog_Box_RainValue:
                String rainStr = (String) target;
                if (StringUtils.isDouble(rainStr)) {
                    Double rainValue = Double.parseDouble(rainStr);
                    double rainNum = DoubleUtils.mul(rainValue, 10d);
                    sendLHBleData("Write rainNum=" + (int) rainNum, Constant.Param_Wirte_Rainth);
                } else {
                    showMsg(2, rainStr + "，请输入正确的雨量阈值（0.2~40）");
                }

                break;

            case Constant.Dailog_Box_JyTime:
                String jyTimeStr = (String) target;
                if (jyTimeStr != null && StringUtils.isDouble(jyTimeStr)) {
                    double jyTime = Double.parseDouble(jyTimeStr);
                    if (jyTime >= 5 && jyTime <= 300) {
                        sendLHBleData("Write jyTime=" + (int) DoubleUtils.round(jyTime, 0), Constant.Param_Write_JyTime);
                        return;
                    }
                }
                showMsg(2, "请输入正确的进样时长，5~300秒之间");

                break;
            case Constant.Dailog_Box_PyTime:
                String pyTimeStr = (String) target;
                if (pyTimeStr != null && StringUtils.isInt(pyTimeStr)) {
                    double pyTime = Double.parseDouble(pyTimeStr);
                    if (pyTime >= 10 && pyTime <= 300) {
                        sendLHBleData("Write pyTime=" + (int) DoubleUtils.round(pyTime, 0), Constant.Param_Write_PyTime);
                        return;
                    }
                }
                showMsg(2, "请输入正确的排样时长，10~300秒之间");

                break;
            case Constant.Dialog_CYX_Input_BatLow:
                String batLowStr = (String) target;
                if (batLowStr != null && StringUtils.isDouble(batLowStr)) {
                    int batLow = (int) (DoubleUtils.round(Double.parseDouble(batLowStr), 2) * 100);
                    if (batLow >= 1000 && batLow <= 2400) {
                        sendLHBleData("Write batLow=" + batLow, Constant.Param_Write_BatLow);
                        return;
                    }
                }
                showMsg(2, "请输入正确的低压阈值(V)，10.00~24.00");

                break;
            case Constant.Dailog_Box_JyThreshold:
                String jyThresholdStr = (String) target;
                if (jyThresholdStr != null && StringUtils.isDouble(jyThresholdStr)) {
                    double jyThreshold = Integer.parseInt(jyThresholdStr);
                    if (jyThreshold > 300 && jyThreshold <= 6000) {
                        sendLHBleData("Write jyTime=" + (int) DoubleUtils.round(jyThreshold, 0), Constant.Param_Write_JyThreshold);
                        return;
                    }
                }
                showMsg(2, "请输入正确的进样阀值，301~6000之间");

                break;
            case Constant.Dailog_Box_TimeMode_Delay:
                int delay = (int) target;
                if (delay > 0 && delay <= 800) {
                    sendLHBleData("Write Effective=" + DateUtil.long2time10(System.currentTimeMillis() + delay * 60 * 1000L).substring(2), Constant.Param_Write_TimeMode_Delay);
                    return;
                }
                showMsg(2, "请输入正确的生效时刻，13个小时以内");

                break;
            case Constant.Dailog_Box_Water_HeightValue:
                String waterHeight = (String) target;
                if (StringUtils.isDouble(waterHeight)) {
                    double waterValue = DoubleUtils.round(Double.parseDouble(waterHeight), 3);

                    if (waterValue >= 20 || waterValue < 0.001) {
                        showMsg(2, "请输入正确液位阀值，20以内，保留3位小数");
                    } else {
                        sendLHBleData("Write waterHeight=" + (int) (waterValue * 1000), Constant.Param_Write_Water_Height);
                    }
                } else {
                    showMsg(2, "请输入正确液位阀值，20以内，保留3位小数");

                }
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
            case Constant.Dialog_SWJ_DevIP:
                String strIP = (String) target;
                if (!StringUtils.isEmpty(strIP) && strIP.length() < 40) {
                    sendLHBleData("Write net=" + strIP, Constant.Param_Write_SWJ_DEVIP);
                } else {
                    showMsg(2, "请输入正确的平台地址");
                }

                break;
            case Constant.Dialog_SWJ_NetPort:
                String strPort = (String) target;
                if (StringUtils.isInt(strPort)) {
                    sendLHBleData("Write port=" + Integer.parseInt(strPort), Constant.Param_Write_SWJ_NetPort);
                } else {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_port));
                }
                break;
            case Constant.Dailog_BLE_MACAddress:
                String strTarget = (String) target;

                sendLHBleData("Write BleAddr=" + strTarget, Constant.Param_Write_Box_Mac_Address);
                break;
            case Constant.RequestCode_Scan_Add_SampleNo:
                showMsg(2, "请扫描录入样品编号");
                startActivityForResult(new Intent(LiherCYX7Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_Add_SampleNo);
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
            case Constant.HitiStrTargetDialog_DX3_Bump:
                int bumpOrder = (int) target;
                if (bumpOrder == 0) {
                    sendLHBleData("Write bump=0", Constant.PARAM_DX3_Bump_Stop);
                } else if (bumpOrder == 1) {
                    sendLHBleData("Write bump=1", Constant.PARAM_DX3_Bump1);
                } else if (bumpOrder == 2) {
                    sendLHBleData("Write bump=2", Constant.PARAM_DX3_Bump2);
                }
                break;
            case Constant.Dialog_Dev_Restart:
                sendLHBleData("Set SysReset", Constant.PARAM_Dev_Restart);
                break;
            case Constant.Dialog_CYX_Input_BatEV:
                String batEvStr = (String) target;
                if (batEvStr != null && StringUtils.isDouble(batEvStr)) {
                    int batEv = (int) (DoubleUtils.round(Double.parseDouble(batEvStr), 2) * 100);
                    if (batEv > 0 && batEv <= 3000) {
                        sendLHBleData("Write vin=" + batEv, Constant.Param_Write_BatEV);
                        return;
                    }
                }
                showMsg(2, "请输入正确的电压值(V)，0.00~30.00");

                break;
            case Constant.Dailog_DX3_WashTime:
               /* String washTimeStr = (String) target;
                if (washTimeStr != null && StringUtils.isInt(washTimeStr)) {
                    int washTime = Integer.parseInt(washTimeStr);
                    if (sampleBox.getJyTime() <= 0) {
                        showMsg(2, "请先录入采样高程，才可以录入润洗时长");
                        return;
                    } else if (washTime > sampleBox.getJyTime()) {
                        showMsg(2, "设置的润洗时长，不可大于当前进样时长！");
                        return;
                    } else if (washTime > 0 && washTime <= 600) {
                        sendLHBleData("Write RxTime=" + washTime, Constant.Param_Write_WashTime);
                        return;
                    }
                }
                showMsg(2, "请输入正确的润洗时长，1~600秒");*/

                sendLHBleData("Write RxTime=" + 0, Constant.Param_Write_WashTime);

                break;
            case Constant.Dialog_Dev_Reset:
                sendLHBleData("ln", Constant.Param_DEV_Reset);
                break;
            case Constant.Dialog_CYX_SWJ:
                if (target != null && target instanceof DX3SWJInfo) {//读取到水文数据的情况

                } else {//未读取到数据的情况

                }
                break;
            case Constant.Dialog_CYX_Wcs:
                if (target != null && target instanceof DX3WcsInfo) {//读取到五参数数据的情况

                } else {//未读取到数据的情况

                }
                break;
            case Constant.HitiStrTargetDialog_RTU_SamInf:
                if (target != null && target instanceof RtuLljInfo) {//读取到参数数据的情况

                } else {//未读取到数据的情况

                }
                break;
            case Constant.Dialog_CYP_NetInfo_Input:
                if (target != null && target instanceof NetInfo) {
                    NetInfo netInfo = (NetInfo) target;
                    sendLHBleData("Write CypIpPort=" + netInfo.getStrIp12() + netInfo.getStrPort5(), Constant.PARAM_CYX_CYP_NetInfo);
                }
                break;

        }
    }

    @Override
    public void onItemClick3(View v, int position, Object object, int type) {
        switch (type) {
            case Constant.Dialog_Select_CYX_Read_Data:
                if (position == 0) {
                    sendLHBleData("Read SamInf", Constant.Param_Read_LLJInfo);
                } else if (position == 1) {
                    startActivity(LiherCYXParamSetActivity.getIntent(LiherCYX7Activity.this, mDeviceName, mDeviceAddress, isConfig));
                }
                break;
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
                        startActivityForResult(new Intent(LiherCYX7Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);

                        break;
                    case "卸瓶":
                        String str = "00" + currentSampleBottle.getBottleNo();
                        if (str.length() == 4) str = str.substring(1);
                        if (str.substring(1).equals(sampleBox.getCurrentSampleBottleNo())) {
                            if ("工作中".equals(StringUtils.getCYXRTUStatus(sampleBox.getDevStatus()))) {
                                showMsg(2, "目前正在采样中，不允许卸瓶！");
                                RxTimerUtil.cancel();
                                RxTimerUtil.timer(1000, new RxTimerUtil.IRxNext() {
                                    @Override
                                    public void doNext(long number) {
                                        refreshDetails();
                                    }
                                });
                                return;
                            } else if (sampleBox.getCurrentNum() < sampleBox.getCyNum() && sampleBox.getCyNum() > 1 && sampleBox.getCurrentNum() > 0) {
                                new HitiStringDailog(LiherCYX7Activity.this, Constant.HitiStrTargetDialog_PutDown_Bottle, currentSampleBottle, "强制卸瓶确认", "目前等比例采样" + sampleBox.getCurrentNum() + "/" + sampleBox.getCyNum() + "次还未完成，是否强制卸瓶？", LiherCYX7Activity.this);
                                showMsg(2, "当前等比例采样未完成，请谨慎选择强制卸瓶！");
                                return;
                            }
                        }
                        sendLHBleData("Write Subaddr=" + str + "000000000000-0000000000000000", Constant.Param_Write_Subaddr);

                        break;
                    case "采样":
                        String strNo = "0" + currentSampleBottle.getBottleNo();
                        if (strNo.length() == 3) strNo = strNo.substring(1);
                        if (strNo.equals(sampleBox.getCurrentSampleBottleNo())) {
                            if ("工作中".equals(StringUtils.getCYXRTUStatus(sampleBox.getDevStatus()))) {
                                showMsg(2, "目前正在采样中，无法手动采样！");
                                RxTimerUtil.cancel();
                                RxTimerUtil.timer(1000, new RxTimerUtil.IRxNext() {
                                    @Override
                                    public void doNext(long number) {
                                        refreshDetails();
                                    }
                                });
                                return;
                            } else if (sampleBox.getCurrentNum() < sampleBox.getCyNum() && sampleBox.getCyNum() > 1 && sampleBox.getCurrentNum() > 0) {
                                showMsg(2, "当前等比例采样未完成，无法手动采样！");
                                RxTimerUtil.cancel();
                                RxTimerUtil.timer(1000, new RxTimerUtil.IRxNext() {
                                    @Override
                                    public void doNext(long number) {
                                        refreshDetails();
                                    }
                                });
                                return;
                            }
                        }
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
        //  popupMenu.getMenu().removeItem(R.id.sample_mix_mode);
        popupMenu.getMenu().removeItem(R.id.sample_rain_mode);
        popupMenu.getMenu().removeItem(R.id.sample_water_mode);
        popupMenu.getMenu().removeItem(R.id.sample_test_mode);
        popupMenu.getMenu().removeItem(R.id.sample_jk_mode);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.sample_485_mode:
                        sendLHBleData("Write WorkMode=0", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_time_mode:
                        sendLHBleData("Write WorkMode=1", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_rain_mode:
                        sendLHBleData("Write WorkMode=2", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_mix_mode:
                        sendLHBleData("Write WorkMode=3", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_plan_mode:
                        sendLHBleData("Write WorkMode=4", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_water_mode:
                        sendLHBleData("Write WorkMode=5", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_test_mode:
                        sendLHBleData("Write WorkMode=7", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_cy_num_mode:
                        sendLHBleData("Write WorkMode=8", Constant.Param_Wirte_Mode);
                        return true;
                    case R.id.sample_jk_mode:
                        sendLHBleData("Write SamPoint=000000#000000#000000#000000#000000#000000#", Constant.PARAM_Clean_Box_Sample_Plan);
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
                        llUIMode.setVisibility(View.GONE);
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
                SpannableStringBuilder builder = new SpannableStringBuilder(msgContext);
                //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
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
                case Constant.Return_BLE_False:
                    showMsg(2, msgContext);
                    if (loadingDialog != null) loadingDialog.dismiss();
                    break;
                case Constant.Return_Read_ProInfo:
                    //000000#00#00#00#00#00000#00000#00030#00030#00000#000000000000#0b2503004650##                000000000000#0b2503001468##000000000000#0b2503001486##000000000000#0b2503004037##000000000000#0b2503004036##000000000000#0b2503001071##000000000000#
                    try {
                        String[] proInfo = msgContext.split("#", -1);
                        if (sampleBox == null) sampleBox = new SampleBox();
                        if (proInfo.length >= 36) {
                            sampleBox.setLockBottles(proInfo[0]);
                            sampleBox.setSampleMode(proInfo[1]);
                            sampleBox.setCurrentSampleBottleNo(proInfo[2]);
                            sampleBox.setCycleTime(Integer.parseInt(proInfo[3]));

                            if (!StringUtils.isEmpty(proInfo[4]) && proInfo[4].split("/").length == 2) {
                                int currentStep = Integer.parseInt(proInfo[4].split("/")[0]);
                                String stepDesr = StringUtils.getCYX7Step(currentStep,version);
                                int allStep = Integer.parseInt(proInfo[4].split("/")[1]);
                                if (currentStep == 0) {
                                    sampleBox.setWorkStep(stepDesr);
                                } else if (currentStep > allStep) {
                                    sampleBox.setWorkStep(stepDesr);
                                } else {
                                    sampleBox.setWorkStep(currentStep * 100 / allStep + "%" + stepDesr);
                                }

                            } else {
                                sampleBox.setWorkStep(proInfo[4]);
                            }
                            sampleBox.setRainNum(Integer.parseInt(proInfo[5].trim()));
                            sampleBox.setWaterHeight(Integer.parseInt(proInfo[6].trim()));
                            sampleBox.setJyTimeD(DoubleUtils.round(Double.parseDouble(proInfo[7]), 3));
                            sampleBox.setPyTimeD(DoubleUtils.round(Double.parseDouble(proInfo[8]), 3));
                            sampleBox.setWashTimeD(DoubleUtils.round(Double.parseDouble(proInfo[9]), 3));
                            sampleBox.setCyNum(Integer.parseInt(proInfo[10].trim()));
                            sampleBox.setNextSampleDate(proInfo[11]);
                            sampleBottleList = new ArrayList<>();
                            for (int i = 0; i < 6; i++) {
                                SampleBottle sampleBottle = new SampleBottle();
                                sampleBottle.setBottleNo(i + 1);
                                if (sampleBottle.getBottleNo() > 0 && sampleBottle.getBottleNo() <= 6 && sampleBox != null && sampleBox.getLockBottles() != null && sampleBox.getLockBottles().length() == 6) {
                                    int isLock = Integer.parseInt(String.valueOf(sampleBox.getLockBottles().toCharArray()[sampleBottle.getBottleNo() - 1]));
                                    sampleBottle.setIsLock(isLock);
                                } else {
                                    sampleBottle.setIsLock(0);
                                }
                                sampleBottle.setBlueMac(proInfo[12 + i * 3]);

                                sampleBottle.setSampleID(proInfo[13 + i * 3]);
                                sampleBottle.setDateTime(proInfo[14 + i * 3]);
                                sampleBottleList.add(sampleBottle);

                            }
                            sampleBox.setRequestCycle(StringUtils.isInt(proInfo[30]) ? Integer.parseInt(proInfo[30]) : Constant.DefaultValue);
                            sampleBox.setGnns(StringUtils.isInt(proInfo[31]) ? Integer.parseInt(proInfo[31]) : Constant.DefaultValue);
                            sampleBox.setBatLow(StringUtils.isInt(proInfo[32]) ? Integer.parseInt(proInfo[32]) : Constant.DefaultValue);
                            sampleBox.setBatDL(StringUtils.isInt(proInfo[33]) ? Integer.parseInt(proInfo[33]) : Constant.DefaultValue);
                            sampleBox.setCurrentNum(StringUtils.isInt(proInfo[34]) ? Integer.parseInt(proInfo[34]) : Constant.DefaultValue);
                            sampleBox.setLlj(StringUtils.isInt(proInfo[35]) ? Integer.parseInt(proInfo[35]) : Constant.DefaultValue);


                            if ("08".equals(sampleBox.getSampleMode()) && sampleBox.getCyNum() >= 0) {
                                tvSampleMode.setText("等比例采样" + sampleBox.getCyNum() + "次");
                            } else if ("13".equals(sampleBox.getSampleMode()) && sampleBox.getCyNum() >= 0) {
                                tvSampleMode.setText("疾控模式，上层等比例" + sampleBox.getCyNum() + "次/瓶");
                            } else {
                                tvSampleMode.setText(StringUtils.getSampleMode(sampleBox.getSampleMode()));
                            }

                            llWorkTimes.setVisibility(View.GONE);
                            llWorkPlan.setVisibility(View.GONE);


                            llWorkWaterHeight.setVisibility(View.GONE);
                            llSampleNextTime.setVisibility(View.GONE);
                            llWorkStep.setVisibility(View.GONE);
                            switch (sampleBox.getSampleMode()) {
                                case "00":
                                    llWorkTimes.setVisibility(View.VISIBLE);
                                    llWorkStep.setVisibility(View.VISIBLE);
                                    break;
                                case "01":
                                    llWorkTimes.setVisibility(View.VISIBLE);
                                    if (sampleBox.getDevStatus().equals("01") || sampleBox.getDevStatus().equals("001")) {
                                        llWorkStep.setVisibility(View.VISIBLE);
                                    } else {
                                        tvSampleNextTitle.setText("下次采样时间");
                                        llSampleNextTime.setVisibility(View.VISIBLE);
                                    }
                                    break;
                                case "02":
                                    llWorkTimes.setVisibility(View.VISIBLE);
                                    llWorkStep.setVisibility(View.VISIBLE);
                                    break;
                                case "03":
                                    llWorkTimes.setVisibility(View.VISIBLE);

                                    if (sampleBox.getDevStatus().equals("01") || sampleBox.getDevStatus().equals("001")) {
                                        llWorkStep.setVisibility(View.VISIBLE);
                                    } else {
                                        tvSampleNextTitle.setText("下次判断时间");
                                        llSampleNextTime.setVisibility(View.VISIBLE);
                                    }
                                    break;
                                case "04":
                                    llWorkPlan.setVisibility(View.VISIBLE);
                                    llWorkStep.setVisibility(View.VISIBLE);
                                    break;
                                case "05":
                                    llWorkTimes.setVisibility(View.VISIBLE);
                                    llWorkWaterHeight.setVisibility(View.VISIBLE);
                                    llWorkStep.setVisibility(View.VISIBLE);
                                    break;
                                case "06":
                                    llWorkTimes.setVisibility(View.VISIBLE);
                                    llWorkStep.setVisibility(View.VISIBLE);
                                    break;
                                case "07":
                                    llWorkTimes.setVisibility(View.VISIBLE);
                                    llWorkStep.setVisibility(View.VISIBLE);
                                    break;
                                case "08":
                                    llWorkTimes.setVisibility(View.VISIBLE);
                                    if (sampleBox.getDevStatus().equals("01") || sampleBox.getDevStatus().equals("001")) {
                                        llWorkStep.setVisibility(View.VISIBLE);
                                    } else {
                                        tvSampleNextTitle.setText("下次采样时间");
                                        llSampleNextTime.setVisibility(View.VISIBLE);
                                    }
                                    break;
                                case "13":
                                    llWorkPlan.setVisibility(View.VISIBLE);
                                    llWorkTimes.setVisibility(View.VISIBLE);
                                    if (sampleBox.getDevStatus().equals("01") || sampleBox.getDevStatus().equals("001")) {
                                        llWorkStep.setVisibility(View.VISIBLE);
                                    } else {
                                        tvSampleNextTitle.setText("下次采样时间");
                                        llSampleNextTime.setVisibility(View.VISIBLE);
                                    }
                                    break;
                            }
                            if (!StringUtils.isEmpty(sampleBox.getSysTime()) && sampleBox.getSysTime().startsWith("2")) {

                                tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" + sampleBox.getSysTime(), "yyyyMMddHHmmss")));
                                if (Math.abs(System.currentTimeMillis() - DateUtil.format2miao("20" + sampleBox.getSysTime(), "yyyyMMddHHmmss")) > 1 * 60 * 1000) {
                                    new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherCYX7Activity.this);
                                }
                            } else {
                                new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherCYX7Activity.this);

                            }

                            tvDevVersion.setText(sampleBox.getVersion());
                            tvWorkStep.setText(sampleBox.getWorkStep());
                            if ("08".equals(sampleBox.getSampleMode()) || "13".equals(sampleBox.getSampleMode())) {
                                tvWorkTimes.setText("等比例" + sampleBox.getCycleTime() + "分钟/次");
                            } else {
                                tvWorkTimes.setText(sampleBox.getCycleTime() + "分钟/次");
                            }
                            version = 0;
                            if (StringUtils.isInt(sampleBox.getVersion())){
                                version =Integer.parseInt(sampleBox.getVersion());
                            }
                            String devStatus = StringUtils.getCYXRTUStatus(sampleBox.getDevStatus());
                            tvWorkStatus.setText(devStatus);
                            if (devStatus != null && devStatus.length() > 3) {
                                tvWorkStatus.setTextColor(getResources().getColor(R.color.color_red));
                                playAlarmSoundAndVibrate();
                                showMsg(1, "警告：" + devStatus);
                            } else {
                                tvWorkStatus.setTextColor(getResources().getColor(R.color.color_black_333333));
                            }

                            if (!StringUtils.isEmpty(sampleBox.getNextSampleDate()) && sampleBox.getNextSampleDate().length() == 12 && sampleBox.getNextSampleDate().startsWith("2")) {
                                tvSampleNextTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" + sampleBox.getNextSampleDate(), "yyyyMMddHHmmss")));
                            } else {
                                tvSampleNextTime.setText("");
                            }

                            if ("13".equals(sampleBox.getSampleMode())) {
                                if (sampleBox.getCurrentSampleBottleNo().length() == 2) {
                                    tvSampleBottleNum.setText("上层瓶" + sampleBox.getCurrentSampleBottleNo().substring(0, 1) + "次/下层瓶" + sampleBox.getCurrentSampleBottleNo().substring(1, 2));
                                } else {
                                    tvSampleBottleNum.setText(sampleBox.getCurrentSampleBottleNo() + "瓶");
                                }
                            } else {
                                tvSampleBottleNum.setText(sampleBox.getCurrentSampleBottleNo() + "瓶");
                            }


                            tvWorkRain.setText(DoubleUtils.div(sampleBox.getRainNum(), 10d, 1) + "");
                            if (sampleBox.getRainNum() > 0 && sampleBox.getRainNumTotal() > 0) {
                                tvWorkRainTotal.setText(DoubleUtils.div(sampleBox.getRainNum() * sampleBox.getRainNumTotal(), 10d, 1) + "mm");
                            } else {
                                tvWorkRainTotal.setText("");
                            }
                            tvWorkWaterHeight.setText(DoubleUtils.div(sampleBox.getWaterHeight(), 1000d, 3) + "");
                            if (sampleBox.getJyTimeD() <= 300) {
                                tvJyTime.setText(sampleBox.getJyTimeD() + "秒");
                                double height = DoubleUtils.div(sampleBox.getJyTimeD() - 10.787, 2.9626, 1);
                                if (height > 0) {
                                    tvJYHeight.setText(DoubleUtils.round(height, 1) + "米");
                                }

                                tvJyThreshold.setText("");
                            } else {
                                tvJyTime.setText("");
                                tvJyThreshold.setText(sampleBox.getJyTimeD() + "脉冲");
                            }
                            if (sampleBox.getPyTimeD() > 0) {
                                tvPyTime.setText(sampleBox.getPyTimeD() + "秒");
                            }

                            if (StringUtils.isEmpty(sampleBox.getRiss()) || "0".equals(sampleBox.getRiss())) {
                                tvDevRiss.setText("离线");
                            } else {
                                tvDevRiss.setText(StringUtils.isEmpty(sampleBox.getRiss()) ? "\t" : "" + sampleBox.getRiss());
                            }
                            tvUpdateMsgCycle.setText(sampleBox.getUpdateCycle() <= 0 ? "不上传" : (sampleBox.getUpdateCycle() + "分钟"));
                            // tvWashTime.setText(sampleBox.getWashTime() > 0 ? (sampleBox.getWashTime() + "秒") : "");
                            tvWashTime.setText(sampleBox.getWashTimeD() > 0 ? "已启动" : "待启动");
                            tvDevEV.setText(sampleBox.getDevEV() + "V");
                            String icc = sampleBox.getIccID();
                            if (StringUtils.isEmpty(icc) || "00000000000000000000".equals(icc.trim())) {
                                sampleBox.setIccID("");
                                tvInternetCID.setText("物联网卡号ICCID值为空");
                            } else {
                                tvInternetCID.setText(icc.trim());
                            }

                            mAdapter.setDatas(sampleBottleList);
                            if (!StringUtils.isEmpty(sampleBox.getNet()))
                                tvDevIP.setText(sampleBox.getNet().trim());
                            if (!StringUtils.isEmpty(sampleBox.getNet()))
                                tvNetPort.setText(sampleBox.getPort().trim() + "");

                            tvRequestMsgCycle.setText(sampleBox.getRequestCycle() > 0 ? (sampleBox.getRequestCycle() + "分钟") : "不同步");
                            gpsSearch = sampleBox.getGnns() == 1;
                            swGPSSearch.setChecked(gpsSearch);
                            tvDatLow.setText(sampleBox.getBatLow() > 0 ? (DoubleUtils.div(sampleBox.getBatLow(), 100d, 2) + "") : "");
                            tvDevEV.setText((sampleBox.getBatDL() + "%\t") + sampleBox.getDevEV() + "V");
                            if ("13".equals(sampleBox.getSampleMode())) {
                                if (sampleBox.getCurrentSampleBottleNo().length() == 2) {
                                    tvSampleBottleNum.setText("上层瓶" + sampleBox.getCurrentSampleBottleNo().substring(0, 1) + "已采" + sampleBox.getCurrentNum() + "次/下层瓶" + sampleBox.getCurrentSampleBottleNo().substring(1, 2));
                                } else {
                                    tvSampleBottleNum.setText(sampleBox.getCurrentSampleBottleNo() + "瓶");
                                }
                            } else {
                                tvSampleBottleNum.setText(sampleBox.getCurrentSampleBottleNo() + "瓶" + (("08".equals(sampleBox.getSampleMode())) ? ("已采" + sampleBox.getCurrentNum() + "次") : ""));
                            }
                            if (sampleBox.getLlj() == 1) {
                                tvSampleLLJ.setText("无水");
                                tvSampleLLJ.setTextColor(getResources().getColor(R.color.color_black_999999));
                            } else if (sampleBox.getLlj() == 0) {
                                tvSampleLLJ.setText("有水");
                                tvSampleLLJ.setTextColor(getResources().getColor(R.color.color_app_normal));
                            } else if (sampleBox.getLlj() == Constant.DefaultValue) {
                                tvSampleLLJ.setText("暂无数据");
                                tvSampleLLJ.setTextColor(getResources().getColor(R.color.color_black_999999));
                            } else {
                                tvSampleLLJ.setText("未知" + sampleBox.getLlj());
                                tvSampleLLJ.setTextColor(getResources().getColor(R.color.color_black_999999));
                            }
                        }

                        if (proInfo.length >= 46) {
                            sampleBox.setCypErr(proInfo[36]);
                            sampleBox.setCypIpPort(proInfo[37]);
                            sampleBox.setTrigger(StringUtils.isInt(proInfo[38]) ? Integer.parseInt(proInfo[38]) : Constant.DefaultBigValue);
                            sampleBox.setPhThMin(StringUtils.isInt(proInfo[39]) ? Integer.parseInt(proInfo[39]) : Constant.DefaultBigValue);
                            sampleBox.setPhThMax(StringUtils.isInt(proInfo[40]) ? Integer.parseInt(proInfo[40]) : Constant.DefaultBigValue);
                            sampleBox.setCondTh(StringUtils.isInt(proInfo[41]) ? Integer.parseInt(proInfo[41]) : Constant.DefaultBigValue);
                            sampleBox.setTurbTh(StringUtils.isInt(proInfo[42]) ? Integer.parseInt(proInfo[42]) : Constant.DefaultBigValue);
                            sampleBox.setOxygenTh(StringUtils.isInt(proInfo[43]) ? Integer.parseInt(proInfo[43]) : Constant.DefaultBigValue);
                            sampleBox.setWaterTempTh(StringUtils.isInt(proInfo[44]) ? Integer.parseInt(proInfo[44]) : Constant.DefaultBigValue);
                            sampleBox.setFlowTh(StringUtils.isInt(proInfo[45]) ? Integer.parseInt(proInfo[45]) : Constant.DefaultBigValue);
                            if (sampleBox.getTrigger() > 0) {
                                byte[] triggers = StringUtils.getDateBit16(sampleBox.getTrigger());
                                StringBuffer sbTrigger = new StringBuffer();
                                llWorkWaterHeight.setVisibility(triggers[0] == 1 ? View.VISIBLE : View.GONE);
                                if (triggers[0] == 1) {
                                    sbTrigger.append("水位、");
                                }
                                llWorkRain.setVisibility(triggers[1] == 1 ? View.VISIBLE : View.GONE);
                                if (triggers[1] == 1) {
                                    sbTrigger.append("雨量、");
                                }
                                llWorkPH.setVisibility(triggers[2] == 1 ? View.VISIBLE : View.GONE);
                                if (triggers[2] == 1) {
                                    sbTrigger.append("pH、");
                                    tvWorkPH.setText((sampleBox.getPhThMin() > 0 ? "下限" + DoubleUtils.div(sampleBox.getPhThMin(), 100d, 2) : "")
                                            + (sampleBox.getPhThMax() > 0 ? "上限" + DoubleUtils.div(sampleBox.getPhThMax(), 100d, 2) : ""));
                                }
                                llWorkCond.setVisibility(triggers[3] == 1 ? View.VISIBLE : View.GONE);
                                if (triggers[3] == 1) {
                                    sbTrigger.append("电导率、");
                                    tvWorkCond.setText(sampleBox.getCondTh() > Constant.DefaultBigValue ? DoubleUtils.div(sampleBox.getCondTh(), 100d, 2) + "" : "");
                                }
                                llWorkTurb.setVisibility(triggers[4] == 1 ? View.VISIBLE : View.GONE);
                                if (triggers[4] == 1) {
                                    sbTrigger.append("浊度、");
                                    tvWorkTurb.setText(sampleBox.getTurbTh() > Constant.DefaultBigValue ? DoubleUtils.div(sampleBox.getTurbTh(), 100d, 2) + "" : "");
                                }
                                llWorkOxygen.setVisibility(triggers[5] == 1 ? View.VISIBLE : View.GONE);
                                if (triggers[5] == 1) {
                                    sbTrigger.append("溶解氧、");
                                    tvWorkOxygen.setText(sampleBox.getOxygenTh() > Constant.DefaultBigValue ? DoubleUtils.div(sampleBox.getOxygenTh(), 100d, 2) + "" : "");
                                }
                                llWorkTemp.setVisibility(triggers[6] == 1 ? View.VISIBLE : View.GONE);
                                if (triggers[6] == 1) {
                                    sbTrigger.append("水温、");
                                    tvWorkTemp.setText(sampleBox.getWaterTempTh() > Constant.DefaultBigValue ? DoubleUtils.div(sampleBox.getWaterTempTh(), 100d, 2) + "" : "");
                                }
                                llWorkFlow.setVisibility(triggers[7] == 1 ? View.VISIBLE : View.GONE);
                                if (triggers[7] == 1) {
                                    sbTrigger.append("瞬时流量、");
                                    tvWorkFlow.setText(sampleBox.getFlowTh() > Constant.DefaultBigValue ? DoubleUtils.div(sampleBox.getFlowTh(), 100d, 2) + "" : "");
                                }
                                if (triggers[8] == 1) {
                                    sbTrigger.append("液位、");
                                }
                                tvTriggerMode.setText(sbTrigger.length() > 0 ? (sbTrigger.substring(0, sbTrigger.length() - 1) + "触发") : "");
                                llWorkTrigger.setVisibility(View.VISIBLE);
                            } else {
                                tvTriggerMode.setText("");
                                llWorkTrigger.setVisibility(View.GONE);
                            }
                            if (!StringUtils.isEmpty(sampleBox.getCypIpPort()) && sampleBox.getCypIpPort().length() == 17) {
                                tvCypIP.setText(StringUtils.getIPAddress(sampleBox.getCypIpPort().substring(0, 12)));
                                tvCypNetPort.setText(StringUtils.isInt(sampleBox.getCypIpPort().substring(12, 17)) ? Integer.parseInt(sampleBox.getCypIpPort().substring(12, 17)) + "" : "");
                            } else {
                                tvCypIP.setText("");
                                tvCypNetPort.setText("");
                            }
                        }

                        if (proInfo.length >= 62) {
                            sampleBox.setPipelen(StringUtils.isInt(proInfo[54])?Integer.parseInt(proInfo[54]):Constant.DefaultValue);
                            sampleBox.setCapacity(StringUtils.isInt(proInfo[55])?Integer.parseInt(proInfo[55]):Constant.DefaultValue);
                            sampleBox.setFzkgState(StringUtils.isInt(proInfo[57])?Integer.parseInt(proInfo[57]):Constant.DefaultValue);
                            String[] rtuVolume= proInfo[58].split("-");
                            if (rtuVolume!=null&&sampleBottleList!=null&&rtuVolume.length==sampleBottleList.size()){
                                for (int i = 0; i < rtuVolume.length; i++){
                                    if (StringUtils.isInt(rtuVolume[i])){
                                        sampleBottleList.get(i).setVolume(Integer.parseInt(rtuVolume[i]));
                                    }
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                            sampleBox.setTopCoefficient100(StringUtils.isInt(proInfo[60])?Integer.parseInt(proInfo[60]):Constant.DefaultValue);



                            sampleBox.setBottomCoefficient100(StringUtils.isInt(proInfo[61])?Integer.parseInt(proInfo[61]):Constant.DefaultValue);

                            llSampleSetOld.setVisibility(View.GONE);
                            llSampleSetNew.setVisibility(View.VISIBLE);
                            tvJyPipeLen.setText(DoubleUtils.div(sampleBox.getPipelen(),1000d,3)+"");
                           // tvJyCapacity.setText(sampleBox.getCapacity()+"");//20260526 陈备提出放弃显示管容量，改成显示进样时长
                            tvJyCapacity.setText(sampleBox.getJyTimeD() + "秒");
                            tvTopCoefficient.setText(sampleBox.getTopCoefficient100()>0?(DoubleUtils.div(sampleBox.getTopCoefficient100(),100,2)+""):"");
                            tvBottomCoefficient.setText(sampleBox.getBottomCoefficient100()>0?(DoubleUtils.div(sampleBox.getBottomCoefficient100(),100,2)+""):"");

                        }
                    } catch (Exception e) {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        showMsg(1, "ProInfo解析失败，\n" + msgContext);
                        e.printStackTrace();
                    }

                    break;
                case Constant.Return_Read_Detail://返回蓝牙RTU基本信息和
                    try {
                        String[] rtuInfos = msgContext.split("#", -1);
                        if (sampleBox == null) sampleBox = new SampleBox();
                        if (rtuInfos.length >= 8) {
                            sampleBox.setVersion(rtuInfos[0]);
                            if (rtuInfos[1] != null && rtuInfos[1].length() == 12 && rtuInfos[1].startsWith("2")) {
                                sampleBox.setSysTime(rtuInfos[1]);
                            }
                            sampleBox.setDevName(rtuInfos[2].trim());
                            sampleBox.setBlueMac(StringUtils.getNewBlueToothMacAddress(rtuInfos[3].trim()));
                            sampleBox.setMN(rtuInfos[4].trim());
                            sampleBox.setDevEV(rtuInfos[5]);
                            sampleBox.setDevNum(rtuInfos[6]);
                            sampleBox.setSamDevType(StringUtils.isInt(rtuInfos[8]) ? Integer.parseInt(rtuInfos[8]) : Constant.DefaultValue);
                        }
                        if (rtuInfos.length >= 40) {
                            sampleBox.setIccID(rtuInfos[11]);
                            sampleBox.setRiss(rtuInfos[13]);
                            sampleBox.setUpdateCycle(StringUtils.isInt(rtuInfos[20]) ? Integer.parseInt(rtuInfos[20]) : Constant.DefaultValue);
                            sampleBox.setNet((!StringUtils.isEmpty(rtuInfos[21]) && rtuInfos[21].length() == 12) ? StringUtils.getIPAddress(rtuInfos[21]) : "");
                            sampleBox.setPort(StringUtils.isInt(rtuInfos[22]) ? Integer.parseInt(rtuInfos[22]) + "" : "");
                            sampleBox.setPwrEn(StringUtils.isInt(rtuInfos[38]) ? Integer.parseInt(rtuInfos[38]) : Constant.DefaultValue);
                            sampleBox.setDevStatus(rtuInfos[39]);
                        }


                    } catch (Exception e) {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        showMsg(1, "Read All解析失败，\n" + msgContext);
                        e.printStackTrace();
                    }
                    sendLHBleData("Read ProInfo", Constant.Param_Read_ProInfo);
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
                            boxSamplePlanDailog = new BoxSamplePlanDailog(LiherCYX7Activity.this, "定时采样计划", samplePlanList, sampleBox, Constant.Dailog_Box_SamplePlan, LiherCYX7Activity.this);

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
                case Constant.Return_Write_BatLow:
                    if (msgContext != null && msgContext.contains("Write batLow")) {
                        showMsg(0, "设置低压阈值成功");
                        refreshDetails();
                    } else {
                        showMsg(1, "设置低压阈值失败,\n" + msgContext);
                    }
                    break;

                case Constant.Return_Wirte_Mode:
                    if (msgContext != null && msgContext.contains("Write WorkMode=")) {
                        if ("Write WorkMode=1".equals(msgContext)) {
                            DlgTime.showYMDDialog(LiherCYX7Activity.this, Calendar.getInstance(), Calendar.getInstance(), null,
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
                        }
                        if ("Write WorkMode=3".equals(msgContext)) {


                            Calendar defaultCalendar = Calendar.getInstance();

                            DlgTime.showYMDDialog(LiherCYX7Activity.this, defaultCalendar, defaultCalendar, null,
                                    true, true, true, true, true, true,
                                    new DlgTime.OnDateSelectedFinishCallback() {
                                        @Override
                                        public void onDateSelected(Date date) {
                                            showMsg(2, "设置单触发模式采样生效时间" + DateUtil.long2time10(date.getTime()).substring(2));
                                            sendLHBleData("Write Effective=" + DateUtil.long2time10(date.getTime()).substring(2), Constant.Param_Write_TimeMode_Delay);

                                        }
                                    });

                            //   new TimeModeDelayDailog(LiherCYX7Activity.this, "设置单触发模式生效时刻", Constant.Dailog_Box_TimeMode_Delay, LiherCYX7Activity.this);
                            sampleBox.setSampleMode("03");
                            tvSampleMode.setText("单触发模式");
                        } else if ("Write WorkMode=8".equals(msgContext)) {
                            showMsg(2, "设置等比例采样次数,1~24次");
                            new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Input_CyNum, "设置等比例采样", "等比例采样次数", "", LiherCYX7Activity.this);
                            sampleBox.setSampleMode("08");
                            tvSampleMode.setText("等比例采样");
                        } else if ("Write WorkMode=13".equals(msgContext)) {
                            showMsg(2, "设置上层瓶的等比例采样次数,1~24次");
                            new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Input_CyNum, "设置上层瓶等比例采样", "上层瓶等比例采样次数", "", LiherCYX7Activity.this);
                            sampleBox.setSampleMode("13");
                            tvSampleMode.setText("疾控模式");
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
                    if (msgContext != null && msgContext.startsWith("Write cycleTime=")) {
                        int cycTime = Integer.parseInt(msgContext.substring(16).trim());
                        if (sampleBox != null) {
                            sampleBox.setCycleTime(cycTime);
                            if ("08".equals(sampleBox.getSampleMode()) || "13".equals(sampleBox.getSampleMode())) {
                                tvWorkTimes.setText("等比例" + cycTime + "分钟/次");
                            }
                        } else {
                            tvWorkTimes.setText(cycTime + "分钟");
                        }
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
                case Constant.Return_OPEN_LOCK:
                    if (msgContext != null && msgContext.equals("ll"))
                        showMsg(0, "开锁命令下发成功~");

                    break;
                case Constant.RETURN_Write_Box_Sample_Plan:
                    if (msgContext != null && msgContext.contains("Write SamPoint=")) {
                        sendLHBleData("Read SamPoint", Constant.PARAM_Work_Plan);
                    } else {
                        showMsg(1, "定时采样计划提交失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_Rainth:
                    if (msgContext != null && msgContext.contains("Write rainNum=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int rainNum = Integer.parseInt(msgContext.substring(14).trim());
                        if (sampleBox != null) sampleBox.setRainNum(rainNum);
                        tvWorkRain.setText(DoubleUtils.div(rainNum, 10d, 1) + "");
                    } else {
                        showMsg(1, "设置采样雨量触发值失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_JyTIme:
                    if (msgContext != null && msgContext.contains("Write jyTime=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int jyTime = Integer.parseInt(msgContext.substring(13).trim());
                        if (sampleBox != null) sampleBox.setJyTimeD(jyTime);
                        tvJyTime.setText(jyTime + "秒");
                        double height = DoubleUtils.div(jyTime - 10.787, 2.9626, 1);
                        if (height > 0) {
                            tvJYHeight.setText(DoubleUtils.round(height, 1) + "米");
                        }
                        tvJyThreshold.setText("");
                    } else {
                        showMsg(1, "设置进样时长失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_JyThreshold:
                    if (msgContext != null && msgContext.contains("Write jyTime=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int jyThreshold = Integer.parseInt(msgContext.substring(13).trim());
                        if (sampleBox != null) sampleBox.setJyTimeD(jyThreshold);
                        tvJyThreshold.setText(jyThreshold + "脉冲");
                        tvJyTime.setText("");
                    } else {
                        showMsg(1, "设置进样阀值失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_PyTIme:
                    if (msgContext != null && msgContext.contains("Write pyTime=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int pyTime = Integer.parseInt(msgContext.substring(13).trim());
                        if (sampleBox != null) sampleBox.setPyTimeD(pyTime);
                        tvPyTime.setText(pyTime + "秒");
                    } else {
                        showMsg(1, "设置排样时长失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_CyNum:
                    if (msgContext != null && msgContext.contains("Write cyNum=")) {
                        Calendar defaultCalendar = Calendar.getInstance();

                        DlgTime.showYMDDialog(LiherCYX7Activity.this, defaultCalendar, defaultCalendar, null,
                                true, true, true, true, true, true,
                                new DlgTime.OnDateSelectedFinishCallback() {
                                    @Override
                                    public void onDateSelected(Date date) {
                                        showMsg(2, "设置等比例采样生效时间" + DateUtil.long2time10(date.getTime()).substring(2));
                                        sendLHBleData("Write Effective=" + DateUtil.long2time10(date.getTime()).substring(2), Constant.Param_Write_TimeMode_Delay);

                                    }
                                });
                        //   new TimeModeDelayDailog(LiherCYX7Activity.this, "设置等比例采样生效时刻", Constant.Dailog_Box_TimeMode_Delay, LiherCYX7Activity.this);

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
                        new HitiStringDailog(LiherCYX7Activity.this, Constant.HitiStrTargetDialog_Bottle_CloseCap, "", "关闭瓶阀", "样瓶阀门未关，请确认关闭瓶阀后才可装瓶", LiherCYX7Activity.this);

                    } else {
                        showMsg(1, msgContext + ",样瓶阀门状态异常无法装瓶！");

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
                    if (msgContext != null && msgContext.startsWith("Write Effective=")) {
                        showMsg(0, "生效时间设置成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "设置生效时间失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_Water_Height:
                    if (msgContext != null && msgContext.contains("Write waterHeight=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int waterValue = Integer.parseInt(msgContext.substring(18).trim());
                        if (sampleBox != null) sampleBox.setWaterHeight(waterValue);
                        tvWorkWaterHeight.setText(DoubleUtils.div(waterValue, 1000d, 3) + "");
                    } else {
                        showMsg(1, "设置采样液位阀值失败,\n" + msgContext);
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

                        tvUpdateMsgCycle.setVisibility(updateType == 0 ? View.GONE : View.VISIBLE);
                    } else {
                        showMsg(1, "设置上传方式失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_Update_Cycle:
                    if (msgContext != null && msgContext.contains("Write reppd=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int updateValue = Integer.parseInt(msgContext.substring(12).trim());
                        if (sampleBox != null) sampleBox.setUpdateCycle(updateValue);
                        if (updateValue == 0) {
                            tvUpdateMsgCycle.setText("上传关闭");
                        } else {
                            tvUpdateMsgCycle.setText(updateValue + "分钟");
                        }

                    } else {
                        showMsg(1, "设置上传周期失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_SamplingBottle:
                    if (msgContext != null && msgContext.contains("Write samdev=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        if (sampleBox != null)
                            sampleBox.setCurrentSampleBottleNo(msgContext.substring(13).trim());
                        tvSampleBottleNum.setText(sampleBox.getCurrentSampleBottleNo() + "瓶");
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
                    if (msgContext != null && msgContext.contains("Write BleAddr=")) {
                        showMsg(0, "设置成功,正在重启...");
                    } else {
                        showMsg(1, "设置蓝牙地址失败,\n" + msgContext);

                    }
                    break;
                case Constant.Return_Write_SWJ_DEVIP:
                    if (msgContext != null && msgContext.contains("Write net=")) {
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_ip_address_success));
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail) + ",\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_SWJ_NetPort:
                    if (msgContext != null && msgContext.contains("Write port="))
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_net_port_success));
                    RxTimerUtil.cancel();
                    RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                        @Override
                        public void doNext(long number) {
                            refreshDetails();
                        }
                    });
                    break;
                case Constant.Return_CYX_ICCID:
                    if ("00000000000000000000".equals(msgContext)) {
                        tvInternetCID.setText("物联网卡号ICCID值为空");
                    } else if (msgContext != null && msgContext.length() == 20) {
                        tvInternetCID.setText(msgContext);
                    }
                    showMsg(0, "读取物联网卡号成功~");

                    break;
                case Constant.Return_DX3_Bump_Stop:
                    if (msgContext != null && msgContext.equals("Write bump=0")) {
                        showMsg(0, "水泵停转指令下发成功");
                    } else {
                        showMsg(1, "水泵停转指令下发失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_DX3_Bump1:
                    if (msgContext != null && msgContext.equals("Write bump=1")) {
                        showMsg(0, "水泵正转指令下发成功");

                    } else {
                        showMsg(1, "水泵正转指令下发失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Dev_Restart:
                    if (msgContext!=null&&msgContext.equals("Set SysReset")){
                        showMsg(0, "设备重启指令下发成功");
                    }else {
                        showMsg(1, "设备重启失败,\n" + msgContext);
                    }

                    break;
                case Constant.Return_DX3_Bump2:
                    if (msgContext != null && msgContext.equals("Write bump=2")) {
                        showMsg(0, "水泵反转指令下发成功");
                    } else {
                        showMsg(1, "水泵反转指令下发失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_BatEV:
                    if (msgContext != null && msgContext.contains("Write vin=")) {
                        showMsg(0, "校正电压值设置成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "校正电压值设置失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_WashTime:
                    if (msgContext != null && msgContext.contains("Write RxTime=")) {
                        showMsg(0, "润洗时长设置成功");
                        refreshDetails();
                    } else {
                        showMsg(1, "润洗时长设置失败,\n" + msgContext);
                    }
                    break;
                case Constant.RETURN_Clean_Box_Sample_Plan:
                    if (msgContext != null && msgContext.equals("Write SamPoint=000000#000000#000000#000000#000000#000000#")) {
                        sendLHBleData("Write WorkMode=13", Constant.Param_Wirte_Mode);
                    } else {
                        showMsg(1, "设置疾控模式前清空定时采样计划失败,\n" + msgContext);
                    }

                    break;


                case Constant.Return_DX3_SWJ_Data:
                    String[] swjData = msgContext.split("#", -1);

                    DX3SWJInfo swjInfo = new DX3SWJInfo();
                    if (swjData.length >= 14) {
                        swjInfo.status = StringUtils.isLong(swjData[0]) ? Long.parseLong(swjData[0].trim()) : Constant.DefaultValue;
                        swjInfo.depth = StringUtils.isDouble(swjData[1]) ? DoubleUtils.div(Double.parseDouble(swjData[1].trim()), 1000d) : Constant.DefaultValue;
                        swjInfo.surfaceV = StringUtils.isDouble(swjData[2]) ? Double.parseDouble(swjData[2].trim()) : Constant.DefaultValue;
                        swjInfo.sectionV = StringUtils.isDouble(swjData[3]) ? Double.parseDouble(swjData[3].trim()) : Constant.DefaultValue;
                        swjInfo.rateFlow = StringUtils.isDouble(swjData[4]) ? Double.parseDouble(swjData[4].trim()) : Constant.DefaultValue;
                        swjInfo.area = StringUtils.isDouble(swjData[5]) ? Double.parseDouble(swjData[5].trim()) : Constant.DefaultValue;
                        swjInfo.cumulativeFlow = StringUtils.isDouble(swjData[6]) ? Double.parseDouble(swjData[6].trim()) : Constant.DefaultValue;
                        swjInfo.cumuFlowNum = StringUtils.isInt(swjData[7]) ? Integer.parseInt(swjData[7].trim()) : Constant.DefaultValue;
                        swjInfo.scumuFlow = StringUtils.isDouble(swjData[8]) ? Double.parseDouble(swjData[8].trim()) : Constant.DefaultValue;
                        swjInfo.scumuFlowNum = StringUtils.isInt(swjData[9]) ? Integer.parseInt(swjData[9].trim()) : Constant.DefaultValue;
                        swjInfo.ncumuFlow = StringUtils.isDouble(swjData[10]) ? Double.parseDouble(swjData[10].trim()) : Constant.DefaultValue;
                        swjInfo.ncumuFlowNum = StringUtils.isInt(swjData[11]) ? Integer.parseInt(swjData[11].trim()) : Constant.DefaultValue;
                        swjInfo.waterLev = StringUtils.isDouble(swjData[12]) ? DoubleUtils.div(Double.parseDouble(swjData[12].trim()), 1000d) : Constant.DefaultValue;
                        swjInfo.intervalFlow = StringUtils.isDouble(swjData[13]) ? Double.parseDouble(swjData[13].trim()) : Constant.DefaultValue;
                    }
                    if (swjInfo.depth <= 0 && swjInfo.surfaceV <= 0 && swjInfo.rateFlow <= 0 && swjInfo.cumulativeFlow <= 0 && swjInfo.waterLev <= 0) {
                        new HitiStringDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_SWJ, null, "水文数据读取", "未读取到正常水文数据，请确认已正确接入水文设备？\n" + msgContext, LiherCYX7Activity.this);
                    } else {
                        new CYX2SWJDataDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_SWJ, "水文数据信息", swjInfo, LiherCYX7Activity.this);
                    }

                    break;
                case Constant.Return_DXCYX3_WCS:
                    String[] wcsData = msgContext.split("#", -1);
                    for (int i = 0; i < wcsData.length; i++) {
                        wcsData[i] = StringUtils.getLastField(wcsData[i], "=");
                    }
                    DX3WcsInfo wcsInfo = new DX3WcsInfo();
                    if (wcsData.length >= 13) {
                        wcsInfo.status = StringUtils.isLong(wcsData[0]) ? Long.parseLong(wcsData[0].trim()) : Constant.DefaultValue;
                        wcsInfo.tem = StringUtils.isDouble(wcsData[1]) ? Double.parseDouble(wcsData[1].trim()) : Constant.DefaultValue;
                        wcsInfo.dp = StringUtils.isDouble(wcsData[2]) ? Double.parseDouble(wcsData[2].trim()) : Constant.DefaultValue;
                        wcsInfo.pH = StringUtils.isDouble(wcsData[3]) ? Double.parseDouble(wcsData[3].trim()) : Constant.DefaultValue;
                        wcsInfo.Do = StringUtils.isDouble(wcsData[4]) ? Double.parseDouble(wcsData[4].trim()) : Constant.DefaultValue;
                        wcsInfo.EC = StringUtils.isDouble(wcsData[5]) ? Double.parseDouble(wcsData[5].trim()) : Constant.DefaultValue;
                        wcsInfo.Tb = StringUtils.isDouble(wcsData[6]) ? Double.parseDouble(wcsData[6].trim()) : Constant.DefaultValue;
                        wcsInfo.Oil = StringUtils.isDouble(wcsData[7]) ? Double.parseDouble(wcsData[7].trim()) : Constant.DefaultValue;
                        wcsInfo.NH = StringUtils.isDouble(wcsData[8]) ? Double.parseDouble(wcsData[8].trim()) : Constant.DefaultValue;
                        wcsInfo.COD = StringUtils.isDouble(wcsData[9]) ? Double.parseDouble(wcsData[9].trim()) : Constant.DefaultValue;
                        wcsInfo.ORP = StringUtils.isDouble(wcsData[10]) ? Double.parseDouble(wcsData[10].trim()) : Constant.DefaultValue;
                        wcsInfo.YLS = StringUtils.isDouble(wcsData[11]) ? Double.parseDouble(wcsData[11].trim()) : Constant.DefaultValue;
                        wcsInfo.LLZ = StringUtils.isDouble(wcsData[12]) ? Double.parseDouble(wcsData[12].trim()) : Constant.DefaultValue;
                    }
                    if (wcsInfo.Do <= 0 && wcsInfo.pH <= 0 && wcsInfo.ORP == 0 && wcsInfo.Tb <= 0 && wcsInfo.COD <= 0 && wcsInfo.LLZ <= 0) {
                        new HitiStringDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Wcs, null, "参数数据读取", "未读取到正常参数数据，请确认已正确接入五参数设备？\n" + msgContext, LiherCYX7Activity.this);
                    } else {
                        new CYX2WCSDataDailog(LiherCYX7Activity.this, Constant.Dialog_CYX_Wcs, "参数数据信息", wcsInfo, LiherCYX7Activity.this);
                    }

                    break;
                case Constant.Return_Write_PutDown_Bottle:
                    if (msgContext != null && msgContext.startsWith("Write Subaddr=")) {
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    }
                    break;
                case Constant.Return_Read_LLJInfo:
                    String[] lljInfos = msgContext.split("#", -1);
                    RtuLljInfo rtuLljInfo = new RtuLljInfo();
                    if (lljInfos.length >= 28) {
                        rtuLljInfo.samWait = StringUtils.isInt(lljInfos[0]) ? Integer.parseInt(lljInfos[0].trim()) : (lljInfos[0].endsWith("N") ? Integer.parseInt(lljInfos[0].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.waterLev = StringUtils.isDouble(lljInfos[1]) ? Double.parseDouble(lljInfos[1].trim()) : (lljInfos[1].endsWith("N") ? Double.parseDouble(lljInfos[1].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.depth = StringUtils.isDouble(lljInfos[2]) ? Double.parseDouble(lljInfos[2].trim()) : (lljInfos[2].endsWith("N") ? Double.parseDouble(lljInfos[2].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.height = StringUtils.isDouble(lljInfos[3]) ? Double.parseDouble(lljInfos[3].trim()) : (lljInfos[3].endsWith("N") ? Double.parseDouble(lljInfos[3].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.direction = StringUtils.isInt(lljInfos[4]) ? Integer.parseInt(lljInfos[4].trim()) : (lljInfos[4].endsWith("N") ? Integer.parseInt(lljInfos[4].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.surfaceV = StringUtils.isDouble(lljInfos[5]) ? Double.parseDouble(lljInfos[5].trim()) : (lljInfos[5].endsWith("N") ? Double.parseDouble(lljInfos[5].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.sectionV = StringUtils.isDouble(lljInfos[6]) ? Double.parseDouble(lljInfos[6].trim()) : (lljInfos[6].endsWith("N") ? Double.parseDouble(lljInfos[6].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.rateFlow = StringUtils.isDouble(lljInfos[7]) ? Double.parseDouble(lljInfos[7].trim()) : (lljInfos[7].endsWith("N") ? Double.parseDouble(lljInfos[7].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.intervalFlow = StringUtils.isDouble(lljInfos[8]) ? Double.parseDouble(lljInfos[8].trim()) : (lljInfos[8].endsWith("N") ? Double.parseDouble(lljInfos[8].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.cumulativeFlow = StringUtils.isDouble(lljInfos[9]) ? Double.parseDouble(lljInfos[9].trim()) : (lljInfos[9].endsWith("N") ? Double.parseDouble(lljInfos[9].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.cumuFlowNum = StringUtils.isInt(lljInfos[10]) ? Integer.parseInt(lljInfos[10].trim()) : (lljInfos[10].endsWith("N") ? Integer.parseInt(lljInfos[10].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.scumuFlow = StringUtils.isDouble(lljInfos[11]) ? Double.parseDouble(lljInfos[11].trim()) : (lljInfos[11].endsWith("N") ? Double.parseDouble(lljInfos[11].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.scumuFlowNum = StringUtils.isInt(lljInfos[12]) ? Integer.parseInt(lljInfos[12].trim()) : (lljInfos[12].endsWith("N") ? Integer.parseInt(lljInfos[12].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.ncumuFlow = StringUtils.isDouble(lljInfos[13]) ? Double.parseDouble(lljInfos[13].trim()) : (lljInfos[13].endsWith("N") ? Double.parseDouble(lljInfos[13].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.ncumuFlowNum = StringUtils.isInt(lljInfos[14]) ? Integer.parseInt(lljInfos[14].trim()) : (lljInfos[14].endsWith("N") ? Integer.parseInt(lljInfos[14].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.abscumuFlow = StringUtils.isDouble(lljInfos[15]) ? Double.parseDouble(lljInfos[15].trim()) : (lljInfos[15].endsWith("N") ? Double.parseDouble(lljInfos[15].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.abscumuFlowNum = StringUtils.isInt(lljInfos[16]) ? Integer.parseInt(lljInfos[16].trim()) : (lljInfos[16].endsWith("N") ? Integer.parseInt(lljInfos[16].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.pitch = StringUtils.isDouble(lljInfos[17]) ? Double.parseDouble(lljInfos[17].trim()) : (lljInfos[17].endsWith("N") ? Double.parseDouble(lljInfos[17].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.roll = StringUtils.isDouble(lljInfos[18]) ? Double.parseDouble(lljInfos[18].trim()) : (lljInfos[18].endsWith("N") ? Double.parseDouble(lljInfos[18].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.batV = StringUtils.isDouble(lljInfos[19]) ? Double.parseDouble(lljInfos[19].trim()) : (lljInfos[19].endsWith("N") ? Double.parseDouble(lljInfos[19].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.thickness = StringUtils.isDouble(lljInfos[20]) ? Double.parseDouble(lljInfos[20].trim()) : (lljInfos[20].endsWith("N") ? Double.parseDouble(lljInfos[20].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.area = StringUtils.isDouble(lljInfos[21]) ? Double.parseDouble(lljInfos[21].trim()) : (lljInfos[21].endsWith("N") ? Double.parseDouble(lljInfos[21].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.ph = StringUtils.isDouble(lljInfos[22]) ? Double.parseDouble(lljInfos[22].trim()) : (lljInfos[22].endsWith("N") ? Double.parseDouble(lljInfos[22].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.oxygen = StringUtils.isDouble(lljInfos[23]) ? Double.parseDouble(lljInfos[23].trim()) : (lljInfos[23].endsWith("N") ? Double.parseDouble(lljInfos[23].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.conductivity = StringUtils.isDouble(lljInfos[24]) ? Double.parseDouble(lljInfos[24].trim()) : (lljInfos[24].endsWith("N") ? Double.parseDouble(lljInfos[24].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.turbidity = StringUtils.isDouble(lljInfos[25]) ? Double.parseDouble(lljInfos[25].trim()) : (lljInfos[25].endsWith("N") ? Double.parseDouble(lljInfos[25].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.orp = StringUtils.isDouble(lljInfos[26]) ? Double.parseDouble(lljInfos[26].trim()) : (lljInfos[26].endsWith("N") ? Double.parseDouble(lljInfos[26].replaceAll(".$", "")) : Constant.DefaultBigValue);
                        rtuLljInfo.rain = StringUtils.isDouble(lljInfos[27]) ? Double.parseDouble(lljInfos[27].trim()) : (lljInfos[27].endsWith("N") ? Double.parseDouble(lljInfos[27].replaceAll(".$", "")) : Constant.DefaultBigValue);
                    }
                    new RTU2SamInfDailog(LiherCYX7Activity.this, Constant.HitiStrTargetDialog_RTU_SamInf, "参数数据",sampleBox, rtuLljInfo, LiherCYX7Activity.this);
                    break;
                case Constant.Return_Write_Trigger_Mode:
                    if (msgContext != null && msgContext.startsWith("Write Trigger=")) {
                        showMsg(0, "配置成功，请确认条件阈值是否录入");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "配置失败，" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_PHTH:
                    if (msgContext != null && msgContext.startsWith("Write PHTh=")) {
                        showMsg(0, "阈值录入成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "录入失败，" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_CondTH:
                    if (msgContext != null && msgContext.startsWith("Write CondTh=")) {
                        showMsg(0, "阈值录入成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "录入失败，" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_TurbTH:
                    if (msgContext != null && msgContext.startsWith("Write TurbTh=")) {
                        showMsg(0, "阈值录入成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "录入失败，" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_OxygenTH:
                    if (msgContext != null && msgContext.startsWith("Write OxygenTh=")) {
                        showMsg(0, "阈值录入成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "录入失败，" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_WaterTempTH:
                    if (msgContext != null && msgContext.startsWith("Write WaterTempTh=")) {
                        showMsg(0, "阈值录入成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "录入失败，" + msgContext);
                    }
                    break;
                case Constant.Return_Wirte_FlowTH:
                    if (msgContext != null && msgContext.startsWith("Write FlowTh=")) {
                        showMsg(0, "阈值录入成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "录入失败，" + msgContext);
                    }
                    break;
                case Constant.RETURN_CYX_CYP_NetInfo:
                    if (msgContext != null && msgContext.startsWith("Write CypIpPort=")) {
                        showMsg(0, "采样通讯地址设置成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "设置失败，" + msgContext);
                    }
                    break;
                case Constant.RETURN_Samflow_Write:
                    if (msgContext != null && msgContext.startsWith("Write Samflow=")) {
                        showMsg(0, "采样流量修正系数录入成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "录入失败，" + msgContext);
                    }
                    break;
                case Constant.RETURN_PipeLen_Write:
                    if (msgContext != null && msgContext.startsWith("Write PipeLen=")) {
                        showMsg(0, "采样管长录入成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "录入失败，" + msgContext);
                    }
                    break;
                case Constant.RETURN_Capacity_Write:
                    if (msgContext != null && msgContext.startsWith("Write Capacity=")) {
                        showMsg(0, "采样管容量录入成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshBottleList();
                            }
                        });
                    } else {
                        showMsg(1, "录入失败，" + msgContext);
                    }
                    break;
            }


        } else if (arg1 == 2) {//日志信息
            String msgContext = (String) obj;
            if (arg2 == Constant.Return_Ble_ConnectStop) {
                SpannableStringBuilder builder = new SpannableStringBuilder("日志:" + msgContext);
                //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                showMsg(2, msgContext);
                if (loadingDialog != null) loadingDialog.dismiss();
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
            showMsg(2, "设备蓝牙Mac为初始化地址，连接后请尽快扫码设置！");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++) {
                    //String ads="19:98:03:17:03:20";
                    if (mble.connectDevice(macAddress, bleCallBack))    //连接蓝牙设备
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
                            if (loadingDialog != null && !LiherCYX7Activity.this.isFinishing()) {
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
                                    sampleNoDialog = new HitiInputStrDailog(LiherCYX7Activity.this, Constant.Dailog_Input_SampleNo, "装瓶样品信息", "样品编号", "", LiherCYX7Activity.this);

                                }
                            }
                        });
                    } else {
                        if (mble.getGatt() != null) {
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
                        new HitiStringDailog(LiherCYX7Activity.this, Constant.Dailog_BLE_MACAddress, macAds, "Mac地址确认", "确认设置Mac地址为：" + StringUtils.getNewBlueToothMacAddress(macAds) + "?\n蓝牙将重启后生效", LiherCYX7Activity.this);
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
                    showMsg(2, "请输入正确的样品编号");
                    break;
            }
        }
    }
}
