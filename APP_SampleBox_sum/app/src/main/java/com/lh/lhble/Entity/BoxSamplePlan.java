package com.lh.lhble.Entity;

import java.io.Serializable;

/**
 * 采样箱定时采样模式
 */
public class BoxSamplePlan implements Serializable,Comparable<BoxSamplePlan> {

    private String status;
    private String HHmm="0000";



    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHHmm() {
        return HHmm;
    }

    public void setHHmm(String HHmm) {
        this.HHmm = HHmm;
    }

    @Override
    public int compareTo(BoxSamplePlan o) {
        if (this.status.equals("00")&&o.status.equals("01")){
            return 1;
        }else if (this.status.equals("01")&&o.status.equals("00")){
            return -1;
        }else{
            // 升序
            return Integer.parseInt("1"+this.HHmm)-Integer.parseInt("1"+o.HHmm);
        }

    }
}
