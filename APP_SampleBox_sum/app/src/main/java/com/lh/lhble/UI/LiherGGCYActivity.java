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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.GGCYBox;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.R;
import com.lh.lhble.UI.View.SelecterObjectDailog;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.google.zxing.activity.CaptureActivity;
import com.lh.lhble.listener.CommonItemOnClickListener3;
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

import static com.lh.lhble.google.zxing.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;

/**
 * 灌溉采样器
 */
public class LiherGGCYActivity extends BaseActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherGGCYActivity.class.getSimpleName();

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
    private TextView tvRefresh;
    private LinearLayout llBottle1SampleNo,llBottle2SampleNo;
    private TextView tvDevRunMode,tvDevIccID;
    private TextView clearLog, tvDevVersion, tvWorkStatus,tvErrorCode,tvSampleTime, tvDevEV, tvMacAddress, tvSetMacAddress,tvBottle1SampleNo, tvBottle1Vol, tvBottle1Ratio,tvBottle2SampleNo, tvBottle2Vol, tvBottle2Ratio, tvCurrentFlow, tvBoxTemperature,tvSampleMaxTime,
            tvMinFlow, tvMinSampleVol,tvSpaceFlow,tvTotalFlow, tvBottleClear, tvClearReset,tvOpenDoor, tvSampleStart, tvSampleStop,tvSysTime,tvSudu1,tvSudu2;

    private ScrollView sclOrders;
    private LinearLayout llOrders;
    private EditText sendData;
    private TextView send;
    private LoadingDialog loadingDialog = null;

    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;

    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private int currentSendType, currentReceiveType;
    private GGCYBox mGGCYBox;
    public static final int RequestCode_Bin_Version = 1001;

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_gg_cyq;
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
        if (HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")){
            setRightText("设置");
            rightTitleTxt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    showMsg(2,"功能正在开发完善中，请先用GRToolbox软件升级");
                   /* final Intent intent = new Intent(LiherGGCYActivity.this, SetActivity.class);

                    intent.putExtra(SetActivity.EXTRAS_SET_IS_CONFIG, isConfig);

                    LiherGGCYActivity.this.startActivityForResult(intent, RequestCode_Bin_Version);*/
                }
            });
        }

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
                showMsg(2,"连接成功，正在初始化...");
            }
        });

    }

    private void refreshDetails() {
        showMsg(2,"正在重新加载设备信息...");
        sendLHBleData("Read Detail", Constant.Param_Read_Detail);
    }


    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherGGCYActivity.class);
        intent.putExtra(LiherGGCYActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherGGCYActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherGGCYActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();
        clearLog = findViewById(R.id.clear_log);
        tvRefresh = findViewById(R.id.tv_refresh);
        tvDevEV = findViewById(R.id.tv_dev_ev);
        tvDevRunMode=findViewById(R.id.tv_dev_run_mode);
        tvDevIccID=findViewById(R.id.tv_dev_icc_id);
        llBottle1SampleNo=findViewById(R.id.ll_bottle1_sample_no);
        llBottle2SampleNo=findViewById(R.id.ll_bottle2_sample_no);
        tvBottle1Vol = findViewById(R.id.tv_bottle1_vol);
        tvBottle1Ratio = findViewById(R.id.tv_bottle1_ratio);
        tvBottle1SampleNo=findViewById(R.id.tv_bottle1_sample_no);
        tvBottle2Vol = findViewById(R.id.tv_bottle2_vol);
        tvBottle2Ratio = findViewById(R.id.tv_bottle2_ratio);
        tvBottle2SampleNo=findViewById(R.id.tv_bottle2_sample_no);
        tvDevVersion = findViewById(R.id.tv_dev_version);


        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);

        tvWorkStatus = findViewById(R.id.tv_work_status);
        tvErrorCode=findViewById(R.id.tv_err_code);
        tvSampleTime=findViewById(R.id.tv_sample_time);
        tvCurrentFlow = findViewById(R.id.tv_current_flow);
        tvBoxTemperature = findViewById(R.id.tv_box_temperature);
        tvSampleMaxTime=findViewById(R.id.tv_sample_max_time);

        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        sendData = findViewById(R.id.sendData);
        send = findViewById(R.id.send);


        // tvMinFlow,tvMinSampleVol,tvBottleClear,tvClearReset,tvSampleStart,tvSampleStop
        tvMinFlow = findViewById(R.id.tv_min_flow);
        tvMinSampleVol = findViewById(R.id.tv_min_sample_vol);
        tvSpaceFlow=findViewById(R.id.tv_space_flow);
        tvTotalFlow=findViewById(R.id.tv_total_flow);
        tvBottleClear = findViewById(R.id.tv_bottle_clear);
        tvClearReset = findViewById(R.id.tv_clear_reset);
        tvOpenDoor= findViewById(R.id.tv_open_door);
        tvSampleStart = findViewById(R.id.tv_sample_start);
        tvSampleStop = findViewById(R.id.tv_sample_stop);
        tvSysTime=findViewById(R.id.tv_sys_time);
        tvSudu1=findViewById(R.id.tv_sudu1);
        tvSudu2=findViewById(R.id.tv_sudu2);
        send.setOnClickListener(LiherGGCYActivity.this);
        tvRefresh.setOnClickListener(LiherGGCYActivity.this);
        tvSetMacAddress.setOnClickListener(LiherGGCYActivity.this);
        llBottle1SampleNo.setOnClickListener(LiherGGCYActivity.this);
        tvBottle1Vol.setOnClickListener(LiherGGCYActivity.this);
        tvBottle1Ratio.setOnClickListener(LiherGGCYActivity.this);
        llBottle2SampleNo.setOnClickListener(LiherGGCYActivity.this);
        tvBottle2Vol.setOnClickListener(LiherGGCYActivity.this);
        tvBottle2Ratio.setOnClickListener(LiherGGCYActivity.this);
        tvMinFlow.setOnClickListener(LiherGGCYActivity.this);
        tvMinSampleVol.setOnClickListener(LiherGGCYActivity.this);
        tvSampleMaxTime.setOnClickListener(LiherGGCYActivity.this);
        tvBottleClear.setOnClickListener(LiherGGCYActivity.this);
        tvClearReset.setOnClickListener(LiherGGCYActivity.this);
        tvOpenDoor.setOnClickListener(LiherGGCYActivity.this);
        tvDevRunMode.setOnClickListener(LiherGGCYActivity.this);
        tvSampleStart.setOnClickListener(LiherGGCYActivity.this);
        tvSampleStop.setOnClickListener(LiherGGCYActivity.this);
        tvSysTime.setOnClickListener(LiherGGCYActivity.this);
        tvSudu1.setOnClickListener(LiherGGCYActivity.this);
        tvSudu2.setOnClickListener(LiherGGCYActivity.this);
        clearLog.setOnClickListener(LiherGGCYActivity.this);
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

    private void showSelectRunModeMenu() {
        PopupMenu popupMenu = new PopupMenu(this, tvDevRunMode);
        popupMenu.inflate(R.menu.menu_select_run_mode);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.run_mode_0:
                        sendLHBleData("Write runmode=0", Constant.Param_Wirte_RunMode);
                        return true;
                    case R.id.run_mode_1:
                        sendLHBleData("Write runmode=1", Constant.Param_Wirte_RunMode);
                        return true;

                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
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
        }else  if (mble == null || !mble.isConnect()) {

            showMsg(2,"设备蓝牙连接已中断，请返回扫描连接后重试");
            finish();
            return;
        }
        if (LiherGGCYActivity.this.isFinishing()){
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
                    addLogText("发送失败！", 0,Constant.Return_Ble_SendFail);
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
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close), 2, Constant.Return_Ble_ConnectStop);
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
                if (!StringUtils.isEmpty(str)) {
                    if (Constant.ReceiveFalse.equals(str)){
                        addLogText("蓝牙返回数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                    }else if (Constant.ReceiveDataListFalse.equals(str)){
                        addLogText("蓝牙返回分包数据非按力合协议", 1, Constant.Return_Ble_ReceiveFalse);
                    }else if (Constant.ReceiveContinue.equals(str)){
                        addLogText("蓝牙消息返回中", 1, Constant.Return_BLE_Continue);
                    }else{
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
                            case Constant.Param_Write_Box_Mac_Address:
                                currentReceiveType = Constant.Return_Write_Box_Mac_Address;
                                break;
                            case Constant.Param_Write_GGCY_Bottle1Ratio:
                                currentReceiveType = Constant.Return_Write_GGCY_Bottle1Ratio;
                                break;
                            case Constant.Param_Write_GGCY_Bottle2Ratio:
                                currentReceiveType = Constant.Return_Write_GGCY_Bottle2Ratio;
                                break;
                            case Constant.Param_Write_GGCY_MinFlow:
                                currentReceiveType = Constant.Return_Write_GGCY_MinFlow;
                                break;
                            case Constant.Param_Write_GGCY_MinSampleVol:
                                currentReceiveType = Constant.Return_Write_GGCY_MinSampleVol;
                                break;
                            case Constant.Param_Write_GGCY_SampleStart:
                                currentReceiveType = Constant.Return_Write_GGCY_SampleStart;
                                break;
                            case Constant.Param_Write_GGCY_SampleStop:
                                currentReceiveType = Constant.Return_Write_GGCY_SampleStop;
                                break;
                            case Constant.Param_OPEN_LOCK:
                                currentReceiveType = Constant.Return_OPEN_LOCK;
                                break;
                            case Constant.Param_Write_GGCY_Clear_Bottles:
                                currentReceiveType = Constant.Return_Write_GGCY_Clear_Bottles;
                                break;
                            case Constant.Param_GGCY_Sudu1:
                                currentReceiveType = Constant.Return_GGCY_Sudu1;
                                break;
                            case Constant.Param_GGCY_Sudu2:
                                currentReceiveType = Constant.Return_GGCY_Sudu2;
                                break;
                            case Constant.Param_DEV_Reset:
                                currentReceiveType = Constant.Return_DEV_Reset;
                                break;
                            case Constant.Param_Write_GGCY_MaxSampleTime:
                                currentReceiveType = Constant.Return_Write_GGCY_MaxSampleTime;
                                break;
                            case Constant.Param_Write_Subaddr:
                                currentReceiveType = Constant.Return_Write_Subaddr;
                                break;
                            case Constant.Param_Wirte_RunMode:
                                currentReceiveType = Constant.Return_Wirte_RunMode;
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
                addLogText("日志：" + logStr, 2, 0);
            }
        }

        @Override
        public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {
            if (!StringUtils.isEmpty(stateMsg)){
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close)+","+stateMsg,2, Constant.Return_Ble_ConnectState);
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
            case R.id.tv_bottle_clear:
                new HitiStringDailog(LiherGGCYActivity.this, Constant.HitiStrTargetDialog_GGCY_BottleClear, "", "清空确认", "确认更换采样瓶并清空信息？", LiherGGCYActivity.this);
                break;
            case R.id.tv_clear_reset:
                new HitiStringDailog(LiherGGCYActivity.this, Constant.HitiStrTargetDialog_GGCY_DevReset, "", "润洗复位", "确认润洗并设备复位？", LiherGGCYActivity.this);
                break;
            case R.id.tv_sample_start:
                sendLHBleData("Write Mode=1", Constant.Param_Write_GGCY_SampleStart);
                break;
            case R.id.tv_sample_stop:
                sendLHBleData("Write Mode=0", Constant.Param_Write_GGCY_SampleStop);
                break;
            case R.id.tv_min_flow:
                new HitiInputStrDailog(LiherGGCYActivity.this, Constant.Dailog_GGCY_MinFlow, "设置最小触发流量", "触发流量L/s：", mGGCYBox == null ? "" : (DoubleUtils.round(mGGCYBox.getMinFlow(), 2) + ""), LiherGGCYActivity.this);
                break;
            case R.id.tv_min_sample_vol:
                new HitiInputStrDailog(LiherGGCYActivity.this, Constant.Dailog_GGCY_MinSampleVol, "设置最小采样量", "最小采样量mL：", mGGCYBox == null ? "" : (mGGCYBox.getMinSampleVol() + ""), LiherGGCYActivity.this);
                break;
            case R.id.ll_bottle1_sample_no:
                showMsg(2,"扫码录入瓶号1的样品编号");
                startActivityForResult(new Intent(LiherGGCYActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_Sample_No1);
                break;
            case R.id.ll_bottle2_sample_no:
                showMsg(2,"扫码录入瓶号2的样品编号");
                startActivityForResult(new Intent(LiherGGCYActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_Sample_No2);
                break;
            case R.id.tv_sample_max_time:
                new HitiInputStrDailog(LiherGGCYActivity.this, Constant.Dialog_GGCY_SampleMaxTime, "设置采样超时时长", "超时时长(小时)：", mGGCYBox == null ? "" : (mGGCYBox.getMaxSampleTime() + ""), LiherGGCYActivity.this);
                break;
            case R.id.tv_bottle1_ratio:
                new HitiInputStrDailog(LiherGGCYActivity.this, Constant.Dailog_GGCY_Bottle1Ratio, "设置1号采样瓶采样比例", "采样比例：", "", LiherGGCYActivity.this);
                break;
            case R.id.tv_bottle2_ratio:
                new HitiInputStrDailog(LiherGGCYActivity.this, Constant.Dailog_GGCY_Bottle2Ratio, "设置2号采样瓶采样比例", "采样比例：", "", LiherGGCYActivity.this);
                break;
            case R.id.tv_set_mac_address:
                startActivityForResult(new Intent(LiherGGCYActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_Box_MAC_Address);
                break;
            case R.id.tv_open_door:
                sendLHBleData("ll", Constant.Param_OPEN_LOCK);
                break;
            case R.id.tv_dev_run_mode:
                showSelectRunModeMenu();
                break;
            case R.id.tv_refresh:
                refreshDetails();
                break;
            case R.id.tv_sys_time:
                new HitiInputStrDailog(LiherGGCYActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置蓝牙瓶系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherGGCYActivity.this);
                break;
            case R.id.tv_sudu1:
                List<JSONObject> jat = new ArrayList<>();
                JSONObject jo0 = new JSONObject();
                jo0.put("MyName", "1-最慢");
                jo0.put("MyObject", "1");
                jat.add(jo0);
                JSONObject jo1 = new JSONObject();
                jo1.put("MyName", "2");
                jo1.put("MyObject", "2");
                jat.add(jo1);
                JSONObject jo2 = new JSONObject();
                jo2.put("MyName", "3");
                jo2.put("MyObject", "3");
                jat.add(jo2);
                JSONObject jo3 = new JSONObject();
                jo3.put("MyName", "4");
                jo3.put("MyObject", "4");
                jat.add(jo3);
                JSONObject jo4 = new JSONObject();
                jo4.put("MyName", "5-最快");
                jo4.put("MyObject", "5");
                jat.add(jo4);
                new SelecterObjectDailog(LiherGGCYActivity.this, "手动模式电机1转速", jat, Constant.Dialog_GGCY_Sudu1, LiherGGCYActivity.this);

                break;
            case R.id.tv_sudu2:
                List<JSONObject> jat2 = new ArrayList<>();
                JSONObject jo20 = new JSONObject();
                jo20.put("MyName", "1-最慢");
                jo20.put("MyObject", "1");
                jat2.add(jo20);
                JSONObject jo21 = new JSONObject();
                jo21.put("MyName", "2");
                jo21.put("MyObject", "2");
                jat2.add(jo21);
                JSONObject jo22 = new JSONObject();
                jo22.put("MyName", "3");
                jo22.put("MyObject", "3");
                jat2.add(jo22);
                JSONObject jo23 = new JSONObject();
                jo23.put("MyName", "4");
                jo23.put("MyObject", "4");
                jat2.add(jo23);
                JSONObject jo24 = new JSONObject();
                jo24.put("MyName", "5-最快");
                jo24.put("MyObject", "5");
                jat2.add(jo24);
                new SelecterObjectDailog(LiherGGCYActivity.this, "手动模式电机2转速", jat2, Constant.Dialog_GGCY_Sudu2, LiherGGCYActivity.this);

                break;
            case R.id.send:
                if (StringUtils.isEmpty(sendData.getText().toString())) {
                    showMsg(2,"请先输入指令内容");
                    return;
                }
                sendLHBleData(sendData.getText().toString().trim(), Constant.PARAM_LIHER_ORDER);
                sendData.setText("");
                break;
        }
    }

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.Dailog_GGCY_Bottle1Ratio:
                String strTarget = (String) target;
                if (StringUtils.isInt(strTarget)) {
                    int targetValue = Integer.parseInt(strTarget);

                    if (targetValue > 10000000 || targetValue < 100) {
                         showMsg(2,"请输入正确采样比例，100~10000000的整数");
                    } else {
                        sendLHBleData("Write scale1=" + targetValue, Constant.Param_Write_GGCY_Bottle1Ratio);

                    }
                } else {
                      showMsg(2,"请输入正确采样比例，100~10000000的整数");
                }
                break;
            case Constant.Dailog_GGCY_Bottle2Ratio:
                String strBottle2Ratio = (String) target;
                if (StringUtils.isInt(strBottle2Ratio)) {
                    int bottle2Ratio = Integer.parseInt(strBottle2Ratio);

                    if (bottle2Ratio > 10000000 || bottle2Ratio < 100) {
                        showMsg(2, "请输入正确采样比例，100~10000000的整数");
                    } else {
                        sendLHBleData("Write scale2=" + bottle2Ratio, Constant.Param_Write_GGCY_Bottle2Ratio);

                    }
                } else {
                    showMsg(2, "请输入正确采样比例，100~10000000的整数");

                }
                break;
            case Constant.Dailog_GGCY_MinFlow:
                String strFlow = (String) target;
                if (StringUtils.isDouble(strFlow)) {
                    double flowValue = Double.parseDouble(strFlow);

                    if (flowValue > 600 || flowValue < 0) {
                        showMsg(2, "请输入正确触发流量，0~600");
                    } else {
                        sendLHBleData("Write mintri=" + DoubleUtils.round(flowValue, 2), Constant.Param_Write_GGCY_MinFlow);

                    }
                } else {
                    showMsg(2, "请输入正确触发流量，0~600");

                }
                break;
            case Constant.Dailog_GGCY_MinSampleVol:
                String strMinSampleVol = (String) target;
                if (StringUtils.isInt(strMinSampleVol)) {
                    int minSampleVol = Integer.parseInt(strMinSampleVol);

                    if (minSampleVol > 5000 || minSampleVol < 10) {
                        showMsg(2, "请输入正确最小采样量，10~500的整数");
                    } else {
                        sendLHBleData("Write minsam=" + minSampleVol, Constant.Param_Write_GGCY_MinSampleVol);

                    }
                } else {
                    showMsg(2, "请输入正确最小采样量，10~500的整数");

                }
                break;
            case Constant.Dialog_GGCY_SampleMaxTime:
                String strSampleMaxTime = (String) target;
                if (StringUtils.isInt(strSampleMaxTime)) {
                    int sampleMaxTime = Integer.parseInt(strSampleMaxTime);

                    if (sampleMaxTime > 99 || sampleMaxTime < 1) {
                        showMsg(2, "请输入正确超时时长小时数，1~99的整数");
                    } else {
                        sendLHBleData("Write samtime=" + sampleMaxTime, Constant.Param_Write_GGCY_MaxSampleTime);

                    }
                } else {
                    showMsg(2, "请输入正确超时时长小时数，1~99的整数");

                }
                break;
            case Constant.RequestCode_Scan_Add_SampleNo:
                showMsg(2, "请扫描录入样品编号");
                startActivityForResult(new Intent(LiherGGCYActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_Add_SampleNo);
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
                String strMac = (String) target;

                sendLHBleData("Write BLEaddr=" + strMac, Constant.Param_Write_Box_Mac_Address);
                break;
            case Constant.HitiStrTargetDialog_GGCY_BottleClear:
                sendLHBleData("lc", Constant.Param_Write_GGCY_Clear_Bottles);
                break;
            case Constant.HitiStrTargetDialog_GGCY_DevReset:
                sendLHBleData("lw", Constant.Param_DEV_Reset);
                break;
        }
    }

    @Override
    public void onItemClick3(View v, int position, Object object, int type) {
        switch (type) {
            case Constant.Dialog_GGCY_Sudu1:
                if (position>=0&&position<=4){
                    sendLHBleData("Write Sudu1="+(position+1),Constant.Param_GGCY_Sudu1);
                }else {
                    showMsg(2,"未识别的转速选项"+position);
                }

                break;
            case Constant.Dialog_GGCY_Sudu2:
                if (position>=0&&position<=4){
                    sendLHBleData("Write Sudu2="+(position+1),Constant.Param_GGCY_Sudu2);
                }else {
                    showMsg(2,"未识别的转速选项"+position);
                }

                break;
        }
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
            }else if (arg2==Constant.Return_Ble_SendFail){
                SpannableStringBuilder builder = new SpannableStringBuilder(msgContext);
                //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
                builder.setSpan(colorSpan, 0, msgContext.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                if (loadingDialog != null) loadingDialog.dismiss();
                showMsg(1,msgContext);
            }
            System.out.println("SendString:" + msgContext);
        } else if (arg1 == 1) {//接收到的消息
            String msgContext = (String) obj;
            SpannableStringBuilder builder = new SpannableStringBuilder("接收:" + msgContext);
            //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(139, 0, 255));
            builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView tView = new TextView(context);
            tView.setText(builder);
            switch (arg2) {
                case Constant.RETURN_LIHER_OREDER:
                    llOrders.addView(tView);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                    break;
                case Constant.Return_Ble_ReceiveFalse:
                    llOrders.addView(tView);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                    if (loadingDialog != null) loadingDialog.dismiss();
                    showMsg(1,msgContext);
                    break;
                case Constant.Return_BLE_Continue:
                    llOrders.addView(tView);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                    break;
                case Constant.Return_BLE_False:
                    // 动态判断消息类型，包含失败则为1，其他为2
                    showMsg(msgContext.contains("失败") ? 1 : 2, msgContext);
                    if (loadingDialog != null) loadingDialog.dismiss();
                    break;
                case Constant.Return_Read_Detail:
                    //220509#0#0.0#2.6#002205100000#0#0#100000#1000000#0.0#0.0#0#24
                    try {
                        String[] details = msgContext.split("#", -1);
                        if (mGGCYBox == null) mGGCYBox = new GGCYBox();
                        if (details.length >= 17) {
                            mGGCYBox.setVersion(details[0]);
                            mGGCYBox.setDevStatus(Integer.parseInt(details[1]));
                            mGGCYBox.setDevEV(details[2]);
                            mGGCYBox.setBoxTemp(Double.parseDouble(details[3]));
                            if (!StringUtils.isEmpty(details[4])&&details[4].length()==12)mGGCYBox.setBlueMac(StringUtils.getNewBlueToothMacAddress(details[4]));
                            mGGCYBox.setBottle1Vol(details[5]);
                            mGGCYBox.setBottle1Ratio(Integer.parseInt(details[6]));
                            mGGCYBox.setBottle2Vol(details[7]);
                            mGGCYBox.setBottle2Ratio(Integer.parseInt(details[8]));
                            mGGCYBox.setCurrentFlow(Double.parseDouble(details[9]));
                            mGGCYBox.setMinFlow(Double.parseDouble(details[10]));
                            mGGCYBox.setMinSampleVol(Integer.parseInt(details[11]));
                            mGGCYBox.setMaxSampleTime(Integer.parseInt(details[12]));
                            mGGCYBox.setIccID(details[13]);
                            mGGCYBox.setRunMode(Integer.parseInt(details[14]));
                            mGGCYBox.setErrCode(Integer.parseInt(details[15]));
                            mGGCYBox.setSampleStartTime(details[16]);
                            if (details.length>=21){
                                mGGCYBox.setWashTime(Integer.parseInt(details[17]));
                                mGGCYBox.setSampleEndTime(details[18]);
                                mGGCYBox.setSpaceFlow(Double.parseDouble(details[19]));
                                mGGCYBox.setTotalFlow(Double.parseDouble(details[20]));
                            }
                            if (details.length>=24){
                                mGGCYBox.setSudu1(StringUtils.isInt(details[21])?Integer.parseInt(details[21]):-1);
                                mGGCYBox.setSudu2(StringUtils.isInt(details[22])?Integer.parseInt(details[22]):-1);
                                mGGCYBox.setSysTime(details[23]);
                            }
                        }
                        setGGCYBoxData();
                        sendLHBleData("Read Subaddr", Constant.Param_Read_Subaddr);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    break;
                case Constant.Return_Read_Subaddr://1-736F6C430000-210930235959-0000000000000000

                    if (loadingDialog != null) loadingDialog.dismiss();
                    String[] subaddrs = msgContext.split("-", -1);
                    if (subaddrs != null && subaddrs.length >= 4) {
                        String sampleNo="";
                        sampleNo=StringUtils.getTargetSpField(subaddrs[3],"_");
                        if (mGGCYBox==null)mGGCYBox=new GGCYBox();
                        switch (Integer.parseInt(subaddrs[0])){
                            case 1:
                                mGGCYBox.setSampleNo1(sampleNo);
                                mGGCYBox.setSampleTime1(subaddrs[2]);
                                tvBottle1SampleNo.setText(sampleNo);
                                break;
                            case 2:
                                mGGCYBox.setSampleNo2(sampleNo);
                                mGGCYBox.setSampleTime2(subaddrs[2]);
                                tvBottle2SampleNo.setText(sampleNo);
                                break;
                        }
                    }
                    break;
                case Constant.Return_Write_Box_Mac_Address:
                    if (msgContext != null && msgContext.contains("Write BLEaddr=")) {
                        showMsg(0, "设置成功,正在重启...");

                    } else {
                        showMsg(1, "设置蓝牙地址失败,\n" + msgContext);

                    }
                    break;
                case Constant.Return_Write_GGCY_Bottle1Ratio:
                    if (msgContext != null && msgContext.contains("Write scale1=")) {
                        showMsg(0, "提交成功...");
                        int bottle1Ratio = Integer.parseInt(msgContext.substring(13).trim());
                        if (mGGCYBox != null) mGGCYBox.setBottle1Ratio(bottle1Ratio);
                        tvBottle1Ratio.setText(bottle1Ratio + "");
                    } else {
                        showMsg(1, "设置采样比例失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_GGCY_Bottle2Ratio:
                    if (msgContext != null && msgContext.contains("Write scale2=")) {
                        showMsg(0, "提交成功...");
                        int bottle2Ratio = Integer.parseInt(msgContext.substring(13).trim());
                        if (mGGCYBox != null) mGGCYBox.setBottle2Ratio(bottle2Ratio);
                        tvBottle2Ratio.setText(bottle2Ratio + "");
                    } else {
                        showMsg(1, "设置采样比例失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_GGCY_MinFlow:
                    if (msgContext != null && msgContext.contains("Write mintri=")) {
                        showMsg(0, "提交成功...");
                        double minFlow = Double.parseDouble(msgContext.substring(13).trim());
                        if (mGGCYBox != null) mGGCYBox.setMinFlow(minFlow);
                        tvMinFlow.setText(DoubleUtils.round(minFlow, 2) + "L/s");
                    } else {
                        showMsg(1, "设置最小触发流量失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_GGCY_MinSampleVol:
                    if (msgContext != null && msgContext.contains("Write minsam=")) {
                        showMsg(0, "提交成功...");
                        int sampleVol = Integer.parseInt(msgContext.substring(13).trim());
                        if (mGGCYBox != null) mGGCYBox.setMinSampleVol(sampleVol);
                        tvMinSampleVol.setText(sampleVol + "mL");
                    } else {
                        showMsg(1, "设置最小采样量失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_GGCY_SampleStart:
                    if (msgContext != null && msgContext.contains("Write Mode=1")) {
                        showMsg(0, "开启采样成功...");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "开启采样失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_GGCY_SampleStop:
                    if (msgContext != null && msgContext.contains("Write Mode=0")) {
                        showMsg(0, "停止采样成功...");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "停止采样失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_OPEN_LOCK:
                    if (msgContext != null && msgContext.contains("ll")) {
                        showMsg(0, "开锁指令下发成功...");

                    } else {
                        showMsg(1, "开锁指令下发失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_GGCY_Sudu1:
                    if (msgContext != null && msgContext.startsWith("Write Sudu1")) {
                        showMsg(0,"下发成功...");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "设置转速1下发失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_GGCY_Sudu2:
                    if (msgContext != null && msgContext.startsWith("Write Sudu2")) {
                        showMsg(0,"下发成功...");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "设置转速1下发失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_GGCY_Clear_Bottles:
                    if (msgContext != null && msgContext.contains("lc")) {
                        showMsg(0, "清空成功...");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "换瓶清空失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_DEV_Reset:
                    if (msgContext != null && msgContext.contains("lw")) {
                        showMsg(0, "复位成功...");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "润洗复位失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_GGCY_MaxSampleTime:
                    if (msgContext != null && msgContext.contains("Write samtime=")) {
                        showMsg(0, "提交成功...");
                        int maxSampleTime = Integer.parseInt(msgContext.substring(14).trim());
                        if (mGGCYBox != null) mGGCYBox.setMaxSampleTime(maxSampleTime);
                        tvSampleMaxTime.setText(maxSampleTime + "小时");
                    } else {
                        showMsg(1, "设置超时时长失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_Subaddr:
                    if (msgContext != null && msgContext.startsWith("Write Subaddr=")) {
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                sendLHBleData("Read Subaddr", Constant.Param_Read_Subaddr);
                            }
                        });
                    }
                    break;
                case Constant.Return_Wirte_RunMode:
                    if (msgContext != null && msgContext.startsWith("Write runmode=")) {
                        showMsg(0, "运行模式设置成功...");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    }else {
                        showMsg(1, "模式设置失败,\n" + msgContext);
                    }
                    break;

                case Constant.Return_Wirte_SysTime:
                    if (msgContext != null && msgContext.contains("Write SysTime=")) {
                        if (mGGCYBox != null)
                            mGGCYBox.setSysTime(msgContext.substring(14).trim());
                        tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" +  mGGCYBox.getSysTime(), "yyyyMMddHHmmss")));
                    } else {
                        showMsg(1, "设置系统校时失败,\n" + msgContext);
                    }
                    break;
            }


        } else if (arg1 == 2) {//日志信息
            String msgContext = (String) obj;
            if (arg2==Constant.Return_Ble_ConnectStop){
                SpannableStringBuilder builder = new SpannableStringBuilder( msgContext);
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            }else if (arg2==Constant.Return_Ble_ConnectState){
                SpannableStringBuilder builder = new SpannableStringBuilder( msgContext);
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
                if (loadingDialog != null) loadingDialog.dismiss();
                showMsg(1,msgContext);
            }else {
                SpannableStringBuilder builder = new SpannableStringBuilder( msgContext);
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

    private void setGGCYBoxData() {
        if (mGGCYBox == null) return;
        tvWorkStatus.setText(StringUtils.getGGCYDevStatus(mGGCYBox.getDevStatus()));
        tvDevVersion.setText(mGGCYBox.getVersion());
        tvMacAddress.setText(mGGCYBox.getBlueMac());
        tvDevEV.setText(mGGCYBox.getDevEV() + "V");
        tvBottle1Vol.setText(mGGCYBox.getBottle1Vol() + "mL");
        tvBottle1Ratio.setText(mGGCYBox.getBottle1Ratio() + "");
        tvBottle2Vol.setText(mGGCYBox.getBottle2Vol() + "mL");
        tvBottle2Ratio.setText(mGGCYBox.getBottle2Ratio() + "");
        tvCurrentFlow.setText(DoubleUtils.round(mGGCYBox.getCurrentFlow(), 3) + "L/s");
        tvBoxTemperature.setText(DoubleUtils.round(mGGCYBox.getBoxTemp(), 1) + "℃");
        tvMinFlow.setText(DoubleUtils.round(mGGCYBox.getMinFlow(), 2) + "L/s");
        tvMinSampleVol.setText(mGGCYBox.getMinSampleVol() + "mL");
        tvSampleMaxTime.setText(mGGCYBox.getMaxSampleTime()+"小时");
        tvDevRunMode.setText(StringUtils.getGGCYDevRunMode(mGGCYBox.getRunMode()));
        tvDevIccID.setText(mGGCYBox.getIccID());
        tvErrorCode.setText(StringUtils.getGGCYErrorCode(mGGCYBox.getErrCode()));
        if (mGGCYBox.getSampleStartTime()!=null&&mGGCYBox.getSampleStartTime().length()==12&&mGGCYBox.getSampleStartTime().startsWith("2")){
            if (mGGCYBox.getSampleEndTime()==null){
                tvSampleTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" +  mGGCYBox.getSampleStartTime(), "yyyyMMddHHmmss")));
            }else if (mGGCYBox.getSampleEndTime().length()==12&&mGGCYBox.getSampleEndTime().startsWith("2")){
                tvSampleTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" +  mGGCYBox.getSampleStartTime(), "yyyyMMddHHmmss"))
                        +"\n-"+DateUtil.long2time4(DateUtil.format2miao("20" +  mGGCYBox.getSampleEndTime(), "yyyyMMddHHmmss")));
            }else {
                tvSampleTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" +  mGGCYBox.getSampleStartTime(), "yyyyMMddHHmmss"))+"-...");
            }
        }
        tvSpaceFlow.setText(DoubleUtils.round(mGGCYBox.getSpaceFlow(),1)+"m³");
        tvTotalFlow.setText(DoubleUtils.round(mGGCYBox.getTotalFlow(),1)+"m³");
        tvSudu1.setText(mGGCYBox.getSudu1()>0?(mGGCYBox.getSudu1()+""):"");
        tvSudu2.setText(mGGCYBox.getSudu2()>0?(mGGCYBox.getSudu2()+""):"");
        if (!StringUtils.isEmpty(mGGCYBox.getSysTime()) && mGGCYBox.getSysTime().startsWith("2")) {

            tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" + mGGCYBox.getSysTime(), "yyyyMMddHHmmss")));
            if (Math.abs(System.currentTimeMillis() - DateUtil.format2miao("20" + mGGCYBox.getSysTime(), "yyyyMMddHHmmss")) > 1 * 60 * 1000){
                new HitiInputStrDailog(LiherGGCYActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherGGCYActivity.this);
            }
        }else {
            new HitiInputStrDailog(LiherGGCYActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherGGCYActivity.this);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RequestCode_Bin_Version:
                    if (data==null){
                        if (mGGCYBox!=null&&!StringUtils.isEmpty(mGGCYBox.getVersion())&&mGGCYBox.getVersion().length()==6){
                            ArrayList<String> macList=new ArrayList<>();
                            macList.add(mDeviceAddress);
                            startActivity(BlueOTAActivity.getIntent(this,macList,mGGCYBox.getVersion()));
                            finish();

                        }else {
                            showMsg(2, "未获取到当前瓶版本信息，请稍后重试");
                            sendLHBleData("Read Detail", Constant.Param_Read_Detail);
                        }

                    }

                    break;
                case Constant.RequestCode_Scan_Box_MAC_Address:
                    String boxQcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(boxQcode)) {
                        showMsg(2, "未读取到二维码信息");
                    } else if (boxQcode.length() != 12) {
                        showMsg(2, "二维码，非力合蓝牙标准码");
                    } else if (boxQcode.startsWith("00")) {
                        String macAds = boxQcode.trim();
                        new HitiStringDailog(LiherGGCYActivity.this, Constant.Dailog_BLE_MACAddress, macAds, "Mac地址确认", "确认设置Mac地址为：" + StringUtils.getNewBlueToothMacAddress(macAds) + "?\n蓝牙将重启后生效", LiherGGCYActivity.this);
                    } else {
                        showMsg(2, boxQcode + "二维码，非力合灌溉采样箱编码规则00");
                    }
                    break;
                case Constant.RequestCode_Scan_Sample_No1:
                    String sampleNo1 = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (!StringUtils.isEmpty(sampleNo1)) {
                        if (sampleNo1.length() > 2 && sampleNo1.length() <= 16) {
                            String strMsg = "01000000000000-" + StringUtils.addCharForNum(sampleNo1.trim(), 16, '_');
                            sendLHBleData("Write Subaddr=" + strMsg, Constant.Param_Write_Subaddr);
                            return;
                        }
                    }
                    showMsg(2, "请输入正确的样品编号");
                    break;
                case Constant.RequestCode_Scan_Sample_No2:
                    String sampleNo2 = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (!StringUtils.isEmpty(sampleNo2)) {
                        if (sampleNo2.length() > 2 && sampleNo2.length() <= 16) {
                            String strMsg = "02000000000000-" + StringUtils.addCharForNum(sampleNo2.trim(), 16, '_');
                            sendLHBleData("Write Subaddr=" + strMsg, Constant.Param_Write_Subaddr);
                            return;
                        }
                    }
                    showMsg(2, "请输入正确的样品编号");
                    break;
            }
        }
    }

}
