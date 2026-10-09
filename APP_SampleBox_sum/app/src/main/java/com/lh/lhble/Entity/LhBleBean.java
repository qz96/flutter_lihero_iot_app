package com.lh.lhble.Entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.lh.lhble.Utils.StringUtils;

public class LhBleBean implements Parcelable {
    int bottle;//序号
    String macStart2;//蓝牙码开头前两位
    String BlueMac;//蓝牙通信MAC地址
    int bleUpgradeState;//蓝牙升级情况 0-未开始 1-进行升级中 2-升级完成 3-已是最新版本 4-连接失败 5-升级失败 6-蓝牙瓶Mac缺失 7-返回超时

    public int getBottle() {
        return bottle;
    }

    public void setBottle(int bottle) {
        this.bottle = bottle;
    }

    public String getMacStart2() {
        return macStart2;
    }

    public void setMacStart2(String macStart2) {
        this.macStart2 = macStart2;
    }

    public String getBlueMac() {
        return BlueMac;
    }

    public void setBlueMac(String blueMac) {
        BlueMac = blueMac;
    }

    public int getBleUpgradeState() {
        return bleUpgradeState;
    }

    public void setBleUpgradeState(int bleUpgradeState) {
        this.bleUpgradeState = bleUpgradeState;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.bottle);
        dest.writeString(this.macStart2);
        dest.writeString(this.BlueMac);
        dest.writeInt(this.bleUpgradeState);
    }

    public LhBleBean() {
    }

    protected LhBleBean(Parcel in) {
        this.bottle = in.readInt();
        this.macStart2 = in.readString();
        this.BlueMac = in.readString();
        this.bleUpgradeState = in.readInt();
    }

    public static final Creator<LhBleBean> CREATOR = new Creator<LhBleBean>() {
        @Override
        public LhBleBean createFromParcel(Parcel source) {
            return new LhBleBean(source);
        }

        @Override
        public LhBleBean[] newArray(int size) {
            return new LhBleBean[size];
        }
    };
}
