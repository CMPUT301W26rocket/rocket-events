package com.example.eventlotteryapp.models;

import com.google.firebase.Timestamp;

/**
 * Represents one user's relationship to one event in the lottery system.
 * Entrant documents are stored at {@code events/{eventId}/entrants/{deviceId}} in Firestore.
 * The {@code eventId} field is not persisted to Firestore; it is populated locally after a query.
 */
public class Entrant {

    /** Status constant indicating the user is on the waitlist. */
    public static final String STATUS_WAITLIST     = "waitlist";
    /** Status constant indicating the user has been invited via the lottery draw. */
    public static final String STATUS_INVITED      = "invited";
    /** Status constant indicating the user accepted the invitation and is enrolled. */
    public static final String STATUS_ENROLLED     = "enrolled";
    /** Status constant indicating the user declined the lottery invitation. */
    public static final String STATUS_DECLINED     = "declined";
    /** Status constant indicating the user cancelled their enrollment. */
    public static final String STATUS_CANCELLED    = "cancelled";
    /** Status constant indicating the user was not selected in the lottery draw. */
    public static final String STATUS_NOT_SELECTED  = "not_selected";
    /** Status constant indicating the user has been invited to join a private event's waitlist. */
    public static final String STATUS_WAITLIST_INVITED  = "waitlist_invited";
    /** Status constant indicating the user declined an invitation to join a private event's waitlist. */
    public static final String STATUS_DECLINED_WAITLIST = "declined_waitlist";

    private String deviceId;
    private String eventId;   // not stored in Firestore doc, populated after query
    private String status;
    private Timestamp joinedAt;
    private Timestamp statusUpdatedAt;

    /** Required empty constructor for Firestore deserialization. */
    public Entrant() {}

    /**
     * Creates a fully initialised Entrant.
     *
     * @param deviceId        the entrant's device ID
     * @param eventId         the event this entrant belongs to (local only, not stored in Firestore)
     * @param status          the entrant's current status (use one of the {@code STATUS_*} constants)
     * @param joinedAt        the timestamp when the user joined the waitlist
     * @param statusUpdatedAt the timestamp when the status was last changed
     */
    public Entrant(String deviceId, String eventId, String status,
                   Timestamp joinedAt, Timestamp statusUpdatedAt) {
        this.deviceId = deviceId;
        this.eventId = eventId;
        this.status = status;
        this.joinedAt = joinedAt;
        this.statusUpdatedAt = statusUpdatedAt;
    }

    // --- Getters ---

    public String getDeviceId() { return deviceId; }
    public String getEventId() { return eventId; }
    public String getStatus() { return status; }
    public Timestamp getJoinedAt() { return joinedAt; }
    public Timestamp getStatusUpdatedAt() { return statusUpdatedAt; }

    // --- Setters ---

    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setStatus(String status) { this.status = status; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }
    public void setStatusUpdatedAt(Timestamp statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }
}
