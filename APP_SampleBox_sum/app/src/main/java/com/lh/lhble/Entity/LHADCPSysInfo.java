package com.lh.lhble.Entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * LHADCP设备详情数据实体类
 * 对应协议文档中 Read Detail 指令返回的52个字段（新版协议）
 */
public class LHADCPSysInfo implements Parcelable {
    
    // (1) 系统时间（12位 yyMMddhhmmss）例260624235959
    private String sysTime;
    
    // (2) 固件版本（6位）例如：230706
    private String firmwareVersion;
    
    // (3) 设备名（12位）
    private String deviceName;
    
    // (4) 设备唯一标识
    private String deviceId;
    
    // (5) 设备所在断面MN号（12位）
    private String deviceMN;
    
    // (6) 设备IP 例：036137024154 表示36.137.24.154
    private String deviceIp;
    
    // (7) 设备端口（5位）
    private String devicePort;
    
    // (8) 蓝牙地址（12位）
    private String bleAddress;
    
    // (9) 设备状态（5位）（见附录二）- 位0:电压状态 位1:网络状态
    private long deviceStatus;
    
    // (10) 操作账号名（24位）
    private String operatorName;
    
    // (11) 设备电池电压（5位，小数点后两位）电压（9.8 ~ 10.6）对应电量0~100
    private double batteryVoltage;
    
    // (12) 内存编号（3位）
    private int memoryNumber;
    
    // (13) 运行模式（3位）0：自动模式 1：手动模式 2：在线模式 3：维护模式
    private int runMode;
    
    // (14) 自动测试模式持续时间（5位，单位秒）
    private int autoTestDuration;
    
    // (15) 手动模式持续时间（5位，单位秒）
    private int manualTestDuration;
    
    // (16) 在线模式采数间隔时间（5位，单位秒）
    private int onlineSampleInterval;
    
    // (17) 在线模式上传间隔时间（5位，单位秒）（传输周期）
    private int onlineUploadInterval;
    
    // (18) 在线模式下供电等待时间（5位，单位秒）- 新增字段
    private int samplingWaitTime;
    
    // (19) 在线模式首次上传时间（12位）yyMMddhhmmss - 新增字段
    private String startTime;
    
    // (20) 212协议ST编号 - 新增字段
    private String devST;
    
    // (21) 上传协议（0：212，1：MQTT）
    private int uploadProtocol;
    
    // (19) SIM卡NCCID号（20位）
    private String simNccid;
    
    // (20) SIM卡IMSI号（15位）
    private String simImsi;
    
    // (21) 网络信号强度（2位）
    private int signalStrength;
    
    // (22) 本地IP（12位）
    private String localIp;
    
    // (23) 流速（小数，位数不定，m/s）- 经过曲线计算后的流速
    private double flowVelocity;
    
    // (24) 原始流速（小数，位数不定，m/s）- 未经曲线计算前的流速
    private double rawFlowVelocity;
    
    // (25) 瞬时流量（小数，位数不定，m³/s）
    private double instantFlow;
    
    // (26) 水深（小数，位数不定，m）
    private double waterDepth;
    
    // (27) 水位（小数，位数不定，m）
    private double waterLevel;
    
    // (28) 断面面积（小数，位数不定，m²）
    private double crossSectionArea;
    
    // (29) 水温（小数，位数不定，摄氏度）
    private double waterTemp;
    
    // (30) 横滚角（小数，位数不定，°）
    private double rollAngle;
    
    // (31) 俯仰角（小数，位数不定，°）
    private double pitchAngle;
    
    // (32) 间隔流量 - 新增字段
    private double intervalFlow;
    
    // (33) 净累计流量
    private double netCumulativeFlow;
    
    // (34) 净累计流量进位次数
    private int netCumulativeCarry;
    
    // (35) 正向累计流量
    private double positiveCumulativeFlow;
    
    // (36) 正向累计流量进位次数
    private int positiveCumulativeCarry;
    
    // (37) 反向累计流量
    private double reverseCumulativeFlow;
    
    // (38) 反向累计流量进位次数
    private int reverseCumulativeCarry;
    
    // (39) 绝对值累计流量
    private double absCumulativeFlow;
    
    // (40) 绝对值累计流量进位次数
    private int absCumulativeCarry;
    
    // (41) 河底高程（位数不定，mm）
    private double riverbedElevation;
    
    // (42) 安装高度（小数，位数不定，m）
    private double installHeight;
    
    // (43) 断面形状（0:矩形 1：梯形 2：V形 3：U形 4：圆形）
    private int crossSectionShape;
    
    // (44) 断面参数1，2，3（小数，位数不定，单位m）（详见附录一）
    private double sectionParam1;
    private double sectionParam2;
    private double sectionParam3;
    
    // (45) 曲线系数A,B,C（小数，位数不定，单位m）
    private double curveCoeffA;
    private double curveCoeffB;
    private double curveCoeffC;

    public LHADCPSysInfo() {
        // 初始化默认值
        this.flowVelocity = -1;
        this.rawFlowVelocity = -1;
        this.instantFlow = -1;
        this.waterDepth = -1;
        this.waterLevel = -1;
        this.crossSectionArea = -1;
        this.waterTemp = -1;
        this.rollAngle = -1;
        this.pitchAngle = -1;
        this.intervalFlow = -1;
        this.netCumulativeFlow = -1;
        this.positiveCumulativeFlow = -1;
        this.reverseCumulativeFlow = -1;
        this.absCumulativeFlow = -1;
        this.riverbedElevation = -1;
        this.installHeight = -1;
        this.sectionParam1 = -1;
        this.sectionParam2 = -1;
        this.sectionParam3 = -1;
        this.curveCoeffA = -1;
        this.curveCoeffB = -1;
        this.curveCoeffC = -1;
    }

    // Getters and Setters

    protected LHADCPSysInfo(Parcel in) {
        sysTime = in.readString();
        firmwareVersion = in.readString();
        deviceName = in.readString();
        deviceId = in.readString();
        deviceMN = in.readString();
        deviceIp = in.readString();
        devicePort = in.readString();
        bleAddress = in.readString();
        deviceStatus = in.readLong();
        operatorName = in.readString();
        batteryVoltage = in.readDouble();
        memoryNumber = in.readInt();
        runMode = in.readInt();
        autoTestDuration = in.readInt();
        manualTestDuration = in.readInt();
        onlineSampleInterval = in.readInt();
        onlineUploadInterval = in.readInt();
        samplingWaitTime = in.readInt(); // 新增
        startTime = in.readString(); // 新增
        devST = in.readString(); // 新增
        uploadProtocol = in.readInt();
        simNccid = in.readString();
        simImsi = in.readString();
        signalStrength = in.readInt();
        localIp = in.readString();
        flowVelocity = in.readDouble();
        rawFlowVelocity = in.readDouble();
        instantFlow = in.readDouble();
        waterDepth = in.readDouble();
        waterLevel = in.readDouble();
        crossSectionArea = in.readDouble();
        waterTemp = in.readDouble();
        rollAngle = in.readDouble();
        pitchAngle = in.readDouble();
        intervalFlow = in.readDouble();
        netCumulativeFlow = in.readDouble();
        netCumulativeCarry = in.readInt();
        positiveCumulativeFlow = in.readDouble();
        positiveCumulativeCarry = in.readInt();
        reverseCumulativeFlow = in.readDouble();
        reverseCumulativeCarry = in.readInt();
        absCumulativeFlow = in.readDouble();
        absCumulativeCarry = in.readInt();
        riverbedElevation = in.readDouble();
        installHeight = in.readDouble();
        crossSectionShape = in.readInt();
        sectionParam1 = in.readDouble();
        sectionParam2 = in.readDouble();
        sectionParam3 = in.readDouble();
        curveCoeffA = in.readDouble();
        curveCoeffB = in.readDouble();
        curveCoeffC = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sysTime);
        dest.writeString(firmwareVersion);
        dest.writeString(deviceName);
        dest.writeString(deviceId);
        dest.writeString(deviceMN);
        dest.writeString(deviceIp);
        dest.writeString(devicePort);
        dest.writeString(bleAddress);
        dest.writeLong(deviceStatus);
        dest.writeString(operatorName);
        dest.writeDouble(batteryVoltage);
        dest.writeInt(memoryNumber);
        dest.writeInt(runMode);
        dest.writeInt(autoTestDuration);
        dest.writeInt(manualTestDuration);
        dest.writeInt(onlineSampleInterval);
        dest.writeInt(onlineUploadInterval);
        dest.writeInt(samplingWaitTime); // 新增
        dest.writeString(startTime); // 新增
        dest.writeString(devST); // 新增
        dest.writeInt(uploadProtocol);
        dest.writeString(simNccid);
        dest.writeString(simImsi);
        dest.writeInt(signalStrength);
        dest.writeString(localIp);
        dest.writeDouble(flowVelocity);
        dest.writeDouble(rawFlowVelocity);
        dest.writeDouble(instantFlow);
        dest.writeDouble(waterDepth);
        dest.writeDouble(waterLevel);
        dest.writeDouble(crossSectionArea);
        dest.writeDouble(waterTemp);
        dest.writeDouble(rollAngle);
        dest.writeDouble(pitchAngle);
        dest.writeDouble(intervalFlow);
        dest.writeDouble(netCumulativeFlow);
        dest.writeInt(netCumulativeCarry);
        dest.writeDouble(positiveCumulativeFlow);
        dest.writeInt(positiveCumulativeCarry);
        dest.writeDouble(reverseCumulativeFlow);
        dest.writeInt(reverseCumulativeCarry);
        dest.writeDouble(absCumulativeFlow);
        dest.writeInt(absCumulativeCarry);
        dest.writeDouble(riverbedElevation);
        dest.writeDouble(installHeight);
        dest.writeInt(crossSectionShape);
        dest.writeDouble(sectionParam1);
        dest.writeDouble(sectionParam2);
        dest.writeDouble(sectionParam3);
        dest.writeDouble(curveCoeffA);
        dest.writeDouble(curveCoeffB);
        dest.writeDouble(curveCoeffC);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LHADCPSysInfo> CREATOR = new Creator<LHADCPSysInfo>() {
        @Override
        public LHADCPSysInfo createFromParcel(Parcel in) {
            return new LHADCPSysInfo(in);
        }

        @Override
        public LHADCPSysInfo[] newArray(int size) {
            return new LHADCPSysInfo[size];
        }
    };

    public String getSysTime() {
        return sysTime;
    }

    public void setSysTime(String sysTime) {
        this.sysTime = sysTime;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceMN() {
        return deviceMN;
    }

    public void setDeviceMN(String deviceMN) {
        this.deviceMN = deviceMN;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public String getDevicePort() {
        return devicePort;
    }

    public void setDevicePort(String devicePort) {
        this.devicePort = devicePort;
    }

    public String getBleAddress() {
        return bleAddress;
    }

    public void setBleAddress(String bleAddress) {
        this.bleAddress = bleAddress;
    }

    public long getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(long deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public int getMemoryNumber() {
        return memoryNumber;
    }

    public void setMemoryNumber(int memoryNumber) {
        this.memoryNumber = memoryNumber;
    }

    public int getRunMode() {
        return runMode;
    }

    public void setRunMode(int runMode) {
        this.runMode = runMode;
    }

    public int getAutoTestDuration() {
        return autoTestDuration;
    }

    public void setAutoTestDuration(int autoTestDuration) {
        this.autoTestDuration = autoTestDuration;
    }

    public int getManualTestDuration() {
        return manualTestDuration;
    }

    public void setManualTestDuration(int manualTestDuration) {
        this.manualTestDuration = manualTestDuration;
    }

    public int getOnlineSampleInterval() {
        return onlineSampleInterval;
    }

    public void setOnlineSampleInterval(int onlineSampleInterval) {
        this.onlineSampleInterval = onlineSampleInterval;
    }

    public int getOnlineUploadInterval() {
        return onlineUploadInterval;
    }

    public void setOnlineUploadInterval(int onlineUploadInterval) {
        this.onlineUploadInterval = onlineUploadInterval;
    }

    // 新增字段的getter/setter
    public int getSamplingWaitTime() {
        return samplingWaitTime;
    }

    public void setSamplingWaitTime(int samplingWaitTime) {
        this.samplingWaitTime = samplingWaitTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getDevST() {
        return devST;
    }

    public void setDevST(String devST) {
        this.devST = devST;
    }

    public int getUploadProtocol() {
        return uploadProtocol;
    }

    public void setUploadProtocol(int uploadProtocol) {
        this.uploadProtocol = uploadProtocol;
    }

    public String getSimNccid() {
        return simNccid;
    }

    public void setSimNccid(String simNccid) {
        this.simNccid = simNccid;
    }

    public String getSimImsi() {
        return simImsi;
    }

    public void setSimImsi(String simImsi) {
        this.simImsi = simImsi;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public double getFlowVelocity() {
        return flowVelocity;
    }

    public void setFlowVelocity(double flowVelocity) {
        this.flowVelocity = flowVelocity;
    }

    public double getRawFlowVelocity() {
        return rawFlowVelocity;
    }

    public void setRawFlowVelocity(double rawFlowVelocity) {
        this.rawFlowVelocity = rawFlowVelocity;
    }

    public double getInstantFlow() {
        return instantFlow;
    }

    public void setInstantFlow(double instantFlow) {
        this.instantFlow = instantFlow;
    }

    public double getWaterDepth() {
        return waterDepth;
    }

    public void setWaterDepth(double waterDepth) {
        this.waterDepth = waterDepth;
    }

    public double getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel(double waterLevel) {
        this.waterLevel = waterLevel;
    }

    public double getCrossSectionArea() {
        return crossSectionArea;
    }

    public void setCrossSectionArea(double crossSectionArea) {
        this.crossSectionArea = crossSectionArea;
    }

    public double getWaterTemp() {
        return waterTemp;
    }

    public void setWaterTemp(double waterTemp) {
        this.waterTemp = waterTemp;
    }

    public double getRollAngle() {
        return rollAngle;
    }

    public void setRollAngle(double rollAngle) {
        this.rollAngle = rollAngle;
    }

    public double getPitchAngle() {
        return pitchAngle;
    }

    public void setPitchAngle(double pitchAngle) {
        this.pitchAngle = pitchAngle;
    }

    public double getIntervalFlow() {
        return intervalFlow;
    }

    public void setIntervalFlow(double intervalFlow) {
        this.intervalFlow = intervalFlow;
    }

    public double getNetCumulativeFlow() {
        return netCumulativeFlow;
    }

    public void setNetCumulativeFlow(double netCumulativeFlow) {
        this.netCumulativeFlow = netCumulativeFlow;
    }

    public int getNetCumulativeCarry() {
        return netCumulativeCarry;
    }

    public void setNetCumulativeCarry(int netCumulativeCarry) {
        this.netCumulativeCarry = netCumulativeCarry;
    }

    public double getPositiveCumulativeFlow() {
        return positiveCumulativeFlow;
    }

    public void setPositiveCumulativeFlow(double positiveCumulativeFlow) {
        this.positiveCumulativeFlow = positiveCumulativeFlow;
    }

    public int getPositiveCumulativeCarry() {
        return positiveCumulativeCarry;
    }

    public void setPositiveCumulativeCarry(int positiveCumulativeCarry) {
        this.positiveCumulativeCarry = positiveCumulativeCarry;
    }

    public double getReverseCumulativeFlow() {
        return reverseCumulativeFlow;
    }

    public void setReverseCumulativeFlow(double reverseCumulativeFlow) {
        this.reverseCumulativeFlow = reverseCumulativeFlow;
    }

    public int getReverseCumulativeCarry() {
        return reverseCumulativeCarry;
    }

    public void setReverseCumulativeCarry(int reverseCumulativeCarry) {
        this.reverseCumulativeCarry = reverseCumulativeCarry;
    }

    public double getAbsCumulativeFlow() {
        return absCumulativeFlow;
    }

    public void setAbsCumulativeFlow(double absCumulativeFlow) {
        this.absCumulativeFlow = absCumulativeFlow;
    }

    public int getAbsCumulativeCarry() {
        return absCumulativeCarry;
    }

    public void setAbsCumulativeCarry(int absCumulativeCarry) {
        this.absCumulativeCarry = absCumulativeCarry;
    }

    public double getRiverbedElevation() {
        return riverbedElevation;
    }

    public void setRiverbedElevation(double riverbedElevation) {
        this.riverbedElevation = riverbedElevation;
    }

    public double getInstallHeight() {
        return installHeight;
    }

    public void setInstallHeight(double installHeight) {
        this.installHeight = installHeight;
    }

    public int getCrossSectionShape() {
        return crossSectionShape;
    }

    public void setCrossSectionShape(int crossSectionShape) {
        this.crossSectionShape = crossSectionShape;
    }

    public double getSectionParam1() {
        return sectionParam1;
    }

    public void setSectionParam1(double sectionParam1) {
        this.sectionParam1 = sectionParam1;
    }

    public double getSectionParam2() {
        return sectionParam2;
    }

    public void setSectionParam2(double sectionParam2) {
        this.sectionParam2 = sectionParam2;
    }

    public double getSectionParam3() {
        return sectionParam3;
    }

    public void setSectionParam3(double sectionParam3) {
        this.sectionParam3 = sectionParam3;
    }

    public double getCurveCoeffA() {
        return curveCoeffA;
    }

    public void setCurveCoeffA(double curveCoeffA) {
        this.curveCoeffA = curveCoeffA;
    }

    public double getCurveCoeffB() {
        return curveCoeffB;
    }

    public void setCurveCoeffB(double curveCoeffB) {
        this.curveCoeffB = curveCoeffB;
    }

    public double getCurveCoeffC() {
        return curveCoeffC;
    }

    public void setCurveCoeffC(double curveCoeffC) {
        this.curveCoeffC = curveCoeffC;
    }

    @Override
    public String toString() {
        return "LHADCPSysInfo{" +
                "sysTime='" + sysTime + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceMN='" + deviceMN + '\'' +
                ", deviceIp='" + deviceIp + '\'' +
                ", devicePort='" + devicePort + '\'' +
                ", bleAddress='" + bleAddress + '\'' +
                ", deviceStatus=" + deviceStatus +
                ", operatorName='" + operatorName + '\'' +
                ", batteryVoltage=" + batteryVoltage +
                ", memoryNumber=" + memoryNumber +
                ", runMode=" + runMode +
                ", autoTestDuration=" + autoTestDuration +
                ", manualTestDuration=" + manualTestDuration +
                ", onlineSampleInterval=" + onlineSampleInterval +
                ", onlineUploadInterval=" + onlineUploadInterval +
                ", samplingWaitTime=" + samplingWaitTime + // 新增
                ", startTime='" + startTime + '\'' + // 新增
                ", devST='" + devST + '\'' + // 新增
                ", uploadProtocol=" + uploadProtocol +
                ", simNccid='" + simNccid + '\'' +
                ", simImsi='" + simImsi + '\'' +
                ", signalStrength=" + signalStrength +
                ", localIp='" + localIp + '\'' +
                ", flowVelocity=" + flowVelocity +
                ", rawFlowVelocity=" + rawFlowVelocity +
                ", instantFlow=" + instantFlow +
                ", waterDepth=" + waterDepth +
                ", waterLevel=" + waterLevel +
                ", crossSectionArea=" + crossSectionArea +
                ", waterTemp=" + waterTemp +
                ", rollAngle=" + rollAngle +
                ", pitchAngle=" + pitchAngle +
                ", intervalFlow=" + intervalFlow +
                ", netCumulativeFlow=" + netCumulativeFlow +
                ", netCumulativeCarry=" + netCumulativeCarry +
                ", positiveCumulativeFlow=" + positiveCumulativeFlow +
                ", positiveCumulativeCarry=" + positiveCumulativeCarry +
                ", reverseCumulativeFlow=" + reverseCumulativeFlow +
                ", reverseCumulativeCarry=" + reverseCumulativeCarry +
                ", absCumulativeFlow=" + absCumulativeFlow +
                ", absCumulativeCarry=" + absCumulativeCarry +
                ", riverbedElevation=" + riverbedElevation +
                ", installHeight=" + installHeight +
                ", crossSectionShape=" + crossSectionShape +
                ", sectionParam1=" + sectionParam1 +
                ", sectionParam2=" + sectionParam2 +
                ", sectionParam3=" + sectionParam3 +
                ", curveCoeffA=" + curveCoeffA +
                ", curveCoeffB=" + curveCoeffB +
                ", curveCoeffC=" + curveCoeffC +
                '}';
    }
}
