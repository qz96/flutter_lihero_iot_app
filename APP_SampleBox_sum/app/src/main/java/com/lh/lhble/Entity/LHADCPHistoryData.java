package com.lh.lhble.Entity;

import java.io.Serializable;

/**
 * LHADCP历史数据实体类
 * 对应协议中Read History返回的数据格式
 */
public class LHADCPHistoryData implements Serializable {
    private static final long serialVersionUID = 1L;

    // （1）数据标识 （3位）（000：未上传数据 ，001：已上传数据）
    private String dataFlag;
    
    // （2）数据时间 （12位）例260521111109
    private String dataTime;
    
    // （3）MN号（12位）
    private String mnNumber;
    
    // （4）操作的蓝牙账号（24位）
    private String operatorName;
    
    // （5）流速（小数，位数不定,m/s）
    private double velocity;
    
    // （6）瞬时流量（小数，位数不定,m3/s）
    private double instantFlow;
    
    // （7）水深（小数，位数不定,m）
    private double waterDepth;
    
    // （8）水位（小数，位数不定,m）
    private double waterLevel;
    
    // （9）过水面积（小数，位数不定,m2）
    private double area;
    
    // （10）水温（小数，位数不定,℃）
    private double waterTemp;
    
    // （11）俯仰角（小数，位数不定,°）
    private double pitchAngle;
    
    // （12）横滚角（小数，位数不定，°）
    private double rollAngle;
    
    // （13）安装高度（小数，位数不定，m）
    private double installHeight;
    
    // （14）河底高程（位数不定，mm）
    private long riverbedElevation;
    
    // （15）电源电压（小数，位数不定，V）
    private double batteryVoltage;

    public LHADCPHistoryData() {
    }

    // Getters and Setters
    public String getDataFlag() {
        return dataFlag;
    }

    public void setDataFlag(String dataFlag) {
        this.dataFlag = dataFlag;
    }

    public String getDataTime() {
        return dataTime;
    }

    public void setDataTime(String dataTime) {
        this.dataTime = dataTime;
    }

    public String getMnNumber() {
        return mnNumber;
    }

    public void setMnNumber(String mnNumber) {
        this.mnNumber = mnNumber;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
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

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public double getWaterTemp() {
        return waterTemp;
    }

    public void setWaterTemp(double waterTemp) {
        this.waterTemp = waterTemp;
    }

    public double getPitchAngle() {
        return pitchAngle;
    }

    public void setPitchAngle(double pitchAngle) {
        this.pitchAngle = pitchAngle;
    }

    public double getRollAngle() {
        return rollAngle;
    }

    public void setRollAngle(double rollAngle) {
        this.rollAngle = rollAngle;
    }

    public double getInstallHeight() {
        return installHeight;
    }

    public void setInstallHeight(double installHeight) {
        this.installHeight = installHeight;
    }

    public long getRiverbedElevation() {
        return riverbedElevation;
    }

    public void setRiverbedElevation(long riverbedElevation) {
        this.riverbedElevation = riverbedElevation;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    /**
     * 获取数据标识文本
     */
    public String getDataFlagText() {
        if ("000".equals(dataFlag)) {
            return "未上传";
        } else if ("001".equals(dataFlag)) {
            return "已上传";
        }
        return "未知";
    }

    /**
     * 获取格式化后的时间字符串
     * 格式：260521111109 -> 2026-05-21 11:11:09
     */
    public String getFormattedTime() {
        if (dataTime == null || dataTime.length() != 12) {
            return dataTime;
        }
        
        try {
            String year = dataTime.substring(0, 2);
            String month = dataTime.substring(2, 4);
            String day = dataTime.substring(4, 6);
            String hour = dataTime.substring(6, 8);
            String minute = dataTime.substring(8, 10);
            String second = dataTime.substring(10, 12);
            
            return "20" + year + "-" + month + "-" + day + " " + 
                   hour + ":" + minute + ":" + second;
        } catch (Exception e) {
            e.printStackTrace();
            return dataTime;
        }
    }
}
