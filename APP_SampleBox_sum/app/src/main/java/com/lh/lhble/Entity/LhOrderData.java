package com.lh.lhble.Entity;

import com.lh.lhble.Utils.ProtocolUtil;

import java.util.ArrayList;
import java.util.List;

public class LhOrderData {
    int sendType;//蓝牙指令的APP定义请求码
    String order;//蓝牙指令原始内容
    List<byte[]> dataList;//蓝牙指令按bagSize分割得出的需发送子包集合
    int bagNum;//总包数
    int bagNo;//当前已发包数
    public static final int bagSize=50;
    int trys;//尝试次数

    public LhOrderData(int sendType, String order) {
        this.sendType = sendType;
        this.order = order;
        int n = order.length();
        int arraySize = (n + bagSize - 1) / bagSize; // 计算需要多少个子字符串
        dataList=new ArrayList<>();
        bagNum=arraySize;
        for (int i = 0; i < arraySize; i++) {
            int startIndex = i * bagSize;
            int endIndex = Math.min(startIndex + bagSize, n); // 确保不会超出原字符串长度
            String str=order.substring(startIndex, endIndex);
            byte[] bytes= ProtocolUtil.getBlueToothMsgForSend(str,i,bagNum);
            dataList.add(bytes);
        }
        bagNo=0;
        trys=0;

    }

    public int getSendType() {
        return sendType;
    }

    public void setSendType(int sendType) {
        this.sendType = sendType;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public List<byte[]> getDataList() {
        return dataList;
    }

    public void setDataList(List<byte[]> dataList) {
        this.dataList = dataList;
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

    public int getTrys() {
        return trys;
    }

    public void setTrys(int trys) {
        this.trys = trys;
    }
}
