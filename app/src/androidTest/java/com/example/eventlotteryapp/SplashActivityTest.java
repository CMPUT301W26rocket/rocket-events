package com.example.eventlotteryapp;

import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.example.eventlotteryapp.ui.auth.ProfileSetupActivity;
import com.example.eventlotteryapp.ui.auth.SplashActivity;
import com.example.eventlotteryapp.ui.main.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Intent-routing tests for {@link SplashActivity}.
 *
 * <p>The admin check, UserRepository, and navigation are all injected via static test fields
 * so no real Firebase connection is made and no real Activity is launched.
 * {@link SplashActivity#skipSeedingForTest} prevents gallery writes during testing.
 *
 * <p>Each test verifies that the correct destination Activity is chosen depending on
 * the result of the admin check and the user lookup.
 */
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    @Mock UserRepository mockUserRepo;

    /** Captured by the NavigationListener — the class SplashActivity tried to navigate to. */
    private Class<?> navigatedTo;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        navigatedTo = null;
        SplashActivity.skipSeedingForTest = true;
        SplashActivity.navigationListenerForTest = dest -> navigatedTo = dest;
    }

    @After
    public void tearDown() {
        SplashActivity.adminCheckProviderForTest = null;
        SplashActivity.userRepositoryForTest = null;
        SplashActivity.skipSeedingForTest = false;
        SplashActivity.navigationListenerForTest = null;
    }

    // -----------------------------------------------------------------------
    // Admin session routing
    // -----------------------------------------------------------------------

    /**
     * When the device has an active admin session, SplashActivity must navigate to AdminActivity.
     */
    @Test
    public void adminSession_exists_navigatesToAdminActivity() {
        SplashActivity.adminCheckProviderForTest = (deviceId, callback) -> callback.onResult(true);
        SplashActivity.userRepositoryForTest = mockUserRepo;

        launchSplash();

        assertEquals(AdminActivity.class, navigatedTo);
    }

    // -----------------------------------------------------------------------
    // Regular user routing
    // -----------------------------------------------------------------------

    /**
     * When no admin session exists and the user has a name, SplashActivity must navigate to MainActivity.
     */
    @Test
    public void noAdminSession_userWithName_navigatesToMainActivity() {
        SplashActivity.adminCheckProviderForTest = (deviceId, callback) -> callback.onResult(false);
        SplashActivity.userRepositoryForTest = mockUserRepo;

        User user = new User();
        user.setName("Alice");
        doAnswer(inv -> {
            UserRepository.FirestoreCallback<User> cb = inv.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(mockUserRepo).getUser(anyString(), any());

        launchSplash();

        assertEquals(MainActivity.class, navigatedTo);
    }

    /**
     * When no admin session exists and the user has no name, SplashActivity must navigate to
     * ProfileSetupActivity so the profile can be completed.
     */
    @Test
    public void noAdminSession_userWithEmptyName_navigatesToProfileSetup() {
        SplashActivity.adminCheckProviderForTest = (deviceId, callback) -> callback.onResult(false);
        SplashActivity.userRepositoryForTest = mockUserRepo;

        User user = new User();
        user.setName("");
        doAnswer(inv -> {
            UserRepository.FirestoreCallback<User> cb = inv.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(mockUserRepo).getUser(anyString(), any());

        launchSplash();

        assertEquals(ProfileSetupActivity.class, navigatedTo);
    }

    /**
     * When no admin session exists and the user document is not found (null),
     * SplashActivity must navigate to ProfileSetupActivity.
     */
    @Test
    public void noAdminSession_noUserDoc_navigatesToProfileSetup() {
        SplashActivity.adminCheckProviderForTest = (deviceId, callback) -> callback.onResult(false);
        SplashActivity.userRepositoryForTest = mockUserRepo;

        doAnswer(inv -> {
            UserRepository.FirestoreCallback<User> cb = inv.getArgument(1);
            cb.onSuccess(null);
            return null;
        }).when(mockUserRepo).getUser(anyString(), any());

        launchSplash();

        assertEquals(ProfileSetupActivity.class, navigatedTo);
    }

    /**
     * When no admin session exists and the user lookup fails,
     * SplashActivity must fall back to ProfileSetupActivity to avoid a dead end.
     */
    @Test
    public void noAdminSession_userCheckFails_navigatesToProfileSetup() {
        SplashActivity.adminCheckProviderForTest = (deviceId, callback) -> callback.onResult(false);
        SplashActivity.userRepositoryForTest = mockUserRepo;

        doAnswer(inv -> {
            UserRepository.FirestoreCallback<User> cb = inv.getArgument(1);
            cb.onFailure(new Exception("Firestore unavailable"));
            return null;
        }).when(mockUserRepo).getUser(anyString(), any());

        launchSplash();

        assertEquals(ProfileSetupActivity.class, navigatedTo);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Launches SplashActivity and waits for the main thread to go idle.
     * Uses startActivitySync so the call returns as soon as onCreate() completes,
     * rather than waiting for RESUMED state (which SplashActivity never reaches because
     * it calls finish() in onCreate()).
     */
    private void launchSplash() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instrumentation.startActivitySync(intent);
        instrumentation.waitForIdleSync();
    }
}
