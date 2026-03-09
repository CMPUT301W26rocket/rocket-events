package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.ui.adapters.EventAdapter;

import java.util.List;

public class MyEventsFragment extends Fragment {

    private RecyclerView myEventsRecyclerView;
    private EventRepository eventRepository;
    private String deviceId;

    public MyEventsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_events, container, false);

        myEventsRecyclerView = view.findViewById(R.id.recycler_my_events);
        myEventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        eventRepository = new EventRepository();

        loadMyEvents();

        return view;
    }

    private void loadMyEvents() {
        eventRepository.getEventsByOrganizerId(deviceId, new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> result) {
                if (getContext() == null) {
                    return;
                }

                EventAdapter adapter = new EventAdapter(result, event -> {
                    Toast.makeText(getContext(),
                            "Open details for: " + event.getTitle(),
                            Toast.LENGTH_SHORT).show();

                    // Later replace with EventDetailsFragment or EventDetailsActivity
                });

                myEventsRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load my events", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}