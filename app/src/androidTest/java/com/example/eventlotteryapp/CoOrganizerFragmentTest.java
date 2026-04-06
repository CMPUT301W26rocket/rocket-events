package com.example.eventlotteryapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.Notification;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.NotificationRepository;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.fragments.CoOrganizerFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UI tests for {@link CoOrganizerFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: search bar and button are visible on launch</li>
 *   <li>Validation: empty query does not call the repository</li>
 *   <li>Search: results are displayed with name and email</li>
 *   <li>Search: empty results show the "No users found" status message</li>
 *   <li>Assign button state: shows "Assigned" (disabled) for existing co-organizers</li>
 *   <li>Assign button state: shows "Assign" (enabled) for users with any other status</li>
 *   <li>Assign flow: success disables button and changes label to "Assigned"</li>
 *   <li>Assign flow: failure re-enables button and restores "Assign" label</li>
 *   <li>Assign guard: getEntrant returning CO_ORGANIZER status blocks proceedWithAssignment</li>
 *   <li>Correctness: assignCoOrganizer is called with the correct event and device IDs</li>
 *   <li>Filtering: the organizer is excluded from search results</li>
 * </ul>
 *
 * @author Leyla
 */
@RunWith(AndroidJUnit4.class)
public class CoOrganizerFragmentTest {

    private static final String EVENT_ID      = "event123";
    private static final String EVENT_TITLE   = "Spring Fair";
    private static final String ORGANIZER_ID  = "organizer456";

    @Mock UserRepository         mockUserRepo;
    @Mock EntrantRepository      mockEntrantRepo;
    @Mock NotificationRepository mockNotifRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private User makeUser(String deviceId, String name, String email, String phone) {
        User u = new User();
        u.setDeviceId(deviceId);
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        return u;
    }

    private Entrant makeEntrant(String deviceId, String status) {
        Entrant e = new Entrant();
        e.setDeviceId(deviceId);
        e.setStatus(status);
        return e;
    }

    /**
     * Launches CoOrganizerFragment with injected mocks.
     * {@code existingEntrants} is what {@code getAllEntrantsForEvent} will return on load,
     * pre-populating the status cache used to render Assign / Assigned buttons.
     */
    private void launch(List<Entrant> existingEntrants) {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(1);
            cb.onSuccess(existingEntrants);
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(any(), any());

        Bundle args = new Bundle();
        args.putString("eventId",          EVENT_ID);
        args.putString("eventTitle",       EVENT_TITLE);
        args.putString("organizerDeviceId", ORGANIZER_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                CoOrganizerFragment fragment = new CoOrganizerFragment();
                fragment.setUserRepository(mockUserRepo);
                fragment.setEntrantRepository(mockEntrantRepo);
                fragment.setNotificationRepository(mockNotifRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                CoOrganizerFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /** Convenience overload — no existing entrants. */
    private void launch() {
        launch(new ArrayList<>());
    }

    /** Stubs searchUsers to return the given list on any query. */
    private void stubSearch(List<User> results) {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<List<User>> cb = invocation.getArgument(1);
            cb.onSuccess(results);
            return null;
        }).when(mockUserRepo).searchUsers(any(), any());
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    /**
     * Search bar, search button, and back button must all be visible on launch.
     */
    @Test
    public void searchBar_searchButton_backButton_areDisplayed() {
        launch();

        onView(withId(R.id.edit_search)).check(matches(isDisplayed()));
        onView(withId(R.id.button_search)).check(matches(isDisplayed()));
        onView(withId(R.id.button_back)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Validation tests
    // -----------------------------------------------------------------------

    /**
     * Clicking Search with an empty query must NOT call the repository.
     */
    @Test
    public void search_withEmptyQuery_doesNotCallRepository() {
        launch();

        onView(withId(R.id.button_search)).perform(click());

        verify(mockUserRepo, never()).searchUsers(any(), any());
    }

    // -----------------------------------------------------------------------
    // Search result display tests
    // -----------------------------------------------------------------------

    /**
     * When the repository returns a user, their name and email must appear in the list.
     */
    @Test
    public void search_returnsResults_displaysNameAndEmail() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(withText("Alice Smith")).check(matches(isDisplayed()));
        onView(withText("alice@email.com")).check(matches(isDisplayed()));
    }

    /**
     * When the repository returns no users the status text must become visible
     * and contain "No users found".
     */
    @Test
    public void search_withNoResults_showsNoUsersFoundMessage() {
        stubSearch(new ArrayList<>());
        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("unknown"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(withId(R.id.text_status)).check(matches(isDisplayed()));
        onView(withId(R.id.text_status)).check(matches(withText(containsString("No users found"))));
    }

    /**
     * The organizer of the event must not appear in the search results, even if the
     * repository returns them in the list.
     */
    @Test
    public void organizerIsExcluded_fromSearchResults() {
        stubSearch(Arrays.asList(
                makeUser(ORGANIZER_ID, "Organizer Person", "org@email.com", ""),
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("test"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(withText("Organizer Person")).check(doesNotExist());
        onView(withText("Alice Smith")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Assign button state tests
    // -----------------------------------------------------------------------

    /**
     * A user who is already a co-organizer (status cached from loadExistingEntrants)
     * must have a disabled "Assigned" button — not an enabled "Assign" button.
     */
    @Test
    public void existingCoOrganizer_showsAssignedButton_disabled() {
        List<Entrant> existing = Collections.singletonList(
                makeEntrant("user1", Entrant.STATUS_CO_ORGANIZER));
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch(existing);

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("Assigned")))
                .check(matches(not(isEnabled())));
    }

    /**
     * A user who is on the waitlist (not yet a co-organizer) must have an enabled
     * "Assign" button — they can still be assigned regardless of their current status.
     */
    @Test
    public void userWithWaitlistStatus_showsAssignButton_enabled() {
        List<Entrant> existing = Collections.singletonList(
                makeEntrant("user1", Entrant.STATUS_WAITLIST));
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch(existing);

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("Assign")))
                .check(matches(isEnabled()));
    }

    /**
     * A user with invited status (not yet a co-organizer) must also show "Assign" enabled.
     */
    @Test
    public void userWithInvitedStatus_showsAssignButton_enabled() {
        List<Entrant> existing = Collections.singletonList(
                makeEntrant("user1", Entrant.STATUS_INVITED));
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch(existing);

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("Assign")))
                .check(matches(isEnabled()));
    }

    // -----------------------------------------------------------------------
    // Assign flow tests
    // -----------------------------------------------------------------------

    /**
     * After a successful assignment the button must be disabled and show "Assigned".
     */
    @Test
    public void assignButton_onSuccess_becomesAssigned() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        // getEntrant returns null — user is not yet an entrant at all
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).assignCoOrganizer(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.button_invite), withText("Assigned")))
                .check(matches(not(isEnabled())));
    }

    /**
     * After a failed assignment the button must be re-enabled and show "Assign" again.
     */
    @Test
    public void assignButton_onFailure_restoresAssignButton() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onFailure(new Exception("Firestore error"));
            return null;
        }).when(mockEntrantRepo).assignCoOrganizer(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("Assign")))
                .check(matches(isEnabled()));
    }

    /**
     * If getEntrant reveals the user is already a co-organizer at click time,
     * proceedWithAssignment must NOT be called and the button must show "Assigned".
     */
    @Test
    public void assignButton_whenGetEntrantReturnsCoOrganizer_doesNotProceed() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        // getEntrant confirms co-organizer status at click time
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(makeEntrant("user1", Entrant.STATUS_CO_ORGANIZER));
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        // assignCoOrganizer must not have been called
        verify(mockEntrantRepo, never()).assignCoOrganizer(any(), any(), any());
        // Button must show Assigned and be disabled
        onView(allOf(withId(R.id.button_invite), withText("Assigned")))
                .check(matches(not(isEnabled())));
    }

    // -----------------------------------------------------------------------
    // Correctness tests
    // -----------------------------------------------------------------------

    /**
     * Full assign flow for a user who is already on the waitlist:
     * clicking Assign must call the repository AND update the button to "Assigned".
     * This confirms that non-co-organizer entrant statuses do not block assignment.
     */
    @Test
    public void assignButton_forWaitlistUser_assignsAsCoOrganizer() {
        // User is already on the waitlist for this event
        List<Entrant> existing = Collections.singletonList(
                makeEntrant("user1", Entrant.STATUS_WAITLIST));
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        // getEntrant returns the waitlist entrant — not a co-organizer, so proceed
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(makeEntrant("user1", Entrant.STATUS_WAITLIST));
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).assignCoOrganizer(any(), any(), any());

        launch(existing);

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        // Button starts as "Assign" since waitlist ≠ co-organizer
        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("Assign")))
                .check(matches(isEnabled()));

        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        // Repository must have been called
        verify(mockEntrantRepo).assignCoOrganizer(eq(EVENT_ID), eq("user1"), any());

        // Button must now reflect the assigned state
        onView(allOf(withId(R.id.button_invite), withText("Assigned")))
                .check(matches(not(isEnabled())));
    }

    /**
     * assignCoOrganizer must be called with the correct eventId and the assigned user's deviceId.
     */
    @Test
    public void assignButton_callsRepository_withCorrectIds() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).assignCoOrganizer(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        verify(mockEntrantRepo).assignCoOrganizer(eq(EVENT_ID), eq("user1"), any());
    }

    // -----------------------------------------------------------------------
    // Notification tests
    // -----------------------------------------------------------------------

    /**
     * A successful assignment must send a co-organizer notification to the assigned user.
     * Verifies that the notification is sent to the correct device ID and that its type
     * is {@link Notification#TYPE_CO_ORGANIZER}.
     */
    @Test
    public void successfulAssign_sendsCoOrganizerNotification_toAssignedUser() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).assignCoOrganizer(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        // Capture the Notification object passed to addNotification
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotifRepo).addNotification(eq("user1"), notifCaptor.capture(), any());

        // Must be a co-organizer notification, not a general or any other type
        assertEquals(Notification.TYPE_CO_ORGANIZER, notifCaptor.getValue().getType());
    }

    /**
     * A failed assignment must NOT send a notification — the user was not actually assigned.
     */
    @Test
    public void failedAssign_doesNotSendNotification() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onFailure(new Exception("Firestore error"));
            return null;
        }).when(mockEntrantRepo).assignCoOrganizer(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        verify(mockNotifRepo, never()).addNotification(any(), any(), any());
    }
}
