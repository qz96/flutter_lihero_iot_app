package com.lh.lhble.Utils;

import android.text.TextUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * description ：
 * project name：CClud2
 * author : Vincent
 * creation date: 2017/5/14 21:33
 * @version 1.0
 */

public class DateUtil {

    public static String DATE_PATTERN = "yyyy-MM-dd";
    public static String SHORT_DATE_PATTERN = "yy-MM-dd";
    public static String WEEK_PATTERN = "yyyy/MM/dd";
    public static String MONTH_PATTERN = "yyyy-MM";
    public static String TIME_PATTERN = "HH:mm:ss";
    public static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static String METHOD_DATE_TIME_PATTERN = "yyyyMMddHHmmss";
    public static String NO_YEAR_DATE_TIME_PATTERN = "MM-dd HH:mm";
    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time(long time){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d = new Date(time);

        return sf.format(d);
    }

    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time2(long time){
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        Date d = new Date(time);
        return sf.format(d);
    }




    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time3(long time){
        SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss");
        Date d = new Date(time);
        return sf.format(d);
    }


    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time4(long time){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date d = new Date(time);
        return sf.format(d);
    }

    /**
     * 显示时间，如果与当前时间差别小于一天，则自动用**秒(分，小时)前，如果大于一天则用format规定的格式显示
     *
     * @author wxy
     * @param time
     *            时间戳
     * @param format
     *            格式 格式描述:例如:yyyy-MM-dd yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static String long2time5(long time, String format) {
        String r = "";
        if(time==0)return r;
        if(format==null)format="yyyy-MM-dd HH:mm:ss";

        long nowtimelong = System.currentTimeMillis();

        long ctimelong = time;
        long result = Math.abs(nowtimelong - ctimelong);

        if(result < 60000){// 一分钟内
            long seconds = result / 1000;
            if(seconds == 0){
                r = "刚刚";
            }else{
                r = seconds + "秒前";
            }
        }else if (result >= 60000 && result < 3600000){// 一小时内
            long seconds = result / 60000;
            r = seconds + "分钟前";
        }else if (result >= 3600000 && result < 86400000){// 一天内
            long seconds = result / 3600000;
            r = seconds + "小时前";
        }else if (result >= 86400000 && result < 604800000){//七天内
            long seconds = result / 86400000;
            r = seconds + "天前";
        }else{// 日期格式
            SimpleDateFormat df = new SimpleDateFormat(format);
            r = df.format(new Date(ctimelong)).toString();
        }
        return r;
    }
    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time6(long time){
        SimpleDateFormat sf = new SimpleDateFormat("mm:ss");
        Date d = new Date(time);
        return sf.format(d);
    }

    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time7(long time){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM");
        Date d = new Date(time);
        return sf.format(d);
    }

    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time8(long time){
        SimpleDateFormat sf = new SimpleDateFormat("HH:mm");
        Date d = new Date(time);
        return sf.format(d);
    }
    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time9(long time){
        if (time<=0)return " ";
        SimpleDateFormat sf = new SimpleDateFormat("MM/dd HH:mm");
        Date d = new Date(time);
        return sf.format(d);
    }

    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time10(long time){
        if (time<=0)return " ";
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date d = new Date(time);
        return sf.format(d);
    }

    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time11(long time){
        if (time<=0)return " ";
        SimpleDateFormat sf = new SimpleDateFormat("ddHHmmss");
        Date d = new Date(time);
        return sf.format(d);
    }

    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time12(long time){
        if (time<=0)return " ";
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        Date d = new Date(time);
        return sf.format(d);
    }

    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time13(long time){
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMM");
        Date d = new Date(time);
        return sf.format(d);
    }

    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time14(long time){
        if (time<=0)return " ";
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmm");
        Date d = new Date(time);
        return sf.format(d);
    }
    /**
     * 时间戳转为时间
     * @param time
     */
    public static String long2time15(long time){
        if (time<=0)return " ";
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        Date d = new Date(time);
        return sf.format(d);
    }
    /**
     * 把指定格式的的时间转化为毫秒
     *
     * @param time
     * @param format
     * @return
     */
    public static long format2miao(String time, String format) {
        if (StringUtils.isEmpty(time))return 0;
        SimpleDateFormat sf = new SimpleDateFormat(format);
        try {
            Date date = sf.parse(time);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //C#DateTime的Json时间格式转化为Date时间戳
    public static long JsonStringToDateLong(String jsonDateTime){
        Date date = JsonStringToDate(jsonDateTime);
        return date.getTime();
    }

    //C#DateTime的Json时间格式转化为int
    public static int JsonStringToDateInt(String jsonDateTime) {
        Date date = JsonStringToDate(jsonDateTime);

        String str_Date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        str_Date= sdf.format(date);
        str_Date=str_Date.replace("-", "");
        return Integer.parseInt(str_Date);
    }

    //Date字符串转化为C#DateTime的Json时间格式
    public static String DateStringToJsonString(String str_Date) {
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = fmt.parse(str_Date);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        long l = date.getTime();
        String str_DateTime = "/Date(" + l + "+0800)/";
        return str_DateTime ;
    }

    //Date to String
    public static String DateToString(Date date) {
        String str_Date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        str_Date= sdf.format(date);
        return str_Date;
    }
    //C#DateTime的JsonString时间格式转化为Date
    public  static Date JsonStringToDate(String jsonDateTime) {
        if (jsonDateTime==null||jsonDateTime.isEmpty())return null;
        int idx1 = jsonDateTime.indexOf("(");
        int idx2 = jsonDateTime.indexOf(")") - 5;
        String str_DateTime = jsonDateTime.substring(idx1 + 1, idx2);
        long l = Long.valueOf(str_DateTime);
        Date date = new Date(l);
        return date;
    }


    //C#DateTime的JsonString时间格式转化为yyyy-MM-dd HH:mm:ss
    public  static String JsonStringToStr(String jsonDateTime) {
        if (jsonDateTime==null||jsonDateTime.length()<5)return null;
        int idx1 = jsonDateTime.indexOf("(");
        int idx2 = jsonDateTime.indexOf(")") - 5;
        String str_DateTime = jsonDateTime.substring(idx1 + 1, idx2);
        long l = Long.valueOf(str_DateTime);

        return long2time(l);
    }

    public static String formatTime(String time, String pattern) {
        if(TextUtils.isEmpty(time)) {
            return "1970-01-01";
        } else {
            String newTime = "";

            try {
                Date e = (new SimpleDateFormat(DATE_TIME_PATTERN)).parse(time);
                long timeMillis = e.getTime();
                newTime = (new SimpleDateFormat(pattern)).format(Long.valueOf(timeMillis));
            } catch (ParseException var6) {
                newTime = time;
                var6.printStackTrace();
            }

            return newTime;
        }
    }

    public static String formatTime(String time, String prePattern, String pattern) {
        if(TextUtils.isEmpty(time)) {
            return "1970-01-01";
        } else {
            String newTime = "";

            try {
                Date e = (new SimpleDateFormat(prePattern)).parse(time);
                long timeMillis = e.getTime();
                newTime = (new SimpleDateFormat(pattern)).format(Long.valueOf(timeMillis));
            } catch (ParseException var7) {
                newTime = time;
                var7.printStackTrace();
            }

            return newTime;
        }
    }

    public static long getTimeMillis(String time, String pattern) {
        Date date = null;

        try {
            date = (new SimpleDateFormat(pattern)).parse(time);
        } catch (ParseException var4) {
            var4.printStackTrace();
            return 0L;
        }

        return date.getTime();
    }

    public static Calendar getStringFormat2Calendar(String time, String dateFormat){
        Date data = new SimpleDateFormat(dateFormat).parse(time, new ParsePosition(0));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(data);
        return calendar;
    }
}
