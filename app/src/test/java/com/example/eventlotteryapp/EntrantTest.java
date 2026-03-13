package com.example.eventlotteryapp;

import com.example.eventlotteryapp.models.Entrant;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link Entrant} model class.
 */
public class EntrantTest {

    private Entrant entrant;

    @Before
    public void setUp() {
        entrant = new Entrant();
        entrant.setDeviceId("device123");
        entrant.setEventId("event456");
        entrant.setStatus(Entrant.STATUS_WAITLIST);
    }

    @Test
    public void statusConstant_waitlist_isCorrect() {
        assertEquals("waitlist", Entrant.STATUS_WAITLIST);
    }

    @Test
    public void statusConstant_invited_isCorrect() {
        assertEquals("invited", Entrant.STATUS_INVITED);
    }

    @Test
    public void statusConstant_enrolled_isCorrect() {
        assertEquals("enrolled", Entrant.STATUS_ENROLLED);
    }

    @Test
    public void statusConstant_declined_isCorrect() {
        assertEquals("declined", Entrant.STATUS_DECLINED);
    }

    @Test
    public void statusConstant_cancelled_isCorrect() {
        assertEquals("cancelled", Entrant.STATUS_CANCELLED);
    }

    @Test
    public void statusConstant_notSelected_isCorrect() {
        assertEquals("not_selected", Entrant.STATUS_NOT_SELECTED);
    }

    @Test
    public void setDeviceId_updatesDeviceId() {
        entrant.setDeviceId("newDevice789");
        assertEquals("newDevice789", entrant.getDeviceId());
    }

    @Test
    public void setEventId_updatesEventId() {
        entrant.setEventId("newEvent999");
        assertEquals("newEvent999", entrant.getEventId());
    }

    @Test
    public void setStatus_updatesStatus() {
        entrant.setStatus(Entrant.STATUS_INVITED);
        assertEquals(Entrant.STATUS_INVITED, entrant.getStatus());
    }

    @Test
    public void statusTransition_waitlistToInvited() {
        entrant.setStatus(Entrant.STATUS_WAITLIST);
        assertEquals(Entrant.STATUS_WAITLIST, entrant.getStatus());

        entrant.setStatus(Entrant.STATUS_INVITED);
        assertEquals(Entrant.STATUS_INVITED, entrant.getStatus());
    }

    @Test
    public void statusTransition_invitedToEnrolled() {
        entrant.setStatus(Entrant.STATUS_INVITED);
        entrant.setStatus(Entrant.STATUS_ENROLLED);
        assertEquals(Entrant.STATUS_ENROLLED, entrant.getStatus());
    }

    @Test
    public void statusTransition_invitedToDeclined() {
        entrant.setStatus(Entrant.STATUS_INVITED);
        entrant.setStatus(Entrant.STATUS_DECLINED);
        assertEquals(Entrant.STATUS_DECLINED, entrant.getStatus());
    }

    @Test
    public void emptyConstructor_createsEntrantWithNullFields() {
        Entrant empty = new Entrant();
        assertNull(empty.getDeviceId());
        assertNull(empty.getEventId());
        assertNull(empty.getStatus());
    }

    @Test
    public void allStatusConstants_areUnique() {
        assertNotEquals(Entrant.STATUS_WAITLIST,     Entrant.STATUS_INVITED);
        assertNotEquals(Entrant.STATUS_WAITLIST,     Entrant.STATUS_ENROLLED);
        assertNotEquals(Entrant.STATUS_WAITLIST,     Entrant.STATUS_DECLINED);
        assertNotEquals(Entrant.STATUS_WAITLIST,     Entrant.STATUS_CANCELLED);
        assertNotEquals(Entrant.STATUS_WAITLIST,     Entrant.STATUS_NOT_SELECTED);
        assertNotEquals(Entrant.STATUS_INVITED,      Entrant.STATUS_ENROLLED);
        assertNotEquals(Entrant.STATUS_INVITED,      Entrant.STATUS_DECLINED);
        assertNotEquals(Entrant.STATUS_ENROLLED,     Entrant.STATUS_CANCELLED);
        assertNotEquals(Entrant.STATUS_DECLINED,     Entrant.STATUS_NOT_SELECTED);
        assertNotEquals(Entrant.STATUS_CANCELLED,    Entrant.STATUS_NOT_SELECTED);
    }
}
