package com.example.eventlotteryapp.models;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class EventTest {

    @Test
    public void defaultConstructor_initializesLists() {
        Event event = new Event();

        assertNotNull(event.getInvitedUsers());
        assertNotNull(event.getEnrolledUsers());
        assertNotNull(event.getCancelledUsers());
        assertTrue(event.getInvitedUsers().isEmpty());
        assertTrue(event.getEnrolledUsers().isEmpty());
        assertTrue(event.getCancelledUsers().isEmpty());
    }

    @Test
    public void fullConstructor_setsFieldsCorrectly() {
        Date openDate = new Date();
        Date closeDate = new Date(openDate.getTime() + 1000);

        Event event = new Event(
                "event1",
                "organizer1",
                "Test Event",
                "Test Description",
                "posterUrl",
                "qr123",
                openDate,
                closeDate,
                true,
                true,
                25
        );

        assertEquals("event1", event.getEventId());
        assertEquals("organizer1", event.getOrganizerId());
        assertEquals("Test Event", event.getTitle());
        assertEquals("Test Description", event.getDescription());
        assertEquals("posterUrl", event.getPosterUrl());
        assertEquals("qr123", event.getQrCodeValue());
        assertEquals(openDate, event.getRegistrationOpenDate());
        assertEquals(closeDate, event.getRegistrationCloseDate());
        assertTrue(event.isGeolocationRequired());
        assertTrue(event.isHasWaitlistLimit());
        assertEquals(25, event.getWaitlistLimit());
    }

    @Test
    public void setters_updateFieldsCorrectly() {
        Event event = new Event();
        Date openDate = new Date();
        Date closeDate = new Date(openDate.getTime() + 5000);

        event.setEventId("e2");
        event.setOrganizerId("org2");
        event.setTitle("Updated Title");
        event.setDescription("Updated Description");
        event.setPosterUrl("newPoster");
        event.setQrCodeValue("newQr");
        event.setRegistrationOpenDate(openDate);
        event.setRegistrationCloseDate(closeDate);
        event.setGeolocationRequired(false);
        event.setHasWaitlistLimit(true);
        event.setWaitlistLimit(10);

        assertEquals("e2", event.getEventId());
        assertEquals("org2", event.getOrganizerId());
        assertEquals("Updated Title", event.getTitle());
        assertEquals("Updated Description", event.getDescription());
        assertEquals("newPoster", event.getPosterUrl());
        assertEquals("newQr", event.getQrCodeValue());
        assertEquals(openDate, event.getRegistrationOpenDate());
        assertEquals(closeDate, event.getRegistrationCloseDate());
        assertFalse(event.isGeolocationRequired());
        assertTrue(event.isHasWaitlistLimit());
        assertEquals(10, event.getWaitlistLimit());
    }
}