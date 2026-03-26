package com.example.eventlotteryapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import com.example.eventlotteryapp.R;
import android.provider.Settings;
import android.util.Log;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.value.LottieValueCallback;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.example.eventlotteryapp.ui.main.MainActivity;
import com.example.eventlotteryapp.utils.TestImageSeeder;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Entry point activity that determines where to route the user on app launch.
 *
 * <p>Auth flow:
 * <ol>
 *   <li>Reads the device's ANDROID_ID.</li>
 *   <li>Checks the {@code admins} collection — if a document exists, navigates to {@link AdminActivity}.</li>
 *   <li>Otherwise checks the {@code users} collection:
 *     <ul>
 *       <li>User found with a non-empty name → {@link MainActivity}</li>
 *       <li>User missing or profile incomplete → {@link ProfileSetupActivity}</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>No user document is pre-created here; profile creation is handled by {@link ProfileSetupActivity}.
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    /**
     * Abstraction over the Firestore admin-session check so tests can inject a fake
     * without needing a real Firebase connection.
     */
    public interface AdminCheckProvider {
        /** Calls back with {@code true} if an admin session exists for the device. */
        interface Callback { void onResult(boolean isAdmin); }
        void checkAdmin(String deviceId, Callback callback);
    }

    /**
     * Intercepts navigation in tests so no real Activity is launched.
     * When set, {@link #navigateTo} calls this instead of {@link #startActivity}.
     */
    public interface NavigationListener {
        void onNavigateTo(Class<?> destination);
    }

    // --- Test injection points ---

    /** Set by tests to replace the real Firebase admin check. Cleared in @After. */
    public static AdminCheckProvider adminCheckProviderForTest = null;
    /** Set by tests to replace the real UserRepository. Cleared in @After. */
    public static UserRepository userRepositoryForTest = null;
    /** Set to true by tests to skip gallery seeding. */
    public static boolean skipSeedingForTest = false;
    /** Set by tests to capture the navigation target without launching the real Activity. Cleared in @After. */
    public static NavigationListener navigationListenerForTest = null;

    // --- Instance fields ---

    private AdminCheckProvider adminCheckProvider;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        LottieAnimationView animation = findViewById(R.id.splashAnimation);
        int magenta = getResources().getColor(R.color.color_primary, null);
        animation.addValueCallback(
                new com.airbnb.lottie.model.KeyPath("**"),
                LottieProperty.COLOR,
                frameInfo -> {
                    int color = frameInfo.getStartValue();
                    int r = android.graphics.Color.red(color);
                    int g = android.graphics.Color.green(color);
                    int b = android.graphics.Color.blue(color);
                    // Keep white and near-white as-is, recolor everything else
                    if (r > 200 && g > 200 && b > 200) return color;
                    return magenta;
                }
        );

        // Use test doubles if injected, otherwise create real implementations
        if (adminCheckProviderForTest != null) {
            adminCheckProvider = adminCheckProviderForTest;
        } else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            adminCheckProvider = (deviceId, callback) ->
                    db.collection("admins").document(deviceId).get()
                            .addOnSuccessListener(doc -> callback.onResult(doc.exists()))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Admin check failed, falling back to user check", e);
                                callback.onResult(false);
                            });
        }

        userRepository = (userRepositoryForTest != null)
                ? userRepositoryForTest : new UserRepository();

        if (!skipSeedingForTest) {
            TestImageSeeder.seedIfNeeded(this);
        }

        startAuthFlow();
    }

    /**
     * Reads the device's ANDROID_ID and begins the auth routing flow.
     * First checks the {@code admins} collection; if the device has an active admin session,
     * navigates directly to {@link AdminActivity}. Otherwise delegates to {@link #checkRegularUser}.
     */
    private void startAuthFlow() {
        final String deviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        adminCheckProvider.checkAdmin(deviceId, isAdmin -> {
            if (isAdmin) {
                navigateTo(AdminActivity.class, deviceId);
            } else {
                checkRegularUser(deviceId);
            }
        });
    }

    /**
     * Fetches the user document for the given device ID from the {@code users} collection.
     * If the user exists and has a non-empty name, navigates to {@link MainActivity}.
     * If the document is missing or the profile is incomplete, navigates to {@link ProfileSetupActivity}.
     * On failure, also falls back to {@link ProfileSetupActivity} to avoid a dead end.
     * No user document is created here — that is handled by {@link ProfileSetupActivity}.
     *
     * @param deviceId the device's ANDROID_ID used to look up the user document
     */
    private void checkRegularUser(String deviceId) {
        userRepository.getUser(deviceId, new UserRepository.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                    navigateTo(MainActivity.class, deviceId);
                } else {
                    // No user doc, or profile incomplete — send to setup.
                    // We do NOT pre-create an empty doc here; ProfileSetupActivity handles creation.
                    navigateTo(ProfileSetupActivity.class, deviceId);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "User check failed", e);
                navigateTo(ProfileSetupActivity.class, deviceId);
            }
        });
    }

    /**
     * Navigates to the given activity class, passing the device ID as an extra.
     * Clears the back stack so the user cannot navigate back to the splash screen.
     *
     * @param destinationClass the activity class to navigate to
     * @param deviceId         the device's ANDROID_ID to pass as an intent extra
     */
    private void navigateTo(Class<?> destinationClass, String deviceId) {
        if (navigationListenerForTest != null) {
            navigationListenerForTest.onNavigateTo(destinationClass);
            finish();
            return;
        }
        Intent intent = new Intent(this, destinationClass);
        intent.putExtra("deviceId", deviceId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
