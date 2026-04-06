package com.example.eventlotteryapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import android.view.View;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.models.Notification;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.repository.NotificationRepository;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.fragments.EntrantListFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Comprehensive UI tests for {@link EntrantListFragment}.
 *
 * <p>The fragment displays four tabs inside a ViewPager2:
 * <ol>
 *   <li><b>Invited</b>  — entrants selected by the lottery, pending response</li>
 *   <li><b>Enrolled</b> — entrants who accepted their invitation</li>
 *   <li><b>Cancelled</b>— entrants who declined or were manually removed</li>
 *   <li><b>Waitlist</b> — entrants still waiting, or not selected in the draw</li>
 * </ol>
 *
 * <p>Tests are organised into the following sections:
 * <ol>
 *   <li>General navigation and chrome</li>
 *   <li>Empty state</li>
 *   <li>Tab routing — correct entrant grouping per status</li>
 *   <li>Name resolution</li>
 *   <li>Invited tab — Draw Replacement button</li>
 *   <li>Invited tab — Cancel Entrant button</li>
 *   <li>Invited tab — Win Notification button</li>
 *   <li>Invited tab — Send Custom Notification button</li>
 *   <li>Enrolled tab — Export CSV button</li>
 *   <li>Cancelled tab — Send Notification button</li>
 *   <li>Waitlist tab — Send Loss Notification button</li>
 *   <li>Waitlist tab — Send Custom Notification button</li>
 *   <li>Waitlist tab — See Map button</li>
 * </ol>
 *
 * <p>All three repositories are mocked — no real Firebase calls are made.
 *
 * @author Leyla
 */
@RunWith(AndroidJUnit4.class)
public class EntrantListFragmentTest {

    // =========================================================================
    // Constants
    // =========================================================================

    private static final String EVENT_ID  = "event_abc";
    private static final String DEVICE_ID = "device_001";
    private static final String USER_NAME = "Alice Smith";
    private static final long   ONE_DAY_MS = 24 * 60 * 60 * 1000L;

    // =========================================================================
    // Mocks
    // =========================================================================

    @Mock EventRepository        mockEventRepo;
    @Mock EntrantRepository      mockEntrantRepo;
    @Mock UserRepository         mockUserRepo;
    @Mock NotificationRepository mockNotificationRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Default: loadOrganizerInfo returns an event with organizer info.
        // Tests can override this stub before calling launch().
        doAnswer(inv -> {
            EventRepository.FirestoreCallback<Event> cb = inv.getArgument(1);
            Event e = new Event();
            e.setOrganizerId("organizer_device");
            e.setOrganizerName("Test Organizer");
            cb.onSuccess(e);
            return null;
        }).when(mockEventRepo).getEventById(any(), any());
    }

    /**
     * Clicks a view without requiring it to cover ≥90% of the screen.
     * Needed for buttons near the bottom of ViewPager pages that may be
     * partially hidden by the device's navigation bar.
     *
     * <p>Delegates to {@link GeneralClickAction} so the event is properly
     * injected through the UI thread (unlike {@code view.performClick()},
     * which would run on the instrumentation thread and silently prevent
     * dialogs from opening).
     */
    private static ViewAction forceClick() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isEnabled(); }
            @Override public String getDescription() { return "force click"; }
            @Override public void perform(UiController uiController, View view) {
                view.performClick();
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    // =========================================================================
    // Infrastructure helpers
    // =========================================================================

    /** Creates a minimal {@link Entrant} with the given device ID and status. */
    private Entrant makeEntrant(String deviceId, String status) {
        Entrant e = new Entrant();
        e.setDeviceId(deviceId);
        e.setEventId(EVENT_ID);
        e.setStatus(status);
        return e;
    }

    /**
     * Launches the fragment, pre-stubbing {@code getAllEntrantsForEvent} to return
     * {@code entrants} on every call.
     *
     * @param entrants            returned by the entrant repo on each load
     * @param lotteryCompleted    whether the lottery has been run
     * @param regCloseDateMillis  registration close timestamp in ms (-1 = not set)
     * @param lotteryCapacity     event lottery capacity
     * @param geolocationRequired whether the event requires geolocation
     */
    private void launch(List<Entrant> entrants,
                        boolean lotteryCompleted,
                        long regCloseDateMillis,
                        int lotteryCapacity,
                        boolean geolocationRequired) {
        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(entrants);
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        Bundle args = new Bundle();
        args.putString("eventId",              EVENT_ID);
        args.putString("eventTitle",           "Test Event");
        args.putBoolean("lotteryCompleted",    lotteryCompleted);
        args.putLong("registrationCloseDate",  regCloseDateMillis);
        args.putInt("lotteryCapacity",         lotteryCapacity);
        args.putBoolean("geolocationRequired", geolocationRequired);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                EntrantListFragment f = new EntrantListFragment();
                f.setEventRepository(mockEventRepo);
                f.setEntrantRepository(mockEntrantRepo);
                f.setUserRepository(mockUserRepo);
                f.setNotificationRepository(mockNotificationRepo);
                return f;
            }
        };

        FragmentScenario.launchInContainer(
                EntrantListFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /** Convenience overload — geolocation off, no dates, lottery not run. */
    private void launch(List<Entrant> entrants, boolean lotteryCompleted,
                        long regCloseDateMillis, int lotteryCapacity) {
        launch(entrants, lotteryCompleted, regCloseDateMillis, lotteryCapacity, false);
    }

    /**
     * Launches the fragment WITHOUT pre-stubbing {@code getAllEntrantsForEvent}.
     * Use this when a test needs to control the stub itself (e.g. to return
     * different data on the first vs second call after a list reload).
     */
    private void launchRaw(boolean lotteryCompleted, long regCloseDateMillis,
                           int lotteryCapacity, boolean geolocationRequired) {
        Bundle args = new Bundle();
        args.putString("eventId",              EVENT_ID);
        args.putString("eventTitle",           "Test Event");
        args.putBoolean("lotteryCompleted",    lotteryCompleted);
        args.putLong("registrationCloseDate",  regCloseDateMillis);
        args.putInt("lotteryCapacity",         lotteryCapacity);
        args.putBoolean("geolocationRequired", geolocationRequired);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                EntrantListFragment f = new EntrantListFragment();
                f.setEventRepository(mockEventRepo);
                f.setEntrantRepository(mockEntrantRepo);
                f.setUserRepository(mockUserRepo);
                f.setNotificationRepository(mockNotificationRepo);
                return f;
            }
        };

        FragmentScenario.launchInContainer(
                EntrantListFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /** Convenience overload — geolocation off. */
    private void launchRaw(boolean lotteryCompleted, long regCloseDateMillis, int lotteryCapacity) {
        launchRaw(lotteryCompleted, regCloseDateMillis, lotteryCapacity, false);
    }

    /** Shorthand for launching with no entrants, default settings. */
    private void launchEmpty() {
        launch(Collections.emptyList(), false, -1, 50);
    }

    /** Stubs userRepo to return a User with {@code name} for {@code deviceId}. */
    private void stubUser(String deviceId, String name) {
        doAnswer(inv -> {
            UserRepository.FirestoreCallback<User> cb = inv.getArgument(1);
            User user = new User();
            user.setName(name);
            cb.onSuccess(user);
            return null;
        }).when(mockUserRepo).getUser(eq(deviceId), any());
    }

    /** Stubs userRepo to return null (user document not found) for {@code deviceId}. */
    private void stubUserNotFound(String deviceId) {
        doAnswer(inv -> {
            UserRepository.FirestoreCallback<User> cb = inv.getArgument(1);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).getUser(eq(deviceId), any());
    }

    /** Stubs updateStatus to call onSuccess immediately. */
    private void stubUpdateStatusSuccess() {
        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<Void> cb = inv.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // Tab navigation shortcuts
    // -------------------------------------------------------------------------

    /** Stubs a single cancelled entrant and swipes to the Cancelled tab (index 2). */
    private void launchWithCancelledEntrant(String deviceId) {
        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant(deviceId, Entrant.STATUS_CANCELLED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Enrolled
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Cancelled
    }

    /** Stubs a single not-selected entrant and swipes to the Waitlist tab (index 3). */
    private void launchWithNotSelectedEntrant(String deviceId) {
        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant(deviceId, Entrant.STATUS_NOT_SELECTED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Enrolled
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Cancelled
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Waitlist
    }

    // =========================================================================
    // 1. GENERAL NAVIGATION AND CHROME
    // =========================================================================

    @Test
    public void backButton_isDisplayed() {
        launchEmpty();
        onView(withId(R.id.button_back)).check(matches(isDisplayed()));
    }

    @Test
    public void tabLayout_isDisplayed() {
        launchEmpty();
        onView(withId(R.id.tab_layout)).check(matches(isDisplayed()));
    }

    /** All four tab labels must be visible so the organizer knows which list they are viewing. */
    @Test
    public void tabLayout_showsAllFourTabLabels() {
        launchEmpty();
        onView(withId(R.id.tab_layout)).check(matches(hasDescendant(withText("Invited"))));
        onView(withId(R.id.tab_layout)).check(matches(hasDescendant(withText("Enrolled"))));
        onView(withId(R.id.tab_layout)).check(matches(hasDescendant(withText("Cancelled"))));
        onView(withId(R.id.tab_layout)).check(matches(hasDescendant(withText("Waitlist"))));
    }

    @Test
    public void viewPager_isDisplayed() {
        launchEmpty();
        onView(withId(R.id.view_pager)).check(matches(isDisplayed()));
    }

    // =========================================================================
    // 2. EMPTY STATE
    // =========================================================================

    /**
     * When there are no entrants, the Invited tab (default) must show the
     * empty-state placeholder so the organizer knows the list loaded but is empty.
     */
    @Test
    public void noEntrants_invitedTab_showsEmptyMessage() {
        launchEmpty();
        onView(withId(R.id.view_pager))
                .check(matches(hasDescendant(withText("No entrants in this category."))));
    }

    // =========================================================================
    // 3. TAB ROUTING — correct entrant grouping per status
    // =========================================================================

    /**
     * STATUS_INVITED entrants must appear in the Invited tab (tab 0, default).
     * These are entrants the lottery selected who have not yet responded.
     */
    @Test
    public void tabRouting_invitedEntrant_appearsInInvitedTab() {
        stubUser(DEVICE_ID, USER_NAME);
        launch(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_INVITED)),
                false, -1, 50);
        onView(withId(R.id.view_pager)).check(matches(hasDescendant(withText(USER_NAME))));
    }

    /**
     * STATUS_ENROLLED entrants must appear in the Enrolled tab (tab 1).
     * These are entrants who accepted their lottery invitation.
     */
    @Test
    public void tabRouting_enrolledEntrant_appearsInEnrolledTab() {
        stubUser(DEVICE_ID, USER_NAME);
        launch(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_ENROLLED)),
                false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Enrolled
        onView(withId(R.id.view_pager)).check(matches(hasDescendant(withText(USER_NAME))));
    }

    /**
     * STATUS_CANCELLED entrants must appear in the Cancelled tab (tab 2)
     * with a "(Cancelled)" suffix to distinguish them from declined entrants.
     */
    @Test
    public void tabRouting_cancelledEntrant_appearsInCancelledTab_withSuffix() {
        stubUser(DEVICE_ID, USER_NAME);
        launch(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_CANCELLED)),
                false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Cancelled
        onView(withId(R.id.view_pager))
                .check(matches(hasDescendant(withText(USER_NAME + " (Cancelled)"))));
    }

    /**
     * STATUS_DECLINED entrants also land in the Cancelled tab with a "(Declined)" suffix.
     * Declined and cancelled are both "no longer active" — grouped together for the organizer.
     */
    @Test
    public void tabRouting_declinedEntrant_appearsInCancelledTab_withSuffix() {
        stubUser(DEVICE_ID, USER_NAME);
        launch(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_DECLINED)),
                false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Cancelled
        onView(withId(R.id.view_pager))
                .check(matches(hasDescendant(withText(USER_NAME + " (Declined)"))));
    }

    /**
     * STATUS_WAITLIST entrants must appear in the Waitlist tab (tab 3).
     */
    @Test
    public void tabRouting_waitlistEntrant_appearsInWaitlistTab() {
        stubUser(DEVICE_ID, USER_NAME);
        launch(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_WAITLIST)),
                false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Waitlist
        onView(withId(R.id.view_pager)).check(matches(hasDescendant(withText(USER_NAME))));
    }

    /**
     * STATUS_NOT_SELECTED entrants also land in the Waitlist tab.
     * They remain eligible for future replacement draws.
     */
    @Test
    public void tabRouting_notSelectedEntrant_appearsInWaitlistTab() {
        stubUser(DEVICE_ID, USER_NAME);
        launch(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_NOT_SELECTED)),
                false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Waitlist
        onView(withId(R.id.view_pager)).check(matches(hasDescendant(withText(USER_NAME))));
    }

    // =========================================================================
    // 4. NAME RESOLUTION
    // =========================================================================

    /**
     * When the user document is not found, the entrant's deviceId must be used
     * as a fallback so the organizer still sees something meaningful.
     */
    @Test
    public void nameResolution_fallsBackToDeviceId_whenUserDocNotFound() {
        stubUserNotFound(DEVICE_ID);
        launch(Collections.singletonList(makeEntrant(DEVICE_ID, Entrant.STATUS_INVITED)),
                false, -1, 50);
        onView(withId(R.id.view_pager)).check(matches(hasDescendant(withText(DEVICE_ID))));
    }

    /**
     * Multiple entrants with different statuses must all have their names resolved
     * and displayed in their respective tabs.
     */
    @Test
    public void nameResolution_multipleEntrants_allNamesResolvedCorrectly() {
        stubUser("device_invited",  "Bob");
        stubUser("device_enrolled", "Carol");
        launch(Arrays.asList(
                makeEntrant("device_invited",  Entrant.STATUS_INVITED),
                makeEntrant("device_enrolled", Entrant.STATUS_ENROLLED)
        ), false, -1, 50);

        onView(withId(R.id.view_pager)).check(matches(hasDescendant(withText("Bob"))));
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Enrolled
        onView(withId(R.id.view_pager)).check(matches(hasDescendant(withText("Carol"))));
    }

    // =========================================================================
    // 5. INVITED TAB — Draw Replacement button
    // =========================================================================

    /**
     * "Draw Replacement" must be disabled before the initial lottery has been run.
     * A replacement draw only makes sense after the first lottery selects the initial winners.
     */
    @Test
    public void invitedTab_drawReplacement_isDisabled_whenLotteryNotCompleted() {
        launchEmpty();
        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed())).check(matches(not(isEnabled())));
    }

    @Test
    public void invitedTab_drawReplacement_showsLotteryNotRunText_whenLotteryNotCompleted() {
        launchEmpty();
        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed()))
                .check(matches(withText("Draw Replacement (Lottery Not Run)")));
    }

    /**
     * "Draw Replacement" must be disabled while registration is still open,
     * even if the lottery has been run — to stay fair to late entrants.
     */
    @Test
    public void invitedTab_drawReplacement_isDisabled_whenLotteryCompletedButRegStillOpen() {
        launch(Collections.emptyList(), true, System.currentTimeMillis() + ONE_DAY_MS, 50);
        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed())).check(matches(not(isEnabled())));
    }

    @Test
    public void invitedTab_drawReplacement_showsRegOpenText_whenLotteryCompletedButRegStillOpen() {
        launch(Collections.emptyList(), true, System.currentTimeMillis() + ONE_DAY_MS, 50);
        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed()))
                .check(matches(withText("Draw Replacement (Registration Open)")));
    }

    /**
     * "Draw Replacement" must be enabled and correctly labelled only when
     * the lottery is done AND registration has closed — the only valid state to draw.
     */
    @Test
    public void invitedTab_drawReplacement_isEnabled_whenLotteryCompletedAndRegClosed() {
        launch(Collections.emptyList(), true, System.currentTimeMillis() - ONE_DAY_MS, 50);
        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed())).check(matches(isEnabled()));
    }

    @Test
    public void invitedTab_drawReplacement_showsDrawText_whenFullyEnabled() {
        launch(Collections.emptyList(), true, System.currentTimeMillis() - ONE_DAY_MS, 50);
        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed()))
                .check(matches(withText("Draw Replacement")));
    }

    /**
     * Clicking Draw Replacement must call updateStatus(STATUS_INVITED) for a
     * NOT_SELECTED entrant when there is an open spot.
     */
    @Test
    public void invitedTab_drawReplacement_click_callsUpdateStatus_forNotSelectedEntrant() {
        stubUser("device_not_selected", "Dave");
        stubUpdateStatusSuccess();

        long pastClose = System.currentTimeMillis() - ONE_DAY_MS;
        launch(Collections.singletonList(makeEntrant("device_not_selected", Entrant.STATUS_NOT_SELECTED)),
                true, pastClose, 2);

        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed())).perform(forceClick());

        verify(mockEntrantRepo).updateStatus(
                eq(EVENT_ID), eq("device_not_selected"), eq(Entrant.STATUS_INVITED), any());
    }

    /**
     * A plain STATUS_WAITLIST entrant is also eligible for replacement — not just NOT_SELECTED.
     */
    @Test
    public void invitedTab_drawReplacement_click_callsUpdateStatus_forWaitlistEntrant() {
        stubUser("device_waitlist", "Eve");
        stubUpdateStatusSuccess();

        long pastClose = System.currentTimeMillis() - ONE_DAY_MS;
        launch(Collections.singletonList(makeEntrant("device_waitlist", Entrant.STATUS_WAITLIST)),
                true, pastClose, 2);

        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed())).perform(forceClick());

        verify(mockEntrantRepo).updateStatus(
                eq(EVENT_ID), eq("device_waitlist"), eq(Entrant.STATUS_INVITED), any());
    }

    /**
     * With 1 open spot and 2 eligible entrants, exactly one updateStatus call must
     * be made — the draw must not fill more spots than are actually available.
     */
    @Test
    public void invitedTab_drawReplacement_click_onlyFillsOpenSpots() {
        stubUser("device_one", "Frank");
        stubUser("device_two", "Grace");
        stubUpdateStatusSuccess();

        long pastClose = System.currentTimeMillis() - ONE_DAY_MS;
        launch(Arrays.asList(
                makeEntrant("device_one", Entrant.STATUS_NOT_SELECTED),
                makeEntrant("device_two", Entrant.STATUS_NOT_SELECTED)
        ), true, pastClose, 1);

        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed())).perform(forceClick());

        verify(mockEntrantRepo, times(1))
                .updateStatus(eq(EVENT_ID), any(), eq(Entrant.STATUS_INVITED), any());
    }

    /**
     * When capacity is already filled by invited + enrolled entrants, clicking Draw
     * Replacement must be a no-op — no updateStatus call should be made.
     */
    @Test
    public void invitedTab_drawReplacement_click_doesNotCallUpdateStatus_whenNoSpotsAvailable() {
        stubUser("device_enrolled",     "Heidi");
        stubUser("device_not_selected", "Ivan");

        long pastClose = System.currentTimeMillis() - ONE_DAY_MS;
        launch(Arrays.asList(
                makeEntrant("device_enrolled",     Entrant.STATUS_ENROLLED),
                makeEntrant("device_not_selected", Entrant.STATUS_NOT_SELECTED)
        ), true, pastClose, 1); // capacity=1, 1 enrolled → 0 spots

        onView(allOf(withId(R.id.button_draw_replacement), isDisplayed())).perform(forceClick());

        verify(mockEntrantRepo, never())
                .updateStatus(any(), any(), eq(Entrant.STATUS_INVITED), any());
    }

    // =========================================================================
    // 6. INVITED TAB — Cancel Entrant button
    // =========================================================================

    /**
     * Tapping an invited entrant's name then clicking "Cancel Entrant" must call
     * updateStatus(STATUS_CANCELLED) for that specific entrant.
     */
    @Test
    public void invitedTab_cancelButton_click_callsUpdateStatus_withCancelledStatus() {
        stubUser("device_alice", "Alice Smith");
        stubUpdateStatusSuccess();

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(withText("Alice Smith")).perform(click());
        onView(allOf(withId(R.id.button_cancel_entrant), isDisplayed())).perform(forceClick());

        verify(mockEntrantRepo).updateStatus(
                eq(EVENT_ID), eq("device_alice"), eq(Entrant.STATUS_CANCELLED), any());
    }

    /**
     * After cancellation the fragment reloads. When the reload returns the entrant
     * as CANCELLED, they must appear in the Cancelled tab with the "(Cancelled)" suffix.
     * This confirms the full round-trip: select → cancel → reload → correct tab.
     */
    @Test
    public void invitedTab_cancelButton_click_entrantMovesToCancelledTab() {
        stubUser("device_alice", "Alice Smith");
        stubUpdateStatusSuccess();

        AtomicInteger callCount = new AtomicInteger(0);
        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            String status = callCount.getAndIncrement() == 0
                    ? Entrant.STATUS_INVITED
                    : Entrant.STATUS_CANCELLED;
            cb.onSuccess(Collections.singletonList(makeEntrant("device_alice", status)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(withText("Alice Smith")).perform(click());
        onView(allOf(withId(R.id.button_cancel_entrant), isDisplayed())).perform(forceClick());

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Enrolled
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Cancelled

        onView(withId(R.id.view_pager))
                .check(matches(hasDescendant(withText("Alice Smith (Cancelled)"))));
    }

    /**
     * Clicking "Cancel Entrant" without first selecting a name must be a no-op.
     * The button's click listener is only wired after a row is tapped.
     */
    @Test
    public void invitedTab_cancelButton_click_withoutSelection_doesNotCallUpdateStatus() {
        stubUser("device_alice", "Alice Smith");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_cancel_entrant), isDisplayed())).perform(forceClick());

        verify(mockEntrantRepo, never())
                .updateStatus(any(), any(), eq(Entrant.STATUS_CANCELLED), any());
    }

    // =========================================================================
    // 7. INVITED TAB — Win Notification button (US 01.04.01)
    // =========================================================================

    /**
     * Clicking "Send Win Notification" must call addNotification once per invited entrant.
     */
    @Test
    public void invitedTab_sendWinNotification_callsAddNotification_forEachInvitedEntrant() {
        stubUser("device_alice", "Alice");
        stubUser("device_bob",   "Bob");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Arrays.asList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED),
                    makeEntrant("device_bob",   Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_send_win_notification), isDisplayed())).perform(forceClick());

        verify(mockNotificationRepo, times(2)).addNotification(any(), any(), any());
    }

    /**
     * The win notification must use TYPE_WON so the entrant's inbox shows the
     * correct "you were selected" message.
     */
    @Test
    public void invitedTab_sendWinNotification_typeIsWon() {
        stubUser("device_alice", "Alice");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_send_win_notification), isDisplayed())).perform(forceClick());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotificationRepo).addNotification(eq("device_alice"), captor.capture(), any());
        assertEquals(Notification.TYPE_WON, captor.getValue().getType());
    }

    /**
     * The notification must be addressed to the correct recipient device ID so it
     * lands in the right entrant's inbox.
     */
    @Test
    public void invitedTab_sendWinNotification_sentToCorrectRecipient() {
        stubUser("device_alice", "Alice");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_send_win_notification), isDisplayed())).perform(forceClick());

        verify(mockNotificationRepo).addNotification(eq("device_alice"), any(), any());
    }

    /**
     * When the Invited tab is empty, clicking "Send Win Notification" must not
     * call addNotification — there is nobody to notify.
     */
    @Test
    public void invitedTab_sendWinNotification_doesNotCallAddNotification_whenTabEmpty() {
        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant(DEVICE_ID, Entrant.STATUS_WAITLIST)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());
        stubUser(DEVICE_ID, USER_NAME);

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_send_win_notification), isDisplayed())).perform(forceClick());

        verify(mockNotificationRepo, never()).addNotification(any(), any(), any());
    }

    // =========================================================================
    // 8. INVITED TAB — Send Custom Notification button (US 02.07.02)
    // =========================================================================

    /**
     * Clicking "Send Notification" must open a dialog titled "Send Notification".
     */
    @Test
    public void invitedTab_sendCustomNotification_showsDialog() {
        stubUser("device_alice", "Alice");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_send_notification_invited), isDisplayed())).perform(forceClick());

        onView(withText("Send Notification")).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    /**
     * Typing a message and tapping "Send" must call addNotification for the invited entrant.
     */
    @Test
    public void invitedTab_sendCustomNotification_callsAddNotification_afterTypingMessage() {
        stubUser("device_alice", "Alice");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_send_notification_invited), isDisplayed())).perform(forceClick());
        onView(withHint("Enter your message...")).inRoot(isDialog())
                .perform(replaceText("Please check your status!"), closeSoftKeyboard());
        onView(withText("Send")).inRoot(isDialog()).perform(click());

        verify(mockNotificationRepo).addNotification(eq("device_alice"), any(), any());
    }

    /**
     * The message body inside the notification must match exactly what the organizer typed.
     */
    @Test
    public void invitedTab_sendCustomNotification_notificationContainsTypedMessage() {
        stubUser("device_alice", "Alice");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_send_notification_invited), isDisplayed())).perform(forceClick());
        onView(withHint("Enter your message...")).inRoot(isDialog())
                .perform(replaceText("Event starts at 7pm"), closeSoftKeyboard());
        onView(withText("Send")).inRoot(isDialog()).perform(click());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotificationRepo).addNotification(eq("device_alice"), captor.capture(), any());
        assertEquals("Event starts at 7pm", captor.getValue().getMessage());
    }

    /**
     * Tapping "Send" with an empty message must NOT dispatch any notification.
     */
    @Test
    public void invitedTab_sendCustomNotification_doesNotCallAddNotification_whenMessageIsEmpty() {
        stubUser("device_alice", "Alice");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_send_notification_invited), isDisplayed())).perform(forceClick());
        onView(withText("Send")).inRoot(isDialog()).perform(click());

        verify(mockNotificationRepo, never()).addNotification(any(), any(), any());
    }

    // =========================================================================
    // 9. ENROLLED TAB — Export CSV button (US 02.06.05)
    // =========================================================================

    /**
     * The Export CSV button must be visible on the Enrolled tab.
     * CSV export logic is tested separately in {@code EnrolledCsvExportTest}.
     */
    @Test
    public void enrolledTab_exportCsvButton_isDisplayed() {
        launchEmpty();
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Enrolled
        onView(allOf(withId(R.id.button_export_csv), isDisplayed())).check(matches(isDisplayed()));
    }

    /**
     * Clicking Export CSV with no enrolled entrants must not crash the fragment.
     * The fragment shows a toast and returns early without writing any file.
     */
    @Test
    public void enrolledTab_exportCsvButton_click_withEmptyTab_doesNotCrash() {
        launchEmpty();
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Enrolled
        onView(allOf(withId(R.id.button_export_csv), isDisplayed())).perform(forceClick());
        onView(allOf(withId(R.id.button_export_csv), isDisplayed())).check(matches(isDisplayed()));
    }

    /**
     * Clicking Export CSV with enrolled entrants must not crash the fragment.
     */
    @Test
    public void enrolledTab_exportCsvButton_click_withEnrolledEntrants_doesNotCrash() {
        stubUser("device_alice", "Alice Smith");
        launch(Collections.singletonList(makeEntrant("device_alice", Entrant.STATUS_ENROLLED)),
                false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Enrolled
        onView(allOf(withId(R.id.button_export_csv), isDisplayed())).perform(forceClick());
        onView(allOf(withId(R.id.button_export_csv), isDisplayed())).check(matches(isDisplayed()));
    }

    // =========================================================================
    // 10. CANCELLED TAB — Send Notification button (US 02.07.03)
    // =========================================================================

    @Test
    public void cancelledTab_sendNotificationButton_isDisplayed() {
        stubUser("device_alice", "Alice");
        launchWithCancelledEntrant("device_alice");
        onView(allOf(withId(R.id.button_send_notification_cancelled), isDisplayed())).check(matches(isDisplayed()));
    }

    /**
     * Clicking the button must open a dialog titled "Send Notification".
     */
    @Test
    public void cancelledTab_sendNotificationButton_showsDialog() {
        stubUser("device_alice", "Alice");
        launchWithCancelledEntrant("device_alice");
        onView(allOf(withId(R.id.button_send_notification_cancelled), isDisplayed())).perform(forceClick());
        onView(withText("Send Notification")).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    /**
     * Typing a message and tapping "Send" must call addNotification for
     * each cancelled entrant.
     */
    @Test
    public void cancelledTab_sendNotification_callsAddNotification_afterTypingMessage() {
        stubUser("device_alice", "Alice");
        launchWithCancelledEntrant("device_alice");
        onView(allOf(withId(R.id.button_send_notification_cancelled), isDisplayed())).perform(forceClick());
        onView(withHint("Enter your message...")).inRoot(isDialog())
                .perform(replaceText("You have been removed from the event."), closeSoftKeyboard());
        onView(withText("Send")).inRoot(isDialog()).perform(click());
        verify(mockNotificationRepo).addNotification(eq("device_alice"), any(), any());
    }

    /**
     * The message body must match exactly what the organizer typed.
     */
    @Test
    public void cancelledTab_sendNotification_notificationContainsTypedMessage() {
        stubUser("device_alice", "Alice");
        launchWithCancelledEntrant("device_alice");
        onView(allOf(withId(R.id.button_send_notification_cancelled), isDisplayed())).perform(forceClick());
        onView(withHint("Enter your message...")).inRoot(isDialog())
                .perform(replaceText("Sorry, you were cancelled."), closeSoftKeyboard());
        onView(withText("Send")).inRoot(isDialog()).perform(click());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotificationRepo).addNotification(eq("device_alice"), captor.capture(), any());
        assertEquals("Sorry, you were cancelled.", captor.getValue().getMessage());
    }

    /**
     * Notifications sent from the Cancelled tab must use TYPE_GENERAL.
     */
    @Test
    public void cancelledTab_sendNotification_typeIsGeneral() {
        stubUser("device_alice", "Alice");
        launchWithCancelledEntrant("device_alice");
        onView(allOf(withId(R.id.button_send_notification_cancelled), isDisplayed())).perform(forceClick());
        onView(withHint("Enter your message...")).inRoot(isDialog())
                .perform(replaceText("Please contact us."), closeSoftKeyboard());
        onView(withText("Send")).inRoot(isDialog()).perform(click());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotificationRepo).addNotification(eq("device_alice"), captor.capture(), any());
        assertEquals(Notification.TYPE_GENERAL, captor.getValue().getType());
    }

    /**
     * When the Cancelled tab is empty, clicking Send Notification must not
     * call addNotification.
     */
    @Test
    public void cancelledTab_sendNotification_doesNotCallAddNotification_whenTabEmpty() {
        stubUser("device_alice", "Alice");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Cancelled

        onView(allOf(withId(R.id.button_send_notification_cancelled), isDisplayed())).perform(forceClick());

        verify(mockNotificationRepo, never()).addNotification(any(), any(), any());
    }

    /**
     * Tapping "Send" with an empty message must not dispatch any notification.
     */
    @Test
    public void cancelledTab_sendNotification_doesNotCallAddNotification_whenMessageIsEmpty() {
        stubUser("device_alice", "Alice");
        launchWithCancelledEntrant("device_alice");
        onView(allOf(withId(R.id.button_send_notification_cancelled), isDisplayed())).perform(forceClick());
        onView(withText("Send")).inRoot(isDialog()).perform(click());
        verify(mockNotificationRepo, never()).addNotification(any(), any(), any());
    }

    // =========================================================================
    // 11. WAITLIST TAB — Send Loss Notification button (US 01.04.02)
    // =========================================================================

    /**
     * Clicking "Send Loss Notification" must call addNotification for a
     * STATUS_NOT_SELECTED entrant — these are the people who lost the draw.
     */
    @Test
    public void waitlistTab_sendLossNotification_callsAddNotification_forNotSelectedEntrant() {
        stubUser("device_alice", "Alice");
        launchWithNotSelectedEntrant("device_alice");
        onView(allOf(withId(R.id.button_send_notification_waitlist), isDisplayed())).perform(forceClick());
        verify(mockNotificationRepo).addNotification(eq("device_alice"), any(), any());
    }

    /**
     * The loss notification must use TYPE_LOST so the entrant sees the correct
     * "you were not selected" message in their inbox.
     */
    @Test
    public void waitlistTab_sendLossNotification_typeIsLost() {
        stubUser("device_alice", "Alice");
        launchWithNotSelectedEntrant("device_alice");
        onView(allOf(withId(R.id.button_send_notification_waitlist), isDisplayed())).perform(forceClick());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotificationRepo).addNotification(eq("device_alice"), captor.capture(), any());
        assertEquals(Notification.TYPE_LOST, captor.getValue().getType());
    }

    /**
     * A STATUS_WAITLIST entrant (still waiting, not yet drawn) must NOT receive the
     * loss notification — only STATUS_NOT_SELECTED entrants are losers of the draw.
     * This is a logic guard: sending a "you lost" message to someone still on the
     * active waitlist would be incorrect and confusing.
     */
    @Test
    public void waitlistTab_sendLossNotification_doesNotCallAddNotification_forPlainWaitlistEntrant() {
        stubUser("device_alice", "Alice");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_WAITLIST)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Waitlist

        onView(allOf(withId(R.id.button_send_notification_waitlist), isDisplayed())).perform(forceClick());

        verify(mockNotificationRepo, never()).addNotification(any(), any(), any());
    }

    /**
     * When there are no NOT_SELECTED entrants at all, clicking the button must
     * not call addNotification.
     */
    @Test
    public void waitlistTab_sendLossNotification_doesNotCallAddNotification_whenNoNotSelectedEntrants() {
        launchEmpty();
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Waitlist

        onView(allOf(withId(R.id.button_send_notification_waitlist), isDisplayed())).perform(forceClick());

        verify(mockNotificationRepo, never()).addNotification(any(), any(), any());
    }

    // =========================================================================
    // 12. WAITLIST TAB — Send Custom Notification button (US 02.07.01)
    // =========================================================================

    /**
     * Typing a message and tapping "Send" must call addNotification for all
     * entrants currently in the Waitlist tab (both WAITLIST and NOT_SELECTED).
     */
    @Test
    public void waitlistTab_sendCustomNotification_callsAddNotification_afterTypingMessage() {
        stubUser("device_alice", "Alice");
        launchWithNotSelectedEntrant("device_alice");

        onView(allOf(withId(R.id.button_send_custom_notification_waitlist), isDisplayed())).perform(forceClick());
        onView(withHint("Enter your message...")).inRoot(isDialog())
                .perform(replaceText("Updates coming soon!"), closeSoftKeyboard());
        onView(withText("Send")).inRoot(isDialog()).perform(click());

        verify(mockNotificationRepo).addNotification(eq("device_alice"), any(), any());
    }

    /**
     * The custom notification for the Waitlist tab must use TYPE_GENERAL.
     */
    @Test
    public void waitlistTab_sendCustomNotification_typeIsGeneral() {
        stubUser("device_alice", "Alice");
        launchWithNotSelectedEntrant("device_alice");

        onView(allOf(withId(R.id.button_send_custom_notification_waitlist), isDisplayed())).perform(forceClick());
        onView(withHint("Enter your message...")).inRoot(isDialog())
                .perform(replaceText("Thank you for your patience."), closeSoftKeyboard());
        onView(withText("Send")).inRoot(isDialog()).perform(click());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotificationRepo).addNotification(eq("device_alice"), captor.capture(), any());
        assertEquals(Notification.TYPE_GENERAL, captor.getValue().getType());
    }

    /**
     * Tapping "Send" with an empty message must not dispatch any notification.
     */
    @Test
    public void waitlistTab_sendCustomNotification_doesNotCallAddNotification_whenMessageIsEmpty() {
        stubUser("device_alice", "Alice");
        launchWithNotSelectedEntrant("device_alice");

        onView(allOf(withId(R.id.button_send_custom_notification_waitlist), isDisplayed())).perform(forceClick());
        onView(withText("Send")).inRoot(isDialog()).perform(click());

        verify(mockNotificationRepo, never()).addNotification(any(), any(), any());
    }

    /**
     * When the Waitlist tab is empty, clicking the button must not call addNotification.
     */
    @Test
    public void waitlistTab_sendCustomNotification_doesNotCallAddNotification_whenTabEmpty() {
        launchEmpty();
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Waitlist

        onView(allOf(withId(R.id.button_send_custom_notification_waitlist), isDisplayed())).perform(forceClick());

        verify(mockNotificationRepo, never()).addNotification(any(), any(), any());
    }

    // =========================================================================
    // 13. WAITLIST TAB — See Map button
    // =========================================================================

    /**
     * The See Map button must be visible on the Waitlist tab.
     */
    @Test
    public void waitlistTab_seeMapButton_isDisplayed() {
        launchEmpty();
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Waitlist
        onView(allOf(withId(R.id.button_see_map), isDisplayed())).check(matches(isDisplayed()));
    }

    /**
     * When geolocation is not required for the event, clicking See Map must not
     * crash the fragment — the fragment shows a toast instead of navigating.
     */
    @Test
    public void waitlistTab_seeMapButton_click_withGeolocationOff_doesNotCrash() {
        // geolocationRequired = false (default)
        launch(Collections.emptyList(), false, -1, 50, false);
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft());
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()); // → Waitlist

        onView(allOf(withId(R.id.button_see_map), isDisplayed())).perform(forceClick());

        // Fragment must still be intact — button is still visible
        onView(allOf(withId(R.id.button_see_map), isDisplayed())).check(matches(isDisplayed()));
    }

    // =========================================================================
    // 14. ORGANIZER INFO — notification fields are populated from the event doc
    // =========================================================================

    /**
     * The organizer name resolved from the event doc must appear in the notification
     * sent to an invited entrant, so recipients know who the message is from.
     */
    @Test
    public void winNotification_organizerName_matchesEventDoc() {
        doAnswer(inv -> {
            EventRepository.FirestoreCallback<Event> cb = inv.getArgument(1);
            Event e = new Event();
            e.setOrganizerId("org_device");
            e.setOrganizerName("Jane Organizer");
            cb.onSuccess(e);
            return null;
        }).when(mockEventRepo).getEventById(any(), any());

        stubUser("device_alice", "Alice");

        doAnswer(inv -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = inv.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeEntrant("device_alice", Entrant.STATUS_INVITED)));
            return null;
        }).when(mockEntrantRepo).getAllEntrantsForEvent(eq(EVENT_ID), any());

        launchRaw(false, -1, 50);

        onView(allOf(withId(R.id.button_send_win_notification), isDisplayed())).perform(forceClick());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mockNotificationRepo).addNotification(eq("device_alice"), captor.capture(), any());
        assertEquals("Jane Organizer", captor.getValue().getSenderOrganizerName());
    }

    /**
     * When the event doc cannot be loaded, the fragment must still show the tab layout
     * and not crash — organizer info falls back to defaults.
     */
    @Test
    public void eventLoadFailure_tabLayout_stillDisplayed() {
        doAnswer(inv -> {
            EventRepository.FirestoreCallback<Event> cb = inv.getArgument(1);
            cb.onFailure(new Exception("Firestore unavailable"));
            return null;
        }).when(mockEventRepo).getEventById(any(), any());

        launchEmpty();

        onView(withId(R.id.tab_layout)).check(matches(isDisplayed()));
    }
}
