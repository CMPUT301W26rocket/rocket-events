package com.example.eventlotteryapp;

import android.app.Activity;
import android.app.Instrumentation;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.example.eventlotteryapp.ui.admin.AdminBrowseEventsActivity;
import com.example.eventlotteryapp.ui.admin.AdminBrowseImagesActivity;
import com.example.eventlotteryapp.ui.admin.AdminBrowseOrganizersActivity;
import com.example.eventlotteryapp.ui.admin.AdminBrowseProfilesActivity;
import com.example.eventlotteryapp.ui.admin.AdminNotificationLogsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

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

    // -------------------------------------------------------------------------
    // Back button behaviour
    // -------------------------------------------------------------------------

    /**
     * Clicking the back button must finish AdminActivity (return to the previous screen).
     */
    @Test
    public void backButton_click_finishesActivity() {
        onView(withId(R.id.button_back_admin)).perform(click());

        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }

    // -------------------------------------------------------------------------
    // Card click navigation
    // -------------------------------------------------------------------------

    @Test
    public void eventsCard_click_opensAdminBrowseEventsActivity() {
        Intents.init();
        try {
            intending(hasComponent(AdminBrowseEventsActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.card_view_events)).perform(click());

            intended(hasComponent(AdminBrowseEventsActivity.class.getName()));
        } finally {
            Intents.release();
        }
    }

    @Test
    public void profilesCard_click_opensAdminBrowseProfilesActivity() {
        Intents.init();
        try {
            intending(hasComponent(AdminBrowseProfilesActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.card_view_profiles)).perform(click());

            intended(hasComponent(AdminBrowseProfilesActivity.class.getName()));
        } finally {
            Intents.release();
        }
    }

    @Test
    public void organizersCard_click_opensAdminBrowseOrganizersActivity() {
        Intents.init();
        try {
            intending(hasComponent(AdminBrowseOrganizersActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.card_view_organizers)).perform(click());

            intended(hasComponent(AdminBrowseOrganizersActivity.class.getName()));
        } finally {
            Intents.release();
        }
    }

    @Test
    public void imagesCard_click_opensAdminBrowseImagesActivity() {
        Intents.init();
        try {
            intending(hasComponent(AdminBrowseImagesActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.card_view_images)).perform(click());

            intended(hasComponent(AdminBrowseImagesActivity.class.getName()));
        } finally {
            Intents.release();
        }
    }

    @Test
    public void notificationLogsCard_click_opensAdminNotificationLogsActivity() {
        Intents.init();
        try {
            intending(hasComponent(AdminNotificationLogsActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.card_view_logs)).perform(scrollTo(), click());

            intended(hasComponent(AdminNotificationLogsActivity.class.getName()));
        } finally {
            Intents.release();
        }
    }
}
