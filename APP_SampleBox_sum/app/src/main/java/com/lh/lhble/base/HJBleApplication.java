package com.lh.lhble.base;

import android.app.Application;
import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lh.lhble.Entity.User;
import com.lh.lhble.Utils.SharedPreferencesUtils;
import com.lh.lhble.Utils.UmengHelper;
import com.umeng.commonsdk.UMConfigure;
import com.wise.ble.WiseBluetoothLe;
import com.wise.wisekit.utils.SPUtils;

public class HJBleApplication extends Application {

    private static final String TAG = HJBleApplication.class.getName();
    public final String bleID="12345678";
    private  static HJBleApplication instance;
    private static SharedPreferencesUtils shared = null;
    public static User user;
    // 是否为配置模式
    private int isBleConfig = 0;
    //当前连接的蓝牙设备通信认证ID
    private String desID;
    //获取应用的context
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        shared = new SharedPreferencesUtils(this, Constant.APP_Share_Tag);
        WiseBluetoothLe.getInstance(getAppContext());
        user = JSONObject.parseObject(HJBleApplication.getShared().getString(Constant.APP_USER_INFO, ""), User.class);
        getUser();
        //日志开关
        UMConfigure.setLogEnabled(false);
        //预初始化
        UmengHelper.preInit(this);
        //正式初始化
        initUmengSDK();
    }

    public static User getUser() {
        if (user == null) {
            user = getInstance();
        }

        return user;
    }

    private static User getInstance() {
        if (user == null) {
            user = new User();
        }
        return user;
    }

    public static SharedPreferencesUtils getShared() {

        return shared;
    }

    /**
     * 同步用户信息并保存（如有值）
     */
    public static User syscUserInfo() {
        if (user != null) {
            HJBleApplication.getShared().putString(Constant.APP_USER_INFO, JSON.toJSONString(user));
        }
        return user;
    }

    public static HJBleApplication shareInstance() {
        return instance;
    }

    public int isBleConfig() {
        return isBleConfig;
    }

    public String getDesID() {
        return desID;
    }

    public void setDesID(String desID) {
        this.desID = desID;
    }

    public void setBleConfig(int bleConfig) {
        isBleConfig = bleConfig;
    }

    // 是否为十六进制模式
    public boolean isBleHex() {

        Boolean hex = (Boolean)SPUtils.get(getAppContext(), "isBleHex", false);
        return hex.booleanValue();
    }

    public void setBleHex(boolean bleHex) {

        SPUtils.put(getAppContext(), "isBleHex", bleHex);
    }
    // 是否为十六进制模式
    public boolean isEncrypt() {

        Boolean isEncrypt = (Boolean)SPUtils.get(getAppContext(), "isEncrypt", true);
        return isEncrypt.booleanValue();
    }

    public void setIsEncrypt(boolean isEncrypt){
        SPUtils.put(getAppContext(), "isEncrypt", isEncrypt);
    }

    // 是否在发送数据后面自动加回车
    public boolean isAddReturn() {

        Boolean addReturn = (Boolean)SPUtils.get(getAppContext(), "isAddReturn", true);
        return addReturn.booleanValue();
    }

    public void setAddReturn(boolean addReturn) {

        SPUtils.put(getAppContext(), "isAddReturn", addReturn);
    }

    /**
     * 初始化推送SDK，在用户隐私政策协议同意后，再做初始化
     */
    private void initUmengSDK() {
        /*
         * 判断用户是否已同意隐私政策
         * 当同意时，直接进行初始化；
         * 当未同意时，待用户同意后，通过PushHelper.init(...)方法进行初始化。
         */
        boolean agreed = true;
        if (agreed && UmengHelper.isMainProcess(this)) {
            //建议在线程中执行初始化
            new Thread(new Runnable() {
                @Override
                public void run() {
                    UmengHelper.init(getApplicationContext());
                }
            }).start();
        }
    }
}
