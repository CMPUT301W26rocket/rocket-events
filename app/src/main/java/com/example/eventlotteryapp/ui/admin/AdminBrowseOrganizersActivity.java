package com.example.eventlotteryapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class AdminBrowseOrganizersActivity extends AppCompatActivity {

    private RecyclerView recyclerOrganizers;
    private TextView textEmptyOrganizers;
    private Button btnBack;
    private FirebaseFirestore db;
    private List<User> organizerList;
    private ProfileAdapter profileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_organizers);

        recyclerOrganizers = findViewById(R.id.recyclerOrganizers);
        textEmptyOrganizers = findViewById(R.id.textEmptyOrganizers);
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        organizerList = new ArrayList<>();

        profileAdapter = new ProfileAdapter(organizerList, user -> {
            Intent intent = new Intent(AdminBrowseOrganizersActivity.this, AdminOrganizerDetailActivity.class);
            intent.putExtra("deviceId", user.getDeviceId());
            intent.putExtra("name", user.getName());
            intent.putExtra("email", user.getEmail());
            intent.putExtra("phone", user.getPhone());
            startActivity(intent);
        });

        recyclerOrganizers.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrganizers.setAdapter(profileAdapter);

        loadOrganizers();

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizers();
    }

    private void loadOrganizers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    organizerList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if (user != null && user.getEventsHosting() != null && !user.getEventsHosting().isEmpty()) {
                            organizerList.add(user);
                        }
                    }

                    profileAdapter.notifyDataSetChanged();

                    if (organizerList.isEmpty()) {
                        textEmptyOrganizers.setVisibility(View.VISIBLE);
                        recyclerOrganizers.setVisibility(View.GONE);
                    } else {
                        textEmptyOrganizers.setVisibility(View.GONE);
                        recyclerOrganizers.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load organizers: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}