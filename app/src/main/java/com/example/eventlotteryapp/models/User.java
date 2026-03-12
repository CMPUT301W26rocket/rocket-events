package com.example.eventlotteryapp.models;

/**
 * Represents a registered user in the event lottery app.
 * User documents are stored in Firestore under {@code users/{deviceId}}.
 */
public class User {
    private String deviceId;
    private String name;
    private String email;
    private String phone;
    private boolean notificationsEnabled;

    /** Required empty constructor for Firestore deserialization. */
    public User() {}

    /**
     * Creates a new User with all required profile fields.
     * Notifications are enabled by default.
     *
     * @param deviceId the device's unique ANDROID_ID, used as the Firestore document ID
     * @param name     the user's display name shown across the app
     * @param email    the user's email address for contact purposes
     * @param phone    the user's phone number (optional, may be empty)
     */
    public User(String deviceId, String name, String email, String phone) {
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.notificationsEnabled = true;
    }

    // --- Getters ---

    /**
     * Returns the device's unique ANDROID_ID.
     * This is also the Firestore document ID for this user under {@code users/}.
     *
     * @return the device ID string
     */
    public String getDeviceId() { return deviceId; }

    /**
     * Returns the user's display name as entered during profile setup.
     *
     * @return the full name of the user
     */
    public String getName() { return name; }

    /**
     * Returns the user's email address as entered during profile setup.
     *
     * @return the email address string
     */
    public String getEmail() { return email; }

    /**
     * Returns the user's phone number. May be empty if the user skipped it during setup.
     *
     * @return the phone number string, or empty string if not provided
     */
    public String getPhone() { return phone; }

    /**
     * Returns whether the user has opted in to push notifications.
     * Defaults to true for all new users.
     *
     * @return true if notifications are enabled, false otherwise
     */
    public boolean isNotificationsEnabled() { return notificationsEnabled; }

    // --- Setters ---

    /**
     * Sets the device ID for this user.
     * Should match the device's ANDROID_ID and the Firestore document ID.
     *
     * @param deviceId the device's unique ANDROID_ID
     */
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    /**
     * Updates the user's display name.
     *
     * @param name the new display name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Updates the user's email address.
     *
     * @param email the new email address
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Updates the user's phone number.
     *
     * @param phone the new phone number, or empty string if not provided
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * Enables or disables push notifications for this user.
     *
     * @param notificationsEnabled true to enable notifications, false to disable
     */
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
}
