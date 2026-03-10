package com.example.eventlotteryapp.models;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private String eventId;
    private String title;
    private String description;
    private String organizerId;
    private String posterUrl;
    private String qrCodeValue;
    private Boolean geolocationRequired;
    private Boolean hasWaitlistLimit;
    private Long waitlistLimit;
    private Object registrationOpenDate;
    private Object registrationCloseDate;
    private List<String> cancelledUsers;
    private List<String> enrolledUsers;
    private List<String> invitedUsers;

    public Event() {
        cancelledUsers = new ArrayList<>();
        enrolledUsers = new ArrayList<>();
        invitedUsers = new ArrayList<>();
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getQrCodeValue() {
        return qrCodeValue;
    }

    public Boolean getGeolocationRequired() {
        return geolocationRequired;
    }

    public Boolean getHasWaitlistLimit() {
        return hasWaitlistLimit;
    }

    public Long getWaitlistLimit() {
        return waitlistLimit;
    }

    public Object getRegistrationOpenDate() {
        return registrationOpenDate;
    }

    public Object getRegistrationCloseDate() {
        return registrationCloseDate;
    }

    public List<String> getCancelledUsers() {
        return cancelledUsers;
    }

    public List<String> getEnrolledUsers() {
        return enrolledUsers;
    }

    public List<String> getInvitedUsers() {
        return invitedUsers;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public void setQrCodeValue(String qrCodeValue) {
        this.qrCodeValue = qrCodeValue;
    }

    public void setGeolocationRequired(Boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    public void setHasWaitlistLimit(Boolean hasWaitlistLimit) {
        this.hasWaitlistLimit = hasWaitlistLimit;
    }

    public void setWaitlistLimit(Long waitlistLimit) {
        this.waitlistLimit = waitlistLimit;
    }

    public void setRegistrationOpenDate(Object registrationOpenDate) {
        this.registrationOpenDate = registrationOpenDate;
    }

    public void setRegistrationCloseDate(Object registrationCloseDate) {
        this.registrationCloseDate = registrationCloseDate;
    }

    public void setCancelledUsers(List<String> cancelledUsers) {
        this.cancelledUsers = cancelledUsers;
    }

    public void setEnrolledUsers(List<String> enrolledUsers) {
        this.enrolledUsers = enrolledUsers;
    }

    public void setInvitedUsers(List<String> invitedUsers) {
        this.invitedUsers = invitedUsers;
    }
}