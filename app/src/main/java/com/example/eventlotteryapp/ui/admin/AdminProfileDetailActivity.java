package com.example.eventlotteryapp.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Admin screen for viewing profile details and deleting a user profile.
 * Displays the selected user's basic information and provides
 * a confirmation step before deletion.
 * User Stories Implemented:
 * US 03.02.01 As an administrator, I want to be able to remove profiles.
 *
 * @author Mazen
 */
public class AdminProfileDetailActivity extends AppCompatActivity {

    private TextView textDeviceId;
    private TextView textName;
    private TextView textEmail;
    private TextView textPhone;
    private Button btnDeleteProfile;
    private ImageButton buttonBack;
    private FirebaseFirestore db;
    private String deviceId;

    /**
     * Initializes the profile detail screen and populates fields from intent extras.
     *
     * @param savedInstanceState saved Android instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile_detail);

        textDeviceId = findViewById(R.id.textDeviceId);
        textName = findViewById(R.id.textName);
        textEmail = findViewById(R.id.textEmail);
        textPhone = findViewById(R.id.textPhone);
        btnDeleteProfile = findViewById(R.id.btnDeleteProfile);
        buttonBack = findViewById(R.id.button_back);

        db = FirebaseFirestore.getInstance();

        deviceId = getIntent().getStringExtra("deviceId");
        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");
        String phone = getIntent().getStringExtra("phone");

        textDeviceId.setText(displayText(deviceId, "No device ID"));
        textName.setText(displayText(name, "No name"));
        textEmail.setText(displayText(email, "No email"));
        textPhone.setText(displayText(phone, "No phone"));

        btnDeleteProfile.setOnClickListener(v -> showDeleteConfirmation());
        buttonBack.setOnClickListener(v -> finish());
    }

    /**
     * Shows a confirmation dialog before permanently deleting the user profile.
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete this profile?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the user's document from the {@code users} Firestore collection
     * and finishes the activity on success.
     */
    private void deleteProfile() {
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Missing device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Returns {@code value} if non-null and non-empty, otherwise returns {@code fallback}.
     *
     * @param value    the string to check
     * @param fallback the fallback string to use if value is blank
     * @return the display string
     */
    private String displayText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}