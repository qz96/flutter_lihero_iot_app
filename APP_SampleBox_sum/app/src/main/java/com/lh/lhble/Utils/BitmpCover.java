package com.lh.lhble.Utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.widget.Toast;

import com.lh.lhble.base.HJBleApplication;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BitmpCover {

	private static FileOutputStream outputStream;

	public static Bitmap revitionImageSize(String path) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(
				new File(path)));
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(in, null, options);
		in.close();
		int i = 0;
		Bitmap bitmap = null;
		while (true) {
			if ((options.outWidth >> i <= 1000)&& (options.outHeight >> i <= 1000)) {
				in = new BufferedInputStream(new FileInputStream(new File(path)));
				options.inSampleSize = (int) Math.pow(2.0D, i);
				options.inJustDecodeBounds = false;
				bitmap = BitmapFactory.decodeStream(in, null, options);
				break;
			}
			i += 1;
		}

		return bitmap;
	}
	
	public static void compressBmpToFile(String path){
		BitmapFactory.Options option = new BitmapFactory.Options();
		Bitmap bmp;
		option.inJustDecodeBounds  = true;
		option.inSampleSize=1;
		BitmapFactory.decodeFile(path,option);
		int hz=option.outHeight/1024;
		int wz=option.outWidth/1024;
		if (hz>1&&wz>1){
			option.inSampleSize=wz;
		}
		option.inJustDecodeBounds=false;
		bmp= BitmapFactory.decodeFile(path,option);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int options =96;
			bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
			while (options>5&&baos.toByteArray().length / 1024 > 512) {
				baos.reset();
				options -= 5;
				bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
			}
			FileOutputStream fos = new FileOutputStream(path);
			fos.write(baos.toByteArray());
			fos.flush();
			fos.close();
			bmp.recycle();
		}catch (NullPointerException ne){
			ne.printStackTrace();
			Toast.makeText(HJBleApplication.getAppContext(),(StringUtils.isEmpty(path)?"":path+"\n")+"请授权应用读取存储和拍照权限",Toast.LENGTH_LONG).show();

		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Bitmap bmpDrawText(String path, float x, float y, String context){
		if (StringUtils.isEmpty(context)){
			context="";
		}
		int n=context.split("\n").length;
		int textSize=38;
		Bitmap bitmap = BitmapFactory.decodeFile(path);
		if (bitmap==null){
			return null;
		}
		//添加文字
		Bitmap bmpTemp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
		Canvas cv = new Canvas(bmpTemp);
		TextPaint p = new TextPaint();
		Typeface font = Typeface.create("宋体", Typeface.NORMAL);
		p.setColor(Color.CYAN);
		p.setTypeface(font);
		p.setTextSize(textSize);
		p.setAntiAlias(true);
		StaticLayout layoutopen = new StaticLayout(context, p, bitmap.getWidth()-textSize , Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
		cv.drawBitmap(bitmap, 0, 0, p);
		cv.save();
		cv.translate(x,y);
	//	cv.translate(x,bitmap.getHeight()-n*(4*textSize/3)-y);//将水印移到照片左下角
		layoutopen.draw(cv);
		cv.restore();
		try {
			outputStream = new FileOutputStream(new File(path));
			bmpTemp.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}finally {
			if (outputStream!=null){
				try {
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bmpTemp;
	}

	
	public static Bitmap getHttpBitmap(String url){
    	URL myFileURL;
    	Bitmap bitmap=null;
    	try{
    		myFileURL = new URL(url);
    		//获得连接
    		HttpURLConnection conn=(HttpURLConnection)myFileURL.openConnection();
    		//设置超时时间为6000毫秒，conn.setConnectionTiem(0);表示没有时间限制
    		conn.setConnectTimeout(6000);
    		//连接设置获得数据流
    		conn.setDoInput(true);
    		//不使用缓存
    		conn.setUseCaches(false);
    		//这句可有可无，没有影响
    		//conn.connect();
    		//得到数据流
    		InputStream is = conn.getInputStream();
    		//解析得到图片
    		bitmap = BitmapFactory.decodeStream(is);
    		//关闭数据流
    		is.close();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	
		return bitmap;
    	
    }

	public static Bitmap convertViewToBitmap(View view) {
		view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		view.buildDrawingCache();
		Bitmap bitmap = view.getDrawingCache();
		return bitmap;
	}


	public static String  getImagePath(Context context,android.content.Intent intent) {
		Uri uri = intent.getData();
		String path="";
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
		if (cursor==null){
			return path;
		}else if (cursor.moveToFirst()){
			 if (cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)>=0){
				String disPlayName=cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
				cursor.close();
				cursor=context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,null,MediaStore.Images.ImageColumns.DISPLAY_NAME+"=?",new String[]{disPlayName},null);
				if (cursor!=null&&cursor.moveToFirst()){
					int index=cursor.getColumnIndex(MediaStore.Images.Media.DATA);
					if (index>=0)path=cursor.getString(index);
				}
			}else if (cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)>=0){
				 String ID=cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID));
				 cursor.close();
				 cursor=context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,null,MediaStore.Images.ImageColumns._ID+"=?",new String[]{ID},null);
				 if (cursor!=null&&cursor.moveToFirst()){
					 int index=cursor.getColumnIndex(MediaStore.Images.Media.DATA);
					 if (index>=0)path=cursor.getString(index);
				 }
			 }else if (cursor.getColumnCount()>0){
				 StringBuffer sb=new StringBuffer();
				 sb.append("ccn");
				 for (String s:cursor.getColumnNames()){
					 sb.append(","+s);
				 }
				 path=sb.toString();
			 }
		}
		return path;
	}
	public static String  getVideoPath(Context context,android.content.Intent intent) {
		Uri uri = intent.getData();
		String path="";
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
		if (cursor==null){
			return path;
		}else if (cursor.moveToFirst()){
			if (cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)>=0){
				String disPlayName=cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME));
				cursor.close();
				cursor=context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,null,MediaStore.Video.VideoColumns.DISPLAY_NAME+"=?",new String[]{disPlayName},null);
				if (cursor!=null&&cursor.moveToFirst()){
					int index=cursor.getColumnIndex(MediaStore.Video.Media.DATA);
					if (index>=0)path=cursor.getString(index);
				}
			}else if (cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID)>=0){
				String ID=cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID));
				cursor.close();
				cursor=context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,null,MediaStore.Video.VideoColumns._ID+"=?",new String[]{ID},null);
				if (cursor!=null&&cursor.moveToFirst()){
					int index=cursor.getColumnIndex(MediaStore.Video.Media.DATA);
					if (index>=0)path=cursor.getString(index);
				}
			}else if (cursor.getColumnCount()>0){
				StringBuffer sb=new StringBuffer();
				sb.append("ccn");
				for (String s:cursor.getColumnNames()){
					sb.append(","+s);
				}
				path=sb.toString();
			}
		}
		return path;
	}
}
