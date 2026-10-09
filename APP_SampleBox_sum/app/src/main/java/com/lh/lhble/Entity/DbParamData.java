package com.lh.lhble.Entity;

/**
 * 地下水采样箱2，达标参数数据
 */
public class DbParamData implements Comparable<DbParamData>{
     int paramNo;
     String paramCode;
     String paramName;
     String data1Value;
     String data2Value;
     String data3Value;
     int isDb;//0-未评判 1-达标 2-未达标

    public DbParamData(int paramNo, String paramCode, String paramName, int isDb) {
        this.paramNo = paramNo;
        this.paramCode = paramCode;
        this.paramName = paramName;
        this.isDb = isDb;
    }

    public int getParamNo() {
        return paramNo;
    }

    public void setParamNo(int paramNo) {
        this.paramNo = paramNo;
    }

    public String getParamCode() {
        return paramCode;
    }

    public void setParamCode(String paramCode) {
        this.paramCode = paramCode;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getData1Value() {
        return data1Value;
    }

    public void setData1Value(String data1Value) {
        this.data1Value = data1Value;
    }

    public String getData2Value() {
        return data2Value;
    }

    public void setData2Value(String data2Value) {
        this.data2Value = data2Value;
    }

    public String getData3Value() {
        return data3Value;
    }

    public void setData3Value(String data3Value) {
        this.data3Value = data3Value;
    }

    public int getIsDb() {
        return isDb;
    }

    public void setIsDb(int isDb) {
        this.isDb = isDb;
    }

    @Override
    public int compareTo(DbParamData o) {
        // 升序
        return this.paramNo-o.paramNo;
    }
}
