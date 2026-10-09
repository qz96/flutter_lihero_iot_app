package com.lh.lhble.Entity;

import com.lh.lhble.Utils.StringUtils;

import java.io.Serializable;

/**
 * description ：
 * project name：CCloud
 * author : Vincent
 * creation date: 2017/5/3 14:09
 * @version 1.0
 */

public class NetInfo implements Comparable<NetInfo>{
    private int No;
    private String strIp12;
    private String strPort5;
    private int  protocolType;//-1表示不考虑协议类型的采样箱0D07采样通道IP端口配置
    private int en=-1;

    public NetInfo( ) {
    }

    public NetInfo(int no, String strIp12, String strPort5, int protocolType, int en) {
        No = no;
        this.strIp12 = strIp12;
        this.strPort5 = strPort5;
        this.protocolType = protocolType;
        this.en = en;
    }

    public int getNo() {
        return No;
    }

    public void setNo(int no) {
        No = no;
    }

    public String getStrIp12() {
        return strIp12;
    }

    public void setStrIp12(String strIp12) {
        this.strIp12 = strIp12;
    }

    public String getStrPort5() {
        return strPort5;
    }

    public void setStrPort5(String strPort5) {
        this.strPort5 = strPort5;
    }


    public int getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(int protocolType) {
        this.protocolType = protocolType;
    }

    public int getEn() {
        return en;
    }

    public void setEn(int en) {
        this.en = en;
    }


    public void checkIp2En(){
        if (StringUtils.isEmpty(strIp12)||strIp12.length()!=12||"000000000000".equals(strIp12)||!StringUtils.isLong(strIp12)){
           en=-1;//通信IP配置被清空或不正常状态
        }
    }

    @Override
    public int compareTo(NetInfo o) {
        // 升序
        return this.No-o.No;
    }
}
