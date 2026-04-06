package com.example.eventlotteryapp;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Notification;
import com.example.eventlotteryapp.repository.NotificationRepository;
import com.example.eventlotteryapp.ui.fragments.NotificationsFragment;
import com.google.firebase.Timestamp;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
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
 * UI tests for {@link NotificationsFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: RecyclerView and header are visible on launch</li>
 *   <li>Empty state: "No notifications yet." text shown when list is empty, hidden otherwise</li>
 *   <li>Content: event title and message body appear for each notification</li>
 *   <li>Multiple notifications: all items rendered</li>
 *   <li>Sort order: notifications sorted newest-first by {@code createdAt}</li>
 *   <li>Unread dot: visible for unread notifications, invisible for read ones</li>
 *   <li>Background: unread notifications use the unread drawable, read notifications use the read (darker) drawable</li>
 *   <li>Mark as read: clicking an unread notification calls {@code markAsRead} on the repository</li>
 *   <li>Correctness: repository called with the correct deviceId from arguments</li>
 *   <li>Failure: RecyclerView remains empty when the repository reports an error</li>
 * </ul>
 *
 * <p><b>Note on click/navigation:</b> The fragment navigates to {@code R.id.fragment_container},
 * which does not exist in the {@link FragmentScenario} host activity. Android's
 * {@code FragmentManager} throws {@code IllegalArgumentException} when it cannot find the
 * container to attach the destination fragment's view, so navigation cannot be tested here.
 * Click tests set {@code eventId = null} to skip the navigation branch entirely.
 *
 * @author Leyla
 */
@RunWith(AndroidJUnit4.class)
public class NotificationsFragmentTest {

    private static final String DEVICE_ID = "device123";

    @Mock NotificationRepository mockNotifRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds an unread notification with the given event title and message.
     * {@code createdAt} is set to now so sorting behaves predictably.
     */
    private Notification makeNotification(String eventId, String eventTitle, String message) {
        Notification n = new Notification();
        n.setNotificationId("notif_" + eventTitle);
        n.setEventId(eventId);
        n.setEventTitle(eventTitle);
        n.setMessage(message);
        n.setRead(false);
        n.setCreatedAt(new Timestamp(System.currentTimeMillis() / 1000, 0));
        return n;
    }

    /**
     * Matches a view whose background drawable was set from the given resource ID.
     * Uses {@link Drawable#getConstantState()} equality, which is reliable for
     * two drawable instances loaded from the same XML resource.
     */
    private static Matcher<View> withBackgroundResource(@DrawableRes final int expectedResId) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("has background drawable resource " + expectedResId);
            }

            @Override
            protected boolean matchesSafely(View view) {
                Context ctx = view.getContext();
                Drawable expected = ContextCompat.getDrawable(ctx, expectedResId);
                Drawable actual = view.getBackground();
                if (expected == null || actual == null) return false;
                Drawable.ConstantState expectedState = expected.getConstantState();
                Drawable.ConstantState actualState = actual.getConstantState();
                return expectedState != null && expectedState.equals(actualState);
            }
        };
    }

    /**
     * Launches NotificationsFragment with a mock repository that immediately returns
     * {@code notifications} via onSuccess.
     */
    private void launch(List<Notification> notifications) {
        doAnswer(invocation -> {
            NotificationRepository.FirestoreCallback<List<Notification>> cb =
                    invocation.getArgument(1);
            cb.onSuccess(notifications);
            return null;
        }).when(mockNotifRepo).getNotificationsForUser(any(), any());

        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                NotificationsFragment fragment = new NotificationsFragment();
                fragment.setNotificationRepository(mockNotifRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                NotificationsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /** Launches with a repository that fires onFailure. */
    private void launchWithFailure() {
        doAnswer(invocation -> {
            NotificationRepository.FirestoreCallback<List<Notification>> cb =
                    invocation.getArgument(1);
            cb.onFailure(new Exception("Firestore unavailable"));
            return null;
        }).when(mockNotifRepo).getNotificationsForUser(any(), any());

        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                NotificationsFragment fragment = new NotificationsFragment();
                fragment.setNotificationRepository(mockNotifRepo);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                NotificationsFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    /**
     * The RecyclerView must be visible on launch.
     */
    @Test
    public void recyclerView_isDisplayed() {
        launch(new ArrayList<>());

        onView(withId(R.id.recycler_notifications)).check(matches(isDisplayed()));
    }

    /**
     * The "Notifications" header must be visible on launch.
     */
    @Test
    public void header_isDisplayed() {
        launch(new ArrayList<>());

        onView(withText("Notifications")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Empty state tests
    // -----------------------------------------------------------------------

    /**
     * When the notification list is empty, "No notifications yet." must be visible.
     */
    @Test
    public void emptyText_isDisplayed_whenNoNotifications() {
        launch(new ArrayList<>());

        onView(withId(R.id.text_notifications_empty)).check(matches(isDisplayed()));
    }

    /**
     * When there is at least one notification, the empty text must be hidden.
     */
    @Test
    public void emptyText_isGone_whenNotificationsExist() {
        launch(Arrays.asList(
                makeNotification("e1", "Spring Festival", "You've been selected!")));

        onView(withId(R.id.text_notifications_empty)).check(matches(not(isDisplayed())));
    }

    // -----------------------------------------------------------------------
    // Content tests
    // -----------------------------------------------------------------------

    /**
     * The event title must appear as the notification heading.
     */
    @Test
    public void notification_eventTitle_isDisplayed() {
        launch(Arrays.asList(
                makeNotification("e1", "Spring Festival", "You've been selected!")));

        onView(withText("Spring Festival")).check(matches(isDisplayed()));
    }

    /**
     * The notification message body must appear beneath the title.
     */
    @Test
    public void notification_message_isDisplayed() {
        launch(Arrays.asList(
                makeNotification("e1", "Spring Festival", "You've been selected!")));

        onView(withText("You've been selected!")).check(matches(isDisplayed()));
    }

    /**
     * When multiple notifications are returned, all their titles must be reachable
     * by scrolling through the list.
     */
    @Test
    public void multipleNotifications_allTitlesDisplayed() {
        launch(Arrays.asList(
                makeNotification("e1", "Spring Festival", "You won!"),
                makeNotification("e2", "Hackathon Night", "Not selected this time.")
        ));

        onView(withId(R.id.recycler_notifications)).perform(scrollToPosition(0));
        onView(withText("Spring Festival")).check(matches(isDisplayed()));

        onView(withId(R.id.recycler_notifications)).perform(scrollToPosition(1));
        onView(withText("Hackathon Night")).check(matches(isDisplayed()));
    }

    /**
     * RecyclerView must show exactly as many items as there are notifications.
     */
    @Test
    public void recyclerView_showsCorrectItemCount() {
        launch(Arrays.asList(
                makeNotification("e1", "Spring Festival", "You won!"),
                makeNotification("e2", "Hackathon Night", "Not selected.")
        ));

        onView(withId(R.id.recycler_notifications)).check(matches(hasChildCount(2)));
    }

    // -----------------------------------------------------------------------
    // Sort order test
    // -----------------------------------------------------------------------

    /**
     * Notifications must be displayed newest-first.
     * Passing an older notification before a newer one — after the fragment's sort,
     * the newer event title must appear at position 0.
     */
    @Test
    public void notifications_areSortedNewestFirst() {
        Notification older = makeNotification("e1", "Old Event", "Older message");
        older.setCreatedAt(new Timestamp(System.currentTimeMillis() / 1000 - 3600, 0));

        Notification newer = makeNotification("e2", "New Event", "Newer message");
        newer.setCreatedAt(new Timestamp(System.currentTimeMillis() / 1000, 0));

        // Pass oldest first — sort should flip the order
        launch(Arrays.asList(older, newer));

        onView(withId(R.id.recycler_notifications)).perform(scrollToPosition(0));
        onView(withText("New Event")).check(matches(isDisplayed()));

        onView(withId(R.id.recycler_notifications)).perform(scrollToPosition(1));
        onView(withText("Old Event")).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Unread dot tests
    // -----------------------------------------------------------------------

    /**
     * The unread dot must be visible for a notification that has not been read yet.
     */
    @Test
    public void unreadDot_isVisible_forUnreadNotification() {
        Notification n = makeNotification("e1", "Spring Festival", "You've been selected!");
        n.setRead(false);

        launch(Arrays.asList(n));

        onView(withId(R.id.view_unread_dot)).check(matches(isDisplayed()));
    }

    /**
     * The unread dot must not be visible for a notification that has already been read.
     */
    @Test
    public void unreadDot_isInvisible_forReadNotification() {
        Notification n = makeNotification("e1", "Spring Festival", "You've been selected!");
        n.setRead(true);

        launch(Arrays.asList(n));

        onView(withId(R.id.view_unread_dot)).check(matches(not(isDisplayed())));
    }

    // -----------------------------------------------------------------------
    // Mark-as-read test
    // -----------------------------------------------------------------------

    /**
     * Clicking an unread notification must call {@code markAsRead} on the repository.
     * The notification's {@code eventId} is set to {@code null} so the navigation
     * branch is skipped (no {@code R.id.fragment_container} in the test host activity).
     */
    @Test
    public void click_onUnreadNotification_callsMarkAsRead() {
        Notification n = makeNotification(null, "Spring Festival", "You've been selected!");
        n.setNotificationId("notif1");
        n.setRead(false);

        doAnswer(invocation -> {
            NotificationRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockNotifRepo).markAsRead(any(), any(), any());

        launch(Arrays.asList(n));

        onView(withId(R.id.recycler_notifications)).perform(
                androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition(0, click()));

        verify(mockNotifRepo).markAsRead(eq(DEVICE_ID), eq("notif1"), any());
    }

    /**
     * After clicking an unread notification and the repository confirms success,
     * the unread dot must become invisible.
     */
    @Test
    public void click_onUnreadNotification_hidesUnreadDotAfterSuccess() {
        Notification n = makeNotification(null, "Spring Festival", "You've been selected!");
        n.setNotificationId("notif1");
        n.setRead(false);

        doAnswer(invocation -> {
            NotificationRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockNotifRepo).markAsRead(any(), any(), any());

        launch(Arrays.asList(n));

        onView(withId(R.id.recycler_notifications)).perform(
                androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition(0, click()));

        onView(withId(R.id.view_unread_dot)).check(matches(not(isDisplayed())));
    }

    // -----------------------------------------------------------------------
    // Background (read/unread appearance) tests
    // -----------------------------------------------------------------------

    /**
     * An unread notification must use the unread card background drawable.
     */
    @Test
    public void background_isUnread_forUnreadNotification() {
        Notification n = makeNotification("e1", "Spring Festival", "You've been selected!");
        n.setRead(false);

        launch(Arrays.asList(n));

        onView(withId(R.id.notification_card))
                .check(matches(withBackgroundResource(R.drawable.notification_card_unread)));
    }

    /**
     * A read notification must use the read (darker) card background drawable.
     */
    @Test
    public void background_isRead_forReadNotification() {
        Notification n = makeNotification("e1", "Spring Festival", "You've been selected!");
        n.setRead(true);

        launch(Arrays.asList(n));

        onView(withId(R.id.notification_card))
                .check(matches(withBackgroundResource(R.drawable.notification_card_read)));
    }

    // -----------------------------------------------------------------------
    // Correctness and failure tests
    // -----------------------------------------------------------------------

    /**
     * The fragment must pass the deviceId from its arguments to
     * {@link NotificationRepository#getNotificationsForUser}.
     */
    @Test
    public void getNotificationsForUser_isCalledWithCorrectDeviceId() {
        launch(new ArrayList<>());

        verify(mockNotifRepo).getNotificationsForUser(eq(DEVICE_ID), any());
    }

    /**
     * When the repository fires onFailure, the RecyclerView must have no items.
     */
    @Test
    public void recyclerView_isEmpty_whenRepositoryFails() {
        launchWithFailure();

        onView(withId(R.id.recycler_notifications)).check(matches(hasChildCount(0)));
    }
}
