package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.ui.adapters.EventAdapter;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;

/**
 * Fragment that displays all available events in a scrollable list.
 * Shown on the Home tab of {@link com.example.eventlotteryapp.ui.main.MainActivity}.
 *
 * <p>Provides buttons to navigate to {@link CreateEventFragment} and a placeholder for QR scanning.
 */
public class HomeFragment extends Fragment {

    private static final String QR_SCHEME = "eventlotteryapp://event/";

    private RecyclerView eventsRecyclerView;
    private EventRepository eventRepository;
    private String deviceId;

    private final ActivityResultLauncher<ScanOptions> qrScanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) return;
                handleQrScanResult(result.getContents());
            });

    /**
     * Required empty public constructor.
     */
    public HomeFragment() {
    }

    /** Allows tests to inject a mock {@link EventRepository} before the fragment loads. */
    public void setEventRepository(EventRepository repo) { this.eventRepository = repo; }

    /**
     * Inflates the fragment layout, sets up the RecyclerView with a LinearLayoutManager,
     * and attaches click listeners for the create event button and the QR scan placeholder.
     * Also reads the device ID from fragment arguments and triggers the initial event load.
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

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        eventsRecyclerView = view.findViewById(R.id.recycler_events);
        ImageButton createButton = view.findViewById(R.id.button_open_create_event);
        ImageButton scanQrButton = view.findViewById(R.id.button_scan_qr);

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (eventRepository == null) eventRepository = new EventRepository();

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

        scanQrButton.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan an event QR code");
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);
            qrScanLauncher.launch(options);
        });

        loadAllEvents();

        return view;
    }

    /**
     * Parses a scanned QR code value and navigates to the matching event's details screen.
     * Expects the format {@code eventlotteryapp://event/{eventId}}.
     * Shows a toast if the code is not a recognised event QR.
     *
     * @param content the raw string decoded from the QR code
     */
    private void handleQrScanResult(String content) {
        if (content != null && content.startsWith(QR_SCHEME)) {
            String eventId = content.substring(QR_SCHEME.length());
            if (!eventId.isEmpty()) {
                EntrantEventDetailsFragment detailsFragment = new EntrantEventDetailsFragment();
                Bundle bundle = new Bundle();
                bundle.putString("eventId", eventId);
                bundle.putString("deviceId", deviceId);
                detailsFragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailsFragment)
                        .addToBackStack(null)
                        .commit();
                return;
            }
        }
        Toast.makeText(getContext(), "Unrecognised QR code", Toast.LENGTH_SHORT).show();
    }

    /**
     * Fetches all events from Firestore via {@link EventRepository#getAllEvents} and
     * binds them to the RecyclerView using an {@link EventAdapter}.
     * Shows a toast if the load fails. Does nothing if the fragment is no longer attached.
     */
    private void loadAllEvents() {
        eventRepository.getAllEvents(new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> result) {
                if (getContext() == null) {
                    return;
                }

                EventAdapter adapter = new EventAdapter(result, event -> {
                    EntrantEventDetailsFragment detailsFragment = new EntrantEventDetailsFragment();
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
