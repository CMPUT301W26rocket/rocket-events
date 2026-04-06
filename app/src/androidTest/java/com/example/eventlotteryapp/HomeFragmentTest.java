package com.example.eventlotteryapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.ui.fragments.HomeFragment;

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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * UI tests for {@link HomeFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: header, search bar, buttons, and RecyclerView are visible on launch</li>
 *   <li>Data: event titles appear when the repository returns events</li>
 *   <li>Private events: events with {@code privateEvent=true} are excluded from the list</li>
 *   <li>Search: filtering by title, description, organizer name, and location</li>
 *   <li>Search — no results: "No results" text shown when nothing matches</li>
 *   <li>Filter chips: Available (default), Upcoming, Small, Medium, Large</li>
 *   <li>Chip mutual exclusion: availability chips deselect each other</li>
 *   <li>Badges: registration status ("Open", "Coming Soon", "Closed") and waitlist status</li>
 *   <li>Date: event start date is shown when set, hidden when absent</li>
 * </ul>
 *
 * <p><b>Important:</b> {@code chipAvailable} is selected by default on launch, which filters
 * out any event where {@link Event#isRegistrationOpen()} is false. Tests that need to display
 * events must use helpers that set valid registration dates:
 * <ul>
 *   <li>{@link #makeOpenEvent} — open now (open date in past, close date in future)</li>
 *   <li>{@link #makeUpcomingEvent} — not yet open (open date in future)</li>
 *   <li>{@link #makeClosedEvent} — registration ended (both dates in past)</li>
 * </ul>
 *
 * @author Leyla
 */
@RunWith(AndroidJUnit4.class)
public class HomeFragmentTest {

    private static final long DAY_MS = 86_400_000L;

    @Mock EventRepository mockEventRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Base event with no registration dates. Will be filtered out by the default chipAvailable. */
    private Event makeEvent(String id, String title) {
        Event e = new Event();
        e.setEventId(id);
        e.setTitle(title);
        e.setOrganizerName("Test Organizer");
        e.setDescription("A test event");
        e.setLocation("Test Location");
        return e;
    }

    /**
     * Open-registration event: openDate yesterday, closeDate tomorrow.
     * Passes the default chipAvailable filter.
     */
    private Event makeOpenEvent(String id, String title) {
        Event e = makeEvent(id, title);
        e.setRegistrationOpenDate(new Date(System.currentTimeMillis() - DAY_MS));
        e.setRegistrationCloseDate(new Date(System.currentTimeMillis() + DAY_MS));
        return e;
    }

    /**
     * Upcoming event: openDate tomorrow, closeDate the day after.
     * Passes chipUpcoming but NOT chipAvailable.
     */
    private Event makeUpcomingEvent(String id, String title) {
        Event e = makeEvent(id, title);
        e.setRegistrationOpenDate(new Date(System.currentTimeMillis() + DAY_MS));
        e.setRegistrationCloseDate(new Date(System.currentTimeMillis() + 2 * DAY_MS));
        return e;
    }

    /**
     * Closed event: both dates in the past.
     * Passes neither chipAvailable nor chipUpcoming; visible only when no availability chip is active.
     */
    private Event makeClosedEvent(String id, String title) {
        Event e = makeEvent(id, title);
        e.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 2 * DAY_MS));
        e.setRegistrationCloseDate(new Date(System.currentTimeMillis() - DAY_MS));
        return e;
    }

    /** Launches HomeFragment with the given event list returned immediately by the mock repo. */
    private void launch(List<Event> events) {
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<List<Event>> cb = invocation.getArgument(0);
            cb.onSuccess(events);
            return null;
        }).when(mockEventRepo).getAllEvents(any());

        Bundle args = new Bundle();
        args.putString("deviceId", "device123");

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                HomeFragment fragment = new HomeFragment();
                fragment.setEventRepository(mockEventRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                HomeFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    @Test
    public void header_isDisplayed() {
        launch(new ArrayList<>());
        onView(withId(R.id.text_events_header)).check(matches(isDisplayed()));
    }

    @Test
    public void searchBar_isDisplayed() {
        launch(new ArrayList<>());
        onView(withId(R.id.edit_search)).check(matches(isDisplayed()));
    }

    @Test
    public void createEventButton_isDisplayed() {
        launch(new ArrayList<>());
        onView(withId(R.id.button_open_create_event)).check(matches(isDisplayed()));
    }

    @Test
    public void qrScanButton_isDisplayed() {
        launch(new ArrayList<>());
        onView(withId(R.id.button_scan_qr)).check(matches(isDisplayed()));
    }

    @Test
    public void recyclerView_isDisplayed() {
        launch(new ArrayList<>());
        onView(withId(R.id.recycler_events)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Data tests
    // -----------------------------------------------------------------------

    @Test
    public void eventTitle_isDisplayed_whenRepositoryReturnsOneEvent() {
        launch(Arrays.asList(makeOpenEvent("e1", "Spring Festival")));
        onView(withText("Spring Festival")).check(matches(isDisplayed()));
    }

    @Test
    public void bothEventTitles_areDisplayed_whenRepositoryReturnsTwoEvents() {
        launch(Arrays.asList(
                makeOpenEvent("e1", "Spring Festival"),
                makeOpenEvent("e2", "Winter Gala")
        ));

        onView(withId(R.id.recycler_events)).perform(scrollToPosition(0));
        onView(withText("Spring Festival")).check(matches(isDisplayed()));

        onView(withId(R.id.recycler_events)).perform(scrollToPosition(1));
        onView(withText("Winter Gala")).check(matches(isDisplayed()));
    }

    @Test
    public void recyclerView_hasNoItems_whenRepositoryReturnsEmptyList() {
        launch(new ArrayList<>());
        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(0)));
    }

    // -----------------------------------------------------------------------
    // Private event
    // -----------------------------------------------------------------------

    /**
     * Events with {@code privateEvent=true} must be stripped out in {@code loadAllEvents}
     * and never appear in the list.
     */
    @Test
    public void privateEvent_isNotShownInList() {
        Event privateEvent = makeOpenEvent("e1", "Secret Gala");
        privateEvent.setPrivateEvent(true);

        launch(Arrays.asList(privateEvent, makeOpenEvent("e2", "Public Event")));

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Public Event")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Search tests
    // -----------------------------------------------------------------------

    /**
     * Typing a query matching the title shows only that event.
     */
    @Test
    public void search_byTitle_filtersResults() {
        launch(Arrays.asList(
                makeOpenEvent("e1", "Jazz Night"),
                makeOpenEvent("e2", "Rock Festival")
        ));

        onView(withId(R.id.edit_search)).perform(typeText("Jazz"), closeSoftKeyboard());

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Jazz Night")).check(matches(isDisplayed()));
    }

    /**
     * Typing a query matching the description (not the title) shows the event.
     */
    @Test
    public void search_byDescription_filtersResults() {
        Event e1 = makeOpenEvent("e1", "Mystery Event");
        e1.setDescription("An outdoor concert in the park");

        Event e2 = makeOpenEvent("e2", "Other Event");
        e2.setDescription("Indoor workshop");

        launch(Arrays.asList(e1, e2));

        onView(withId(R.id.edit_search)).perform(typeText("outdoor concert"), closeSoftKeyboard());

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Mystery Event")).check(matches(isDisplayed()));
    }

    /**
     * Typing a query matching the organizer name shows the event.
     */
    @Test
    public void search_byOrganizerName_filtersResults() {
        Event e1 = makeOpenEvent("e1", "Fundraiser");
        e1.setOrganizerName("Alice Smith");

        Event e2 = makeOpenEvent("e2", "Tech Talk");
        e2.setOrganizerName("Bob Jones");

        launch(Arrays.asList(e1, e2));

        onView(withId(R.id.edit_search)).perform(typeText("Alice"), closeSoftKeyboard());

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Fundraiser")).check(matches(isDisplayed()));
    }

    /**
     * Typing a query matching the location shows the event.
     */
    @Test
    public void search_byLocation_filtersResults() {
        Event e1 = makeOpenEvent("e1", "Downtown Fair");
        e1.setLocation("City Hall");

        Event e2 = makeOpenEvent("e2", "Campus Run");
        e2.setLocation("University Campus");

        launch(Arrays.asList(e1, e2));

        onView(withId(R.id.edit_search)).perform(typeText("City Hall"), closeSoftKeyboard());

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Downtown Fair")).check(matches(isDisplayed()));
    }

    /**
     * When the search query matches nothing, the "No results" text must be visible
     * and the RecyclerView must be empty.
     */
    @Test
    public void search_noMatch_showsNoResultsText() {
        launch(Arrays.asList(makeOpenEvent("e1", "Jazz Night")));

        onView(withId(R.id.edit_search)).perform(typeText("xyznothing"), closeSoftKeyboard());

        onView(withId(R.id.text_no_results)).check(matches(isDisplayed()));
        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(0)));
    }

    /**
     * When the event list is not empty and no query is entered, the "No results"
     * text must not be visible.
     */
    @Test
    public void noResultsText_isHidden_whenEventsExist() {
        launch(Arrays.asList(makeOpenEvent("e1", "Jazz Night")));

        onView(withId(R.id.text_no_results)).check(matches(not(isDisplayed())));
    }

    // -----------------------------------------------------------------------
    // Filter chip tests
    // -----------------------------------------------------------------------

    /**
     * The "Available" chip is selected by default. An upcoming event (not yet open)
     * must be filtered out while only the open event is shown.
     */
    @Test
    public void filter_available_isDefaultAndShowsOnlyOpenEvents() {
        launch(Arrays.asList(
                makeOpenEvent("e1", "Open Event"),
                makeUpcomingEvent("e2", "Upcoming Event")
        ));

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Open Event")).check(matches(isDisplayed()));
    }

    /**
     * Clicking "Upcoming" deselects "Available" and shows only upcoming events.
     */
    @Test
    public void filter_upcoming_showsOnlyUpcomingEvents() {
        launch(Arrays.asList(
                makeOpenEvent("e1", "Open Event"),
                makeUpcomingEvent("e2", "Upcoming Event")
        ));

        onView(withId(R.id.chip_upcoming)).perform(click());

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Upcoming Event")).check(matches(isDisplayed()));
    }

    /**
     * Availability chips are mutually exclusive: clicking "Upcoming" hides the open event
     * that was visible under the default "Available" selection.
     */
    @Test
    public void availabilityChips_areMutuallyExclusive() {
        launch(Arrays.asList(
                makeOpenEvent("e1", "Open Event"),
                makeUpcomingEvent("e2", "Upcoming Event")
        ));

        // Default: chipAvailable → only open event visible
        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));

        // Switch to chipUpcoming → only upcoming event visible
        onView(withId(R.id.chip_upcoming)).perform(click());
        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Upcoming Event")).check(matches(isDisplayed()));
    }

    /**
     * The "Small" chip shows only events with lottery capacity 1–20.
     */
    @Test
    public void filter_small_showsCapacity1To20() {
        Event small = makeOpenEvent("e1", "Small Event");
        small.setLotteryCapacity(10);

        Event large = makeOpenEvent("e2", "Large Event");
        large.setLotteryCapacity(100);

        launch(Arrays.asList(small, large));

        onView(withId(R.id.chip_small)).perform(click());

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Small Event")).check(matches(isDisplayed()));
    }

    /**
     * The "Medium" chip shows only events with lottery capacity 21–50.
     */
    @Test
    public void filter_medium_showsCapacity21To50() {
        Event small = makeOpenEvent("e1", "Small Event");
        small.setLotteryCapacity(10);

        Event medium = makeOpenEvent("e2", "Medium Event");
        medium.setLotteryCapacity(30);

        launch(Arrays.asList(small, medium));

        onView(withId(R.id.chip_medium)).perform(scrollTo(), click());

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Medium Event")).check(matches(isDisplayed()));
    }

    /**
     * The "Large" chip shows only events with lottery capacity above 50.
     */
    @Test
    public void filter_large_showsCapacityOver50() {
        Event small = makeOpenEvent("e1", "Small Event");
        small.setLotteryCapacity(20);

        Event large = makeOpenEvent("e2", "Large Event");
        large.setLotteryCapacity(200);

        launch(Arrays.asList(small, large));

        onView(withId(R.id.chip_large)).perform(scrollTo(), click());

        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(1)));
        onView(withText("Large Event")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Badge (tag) tests
    // -----------------------------------------------------------------------

    /**
     * An open-registration event displays the "Open" registration badge.
     */
    @Test
    public void badge_registration_showsOpen_forOpenEvent() {
        launch(Arrays.asList(makeOpenEvent("e1", "Spring Festival")));

        onView(withText("Open")).check(matches(isDisplayed()));
    }

    /**
     * An upcoming event displays the "Coming Soon" registration badge.
     * chipUpcoming is selected first so the event passes the availability filter.
     */
    @Test
    public void badge_registration_showsComingSoon_forUpcomingEvent() {
        launch(Arrays.asList(makeUpcomingEvent("e1", "Future Fest")));

        onView(withId(R.id.chip_upcoming)).perform(click());

        onView(withText("Coming Soon")).check(matches(isDisplayed()));
    }

    /**
     * A closed-registration event displays the "Closed" registration badge.
     * chipAvailable is deselected first (by clicking it) so the closed event is not filtered out.
     */
    @Test
    public void badge_registration_showsClosed_forClosedEvent() {
        launch(Arrays.asList(makeClosedEvent("e1", "Past Event")));

        // Deselect the default chipAvailable so the closed event is not filtered out
        onView(withId(R.id.chip_available)).perform(click());

        onView(withText("Closed")).check(matches(isDisplayed()));
    }

    /**
     * When an event has a waitlist limit and spots remain, the "Spots Available" badge is shown.
     */
    @Test
    public void badge_waitlist_showsSpotsAvailable() {
        Event e = makeOpenEvent("e1", "Spring Festival");
        e.setHasWaitlistLimit(true);
        e.setWaitlistLimit(10);
        e.setCurrentWaitlistCount(5);

        launch(Arrays.asList(e));

        onView(withText("Spots Available")).check(matches(isDisplayed()));
    }

    /**
     * When an event's waitlist is at capacity, the "Waitlist Full" badge is shown.
     */
    @Test
    public void badge_waitlist_showsWaitlistFull() {
        Event e = makeOpenEvent("e1", "Packed Event");
        e.setHasWaitlistLimit(true);
        e.setWaitlistLimit(10);
        e.setCurrentWaitlistCount(10);

        launch(Arrays.asList(e));

        onView(withText("Waitlist Full")).check(matches(isDisplayed()));
    }

    /**
     * When an event has no waitlist limit, the waitlist badge must not be visible.
     */
    @Test
    public void badge_waitlist_isGone_whenNoWaitlistLimit() {
        Event e = makeOpenEvent("e1", "Spring Festival");
        e.setHasWaitlistLimit(false);

        launch(Arrays.asList(e));

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
        Event e = makeOpenEvent("e1", "Spring Festival");
        e.setEventStartDate(new Date());

        launch(Arrays.asList(e));

        onView(withId(R.id.text_event_date)).check(matches(isDisplayed()));
    }

    /**
     * When an event has no {@code eventStartDate}, the date TextView must be gone.
     */
    @Test
    public void date_isGone_whenEventHasNoStartDate() {
        Event e = makeOpenEvent("e1", "Spring Festival");
        // eventStartDate intentionally not set

        launch(Arrays.asList(e));

        onView(withId(R.id.text_event_date)).check(matches(not(isDisplayed())));
    }
}
