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
    public static final String STATUS_NOT_SELECTED = "not_selected";

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

    /**
     * Returns the device ID of the user who joined this event's waitlist.
     * Matches the Firestore document ID under {@code users/} and the subcollection document ID.
     *
     * @return the entrant's device ID string
     */
    public String getDeviceId() { return deviceId; }

    /**
     * Returns the event ID this entrant is associated with.
     * This field is not stored in the Firestore document — it is set locally after querying
     * the entrant from a subcollection.
     *
     * @return the event ID string, populated locally after a Firestore query
     */
    public String getEventId() { return eventId; }

    /**
     * Returns the entrant's current status in the event lottery lifecycle.
     * Use the {@code STATUS_*} constants to compare against known values
     * (e.g. {@link #STATUS_WAITLIST}, {@link #STATUS_INVITED}, {@link #STATUS_ENROLLED}).
     *
     * @return the status string representing the entrant's current state
     */
    public String getStatus() { return status; }

    /**
     * Returns the timestamp when the user first joined the waitlist for this event.
     *
     * @return the {@link Timestamp} of when the entrant joined, or null if not set
     */
    public Timestamp getJoinedAt() { return joinedAt; }

    /**
     * Returns the timestamp when the entrant's status was last updated.
     * Updated whenever the status changes (e.g. from waitlist to invited).
     *
     * @return the {@link Timestamp} of the most recent status change, or null if not set
     */
    public Timestamp getStatusUpdatedAt() { return statusUpdatedAt; }

    // --- Setters ---

    /**
     * Sets the device ID for this entrant.
     * Should match the Firestore subcollection document ID and the user's ANDROID_ID.
     *
     * @param deviceId the entrant's device ID string
     */
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    /**
     * Sets the event ID this entrant is associated with.
     * This field is local only — it is not written to the Firestore document.
     * Use it to track which event an entrant belongs to after a collection group query.
     *
     * @param eventId the event ID string
     */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /**
     * Updates the entrant's current status in the lottery lifecycle.
     * Use one of the {@code STATUS_*} constants (e.g. {@link #STATUS_INVITED}).
     *
     * @param status the new status string
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * Sets the timestamp for when this entrant joined the waitlist.
     * Typically set once when the entrant document is first created.
     *
     * @param joinedAt the {@link Timestamp} of when the user joined
     */
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    /**
     * Sets the timestamp for when the entrant's status was last updated.
     * Should be updated every time the status field changes.
     *
     * @param statusUpdatedAt the {@link Timestamp} of the most recent status change
     */
    public void setStatusUpdatedAt(Timestamp statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }
}
