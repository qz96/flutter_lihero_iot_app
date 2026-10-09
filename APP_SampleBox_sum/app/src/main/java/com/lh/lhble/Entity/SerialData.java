package com.lh.lhble.Entity;

import java.util.List;

public class SerialData {
    private int rowIndex;//分包号
    private int totalRowNum; //总包号
    int ModbusAdr;//modbus地址
    int orderType;//指令类型 03-读 10-写
    int byteCount;//数据段字节个数（一个寄存器2个字节）
    List<MRegister>mRegisters;//数据段转换的寄存器信息
    int crc;
    String originData;//原始数据

    public SerialData(String originData) {
        this.originData = originData;
    }

    public SerialData(String originData, int orderType) {
        this.originData = originData;
        this.orderType=orderType;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public int getTotalRowNum() {
        return totalRowNum;
    }

    public void setTotalRowNum(int totalRowNum) {
        this.totalRowNum = totalRowNum;
    }

    public int getModbusAdr() {
        return ModbusAdr;
    }

    public void setModbusAdr(int modbusAdr) {
        ModbusAdr = modbusAdr;
    }

    public int getOrderType() {
        return orderType;
    }

    public void setOrderType(int orderType) {
        this.orderType = orderType;
    }

    public int getByteCount() {
        return byteCount;
    }

    public void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }

    public List<MRegister> getmRegisters() {
        return mRegisters;
    }

    public void setmRegisters(List<MRegister> mRegisters) {
        this.mRegisters = mRegisters;
    }

    public int getCrc() {
        return crc;
    }

    public void setCrc(int crc) {
        this.crc = crc;
    }

    public String getOriginData() {
        return originData;
    }

    public void setOriginData(String originData) {
        this.originData = originData;
    }

    @Override
    public String toString() {
        return "SerialData{" +
                "ModbusAdr=" + ModbusAdr +
                ", orderType=" + orderType +
                ", byteCount=" + byteCount +
                ", mRegisters=" + mRegisters +
                ", crc=" + crc +
                ", originData='" + originData + '\'' +
                '}';
    }
}
