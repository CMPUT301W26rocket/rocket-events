package com.example.eventlotteryapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.MainActivity;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No layout needed, but consider adding a simple layout with a logo/spinner
        // if Firestore checks take more than 1 second.

        db = FirebaseFirestore.getInstance();
        startAuthFlow();
    }

    private void startAuthFlow() {
        final String deviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        // 1. Check Admin First
        db.collection("admins").document(deviceId).get()
                .addOnSuccessListener(adminDoc -> {
                    if (adminDoc.exists()) {
                        navigateTo(AdminActivity.class, deviceId);
                    } else {
                        checkRegularUser(deviceId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Admin check failed, falling back to user check", e);
                    checkRegularUser(deviceId);
                });
    }

    private void checkRegularUser(String deviceId) {
        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                            navigateTo(MainActivity.class, deviceId);
                        } else {
                            navigateTo(ProfileSetupActivity.class, deviceId);
                        }
                    } else {
                        createNewUser(deviceId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "User check failed", e);
                    // If Firestore fails, we usually send them to setup to try again
                    navigateTo(ProfileSetupActivity.class, deviceId);
                });
    }

    private void createNewUser(String deviceId) {
        User newUser = new User(deviceId, "", "", "");
        db.collection("users").document(deviceId).set(newUser)
                .addOnSuccessListener(v -> navigateTo(ProfileSetupActivity.class, deviceId))
                .addOnFailureListener(e -> navigateTo(ProfileSetupActivity.class, deviceId));
    }

    /**
     * One "Clean" method to handle all navigation.
     * This uses Flags to ensure the user can't "Go Back" to the Splash screen.
     */
    private void navigateTo(Class<?> destinationClass, String deviceId) {
        Intent intent = new Intent(this, destinationClass);
        intent.putExtra("deviceId", deviceId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}