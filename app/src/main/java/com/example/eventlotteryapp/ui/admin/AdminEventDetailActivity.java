package com.example.eventlotteryapp.ui.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;

public class AdminEventDetailActivity extends AppCompatActivity {

    private TextView textEventId, textEventTitle, textEventDescription, textOrganizerId;
    private Button btnDeleteEvent, btnBack;

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

        String eventId = getIntent().getStringExtra("eventId");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String organizerId = getIntent().getStringExtra("organizerId");

        textEventId.setText(eventId == null || eventId.isEmpty() ? "No event ID" : eventId);
        textEventTitle.setText(title == null || title.isEmpty() ? "No title" : title);
        textEventDescription.setText(description == null || description.isEmpty() ? "No description" : description);
        textOrganizerId.setText(organizerId == null || organizerId.isEmpty() ? "No organizer ID" : organizerId);

        btnBack.setOnClickListener(v -> finish());
    }
}