package com.lh.lhble.Entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class T_BottleState implements Parcelable {

    String VSamGUID;// 智能终端采样唯一ID
    String BottleID;// 瓶子条码唯一标识号
    int BottleType;// 0 普通主瓶子，1 备份瓶
    int Bottle;// 瓶号 1~6 or 1~12 瓶子在柜子里面的位置号
    int RealNum;//实际采样次数
    int StateDen;// 总采样次数要求
    String StartTime;// 开始时间
    String EndTime;// 结束时间
    String SamMode;// 采样模式
    String SamFlag;// 采样状态标识 0=进行中，1=正常完成，2=异常结束，3=干涉终止
    String IsBackup;// 是否有备份
    String BlueMac;//瓶子MAC地址
    int bleUpgradeState;//蓝牙瓶升级情况 0-未开始 1-进行升级中 2-升级完成 3-已是最新版本 4-连接失败 5-升级失败 6-蓝牙瓶Mac缺失

    public String getVSamGUID() {
        return VSamGUID;
    }

    public void setVSamGUID(String VSamGUID) {
        this.VSamGUID = VSamGUID;
    }

    public String getBottleID() {
        return BottleID;
    }

    public void setBottleID(String bottleID) {
        BottleID = bottleID;
    }

    public int getBottleType() {
        return BottleType;
    }

    public void setBottleType(int bottleType) {
        BottleType = bottleType;
    }

    public int getBottle() {
        return Bottle;
    }

    public void setBottle(int bottle) {
        Bottle = bottle;
    }

    public int getRealNum() {
        return RealNum;
    }

    public void setRealNum(int realNum) {
        RealNum = realNum;
    }

    public int getStateDen() {
        return StateDen;
    }

    public void setStateDen(int stateDen) {
        StateDen = stateDen;
    }

    public String getStartTime() {
        return StartTime;
    }

    public void setStartTime(String startTime) {
        StartTime = startTime;
    }

    public String getEndTime() {
        return EndTime;
    }

    public void setEndTime(String endTime) {
        EndTime = endTime;
    }

    public String getSamMode() {
        return SamMode;
    }

    public void setSamMode(String samMode) {
        SamMode = samMode;
    }

    public String getSamFlag() {
        return SamFlag;
    }

    public void setSamFlag(String samFlag) {
        SamFlag = samFlag;
    }

    public String getIsBackup() {
        return IsBackup;
    }

    public void setIsBackup(String isBackup) {
        IsBackup = isBackup;
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
        dest.writeString(this.VSamGUID);
        dest.writeString(this.BottleID);
        dest.writeInt(this.BottleType);
        dest.writeInt(this.Bottle);
        dest.writeInt(this.RealNum);
        dest.writeInt(this.StateDen);
        dest.writeString(this.StartTime);
        dest.writeString(this.EndTime);
        dest.writeString(this.SamMode);
        dest.writeString(this.SamFlag);
        dest.writeString(this.IsBackup);
        dest.writeString(this.BlueMac);
        dest.writeInt(this.bleUpgradeState);
    }

    public T_BottleState() {
    }

    protected T_BottleState(Parcel in) {
        this.VSamGUID = in.readString();
        this.BottleID = in.readString();
        this.BottleType = in.readInt();
        this.Bottle = in.readInt();
        this.RealNum = in.readInt();
        this.StateDen = in.readInt();
        this.StartTime = in.readString();
        this.EndTime = in.readString();
        this.SamMode = in.readString();
        this.SamFlag = in.readString();
        this.IsBackup = in.readString();
        this.BlueMac = in.readString();
        this.bleUpgradeState = in.readInt();
    }

    public static final Creator<T_BottleState> CREATOR = new Creator<T_BottleState>() {
        @Override
        public T_BottleState createFromParcel(Parcel source) {
            return new T_BottleState(source);
        }

        @Override
        public T_BottleState[] newArray(int size) {
            return new T_BottleState[size];
        }
    };
}
