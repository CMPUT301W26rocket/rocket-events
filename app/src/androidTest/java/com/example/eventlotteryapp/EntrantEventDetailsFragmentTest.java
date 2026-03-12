package com.example.eventlotteryapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.ui.fragments.EntrantEventDetailsFragment;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * UI tests for {@link EntrantEventDetailsFragment}.
 *
 * These tests use Mockito to mock the repositories so we never touch Firebase.
 * Instead, each test controls exactly what data the fragment receives and
 * then checks that the UI responds correctly.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailsFragmentTest {

    // --- Mocks ---
    // @Mock tells Mockito to create a fake version of these classes.
    // The fake does nothing by default — we tell it what to return in each test.
    @Mock EventRepository mockEventRepo;
    @Mock EntrantRepository mockEntrantRepo;
    @Mock ListenerRegistration mockListenerRegistration;

    // A fake event we'll reuse across tests
    private Event fakeEvent;

    @Before
    public void setUp() {
        // Initialise all the @Mock fields above
        MockitoAnnotations.openMocks(this);

        // Build a fake event with realistic data
        fakeEvent = new Event();
        fakeEvent.setEventId("event123");
        fakeEvent.setTitle("Test Event");
        fakeEvent.setDescription("A great event description");
        fakeEvent.setOrganizerName("Jane Smith");
        fakeEvent.setLocation("Edmonton Convention Centre");
        fakeEvent.setRegistrationFee(0.0);
        fakeEvent.setLotteryCapacity(50);
        fakeEvent.setGeolocationRequired(false);
        fakeEvent.setHasWaitlistLimit(false);

        // Registration window: opened yesterday, closes tomorrow → currently open
        fakeEvent.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 86400000L));
        fakeEvent.setRegistrationCloseDate(new Date(System.currentTimeMillis() + 86400000L));

        // The waitlist count listener fires immediately with a count of 3.
        // doAnswer lets us intercept the call and immediately invoke the callback ourselves.
        // This simulates Firestore calling us back with data — without actually calling Firestore.
        doAnswer(invocation -> {
            // The callback is the 2nd argument (index 1) passed to listenToWaitlistCount()
            EntrantRepository.FirestoreCallback<Integer> cb = invocation.getArgument(1);
            cb.onSuccess(3); // pretend there are 3 people on the waitlist
            return mockListenerRegistration; // return a fake registration (so remove() doesn't crash)
        }).when(mockEntrantRepo).listenToWaitlistCount(any(), any());
    }

    /**
     * Helper that launches the fragment with a specific entrant status.
     *
     * @param entrant pass an Entrant with a status to simulate a user already on the waitlist,
     *                or pass null to simulate a user who has never joined.
     */
    private void launch(Entrant entrant) {
        // Tell the mock event repo what to return when getEventById() is called
        doAnswer(invocation -> {
            // The callback is the 2nd argument (index 1)
            EventRepository.FirestoreCallback<Event> cb = invocation.getArgument(1);
            cb.onSuccess(fakeEvent); // return our fake event
            return null;
        }).when(mockEventRepo).getEventById(any(), any());

        // Tell the mock entrant repo what to return when getEntrant() is called
        doAnswer(invocation -> {
            // The callback is the 3rd argument (index 2)
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(entrant); // return the entrant we were given (can be null)
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        // Bundle the arguments the fragment expects
        Bundle args = new Bundle();
        args.putString("eventId", "event123");
        args.putString("deviceId", "device123");

        // FragmentFactory lets us inject our mocks into the fragment BEFORE it loads.
        // Without this, the fragment would create real repositories and call real Firestore.
        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                EntrantEventDetailsFragment fragment = new EntrantEventDetailsFragment();
                // Inject our mocks via the setters we added to the fragment
                fragment.setEventRepository(mockEventRepo);
                fragment.setEntrantRepository(mockEntrantRepo);
                return fragment;
            }
        };

        // Launch the fragment inside a test container (no full Activity needed)
        FragmentScenario.launchInContainer(EntrantEventDetailsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    // --- Tests ---

    @Test
    public void eventTitle_isDisplayed() {
        // Launch with no entrant (user hasn't joined)
        launch(null);

        // The fragment should display the event title we set on fakeEvent
        onView(withId(R.id.text_detail_title)).check(matches(withText("Test Event")));
    }

    @Test
    public void eventOrganizer_isDisplayed() {
        launch(null);

        // Organizer is shown as "By <name>"
        onView(withId(R.id.text_detail_organizer)).check(matches(withText("By Jane Smith")));
    }

    @Test
    public void waitlistCount_isDisplayed() {
        launch(null);

        // The mock returns 3, so the count view should show "Current Waitlist: 3"
        onView(withId(R.id.text_detail_waitlist_count))
                .check(matches(withText("Current Waitlist: 3")));
    }

    @Test
    public void whenNotOnWaitlist_buttonShowsJoinWaitlist() {
        // null entrant = this user has never joined the event
        launch(null);

        // With registration open and no entrant record, the button should say "Join Waitlist"
        onView(withId(R.id.button_entrant_action)).check(matches(withText("Join Waitlist")));
    }

    @Test
    public void whenNotOnWaitlist_buttonIsEnabled() {
        launch(null);

        // The button should be clickable (registration is open)
        onView(withId(R.id.button_entrant_action)).check(matches(isEnabled()));
    }

    @Test
    public void whenOnWaitlist_buttonShowsLeaveWaitlist() {
        // Create an entrant with STATUS_WAITLIST to simulate the user already being on the waitlist
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_WAITLIST);
        launch(entrant);

        onView(withId(R.id.button_entrant_action)).check(matches(withText("Leave Waitlist")));
    }

    @Test
    public void whenInvited_buttonShowsInvited() {
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_INVITED);
        launch(entrant);

        onView(withId(R.id.button_entrant_action)).check(matches(withText("Invited")));
    }

    @Test
    public void whenInvited_buttonIsEnabled() {
        // The invited button should be clickable (tapping it opens the accept/decline dialog)
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_INVITED);
        launch(entrant);

        onView(withId(R.id.button_entrant_action)).check(matches(isEnabled()));
    }

    @Test
    public void whenEnrolled_buttonShowsEnrolled() {
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_ENROLLED);
        launch(entrant);

        onView(withId(R.id.button_entrant_action)).check(matches(withText("Enrolled")));
    }

    @Test
    public void whenEnrolled_buttonIsDisabled() {
        // Enrolled is a final state — the button should be greyed out and unclickable
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_ENROLLED);
        launch(entrant);

        onView(withId(R.id.button_entrant_action)).check(matches(not(isEnabled())));
    }

    @Test
    public void whenDeclined_buttonIsDisabled() {
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_DECLINED);
        launch(entrant);

        onView(withId(R.id.button_entrant_action)).check(matches(not(isEnabled())));
    }

    @Test
    public void whenNotSelected_buttonShowsLeaveWaitlist() {
        // Not-selected users can still leave so they stop appearing in future draws
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_NOT_SELECTED);
        launch(entrant);

        onView(withId(R.id.button_entrant_action)).check(matches(withText("Leave Waitlist")));
    }

    @Test
    public void whenRegistrationClosed_buttonShowsRegistrationClosed() {
        // Move both dates to the past so registration has ended
        fakeEvent.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 172800000L));
        fakeEvent.setRegistrationCloseDate(new Date(System.currentTimeMillis() - 86400000L));

        // null entrant = user hasn't joined, but registration is now closed
        launch(null);

        onView(withId(R.id.button_entrant_action)).check(matches(withText("Registration Closed")));
    }

    @Test
    public void whenRegistrationClosed_buttonIsDisabled() {
        fakeEvent.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 172800000L));
        fakeEvent.setRegistrationCloseDate(new Date(System.currentTimeMillis() - 86400000L));
        launch(null);

        onView(withId(R.id.button_entrant_action)).check(matches(not(isEnabled())));
    }

    @Test
    public void whenRegistrationNotYetOpen_buttonShowsNotOpenYet() {
        // Move both dates to the future so registration hasn't started
        fakeEvent.setRegistrationOpenDate(new Date(System.currentTimeMillis() + 86400000L));
        fakeEvent.setRegistrationCloseDate(new Date(System.currentTimeMillis() + 172800000L));
        launch(null);

        onView(withId(R.id.button_entrant_action))
                .check(matches(withText("Registration Not Open Yet")));
    }

    @Test
    public void whenCancelled_buttonShowsCancelled() {
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_CANCELLED);
        launch(entrant);

        onView(withId(R.id.button_entrant_action)).check(matches(withText("Cancelled")));
    }

    @Test
    public void whenCancelled_buttonIsDisabled() {
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_CANCELLED);
        launch(entrant);

        onView(withId(R.id.button_entrant_action)).check(matches(not(isEnabled())));
    }

    // --- Button click tests ---

    @Test
    public void clickJoinWaitlist_buttonChangesToLeaveWaitlist() {
        // Mock joinWaitlist() to immediately call onSuccess — simulates a successful Firestore write
        doAnswer(invocation -> {
            // The callback is the 3rd argument (index 2)
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).joinWaitlist(any(), any(), any());

        // Launch with null entrant — user is not on the waitlist yet
        launch(null);

        // Button currently says "Join Waitlist" — click it
        onView(withId(R.id.button_entrant_action)).perform(click());

        // After the mock joinWaitlist succeeds, the fragment updates currentEntrant
        // and calls updateButton() — button should now say "Leave Waitlist"
        onView(withId(R.id.button_entrant_action)).check(matches(withText("Leave Waitlist")));
    }

    @Test
    public void clickLeaveWaitlist_buttonChangesToJoinWaitlist() {
        // Mock leaveWaitlist() to immediately call onSuccess
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).leaveWaitlist(any(), any(), any());

        // Launch with a waitlist entrant — user is already on the waitlist
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_WAITLIST);
        launch(entrant);

        // Button currently says "Leave Waitlist" — click it
        onView(withId(R.id.button_entrant_action)).perform(click());

        // After the mock leaveWaitlist succeeds, currentEntrant is set to null
        // and updateButton() runs — button should go back to "Join Waitlist"
        onView(withId(R.id.button_entrant_action)).check(matches(withText("Join Waitlist")));
    }

    @Test
    public void clickInvited_showsInvitationDialog() {
        // Launch with an invited entrant
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_INVITED);
        launch(entrant);

        // Click the "Invited" button
        onView(withId(R.id.button_entrant_action)).perform(click());

        // The invitation dialog should appear with the correct title
        onView(withText("You're Invited!")).check(matches(isDisplayed()));
    }

    @Test
    public void clickAcceptInvitation_buttonBecomesEnrolledAndDisabled() {
        // Mock updateStatus() to immediately call onSuccess
        doAnswer(invocation -> {
            // The callback is the 4th argument (index 3)
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_INVITED);
        launch(entrant);

        // Open the invitation dialog
        onView(withId(R.id.button_entrant_action)).perform(click());

        // Click "Accept" in the dialog
        // This calls respondToInvitation(STATUS_ENROLLED) which calls updateStatus()
        onView(withText("Accept")).perform(click());

        // After the mock updateStatus succeeds, currentEntrant status is set to "enrolled"
        // and updateButton() runs — button should say "Enrolled" and be disabled
        onView(withId(R.id.button_entrant_action)).check(matches(withText("Enrolled")));
        onView(withId(R.id.button_entrant_action)).check(matches(not(isEnabled())));
    }

    @Test
    public void clickDeclineInvitation_buttonBecomesDeclinedAndDisabled() {
        // Mock updateStatus() to immediately call onSuccess
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.STATUS_INVITED);
        launch(entrant);

        // Open the invitation dialog
        onView(withId(R.id.button_entrant_action)).perform(click());

        // Click "Decline" in the dialog
        onView(withText("Decline")).perform(click());

        // Button should say "Declined" and be disabled
        onView(withId(R.id.button_entrant_action)).check(matches(withText("Declined")));
        onView(withId(R.id.button_entrant_action)).check(matches(not(isEnabled())));
    }

    @Test
    public void whenWaitlistFull_clickJoinWaitlist_buttonStaysEnabledAsJoinWaitlist() {
        // Enable waitlist limit on the fake event
        fakeEvent.setHasWaitlistLimit(true);
        fakeEvent.setWaitlistLimit(10);

        // Mock isWaitlistFull() to return true — the waitlist has no room
        doAnswer(invocation -> {
            // The callback is the 3rd argument (index 2)
            EntrantRepository.FirestoreCallback<Boolean> cb = invocation.getArgument(2);
            cb.onSuccess(true); // waitlist is full
            return null;
        }).when(mockEntrantRepo).isWaitlistFull(any(), any(int.class), any());

        // Launch with null entrant — user hasn't joined yet
        launch(null);

        // Click "Join Waitlist"
        onView(withId(R.id.button_entrant_action)).perform(click());

        // The fragment checks isWaitlistFull first — since it's full, joinWaitlist is never called.
        // The button should be re-enabled and still say "Join Waitlist" (join was blocked)
        onView(withId(R.id.button_entrant_action)).check(matches(withText("Join Waitlist")));
        onView(withId(R.id.button_entrant_action)).check(matches(isEnabled()));
    }
}
