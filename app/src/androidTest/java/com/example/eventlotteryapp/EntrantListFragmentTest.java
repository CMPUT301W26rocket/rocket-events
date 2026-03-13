package com.example.eventlotteryapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.fragments.EntrantListFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

/**
 * UI tests for {@link EntrantListFragment}.
 *
 * <p>Both repositories are mocked so no real Firebase calls are made.
 * Tests cover the grouped display of entrants, name resolution, fallback
 * to deviceId, and empty-state messaging.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantListFragmentTest {

    private static final String EVENT_ID  = "event_abc";
    private static final String DEVICE_ID = "device_001";
    private static final String USER_NAME = "Alice Smith";

    @Mock EntrantRepository mockEntrantRepo;
    @Mock UserRepository    mockUserRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Creates a minimal Entrant with the given device ID and status. */
    private Entrant makeEntrant(String deviceId, String status) {
        Entrant e = new Entrant();
        e.setDeviceId(deviceId);
        e.setEventId(EVENT_ID);
        e.setStatus(status);
        return e;
    }

    /**
     * Launches {@link EntrantListFragment} using a {@link FragmentFactory} so mocks are
     * injected before {@code onCreateView} runs and no real Firebase calls are made.
     * All stubs must be configured before calling this method.
     */
    private void launch() {
        Bundle args = new Bundle();
        args.putString("eventId", EVENT_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                EntrantListFragment fragment = new EntrantListFragment();
                fragment.setEntrantRepository(mockEntrantRepo);
                fragment.setUserRepository(mockUserRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                EntrantListFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /** Stubs entrantRepo to immediately call onSuccess with the given list. */
    private void stubEntrants(List<Entrant> entrants) {
        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(entrants);
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());
    }

    /** Stubs userRepo to return a User with the given name for the given deviceId. */
    private void stubUser(String deviceId, String name) {
        doAnswer(inv -> {
            UserRepository.FirestoreCallback<User> cb = inv.getArgument(1);
            User user = new User();
            user.setName(name);
            cb.onSuccess(user);
            return null;
        }).when(mockUserRepo).getUser(eq(deviceId), any());
    }

    /** Stubs userRepo to return null (user not found) for the given deviceId. */
    private void stubUserNotFound(String deviceId) {
        doAnswer(inv -> {
            UserRepository.FirestoreCallback<User> cb = inv.getArgument(1);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).getUser(eq(deviceId), any());
    }

    /** Stubs userRepo to call onFailure for the given deviceId. */
    private void stubUserFailure(String deviceId) {
        doAnswer(inv -> {
            UserRepository.FirestoreCallback<User> cb = inv.getArgument(1);
            cb.onFailure(new Exception("lookup failed"));
            return null;
        }).when(mockUserRepo).getUser(eq(deviceId), any());
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    /**
     * The back button must be visible when the fragment opens.
     */
    @Test
    public void backButton_isDisplayed() {
        stubEntrants(Collections.emptyList());
        launch();
        onView(withId(R.id.button_back)).check(matches(isDisplayed()));
    }

    /**
     * The entrants RecyclerView must be visible when the fragment opens.
     */
    @Test
    public void recyclerView_isDisplayed() {
        stubEntrants(Collections.emptyList());
        launch();
        onView(withId(R.id.recycler_entrants)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Empty state
    // -----------------------------------------------------------------------

    /**
     * When the event has no entrants, the list must show "No entrants yet".
     */
    @Test
    public void noEntrants_showsEmptyMessage() {
        stubEntrants(Collections.emptyList());
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("No entrants yet"))));
    }

    /**
     * When entrantRepo returns null, the list must show "No entrants yet".
     */
    @Test
    public void nullEntrantList_showsEmptyMessage() {
        stubEntrants(null);
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("No entrants yet"))));
    }

    // -----------------------------------------------------------------------
    // Name resolution
    // -----------------------------------------------------------------------

    /**
     * When the user's name is found in Firestore, their name must be shown in the list.
     */
    @Test
    public void entrantWithResolvedName_showsUserName() {
        stubEntrants(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_WAITLIST)));
        stubUser(DEVICE_ID, USER_NAME);
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText(USER_NAME))));
    }

    /**
     * When the user document is not found, the list must fall back to showing the deviceId.
     */
    @Test
    public void entrantWithNoUserDoc_fallsBackToDeviceId() {
        stubEntrants(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_WAITLIST)));
        stubUserNotFound(DEVICE_ID);
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText(DEVICE_ID))));
    }

    /**
     * When the user lookup fails, the list must fall back to showing the deviceId.
     */
    @Test
    public void entrantWithUserLookupFailure_fallsBackToDeviceId() {
        stubEntrants(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_WAITLIST)));
        stubUserFailure(DEVICE_ID);
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText(DEVICE_ID))));
    }

    // -----------------------------------------------------------------------
    // Section headers — correct label per status
    // -----------------------------------------------------------------------

    /**
     * A waitlist entrant must appear under the "Waitlist" section header.
     */
    @Test
    public void waitlistEntrant_showsWaitlistHeader() {
        stubEntrants(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_WAITLIST)));
        stubUser(DEVICE_ID, USER_NAME);
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("Waitlist"))));
    }

    /**
     * An invited entrant must appear under the "Invited (Pending Response)" section header.
     */
    @Test
    public void invitedEntrant_showsInvitedHeader() {
        stubEntrants(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_INVITED)));
        stubUser(DEVICE_ID, USER_NAME);
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("Invited (Pending Response)"))));
    }

    /**
     * An enrolled entrant must appear under the "Enrolled" section header.
     */
    @Test
    public void enrolledEntrant_showsEnrolledHeader() {
        stubEntrants(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_ENROLLED)));
        stubUser(DEVICE_ID, USER_NAME);
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("Enrolled"))));
    }

    /**
     * A not-selected entrant must appear under the "Not Selected" section header.
     */
    @Test
    public void notSelectedEntrant_showsNotSelectedHeader() {
        stubEntrants(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_NOT_SELECTED)));
        stubUser(DEVICE_ID, USER_NAME);
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("Not Selected"))));
    }

    // -----------------------------------------------------------------------
    // Multi-status grouping
    // -----------------------------------------------------------------------

    /**
     * When entrants have different statuses, both section headers must appear.
     */
    @Test
    public void multipleStatuses_showBothSectionHeaders() {
        stubEntrants(Arrays.asList(
                makeEntrant("device_invited",  Entrant.STATUS_INVITED),
                makeEntrant("device_waitlist", Entrant.STATUS_WAITLIST)
        ));
        stubUser("device_invited",  "Bob");
        stubUser("device_waitlist", "Carol");
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("Invited (Pending Response)"))));
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("Waitlist"))));
    }

    /**
     * When entrants have different statuses, both names must appear in the list.
     */
    @Test
    public void multipleStatuses_showBothEntrantNames() {
        stubEntrants(Arrays.asList(
                makeEntrant("device_invited",  Entrant.STATUS_INVITED),
                makeEntrant("device_waitlist", Entrant.STATUS_WAITLIST)
        ));
        stubUser("device_invited",  "Bob");
        stubUser("device_waitlist", "Carol");
        launch();
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("Bob"))));
        onView(withId(R.id.recycler_entrants))
                .check(matches(hasDescendant(withText("Carol"))));
    }
}
