package com.example.eventlotteryapp.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Admin screen for viewing profile details and deleting a user profile.
 * Displays the selected user's basic information and provides
 * a confirmation step before deletion.
 */
public class AdminProfileDetailActivity extends AppCompatActivity {

    private TextView textDeviceId;
    private TextView textName;
    private TextView textEmail;
    private TextView textPhone;
    private Button btnDeleteProfile;
    private Button btnBack;
    private FirebaseFirestore db;
    private String deviceId;

    /**
     * Initializes the profile detail screen and loads profile data
     * passed from the admin profile list.
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
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();

        deviceId = getIntent().getStringExtra("deviceId");
        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");
        String phone = getIntent().getStringExtra("phone");

        textDeviceId.setText(deviceId == null || deviceId.isEmpty() ? "No device ID" : deviceId);
        textName.setText(name == null || name.isEmpty() ? "No name" : name);
        textEmail.setText(email == null || email.isEmpty() ? "No email" : email);
        textPhone.setText(phone == null || phone.isEmpty() ? "No phone" : phone);

        btnDeleteProfile.setOnClickListener(v -> showDeleteConfirmation());
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Shows a confirmation dialog before deleting the selected profile.
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
     * Deletes the selected user profile from Firestore.
     * Shows a success or failure toast depending on the result.
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
}