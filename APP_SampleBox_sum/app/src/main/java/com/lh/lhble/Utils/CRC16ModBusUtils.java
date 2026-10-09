package com.lh.lhble.Utils;

import com.lh.lhble.base.HJBleApplication;
import com.wise.wisekit.utils.ToastUtils;

/**
 * CRC-16/MODBUS 校验工具类（严格遵循Modbus协议规范）
 * 适用场景：Modbus RTU/TCP协议的数据完整性校验
 */
public class CRC16ModBusUtils {

    // CRC-16/MODBUS 固定参数
    private static final int POLY = 0x8005;    // 生成多项式
    private static final int INIT_VALUE = 0xFFFF; // 初始值
    private static final int FINAL_XOR = 0x0000; // 最终异或值


    public static int getCRC(byte[] bytes) {
        int CRC = 0x0000ffff;
        int POLYNOMIAL = 0x0000a001;

        int i, j;
        for (i = 0; i < bytes.length; i++) {
            CRC ^= ((int) bytes[i] & 0x000000ff);
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) != 0) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }
        return CRC;
    }



    /**
     * 将CRC-16/MODBUS校验值转为Modbus要求的小端字节数组（2字节）
     * @param crcValue calculateCrc16Modbus返回的整数校验值
     * @return 小端字节数组：index0=低位字节，index1=高位字节（Modbus RTU强制顺序）
     */
    public static byte[] crc16ModbusToLittleEndianBytes(int crcValue) {
        byte lowByte = (byte) (crcValue & 0xFF);  // 低位字节（取低8位）
        byte highByte = (byte) (crcValue >> 8);   // 高位字节（右移8位，取高8位）
        return new byte[]{lowByte, highByte};     // 小端序：低位在前，高位在后
    }

    /**
     * 验证Modbus接收帧的CRC是否正确（含CRC校验码的完整帧）
     * @param receivedFrame 接收的完整帧（数据 + 2字节CRC校验码，小端序）
     * @return true：CRC校验通过；false：CRC校验失败
     * @throws IllegalArgumentException 若帧长度<2（不足CRC字节），抛出异常
     */
    public static boolean verifyReceivedFrame(byte[] receivedFrame) {
        // 完整帧必须包含至少2字节CRC，否则无效
        if (receivedFrame == null || receivedFrame.length < 2) {
            throw new IllegalArgumentException("接收帧长度不足，无法验证CRC");
        }

        // 1. 拆分“数据部分”和“接收的CRC字节”
        int dataLength = receivedFrame.length - 2;
        byte[] dataPart = new byte[dataLength];
        System.arraycopy(receivedFrame, 0, dataPart, 0, dataLength); // 数据部分（前n-2字节）

        byte[] receivedCrcBytes = new byte[2];
        System.arraycopy(receivedFrame, dataLength, receivedCrcBytes, 0, 2); // 接收的CRC（后2字节）

        // 2. 计算数据部分的CRC
        int calculatedCrc = getCRC(dataPart);
        byte[] calculatedCrcBytes = crc16ModbusToLittleEndianBytes(calculatedCrc);
        if (!(calculatedCrcBytes[0] == receivedCrcBytes[0]&& calculatedCrcBytes[1] == receivedCrcBytes[1])){
            ToastUtils.showErrorSingleMsg(HJBleApplication.shareInstance(),"cCRC:"+bytesToHexString(calculatedCrcBytes)+"\nrCRC:"+bytesToHexString(receivedCrcBytes));

        }
         // 3. 对比“计算的CRC”与“接收的CRC”是否一致
        return calculatedCrcBytes[0] == receivedCrcBytes[0]
                && calculatedCrcBytes[1] == receivedCrcBytes[1];
    }

    /**
     * 辅助方法：字节数组转为十六进制字符串（用于打印调试）
     * @param bytes 输入字节数组
     * @return 大写十六进制字符串（空格分隔，如“01 03 00 00 00 01 0B 94”）
     */
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString().trim(); // 去除末尾空格
    }
}
