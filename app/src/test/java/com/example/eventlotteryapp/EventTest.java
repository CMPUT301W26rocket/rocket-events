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

    @Test
    public void registrationOpen_whenCurrentDateIsBetweenDates() {
        Date past   = new Date(System.currentTimeMillis() - 86400000L); // yesterday
        Date future = new Date(System.currentTimeMillis() + 86400000L); // tomorrow
        event.setRegistrationOpenDate(past);
        event.setRegistrationCloseDate(future);

        Date now = new Date();
        assertTrue(now.after(event.getRegistrationOpenDate())
                && now.before(event.getRegistrationCloseDate()));
    }

    @Test
    public void registrationClosed_whenCloseDateIsInPast() {
        Date past1 = new Date(System.currentTimeMillis() - 172800000L); // 2 days ago
        Date past2 = new Date(System.currentTimeMillis() - 86400000L);  // yesterday
        event.setRegistrationOpenDate(past1);
        event.setRegistrationCloseDate(past2);

        Date now = new Date();
        assertFalse(now.before(event.getRegistrationCloseDate()));
    }
}
