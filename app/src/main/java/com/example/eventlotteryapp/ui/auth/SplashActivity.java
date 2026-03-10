package com.example.eventlotteryapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.example.eventlotteryapp.ui.main.MainActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private UserRepository userRepository;
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();
        startAuthFlow();
    }

    private void startAuthFlow() {
        final String deviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        // Check admins collection first
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

    private void navigateTo(Class<?> destinationClass, String deviceId) {
        Intent intent = new Intent(this, destinationClass);
        intent.putExtra("deviceId", deviceId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
