package com.example.eventlotteryapp.models;

import com.google.firebase.Timestamp;

/**
 * Represents a notification sent to a user.
 * Documents are stored at {@code users/{deviceId}/notifications/{notificationId}}.
 *
 * @author Mazen
 */
public class Notification {

    /** Notification type for users selected in the lottery. */
    public static final String TYPE_WON = "won";

    /** Notification type for users not selected in the lottery. */
    public static final String TYPE_LOST = "lost";

    /** Notification type for users chosen as replacement entrants. */
    public static final String TYPE_REPLACEMENT = "replacement";

    /** Notification type for general organizer or system messages. */
    public static final String TYPE_GENERAL = "general";

    /** Notification type for private waitlist invitations. */
    public static final String TYPE_WAITLIST_INVITE = "waitlist_invite";

    /** Notification type for co-organizer assignments. */
    public static final String TYPE_CO_ORGANIZER = "co_organizer";

    private String notificationId;
    private String eventId;
    private String eventTitle;
    private String type;
    private String message;
    private boolean read;
    private Timestamp createdAt;

    private String senderOrganizerId;
    private String senderOrganizerName;
    private String recipientDeviceId;
    private String recipientName;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Notification() {}

    /**
     * Creates a notification with basic event and message information.
     *
     * @param eventId related event ID
     * @param eventTitle related event title
     * @param type notification type
     * @param message notification message body
     */
    public Notification(String eventId, String eventTitle, String type, String message) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.type = type;
        this.message = message;
        this.read = false;
        this.createdAt = Timestamp.now();
    }

    /**
     * Creates a notification with sender and recipient details included.
     *
     * @param eventId related event ID
     * @param eventTitle related event title
     * @param type notification type
     * @param message notification message body
     * @param senderOrganizerId organizer device ID of the sender
     * @param senderOrganizerName organizer name of the sender
     * @param recipientDeviceId device ID of the recipient
     * @param recipientName display name of the recipient
     */
    public Notification(String eventId,
                        String eventTitle,
                        String type,
                        String message,
                        String senderOrganizerId,
                        String senderOrganizerName,
                        String recipientDeviceId,
                        String recipientName) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.type = type;
        this.message = message;
        this.senderOrganizerId = senderOrganizerId;
        this.senderOrganizerName = senderOrganizerName;
        this.recipientDeviceId = recipientDeviceId;
        this.recipientName = recipientName;
        this.read = false;
        this.createdAt = Timestamp.now();
    }

    /**
     * Returns the notification document ID.
     *
     * @return notification ID
     */
    public String getNotificationId() { return notificationId; }

    /**
     * Returns the related event ID.
     *
     * @return event ID
     */
    public String getEventId() { return eventId; }

    /**
     * Returns the related event title.
     *
     * @return event title
     */
    public String getEventTitle() { return eventTitle; }

    /**
     * Returns the notification type.
     *
     * @return notification type
     */
    public String getType() { return type; }

    /**
     * Returns the notification message body.
     *
     * @return notification message
     */
    public String getMessage() { return message; }

    /**
     * Returns whether the notification has been read.
     *
     * @return true if the notification has been read, false otherwise
     */
    public boolean isRead() { return read; }

    /**
     * Returns the notification creation timestamp.
     *
     * @return creation timestamp
     */
    public Timestamp getCreatedAt() { return createdAt; }

    /**
     * Returns the sender organizer's device ID.
     *
     * @return sender organizer ID
     */
    public String getSenderOrganizerId() { return senderOrganizerId; }

    /**
     * Returns the sender organizer's name.
     *
     * @return sender organizer name
     */
    public String getSenderOrganizerName() { return senderOrganizerName; }

    /**
     * Returns the recipient device ID.
     *
     * @return recipient device ID
     */
    public String getRecipientDeviceId() { return recipientDeviceId; }

    /**
     * Returns the recipient name.
     *
     * @return recipient name
     */
    public String getRecipientName() { return recipientName; }

    /**
     * Sets the notification document ID.
     *
     * @param notificationId notification ID
     */
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    /**
     * Sets the related event ID.
     *
     * @param eventId event ID
     */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /**
     * Sets the related event title.
     *
     * @param eventTitle event title
     */
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    /**
     * Sets the notification type.
     *
     * @param type notification type
     */
    public void setType(String type) { this.type = type; }

    /**
     * Sets the notification message body.
     *
     * @param message notification message
     */
    public void setMessage(String message) { this.message = message; }

    /**
     * Sets whether the notification has been read.
     *
     * @param read read status
     */
    public void setRead(boolean read) { this.read = read; }

    /**
     * Sets the notification creation timestamp.
     *
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /**
     * Sets the sender organizer's device ID.
     *
     * @param senderOrganizerId sender organizer ID
     */
    public void setSenderOrganizerId(String senderOrganizerId) { this.senderOrganizerId = senderOrganizerId; }

    /**
     * Sets the sender organizer's name.
     *
     * @param senderOrganizerName sender organizer name
     */
    public void setSenderOrganizerName(String senderOrganizerName) { this.senderOrganizerName = senderOrganizerName; }

    /**
     * Sets the recipient device ID.
     *
     * @param recipientDeviceId recipient device ID
     */
    public void setRecipientDeviceId(String recipientDeviceId) { this.recipientDeviceId = recipientDeviceId; }

    /**
     * Sets the recipient name.
     *
     * @param recipientName recipient name
     */
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
}