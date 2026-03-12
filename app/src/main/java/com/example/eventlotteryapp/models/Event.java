package com.example.eventlotteryapp.models;

import java.util.Date;

/**
 * Represents an event in the lottery system.
 * Event documents are stored in Firestore under {@code events/{eventId}}.
 * Each event has an organizer, registration window, lottery capacity, and optional waitlist limit.
 */
public class Event {
    private String eventId;
    private String organizerId;
    private String organizerName;
    private String title;
    private String description;
    private String location;
    private String posterUrl;
    private String qrCodeValue;
    private double registrationFee;      // defaults to 0.0 (free)
    private int lotteryCapacity;         // how many people get selected in the lottery
    private Date eventStartDate;         // when the event actually happens
    private Date registrationOpenDate;   // when people can start joining the waitlist
    private Date registrationCloseDate;  // when waitlist closes and lottery runs
    private boolean geolocationRequired;
    private boolean hasWaitlistLimit;
    private int waitlistLimit;

    /** Required empty constructor for Firestore deserialization. */
    public Event() {}

    // --- Getters ---

    /**
     * Returns the unique Firestore document ID for this event.
     * Used to identify the event across the app and in subcollections.
     *
     * @return the event ID string
     */
    public String getEventId() { return eventId; }

    /**
     * Returns the device ID of the user who created this event.
     * Matches the Firestore document ID under {@code users/}.
     *
     * @return the organizer's device ID string
     */
    public String getOrganizerId() { return organizerId; }

    /**
     * Returns the display name of the organizer who created this event.
     * Populated at event creation time from the organizer's user profile.
     *
     * @return the organizer's display name
     */
    public String getOrganizerName() { return organizerName; }

    /**
     * Returns the title of the event as entered by the organizer.
     *
     * @return the event title string
     */
    public String getTitle() { return title; }

    /**
     * Returns the full description of the event as entered by the organizer.
     *
     * @return the event description string
     */
    public String getDescription() { return description; }

    /**
     * Returns the physical location or venue of the event.
     *
     * @return the location string, or null if not set
     */
    public String getLocation() { return location; }

    /**
     * Returns the Firebase Storage download URL for this event's poster image.
     * May be null if no poster has been uploaded.
     *
     * @return the poster image URL, or null if no poster was uploaded
     */
    public String getPosterUrl() { return posterUrl; }

    /**
     * Returns the QR code value associated with this event.
     * Used by entrants to look up or join the event by scanning a QR code.
     *
     * @return the QR code string, or null if not generated
     */
    public String getQrCodeValue() { return qrCodeValue; }

    /**
     * Returns the registration fee for this event in dollars.
     * Defaults to {@code 0.0} for free events.
     *
     * @return the registration fee as a double (e.g. 10.0 means $10.00)
     */
    public double getRegistrationFee() { return registrationFee; }

    /**
     * Returns the maximum number of participants that will be selected in the lottery draw.
     *
     * @return the lottery capacity as an integer
     */
    public int getLotteryCapacity() { return lotteryCapacity; }

    /**
     * Returns the date and time when the event itself takes place.
     * This is distinct from the registration window dates.
     *
     * @return the event start {@link Date}, or null if not set
     */
    public Date getEventStartDate() { return eventStartDate; }

    /**
     * Returns the date and time when the waitlist opens for this event.
     * Users cannot join the waitlist before this date.
     *
     * @return the registration open {@link Date}, or null if not set
     */
    public Date getRegistrationOpenDate() { return registrationOpenDate; }

    /**
     * Returns the date and time when the waitlist closes for this event.
     * After this date, the lottery draw runs and no new entries are accepted.
     *
     * @return the registration close {@link Date}, or null if not set
     */
    public Date getRegistrationCloseDate() { return registrationCloseDate; }

    /**
     * Returns whether entrants are required to share their geolocation when joining the waitlist.
     *
     * @return true if geolocation is required, false otherwise
     */
    public boolean isGeolocationRequired() { return geolocationRequired; }

    /**
     * Returns whether this event has a maximum waitlist size enforced.
     * If true, no more than {@link #getWaitlistLimit()} users can join the waitlist.
     *
     * @return true if a waitlist limit is active, false if the waitlist is unlimited
     */
    public boolean isHasWaitlistLimit() { return hasWaitlistLimit; }

    /**
     * Returns the maximum number of users allowed on the waitlist.
     * Only meaningful when {@link #isHasWaitlistLimit()} returns true.
     *
     * @return the waitlist size limit as an integer
     */
    public int getWaitlistLimit() { return waitlistLimit; }

    // --- Setters ---

    /**
     * Sets the Firestore document ID for this event.
     * Typically set after the document is created in Firestore.
     *
     * @param eventId the unique event ID string
     */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /**
     * Sets the device ID of the user who owns this event.
     * Should match the Firestore document ID under {@code users/}.
     *
     * @param organizerId the organizer's device ID string
     */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    /**
     * Sets the display name of the organizer for this event.
     * Shown in the event list so attendees know who created the event.
     *
     * @param organizerName the organizer's display name
     */
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    /**
     * Updates the title of this event.
     *
     * @param title the new event title
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Updates the full description of this event.
     *
     * @param description the new event description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Updates the physical location or venue for this event.
     *
     * @param location the new location string
     */
    public void setLocation(String location) { this.location = location; }

    /**
     * Sets the Firebase Storage download URL for this event's poster image.
     * Called after a successful image upload to link the image to the event.
     *
     * @param posterUrl the public download URL of the uploaded poster image
     */
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    /**
     * Sets the QR code value for this event.
     * Used to generate a scannable QR code that links to this event.
     *
     * @param qrCodeValue the QR code content string
     */
    public void setQrCodeValue(String qrCodeValue) { this.qrCodeValue = qrCodeValue; }

    /**
     * Updates the registration fee for this event.
     * Use {@code 0.0} for free events.
     *
     * @param registrationFee the fee in dollars (e.g. 10.0 means $10.00)
     */
    public void setRegistrationFee(double registrationFee) { this.registrationFee = registrationFee; }

    /**
     * Sets the maximum number of participants that can be selected in the lottery draw.
     *
     * @param lotteryCapacity the number of winners to select
     */
    public void setLotteryCapacity(int lotteryCapacity) { this.lotteryCapacity = lotteryCapacity; }

    /**
     * Sets the date and time when the event itself takes place.
     *
     * @param eventStartDate the {@link Date} when the event begins
     */
    public void setEventStartDate(Date eventStartDate) { this.eventStartDate = eventStartDate; }

    /**
     * Sets the date and time when the waitlist opens for this event.
     * Users can join the waitlist starting from this date.
     *
     * @param registrationOpenDate the {@link Date} when registration opens
     */
    public void setRegistrationOpenDate(Date registrationOpenDate) { this.registrationOpenDate = registrationOpenDate; }

    /**
     * Sets the date and time when the waitlist closes for this event.
     * After this date, the lottery draw runs and no new entries are accepted.
     *
     * @param registrationCloseDate the {@link Date} when registration closes
     */
    public void setRegistrationCloseDate(Date registrationCloseDate) { this.registrationCloseDate = registrationCloseDate; }

    /**
     * Sets whether entrants must share their geolocation when joining the waitlist.
     *
     * @param geolocationRequired true to require geolocation, false to make it optional
     */
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }

    /**
     * Sets whether a maximum waitlist size should be enforced for this event.
     *
     * @param hasWaitlistLimit true to enable the waitlist limit, false to allow unlimited entries
     */
    public void setHasWaitlistLimit(boolean hasWaitlistLimit) { this.hasWaitlistLimit = hasWaitlistLimit; }

    /**
     * Sets the maximum number of users allowed on the waitlist.
     * Only enforced when {@link #isHasWaitlistLimit()} returns true.
     *
     * @param waitlistLimit the maximum waitlist size
     */
    public void setWaitlistLimit(int waitlistLimit) { this.waitlistLimit = waitlistLimit; }
}
