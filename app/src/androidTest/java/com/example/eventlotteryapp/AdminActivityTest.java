package com.example.eventlotteryapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.ui.admin.AdminActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * UI tests for {@link AdminActivity}.
 *
 * Verifies that all dashboard cards, the back button, and the logout link
 * are present on launch. Firebase is initialised but no network calls are
 * made (logout is not triggered).
 *
 * @author Leyla
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

    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    @Test
    public void backButton_isDisplayed() {
        onView(withId(R.id.button_back_admin)).check(matches(isDisplayed()));
    }

    @Test
    public void title_isDisplayed() {
        onView(withText("Admin View")).check(matches(isDisplayed()));
    }

    // -------------------------------------------------------------------------
    // Navigation cards
    // -------------------------------------------------------------------------

    @Test
    public void eventsCard_isDisplayed() {
        onView(withId(R.id.card_view_events)).check(matches(isDisplayed()));
    }

    @Test
    public void profilesCard_isDisplayed() {
        onView(withId(R.id.card_view_profiles)).check(matches(isDisplayed()));
    }

    @Test
    public void organizersCard_isDisplayed() {
        onView(withId(R.id.card_view_organizers)).check(matches(isDisplayed()));
    }

    @Test
    public void imagesCard_isDisplayed() {
        onView(withId(R.id.card_view_images)).check(matches(isDisplayed()));
    }

    @Test
    public void notificationLogsCard_isDisplayed() {
        onView(withId(R.id.card_view_logs)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    // -------------------------------------------------------------------------
    // Card label text
    // -------------------------------------------------------------------------

    @Test
    public void eventsCard_showsCorrectLabel() {
        onView(withText("View All Events")).check(matches(isDisplayed()));
    }

    @Test
    public void profilesCard_showsCorrectLabel() {
        onView(withText("View All Profiles")).check(matches(isDisplayed()));
    }

    @Test
    public void organizersCard_showsCorrectLabel() {
        onView(withText("View All Organizers")).check(matches(isDisplayed()));
    }

    @Test
    public void imagesCard_showsCorrectLabel() {
        onView(withText("View All Images")).check(matches(isDisplayed()));
    }

    @Test
    public void notificationLogsCard_showsCorrectLabel() {
        onView(withText("View All Notification Logs")).perform(scrollTo()).check(matches(isDisplayed()));
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Test
    public void logoutText_isDisplayed() {
        onView(withId(R.id.text_logout)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void logoutText_showsCorrectLabel() {
        onView(withText("Logout")).perform(scrollTo()).check(matches(isDisplayed()));
    }
}
