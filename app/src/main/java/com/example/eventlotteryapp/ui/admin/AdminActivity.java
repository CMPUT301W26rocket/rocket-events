package com.example.eventlotteryapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.ui.auth.SplashActivity;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Main administrator dashboard for the application.
 * Provides navigation to admin management screens for events, profiles,
 * organizers, and images, and supports administrator logout.
 *
 * <p>Logout removes the device's document from the {@code admins} Firestore collection.
 * {@link SplashActivity} then re-evaluates whether to route the device as a regular user
 * or back to profile setup.
 * User Stories Left:
 * US 03.08.01 As an administrator, I want to review logs of all notifications sent to entrants by organizers.
 *
 * @author Leyla
 * @author Mazen
 */
public class AdminActivity extends AppCompatActivity {

    private View cardViewEvents;
    private View cardViewProfiles;
    private View cardViewOrganizers;
    private View cardViewImages;
    private View cardViewLogs;
    private TextView textLogout;
    private ImageButton buttonBackAdmin;

    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = FirebaseFirestore.getInstance();
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        buttonBackAdmin = findViewById(R.id.button_back_admin);
        cardViewEvents = findViewById(R.id.card_view_events);
        cardViewProfiles = findViewById(R.id.card_view_profiles);
        cardViewOrganizers = findViewById(R.id.card_view_organizers);
        cardViewImages = findViewById(R.id.card_view_images);
        cardViewLogs = findViewById(R.id.card_view_logs);
        textLogout = findViewById(R.id.text_logout);

        buttonBackAdmin.setOnClickListener(v -> finish());

        cardViewEvents.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminBrowseEventsActivity.class);
            startActivity(intent);
        });

        cardViewProfiles.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminBrowseProfilesActivity.class);
            startActivity(intent);
        });

        cardViewOrganizers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminBrowseOrganizersActivity.class);
            startActivity(intent);
        });

        cardViewImages.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminBrowseImagesActivity.class);
            startActivity(intent);
        });

        cardViewLogs.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminNotificationLogsActivity.class);
            startActivity(intent);
        });
        textLogout.setOnClickListener(v -> logoutAdmin());
    }

    /**
     * Ends the admin session by deleting the device's document from the {@code admins} collection.
     * Navigates to {@link SplashActivity} which re-evaluates the device's role.
     */
    private void logoutAdmin() {
        db.collection("admins").document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AdminActivity.this, SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Logout failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}