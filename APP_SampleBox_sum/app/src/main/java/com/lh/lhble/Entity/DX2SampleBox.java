package com.lh.lhble.Entity;

public class DX2SampleBox {
    String MN;
    String version;
    String sampleMode;//00-手动模式 01-等时模式 02-雨量触发模式 03-混合模式 04-定时采样模式  05-液位触发模式 06-手动测试模式 07-采样流程测试 08-等比例采样
    String blueMac;//蓝牙通讯地址
    String workStep;//09/38 当前流程步骤/总步骤
    int cycleTime;//等时采样周期时间，单位:分钟
    String sysTime;//系统时间
    String lockBottles;//000101表示4号瓶和6号瓶被禁用，其他未被禁用
    String devEV;//设备电池电压
    int nextSampleTime;//采样倒计时,单位:秒
    String currentSampleBottleNo;//当前采样瓶号 01~06
    String devStatus;//00 设备正常，其它为故障码
    int alarmState;//0-未开启 1-开启
    String samplePointID;
    int rainNum;//雨量触发值，*0.2毫米为用户真实输入值。0.2mm~40mm的区间范围
    int waterHeight;//触发采样液位阀值 厘米 1~1000
    int requestCycle;//与平台指令同步周期 分钟 1-10080 0为关闭
    int updateType;//上传类型 0-不主动上传 1-全部上传 2-水参数上传 3-GPS上传
    int updateCycle;//上传间隔周期 0~1000 分钟 0为关闭
    int jyTime;//进样时长 5~300秒
    int pyTime;//排样时长10~300秒
    String riss;//信号强度
    int cyNum;//等比例采样
    String iccID;//物联网卡号
    int gnns;//是否开启经纬度查询 0-未开启 1-已开启
    int pulseValvePeriod;//气囊泵启停间隔时间,秒
    int jyWarningSec;//进样超时时长，秒
    int washTime;//洗井时间.秒
    int WaterPressure;//水位数据，cm
    int judge;//水位判断标志 0-不判断 1-判断
    int startFlag;//采样启停 1-开始采样 0-已停止采样
    int sampleStatus;//采样状态 0-空闲 1-管路润洗中 2-开始洗井 3-水质参数达标，开始采样 4-洗井超时，开始采样
    int BaseWaterPressure;//水位初始值，cm
    int WaterStatus;//水位状态 0-正常 1-水位波动大于10cm
    int pulseValveFlag;//气囊泵工作状态 0-关闭 1-开启
    int llj;//液位传感器是否有水 0-有水 1-无水
    int isLoad;//APP操作是否将达标参数下载保存 0-未保存 1-已保存

    public String getIccID() {
        return iccID;
    }

    public void setIccID(String iccID) {
        this.iccID = iccID;
    }

    public String getMN() {
        return MN;
    }

    public void setMN(String MN) {
        this.MN = MN;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSampleMode() {
        return sampleMode;
    }

    public void setSampleMode(String sampleMode) {
        this.sampleMode = sampleMode;
    }

    public String getBlueMac() {
        return blueMac;
    }

    public void setBlueMac(String blueMac) {
        this.blueMac = blueMac;
    }

    public String getWorkStep() {
        return workStep;
    }

    public void setWorkStep(String workStep) {
        this.workStep = workStep;
    }

    public int getCycleTime() {
        return cycleTime;
    }

    public void setCycleTime(int cycleTime) {
        this.cycleTime = cycleTime;
    }

    public String getSysTime() {
        return sysTime;
    }

    public void setSysTime(String sysTime) {
        this.sysTime = sysTime;
    }

    public String getLockBottles() {
        return lockBottles;
    }

    public void setLockBottles(String lockBottles) {
        this.lockBottles = lockBottles;
    }

    public String getDevEV() {
        return devEV;
    }

    public void setDevEV(String devEV) {
        this.devEV = devEV;
    }

    public int getNextSampleTime() {
        return nextSampleTime;
    }

    public void setNextSampleTime(int nextSampleTime) {
        this.nextSampleTime = nextSampleTime;
    }

    public String getCurrentSampleBottleNo() {
        return currentSampleBottleNo;
    }

    public void setCurrentSampleBottleNo(String currentSampleBottleNo) {
        this.currentSampleBottleNo = currentSampleBottleNo;
    }

    public String getDevStatus() {
        return devStatus;
    }

    public void setDevStatus(String devStatus) {
        this.devStatus = devStatus;
    }

    public int getAlarmState() {
        return alarmState;
    }

    public void setAlarmState(int alarmState) {
        this.alarmState = alarmState;
    }

    public String getSamplePointID() {
        return samplePointID;
    }

    public void setSamplePointID(String samplePointID) {
        this.samplePointID = samplePointID;
    }

    public int getRainNum() {
        return rainNum;
    }

    public void setRainNum(int rainNum) {
        this.rainNum = rainNum;
    }

    public int getWaterHeight() {
        return waterHeight;
    }

    public void setWaterHeight(int waterHeight) {
        this.waterHeight = waterHeight;
    }

    public int getRequestCycle() {
        return requestCycle;
    }

    public void setRequestCycle(int requestCycle) {
        this.requestCycle = requestCycle;
    }

    public int getUpdateType() {
        return updateType;
    }

    public void setUpdateType(int updateType) {
        this.updateType = updateType;
    }

    public int getUpdateCycle() {
        return updateCycle;
    }

    public void setUpdateCycle(int updateCycle) {
        this.updateCycle = updateCycle;
    }

    public int getJyTime() {
        return jyTime;
    }

    public void setJyTime(int jyTime) {
        this.jyTime = jyTime;
    }

    public int getPyTime() {
        return pyTime;
    }

    public void setPyTime(int pyTime) {
        this.pyTime = pyTime;
    }

    public String getRiss() {
        return riss;
    }

    public void setRiss(String riss) {
        this.riss = riss;
    }

    public int getCyNum() {
        return cyNum;
    }

    public void setCyNum(int cyNum) {
        this.cyNum = cyNum;
    }

    public int getGnns() {
        return gnns;
    }

    public void setGnns(int gnns) {
        this.gnns = gnns;
    }

    public int getPulseValvePeriod() {
        return pulseValvePeriod;
    }

    public void setPulseValvePeriod(int pulseValvePeriod) {
        this.pulseValvePeriod = pulseValvePeriod;
    }

    public int getJyWarningSec() {
        return jyWarningSec;
    }

    public void setJyWarningSec(int jyWarningSec) {
        this.jyWarningSec = jyWarningSec;
    }

    public int getWashTime() {
        return washTime;
    }

    public void setWashTime(int washTime) {
        this.washTime = washTime;
    }

    public int getWaterPressure() {
        return WaterPressure;
    }

    public void setWaterPressure(int waterPressure) {
        WaterPressure = waterPressure;
    }

    public int getJudge() {
        return judge;
    }

    public void setJudge(int judge) {
        this.judge = judge;
    }

    public int getStartFlag() {
        return startFlag;
    }

    public void setStartFlag(int startFlag) {
        this.startFlag = startFlag;
    }

    public int getSampleStatus() {
        return sampleStatus;
    }

    public void setSampleStatus(int sampleStatus) {
        this.sampleStatus = sampleStatus;
    }

    public int getBaseWaterPressure() {
        return BaseWaterPressure;
    }

    public void setBaseWaterPressure(int baseWaterPressure) {
        BaseWaterPressure = baseWaterPressure;
    }

    public int getWaterStatus() {
        return WaterStatus;
    }

    public void setWaterStatus(int waterStatus) {
        WaterStatus = waterStatus;
    }

    public int getPulseValveFlag() {
        return pulseValveFlag;
    }

    public void setPulseValveFlag(int pulseValveFlag) {
        this.pulseValveFlag = pulseValveFlag;
    }

    public int getIsLoad() {
        return isLoad;
    }

    public void setIsLoad(int isLoad) {
        this.isLoad = isLoad;
    }

    public int getLlj() {
        return llj;
    }

    public void setLlj(int llj) {
        this.llj = llj;
    }
}
