package com.example.eventlotteryapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.ui.admin.AdminBrowseEventsActivity;
import com.example.eventlotteryapp.ui.admin.AdminBrowseImagesActivity;
import com.example.eventlotteryapp.ui.admin.AdminBrowseOrganizersActivity;
import com.example.eventlotteryapp.ui.admin.AdminBrowseProfilesActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;

/**
 * UI tests for admin browse screens.
 *
 * Verifies that key layout elements are present on launch for each
 * admin browse activity. Firebase may be initialized but no network
 * calls need to succeed for these structural checks.
 */
@RunWith(AndroidJUnit4.class)
public class AdminBrowseActivitiesTest {

    // ── AdminBrowseProfilesActivity ──────────────────────────────────────────

    @Test
    public void browseProfiles_backButton_isDisplayed() {
        try (ActivityScenario<AdminBrowseProfilesActivity> s =
                     ActivityScenario.launch(AdminBrowseProfilesActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void browseProfiles_recyclerView_existsInLayout() {
        try (ActivityScenario<AdminBrowseProfilesActivity> s =
                     ActivityScenario.launch(AdminBrowseProfilesActivity.class)) {
            onView(withId(R.id.recyclerProfiles)).check(matches(anything()));
        }
    }

    // ── AdminBrowseEventsActivity ────────────────────────────────────────────

    @Test
    public void browseEvents_backButton_isDisplayed() {
        try (ActivityScenario<AdminBrowseEventsActivity> s =
                     ActivityScenario.launch(AdminBrowseEventsActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void browseEvents_recyclerView_existsInLayout() {
        try (ActivityScenario<AdminBrowseEventsActivity> s =
                     ActivityScenario.launch(AdminBrowseEventsActivity.class)) {
            onView(withId(R.id.recyclerEvents)).check(matches(anything()));
        }
    }

    // ── AdminBrowseImagesActivity ────────────────────────────────────────────

    @Test
    public void browseImages_backButton_isDisplayed() {
        try (ActivityScenario<AdminBrowseImagesActivity> s =
                     ActivityScenario.launch(AdminBrowseImagesActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void browseImages_recyclerView_existsInLayout() {
        try (ActivityScenario<AdminBrowseImagesActivity> s =
                     ActivityScenario.launch(AdminBrowseImagesActivity.class)) {
            onView(withId(R.id.recyclerImages)).check(matches(anything()));
        }
    }

    // ── AdminBrowseOrganizersActivity ────────────────────────────────────────

    @Test
    public void browseOrganizers_backButton_isDisplayed() {
        try (ActivityScenario<AdminBrowseOrganizersActivity> s =
                     ActivityScenario.launch(AdminBrowseOrganizersActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void browseOrganizers_recyclerView_existsInLayout() {
        try (ActivityScenario<AdminBrowseOrganizersActivity> s =
                     ActivityScenario.launch(AdminBrowseOrganizersActivity.class)) {
            onView(withId(R.id.recyclerOrganizers)).check(matches(anything()));
        }
    }
}
