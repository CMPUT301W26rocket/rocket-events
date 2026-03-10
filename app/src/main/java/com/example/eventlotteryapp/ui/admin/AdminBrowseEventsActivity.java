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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminBrowseEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerEvents;
    private TextView textEmptyEvents;
    private Button btnBack;
    private FirebaseFirestore db;
    private List<Event> eventList;
    private EventAdapter eventAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        recyclerEvents = findViewById(R.id.recyclerEvents);
        textEmptyEvents = findViewById(R.id.textEmptyEvents);
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        eventList = new ArrayList<>();

        eventAdapter = new EventAdapter(eventList, event -> {
            Intent intent = new Intent(AdminBrowseEventsActivity.this, AdminEventDetailActivity.class);
            intent.putExtra("eventId", event.getEventId());
            intent.putExtra("title", event.getTitle());
            intent.putExtra("description", event.getDescription());
            intent.putExtra("organizerId", event.getOrganizerId());
            startActivity(intent);
        });

        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerEvents.setAdapter(eventAdapter);

        loadEvents();

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            if (event.getEventId() == null || event.getEventId().isEmpty()) {
                                event.setEventId(document.getId());
                            }
                            eventList.add(event);
                        }
                    }

                    eventAdapter.notifyDataSetChanged();

                    if (eventList.isEmpty()) {
                        textEmptyEvents.setVisibility(View.VISIBLE);
                        recyclerEvents.setVisibility(View.GONE);
                    } else {
                        textEmptyEvents.setVisibility(View.GONE);
                        recyclerEvents.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}