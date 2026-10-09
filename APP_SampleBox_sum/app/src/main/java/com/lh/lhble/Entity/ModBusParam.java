package com.lh.lhble.Entity;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.lh.lhble.Utils.DoubleUtils;
import com.lh.lhble.Utils.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModBusParam implements Comparable<ModBusParam>{
    int modbusAdr;//ModBus地址 eg 0x55
    int address;//参数的Modbus起始地址，例如 40047
    int addNum;//组成参数信息的寄存器个数
    boolean canWrite;//是否可以写入
    boolean canRead;//是否可以读取
    int dataType;//0-无符号整形 1-浮点型(小端在前) 2-有符号整型 3-枚举形整数 4-累计流量 5-浮点型（大端在前）
    List<MRegister> originRegisters;//读取到的寄存器值


    String paramName;
    String unit;//显示单位
    int scale;//显示修约位数
    double dm;//待乘的倍数，乘积用于显示
    HashMap<Integer,String> dataMenu;//枚举关系对应
    String dataValue;
    int tvId;//用于显示的textViewID;

    int childrenNum;//内部子参数个数
    List<ModBusParam> childrenParams;//内部子参数信息

    List<MRegister> writeRegisters;//待写入的寄存器值


    public ModBusParam(int address, int addNum, boolean canWrite, boolean canRead, int dataType, String paramName, String unit, int scale,double dm,int tvId, int childrenNum, List<ModBusParam> childrenParams) {
        this.address = address;
        this.addNum = addNum;
        this.canWrite = canWrite;
        this.canRead = canRead;
        this.paramName = paramName;
        this.unit = unit;
        this.scale = scale;
        this.dataType = dataType;
        this.tvId=tvId;
        this.childrenNum = childrenNum;
        this.childrenParams = childrenParams;
        this.dm=dm;
    }

    public ModBusParam(int address, int addNum, boolean canWrite, boolean canRead, int dataType, String paramName,HashMap<Integer,String> dataMenu,int tvId, int childrenNum, List<ModBusParam> childrenParams) {
        this.address = address;
        this.addNum = addNum;
        this.canWrite = canWrite;
        this.canRead = canRead;
        this.paramName = paramName;
        this.dataType = dataType;
        this.tvId=tvId;
        this.childrenNum = childrenNum;
        this.childrenParams = childrenParams;
        this.dataMenu=dataMenu;
    }


    public void setData(List<MRegister> mRegisters){
        this.originRegisters=mRegisters;
        if (childrenNum<=0){
            switch (dataType){
                case 0://无符号整形数值
                    int intValue=(mRegisters.get(0).getValue())&0xFFFF;
                    if (dm>0&&DoubleUtils.hasDecimalPart(dm)){
                        dataValue= DoubleUtils.round((dm>0?DoubleUtils.mul(intValue,dm):intValue),scale>=0?scale:0)+"";
                    }else if (dm>0){
                        dataValue= DoubleUtils.round((dm>0?DoubleUtils.mul(intValue,dm):intValue),0)+"";
                    }else {
                        dataValue=intValue+"";
                    }

                    break;
                case 1://浮点型数值 小端在前
                    if (mRegisters.size()==2){
                        int iH1=mRegisters.get(1).getHiByteValue();//79 2
                        int iL1=mRegisters.get(1).getLoByteValue();//E9 3
                        int iH0=mRegisters.get(0).getHiByteValue();//F6 0
                        int iL0=mRegisters.get(0).getLoByteValue();//42 1
                      /*  float floatValue1=Float.intBitsToFloat((iH1|iL1<<8|iH0<<16|iL0<<24)&0xFFFFFFFF);
                        float floatValue2=Float.intBitsToFloat((iH1<<24|iL1<<16|iH0<<8|iL0)&0xFFFFFFFF);
                        float floatValue3=Float.intBitsToFloat((iL1|iH1<<8|iL0<<16|iH0<<24)&0xFFFFFFFF);
                        float floatValue4=Float.intBitsToFloat((iL1<<24|iH1<<16|iL0<<8|iH0 )&0xFFFFFFFF);*/


                        float floatValue=Float.intBitsToFloat((mRegisters.get(1).getValue()<<16|mRegisters.get(0).getValue()));

                        dataValue=StringUtils.formatDoublePointNum( DoubleUtils.round((dm>0?DoubleUtils.mul(floatValue,dm):floatValue),scale>=0?scale:0),scale);
                    }
                    break;
                case 2://有符号整形数值
                    int intyValue=0;
                    for (int i=0;i<addNum;i++){
                        intyValue=((mRegisters.get(i).getValue())&0xFFFF)<<(16*(addNum-1-i))|intyValue;
                    }
                    if (dm>0&&DoubleUtils.hasDecimalPart(dm)){
                        dataValue= DoubleUtils.round((dm>0?DoubleUtils.mul(intyValue,dm):intyValue),scale>=0?scale:0)+"";
                    }else if (dm>0){
                        dataValue= DoubleUtils.round((dm>0?DoubleUtils.mul(intyValue,dm):intyValue),0)+"";
                    }else {
                        dataValue=intyValue+"";
                    }

                    break;
                case 3://枚举形无符号整数
                    int menuValue=(mRegisters.get(0).getValue())&0xFFFF;

                    if (dataMenu!=null&&dataMenu.containsKey(menuValue)){
                        dataValue=dataMenu.get(menuValue);
                    }else {
                        dataValue=null;
                    }

                    break;
                case 4:
                    if (mRegisters.size()==3){
                        float floatValue=Float.intBitsToFloat((mRegisters.get(1).getValue()<<16|mRegisters.get(0).getValue()));
                        int bigValue=(mRegisters.get(2).getValue())&0xFFFF;
                        dataValue=String.format("%."+(scale>0?scale:0)+"f",((bigValue>0?(bigValue*((int)Math.pow(10,7))):0)+ DoubleUtils.round(floatValue,scale>=0?scale:0)));
                    }

                    break;
                case 5://浮点型数值 大端在前
                    if (mRegisters.size()==2){
                        int iH1=mRegisters.get(1).getHiByteValue();//79 2
                        int iL1=mRegisters.get(1).getLoByteValue();//E9 3
                        int iH0=mRegisters.get(0).getHiByteValue();//F6 0
                        int iL0=mRegisters.get(0).getLoByteValue();//42 1


                        float floatValue=Float.intBitsToFloat((mRegisters.get(0).getValue()<<16|mRegisters.get(1).getValue()));

                        dataValue=StringUtils.formatDoublePointNum( DoubleUtils.round((dm>0?DoubleUtils.mul(floatValue,dm):floatValue),scale>=0?scale:0),scale);
                    }
                    break;
            }

        }else {
            int firstAddress=childrenParams.get(0).getAddress();
            for (ModBusParam childParam:childrenParams){
                List<MRegister> cRegisters=mRegisters.subList(childParam.getAddress()-firstAddress,childParam.getAddress()+childParam.getAddNum()-firstAddress);
                childParam.setData(cRegisters);
            }
        }
    }

    public void notifyDataSetChanged(View contentView){
        if (childrenNum<=0){
            if (tvId==0){
                return;
            }
            TextView tv=contentView.findViewById(tvId);
            if (tv!=null){
                tv.setText(StringUtils.isEmpty(dataValue)?"":dataValue);
            }
        }else {
            for (ModBusParam childParam:childrenParams){
                 childParam.notifyDataSetChanged(contentView);
            }
        }
    }

    public void notifyDataWaitChanged(View contentView){
        if (childrenNum<=0){
            if (tvId==0){
                return;
            }
            TextView tv=contentView.findViewById(tvId);
            if (tv!=null){
                tv.setText(StringUtils.isEmpty(dataValue)?"":dataValue+"待写");
            }
        }else {
            for (ModBusParam childParam:childrenParams){
                childParam.notifyDataWaitChanged(contentView);
            }
        }
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getAddNum() {
        return addNum;
    }

    public void setAddNum(int addNum) {
        this.addNum = addNum;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public String getDataValue() {
        return dataValue;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public List<MRegister> getOriginRegisters() {
        return originRegisters;
    }

    public void setOriginRegisters(List<MRegister> originRegisters) {
        this.originRegisters = originRegisters;
    }

    public double getDm() {
        return dm;
    }

    public void setDm(double dm) {
        this.dm = dm;
    }

    public int getTvId() {
        return tvId;
    }

    public void setTvId(int tvId) {
        this.tvId = tvId;
    }

    public List<MRegister> getWriteRegisters() {
        return writeRegisters;
    }

    public void setWriteRegisters(List<MRegister> writeRegisters) {
        this.writeRegisters = writeRegisters;
    }

    public HashMap<Integer, String> getDataMenu() {
        return dataMenu;
    }

    public void setDataMenu(HashMap<Integer, String> dataMenu) {
        this.dataMenu = dataMenu;
    }

    public int getChildrenNum() {
        return childrenNum;
    }

    public void setChildrenNum(int childrenNum) {
        this.childrenNum = childrenNum;
    }

    public List<ModBusParam> getChildrenParams() {
        return childrenParams;
    }

    public void setChildrenParams(List<ModBusParam> childrenParams) {
        this.childrenParams = childrenParams;
    }

    @Override
    public int compareTo(ModBusParam o) {
        // 升序
        return this.address-o.address;
    }
}
