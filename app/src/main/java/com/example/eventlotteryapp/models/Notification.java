package com.example.eventlotteryapp.models;

import com.google.firebase.Timestamp;

/**
 * Represents a notification sent to a user.
 * Documents are stored at {@code users/{deviceId}/notifications/{notificationId}}.
 */
public class Notification {

    /** Notification type: user was selected in the lottery. */
    public static final String TYPE_WON         = "won";
    /** Notification type: user was not selected in the lottery. */
    public static final String TYPE_LOST        = "lost";
    /** Notification type: user has been given a replacement invitation. */
    public static final String TYPE_REPLACEMENT = "replacement";
    /** Notification type: general message from the organizer. */
    public static final String TYPE_GENERAL     = "general";

    private String notificationId;
    private String eventId;
    private String eventTitle;
    private String type;
    private String message;
    private boolean read;
    private Timestamp createdAt;

    /** Required empty constructor for Firestore deserialization. */
    public Notification() {}

    public Notification(String eventId, String eventTitle, String type, String message) {
        this.eventId    = eventId;
        this.eventTitle = eventTitle;
        this.type       = type;
        this.message    = message;
        this.read       = false;
        this.createdAt  = Timestamp.now();
    }

    // --- Getters ---

    public String getNotificationId() { return notificationId; }
    public String getEventId()        { return eventId; }
    public String getEventTitle()     { return eventTitle; }
    public String getType()           { return type; }
    public String getMessage()        { return message; }
    public boolean isRead()           { return read; }
    public Timestamp getCreatedAt()   { return createdAt; }

    // --- Setters ---

    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setEventId(String eventId)               { this.eventId = eventId; }
    public void setEventTitle(String eventTitle)         { this.eventTitle = eventTitle; }
    public void setType(String type)                     { this.type = type; }
    public void setMessage(String message)               { this.message = message; }
    public void setRead(boolean read)                    { this.read = read; }
    public void setCreatedAt(Timestamp createdAt)        { this.createdAt = createdAt; }
}
