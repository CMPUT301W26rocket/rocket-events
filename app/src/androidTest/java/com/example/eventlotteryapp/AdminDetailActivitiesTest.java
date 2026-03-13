package com.example.eventlotteryapp;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.ui.admin.AdminEventDetailActivity;
import com.example.eventlotteryapp.ui.admin.AdminImageDetailActivity;
import com.example.eventlotteryapp.ui.admin.AdminOrganizerDetailActivity;
import com.example.eventlotteryapp.ui.admin.AdminProfileDetailActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * UI tests for admin detail screens.
 *
 * Verifies that data passed via Intent extras is displayed correctly,
 * that action buttons are present, and that delete/remove actions
 * show a confirmation dialog. No network calls are required because
 * all displayed data comes from intent extras.
 */
@RunWith(AndroidJUnit4.class)
public class AdminDetailActivitiesTest {

    // ── AdminProfileDetailActivity ───────────────────────────────────────────

    private Intent profileIntent(String deviceId, String name, String email, String phone) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminProfileDetailActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("name", name);
        intent.putExtra("email", email);
        intent.putExtra("phone", phone);
        return intent;
    }

    @Test
    public void profileDetail_nameIsDisplayed() {
        try (ActivityScenario<AdminProfileDetailActivity> s =
                     ActivityScenario.launch(profileIntent("dev1", "Alice", "a@b.com", "555"))) {
            onView(withId(R.id.textName)).check(matches(withText("Alice")));
        }
    }

    @Test
    public void profileDetail_deleteButton_isDisplayed() {
        try (ActivityScenario<AdminProfileDetailActivity> s =
                     ActivityScenario.launch(profileIntent("dev1", "Alice", "a@b.com", "555"))) {
            onView(withId(R.id.btnDeleteProfile)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void profileDetail_deleteButton_showsConfirmationDialog() {
        try (ActivityScenario<AdminProfileDetailActivity> s =
                     ActivityScenario.launch(profileIntent("dev1", "Alice", "a@b.com", "555"))) {
            onView(withId(R.id.btnDeleteProfile)).perform(click());
            onView(withText("Delete Profile")).check(matches(isDisplayed()));
        }
    }

    // ── AdminEventDetailActivity ─────────────────────────────────────────────

    private Intent eventIntent(String eventId, String title, String description, String organizerId) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminEventDetailActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("title", title);
        intent.putExtra("description", description);
        intent.putExtra("organizerId", organizerId);
        return intent;
    }

    @Test
    public void eventDetail_titleIsDisplayed() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent("evt1", "Spring Fair", "A fun fair", "org1"))) {
            onView(withId(R.id.textEventTitle)).check(matches(withText("Spring Fair")));
        }
    }

    @Test
    public void eventDetail_deleteButton_isDisplayed() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent("evt1", "Spring Fair", "A fun fair", "org1"))) {
            onView(withId(R.id.btnDeleteEvent)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void eventDetail_deleteButton_showsConfirmationDialog() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent("evt1", "Spring Fair", "A fun fair", "org1"))) {
            onView(withId(R.id.btnDeleteEvent)).perform(click());
            onView(withText("Delete Event")).check(matches(isDisplayed()));
        }
    }

    // ── AdminImageDetailActivity ─────────────────────────────────────────────

    private Intent imageIntent(String eventId, String title, String posterUrl) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminImageDetailActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("title", title);
        intent.putExtra("posterUrl", posterUrl);
        return intent;
    }

    @Test
    public void imageDetail_titleIsDisplayed() {
        try (ActivityScenario<AdminImageDetailActivity> s =
                     ActivityScenario.launch(imageIntent("evt1", "Summer Fest", ""))) {
            onView(withId(R.id.textTitle)).check(matches(withText("Summer Fest")));
        }
    }

    @Test
    public void imageDetail_removeButton_isDisplayed() {
        try (ActivityScenario<AdminImageDetailActivity> s =
                     ActivityScenario.launch(imageIntent("evt1", "Summer Fest", ""))) {
            onView(withId(R.id.btnRemoveImage)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void imageDetail_removeButton_showsConfirmationDialog() {
        try (ActivityScenario<AdminImageDetailActivity> s =
                     ActivityScenario.launch(imageIntent("evt1", "Summer Fest", ""))) {
            onView(withId(R.id.btnRemoveImage)).perform(click());
            onView(withText("Remove Image")).check(matches(isDisplayed()));
        }
    }

    // ── AdminOrganizerDetailActivity ─────────────────────────────────────────

    private Intent organizerIntent(String deviceId, String name, String email, String phone) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminOrganizerDetailActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("name", name);
        intent.putExtra("email", email);
        intent.putExtra("phone", phone);
        return intent;
    }

    @Test
    public void organizerDetail_nameIsDisplayed() {
        try (ActivityScenario<AdminOrganizerDetailActivity> s =
                     ActivityScenario.launch(organizerIntent("org1", "Bob", "bob@c.com", "999"))) {
            onView(withId(R.id.textName)).check(matches(withText("Bob")));
        }
    }

    @Test
    public void organizerDetail_removeButton_isDisplayed() {
        try (ActivityScenario<AdminOrganizerDetailActivity> s =
                     ActivityScenario.launch(organizerIntent("org1", "Bob", "bob@c.com", "999"))) {
            onView(withId(R.id.btnRemoveOrganizer)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void organizerDetail_removeButton_showsConfirmationDialog() {
        try (ActivityScenario<AdminOrganizerDetailActivity> s =
                     ActivityScenario.launch(organizerIntent("org1", "Bob", "bob@c.com", "999"))) {
            onView(withId(R.id.btnRemoveOrganizer)).perform(click());
            onView(withText("Remove Organizer")).check(matches(isDisplayed()));
        }
    }
}
