package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays all available events in a scrollable list.
 * Shown on the Home tab of {@link com.example.eventlotteryapp.ui.main.MainActivity}.
 * <p>Provides buttons to navigate to {@link CreateEventFragment} and a placeholder for QR scanning.
 * User Stories Implemented:
 * US 01.01.03 As an entrant, I want to be able to see a list of events that I can join the waiting list for.
 * US 01.01.04 As an entrant, I want to filter events based on my availability and event capacity.
 * US 01.01.05 As an entrant, I want to search for events by keyword to find events based on my interests.
 * US 01.01.06 As an entrant, I want to use keyword search with filtering to narrow my event search.
 * @author Daniel
 * @author Leyla
 * @author William
 */
public class HomeFragment extends Fragment {

    private static final String QR_SCHEME = "eventlotteryapp://event/";

    private RecyclerView eventsRecyclerView;
    private EventRepository eventRepository;
    private String deviceId;

    private EditText searchEditText;
    private TextView noResultsText;
    private TextView chipAvailable;
    private TextView chipUpcoming;
    private TextView chipSmall;
    private TextView chipMedium;
    private TextView chipLarge;

    private List<Event> allEvents = new ArrayList<>();
    private String currentSearchQuery = "";

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
     * Also reads the device ID from fragment arguments, initialises search and filter chips,
     * and triggers the initial event load.
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
        searchEditText = view.findViewById(R.id.edit_search);
        noResultsText = view.findViewById(R.id.text_no_results);
        chipAvailable = view.findViewById(R.id.chip_available);
        chipUpcoming = view.findViewById(R.id.chip_upcoming);
        chipSmall = view.findViewById(R.id.chip_small);
        chipMedium = view.findViewById(R.id.chip_medium);
        chipLarge = view.findViewById(R.id.chip_large);

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
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
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

        setupSearch();
        setupFilterChips();
        chipAvailable.setSelected(true);
        loadAllEvents();

        return view;
    }

    /**
     * Sets up the search EditText with a TextWatcher that filters events as the user types.
     */
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Sets up filter chip click listeners. Each chip toggles its selected state and re-filters.
     * Availability chips (Open Now, Upcoming) are mutually exclusive.
     * Capacity chips (Small, Medium, Large) are mutually exclusive.
     */
    private void setupFilterChips() {
        View.OnClickListener availabilityChipListener = v -> {
            TextView chip = (TextView) v;
            boolean wasSelected = chip.isSelected();
            chipAvailable.setSelected(false);
            chipUpcoming.setSelected(false);
            chip.setSelected(!wasSelected);
            applyFilters();
        };

        View.OnClickListener capacityChipListener = v -> {
            TextView chip = (TextView) v;
            boolean wasSelected = chip.isSelected();
            chipSmall.setSelected(false);
            chipMedium.setSelected(false);
            chipLarge.setSelected(false);
            chip.setSelected(!wasSelected);
            applyFilters();
        };

        chipAvailable.setOnClickListener(availabilityChipListener);
        chipUpcoming.setOnClickListener(availabilityChipListener);
        chipSmall.setOnClickListener(capacityChipListener);
        chipMedium.setOnClickListener(capacityChipListener);
        chipLarge.setOnClickListener(capacityChipListener);
    }

    /**
     * Applies the current search query and active filter chips to produce a filtered event list.
     * Keyword search matches against title, description, location, and organizer name.
     */
    private void applyFilters() {
        List<Event> filtered = new ArrayList<>();

        for (Event event : allEvents) {
            if (!currentSearchQuery.isEmpty()) {
                boolean matches = false;
                if (event.getTitle() != null && event.getTitle().toLowerCase().contains(currentSearchQuery)) {
                    matches = true;
                } else if (event.getDescription() != null && event.getDescription().toLowerCase().contains(currentSearchQuery)) {
                    matches = true;
                } else if (event.getLocation() != null && event.getLocation().toLowerCase().contains(currentSearchQuery)) {
                    matches = true;
                } else if (event.getOrganizerName() != null && event.getOrganizerName().toLowerCase().contains(currentSearchQuery)) {
                    matches = true;
                }
                if (!matches) continue;
            }

            if (chipAvailable.isSelected() && !event.isRegistrationOpen()) continue;
            if (chipUpcoming.isSelected() && !event.isRegistrationNotYetOpen()) continue;

            int capacity = event.getLotteryCapacity();
            if (chipSmall.isSelected() && (capacity < 1 || capacity > 20)) continue;
            if (chipMedium.isSelected() && (capacity < 21 || capacity > 50)) continue;
            if (chipLarge.isSelected() && capacity <= 50) continue;

            filtered.add(event);
        }

        updateEventList(filtered);
    }

    /**
     * Updates the RecyclerView adapter with the given event list and toggles the empty state text.
     *
     * @param events the filtered list of {@link Event} objects to display
     */
    private void updateEventList(List<Event> events) {
        if (getContext() == null) return;

        boolean hasActiveFilter = !currentSearchQuery.isEmpty()
                || chipAvailable.isSelected() || chipUpcoming.isSelected()
                || chipSmall.isSelected() || chipMedium.isSelected() || chipLarge.isSelected();

        noResultsText.setVisibility(events.isEmpty() && hasActiveFilter ? View.VISIBLE : View.GONE);

        EventAdapter adapter = new EventAdapter(events, event -> {
            EntrantEventDetailsFragment detailsFragment = new EntrantEventDetailsFragment();
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId());
            bundle.putString("deviceId", deviceId);
            detailsFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragment_container, detailsFragment)
                    .addToBackStack(null)
                    .commit();
        });

        eventsRecyclerView.setAdapter(adapter);
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
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
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
     * stores them for client-side filtering. Shows a toast if the load fails.
     */
    private void loadAllEvents() {
        eventRepository.getAllEvents(new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> result) {
                if (getContext() == null) {
                    return;
                }

                allEvents = new ArrayList<>();
                for (Event event : result) {
                    if (!event.isPrivateEvent()) allEvents.add(event);
                }
                applyFilters();
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
