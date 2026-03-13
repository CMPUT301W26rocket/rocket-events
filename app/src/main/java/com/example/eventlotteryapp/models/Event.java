package com.example.eventlotteryapp.models;

import java.util.Date;
import java.util.Calendar;

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

    // --- Business logic ---

    /**
     * Returns whether the registration window is currently open.
     * Returns {@code false} if either date is null.
     */
    public boolean isRegistrationOpen() {
        if (registrationOpenDate == null || registrationCloseDate == null) return false;
        Date now = Calendar.getInstance().getTime();
        return !now.before(registrationOpenDate) && !now.after(registrationCloseDate);
    }

    /**
     * Returns whether the registration window has not yet opened.
     * Returns {@code false} if {@code registrationOpenDate} is null.
     */
    public boolean isRegistrationNotYetOpen() {
        if (registrationOpenDate == null) return false;
        return Calendar.getInstance().getTime().before(registrationOpenDate);
    }

    // --- Getters ---

    public String getEventId() { return eventId; }
    public String getOrganizerId() { return organizerId; }
    public String getOrganizerName() { return organizerName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getPosterUrl() { return posterUrl; }
    public String getQrCodeValue() { return qrCodeValue; }
    public double getRegistrationFee() { return registrationFee; }
    public int getLotteryCapacity() { return lotteryCapacity; }
    public Date getEventStartDate() { return eventStartDate; }
    public Date getRegistrationOpenDate() { return registrationOpenDate; }
    public Date getRegistrationCloseDate() { return registrationCloseDate; }
    public boolean isGeolocationRequired() { return geolocationRequired; }
    public boolean isHasWaitlistLimit() { return hasWaitlistLimit; }
    public int getWaitlistLimit() { return waitlistLimit; }

    // --- Setters ---

    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public void setQrCodeValue(String qrCodeValue) { this.qrCodeValue = qrCodeValue; }
    public void setRegistrationFee(double registrationFee) { this.registrationFee = registrationFee; }
    public void setLotteryCapacity(int lotteryCapacity) { this.lotteryCapacity = lotteryCapacity; }
    public void setEventStartDate(Date eventStartDate) { this.eventStartDate = eventStartDate; }
    public void setRegistrationOpenDate(Date registrationOpenDate) { this.registrationOpenDate = registrationOpenDate; }
    public void setRegistrationCloseDate(Date registrationCloseDate) { this.registrationCloseDate = registrationCloseDate; }
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }
    public void setHasWaitlistLimit(boolean hasWaitlistLimit) { this.hasWaitlistLimit = hasWaitlistLimit; }
    public void setWaitlistLimit(int waitlistLimit) { this.waitlistLimit = waitlistLimit; }
}
