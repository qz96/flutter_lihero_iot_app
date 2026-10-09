package com.lh.lhble.Entity;

public class SamplePlan implements Comparable<SamplePlan>{
    int No;//1~6
    String status;//00-关闭 01-开启
    String dateTime;//23:59 每天的定时采样时间 小时:分钟

    public int getNo() {
        return No;
    }

    public void setNo(int no) {
        No = no;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public int compareTo(SamplePlan o) {
        // 升序
        return this.No-o.No;
    }
}
