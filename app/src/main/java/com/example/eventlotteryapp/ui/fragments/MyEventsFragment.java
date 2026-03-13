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

/**
 * Fragment that displays the events created by the currently logged-in user.
 * Shown on the My Events tab of {@link com.example.eventlotteryapp.ui.main.MainActivity}.
 *
 * <p>Queries Firestore for events where {@code organizerId} matches the device's ID.
 */
public class MyEventsFragment extends Fragment {

    private RecyclerView myEventsRecyclerView;
    private EventRepository eventRepository;
    private String deviceId;

    /**
     * Required empty public constructor.
     */
    public MyEventsFragment() {}

    /**
     * Injects a mock {@link EventRepository} for testing. Must be called before
     * the fragment is attached to a host (i.e. inside a {@link androidx.fragment.app.FragmentFactory}).
     *
     * @param repo the repository to use instead of creating a new one
     */
    public void setEventRepository(EventRepository repo) {
        this.eventRepository = repo;
    }

    /**
     * Inflates the fragment layout, sets up the RecyclerView with a LinearLayoutManager,
     * and reads the device ID from fragment arguments. Triggers the initial load of the
     * user's hosted events.
     *
     * @param inflater  the LayoutInflater used to inflate the fragment's view
     * @param container the parent view that the fragment's UI will be attached to, or null
     * @param savedInstanceState previously saved state, or null if none
     * @return the root {@link View} of the inflated fragment layout
     */
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

        if (eventRepository == null) eventRepository = new EventRepository();

        loadMyEvents();

        return view;
    }

    /**
     * Fetches all events where {@code organizerId} matches the current device ID via
     * {@link EventRepository#getEventsByOrganizerId} and binds them to the RecyclerView
     * using an {@link EventAdapter}. Shows a toast if the load fails or the fragment is detached.
     */
    private void loadMyEvents() {
        eventRepository.getEventsByOrganizerId(deviceId, new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> result) {
                if (getContext() == null) {
                    return;
                }

                EventAdapter adapter = new EventAdapter(result, event -> {
                    OrganizerEventDetailsFragment detailsFragment = new OrganizerEventDetailsFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("eventId", event.getEventId());
                    bundle.putString("deviceId", deviceId);
                    detailsFragment.setArguments(bundle);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, detailsFragment)
                            .addToBackStack(null)
                            .commit();
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
