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
import com.example.eventlotteryapp.ui.fragments.EventHistoryFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * UI tests for {@link EventHistoryFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: header text and RecyclerView are visible on launch</li>
 *   <li>Data: event title and formatted status appear for each history entry</li>
 *   <li>Status formatting: each Entrant status maps to the correct display string</li>
 *   <li>Empty state: RecyclerView has no items when the entrant list is empty</li>
 *   <li>Failure state: RecyclerView has no items when the repository reports an error</li>
 *   <li>Correctness: repository is called with the deviceId passed via arguments</li>
 * </ul>
 *
 * <p>Loading is two-step: {@code entrantRepository.getUserEventHistory()} returns a list
 * of Entrants, then for each Entrant {@code eventRepository.getEventById()} fetches the
 * matching Event. Both calls are mocked via Mockito {@code doAnswer}.
 */
@RunWith(AndroidJUnit4.class)
public class EventHistoryFragmentTest {

    private static final String DEVICE_ID = "device123";

    @Mock EntrantRepository mockEntrantRepo;
    @Mock EventRepository mockEventRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a fake Entrant with the given eventId and status.
     */
    private Entrant makeEntrant(String eventId, String status) {
        Entrant e = new Entrant();
        e.setDeviceId(DEVICE_ID);
        e.setEventId(eventId);
        e.setStatus(status);
        return e;
    }

    /**
     * Builds a fake Event with just enough data for the adapter to display.
     */
    private Event makeEvent(String id, String title) {
        Event e = new Event();
        e.setEventId(id);
        e.setTitle(title);
        return e;
    }

    /**
     * Launches the fragment with mocked repositories.
     *
     * <p>The entrant repo immediately returns {@code entrants}. The event repo
     * resolves each entrant's eventId to a matching event from {@code events} by
     * position (entrant[i] → event[i]).
     *
     * @param entrants list of Entrant objects the entrant repo will return
     * @param events   list of Event objects the event repo will return, one per entrant
     */
    private void launchWithData(List<Entrant> entrants, List<Event> events) {
        // Step 1: getUserEventHistory returns the entrant list immediately
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(1);
            cb.onSuccess(entrants);
            return null;
        }).when(mockEntrantRepo).getUserEventHistory(any(), any());

        // Step 2: for each entrant's eventId, return the matching event
        doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            EventRepository.FirestoreCallback<Event> cb = invocation.getArgument(1);
            for (Event event : events) {
                if (event.getEventId().equals(eventId)) {
                    cb.onSuccess(event);
                    return null;
                }
            }
            cb.onSuccess(null); // not found
            return null;
        }).when(mockEventRepo).getEventById(any(), any());

        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                EventHistoryFragment fragment = new EventHistoryFragment();
                fragment.setEntrantRepository(mockEntrantRepo);
                fragment.setEventRepository(mockEventRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                EventHistoryFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /**
     * Launches with a mocked entrant repo that fires onFailure.
     */
    private void launchWithFailure() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(1);
            cb.onFailure(new Exception("Firestore unavailable"));
            return null;
        }).when(mockEntrantRepo).getUserEventHistory(any(), any());

        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                EventHistoryFragment fragment = new EventHistoryFragment();
                fragment.setEntrantRepository(mockEntrantRepo);
                fragment.setEventRepository(mockEventRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                EventHistoryFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    /**
     * The "Event History" header must be visible when the fragment opens.
     * Note: the header TextView has no android:id, so we match by text.
     */
    @Test
    public void header_isDisplayed() {
        launchWithData(new ArrayList<>(), new ArrayList<>());

        onView(withText("Event History")).check(matches(isDisplayed()));
    }

    /**
     * The RecyclerView must be visible when the fragment opens.
     */
    @Test
    public void recyclerView_isDisplayed() {
        launchWithData(new ArrayList<>(), new ArrayList<>());

        onView(withId(R.id.recycler_event_history)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Data tests
    // -----------------------------------------------------------------------

    /**
     * When history has one entry, the event title must appear in the list.
     */
    @Test
    public void eventTitle_isDisplayed_whenHistoryHasOneEntry() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_ENROLLED)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Summer Camp")).check(matches(isDisplayed()));
    }

    /**
     * When history has two entries, both event titles must be reachable by scrolling.
     */
    @Test
    public void bothEventTitles_areDisplayed_whenHistoryHasTwoEntries() {
        launchWithData(
                Arrays.asList(
                        makeEntrant("e1", Entrant.STATUS_ENROLLED),
                        makeEntrant("e2", Entrant.STATUS_WAITLIST)
                ),
                Arrays.asList(
                        makeEvent("e1", "Summer Camp"),
                        makeEvent("e2", "Hackathon Night")
                )
        );

        onView(withId(R.id.recycler_event_history)).perform(scrollToPosition(0));
        onView(withText("Summer Camp")).check(matches(isDisplayed()));

        onView(withId(R.id.recycler_event_history)).perform(scrollToPosition(1));
        onView(withText("Hackathon Night")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Status formatting tests
    // -----------------------------------------------------------------------

    /**
     * "waitlist" status must be displayed as "Waitlisted".
     */
    @Test
    public void status_waitlist_isFormattedCorrectly() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_WAITLIST)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Waitlisted")).check(matches(isDisplayed()));
    }

    /**
     * "enrolled" status must be displayed as "Enrolled".
     */
    @Test
    public void status_enrolled_isFormattedCorrectly() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_ENROLLED)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Enrolled")).check(matches(isDisplayed()));
    }

    /**
     * "invited" status must be displayed as "Invited to Enroll".
     */
    @Test
    public void status_invited_isFormattedCorrectly() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_INVITED)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Invited to Enroll")).check(matches(isDisplayed()));
    }

    /**
     * "not_selected" status must be displayed as "Not Selected".
     */
    @Test
    public void status_notSelected_isFormattedCorrectly() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_NOT_SELECTED)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Not Selected")).check(matches(isDisplayed()));
    }

    /**
     * "declined" status must be displayed as "Declined".
     */
    @Test
    public void status_declined_isFormattedCorrectly() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_DECLINED)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Declined")).check(matches(isDisplayed()));
    }

    /**
     * "cancelled" status must be displayed as "Cancelled".
     */
    @Test
    public void status_cancelled_isFormattedCorrectly() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_CANCELLED)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Cancelled")).check(matches(isDisplayed()));
    }

    /**
     * "waitlist_invited" status must be displayed as "Invited to Join Waitlist".
     * This status is used when an organizer invites a non-waitlisted user to the waitlist
     * for a private event.
     */
    @Test
    public void status_waitlistInvited_isFormattedCorrectly() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_WAITLIST_INVITED)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Invited to Join Waitlist")).check(matches(isDisplayed()));
    }

    /**
     * "declined_waitlist" status must be displayed as "Waitlist Invite Declined".
     * This status is set when a user declines a private-event waitlist invitation.
     */
    @Test
    public void status_declinedWaitlist_isFormattedCorrectly() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_DECLINED_WAITLIST)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Waitlist Invite Declined")).check(matches(isDisplayed()));
    }

    /**
     * "co_organizer" status must be displayed as "Co-organizer".
     */
    @Test
    public void status_coOrganizer_isFormattedCorrectly() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", Entrant.STATUS_CO_ORGANIZER)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Co-organizer")).check(matches(isDisplayed()));
    }

    /**
     * A null status must be displayed as "Unknown" rather than crashing.
     */
    @Test
    public void status_null_isFormattedAsUnknown() {
        launchWithData(
                Arrays.asList(makeEntrant("e1", null)),
                Arrays.asList(makeEvent("e1", "Summer Camp"))
        );

        onView(withText("Unknown")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Empty and failure states
    // -----------------------------------------------------------------------

    /**
     * When the entrant list is empty, the RecyclerView should have no child views.
     * The fragment returns early without setting an adapter when the list is empty.
     */
    @Test
    public void recyclerView_hasNoItems_whenEntrantListIsEmpty() {
        launchWithData(new ArrayList<>(), new ArrayList<>());

        onView(withId(R.id.recycler_event_history)).check(matches(hasChildCount(0)));
    }

    /**
     * When the entrant repository fires onFailure, no adapter is set and the
     * RecyclerView should have no child views.
     */
    @Test
    public void recyclerView_hasNoItems_whenRepositoryFails() {
        launchWithFailure();

        onView(withId(R.id.recycler_event_history)).check(matches(hasChildCount(0)));
    }

    // -----------------------------------------------------------------------
    // Correctness — deviceId forwarded to repository
    // -----------------------------------------------------------------------

    /**
     * The fragment must pass the deviceId from its arguments to
     * {@link EntrantRepository#getUserEventHistory}, ensuring the correct
     * user's history is requested.
     */
    @Test
    public void getUserEventHistory_isCalledWithCorrectDeviceId() {
        launchWithData(new ArrayList<>(), new ArrayList<>());

        verify(mockEntrantRepo).getUserEventHistory(eq(DEVICE_ID), any());
    }
}
