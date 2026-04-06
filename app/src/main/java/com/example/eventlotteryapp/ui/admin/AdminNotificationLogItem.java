package com.example.eventlotteryapp.ui.admin;

import com.google.firebase.Timestamp;
/**
 * Data model for one notification log entry shown in the admin logs screen.
 * Each item stores the recipient device ID, event title, notification type,
 * message body, and creation timestamp.
 *
 * @author Mazen
 */
public class AdminNotificationLogItem {

    private final String recipientDeviceId;
    private String recipientName;
    private final String eventTitle;
    private final String type;
    private final String message;
    private final Timestamp createdAt;

    /**
     * Creates a new admin notification log item.
     *
     * @param recipientDeviceId device ID of the notification recipient
     * @param eventTitle title of the related event
     * @param type notification type
     * @param message notification message body
     * @param createdAt time the notification was created
     */
    public AdminNotificationLogItem(String recipientDeviceId,
                                    String eventTitle,
                                    String type,
                                    String message,
                                    Timestamp createdAt) {
        this.recipientDeviceId = recipientDeviceId;
        this.eventTitle = eventTitle;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
    }
    /**
     * Returns the recipient device ID for this notification log item.
     *
     * @return recipient device ID
     */
    public String getRecipientDeviceId() {
        return recipientDeviceId;
    }

    /**
     * Returns the display name of the recipient, or {@code null} if not yet resolved.
     *
     * @return recipient name
     */
    public String getRecipientName() {
        return recipientName;
    }

    /**
     * Sets the display name of the recipient after resolving from Firestore.
     *
     * @param recipientName the resolved display name
     */
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }
    /**
     * Returns the title of the event associated with this notification.
     *
     * @return event title
     */
    public String getEventTitle() {
        return eventTitle;
    }
    /**
     * Returns the notification type.
     *
     * @return notification type
     */
    public String getType() {
        return type;
    }
    /**
     * Returns the notification message body.
     *
     * @return notification message
     */
    public String getMessage() {
        return message;
    }
    /**
     * Returns the time the notification was created.
     *
     * @return creation timestamp
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }
}