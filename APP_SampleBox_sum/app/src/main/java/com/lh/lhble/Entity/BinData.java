package com.lh.lhble.Entity;

public class BinData {
    String versionCode;
    byte[] binBytes;
    int crcNum;
    int bagNum;
    int bagNo;
    public static final int bagSize=200;

    public BinData(String versionCode, byte[] binBytes, int crcNum, int bagNum) {
        this.versionCode = versionCode;
        this.binBytes = binBytes;
        this.crcNum = crcNum;
        this.bagNum = bagNum;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public byte[] getBinBytes() {
        return binBytes;
    }

    public void setBinBytes(byte[] binBytes) {
        this.binBytes = binBytes;
    }

    public int getCrcNum() {
        return crcNum;
    }

    public void setCrcNum(int crcNum) {
        this.crcNum = crcNum;
    }

    public int getBagNum() {
        return bagNum;
    }

    public void setBagNum(int bagNum) {
        this.bagNum = bagNum;
    }

    public int getBagNo() {
        return bagNo;
    }

    public void setBagNo(int bagNo) {
        this.bagNo = bagNo;
    }

    public static int getBagSize() {
        return bagSize;
    }
}
