package com.example.eventlotteryapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.FirebaseConnector;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.example.eventlotteryapp.ui.fragments.ProfileFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UI tests for {@link ProfileFragment}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Display: all fields and buttons are visible on launch</li>
 *   <li>Load: fields are populated from the repository when a user is found</li>
 *   <li>Load: notifications button label reflects the stored preference</li>
 *   <li>Validation: save shows errors for empty name, empty email, invalid email</li>
 *   <li>Save: button is disabled during submission and re-enabled on success and failure</li>
 *   <li>Notifications: clicking the button shows a dialog; confirming updates the button label</li>
 *   <li>Delete: clicking the button shows a confirmation dialog; cancelling skips the repo call</li>
 *   <li>Correctness: getUser is called with the deviceId passed via arguments</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class ProfileFragmentTest {

    private static final String DEVICE_ID = "device123";

    @Mock UserRepository mockUserRepo;
    @Mock FirebaseConnector mockFirebaseConnector;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Default: device is not an admin
        doAnswer(invocation -> {
            FirebaseConnector.IsAdminCallback cb = invocation.getArgument(1);
            cb.onResult(false);
            return null;
        }).when(mockFirebaseConnector).isAdminDevice(any(), any());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a fake User with the given fields.
     */
    private User makeUser(String name, String email, String phone, boolean notificationsEnabled) {
        User u = new User();
        u.setDeviceId(DEVICE_ID);
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setNotificationsEnabled(notificationsEnabled);
        return u;
    }

    /**
     * Launches ProfileFragment with a mock that returns the given user on load.
     *
     * @param user the User object the mock repo will return, or null to simulate not found
     */
    private void launchWithUser(User user) {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<User> cb = invocation.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(mockUserRepo).getUser(any(), any());

        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                ProfileFragment fragment = new ProfileFragment();
                fragment.setUserRepository(mockUserRepo);
                fragment.setFirebaseConnector(mockFirebaseConnector);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                ProfileFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /**
     * Launches ProfileFragment with a mock that fires onFailure on load.
     */
    private void launchWithLoadFailure() {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<User> cb = invocation.getArgument(1);
            cb.onFailure(new Exception("Firestore unavailable"));
            return null;
        }).when(mockUserRepo).getUser(any(), any());

        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                ProfileFragment fragment = new ProfileFragment();
                fragment.setUserRepository(mockUserRepo);
                fragment.setFirebaseConnector(mockFirebaseConnector);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                ProfileFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    /**
     * Launches ProfileFragment with the device treated as admin-eligible.
     */
    private void launchWithAdminEligible(User user) {
        doAnswer(invocation -> {
            FirebaseConnector.IsAdminCallback cb = invocation.getArgument(1);
            cb.onResult(true);
            return null;
        }).when(mockFirebaseConnector).isAdminDevice(any(), any());

        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<User> userCb = invocation.getArgument(1);
            userCb.onSuccess(user);
            return null;
        }).when(mockUserRepo).getUser(any(), any());

        Bundle args = new Bundle();
        args.putString("deviceId", DEVICE_ID);

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                ProfileFragment fragment = new ProfileFragment();
                fragment.setUserRepository(mockUserRepo);
                fragment.setFirebaseConnector(mockFirebaseConnector);
                return fragment;
            }
        };

        FragmentScenario.launchInContainer(
                ProfileFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    /**
     * Name, email, and phone fields must be visible when the fragment opens.
     */
    @Test
    public void inputFields_areDisplayed() {
        launchWithUser(null);

        onView(withId(R.id.editTextName)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.editTextEmail)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPhone)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    /**
     * Save, notifications, event history, and delete buttons must all be visible.
     */
    @Test
    public void allButtons_areDisplayed() {
        launchWithUser(null);

        onView(withId(R.id.buttonSaveProfile)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.buttonOptOutNotifications)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.buttonEventHistory)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.buttonDeleteProfile)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Profile load tests
    // -----------------------------------------------------------------------

    /**
     * When the repository returns a user, name, email, and phone fields must be
     * pre-populated with the user's stored values.
     */
    @Test
    public void profileFields_arePopulated_whenUserLoadSucceeds() {
        launchWithUser(makeUser("Leyla Ahmed", "leyla@email.com", "780-555-0123", true));

        onView(withId(R.id.editTextName)).check(matches(withText("Leyla Ahmed")));
        onView(withId(R.id.editTextEmail)).check(matches(withText("leyla@email.com")));
        onView(withId(R.id.editTextPhone)).check(matches(withText("780-555-0123")));
    }

    /**
     * When the loaded user has notifications enabled, the button must say "Opt Out of Notifications".
     */
    @Test
    public void notificationsButton_showsOptOut_whenNotificationsEnabled() {
        launchWithUser(makeUser("Leyla Ahmed", "leyla@email.com", "", true));

        onView(withId(R.id.buttonOptOutNotifications))
                .perform(scrollTo())
                .check(matches(withText("Opt Out of Notifications")));
    }

    /**
     * When the loaded user has notifications disabled, the button must say "Opt In to Notifications".
     */
    @Test
    public void notificationsButton_showsOptIn_whenNotificationsDisabled() {
        launchWithUser(makeUser("Leyla Ahmed", "leyla@email.com", "", false));

        onView(withId(R.id.buttonOptOutNotifications))
                .perform(scrollTo())
                .check(matches(withText("Opt In to Notifications")));
    }

    /**
     * When the repository fires onFailure on load, fields must remain empty.
     */
    @Test
    public void profileFields_remainEmpty_whenUserLoadFails() {
        launchWithLoadFailure();

        onView(withId(R.id.editTextName)).check(matches(withText("")));
        onView(withId(R.id.editTextEmail)).check(matches(withText("")));
        onView(withId(R.id.editTextPhone)).check(matches(withText("")));
    }

    // -----------------------------------------------------------------------
    // Validation tests
    // -----------------------------------------------------------------------

    /**
     * Clicking save with an empty name field must show a "Name is required" error
     * on the name field without calling the repository.
     */
    @Test
    public void saveButton_showsError_whenNameIsEmpty() {
        launchWithUser(null);

        onView(withId(R.id.editTextName)).perform(scrollTo(), replaceText(""));
        onView(withId(R.id.editTextEmail)).perform(scrollTo(), replaceText("test@email.com"));
        onView(withId(R.id.buttonSaveProfile)).perform(scrollTo(), click());

        onView(withId(R.id.editTextName)).check(matches(hasErrorText("Name is required")));
        verify(mockUserRepo, never()).saveUserProfile(any(), any(), any(), any(), anyBoolean(), any());
    }

    /**
     * Clicking save with an empty email field must show an "Email is required" error
     * on the email field without calling the repository.
     */
    @Test
    public void saveButton_showsError_whenEmailIsEmpty() {
        launchWithUser(null);

        onView(withId(R.id.editTextName)).perform(scrollTo(), replaceText("Leyla Ahmed"));
        onView(withId(R.id.editTextEmail)).perform(scrollTo(), replaceText(""));
        onView(withId(R.id.buttonSaveProfile)).perform(scrollTo(), click());

        onView(withId(R.id.editTextEmail)).check(matches(hasErrorText("Email is required")));
        verify(mockUserRepo, never()).saveUserProfile(any(), any(), any(), any(), anyBoolean(), any());
    }

    /**
     * Clicking save with a malformed email must show a "Please enter a valid email" error
     * without calling the repository.
     */
    @Test
    public void saveButton_showsError_whenEmailIsInvalid() {
        launchWithUser(null);

        onView(withId(R.id.editTextName)).perform(scrollTo(), replaceText("Leyla Ahmed"));
        onView(withId(R.id.editTextEmail)).perform(scrollTo(), replaceText("not-an-email"));
        onView(withId(R.id.buttonSaveProfile)).perform(scrollTo(), click());

        onView(withId(R.id.editTextEmail)).check(matches(hasErrorText("Please enter a valid email")));
        verify(mockUserRepo, never()).saveUserProfile(any(), any(), any(), any(), anyBoolean(), any());
    }

    // -----------------------------------------------------------------------
    // Save flow tests
    // -----------------------------------------------------------------------

    /**
     * After a successful save the button must be re-enabled and show "Save Changes" again.
     * The mock calls onSuccess immediately so we can check the final state synchronously.
     */
    @Test
    public void saveButton_isReEnabled_afterSuccessfulSave() {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<Void> cb = invocation.getArgument(5);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).saveUserProfile(any(), any(), any(), any(), anyBoolean(), any());

        launchWithUser(null);

        onView(withId(R.id.editTextName)).perform(scrollTo(), replaceText("Leyla Ahmed"));
        onView(withId(R.id.editTextEmail)).perform(scrollTo(), replaceText("leyla@email.com"));
        onView(withId(R.id.buttonSaveProfile)).perform(scrollTo(), click());

        onView(withId(R.id.buttonSaveProfile))
                .check(matches(isEnabled()))
                .check(matches(withText("Save Changes")));
    }

    /**
     * After a failed save the button must be re-enabled and show "Save Changes" again,
     * not stay stuck in the "Saving..." disabled state.
     */
    @Test
    public void saveButton_isReEnabled_afterFailedSave() {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<Void> cb = invocation.getArgument(5);
            cb.onFailure(new Exception("Network error"));
            return null;
        }).when(mockUserRepo).saveUserProfile(any(), any(), any(), any(), anyBoolean(), any());

        launchWithUser(null);

        onView(withId(R.id.editTextName)).perform(scrollTo(), replaceText("Leyla Ahmed"));
        onView(withId(R.id.editTextEmail)).perform(scrollTo(), replaceText("leyla@email.com"));
        onView(withId(R.id.buttonSaveProfile)).perform(scrollTo(), click());

        onView(withId(R.id.buttonSaveProfile))
                .check(matches(isEnabled()))
                .check(matches(withText("Save Changes")));
    }

    // -----------------------------------------------------------------------
    // Notifications dialog tests
    // -----------------------------------------------------------------------

    /**
     * Clicking the notifications button when notifications are enabled must show
     * the opt-out confirmation dialog.
     */
    @Test
    public void clickingNotificationsButton_showsDialog_whenEnabled() {
        launchWithUser(makeUser("Leyla Ahmed", "leyla@email.com", "", true));

        onView(withId(R.id.buttonOptOutNotifications)).perform(scrollTo(), click());

        // The dialog title should be visible
        onView(withText("Opt Out of Notifications")).check(matches(isDisplayed()));
    }

    /**
     * After confirming opt-out in the dialog, the notifications button label must
     * switch to "Opt In to Notifications" to reflect the new state.
     */
    @Test
    public void confirmingOptOut_updatesNotificationsButtonLabel() {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).setNotificationsEnabled(any(), eq(false), any());

        launchWithUser(makeUser("Leyla Ahmed", "leyla@email.com", "", true));

        // Open dialog and confirm
        onView(withId(R.id.buttonOptOutNotifications)).perform(scrollTo(), click());
        onView(withText("Opt Out")).perform(click());

        // Button label must now reflect opted-out state
        onView(withId(R.id.buttonOptOutNotifications))
                .check(matches(withText("Opt In to Notifications")));
    }

    /**
     * After confirming opt-in in the dialog, the notifications button label must
     * switch to "Opt Out of Notifications" to reflect the new state.
     */
    @Test
    public void confirmingOptIn_updatesNotificationsButtonLabel() {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<Void> cb = invocation.getArgument(2);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).setNotificationsEnabled(any(), eq(true), any());

        launchWithUser(makeUser("Leyla Ahmed", "leyla@email.com", "", false));

        // Open dialog and confirm
        onView(withId(R.id.buttonOptOutNotifications)).perform(scrollTo(), click());
        onView(withText("Opt In")).perform(click());

        // Button label must now reflect opted-in state
        onView(withId(R.id.buttonOptOutNotifications))
                .check(matches(withText("Opt Out of Notifications")));
    }

    // -----------------------------------------------------------------------
    // Delete profile dialog tests
    // -----------------------------------------------------------------------

    /**
     * Clicking the delete button must show the "Delete Profile" confirmation dialog.
     */
    @Test
    public void clickingDeleteButton_showsConfirmationDialog() {
        launchWithUser(null);

        onView(withId(R.id.buttonDeleteProfile)).perform(scrollTo(), click());

        onView(withText("Delete Profile")).check(matches(isDisplayed()));
    }

    /**
     * Pressing Cancel on the delete dialog must NOT call the repository.
     * The user's profile must remain intact.
     */
    @Test
    public void cancellingDelete_doesNotCallRepository() {
        launchWithUser(null);

        onView(withId(R.id.buttonDeleteProfile)).perform(scrollTo(), click());
        onView(withText("Cancel")).perform(click());

        verify(mockUserRepo, never()).deleteUser(any(), any());
    }

    // -----------------------------------------------------------------------
    // Correctness test
    // -----------------------------------------------------------------------

    /**
     * The fragment must pass the deviceId from its arguments to
     * {@link UserRepository#getUser}, ensuring the correct user's profile is loaded.
     */
    @Test
    public void getUser_isCalledWithCorrectDeviceId() {
        launchWithUser(null);

        verify(mockUserRepo).getUser(eq(DEVICE_ID), any());
    }

    // -----------------------------------------------------------------------
    // Admin panel button tests
    // -----------------------------------------------------------------------

    /**
     * The admin panel button must be hidden (GONE) when the device is not in the
     * adminDevices collection.
     */
    @Test
    public void adminPanelButton_isHidden_byDefault() {
        launchWithUser(null);

        onView(withId(R.id.buttonAdminPanel))
                .check(matches(not(isDisplayed())));
    }

    /**
     * The admin panel button must become visible when {@link FirebaseConnector#isAdminDevice}
     * returns true for this device.
     */
    @Test
    public void adminPanelButton_isVisible_whenDeviceIsAdminEligible() {
        launchWithAdminEligible(null);

        onView(withId(R.id.buttonAdminPanel))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    /**
     * Clicking the admin panel button must launch {@link AdminActivity}.
     */
    @Test
    public void adminPanelButton_click_navigatesToAdminActivity() {
        launchWithAdminEligible(null);

        androidx.test.espresso.intent.Intents.init();
        try {
            androidx.test.espresso.intent.Intents.intending(
                    hasComponent(AdminActivity.class.getName()))
                    .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            onView(withId(R.id.buttonAdminPanel)).perform(scrollTo(), click());

            intended(hasComponent(AdminActivity.class.getName()));
        } finally {
            androidx.test.espresso.intent.Intents.release();
        }
    }

    // -----------------------------------------------------------------------
    // Notifications dialog — cancel path
    // -----------------------------------------------------------------------

    /**
     * Pressing Cancel on the notifications dialog must NOT call
     * {@link UserRepository#setNotificationsEnabled}.
     */
    @Test
    public void notificationsDialog_cancel_doesNotCallRepository() {
        launchWithUser(makeUser("Leyla Ahmed", "leyla@email.com", "", true));

        onView(withId(R.id.buttonOptOutNotifications)).perform(scrollTo(), click());
        onView(withText("Cancel")).perform(click());

        verify(mockUserRepo, never()).setNotificationsEnabled(any(), any(Boolean.class), any());
    }

    // -----------------------------------------------------------------------
    // Delete profile — confirm path
    // -----------------------------------------------------------------------

    /**
     * Confirming the delete dialog must call {@link UserRepository#deleteUser}
     * with the correct device ID.
     */
    @Test
    public void deleteProfile_confirm_callsDeleteUser() {
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<Void> cb = invocation.getArgument(1);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).deleteUser(any(), any());

        launchWithUser(null);

        onView(withId(R.id.buttonDeleteProfile)).perform(scrollTo(), click());
        onView(withText("Delete")).perform(click());

        verify(mockUserRepo).deleteUser(eq(DEVICE_ID), any());
    }
}
