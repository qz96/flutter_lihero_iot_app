package com.lh.lhble.UI;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alibaba.fastjson.JSON;
import com.goodix.ble.gr.toolbox.app.libfastdfu.DfuProgressCallback;
import com.goodix.ble.gr.toolbox.app.libfastdfu.EasyDfu2;
import com.lh.lhble.Entity.LhBleBean;
import com.lh.lhble.Entity.Result;
import com.lh.lhble.R;
import com.lh.lhble.Utils.ProtocolUtil;
import com.lh.lhble.Utils.RxTimerUtil;
import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.UI.View.HitiStringDailog;
import com.lh.lhble.adapter.UpgradeLhBleDevAdapter;
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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.lh.lhble.google.zxing.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;


public class BlueOTAActivity extends BaseActivity implements CommontClickListener, View.OnClickListener, AdapterView.OnItemClickListener, DfuProgressCallback {
    private final static String TAG = BlueOTAActivity.class.getSimpleName();
    private List<String> macList;
    //蓝牙设备的Mac地址
    public List<LhBleBean> bleBeans;
    public LhBleBean currentBean;
    private WiseBluetoothLe mble;
    private Context context;
    private TextView tvBleBin;
    private SwipeRefreshLayout srlBottles;
    private ListView lvLhBleDevs;
    private Button btUpgradeAll,btUpgradeResidue;
    private UpgradeLhBleDevAdapter mAdapter;
    private Handler mHandler;
    public static final int RequestCode_System_Doc=1002;
    // 发送服务
    private WiseCharacteristic mSendCharact = Constant.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = Constant.Ble_Data_Receive_Service;
    private static final int MSG_DATA_CHANGE = 0x11;
    private String sampleId;
    private String bleVersion;
    private Uri fileUri;//目标升级文件的Uri
    private String fileName;
    private int fileCode;
    private BluetoothDevice bluetoothDevice;
    //蓝牙升级服务
    private EasyDfu2 dfu2;
    private InputStream r;
    private LoadingDialog loadingDialog = null;
    private int upgradeSuccess;
    private int upgradeFail;
    private int upgradeDisConnect;
    String devMacStart;


    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_blue_ota;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        macList=intent.getStringArrayListExtra("MacList");
        bleVersion=intent.getStringExtra("BleVersion");
        bleBeans=new ArrayList<>();
        if (macList!=null){
            for (int i=0;i<macList.size();i++){
                LhBleBean bean=new LhBleBean();
                bean.setBottle(i+1);
                bean.setBlueMac(macList.get(i));
                bleBeans.add(bean);
            }
            mAdapter.setDatas(bleBeans);
            if (bleBeans.size()>0) {
                devMacStart=bleBeans.get(0).getBlueMac().substring(0,2);
            }
        }

        topLeftBtn.setVisibility(View.VISIBLE);

        setRightText("扫码添加");
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bleBeans!=null&&bleBeans.size()>=20){
                    showMsg(2,"单次批量升级上限：20个");
                }else {
                    if (!StringUtils.isEmpty(devMacStart)&&("0A".equals(devMacStart)||"0B".equals(devMacStart))){
                        showMsg(2,"请扫描蓝牙瓶盖二维码");
                    }else {
                        showMsg(2,"请扫描蓝牙设备二维码");

                    }

                    startActivityForResult(new Intent(BlueOTAActivity.this, CaptureActivity.class), Constant.RequestCode_Scan_MAC_Address);

                }

            }
        });
        context = this;
        mble = WiseBluetoothLe.getInstance(getApplicationContext());    //获取蓝牙实例
        mble.mBleCallBack = bleCallBack;

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        int color = msg.arg1;
                        String strData = (String) msg.obj;
                        if (msg.arg2 > 42 && strData.contains("#") && strData.split("#", -1).length >= 7) {
                            setReadDetailData(strData);
                        }else if (msg.arg2==26&&strData.contains("Write SysTime=")&&color==Color.rgb(139, 0, 255)){
                            mble.disconnectDevice();
                            showMsg(0,"设备时间校准成功");
                            RxTimerUtil.cancel();
                            RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                                @Override
                                public void doNext(long number) {
                                    startUpgrade(currentBean);
                                }
                            });
                        }else if ("蓝牙已断开".equals(strData)){
                            showMsg(2,"蓝牙已断开");
                            finish();
                        }else {

                            showMsg(2,"返回："+strData);
                        }


                        break;

                    default:
                        break;
                }

                return false;
            }
        });


    }

    private void checkNewBlueBin() {
        showMsg(2,"请选择您的蓝牙程序升级文件(.bin)");
        Intent intent =new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*") ;
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        BlueOTAActivity.this.startActivityForResult(Intent.createChooser(intent, "选择蓝牙升级文件"), RequestCode_System_Doc);
    }

    private void setReadDetailData(String strData) {
        String[] details = strData.split("#", -1);
        switch (devMacStart){
            case "0A":
            case "0B":
                if (details.length>5){
                    sampleId = details[4];
                    bleVersion = details[5];
                    bleUpgrade();
                }
                break;

            case "0D":
                if (details.length >= 18) {
                    sampleId = details[10];
                    bleVersion = details[7];
                    bleUpgrade();
                }
                break;
        }



    }

    private void bleUpgrade() {
        if ((fileCode<=0||Integer.parseInt(bleVersion) <= fileCode) ) {
            bluetoothDevice = mble.getGatt().getDevice();
            mble.disconnectDevice();
            RxTimerUtil.cancel();
            if (currentBean!=null&&Integer.parseInt(bleVersion)>200000){
                currentBean.setBleUpgradeState(Integer.parseInt(bleVersion));
                mAdapter.notifyDataSetChanged();
            }
            RxTimerUtil.timer(5100L, new RxTimerUtil.IRxNext() {//20230110 陈翔 5100+5000=100+10000,确保避开蓝牙程序的休眠最大时长
                @Override
                public void doNext(long number) {
                    try {

                        r = null;
                        ContentResolver resolver = BlueOTAActivity.this.getContentResolver();
                        r = resolver.openInputStream(fileUri);
                        if (dfu2.startDfuInCopyMode(BlueOTAActivity.this, bluetoothDevice, r, 0x01040000)){
                            showMsg(2,"开始蓝牙程序升级...(请勿中途退出！)");
                        }else {

                            showMsg(1,"蓝牙升级服务开启失败");
                            currentBean.setBleUpgradeState(5);
                            mAdapter.notifyDataSetChanged();
                            startUpgrade(currentBean);
                        }
                    }catch (FileNotFoundException e) {
                        e.printStackTrace();
                        showMsg(2,"未读取到升级程序文件");
                        currentBean.setBleUpgradeState(5);
                        mAdapter.notifyDataSetChanged();
                        startUpgrade(currentBean);
                    }
                }
            });
        }else if (fileCode>0||Integer.parseInt(bleVersion) >= fileCode){
            if (currentBean!=null) {
                currentBean.setBleUpgradeState(3);
            }
            mAdapter.notifyDataSetChanged();
            startUpgrade(currentBean);
        }
    }

    public static Intent getIntent(Context context, ArrayList<String> macList,String bleVersion) {
        Intent intent = new Intent(context, BlueOTAActivity.class);
        intent.putStringArrayListExtra("MacList",macList);
        intent.putExtra("BleVersion",bleVersion);
        return intent;
    }

    @Override
    protected void initView() {
        super.initView();

        tvBleBin= findViewById(R.id.tv_ble_bin);
        srlBottles=findViewById(R.id.srl_sample_bottles);
        lvLhBleDevs=findViewById(R.id.lv_lh_ble_dev);
        btUpgradeAll=findViewById(R.id.bt_upgrade_all);
        btUpgradeResidue=findViewById(R.id.bt_upgrade_residue);
        mAdapter=new UpgradeLhBleDevAdapter(BlueOTAActivity.this);
        lvLhBleDevs.setAdapter(mAdapter);
        tvBleBin.setOnClickListener(this);
        btUpgradeAll.setOnClickListener(this);
        btUpgradeResidue.setOnClickListener(this);
        lvLhBleDevs.setOnItemClickListener(BlueOTAActivity.this);
        dfu2 = new EasyDfu2();
        dfu2.setListener(BlueOTAActivity.this);

    }
    private boolean initBlueTooth() {
        if (!mble.isBleSupported() || !mble.connectLocalDevice()) {
            showMsg(2,"本设备未开通蓝牙服务");
        }


        //判断本地蓝牙是否已打开
        if (!mble.isOpened()) {
            Intent openIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(openIntent, Constant.RequestCode_BlueTooth_Open);
            return false;
        }
        return true;

    }


    private void startUpgrade(LhBleBean lastBean) {
        if (loadingDialog!=null&&loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        int i=0;
        if (bleBeans==null||bleBeans.size()<=0){

            return;
        }else if (lastBean!=null&&bleBeans.contains(lastBean)){
            i=bleBeans.indexOf(lastBean)+1;
        }
        if (bleBeans.size()<=i){
            upgradeSuccess = 0;
            upgradeFail = 0;
            upgradeDisConnect = 0;
            for (LhBleBean bean:bleBeans){
                if (bean.getBleUpgradeState() == 2||bean.getBleUpgradeState() == 3){
                    upgradeSuccess++;
                }else if (bean.getBleUpgradeState()!=2&&bean.getBleUpgradeState()!=3&&bean.getBleUpgradeState()>0){
                    upgradeFail++;
                    if (bean.getBleUpgradeState()==4) {
                        upgradeDisConnect++;
                    }
                }
            }
            if (upgradeDisConnect >2){
                showMsg(2,"手机蓝牙服务过载，请在‘手机设置’中重启蓝牙功能后再试~");
            }
            new HitiStringDailog(BlueOTAActivity.this,Constant.HitiStrTargetDialog_Upgrade_LHBLEList_Result,null,"蓝牙升级结果确认","本轮蓝牙升级，已升为最新版本"+ upgradeSuccess +"个，升级失败"+ upgradeFail +"个",BlueOTAActivity.this);
            btUpgradeAll.setVisibility(View.GONE);
            rightTitleTxt.setVisibility(View.GONE);
            btUpgradeResidue.setVisibility(upgradeFail>0?View.VISIBLE:View.GONE);
            currentBean=null;
            return;
        }

        for (int j=i;j<bleBeans.size();j++) {
            LhBleBean bleBean=bleBeans.get(j);
            if (bleBean.getBleUpgradeState() != 2 && bleBean.getBleUpgradeState() != 3) {
                currentBean = bleBean;
                String MacAddress = currentBean.getBlueMac();
                if (MacAddress.trim().length() == 17 && (MacAddress.startsWith("0A") || MacAddress.startsWith("0B") || MacAddress.startsWith("0C")|| MacAddress.startsWith("0D"))) {
                    connectBle(MacAddress, 255, false);
                } else if (MacAddress.trim().length() == 12 && (MacAddress.startsWith("0A") || MacAddress.startsWith("0B") || MacAddress.startsWith("0C")|| MacAddress.startsWith("0D"))) {
                    MacAddress = StringUtils.getNewBlueToothMacAddress(MacAddress);
                    connectBle(MacAddress.trim(), 255, false);
                } else {
                    showMsg(2,MacAddress + "，非力合标准码");
                    currentBean.setBleUpgradeState(6);
                    mAdapter.notifyDataSetChanged();
                    startUpgrade(currentBean);
                }
                return;
            }
        }
    }


    // 连接蓝牙
    void connectBle(final String macAddress, final int sendDataLenMax, final boolean isConfig) {
        if (loadingDialog!=null) {
            loadingDialog.dismiss();
        }
        loadingDialog=buildDialog("正在蓝牙连接...");
        if (BlueOTAActivity.this.isFinishing()){
            return;
        }
        loadingDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++) {
                    if (mble.connectDevice(macAddress,bleCallBack))    //连接蓝牙设备
                    {
                        break;
                    }
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
                            if (BlueOTAActivity.this.isFinishing()){
                                return;
                            }
                            if (loadingDialog!=null) {
                                loadingDialog.dismiss();
                            }

                            showMsg(1,"连接失败");
                            currentBean.setBleUpgradeState(4);
                            mAdapter.notifyDataSetChanged();
                            startUpgrade(currentBean);
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
                            if (BlueOTAActivity.this.isFinishing()){
                                return;
                            }
                            if (loadingDialog!=null) {
                                loadingDialog.dismiss();
                            }
                            showMsg(1,"连接失败1");
                            currentBean.setBleUpgradeState(4);
                            mAdapter.notifyDataSetChanged();
                            startUpgrade(currentBean);
                        }
                    });
                    return;
                }


                if (mble.openNotify(Constant.Ble_Data_Receive_Service)) {
                    if (mble.requestMtu(sendDataLenMax)) {
                        mble.setSendDataLenMax(sendDataLenMax);
                        // mble.mBleCallBack = bleCallBack;
                        // refreshBottleInfo();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (BlueOTAActivity.this.isFinishing()){
                                    return;
                                }
                                if (loadingDialog!=null) {
                                    loadingDialog.dismiss();
                                }

                                HJBleApplication.shareInstance().setDesID("12345678");
                                if (currentBean!=null&&currentBean.getBleUpgradeState()==2){
                                    showMsg(2,"校准设备系统时间");
                                    RxTimerUtil.cancel();
                                    RxTimerUtil.timer(500, new RxTimerUtil.IRxNext() {
                                        @Override
                                        public void doNext(long number) {
                                            sendLHBleData("Write SysTime=" + DateUtils.formatTime( DateUtils.long2time(System.currentTimeMillis()), DateUtils.DATE_TIME_PATTERN, DateUtils.METHOD_DATE_TIME_PATTERN).substring(2));

                                        }
                                    });
                                  }else if (fileUri!=null){
                                    showMsg(0,"蓝牙连接成功");
                                    if (BlueOTAActivity.this.isFinishing()){
                                        return;
                                    }
                                    loadingDialog=buildDialog("正在获取当前设备信息...");
                                    loadingDialog.show();
                                    RxTimerUtil.cancel();
                                    RxTimerUtil.timer(500L, new RxTimerUtil.IRxNext() {
                                        @Override
                                        public void doNext(long number) {
                                            sendLHBleData("Read Detail");
                                        }
                                    });
                                }else {
                                    showMsg(2,"请先选择蓝牙程序升级文件（.bin）");
                                }

                            }
                        });

                        return;
                    } else {
                        if (mble.getGatt()!=null){
                            mble.getGatt().disconnect();
                            mble.getGatt().close();
                        }
                        mble.disconnectDevice();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (BlueOTAActivity.this.isFinishing()){
                                    return;
                                }
                                if (loadingDialog!=null) {
                                    loadingDialog.dismiss();
                                }
                                showMsg(1,"连接失败3,设置MTU失败");
                                if (currentBean!=null){
                                    currentBean.setBleUpgradeState(4);
                                    mAdapter.notifyDataSetChanged();
                                    startUpgrade(currentBean);
                                }else {
                                    mAdapter.notifyDataSetChanged();
                                }

                            }
                        });
                    }

                } else {
                    mble.disconnectDevice();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (BlueOTAActivity.this.isFinishing()){
                                return;
                            }
                            if (loadingDialog!=null) {
                                loadingDialog.dismiss();
                            }
                            showMsg(1,"连接失败2");
                            currentBean.setBleUpgradeState(4);
                            mAdapter.notifyDataSetChanged();
                            startUpgrade(currentBean);
                        }
                    });
                    return;
                }
            }
        }).start();
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
    protected void onResume() {
        super.onResume();
        setTitle("蓝牙程序升级");
        initBlueTooth();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    //发送数据
    void sendLHBleData(String sendString) {
        byte[] tmpBytes;
        if (sendString == null || sendString.isEmpty()) {
            showMsg(2,"发送数据内容不能为空");
            return;
        }

        tmpBytes = ProtocolUtil.getBlueToothMsgForSend(HJBleApplication.shareInstance().getDesID(), sendString);

        if (tmpBytes == null) {
            return;
        }else  if (mble == null || !mble.isConnect()) {
            showMsg(2,"蓝牙连接已中断，请返回重试");
            finish();
            return;
        }

        addLogText("发送：\r\n      " + sendString, Color.BLUE, sendString.length());
        insertLog(currentBean==null?"":currentBean.getBlueMac(), sampleId, sendString);
        final byte[] bytes = tmpBytes;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mble.sendData(mSendCharact, bytes)) {
                    addLogText("发送失败！", Color.RED, 0);
                }

            }
        }).start();

    }



    void addLogText(final String log, final int color, int byteLen) {
        Message message = new Message();
        message.what = MSG_DATA_CHANGE;
        message.arg1 = color;
        message.arg2 = byteLen;
        message.obj = log;
        mHandler.sendMessage(message);
    }

    WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
        @Override
        public void OnWiseBluetoothState(int state) {

            if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED&&currentBean==null) {
                addLogText("蓝牙已断开", Color.BLACK, 0);
            }

        }

        @Override
        public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {

            if (characteristic.equals(mReceiveCharact)) {

                String str = "";
                if (HJBleApplication.shareInstance().isBleHex()) {
                    str = ConvertData.bytesToHexString(recvData, false);
                } else if (HJBleApplication.shareInstance().isBleConfig() == 0) {
                    str = ProtocolUtil.getReturnMsgForReceive(recvData);
                } else {
                    str = new String(recvData);
                }
                addLogText("接收：\r\n      " + str, Color.rgb(139, 0, 255), str.length());
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
                        showMsg(2,stateMsg);
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position>=0&&bleBeans!=null&&bleBeans.size()>position){
            LhBleBean bean=bleBeans.get(position);
            if (bean.getBleUpgradeState()==1) {
                if (!StringUtils.isEmpty(devMacStart)&&("0A".equals(devMacStart)||"0B".equals(devMacStart))){
                    showMsg(2,"正在进行中的样品瓶，请谨慎操作~");
                }else {
                    showMsg(2,"正在进行中的蓝牙设备，请谨慎操作~");
                }
            }else if (bean.getBleUpgradeState()==2){
                if (!StringUtils.isEmpty(devMacStart)&&("0A".equals(devMacStart)||"0B".equals(devMacStart))){
                    showMsg(0,"此瓶蓝牙程序已升级完成");
                }else {
                    showMsg(0,"此设备蓝牙程序已升级完成");
                }
            }else if (bean.getBleUpgradeState()==3){
                if (!StringUtils.isEmpty(devMacStart)&&("0A".equals(devMacStart)||"0B".equals(devMacStart))){

                    showMsg(0,"此瓶蓝牙程序已是最新版");
                }else {
                    showMsg(0,"此设备蓝牙程序已是最新版");
                }
            }else if (bean.getBleUpgradeState()>200000){
                if (!StringUtils.isEmpty(devMacStart)&&("0A".equals(devMacStart)||"0B".equals(devMacStart))){
                    showMsg(2,"正在进行中的样品瓶，请谨慎操作~");
                }else {
                    showMsg(2,"正在进行中的蓝牙设备，请谨慎操作~");
                }
            }else {
                currentBean = bean;
                String MacAddress = currentBean.getBlueMac();
                if (MacAddress.trim().length() == 17 && (MacAddress.startsWith("0A") || MacAddress.startsWith("0B") || MacAddress.startsWith("0C")||MacAddress.startsWith("0D"))) {
                    connectBle(MacAddress, 255, false);
                } else if (MacAddress.trim().length() == 12 && (MacAddress.startsWith("0A") || MacAddress.startsWith("0B") || MacAddress.startsWith("0C")||MacAddress.startsWith("0D"))) {
                    MacAddress = StringUtils.getNewBlueToothMacAddress(MacAddress);
                    connectBle(MacAddress.trim(), 255, false);
                } else {
                    showMsg(2,MacAddress + "，非力合标准码");
                    currentBean.setBleUpgradeState(6);
                    mAdapter.notifyDataSetChanged();
                    startUpgrade(currentBean);
                }
                return;
            }
        }
    }

    @Override
    public void onViewClick(int position, Object target, View view) {
        switch (position) {
            case Constant.HitiStrTargetDialog_Upgrade_LHBLEList_Result:
                currentBean=null;
                if (upgradeDisConnect>2){
                    Toast.makeText(BlueOTAActivity.this,"手机蓝牙服务过载，请在‘手机设置’中关闭重启蓝牙功能后再试~",Toast.LENGTH_LONG).show();

                    Intent openIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(openIntent, Constant.RequestCode_BlueTooth_Open);
                }
                break;

            case Constant.HitiStrTargetDialog_Upgrade_LHBLE:
                startUpgrade(null);
                break;

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_ble_bin:
                if (bleBeans==null||bleBeans.size()<=0){
                    Toast.makeText(BlueOTAActivity.this,"未获取到待升级的蓝牙设备",Toast.LENGTH_SHORT).show();
                    return;
                }else if (StringUtils.isEmpty(bleBeans.get(0).getBlueMac())||bleBeans.get(0).getBlueMac().length()!=17){
                    Toast.makeText(BlueOTAActivity.this,"待升级的蓝牙设备异常,"+bleBeans.get(0).getBlueMac(),Toast.LENGTH_SHORT).show();
                    return;
                }else if (StringUtils.isEmpty(devMacStart)){
                    devMacStart=bleBeans.get(0).getBlueMac().substring(0,2);
                }
                checkNewBlueBin();
                break;
            case R.id.bt_upgrade_all:
                if (bleBeans == null || bleBeans.size() <= 0) {
                    Toast.makeText(BlueOTAActivity.this,"未获取到待升级的蓝牙设备",Toast.LENGTH_SHORT).show();
                } else if (fileUri == null) {
                    Toast.makeText(BlueOTAActivity.this,"请先选择你的蓝牙程序升级文件(.bin)",Toast.LENGTH_SHORT).show();
                }else if (currentBean!=null){
                    Toast.makeText(BlueOTAActivity.this,"正在批量升级蓝牙设备，请勿重复点击",Toast.LENGTH_SHORT).show();
                }else {
                    new HitiStringDailog(BlueOTAActivity.this,Constant.HitiStrTargetDialog_Upgrade_LHBLE,bleBeans,"开始批量蓝牙升级","升级期间请勿退出当前页面，并请勿走离。大约需要"+bleBeans.size()+"分钟",BlueOTAActivity.this);
                }
                break;
            case R.id.bt_upgrade_residue:
                if (bleBeans == null || bleBeans.size() <= 0) {
                    Toast.makeText(BlueOTAActivity.this,"未获取到待升级的蓝牙设备",Toast.LENGTH_SHORT).show();

                } else if (fileUri == null) {
                    Toast.makeText(BlueOTAActivity.this,"请先选择你的蓝牙程序升级文件(.bin)",Toast.LENGTH_SHORT).show();

                } else {
                    startUpgrade(null);
                }
                break;

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
                            e.printStackTrace();
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }

    }

    @Override
    public void onDfuStart() {
        Log.d(TAG, "EasyDfu2 onDfuStart()");
        if (loadingDialog!=null) {
            loadingDialog.dismiss();
        }
        loadingDialog=buildDialog("蓝牙升级中...");
        loadingDialog.show();
        if (currentBean!=null) {
            currentBean.setBleUpgradeState(1);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDfuProgress(int i) {
        Log.d(TAG, "EasyDfu2 onDfuProgress() " + i);
    }

    @Override
    public void onDfuComplete() {
        if (loadingDialog!=null) {
            loadingDialog.dismiss();
        }
        if (currentBean!=null){
            Toast.makeText(BlueOTAActivity.this,currentBean.getBottle()+"号蓝牙升级成功",Toast.LENGTH_LONG).show();
            currentBean.setBleUpgradeState(2);
            mAdapter.notifyDataSetChanged();
            RxTimerUtil.cancel();
            RxTimerUtil.timer(3000L, new RxTimerUtil.IRxNext() {
                @Override
                public void doNext(long number) {
                    connectBle(currentBean.getBlueMac().trim(), 255, false);
                }
            });


        }else {
            Toast.makeText(BlueOTAActivity.this,"升级成功",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDfuError(String s, Error error) {
        if (loadingDialog!=null) {
            loadingDialog.dismiss();
        }
        Log.d(TAG, "EasyDfu2 onDfuError() " + s);
        if (currentBean!=null){
            currentBean.setBleUpgradeState(5);
            mAdapter.notifyDataSetChanged();
            RxTimerUtil.cancel();
            RxTimerUtil.timer(3000, new RxTimerUtil.IRxNext() {
                @Override
                public void doNext(long number) {
                    startUpgrade(currentBean);
                }
            });
        }
    }
    @Override
    protected void onDestroy() {

        dfu2 = null;
        if (mble != null && mble.isOpened() && mble.isConnect()) {
            mble.disconnectDevice();
            mble.disconnectLocalDevice();
        }
        bleBeans=new ArrayList<>();
        Log.d(TAG, "destroy");

        Log.d(TAG, "销毁");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RequestCode_System_Doc:
                    if (data != null && data.getData() != null) {
                        Uri fileUri = data.getData();

                        if (fileUri != null) {
                            ContentResolver resolver = BlueOTAActivity.this.getContentResolver();
                            if (fileUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {

                                ContentResolver contentResolver = BlueOTAActivity.this.getContentResolver();
                                Cursor cursor = contentResolver.query(fileUri, null, null, null, null);
                                if (cursor.moveToFirst()) {
                                    String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                    if (displayName==null){
                                        displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE));
                                    }
                                    if (displayName==null){
                                        Toast.makeText(BlueOTAActivity.this,"未获取到您选的文件名",Toast.LENGTH_SHORT).show();
                                        return;
                                    }else if ("0A".equals(devMacStart)||"0B".equals(devMacStart)){
                                        if (!displayName.contains("cyq")||displayName.length()<9){
                                            Toast.makeText(BlueOTAActivity.this,"请选择正确蓝牙采样瓶程序文件(cyq)",Toast.LENGTH_SHORT).show();
                                            return;
                                        } else {

                                            if (this.fileCode<=0&&!StringUtils.isEmpty(bleVersion)&&Integer.parseInt(bleVersion)>=Integer.parseInt(displayName.substring(displayName.lastIndexOf("cyq")+3,displayName.lastIndexOf("cyq")+9))){
                                                Toast.makeText(BlueOTAActivity.this,"当前蓝牙程序已是最新版本"+bleVersion,Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            this.fileUri=fileUri;
                                            fileName=displayName;
                                            this.fileCode = Integer.parseInt(displayName.substring(displayName.lastIndexOf("cyq")+3,displayName.lastIndexOf("cyq")+9));
                                            tvBleBin.setText(fileName+"");
                                        }
                                    } else if ("0D".equals(devMacStart)){
                                        if (!displayName.contains("CYX")||displayName.length()<9){
                                            Toast.makeText(BlueOTAActivity.this,"请选择正确蓝牙采样箱程序文件(CYX)",Toast.LENGTH_SHORT).show();
                                            return;
                                        } else {
                                            if (this.fileCode<=0&&!StringUtils.isEmpty(bleVersion)&&Integer.parseInt(bleVersion)>=Integer.parseInt(displayName.substring(displayName.lastIndexOf("CYX")+3,displayName.lastIndexOf("CYX")+9))){
                                                Toast.makeText(BlueOTAActivity.this,"当前蓝牙程序已是最新版本"+bleVersion,Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            this.fileUri=fileUri;
                                            fileName=displayName;
                                            this.fileCode = Integer.parseInt(displayName.substring(displayName.lastIndexOf("CYX")+3,displayName.lastIndexOf("CYX")+9));
                                            tvBleBin.setText(fileName+"");
                                        }
                                    }
                                }
                            }


                            return;
                        } else {
                            Toast.makeText(BlueOTAActivity.this,"未识别出文件名称！",Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        Toast.makeText(BlueOTAActivity.this,"请选择设备蓝牙程序文件升级(.bin)",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constant.RequestCode_Scan_MAC_Address:
                    if (resultCode == RESULT_OK) {
                        String qcode = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                        if (StringUtils.isEmpty(qcode)) {
                            Toast.makeText(BlueOTAActivity.this, "未读取到二维码信息", Toast.LENGTH_LONG).show();
                        } else if (qcode.length() != 12) {
                            Toast.makeText(BlueOTAActivity.this, "二维码，非力合蓝牙标准码", Toast.LENGTH_LONG).show();
                        }else if (!StringUtils.isEmpty(devMacStart)&&!qcode.startsWith(devMacStart)){
                            if ("00".equals(devMacStart)){
                                Toast.makeText(BlueOTAActivity.this, "二维码，非力合灌溉农用水蓝牙标准码", Toast.LENGTH_LONG).show();
                            }else if ("0D".equals(devMacStart)){
                                Toast.makeText(BlueOTAActivity.this, "二维码，非力合采样箱蓝牙标准码", Toast.LENGTH_LONG).show();
                            }else if ("0E".equals(devMacStart)){
                                Toast.makeText(BlueOTAActivity.this, "二维码，非力合质控仪器蓝牙标准码", Toast.LENGTH_LONG).show();
                            }else if ("0A".equals(devMacStart)){
                                Toast.makeText(BlueOTAActivity.this, "二维码，非力合0A蓝牙瓶标准码", Toast.LENGTH_LONG).show();
                            }else if ("0B".equals(devMacStart)){
                                Toast.makeText(BlueOTAActivity.this, "二维码，非力合0B蓝牙瓶标准码", Toast.LENGTH_LONG).show();
                            }else if ("11".equals(devMacStart)){
                                Toast.makeText(BlueOTAActivity.this, "二维码，非力合运输箱蓝牙标准码", Toast.LENGTH_LONG).show();
                            }else {
                                Toast.makeText(BlueOTAActivity.this, "二维码，非"+devMacStart+"开头，无法一起升级", Toast.LENGTH_LONG).show();
                            }
                        }else {
                            if (StringUtils.isEmpty(devMacStart)) {
                                devMacStart=qcode.substring(0,2);
                            }
                            String macAds=StringUtils.getNewBlueToothMacAddress(qcode);
                            if (bleBeans==null) {
                                bleBeans=new ArrayList<>();
                            }
                            for (LhBleBean bean:bleBeans){
                                if (macAds.equals(bean.getBlueMac())){
                                    Toast.makeText(BlueOTAActivity.this, qcode+",请勿重复添加", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                            LhBleBean bleBean=new LhBleBean();
                            bleBean.setBottle(bleBeans.size()+1);
                            bleBean.setBlueMac(macAds);
                            bleBeans.add(bleBean);
                            mAdapter.setDatas(bleBeans);
                        }
                    }
                    break;
            }
        }
    }


}
