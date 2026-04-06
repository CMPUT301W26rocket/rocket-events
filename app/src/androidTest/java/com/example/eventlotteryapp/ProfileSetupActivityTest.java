package com.example.eventlotteryapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.example.eventlotteryapp.ui.auth.ProfileSetupActivity;
import com.example.eventlotteryapp.ui.main.MainActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.junit.Before;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;

/**
 * UI tests for {@link ProfileSetupActivity}.
 * Tests form validation and navigation without requiring Firebase to respond.
 *
 * <p>Note: The admin login success flow (correct password → {@link com.example.eventlotteryapp.ui.admin.AdminActivity})
 * is not tested here because it requires a real Firestore write to create the admin session,
 * which cannot be mocked since {@code db} is used directly in the activity rather than
 * through a repository. Full intent testing for this flow will be covered in Part 4
 * using the Firebase Emulator Suite.
 */
@RunWith(AndroidJUnit4.class)
public class ProfileSetupActivityTest {

    @Mock UserRepository mockUserRepo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

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

    /**
     * Saving a valid profile must navigate to {@link MainActivity}.
     *
     * <p>The mock repository fires onSuccess immediately so we can intercept the intent
     * without waiting for a real Firestore response.
     */
    @Test
    public void saveProfile_success_navigatesToMainActivity() {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<Void> cb = invocation.getArgument(5);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).saveUserProfile(any(), any(), any(), any(), anyBoolean(), any());

        ActivityScenario<ProfileSetupActivity> scenario = launchActivity();
        // Replace the real repo with our mock before clicking Save
        scenario.onActivity(activity -> activity.setUserRepository(mockUserRepo));

        Intents.init();
        try {
            intending(hasComponent(MainActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.editTextName)).perform(replaceText("Leyla Ahmed"), closeSoftKeyboard());
            onView(withId(R.id.editTextEmail)).perform(replaceText("leyla@email.com"), closeSoftKeyboard());
            onView(withId(R.id.buttonSaveProfile)).perform(click());

            intended(hasComponent(MainActivity.class.getName()));
        } finally {
            Intents.release();
        }
    }

    /**
     * Entering a wrong admin password must NOT navigate away — the admin dialog
     * is dismissed and the setup screen remains visible.
     */
    @Test
    public void adminLogin_withWrongPassword_staysOnSetupScreen() {
        launchActivity();

        // Open the admin login dialog
        onView(withId(R.id.textAdminLogin)).perform(click());

        // Type the wrong password
        onView(withHint("Enter admin password")).perform(replaceText("wrongpassword"), closeSoftKeyboard());

        // Click Login
        onView(withText("Login")).perform(click());

        // Setup screen is still visible — navigation did not happen
        onView(withId(R.id.buttonSaveProfile)).check(matches(isDisplayed()));
    }
}
