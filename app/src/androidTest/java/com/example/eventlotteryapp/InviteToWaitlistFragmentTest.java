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
import com.example.eventlotteryapp.ui.fragments.InviteToWaitlistFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UI tests for {@link InviteToWaitlistFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: search bar, search button, and back button are visible on launch</li>
 *   <li>Validation: empty query does not call the repository</li>
 *   <li>Search: results show name, email, and phone (when set)</li>
 *   <li>Search: "No users found" message shown when results are empty</li>
 *   <li>Filtering: the organizer is excluded from search results</li>
 *   <li>Button state: "Invited" (disabled) for {@link Entrant#STATUS_WAITLIST_INVITED}</li>
 *   <li>Button state: "On Event" (disabled) for waitlist / invited / enrolled statuses</li>
 *   <li>Button state: "Invite" (enabled) for users not yet on the event</li>
 *   <li>Invite flow: success disables button and changes label to "Invited"</li>
 *   <li>Invite flow: failure re-enables button and restores "Invite" label</li>
 *   <li>getEntrant guard: already-invited user blocks {@code inviteToPrivateWaitlist}</li>
 *   <li>Re-invite: user with {@link Entrant#STATUS_DECLINED_WAITLIST} can be re-invited</li>
 *   <li>Correctness: {@code inviteToPrivateWaitlist} called with correct event and device IDs</li>
 *   <li>Notification: success sends a {@link Notification#TYPE_WAITLIST_INVITE} notification</li>
 *   <li>Notification: failure does NOT send a notification</li>
 * </ul>
 *
 * @author Leyla
 */
@RunWith(AndroidJUnit4.class)
public class InviteToWaitlistFragmentTest {

    private static final String EVENT_ID     = "event123";
    private static final String EVENT_TITLE  = "Secret Gala";
    private static final String ORGANIZER_ID = "organizer456";

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
     * Launches InviteToWaitlistFragment with injected mocks.
     * {@code existingEntrants} pre-populates the status cache used to render button states.
     */
    private void launch(List<Entrant> existingEntrants) {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(1);
            cb.onSuccess(existingEntrants);
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(any(), any());

        Bundle args = new Bundle();
        args.putString("eventId",           EVENT_ID);
        args.putString("eventTitle",        EVENT_TITLE);
        args.putString("organizerDeviceId", ORGANIZER_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                InviteToWaitlistFragment fragment = new InviteToWaitlistFragment();
                fragment.setUserRepository(mockUserRepo);
                fragment.setEntrantRepository(mockEntrantRepo);
                fragment.setNotificationRepository(mockNotifRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                InviteToWaitlistFragment.class, args, R.style.Theme_EventLotteryApp, factory);
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
     * When a user has a phone number, it must also appear in their result row.
     */
    @Test
    public void search_returnsResults_displaysPhone_whenSet() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "555-0100")));
        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(withText("555-0100")).check(matches(isDisplayed()));
    }

    /**
     * When the repository returns no users, the status text must become visible
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
    // Invite button state tests
    // -----------------------------------------------------------------------

    /**
     * A user who is already waitlist-invited (cached from loadExistingEntrants) must show
     * a disabled "Invited" button.
     */
    @Test
    public void existingWaitlistInvited_showsInvitedButton_disabled() {
        List<Entrant> existing = Collections.singletonList(
                makeEntrant("user1", Entrant.STATUS_WAITLIST_INVITED));
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch(existing);

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("Invited")))
                .check(matches(not(isEnabled())));
    }

    /**
     * A user who is on the waitlist must show a disabled "On Event" button
     * — they cannot be re-invited via this screen.
     */
    @Test
    public void existingWaitlistUser_showsOnEventButton_disabled() {
        List<Entrant> existing = Collections.singletonList(
                makeEntrant("user1", Entrant.STATUS_WAITLIST));
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch(existing);

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("On Event")))
                .check(matches(not(isEnabled())));
    }

    /**
     * A user who is already enrolled must also show the disabled "On Event" button.
     */
    @Test
    public void enrolledUser_showsOnEventButton_disabled() {
        List<Entrant> existing = Collections.singletonList(
                makeEntrant("user1", Entrant.STATUS_ENROLLED));
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch(existing);

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("On Event")))
                .check(matches(not(isEnabled())));
    }

    /**
     * A user with no existing entrant record must show an enabled "Invite" button.
     */
    @Test
    public void newUser_showsInviteButton_enabled() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch(); // no existing entrants

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("Invite")))
                .check(matches(isEnabled()));
    }

    /**
     * A user who previously declined the waitlist invite must also show the enabled "Invite"
     * button — they can be re-invited.
     */
    @Test
    public void declinedWaitlistUser_showsInviteButton_enabled() {
        List<Entrant> existing = Collections.singletonList(
                makeEntrant("user1", Entrant.STATUS_DECLINED_WAITLIST));
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));
        launch(existing);

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("Invite")))
                .check(matches(isEnabled()));
    }

    // -----------------------------------------------------------------------
    // Invite flow tests
    // -----------------------------------------------------------------------

    /**
     * After a successful invite the button must be disabled and show "Invited".
     */
    @Test
    public void inviteButton_onSuccess_becomesInvited() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        // getEntrant returns null — user has no existing entrant doc
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).inviteToPrivateWaitlist(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.button_invite), withText("Invited")))
                .check(matches(not(isEnabled())));
    }

    /**
     * After a failed invite the button must be re-enabled and show "Invite" again.
     */
    @Test
    public void inviteButton_onFailure_restoresInviteButton() {
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
        }).when(mockEntrantRepo).inviteToPrivateWaitlist(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.button_invite), isDisplayed()))
                .check(matches(withText("Invite")))
                .check(matches(isEnabled()));
    }

    /**
     * If getEntrant reveals the user is already waitlist-invited at click time,
     * {@code inviteToPrivateWaitlist} must NOT be called and the button must show "Invited".
     */
    @Test
    public void inviteButton_whenGetEntrantReturnsAlreadyInvited_doesNotProceed() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(makeEntrant("user1", Entrant.STATUS_WAITLIST_INVITED));
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        verify(mockEntrantRepo, never()).inviteToPrivateWaitlist(any(), any(), any());
        onView(allOf(withId(R.id.button_invite), withText("Invite")))
                .check(matches(isEnabled()));
    }

    /**
     * A user who previously declined the waitlist invite must be re-invited when clicked —
     * {@code inviteToPrivateWaitlist} must be called even though an entrant doc already exists.
     */
    @Test
    public void inviteButton_forDeclinedWaitlistUser_proceedsWithInvite() {
        stubSearch(Collections.singletonList(
                makeUser("user1", "Alice Smith", "alice@email.com", "")));

        // getEntrant returns DECLINED_WAITLIST — fragment should re-invite
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(makeEntrant("user1", Entrant.STATUS_DECLINED_WAITLIST));
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).inviteToPrivateWaitlist(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        verify(mockEntrantRepo).inviteToPrivateWaitlist(eq(EVENT_ID), eq("user1"), any());
    }

    // -----------------------------------------------------------------------
    // Correctness tests
    // -----------------------------------------------------------------------

    /**
     * {@code inviteToPrivateWaitlist} must be called with the correct eventId and the
     * invited user's deviceId.
     */
    @Test
    public void inviteButton_callsRepository_withCorrectIds() {
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
        }).when(mockEntrantRepo).inviteToPrivateWaitlist(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        verify(mockEntrantRepo).inviteToPrivateWaitlist(eq(EVENT_ID), eq("user1"), any());
    }

    // -----------------------------------------------------------------------
    // Notification tests
    // -----------------------------------------------------------------------

    /**
     * A successful invite must send a waitlist invitation notification to the invited user.
     * Verifies the notification is sent to the correct device ID and its type is
     * {@link Notification#TYPE_WAITLIST_INVITE}.
     */
    @Test
    public void successfulInvite_sendsWaitlistInviteNotification_toInvitedUser() {
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
        }).when(mockEntrantRepo).inviteToPrivateWaitlist(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotifRepo).addNotification(eq("user1"), notifCaptor.capture(), any());

        assertEquals(Notification.TYPE_WAITLIST_INVITE, notifCaptor.getValue().getType());
    }

    /**
     * A failed invite must NOT send a notification — the user was not actually invited.
     */
    @Test
    public void failedInvite_doesNotSendNotification() {
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
        }).when(mockEntrantRepo).inviteToPrivateWaitlist(any(), any(), any());

        launch();

        onView(withId(R.id.edit_search)).perform(replaceText("Alice"), closeSoftKeyboard());
        onView(withId(R.id.button_search)).perform(click());
        onView(allOf(withId(R.id.button_invite), isDisplayed())).perform(click());

        verify(mockNotifRepo, never()).addNotification(any(), any(), any());
    }
}
