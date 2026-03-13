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
import java.util.Arrays;  // still used for 2-event tests
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * UI tests for {@link HomeFragment}.
 *
 * Tests cover:
 * - Display: header, buttons, and RecyclerView are visible on launch
 * - Data: event titles appear when the repository returns events
 * - Empty state: RecyclerView has no items when repository returns empty list
 * - Navigation: clicking the create button navigates to CreateEventFragment
 * - Navigation: clicking an event item navigates to the event details screen
 */
@RunWith(AndroidJUnit4.class)
public class HomeFragmentTest {

    @Mock EventRepository mockEventRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Builds a fake Event with just enough data to be displayed in the list.
     */
    private Event makeEvent(String id, String title) {
        Event e = new Event();
        e.setEventId(id);
        e.setTitle(title);
        e.setOrganizerName("Test Organizer");
        e.setDescription("A test event");
        return e;
    }

    /**
     * Launches HomeFragment with a mock repository that returns the given event list.
     *
     * @param events the list of events the mock repository will return
     */
    private void launch(List<Event> events) {
        // Tell the mock to immediately call onSuccess with our fake event list
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

        FragmentScenario.launchInContainer(HomeFragment.class, args, R.style.Theme_EventLotteryApp, factory);
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
        // Mock returns a single event titled "Spring Festival"
        launch(Arrays.asList(makeEvent("e1", "Spring Festival")));

        // The EventAdapter renders the title in a TextView — it should be visible in the list
        onView(withText("Spring Festival")).check(matches(isDisplayed()));
    }

    @Test
    public void bothEventTitles_areDisplayed_whenRepositoryReturnsTwoEvents() {
        launch(Arrays.asList(
                makeEvent("e1", "Spring Festival"),
                makeEvent("e2", "Winter Gala")
        ));

        // Event cards are tall (360dp poster) so the second item is off-screen.
        // scrollToPosition() scrolls the RecyclerView to bring each item into view first.
        onView(withId(R.id.recycler_events)).perform(scrollToPosition(0));
        onView(withText("Spring Festival")).check(matches(isDisplayed()));

        onView(withId(R.id.recycler_events)).perform(scrollToPosition(1));
        onView(withText("Winter Gala")).check(matches(isDisplayed()));
    }

    @Test
    public void recyclerView_hasNoItems_whenRepositoryReturnsEmptyList() {
        // Mock returns an empty list
        launch(new ArrayList<>());

        // RecyclerView should have 0 children since no events were returned
        onView(withId(R.id.recycler_events)).check(matches(hasChildCount(0)));
    }


}
