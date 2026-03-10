package com.example.eventlotteryapp.models;

import com.google.firebase.Timestamp;

public class Entrant {

    // Status constants
    public static final String STATUS_WAITLIST     = "waitlist";
    public static final String STATUS_INVITED      = "invited";
    public static final String STATUS_ENROLLED     = "enrolled";
    public static final String STATUS_DECLINED     = "declined";
    public static final String STATUS_CANCELLED    = "cancelled";
    public static final String STATUS_NOT_SELECTED = "not_selected";

    private String deviceId;
    private String eventId;   // not stored in Firestore doc, populated after query
    private String status;
    private Timestamp joinedAt;
    private Timestamp statusUpdatedAt;

    // Required empty constructor for Firestore
    public Entrant() {}

    public Entrant(String deviceId, String eventId, String status,
                   Timestamp joinedAt, Timestamp statusUpdatedAt) {
        this.deviceId = deviceId;
        this.eventId = eventId;
        this.status = status;
        this.joinedAt = joinedAt;
        this.statusUpdatedAt = statusUpdatedAt;
    }

    // Getters
    public String getDeviceId() { return deviceId; }
    public String getEventId() { return eventId; }
    public String getStatus() { return status; }
    public Timestamp getJoinedAt() { return joinedAt; }
    public Timestamp getStatusUpdatedAt() { return statusUpdatedAt; }

    // Setters
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setStatus(String status) { this.status = status; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }
    public void setStatusUpdatedAt(Timestamp statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }
}
