package com.lh.lhble.Utils;


import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class AESUtil {
	/**
	 * 解密
 	 * @param sSrc
	 * @param sKey
	 * @return
	 */
	public static String decrypt(String sSrc, String sKey) {
		if (sKey == null) {
			return null;
		}
		try {
			byte[] raw = sKey.getBytes("utf-8");
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec);
			try {
				//byte[] encrypted1 = Base64.getDecoder().decode(sSrc);// 先用base64解密
				byte[] encrypted1 = Base64.decode(sSrc, Base64.NO_WRAP);//略掉换行符模式
				byte[] original = cipher.doFinal(encrypted1);
				String originalString = new String(original, "utf-8");
				return originalString;
			} catch (Exception e) {

				return null;
			}
		} catch (Exception ex) {
			//logger.error("AES decrypt failed!", ex.getMessage());
			return null;
		}
	}

	/**
	 * 加密
	 * @param sSrc
	 * @param sKey
	 * @return
	 */
	public static String encrypt(String sSrc, String sKey) {
		if (sKey == null) {
			return null;
		}

		byte[] raw;
		try {
			raw = sKey.getBytes("utf-8");

			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");// "算法/模式/补码方式"
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
			//String result = Base64.getEncoder().encodeToString(encrypted);
			String result = Base64.encodeToString(encrypted,Base64.NO_WRAP);//略掉换行符模式
			return result;
		} catch (Exception e) {
			//logger.error("AES encrypt failed!", e.getMessage());
			return null;
		}
	}

	/**
	 * AES算法加密明文
	 * @param data 明文
	 * @param key 密钥，长度16
	 * @param iv 偏移量，长度16
	 * @return 密文
	 */
	public static byte[] encrypt128NoPadding(String data, byte[] key,String iv) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding"); //or NoPadding
			byte[] dataBytes = data.getBytes();
			if (data.getBytes().length%16!=0){
				dataBytes=new byte[(data.getBytes().length/16)*16+16];
				System.arraycopy(data.getBytes(), 0, dataBytes, 0, data.getBytes().length);


			}
			byte[] plaintext = new byte[dataBytes.length];
			System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);

			SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
			IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());

			cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
			byte[] encrypted = cipher.doFinal(plaintext);
			return encrypted;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
