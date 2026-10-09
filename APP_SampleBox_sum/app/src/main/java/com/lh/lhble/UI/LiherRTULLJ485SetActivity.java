package com.lh.lhble.UI;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
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
import com.lh.lhble.Entity.MRegister;
import com.lh.lhble.Entity.ModBusParam;
import com.lh.lhble.Entity.RTUInfo;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.Entity.RtuLljInfo;
import com.lh.lhble.Entity.SerialData;
import com.lh.lhble.R;
import com.lh.lhble.UI.View.HitiInputModBusParamDailog;
import com.lh.lhble.UI.View.HitiInputSSXSParamDailog;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommonItemOnClickListener3;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.network.RetrofitUtils;
import com.umeng.commonsdk.debug.I;
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
 * RTU流量计-基础配置（蓝牙透传-485串口通信）
 */
public class LiherRTULLJ485SetActivity extends BaseActivity implements CommontClickListener, View.OnClickListener, CommonItemOnClickListener3 {
    private final static String TAG = LiherRTULLJ485SetActivity.class.getSimpleName();

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
    private RTUInfo mRtuInfo;

    private ScrollView sclOrders;
    private LinearLayout llOrderMode, llOrders;
    private EditText edFirstNo, edDataNum, edDataNo, edDataValue;
    private TextView tvRead485, tvWrite485;
    private LinearLayout llUiMode;
    private TextView tvRefresh;
    private TextView tvLLJHeight,tvLLJHdgc,tvLLJAzfx,tvLLJLsfxlc,tvLLJFsgl,tvLLJLmd,tvLLJDmxz,tvLLJDmParamName0,tvLLJDmParam0,tvLLJDmParam1,tvLLJDmParam2,
            tvLLJYjhd,tvLLJInitLjll,tvLLJInitZxljll,tvLLJInitFxljll,tvLLJInitJdzljll,tvLljSS0,tvLljSSParam0,tvLljSS1,tvLljSSParam1,tvLljSS2,tvLljSSParam2,
            tvLljSS3,tvLljSSParam3,tvLljSS4,tvLljSSParam4,tvLljSS5,tvLljSSParam5,tvLljSS6,tvLljSSParam6,tvLljSS7,tvLljSSParam7,tvLljSS8,tvLljSSParam8,
            tvLljSS9,tvLljSSParam9;
    private ImageView ivExpandContract;
    private LinearLayout llLLJDmParamMore,llLLJSSParams,llLLJSSParam0,llLLJSSParam1,llLLJSSParam2,llLLJSSParam3,llLLJSSParam4,llLLJSSParam5,llLLJSSParam6,llLLJSSParam7,
            llLLJSSParam8,llLLJSSParam9;
    private LoadingDialog loadingDialog = null;

    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;

    private WiseCharacteristic mLogCharact = Constant.Ble_LOG_Receive_Service;
    private int currentSendType, currentReceiveType;
    private int ModbusFirstAdds=40001;
    private List<ModBusParam> tempParams;

    private ModBusParam azgdParam,hdgzParam,azfxParam,lsfxlcParam,fsglParam,lmdParam,dmxzParam,dmParam0,dmParam1,dmParam2,yjhdParam,initLjllParam,
            initZxljllParam,initFXljllParam,initJdzljllParam,ssxs0Param,ssxs1Param,ssxs2Param,ssxs3Param,ssxs4Param,ssxs5Param,ssxs6Param,ssxs7Param,
            ssxs8Param,ssxs9Param;


    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_liher_rtu_llj_485_set;
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

      /*  RxTimerUtil.cancel();
        RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                refresh();
                showMsg(2, HJBleApplication.getAppContext().getString(R.string.init_data));
            }
        });*/
    }


    public static Intent getIntent(Context context, String name, String mac, boolean isConfig) {
        Intent intent = new Intent(context, LiherRTULLJ485SetActivity.class);
        intent.putExtra(LiherRTULLJ485SetActivity.EXTRAS_DEVICE_NAME, name);
        intent.putExtra(LiherRTULLJ485SetActivity.EXTRAS_DEVICE_ADDRESS, mac);
        intent.putExtra(LiherRTULLJ485SetActivity.EXTRAS_DEVICE_IS_CONFIG, isConfig);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();
        llOrderMode = findViewById(R.id.ll_order_mode);
        sclOrders = findViewById(R.id.scl_orders);
        llOrders = findViewById(R.id.ll_orders);
        edFirstNo = findViewById(R.id.ed_first_no);
        edDataNum = findViewById(R.id.ed_data_num);
        edDataNo = findViewById(R.id.ed_data_no);
        edDataValue = findViewById(R.id.ed_data_value);
        tvRead485 = findViewById(R.id.tv_read_485);
        tvWrite485 = findViewById(R.id.tv_write_485);
        llUiMode = findViewById(R.id.ll_ui_mode);
        tvRefresh = findViewById(R.id.tv_refresh);
        tvLLJHeight=findViewById(R.id.tv_llj_height);
        tvLLJHdgc=findViewById(R.id.tv_llj_hdgc);
        tvLLJAzfx=findViewById(R.id.tv_llj_azfx);
        tvLLJLsfxlc=findViewById(R.id.tv_llj_lsfxlc);
        tvLLJFsgl=findViewById(R.id.tv_llj_fsgl);
        tvLLJLmd=findViewById(R.id.tv_llj_lmd);
        tvLLJDmxz=findViewById(R.id.tv_llj_dmxz);
        tvLLJDmParamName0=findViewById(R.id.tv_llj_dm_param_name_0);
        tvLLJDmParam0=findViewById(R.id.tv_llj_dm_param0);
        tvLLJDmParam1=findViewById(R.id.tv_llj_dm_param1);
        tvLLJDmParam2=findViewById(R.id.tv_llj_dm_param2);
        tvLLJYjhd=findViewById(R.id.tv_llj_yjhd);
        tvLLJInitLjll=findViewById(R.id.tv_llj_init_ljll);
        tvLLJInitZxljll=findViewById(R.id.tv_llj_init_zxljll);
        tvLLJInitFxljll=findViewById(R.id.tv_llj_init_fxljll);
        tvLLJInitJdzljll=findViewById(R.id.tv_llj_init_jdzljll);
        llLLJDmParamMore=findViewById(R.id.ll_llj_dm_param_more);
        llLLJSSParams=findViewById(R.id.ll_llj_ss_params);
        ivExpandContract=findViewById(R.id.iv_expand_contract);
        tvLljSS0=findViewById(R.id.tv_llj_ss0);
        tvLljSSParam0=findViewById(R.id.tv_llj_ss_param0);
        tvLljSS1=findViewById(R.id.tv_llj_ss1);
        tvLljSSParam1=findViewById(R.id.tv_llj_ss_param1);
        tvLljSS2=findViewById(R.id.tv_llj_ss2);
        tvLljSSParam2=findViewById(R.id.tv_llj_ss_param2);
        tvLljSS3=findViewById(R.id.tv_llj_ss3);
        tvLljSSParam3=findViewById(R.id.tv_llj_ss_param3);
        tvLljSS4=findViewById(R.id.tv_llj_ss4);
        tvLljSSParam4=findViewById(R.id.tv_llj_ss_param4);
        tvLljSS5=findViewById(R.id.tv_llj_ss5);
        tvLljSSParam5=findViewById(R.id.tv_llj_ss_param5);
        tvLljSS6=findViewById(R.id.tv_llj_ss6);
        tvLljSSParam6=findViewById(R.id.tv_llj_ss_param6);
        tvLljSS7=findViewById(R.id.tv_llj_ss7);
        tvLljSSParam7=findViewById(R.id.tv_llj_ss_param7);
        tvLljSS8=findViewById(R.id.tv_llj_ss8);
        tvLljSSParam8=findViewById(R.id.tv_llj_ss_param8);
        tvLljSS9=findViewById(R.id.tv_llj_ss9);
        tvLljSSParam9=findViewById(R.id.tv_llj_ss_param9);

        llLLJSSParam0=findViewById(R.id.ll_llj_ss_param0);
        llLLJSSParam1=findViewById(R.id.ll_llj_ss_param1);
        llLLJSSParam2=findViewById(R.id.ll_llj_ss_param2);
        llLLJSSParam3=findViewById(R.id.ll_llj_ss_param3);
        llLLJSSParam4=findViewById(R.id.ll_llj_ss_param4);
        llLLJSSParam5=findViewById(R.id.ll_llj_ss_param5);
        llLLJSSParam6=findViewById(R.id.ll_llj_ss_param6);
        llLLJSSParam7=findViewById(R.id.ll_llj_ss_param7);
        llLLJSSParam8=findViewById(R.id.ll_llj_ss_param8);
        llLLJSSParam9=findViewById(R.id.ll_llj_ss_param9);
        ivExpandContract.setOnClickListener(this);
        llLLJSSParam0.setOnClickListener(this);
        llLLJSSParam1.setOnClickListener(this);
        llLLJSSParam2.setOnClickListener(this);
        llLLJSSParam3.setOnClickListener(this);
        llLLJSSParam4.setOnClickListener(this);
        llLLJSSParam5.setOnClickListener(this);
        llLLJSSParam6.setOnClickListener(this);
        llLLJSSParam7.setOnClickListener(this);
        llLLJSSParam8.setOnClickListener(this);
        llLLJSSParam9.setOnClickListener(this);
        tvLLJHeight.setOnClickListener(this);
        tvLLJHdgc.setOnClickListener(this);
        tvLLJAzfx.setOnClickListener(this);
        tvLLJLsfxlc.setOnClickListener(this);
        tvLLJFsgl.setOnClickListener(this);
        tvLLJLmd.setOnClickListener(this);
        tvLLJDmxz.setOnClickListener(this);
        tvLLJDmParam0.setOnClickListener(this);
        tvLLJDmParam1.setOnClickListener(this);
        tvLLJDmParam2.setOnClickListener(this);
        tvLLJYjhd.setOnClickListener(this);
        tvLLJInitLjll.setOnClickListener(this);
        tvLLJInitZxljll.setOnClickListener(this);
        tvLLJInitFxljll.setOnClickListener(this);
        tvLLJInitJdzljll.setOnClickListener(this);
        tvRead485.setOnClickListener(LiherRTULLJ485SetActivity.this);
        tvWrite485.setOnClickListener(LiherRTULLJ485SetActivity.this);
        tvRefresh.setOnClickListener(LiherRTULLJ485SetActivity.this);
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
        azgdParam=new ModBusParam(40054,1,true,true,0,"安装高度","m",3,0.001d,tvLLJHeight.getId(),0,null);
        hdgzParam=new ModBusParam(40058,2,true,true,2,"河底高程","m",3,0.001d,tvLLJHdgc.getId(),0,null);
        HashMap<Integer,String> azfxMenu=new HashMap<>();
        azfxMenu.put(0,"正向");
        azfxMenu.put(1,"反向");
        azfxParam=new ModBusParam(40155,1,true,true,3,"安装方向",azfxMenu,tvLLJAzfx.getId(),0,null);

        HashMap<Integer,String> lsfxlcMenu=new HashMap<>();
        lsfxlcMenu.put(0,"不滤除");
        lsfxlcMenu.put(1,"滤除正向");
        lsfxlcMenu.put(2,"滤除反向");
        lsfxlcParam=new ModBusParam(40097,1,true,true,3,"流速方向滤除",lsfxlcMenu,tvLLJLsfxlc.getId(),0,null);
        fsglParam=new ModBusParam(40105,1,true,true,0,"发射功率",null,0,0,tvLLJFsgl.getId(),0,null);
        HashMap<Integer,String> lmdMenu=new HashMap<>();
        lmdMenu.put(1,"低");
        lmdMenu.put(5,"中");
        lmdMenu.put(10,"高");
        lmdParam=new ModBusParam(40094,1,true,true,3,"灵敏度",lmdMenu,tvLLJLmd.getId(),0,null);
        HashMap<Integer,String> dmxzMenu=new HashMap<>();
        dmxzMenu.put(0,"矩形");
        dmxzMenu.put(1,"梯形");
        dmxzMenu.put(2,"V形");
        dmxzMenu.put(3,"U形");
        dmxzMenu.put(4,"圆形");
        dmxzMenu.put(5,"不规则");
        dmxzParam=new ModBusParam(40085,1,true,true,3,"断面形状",dmxzMenu,tvLLJDmxz.getId(),0,null);
        tvLLJDmxz.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                  if ("矩形".endsWith(s.toString())){
                      if (dmParam0!=null){
                          dmParam0.setParamName("断面宽度");
                          dmParam0.setUnit("m");
                      }
                      tvLLJDmParamName0.setText("断面宽度(m)");
                      llLLJDmParamMore.setVisibility(View.GONE);
                  }else if ("梯形".endsWith(s.toString())){
                      if (dmParam0!=null){
                          dmParam0.setParamName("断面底宽");
                          dmParam0.setUnit("m");
                      }
                      tvLLJDmParamName0.setText("断面底宽(m)");
                      llLLJDmParamMore.setVisibility(View.VISIBLE);
                  }else if ("V形".endsWith(s.toString())){
                      if (dmParam0!=null){
                          dmParam0.setParamName("断面角度");
                          dmParam0.setUnit("°");
                      }
                      tvLLJDmParamName0.setText("断面角度(°)");
                      llLLJDmParamMore.setVisibility(View.GONE);
                  }else if ("U形".endsWith(s.toString())){
                      if (dmParam0!=null){
                          dmParam0.setParamName("断面半径");
                          dmParam0.setUnit("m");
                      }
                      tvLLJDmParamName0.setText("断面半径(m)");
                      llLLJDmParamMore.setVisibility(View.GONE);
                  }else if ("圆形".endsWith(s.toString())){
                      if (dmParam0!=null){
                          dmParam0.setParamName("断面半径");
                          dmParam0.setUnit("m");
                      }
                      tvLLJDmParamName0.setText("断面半径(m)");
                      llLLJDmParamMore.setVisibility(View.GONE);
                  }else if ("不规则".endsWith(s.toString())){
                      if (dmParam0!=null){
                          dmParam0.setParamName("断面宽度");
                          dmParam0.setUnit("m");
                      }
                      tvLLJDmParamName0.setText("断面宽度(m)");
                      llLLJDmParamMore.setVisibility(View.GONE);
                  }
            }
        });
        dmParam0=new ModBusParam(40086,2,true,true,1,"断面参数1",null,3,0,tvLLJDmParam0.getId(),0,null);
        dmParam1=new ModBusParam(40088,2,true,true,1,"断面顶宽","m",3,0,tvLLJDmParam1.getId(),0,null);
        dmParam2=new ModBusParam(40090,2,true,true,1,"断面高度","m",3,0,tvLLJDmParam2.getId(),0,null);
        yjhdParam=new ModBusParam(40092,2,true,true,1,"淤积厚度","m",3,0,tvLLJYjhd.getId(),0,null);
        initLjllParam=new ModBusParam(40100,3,true,true,4,"累计流量初始值","m³",3,0,tvLLJInitLjll.getId(),0,null);
        initZxljllParam=new ModBusParam(40109,3,true,true,4,"正向累计流量初始值","m³",3,0,tvLLJInitZxljll.getId(),0,null);
        initFXljllParam=new ModBusParam(40115,3,true,true,4,"反向累计流量初始值","m³",3,0,tvLLJInitFxljll.getId(),0,null);
        initJdzljllParam=new ModBusParam(40121,3,true,true,4,"绝对值累计流量初始值","m³",3,0,tvLLJInitJdzljll.getId(),0,null);

        List<ModBusParam> ssxs0Children=new ArrayList<>();
        ssxs0Children.add(new ModBusParam(40200,2,true,false,1,"0号水深","m",3,0,tvLljSS0.getId(),0,null));
        ssxs0Children.add(new ModBusParam(40202,2,true,false,1,"0号系数","",3,0,tvLljSSParam0.getId(),0,null));
        ssxs0Param=new ModBusParam(40200,4,true,false,0,"0号-水深-系数",null,0,0,0,2,ssxs0Children);
        List<ModBusParam> ssxs1Children=new ArrayList<>();
        ssxs1Children.add(new ModBusParam(40204,2,true,false,1,"1号水深","m",3,0,tvLljSS1.getId(),0,null));
        ssxs1Children.add(new ModBusParam(40206,2,true,false,1,"1号系数","",3,0,tvLljSSParam1.getId(),0,null));
        ssxs1Param=new ModBusParam(40204,4,true,false,0,"1号-水深-系数",null,0,0,0,2,ssxs1Children);
        List<ModBusParam> ssxs2Children=new ArrayList<>();
        ssxs2Children.add(new ModBusParam(40208,2,true,false,1,"2号水深","m",3,0,tvLljSS2.getId(),0,null));
        ssxs2Children.add(new ModBusParam(40210,2,true,false,1,"2号系数","",3,0,tvLljSSParam2.getId(),0,null));
        ssxs2Param=new ModBusParam(40208,4,true,false,0,"2号-水深-系数",null,0,0,0,2,ssxs2Children);
        List<ModBusParam> ssxs3Children=new ArrayList<>();
        ssxs3Children.add(new ModBusParam(40212,2,true,false,1,"3号水深","m",3,0,tvLljSS3.getId(),0,null));
        ssxs3Children.add(new ModBusParam(40214,2,true,false,1,"3号系数","",3,0,tvLljSSParam3.getId(),0,null));
        ssxs3Param=new ModBusParam(40212,4,true,false,0,"3号-水深-系数",null,0,0,0,2,ssxs3Children);
        List<ModBusParam> ssxs4Children=new ArrayList<>();
        ssxs4Children.add(new ModBusParam(40216,2,true,false,1,"4号水深","m",3,0,tvLljSS4.getId(),0,null));
        ssxs4Children.add(new ModBusParam(40218,2,true,false,1,"4号系数","",3,0,tvLljSSParam4.getId(),0,null));
        ssxs4Param=new ModBusParam(40216,4,true,false,0,"4号-水深-系数",null,0,0,0,2,ssxs4Children);
        List<ModBusParam> ssxs5Children=new ArrayList<>();
        ssxs5Children.add(new ModBusParam(40220,2,true,false,1,"5号水深","m",3,0,tvLljSS5.getId(),0,null));
        ssxs5Children.add(new ModBusParam(40222,2,true,false,1,"5号系数","",3,0,tvLljSSParam5.getId(),0,null));
        ssxs5Param=new ModBusParam(40220,4,true,false,0,"5号-水深-系数",null,0,0,0,2,ssxs5Children);
        List<ModBusParam> ssxs6Children=new ArrayList<>();
        ssxs6Children.add(new ModBusParam(40224,2,true,false,1,"6号水深","m",3,0,tvLljSS6.getId(),0,null));
        ssxs6Children.add(new ModBusParam(40226,2,true,false,1,"6号系数","",3,0,tvLljSSParam6.getId(),0,null));
        ssxs6Param=new ModBusParam(40224,4,true,false,0,"6号-水深-系数",null,0,0,0,2,ssxs6Children);
        List<ModBusParam> ssxs7Children=new ArrayList<>();
        ssxs7Children.add(new ModBusParam(40228,2,true,false,1,"7号水深","m",3,0,tvLljSS7.getId(),0,null));
        ssxs7Children.add(new ModBusParam(40230,2,true,false,1,"7号系数","",3,0,tvLljSSParam7.getId(),0,null));
        ssxs7Param=new ModBusParam(40228,4,true,false,0,"7号-水深-系数",null,0,0,0,2,ssxs7Children);
        List<ModBusParam> ssxs8Children=new ArrayList<>();
        ssxs8Children.add(new ModBusParam(40232,2,true,false,1,"8号水深","m",3,0,tvLljSS8.getId(),0,null));
        ssxs8Children.add(new ModBusParam(40234,2,true,false,1,"8号系数","",3,0,tvLljSSParam8.getId(),0,null));
        ssxs8Param=new ModBusParam(40232,4,true,false,0,"8号-水深-系数",null,0,0,0,2,ssxs8Children);
        List<ModBusParam> ssxs9Children=new ArrayList<>();
        ssxs9Children.add(new ModBusParam(40236,2,true,false,1,"9号水深","m",3,0,tvLljSS9.getId(),0,null));
        ssxs9Children.add(new ModBusParam(40238,2,true,false,1,"9号系数","",3,0,tvLljSSParam9.getId(),0,null));
        ssxs9Param=new ModBusParam(40236,4,true,false,0,"9号-水深-系数",null,0,0,0,2,ssxs9Children);


        refresh();
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

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.tv_read_485:
                if (StringUtils.isEmpty(edFirstNo.getText().toString()) || StringUtils.isEmpty(edDataNum.getText().toString())) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_order));
                    return;
                } else if (!StringUtils.isInt(edFirstNo.getText().toString()) || !StringUtils.isInt(edDataNum.getText().toString())) {
                    showMsg(2, "请录入正确的ModBus读取指令");
                    return;
                }else if (Integer.parseInt(edFirstNo.getText().toString())<ModbusFirstAdds){
                    showMsg(2, "请录入正确的ModBus地址，≥"+ModbusFirstAdds);
                    return;
                }
                sendLHBleData("Write DIY=" + ProtocolUtil.getReadHexData(0x4A, Integer.parseInt(edFirstNo.getText().toString()) ,
                        Integer.parseInt(edDataNum.getText().toString())), Constant.PARAM_DIY_485_READ);

                break;
            case R.id.tv_write_485:
                if (StringUtils.isEmpty(edDataNo.getText().toString()) || StringUtils.isEmpty(edDataValue.getText().toString())) {
                    showMsg(2, HJBleApplication.getAppContext().getString(R.string.input_order));
                    return;
                } else if (!StringUtils.isInt(edDataNo.getText().toString()) || (!StringUtils.isInt(edDataValue.getText().toString()) && !StringUtils.isDouble(edDataValue.getText().toString()))) {
                    showMsg(2, "请录入正确的ModBus写入指令");
                    return;
                }else if (Integer.parseInt(edDataNo.getText().toString())<ModbusFirstAdds){
                    showMsg(2, "请录入正确的ModBus地址，≥"+ModbusFirstAdds);
                    return;
                }
                if (StringUtils.isInt(edDataValue.getText().toString())) {
                    List<Integer> data = new ArrayList<>();
                    data.add(Integer.parseInt(edDataValue.getText().toString()));
                    sendLHBleData("Write DIY=" + ProtocolUtil.getWriteHexData(0x4A,
                            Integer.parseInt(edDataNo.getText().toString()), data), Constant.PARAM_DIY_485_Write);
                }

                break;
            case R.id.tv_refresh:
                refresh();
                break;
            case R.id.tv_llj_height:
                if (StringUtils.isEmpty(azgdParam.getDataValue())){
                    List<ModBusParam> params=new ArrayList<>(Arrays.asList(azgdParam));
                    Collections.sort(params);
                    readData(params);
                }else {

                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            azgdParam,null,LiherRTULLJ485SetActivity.this);
                }

                break;
            case R.id.tv_llj_hdgc:
                if (StringUtils.isEmpty(hdgzParam.getDataValue())){
                    List<ModBusParam> params=new ArrayList<>(Arrays.asList(hdgzParam));
                    Collections.sort(params);
                    readData(params);
                }else {

                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            hdgzParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_azfx:
                if (azfxParam==null||azfxParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"安装方向参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            azfxParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_lsfxlc:
                if (lsfxlcParam==null||lsfxlcParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"流速方向滤除参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            lsfxlcParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_fsgl:
                if (fsglParam==null||fsglParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"发射功率参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            fsglParam,"发射功率取值范围：0~7（0-功率最大，7-功率最小）",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_lmd:
                if (lmdParam==null||lmdParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"灵敏度参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            lmdParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_dmxz:
                if (dmxzParam==null||dmxzParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"断面形状参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            dmxzParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_dm_param0:
                if (dmParam0==null||dmParam0.getAddress()<ModbusFirstAdds){
                    showMsg(2,"断面参数0丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            dmParam0,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_dm_param1:
                if (dmParam1==null||dmParam1.getAddress()<ModbusFirstAdds){
                    showMsg(2,"断面参数1丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            dmParam1,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_dm_param2:
                if (dmParam2==null||dmParam2.getAddress()<ModbusFirstAdds){
                    showMsg(2,"断面参数2丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            dmParam2,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_yjhd:
                if (yjhdParam==null||yjhdParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"淤积厚度参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            yjhdParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_init_ljll:
                if (initLjllParam==null||initLjllParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"累计流量初始值参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            initLjllParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;

            case R.id.tv_llj_init_zxljll:
                if (initZxljllParam==null||initZxljllParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"正向累计流量初始值参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            initZxljllParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_init_fxljll:
                if (initFXljllParam==null||initFXljllParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"反向累计流量初始值参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            initFXljllParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.tv_llj_init_jdzljll:
                if (initJdzljllParam==null||initJdzljllParam.getAddress()<ModbusFirstAdds){
                    showMsg(2,"绝对值累计流量初始值参数丢失");
                }else {
                    new HitiInputModBusParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_ModBus_Param_Write,"参数信息录入",
                            initJdzljllParam,null,LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.iv_expand_contract:
                if (llLLJSSParams.getVisibility() == View.VISIBLE) {
                    ivExpandContract.setSelected(false);
                    llLLJSSParams.setVisibility(View.GONE);
                } else {
                    ivExpandContract.setSelected(true);
                    llLLJSSParams.setVisibility(View.VISIBLE);
                    refreshSSXSParams();

                }
                break;
            case R.id.ll_llj_ss_param0:
                if (ssxs0Param==null||ssxs0Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"0号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",ssxs0Param,
                            "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.ll_llj_ss_param1:
                if (ssxs1Param==null||ssxs1Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"1号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",ssxs1Param,
                            "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.ll_llj_ss_param2:
                if (ssxs2Param==null||ssxs2Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"2号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",
                            ssxs2Param, "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.ll_llj_ss_param3:
                if (ssxs3Param==null||ssxs3Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"3号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",ssxs3Param,
                            "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.ll_llj_ss_param4:
                if (ssxs4Param==null||ssxs4Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"4号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",ssxs4Param,
                            "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.ll_llj_ss_param5:
                if (ssxs5Param==null||ssxs5Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"5号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",ssxs5Param,
                            "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.ll_llj_ss_param6:
                if (ssxs6Param==null||ssxs6Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"6号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",ssxs6Param,
                            "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.ll_llj_ss_param7:
                if (ssxs7Param==null||ssxs7Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"7号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",ssxs7Param,
                            "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.ll_llj_ss_param8:
                if (ssxs8Param==null||ssxs8Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"8号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",ssxs8Param,
                            "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;
            case R.id.ll_llj_ss_param9:
                if (ssxs9Param==null||ssxs9Param.getAddress()<ModbusFirstAdds){
                    showMsg(2,"9号水深系数参数丢失");
                }else {
                    new HitiInputSSXSParamDailog(LiherRTULLJ485SetActivity.this,Constant.Dialog_SSXS_Param_Write,"水深-系数数据录入",ssxs9Param,
                            "水深值>=0,保留三位小数\n系数值，保留三位小数",LiherRTULLJ485SetActivity.this);
                }
                break;

        }
    }

    private void refreshSSXSParams() {
        List<ModBusParam> params=new ArrayList<>(Arrays.asList(ssxs0Param,ssxs1Param,ssxs2Param,ssxs3Param,ssxs4Param,ssxs5Param,ssxs6Param,ssxs7Param,
                ssxs8Param,ssxs9Param));
        Collections.sort(params);
        readData(params);
    }

    private void readData(List<ModBusParam> params) {
        if (params==null||params.size()<=0)return;
        int firstAddress=params.get(0).getAddress();
        int lastAddress=params.get(params.size()-1).getAddress()+(params.get(params.size()-1).getAddNum()-1);
        if (firstAddress<ModbusFirstAdds){
            showMsg(2,firstAddress+",读取起始地址异常");
        }else if (lastAddress<firstAddress){
            showMsg(2,"读取寄存器个数不可小于1");
        }else {
            tempParams=params;
            sendLHBleData("Write DIY="+ProtocolUtil.getReadHexData(0x4A,firstAddress,lastAddress-firstAddress+1),Constant.PARAM_ModBus_Param_READ);
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

        sendLHBleData("Write DIY="+ProtocolUtil.getWriteHexData(0X4A,param.getAddress(),data),Constant.PARAM_ModBus_Param_Write);


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
            case Constant.Dialog_SSXS_Param_Write:
                ModBusParam ssxsParam= (ModBusParam) target;
                if (ssxsParam.getWriteRegisters()==null||ssxsParam.getWriteRegisters().size()<=0){
                    showMsg(1,"请录入参数信息");
                }else if (ssxsParam.getAddress()<ModbusFirstAdds){
                    showMsg(2, "参数起始寄存器地址丢失，请退出重试");
                }else {
                    List<Integer> data = new ArrayList<>();
                    data.add((ssxsParam.getAddress()-40200)/4);
                    for (MRegister writeRegister:ssxsParam.getWriteRegisters()){
                        data.add(writeRegister.getValue());
                    }

                    sendLHBleData("Write DIY="+ProtocolUtil.getWriteHexData(0X4A,40200,data),Constant.PARAM_SSXS_Param_Write);

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
        if (LiherRTULLJ485SetActivity.this.isFinishing()) {
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
                case Constant.RETURN_DIY_485_READ:
                    try {
                        SerialData serialData = ProtocolUtil.getReturnMsgForSerial(msgContext);
                        if (serialData.getOrderType() == 0x03) {
                            showMsg(0, "返回" + serialData.getmRegisters().size() + "个寄存器");
                        } else if (serialData.getOrderType() == 0x10) {
                            showMsg(0, "写入成功");
                        }
                    } catch (Exception e) {
                        showMsg(1, "解析失败，\n" + msgContext);
                    }

                    break;
                case Constant.RETURN_DIY_485_Write:
                    try {
                        SerialData serialData = ProtocolUtil.getReturnMsgForSerial(msgContext);
                        if (serialData.getOrderType() == 0x03) {
                            showMsg(0, "返回" + serialData.getmRegisters().size() + "个寄存器");
                        } else if (serialData.getOrderType() == 0x10) {
                            showMsg(0, "写入成功");
                        }
                    } catch (Exception e) {
                        showMsg(1, "解析失败，\n" + msgContext);
                    }

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
                                    modBusParam.notifyDataSetChanged(LiherRTULLJ485SetActivity.this.topContentView);
                                }

                            }
                        } else if (serialData.getOrderType() == 0x10) {
                            showMsg(0, "写入成功");
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
                            refresh();
                        }
                    } catch (Exception e) {
                        showMsg(1, "解析失败，\n" + msgContext);
                    }
                    break;
                case Constant.RETURN_SSXS_Param_Write:
                    try {
                        SerialData serialData = ProtocolUtil.getReturnMsgForSerial(msgContext);
                        if (serialData.getOrderType() == 0x03) {
                            showMsg(2, "返回" + serialData.getmRegisters().size() + "个寄存器");
                        } else if (serialData.getOrderType() == 0x10) {
                            showMsg(0, "写入成功");
                            RxTimerUtil.cancel();
                            RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                                @Override
                                public void doNext(long number) {
                                    refreshSSXSParams();
                                }
                            });
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

    /**
     * 延时500毫秒后刷新数据
     */
    private void refresh() {
        RxTimerUtil.cancel();
        RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
            @Override
            public void doNext(long number) {
                List<ModBusParam> params=new ArrayList<>(Arrays.asList(azgdParam,hdgzParam,azfxParam,lsfxlcParam,fsglParam,lmdParam,dmxzParam,dmParam0,dmParam1,dmParam2,yjhdParam,initLjllParam,
                        initZxljllParam,initFXljllParam,initJdzljllParam));
                Collections.sort(params);
                readData(params);
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
