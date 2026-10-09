package com.lh.lhble.UI;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.githang.statusbar.StatusBarCompat;
import com.lh.lhble.Entity.AppUpgrade;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.R;
import com.lh.lhble.UI.View.SelecterObjectDailog;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.UI.View.HitiVersionDailog;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.listener.CommonItemOnClickListener3;
import com.lh.lhble.listener.CommontClickListener;
import com.lh.lhble.UI.View.HitiInputStrDailog;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.google.zxing.activity.CaptureActivity;
import com.lh.lhble.network.RetrofitUtils;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.ble.scan.BleParamsOptions;
import com.wise.ble.scan.BluetoothScanManager;
import com.wise.ble.scan.ScanOverListener;
import com.wise.ble.scan.bluetoothcompat.ScanCallbackCompat;
import com.wise.ble.scan.bluetoothcompat.ScanResultCompat;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import update.network.DownloadInstall;
import update.network.DownloadManager;
import update.po.ApkInfo;

import static com.lh.lhble.google.zxing.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;

public class ScanBleActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks, CommontClickListener, CommonItemOnClickListener3 {

    // 扫描是否过滤
    private static final boolean SCAN_FILTER = true;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int RC_PERM_CODE = 124;

    private String[] permissionList = {Manifest.permission.CAMERA,Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private boolean mScanning=true;
    private TextView tvScanMac,tvBleSearch;
    private ListView mListView = null;

    private LeDeviceListAdapter mLeDeviceListAdapter;

    private WiseBluetoothLe mble;

    private LoadingDialog loadingDialog = null;

    private ProgressBar scanProgress;

    private BluetoothScanManager scanManager;

    private Timer timer = new Timer();
    private TimerTask timerTask;
    private String searchBleName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mble=WiseBluetoothLe.getInstance(HJBleApplication.shareInstance());
        mble.mBleCallBack=bleCallBack;

        //判断本设备是否支持蓝牙ble，并连接本地蓝牙设备
        if(!mble.isBleSupported() || !mble.connectLocalDevice())
        {
             showMsg(1,HJBleApplication.getAppContext().getString(R.string.not_support)+"BLE");
            finish();
            return ;
        }

        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_scan_ble;
    }

    /**
     * 获取版本名称
     * @param context 上下文
     * @return 版本名称
     */
    public String getVersionName(Context context) {

        //获取包管理器
        PackageManager pm = context.getPackageManager();
        //获取包信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            //返回版本号
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    protected void initView() {
        super.initView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorNavBackground, null), false);
        }

        setTitle(HJBleApplication.getAppContext().getString(R.string.scan_list));
        topLeftBtn.setImageResource(R.mipmap.info);
        topLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String appUrl="http://cloud.lihero.com:3000/apk2/#/login?file=http%3A%2F%2Fcloud.lihero.com%3A3001%2FfileServer&id=543450080397492224";
                new HitiVersionDailog(ScanBleActivity.this, Constant.Hiti_Version_Dialog,null,getString(R.string.app_name)+"安装包二维码","扫描上方二维码，下载"+getString(R.string.app_name),appUrl,ScanBleActivity.this);
               /* AlertDialog.Builder ab=new AlertDialog.Builder(ScanBleActivity.this);  //(普通消息框)
                ab.setTitle(HJBleApplication.getAppContext().getString(R.string.version_info));  //设置标题
                ab.setMessage("V " + getVersionName(ScanBleActivity.this));//设置消息内容
                ab.setNegativeButton(HJBleApplication.getAppContext().getString(R.string.ensure),null);//设置取消按钮
                ab.show();//显示弹出框*/
            }
        });

        setRightText(HJBleApplication.getAppContext().getString(R.string.stop_scan));
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScanning) {
                    scanLeDevice(false);
                }else {
                    mLeDeviceListAdapter.clear();
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    scanLeDevice(true);
                }
            }
        });
        tvScanMac=findViewById(R.id.tv_scan_mac);
        tvBleSearch=findViewById(R.id.tv_ble_search);
        mListView = findViewById(R.id.list_view);
        scanProgress = findViewById(R.id.progressBar1);

        LoadingDialog.Builder loadBuilder=new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage(HJBleApplication.getAppContext().getString(R.string.connecting))
                .setCancelOutside(false)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        mble.disconnectDevice();
                    }
                });
        loadingDialog = loadBuilder.create();

        tvScanMac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(ScanBleActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);


            }
        });
        tvBleSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(false);
                new HitiInputStrDailog(ScanBleActivity.this, Constant.Dailog_BLE_Search,HJBleApplication.getAppContext().getString(R.string.search_target_ble_dlg),HJBleApplication.getAppContext().getString(R.string.ble_name),"",ScanBleActivity.this);
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                scanLeDevice(false);
                loadingDialog.setMessage(HJBleApplication.getAppContext().getString(R.string.connecting));
                loadingDialog.show();

                HJBleScanDevice scanDevice = mLeDeviceListAdapter.getScanDeviceInfo(position);
                connectBle(scanDevice.device, scanDevice.sendDataLenMax, scanDevice.isConfig);

            }
        });

        scanManager = BluetoothScanManager.getInstance(this);
        scanManager.setScanOverListener(new ScanOverListener() {
            @Override
            public void onScanOver() {
                if (scanManager!=null&&scanManager.isPauseScanning()){
                }
            }
        });
        scanManager.setScanCallbackCompat(scanCallback);

        BleParamsOptions.Builder builder = new BleParamsOptions.Builder()
                .setBackgroundScanPeriod(5000)
                .setForegroundBetweenScanPeriod(300);

        BluetoothScanManager.setBleParamsOptions(builder.build());

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //判断本地蓝牙是否已打开
        if(!mble.isOpened())
        {
            Intent openIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(openIntent, REQUEST_ENABLE_BT);
        }

        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();

        if (!checkPermissions()) {
            showMsg(2,HJBleApplication.getAppContext().getString(R.string.request_permission_list));
         }else {
            checkVersion();
        }

        EasyPermissions.requestPermissions(this,HJBleApplication.getAppContext().getString(R.string.request_permission_list),RC_PERM_CODE,permissionList);

        startTimer();
    }

    private void checkVersion() {
        Map<String,Object> params=new HashMap<>();
        params.put("appResourceId", HJBleApplication.getUser().getDefaultProject());
        params.put("appCode","lhble");
        RetrofitUtils.getApiService().appUpgrade(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Result>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Result result) {
                        if (result.getCode()==Constant.SuccessCode){
                            if (result.getData()==null){
                                return;
                            }else {
                                AppUpgrade bean= JSONObject.parseObject(JSON.toJSONString(result.getData()),AppUpgrade.class);
                                ApkInfo apk = new ApkInfo();


                                String url ="http:"+Constant.SERVICE_API_ADDRESS.split(":")[1]+":"+bean.getPort()+"/fileServer/"+bean.getAppPath();
                                apk.setDownloadUrl(url);
                                apk.setApkCode(Integer.parseInt(bean.getAppVersion()));
                                apk.setApkVersion(bean.getAppVersionName());
                                apk.setApkSize(bean.getAppSize());
                                apk.setApkLog(bean.getAppLog());
                                String name = getApplicationContext().getPackageName();
                                boolean isNeedUpgrade = true;
                                if(bean.getIsNeedUpgrade() == 0){
                                    isNeedUpgrade = false;
                                }else if(bean.getIsNeedUpgrade() == 1){
                                    isNeedUpgrade = true;
                                }
                                try {
                                    PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                                    if (apk.getApkCode()<=pi.versionCode){
                                        return ;
                                    }
                                } catch (PackageManager.NameNotFoundException var2) {
                                    var2.printStackTrace();
                                }
                                if (ScanBleActivity.this!=null&&!ScanBleActivity.this.isFinishing()){
                                    DownloadManager dm = new DownloadManager(ScanBleActivity.this,apk,true,name,isNeedUpgrade);
                                }
                            }
                        }else {
                            showMsg(2,HJBleApplication.getAppContext().getString(R.string.request_version_err)+result.getCode());
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        showMsg(2,HJBleApplication.getAppContext().getString(R.string.request_err_normal));
                     }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
        }

        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }
            };
        }

        if (timer != null) {
            timer.schedule(timerTask,3000,10000);//延时1s，每隔2秒执行一次run方法
        }
    }

    private  void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        scanLeDevice(false);		//停止蓝牙扫描
        mLeDeviceListAdapter.clear();	//清空list
        mLeDeviceListAdapter.notifyDataSetChanged();
        stopTimer();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1){
                mLeDeviceListAdapter.sort();
                mLeDeviceListAdapter.notifyDataSetChanged();
            }else if (msg.what==2){
                if (loadingDialog!=null&&loadingDialog.isShowing()&&!StringUtils.isEmpty(loadingDialog.getMessage())){
                    loadingDialog.setMessage("正在尝试第"+(msg.arg2+1)+"次蓝牙连接");
                }


            }
            super.handleMessage(msg);
        }
    };


    // 连接蓝牙
    void connectBle(final BluetoothDevice device, final int sendDataLenMax, final boolean isConfig) {
        if ("00:00:00:00:00:00".equals(device.getAddress())){
           showMsg(2,HJBleApplication.getAppContext().getString(R.string.initial_ble_tips));
        }
        new Thread(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++)
                {
                    if (i>0){
                        Message message = new Message();
                        message.what = 2;
                        message.arg2=i;
                        handler.sendMessage(message);
                    }
                    //String ads="19:98:03:17:03:20";
                    if (mble==null){
                        return;
                    }else if(mble.connectDevice(device.getAddress(),bleCallBack))	//连接蓝牙设备
                        break;

                    mble.disconnectDevice();
                    try {
                        Thread.sleep(500,0);//200ms
                    }
                    catch (Exception e){

                    }
                }
                if(i == 5)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (!ScanBleActivity.this.isFinishing()){
                                loadingDialog.dismiss();
                                scanLeDevice(true);
                                if (device!=null&&device.getAddress()!=null&&(device.getAddress().startsWith("0B")||device.getAddress().startsWith("0C"))){
                                    new HitiStringDailog(ScanBleActivity.this,Constant.HitiStrTargetDialog_Connect_Fail,null,getResources().getString(R.string.connect_err),getResources().getString(R.string.connect_err_0),ScanBleActivity.this);
                                }else {
                                    showMsg(1,HJBleApplication.getAppContext().getString(R.string.connect_err));
                                }
                            }
                        }
                    });
                    return ;
                }

                try {
                    Thread.sleep(1000,0);//200ms
                }
                catch (Exception e){

                }
                if (mble==null){
                    return;
                }

                // 打开配置通知
                if(isConfig && !mble.openNotify(Constant.Ble_Config_Receive_Service))
                {
                    mble.disconnectDevice();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!ScanBleActivity.this.isFinishing()){
                                showMsg(1,HJBleApplication.getAppContext().getString(R.string.connect_err)+"1");
                                loadingDialog.dismiss();
                                scanLeDevice(true);
                            }

                        }
                    });
                    return ;
                }
                if (mble==null){
                    return;
                }

               // if(mble.openNotify(BleConfig.Ble_Data_Receive_Service)&&mble.openNotify(BleConfig.Ble_LOG_Receive_Service))
                if(mble.openNotify(Constant.Ble_Data_Receive_Service))
                {
                    if (mble.requestMtu(255)){
                        mble.setSendDataLenMax(sendDataLenMax);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                               if (ScanBleActivity.this.isFinishing())return;
                               loadingDialog.dismiss();
                                HJBleApplication.shareInstance().setDesID("12345678");
                                if (device.getAddress().startsWith("00")){
                                    ScanBleActivity.this.startActivity(LiherGGCYActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0D:01")){
                                    ScanBleActivity.this.startActivity(LiherDXCYXActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0D:02")){
                                    ScanBleActivity.this.startActivity(LiherDXCYX2Activity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0D:03")){
                                    List<JSONObject> jat = new ArrayList<>();
                                    JSONObject jo0 = new JSONObject();
                                    jo0.put("MyName", "2025年前采样箱程序");
                                    jo0.put("MyObject", LiherDXCYX3Activity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                    jat.add(jo0);
                                    JSONObject jo1 = new JSONObject();
                                    jo1.put("MyName", "2026年兼容RTU采样箱程序" );
                                    jo1.put("MyObject", LiherCYX7Activity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));

                                    jat.add(jo1);
                                    new SelecterObjectDailog(ScanBleActivity.this, "选择采样箱硬件程序", jat, Constant.Dialog_Select_CYX_Version, ScanBleActivity.this);

                                }else if (device.getAddress().startsWith("0D:04")){
                                    ScanBleActivity.this.startActivity(LiherCYX4Activity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0D:05")){
                                    ScanBleActivity.this.startActivity(LiherCYX5Activity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0D:06")){
                                    ScanBleActivity.this.startActivity(LiherCYX6Activity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0D:07")){
                                    ScanBleActivity.this.startActivity(LiherCYX7Activity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0D")){
                                    ScanBleActivity.this.startActivity(LiherCYXActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0E")){
                                    ScanBleActivity.this.startActivity(LiherZKYActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0A")){
                                    ScanBleActivity.this.startActivity(LiherAdvancedBottleActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("11")){
                                    ScanBleActivity.this.startActivity(LiherYSXActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("12")){
                                    ScanBleActivity.this.startActivity(LiherSWJ1Activity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("15")){
                                    ScanBleActivity.this.startActivity(LiherRTUActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("16")){
                                    ScanBleActivity.this.startActivity(LiherADCPActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else if (device.getAddress().startsWith("0C")){
                                    ScanBleActivity.this.startActivity(LiherShortBottleActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
                                }else {
                                    ScanBleActivity.this.startActivity(LiherActivity.getIntent(ScanBleActivity.this,device.getName(),device.getAddress().toUpperCase(),isConfig));
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
                                if (!ScanBleActivity.this.isFinishing()){
                                    showMsg(1,HJBleApplication.getAppContext().getString(R.string.connect_err)+"3,"+HJBleApplication.getAppContext().getString(R.string.mtu_err));
                                    loadingDialog.dismiss();
                                    scanLeDevice(true);
                                }

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
                            if (!ScanBleActivity.this.isFinishing()) {
                              showMsg(1,HJBleApplication.getAppContext().getString(R.string.connect_err)+"2");
                                loadingDialog.dismiss();
                                scanLeDevice(true);
                            }
                        }
                    });
                    return ;
                }

            }
        }).start();
    }


    // 连接蓝牙
    void connectBleMac(final String deviceName, final String macAddress, final int sendDataLenMax, final boolean isConfig) {
        if ("00:00:00:00:00:00".equals(macAddress)){
            showMsg(2,HJBleApplication.getAppContext().getString(R.string.initial_ble_tips));
        }else if (!mble.getBluetoothAdapter().checkBluetoothAddress(macAddress)){
            showMsg(2,HJBleApplication.getAppContext().getString(R.string.not_lh_qcode));
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++)
                {
                    if (i>0){
                        Message message = new Message();
                        message.what = 2;
                        message.arg2=i;
                        handler.sendMessage(message);
                    }
                    if (mble==null){
                        return;
                    }else if(mble.connectDevice(macAddress,bleCallBack))	//连接蓝牙设备
                        break;

                    mble.disconnectDevice();
                    try {
                        Thread.sleep(500,0);//200ms
                    } catch (Exception e){

                    }


                }
                if(i == 5)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!ScanBleActivity.this.isFinishing()) {
                                loadingDialog.dismiss();
                                scanLeDevice(true);
                                if (macAddress!=null&&(macAddress.startsWith("0B")||macAddress.startsWith("0C"))){
                                    new HitiStringDailog(ScanBleActivity.this,Constant.HitiStrTargetDialog_Connect_Fail,null,getResources().getString(R.string.connect_err),getResources().getString(R.string.connect_err_0),ScanBleActivity.this);
                                }else {
                                       showMsg(1,HJBleApplication.getAppContext().getString(R.string.connect_err));
                                }


                            }
                        }
                    });
                    return ;
                }

                try {
                    Thread.sleep(1000,0);//200ms
                }
                catch (Exception e){

                }
                if (mble==null){
                    return;
                }

                // 打开配置通知
                if(isConfig && !mble.openNotify(Constant.Ble_Config_Receive_Service))
                {
                    mble.disconnectDevice();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMsg(1,HJBleApplication.getAppContext().getString(R.string.connect_err)+"1");
                            loadingDialog.dismiss();
                            scanLeDevice(true);
                        }
                    });
                    return ;
                }

                if (mble==null){
                    return;
                }

                if(mble.openNotify(Constant.Ble_Data_Receive_Service))
                {
                    if (mble.requestMtu(255)){
                        mble.setSendDataLenMax(sendDataLenMax);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (ScanBleActivity.this.isFinishing())return;
                                loadingDialog.dismiss();
                                HJBleApplication.shareInstance().setDesID("12345678");
                              //  ScanBleActivity.this.startActivity(LiherActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));

                                if (macAddress.startsWith("00")){
                                    ScanBleActivity.this.startActivity(LiherGGCYActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("0D:01")){
                                    ScanBleActivity.this.startActivity(LiherDXCYXActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("0D:02")){
                                    ScanBleActivity.this.startActivity(LiherDXCYX2Activity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("0D:03")){
                                    List<JSONObject> jat = new ArrayList<>();
                                    JSONObject jo0 = new JSONObject();
                                    jo0.put("MyName", "2025年前采样箱程序");
                                    jo0.put("MyObject", LiherDXCYX3Activity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                    jat.add(jo0);
                                    JSONObject jo1 = new JSONObject();
                                    jo1.put("MyName", "2026年兼容RTU采样箱程序" );
                                    jo1.put("MyObject", LiherCYX7Activity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                    jat.add(jo1);
                                    new SelecterObjectDailog(ScanBleActivity.this, "选择采样箱硬件程序", jat, Constant.Dialog_Select_CYX_Version, ScanBleActivity.this);

                                }else if (macAddress.startsWith("0D:04")){
                                    ScanBleActivity.this.startActivity(LiherCYX4Activity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("0D:05")){
                                    ScanBleActivity.this.startActivity(LiherCYX5Activity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("0D:07")){
                                    ScanBleActivity.this.startActivity(LiherCYX7Activity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("0D")){
                                    ScanBleActivity.this.startActivity(LiherCYXActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("0E")){

                                    ScanBleActivity.this.startActivity(LiherZKYActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("0A")){
                                    ScanBleActivity.this.startActivity(LiherAdvancedBottleActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("11")){
                                    if (!HJBleApplication.getUser().getStrPermissions().contains("PRO_BLE_FIX")&&!HJBleApplication.getUser().getStrPermissions().contains("PRO_Qualification")){
                                         showMsg(2,HJBleApplication.getAppContext().getString(R.string.not_pro_fix_permission));
                                        return;
                                    }
                                    ScanBleActivity.this.startActivity(LiherYSXActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("12")){
                                    ScanBleActivity.this.startActivity(LiherSWJ1Activity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("15")){
                                    ScanBleActivity.this.startActivity(LiherRTUActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("16")){
                                    ScanBleActivity.this.startActivity(LiherADCPActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else if (macAddress.startsWith("0C")){
                                    ScanBleActivity.this.startActivity(LiherShortBottleActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
                                }else {
                                    ScanBleActivity.this.startActivity(LiherActivity.getIntent(ScanBleActivity.this,deviceName,macAddress.toUpperCase(),isConfig));
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
                                if (!ScanBleActivity.this.isFinishing()){
                                showMsg(2,HJBleApplication.getAppContext().getString(R.string.connect_err)+"3,"+HJBleApplication.getAppContext().getString(R.string.mtu_err));
                                loadingDialog.dismiss();
                                scanLeDevice(true);
                                }
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
                            if (!ScanBleActivity.this.isFinishing()) {
                               showMsg(1,HJBleApplication.getAppContext().getString(R.string.connect_err)+"2");
                                loadingDialog.dismiss();
                                scanLeDevice(true);
                            }
                        }
                    });
                    return ;
                }

            }
        }).start();
    }
    private void installAPK() {
        String apkPath = DownloadInstall.apkPath;
        if (apkPath==null||apkPath.length()<=0){
            checkVersion();
        }else {
            try {
                Uri apkUri = FileProvider.getUriForFile(ScanBleActivity.this, getPackageName()+".fileprovider",
                        new File(apkPath));//在AndroidManifest中的android:authorities值
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                install.setDataAndType(apkUri, "application/vnd.android.package-archive");
                startActivity(install);
            }catch (IllegalArgumentException exception){
                showMsg(2,"版本更新失败，请点击右上角\tⓘ\t，手动安装最新版本");
            }

        }
    }

    @Override
    protected void onDestroy() {
        if (loadingDialog!=null)loadingDialog.dismiss();
        scanLeDevice(false);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==10001){
            if (resultCode == RESULT_OK) {
                installAPK();
            } else {
                new HitiStringDailog(ScanBleActivity.this,Constant.Dailog_INSTALL_APP_Hit,"",getString(R.string.app_name),HJBleApplication.getAppContext().getString(R.string.install_apk_request),ScanBleActivity.this);

            }
        }else if (requestCode==Constant.RequestCode_INSTALL_APP){
            if (resultCode==RESULT_OK){
                installAPK();
            }else {
                showMsg(2,HJBleApplication.getAppContext().getString(R.string.install_apk_request));
             }
        }else if(requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {//打开蓝牙结果
            finish();
            return ;
        }else if (requestCode== Constant.RequestCode_Scan_MAC_Address){
            if (resultCode==RESULT_OK){
                String qcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                if (StringUtils.isEmpty(qcode)){
                    showMsg(2,HJBleApplication.getAppContext().getString(R.string.not_qcode_info));
                }else if (qcode.length()!=12){
                    showMsg(2,HJBleApplication.getAppContext().getString(R.string.not_lh_qcode));
                     }else {
                    new HitiStringDailog(ScanBleActivity.this, Constant.Dailog_BLE_MACAddress,qcode,HJBleApplication.getAppContext().getString(R.string.hiti_macaddress_title),HJBleApplication.getAppContext().getString(R.string.hiti_macaddress_context_0)+StringUtils.getNewBlueToothMacAddress(qcode)+HJBleApplication.getAppContext().getString(R.string.hiti_macaddress_context_1),ScanBleActivity.this);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 扫描回调
    ScanCallbackCompat scanCallback = new ScanCallbackCompat() {
        @Override
        public void onBatchScanResults(List<ScanResultCompat> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(final int errorCode) {
            super.onScanFailed(errorCode);

            scanLeDevice(false);

            if (errorCode == SCAN_FAILED_LOCATION_CLOSE){
                showMsg(1, HJBleApplication.getAppContext().getString(R.string.gps_close_tips));
             }else if(errorCode == SCAN_FAILED_LOCATION_PERMISSION_FORBID){
                showMsg(1,HJBleApplication.getAppContext().getString(R.string.not_location_permission));
                 }else if (errorCode==SCAN_FAILED_APPLICATION_REGISTRATION_FAILED){
                showMsg(1,HJBleApplication.getAppContext().getString(R.string.connect_ble_to_more));

            }else{
                showMsg(1,HJBleApplication.getAppContext().getString(R.string.unknown_error)+"，"+errorCode);
            }

        }

        @Override
        public void onScanResult(int callbackType, ScanResultCompat result) {
            super.onScanResult(callbackType, result);
            byte[] scanRecord = result.getScanRecord().getBytes();

            boolean isEasy = false;
            boolean isConfig = false;
            int maxLen = 255;

            // 是否扫描过滤
            if (SCAN_FILTER) {

                //02010603035869
                if(scanRecord.length >= 7 && scanRecord[0] == 0x02 && scanRecord[1] == 0x01 &&
                        scanRecord[2] == 0x06 && scanRecord[3] == 0x03 && scanRecord[4] == 0x03 &&
                        scanRecord[5] == 0x58 && scanRecord[6] == 0x69) {
                    isEasy = false;
                    maxLen = 255;
                    isConfig = false;
                }else if(scanRecord.length >= 7 && scanRecord[0] == 0x02 && scanRecord[1] == 0x01 &&
                        scanRecord[2] == 0x06 && scanRecord[3] == 0x09 && scanRecord[4] == 0x03 &&
                        scanRecord[5] == 0x58 && scanRecord[6] == 0x69) {
                    isEasy = false;
                    maxLen = 255;
                    isConfig = true;
                }else if(scanRecord.length >= 7 && scanRecord[0] == 0x02 && scanRecord[1] == 0x01 &&
                        scanRecord[2] == 0x06 && scanRecord[3] == 0x09 && scanRecord[4] == 0x03 &&
                        scanRecord[5] == 0x59 && scanRecord[6] == 0x69) {
                    isEasy = true;
                    maxLen = 255;
                    isConfig = true;
                }else if(scanRecord.length >= 7 && scanRecord[0] == 0x02 && scanRecord[1] == 0x01 &&
                        scanRecord[2] == 0x06 ) {
                    isEasy = false;
                    maxLen = 255;
                    isConfig = true;
                }else {
                    Log.d(TAG, ConvertData.bytesToHexString(scanRecord, false));
                    return;
                }
            }
           // maxLen=WiseBluetoothLe.DEFALUT_BLE_SEND_DATA_LEN_MAX;
            Log.d(TAG, ConvertData.bytesToHexString(scanRecord, false));
            HJBleScanDevice scanDevice = new HJBleScanDevice();
            scanDevice.device = result.getDevice();
            scanDevice.rssi = result.getRssi();
            scanDevice.record = ConvertData.bytesToHexString(scanRecord, false);
            scanDevice.isEasy = isEasy;
            scanDevice.isConfig = isConfig;
            scanDevice.sendDataLenMax = maxLen;
            if (!StringUtils.isEmpty(searchBleName)){
                if (scanDevice.device==null)return;
                String deviceName=scanDevice.device.getName();
                String deviceAddress=scanDevice.device.getAddress();
                try {
                    if (StringUtils.isEmpty(deviceName)){
                        return;
                    }else if (StringUtils.isEmpty(deviceAddress)){
                        return;
                    }else if (!deviceName.contains(searchBleName)&&!deviceAddress.contains(searchBleName)){
                        return;
                    }
                }catch (NullPointerException exception){
                    return;
                }

            }
            mLeDeviceListAdapter.addDevice(scanDevice);
            //20221011 尝试通过降低每次扫码到新设备就刷新列表来降低错误点击
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    };

    //enable = true表示蓝牙开始扫描，否则表示停止扫描
    private void scanLeDevice(final boolean enable)
    {
        if (enable){
            mScanning = true;
            scanManager.startScanNow();	//开始蓝牙扫描

            setRightText(HJBleApplication.getAppContext().getString(R.string.stop_scan));
            scanProgress.setVisibility(View.VISIBLE);
        }else {
            //取消停止扫描的线程
            mScanning = false;
            scanManager.stopCycleScan();	//停止蓝牙扫描

            setRightText(HJBleApplication.getAppContext().getString(R.string.start_scan));
            scanProgress.setVisibility(View.INVISIBLE);
        }
    }

    //检测权限
    private boolean checkPermissions() {

        if (Build.VERSION.SDK_INT >= 23){
            boolean hasPermission = EasyPermissions.hasPermissions(this, permissionList);

            boolean bResult = isGpsProviderEnabled(this);

            if (!bResult){
                return false;
            }
            return hasPermission;
        }
        return true;

    }

    /**
     * is open gps
     * @param context
     * @return
     */
    public static boolean isGpsProviderEnabled(Context context){
        LocationManager service = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        if (service.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            return true;
        }else {
            try {
                int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
                return locationMode !=Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }
    }


    @AfterPermissionGranted(RC_PERM_CODE)
    public void onPermissionsSuccess() {
        if (checkPermissions()) {

            scanLeDevice(true);
        }

    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    @Override
    public void onRationaleAccepted(int requestCode) {

    }

    @Override
    public void onRationaleDenied(int requestCode) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onViewClick(int position, Object target, View view) {
        if (position== Constant.Dailog_BLE_MACAddress){
            String qcode= (String) target;
            scanLeDevice(false);
            loadingDialog.setMessage(HJBleApplication.getAppContext().getString(R.string.connecting));
            loadingDialog.show();
            connectBleMac(StringUtils.getNewBlueName(qcode),StringUtils.getNewBlueToothMacAddress(qcode),255,false);
            showMsg(2,HJBleApplication.getAppContext().getString(R.string.connecting));

        }else if (position== Constant.Dailog_BLE_Search){
            searchBleName = (String) target;
            if (StringUtils.isEmpty(searchBleName)){
                tvBleSearch.setText("");
                searchBleName="";
            }else {
                tvBleSearch.setText(HJBleApplication.getAppContext().getString(R.string.filter)+ searchBleName);
            }

            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
            scanLeDevice(true);
        }else if (position==Constant.Dailog_INSTALL_APP_Hit){
            Uri packageURI = Uri.parse("package:" + getPackageName());
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);

            startActivityForResult(intent, Constant.RequestCode_INSTALL_APP);
        }else if (position==Constant.Hiti_Version_Dialog){

        }else if (position==Constant.HitiStrTargetDialog_Connect_Fail){

        }
    }

    @Override
    public void onItemClick3(View v, int position, Object object, int type) {
        switch (type){
            case Constant.Dialog_Select_CYX_Version:
                if (position==0||position==1){
                    JSONObject data= (JSONObject) object;
                    Intent intent= (Intent) data.getObject("MyObject",Intent.class);
                    startActivity(intent);
                }
                break;
        }
    }

    private class HJBleScanDevice implements Comparable<HJBleScanDevice> {
        // 蓝牙设备
        BluetoothDevice device;
        // 信号强度
        int rssi;
        // 广播数据
        String record;
        // 是否为简易模式
        boolean isEasy;
        // 是否支持配置
        boolean isConfig;
        // 最大发送数据长度
        int sendDataLenMax;

        // 时间
        Time time;

        @Override
        public int compareTo(HJBleScanDevice o) {

            // 降序
            return o.rssi - this.rssi;
        }
    }
    WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
        @Override
        public void OnWiseBluetoothState(int state) {

            if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED) {
                if (loadingDialog != null) loadingDialog.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        showMsg(1,"蓝牙已断开");
                    }
                });
            }

        }

        @Override
        public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {

        }

        @Override
        public void OnWiseBluetoothConnectStateCode(int state, String stateMsg) {
            if (!StringUtils.isEmpty(stateMsg)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                         showMsg(2,stateMsg);
                    }
                });
            }
        }
    };

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private ArrayList<HJBleScanDevice> mScanDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(Context context)
        {
            super();
            mScanDevices = new ArrayList<>();
            mInflator = LayoutInflater.from(context);
        }

        public void addDevice(HJBleScanDevice scanDevice)
        {
            boolean isAdd = true;

            for (int i=0; i<mScanDevices.size(); i++) {
                if (mScanDevices.get(i).device.getAddress().equals(scanDevice.device.getAddress())) {
                    scanDevice.time = new Time();
                    scanDevice.time.setToNow();
                    mScanDevices.set(i, scanDevice);
                    isAdd = false;
                    break;
                }
            }

            if (isAdd) {
                scanDevice.time = new Time();
                scanDevice.time.setToNow();
                mScanDevices.add(scanDevice);
            }
        }

        public void sort() {
            Collections.sort(mScanDevices);
        }

        public BluetoothDevice getDevice(int position){
            return mScanDevices.get(position).device;
        }

        public HJBleScanDevice getScanDeviceInfo(int position){
            return mScanDevices.get(position);
        }


        public void clear() {
            mScanDevices.clear();
        }

        @Override
        public int getCount()
        {
            return mScanDevices.size();
        }

        @Override
        public Object getItem(int i)
        {
            return mScanDevices.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            ViewHolder viewHolder;
            if (view == null){
//                view = mInflator.inflate(R.layout.activity_main, null);
                view = mInflator.inflate(R.layout.list, null);
                viewHolder = new ViewHolder();
                viewHolder.itemBgLayout = view.findViewById(R.id.item_bg);
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRecord = (TextView)view.findViewById(R.id.device_record);
                viewHolder.deviceTime = (TextView)view.findViewById(R.id.device_time);
                viewHolder.imageView = view.findViewById(R.id.icon);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            HJBleScanDevice scanDevice = mScanDevices.get(i);
            final String deviceName = scanDevice.device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(HJBleApplication.getAppContext().getString(R.string.unknown_device));

            viewHolder.deviceAddress.setText("address:"+scanDevice.device.getAddress() + "     RSSI:"+scanDevice.rssi+"dB");
            viewHolder.deviceRecord.setText("broadcast:"+scanDevice.record);
            String timeStr = "time: " + scanDevice.time.year + "-" + scanDevice.time.month + "-" + scanDevice.time.monthDay + " " +
                    scanDevice.time.hour + ":" + scanDevice.time.minute + ":" + scanDevice.time.second;
            viewHolder.deviceTime.setText(timeStr);

            if (!scanDevice.isEasy) {
//                viewHolder.imageView.setVisibility(View.GONE);
                viewHolder.itemBgLayout.setBackgroundColor(Color.parseColor("#ffffff"));
                viewHolder.deviceName.setTextColor(Color.parseColor("#333333"));
                viewHolder.deviceAddress.setTextColor(Color.parseColor("#333333"));
                viewHolder.deviceRecord.setTextColor(Color.parseColor("#333333"));
                viewHolder.deviceTime.setTextColor(Color.parseColor("#333333"));
            }
            else {
//                viewHolder.imageView.setVisibility(View.VISIBLE);
                viewHolder.itemBgLayout.setBackgroundColor(Color.parseColor("#1FA7D3"));
                viewHolder.deviceName.setTextColor(Color.WHITE);
                viewHolder.deviceAddress.setTextColor(Color.WHITE);
                viewHolder.deviceRecord.setTextColor(Color.WHITE);
                viewHolder.deviceTime.setTextColor(Color.WHITE);
            }

            return view;
        }
    }

    static class ViewHolder
    {
        LinearLayout itemBgLayout;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRecord;
        TextView deviceTime;
        ImageView imageView;
    }
}
