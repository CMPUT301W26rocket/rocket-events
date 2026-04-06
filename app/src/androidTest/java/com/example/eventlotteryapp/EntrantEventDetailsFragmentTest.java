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
import com.example.eventlotteryapp.ui.fragments.EntrantEventDetailsFragment;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UI tests for {@link EntrantEventDetailsFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: event title, organizer, waitlist count</li>
 *   <li>Button states: all entrant status → button text and enabled/disabled</li>
 *   <li>Organizer/co-organizer: button disabled with appropriate label</li>
 *   <li>Private event: waitlist-invited and declined-waitlist states and dialogs</li>
 *   <li>Private event: leaving waitlist updates status instead of deleting</li>
 *   <li>Ticket button: visible only when status is INVITED</li>
 *   <li>Click flows: join, leave, accept/decline invitation, waitlist invitation dialogs</li>
 *   <li>Comments: send button, empty/valid text, input cleared on success</li>
 * </ul>
 *
 * @author Leyla
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailsFragmentTest {

    private static final String DEVICE_ID  = "device123";
    private static final String EVENT_ID   = "event123";
    private static final String ORG_ID     = "organizer456"; // different from DEVICE_ID

    @Mock EventRepository        mockEventRepo;
    @Mock EntrantRepository      mockEntrantRepo;
    @Mock CommentRepository      mockCommentRepo;
    @Mock UserRepository         mockUserRepo;
    @Mock ListenerRegistration   mockWaitlistListener;
    @Mock ListenerRegistration   mockCommentsListener;

    private Event fakeEvent;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        fakeEvent = new Event();
        fakeEvent.setEventId(EVENT_ID);
        fakeEvent.setOrganizerId(ORG_ID);   // default: user is NOT the organizer
        fakeEvent.setTitle("Test Event");
        fakeEvent.setDescription("A great event description");
        fakeEvent.setOrganizerName("Jane Smith");
        fakeEvent.setLocation("Edmonton Convention Centre");
        fakeEvent.setRegistrationFee(0.0);
        fakeEvent.setLotteryCapacity(50);
        fakeEvent.setGeolocationRequired(false);
        fakeEvent.setHasWaitlistLimit(false);
        fakeEvent.setPrivateEvent(false);
        // Registration window: opened yesterday, closes tomorrow → currently open
        fakeEvent.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 86400000L));
        fakeEvent.setRegistrationCloseDate(new Date(System.currentTimeMillis() + 86400000L));

        // Waitlist count listener fires immediately with 3
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Integer> cb = invocation.getArgument(1);
            cb.onSuccess(3);
            return mockWaitlistListener;
        }).when(mockEntrantRepo).listenToWaitlistCount(any(), any());

        // Comments listener fires immediately with an empty list
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<List<Comment>> cb = invocation.getArgument(1);
            cb.onSuccess(new ArrayList<>());
            return mockCommentsListener;
        }).when(mockCommentRepo).listenToComments(any(), any());

        // User name lookup returns "Test User" by default
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<User> cb = invocation.getArgument(1);
            User u = new User();
            u.setName("Test User");
            cb.onSuccess(u);
            return null;
        }).when(mockUserRepo).getUser(any(), any());
    }

    // -----------------------------------------------------------------------
    // Helper — launch fragment
    // -----------------------------------------------------------------------

    /**
     * Launches the fragment with a specific entrant status.
     *
     * @param entrant pass an Entrant to simulate an existing relationship to the event,
     *                or null to simulate a user who has never joined
     */
    private void launch(Entrant entrant) {
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<Event> cb = invocation.getArgument(1);
            cb.onSuccess(fakeEvent);
            return null;
        }).when(mockEventRepo).getEventById(any(), any());

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Entrant> cb = invocation.getArgument(2);
            cb.onSuccess(entrant);
            return null;
        }).when(mockEntrantRepo).getEntrant(any(), any(), any());

        Bundle args = new Bundle();
        args.putString("eventId",  EVENT_ID);
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                EntrantEventDetailsFragment fragment = new EntrantEventDetailsFragment();
                fragment.setEventRepository(mockEventRepo);
                fragment.setEntrantRepository(mockEntrantRepo);
                fragment.setCommentRepository(mockCommentRepo);
                fragment.setUserRepository(mockUserRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                EntrantEventDetailsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    @Test
    public void eventTitle_isDisplayed() {
        launch(null);
        onView(withId(R.id.text_detail_title)).check(matches(withText("Test Event")));
    }

    @Test
    public void eventOrganizer_isDisplayed() {
        launch(null);
        onView(withId(R.id.text_detail_organizer)).check(matches(withText("By Jane Smith")));
    }

    @Test
    public void waitlistCount_isDisplayed() {
        launch(null);
        onView(withId(R.id.text_detail_waitlist_count))
                .check(matches(withText("Current Waitlist: 3")));
    }

    // -----------------------------------------------------------------------
    // Button state tests — no entrant
    // -----------------------------------------------------------------------

    @Test
    public void whenNotOnWaitlist_buttonShowsJoinWaitlist() {
        launch(null);
        onView(withId(R.id.action_button)).check(matches(withText("Join Waitlist")));
    }

    @Test
    public void whenNotOnWaitlist_buttonIsEnabled() {
        launch(null);
        onView(withId(R.id.action_button)).check(matches(isEnabled()));
    }

    @Test
    public void whenRegistrationClosed_buttonShowsRegistrationClosed() {
        fakeEvent.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 172800000L));
        fakeEvent.setRegistrationCloseDate(new Date(System.currentTimeMillis() - 86400000L));
        launch(null);
        onView(withId(R.id.action_button)).check(matches(withText("Registration Closed")));
    }

    @Test
    public void whenRegistrationClosed_buttonIsDisabled() {
        fakeEvent.setRegistrationOpenDate(new Date(System.currentTimeMillis() - 172800000L));
        fakeEvent.setRegistrationCloseDate(new Date(System.currentTimeMillis() - 86400000L));
        launch(null);
        onView(withId(R.id.action_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void whenRegistrationNotYetOpen_buttonShowsNotOpenYet() {
        fakeEvent.setRegistrationOpenDate(new Date(System.currentTimeMillis() + 86400000L));
        fakeEvent.setRegistrationCloseDate(new Date(System.currentTimeMillis() + 172800000L));
        launch(null);
        onView(withId(R.id.action_button)).check(matches(withText("Registration Not Open Yet")));
    }

    // -----------------------------------------------------------------------
    // Button state tests — with entrant status
    // -----------------------------------------------------------------------

    @Test
    public void whenOnWaitlist_buttonShowsLeaveWaitlist() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_WAITLIST);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(withText("Leave Waitlist")));
    }

    @Test
    public void whenInvited_buttonShowsInvited() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_INVITED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(withText("Invited")));
    }

    @Test
    public void whenInvited_buttonIsEnabled() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_INVITED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(isEnabled()));
    }

    @Test
    public void whenEnrolled_buttonShowsEnrolled() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_ENROLLED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(withText("Enrolled")));
    }

    @Test
    public void whenEnrolled_buttonIsDisabled() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_ENROLLED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void whenDeclined_buttonIsDisabled() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_DECLINED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void whenCancelled_buttonShowsCancelled() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_CANCELLED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(withText("Cancelled")));
    }

    @Test
    public void whenCancelled_buttonIsDisabled() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_CANCELLED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void whenNotSelected_buttonShowsLeaveWaitlist() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_NOT_SELECTED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(withText("Leave Waitlist")));
    }

    // -----------------------------------------------------------------------
    // Organizer / co-organizer button states
    // -----------------------------------------------------------------------

    /**
     * When the current device is the event organizer the button must say
     * "You're the Organizer" and be disabled.
     */
    @Test
    public void whenUserIsOrganizer_buttonShowsYoureTheOrganizer() {
        fakeEvent.setOrganizerId(DEVICE_ID); // matches the test device
        launch(null);
        onView(withId(R.id.action_button)).check(matches(withText("You're the Organizer")));
    }

    @Test
    public void whenUserIsOrganizer_buttonIsDisabled() {
        fakeEvent.setOrganizerId(DEVICE_ID);
        launch(null);
        onView(withId(R.id.action_button)).check(matches(not(isEnabled())));
    }

    /**
     * When the user has co-organizer status the button must say
     * "You're a Co-organizer" and be disabled.
     */
    @Test
    public void whenUserIsCoOrganizer_buttonShowsYoureACoOrganizer() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_CO_ORGANIZER);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(withText("You're a Co-organizer")));
    }

    @Test
    public void whenUserIsCoOrganizer_buttonIsDisabled() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_CO_ORGANIZER);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(not(isEnabled())));
    }

    // -----------------------------------------------------------------------
    // Private event — waitlist invitation states
    // -----------------------------------------------------------------------

    /**
     * STATUS_WAITLIST_INVITED means the organizer sent a private-event waitlist invite.
     * The button should say "Invited to Waitlist" and be enabled so the user can respond.
     */
    @Test
    public void whenWaitlistInvited_buttonShowsInvitedToWaitlist() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_WAITLIST_INVITED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(withText("Invited to Waitlist")));
    }

    @Test
    public void whenWaitlistInvited_buttonIsEnabled() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_WAITLIST_INVITED);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(isEnabled()));
    }

    /**
     * STATUS_DECLINED_WAITLIST means the user declined (or left) a private-event waitlist.
     * The button should say "Waitlist Invite Declined" and remain enabled so they can reconsider.
     */
    @Test
    public void whenDeclinedWaitlist_buttonShowsWaitlistInviteDeclined() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_DECLINED_WAITLIST);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(withText("Waitlist Invite Declined")));
    }

    @Test
    public void whenDeclinedWaitlist_buttonIsEnabled() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_DECLINED_WAITLIST);
        launch(e);
        onView(withId(R.id.action_button)).check(matches(isEnabled()));
    }

    // -----------------------------------------------------------------------
    // Private event — waitlist invitation click flows
    // -----------------------------------------------------------------------

    /**
     * Clicking the button in WAITLIST_INVITED state must open the "Waitlist Invitation" dialog.
     */
    @Test
    public void clickWaitlistInvited_showsWaitlistInvitationDialog() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_WAITLIST_INVITED);
        launch(e);
        onView(withId(R.id.action_button)).perform(click());
        onView(withText("Waitlist Invitation")).check(matches(isDisplayed()));
    }

    /**
     * Accepting the waitlist invitation must update status to WAITLIST and change the
     * button to "Leave Waitlist".
     */
    @Test
    public void acceptWaitlistInvitation_buttonChangesToLeaveWaitlist() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_WAITLIST_INVITED);
        launch(e);

        onView(withId(R.id.action_button)).perform(click());
        onView(withText("Accept")).perform(click());

        onView(withId(R.id.action_button)).check(matches(withText("Leave Waitlist")));
        onView(withId(R.id.action_button)).check(matches(isEnabled()));
    }

    /**
     * Declining the waitlist invitation must update status to DECLINED_WAITLIST and
     * change the button to "Waitlist Invite Declined".
     */
    @Test
    public void declineWaitlistInvitation_buttonChangesToWaitlistInviteDeclined() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_WAITLIST_INVITED);
        launch(e);

        onView(withId(R.id.action_button)).perform(click());
        onView(withText("Decline")).perform(click());

        onView(withId(R.id.action_button)).check(matches(withText("Waitlist Invite Declined")));
    }

    /**
     * Clicking the button in DECLINED_WAITLIST state must open the "Reconsider?" dialog
     * offering to rejoin the waitlist.
     */
    @Test
    public void clickDeclinedWaitlist_showsReconsiderDialog() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_DECLINED_WAITLIST);
        launch(e);
        onView(withId(R.id.action_button)).perform(click());
        onView(withText("Reconsider?")).check(matches(isDisplayed()));
    }

    /**
     * Accepting the "Reconsider?" dialog must update status to WAITLIST and change
     * the button to "Leave Waitlist".
     */
    @Test
    public void acceptRejoinWaitlist_buttonChangesToLeaveWaitlist() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_DECLINED_WAITLIST);
        launch(e);

        onView(withId(R.id.action_button)).perform(click());
        onView(withText("Join Waitlist")).perform(click());

        onView(withId(R.id.action_button)).check(matches(withText("Leave Waitlist")));
    }

    // -----------------------------------------------------------------------
    // Private event — leaving waitlist keeps the entrant doc
    // -----------------------------------------------------------------------

    /**
     * For a private event, leaving the waitlist must call updateStatus(DECLINED_WAITLIST)
     * rather than deleting the entrant doc. The button must show "Waitlist Invite Declined"
     * (not "Join Waitlist") so the user can reconsider via the Reconsider dialog.
     */
    @Test
    public void privateEvent_leaveWaitlist_setsDeclinedWaitlistStatus() {
        fakeEvent.setPrivateEvent(true);

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_WAITLIST);
        launch(e);

        onView(withId(R.id.action_button)).perform(click());

        // Status must be updated, not deleted
        verify(mockEntrantRepo, never()).leaveWaitlist(any(), any(), any());
        // Button reflects the new declined state (user can still reconsider)
        onView(withId(R.id.action_button)).check(matches(withText("Waitlist Invite Declined")));
    }

    // -----------------------------------------------------------------------
    // Ticket (confirmation) button visibility
    // -----------------------------------------------------------------------

    /**
     * The download ticket button must be visible when the user has been invited
     * (STATUS_INVITED), so they can save their confirmation PDF.
     */
    @Test
    public void whenInvited_ticketButtonIsVisible() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_INVITED);
        launch(e);
        onView(withId(R.id.confirmation)).check(matches(isDisplayed()));
    }

    /**
     * The ticket button must be hidden for any status other than INVITED.
     */
    @Test
    public void whenOnWaitlist_ticketButtonIsHidden() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_WAITLIST);
        launch(e);
        onView(withId(R.id.confirmation)).check(matches(not(isDisplayed())));
    }

    @Test
    public void whenEnrolled_ticketButtonIsHidden() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_ENROLLED);
        launch(e);
        onView(withId(R.id.confirmation)).check(matches(not(isDisplayed())));
    }

    // -----------------------------------------------------------------------
    // Join / leave waitlist click flows
    // -----------------------------------------------------------------------

    @Test
    public void clickJoinWaitlist_buttonChangesToLeaveWaitlist() {
        doAnswer(invocation -> {
            // joinWaitlist(eventId, deviceId, lat, lng, callback) — callback is argument 4
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(4);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).joinWaitlist(any(), any(), any(), any(), any());

        launch(null);

        onView(withId(R.id.action_button)).perform(click());
        onView(withText("Agree")).perform(click());

        onView(withId(R.id.action_button)).check(matches(withText("Leave Waitlist")));
    }

    @Test
    public void clickLeaveWaitlist_buttonChangesToJoinWaitlist() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).leaveWaitlist(any(), any(), any());

        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_WAITLIST);
        launch(e);

        onView(withId(R.id.action_button)).perform(click());

        onView(withId(R.id.action_button)).check(matches(withText("Join Waitlist")));
    }

    @Test
    public void whenWaitlistFull_clickJoinWaitlist_buttonStaysEnabledAsJoinWaitlist() {
        fakeEvent.setHasWaitlistLimit(true);
        fakeEvent.setWaitlistLimit(10);

        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Boolean> cb = invocation.getArgument(2);
            cb.onSuccess(true);
            return null;
        }).when(mockEntrantRepo).isWaitlistFull(any(), any(int.class), any());

        launch(null);

        onView(withId(R.id.action_button)).perform(click());
        onView(withText("Agree")).perform(click());

        onView(withId(R.id.action_button)).check(matches(withText("Join Waitlist")));
        onView(withId(R.id.action_button)).check(matches(isEnabled()));
    }

    // -----------------------------------------------------------------------
    // Invitation dialog flows
    // -----------------------------------------------------------------------

    @Test
    public void clickInvited_showsInvitationDialog() {
        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_INVITED);
        launch(e);
        onView(withId(R.id.action_button)).perform(click());
        onView(withText("You're Invited!")).check(matches(isDisplayed()));
    }

    @Test
    public void clickAcceptInvitation_buttonBecomesEnrolledAndDisabled() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_INVITED);
        launch(e);

        onView(withId(R.id.action_button)).perform(click());
        onView(withText("Accept")).perform(click());

        onView(withId(R.id.action_button)).check(matches(withText("Enrolled")));
        onView(withId(R.id.action_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void clickDeclineInvitation_buttonBecomesDeclinedAndDisabled() {
        doAnswer(invocation -> {
            EntrantRepository.FirestoreCallback<Void> cb = invocation.getArgument(3);
            cb.onSuccess(null);
            return null;
        }).when(mockEntrantRepo).updateStatus(any(), any(), any(), any());

        Entrant e = new Entrant(); e.setStatus(Entrant.STATUS_INVITED);
        launch(e);

        onView(withId(R.id.action_button)).perform(click());
        onView(withText("Decline")).perform(click());

        onView(withId(R.id.action_button)).check(matches(withText("Declined")));
        onView(withId(R.id.action_button)).check(matches(not(isEnabled())));
    }

    // -----------------------------------------------------------------------
    // Comment tests
    // -----------------------------------------------------------------------

    /**
     * When the comments listener returns a comment, the author and text must be
     * rendered in the comments section. This covers the rendering path that is
     * triggered both on initial load and after a new comment is pushed by Firestore.
     */
    @Test
    public void commentsListener_withComment_displaysAuthorAndText() {
        // Override the default empty-list stub to return one real comment
        com.google.firebase.Timestamp ts = com.google.firebase.Timestamp.now();
        Comment comment = new Comment("user1", "Alice Smith", "Great event!", ts);

        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<List<Comment>> cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(comment));
            return mockCommentsListener;
        }).when(mockCommentRepo).listenToComments(any(), any());

        launch(null);

        onView(withText("Alice Smith")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("Great event!")).perform(scrollTo()).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Comment "Show more / Show less" tests
    // -----------------------------------------------------------------------

    // A text long enough to overflow 4 lines at 14sp on any phone screen.
    // ~450 chars ≈ 8+ lines at typical screen widths.
    private static final String LONG_COMMENT =
            "This is a very long comment that is definitely going to span more than four lines " +
            "when rendered on screen. It keeps going and going with more text to ensure " +
            "that the view tree observer detects the overflow correctly. Adding even more " +
            "text here to push this well past the four line limit on any device size. " +
            "And some more text for good measure to make absolutely certain this overflows.";

    private void launchWithLongComment() {
        com.google.firebase.Timestamp ts = com.google.firebase.Timestamp.now();
        Comment comment = new Comment("user1", "Alice Smith", LONG_COMMENT, ts);

        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<List<Comment>> cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(comment));
            return mockCommentsListener;
        }).when(mockCommentRepo).listenToComments(any(), any());

        launch(null);
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
        // Expand
        onView(withId(R.id.comment_show_more)).perform(scrollTo(), click());
        // Collapse
        onView(withId(R.id.comment_show_more)).perform(scrollTo(), click());
        onView(withId(R.id.comment_show_more)).check(matches(withText("Show more")));
    }

    /**
     * The comment input and send button must both be visible.
     */
    @Test
    public void commentInput_andSendButton_areDisplayed() {
        launch(null);
        onView(withId(R.id.edit_comment_input)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.button_send_comment)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * Clicking Send with an empty input must NOT call the repository.
     */
    @Test
    public void sendComment_withEmptyText_doesNotCallRepository() {
        launch(null);
        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());
        verify(mockCommentRepo, never()).addComment(any(), any(), any());
    }

    /**
     * Typing text and clicking Send must call {@link CommentRepository#addComment}.
     */
    @Test
    public void sendComment_withValidText_callsRepository() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockCommentRepo).addComment(any(), any(), any());

        launch(null);

        onView(withId(R.id.edit_comment_input))
                .perform(scrollTo(), replaceText("Great event!"), closeSoftKeyboard());
        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());

        verify(mockCommentRepo).addComment(any(), any(), any());
    }

    /**
     * After a successful send the comment input field must be cleared.
     */
    @Test
    public void sendComment_onSuccess_clearsInputField() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockCommentRepo).addComment(any(), any(), any());

        launch(null);

        onView(withId(R.id.edit_comment_input))
                .perform(scrollTo(), replaceText("Great event!"), closeSoftKeyboard());
        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());

        onView(withId(R.id.edit_comment_input)).check(matches(withText("")));
    }

    /**
     * After a failed send the send button must be re-enabled so the user can retry.
     */
    @Test
    public void sendComment_onFailure_reEnablesSendButton() {
        doAnswer(invocation -> {
            CommentRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onFailure(new Exception("Firestore error"));
            return null;
        }).when(mockCommentRepo).addComment(any(), any(), any());

        launch(null);

        onView(withId(R.id.edit_comment_input))
                .perform(scrollTo(), replaceText("Great event!"), closeSoftKeyboard());
        onView(withId(R.id.button_send_comment)).perform(scrollTo(), click());

        onView(withId(R.id.button_send_comment)).check(matches(isEnabled()));
    }
}
