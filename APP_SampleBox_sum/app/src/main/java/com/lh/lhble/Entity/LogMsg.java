package com.lh.lhble.Entity;

public class LogMsg {
    int type;//1 send 2 error 3 tips 4 receive 5 other
    String content;
    String dateTime;



    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
