package com.example.eventlotteryapp;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.ui.admin.AdminEventDetailActivity;
import com.example.eventlotteryapp.ui.admin.AdminImageDetailActivity;
import com.example.eventlotteryapp.ui.admin.AdminNotificationLogsActivity;
import com.example.eventlotteryapp.ui.admin.AdminOrganizerDetailActivity;
import com.example.eventlotteryapp.ui.admin.AdminProfileDetailActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;

/**
 * UI and behaviour tests for admin detail screens.
 *
 * <p>Data is supplied via Intent extras so no Firestore reads are needed
 * for the initial display assertions. Behaviour tests (dialog open/cancel)
 * verify that confirmation dialogs appear and that cancelling leaves the
 * activity alive.
 *
 * <p>Note on {@link AdminNotificationLogsActivity} detail dialog: the detail
 * view is an AlertDialog triggered by tapping a list item that is populated
 * from Firestore. Without a Firestore emulator or mock, the list is empty at
 * test time and the dialog cannot be triggered. Structural tests for that
 * screen are covered in {@code AdminBrowseActivitiesTest}.
 *
 * @author Leyla
 */
@RunWith(AndroidJUnit4.class)
public class AdminDetailActivitiesTest {

    // =========================================================================
    // AdminProfileDetailActivity
    // =========================================================================

    private Intent profileIntent(String deviceId, String name, String email, String phone) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminProfileDetailActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("name",     name);
        intent.putExtra("email",    email);
        intent.putExtra("phone",    phone);
        return intent;
    }

    @Test
    public void profileDetail_backButton_isDisplayed() {
        try (ActivityScenario<AdminProfileDetailActivity> s =
                     ActivityScenario.launch(profileIntent("dev1", "Alice", "a@b.com", "555"))) {
            onView(withId(R.id.button_back)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void profileDetail_nameIsDisplayed() {
        try (ActivityScenario<AdminProfileDetailActivity> s =
                     ActivityScenario.launch(profileIntent("dev1", "Alice", "a@b.com", "555"))) {
            onView(withId(R.id.textName)).check(matches(withText("Alice")));
        }
    }

    @Test
    public void profileDetail_emailIsDisplayed() {
        try (ActivityScenario<AdminProfileDetailActivity> s =
                     ActivityScenario.launch(profileIntent("dev1", "Alice", "a@b.com", "555"))) {
            onView(withId(R.id.textEmail)).check(matches(withText("a@b.com")));
        }
    }

    @Test
    public void profileDetail_phoneIsDisplayed() {
        try (ActivityScenario<AdminProfileDetailActivity> s =
                     ActivityScenario.launch(profileIntent("dev1", "Alice", "a@b.com", "555"))) {
            onView(withId(R.id.textPhone)).check(matches(withText("555")));
        }
    }

    @Test
    public void profileDetail_deviceIdIsDisplayed() {
        try (ActivityScenario<AdminProfileDetailActivity> s =
                     ActivityScenario.launch(profileIntent("dev1", "Alice", "a@b.com", "555"))) {
            onView(withId(R.id.textDeviceId)).check(matches(withText("dev1")));
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
            onView(withText("Delete Profile")).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void profileDetail_deleteDialog_cancelKeepsActivity() {
        try (ActivityScenario<AdminProfileDetailActivity> s =
                     ActivityScenario.launch(profileIntent("dev1", "Alice", "a@b.com", "555"))) {
            onView(withId(R.id.btnDeleteProfile)).perform(click());
            onView(withText("Cancel")).inRoot(isDialog()).perform(click());
            onView(withId(R.id.btnDeleteProfile)).check(matches(isDisplayed()));
        }
    }

    // =========================================================================
    // AdminEventDetailActivity
    // =========================================================================

    private Intent eventIntent(String eventId, String title, String description,
                               String organizerId, String organizerName) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminEventDetailActivity.class);
        intent.putExtra("eventId",       eventId);
        intent.putExtra("title",         title);
        intent.putExtra("description",   description);
        intent.putExtra("organizerId",   organizerId);
        intent.putExtra("organizerName", organizerName);
        return intent;
    }

    @Test
    public void eventDetail_backButton_isDisplayed() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.button_back)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void eventDetail_titleIsDisplayed() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.text_detail_title)).check(matches(withText("Spring Fair")));
        }
    }

    @Test
    public void eventDetail_organizerNameIsDisplayed() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            // Format: "Organizer: <name> (ID: <id>)"
            onView(withId(R.id.text_detail_organizer_name))
                    .check(matches(withText(containsString("Org Corp"))));
        }
    }

    @Test
    public void eventDetail_organizerNameIncludesId() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.text_detail_organizer_name))
                    .check(matches(withText(containsString("org1"))));
        }
    }

    @Test
    public void eventDetail_descriptionIsDisplayed() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.text_detail_description))
                    .check(matches(withText("A fun fair")));
        }
    }

    @Test
    public void eventDetail_eventIdIsDisplayed() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.text_detail_event_id)).check(matches(withText("evt1")));
        }
    }

    @Test
    public void eventDetail_commentsHeader_isDisplayed() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withText("Comments")).check(matches(isDisplayed()));
        }
    }

    /** Comments placeholder exists in the layout regardless of load state. */
    @Test
    public void eventDetail_commentsPlaceholder_existsInLayout() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.text_no_comments)).check(matches(anything()));
        }
    }

    @Test
    public void eventDetail_deleteButton_isDisplayed() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.btnDeleteEvent)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void eventDetail_deleteButton_showsConfirmationDialog() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.btnDeleteEvent)).perform(click());
            onView(withText("Delete Event")).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void eventDetail_deleteDialog_hasDeleteConfirmButton() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.btnDeleteEvent)).perform(click());
            onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void eventDetail_deleteDialog_cancelKeepsActivity() {
        try (ActivityScenario<AdminEventDetailActivity> s =
                     ActivityScenario.launch(eventIntent(
                             "evt1", "Spring Fair", "A fun fair", "org1", "Org Corp"))) {
            onView(withId(R.id.btnDeleteEvent)).perform(click());
            onView(withText("Cancel")).inRoot(isDialog()).perform(click());
            onView(withId(R.id.btnDeleteEvent)).check(matches(isDisplayed()));
        }
    }

    // =========================================================================
    // AdminImageDetailActivity
    // =========================================================================

    private Intent imageIntent(String eventId, String title, String posterUrl) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminImageDetailActivity.class);
        intent.putExtra("eventId",   eventId);
        intent.putExtra("title",     title);
        intent.putExtra("posterUrl", posterUrl);
        return intent;
    }

    @Test
    public void imageDetail_backButton_isDisplayed() {
        try (ActivityScenario<AdminImageDetailActivity> s =
                     ActivityScenario.launch(imageIntent("evt1", "Summer Fest", ""))) {
            onView(withId(R.id.button_back)).check(matches(isDisplayed()));
        }
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
            onView(withText("Remove Image")).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void imageDetail_removeDialog_cancelKeepsActivity() {
        try (ActivityScenario<AdminImageDetailActivity> s =
                     ActivityScenario.launch(imageIntent("evt1", "Summer Fest", ""))) {
            onView(withId(R.id.btnRemoveImage)).perform(click());
            onView(withText("Cancel")).inRoot(isDialog()).perform(click());
            onView(withId(R.id.btnRemoveImage)).check(matches(isDisplayed()));
        }
    }

    // =========================================================================
    // AdminOrganizerDetailActivity
    // =========================================================================

    private Intent organizerIntent(String deviceId, String name, String email, String phone) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AdminOrganizerDetailActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("name",     name);
        intent.putExtra("email",    email);
        intent.putExtra("phone",    phone);
        return intent;
    }

    @Test
    public void organizerDetail_backButton_isDisplayed() {
        try (ActivityScenario<AdminOrganizerDetailActivity> s =
                     ActivityScenario.launch(organizerIntent("org1", "Bob", "bob@c.com", "999"))) {
            onView(withId(R.id.button_back)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void organizerDetail_nameIsDisplayed() {
        try (ActivityScenario<AdminOrganizerDetailActivity> s =
                     ActivityScenario.launch(organizerIntent("org1", "Bob", "bob@c.com", "999"))) {
            onView(withId(R.id.textName)).check(matches(withText("Bob")));
        }
    }

    @Test
    public void organizerDetail_emailIsDisplayed() {
        try (ActivityScenario<AdminOrganizerDetailActivity> s =
                     ActivityScenario.launch(organizerIntent("org1", "Bob", "bob@c.com", "999"))) {
            onView(withId(R.id.textEmail)).check(matches(withText("bob@c.com")));
        }
    }

    @Test
    public void organizerDetail_phoneIsDisplayed() {
        try (ActivityScenario<AdminOrganizerDetailActivity> s =
                     ActivityScenario.launch(organizerIntent("org1", "Bob", "bob@c.com", "999"))) {
            onView(withId(R.id.textPhone)).check(matches(withText("999")));
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
            onView(withText("Remove Organizer")).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    /** The dialog must warn that all of the organizer's events will be deleted. */
    @Test
    public void organizerDetail_removeDialog_warnsAboutDeletingEvents() {
        try (ActivityScenario<AdminOrganizerDetailActivity> s =
                     ActivityScenario.launch(organizerIntent("org1", "Bob", "bob@c.com", "999"))) {
            onView(withId(R.id.btnRemoveOrganizer)).perform(click());
            onView(withText(containsString("delete all events")))
                    .inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void organizerDetail_removeDialog_cancelKeepsActivity() {
        try (ActivityScenario<AdminOrganizerDetailActivity> s =
                     ActivityScenario.launch(organizerIntent("org1", "Bob", "bob@c.com", "999"))) {
            onView(withId(R.id.btnRemoveOrganizer)).perform(click());
            onView(withText("Cancel")).inRoot(isDialog()).perform(click());
            onView(withId(R.id.btnRemoveOrganizer)).check(matches(isDisplayed()));
        }
    }

    // Note: AdminNotificationLogsActivity structural tests (back button, title,
    // RecyclerView) are in AdminBrowseActivitiesTest. The per-item detail view
    // is an AlertDialog triggered by tapping a Firestore-populated list row and
    // cannot be tested here without a Firestore emulator or mock.
}
