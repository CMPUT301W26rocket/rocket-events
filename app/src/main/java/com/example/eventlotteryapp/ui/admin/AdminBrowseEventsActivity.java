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
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.ui.adapters.EventAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for browsing all events in the system.
 * Allows an administrator to review events and open the event detail screen.
 * User Stories Implemented:
 * US 03.04.01 As an administrator, I want to be able to browse events.
 * @author Mazen
 */
public class AdminBrowseEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerEvents;
    private TextView textEmptyEvents;
    private Button btnBack;
    private EventRepository eventRepository;
    private List<Event> eventList;
    private EventAdapter eventAdapter;

    /**
     * Initializes the event list screen, connects the shared event adapter,
     * and loads all events for administrator review.
     *
     * @param savedInstanceState saved Android instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        recyclerEvents = findViewById(R.id.recyclerEvents);
        textEmptyEvents = findViewById(R.id.textEmptyEvents);
        btnBack = findViewById(R.id.btnBack);

        eventRepository = new EventRepository();
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

    /**
     * Reloads the event list whenever the screen returns to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    /**
     * Loads all events from the repository and updates the administrator list UI.
     * Displays an empty-state message when no events are available.
     */
    private void loadEvents() {
        eventRepository.getAllEvents(new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> result) {
                eventList.clear();
                eventList.addAll(result);
                eventAdapter.notifyDataSetChanged();

                if (eventList.isEmpty()) {
                    textEmptyEvents.setVisibility(View.VISIBLE);
                    recyclerEvents.setVisibility(View.GONE);
                } else {
                    textEmptyEvents.setVisibility(View.GONE);
                    recyclerEvents.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        AdminBrowseEventsActivity.this,
                        "Failed to load events: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}