package com.example.eventlotteryapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import com.example.eventlotteryapp.R;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.ui.auth.SplashActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {
    private Button btnBrowseEvents, btnBrowseProfiles, btnBrowseOrganizers, btnBrowseImages, btnViewLogs, btnLogout;

    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        btnBrowseEvents = findViewById(R.id.btnBrowseEvents);
        btnBrowseProfiles = findViewById(R.id.btnBrowseProfiles);
        btnBrowseImages = findViewById(R.id.btnBrowseImages);
        btnViewLogs = findViewById(R.id.btnViewLogs);
        btnLogout = findViewById(R.id.btnLogout);
        btnBrowseOrganizers = findViewById(R.id.btnBrowseOrganizers);
        btnBrowseEvents.setOnClickListener(v -> {
            Toast.makeText(this, "Browse Events screen next", Toast.LENGTH_SHORT).show();
        });

        btnBrowseProfiles.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminBrowseProfilesActivity.class);
            startActivity(intent);
        });
        btnBrowseImages.setOnClickListener(v -> {
            Toast.makeText(this, "Browse Images coming soon", Toast.LENGTH_SHORT).show();
        });

        btnViewLogs.setOnClickListener(v -> {
            Toast.makeText(this, "Notification Logs coming soon", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> logoutAdmin());

        btnBrowseOrganizers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminBrowseOrganizersActivity.class);
            startActivity(intent);
        });
    }

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