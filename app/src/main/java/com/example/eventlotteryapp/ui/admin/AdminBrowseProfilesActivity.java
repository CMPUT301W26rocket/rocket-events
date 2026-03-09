package com.example.eventlotteryapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminBrowseProfilesActivity extends AppCompatActivity {
    private android.widget.TextView textEmptyProfiles;
    private RecyclerView recyclerProfiles;
    private Button btnBack;
    private FirebaseFirestore db;
    private List<User> userList;
    private ProfileAdapter profileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_profiles);
        textEmptyProfiles = findViewById(R.id.textEmptyProfiles);
        recyclerProfiles = findViewById(R.id.recyclerProfiles);
        btnBack = findViewById(R.id.btnBack);

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

        loadProfiles();

        btnBack.setOnClickListener(v -> finish());
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
                        User user = document.toObject(User.class);
                        if (user != null) {
                            userList.add(user);
                        }
                    }

                    profileAdapter.notifyDataSetChanged();

                    if (userList.isEmpty()) {
                        textEmptyProfiles.setVisibility(android.view.View.VISIBLE);
                        recyclerProfiles.setVisibility(android.view.View.GONE);
                    } else {
                        textEmptyProfiles.setVisibility(android.view.View.GONE);
                        recyclerProfiles.setVisibility(android.view.View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profiles: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}