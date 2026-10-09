package com.lh.lhble.Entity;

import java.util.List;

public class ReciveData {
    private int index;
    private int total;
    private  byte[][] dataList;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public byte[][] getDataList() {
        return dataList;
    }

    public void setDataList(byte[][] dataList) {
        this.dataList = dataList;
    }

    /**
     * 添加包数据内容，进接收数据内容集合
     * @param index 加入到的集合序号
     * @param bytes 加入的数据内容
     * @return
     */
    public int addData2List(int index,byte[] bytes){
        if (index<0||this.dataList==null||this.dataList.length<=index){
            return 0;//无法插入
        }
        this.dataList[index]=bytes;
        for (byte[] bs:this.dataList){
            if (bs==null||bs.length<=0){
                return 1;//已录入，但未录完
            }
        }
        return 2;//已录入且已录完毕

    }

}
