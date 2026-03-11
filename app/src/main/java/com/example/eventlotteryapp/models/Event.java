package com.example.eventlotteryapp.models;

import java.util.Date;

public class Event {
    private String eventId;
    private String organizerId;
    private String organizerName;
    private String title;
    private String description;
    private String posterUrl;
    private String qrCodeValue;
    private Date registrationOpenDate;
    private Date registrationCloseDate;
    private boolean geolocationRequired;
    private boolean hasWaitlistLimit;
    private int waitlistLimit;

    // Required empty constructor for Firestore
    public Event() {}

    public Event(String eventId, String organizerId, String title, String description,
                 String posterUrl, String qrCodeValue, Date registrationOpenDate,
                 Date registrationCloseDate, boolean geolocationRequired,
                 boolean hasWaitlistLimit, int waitlistLimit) {
        this.eventId = eventId;
        this.organizerId = organizerId;
        this.title = title;
        this.description = description;
        this.posterUrl = posterUrl;
        this.qrCodeValue = qrCodeValue;
        this.registrationOpenDate = registrationOpenDate;
        this.registrationCloseDate = registrationCloseDate;
        this.geolocationRequired = geolocationRequired;
        this.hasWaitlistLimit = hasWaitlistLimit;
        this.waitlistLimit = waitlistLimit;
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getOrganizerId() { return organizerId; }
    public String getOrganizerName() { return organizerName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPosterUrl() { return posterUrl; }
    public String getQrCodeValue() { return qrCodeValue; }
    public Date getRegistrationOpenDate() { return registrationOpenDate; }
    public Date getRegistrationCloseDate() { return registrationCloseDate; }
    public boolean isGeolocationRequired() { return geolocationRequired; }
    public boolean isHasWaitlistLimit() { return hasWaitlistLimit; }
    public int getWaitlistLimit() { return waitlistLimit; }

    // Setters
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public void setQrCodeValue(String qrCodeValue) { this.qrCodeValue = qrCodeValue; }
    public void setRegistrationOpenDate(Date registrationOpenDate) { this.registrationOpenDate = registrationOpenDate; }
    public void setRegistrationCloseDate(Date registrationCloseDate) { this.registrationCloseDate = registrationCloseDate; }
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }
    public void setHasWaitlistLimit(boolean hasWaitlistLimit) { this.hasWaitlistLimit = hasWaitlistLimit; }
    public void setWaitlistLimit(int waitlistLimit) { this.waitlistLimit = waitlistLimit; }
}
