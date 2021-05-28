package com.mechat.app.model;

public class UserState {
    private String time;
    private String status;
    private long date;

    public UserState(String time, String status, long date) {
        this.time = time;
        this.status = status;
        this.date = date;

    }

    public UserState() {
    }


    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
