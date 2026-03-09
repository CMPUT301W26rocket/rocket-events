package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;

import java.util.List;

public class MyEventsFragment extends Fragment {

    private TextView eventsTextView;
    private EventRepository eventRepository;

    public MyEventsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_events, container, false);

        eventsTextView = view.findViewById(R.id.text_events_list);
        eventRepository = new EventRepository();

        loadEvents();

        return view;
    }

    private void loadEvents() {
        eventRepository.getAllEvents(new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> result) {
                if (result == null || result.isEmpty()) {
                    eventsTextView.setText("No events yet.");
                    return;
                }

                StringBuilder builder = new StringBuilder();

                for (Event event : result) {
                    builder.append("Title: ")
                            .append(event.getTitle() != null ? event.getTitle() : "")
                            .append("\n");

                    builder.append("Description: ")
                            .append(event.getDescription() != null ? event.getDescription() : "")
                            .append("\n");

                    builder.append("Waitlist limited: ")
                            .append(event.isHasWaitlistLimit() ? "Yes" : "No")
                            .append("\n");

                    builder.append("Waitlist limit: ")
                            .append(event.getWaitlistLimit())
                            .append("\n");

                    builder.append("Geolocation required: ")
                            .append(event.isGeolocationRequired() ? "Yes" : "No")
                            .append("\n\n");
                }

                eventsTextView.setText(builder.toString());
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
                }
                eventsTextView.setText("Could not load events.");
            }
        });
    }
}