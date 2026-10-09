package com.lh.lhble.Entity;

import com.lh.lhble.base.Constant;

import java.util.List;

public class RTUInfo {
    private String version;
    private long SysTime;
    private String devName,bleMac,devMN,devNum;
    private double batV;
    private int devState= Constant.DefaultValue;
    private int samDevType= Constant.DefaultValue;//当前连接设备
    private int lotType;
    private String localIP,NCCID,IMSI;
    private int lotCsq=Constant.DefaultValue;//网络信号强度
    private int saveNum;//目前待上传的存储数据数量
    private int lotState=Constant.DefaultValue;//网络状态
    private int lotError;//网络错误码
    private long updateEndTime;//最近一次上传成功时间
    private int lotStep;//上传步骤
    private int sapInterval;
    private int upInterval;
    private int ipEns;
    private List<NetInfo> netInfoList;
    private String devST;
    private int pwrEn=Constant.DefaultValue;
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getSysTime() {
        return SysTime;
    }

    public void setSysTime(long sysTime) {
        SysTime = sysTime;
    }

    public String getDevName() {
        return devName;
    }

    public void setDevName(String devName) {
        this.devName = devName;
    }

    public String getBleMac() {
        return bleMac;
    }

    public void setBleMac(String bleMac) {
        this.bleMac = bleMac;
    }

    public String getDevMN() {
        return devMN;
    }

    public void setDevMN(String devMN) {
        this.devMN = devMN;
    }

    public String getDevNum() {
        return devNum;
    }

    public void setDevNum(String devNum) {
        this.devNum = devNum;
    }

    public double getBatV() {
        return batV;
    }

    public void setBatV(double batV) {
        this.batV = batV;
    }

    public int getDevState() {
        return devState;
    }

    public void setDevState(int devState) {
        this.devState = devState;
    }

    public int getLotType() {
        return lotType;
    }

    public void setLotType(int lotType) {
        this.lotType = lotType;
    }

    public String getLocalIP() {
        return localIP;
    }

    public void setLocalIP(String localIP) {
        this.localIP = localIP;
    }

    public String getNCCID() {
        return NCCID;
    }

    public void setNCCID(String NCCID) {
        this.NCCID = NCCID;
    }

    public String getIMSI() {
        return IMSI;
    }

    public void setIMSI(String IMSI) {
        this.IMSI = IMSI;
    }

    public int getLotCsq() {
        return lotCsq;
    }

    public void setLotCsq(int lotCsq) {
        this.lotCsq = lotCsq;
    }

    public int getSaveNum() {
        return saveNum;
    }

    public void setSaveNum(int saveNum) {
        this.saveNum = saveNum;
    }

    public int getLotState() {
        return lotState;
    }

    public void setLotState(int lotState) {
        this.lotState = lotState;
    }

    public int getLotError() {
        return lotError;
    }

    public void setLotError(int lotError) {
        this.lotError = lotError;
    }

    public long getUpdateEndTime() {
        return updateEndTime;
    }

    public void setUpdateEndTime(long updateEndTime) {
        this.updateEndTime = updateEndTime;
    }

    public int getLotStep() {
        return lotStep;
    }

    public void setLotStep(int lotStep) {
        this.lotStep = lotStep;
    }

    public int getSamDevType() {
        return samDevType;
    }

    public void setSamDevType(int samDevType) {
        this.samDevType = samDevType;
    }

    public int getSapInterval() {
        return sapInterval;
    }

    public void setSapInterval(int sapInterval) {
        this.sapInterval = sapInterval;
    }

    public int getUpInterval() {
        return upInterval;
    }

    public void setUpInterval(int upInterval) {
        this.upInterval = upInterval;
    }

    public int getIpEns() {
        return ipEns;
    }

    public void setIpEns(int ipEns) {
        this.ipEns = ipEns;
    }

    public List<NetInfo> getNetInfoList() {
        return netInfoList;
    }

    public void setNetInfoList(List<NetInfo> netInfoList) {
        this.netInfoList = netInfoList;
    }

    public String getDevST() {
        return devST;
    }

    public void setDevST(String devST) {
        this.devST = devST;
    }

    public int getPwrEn() {
        return pwrEn;
    }

    public void setPwrEn(int pwrEn) {
        this.pwrEn = pwrEn;
    }
}
