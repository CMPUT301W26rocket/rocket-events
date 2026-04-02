package com.example.eventlotteryapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for browsing all user profiles in the system.
 * Allows an administrator to review basic profile information
 * and open the profile detail screen.
 * User Stories Implemented:
 * US 03.05.01 As an administrator, I want to be able to browse profiles.
 *
 * @author Mazen
 */
public class AdminBrowseProfilesActivity extends AppCompatActivity {

    private TextView textEmptyProfiles;
    private RecyclerView recyclerProfiles;
    private ImageButton buttonBack;
    private FirebaseFirestore db;
    private List<User> userList;
    private ProfileAdapter profileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_profiles);

        textEmptyProfiles = findViewById(R.id.textEmptyProfiles);
        recyclerProfiles = findViewById(R.id.recyclerProfiles);
        buttonBack = findViewById(R.id.button_back);

        db = FirebaseFirestore.getInstance();
        userList = new ArrayList<>();

        profileAdapter = new ProfileAdapter(userList, user -> {
            Intent intent = new Intent(AdminBrowseProfilesActivity.this, AdminProfileDetailActivity.class);
            intent.putExtra("deviceId", user.getDeviceId());
            intent.putExtra("name", user.getName());
            intent.putExtra("email", user.getEmail());
            intent.putExtra("phone", user.getPhone());
            startActivity(intent);
        });

        recyclerProfiles.setLayoutManager(new LinearLayoutManager(this));
        recyclerProfiles.setAdapter(profileAdapter);

        buttonBack.setOnClickListener(v -> finish());

        loadProfiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfiles();
    }

    private void loadProfiles() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                        User user = new User();
                        user.setDeviceId(document.getId());
                        user.setName(document.getString("name"));
                        user.setEmail(document.getString("email"));
                        user.setPhone(document.getString("phone"));
                        userList.add(user);
                    }

                    profileAdapter.notifyDataSetChanged();

                    if (userList.isEmpty()) {
                        textEmptyProfiles.setVisibility(View.VISIBLE);
                        recyclerProfiles.setVisibility(View.GONE);
                    } else {
                        textEmptyProfiles.setVisibility(View.GONE);
                        recyclerProfiles.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load profiles: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}