package com.example.eventlotteryapp.models;

public class User {
    private String deviceId;
    private String name;
    private String email;
    private String phone;
    private boolean notificationsEnabled;

    // Required empty constructor for Firestore
    public User() {}

    public User(String deviceId, String name, String email, String phone) {
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.notificationsEnabled = true;
    }

    // Getters
    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }

    // Setters
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
}
