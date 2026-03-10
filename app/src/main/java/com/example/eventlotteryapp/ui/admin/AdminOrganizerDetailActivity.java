package com.example.eventlotteryapp.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;

import java.util.List;

public class AdminOrganizerDetailActivity extends AppCompatActivity {

    private TextView textDeviceId, textName, textEmail, textPhone;
    private Button btnRemoveOrganizer, btnBack;
    private String deviceId;
    private EventRepository eventRepository;

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

        eventRepository = new EventRepository();

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
                .setMessage("Are you sure you want to remove this organizer? This will delete all events created by this organizer.")
                .setPositiveButton("Remove", (dialog, which) -> removeOrganizer())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeOrganizer() {
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Missing organizer ID", Toast.LENGTH_SHORT).show();
            return;
        }

        eventRepository.getEventsByOrganizerId(deviceId, new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                if (events.isEmpty()) {
                    Toast.makeText(AdminOrganizerDetailActivity.this, "No events found for this organizer", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                deleteEventsSequentially(events, 0);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AdminOrganizerDetailActivity.this,
                        "Failed to load organizer events: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteEventsSequentially(List<Event> events, int index) {
        if (index >= events.size()) {
            Toast.makeText(this, "Organizer removed", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Event event = events.get(index);
        String eventId = event.getEventId();

        if (eventId == null || eventId.isEmpty()) {
            deleteEventsSequentially(events, index + 1);
            return;
        }

        eventRepository.deleteEvent(eventId, new EventRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                deleteEventsSequentially(events, index + 1);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AdminOrganizerDetailActivity.this,
                        "Failed to delete event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}