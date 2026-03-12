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
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EventRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Admin screen for browsing all organizers currently represented by event ownership.
 * Organizers are derived from event organizer IDs rather than a separate organizer table.
 */
public class AdminBrowseOrganizersActivity extends AppCompatActivity {

    private EventRepository eventRepository;
    private RecyclerView recyclerOrganizers;
    private TextView textEmptyOrganizers;
    private Button btnBack;
    private FirebaseFirestore db;
    private List<User> organizerList;
    private ProfileAdapter profileAdapter;

    /**
     * Initializes the organizer list screen, connects the adapter,
     * and loads organizers based on current event ownership.
     *
     * @param savedInstanceState saved Android instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_organizers);

        recyclerOrganizers = findViewById(R.id.recyclerOrganizers);
        textEmptyOrganizers = findViewById(R.id.textEmptyOrganizers);
        btnBack = findViewById(R.id.btnBack);

        eventRepository = new EventRepository();
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

    /**
     * Reloads the organizer list whenever the screen returns to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizers();
    }

    /**
     * Loads organizers by first gathering unique organizer IDs from events,
     * then resolving those IDs to user documents for display in the admin UI.
     */
    private void loadOrganizers() {
        eventRepository.getAllEvents(new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                Set<String> organizerIds = new HashSet<>();

                for (Event event : events) {
                    if (event.getOrganizerId() != null && !event.getOrganizerId().isEmpty()) {
                        organizerIds.add(event.getOrganizerId());
                    }
                }

                if (organizerIds.isEmpty()) {
                    organizerList.clear();
                    profileAdapter.notifyDataSetChanged();
                    textEmptyOrganizers.setVisibility(View.VISIBLE);
                    recyclerOrganizers.setVisibility(View.GONE);
                    return;
                }

                db.collection("users")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            organizerList.clear();

                            for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                                String userId = document.getId();

                                if (organizerIds.contains(userId)) {
                                    User user = new User();
                                    user.setDeviceId(userId);
                                    user.setName(document.getString("name"));
                                    user.setEmail(document.getString("email"));
                                    user.setPhone(document.getString("phone"));
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
                                Toast.makeText(
                                        AdminBrowseOrganizersActivity.this,
                                        "Failed to load organizers: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        AdminBrowseOrganizersActivity.this,
                        "Failed to load events: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}