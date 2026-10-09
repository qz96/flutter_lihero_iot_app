package com.lh.lhble.Entity;

/**
 * 灌溉采样器
 * 220509#0#0.0#2.6#002205100000#0#0#100000#1000000#0.0#0.0#0
 */
public class GGCYBox {
    String blueMac;//蓝牙通讯地址
    String version;
    int devStatus;//0空闲，1工作中，2采样结束，3采样异常，4无信号

    double boxTemp;//箱内温度
    String devEV;//设备电池电压
    String sampleNo1;//瓶1 样品编号
    String sampleTime1;//瓶1 采样时间（未用）
    String bottle1Vol;//瓶1 采样体积
    int bottle1Ratio;//瓶1 采样比例
    String sampleNo2;//瓶2 样品编号
    String sampleTime2;//瓶2 采样时间（未用）
    String bottle2Vol;//瓶2 采样体积
    int bottle2Ratio;//瓶2 采样比例
    double currentFlow;//实时流量
    double minFlow;//最小触发流量
    int minSampleVol;//最小采集量
    int maxSampleTime;//采样超时时长
    String iccID;//物联网卡号
    int runMode;//运行模式
    int errCode;//故障码
    String sampleStartTime;//设备采样开始时间
    String sampleEndTime;//设备采样结束时间
    int washTime;//润洗时间
    double spaceFlow;//间隔流量
    double totalFlow;//累计流量
    int sudu1;//手动模式电机1转速
    int sudu2;//手动模式电机2转速
    String sysTime;//系统时间

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

    public String getDevEV() {
        return devEV;
    }

    public void setDevEV(String devEV) {
        this.devEV = devEV;
    }

    public String getSampleNo1() {
        return sampleNo1;
    }

    public void setSampleNo1(String sampleNo1) {
        this.sampleNo1 = sampleNo1;
    }

    public String getSampleNo2() {
        return sampleNo2;
    }

    public void setSampleNo2(String sampleNo2) {
        this.sampleNo2 = sampleNo2;
    }

    public String getBottle1Vol() {
        return bottle1Vol;
    }

    public void setBottle1Vol(String bottle1Vol) {
        this.bottle1Vol = bottle1Vol;
    }

    public int getBottle1Ratio() {
        return bottle1Ratio;
    }

    public void setBottle1Ratio(int bottle1Ratio) {
        this.bottle1Ratio = bottle1Ratio;
    }

    public String getBottle2Vol() {
        return bottle2Vol;
    }

    public void setBottle2Vol(String bottle2Vol) {
        this.bottle2Vol = bottle2Vol;
    }

    public int getBottle2Ratio() {
        return bottle2Ratio;
    }

    public void setBottle2Ratio(int bottle2Ratio) {
        this.bottle2Ratio = bottle2Ratio;
    }

    public double getMinFlow() {
        return minFlow;
    }

    public void setMinFlow(double minFlow) {
        this.minFlow = minFlow;
    }

    public int getMinSampleVol() {
        return minSampleVol;
    }

    public void setMinSampleVol(int minSampleVol) {
        this.minSampleVol = minSampleVol;
    }

    public double getCurrentFlow() {
        return currentFlow;
    }

    public void setCurrentFlow(double currentFlow) {
        this.currentFlow = currentFlow;
    }

    public int getMaxSampleTime() {
        return maxSampleTime;
    }

    public void setMaxSampleTime(int maxSampleTime) {
        this.maxSampleTime = maxSampleTime;
    }

    public String getIccID() {
        return iccID;
    }

    public void setIccID(String iccID) {
        this.iccID = iccID;
    }

    public int getRunMode() {
        return runMode;
    }

    public void setRunMode(int runMode) {
        this.runMode = runMode;
    }

    public String getSampleTime1() {
        return sampleTime1;
    }

    public void setSampleTime1(String sampleTime1) {
        this.sampleTime1 = sampleTime1;
    }

    public String getSampleTime2() {
        return sampleTime2;
    }

    public void setSampleTime2(String sampleTime2) {
        this.sampleTime2 = sampleTime2;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public String getSampleStartTime() {
        return sampleStartTime;
    }

    public void setSampleStartTime(String sampleStartTime) {
        this.sampleStartTime = sampleStartTime;
    }

    public String getSampleEndTime() {
        return sampleEndTime;
    }

    public void setSampleEndTime(String sampleEndTime) {
        this.sampleEndTime = sampleEndTime;
    }

    public int getWashTime() {
        return washTime;
    }

    public void setWashTime(int washTime) {
        this.washTime = washTime;
    }

    public double getSpaceFlow() {
        return spaceFlow;
    }

    public void setSpaceFlow(double spaceFlow) {
        this.spaceFlow = spaceFlow;
    }

    public double getTotalFlow() {
        return totalFlow;
    }

    public void setTotalFlow(double totalFlow) {
        this.totalFlow = totalFlow;
    }

    public int getSudu1() {
        return sudu1;
    }

    public void setSudu1(int sudu1) {
        this.sudu1 = sudu1;
    }

    public int getSudu2() {
        return sudu2;
    }

    public void setSudu2(int sudu2) {
        this.sudu2 = sudu2;
    }
}
