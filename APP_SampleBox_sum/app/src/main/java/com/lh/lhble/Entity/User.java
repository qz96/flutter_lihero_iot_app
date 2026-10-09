package com.lh.lhble.Entity;

import com.lh.lhble.Utils.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * description ：
 * project name：CCloud
 * author : Vincent
 * creation date: 2021/4/25 11:13
 *
 * @version 1.0
 */

public class User implements Serializable {

    private String userId;// 用户ID
    private String username;//用户名字
    private String userToken;// 唯一Token
    private String headPic;//头像
    private String srcId;//手机生成的WebSocket通信srcID
    private String defaultProject;//用户项目编号
    private String sampleType;//采样类型
    private String hasTransitCases;//运输箱情况 0-无 1-有 2-存在前述两种情况
    private String logisticsMode;//物流方式 0-自运 1-第三方物流 2-存在前述两种情况
    private String testType;//接样和测试类型 0-分析小屋 1-实验室分析系统
    private int isPatrol;//是否巡村 0-否 1-是
    /*APP_SAMPLE_ADMIN   系统管理员       APP_SAMPLE_XC_ADMIN	巡村/采样管理员
      APP_SAMPLE_XC_NORMAL 巡村/采样人员    APP_SAMPLE_LAB_ADMIN	实验室管理员
      APP_SAMPLE_LAB_RECV  实验室接样人员   APP_SAMPLE_LAB_ANA	实验室接分析人员*/
    private String strPermissions;
    private List<Child> children;//用户拥有显示权限的模块单元
    private List<Project> userProjectList;//用户所有的业务项目
    private Project currentProject;//用户当前登录的项目编号
    private String realName;//中文名字
    private String phone;//电话号码
    private String deptCode;//部门代码
    private String deptName;//部门名称
    private String umDeviceToken;//友盟设备唯一识别号
    private String okPwd;//登陆密码加密
    private String guid;//设备唯一标识
    private HashMap<String,String> diyBleOrders;//自定义按钮的蓝牙指令


    public String getOkPwd() {
        return okPwd;
    }

    public void setOkPwd(String okPwd) {
        this.okPwd = okPwd;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getUmDeviceToken() {
        return umDeviceToken;
    }

    public void setUmDeviceToken(String umDeviceToken) {
        this.umDeviceToken = umDeviceToken;
    }

    public String getHeadPic() {
        return headPic;
    }

    public void setHeadPic(String headPic) {
        this.headPic = headPic;
    }

    public String getSrcId() {
        return srcId;
    }

    public void setSrcId(String srcId) {
        this.srcId = srcId;
    }

    public String getDefaultProject() {
        return defaultProject;
    }

    public void setDefaultProject(String defaultProject) {
        this.defaultProject = defaultProject;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getHasTransitCases() {
        return hasTransitCases;
    }

    public void setHasTransitCases(String hasTransitCases) {
        this.hasTransitCases = hasTransitCases;
    }

    public String getLogisticsMode() {
        return logisticsMode;
    }

    public void setLogisticsMode(String logisticsMode) {
        this.logisticsMode = logisticsMode;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public int getIsPatrol() {
        return isPatrol;
    }

    public void setIsPatrol(int isPatrol) {
        this.isPatrol = isPatrol;
    }

    public List<Child> getChildren() {
        return children;
    }

    public void setChildren(List<Child> children) {
        this.children = children;
    }

    public List<Project> getUserProjectList() {
        return userProjectList;
    }

    public void setUserProjectList(List<Project> userProjectList) {
        this.userProjectList = userProjectList;
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    public String getStrPermissions() {
        if (strPermissions==null){
            strPermissions="";
        }
        return strPermissions;
    }

    public void setStrPermissions(String strPermissions) {
        this.strPermissions = strPermissions;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public HashMap<String, String> getDiyBleOrders() {
        return diyBleOrders;
    }

    public void setDiyBleOrders(HashMap<String, String> diyBleOrders) {
        this.diyBleOrders = diyBleOrders;
    }
}
