package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.ui.adapters.EventHistoryAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EventHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private EntrantRepository entrantRepository;
    private EventRepository eventRepository;
    private String deviceId;

    public EventHistoryFragment() {}

    /**
     * Injects a mock {@link EntrantRepository} for testing. Must be called inside a
     * {@link androidx.fragment.app.FragmentFactory} before the fragment attaches.
     */
    public void setEntrantRepository(EntrantRepository repo) {
        this.entrantRepository = repo;
    }

    /**
     * Injects a mock {@link EventRepository} for testing. Must be called inside a
     * {@link androidx.fragment.app.FragmentFactory} before the fragment attaches.
     */
    public void setEventRepository(EventRepository repo) {
        this.eventRepository = repo;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_history, container, false);

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        recyclerView = view.findViewById(R.id.recycler_event_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (entrantRepository == null) entrantRepository = new EntrantRepository();
        if (eventRepository == null) eventRepository = new EventRepository();

        loadHistory();

        return view;
    }

    private void loadHistory() {
        entrantRepository.getUserEventHistory(deviceId, new EntrantRepository.FirestoreCallback<List<Entrant>>() {
            @Override
            public void onSuccess(List<Entrant> entrants) {
                if (!isAdded() || entrants.isEmpty()) return;

                List<Event> events = new ArrayList<>();
                List<String> statuses = new ArrayList<>();
                AtomicInteger remaining = new AtomicInteger(entrants.size());

                for (Entrant entrant : entrants) {
                    eventRepository.getEventById(entrant.getEventId(), new EventRepository.FirestoreCallback<Event>() {
                        @Override
                        public void onSuccess(Event event) {
                            if (event != null) {
                                events.add(event);
                                statuses.add(entrant.getStatus());
                            }
                            if (remaining.decrementAndGet() == 0 && isAdded()) {
                                bindEvents(events, statuses);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (remaining.decrementAndGet() == 0 && isAdded()) {
                                bindEvents(events, statuses);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("EventHistory", "getUserEventHistory failed", e);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load event history.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindEvents(List<Event> events, List<String> statuses) {
        EventHistoryAdapter adapter = new EventHistoryAdapter(events, statuses, event -> {
            EntrantEventDetailsFragment detailFragment = new EntrantEventDetailsFragment();
            Bundle args = new Bundle();
            args.putString("eventId", event.getEventId());
            args.putString("deviceId", deviceId);
            detailFragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerView.setAdapter(adapter);
    }
}
