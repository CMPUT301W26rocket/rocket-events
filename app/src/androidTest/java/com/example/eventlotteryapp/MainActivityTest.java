package com.example.eventlotteryapp;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.ui.main.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * UI tests for {@link MainActivity}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: bottom navigation bar is visible on launch</li>
 *   <li>Default: HomeFragment is loaded on first launch</li>
 *   <li>Navigation: clicking each bottom nav tab shows the correct fragment</li>
 *   <li>Notifications tab: does not crash when clicked (not yet implemented)</li>
 * </ul>
 *
 * <p>Note: MainActivity creates fragments internally so repository mocking is not possible
 * here. Fragments make real Firebase calls in the background but handle failures gracefully,
 * so structural view checks complete before any Firebase response is needed.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private static final String DEVICE_ID = "test_device_123";

    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra("deviceId", DEVICE_ID);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    /**
     * The bottom navigation bar must be visible when MainActivity opens.
     */
    @Test
    public void bottomNavigation_isDisplayed() {
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
    }

    /**
     * The fragment container must be present in the layout.
     */
    @Test
    public void fragmentContainer_isDisplayed() {
        onView(withId(R.id.fragment_container)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Default fragment test
    // -----------------------------------------------------------------------

    /**
     * On first launch, HomeFragment must be loaded by default.
     * Verified by checking that HomeFragment's events RecyclerView is visible.
     */
    @Test
    public void homeFragment_isLoadedByDefault() {
        onView(withId(R.id.recycler_events)).check(matches(isDisplayed()));
    }

    /**
     * The Home tab header "Upcoming Events" must be visible on launch.
     */
    @Test
    public void homeFragment_headerText_isDisplayed() {
        onView(withId(R.id.text_events_header)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Tab navigation tests
    // -----------------------------------------------------------------------

    /**
     * Clicking the My Events tab must show MyEventsFragment.
     * Verified by checking that MyEventsFragment's RecyclerView is visible.
     */
    @Test
    public void clickingMyEventsTab_showsMyEventsFragment() {
        onView(withId(R.id.nav_my_events)).perform(click());

        onView(withId(R.id.recycler_my_events)).check(matches(isDisplayed()));
    }

    /**
     * Clicking the My Events tab must show the "My Hosted Events" header.
     */
    @Test
    public void clickingMyEventsTab_showsCorrectHeader() {
        onView(withId(R.id.nav_my_events)).perform(click());

        onView(withId(R.id.text_my_events_header)).check(matches(isDisplayed()));
    }

    /**
     * Clicking the Profile tab must show ProfileFragment.
     * Verified by checking that the name input field is visible.
     */
    @Test
    public void clickingProfileTab_showsProfileFragment() {
        onView(withId(R.id.nav_profile)).perform(click());

        onView(withId(R.id.editTextName)).check(matches(isDisplayed()));
    }

    /**
     * Clicking the Profile tab must show the save profile button.
     */
    @Test
    public void clickingProfileTab_showsSaveButton() {
        onView(withId(R.id.nav_profile)).perform(click());

        onView(withId(R.id.buttonSaveProfile)).check(matches(isDisplayed()));
    }

    /**
     * Clicking the Notifications tab must not crash the app.
     * The tab is not yet implemented so it returns false and no fragment is swapped,
     * meaning the previously loaded fragment remains visible.
     */
    @Test
    public void clickingNotificationsTab_doesNotCrash() {
        onView(withId(R.id.nav_notifications)).perform(click());

        // No fragment swap happens — Home fragment still visible
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
    }

    /**
     * After navigating to Profile and then clicking Home, HomeFragment must
     * be shown again with its RecyclerView visible.
     */
    @Test
    public void clickingHomeTab_afterNavigatingAway_showsHomeFragment() {
        onView(withId(R.id.nav_profile)).perform(click());
        onView(withId(R.id.nav_home)).perform(click());

        onView(withId(R.id.recycler_events)).check(matches(isDisplayed()));
    }
}
