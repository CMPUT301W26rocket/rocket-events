package com.example.eventlotteryapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Comment;
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.CommentRepository;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.repository.UserRepository;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.eventlotteryapp.ui.fragments.OrganizerEventDetailsFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import androidx.test.espresso.matcher.ViewMatchers;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
 *   <li>Display: detail views, all action buttons visible on launch</li>
 *   <li>Data: event fields populated correctly from the repository</li>
 *   <li>Fee / waitlist / geolocation formatting</li>
 *   <li>Private event: "Invite to Waitlist" button shown only for private events</li>
 *   <li>Lottery button states: Lottery Completed, Not Open Yet, Open/Pending, Draw Lottery</li>
 *   <li>Lottery logic: winners get STATUS_INVITED, rest get STATUS_NOT_SELECTED,
 *       capacity vs waitlist size is respected, empty waitlist re-enables button</li>
 *   <li>Lottery completion: {@code updateLotteryCompleted} called, button text updated</li>
 *   <li>Comments: rendered with author + text, delete button visible, posting calls repo,
 *       empty post blocked, send button state, input cleared after success</li>
 *   <li>Failure: layout remains visible when event load fails</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEventDetailsFragmentTest {

    private static final String EVENT_ID  = "event123";
    private static final String DEVICE_ID = "device123";
    private static final long   DAY_MS    = 86_400_000L;

    @Mock EventRepository       mockEventRepo;
    @Mock EntrantRepository     mockEntrantRepo;
    @Mock CommentRepository     mockCommentRepo;
    @Mock UserRepository        mockUserRepo;
    @Mock ListenerRegistration  mockListenerReg;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Default: no comments
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<List<Comment>> cb = invocation.getArgument(1);
            cb.onSuccess(new ArrayList<>());
            return mockListenerReg;
        }).when(mockCommentRepo).listenToComments(any(), any());

        // Default: no current user name resolved
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<User> cb = invocation.getArgument(1);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).getUser(any(), any());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Base event builder. regCloseDate controls when registration ends. */
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

    /** Event whose registration is currently open (open date past, close date future). */
    private Event makeOpenRegistrationEvent() {
        Event e = makeEvent(0, false, 0, false, new Date(System.currentTimeMillis() + DAY_MS));
        e.setRegistrationOpenDate(new Date(System.currentTimeMillis() - DAY_MS));
        return e;
    }

    /** Event whose registration has not opened yet (open date future). */
    private Event makeUpcomingEvent() {
        Event e = makeEvent(0, false, 0, false,
                new Date(System.currentTimeMillis() + 3 * DAY_MS));
        e.setRegistrationOpenDate(new Date(System.currentTimeMillis() + 2 * DAY_MS));
        return e;
    }

    /** Event with lottery already completed. */
    private Event makeCompletedLotteryEvent() {
        Event e = makeEvent(0, false, 0, false, pastDate());
        e.setLotteryCompleted(true);
        return e;
    }

    private Date pastDate()   { return new Date(System.currentTimeMillis() - DAY_MS); }
    private Date futureDate() { return new Date(System.currentTimeMillis() + DAY_MS); }

    private Comment makeComment(String id, String authorName, String text) {
        Comment c = new Comment();
        c.setCommentId(id);
        c.setAuthorName(authorName);
        c.setText(text);
        return c;
    }

    private Entrant makeEntrant(String deviceId) {
        Entrant e = new Entrant();
        e.setDeviceId(deviceId);
        e.setStatus(Entrant.STATUS_WAITLIST);
        return e;
    }

    /**
     * Launches OrganizerEventDetailsFragment with all mocked repositories.
     * Individual tests may re-stub {@code listenToComments} or {@code getUser}
     * before calling this method to override the setUp defaults.
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
                fragment.setCommentRepository(mockCommentRepo);
                fragment.setUserRepository(mockUserRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                OrganizerEventDetailsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

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
                fragment.setCommentRepository(mockCommentRepo);
                fragment.setUserRepository(mockUserRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                OrganizerEventDetailsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /** Stubs getEntrantsByStatus + updateStatus for lottery tests. */
    private void stubWaitlist(List<Entrant> waitlist) {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(2);
            cb.onSuccess(waitlist);
            return null;
        }).when(mockEntrantRepo).getEntrantsByStatus(any(), any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    @Test
    public void allDetailViews_areDisplayed() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.text_detail_title)).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_organizer)).check(matches(isDisplayed()));
        // description is below the poster, needs scrolling
        onView(withId(R.id.text_detail_description)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_location)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_fee)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_capacity)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_geolocation)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.text_detail_waitlist)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void allActionButtons_areDisplayed() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        // lottery_button, entrants_button, co_organizer_button are fixed below the ScrollView —
        // scrollTo() does not work on them; use withEffectiveVisibility instead.
        onView(withId(R.id.lottery_button))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.entrants_button))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.co_organizer_button))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        // button_update_poster is inside the ScrollView — scrollTo is fine here.
        onView(withId(R.id.button_update_poster)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void commentSection_isDisplayed() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.edit_comment_input)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.button_send_comment)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Data population tests
    // -----------------------------------------------------------------------

    @Test
    public void eventTitle_isDisplayed_whenEventLoads() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));
        onView(withId(R.id.text_detail_title)).check(matches(withText("Spring Festival")));
    }

    @Test
    public void organizerName_isDisplayed_withByPrefix() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));
        onView(withId(R.id.text_detail_organizer)).check(matches(withText("By Test Organizer")));
    }

    @Test
    public void eventDescription_isDisplayed_whenEventLoads() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));
        onView(withId(R.id.text_detail_description)).check(matches(withText("A fun spring event")));
    }

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

    @Test
    public void fee_showsFree_whenFeeIsZero() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));
        onView(withId(R.id.text_detail_fee))
                .perform(scrollTo()).check(matches(withText("Fee: Free")));
    }

    @Test
    public void fee_showsAmount_whenFeeIsNonZero() {
        launchWithEvent(makeEvent(15.0, false, 0, false, futureDate()));
        onView(withId(R.id.text_detail_fee))
                .perform(scrollTo()).check(matches(withText("Fee: $15.00")));
    }

    // -----------------------------------------------------------------------
    // Waitlist / geolocation tests
    // -----------------------------------------------------------------------

    @Test
    public void waitlist_showsUnlimited_whenNoLimit() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));
        onView(withId(R.id.text_detail_waitlist))
                .perform(scrollTo()).check(matches(withText("Waitlist Limit: Unlimited")));
    }

    @Test
    public void waitlist_showsLimit_whenLimitIsSet() {
        launchWithEvent(makeEvent(0, true, 100, false, futureDate()));
        onView(withId(R.id.text_detail_waitlist))
                .perform(scrollTo()).check(matches(withText("Waitlist Limit: 100")));
    }

    @Test
    public void geolocation_showsYes_whenRequired() {
        launchWithEvent(makeEvent(0, false, 0, true, futureDate()));
        onView(withId(R.id.text_detail_geolocation))
                .perform(scrollTo()).check(matches(withText("Geolocation Required: Yes")));
    }

    @Test
    public void geolocation_showsNo_whenNotRequired() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));
        onView(withId(R.id.text_detail_geolocation))
                .perform(scrollTo()).check(matches(withText("Geolocation Required: No")));
    }

    // -----------------------------------------------------------------------
    // Private event button test
    // -----------------------------------------------------------------------

    /**
     * "Invite to Waitlist" button must be visible only for private events.
     * Uses withEffectiveVisibility rather than isDisplayed() because the button
     * may sit below the fold in a non-scrollable layout section.
     */
    @Test
    public void inviteWaitlistButton_isVisible_forPrivateEvent() {
        Event e = makeEvent(0, false, 0, false, futureDate());
        e.setPrivateEvent(true);
        launchWithEvent(e);

        onView(withId(R.id.invite_waitlist_button))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    /**
     * "Invite to Waitlist" button must be hidden for public events.
     */
    @Test
    public void inviteWaitlistButton_isGone_forPublicEvent() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.invite_waitlist_button)).check(matches(not(isDisplayed())));
    }

    // -----------------------------------------------------------------------
    // Lottery button state tests
    // -----------------------------------------------------------------------

    /**
     * When the lottery has already been completed, the button must show
     * "Lottery Completed" and be disabled.
     */
    // Note: lottery_button is outside the ScrollView (fixed at bottom), so scrollTo() cannot be
    // used. withText() and isEnabled() work on off-screen views without requiring scrolling.

    @Test
    public void lotteryButton_showsLotteryCompleted_whenLotteryAlreadyDone() {
        launchWithEvent(makeCompletedLotteryEvent());

        onView(withId(R.id.lottery_button))
                .check(matches(withText("Lottery Completed")))
                .check(matches(not(isEnabled())));
    }

    /**
     * When registration has not opened yet, the button must show
     * "Registration Not Open Yet" and be disabled.
     */
    @Test
    public void lotteryButton_showsNotOpenYet_whenRegistrationIsUpcoming() {
        launchWithEvent(makeUpcomingEvent());

        onView(withId(R.id.lottery_button))
                .check(matches(withText(containsString("Not Open Yet"))))
                .check(matches(not(isEnabled())));
    }

    /**
     * When registration is currently open, the button must show
     * "Registration Open (Lottery Pending)" and be disabled.
     */
    @Test
    public void lotteryButton_showsRegistrationOpenPending_whenOpenNow() {
        launchWithEvent(makeOpenRegistrationEvent());

        onView(withId(R.id.lottery_button))
                .check(matches(withText(containsString("Lottery Pending"))))
                .check(matches(not(isEnabled())));
    }

    /**
     * When registration has closed and the lottery has not been run,
     * the button must show "Draw Lottery" and be enabled.
     */
    @Test
    public void lotteryButton_showsDrawLottery_whenRegistrationClosed() {
        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button))
                .check(matches(withText("Draw Lottery")))
                .check(matches(isEnabled()));
    }

    // -----------------------------------------------------------------------
    // Lottery click / logic tests
    // -----------------------------------------------------------------------

    /**
     * Clicking the lottery button when registration period has not ended must NOT
     * call the entrant repository — the fragment shows a toast instead.
     */
    @Test
    public void lotteryButton_doesNotCallRepo_whenRegistrationNotClosed() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        verify(mockEntrantRepo, never()).getEntrantsByStatus(any(), any(), any());
    }

    /**
     * Clicking the lottery button after registration closes must call
     * {@link EntrantRepository#getEntrantsByStatus} with the correct event ID and
     * "waitlist" status.
     */
    @Test
    public void lotteryButton_callsGetEntrantsByStatus_whenRegistrationClosed() {
        stubWaitlist(Collections.emptyList());
        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        verify(mockEntrantRepo).getEntrantsByStatus(eq(EVENT_ID), eq("waitlist"), any());
    }

    /**
     * When capacity (1) < waitlist size (2), exactly one entrant must be invited
     * and the other must be marked not selected.
     */
    @Test
    public void lottery_invitesCapacityCount_andMarksRestNotSelected() {
        stubWaitlist(Arrays.asList(makeEntrant("user1"), makeEntrant("user2")));

        Event event = makeEvent(0, false, 0, false, pastDate());
        event.setLotteryCapacity(1);
        launchWithEvent(event);

        onView(withId(R.id.lottery_button)).perform(click());

        verify(mockEntrantRepo, times(1))
                .updateStatus(eq(EVENT_ID), any(), eq(Entrant.STATUS_INVITED), any());
        verify(mockEntrantRepo, times(1))
                .updateStatus(eq(EVENT_ID), any(), eq(Entrant.STATUS_NOT_SELECTED), any());
    }

    /**
     * When waitlist size (2) <= capacity (50), all entrants must be invited
     * and none marked not selected.
     */
    @Test
    public void lottery_invitesAll_whenWaitlistSmallerThanCapacity() {
        stubWaitlist(Arrays.asList(makeEntrant("user1"), makeEntrant("user2")));
        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        verify(mockEntrantRepo).updateStatus(eq(EVENT_ID), eq("user1"), eq(Entrant.STATUS_INVITED), any());
        verify(mockEntrantRepo).updateStatus(eq(EVENT_ID), eq("user2"), eq(Entrant.STATUS_INVITED), any());
        verify(mockEntrantRepo, never())
                .updateStatus(any(), any(), eq(Entrant.STATUS_NOT_SELECTED), any());
    }

    /**
     * After a successful lottery draw, {@code updateLotteryCompleted} must be called
     * with {@code true} so the lottery cannot be run again.
     */
    @Test
    public void lottery_callsUpdateLotteryCompleted_afterSuccessfulDraw() {
        stubWaitlist(Collections.singletonList(makeEntrant("user1")));
        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        verify(mockEventRepo).updateLotteryCompleted(eq(EVENT_ID), eq(true), any());
    }

    /**
     * After a successful draw and {@code updateLotteryCompleted} confirms success,
     * the lottery button must switch to "Lottery Completed" and be disabled.
     */
    @Test
    public void lotteryButton_showsLotteryCompleted_afterSuccessfulDraw() {
        stubWaitlist(Collections.singletonList(makeEntrant("user1")));

        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEventRepo).updateLotteryCompleted(any(), anyBoolean(), any());

        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        onView(withId(R.id.lottery_button))
                .check(matches(withText("Lottery Completed")))
                .check(matches(not(isEnabled())));
    }

    /**
     * When the waitlist is empty, the lottery button must be re-enabled so the
     * organizer can try again later.
     */
    @Test
    public void lotteryButton_isReEnabled_whenWaitlistIsEmpty() {
        stubWaitlist(Collections.emptyList());
        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        onView(withId(R.id.lottery_button)).check(matches(isEnabled()));
    }

    /**
     * After clicking with a closed registration and entrants on the waitlist,
     * the button must stay disabled (lottery is running / completed).
     */
    @Test
    public void lotteryButton_isDisabled_afterLotteryRun() {
        stubWaitlist(Collections.singletonList(makeEntrant("user1")));
        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        onView(withId(R.id.lottery_button)).check(matches(not(isEnabled())));
    }

    // -----------------------------------------------------------------------
    // Additional data tests
    // -----------------------------------------------------------------------

    /**
     * The capacity field must show the lottery capacity with the correct prefix.
     */
    @Test
    public void capacity_showsCorrectText() {
        Event e = makeEvent(0, false, 0, false, futureDate());
        e.setLotteryCapacity(50);
        launchWithEvent(e);

        onView(withId(R.id.text_detail_capacity))
                .perform(scrollTo())
                .check(matches(withText("Lottery Capacity: 50")));
    }

    // -----------------------------------------------------------------------
    // Lottery failure path
    // -----------------------------------------------------------------------

    /**
     * When {@code getEntrantsByStatus} fires onFailure, the lottery button must be
     * re-enabled so the organizer can try again.
     */
    @Test
    public void lotteryButton_isReEnabled_whenWaitlistFetchFails() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<List<Entrant>> cb = invocation.getArgument(2);
            cb.onFailure(new Exception("Firestore error"));
            return null;
        }).when(mockEntrantRepo).getEntrantsByStatus(any(), any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, pastDate()));

        onView(withId(R.id.lottery_button)).perform(click());

        onView(withId(R.id.lottery_button)).check(matches(isEnabled()));
    }

    // -----------------------------------------------------------------------
    // Comment tests
    // -----------------------------------------------------------------------

    /**
     * When the real-time listener delivers a comment, it must be rendered with
     * the author name and message body.
     */
    @Test
    public void comment_isRendered_withAuthorAndText() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<List<Comment>> cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeComment("c1", "Alice", "Great event!")));
            return mockListenerReg;
        }).when(mockCommentRepo).listenToComments(any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        // Comments are below the fold — scroll to them first
        onView(withText("Alice")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("Great event!")).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * The organizer always sees a "Delete" button for every comment so they can
     * moderate the discussion.
     */
    @Test
    public void deleteButton_isVisible_forEachComment() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<List<Comment>> cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeComment("c1", "Alice", "Great event!")));
            return mockListenerReg;
        }).when(mockCommentRepo).listenToComments(any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(allOf(withId(R.id.comment_delete), withText("Delete")))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /**
     * Clicking a comment's "Delete" button must call
     * {@link CommentRepository#deleteComment} with the correct comment ID.
     */
    @Test
    public void deleteButton_click_callsDeleteComment_withCorrectId() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<List<Comment>> cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeComment("c1", "Alice", "Great event!")));
            return mockListenerReg;
        }).when(mockCommentRepo).listenToComments(any(), any());

        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockCommentRepo).deleteComment(any(), any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(allOf(withId(R.id.comment_delete), withText("Delete")))
                .perform(scrollTo(), click());

        verify(mockCommentRepo).deleteComment(eq(EVENT_ID), eq("c1"), any());
    }

    /**
     * Typing text and clicking Send must call
     * {@link CommentRepository#addComment} with the correct event ID.
     */
    @Test
    public void sendComment_withText_callsAddComment() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockCommentRepo).addComment(any(), any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.edit_comment_input))
                .perform(scrollTo(), replaceText("Looks amazing!"));
        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());

        verify(mockCommentRepo).addComment(eq(EVENT_ID), any(), any());
    }

    /**
     * Clicking Send with an empty input must NOT call the repository.
     */
    @Test
    public void sendComment_withEmptyText_doesNotCallRepo() {
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());

        verify(mockCommentRepo, never()).addComment(any(), any(), any());
    }

    /**
     * The send button must be disabled while the post is in-flight (before the callback).
     * This prevents double-posting.
     */
    @Test
    public void sendButton_isDisabled_whilePosting() {
        // addComment never calls back — simulates an in-flight request
        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.edit_comment_input))
                .perform(scrollTo(), replaceText("Hello!"));
        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());

        onView(withId(R.id.button_send_comment)).check(matches(not(isEnabled())));
    }

    /**
     * After a successful post the send button must be re-enabled.
     */
    @Test
    public void sendButton_isReEnabled_afterSuccessfulPost() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockCommentRepo).addComment(any(), any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.edit_comment_input))
                .perform(scrollTo(), replaceText("Hello!"));
        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());

        onView(withId(R.id.button_send_comment)).check(matches(isEnabled()));
    }

    /**
     * After a failed post the send button must also be re-enabled so the organizer
     * can try again.
     */
    @Test
    public void sendButton_isReEnabled_afterFailedPost() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onFailure(new Exception("Firestore error"));
            return null;
        }).when(mockCommentRepo).addComment(any(), any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.edit_comment_input))
                .perform(scrollTo(), replaceText("Hello!"));
        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());

        onView(withId(R.id.button_send_comment)).check(matches(isEnabled()));
    }

    /**
     * After a successful post the comment input must be cleared automatically.
     */
    @Test
    public void commentInput_isClearedAfterSuccessfulPost() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockCommentRepo).addComment(any(), any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));

        onView(withId(R.id.edit_comment_input))
                .perform(scrollTo(), replaceText("Hello!"));
        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());

        onView(withId(R.id.edit_comment_input)).check(matches(withText("")));
    }

    // -----------------------------------------------------------------------
    // Comment "Show more / Show less" tests
    // -----------------------------------------------------------------------

    // ~450 chars — enough to overflow 4 lines at any phone screen width
    private static final String LONG_COMMENT =
            "This is a very long comment that is definitely going to span more than four lines " +
            "when rendered on screen. It keeps going and going with more text to ensure " +
            "that the view tree observer detects the overflow correctly. Adding even more " +
            "text here to push this well past the four line limit on any device size. " +
            "And some more text for good measure to make absolutely certain this overflows.";

    private void launchWithLongComment() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<List<Comment>> cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(
                    makeComment("c1", "Alice", LONG_COMMENT)));
            return mockListenerReg;
        }).when(mockCommentRepo).listenToComments(any(), any());

        launchWithEvent(makeEvent(0, false, 0, false, futureDate()));
    }

    /**
     * A comment that overflows 4 lines must show the "Show more" button.
     */
    @Test
    public void longComment_showMoreButton_isDisplayed() {
        launchWithLongComment();
        onView(withId(R.id.comment_show_more)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.comment_show_more)).check(matches(withText("Show more")));
    }

    /**
     * Clicking "Show more" must expand the comment and change the label to "Show less".
     */
    @Test
    public void longComment_clickShowMore_changesLabelToShowLess() {
        launchWithLongComment();
        onView(withId(R.id.comment_show_more)).perform(scrollTo(), click());
        onView(withId(R.id.comment_show_more)).check(matches(withText("Show less")));
    }

    /**
     * Clicking "Show less" after expanding must collapse the comment and restore "Show more".
     */
    @Test
    public void longComment_clickShowLess_restoresShowMore() {
        launchWithLongComment();
        onView(withId(R.id.comment_show_more)).perform(scrollTo(), click()); // expand
        onView(withId(R.id.comment_show_more)).perform(scrollTo(), click()); // collapse
        onView(withId(R.id.comment_show_more)).check(matches(withText("Show more")));
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

        // button_back is at the top of the ScrollView — always visible
        onView(withId(R.id.button_back)).check(matches(isDisplayed()));
        // lottery_button is outside the ScrollView — use effective visibility
        onView(withId(R.id.lottery_button))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }
}
