package com.lh.lhble.Utils;


import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Utilty {

    private static Utilty instance = new Utilty();

    public static Utilty getInstance() {
        return instance;
    }

    public static final int MIN_MID_VALUE = 1;

    public static final int MAX_MID_VALUE = 65535;

    /* 
    * CRC16-ITU lookup table as calculated by the table generator. 
    * The CRC-16 generator polynomial(1021): x0 + x5 + x12 + x16. 
    */  
    public static final int[] itu_crc16Table = {
    		0x0000, 0x1081, 0x2102, 0x3183, 0x4204, 0x5285, 0x6306, 0x7387, 
    		0x8408, 0x9489, 0xa50a, 0xb58b, 0xc60c, 0xd68d, 0xe70e, 0xf78f 
    };

    //
    public int bytes2Int(byte[] b, int start, int length) {
        int sum = 0;
        int end = start + length;
        for (int k = start; k < end; k++) {
            int n = ((int) b[k]) & 0xff;
            n <<= (--length) * 8;
            sum += n;
        }
        return sum;
    }

    //
    public byte[] int2Bytes(int value, int length) {
        byte[] b = new byte[length];
        for (int k = 0; k < length; k++) {
            b[length - k - 1] = (byte) ((value >> 8 * k) & 0xff);
        }
        return b;
    }

    //
    public boolean isValidofMid(int mId) {
        if (mId < MIN_MID_VALUE || mId > MAX_MID_VALUE) {
            return false;
        }
        return true;
    }

    /***
     * 将HEX字节数据显示的转换成HEX字符串数据
     */
    public static String parseByte2HexStr(byte[] buf) {
        if (null == buf) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }
    
    public static byte charToByte(char c) {   
        return (byte) "0123456789ABCDEF".indexOf(c);   
    } 
    
    // 16进制字符串转byte[]
    public static byte[] parseHexStringToBytes(String hexString) {   
        if (hexString == null || hexString.equals("")) {   
            return null;   
        }   
        hexString = hexString.toUpperCase();   
        int length = hexString.length() / 2;   
        char[] hexChars = hexString.toCharArray();   
        byte[] d = new byte[length];   
        for (int i = 0; i < length; i++) {   
            int pos = i * 2;   
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));   
        }   
        return d;   
    }    
    
    // 计算校验
    public static byte getCs(byte[] buf, int pos, int len) {   
    	byte bCs = 0;

        for (int i = 0; i < len; i++) {   
            bCs += buf[pos + i];
        }   
        
        bCs ^= 0xFF;
        return bCs;
    } 
    
    //
    public static int bytes2IntLittle(byte[] b, int start, int length) {
        int sum = 0;
        int end = start + length;
        int i = 0;
        for (int k = start; k < end; k++) {
            int n = ((int) b[k]) & 0xff;
            n <<= (i++) * 8;
            sum += n;
        }
        return sum;
    } 

    //
    public static byte[] int2BytesLittle(int value, int length) {
        byte[] b = new byte[length];
        for (int k = 0; k < length; k++) {
            b[k] = (byte) ((value >> 8 * k) & 0xff);
        }
        return b;
    }
    
    // 从byte数组的index处的连续4个字节获得一个float
    public static float bytes2FloatLittle(byte[] b, int start) {
        return Float.intBitsToFloat(bytes2IntLittle(b, start, 4));
    }
    
    // float转换为byte[4]数组  
    public static byte[] float2BytesLittle(float f) {  
        int intbits = Float.floatToIntBits(f);//将float里面的二进制串解释为int整数  
        return int2BytesLittle(intbits, 4);  
    }
    
    // itu-crc16 calc
    public static int getItuCrc16(byte[] b) {
    	int crc16 = 0xFFFF;
    	int i = 0;
    	
    	for (i = 0; i < b.length; i++) {    		
    		for (int j = 0; j < 5; j += 4) {
    			crc16 = (crc16 >> 4) ^ itu_crc16Table[(crc16 ^ (b[i] >> j)) & 0x0F];
    		}
    	}
    	
    	int tmp = (~crc16) & 0xFFFF;
    	
    	return tmp;
    }

    public static byte[] getAssetBinData(Context context,String binName) {
        AssetManager assetManager = context.getApplicationContext().getAssets();
        InputStream inputStream = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            inputStream = assetManager.open(binName);

            byte[] buffer = new byte[1024];
            int num = inputStream.read(buffer);
            while (num != -1) {
                baos.write(buffer, 0, num);
                num = inputStream.read(buffer);
            }
            baos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos!=null){
                    baos.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

}
