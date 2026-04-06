package com.example.eventlotteryapp;

import com.example.eventlotteryapp.models.Notification;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link Notification} model class.
 * Covers type constants, getters/setters, and default field values.
 * Note: constructors that call {@code Timestamp.now()} are not tested here
 * since Firebase is unavailable in unit tests — the no-arg constructor
 * and setters are used instead.
 * @author Leyla
 */
public class NotificationTest {

    private Notification notification;

    @Before
    public void setUp() {
        notification = new Notification();
        notification.setNotificationId("notif001");
        notification.setEventId("event123");
        notification.setEventTitle("Spring Gala");
        notification.setType(Notification.TYPE_WON);
        notification.setMessage("Congratulations, you have been selected!");
        notification.setSenderOrganizerId("organizer99");
        notification.setSenderOrganizerName("Alice");
        notification.setRecipientDeviceId("device42");
        notification.setRecipientName("Bob");
    }

    // --- Default constructor ---

    @Test
    public void emptyConstructor_allFieldsDefaulted() {
        // A freshly created notification should have all fields null/false
        Notification empty = new Notification();

        assertNull(empty.getNotificationId());
        assertNull(empty.getEventId());
        assertNull(empty.getEventTitle());
        assertNull(empty.getType());
        assertNull(empty.getMessage());
        assertNull(empty.getCreatedAt());
        assertNull(empty.getSenderOrganizerId());
        assertNull(empty.getSenderOrganizerName());
        assertNull(empty.getRecipientDeviceId());
        assertNull(empty.getRecipientName());
        assertFalse(empty.isRead());
    }

    // --- Type constants ---

    @Test
    public void typeConstant_won_isCorrect() {
        // Sent when an entrant is selected in the lottery
        assertEquals("won", Notification.TYPE_WON);
    }

    @Test
    public void typeConstant_lost_isCorrect() {
        // Sent when an entrant is not selected in the lottery
        assertEquals("lost", Notification.TYPE_LOST);
    }

    @Test
    public void typeConstant_replacement_isCorrect() {
        // Sent when a replacement spot opens up and the entrant is chosen
        assertEquals("replacement", Notification.TYPE_REPLACEMENT);
    }

    @Test
    public void typeConstant_general_isCorrect() {
        // Used for organizer broadcast messages not tied to lottery outcome
        assertEquals("general", Notification.TYPE_GENERAL);
    }

    @Test
    public void typeConstant_waitlistInvite_isCorrect() {
        // Sent to invite a user to join a private event's waitlist
        assertEquals("waitlist_invite", Notification.TYPE_WAITLIST_INVITE);
    }

    @Test
    public void typeConstant_coOrganizer_isCorrect() {
        // Sent when a user is assigned as a co-organizer for an event
        assertEquals("co_organizer", Notification.TYPE_CO_ORGANIZER);
    }

    @Test
    public void allTypeConstants_areDistinct() {
        // No two type constants should share the same string value
        String[] types = {
                Notification.TYPE_WON,
                Notification.TYPE_LOST,
                Notification.TYPE_REPLACEMENT,
                Notification.TYPE_GENERAL,
                Notification.TYPE_WAITLIST_INVITE,
                Notification.TYPE_CO_ORGANIZER
        };
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals("Duplicate type constant: " + types[i], types[i], types[j]);
            }
        }
    }

    // --- Basic setters ---

    @Test
    public void setNotificationId_updatesNotificationId() {
        notification.setNotificationId("notif999");
        assertEquals("notif999", notification.getNotificationId());
    }

    @Test
    public void setEventId_updatesEventId() {
        notification.setEventId("event456");
        assertEquals("event456", notification.getEventId());
    }

    @Test
    public void setEventTitle_updatesEventTitle() {
        notification.setEventTitle("Winter Formal");
        assertEquals("Winter Formal", notification.getEventTitle());
    }

    @Test
    public void setType_updatesType() {
        notification.setType(Notification.TYPE_GENERAL);
        assertEquals(Notification.TYPE_GENERAL, notification.getType());
    }

    @Test
    public void setMessage_updatesMessage() {
        notification.setMessage("The event has been cancelled.");
        assertEquals("The event has been cancelled.", notification.getMessage());
    }

    // --- Read status ---

    @Test
    public void isRead_defaultsFalse() {
        // Notifications start unread — user must explicitly open/dismiss them
        Notification fresh = new Notification();
        assertFalse(fresh.isRead());
    }

    @Test
    public void setRead_marksNotificationAsRead() {
        notification.setRead(true);
        assertTrue(notification.isRead());
    }

    @Test
    public void setRead_canBeToggledBackToUnread() {
        notification.setRead(true);
        notification.setRead(false);
        assertFalse(notification.isRead());
    }

    // --- Sender / recipient fields ---

    @Test
    public void setSenderOrganizerId_updatesSenderId() {
        notification.setSenderOrganizerId("organizer77");
        assertEquals("organizer77", notification.getSenderOrganizerId());
    }

    @Test
    public void setSenderOrganizerName_updatesSenderName() {
        notification.setSenderOrganizerName("Carol");
        assertEquals("Carol", notification.getSenderOrganizerName());
    }

    @Test
    public void setRecipientDeviceId_updatesRecipientId() {
        notification.setRecipientDeviceId("device88");
        assertEquals("device88", notification.getRecipientDeviceId());
    }

    @Test
    public void setRecipientName_updatesRecipientName() {
        notification.setRecipientName("Dave");
        assertEquals("Dave", notification.getRecipientName());
    }
}
