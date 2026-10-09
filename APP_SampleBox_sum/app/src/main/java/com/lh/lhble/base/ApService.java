package com.lh.lhble.base;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ApService extends Service {
    private ApServiceBinder mBinder=new ApServiceBinder();
    private WifiManager manager;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (manager==null){
            manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    String ssid = reservation.getWifiConfiguration().SSID;
                    String pwd = reservation.getWifiConfiguration().preSharedKey;
                    System.out.println("SSID:"+ssid+" PreSharedKey"+pwd);
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    System.out.println("app开启热点：热点已关闭");
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    System.out.println("app开启热点：热点开启失败");
                }
            }, null);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        closeAp();
        super.onDestroy();
    }

    private void closeAp(){
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Method method = wifiManager.getClass().getMethod("cancelLocalOnlyHotspotRequest");
            method.setAccessible(true);
            method.invoke(wifiManager);
            System.out.println("app开启热点：热点已关闭");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    //用于Activity和Service通讯
    public class ApServiceBinder extends Binder {

        public ApService getService() {
            return ApService.this;
        }

    }

}
