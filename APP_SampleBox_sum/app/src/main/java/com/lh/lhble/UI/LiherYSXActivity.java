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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.Entity.SampleBottle;
import com.lh.lhble.Entity.TransportBox;
import com.lh.lhble.R;
import com.lh.lhble.Utils.DateUtil;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.UI.View.SelecterObjectDailog;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.adapter.YSXSampleBottleAdapter;
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
 * 样品运输箱
 */
public class LiherYSXActivity extends BaseActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherYSXActivity.class.getSimpleName();

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
    private LinearLayout llOrderMode, llUIMode, llBoxReport;
    private TextView tvDevEV, tvRiss, tvRefresh;
    private TextView tvReadData, tvDevVersion, tvSysTime, tvWorkStatus, tvBoxTemp, tvMacAddress, tvDevIccID, tvBoxReport, tvWorkMode, tvSetMacAddress, tvTranSportNum,
            tvSetTranSportNum, tvAddBottle, tvClearBottle, tvDevLocation;

    private TextView tvDevOpen;

    private ScrollView sclOrders;
    private LinearLayout llOrders;
    private EditText sendData;
    private TextView send;

    private SwipeRefreshLayout srlSampleBottles;
    private ListView lvSampleBottles;
    private YSXSampleBottleAdapter mAdapter;
    private LoadingDialog loadingDialog = null;
    private TransportBox box;
    private List<SampleBottle> sampleBottleList;
    private SampleBottle currentSampleBottle;
    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;

    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private int currentSendType, currentReceiveType;

    private String tempBottleMac;
    private HitiInputStrDailog sampleNoDialog;


    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_ysx;
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
        if (HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {
            tvSetMacAddress.setVisibility(View.VISIBLE);
        } else {
            tvSetMacAddress.setVisibility(View.INVISIBLE);
        }
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
        showMsg(2, "正在重新加载运输箱信息...");
        sendLHBleData("Read Detail", Constant.Param_Read_Detail);
    }

    private void refreshBottleList() {
        showMsg(2, "正在加载瓶位信息...");
        if (sampleBottleList == null) sampleBottleList = new ArrayList<>();
        sampleBottleList.clear();
        sendLHBleData("Read Subaddr", Constant.Param_Read_Subaddr);
    }


    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherYSXActivity.class);
        intent.putExtra(LiherYSXActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherYSXActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherYSXActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();
        llOrderMode = findViewById(R.id.ll_order_mode);
        llUIMode = findViewById(R.id.ll_ui_mode);
        tvRefresh = findViewById(R.id.tv_refresh);

        tvDevVersion = findViewById(R.id.tv_dev_version);
        tvSysTime = findViewById(R.id.tv_sys_time);
        tvReadData = findViewById(R.id.tv_read_data);
        tvDevEV = findViewById(R.id.tv_dev_ev);
        tvRiss = findViewById(R.id.tv_dev_riss);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvDevIccID = findViewById(R.id.tv_dev_icc_id);
        llBoxReport = findViewById(R.id.ll_box_report);
        tvBoxReport = findViewById(R.id.tv_box_report);
        tvWorkMode = findViewById(R.id.tv_work_mode);
        tvSetMacAddress = findViewById(R.id.tv_set_mac_address);
        tvBoxTemp = findViewById(R.id.tv_box_temp);
        tvDevLocation = findViewById(R.id.tv_dev_location);
        tvWorkStatus = findViewById(R.id.tv_work_status);
        tvTranSportNum = findViewById(R.id.tv_transport_no);
        tvSetTranSportNum = findViewById(R.id.tv_set_transport_no);
        tvAddBottle = findViewById(R.id.tv_add_bottle);
        tvClearBottle = findViewById(R.id.tv_clear_bottle);
        tvDevOpen = findViewById(R.id.tv_dev_open);
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        sendData = findViewById(R.id.sendData);
        send = findViewById(R.id.send);
        srlSampleBottles = findViewById(R.id.srl_sample_bottles);
        lvSampleBottles = findViewById(R.id.lv_sample_bottles);

        sampleBottleList = new ArrayList<>();
        mAdapter = new YSXSampleBottleAdapter(LiherYSXActivity.this);
        lvSampleBottles.setAdapter(mAdapter);
        tvRefresh.setOnClickListener(LiherYSXActivity.this);
        tvSysTime.setOnClickListener(LiherYSXActivity.this);
        tvSetMacAddress.setOnClickListener(LiherYSXActivity.this);
        tvReadData.setOnClickListener(LiherYSXActivity.this);
        tvSetTranSportNum.setOnClickListener(this);
        tvClearBottle.setOnClickListener(this);
        tvAddBottle.setOnClickListener(this);
        tvDevOpen.setOnClickListener(LiherYSXActivity.this);
        send.setOnClickListener(LiherYSXActivity.this);
        tvBoxReport.setOnClickListener(LiherYSXActivity.this);
        tvWorkMode.setOnClickListener(LiherYSXActivity.this);
        box = new TransportBox();
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

            showMsg(2, "设备蓝牙连接已中断，请返回扫描连接后重试");
            finish();
            return;
        }
        if (LiherYSXActivity.this.isFinishing()) {
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
                    addLogText("发送失败！", 0, Constant.Return_BLE_False);
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
                addLogText("蓝牙已断开", Color.BLACK, 0);
                //finish();
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
                        case Constant.Param_Write_Subaddr:
                            currentReceiveType = Constant.Return_Write_Subaddr;
                            break;
                        case Constant.Param_OPEN_LOCK:
                            currentReceiveType = Constant.Return_OPEN_LOCK;
                            break;
                        case Constant.Param_Wirte_SysTime:
                            currentReceiveType = Constant.Return_Wirte_SysTime;
                            break;
                        case Constant.Param_Write_Box_Mac_Address:
                            currentReceiveType = Constant.Return_Write_Box_Mac_Address;
                            break;
                        case Constant.Param_Write_Update_Start:
                            currentReceiveType = Constant.Return_Write_Update_Start;
                            break;
                        case Constant.Param_Write_Update_End:
                            currentReceiveType = Constant.Return_Write_Update_End;
                            break;
                        case Constant.Param_Write_YSX_Clear_Bottles:
                            currentReceiveType = Constant.Return_Write_YSX_Clear_Bottles;
                            break;
                        case Constant.Param_Write_YSX_Transport_NO:
                            currentReceiveType = Constant.Return_Write_YSX_Transport_NO;
                            break;
                        case Constant.Param_Read_YSX_DATA:
                            currentReceiveType = Constant.Return_Read_YSX_DATA;
                            break;
                        case Constant.Param_Write_YSX_WorkMode:
                            currentReceiveType = Constant.Return_Write_YSX_WorkMode;
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
            if (!StringUtils.isEmpty(stateMsg)) {
                addLogText(HJBleApplication.getAppContext().getString(R.string.ble_close) + "," + stateMsg, Color.BLACK, 0);
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
            case R.id.tv_set_transport_no:
                showMsg(2, "请录入12位运输单号");
                startActivityForResult(new Intent(LiherYSXActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_YSX_Transport_No);
                break;
            case R.id.tv_set_mac_address:
                startActivityForResult(new Intent(LiherYSXActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_Box_MAC_Address);
                break;
            case R.id.tv_refresh:
                refreshDetails();
                break;
            case R.id.tv_read_data:
                if (box.getUpdateTime() <= 0) {
                    showMsg(2, "请先开启运输数据上报功能，再读取运输箱位置~");
                    return;
                }
                if (llOrderMode.getVisibility() != View.VISIBLE)
                    llOrderMode.setVisibility(View.VISIBLE);
                llOrders.removeAllViews();
                showMsg(2, "正在定位当前经纬度,请在上方消息窗口查看~");
                sendLHBleData("Read GPS", Constant.Param_Read_YSX_DATA);
                break;
            case R.id.tv_sys_time:
                new HitiInputStrDailog(LiherYSXActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置蓝牙瓶系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherYSXActivity.this);
                break;
            case R.id.tv_add_bottle:
                List<JSONObject> jat = new ArrayList<>();
                JSONObject jo0 = new JSONObject();
                jo0.put("MyName", "普通瓶");
                jo0.put("MyObject", 0);
                jat.add(jo0);
                JSONObject jo1 = new JSONObject();
                jo1.put("MyName", "蓝牙瓶");
                jo1.put("MyObject", 1);
                jat.add(jo1);
                new SelecterObjectDailog(LiherYSXActivity.this, "选择装入的样品瓶类型", jat, Constant.Dialog_YSX_SampleBottle_Type, LiherYSXActivity.this);
                break;
            case R.id.tv_clear_bottle:
                new HitiStringDailog(LiherYSXActivity.this, Constant.HitiStrTargetDialog_YSX_Clear_Bottles, "", "清空确认", "确认清空运输箱内所有样品瓶的信息？", LiherYSXActivity.this);
                break;
            case R.id.tv_box_report:
                showMsg(2, "设置0为关闭数据上报");
                new HitiInputStrDailog(LiherYSXActivity.this, Constant.Dialog_YSX_Report_Time, "运输箱数据上报间隔时长", "上报间隔(分钟):", box.getUpdateTime() + "", LiherYSXActivity.this);

                break;
            case R.id.tv_work_mode:
                if (box != null && box.getWorkMode() == 1) {
                    showMsg(2,"工作模式：正常采集GPS和温度记录并上传数据（装瓶后运输箱处于工作模式）");
                    //new HitiStringDailog(LiherYSXActivity.this, Constant.Dialog_YSX_WorkMode, 0, "手动切换工作模式", "将运输箱运行模式改成节能模式（不采集GPS和温度，不网络拨号，不上传数据）？(卸完瓶后会自动为节能模式！)", LiherYSXActivity.this);
                } else if (box != null && box.getWorkMode() == 0) {
                    showMsg(2,"节能模式：不采集GPS和温度，不联网上传数据（卸完全部瓶后，运输箱处于节能模式）");
                    //new HitiStringDailog(LiherYSXActivity.this, Constant.Dialog_YSX_WorkMode, 1, "手动切换工作模式", "将运输箱运行模式改成工作模式？(装瓶后运输箱会自动为工作模式！)", LiherYSXActivity.this);
                }

                break;

            case R.id.tv_dev_open:
                sendLHBleData("ll", Constant.Param_OPEN_LOCK);
                break;

            case R.id.send:
                if (StringUtils.isEmpty(sendData.getText().toString())) {
                    showMsg(2, "请先输入指令内容");
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
            case Constant.Dialog_YSX_WorkMode:
                if (HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")) {
                    int currentMode = (int) target;
                    if (currentMode == 1) {
                        sendLHBleData("Write Start=1", Constant.Param_Write_YSX_WorkMode);
                    } else if (currentMode == 1) {
                        sendLHBleData("Write Start=0", Constant.Param_Write_YSX_WorkMode);
                    }
                }else {
                    showMsg(2, "您的账号无生产维修权限，无法手动切换工作模式");
                }

                break;

            case Constant.Dialog_YSX_Report_Time:
                if (target != null && target instanceof String && StringUtils.isInt((String) target)) {
                    int reportTime = Integer.parseInt((String) target);
                    if (reportTime >= 5) {
                        sendLHBleData("Write reppd=" + reportTime, Constant.Param_Write_Update_Start);
                    } else if (reportTime == 0) {
                        sendLHBleData("Write reppd=0", Constant.Param_Write_Update_End);
                    } else {
                        showMsg(2, reportTime + ",请输入正确的上报间隔分钟数，不小于5的整数！");
                        return;
                    }
                } else {
                    showMsg(2, "请输入正确的上报间隔分钟数，不小于5的整数！");
                    return;
                }
                break;
            case Constant.HitiStrTargetDialog_YSX_Clear_Bottles:
                sendLHBleData("Write reset=1", Constant.Param_Write_YSX_Clear_Bottles);
                break;
            case Constant.Dailog_Input_SampleNo:
                if (target != null && target instanceof String) {
                    String sampleNo = (String) target;
                    int no = 0;
                    for (int n = 0; n < sampleBottleList.size(); n++) {
                        if (StringUtils.isEmpty(sampleBottleList.get(n).getSampleID()) || sampleBottleList.get(n).getSampleID().length() != 16 || "0000000000000000".equals(sampleBottleList.get(n).getSampleID())) {
                            no = 1 + n;
                            break;
                        } else if (sampleBottleList.get(n).getSampleID().equals(StringUtils.addCharForNum(sampleNo.trim(), 16, '_'))) {
                            showMsg(2, "样品" + sampleNo + "已装入本箱，请勿重复装入！");
                            return;
                        } else if (tempBottleMac.length() == 12 && !"000000000000".equals(tempBottleMac) && tempBottleMac.equals(sampleBottleList.get(n).getBlueMac())) {
                            showMsg(2, "蓝牙瓶" + tempBottleMac + "已装入本箱，请勿重复装入！");
                            return;
                        }
                    }
                    if (no == 0) {
                        showMsg(2, "本箱的瓶位已全部装满！");
                        return;
                    }
                    if (sampleNo.length() > 2 && sampleNo.length() <= 16) {
                        String strMsg = StringUtils.formatString(no, 2) + "-" + tempBottleMac + "-" + DateUtil.long2time10(System.currentTimeMillis()).substring(2) + "-" + StringUtils.addCharForNum(sampleNo.trim(), 16, '_');
                        sendLHBleData("Write Subaddr=" + strMsg, Constant.Param_Write_Subaddr);
                    } else {
                        showMsg(2, "请输入正确的样品编号");

                    }
                }
                break;
            case Constant.RequestCode_Scan_Add_SampleNo:
                showMsg(2, "请扫描录入样品编号");
                startActivityForResult(new Intent(LiherYSXActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_Add_SampleNo);
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

        }
    }

    @Override
    public void onItemClick3(View v, int position, Object object, int type) {
        switch (type) {
            case Constant.Dialog_YSX_SampleBottle_Type:
                JSONObject jo = (JSONObject) object;
                int bottleType = jo.getObject("MyObject", Integer.class);
                if (bottleType == 1) {
                    showMsg(2, "请扫描蓝牙瓶盖二维码");
                    startActivityForResult(new Intent(LiherYSXActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_YSXAdd_MAC_Address);

                } else {
                    tempBottleMac = "000000000000";
                    showMsg(2, "请录入瓶身的样品编号");
                    sampleNoDialog = new HitiInputStrDailog(LiherYSXActivity.this, Constant.Dailog_Input_SampleNo, "装箱样品信息", "样品编号", "", LiherYSXActivity.this);

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
     * @param arg1 0-发送 1-接收 2-日志
     * @param arg2 消息类型
     * @param obj  消息内容
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
                sendData.setText(msgContext);
            } else if (arg2 == Constant.Return_BLE_False) {
                showMsg(2, msgContext);
                if (loadingDialog != null) loadingDialog.dismiss();
            }

            System.out.println("SendString:" + msgContext);
        } else if (arg1 == 1) {//接收到的消息
            String msgContext = (String) obj;
            if (llOrderMode.getVisibility() == View.VISIBLE) {
                SpannableStringBuilder builder = new SpannableStringBuilder("接收:" + msgContext);
                //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(139, 0, 255));
                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tView = new TextView(context);
                tView.setText(builder);
                llOrders.addView(tView);
                sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
            }
            switch (arg2) {
                case Constant.RETURN_LIHER_OREDER:
                   /* SpannableStringBuilder builder = new SpannableStringBuilder("接收:" + msgContext);
                    //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.rgb(139, 0, 255));
                    builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    TextView tView = new TextView(context);
                    tView.setText(builder);
                    llOrders.addView(tView);
                    sclOrders.fullScroll(ScrollView.FOCUS_DOWN);*/
                    break;
                case Constant.Return_BLE_False:
                    showMsg(2, msgContext);
                    if (loadingDialog != null) loadingDialog.dismiss();
                    break;
                case Constant.Return_Read_Detail:
                    //220412#220415153228#3#24.0#0.0#abc12345#0#0#151.123459#28.123457#12
                    //220412#200311192650#3#24.2#0.0##0#1#  0.000000# 0.000000#
                    //00#01#00/13#00060#003600#007#000000#240316#200311201805#0#112303000001#3.1#                 0#001#100#1500#0#5#70#  #0#0	#                    #0#  0#
                    try {
                        String[] details = msgContext.split("#", -1);
                        if (box == null) box = new TransportBox();

                        if (details.length >= 26) {
                            box.setDevStatus(Integer.parseInt(details[5]));
                            box.setVersion(details[7]);
                            box.setSysTime(details[8]);
                            box.setMN(details[10]);
                            box.setDevEV(details[11]);
                            box.setUpdateTime(Integer.parseInt(details[16]));
                            box.setRssi(details[19]);
                            String icc = details[22];
                            if (StringUtils.isEmpty(icc) || "00000000000000000000".equals(icc.trim())) {
                                box.setIccID("");

                            } else {
                                box.setIccID(icc.trim());
                            }
                            box.setBoxTemp(DoubleUtils.div(Integer.parseInt(details[24].trim()), 10d, 1));
                            box.setTransportNum(details[25]);

                            box.setBlueMac(mDeviceAddress);
                            if (details.length >= 27) {
                                box.setWorkMode(Integer.parseInt(details[26]));
                            }
                        }

                        setTransportBoxData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case Constant.Return_Read_Subaddr://1-736F6C430000-210930235959-0000000000000000
                    if (StringUtils.isEmpty(msgContext)) {
                        showMsg(2, "瓶信息返回为空");
                    } else if (msgContext != null && "0-000000000000-000000000000-0000000000000000".equals(msgContext.trim())) {
                        sampleBottleList = new ArrayList<>();
                        mAdapter.setDatas(sampleBottleList);
                    } else {
                        if (sampleBottleList == null) sampleBottleList = new ArrayList<>();
                        String[] subaddrs = msgContext.split("-", -1);
                        if (subaddrs != null && subaddrs.length >= 4) {
                            SampleBottle sampleBottle = new SampleBottle();
                            sampleBottle.setBottleNo(Integer.parseInt(subaddrs[0]));
                            sampleBottle.setBlueMac(subaddrs[1]);
                            sampleBottle.setDateTime(subaddrs[2]);
                            sampleBottle.setSampleID(subaddrs[3]);
                            sampleBottleList.add(sampleBottle);
                            mAdapter.setDatas(sampleBottleList);
                        }
                    }


                    break;

                case Constant.Return_Write_Subaddr:
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
                case Constant.Return_Write_Banned:
                    if (msgContext != null && msgContext.startsWith("Write Banned=")) {
                        refreshDetails();
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
                case Constant.Return_Write_Update_Start:
                    if (msgContext != null && msgContext.startsWith("Write reppd=")) {
                        showMsg(2, "开始运输数据上报中~");
                        if (box != null)
                            box.setUpdateTime(Integer.parseInt(msgContext.substring(12).trim()));
                        tvBoxReport.setText("每" + box.getUpdateTime() + "分钟1次");
                    } else {
                        showMsg(1, "开启运输数据上报失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_Update_End:
                    if (msgContext != null && msgContext.equals("Write reppd=0")) {
                        showMsg(2, "结束运输数据上报");
                        tvBoxReport.setText("");
                    } else {
                        showMsg(1, "关闭运输数据上报失败,\n" + msgContext);
                    }
                    break;
                case Constant.Return_Write_YSX_Clear_Bottles:
                    if (msgContext != null && msgContext.equals("Write reset=1")) {
                        showMsg(2, "已清空运输箱样品瓶");
                        sampleBottleList = new ArrayList<>();
                        mAdapter.setDatas(sampleBottleList);
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    }
                    break;
                case Constant.Return_Write_YSX_Transport_NO:
                    if (msgContext != null && msgContext.startsWith("Write yundan=")) {
                        showMsg(0, "写入运输单号成功");
                        if (msgContext.length() == 25) {
                            tvTranSportNum.setText(msgContext.substring(13, 25));
                        } else {
                            RxTimerUtil.cancel();
                            RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                                @Override
                                public void doNext(long number) {
                                    refreshDetails();
                                }
                            });

                        }
                    }
                    break;
                case Constant.Return_Read_YSX_DATA:
                    String[] details = msgContext.split("/", -1);
                    if (details != null && details.length >= 3) {
                        analysisMsg(2, currentReceiveType, "当前坐标经度：" + details[0].substring(0, details[0].length() - 1).trim() + "\t\t纬度：" + details[1].substring(0, details[1].length() - 1).trim());
                        tvDevLocation.setText(details[0].substring(0, details[0].length() - 1).trim() + "," + details[1].substring(0, details[1].length() - 1).trim());
                    }

                    break;
                case Constant.Return_Write_YSX_WorkMode:
                    if (msgContext != null && msgContext.contains("Write start=")) {
                        showMsg(0, "工作模式切换成功");
                        RxTimerUtil.cancel();
                        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                            @Override
                            public void doNext(long number) {
                                refreshDetails();
                            }
                        });
                    } else {
                        showMsg(1, "工作模式切换失败，" + msgContext);
                    }

                    break;

                case Constant.Return_Wirte_SysTime:
                    if (msgContext != null && msgContext.contains("Write SysTime=")) {
                        if (box != null)
                            box.setSysTime(msgContext.substring(14).trim());
                        tvSysTime.setText("20" + msgContext.substring(14, 24).trim());
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

            }


        } else if (arg1 == 2) {//日志信息
            String msgContext = (String) obj;
            SpannableStringBuilder builder = new SpannableStringBuilder("日志:" + msgContext);
            //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLACK);
            builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView tView = new TextView(context);
            tView.setText(builder);
            llOrders.addView(tView);
            sclOrders.fullScroll(ScrollView.FOCUS_DOWN);
        }
    }

    private void setTransportBoxData() {
        if (box == null) return;
        tvMacAddress.setText(box.getBlueMac());
        tvDevEV.setText(box.getDevEV() + "v");
        tvRiss.setText(box.getRssi());
        tvDevIccID.setText(StringUtils.isEmpty(box.getIccID()) ? "物联网卡号ICCID值为空" : box.getIccID());
        tvDevVersion.setText(box.getVersion());
        tvWorkStatus.setText(StringUtils.getYSXWorkStatue(box.getDevStatus()));
        tvWorkStatus.setTextColor(getResources().getColor(box.getDevStatus() > 1 ? R.color.color_red : R.color.color_black_333333));
        tvBoxTemp.setText(DoubleUtils.round(box.getBoxTemp(), 1) + "℃");
        tvTranSportNum.setText(box.getTransportNum());
        tvBoxReport.setText(box.getUpdateTime() > 0 ? ("每" + box.getUpdateTime() + "分钟1次") : "");
        if (box.getWorkMode() == 0) {
            tvWorkMode.setText("节能模式");
        } else {
            tvWorkMode.setText("工作模式");
        }
        if (!StringUtils.isEmpty(box.getSysTime()) && box.getSysTime().length() == 12 && box.getSysTime().startsWith("2")) {
            tvSysTime.setText(DateUtil.long2time4(DateUtil.format2miao("20" + box.getSysTime(), "yyyyMMddHHmmss")));
            if (Math.abs(System.currentTimeMillis() - DateUtil.format2miao("20" + box.getSysTime(), "yyyyMMddHHmmss")) > 1 * 60 * 1000) {
                RxTimerUtil.cancel();
                RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                    @Override
                    public void doNext(long number) {
                        new HitiInputStrDailog(LiherYSXActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherYSXActivity.this);
                    }
                });
            } else {
                if (sampleBottleList == null) sampleBottleList = new ArrayList<>();
                sampleBottleList.clear();
                sendLHBleData("Read Subaddr", Constant.Param_Read_Subaddr);
            }
        } else {
            RxTimerUtil.cancel();
            RxTimerUtil.timer(1000L, new RxTimerUtil.IRxNext() {
                @Override
                public void doNext(long number) {
                    new HitiInputStrDailog(LiherYSXActivity.this, Constant.Dailog_BLE_SYSDateTime, "设置系统时间", "当前正确时间：", DateUtils.long2time(System.currentTimeMillis()), LiherYSXActivity.this);
                }
            });
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

                case Constant.RequestCode_Scan_YSXAdd_MAC_Address:
                    String ysxBottleMacQcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(ysxBottleMacQcode)) {
                        showMsg(2, "未读取到二维码信息");
                    } else if (ysxBottleMacQcode.length() != 12) {
                        showMsg(2, "二维码，非力合蓝牙标准码");
                    } else {
                        tempBottleMac = ysxBottleMacQcode;
                        showMsg(2, "请录入瓶身的样品编号");
                        sampleNoDialog = new HitiInputStrDailog(LiherYSXActivity.this, Constant.Dailog_Input_SampleNo, "装瓶样品信息", "样品编号", "", LiherYSXActivity.this);


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
                case Constant.RequestCode_Scan_Box_MAC_Address:
                    String boxQcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(boxQcode)) {
                        showMsg(2, "未读取到二维码信息");
                    } else if (boxQcode.length() != 12) {
                        showMsg(2, "二维码，非力合蓝牙标准码");
                    } else if (boxQcode.startsWith("11")) {
                        String macAds = boxQcode.trim();
                        new HitiStringDailog(LiherYSXActivity.this, Constant.Dailog_BLE_MACAddress, macAds, "Mac地址确认", "确认设置Mac地址为：" + StringUtils.getNewBlueToothMacAddress(macAds) + "?\n蓝牙将重启后生效", LiherYSXActivity.this);
                    } else {
                        showMsg(2, boxQcode + "二维码，非力合运输箱编码规则11");
                    }
                    break;
                case Constant.RequestCode_Scan_YSX_Transport_No:
                    String transportNo = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    if (StringUtils.isEmpty(transportNo)) {
                        showMsg(2, "未读取到运单号信息");
                    } else if (transportNo.length() != 12 || !StringUtils.isLong(transportNo)) {
                        showMsg(2, "请录入12位数字运单号，" + transportNo);
                    } else {
                        sendLHBleData("Write yundan=" + transportNo, Constant.Param_Write_YSX_Transport_NO);
                    }

                    break;
            }
        }
    }

}
