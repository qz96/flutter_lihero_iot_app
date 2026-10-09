package com.lh.lhble.Utils;


import com.lh.lhble.base.Constant;
import com.lh.lhble.base.HJBleApplication;
import com.lh.lhble.R;

import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * description ：
 * project name：CCloud
 * author : Vincent
 * creation date: 2017/4/25 23:18
 *
 * @version 1.0
 */

public class StringUtils {
    /**
     * 验证手机号码
     *
     * @return
     * @throws PatternSyntaxException
     */
    public static boolean isChinaPhoneLegal(String phone) {

        String regex = "1[3456789]\\d{9}";
        if (phone.matches(regex))// 进行匹配
            return true;
        else
            return false;
    }

    public static int parseIntWithoutException(String str) {
        int rel = 0;
        try {
            rel = Integer.parseInt(str);
        } catch (Exception e) {

        }
        return rel;
    }

    public static String bytesToUtf8(byte[] bytes) {

        try {
            return new String(bytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 去除换行
     *
     * @return
     * @throws PatternSyntaxException
     */
    public static String replacehuanhang(String message) {
        if (StringUtils.isEmpty(message) || !message.contains("\\n")) {
            return message;
        } else {
            return message.replace("\\n", "\n");
        }

    }

    /**
     * 验证邮箱时候正确
     *
     * @param email
     * @return
     */
    public static boolean isEmail(String email) {
        // String str="^([a-zA-Z0-9]*[-_]?[a-zA-Z0-9]+)*@([a-zA-Z0-9]*[-_]?[a-zA-Z0-9]+)+[\\.][A-Za-z]{2,3}([\\.][A-Za-z]{2})?$";
        String str1 = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";

        Pattern p = Pattern.compile(str1);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    /**
     * 去掉字符串的第一个和最后一个字符
     * 替换字符串中所有的\符号
     *
     * @param str
     * @return
     */
    public static String clearStartAndEndStr(String str) {
        String s = str.substring(1, str.length() - 1);
        return s.replace("\\", "");
    }

    /**
     * 去掉首尾字符
     *
     * @param s
     * @return
     */
    public static String clearStartAndEnd(String s) {
        return s.substring(1, s.length() - 1);
    }


    /**
     * 将int类型版本号转换成 100 ==》1.00
     *
     * @param s
     * @return
     */
    public static String versionCode2Name(String s) {

        return s.substring(0, 1) + "." + s.substring(1, s.length());
    }

    public static String hintPhone(String s) {
        if (s.length() > 7) {
            String s0 = s.substring(0, 3);
            String s2 = s.substring(7, s.length());
            return s0 + "****" + s2;
        } else {
            return s;
        }
    }

    public static String hintBankNo(String s) {
        if (s.length() > 11) {
            String s0 = s.substring(0, 4);
            String s2 = s.substring(11, s.length());
            return s0 + "****" + s2;
        } else {
            return s;
        }
    }

    public static String hintUserName(String s) {
        if (s.length() >= 1) {
            String s0 = s.substring(0, 1);
            return s0 + "****";
        } else {
            return s;
        }
    }


    /**
     * 20180801 chenx 判断是否为空字符串
     *
     * @param o
     * @return
     */
    public static boolean isEmpty(Object o) {
        if (o == null || !(o instanceof String)) {
            return true;
        } else if (((String) o).isEmpty()) {
            return true;
        } else if (((String) o).trim().length() == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param str       字符串只能为两位小数或者整数
     * @param isDecimal 是否是小数
     * @Description 格式化字符串，每三位用逗号隔开
     */
    public static String addComma(String str, boolean isDecimal) {
        //先将字符串颠倒顺序
        str = new StringBuilder(str).reverse().toString();
        if (str.equals("0")) {
            return str;
        }
        String str2 = "";
        for (int i = 0; i < str.length(); i++) {
            if (i * 3 + 3 > str.length()) {
                str2 += str.substring(i * 3, str.length());
                break;
            }
            str2 += str.substring(i * 3, i * 3 + 3) + ",";
        }
        if (str2.endsWith(",")) {
            str2 = str2.substring(0, str2.length() - 1);
        }
        //最后再将顺序反转过来
        String temp = new StringBuilder(str2).reverse().toString();
        if (isDecimal) {
            //去掉最后的","
            return temp.substring(0, temp.lastIndexOf(",")) + temp.substring(temp.lastIndexOf(",") + 1, temp.length());
        } else {
            return temp;
        }
    }

    /**
     * @Description 保持小数点后两位
     */
    public static String formatDoublePointTwo(double money) {
        DecimalFormat formater = new DecimalFormat();
        formater.setMaximumFractionDigits(2);
        formater.setGroupingSize(0);
        formater.setRoundingMode(RoundingMode.FLOOR);
        return formater.format(money);
    }

    /**
     * @Description double类型保持小数点指定位数的字符串返回值
     */
    public static String formatDoublePointNum(double d, int num) {
        DecimalFormat formater = new DecimalFormat();
        formater.setMaximumFractionDigits(num);
        formater.setGroupingSize(0);
        formater.setGroupingUsed(false);
        formater.setRoundingMode(RoundingMode.HALF_UP);
        return formater.format(d);
    }

    public static boolean isInt(String s) {
        if (s == null) return false;
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isLong(String s) {
        if (s == null) return false;
        try {
            Long.parseLong(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isDouble(String s) {
        if (s == null) return false;
        try {
            if (s.endsWith("F"))return false;
            Double.parseDouble(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @auther: Tom
     * created at 2016/8/1 17:37
     * 按目标sp，切割s后返回第i截字段
     */
    public static String getTargetField(String s, String sp, int i) {
        if (s == null || s.isEmpty() || sp == null || sp.isEmpty()) {
            return null;
        } else try {
            return s.split(sp)[i];
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * @auther: Tom
     * created at 2016/8/1 17:37
     * 按目标sp，切割s后返回最后一截字段
     */
    public static String getLastField(String s, String sp) {
        if (s == null || !s.contains(sp)) {
            return null;
        } else try {
            return s.split(sp)[s.split(sp).length - 1];
        } catch (Exception ex) {

        }
        return null;
    }

    /**
     * @auther: Tom
     * created at 2016/8/1 17:37
     * 切割s后返回字符长度不大于i的字段
     */
    public static String getTargetIntField(String s, int i) {
        if (s == null) {
            return null;
        } else if (s.length() <= i) {
            return s;
        } else try {
            return s.substring(0, i);
        } catch (Exception ex) {

        }
        return null;
    }

    /**
     * @auther: Tom
     * created at 2016/8/1 17:37
     * 切割s后指定位数，返回剩余长度的字符串
     */
    public static String getSpIntField(String s, int i) {
        if (s == null) {
            return null;
        } else if (s.length() <= i) {
            return s;
        } else try {
            return s.substring(0, s.length() - i);
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * @auther: Tom
     * created at 2016/8/1 17:37
     * 按目标sp，切割s后返回第一段内容
     */
    public static String getTargetSpField(String s, String sp) {
        if (s == null || s.isEmpty() || sp == null || sp.isEmpty()) {
            return "";
        } else if (!s.contains(sp)) {
            return s;
        } else {
            int index = s.indexOf(sp);
            return s.substring(0, index);
        }
    }

    public static String encodeHeadInfo(String headInfo) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0, length = headInfo.length(); i < length; i++) {
            char c = headInfo.charAt(i);
            if (c <= '\u001f' || c >= '\u007f') {
                stringBuffer.append(String.format("\\u%04x", (int) c));
            } else {
                stringBuffer.append(c);
            }
        }
        return stringBuffer.toString();
    }

    public static String toLHParamStringList(List<String> datas) {
        if (datas != null && datas.size() > 0) {
            StringBuffer sb = new StringBuffer();
            for (String str : datas) {
                sb.append("\'" + str + "\',");
            }
            return sb.substring(0, sb.length() - 1);
        } else {
            return "";
        }

    }

    /**
     * 仪器当前状态
     *
     * @param i
     * @return
     */
    public static String yqProcessNameMap(int i) {

        switch (i) {
            case 0xFFFF:
                return "空闲";
            case 0:
                return "推空液位管";
            case 1:
                return "计量传感器";
            case 2:
                return "测量传感器";
            case 3:
                return "润洗液位管";
            case 4:
                return "抽取样品";
            case 5:
                return "抽取掩蔽剂";
            case 6:
                return "抽取消解液";
            case 7:
                return "抽空气搅拌";
            case 8:
                return "加热消解";
            case 9:
                return "自然冷却";
            case 10:
                return "加速冷却";
            case 11:
                return "读取样品值";
            case 12:
                return "清洗检测池";
            case 13:
                return "读取基线值";
            case 14:
                return "清洗管路";
            case 15:
                return "清洗吹气池";
            case 16:
                return "抽取调节液";
            case 17:
                return "抽取显色剂";
            case 18:
                return "加热反应";
            case 19:
                return "紫外消解";
            case 20:
                return "抽取还原液";
            case 21:
                return "加清水冷却";
            case 22:
                return "抽取滴定液";
            case 23:
                return "正在滴定";
            case 24:
                return "抽取缓冲液";
            case 25:
                return "正在反应";
            case 26:
                return "抽取氧化剂";
            case 27:
                return "读取余氯值";
            case 28:
                return "读取总氯值";
            case 29:
                return "抽取释放剂";
            case 30:
                return "抽取标液一";
            case 31:
                return "读取电位一";
            case 32:
                return "抽取标液二";
            case 33:
                return "读取电位二";
            case 34:
                return "保护电极";
            case 35:
                return "抽取吸收液";
            case 36:
                return "加热蒸馏";
            case 37:
                return "抽取中和液";
            case 38:
                return "显色反应";
            case 39:
                return "抽取萃取液";
            case 40:
                return "静置";
            case 41:
                return "氧化反应";
            case 42:
                return "加催化剂";
            case 43:
                return "开启取样泵";
            case 44:
                return "流入比色皿";
            case 45:
                return "排废液";
            case 46:
                return "单抽";
            case 47:
                return "单推";
            case 48:
                return "单点控制";
            case 49:
                return "模块内部指令";
            case 50:
                return "清洗液位管";
            case 51:
                return "读取暗信号";
            case 52:
                return "计算测试结果";
            case 53:
                return "抽取清水";
            case 54:
                return "流程开始";
            case 55:
                return "流程结束";
            case 56:
                return "流程子程序";
            case 57:
                return "抽样子程序";
            case 58:
                return "控制取样目标阀";
            case 59:
                return "试剂补抽";
            case 60:
                return "控制取样目标阀";
            case 61:
                return "扫描当前光敏值";
            case 62:
                return "取消解后的样入检测池";
            case 63:
                return "富集";
            case 64:
                return "平置";
            case 65:
                return "溶出";
            case 66:
                return "加标富集";
            case 67:
                return "加标平置";
            case 68:
                return "加标溶出";
            case 69:
                return "清洗消解池";
            case 70:
                return "线性扫描";
            case 71:
                return "控制DAC输出值";
            case 72:
                return "电极连接电路板控制";
            case 73:
                return "设置DAC校准参数";
            case 74:
                return "电磁阀单点控制";
            case 75:
                return "富集时间";
            case 76:
                return "抽取电解液";
            case 77:
                return "抽取标准溶液";
            case 78:
                return "信号采集";
            case 79:
                return "光源电流";
            case 80:
                return "PMT负高压";
            case 81:
                return "激发光源";
            case 82:
                return "抽取镀膜液";
            case 83:
                return "循环伏安";
            case 84:
                return "推空样品管";
            case 85:
                return "抽取清洗液";
            case 86:
                return "打开光源";
            case 87:
                return "关闭光源";
            case 88:
                return "取样定位";
            case 89:
                return "模块复位";
            case 90:
                return "模块复位";
            case 91:
                return "抽取絮凝剂";
            case 92:
                return "抽取培养基";
            case 93:
                return "抽取消毒液";
            case 101:
                return "电磁阀整体控制";
            case 102:
                return "电磁阀单点控制";
            case 103:
                return "温度单点控制";
            case 104:
                return "蠕动泵控制";
            case 105:
                return "待机";
            case 106:
                return "进样";
            case 107:
                return "吹扫";
            case 108:
                return "干吹";
            case 109:
                return "预解析";
            case 110:
                return "解析";
            case 111:
                return "烘烤";
            case 112:
                return "设置工作模式";
            case 113:
                return "采样速率设置";
            case 114:
                return "设置捕集井温度";
            case 115:
                return "设置FID温度";
            case 116:
                return "设置箱体温度";
            case 117:
                return "设置毛细柱升温";
            case 118:
                return "高压输出";
            case 119:
                return "点火";
            case 120:
                return "数据采集";
            case 121:
                return "等待温度";
            case 122:
                return "设置线性升温阶梯";
            case 123:
                return "设置线性升温斜率";
            case 124:
                return "设置保持时间";
            case 125:
                return "设置PID参数";
            case 126:
                return "启动线性升温";
            case 127:
                return "设置第N路温度";
            case 128:
                return "启动升温";
            case 129:
                return "设置DA值";
            case 130:
                return "开关ECD电源";
            case 131:
                return "采样速率";
            case 132:
                return "设置吸富柱温度";
            case 133:
                return "设置ECD温度";
            case 134:
                return "萃取";
            case 135:
                return "反复抽推";
            case 136:
                return "反复抽样";
            case 137:
                return "设置进样瓶温度";
            case 138:
                return "设置萃取室温度";
            case 139:
                return "单次推样";
            case 140:
                return "反吹";
            case 141:
                return "气泵控制";
            case 142:
                return "计算液位步数";
            case 143:
                return "流量控制";
            case 144:
                return "计算液位体积";
            case 145:
                return "发送测试命令";
            case 146:
                return "等待测试结果";
            case 147:
                return "打开风扇";
            case 148:
                return "关闭风扇";
            case 149:
                return "打开搅拌";
            case 150:
                return "关闭搅拌";
            case 151:
                return "柱塞泵终止";
            case 152:
                return "设置加热功率";
            case 153:
                return "动态选择模式";
            case 154:
                return "检测模块状态";
            default:
                return "未知" + i;

        }
    }


    /**
     * 仪器工作状态
     *
     * @param i
     * @return
     */
    public static String yqWorkStatu(int i) {

        switch (i) {

            case 0x00:
                return "空闲";
            case 0x01:
                return "采样中";
            case 0x02:
                return "终止采样";
            case 0x03:
                return "采样超时";
            case 0x004:
                return "通行故障";
            default:
                return "未知" + i;

        }
    }

    /**
     * 仪器采样模式
     *
     * @param i
     * @return
     */
    public static String yqSamplingMode(int i) {

        switch (i) {

            case 0x00:
                return "维护模式";
            case 0x01:
                return "等时模式";
            case 0x02:
                return "等量模式";
            case 0x03:
                return "计划定时";
            case 0x004:
                return "计划定量";
            default:
                return "未知" + i;

        }
    }

    public static String SampleLogActionName(String action) {
        if (isEmpty(action)) return "";
        switch (action) {
            case "start":
                return "开始";
            case "move":
                return "移动";
            case "testcmd":
                return "水样测试命令";
            case "jyfinished":
                return "进样完成";
            case "save":
                return "保存数据";
        }
        return action;
    }

    /**
     * 将HH:MM的字符串转成HH*60+MM的数字
     *
     * @param HHMM
     * @return
     */
    public static int HHMMtoInt(String HHMM) {
        if (StringUtils.isEmpty(HHMM) || !HHMM.contains(":") || HHMM.length() != 5) {
            return 0;
        } else {
            return Integer.parseInt(HHMM.substring(0, 2)) * 60 + Integer.parseInt(HHMM.substring(3, 5));
        }
    }

    public static String sampleProjectListStatue(int statue) {

        switch (statue) {
            case 0:
                return "待测";
            case 1:
                return "正在上样";
            case 2:
                return "已发仪器测试命令";
            case 3:
                return "测试完成";
            case 4:
                return "样品信息丢失";
        }
        return statue + ",未知状态";
    }

    public static String getPosTitle(int pos) {
        switch (pos) {
            case 6:
                return "6-RFID读写位";
            case 7:
                return "7-前处理位";
            case 8:
                return "8-清洗瓶位";
            case 17:
                return "17-检测缓存位";
            case 18:
                return "18-检测缓存位";
            case 19:
                return "19-检测缓存位";
            case 20:
                return "20-检测缓存位";
            default:
                return pos + "-检测位";
        }
    }

    public static String addCharForNum(String str, int strLength, char c) {
        int strLen = str.length();
        if (strLen < strLength) {
            while (strLen < strLength) {
                StringBuffer sb = new StringBuffer();
                // sb.append("0").append(str);//左补0
                sb.append(str).append(c);//右补0
                str = sb.toString();
                strLen = str.length();
            }
        }

        return str;
    }


    public static String getOldBlueToothMacAddress(String qcode) {
        StringBuffer sb = new StringBuffer();
        sb.append("3" + qcode.charAt(11));
        sb.append(":");
        sb.append("3" + qcode.charAt(10));
        sb.append(":");
        sb.append("3" + qcode.charAt(9));
        sb.append(":");
        sb.append("3" + qcode.charAt(8));
        sb.append(":");
        sb.append("3" + qcode.charAt(7));
        sb.append(":");
        sb.append("3" + qcode.charAt(6));
        return sb.toString();
    }

    public static String getNewBlueToothMacAddress(String qcode) {

        StringBuffer sb = new StringBuffer();
        sb.append(qcode.substring(0, 2));
        sb.append(":");
        sb.append(qcode.substring(2, 4));
        sb.append(":");
        sb.append(qcode.substring(4, 6));
        sb.append(":");
        sb.append(qcode.substring(6, 8));
        sb.append(":");
        sb.append(qcode.substring(8, 10));
        sb.append(":");
        sb.append(qcode.substring(10, 12));
        return sb.toString();
    }

    public static String getOldBlueName(String qcode) {
        return qcode.substring(qcode.length() - 6);
    }

    public static String getNewBlueName(String qcode) {
        return qcode;
    }

    public static String getSampleMode(String code) {
        if (code == null) return "未知";
        switch (code) {
            case "00":
                return "维护模式";
            case "01":
                return "等时模式";
            case "02":
                return "雨量触发模式";
            case "03":
                return "单触发模式";
            case "04":
                return "定时采样模式";
            case "05":
                return "液位触发模式";
            case "06":
                return "手动测试模式";
            case "07":
                return "采样流程测试";
            case "08":
                return "等比例采样";
            case "09":
                return "远程触发";
            case "10":
                return "浮子开关触发";
            case "11":
                return "五参数触发";
            case "12":
                return "平台雨量触发";
            case "13":
                return "疾控模式";
            case "14":
                return "受控模式";
        }
        return "未知" + code;
    }

    public static String getDevStatus(String code) {
        if (code == null) return "未知状态";
        switch (code) {
            case "00":
            case "000":
            case "0000":
            case "00000":
            case "000000":
                return "正常";
            case "01":
            case "001":
                return "工作中";
            case "002":
                return "采样故障";
            case "003":
                return "流量故障";
            case "004":
                return "网络故障";
            case "005":
                return "GPS故障";
            case "006":
                return "高温警报";
            case "007":
                return "电量过低";
            case "008":
                return "蓝牙通讯故障";
            case "009":
                return "采样瓶故障";
            case "010":
                return "五参数电极离线";
            case "011":
                return "水位计离线";
            case "012":
                return "水浸报警";

        }
        return "故障码" + code;
    }

    public static String getCYXRTUStatus(String code) {
        if (code == null) return "未知状态";
        switch (code) {
            case "00":
            case "000":
            case "0000":
            case "00000":
            case "000000":
                return "正常";
            case "01":
            case "001":
                return "工作中";
            case "002":
                return "采样故障";
            case "003":
                return "流量故障";
            case "004":
                return "网络故障";
            case "005":
                return "GPS故障";
            case "006":
                return "高温警报";
            case "007":
                return "电量过低";
            case "008":
                return "蓝牙通讯故障";
            case "009":
                return "采样瓶故障";
            case "010":
                return "无可用采样瓶";
            case "011":
                return "水位计离线";
            case "012":
                return "水浸报警";

        }
        return "故障码" + code;
    }

    public static String getZKYDevStatus(int code) {
        if (code < 0) return "未知";
        switch (code) {
            case 0x0:
                return "空闲";
            case 0x1:
                return "工作中";
            case 0x12:
                return "抽取超限";
            case 0x13:
                return "母液不足";
            case 0x21:
                return "速度参数错误";
            case 0x22:
                return "步数参数错误";
            case 0x23:
                return "电流设置错误";
            case 0x31:
                return "电机正忙";
            case 0x32:
                return "电机堵转";
            case 0x33:
                return "电机线未接好";
            case 0x34:
                return "极限故障";
            case 0x41:
                return "缺水样";
            case 0x42:
                return "排样阀/液位故障";
            case 0x43:
                return "隔膜泵抽纯水故障";
            case 0x51:
                return "转臂超时";
            default:
                return "故障码" + code;
        }
    }

    public static String getCYXUpdateType(int type) {
        switch (type) {
            case 0:
                return "不主动上传";
            case 1:
                return "全部上传";
            case 2:
                return "水参数上传";
            case 3:
                return "GPS上传";
            default:
                return "未知类型" + type;
        }
    }

    //0-正常，1-工作中，2-采样故障，3-流量故障，4-网络故障，5-GPS故障，6-高温报警，7-电池电量低
    public static String getYSXWorkStatue(int statue) {
        switch (statue) {
            case 0:
                return "正常";
            case 1:
                return "工作中";
            case 2:
                return "采样故障";
            case 3:
                return "流量故障";
            case 4:
                return "网络故障";
            case 5:
                return "GPS故障";
            case 6:
                return "高温报警";
            case 7:
                return "电池电量低";
            default:
                return "未知状态" + statue;
        }
    }

    /**
     * 0空闲，1工作中，2采样结束，3采样异常，4无信号
     *
     * @param code
     * @return
     */
    public static String getGGCYDevStatus(int code) {

        switch (code) {
            case 0:
                return "空闲";
            case 1:
                return "工作中";
            case 2:
                return "采样结束";
            case 3:
                return "采样异常";
            case 4:
                return "无信号";
        }
        return "故障码" + code;
    }

    /**
     * 0触发模式，1手动模式
     *
     * @param code
     * @return
     */
    public static String getGGCYDevRunMode(int code) {

        switch (code) {
            case 0:
                return "触发模式";
            case 1:
                return "手动模式";

        }
        return "模式" + code;
    }

    /**
     *
     *
     * @param code
     * @return
     */
    public static String getGGCYErrorCode(int code) {

        switch (code) {
            case 0:
                return "";
            case 1:
                return "网络故障";
            case 2:
                return "流量计通信故障";
            case 3:
                return "流量过大告警";
            case 4:
                return "采样超时告警";
        }
        return "故障码" + code;
    }

    public static boolean isIPAddress(String str) {
        if (str==null)return false;
        String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
        // 判断ip地址是否与正则表达式匹配
        if (str.matches(regex)) {
            String[] arr = str.split("\\.");
            for (int i = 0; i < 4; i++) {
                int temp = Integer.parseInt(arr[i]);
                //如果某个数字不是0到255之间的数 就返回false
                if (temp < 0 || temp > 255) return false;
            }
            return true;
        } else {
            return false;
        }

    }

    /**
     * 将12位的IP字符串转换成正常IP地址显示
     * @param strIp12 036010168001
     * @return  36.10.168.1
     */
    public static String getIPAddress(String strIp12) {

        String ipAddress="";
        // 判断ip地址是否与正则表达式匹配
        if (StringUtils.isLong(strIp12)&&strIp12.length()==12) {
            ipAddress=Integer.parseInt(strIp12.substring(0,3))+"."+Integer.parseInt(strIp12.substring(3,6))+"."
                    +Integer.parseInt(strIp12.substring(6,9))+"."+Integer.parseInt(strIp12.substring(9,12));

        }
        return ipAddress;
    }


    /**
     * 前方用0补全指定长度
     *
     * @param count 输入数字
     * @param sNum  补全位数
     * @return 补全后字符串，eg:004
     */
    public static String formatString(int count, int sNum) {
        // 1. 基础校验：如果指定位数不合法，直接返回 "0"
        if (sNum <= 0) {
            return "0";
        }

        // 2. 处理数值溢出/截断逻辑
        // 计算 sNum 长度能容纳的最大绝对值。
        // 例如 sNum=3，能显示的最大数字是 099 (正数) 或 -99 (负数)。
        // 计算公式：10^(3-1) - 1 = 99
        int maxAbsValue = (int) Math.pow(10, sNum - 1) - 1;
        int maxValue = (int) Math.pow(10, sNum ) - 1;
        // 修正：针对 sNum=1 的特殊情况，最大绝对值应为 0 (即只能显示 0, -0 不存在)
        if (sNum == 1) {
            maxAbsValue = 0;
            maxValue=9;
        }

//        // 如果输入数字的绝对值超过了最大可显示范围，进行截断
//        if (Math.abs(count) > maxAbsValue) {
//            // 保持原数字的符号，但数值部分显示为最大值
//            // 例如：sNum=3, count=1234 -> "99"
//            // 例如：sNum=3, count=-1234 -> "-99"
//            return (count < 0 ? "-" : "") + String.format("%0" + (sNum - (count < 0 ? 1 : 0)) + "d", maxAbsValue);
//        }

        // 如果输入数字的绝对值超过了最大可显示范围，进行截断
        if (count<0&&Math.abs(count) > maxAbsValue) {
            // 保持原数字的符号，但数值部分显示为最大值
            // 例如：sNum=3, count=1234 -> "99"
            // 例如：sNum=3, count=-1234 -> "-99"
            return "-" + String.format("%0" + (sNum - 1) + "d", maxAbsValue);
        }else if (count>0&&count>maxValue){
            return ""+maxValue;
        }

        // 3. 正常格式化
        // String.format 会自动处理负号。
        // 关键点：负号会占用宽度。例如 count=-4, sNum=4 -> "-004"
        return String.format("%0" + sNum + "d", count);
    }

    public static String getSWJWorkStatue(int statue) {
        switch (statue) {
            case 0:
                return HJBleApplication.getAppContext().getString(R.string.work_state_0);
            case 1:
                return HJBleApplication.getAppContext().getString(R.string.work_state_1);
            case 2:
                return HJBleApplication.getAppContext().getString(R.string.work_state_2);
            case 3:
                return HJBleApplication.getAppContext().getString(R.string.work_state_3);
            case 4:
                return HJBleApplication.getAppContext().getString(R.string.work_state_4);
            default:
                return HJBleApplication.getAppContext().getString(R.string.work_state_unknown) + statue;
        }
    }

    /**
     * 整型数值 转换成 16位二进制数组
     * @param value
     * @return
     */
    public static byte[] getDateBit16(int value){
        byte[] dateBytes = new byte[16];
        dateBytes[0]= (byte) ((value & 0xff) % 2);
        dateBytes[1]= (byte) ((value>>1 & 0xff) % 2);
        dateBytes[2]= (byte) ((value>>2 & 0xff) % 2);
        dateBytes[3]= (byte) ((value>>3 & 0xff) % 2);
        dateBytes[4]= (byte) ((value>>4 & 0xff) % 2);
        dateBytes[5]= (byte) ((value>>5 & 0xff) % 2);
        dateBytes[6]= (byte) ((value>>6 & 0xff) % 2);
        dateBytes[7]= (byte) ((value>>7 & 0xff) % 2);
        dateBytes[8]= (byte) ((value>>8 & 0xff) % 2);
        dateBytes[9]= (byte) ((value>>9 & 0xff) % 2);
        dateBytes[10]= (byte) ((value>>10 & 0xff) % 2);
        dateBytes[11]= (byte) ((value>>11 & 0xff) % 2);
        dateBytes[12]= (byte) ((value>>12 & 0xff) % 2);
        dateBytes[13]= (byte) ((value>>13 & 0xff) % 2);
        dateBytes[14]= (byte) ((value>>14 & 0xff) % 2);
        dateBytes[15]= (byte) ((value>>15 & 0xff) % 2);
        return dateBytes;
    }

    public static String getRTUWorkStateErr(byte[] bytes){
        StringBuffer sb=new StringBuffer();
        if (bytes[3]==1) sb.append("采样故障,");
        if (bytes[4]==1) sb.append("流量故障,");
        if (bytes[5]==1) sb.append("网络故障,");
        if (bytes[6]==1) sb.append("GPS故障,");
        if (bytes[7]==1) sb.append("高温警报,");
        if (bytes[8]==1) sb.append("电量过低,");
        if (bytes[9]==1) sb.append("蓝牙通讯故障,");
        if (bytes[10]==1) sb.append("采样瓶故障,");
        if (bytes[11]==1) sb.append("五参数电极离线,");
        if (bytes[12]==1) sb.append("水位计离线,");
        if (bytes[13]==1) sb.append("水浸报警,");
        return sb.length()>1?sb.substring(0,sb.length()-1):sb.toString();
    }

    public static int bit162Int(byte[] byte16){
        if (byte16==null||byte16.length!=16)return 0;
        int value=(byte16[0]&0xff)+((byte16[1]&0xff)<<1)
                +((byte16[2]&0xff)<<2)+((byte16[3]&0xff)<<3)
                +((byte16[4]&0xff)<<4)+((byte16[5]&0xff)<<5)
                +((byte16[6]&0xff)<<6)+((byte16[7]&0xff)<<7)
                +((byte16[8]&0xff)<<8)+((byte16[9]&0xff)<<9)
                +((byte16[10]&0xff)<<10)+((byte16[11]&0xff)<<11)
                +((byte16[12]&0xff)<<12)+((byte16[13]&0xff)<<13)
                +((byte16[14]&0xff)<<14)+((byte16[15]&0xff)<<15);
        return value;
    }
    public static String getSWJWorkStatue16(long value8) {
        byte[] moduleSTABytes = new byte[16];
        moduleSTABytes[0] = (byte) ((value8 & 0xff) % 2);
        moduleSTABytes[1] = (byte) ((value8 >> 1 & 0xff) % 2);
        moduleSTABytes[2] = (byte) ((value8 >> 2 & 0xff) % 2);
        moduleSTABytes[3] = (byte) ((value8 >> 3 & 0xff) % 2);
        moduleSTABytes[4] = (byte) ((value8 >> 4 & 0xff) % 2);
        moduleSTABytes[5] = (byte) ((value8 >> 5 & 0xff) % 2);
        moduleSTABytes[6] = (byte) ((value8 >> 6 & 0xff) % 2);
        moduleSTABytes[7] = (byte) ((value8 >> 7 & 0xff) % 2);
        moduleSTABytes[8] = (byte) ((value8 >> 8 & 0xff) % 2);
        moduleSTABytes[9] = (byte) ((value8 >> 9 & 0xff) % 2);
        moduleSTABytes[10] = (byte) ((value8 >> 10 & 0xff) % 2);
        moduleSTABytes[11] = (byte) ((value8 >> 11 & 0xff) % 2);
        moduleSTABytes[12] = (byte) ((value8 >> 12 & 0xff) % 2);
        moduleSTABytes[13] = (byte) ((value8 >> 13 & 0xff) % 2);
        moduleSTABytes[14] = (byte) ((value8 >> 14 & 0xff) % 2);
        moduleSTABytes[15] = (byte) ((value8 >> 15 & 0xff) % 2);
        if (moduleSTABytes[0] == 1) {
            StringBuffer sb = new StringBuffer();
            if (moduleSTABytes[3] == 1) {
                sb.append("雷达串口通信异常,");
            }
            if (moduleSTABytes[4] == 1) {
                sb.append("Lot模块通信异常,");
            }
            if (moduleSTABytes[5] == 1) {
                sb.append("网络信号异常,");
            }
            if (moduleSTABytes[6] == 1) {
                sb.append("网络连接异常,");
            }
            if (moduleSTABytes[7] == 1) {
                sb.append("SIM卡异常,");
            }
            if (sb.length() <= 0) {
                return "未知异常" + value8;
            } else {
                return sb.substring(0, sb.length() - 1);
            }

        } else if (moduleSTABytes[1] == 1) {
            return "采样中";
        } else if (moduleSTABytes[2] == 1) {
            return "上传中";
        } else if (moduleSTABytes[8] == 0) {
            return "未启动";
        } else {
            return "已启动";
        }
    }

    public static String getSWJUploadType(int type) {
        switch (type) {
            case 0:
                return "212";
            case 1:
                return "Mqtt";

            default:
                return HJBleApplication.getAppContext().getString(R.string.work_state_unknown) + type;
        }
    }

    public static String getSWJRTUType(int type) {
        switch (type) {
            case 0:
                return HJBleApplication.getAppContext().getString(R.string.rtu_4G_type);
            case 1:
                return HJBleApplication.getAppContext().getString(R.string.rtu_NB_type);

            default:
                return HJBleApplication.getAppContext().getString(R.string.work_state_unknown) + type;
        }
    }

    public static String getSWJModbusMode(int type) {
        switch (type) {
            case 0:
                return HJBleApplication.getAppContext().getString(R.string.mode_rtu);
            case 1:
                return HJBleApplication.getAppContext().getString(R.string.mode_485);

            default:
                return HJBleApplication.getAppContext().getString(R.string.work_state_unknown) + type;
        }
    }
//0-空闲 1-管路润洗中 2-开始洗井 3-水质参数达标，开始采样 4-洗井超时，开始采样
    public static String getDX2SampleStatus(int status) {

        switch (status) {
            case 0:
                return "空闲";
            case 1:
                return "管路润洗中";
            case 2:
                return "开始洗井";
            case 3:
                return "水质参数达标，可以开始采样";
            case 4:
                return "洗井超时，可以开始采样";
        }
        return "未知" + status;
    }
    public static String getLotLex(int rx) {
        if (rx<0){
            return "";
        }else if (rx<=16){
            return rx+"弱";
        }else if (rx<=20){
            return rx+"中";
        }else if (rx<99){
            return rx+"强";
        }else if (rx==99){
            return "无信号";
        }else {
            return rx+"";
        }
    }

    public static String getRtuLotType(int i) {
        if (i<0){
            return "";
        }else if (i==0){
            return "NB";
        }else if (i==1){
            return "4G";
        }else {
            return "未知"+i;
        }
    }

    public static String getRtuLljDirection(int direction) {
        if (direction<=0){
            return "";
        }else if (direction==1){
            return "靠近";
        }else if (direction==2){
            return "远离";
        }else {
            return "未知"+direction;
        }
    }
    public static String getRxLex(int rx) {
        if (rx<0){
         return "";
        }else if (rx<=10){
            return rx+"弱";
        }else if (rx<=20){
            return rx+"中";
        }else if (rx<99){
            return rx+"强";
        }else if (rx==99){
            return "无信号";
        }else {
            return rx+"";
        }
    }
    public static String getLotStep(int step) {
        if (step==10){
            return  "等待上传任务";
        }else if ( step==11){
            return  "检查SIM卡";
        }else if ( step==12){
            return  "检测NB信号强度";
        }else if ( step==13){
            return  "查看网络是否附着";
        }else if ( step==14){
            return  "激活PDP";
        }else if ( step==15){
            return  "时间校准";
        }else if ( step==16){
            return  "上传协议判断";
        }else if ( step==34){
            return  "激活TCP Socket";
        }else if ( step==35){
            return  "开启下行数据检测";
        }else if ( step==36){
            return  "连接指定服务器";
        }else if ( step==37){
            return  "发送采样数据";
        }else if ( step==38){
            return  "接收到下行回包数据";
        }else if ( step==39){
            return  "关闭TCP Socket";
        }else if ( step==50){
            return  "所有数据上传完毕";
        }else {
            return "未知上传步骤"+step;
        }
    }

    public static String getRtuProtocolType(int t) {
         if (t<0){
            return "";
         }else if (t==0){
            return "212协议";
         }else if (t==1){
             return "Mqtt协议";
         }else {
             return "未知"+t;
         }
    }

    public static String getDevSamdevtype(int status) {
        StringBuffer sb=new StringBuffer();
        if (status%2==1){
            sb.append("雷达水位计,");
        }
        if ((status>>1& 0xff)%2==1){
            sb.append("雷达流量计，");
        }
        if ((status>>2& 0xff)%2==1){
            sb.append("航征流量计，");
        }
        if ((status>>3& 0xff)%2==1){
            sb.append("铭控液位计，");
        }
        if ((status>>4& 0xff)%2==1){
            sb.append("水中油探头，");
        }
        if ((status>>5& 0xff)%2==1){
            sb.append("pH探头，");
        }
        if ((status>>6& 0xff)%2==1){
            sb.append("溶解氧探头，");
        }

        if ((status>>7& 0xff)%2==1){
            sb.append("DDL，");
        }

        if ((status>>8& 0xff)%2==1){
            sb.append("浊度，");
        }

        if ((status>>9& 0xff)%2==1){
            sb.append("氨氮，");
        }

        if ((status>>10& 0xff)%2==1){
            sb.append("COD，");
        }
        if ((status>>11& 0xff)%2==1){
            sb.append("ORP，");
        }
        if ((status>>12& 0xff)%2==1){
            sb.append("叶绿素A，");
        }
        if ((status>>13& 0xff)%2==1){
            sb.append("蓝绿藻，");
        }

        return sb.length()>0?sb.substring(0,sb.length()-1):"";
    }


    public static String getBottleLZ(int lz){
        switch (lz){
            case 0:
                return "电机状态正常";
            case 1:
                return "电机正在工作";
            case 10:
                return "电机起步卡停";
            case 11:
                return "电机运动超时";
            case 12:
                return "电机开路，电流低";
            case 13:
                return "阀门电机跑反";
            case 20:
                return "电机运动中，电压<3.3V";
            case 21:
                return "关阀时电机卡死";
            case 22:
                return "开阀时电机卡死，电机回退至关阀";
            default:
                return "电机未知状态"+lz;
        }
    }

    public static String getDX3SWDevStatus(long value8) {
        byte[] devSTABytes = new byte[16];
        devSTABytes[0] = (byte) ((value8 & 0xff) % 2);
        devSTABytes[1] = (byte) ((value8 >> 1 & 0xff) % 2);
        devSTABytes[2] = (byte) ((value8 >> 2 & 0xff) % 2);
        devSTABytes[3] = (byte) ((value8 >> 3 & 0xff) % 2);
        devSTABytes[4] = (byte) ((value8 >> 4 & 0xff) % 2);
        devSTABytes[5] = (byte) ((value8 >> 5 & 0xff) % 2);
        devSTABytes[6] = (byte) ((value8 >> 6 & 0xff) % 2);
        devSTABytes[7] = (byte) ((value8 >> 7 & 0xff) % 2);
        devSTABytes[8] = (byte) ((value8 >> 8 & 0xff) % 2);
        devSTABytes[9] = (byte) ((value8 >> 9 & 0xff) % 2);
        devSTABytes[10] = (byte) ((value8 >> 10 & 0xff) % 2);
        devSTABytes[11] = (byte) ((value8 >> 11 & 0xff) % 2);
        devSTABytes[12] = (byte) ((value8 >> 12 & 0xff) % 2);
        devSTABytes[13] = (byte) ((value8 >> 13 & 0xff) % 2);
        devSTABytes[14] = (byte) ((value8 >> 14 & 0xff) % 2);
        devSTABytes[15] = (byte) ((value8 >> 15 & 0xff) % 2);
        StringBuffer sb=new StringBuffer();
        if (devSTABytes[11]==1&&devSTABytes[12]==1){
           return "雷达水位计和流量计电极在线";
        }else if (devSTABytes[11]==1){
           return "雷达流量计电极在线";
        }else if (devSTABytes[12]==1){
            return "雷达水位计电极在线";
        }else {
            return "无电极在线的水位计或流量计";
        }

    }

    public static byte[] getDX3WCSDevStatuByte(long value8){
        byte[] devSTABytes = new byte[16];
        devSTABytes[0] = (byte) ((value8 & 0xff) % 2);
        devSTABytes[1] = (byte) ((value8 >> 1 & 0xff) % 2);
        devSTABytes[2] = (byte) ((value8 >> 2 & 0xff) % 2);
        devSTABytes[3] = (byte) ((value8 >> 3 & 0xff) % 2);
        devSTABytes[4] = (byte) ((value8 >> 4 & 0xff) % 2);
        devSTABytes[5] = (byte) ((value8 >> 5 & 0xff) % 2);
        devSTABytes[6] = (byte) ((value8 >> 6 & 0xff) % 2);
        devSTABytes[7] = (byte) ((value8 >> 7 & 0xff) % 2);
        devSTABytes[8] = (byte) ((value8 >> 8 & 0xff) % 2);
        devSTABytes[9] = (byte) ((value8 >> 9 & 0xff) % 2);
        devSTABytes[10] = (byte) ((value8 >> 10 & 0xff) % 2);
        devSTABytes[11] = (byte) ((value8 >> 11 & 0xff) % 2);
        devSTABytes[12] = (byte) ((value8 >> 12 & 0xff) % 2);
        devSTABytes[13] = (byte) ((value8 >> 13 & 0xff) % 2);
        devSTABytes[14] = (byte) ((value8 >> 14 & 0xff) % 2);
        devSTABytes[15] = (byte) ((value8 >> 15 & 0xff) % 2);
        return devSTABytes;
    }

    public static String getDX3WCSDevStatus(long value8) {
        byte[] devSTABytes = new byte[16];
        devSTABytes[0] = (byte) ((value8 & 0xff) % 2);
        devSTABytes[1] = (byte) ((value8 >> 1 & 0xff) % 2);
        devSTABytes[2] = (byte) ((value8 >> 2 & 0xff) % 2);
        devSTABytes[3] = (byte) ((value8 >> 3 & 0xff) % 2);
        devSTABytes[4] = (byte) ((value8 >> 4 & 0xff) % 2);
        devSTABytes[5] = (byte) ((value8 >> 5 & 0xff) % 2);
        devSTABytes[6] = (byte) ((value8 >> 6 & 0xff) % 2);
        devSTABytes[7] = (byte) ((value8 >> 7 & 0xff) % 2);
        devSTABytes[8] = (byte) ((value8 >> 8 & 0xff) % 2);
        devSTABytes[9] = (byte) ((value8 >> 9 & 0xff) % 2);
        devSTABytes[10] = (byte) ((value8 >> 10 & 0xff) % 2);
        devSTABytes[11] = (byte) ((value8 >> 11 & 0xff) % 2);
        devSTABytes[12] = (byte) ((value8 >> 12 & 0xff) % 2);
        devSTABytes[13] = (byte) ((value8 >> 13 & 0xff) % 2);
        devSTABytes[14] = (byte) ((value8 >> 14 & 0xff) % 2);
        devSTABytes[15] = (byte) ((value8 >> 15 & 0xff) % 2);
        StringBuffer sb=new StringBuffer();
        if (devSTABytes[0]==0){
            sb.append("水位计、");
        }
        if (devSTABytes[1]==0){
            sb.append("pH、");
        }
        if (devSTABytes[2]==0){
            sb.append("溶解氧、");
        }
        if (devSTABytes[3]==0){
            sb.append("电导率、");
        }
        if (devSTABytes[4]==0){
            sb.append("浊度、");
        }
        if (devSTABytes[5]==0){
            sb.append("水中油、");
        }
        if (devSTABytes[6]==0){
            sb.append("氨氮、");
        }
        if (devSTABytes[7]==0){
            sb.append("化学需氧量、");
        }
        if (devSTABytes[8]==0){
            sb.append("氧化还原电位、");
        }
        if (devSTABytes[9]==0){
            sb.append("叶绿素-a、");
        }
        if (devSTABytes[10]==0){
            sb.append("蓝绿藻、");
        }
       if (sb.length()>1){
           return sb.substring(0,sb.length()-1)+"电极不在线";
       }else {
           return "电极均在线";
       }
    }

    public static String getCYX7Step(int step,int version) {
        if (version>=260501){
            if (step==0){
                return  "检测蓝牙瓶状态";
            }else if ( step==1){
                return  "开排水泵反转排液";
            }else if ( step==2){
                return  "发起蓝牙连接";
            }else if ( step==3){
                return  "等待蓝牙连接成功";
            }else if ( step==4){
                return  "发送蓝牙瓶开阀命令";
            }else if ( step==5){
                return  "等待开阀成功";
            }else if ( step==6){
                return  "更新阀门状态";
            }else if ( step==7){
                return  "断开蓝牙瓶连接";
            }else if ( step==8){
                return  "开启排样";
            }else if ( step==9){
                return  "等待排空";
            }else if ( step==10){
                return  "关闭排样";
            }else if ( step==11){
                return  "润洗进样";
            }else if ( step==12){
                return  "检测液位";
            }else if ( step==13){
                return  "润洗进样后排样";
            }else if ( step==14){
                return  "等待排空";
            }else if ( step==15){
                return  "关排样";
            }else if ( step==16){
                return  "开启进样";
            }else if ( step==17){
                return  "检测液位";
            }else if ( step==18){
                return  "等待进样完成";
            }else if ( step==19){
                return  "发起蓝牙连接";
            }else if ( step==20){
                return  "等待蓝牙连接成功";
            }else if ( step==21){
                return  "发送蓝牙瓶关阀命令";
            }else if ( step==22){
                return  "等待关阀成功";
            }else if ( step==23){
                return  "更新阀门状态";
            }else if ( step==24){
                return  "断开蓝牙瓶连接";
            }else if ( step==25){
                return  "开排水泵反转排液";
            }else if ( step==26){
                return  "结束采样流程";
            }else if ( step==255){
                return  "等待下次采样";
            }else {
                return "未知步骤"+step;
            }
        }else {
            if (step==0){
                return  "转子泵反吹";
            }else if ( step==1){
                return  "检测蓝牙瓶状态";
            }else if ( step==2){
                return  "发起蓝牙连接";
            }else if ( step==3){
                return  "等待蓝牙连接成功";
            }else if ( step==4){
                return  "发送蓝牙瓶开阀命令";
            }else if ( step==5){
                return  "等待开阀成功";
            }else if ( step==6){
                return  "更新阀门状态";
            }else if ( step==7){
                return  "断开蓝牙瓶连接";
            }else if ( step==8){
                return  "排样";
            }else if ( step==9){
                return  "润洗进样";
            }else if ( step==10){
                return  "检测液位";
            }else if ( step==11){
                return  "润洗进样后排样";
            }else if ( step==12){
                return  "转子泵取水进样";
            }else if ( step==13){
                return  "检测液位";
            }else if ( step==14){
                return  "等待进样完成";
            }else if ( step==15){
                return  "发起蓝牙连接";
            }else if ( step==16){
                return  "等待蓝牙连接成功";
            }else if ( step==17){
                return  "发送蓝牙瓶关阀命令";
            }else if ( step==18){
                return  "等待关阀成功";
            }else if ( step==19){
                return  "更新阀门状态";
            }else if ( step==20){
                return  "断开蓝牙瓶连接";
            }else if ( step==21){
                return  "转子泵反吹";
            }else if ( step==22){
                return  "结束采样流程";
            }else if ( step==255){
                return  "等待下次采样";
            }else {
                return "未知步骤"+step;
            }
        }

    }


    // ==================== LHADCP设备相关转换方法 ====================

    /**
     * 获取LHADCP设备状态文本
     * @param status 16位状态字
     * @return 状态描述文本
     */
    public static String getLHADCPDeviceStatusText(long status) {
        if (status == 0) {
            return "正常";
        }
        
        StringBuilder sb = new StringBuilder("异常");
        // 根据状态位详细解析（见协议附录二）
        if ((status & 0x01) != 0) sb.append("[电压]");
        if ((status & 0x02) != 0) sb.append("[网络]");
        if ((status & 0x04) != 0) sb.append("[位2]");
        if ((status & 0x08) != 0) sb.append("[位3]");
        if ((status & 0x10) != 0) sb.append("[位4]");
        
        return sb.toString();
    }

    /**
     * 获取LHADCP运行模式文本
     * @param mode 运行模式代码
     * @return 模式描述文本
     */
    public static String getLHADCPRunModeText(int mode) {
        switch (mode) {
            case 0: return "自动模式";
            case 1: return "手动模式";
            case 2: return "在线模式";
            case 3: return "维护模式";
            default: return "未知模式(" + mode + ")";
        }
    }

    /**
     * 获取LHADCP断面形状文本
     * @param shape 断面形状代码
     * @return 形状描述文本
     */
    public static String getLHADCPCrossSectionShapeText(int shape) {
        switch (shape) {
            case 0: return "矩形";
            case 1: return "梯形";
            case 2: return "V形";
            case 3: return "U形";
            case 4: return "圆形";
            default: return "未知形状(" + shape + ")";
        }
    }

    /**
     * 获取LHADCP上传协议文本
     * @param protocol 协议代码
     * @return 协议描述文本
     */
    public static String getLHADCPUploadProtocolText(int protocol) {
        switch (protocol) {
            case 0: return "212协议";
            case 1: return "MQTT协议";
            default: return "未知协议(" + protocol + ")";
        }
    }


}
