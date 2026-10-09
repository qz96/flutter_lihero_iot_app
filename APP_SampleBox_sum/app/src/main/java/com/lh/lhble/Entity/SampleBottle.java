package com.lh.lhble.Entity;

import com.lh.lhble.base.Constant;

public class SampleBottle implements Comparable<SampleBottle>{
    int bottleNo;//样品瓶号 97表示添加样品瓶 98表示清空样品瓶
    String blueMac;//蓝牙通讯地址,000000000000或者非12位值表示空位待装瓶
    String sampleID;//样品编号
    String dateTime;//采样时间:210901235959
    int isLock;//0-未禁用 1-禁用 2-留样
    int volume= Constant.DefaultValue;//样品容量mL


    public int getBottleNo() {
        return bottleNo;
    }

    public void setBottleNo(int bottleNo) {
        this.bottleNo = bottleNo;
    }

    public String getBlueMac() {
        return blueMac;
    }

    public void setBlueMac(String blueMac) {
        this.blueMac = blueMac;
    }

    public String getSampleID() {
        return sampleID;
    }

    public void setSampleID(String sampleID) {
        this.sampleID = sampleID;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public int getIsLock() {
        return isLock;
    }

    public void setIsLock(int isLock) {
        this.isLock = isLock;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    @Override
    public int compareTo(SampleBottle o) {
        // 升序
        return this.bottleNo-o.bottleNo;
    }
}
