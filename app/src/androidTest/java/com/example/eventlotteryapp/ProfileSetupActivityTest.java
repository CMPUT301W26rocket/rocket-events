package com.example.eventlotteryapp;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.ui.auth.ProfileSetupActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * UI tests for {@link ProfileSetupActivity}.
 * Tests form validation without requiring Firebase to respond.
 */
@RunWith(AndroidJUnit4.class)
public class ProfileSetupActivityTest {

    /** Launches ProfileSetupActivity with a fake device ID. */
    private ActivityScenario<ProfileSetupActivity> launchActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                ProfileSetupActivity.class);
        intent.putExtra("deviceId", "test_device_123");
        return ActivityScenario.launch(intent);
    }

    @Test
    public void allFields_areDisplayed() {
        // Launch the profile setup screen
        launchActivity();

        // Check all input fields are visible
        onView(withId(R.id.editTextName)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPhone)).check(matches(isDisplayed()));

        // Check the save button is visible
        onView(withId(R.id.buttonSaveProfile)).check(matches(isDisplayed()));

        // Check the admin login link is visible
        onView(withId(R.id.textAdminLogin)).check(matches(isDisplayed()));
    }

    @Test
    public void saveProfile_withEmptyName_showsError() {
        // Launch the profile setup screen
        launchActivity();

        // Leave name field empty
        onView(withId(R.id.editTextName)).perform(clearText(), closeSoftKeyboard());

        // Click Save Profile
        onView(withId(R.id.buttonSaveProfile)).perform(click());

        // Check that the name field shows the required error
        onView(withId(R.id.editTextName)).check(matches(hasErrorText("Name is required")));
    }

    @Test
    public void saveProfile_withEmptyEmail_showsError() {
        // Launch the profile setup screen
        launchActivity();

        // Type a valid name
        onView(withId(R.id.editTextName)).perform(typeText("Jane Smith"), closeSoftKeyboard());

        // Leave email field empty
        onView(withId(R.id.editTextEmail)).perform(clearText(), closeSoftKeyboard());

        // Click Save Profile
        onView(withId(R.id.buttonSaveProfile)).perform(click());

        // Check that the email field shows the required error
        onView(withId(R.id.editTextEmail)).check(matches(hasErrorText("Email is required")));
    }

    @Test
    public void saveProfile_withInvalidEmail_showsError() {
        // Launch the profile setup screen
        launchActivity();

        // Type a valid name
        onView(withId(R.id.editTextName)).perform(typeText("Jane Smith"), closeSoftKeyboard());

        // Type an invalid email (no @ symbol)
        onView(withId(R.id.editTextEmail)).perform(typeText("notanemail"), closeSoftKeyboard());

        // Click Save Profile
        onView(withId(R.id.buttonSaveProfile)).perform(click());

        // Check that the email field shows the invalid email error
        onView(withId(R.id.editTextEmail)).check(matches(hasErrorText("Please enter a valid email")));
    }

    @Test
    public void saveButton_hasCorrectLabel() {
        // Launch the profile setup screen
        launchActivity();

        // Check that the save button shows the correct text
        onView(withId(R.id.buttonSaveProfile)).check(matches(withText("Save Profile")));
    }

    @Test
    public void adminLoginLink_isClickable() {
        // Launch the profile setup screen
        launchActivity();

        // Check the admin login link is visible
        onView(withId(R.id.textAdminLogin)).check(matches(isDisplayed()));

        // Click the admin login link
        onView(withId(R.id.textAdminLogin)).perform(click());

        // Check that the admin login dialog appears
        onView(withText("Admin Login")).check(matches(isDisplayed()));
    }
}
