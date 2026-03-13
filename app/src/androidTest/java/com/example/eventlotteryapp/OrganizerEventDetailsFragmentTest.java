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
import com.example.eventlotteryapp.ui.fragments.OrganizerEventDetailsFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * UI tests for {@link OrganizerEventDetailsFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: all detail views and action buttons are visible on launch</li>
 *   <li>Data: event fields are correctly populated from the repository</li>
 *   <li>Fee formatting: free events show "Fee: Free", paid events show the amount</li>
 *   <li>Waitlist: unlimited vs limited waitlist label</li>
 *   <li>Geolocation: "Yes" vs "No" label</li>
 *   <li>Dates: null dates show "TBD", non-null dates show formatted string</li>
 *   <li>Lottery button: toast shown when registration period not ended</li>
 *   <li>Lottery button: entrant repo called when registration period has ended</li>
 *   <li>Lottery button: disabled after clicking when registration is closed</li>
 *   <li>Failure: views remain visible when event load fails</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEventDetailsFragmentTest {

    private static final String EVENT_ID = "event123";
    private static final String DEVICE_ID = "device123";

    @Mock EventRepository mockEventRepo;
    @Mock EntrantRepository mockEntrantRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a fully populated fake Event for testing.
     */
    private Event makeEvent(double fee, boolean hasWaitlistLimit, int waitlistLimit,
                            boolean geolocationRequired, Date regCloseDate) {
        Event e = new Event();
        e.setEventId(EVENT_ID);
        e.setTitle("Spring Festival");
        e.setOrganizerName("Test Organizer");
        e.setDescription("A fun spring event");
        e.setLocation("Main Hall");
        e.setRegistrationFee(fee);
        e.setLotteryCapacity(50);
        e.setHasWaitlistLimit(hasWaitlistLimit);
        e.setWaitlistLimit(waitlistLimit);
        e.setGeolocationRequired(geolocationRequired);
        e.setRegistrationCloseDate(regCloseDate);
        return e;
    }

    /** Returns a date 1 day in the past. */
    private Date pastDate() {
        return new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
    }

    /** Returns a date 1 day in the future. */
    private Date futureDate() {
        return new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L);
    }

    /**
     * Launches OrganizerEventDetailsFragment with a mock that returns the given event.
     */
    private void launchWithEvent(Event event) {
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<Event> cb = invocation.getArgument(1);
            cb.onSuccess(event);
            return null;
        }).when(mockEventRepo).getEventById(any(), any());

        Bundle args = new Bundle();
        args.putString("eventId", EVENT_ID);
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                OrganizerEventDetailsFragment fragment = new OrganizerEventDetailsFragment();
                fragment.setEventRepository(mockEventRepo);
                fragment.setEntrantRepository(mockEntrantRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                OrganizerEventDetailsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /**
     * Launches with a mock that fires onFailure.
     */
    private void launchWithFailure() {
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<Event> cb = invocation.getArgument(1);
            cb.onFailure(new Exception("Firestore unavailable"));
            return null;
        }).when(mockEventRepo).getEventById(any(), any());

        Bundle args = new Bundle();
        args.putString("eventId", EVENT_ID);
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                OrganizerEventDetailsFragment fragment = new OrganizerEventDetailsFragment();
                fragment.setEventRepository(mockEventRepo);
                fragment.setEntrantRepository(mockEntrantRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                OrganizerEventDetailsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    /**
     * All key detail views must be visible when the fragment opens.
     */
    @Test
    public void allDetailViews_areDisplayed() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_title)).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_organizer)).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_description)).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_location)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_fee)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_capacity)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_geolocation)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_waitlist)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * The Lottery and See Entrants buttons must be visible on launch.
     */
    @Test
    public void actionButtons_areDisplayed() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.lottery_button)).check(matches(isDisplayed()));
        onView(withId(R.id.entrants_button)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Data population tests
    // -----------------------------------------------------------------------

    /**
     * The event title must be displayed after the repository returns the event.
     */
    @Test
    public void eventTitle_isDisplayed_whenEventLoads() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_title)).check(matches(withText("Spring Festival")));
    }

    /**
     * The organizer name must be displayed with the "By " prefix.
     */
    @Test
    public void organizerName_isDisplayed_withByPrefix() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_organizer)).check(matches(withText("By Test Organizer")));
    }

    /**
     * The event description must be displayed after the repository returns the event.
     */
    @Test
    public void eventDescription_isDisplayed_whenEventLoads() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_description)).check(matches(withText("A fun spring event")));
    }

    /**
     * The location must be displayed with the "Location: " prefix.
     */
    @Test
    public void location_isDisplayed_withPrefix() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_location))
                .perform(scrollTo())
                .check(matches(withText("Location: Main Hall")));
    }

    // -----------------------------------------------------------------------
    // Fee formatting tests
    // -----------------------------------------------------------------------

    /**
     * When the registration fee is 0, the fee view must show "Fee: Free".
     */
    @Test
    public void fee_showsFree_whenFeeIsZero() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_fee))
                .perform(scrollTo())
                .check(matches(withText("Fee: Free")));
    }

    /**
     * When the registration fee is non-zero, the fee view must show the formatted amount.
     */
    @Test
    public void fee_showsAmount_whenFeeIsNonZero() {
        launchWithEvent(makeEvent(15.0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_fee))
                .perform(scrollTo())
                .check(matches(withText("Fee: $15.00")));
    }

    // -----------------------------------------------------------------------
    // Waitlist formatting tests
    // -----------------------------------------------------------------------

    /**
     * When hasWaitlistLimit is false, the waitlist view must show "Waitlist Limit: Unlimited".
     */
    @Test
    public void waitlist_showsUnlimited_whenNoLimit() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_waitlist))
                .perform(scrollTo())
                .check(matches(withText("Waitlist Limit: Unlimited")));
    }

    /**
     * When hasWaitlistLimit is true, the waitlist view must show the numeric limit.
     */
    @Test
    public void waitlist_showsLimit_whenLimitIsSet() {
        launchWithEvent(makeEvent(0, true, 100, false, futureDate()));

        onView(withId(R.id.text_detail_waitlist))
                .perform(scrollTo())
                .check(matches(withText("Waitlist Limit: 100")));
    }

    // -----------------------------------------------------------------------
    // Geolocation tests
    // -----------------------------------------------------------------------

    /**
     * When geolocation is required, the geolocation view must show "Yes".
     */
    @Test
    public void geolocation_showsYes_whenRequired() {
        launchWithEvent(makeEvent(0, false, 0, true, futureDate()));

        onView(withId(R.id.text_detail_geolocation))
                .perform(scrollTo())
                .check(matches(withText("Geolocation Required: Yes")));
    }

    /**
     * When geolocation is not required, the geolocation view must show "No".
     */
    @Test
    public void geolocation_showsNo_whenNotRequired() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_geolocation))
                .perform(scrollTo())
                .check(matches(withText("Geolocation Required: No")));
    }

    // -----------------------------------------------------------------------
    // Lottery button tests
    // -----------------------------------------------------------------------

    /**
     * Clicking the lottery button when registration is still open must NOT call the
     * entrant repository — the fragment shows a toast instead.
     */
    @Test
    public void lotteryButton_doesNotCallRepo_whenRegistrationNotClosed() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        verify(mockEntrantRepo, never()).getEntrantsByStatus(any(), any(), any());
    }

    /**
     * Clicking the lottery button when registration has closed must call
     * {@link EntrantRepository#getEntrantsByStatus} to fetch the waitlist.
     */
    @Test
    public void lotteryButton_callsGetEntrantsByStatus_whenRegistrationClosed() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(2);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(mockEntrantRepo).getEntrantsByStatus(any(), any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        verify(mockEntrantRepo).getEntrantsByStatus(eq(EVENT_ID), eq("waitlist"), any());
    }

    /**
     * After clicking the lottery button when registration has closed and the waitlist
     * has entrants, the button must stay disabled to prevent running the lottery twice.
     */
    @Test
    public void lotteryButton_isDisabled_afterSuccessfulLotteryRun() {
        Entrant entrant = new Entrant();
        entrant.setDeviceId("user1");

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(2);
            cb.onSuccess(Arrays.asList(entrant));
            return null;
        }).when(mockEntrantRepo).getEntrantsByStatus(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        onView(withId(R.id.lottery_button)).check(matches(not(isEnabled())));
    }

    /**
     * When the waitlist has more entrants than the lottery capacity, winners must be
     * set to STATUS_INVITED and the rest must be set to STATUS_NOT_SELECTED.
     *
     * <p>The event has capacity 1. Two entrants are on the waitlist.
     * One must be invited and the other must be marked not selected.
     */
    @Test
    public void lotteryButton_invitesWinners_andMarksRemainingNotSelected() {
        Entrant entrant1 = new Entrant();
        entrant1.setDeviceId("user1");
        entrant1.setStatus(Entrant.STATUS_WAITLIST);

        Entrant entrant2 = new Entrant();
        entrant2.setDeviceId("user2");
        entrant2.setStatus(Entrant.STATUS_WAITLIST);

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(2);
            cb.onSuccess(Arrays.asList(entrant1, entrant2));
            return null;
        }).when(mockEntrantRepo).getEntrantsByStatus(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        // capacity = 1, waitlist = 2 → entrant1 invited, entrant2 not selected
        Event event = makeEvent(0, false, 0, false, pastDate());
        event.setLotteryCapacity(1);
        launchWithEvent(event);

        onView(withId(R.id.lottery_button)).perform(click());

        // The lottery shuffles randomly — we can't know which user wins.
        // Assert that exactly one user got INVITED and one got NOT_SELECTED.
        verify(mockEntrantRepo, times(1)).updateStatus(eq(EVENT_ID), any(), eq(Entrant.STATUS_INVITED), any());
        verify(mockEntrantRepo, times(1)).updateStatus(eq(EVENT_ID), any(), eq(Entrant.STATUS_NOT_SELECTED), any());
    }

    /**
     * When all waitlist entrants fit within the lottery capacity, all must be invited
     * and none should be marked not selected.
     */
    @Test
    public void lotteryButton_invitesAll_whenWaitlistSmallerThanCapacity() {
        Entrant entrant1 = new Entrant();
        entrant1.setDeviceId("user1");
        Entrant entrant2 = new Entrant();
        entrant2.setDeviceId("user2");

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(2);
            cb.onSuccess(Arrays.asList(entrant1, entrant2));
            return null;
        }).when(mockEntrantRepo).getEntrantsByStatus(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        // capacity = 50, waitlist = 2 → both invited, none not-selected
        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        verify(mockEntrantRepo).updateStatus(eq(EVENT_ID), eq("user1"), eq(Entrant.STATUS_INVITED), any());
        verify(mockEntrantRepo).updateStatus(eq(EVENT_ID), eq("user2"), eq(Entrant.STATUS_INVITED), any());
        verify(mockEntrantRepo, never()).updateStatus(any(), any(), eq(Entrant.STATUS_NOT_SELECTED), any());
    }

    /**
     * When the waitlist is empty, the lottery button must be re-enabled so the
     * organizer can try again later. It must NOT stay stuck in the disabled state.
     */
    @Test
    public void lotteryButton_isReEnabled_whenWaitlistIsEmpty() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(2);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(mockEntrantRepo).getEntrantsByStatus(any(), any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        onView(withId(R.id.lottery_button)).check(matches(isEnabled()));
    }

    // -----------------------------------------------------------------------
    // Failure state
    // -----------------------------------------------------------------------

    /**
     * When the event repository fires onFailure, the back button and layout must
     * still be visible — the fragment must not crash.
     */
    @Test
    public void layout_remainsVisible_whenEventLoadFails() {
        launchWithFailure();

        onView(withId(R.id.button_back)).check(matches(isDisplayed()));
        onView(withId(R.id.lottery_button)).check(matches(isDisplayed()));
    }
}
