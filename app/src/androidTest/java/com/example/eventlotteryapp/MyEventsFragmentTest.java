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
 * UI tests for {@link MyEventsFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: header and RecyclerView are visible on launch</li>
 *   <li>Data: event titles appear when the repository returns events</li>
 *   <li>Empty state: RecyclerView shows no items when the list is empty</li>
 *   <li>Correctness: repository is called with the deviceId passed via arguments</li>
 *   <li>Failure: RecyclerView remains empty when the repository reports an error</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class MyEventsFragmentTest {

    private static final String DEVICE_ID = "device123";

    @Mock EventRepository mockEventRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Builds a fake Event with just enough data to display in the list.
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
}
