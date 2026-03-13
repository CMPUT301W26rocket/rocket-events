package com.example.eventlotteryapp;

import com.example.eventlotteryapp.models.Event;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link Event} model class.
 */
public class EventTest {

    private Event event;

    @Before
    public void setUp() {
        event = new Event();
        event.setEventId("event123");
        event.setOrganizerId("device123");
        event.setOrganizerName("Jane Smith");
        event.setTitle("Test Event");
        event.setDescription("A test event description");
        event.setLocation("Edmonton Convention Centre");
        event.setRegistrationFee(10.00);
        event.setLotteryCapacity(50);
        event.setGeolocationRequired(false);
        event.setHasWaitlistLimit(true);
        event.setWaitlistLimit(100);
    }

    @Test
    public void setTitle_updatesTitle() {
        event.setTitle("New Title");
        assertEquals("New Title", event.getTitle());
    }

    @Test
    public void setDescription_updatesDescription() {
        event.setDescription("Updated description");
        assertEquals("Updated description", event.getDescription());
    }

    @Test
    public void setLocation_updatesLocation() {
        event.setLocation("MacEwan University");
        assertEquals("MacEwan University", event.getLocation());
    }

    @Test
    public void setOrganizerId_updatesOrganizerId() {
        event.setOrganizerId("newDevice456");
        assertEquals("newDevice456", event.getOrganizerId());
    }

    @Test
    public void setOrganizerName_updatesOrganizerName() {
        event.setOrganizerName("John Doe");
        assertEquals("John Doe", event.getOrganizerName());
    }

    @Test
    public void registrationFee_defaultsToZero() {
        Event freshEvent = new Event();
        assertEquals(0.0, freshEvent.getRegistrationFee(), 0.001);
    }

    @Test
    public void setRegistrationFee_updatesFee() {
        event.setRegistrationFee(25.50);
        assertEquals(25.50, event.getRegistrationFee(), 0.001);
    }

    @Test
    public void setLotteryCapacity_updatesCapacity() {
        event.setLotteryCapacity(200);
        assertEquals(200, event.getLotteryCapacity());
    }

    @Test
    public void setWaitlistLimit_updatesLimit() {
        event.setWaitlistLimit(150);
        assertEquals(150, event.getWaitlistLimit());
    }

    @Test
    public void setHasWaitlistLimit_updatesFlag() {
        event.setHasWaitlistLimit(false);
        assertFalse(event.isHasWaitlistLimit());
    }

    @Test
    public void setGeolocationRequired_updatesFlag() {
        event.setGeolocationRequired(true);
        assertTrue(event.isGeolocationRequired());
    }

    @Test
    public void setRegistrationDates_updatesDates() {
        Date open  = new Date(1000000L);
        Date close = new Date(2000000L);
        event.setRegistrationOpenDate(open);
        event.setRegistrationCloseDate(close);
        assertEquals(open, event.getRegistrationOpenDate());
        assertEquals(close, event.getRegistrationCloseDate());
    }

    @Test
    public void setEventStartDate_updatesDate() {
        Date start = new Date(3000000L);
        event.setEventStartDate(start);
        assertEquals(start, event.getEventStartDate());
    }

    @Test
    public void setPosterUrl_updatesPosterUrl() {
        event.setPosterUrl("https://example.com/poster.jpg");
        assertEquals("https://example.com/poster.jpg", event.getPosterUrl());
    }

    // --- isRegistrationOpen() tests ---

    @Test
    public void isRegistrationOpen_returnsTrueWhenNowIsBetweenDates() {
        // Open date: yesterday, close date: tomorrow — registration is active
        event.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 86400000L));
        event.setRegistrationCloseDate(new Date(System.currentTimeMillis() + 86400000L));
        assertTrue(event.isRegistrationOpen());
    }

    @Test
    public void isRegistrationOpen_returnsFalseWhenCloseDateIsInPast() {
        // Both dates in the past — registration has ended
        event.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 172800000L));
        event.setRegistrationCloseDate(new Date(System.currentTimeMillis() - 86400000L));
        assertFalse(event.isRegistrationOpen());
    }

    @Test
    public void isRegistrationOpen_returnsFalseWhenOpenDateIsInFuture() {
        // Both dates in the future — registration has not started
        event.setRegistrationOpenDate(new Date(System.currentTimeMillis() + 86400000L));
        event.setRegistrationCloseDate(new Date(System.currentTimeMillis() + 172800000L));
        assertFalse(event.isRegistrationOpen());
    }

    @Test
    public void isRegistrationOpen_returnsFalseWhenDatesAreNull() {
        // No dates set — should not crash and should return false
        event.setRegistrationOpenDate(null);
        event.setRegistrationCloseDate(null);
        assertFalse(event.isRegistrationOpen());
    }

    // --- isRegistrationNotYetOpen() tests ---

    @Test
    public void isRegistrationNotYetOpen_returnsTrueWhenOpenDateIsInFuture() {
        // Open date is tomorrow — registration hasn't started yet
        event.setRegistrationOpenDate(new Date(System.currentTimeMillis() + 86400000L));
        assertTrue(event.isRegistrationNotYetOpen());
    }

    @Test
    public void isRegistrationNotYetOpen_returnsFalseWhenOpenDateIsInPast() {
        // Open date was yesterday — registration has already opened
        event.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 86400000L));
        assertFalse(event.isRegistrationNotYetOpen());
    }

    @Test
    public void isRegistrationNotYetOpen_returnsFalseWhenOpenDateIsNull() {
        // No open date set — should not crash and should return false
        event.setRegistrationOpenDate(null);
        assertFalse(event.isRegistrationNotYetOpen());
    }
}
