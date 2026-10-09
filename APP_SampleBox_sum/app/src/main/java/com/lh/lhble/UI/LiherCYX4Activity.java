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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.BoxSamplePlan;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.Entity.SampleBottle;
import com.lh.lhble.Entity.SampleBox;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DlgTime;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.UI.View.BoxSamplePlanDailog;
import com.lh.lhble.UI.View.CYX4JYTimeInputDailog;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.UI.View.SelecterObjectDailog;
import com.lh.lhble.UI.View.TimeModeDelayDailog;
import com.lh.lhble.adapter.SampleBottle1Adapter;
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
 * 0D04单瓶采样箱
 */
public class LiherCYX4Activity extends BaseFragmentActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherCYX4Activity.class.getSimpleName();

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
    private TextView tvDevName, tvDevNum, tvDevVersion, tvSysTime, tvDevIP,tvNetPort, tvMacAddress, tvSetMacAddress;
    private TextView tvSampleMode, tvWorkStep, tvWorkTimes, tvWorkPlan, tvWorkRain, tvWorkWaterHeight, tvRequestMsgCycle, tvWorkStatus, tvSampleNextTime, tvDevEV, tvUpdateMsgCycle, tvJyTime, tvSampleBottleNum,
            tvPyTime,tvWashTime, tvDevRiss,tvInternetCID,tvDatLow,tvSampleLLJ;
    private Switch swAlarmState,swGPSSearch;
    private TextView tvDevReset, tvDevOpen;
    private ImageView ivExpandContract;
    private LinearLayout llOrderMode, llWorkTimes, llWorkPlan, llWorkRain, llWorkWaterHeight, llRequestMsgCycle, llSampleNextTime, llWorkStep,llSettingInfo,llBatLow;
    private ScrollView sclOrders;
    private LinearLayout llOrders;
    private EditText sendData;
    private TextView send;

    private SwipeRefreshLayout srlSampleBottles;
    private ListView lvSampleBottles;
    private SampleBottle1Adapter mAdapter;
    private LoadingDialog loadingDialog = null;
    private LoadingDialog sampleDialog=null;
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
    private boolean alarmState,gpsSearch;
    private String tempBottleMac;
    private HitiInputStrDailog sampleNoDialog;
    private int isBack;//是否因蓝牙中断回退页面 1-回退
    private long needSecond=370,tempSecond;//采样所需倒计时秒，当前倒计时秒
    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_cyx4;
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
        Intent intent = new Intent(context, LiherCYX4Activity.class);
        intent.putExtra(LiherCYX4Activity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherCYX4Activity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherCYX4Activity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
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
        tvNetPort=findViewById(R.id.tv_net_port);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);
        tvSampleMode = findViewById(R.id.tv_sample_mode);
        tvWorkStep = findViewById(R.id.tv_work_step);
        tvWorkTimes = findViewById(R.id.tv_work_times);
        tvWorkPlan = findViewById(R.id.tv_work_plan);
        tvWorkRain = findViewById(R.id.tv_work_rain);
        tvWorkWaterHeight = findViewById(R.id.tv_work_water_height);
        tvRequestMsgCycle = findViewById(R.id.tv_request_msg_cycle);
        tvUpdateMsgCycle = findViewById(R.id.tv_update_msg_cycle);
        tvWorkStatus = findViewById(R.id.tv_work_status);
        tvSampleNextTime = findViewById(R.id.tv_sample_next_time);
        tvDevEV = findViewById(R.id.tv_dev_ev);
        tvJyTime = findViewById(R.id.tv_jy_time);
        tvPyTime = findViewById(R.id.tv_py_time);
        tvWashTime=findViewById(R.id.tv_wash_time);
        tvDevRiss = findViewById(R.id.tv_dev_riss);
        tvInternetCID=findViewById(R.id.tv_internet_cid);
        tvDatLow=findViewById(R.id.tv_bat_low);
        tvSampleLLJ=findViewById(R.id.tv_sample_llj);
        tvSampleBottleNum = findViewById(R.id.tv_sample_bottle_num);
        swAlarmState = findViewById(R.id.sw_alarm_state);
        swGPSSearch=findViewById(R.id.sw_gps_search);
        tvDevReset = findViewById(R.id.tv_dev_reset);
        tvDevOpen = findViewById(R.id.tv_dev_open);
        llOrderMode = findViewById(R.id.ll_order_mode);
        llWorkTimes = findViewById(R.id.ll_work_times);
        llWorkPlan = findViewById(R.id.ll_work_plan);
        llWorkRain = findViewById(R.id.ll_work_rain);
        llWorkWaterHeight = findViewById(R.id.ll_work_water_height);
        llRequestMsgCycle = findViewById(R.id.ll_request_msg_cycle);
        llSettingInfo=findViewById(R.id.ll_setting_info);
        ivExpandContract=findViewById(R.id.iv_expand_contract);
        llSampleNextTime = findViewById(R.id.ll_sample_next_time);
        llWorkStep = findViewById(R.id.ll_work_step);
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        sendData = findViewById(R.id.sendData);
        llBatLow=findViewById(R.id.ll_bat_low);
        send = findViewById(R.id.send);
        srlSampleBottles = findViewById(R.id.srl_sample_bottles);
        lvSampleBottles = findViewById(R.id.lv_sample_bottles);
        sampleBottleList = new ArrayList<>();
        mAdapter = new SampleBottle1Adapter(LiherCYX4Activity.this);
        lvSampleBottles.setAdapter(mAdapter);
        tvRefresh.setOnClickListener(LiherCYX4Activity.this);
        tvSysTime.setOnClickListener(LiherCYX4Activity.this);
        tvSetMacAddress.setOnClickListener(LiherCYX4Activity.this);
        tvSampleMode.setOnClickListener(LiherCYX4Activity.this);
        tvWorkTimes.setOnClickListener(LiherCYX4Activity.this);
        tvWorkPlan.setOnClickListener(LiherCYX4Activity.this);
        tvWorkRain.setOnClickListener(LiherCYX4Activity.this);
        tvWorkWaterHeight.setOnClickListener(LiherCYX4Activity.this);
        tvRequestMsgCycle.setOnClickListener(LiherCYX4Activity.this);
        tvDevIP.setOnClickListener(LiherCYX4Activity.this);
        tvNetPort.setOnClickListener(LiherCYX4Activity.this);
        ivExpandContract.setOnClickListener(LiherCYX4Activity.this);
        tvJyTime.setOnClickListener(LiherCYX4Activity.this);
        tvDevEV.setOnClickListener(LiherCYX4Activity.this);
        tvPyTime.setOnClickListener(LiherCYX4Activity.this);
        tvWashTime.setOnClickListener(LiherCYX4Activity.this);
        tvInternetCID.setOnClickListener(LiherCYX4Activity.this);
        tvDatLow.setOnClickListener(LiherCYX4Activity.this);
        tvUpdateMsgCycle.setOnClickListener(LiherCYX4Activity.this);
        tvSampleBottleNum.setOnClickListener(LiherCYX4Activity.this);
        tvDevReset.setOnClickListener(LiherCYX4Activity.this);
        tvDevOpen.setOnClickListener(LiherCYX4Activity.this);
        send.setOnClickListener(LiherCYX4Activity.this);
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
                     /*   JSONObject jo0 = new JSONObject();
                        jo0.put("MyName", "装瓶");
                        jo0.put("MyObject", sampleBottle);
                        jat.add(jo0);*/
                        currentSampleBottle=sampleBottle;
                        if (currentSampleBottle.getBottleNo() < 1) {
                            refreshBottleList();
                            return;
                        }
                        startActivityForResult(new Intent(LiherCYX4Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);
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
                       /* JSONObject jo4 = new JSONObject();
                        jo4.put("MyName", "留样");
                        jo4.put("MyObject", sampleBottle);
                        jat.add(jo4);*/
                    }
                    new SelecterObjectDailog(LiherCYX4Activity.this, sampleBottle.getBottleNo()+"号瓶操作指令", jat, Constant.Dailog_Select_SampleBottle_Action, LiherCYX4Activity.this);

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
        if (HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
            llBatLow.setVisibility(View.VISIBLE);
            tvSetMacAddress.setVisibility(View.VISIBLE);
        }else {
            llBatLow.setVisibility(View.GONE);
            tvSetMacAddress.setVisibility(View.GONE);
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
        }else  if (mble == null || !mble.isConnect()) {

            showMsg(2, "蓝牙连接已中断，请返回扫描连接后重试");
            if (sampleDialog!=null&&sampleDialog.isShowing()&&!StringUtils.isEmpty(sampleDialog.getMessage())&&sampleDialog.getMessage().startsWith("进度倒计时")){

            }else if (isBack<1){
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
        if (LiherCYX4Activity.this.isFinishing()){
            return;
        }
        loadingDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mSendCharact, bytes)) {
                    addLogText("发送失败！",0, Constant.Return_BLE_False);
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
                if (sampleDialog!=null&&sampleDialog.isShowing()&&!StringUtils.isEmpty(sampleDialog.getMessage())&&sampleDialog.getMessage().startsWith("进度倒计时")){

                }else {
                   // if (StringUtils.isEmpty(tempBottleMac))finish();
                }

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
                        case Constant.Param_OPEN_LOCK:
                            currentReceiveType = Constant.Return_OPEN_LOCK;
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
                        case Constant.Param_Write_SWJ_DEVIP:
                            currentReceiveType=Constant.Return_Write_SWJ_DEVIP;
                            break;
                        case Constant.Param_Write_SWJ_NetPort:
                            currentReceiveType = Constant.Return_Write_SWJ_NetPort;
                            break;
                        case Constant.Param_Write_Box_Mac_Address:
                            currentReceiveType = Constant.Return_Write_Box_Mac_Address;
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
                            currentReceiveType=Constant.Return_Read_LS;
                            break;
                        case Constant.PARAM_BLE_CloseCap:
                            currentReceiveType=Constant.Return_BLE_CloseCap;
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
                        case Constant.Param_Write_WashTime:
                            currentReceiveType = Constant.Return_Write_WashTime;
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
                        showMsg(2, stateMsg);
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
                startActivityForResult(new Intent(LiherCYX4Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_Box_MAC_Address);
                break;
            case R.id.tv_refresh:
                refreshDetails();
                break;
            case R.id.iv_expand_contract:
                if (llSettingInfo.getVisibility()==View.VISIBLE){
                    ivExpandContract.setSelected(false);
                    llSettingInfo.setVisibility(View.GONE);
                }else {
                    ivExpandContract.setSelected(true);
                    llSettingInfo.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.tv_sample_mode:
                showSelectSampleModeMenu();
                break;
            case R.id.tv_sys_time:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherCYX4Activity.this);
                break;
            case R.id.tv_dev_ip:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dialog_SWJ_DevIP, "设置平台地址","平台地址：", (sampleBox!=null&&StringUtils.isEmpty(sampleBox.getNet()))?sampleBox.getNet().trim():"", LiherCYX4Activity.this);
                break;
            case R.id.tv_net_port:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dialog_SWJ_NetPort, HJBleApplication.getAppContext().getString(R.string.set_net_port), HJBleApplication.getAppContext().getString(R.string.net_port), (sampleBox!=null&&StringUtils.isEmpty(sampleBox.getPort()))?(sampleBox.getPort().trim()+""):"", LiherCYX4Activity.this);
                break;
            case R.id.tv_work_times:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_BLE_CycTime, "等时采样周期", "等时周期（分钟）:", "", LiherCYX4Activity.this);
                break;
            case R.id.tv_dev_reset:
                sendLHBleData("ln", Constant.Param_DEV_Reset);
                break;
            case R.id.tv_dev_open:
                sendLHBleData("ll", Constant.Param_OPEN_LOCK);
                break;
            case R.id.tv_jy_time:
                new CYX4JYTimeInputDailog(LiherCYX4Activity.this,Constant.Dailog_Box_JyTime,"通过目标采样量调整进样时长",(sampleBox!=null&&sampleBox.getJyTime()>0)?(sampleBox.getJyTime()+""):"",LiherCYX4Activity.this);
              //  new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_Box_JyTime, "每次采样的进样时长", "进样时长(秒):", "", LiherCYX4Activity.this);
                break;
            case R.id.tv_py_time:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_Box_PyTime, "每次排空水样的时长", "排样时长(秒):", "", LiherCYX4Activity.this);
                break;
            case R.id.tv_wash_time:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_DX3_WashTime, "每次润洗时长", "润洗时长(秒):", "", LiherCYX4Activity.this);

                break;
            case R.id.tv_internet_cid:
                sendLHBleData("Read CID", Constant.Param_CYX_ICCID);
                break;
            case R.id.tv_bat_low:
                if (sampleBox==null||sampleBox.getBatLow()<=0){
                    showMsg(2, "当前采样箱版本不支持设置低压阈值，请尽快升级设备程序");

                }else {
                    new HitiInputStrDailog(LiherCYX4Activity.this,Constant.Dialog_CYX_Input_BatLow,"修改低压阈值","低压阈值(V):",sampleBox.getBatLow()>0?(DoubleUtils.div(sampleBox.getBatLow(),100d,2)+""):"","请输入正确的低压阈值(V)，10.00~24.00", LiherCYX4Activity.this);

                }

                break;
            case R.id.tv_sample_bottle_num:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dialog_Box_Sample_Bottle, "修改当前采样瓶号", "采样瓶号:", "", LiherCYX4Activity.this);
                break;
            case R.id.tv_work_rain:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_Box_RainValue, "触发采样雨量值", "触发雨量(毫米):", "", LiherCYX4Activity.this);
                break;
            case R.id.tv_work_water_height:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_Box_Water_HeightValue, "触发采样液位值", "液位阀值(厘米):", "", LiherCYX4Activity.this);
                break;
            case R.id.tv_request_msg_cycle:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_Box_RequestMsg_Cycle, "同步平台指令周期", "同步周期(分钟):", "", LiherCYX4Activity.this);
                break;

            case R.id.tv_update_msg_cycle:
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_Box_UpdateMsg_Cycle, "上传数据间隔周期", "上传周期(分钟):", "", LiherCYX4Activity.this);
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
                new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_Input_CyNum, "设置等比例采样", "等比例采样次数", "", LiherCYX4Activity.this);

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
            case Constant.Dailog_Box_RainValue:
                String rainStr = (String) target;
                if (StringUtils.isDouble(rainStr)) {
                    Double rainValue = Double.parseDouble(rainStr);
                    double rainNum = DoubleUtils.div(rainValue, 0.2, 1);
                    if (rainNum > 200 || rainNum < 1) {
                        showMsg(2, "请输入正确的雨量值，0.2~40毫米之间");
                    } else {
                        sendLHBleData("Write Rainth=" + (int) rainNum, Constant.Param_Wirte_Rainth);
                    }
                }

                break;

            case Constant.Dailog_Box_JyTime:
                String jyTimeStr = (String) target;
                if (jyTimeStr != null && StringUtils.isInt(jyTimeStr)) {
                    int jyTime = Integer.parseInt(jyTimeStr);
                    if (jyTime >= 5 && jyTime <= 350) {
                        sendLHBleData("Write JyTime=" + jyTime, Constant.Param_Write_JyTime);
                        return;
                    }
                }
                showMsg(2, "请输入正确的进样时长，5~350秒之间");

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
            case Constant.Dialog_CYX_Input_BatLow:
                String batLowStr = (String) target;
                if (batLowStr != null && StringUtils.isDouble(batLowStr)) {
                    int batLow = (int) (DoubleUtils.round(Double.parseDouble(batLowStr),2)*100);
                    if (batLow >= 1000&&batLow<=2400) {
                        sendLHBleData("Write lowbatt=" + batLow, Constant.Param_Write_BatLow);
                        return;
                    }
                }
                showMsg(2, "请输入正确的低压阈值(V)，10.00~24.00");

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
            case Constant.Dailog_Box_Water_HeightValue:
                String waterHeight = (String) target;
                if (StringUtils.isInt(waterHeight)) {
                    int waterValue = Integer.parseInt(waterHeight);

                    if (waterValue > 1000 || waterValue < 1) {
                        showMsg(2, "请输入正确液位阀值，1~1000厘米的整数");
                    } else {
                        sendLHBleData("Write SamYW=" + (int) waterValue, Constant.Param_Write_Water_Height);

                    }
                } else {
                    showMsg(2, "请输入正确液位阀值，1~1000厘米的整数");

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
            case Constant.Dialog_SWJ_DevIP:
                String strIP = (String) target;
                if (!StringUtils.isEmpty(strIP)&&strIP.length()<40){
                    sendLHBleData("Write net=" +strIP, Constant.Param_Write_SWJ_DEVIP);
                }else {
                    showMsg(2, "请输入正确的平台地址");
                }

                break;
            case Constant.Dialog_SWJ_NetPort:
                String strPort = (String) target;
                if (StringUtils.isInt(strPort)){
                    sendLHBleData("Write port=" +Integer.parseInt(strPort), Constant.Param_Write_SWJ_NetPort);
                }else {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_right_port));
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
                startActivityForResult(new Intent(LiherCYX4Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_Add_SampleNo);
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

                sendLHBleData("lc",Constant.PARAM_BLE_CloseCap);
                break;
            case Constant.Dailog_DX3_WashTime:
                String washTimeStr = (String) target;
                if (washTimeStr != null && StringUtils.isInt(washTimeStr)) {
                    int washTime = Integer.parseInt(washTimeStr);
                    if (washTime >0&&washTime<=600) {
                        sendLHBleData("Write WashTime=" + washTime, Constant.Param_Write_WashTime);
                        return;
                    }
                }
                showMsg(2, "请输入正确的润洗时长，1~600秒");

                break;
            case Constant.HitiStrTargetDialog_CYX4_WorkStep:
                refreshDetails();

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
                        startActivityForResult(new Intent(LiherCYX4Activity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);

                        break;
                    case "卸瓶":
                        String str = "0" + currentSampleBottle.getBottleNo();
                        if (str.length() == 3) str = str.substring(1);
                        sendLHBleData("Write Subaddr=" + str + "000000000000-0000000000000000", Constant.Param_Write_Subaddr);
                        break;
                    case "采样":
                        showMsg(2, "发起"+currentSampleBottle.getBottleNo()+"号瓶采样");
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
        if (!HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){//无生成维修权限的用户无版本更新功能
            popupMenu.getMenu().findItem(R.id.order_mode).setVisible(false);
        }
        popupMenu.getMenu().findItem(R.id.version_upgrade).setVisible(false);
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
            }else if (arg2==Constant.Return_BLE_False){
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
                   //    00#01#00/13#00060#000000#000#000000#230223#230303171556#0#0D0023020001#24.0#                 0#001#100#800#0#0#55#  #0#0	#-48-48-48-48-48-48-48-48-48-48-48-48-48-48-48-48-48-48-48-48
                  try {
                      String[] details = msgContext.split("#", -1);
                      if (sampleBox == null) sampleBox = new SampleBox();
                      if (details.length >= 18) {
                          sampleBox.setSampleMode(details[0]);
                          sampleBox.setCurrentSampleBottleNo(details[1]);
                          int jyTime=Integer.parseInt(details[15]);
                          sampleBox.setJyTime(jyTime);
                          needSecond=jyTime+105;


                          sampleBox.setCycleTime(Integer.parseInt(details[3]));
                          if (!"_".equals(details[4].trim()))
                              sampleBox.setNextSampleTime(Integer.parseInt(details[4]));
                          sampleBox.setDevStatus(details[5]);
                          String devStatus=StringUtils.getDevStatus(sampleBox.getDevStatus());
                          tvWorkStatus.setText(devStatus);
                          if (devStatus!=null&&devStatus.length()>3){
                              tvWorkStatus.setTextColor(getResources().getColor(R.color.color_red));
                              playAlarmSoundAndVibrate();
                              showMsg(2, "警告："+devStatus);
                          }else {
                              tvWorkStatus.setTextColor(getResources().getColor(R.color.color_black_333333));
                          }



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
                          sampleBox.setUpdateCycle(Integer.parseInt(details[16].trim()));
                          sampleBox.setRequestCycle(Integer.parseInt(details[17].trim()));
                          if (details.length>=23){
                              sampleBox.setPyTime(Integer.parseInt(details[18].trim()));
                              sampleBox.setRiss(details[19]);
                              sampleBox.setCyNum(Integer.parseInt(details[20].trim()));
                              String icc=details[22];
                              if (StringUtils.isEmpty(icc)||"00000000000000000000".equals(icc.trim())){
                                  sampleBox.setIccID("");
                                  tvInternetCID.setText("物联网卡号ICCID值为空");
                              }else if (icc.trim().length()!=20){
                                  sampleBox.setIccID(icc.trim());
                                  showMsg(2, "物联网卡号非20位规范值！\n"+icc.trim());
                                  tvInternetCID.setText("返回值非20位规范卡号！");
                              }else {
                                  sampleBox.setIccID(icc.trim());
                                  tvInternetCID.setText(icc.trim());
                              }
                          }
                          if (details.length>=27){
                              gpsSearch=Integer.parseInt(details[23].trim())==1?true:false;
                              sampleBox.setGnns(gpsSearch?1:0);
                              swGPSSearch.setChecked(gpsSearch);
                              sampleBox.setBatLow(Integer.parseInt(details[24].trim()));
                              tvDatLow.setText(DoubleUtils.div(sampleBox.getBatLow(),100d,2)+"");
                              sampleBox.setLlj(Integer.parseInt(details[25].trim()));
                              sampleBox.setWashTime(Integer.parseInt(details[26].trim()));
                          }
                          if (details.length>=29){
                              sampleBox.setNet(details[27]);
                              sampleBox.setPort(details[28]);
                              if (!StringUtils.isEmpty(sampleBox.getNet()))tvDevIP.setText(sampleBox.getNet().trim());
                              if (!StringUtils.isEmpty(sampleBox.getNet()))tvNetPort.setText(sampleBox.getPort().trim()+"");
                          }
                          if ("08".equals(sampleBox.getSampleMode()) && sampleBox.getCyNum() >= 0) {
                              tvSampleMode.setText("等比例采样" + sampleBox.getCyNum() + "次");
                          } else {
                              tvSampleMode.setText(StringUtils.getSampleMode(sampleBox.getSampleMode()));
                          }

                          llWorkTimes.setVisibility(View.GONE);
                          llWorkPlan.setVisibility(View.GONE);
                          llWorkRain.setVisibility(View.GONE);
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
                                  llSampleNextTime.setVisibility(View.VISIBLE);
                                  break;
                              case "02":
                                  llWorkTimes.setVisibility(View.VISIBLE);
                                  llWorkRain.setVisibility(View.VISIBLE);
                                  llWorkStep.setVisibility(View.VISIBLE);
                                  break;
                              case "03":
                                  llWorkTimes.setVisibility(View.VISIBLE);
                                  llWorkRain.setVisibility(View.VISIBLE);
                                  llWorkStep.setVisibility(View.VISIBLE);
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
                                  llSampleNextTime.setVisibility(View.VISIBLE);
                                  break;
                              case "09":
                                  llWorkStep.setVisibility(View.VISIBLE);
                                  break;
                              case "10":
                                  llWorkStep.setVisibility(View.VISIBLE);
                                  break;
                              case "11":
                                  llWorkStep.setVisibility(View.VISIBLE);
                                  break;
                              case "12":
                                  llWorkStep.setVisibility(View.VISIBLE);
                                  break;
                              case "13":
                                  llWorkStep.setVisibility(View.VISIBLE);
                                  break;
                              case "14":
                                  llWorkStep.setVisibility(View.VISIBLE);
                                  break;
                          }
                          if (!StringUtils.isEmpty(sampleBox.getSysTime()) && sampleBox.getSysTime().startsWith("2")) {

                              tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" + sampleBox.getSysTime(), "yyyyMMddHHmmss")));
                              if (Math.abs(System.currentTimeMillis() - DateUtil.format2miao("20" + sampleBox.getSysTime(), "yyyyMMddHHmmss")) > 1* 60 * 1000){
                                  new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherCYX4Activity.this);
                              }
                          }else {
                              new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherCYX4Activity.this);

                          }

                          tvDevVersion.setText(sampleBox.getVersion());

                          tvWorkTimes.setText(sampleBox.getCycleTime() + "分钟");

                          tvSampleNextTime.setText(sampleBox.getNextSampleTime() / 60 + "分钟");
                          tvDevEV.setText(" "+sampleBox.getDevEV() + "V");
                          tvSampleBottleNum.setText(sampleBox.getCurrentSampleBottleNo() + "瓶");
                          swAlarmState.setChecked(sampleBox.getAlarmState() == 1);
                          tvWorkRain.setText(DoubleUtils.mul(sampleBox.getRainNum(), 0.2) + "mm");
                          tvWorkWaterHeight.setText(sampleBox.getWaterHeight() + "cm");
                          if (sampleBox.getJyTime() <= 350&&sampleBox.getJyTime()>0) {
                              double height=0;
                              if (jyTime>202){
                                  height=  DoubleUtils.round( (jyTime-172)/5,2);
                              }else if (jyTime>=34){
                                  height=  DoubleUtils.round( (jyTime-34)/28,2);
                              }
                              tvJyTime.setText(height + "m");
                          } else {
                              tvJyTime.setText("");
                          }
                          if (sampleBox.getPyTime() > 0) {
                              tvPyTime.setText(sampleBox.getPyTime() + "秒");
                          }
                          if (sampleBox.getWashTime()>0){
                              tvWashTime.setText(sampleBox.getWashTime()+"秒");
                          }
                          if (sampleBox.getPyTime()>0&&(StringUtils.isEmpty(sampleBox.getRiss()) || "0".equals(sampleBox.getRiss()))) {
                              tvDevRiss.setText(" 离线");
                          } else {
                              tvDevRiss.setText(StringUtils.isEmpty(sampleBox.getRiss())?" ":" "+sampleBox.getRiss());
                          }
                          tvUpdateMsgCycle.setText(sampleBox.getUpdateCycle() == 0 ? "不上传" : (sampleBox.getUpdateCycle() + "分钟"));
                          tvRequestMsgCycle.setText(sampleBox.getRequestCycle() + "分钟");
                          if (sampleBox.getLlj()==1){
                              tvSampleLLJ.setText("无水");
                              tvSampleLLJ.setTextColor(getResources().getColor(R.color.color_black_999999));
                          }else if (sampleBox.getLlj()==0){
                              tvSampleLLJ.setText("有水");
                              tvSampleLLJ.setTextColor(getResources().getColor(R.color.color_app_normal));
                          }else {
                              tvSampleLLJ.setText("未知"+sampleBox.getLlj());
                              tvSampleLLJ.setTextColor(getResources().getColor(R.color.color_black_999999));
                          }
                          sampleBottleList = new ArrayList<>();
                          sendLHBleData("Read Subaddr", Constant.Param_Read_Subaddr);
                          if (!StringUtils.isEmpty(details[2])&&details[2].split("/").length==2){
                              int currentStep=Integer.parseInt(details[2].split("/")[0]);
                              int allStep=Integer.parseInt(details[2].split("/")[1]);
                              if (currentStep==0){
                                  sampleBox.setWorkStep("");
                              }else if (currentStep>allStep){
                                  sampleBox.setWorkStep("\t100%");
                              }else {
                                  sampleBox.setWorkStep("\t" + currentStep*100/allStep+"%");
                                  // startSampleing();
                                  new HitiStringDailog(LiherCYX4Activity.this,Constant.HitiStrTargetDialog_CYX4_WorkStep,null,"查询最新采样进度","目前采样进度为"+ currentStep*100/allStep+"%"+((devStatus!=null&&devStatus.length()>3)?",故障警告："+devStatus:"")+"\n是否刷新获取最新采样进度？",LiherCYX4Activity.this);
                              }

                          }else {
                              sampleBox.setWorkStep(details[2]);
                          }
                          tvWorkStep.setText(sampleBox.getWorkStep());
                      }
                  }catch (Exception e){
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
                            boxSamplePlanDailog = new BoxSamplePlanDailog(LiherCYX4Activity.this, "定时采样计划", samplePlanList, sampleBox,Constant.Dailog_Box_SamplePlan, LiherCYX4Activity.this);

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
                    if (msgContext != null && msgContext.contains("Write lowbatt=")){
                        showMsg(0, "设置低压阈值成功");
                        refreshDetails();
                    }else {
                        showMsg(1, "设置低压阈值失败,\n" + msgContext);
                    }
                    break;

                case Constant.Return_Wirte_Mode:
                    if (msgContext != null && msgContext.contains("Write Mode=")) {
                        if ("Write Mode=1".equals(msgContext)) {
                            DlgTime.showYMDDialog(LiherCYX4Activity.this, Calendar.getInstance(), Calendar.getInstance(), null,
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
                            new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_Input_CyNum, "设置等比例采样", "等比例采样次数", "", LiherCYX4Activity.this);
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
                        tvWorkTimes.setText(cycTime + "分钟");
                    }
                    break;
                case Constant.Return_Write_Subaddr:
                    if (msgContext != null && msgContext.startsWith("Write Subaddr=")) {
                        tempBottleMac=null;
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
                       /* RxTimerUtil.cancel();
                        RxTimerUtil.timer(1000, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();

                            }
                        });*/
                        startSampleing();

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
                    if (msgContext != null && msgContext.contains("Write Rainth=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int rainNum = Integer.parseInt(msgContext.substring(13).trim());
                        if (sampleBox != null) sampleBox.setRainNum(rainNum);
                        tvWorkRain.setText(DoubleUtils.mul(rainNum, 0.2) + "mm");
                    } else {
                        showMsg(1, "设置采样雨量触发值失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_JyTIme:
                    if (msgContext != null && msgContext.contains("Write JyTime=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int jyTime = Integer.parseInt(msgContext.substring(13).trim());
                        if (sampleBox != null) sampleBox.setJyTime(jyTime);
                        double height=0;
                        if (jyTime>202){
                            height=  DoubleUtils.round( (jyTime-172)/5,2);
                        }else if (jyTime>=34){
                            height=  DoubleUtils.round( (jyTime-34)/28,2);
                        }
                        tvJyTime.setText(height + "m");
                    } else {
                        showMsg(1, "设置进样时长失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_JyThreshold:
                    if (msgContext != null && msgContext.contains("Write JyTime=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int jyThreshold = Integer.parseInt(msgContext.substring(13).trim());
                        if (sampleBox != null) sampleBox.setJyTime(jyThreshold);
                        tvJyTime.setText("");
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
                case Constant.Return_Wirte_CyNum:
                    if (msgContext != null && msgContext.contains("Write scale=")) {
                        DlgTime.showYMDDialog(LiherCYX4Activity.this, Calendar.getInstance(), Calendar.getInstance(), null,
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
                        connectBleMac(mDeviceAddress,255,false);
                    } else if (msgContext != null && msgContext.contains("1")){
                        if (loadingDialog != null) loadingDialog.dismiss();
                        new HitiStringDailog(LiherCYX4Activity.this,Constant.HitiStrTargetDialog_Bottle_CloseCap,"","关闭瓶阀","样瓶阀门未关，请确认关闭瓶阀后才可装瓶", LiherCYX4Activity.this);

                    }else{
                        showMsg(1, msgContext+"样瓶阀门状态异常无法装瓶！");

                    }
                    break;
                case Constant.Return_BLE_CloseCap:
                    if (msgContext != null && msgContext.trim().equals("olc")) {
                        mble.disconnectDevice();
                        mble.disconnectLocalDevice();
                        showMsg(0, "阀门关闭成功");
                        connectBleMac(mDeviceAddress,255,false);


                    }else if (msgContext != null && !msgContext.trim().equals("lc")){
                        showMsg(1, msgContext+"样瓶关阀失败，无法装瓶！");

                    }else if (msgContext!=null&&msgContext.trim().equals("lc")){
                        showMsg(2, "正在关闭瓶阀，请稍后...");
                    }
                    break;

                case Constant.Return_Write_TimeMode_Delay:
                    if (msgContext != null && msgContext.startsWith("Write delay=")) {
                        int delay = Integer.parseInt(msgContext.substring(12).trim());
                        showMsg(2, delay / 60 + "分钟后生效");
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
                case Constant.Return_Write_Water_Height:
                    if (msgContext != null && msgContext.contains("Write SamYW=")) {
                        showMsg(0, "提交成功，正在重新加载信息...");
                        int waterValue = Integer.parseInt(msgContext.substring(12).trim());
                        if (sampleBox != null) sampleBox.setWaterHeight(waterValue);
                        tvWorkWaterHeight.setText(waterValue + "cm");
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
                        tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" +  sampleBox.getSysTime(), "yyyyMMddHHmmss")));
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
                    if ("00000000000000000000".equals(msgContext)){
                        tvInternetCID.setText("物联网卡号ICCID值为空");
                        showMsg(0, "读取物联网卡号成功~");
                    }else if (msgContext != null && msgContext.length()==20){
                        tvInternetCID.setText(msgContext);
                        showMsg(0, "读取物联网卡号成功~");
                    }else {
                        showMsg(2, "返回的物联网卡号非20位规范值！\n"+msgContext.trim());
                    }


                    break;
                case Constant.Return_Write_WashTime:
                    if (msgContext != null && msgContext.contains("Write WashTime=")){
                        showMsg(0, "润洗时长设置成功");
                        refreshDetails();
                    }else {
                        showMsg(1, "润洗时长设置失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_SWJ_DEVIP:
                    if (msgContext != null && msgContext.contains("Write net=")){
                        showMsg(0, HJBleApplication.getAppContext().getString(R.string.set_ip_address_success));
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    }else {
                        showMsg(1, HJBleApplication.getAppContext().getString(R.string.set_fail)+",\n" + msgContext);
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

            }


        } else if (arg1 == 2) {//日志信息

        }
    }

    /**
     * 开始采样倒计时
     */
    private void startSampleing() {
        RxTimerUtil.cancel();
        playAlarmSoundAndVibrate();
        RxTimerUtil.interval(1000, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                if (number==2){
                    tempSecond=2;
                    playAlarmSoundAndVibrate();
                    if (sampleDialog!=null&&sampleDialog.isShowing()&&!StringUtils.isEmpty(sampleDialog.getMessage())&&sampleDialog.getMessage().startsWith("进度倒计时")){
                        sampleDialog.setMessage("进度倒计时"+(needSecond-tempSecond)+"秒");
                    }else {
                        LoadingDialog.Builder sampleBuilder = new LoadingDialog.Builder(LiherCYX4Activity.this)
                                .setCancelable(true)
                                .setShowMessage(true)
                                .setMessage("进度倒计时"+(needSecond-tempSecond)+"秒")
                                .setCancelOutside(true).setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        RxTimerUtil.cancel();
                                    }
                                });
                        sampleDialog = sampleBuilder.create();
                        sampleDialog.show();
                    }
                }else if (number>2){
                    if (number%4==0){
                        playAlarmSoundAndVibrate();
                    }
                    tempSecond=number;
                    if (sampleDialog!=null&&sampleDialog.isShowing()&&!StringUtils.isEmpty(sampleDialog.getMessage())&&sampleDialog.getMessage().startsWith("进度倒计时")){
                        if (tempSecond>=needSecond){
                            sampleDialog.cancel();
                        }else {
                            sampleDialog.setMessage("进度倒计时"+(needSecond-tempSecond)+"秒");
                        }
                    }else if (sampleDialog!=null&&!LiherCYX4Activity.this.isFinishing()){
                        sampleDialog.cancel();
                    }
                }

            }
        });
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
            llUIMode.setVisibility(View.VISIBLE);
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
            llUIMode.setVisibility(View.VISIBLE);
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
    // 连接蓝牙
    private void connectBleMac( final String macAddress, final int sendDataLenMax, final boolean isConfig) {
        if ("00:00:00:00:00:00".equals(macAddress)){
            showMsg(2, "设备蓝牙Mac为初始化地址，连接后请尽快扫码设置！");
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
                    }catch (Exception e){

                    }
                }
                if(i == 5)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMsg(1, "连接失败");
                            loadingDialog.dismiss();
                        }
                    });
                    return ;
                }

                try {
                    Thread.sleep(1000,0);//200ms
                }
                catch (Exception e){

                }


                // 打开配置通知
                if(isConfig && !mble.openNotify(Constant.Ble_Config_Receive_Service))
                {
                    mble.disconnectDevice();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMsg(1, "连接失败1");
                            if (loadingDialog!=null&&!LiherCYX4Activity.this.isFinishing()){
                                loadingDialog.dismiss();
                            }


                        }
                    });
                    return ;
                }

                if(mble.openNotify(Constant.Ble_Data_Receive_Service))
                {
                    if (mble.requestMtu(255)){
                        mble.setSendDataLenMax(sendDataLenMax);
                        mble.mBleCallBack = bleCallBack;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                loadingDialog.dismiss();
                                HJBleApplication.shareInstance().setDesID("12345678");
                                //  ScanBleActivity.this.startActivity(LiherActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                               if (macAddress.startsWith("0B")||macAddress.startsWith("0C")){
                                   RxTimerUtil.cancel();
                                   RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                                       @Override
                                       public void doNext(long number) {
                                           sendLHBleData("ls",Constant.Param_Read_LS);
                                       }
                                   });
                               }else if (macAddress.startsWith("0D")){
                                   sampleNoDialog = new HitiInputStrDailog(LiherCYX4Activity.this, Constant.Dailog_Input_SampleNo, "装瓶样品信息", "样品编号", "", LiherCYX4Activity.this);

                               }
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
                                showMsg(1, "连接失败3,设置MTU失败");
                                loadingDialog.dismiss();

                            }
                        });
                    }

                    return ;
                }
                else
                {
                    mble.disconnectDevice();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMsg(1, "连接失败2");
                            loadingDialog.dismiss();
                        }
                    });
                    return ;
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
                        showMsg(2, "未读取到二维码信息");
                    } else if (qcode.length() != 12||!(qcode.startsWith("0B")||qcode.startsWith("0C"))) {
                        showMsg(2, "二维码，非力合蓝牙瓶标准码");
                    } else {
                        if (currentSampleBottle == null || currentSampleBottle.getBottleNo() == 0) {
                            refreshBottleList();
                        } else {
                            tempBottleMac = qcode;
                            if (mble==null){
                                showMsg(2, "蓝牙连接丢失，请返回重试");
                                finish();
                            }else {
                                mble.disconnectDevice();
                                mble.disconnectLocalDevice();
                                showMsg(2, "正在连接采样瓶...");

                                connectBleMac(StringUtils.getNewBlueToothMacAddress(tempBottleMac),255,false);

                            }

                        }

                    }
                    break;
                case Constant.RequestCode_Scan_Box_MAC_Address:
                    String boxQcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(boxQcode)) {
                        showMsg(2, "未读取到二维码信息");
                    } else if (boxQcode.length() != 12) {
                        showMsg(2, "二维码，非力合蓝牙标准码");
                    } else if (boxQcode.startsWith("0D")) {
                        String macAds = boxQcode.trim();
                        new HitiStringDailog(LiherCYX4Activity.this, Constant.Dailog_BLE_MACAddress, macAds, "Mac地址确认", "确认设置Mac地址为：" + StringUtils.getNewBlueToothMacAddress(macAds) + "?\n蓝牙将重启后生效", LiherCYX4Activity.this);
                    } else {
                        showMsg(2, boxQcode + "二维码，非力合采样箱编码规则0D");
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
