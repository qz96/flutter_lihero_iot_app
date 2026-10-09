package com.lh.lhble.Entity;

public class MRegister {
	private int value;

	public MRegister() {
	}

	public MRegister(int value_) {
		value = value_;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public int getHiByteValue() {
		 String str= Integer.toHexString(value);
		 int count=str.length();
		 for(int i=0; i<4-count;i++)
			 str="0"+str;		 
		 String hi=str.substring(0,2);
		 int hiv= Integer.parseInt(hi, 16);
		 return hiv;
	}
	//疑似应该是charAt(0) charAt(1)
	public String getHiByteHexStr(){
		 String str= Integer.toHexString(value);
		 int count=str.length();
		 for(int i=0; i<4-count;i++)
			 str="0"+str;		 
		 String hi=str.charAt(1)+""+str.charAt(2);
		 return hi.toUpperCase();
	}
	public int getLoByteValue() {
		 String str= Integer.toHexString(value);
		 int count=str.length();
		 for(int i=0; i<4-count;i++)
			 str="0"+str;
		 String lo=str.substring(2);
		 int lov= Integer.parseInt(lo, 16);
		 return lov;
	}
	//疑似应该是charAt(2) charAt(3)
	public String getLoByteHexStr(){
		 String str= Integer.toHexString(value);
		 int count=str.length();
		 for(int i=0; i<4-count;i++)
			 str="0"+str;		 
		 String lo=str.charAt(0)+""+str.charAt(3) ;
		 return lo.toUpperCase();
	}
	public String getStringValue(){
		 String str= Integer.toHexString(value);
		 int count=str.length();
		 for(int i=0; i<4-count;i++)
			 str="0"+str;	
		 return str;
	}

	/**
	 * 0-9的ASCII码 转成数字后的字符串
	 * @return
	 */
	public String getAsciiIntegerStr(){
		String str=(char)getHiByteValue()+""+(char)getLoByteValue();
		return str;
	}
}
