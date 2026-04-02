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
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for browsing all events in the system.
 * Allows an administrator to review events and open the event detail screen.
 *
 * User Stories Implemented:
 * US 03.04.01 As an administrator, I want to be able to browse events.
 *
 * @author Mazen
 */
public class AdminBrowseEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerEvents;
    private TextView textEmptyEvents;
    private ImageButton buttonBack;

    private EventRepository eventRepository;
    private List<Event> eventList;
    private AdminEventAdapter eventAdapter;

    /**
     * Initializes the event browsing screen, sets up the RecyclerView,
     * and loads all events from the repository.
     *
     * @param savedInstanceState saved Android instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        recyclerEvents = findViewById(R.id.recyclerEvents);
        textEmptyEvents = findViewById(R.id.textEmptyEvents);
        buttonBack = findViewById(R.id.button_back);

        eventRepository = new EventRepository();
        eventList = new ArrayList<>();

        eventAdapter = new AdminEventAdapter(eventList, event -> {
            Intent intent = new Intent(AdminBrowseEventsActivity.this, AdminEventDetailActivity.class);
            intent.putExtra("eventId", event.getEventId());
            intent.putExtra("title", event.getTitle());
            intent.putExtra("description", event.getDescription());
            intent.putExtra("organizerId", event.getOrganizerId());
            intent.putExtra("organizerName", event.getOrganizerName());
            startActivity(intent);
        });

        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerEvents.setAdapter(eventAdapter);

        buttonBack.setOnClickListener(v -> finish());

        loadEvents();
    }

    /**
     * Reloads the event list when the activity returns to the foreground.
     * This keeps the displayed data up to date after returning from the
     * event detail screen.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    /**
     * Loads all events from Firestore through the repository and updates
     * the RecyclerView and empty-state visibility based on the result.
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