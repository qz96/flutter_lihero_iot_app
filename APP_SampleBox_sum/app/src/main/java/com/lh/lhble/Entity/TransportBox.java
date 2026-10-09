package com.lh.lhble.Entity;

public class TransportBox {
    String blueMac;//蓝牙通讯地址
    String version;
    int devStatus;//0-正常，1-工作中，2采样故障，3-流量故障，4-网络故障，5-GPS故障，6-高温报警，7-电池电量低
    int lockStatue;//0关闭，1打开
    String sysTime;//系统时间
    double boxTemp;//箱内温度
    String transportNum;//运单号
    int  bottleCount;//箱内样品数量
    String devEV;//设备电池电压
    String rssi;//信号强度
    double lng;//经度
    double lat;//纬度
    String MN;//运输箱MN
    int updateTime;//上报时间间隔(单位秒), 0-不上报,间隔时长最小15秒
    String iccID;//物联网卡号
    int workMode=1;//1-工作模式 0-节能模式

    public String getBlueMac() {
        return blueMac;
    }

    public void setBlueMac(String blueMac) {
        this.blueMac = blueMac;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getDevStatus() {
        return devStatus;
    }

    public void setDevStatus(int devStatus) {
        this.devStatus = devStatus;
    }

    public int getLockStatue() {
        return lockStatue;
    }

    public void setLockStatue(int lockStatue) {
        this.lockStatue = lockStatue;
    }

    public String getSysTime() {
        return sysTime;
    }

    public void setSysTime(String sysTime) {
        this.sysTime = sysTime;
    }

    public double getBoxTemp() {
        return boxTemp;
    }

    public void setBoxTemp(double boxTemp) {
        this.boxTemp = boxTemp;
    }

    public String getTransportNum() {
        return transportNum;
    }

    public void setTransportNum(String transportNum) {
        this.transportNum = transportNum;
    }

    public int getBottleCount() {
        return bottleCount;
    }

    public void setBottleCount(int bottleCount) {
        this.bottleCount = bottleCount;
    }

    public String getDevEV() {
        return devEV;
    }

    public void setDevEV(String devEV) {
        this.devEV = devEV;
    }

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public String getMN() {
        return MN;
    }

    public void setMN(String MN) {
        this.MN = MN;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public String getIccID() {
        return iccID;
    }

    public void setIccID(String iccID) {
        this.iccID = iccID;
    }

    public int getWorkMode() {
        return workMode;
    }

    public void setWorkMode(int workMode) {
        this.workMode = workMode;
    }
}
