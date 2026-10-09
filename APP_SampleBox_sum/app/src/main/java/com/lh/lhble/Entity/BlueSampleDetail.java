package com.lh.lhble.Entity;


import java.util.List;

public class BlueSampleDetail {
    private String id;//样品ID
    private String labNo;//实验室编号
    private String sampleNo;//样品编号
    private String blueMac;//瓶盖蓝牙地址

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public String getSampleNo() {
        return sampleNo;
    }

    public void setSampleNo(String sampleNo) {
        this.sampleNo = sampleNo;
    }

    public String getBlueMac() {
        return blueMac;
    }

    public void setBlueMac(String blueMac) {
        this.blueMac = blueMac;
    }
}
