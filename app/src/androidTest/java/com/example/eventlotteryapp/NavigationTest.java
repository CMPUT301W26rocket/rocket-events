package com.example.eventlotteryapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.auth.ProfileSetupActivity;
import com.example.eventlotteryapp.ui.fragments.HomeFragment;
import com.example.eventlotteryapp.ui.fragments.ProfileFragment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Navigation tests that verify fragment-to-fragment and fragment-to-Activity transitions.
 *
 * <p>Uses {@link NavigationTestContainerActivity} — a minimal test-only activity with a real
 * {@code R.id.fragment_container} — so fragment navigation calls actually work.
 *
 * <p>Only fragments whose destination does NOT create a real Firebase connection are
 * tested here. Fragments that navigate to details screens (EntrantEventDetailsFragment,
 * OrganizerEventDetailsFragment, EventHistoryFragment) are excluded because those
 * destinations open a Firestore gRPC watch stream that conflicts with the test classpath.
 *
 * <p>Tests covered:
 * <ul>
 *   <li>HomeFragment → CreateEventFragment (create button click)</li>
 *   <li>ProfileFragment → ProfileSetupActivity (delete profile confirmed) — intent test</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class NavigationTest {

    private static final String DEVICE_ID = "device123";

    @Mock EventRepository mockEventRepo;
    @Mock UserRepository mockUserRepo;

    private ActivityScenario<NavigationTestContainerActivity> scenario;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        scenario = ActivityScenario.launch(NavigationTestContainerActivity.class);
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private User makeUser() {
        User u = new User();
        u.setDeviceId(DEVICE_ID);
        u.setName("Leyla Ahmed");
        u.setEmail("leyla@email.com");
        u.setNotificationsEnabled(true);
        return u;
    }

    /**
     * Places a fragment into NavigationTestContainerActivity's fragment_container on the main thread.
     */
    private void placeFragment(androidx.fragment.app.Fragment fragment) {
        scenario.onActivity(activity ->
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commitNow()
        );
    }

    // -----------------------------------------------------------------------
    // HomeFragment navigation
    // -----------------------------------------------------------------------

    /**
     * Clicking the Create Event button in HomeFragment must navigate to
     * CreateEventFragment. Verified by checking that the event title input field appears.
     * CreateEventFragment only contacts Firebase when the form is submitted, not on load,
     * so this test is safe to run without a real Firebase connection.
     */
    @Test
    public void homeFragment_createEventButton_navigatesToCreateEventFragment() {
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<java.util.List<Event>> cb = invocation.getArgument(0);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(mockEventRepo).getAllEvents(any());

        HomeFragment fragment = new HomeFragment();
        fragment.setEventRepository(mockEventRepo);
        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);
        fragment.setArguments(args);
        placeFragment(fragment);

        onView(withId(R.id.button_open_create_event)).perform(click());

        // CreateEventFragment's title field — its presence confirms navigation succeeded
        onView(withId(R.id.edit_event_title)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // ProfileFragment intent test
    // -----------------------------------------------------------------------

    /**
     * Confirming profile deletion in ProfileFragment must start {@link ProfileSetupActivity}.
     *
     * <p>This is an intent test — {@code Intents.init()} intercepts the outgoing intent so
     * ProfileSetupActivity never actually launches. {@code intending(...).respondWith(...)}
     * stubs the response so the test doesn't block.
     */
    @Test
    public void profileFragment_confirmDelete_startsProfileSetupActivity() {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<User> cb = invocation.getArgument(1);
            cb.onSuccess(makeUser());
            return null;
        }).when(mockUserRepo).getUser(any(), any());

        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<Void> cb = invocation.getArgument(1);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).deleteUser(any(), any());

        ProfileFragment fragment = new ProfileFragment();
        fragment.setUserRepository(mockUserRepo);
        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);
        fragment.setArguments(args);
        placeFragment(fragment);

        Intents.init();
        try {
            // Stub so ProfileSetupActivity doesn't actually launch
            intending(hasComponent(ProfileSetupActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.buttonDeleteProfile)).perform(scrollTo(), click());
            onView(withText("Delete")).perform(click());

            intended(hasComponent(ProfileSetupActivity.class.getName()));
        } finally {
            Intents.release();
        }
    }
}
