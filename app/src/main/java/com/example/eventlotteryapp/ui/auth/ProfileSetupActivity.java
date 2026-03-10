package com.example.eventlotteryapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.example.eventlotteryapp.ui.main.MainActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSetupActivity";

    private EditText editTextName, editTextEmail, editTextPhone;
    private Button buttonSave;
    private UserRepository userRepository;
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        deviceId = getIntent().getStringExtra("deviceId");
        userRepository = new UserRepository();
        db = FirebaseFirestore.getInstance();

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

        buttonSave.setEnabled(false);
        buttonSave.setText("Saving...");

        // set+merge creates the doc if it doesn't exist, or updates existing fields
        userRepository.saveUserProfile(deviceId, name, email, phone,
                new UserRepository.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "Profile saved successfully");
                        Toast.makeText(ProfileSetupActivity.this,
                                "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                        goToMain();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to save profile: " + e.getMessage());
                        Toast.makeText(ProfileSetupActivity.this,
                                "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show();
                        buttonSave.setEnabled(true);
                        buttonSave.setText("Save Profile");
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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

            if (password.equals("admin123")) {
                createAdminSession();
            } else {
                Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void createAdminSession() {
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("deviceId", deviceId);
        adminData.put("loginTimestamp", System.currentTimeMillis());

        db.collection("admins")
                .document(deviceId)
                .set(adminData)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Admin session created for " + deviceId);
                    Toast.makeText(this, "Admin access granted!", Toast.LENGTH_SHORT).show();
                    goToAdmin();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create admin session: " + e.getMessage());
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void goToAdmin() {
        Intent intent = new Intent(this, AdminActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
