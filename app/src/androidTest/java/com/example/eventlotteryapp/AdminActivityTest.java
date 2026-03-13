package com.example.eventlotteryapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.ui.admin.AdminActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * UI tests for {@link AdminActivity}.
 *
 * Verifies that all dashboard navigation buttons are visible on launch.
 * Firebase is initialized but no network calls are made (logout is not triggered).
 */
@RunWith(AndroidJUnit4.class)
public class AdminActivityTest {

    private ActivityScenario<AdminActivity> scenario;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(AdminActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
    }

    @Test
    public void browseEventsButton_isDisplayed() {
        onView(withId(R.id.btnBrowseEvents)).check(matches(isDisplayed()));
    }

    @Test
    public void browseProfilesButton_isDisplayed() {
        onView(withId(R.id.btnBrowseProfiles)).check(matches(isDisplayed()));
    }

    @Test
    public void browseImagesButton_isDisplayed() {
        onView(withId(R.id.btnBrowseImages)).check(matches(isDisplayed()));
    }

    @Test
    public void browseOrganizersButton_isDisplayed() {
        onView(withId(R.id.btnBrowseOrganizers)).check(matches(isDisplayed()));
    }

    @Test
    public void logoutButton_isDisplayed() {
        onView(withId(R.id.btnLogout)).check(matches(isDisplayed()));
    }
}
