package com.lh.lhble.Entity;

import com.lh.lhble.Utils.StringUtils;
import com.lh.lhble.base.Constant;

public class SampleBox {
    String MN;//采样箱MN,0D2309000001
    String devName;//设备名称
    String devNum;//设备编号
    String version;//采样箱程序版本号
    String sampleMode;//00-手动模式 01-等时模式 02-雨量触发模式 03-混合模式 04-定时采样模式  05-液位触发模式 06-手动测试模式 07-采样流程测试 08-等比例采样 13-疾控模式
    String blueMac;//蓝牙通讯地址
    String workStep;//09/38 当前流程步骤/总步骤
    int cycleTime;//等时采样周期时间，单位:分钟
    String sysTime;//系统时间230101125959
    String lockBottles;//000101表示4号瓶和6号瓶被禁用，其他未被禁用
    String devEV;//设备电池电压
    int nextSampleTime;//采样倒计时,单位:秒
    String nextSampleDate;//下次采样时间,yyMMddHHmmss
    String currentSampleBottleNo;//当前采样瓶号 01~06
    String devStatus;//00 设备正常，其它为故障码
    int alarmState;//报警功能 0-未开启 1-开启
    String samplePointID;//平台采样点ID
    int rainNum;//雨量触发值，*0.2毫米=用户真实输入值。0.2mm~40mm的区间范围\
    int rainNumTotal;//雨量触发记录次数
    int waterHeight;//触发采样液位阀值 厘米 1~1000
    int requestCycle;//与平台指令同步周期 分钟 1-10080 0为关闭
    int updateType;//上传类型 0-不主动上传 1-全部上传 2-水参数上传 3-GPS上传
    int updateCycle;//上传间隔周期 0~1000 分钟 0为关闭
    int jyTime;//进样时长 5~300秒
    double jyTimeD;//进样时长 浮点型秒
    int pyTime;//排样时长10~300秒
    double pyTimeD;//排样时长 浮点型秒
    String riss;//信号强度
    int cyNum;//等比例采样次数（1~24的取值范围）
    String iccID;//物联网卡号
    int gnns;//是否开启经纬度查询 0-未开启 1-已开启
    int batLow;//电池低压阈值*100
    int washTime;//润洗时长 s
    double washTimeD;//润洗时长 浮点型秒
    int batDL;//剩余电量百分数
    int llj;//液位传感器是否有水 0-有水 1-无水
    int currentNum;//等比例采样当前采样瓶的已采样次数
    String net;//平台地址 city.lihero.com
    String port;//平台端口号
    int pwrEn= Constant.DefaultValue;
    String cypErr;
    String CypIpPort;
    int trigger;//触发模式
    int phThMax,phThMin;//PH阈值上下限
    int CondTh;//电导阈值  （us/cm）
    int	TurbTh;//浊度阈值  NTU
    int	OxygenTh;//溶解氧阈值 mg/L
    int WaterTempTh;//水温阈值 	摄氏度
    int	FlowTh;//流量触发阈值(m³/s)
    private int samDevType= Constant.DefaultValue;//当前连接设备
    int	pipelen;//管长 mm
    int	capacity;//采样管容量 mL
    int	fzkgState;//浮子开关状态（液位触发开关 0-未触发 >0-已触发）
    int topCoefficient100;//上层采样流量修正系数*100
    int bottomCoefficient100;//下层采样流量修正系数*100

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

    public String getDevName() {
        return devName;
    }

    public void setDevName(String devName) {
        this.devName = devName;
    }

    public String getDevNum() {
        return devNum;
    }

    public void setDevNum(String devNum) {
        this.devNum = devNum;
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

    public int getRainNumTotal() {
        return rainNumTotal;
    }

    public void setRainNumTotal(int rainNumTotal) {
        this.rainNumTotal = rainNumTotal;
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

    public int getBatLow() {
        return batLow;
    }

    public void setBatLow(int batLow) {
        this.batLow = batLow;
    }

    public int getWashTime() {
        return washTime;
    }

    public void setWashTime(int washTime) {
        this.washTime = washTime;
    }

    public int getBatDL() {
        return batDL;
    }

    public void setBatDL(int batDL) {
        this.batDL = batDL;
    }

    public int getLlj() {
        return llj;
    }

    public void setLlj(int llj) {
        this.llj = llj;
    }

    public String getCypIpPort() {
        return CypIpPort;
    }

    public void setCypIpPort(String cypIpPort) {
        CypIpPort = cypIpPort;
    }

    public int getTrigger() {
        return trigger;
    }

    public void setTrigger(int trigger) {
        this.trigger = trigger;
    }

    public int getPhThMax() {
        return phThMax;
    }

    public void setPhThMax(int phThMax) {
        this.phThMax = phThMax;
    }

    public int getPhThMin() {
        return phThMin;
    }

    public void setPhThMin(int phThMin) {
        this.phThMin = phThMin;
    }

    public int getCondTh() {
        return CondTh;
    }

    public void setCondTh(int condTh) {
        CondTh = condTh;
    }

    public int getTurbTh() {
        return TurbTh;
    }

    public void setTurbTh(int turbTh) {
        TurbTh = turbTh;
    }

    public int getOxygenTh() {
        return OxygenTh;
    }

    public void setOxygenTh(int oxygenTh) {
        OxygenTh = oxygenTh;
    }

    public int getWaterTempTh() {
        return WaterTempTh;
    }

    public void setWaterTempTh(int waterTempTh) {
        WaterTempTh = waterTempTh;
    }

    public int getFlowTh() {
        return FlowTh;
    }

    public void setFlowTh(int flowTh) {
        FlowTh = flowTh;
    }

    public int getCurrentNum() {
        return currentNum;
    }

    public void setCurrentNum(int currentNum) {
        this.currentNum = currentNum;
    }

    public String getNet() {
        return net;
    }

    public void setNet(String net) {
        this.net = net;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getNextSampleDate() {
        return nextSampleDate;
    }

    public void setNextSampleDate(String nextSampleDate) {
        this.nextSampleDate = nextSampleDate;
    }

    public int getPwrEn() {
        return pwrEn;
    }

    public void setPwrEn(int pwrEn) {
        this.pwrEn = pwrEn;
    }

    public String getCypErr() {
        return cypErr;
    }

    public void setCypErr(String cypErr) {
        this.cypErr = cypErr;
    }

    public int getSamDevType() {
        return samDevType;
    }

    public void setSamDevType(int samDevType) {
        this.samDevType = samDevType;
    }

    public double getJyTimeD() {
        return jyTimeD;
    }

    public void setJyTimeD(double jyTimeD) {
        this.jyTimeD = jyTimeD;
    }

    public double getPyTimeD() {
        return pyTimeD;
    }

    public void setPyTimeD(double pyTimeD) {
        this.pyTimeD = pyTimeD;
    }

    public double getWashTimeD() {
        return washTimeD;
    }

    public void setWashTimeD(double washTimeD) {
        this.washTimeD = washTimeD;
    }

    public int getFzkgState() {
        return fzkgState;
    }

    public void setFzkgState(int fzkgState) {
        this.fzkgState = fzkgState;
    }

    public int getPipelen() {
        return pipelen;
    }

    public void setPipelen(int pipelen) {
        this.pipelen = pipelen;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getTopCoefficient100() {
        return topCoefficient100;
    }

    public void setTopCoefficient100(int topCoefficient100) {
        this.topCoefficient100 = topCoefficient100;
    }

    public int getBottomCoefficient100() {
        return bottomCoefficient100;
    }

    public void setBottomCoefficient100(int bottomCoefficient100) {
        this.bottomCoefficient100 = bottomCoefficient100;
    }
}
