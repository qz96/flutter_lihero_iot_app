package com.lh.lhble.Utils;


import android.util.Log;

import com.lh.lhble.Entity.MRegister;
import com.lh.lhble.Entity.ReciveData;
import com.lh.lhble.Entity.SerialData;
import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.wise.ble.ConvertData;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


public class ProtocolUtil {
	public static ReciveData reciveData;
	/**
	 * @param pBuf 要发送的字节数组
	 * @param start  起始位置
	 * @param length 截取的长度
	 * @return
	 */
	public static byte getCRC8(byte[] pBuf, int start, int length) {
		char i;
		byte crc = 0;
		int count = length;
		int index = start;
		while (count-- != 0) {
			for (i = 0x80; i != 0; i /= 2) {
				if ((crc & 0x80) != 0) {
					crc *= 2;
					crc ^= 0xE5;
				} else
					crc *= 2;

				if ((pBuf[index] & i) != 0)
					crc ^= 0xE5;
			}
			index++;
		}
		return crc;
	}

	public static byte getXOR(byte[] pBuf, int start, int length){
		byte xor= (byte) 0xFF;
		for (int i=0;i<length;i++){
			xor^=pBuf[start+i];
		}
		return xor;
	}


	public static byte[] getHeatbeatData() {
		byte[] czBuf = new byte[22];
		int iLen = 22;
		czBuf[0] = (byte) 0xAA;
		czBuf[1] = (byte) 0xBB;
		czBuf[2] = (byte) (iLen >> 8);
		czBuf[3] = (byte) (iLen & 0xff);
		czBuf[4] = 0x1;
		for (int i = 5; i < 19; i++) {
			czBuf[i] = 0x0;
		}
		byte crc = getCRC8(czBuf, 2, 17);
		czBuf[19] = crc;
		czBuf[20] = (byte) 0xEE;
		czBuf[21] = (byte) 0xFF;
		return czBuf;
	}



	public static byte[] getBlueToothMsgForSend( String desID,
												   String czSql) {
		if (StringUtils.isEmpty(desID)){
			HJBleApplication.shareInstance().setDesID("12345678");
			desID="12345678";
		}
		int iLen = czSql.getBytes().length + 17;
		byte[] czBuf = new byte[iLen];
		czBuf[0] = (byte) 0xAA;
		czBuf[1] = (byte) 0xBB;
		czBuf[2] = (byte) (iLen >> 8);
		czBuf[3] = (byte) (iLen & 0xff);
		for (int i = 0; i < 8; i++) {
			czBuf[4 + i] = desID.getBytes()[i];
		}
		czBuf[12 ] = (byte) (czSql.getBytes().length  >> 8);
		czBuf[13] = (byte) (czSql.getBytes().length  & 0xff);
		for (int i = 0; i < czSql.getBytes().length ; i++) {
			czBuf[14 +  i] = czSql.getBytes()[i];
		}
		byte crc = getXOR(czBuf, 2, iLen - 5);
		czBuf[14+  czSql.getBytes().length ] = crc;
		czBuf[15 +  czSql.getBytes().length ] = (byte) ( 0xEE);
		czBuf[16+  czSql.getBytes().length ] = (byte) ( 0xFF);
		System.out.println("BlueToothMsgForSend-->"+ ConvertData.bytesToHexString(czBuf,true));

		return HJBleApplication.shareInstance().isEncrypt()?transformNS(czBuf,czBuf.length):czBuf;
	}

	/**
	 * 带有分包号总包号的力合蓝牙通信新协议
	 * @param czSql
	 * @param bagNo
	 * @param bagNum
	 * @return
	 */
	public static byte[] getBlueToothMsgForSend( String czSql,int bagNo,int bagNum) {

		int iLen = czSql.getBytes().length + 17;
		byte[] czBuf = new byte[iLen];
		czBuf[0] = (byte) 0xAA;
		czBuf[1] = (byte) 0xBB;
		czBuf[2] = (byte) (iLen >> 8);
		czBuf[3] = (byte) (iLen & 0xff);
		for (int i = 0; i < 4; i++) {
			czBuf[4 + i] = "lihe".getBytes()[i];
		}
		czBuf[8] = (byte) (bagNo >> 8);
		czBuf[9] = (byte) (bagNo & 0xff);
		czBuf[10] = (byte) (bagNum >> 8);
		czBuf[11] = (byte) (bagNum & 0xff);
		czBuf[12 ] = (byte) (czSql.getBytes().length  >> 8);
		czBuf[13] = (byte) (czSql.getBytes().length  & 0xff);
		for (int i = 0; i < czSql.getBytes().length ; i++) {
			czBuf[14 +  i] = czSql.getBytes()[i];
		}
		byte crc = getXOR(czBuf, 2, iLen - 5);
		czBuf[14+  czSql.getBytes().length ] = crc;
		czBuf[15 +  czSql.getBytes().length ] = (byte) ( 0xEE);
		czBuf[16+  czSql.getBytes().length ] = (byte) ( 0xFF);
		System.out.println("BlueToothMsgForSend-->"+ ConvertData.bytesToHexString(czBuf,true));

		return HJBleApplication.shareInstance().isEncrypt()?transformNS(czBuf,czBuf.length):czBuf;
	}


	public static byte[] getBlueToothMsgForReturn(
												 String czSql) {
		String desID="lihe0000";
		int iLen = czSql.getBytes().length + 17;
		byte[] czBuf = new byte[iLen];
		czBuf[0] = (byte) 0xAA;
		czBuf[1] = (byte) 0xBB;
		czBuf[2] = (byte) (iLen >> 8);
		czBuf[3] = (byte) (iLen & 0xff);
		for (int i = 0; i < 8; i++) {
			czBuf[4 + i] = desID.getBytes()[i];
		}
		czBuf[12 ] = (byte) (czSql.getBytes().length  >> 8);
		czBuf[13] = (byte) (czSql.getBytes().length  & 0xff);
		for (int i = 0; i < czSql.getBytes().length ; i++) {
			czBuf[14 +  i] = czSql.getBytes()[i];
		}
		byte crc = getXOR(czBuf, 2, iLen - 5);
		czBuf[14+  czSql.getBytes().length ] = crc;
		czBuf[15 +  czSql.getBytes().length ] = (byte) ( 0xEE);
		czBuf[16+  czSql.getBytes().length ] = (byte) ( 0xFF);
		System.out.println("BlueToothMsgForReturn-->"+ ConvertData.bytesToHexString(czBuf,true));

		return czBuf;
	}

	public static String getReturnMsgForReceive(byte[] receiveBytes){
		if (receiveBytes==null||receiveBytes.length<=0){
			reciveData=null;
			return Constant.ReceiveFalse;
		}
		System.out.println("ReturnMsg-->"+ ConvertData.bytesToUtf8(receiveBytes));
		System.out.println("ReturnMsg receiveBytes-->"+ Arrays.toString(receiveBytes));
		if (receiveBytes[0]==(byte)0xAA&&receiveBytes[1]==(byte)0xBB&&receiveBytes[receiveBytes.length-2]==(byte)0xEE&&receiveBytes[receiveBytes.length-1]==(byte)0xFF){

			if (getShort(receiveBytes[2],receiveBytes[3])==receiveBytes.length){
				if (receiveBytes[receiveBytes.length-3]!=getXOR(receiveBytes,2,receiveBytes.length-5)){
					reciveData=null;
					return Constant.ReceiveFalse;
				}
				byte[] dataType=new byte[4];
				System.arraycopy(receiveBytes,4,dataType,0,4);
				short index=getShort(receiveBytes[8],receiveBytes[9]);//13622
				short total=getShort(receiveBytes[10],receiveBytes[11]);//14136
				short dataLen=getShort(receiveBytes[12],receiveBytes[13]);
				byte[] recData=new byte[dataLen];
				System.arraycopy(receiveBytes,14,recData,0,dataLen);
				if ("LIHE".equals(ConvertData.bytesToUtf8(dataType).trim())){
					if (total<=0){
						reciveData=null;
						return ConvertData.bytesToUtf8(recData);
					}else {
						if (reciveData==null||reciveData.getDataList()==null||reciveData.getDataList().length!=total){
							reciveData=new ReciveData();
							reciveData.setDataList(new byte[total][]);
						}
						switch (reciveData.addData2List(index,recData)){
							case 0:
								reciveData=null;
								return Constant.ReceiveDataListFalse;
							case 1:
								return Constant.ReceiveContinue;
							case 2:
								int totalByteLength=0;
								for (byte[] bData:reciveData.getDataList()){
									totalByteLength=totalByteLength+bData.length;
								}
								ByteBuffer bufferTotal = ByteBuffer.allocate(totalByteLength);
								for (byte[] bData:reciveData.getDataList()){
									bufferTotal.put(bData);
								}
								reciveData=null;
								return ConvertData.bytesToUtf8(bufferTotal.array());
						}
						reciveData=null;
						return Constant.ReceiveDataListFalse;
					}
				}else {
					reciveData=null;
					return ConvertData.bytesToUtf8(recData);
				}

			}else {
				reciveData=null;
				return Constant.ReceiveFalse;
			}

		}else {
			reciveData=null;
			return Constant.ReceiveFalse;
		}
	}

	public static short getShort(byte h, byte l) {
		return (short) (((h << 8) | l & 0xff));
	}

	/**
	 * 通信加密
	 * @param czBuf 转换前字节数组
	 * @param length 字节数组长度
	 * @return  转换后字节数组
	 */
	public static byte[] transformNS(byte[] czBuf,int length){
		short key=256;
		Log.w("ProtocolUtil","transformNS() czBuf:"+ConvertData.bytesToHexString(czBuf,true));
		byte c1=6;
		byte c2=9;
		byte[] cdBuf=new byte[length];
		for (int i=0;i<length;i++){
			cdBuf[i]= (byte) (czBuf[i]^(key>>8));
			key= (short) (((cdBuf[i]&0xff)+key)*c1+c2);//Java的字节是有符号的，所以参与普通运算时候要加入&0xff来转换成无符号
		}
		byte[] resultBuf=new byte[length*2];
		byte[] tmpBuf=new byte[2];
		byte temp;
		for (int k=0;k<length;k++){
			temp=cdBuf[k];
			tmpBuf[0]= (byte) (65+(temp&0xff)/26);
			tmpBuf[1]=(byte) (65+(temp&0xff)%26);
			resultBuf[k*2]=tmpBuf[0];
			resultBuf[k*2+1]=tmpBuf[1];
		}
		Log.w("ProtocolUtil","transformNS() resultBuf:"+ConvertData.bytesToHexString(resultBuf,true));
		try {
			String s=new String(resultBuf,"UTF-8");
			Log.w("ProtocolUtil",s);
		}catch (UnsupportedEncodingException  exception){
			exception.printStackTrace();
		}

		return resultBuf;
	}

	/**
	 * 串口通信读取指令
	 * @param modbusAdr ModBus地址 eg 0x55
	 * @param firstNo 起始读取位
	 * @param count 读取寄存器个数
	 * @return
	 */
	public static String getReadHexData(int modbusAdr,int firstNo,int count){
		firstNo=firstNo-40001;
		byte[] bytes=new byte[6];
		bytes[0]= (byte) (modbusAdr&0xFF);
		bytes[1]=0x03;
		bytes[2]= (byte) (firstNo>>8&0xFF);
		bytes[3]=(byte) (firstNo&0xFF);
		bytes[4]= (byte) (count>>8&0xFF);
		bytes[5]=(byte) (count&0xFF);
		int crcValue=CRC16ModBusUtils.getCRC(bytes);
		StringBuffer hexData=new StringBuffer();
		for (byte b:bytes){
			hexData.append(String.format("%02X", b));
		}

		hexData.append(CRC16ModBusUtils.bytesToHexString(CRC16ModBusUtils.crc16ModbusToLittleEndianBytes(crcValue)));
		return  hexData.toString();
	}

	public static SerialData getReturnMsgForSerial(String hexData){

		if (hexData==null||hexData.length()<14){
			return null;
		}else if (hexData.length()%2!=0){

			return new SerialData(hexData,-1);
		}
		byte[] bytes=new byte[hexData.length()/2];
		for (int i=0;i<bytes.length;i++){
			bytes[i]=(byte)(Integer.parseInt(hexData.substring(i*2,i*2+2),16)&0xFF);//仅保留低8位转换成字节
		}


		if (CRC16ModBusUtils.verifyReceivedFrame(bytes)){//CRC校验通过
			SerialData serialData=new SerialData(hexData);
			serialData.setRowIndex(((bytes[0]&0xFF)<<8)+bytes[1]&0xFF);
			serialData.setTotalRowNum(((bytes[2]&0xFF)<<8)+bytes[3]&0xFF);
			serialData.setModbusAdr(bytes[0]&0xFF);
			serialData.setOrderType(bytes[1]);
			if (serialData.getOrderType()==0x03){
				serialData.setByteCount(bytes[2]&0xFF);
				List<MRegister> mRegisters=new ArrayList<>();
				for (int i=0;i<serialData.getByteCount()/2;i++){
					MRegister mRegister=new MRegister();
					mRegister.setValue((bytes[2*i+4]&0xFF)+((bytes[2*i+3]&0xFF)<<8));
					mRegisters.add(mRegister);
				}
				serialData.setmRegisters(mRegisters);

			}else if (serialData.getOrderType()==0x10){
				int count=((bytes[2]&0xFF)<<24)+((bytes[3]&0xFF)<<16)+((bytes[4]&0xFF)<<8)+(bytes[5]&0xFF);//包含了默认的起始位置参数个数
				//int count=((bytes[4]&0xFF)<<8)+(bytes[5]&0xFF);
				serialData.setByteCount(count);
			}
			serialData.setCrc((bytes[bytes.length-1]&0xFF)|(bytes[bytes.length-2]&0xFF)<<8);
			return serialData;
		}else {
			return new SerialData(hexData,-2);
		}
	}


	/**
	 * 串口通信读取指令
	 * @param modbusAdr ModBus地址 eg 0x55
	 * @param value40001 起始写入位置
	 * @param data 写入的参数值列表 不得超过127个待写入参数！！！
	 * @return
	 */
	public static String getWriteHexData(int modbusAdr,int value40001,List<Integer> data){
		if (value40001>40000){
			value40001=value40001-40000;//20260207 大于40000的表示是力合的，需要减少40000。小于40000的表示是通用的，已经考虑了减40001
		}

		if (data==null||data.size()<=0){
			return null;
		}else if (data.size()>127){
			Log.w("ProtocolUtil","getWriteHexData():"+data.size()+"参数超长，大于127个");

			return "";
		}
		int count=data.size()+1;//数据默认包含第一个起始位置的参数
		byte[] bytes=new byte[7+count*2];
		bytes[0]= (byte) (modbusAdr&0xFF);
		bytes[1]=0x10;

		bytes[2]= (byte) (count>>24&0xFF);
		bytes[3]= (byte) (count>>16&0xFF);
		bytes[4]= (byte) (count>>8&0xFF);
		bytes[5]=(byte) (count&0xFF);
		bytes[6]=(byte) ((2*count)&0xFF);
		bytes[7]= (byte) (value40001>>8&0xFF);
		bytes[8]=(byte) (value40001&0xFF);
		for (int i=0;i<count-1;i++){
			bytes[9+i*2]= (byte) (data.get(i)>>8&0xFF);
			bytes[10+i*2]=(byte) (data.get(i)&0xFF);
		}
		int crcValue=CRC16ModBusUtils.getCRC(bytes);
		StringBuffer hexData=new StringBuffer();
		for (byte b:bytes){
			hexData.append(String.format("%02X", b));
		}

		hexData.append(CRC16ModBusUtils.bytesToHexString(CRC16ModBusUtils.crc16ModbusToLittleEndianBytes(crcValue)));
		return  hexData.toString();
	}


	/**
	 * 串口通信读取指令
	 * @param modbusAdr ModBus地址 eg 0x55
	 * @param value40001 起始写入位置
	 * @param data 写入的参数值列表 不得超过127个待写入参数！！！
	 * @return
	 */
	public static String getWriteHexDataEndLength(int modbusAdr,int value40001,List<Integer> data){
		if (value40001>40000){
			value40001=value40001-40000;//20260207 大于40000的表示是力合的，需要减少40000。小于40000的表示是通用的，已经考虑了减40001
		}

		if (data==null||data.size()<=0){
			return null;
		}else if (data.size()>127){
			Log.w("ProtocolUtil","getWriteHexData():"+data.size()+"参数超长，大于127个");

			return "";
		}
		int count=data.size() ;//数据寄存器个数
		byte[] bytes=new byte[7+count*2];
		bytes[0]= (byte) (modbusAdr&0xFF);
		bytes[1]=0x10;
		bytes[2]= (byte) (value40001>>8&0xFF);
		bytes[3]=(byte) (value40001&0xFF);
		bytes[4]= (byte) (count>>8&0xFF);
		bytes[5]=(byte) (count&0xFF);
		bytes[6]=(byte) ((2*count)&0xFF);
		for (int i=0;i<count;i++){
			bytes[7+i*2]= (byte) (data.get(i)>>8&0xFF);
			bytes[8+i*2]=(byte) (data.get(i)&0xFF);
		}
		int crcValue=CRC16ModBusUtils.getCRC(bytes);
		StringBuffer hexData=new StringBuffer();
		for (byte b:bytes){
			hexData.append(String.format("%02X", b));
		}
		hexData.append(CRC16ModBusUtils.bytesToHexString(CRC16ModBusUtils.crc16ModbusToLittleEndianBytes(crcValue)));
		return  hexData.toString();
	}

}