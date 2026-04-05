package com.example.eventlotteryapp;

import com.example.eventlotteryapp.models.Entrant;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link Entrant} model class.
 * Covers all status constants, getters/setters, status transitions,
 * and the latitude/longitude location fields added for the maps feature.
 * @author Leyla
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

    // --- Default constructor ---

    @Test
    public void emptyConstructor_createsEntrantWithNullFields() {
        Entrant empty = new Entrant();
        assertNull(empty.getDeviceId());
        assertNull(empty.getEventId());
        assertNull(empty.getStatus());
        assertNull(empty.getJoinedAt());
        assertNull(empty.getStatusUpdatedAt());
        assertNull(empty.getLatitude());
        assertNull(empty.getLongitude());
    }

    // --- Getters/Setters ---

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

    // --- Location fields ---

    @Test
    public void latitudeLongitude_nullByDefault() {
        assertNull(entrant.getLatitude());
        assertNull(entrant.getLongitude());
    }

    @Test
    public void setLatitude_updatesLatitude() {
        entrant.setLatitude(53.5461);
        assertEquals(53.5461, entrant.getLatitude(), 0.0001);
    }

    @Test
    public void setLongitude_updatesLongitude() {
        entrant.setLongitude(-113.4938);
        assertEquals(-113.4938, entrant.getLongitude(), 0.0001);
    }

    @Test
    public void setLatitudeLongitude_canBeResetToNull() {
        entrant.setLatitude(10.0);
        entrant.setLongitude(20.0);
        entrant.setLatitude(null);
        entrant.setLongitude(null);
        assertNull(entrant.getLatitude());
        assertNull(entrant.getLongitude());
    }

    // --- Core status constants ---

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

    // --- New status constants ---

    @Test
    public void statusConstant_waitlistInvited_isCorrect() {
        assertEquals("waitlist_invited", Entrant.STATUS_WAITLIST_INVITED);
    }

    @Test
    public void statusConstant_declinedWaitlist_isCorrect() {
        assertEquals("declined_waitlist", Entrant.STATUS_DECLINED_WAITLIST);
    }

    @Test
    public void statusConstant_coOrganizer_isCorrect() {
        assertEquals("co_organizer", Entrant.STATUS_CO_ORGANIZER);
    }

    // --- Status transitions (reflect real app flows) ---

    @Test
    public void statusTransition_waitlistToInvited() {
        // Lottery selects the entrant
        entrant.setStatus(Entrant.STATUS_INVITED);
        assertEquals(Entrant.STATUS_INVITED, entrant.getStatus());
    }

    @Test
    public void statusTransition_invitedToEnrolled() {
        // Entrant accepts their lottery invitation
        entrant.setStatus(Entrant.STATUS_INVITED);
        entrant.setStatus(Entrant.STATUS_ENROLLED);
        assertEquals(Entrant.STATUS_ENROLLED, entrant.getStatus());
    }

    @Test
    public void statusTransition_invitedToDeclined() {
        // Entrant declines their lottery invitation
        entrant.setStatus(Entrant.STATUS_INVITED);
        entrant.setStatus(Entrant.STATUS_DECLINED);
        assertEquals(Entrant.STATUS_DECLINED, entrant.getStatus());
    }

    @Test
    public void statusTransition_waitlistInvitedToWaitlist() {
        // Entrant accepts a private event waitlist invitation
        entrant.setStatus(Entrant.STATUS_WAITLIST_INVITED);
        entrant.setStatus(Entrant.STATUS_WAITLIST);
        assertEquals(Entrant.STATUS_WAITLIST, entrant.getStatus());
    }

    @Test
    public void statusTransition_waitlistInvitedToDeclinedWaitlist() {
        // Entrant declines a private event waitlist invitation
        entrant.setStatus(Entrant.STATUS_WAITLIST_INVITED);
        entrant.setStatus(Entrant.STATUS_DECLINED_WAITLIST);
        assertEquals(Entrant.STATUS_DECLINED_WAITLIST, entrant.getStatus());
    }

    // --- All constants are unique ---

    @Test
    public void allStatusConstants_areUnique() {
        String[] statuses = {
                Entrant.STATUS_WAITLIST,
                Entrant.STATUS_INVITED,
                Entrant.STATUS_ENROLLED,
                Entrant.STATUS_DECLINED,
                Entrant.STATUS_CANCELLED,
                Entrant.STATUS_NOT_SELECTED,
                Entrant.STATUS_WAITLIST_INVITED,
                Entrant.STATUS_DECLINED_WAITLIST,
                Entrant.STATUS_CO_ORGANIZER
        };
        for (int i = 0; i < statuses.length; i++) {
            for (int j = i + 1; j < statuses.length; j++) {
                assertNotEquals("Duplicate status: " + statuses[i], statuses[i], statuses[j]);
            }
        }
    }
}
