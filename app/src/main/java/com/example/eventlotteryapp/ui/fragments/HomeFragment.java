package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

public class HomeFragment extends Fragment {

    private RecyclerView eventsRecyclerView;
    private EventRepository eventRepository;
    private String deviceId;

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        eventsRecyclerView = view.findViewById(R.id.recycler_events);
        ImageButton createButton = view.findViewById(R.id.button_open_create_event);
        ImageButton scanQrButton = view.findViewById(R.id.button_scan_qr);

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRepository = new EventRepository();

        createButton.setOnClickListener(v -> {
            CreateEventFragment fragment = new CreateEventFragment();
            Bundle bundle = new Bundle();
            bundle.putString("deviceId", deviceId);
            fragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        scanQrButton.setOnClickListener(v ->
                Toast.makeText(getContext(), "QR scanner coming next", Toast.LENGTH_SHORT).show()
        );

        loadAllEvents();

        return view;
    }

    private void loadAllEvents() {
        eventRepository.getAllEvents(new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> result) {
                if (getContext() == null) {
                    return;
                }

                EventAdapter adapter = new EventAdapter(result, event -> {
                    Toast.makeText(getContext(),
                            "Open details for: " + event.getTitle(),
                            Toast.LENGTH_SHORT).show();
                });

                eventsRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}