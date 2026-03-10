package com.example.eventlotteryapp.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class AdminOrganizerDetailActivity extends AppCompatActivity {

    private TextView textDeviceId, textName, textEmail, textPhone;
    private Button btnRemoveOrganizer, btnBack;
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_organizer_detail);

        textDeviceId = findViewById(R.id.textDeviceId);
        textName = findViewById(R.id.textName);
        textEmail = findViewById(R.id.textEmail);
        textPhone = findViewById(R.id.textPhone);
        btnRemoveOrganizer = findViewById(R.id.btnRemoveOrganizer);
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

        btnRemoveOrganizer.setOnClickListener(v -> showRemoveConfirmation());
        btnBack.setOnClickListener(v -> finish());
    }

    private void showRemoveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Organizer")
                .setMessage("Are you sure you want to remove organizer status from this user?")
                .setPositiveButton("Remove", (dialog, which) -> removeOrganizerStatus())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeOrganizerStatus() {
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Missing device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    user.setEventsHosting(new ArrayList<>());

                    db.collection("users").document(deviceId)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Organizer status removed", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}