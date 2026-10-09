package com.lh.lhble.Entity;

import java.io.Serializable;

/**
 * description ：
 * project name：CCloud
 * author : Vincent
 * creation date: 2017/5/3 14:09
 * @version 1.0
 */

public class Result<T> implements Serializable {

    private int code;
    private String msg;
    private  T data;
    private String serverTime;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }
}
