package com.example.eventlotteryapp;

import com.example.eventlotteryapp.models.User;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link User} model class.
 * @author Leyla
 */
public class UserTest {

    private User user;

    @Before
    public void setUp() {
        user = new User("device123", "Jane Smith", "jane@email.com", "780-555-0123");
    }

    @Test
    public void constructor_setsAllFields() {
        assertEquals("device123", user.getDeviceId());
        assertEquals("Jane Smith", user.getName());
        assertEquals("jane@email.com", user.getEmail());
        assertEquals("780-555-0123", user.getPhone());
    }

    @Test
    public void constructor_enablesNotificationsByDefault() {
        assertTrue(user.isNotificationsEnabled());
    }

    @Test
    public void emptyConstructor_createsUserWithNullFields() {
        User empty = new User();
        assertNull(empty.getDeviceId());
        assertNull(empty.getName());
        assertNull(empty.getEmail());
        assertNull(empty.getPhone());
    }

    @Test
    public void setName_updatesName() {
        user.setName("John Doe");
        assertEquals("John Doe", user.getName());
    }

    @Test
    public void setEmail_updatesEmail() {
        user.setEmail("john@email.com");
        assertEquals("john@email.com", user.getEmail());
    }

    @Test
    public void setPhone_updatesPhone() {
        user.setPhone("587-000-1234");
        assertEquals("587-000-1234", user.getPhone());
    }

    @Test
    public void setDeviceId_updatesDeviceId() {
        user.setDeviceId("newDevice456");
        assertEquals("newDevice456", user.getDeviceId());
    }

    @Test
    public void setNotificationsEnabled_updatesFlag() {
        user.setNotificationsEnabled(false);
        assertFalse(user.isNotificationsEnabled());

        user.setNotificationsEnabled(true);
        assertTrue(user.isNotificationsEnabled());
    }
}
