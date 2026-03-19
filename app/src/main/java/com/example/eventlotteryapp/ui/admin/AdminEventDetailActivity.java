package com.example.eventlotteryapp.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.repository.EventRepository;

/**
 * Admin screen for viewing event details and deleting an event.
 * Displays the selected event information and provides a confirmation
 * step before permanent removal.
 * User Stories Implemented:
 * US 03.01.01 As an administrator, I want to be able to remove events.
 * @author Mazen
 */
public class AdminEventDetailActivity extends AppCompatActivity {

    private TextView textEventId;
    private TextView textEventTitle;
    private TextView textEventDescription;
    private TextView textOrganizerId;
    private Button btnDeleteEvent;
    private Button btnBack;
    private EventRepository eventRepository;
    private String eventId;

    /**
     * Initializes the event detail screen and loads event data passed
     * from the admin event list.
     *
     * @param savedInstanceState saved Android instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_detail);

        textEventId = findViewById(R.id.textEventId);
        textEventTitle = findViewById(R.id.textEventTitle);
        textEventDescription = findViewById(R.id.textEventDescription);
        textOrganizerId = findViewById(R.id.textOrganizerId);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        btnBack = findViewById(R.id.btnBack);

        eventRepository = new EventRepository();

        eventId = getIntent().getStringExtra("eventId");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String organizerId = getIntent().getStringExtra("organizerId");

        textEventId.setText(eventId == null || eventId.isEmpty() ? "No event ID" : eventId);
        textEventTitle.setText(title == null || title.isEmpty() ? "No title" : title);
        textEventDescription.setText(description == null || description.isEmpty() ? "No description" : description);
        textOrganizerId.setText(organizerId == null || organizerId.isEmpty() ? "No organizer ID" : organizerId);

        btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmation());
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Shows a confirmation dialog before deleting the selected event.
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the selected event through the shared event repository.
     * Shows a success or failure toast depending on the result.
     */
    private void deleteEvent() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        eventRepository.deleteEvent(eventId, new EventRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(AdminEventDetailActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        AdminEventDetailActivity.this,
                        "Delete failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}