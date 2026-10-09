package com.lh.lhble.Entity;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExcelInfoBean implements Serializable {

    //sheet名字--与数据对象名字一致
    private String sheetTitle;
    //excel列名的集合
    private List<String> keys;
    //key属性名，value属性值--excel头，excel值
    private List<HashMap<String, String>> data = new ArrayList<>();

    public ExcelInfoBean(@NonNull String sheetTitle,List<String>keys, List<HashMap<String, String>> data) {
        this.sheetTitle = sheetTitle;
        this.keys=keys;
        this.data = data;
    }

    public String getSheetTitle() {
        return sheetTitle;
    }

    public void setSheetTitle(String sheetTitle) {
        this.sheetTitle = sheetTitle;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public List<HashMap<String, String>> getData() {
        return data;
    }

    public void setData(List<HashMap<String, String>> data) {
        this.data = data;
    }
}

