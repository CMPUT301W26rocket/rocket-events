package com.example.eventlotteryapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.ui.fragments.MyEventsFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * UI tests for {@link MyEventsFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: header and RecyclerView are visible on launch</li>
 *   <li>Data: event titles and organizer name appear when the repository returns events</li>
 *   <li>Private events: organizer's private events ARE shown (no filter, unlike HomeFragment)</li>
 *   <li>Empty state: RecyclerView shows no items when the list is empty</li>
 *   <li>Failure: RecyclerView remains empty when the repository reports an error</li>
 *   <li>Correctness: repository is called with the deviceId passed via arguments</li>
 *   <li>Registration badges: "Open", "Coming Soon", "Closed" rendered correctly</li>
 *   <li>Waitlist badges: "Spots Available", "Waitlist Full", hidden when no limit</li>
 *   <li>Date: start date shown when set, hidden when absent</li>
 * </ul>
 *
 * @author Leyla
 */
@RunWith(AndroidJUnit4.class)
public class MyEventsFragmentTest {

    private static final String DEVICE_ID = "device123";
    private static final long   DAY_MS    = 86_400_000L;

    @Mock EventRepository mockEventRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /** Base event — no registration dates, so badge renders as "Closed". */
    private Event makeEvent(String id, String title) {
        Event e = new Event();
        e.setEventId(id);
        e.setTitle(title);
        e.setOrganizerName("Test Organizer");
        e.setDescription("A test event");
        return e;
    }

    /** Open-registration event: openDate yesterday, closeDate tomorrow → badge "Open". */
    private Event makeOpenEvent(String id, String title) {
        Event e = makeEvent(id, title);
        e.setRegistrationOpenDate(new Date(System.currentTimeMillis() - DAY_MS));
        e.setRegistrationCloseDate(new Date(System.currentTimeMillis() + DAY_MS));
        return e;
    }

    /** Upcoming event: openDate tomorrow → badge "Coming Soon". */
    private Event makeUpcomingEvent(String id, String title) {
        Event e = makeEvent(id, title);
        e.setRegistrationOpenDate(new Date(System.currentTimeMillis() + DAY_MS));
        e.setRegistrationCloseDate(new Date(System.currentTimeMillis() + 2 * DAY_MS));
        return e;
    }

    /**
     * Launches MyEventsFragment with a mock repository that immediately returns
     * the given event list via onSuccess.
     *
     * @param events the list of events the mock repository will return
     */
    private void launchWithEvents(List<Event> events) {
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<List<Event>> cb = invocation.getArgument(1);
            cb.onSuccess(events);
            return null;
        }).when(mockEventRepo).getEventsByOrganizerId(any(), any());

        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                MyEventsFragment fragment = new MyEventsFragment();
                fragment.setEventRepository(mockEventRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                MyEventsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /**
     * Launches MyEventsFragment with a mock repository that immediately fires onFailure.
     */
    private void launchWithFailure() {
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<List<Event>> cb = invocation.getArgument(1);
            cb.onFailure(new Exception("Firestore unavailable"));
            return null;
        }).when(mockEventRepo).getEventsByOrganizerId(any(), any());

        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                MyEventsFragment fragment = new MyEventsFragment();
                fragment.setEventRepository(mockEventRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                MyEventsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    /**
     * The "My Hosted Events" header TextView must be visible when the fragment opens.
     */
    @Test
    public void header_isDisplayed() {
        launchWithEvents(new ArrayList<>());

        onView(withId(R.id.text_my_events_header)).check(matches(isDisplayed()));
    }

    /**
     * The RecyclerView must be visible when the fragment opens.
     */
    @Test
    public void recyclerView_isDisplayed() {
        launchWithEvents(new ArrayList<>());

        onView(withId(R.id.recycler_my_events)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Data tests
    // -----------------------------------------------------------------------

    /**
     * When the repository returns one event, its title must appear in the list.
     */
    @Test
    public void eventTitle_isDisplayed_whenRepositoryReturnsOneEvent() {
        launchWithEvents(Arrays.asList(makeEvent("e1", "Summer Camp")));

        onView(withText("Summer Camp")).check(matches(isDisplayed()));
    }

    /**
     * When the repository returns two events, both titles must be reachable by scrolling.
     * Event cards are tall so the second item may be off-screen — scrollToPosition() handles this.
     */
    @Test
    public void bothEventTitles_areDisplayed_whenRepositoryReturnsTwoEvents() {
        launchWithEvents(Arrays.asList(
                makeEvent("e1", "Summer Camp"),
                makeEvent("e2", "Hackathon Night")
        ));

        onView(withId(R.id.recycler_my_events)).perform(scrollToPosition(0));
        onView(withText("Summer Camp")).check(matches(isDisplayed()));

        onView(withId(R.id.recycler_my_events)).perform(scrollToPosition(1));
        onView(withText("Hackathon Night")).check(matches(isDisplayed()));
    }

    /**
     * When the repository returns an empty list, the RecyclerView should have no child views.
     */
    @Test
    public void recyclerView_hasNoItems_whenRepositoryReturnsEmptyList() {
        launchWithEvents(new ArrayList<>());

        onView(withId(R.id.recycler_my_events)).check(matches(hasChildCount(0)));
    }

    // -----------------------------------------------------------------------
    // Correctness test — deviceId is forwarded to the repository
    // -----------------------------------------------------------------------

    /**
     * The fragment must pass the deviceId from its arguments to
     * {@link EventRepository#getEventsByOrganizerId}, not a null or hardcoded value.
     * This verifies that only the current user's hosted events are requested.
     */
    @Test
    public void getEventsByOrganizerId_isCalledWithCorrectDeviceId() {
        launchWithEvents(new ArrayList<>());

        // verify the repo was called with the exact deviceId we put in the args bundle
        verify(mockEventRepo).getEventsByOrganizerId(eq(DEVICE_ID), any());
    }

    // -----------------------------------------------------------------------
    // Failure state
    // -----------------------------------------------------------------------

    /**
     * When the repository fires onFailure, the RecyclerView should still have no items
     * (no adapter was set). The fragment shows a toast, but toasts cannot be checked with
     * standard Espresso matchers.
     */
    @Test
    public void recyclerView_hasNoItems_whenRepositoryFails() {
        launchWithFailure();

        onView(withId(R.id.recycler_my_events)).check(matches(hasChildCount(0)));
    }

    // -----------------------------------------------------------------------
    // Private event test
    // -----------------------------------------------------------------------

    /**
     * A private event created by the organizer must appear in My Events.
     * Unlike HomeFragment, MyEventsFragment applies no privacy filter —
     * organizers should always see all their own events.
     */
    @Test
    public void privateEvent_isShownInMyEvents() {
        Event privateEvent = makeEvent("e1", "Secret Gala");
        privateEvent.setPrivateEvent(true);

        launchWithEvents(Arrays.asList(privateEvent));

        onView(withText("Secret Gala")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Organizer name test
    // -----------------------------------------------------------------------

    /**
     * The organizer name must appear as the subtitle beneath the event title.
     */
    @Test
    public void organizerName_isDisplayedAsSubtitle() {
        Event e = makeEvent("e1", "Summer Camp");
        e.setOrganizerName("Jane Doe");

        launchWithEvents(Arrays.asList(e));

        onView(withText("Jane Doe")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Registration badge tests
    // -----------------------------------------------------------------------

    /**
     * An event with no registration dates must show the "Closed" badge.
     */
    @Test
    public void badge_registration_showsClosed_forEventWithNoDates() {
        launchWithEvents(Arrays.asList(makeEvent("e1", "Summer Camp")));

        onView(withText("Closed")).check(matches(isDisplayed()));
    }

    /**
     * An open-registration event must show the "Open" badge.
     */
    @Test
    public void badge_registration_showsOpen_forOpenEvent() {
        launchWithEvents(Arrays.asList(makeOpenEvent("e1", "Summer Camp")));

        onView(withText("Open")).check(matches(isDisplayed()));
    }

    /**
     * An upcoming event (registration not yet open) must show the "Coming Soon" badge.
     */
    @Test
    public void badge_registration_showsComingSoon_forUpcomingEvent() {
        launchWithEvents(Arrays.asList(makeUpcomingEvent("e1", "Future Fest")));

        onView(withText("Coming Soon")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Waitlist badge tests
    // -----------------------------------------------------------------------

    /**
     * When an event has a waitlist limit and spots remain, the "Spots Available" badge is shown.
     */
    @Test
    public void badge_waitlist_showsSpotsAvailable() {
        Event e = makeEvent("e1", "Summer Camp");
        e.setHasWaitlistLimit(true);
        e.setWaitlistLimit(10);
        e.setCurrentWaitlistCount(5);

        launchWithEvents(Arrays.asList(e));

        onView(withText("Spots Available")).check(matches(isDisplayed()));
    }

    /**
     * When an event's waitlist is at capacity, the "Waitlist Full" badge is shown.
     */
    @Test
    public void badge_waitlist_showsWaitlistFull() {
        Event e = makeEvent("e1", "Packed Event");
        e.setHasWaitlistLimit(true);
        e.setWaitlistLimit(10);
        e.setCurrentWaitlistCount(10);

        launchWithEvents(Arrays.asList(e));

        onView(withText("Waitlist Full")).check(matches(isDisplayed()));
    }

    /**
     * When an event has no waitlist limit, the waitlist badge must not be visible.
     */
    @Test
    public void badge_waitlist_isGone_whenNoWaitlistLimit() {
        Event e = makeEvent("e1", "Summer Camp");
        e.setHasWaitlistLimit(false);

        launchWithEvents(Arrays.asList(e));

        onView(withId(R.id.badge_waitlist)).check(matches(not(isDisplayed())));
    }

    // -----------------------------------------------------------------------
    // Date display tests
    // -----------------------------------------------------------------------

    /**
     * When an event has an {@code eventStartDate}, the date TextView must be visible.
     */
    @Test
    public void date_isDisplayed_whenEventHasStartDate() {
        Event e = makeEvent("e1", "Summer Camp");
        e.setEventStartDate(new Date());

        launchWithEvents(Arrays.asList(e));

        onView(withId(R.id.text_event_date)).check(matches(isDisplayed()));
    }

    /**
     * When an event has no {@code eventStartDate}, the date TextView must not be visible.
     */
    @Test
    public void date_isGone_whenEventHasNoStartDate() {
        launchWithEvents(Arrays.asList(makeEvent("e1", "Summer Camp")));

        onView(withId(R.id.text_event_date)).check(matches(not(isDisplayed())));
    }
}
