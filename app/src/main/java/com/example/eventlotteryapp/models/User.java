package com.example.eventlotteryapp.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class User {
    private String deviceId;
    private String name;
    private String email;
    private String phone;
    private boolean notificationsEnabled;

    // NEW: Track user activity
    private List<String> eventsHosting;   // Events I'm organizing
    private List<String> eventsJoined;    // Events I'm currently waiting for
    private List<Map<String, Object>> eventsHistory;  // Full history

    // Required empty constructor for Firestore
    public User() {
        this.eventsHosting = new ArrayList<>();
        this.eventsJoined = new ArrayList<>();
        this.eventsHistory = new ArrayList<>();
    }

    public User(String deviceId, String name, String email, String phone) {
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.notificationsEnabled = true;
        this.eventsHosting = new ArrayList<>();
        this.eventsJoined = new ArrayList<>();
        this.eventsHistory = new ArrayList<>();
    }

    // Getters
    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public List<String> getEventsHosting() { return eventsHosting; }
    public List<String> getEventsJoined() { return eventsJoined; }
    public List<Map<String, Object>> getEventsHistory() { return eventsHistory; }

    // Setters
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
    public void setEventsHosting(List<String> eventsHosting) {
        this.eventsHosting = eventsHosting;
    }
    public void setEventsJoined(List<String> eventsJoined) {
        this.eventsJoined = eventsJoined;
    }
    public void setEventsHistory(List<Map<String, Object>> eventsHistory) {
        this.eventsHistory = eventsHistory;
    }
}