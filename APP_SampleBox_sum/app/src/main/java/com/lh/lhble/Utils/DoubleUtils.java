package com.lh.lhble.Utils;

import java.math.BigDecimal;

/**
 * Created by tom on 2018/8/1.
 */

public class DoubleUtils {
    // 默认除法运算精度
    private static final int DEF_DIV_SCALE = 3;


//private static Logger logger = Logger.getLogger(DoubleUtil.class);


    /**
     * 判断double是否小数点后有值
     * @param d 待判断的double
     * @return true：有小数部分；false：无小数部分
     */
    public static boolean hasDecimalPart(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new IllegalArgumentException("无效的double值：" + d);
        }
        // 向上取整和向下取整都等于自身 → 是整数（无小数）
        return Math.ceil(d) != d;
        // 或用 floor：return Math.floor(d) != d; 结果一致
    }


    /**
     * 提供精确的加法运算。
     *
     * @param v1
     *            被加数
     * @param v2
     *            加数
     * @return 两个参数的和
     */
    public static double add(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.add(b2).doubleValue();
    }


    /**
     * 提供精确的减法运算。
     *
     * @param v1
     *            被减数
     * @param v2
     *            减数
     * @return 两个参数的差
     */
    public static double sub(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.subtract(b2).doubleValue();
    }


    /**
     * 提供精确的乘法运算。
     *
     * @param v1
     *            被乘数
     * @param v2
     *            乘数
     * @return 两个参数的积
     */


    public static double mul(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.multiply(b2).doubleValue();
    }


    /**
     *
     * 提供（相对）精确的除法运算，当发生除不尽的情况时，精确到
     *
     * 小数点以后2位，以后的数字四舍五入。
     *
     * @param v1
     *            被除数
     *
     * @param v2
     *            除数
     *
     * @return 两个参数的商
     *
     */


    public static double div(double v1, double v2) {
        return div(v1, v2, DEF_DIV_SCALE);
    }


    /**
     *
     * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指
     *
     * 定精度，以后的数字四舍五入。
     *
     * @param v1
     *            被除数
     *
     * @param v2
     *            除数
     *
     * @param scale
     *            表示表示需要精确到小数点以后几位。
     *
     * @return 两个参数的商
     *
     */


    public static double div(double v1, double v2, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();


    }


    /**
     *
     * 提供精确的小数位四舍五入处理。
     *
     * @param v
     *            需要四舍五入的数字
     *
     * @param scale
     *            小数点后保留几位
     *
     * @return 四舍五入后的结果
     *
     */

    public static double round(double v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b = new BigDecimal(Double.toString(v));
        BigDecimal one = new BigDecimal(1);
        return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();


    }

    public static boolean isEmpty(Double[] str) {
        return str == null || str.length == 0;
    }

    public static double getScaleOfNum(double wcl, int scale) {
        BigDecimal b = new BigDecimal(wcl);
        return b.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 有多少个整数位
     * @param value
     * @return
     */
    public static int getIntegerDigitCount(double value) {
        if (value < 0) {
            value = -value; // 取绝对值，忽略符号
        }
        if (value == 0) {
            return 1;
        }
        if (value < 1) {
            return 1; // 整数部分是 0，算 1 位（如 0.5 → "0"）
        }

        // 取整数部分（防止 159.9999999 因浮点误差影响 log）
        long integerPart = (long) Math.floor(value);

        // 防止 floor 后为 0（理论上不会，但安全起见）
        if (integerPart == 0) return 1;

        return (int) Math.floor(Math.log10(integerPart)) + 1;
    }
}
