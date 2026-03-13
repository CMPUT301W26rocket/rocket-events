package com.example.eventlotteryapp.models;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class EntrantTest {

    @Test
    public void defaultConstructor_initializesFields() {
        Entrant entrant = new Entrant();

        assertNull(entrant.getDeviceId());
        assertNull(entrant.getStatus());
        assertNull(entrant.getJoinedAt());
        assertNull(entrant.getStatusUpdatedAt());
    }

    @Test
    public void fullConstructor_setsAllFieldsCorrectly() {
        Date joinedAt = new Date();
        Date updatedAt = new Date(joinedAt.getTime() + 1000);

        Entrant entrant = new Entrant("abc123", Entrant.STATUS_WAITLIST, joinedAt, updatedAt);

        assertEquals("abc123", entrant.getDeviceId());
        assertEquals(Entrant.STATUS_WAITLIST, entrant.getStatus());
        assertEquals(joinedAt, entrant.getJoinedAt());
        assertEquals(updatedAt, entrant.getStatusUpdatedAt());
    }

    @Test
    public void setters_updateFieldsCorrectly() {
        Entrant entrant = new Entrant();
        Date joinedAt = new Date();
        Date updatedAt = new Date(joinedAt.getTime() + 2000);

        entrant.setDeviceId("user42");
        entrant.setStatus(Entrant.STATUS_INVITED);
        entrant.setJoinedAt(joinedAt);
        entrant.setStatusUpdatedAt(updatedAt);

        assertEquals("user42", entrant.getDeviceId());
        assertEquals(Entrant.STATUS_INVITED, entrant.getStatus());
        assertEquals(joinedAt, entrant.getJoinedAt());
        assertEquals(updatedAt, entrant.getStatusUpdatedAt());
    }

    @Test
    public void statusConstants_haveExpectedValues() {
        assertEquals("waitlist", Entrant.STATUS_WAITLIST);
        assertEquals("invited", Entrant.STATUS_INVITED);
        assertEquals("enrolled", Entrant.STATUS_ENROLLED);
        assertEquals("declined", Entrant.STATUS_DECLINED);
        assertEquals("cancelled", Entrant.STATUS_CANCELLED);
        assertEquals("not_selected", Entrant.STATUS_NOT_SELECTED);
    }
}