package com.example.eventlotteryapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.MainActivity;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPhone;
    private Button buttonSave;
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        // Get deviceId passed from SplashActivity
        deviceId = getIntent().getStringExtra("deviceId");
        db = FirebaseFirestore.getInstance();

        // Hook up views
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonSave = findViewById(R.id.buttonSaveProfile);

        buttonSave.setOnClickListener(v -> saveProfile());

        TextView textAdminLogin = findViewById(R.id.textAdminLogin);
        textAdminLogin.setOnClickListener(v -> showAdminLoginDialog());
    }

    private void saveProfile() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        // Validate required fields
        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Name is required");
            editTextName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        // Disable button to prevent double taps
        buttonSave.setEnabled(false);
        buttonSave.setText("Saving...");

        // Build the update map - only update these fields, keep role etc.
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone);

        db.collection("users")
                .document(deviceId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d("ProfileSetup", "Profile saved successfully!");
                    Toast.makeText(this,
                            "Welcome, " + name + "!",
                            Toast.LENGTH_SHORT).show();
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileSetup", "Failed to save: " + e.getMessage());
                    Toast.makeText(this,
                            "Failed to save profile. Please try again.",
                            Toast.LENGTH_SHORT).show();
                    buttonSave.setEnabled(true);
                    buttonSave.setText("Save Profile");
                });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("deviceId", deviceId);
        // Clear back stack so user can't go back to setup
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showAdminLoginDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Admin Login");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter admin password");
        builder.setView(input);

        builder.setPositiveButton("Login", (dialog, which) -> {
            String password = input.getText().toString();

            Log.d("ProfileSetup", "Password entered: " + password);

            if (password.equals("admin123")) {
                Log.d("ProfileSetup", "Password correct! Creating admin session...");

                // FIRST: Delete the user document if it exists
                db.collection("users")
                        .document(deviceId)
                        .delete()
                        .addOnSuccessListener(unused -> {
                            Log.d("ProfileSetup", "User document deleted");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ProfileSetup", "Failed to delete user: " + e.getMessage());
                        });

                // THEN: Create admin document
                Map<String, Object> adminSession = new HashMap<>();
                adminSession.put("deviceId", deviceId);
                adminSession.put("loginTimestamp", System.currentTimeMillis());

                Log.d("ProfileSetup", "Writing to admins/" + deviceId);

                db.collection("admins")
                        .document(deviceId)
                        .set(adminSession)
                        .addOnSuccessListener(unused -> {
                            Log.d("ProfileSetup", "Admin document created successfully!");
                            Toast.makeText(this, "Admin access granted!",
                                    Toast.LENGTH_SHORT).show();
                            goToAdmin();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ProfileSetup", "Failed to create admin doc: " + e.getMessage());
                            Toast.makeText(this, "Login failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
            } else {
                Log.d("ProfileSetup", "Password incorrect");
                Toast.makeText(this, "Invalid password",
                        Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void goToAdmin() {
        // We'll create AdminActivity later
        Intent intent = new Intent(this, AdminActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}