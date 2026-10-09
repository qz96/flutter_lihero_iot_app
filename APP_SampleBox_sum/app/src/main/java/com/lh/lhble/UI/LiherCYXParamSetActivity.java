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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;

import com.alibaba.fastjson.JSON;
import com.lh.lhble.Entity.MRegister;
import com.lh.lhble.Entity.ModBusParam;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.Entity.SerialData;
import com.lh.lhble.R;
import com.lh.lhble.UI.View.HitiInputModBusParamDailog;
import com.lh.lhble.UI.View.HitiInputParamPointDailog;
import com.lh.lhble.UI.View.HitiInputSSXSParamDailog;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.UI.View.JZParamPointInputDailog;
import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommonItemOnClickListener3;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.network.RetrofitUtils;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 采样箱参数校准（蓝牙透传-485串口通信）
 */
public class LiherCYXParamSetActivity extends BaseActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherCYXParamSetActivity.class.getSimpleName();

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
    private String sampleId = "LhRTULLJ";
    private ScrollView sclOrders;
    private LinearLayout llOrderMode, llOrders;
    private LinearLayout llUiMode;
    private TextView tvRefresh;
    private TextView tvParamName;
    private TextView tvJZPHValue,tvJZPHRiss,tvJZPHPoint1,tvJZPHPoint1Riss,tvJZPHPoint2,tvJZPHPoint2Riss,tvJZPHPoint3,tvJZPHPoint3Riss,tvJZPHTemp,
            tvJZORPValue,tvJZORPRiss,tvJZORPPoint1,tvJZORPPoint1Riss,tvJZORPPoint2,tvJZORPPoint2Riss,tvJZORPPoint3,tvJZORPPoint3Riss,tvJZORPTemp,
            tvJZDDLValue,tvJZDDLRiss,tvJZDDLPoint1,tvJZDDLPoint1Riss,tvJZDDLTemp;
    private TextView tvWritePoint,tvStartPoint;
    private LinearLayout llJZPH,llJZORP,llJZDDL;
    private LoadingDialog loadingDialog = null;

    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;

    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private int currentSendType, currentReceiveType;
    private int ModbusFirstAdds=40001;
    private List<ModBusParam> tempParams;
    private int paramModBusAdr;//3-pH 5-ORP 4-电导率
    private ModBusParam phValueParam,phRissParam,phPoint1Param,phPoint2Param,phPoint3Param,
            orpValueParam,orpRissParam,orpPoint1Param,orpPoint2Param,orpPoint3Param,
            ddlValueParam,ddlRissParam,ddlPoint1Param,phTempParam,orpTempParam,ddlTempParam,startPointParam,startTempParam;
    private int tempPointNo;
    private ModBusParam[] pointWrite=new ModBusParam[]{null,null,null};

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_cyx_param_jz;
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
               showSelectJZParamMenu();
            }
        });
    }


    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherCYXParamSetActivity.class);
        intent.putExtra(LiherCYXParamSetActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherCYXParamSetActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherCYXParamSetActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();
        llOrderMode = findViewById(R.id.ll_order_mode);
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        llUiMode = findViewById(R.id.ll_ui_mode);
        tvRefresh = findViewById(R.id.tv_refresh);
        tvJZPHValue=findViewById(R.id.tv_jz_ph_value);
        tvJZPHRiss=findViewById(R.id.tv_jz_ph_riss);
        tvJZPHPoint1=findViewById(R.id.tv_jz_ph_point1);
        tvJZPHPoint1Riss=findViewById(R.id.tv_jz_ph_point1_riss);

        tvJZPHPoint2=findViewById(R.id.tv_jz_ph_point2);
        tvJZPHPoint2Riss=findViewById(R.id.tv_jz_ph_point2_riss);
        tvJZPHPoint3=findViewById(R.id.tv_jz_ph_point3);
        tvJZPHPoint3Riss=findViewById(R.id.tv_jz_ph_point3_riss);
        tvJZPHTemp=findViewById(R.id.tv_jz_ph_temp);

        tvJZORPValue=findViewById(R.id.tv_jz_orp_value);
        tvJZORPRiss=findViewById(R.id.tv_jz_orp_riss);
        tvJZORPPoint1=findViewById(R.id.tv_jz_orp_point1);
        tvJZORPPoint1Riss=findViewById(R.id.tv_jz_orp_point1_riss);
        tvJZORPPoint2=findViewById(R.id.tv_jz_orp_point2);
        tvJZORPPoint2Riss=findViewById(R.id.tv_jz_orp_point2_riss);
        tvJZORPPoint3=findViewById(R.id.tv_jz_orp_point3);
        tvJZORPPoint3Riss=findViewById(R.id.tv_jz_orp_point3_riss);
        tvJZORPTemp=findViewById(R.id.tv_jz_orp_temp);
        tvJZDDLValue=findViewById(R.id.tv_jz_ddl_value);
        tvJZDDLRiss=findViewById(R.id.tv_jz_ddl_riss);
        tvJZDDLPoint1=findViewById(R.id.tv_jz_ddl_point1);
        tvJZDDLPoint1Riss=findViewById(R.id.tv_jz_ddl_point1_riss);
        tvJZDDLTemp=findViewById(R.id.tv_jz_ddl_temp);
        llJZPH=findViewById(R.id.ll_jz_ph);
        llJZORP=findViewById(R.id.ll_jz_orp);
        llJZDDL=findViewById(R.id.ll_jz_ddl);
        tvParamName=findViewById(R.id.tv_jz_param);
        tvWritePoint=findViewById(R.id.tv_write_point);
        tvStartPoint=findViewById(R.id.tv_start_point);

        tvWritePoint.setOnClickListener(this);
        tvStartPoint.setOnClickListener(this);
        tvParamName.setOnClickListener(this);
        tvJZPHPoint1.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZPHPoint2.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZPHPoint3.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZPHPoint1Riss.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZPHPoint2Riss.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZPHPoint3Riss.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZPHTemp.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZORPPoint1.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZORPPoint2.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZORPPoint3.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZORPPoint1Riss.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZORPPoint2Riss.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZORPPoint3Riss.setOnClickListener(LiherCYXParamSetActivity.this);

        tvJZORPTemp.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZDDLPoint1.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZDDLPoint1Riss.setOnClickListener(LiherCYXParamSetActivity.this);
        tvJZDDLTemp.setOnClickListener(LiherCYXParamSetActivity.this);

        tvRefresh.setOnClickListener(LiherCYXParamSetActivity.this);
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
        phValueParam=new ModBusParam(40002,2,false,true,5,"pH值","无量纲",2,0,tvJZPHValue.getId(),0,null);
        phRissParam=new ModBusParam(40008,2,false,true,5,"pH信号值","",2,0,tvJZPHRiss.getId(),0,null);
        orpValueParam=new ModBusParam(40002,2,false,true,5,"ORP值","",2,0,tvJZORPValue.getId(),0,null);
        orpRissParam=new ModBusParam(40008,2,false,true,5,"ORP信号值","",2,0,tvJZORPRiss.getId(),0,null);
        ddlValueParam=new ModBusParam(40002,2,false,true,5,"电导率值","",2,0,tvJZDDLValue.getId(),0,null);
        ddlRissParam=new ModBusParam(40008,2,false,true,5,"电导率信号值","",2,0,tvJZDDLRiss.getId(),0,null);

        List<ModBusParam> phPoint1Children=new ArrayList<>();
        phPoint1Children.add(new ModBusParam(40049,2,true,true,5,"第一点信号值","mV",2,0,tvJZPHPoint1Riss.getId(),0,null));
        phPoint1Children.add(new ModBusParam(40051,1,true,true,0,"第一点pH值","",2,0.01,tvJZPHPoint1.getId(),0,null));
        phPoint1Param=new ModBusParam(40049,3,true,true,0,"第一点设置","",0,0,0,2,phPoint1Children);

        List<ModBusParam> phPoint2Children=new ArrayList<>();
        phPoint2Children.add(new ModBusParam(40052,2,true,true,5,"第二点信号值","mV",2,0,tvJZPHPoint2Riss.getId(),0,null));
        phPoint2Children.add(new ModBusParam(40054,1,true,true,0,"第二点pH值","",2,0.01,tvJZPHPoint2.getId(),0,null));
        phPoint2Param=new ModBusParam(40052,3,true,true,0,"第二点设置","",0,0,0,2,phPoint2Children);

        List<ModBusParam> phPoint3Children=new ArrayList<>();
        phPoint3Children.add(new ModBusParam(40055,2,true,true,5,"第三点信号值","mV",2,0,tvJZPHPoint3Riss.getId(),0,null));
        phPoint3Children.add(new ModBusParam(40057,1,true,true,0,"第三点pH值","",2,0.01,tvJZPHPoint3.getId(),0,null));
        phPoint3Param=new ModBusParam(40055,3,true,true,0,"第三点设置","",0,0,0,2,phPoint3Children);

        phTempParam=new ModBusParam(40059,2,true,true,5,"温度值","°C",3,0,tvJZPHTemp.getId(),0,null);


        List<ModBusParam> orpPoint1Children=new ArrayList<>();
        orpPoint1Children.add(new ModBusParam(40049,2,true,true,5,"第一点信号值","mV",2,0,tvJZORPPoint1Riss.getId(),0,null));
        orpPoint1Children.add(new ModBusParam(40051,1,true,true,0,"第一点ORP值","mV",2,0.01,tvJZORPPoint1.getId(),0,null));
        orpPoint1Param=new ModBusParam(40049,3,true,true,0,"第一点设置","",0,0,0,2,orpPoint1Children);

        List<ModBusParam> orpPoint2Children=new ArrayList<>();
        orpPoint2Children.add(new ModBusParam(40052,2,true,true,5,"第二点信号值","mV",2,0,tvJZORPPoint2Riss.getId(),0,null));
        orpPoint2Children.add(new ModBusParam(40054,1,true,true,0,"第二点ORP值","mV",2,0.01,tvJZORPPoint2.getId(),0,null));
        orpPoint2Param=new ModBusParam(40052,3,true,true,0,"第二点设置","",0,0,0,2,orpPoint2Children);

        List<ModBusParam> orpPoint3Children=new ArrayList<>();
        orpPoint3Children.add(new ModBusParam(40055,2,true,true,5,"第三点信号值","mV",2,0,tvJZORPPoint3Riss.getId(),0,null));
        orpPoint3Children.add(new ModBusParam(40057,1,true,true,0,"第三点ORP值","mV",2,0.01,tvJZORPPoint3.getId(),0,null));
        orpPoint3Param=new ModBusParam(40057,1,true,true,0,"第三点ORP","",0,0,0,2,orpPoint3Children);

        orpTempParam=new ModBusParam(40059,2,true,true,5,"温度值","°C",3,0,tvJZORPTemp.getId(),0,null);

        List<ModBusParam> ddlPoint1Children=new ArrayList<>();
        ddlPoint1Children.add(new ModBusParam(40050,2,true,true,5,"第一点信号值","mV",2,0,tvJZDDLPoint1Riss.getId(),0,null));
        ddlPoint1Children.add(new ModBusParam(40052,2,true,true,5,"第一点电导率值","",2,0,tvJZDDLPoint1.getId(),0,null));
        ddlPoint1Param=new ModBusParam(40050,4,true,true,0,"第一点设置","",0,0,0,2,ddlPoint1Children);
        ddlTempParam=new ModBusParam(40059,2,true,true,5,"温度值","°C",3,0,tvJZDDLTemp.getId(),0,null);



        startPointParam=new ModBusParam(40058,1,true,false,0,"启动参数标定",null,0,0,0,0,null);
        startTempParam=new ModBusParam(40061,1,true,false,0,"启动温度标定",null,0,0,0,0,null);

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

    }


    private void showSelectJZParamMenu() {
        PopupMenu popupMenu = new PopupMenu(this, tvParamName);
        popupMenu.inflate(R.menu.menu_select_param);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                pointWrite=new ModBusParam[]{null,null,null};
                switch (menuItem.getItemId()) {
                    case R.id.ph:
                        paramModBusAdr=3;
                        cleanPHPoint();
                        tvParamName.setText("pH");
                        showLLView(llJZPH.getId());
                        llJZPH.setVisibility(View.VISIBLE);
                        List<ModBusParam> params=new ArrayList<>(Arrays.asList(phValueParam,phRissParam));
                        Collections.sort(params);
                        readData(params);

                        return true;
                    case R.id.orp:
                        paramModBusAdr=5;

                        tvParamName.setText("ORP");
                        showLLView(llJZORP.getId());
                        List<ModBusParam> orpParams=new ArrayList<>(Arrays.asList(orpValueParam,orpRissParam));
                        Collections.sort(orpParams);
                        readData(orpParams);
                        return true;
                    case R.id.ddl:
                        paramModBusAdr=4;
                        cleanDDLPoint();
                        tvParamName.setText("电导率");
                        showLLView(llJZDDL.getId());
                        List<ModBusParam> ddlParams=new ArrayList<>(Arrays.asList(ddlValueParam,ddlRissParam));
                        Collections.sort(ddlParams);
                        readData(ddlParams);
                        return true;
                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void showLLView(int viewId) {
        llJZPH.setVisibility(View.GONE);
        llJZORP.setVisibility(View.GONE);
        llJZDDL.setVisibility(View.GONE);
        findViewById(viewId).setVisibility(View.VISIBLE);
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
            case R.id.tv_jz_param:
                showSelectJZParamMenu();

                break;
            case R.id.tv_jz_ph_point1:
            case R.id.tv_jz_ph_point1_riss:
                  new HitiInputParamPointDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_PH_Point_Input,"第一点设置",phPoint1Param,paramModBusAdr,"请输入第一点pH值和信号值，保留两位小数",LiherCYXParamSetActivity.this);
                break;
            case R.id.tv_jz_ph_point2:
            case R.id.tv_jz_ph_point2_riss:
                if (pointWrite[0]==null){
                    showMsg(2,"请先写入第一点设置");
                    return;
                }
                new HitiInputParamPointDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_PH_Point_Input,"第二点设置",phPoint2Param,paramModBusAdr,"请输入第二点pH值，保留两位小数",LiherCYXParamSetActivity.this);

                break;
            case R.id.tv_jz_ph_point3:
            case R.id.tv_jz_ph_point3_riss:
                if (pointWrite[1]==null){
                    showMsg(2,"请先写入第二点设置");
                    return;
                }
                new HitiInputParamPointDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_PH_Point_Input,"第三点设置",phPoint3Param,paramModBusAdr,"请输入第三点pH值，保留两位小数",LiherCYXParamSetActivity.this);

                break;
            case R.id.tv_jz_ph_temp:
                new HitiInputModBusParamDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_PH_Temp_Input,"pH温度定标",phTempParam,"请输入定标温度值，保留三位小数",LiherCYXParamSetActivity.this);
                break;
            case R.id.tv_jz_orp_point1:
            case R.id.tv_jz_orp_point1_riss:
                new HitiInputParamPointDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_ORP_Point_Input,"第一点设置",orpPoint1Param,paramModBusAdr,"请输入第一点ORP，保留两位小数",LiherCYXParamSetActivity.this);
                break;
            case R.id.tv_jz_orp_point2:
            case R.id.tv_jz_orp_point2_riss:
                if (pointWrite[0]==null){
                    showMsg(2,"请先写入第一点设置");
                    return;
                }
                new HitiInputModBusParamDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_ORP_Point_Input,"第二点设置",orpPoint2Param,"请输入第二点ORP，保留两位小数",LiherCYXParamSetActivity.this);

                break;
            case R.id.tv_jz_orp_point3:
            case R.id.tv_jz_orp_point3_riss:
                if (pointWrite[1]==null){
                    showMsg(2,"请先写入第一点设置");
                    return;
                }
                new HitiInputModBusParamDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_ORP_Point_Input,"第三点设置",orpPoint3Param,"请输入第三点ORP，保留两位小数",LiherCYXParamSetActivity.this);

                break;
            case R.id.tv_jz_orp_temp:
                new HitiInputModBusParamDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_ORP_Temp_Input,"ORP温度定标",orpTempParam,"请输入定标温度值，保留三位小数",LiherCYXParamSetActivity.this);
                break;

            case R.id.tv_jz_ddl_point1:
            case R.id.tv_jz_ddl_point1_riss:
                new HitiInputParamPointDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_DDL_Point_Input,"第一点设置",ddlPoint1Param,paramModBusAdr,"请输入第一点电导率值和信号值，保留两位小数",LiherCYXParamSetActivity.this);
                break;
            case R.id.tv_jz_ddl_temp:
                new HitiInputModBusParamDailog(LiherCYXParamSetActivity.this,Constant.Dialog_JZ_DDL_Temp_Input,"电导率温度定标",ddlTempParam,"请输入定标温度值，保留三位小数",LiherCYXParamSetActivity.this);
                break;


            case R.id.tv_write_point:
                tempPointNo=0;
                switch (paramModBusAdr){
                    case 3:
                        for (ModBusParam i:pointWrite){
                            tempPointNo=tempPointNo+(i==null?0:1);
                        }
                        if (tempPointNo<=0){
                            showMsg(2,"请先写入设置点数据");
                        }else if (tempPointNo==2&&pointWrite[1]==null){
                            showMsg(2,"两点定标需要以第一点和第二点设置的数据来写入");
                        }else {
                            List<Integer> data = new ArrayList<>();
                            for (ModBusParam paramPoint:pointWrite){
                                if (paramPoint!=null){
                                    for (MRegister writeRegister : paramPoint.getWriteRegisters()) {
                                        data.add(writeRegister.getValue());
                                    }
                                }

                            }

                            sendLHBleData("Write DIY=" + ProtocolUtil.getWriteHexDataEndLength(paramModBusAdr, pointWrite[0].getAddress() - 40001, data), Constant.PARAM_PH_Point_Write);
                        }
                        break;
                    case 4:
                        for (ModBusParam i:pointWrite){
                            tempPointNo=tempPointNo+(i==null?0:1);
                        }
                        if (tempPointNo!=1){
                            showMsg(2,"电导率定标校准需要先设置第一点的数据");
                        }else if (tempPointNo==1&&pointWrite[0]==null){
                            showMsg(2,"电导率定标校准需要先设置第一点的数据");
                        }else {
                            List<Integer> data = new ArrayList<>();
                            for (MRegister writeRegister : pointWrite[0].getWriteRegisters()) {
                                data.add(writeRegister.getValue());
                            }
                            sendLHBleData("Write DIY=" + ProtocolUtil.getWriteHexDataEndLength(paramModBusAdr, pointWrite[0].getAddress() - 40001, data), Constant.PARAM_DDL_Point_Write);
                        }
                        break;
                    case 5:
                        for (ModBusParam i:pointWrite){
                            tempPointNo=tempPointNo+(i==null?0:1);
                        }
                        if (tempPointNo<=0){
                            showMsg(2,"请先写入设置点数据");
                        }else if (tempPointNo==2&&pointWrite[1]==null){
                            showMsg(2,"两点定标需要以第一点和第二点设置的数据来写入");
                        }else {
                            List<Integer> data = new ArrayList<>();
                            for (ModBusParam paramPoint:pointWrite){
                                if (paramPoint!=null){
                                    for (MRegister writeRegister : paramPoint.getWriteRegisters()) {
                                        data.add(writeRegister.getValue());
                                    }
                                }

                            }

                            sendLHBleData("Write DIY=" + ProtocolUtil.getWriteHexDataEndLength(paramModBusAdr, pointWrite[0].getAddress() - 40001, data), Constant.PARAM_ORP_Point_Write);
                        }
                        break;
                }

                break;
            case R.id.tv_start_point:

                switch (paramModBusAdr){
                    case 3:
                        if (tempPointNo>0){
                            startPoint(tempPointNo);
                        }else {
                            showMsg(2,"请先下发定标点数据");
                        }

                        break;
                    case 4:
                        if (tempPointNo>0){
                            startPoint(tempPointNo);
                        }else {
                            showMsg(2,"请先下发定标点数据");
                        }

                        break;
                }
                break;

            case R.id.tv_refresh:
                refresh(500);
                break;


        }
    }



    private void readData(List<ModBusParam> params) {
        if (params==null||params.size()<=0)return;
        int firstAddress=params.get(0).getAddress();
        int lastAddress=params.get(params.size()-1).getAddress()+(params.get(params.size()-1).getAddNum()-1);
        if (paramModBusAdr<=0){
            showMsg(2,"请选择参数");
        }else if (firstAddress<ModbusFirstAdds){
            showMsg(2,firstAddress+",读取起始地址异常");
        }else if (lastAddress<firstAddress){
            showMsg(2,"读取寄存器个数不可小于1");
        }else {
            tempParams=params;
            sendLHBleData("Write DIY="+ProtocolUtil.getReadHexData(paramModBusAdr,firstAddress,lastAddress-firstAddress+1),Constant.PARAM_ModBus_Param_READ);
        }
    }


    public void writeData(ModBusParam param){
        if (param==null||param.getWriteRegisters()==null||param.getWriteRegisters().size()<=0){
            showMsg(1,"未获取到您想写入的数据信息");
            return;
        }
        List<Integer> data = new ArrayList<>();
        for (MRegister writeRegister:param.getWriteRegisters()){
            data.add(writeRegister.getValue());
        }

        sendLHBleData("Write DIY="+ProtocolUtil.getWriteHexDataEndLength(paramModBusAdr,param.getAddress()-40001,data),Constant.PARAM_ModBus_Param_Write);


    }

    public void startParamPointsData(ModBusParam param){
        if (param==null||param.getWriteRegisters()==null||param.getWriteRegisters().size()<=0){
            showMsg(1,"未获取到您想写入的数据信息");
            return;
        }
        List<Integer> data = new ArrayList<>();
        for (MRegister writeRegister:param.getWriteRegisters()){
            data.add(writeRegister.getValue());
        }

        sendLHBleData("Write DIY="+ProtocolUtil.getWriteHexDataEndLength(paramModBusAdr,param.getAddress()-40001,data),Constant.PARAM_Start_Param_Points);


    }

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.Dialog_ModBus_Param_Write:
                ModBusParam param= (ModBusParam) target;
                if (param.getWriteRegisters()==null||param.getWriteRegisters().size()<=0){
                    showMsg(1,"请录入参数信息");
                }else if (param.getAddress()<ModbusFirstAdds){
                    showMsg(2, "参数起始寄存器地址丢失，请退出重试");
                }else if (!param.isCanWrite()){
                    showMsg(1,"目标参数不可写入");
                }else {
                       /* azgdParam.setWriteRegisters(new ArrayList<>(Arrays.asList(new MRegister(16))));
                     writeData(azgdParam);*/
                    writeData(param);
                }

                break;

            case Constant.Dialog_JZ_PH_Point_Input:
                ModBusParam phPointParam= (ModBusParam) target;
                if (phPointParam==null){
                    showMsg(1,"未获取到您想写入的数据信息");
                }else if (phPointParam.getWriteRegisters()==null||phPointParam.getWriteRegisters().size()<=0){
                    showMsg(1,"请录入参数信息");
                }else if (phPointParam.getAddress()<ModbusFirstAdds){
                    showMsg(2, "参数起始寄存器地址丢失，请退出重试");
                }else if (!phPointParam.isCanWrite()){
                    showMsg(1,"目标参数不可写入");
                }else {
                    if (phPointParam.getAddress()==phPoint1Param.getAddress()){
                        pointWrite[0]=phPointParam;
                    }else if (phPointParam.getAddress()==phPoint2Param.getAddress()){
                        pointWrite[1]=phPointParam;
                    }else if (phPointParam.getAddress()==phPoint3Param.getAddress()){
                        pointWrite[2]=phPointParam;
                    }else {
                        showMsg(1,"未获取到当前数据对应的标点点序号");
                        return;
                    }
                    phPointParam.notifyDataWaitChanged(LiherCYXParamSetActivity.this.topContentView);

                }
                break;
            case Constant.Dialog_JZ_PH_Temp_Input:
                ModBusParam phTempParam= (ModBusParam) target;
                if (phTempParam.getWriteRegisters()==null||phTempParam.getWriteRegisters().size()<=0){
                    showMsg(1,"请录入参数信息");
                }else if (phTempParam.getAddress()<ModbusFirstAdds){
                    showMsg(2, "参数起始寄存器地址丢失，请退出重试");
                }else if (!phTempParam.isCanWrite()){
                    showMsg(1,"目标参数不可写入");
                }else {
                    if (phTempParam==null||phTempParam.getWriteRegisters()==null||phTempParam.getWriteRegisters().size()<=0){
                        showMsg(1,"未获取到您想写入的数据信息");
                        return;
                    }
                    List<Integer> tempData = new ArrayList<>();
                    for (MRegister writeRegister:phTempParam.getWriteRegisters()){
                        tempData.add(writeRegister.getValue());
                    }

                    sendLHBleData("Write DIY="+ProtocolUtil.getWriteHexDataEndLength(paramModBusAdr,phTempParam.getAddress()-40001,tempData),Constant.PARAM_PH_Temp_Write);

                }
                break;

            case Constant.Dialog_JZ_ORP_Point_Input:
                ModBusParam orpPointParam= (ModBusParam) target;
                if (orpPointParam==null){
                    showMsg(1,"未获取到您想写入的数据信息");
                }else if (orpPointParam.getWriteRegisters()==null||orpPointParam.getWriteRegisters().size()<=0){
                    showMsg(1,"请录入参数信息");
                }else if (orpPointParam.getAddress()<ModbusFirstAdds){
                    showMsg(2, "参数起始寄存器地址丢失，请退出重试");
                }else if (!orpPointParam.isCanWrite()){
                    showMsg(1,"目标参数不可写入");
                }else {
                    if (orpPointParam.getAddress()==orpPoint1Param.getAddress()){
                        pointWrite[0]=orpPointParam;
                    }else if (orpPointParam.getAddress()==orpPoint2Param.getAddress()){
                        pointWrite[1]=orpPointParam;
                    }else if (orpPointParam.getAddress()==orpPoint3Param.getAddress()){
                        pointWrite[2]=orpPointParam;
                    }else {
                        showMsg(1,"未获取到当前数据对应的标点点序号");
                        return;
                    }
                    List<Integer> data = new ArrayList<>();
                    for (MRegister writeRegister:orpPointParam.getWriteRegisters()){
                        data.add(writeRegister.getValue());
                    }
                    orpPointParam.notifyDataWaitChanged(LiherCYXParamSetActivity.this.topContentView);
                   // sendLHBleData("Write DIY="+ProtocolUtil.getWriteHexDataEndLength(paramModBusAdr,orpPointParam.getAddress()-40001,data),Constant.PARAM_ORP_Point_Write);

                }
                break;
            case Constant.Dialog_JZ_ORP_Temp_Input:
                ModBusParam orpTempParam= (ModBusParam) target;
                if (orpTempParam.getWriteRegisters()==null||orpTempParam.getWriteRegisters().size()<=0){
                    showMsg(1,"请录入参数信息");
                }else if (orpTempParam.getAddress()<ModbusFirstAdds){
                    showMsg(2, "参数起始寄存器地址丢失，请退出重试");
                }else if (!orpTempParam.isCanWrite()){
                    showMsg(1,"目标参数不可写入");
                }else {
                    if (orpTempParam==null||orpTempParam.getWriteRegisters()==null||orpTempParam.getWriteRegisters().size()<=0){
                        showMsg(1,"未获取到您想写入的数据信息");
                        return;
                    }
                    List<Integer> tempData = new ArrayList<>();
                    for (MRegister writeRegister:orpTempParam.getWriteRegisters()){
                        tempData.add(writeRegister.getValue());
                    }

                    sendLHBleData("Write DIY="+ProtocolUtil.getWriteHexDataEndLength(paramModBusAdr,orpTempParam.getAddress()-40001,tempData),Constant.PARAM_ORP_Temp_Write);

                }
                break;
            case Constant.Dialog_JZ_DDL_Point_Input:
                ModBusParam ddlPointParam= (ModBusParam) target;
                if (ddlPointParam==null){
                    showMsg(1,"未获取到您想写入的数据信息");
                }else if (ddlPointParam.getWriteRegisters()==null||ddlPointParam.getWriteRegisters().size()<=0){
                    showMsg(1,"请录入参数信息");
                }else if (ddlPointParam.getAddress()<ModbusFirstAdds){
                    showMsg(2, "参数起始寄存器地址丢失，请退出重试");
                }else if (!ddlPointParam.isCanWrite()){
                    showMsg(1,"目标参数不可写入");
                }else {
                    if (ddlPointParam.getAddress()==ddlPoint1Param.getAddress()){
                        pointWrite[0]=ddlPointParam;
                    }else {
                        showMsg(1,"未获取到当前数据对应的标点点序号");
                        return;
                    }
                    ddlPointParam.notifyDataWaitChanged(LiherCYXParamSetActivity.this.topContentView);

                }
                break;
            case Constant.Dialog_JZ_DDL_Temp_Input:
                ModBusParam ddlTempParam= (ModBusParam) target;
                if (ddlTempParam.getWriteRegisters()==null||ddlTempParam.getWriteRegisters().size()<=0){
                    showMsg(1,"请录入参数信息");
                }else if (ddlTempParam.getAddress()<ModbusFirstAdds){
                    showMsg(2, "参数起始寄存器地址丢失，请退出重试");
                }else if (!ddlTempParam.isCanWrite()){
                    showMsg(1,"目标参数不可写入");
                }else {
                    if (ddlTempParam==null||ddlTempParam.getWriteRegisters()==null||ddlTempParam.getWriteRegisters().size()<=0){
                        showMsg(1,"未获取到您想写入的数据信息");
                        return;
                    }
                    List<Integer> tempData = new ArrayList<>();
                    for (MRegister writeRegister:ddlTempParam.getWriteRegisters()){
                        tempData.add(writeRegister.getValue());
                    }

                    sendLHBleData("Write DIY="+ProtocolUtil.getWriteHexDataEndLength(paramModBusAdr,ddlTempParam.getAddress()-40001,tempData),Constant.PARAM_DDL_Temp_Write);

                }
                break;
        }
    }

    @Override
    public void onItemClick3(View v, int position, Object object, int type) {
        switch (type) {

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
        if (LiherCYXParamSetActivity.this.isFinishing()) {
            return;
        }

        loadingDialog.show();
        addLogText(sendString, 0, sendType);
        insertLog(mDeviceAddress, sampleId, sendString);
        final byte[] bytes = tmpBytes;
        currentSendType = sendType;
        if (sendString.length() > 60) {
            mble.sendLhOrderDataList(mSendCharact, mReceiveCharact, sendString, currentSendType);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!mble.sendData(mSendCharact, bytes)) {
                        addLogText(HJBleApplication.getAppContext().getString(R.string.send_fail) + "！", 0, Constant.Return_Ble_SendFail);
                        currentSendType = 0;

                    } else if (!mble.waitReturn(mReceiveCharact, 15 * 1000)) {
                        addLogText(getResources().getString(R.string.ble_rtn_timeout), 1, Constant.Return_BLE_False);
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
                if (recvData == null || recvData.length <= 0) {
                    str = "";
                } else if (HJBleApplication.shareInstance().isBleHex()) {
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
                            case Constant.PARAM_DIY_485_READ:
                                currentReceiveType = Constant.RETURN_DIY_485_READ;
                                break;
                            case Constant.PARAM_DIY_485_Write:
                                currentReceiveType = Constant.RETURN_DIY_485_Write;
                                break;
                            case Constant.PARAM_ModBus_Param_READ:
                                currentReceiveType = Constant.RETURN_ModBus_Param_READ;
                                break;
                            case Constant.PARAM_ModBus_Param_Write:
                                currentReceiveType = Constant.RETURN_ModBus_Param_Write;
                                break;
                            case Constant.PARAM_SSXS_Param_Write:
                                currentReceiveType = Constant.RETURN_SSXS_Param_Write;
                                break;
                            case Constant.PARAM_PH_Point_Write:
                                currentReceiveType=Constant.RETURN_PH_Point_Write;
                                break;
                            case Constant.PARAM_PH_Temp_Write:
                                currentReceiveType=Constant.RETURN_PH_Temp_Write;
                                break;
                            case Constant.PARAM_ORP_Point_Write:
                                currentReceiveType=Constant.RETURN_ORP_Point_Write;
                                break;
                            case Constant.PARAM_ORP_Temp_Write:
                                currentReceiveType=Constant.RETURN_ORP_Temp_Write;
                                break;
                            case Constant.PARAM_Start_Param_Points:
                                currentReceiveType = Constant.RETURN_Start_Param_Points;
                                break;
                            case Constant.PARAM_DDL_Point_Write:
                                currentReceiveType=Constant.RETURN_DDL_Point_Write;
                                break;
                            case Constant.PARAM_DDL_Temp_Write:
                                currentReceiveType=Constant.RETURN_DDL_Temp_Write;
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
                case Constant.RETURN_ModBus_Param_READ:
                    try {
                        SerialData serialData = ProtocolUtil.getReturnMsgForSerial(msgContext);
                        if (serialData.getOrderType() == 0x03) {
                            showMsg(0, "返回" + serialData.getmRegisters().size() + "个寄存器");
                            if (tempParams!=null){
                                int firstAddress=tempParams.get(0).getAddress();
                                for (ModBusParam modBusParam:tempParams){
                                    List<MRegister> mRegisters=serialData.getmRegisters().subList(modBusParam.getAddress()-firstAddress,modBusParam.getAddress()+modBusParam.getAddNum()-firstAddress);
                                    modBusParam.setData(mRegisters);
                                    modBusParam.notifyDataSetChanged(LiherCYXParamSetActivity.this.topContentView);
                                }

                            }
                        } else if (serialData.getOrderType() == 0x10) {
                            showMsg(0, "写入成功" );
                        }
                    } catch (Exception e) {
                        showMsg(1, "解析失败，\n" + msgContext);
                    }

                    break;
                case Constant.RETURN_ModBus_Param_Write:
                    try {
                        SerialData serialData = ProtocolUtil.getReturnMsgForSerial(msgContext);
                        if (serialData.getOrderType() == 0x03) {
                            showMsg(2, "返回" + serialData.getmRegisters().size() + "个寄存器");
                        } else if (serialData.getOrderType() == 0x10) {
                            showMsg(0, "写入成功");

                            refresh(1000);
                        }
                    } catch (Exception e) {
                        showMsg(1, "解析失败，\n" + msgContext);
                    }
                    break;
                case Constant.RETURN_Start_Param_Points:
                    try {
                        SerialData serialData = ProtocolUtil.getReturnMsgForSerial(msgContext);
                         if (serialData.getOrderType() == 0x10) {
                            showMsg(0, "启动"+tempPointNo+"点定标指令下发成功" );
                            tempPointNo=0;
                            refresh(1000);
                        }else {
                             showMsg(1, "启动定标指令下发失败\n"+serialData.getOriginData() );
                             refresh(1000);
                        }
                    } catch (Exception e) {
                        showMsg(1, "解析失败，\n" + msgContext);
                    }
                    break;

                case Constant.RETURN_PH_Point_Write:
                    try {
                        SerialData serialData = ProtocolUtil.getReturnMsgForSerial(msgContext);
                        if (serialData.getOrderType() == 0x10) {
                            if (tempPointNo>=0&&tempPointNo<=pointWrite.length){
                                cleanPHPoint();

                                showMsg(0,"定标点数据写入成功，待启动");
                                RxTimerUtil.cancel();
                                RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                                    @Override
                                    public void doNext(long number) {
                                        refresh(1000);
                                    }
                                });
                            }else {
                                showMsg(2,"定标点数据异常，标点数："+tempPointNo);
                            }
                        }else {
                            showMsg(0,"定标点数据写入失败，"+serialData.getOriginData());
                        }
                    } catch (Exception e) {
                        showMsg(1, "解析失败，\n" + msgContext);
                    }
                    break;
                case Constant.RETURN_PH_Temp_Write:
                    showMsg(0,"正在启动温度定标");
                    startTemp();
                    break;
                case Constant.RETURN_DDL_Temp_Write:
                    showMsg(0,"正在启动温度定标");
                    startTemp();
                    break;
                case Constant.RETURN_ORP_Point_Write:
                    try {
                        SerialData serialData = ProtocolUtil.getReturnMsgForSerial(msgContext);
                        if (serialData.getOrderType() == 0x10) {
                            if (tempPointNo>=0&&tempPointNo<=pointWrite.length){
                                cleanORPPoint();
                                showMsg(0,"定标点数据写入成功，待启动");
                                RxTimerUtil.cancel();
                                RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                                    @Override
                                    public void doNext(long number) {
                                        refresh(1000);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        showMsg(1, "解析失败，\n" + msgContext);
                    }


                    break;
                case Constant.RETURN_ORP_Temp_Write:
                    showMsg(0,"正在启动温度定标");
                    startTemp();
                    break;
                case Constant.RETURN_DDL_Point_Write:
                    try {
                        SerialData serialData = ProtocolUtil.getReturnMsgForSerial(msgContext);
                        if (serialData.getOrderType() == 0x10) {
                            if (tempPointNo>=0&&tempPointNo<=pointWrite.length){
                                cleanDDLPoint();

                                showMsg(0,"电导率定标点数据写入成功，待启动");
                                RxTimerUtil.cancel();
                                RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                                    @Override
                                    public void doNext(long number) {
                                        refresh(1000);
                                    }
                                });
                            }else {
                                showMsg(2,"定标点数据异常，标点数："+tempPointNo);
                            }
                        }else {
                            showMsg(0,"定标点数据写入失败，"+serialData.getOriginData());
                        }
                    } catch (Exception e) {
                        showMsg(1, "解析失败，\n" + msgContext);
                    }
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

    private void cleanPHPoint() {
        pointWrite=new ModBusParam[]{null,null,null};
        tvJZPHPoint1.setText("");
        tvJZPHPoint2.setText("");
        tvJZPHPoint2.setText("");
        tvJZPHPoint1Riss.setText("");
        tvJZPHPoint2Riss.setText("");
        tvJZPHPoint3Riss.setText("");
    }
    private void cleanORPPoint() {
        pointWrite=new ModBusParam[]{null,null,null};
        tvJZORPPoint1.setText("");
        tvJZORPPoint2.setText("");
        tvJZORPPoint2.setText("");
        tvJZORPPoint1Riss.setText("");
        tvJZORPPoint2Riss.setText("");
        tvJZORPPoint3Riss.setText("");
    }

    private void cleanDDLPoint() {
        pointWrite=new ModBusParam[]{null,null,null};
        tvJZDDLPoint1.setText("");
        tvJZDDLPoint1Riss.setText("");

    }


    /**
     * 启动定标点
     * @param tempPointNo
     */
    private void startPoint(int tempPointNo) {
        showMsg(0,"正在启动"+tempPointNo+"定标");
        startPointParam.setWriteRegisters(new ArrayList<>(Arrays.asList(new MRegister(tempPointNo))));
        startParamPointsData(startPointParam);
    }

    /**
     * 启动
     */
    private void startTemp() {
        startTempParam.setWriteRegisters(new ArrayList<>(Arrays.asList(new MRegister(1))));
        writeData(startTempParam);
    }

    /**
     * 延时刷新
     * @param milliseconds 延时的毫秒值，>=500ms
     */
    private void refresh(int milliseconds) {
        RxTimerUtil.cancel();
        RxTimerUtil.timer(milliseconds>500?milliseconds:500, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                List<ModBusParam> params;
                switch (paramModBusAdr){
                    case 3:
                        params=new ArrayList<>(Arrays.asList(phValueParam,phRissParam));
                        Collections.sort(params);
                        readData(params);
                        break;
                    case 5:
                        params=new ArrayList<>(Arrays.asList(orpValueParam,orpRissParam));
                        Collections.sort(params);
                        readData(params);
                        break;
                    case 4:
                        params=new ArrayList<>(Arrays.asList(ddlValueParam,ddlRissParam));
                        Collections.sort(params);
                        readData(params);
                        break;

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

    private LoadingDialog buildDialog(String msg){
        LoadingDialog.Builder loadBuilder=new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage(msg)
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
        return loadBuilder.create();
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
            }
        }
    }


}
