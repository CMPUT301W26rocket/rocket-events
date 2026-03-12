package com.example.eventlotteryapp.models;

import java.util.Date;

public class Entrant {

    public static final String STATUS_WAITLIST = "waitlist";
    public static final String STATUS_INVITED = "invited";

    public static final String STATUS_ENROLLED = "enrolled";
    public static final String STATUS_DECLINED = "declined";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_NOT_SELECTED = "not_selected";

    private String deviceId;
    private String status;
    private Date joinedAt;
    private Date statusUpdatedAt;

    public Entrant() {
    }

    public Entrant(String deviceId, String status, Date joinedAt, Date statusUpdatedAt) {
        this.deviceId = deviceId;
        this.status = status;
        this.joinedAt = joinedAt;
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getStatus() {
        return status;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }

    public Date getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setStatusUpdatedAt(Date statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }
}