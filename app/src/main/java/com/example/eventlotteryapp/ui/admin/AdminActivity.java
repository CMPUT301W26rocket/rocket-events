package com.example.eventlotteryapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.ui.auth.SplashActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    private String deviceId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        deviceId = getIntent().getStringExtra("deviceId");
        db = FirebaseFirestore.getInstance();

        // Hook up views
        TextView textWelcome = findViewById(R.id.textAdminWelcome);
        Button btnBrowseEvents = findViewById(R.id.btnBrowseEvents);
        Button btnBrowseProfiles = findViewById(R.id.btnBrowseProfiles);
        Button btnBrowseImages = findViewById(R.id.btnBrowseImages);
        Button btnViewLogs = findViewById(R.id.btnViewLogs);
        Button btnLogout = findViewById(R.id.btnLogout);

        textWelcome.setText("Welcome, Admin!");

        // Placeholder actions for now
        btnBrowseEvents.setOnClickListener(v ->
                Toast.makeText(this, "Browse Events - Coming Soon", Toast.LENGTH_SHORT).show()
        );

        btnBrowseProfiles.setOnClickListener(v ->
                Toast.makeText(this, "Browse Profiles - Coming Soon", Toast.LENGTH_SHORT).show()
        );

        btnBrowseImages.setOnClickListener(v ->
                Toast.makeText(this, "Browse Images - Coming Soon", Toast.LENGTH_SHORT).show()
        );

        btnViewLogs.setOnClickListener(v ->
                Toast.makeText(this, "View Notification Logs - Coming Soon", Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        // Remove from admins collection
        db.collection("admins")
                .document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Admin logged out", Toast.LENGTH_SHORT).show();
                    // Go back to splash - it will reload the user as a regular user
                    Intent intent = new Intent(this, SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}