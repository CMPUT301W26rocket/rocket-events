package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.EventRepository;

import java.text.SimpleDateFormat;
import java.util.Locale;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * Fragment that displays the details of an event from the organizer's perspective.
 * Shows event information such as poster, title, description, dates, capacity, etc.
 */
public class OrganizerEventDetailsFragment extends Fragment {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private String eventId;

    private ImageView posterImageView;
    private TextView titleView, organizerView, descriptionView, locationView;
    private TextView feeView, capacityView, eventDateView, regOpenView, regCloseView;
    private TextView geolocationView, waitlistView;
    private Button lotteryButton;
    private Event currentEvent;


    private EventRepository eventRepository;
    private EntrantRepository entrantRepository;


    /**
     * Required empty public constructor for fragment instantiation.
     */
    public OrganizerEventDetailsFragment() {}

    /** Injects a mock {@link EventRepository} for testing. */
    public void setEventRepository(EventRepository repo) { this.eventRepository = repo; }

    /** Injects a mock {@link EntrantRepository} for testing. */
    public void setEntrantRepository(EntrantRepository repo) { this.entrantRepository = repo; }
    /**
     * Inflates the organizer event details layout and initializes all UI components.
     * Retrieves the event ID from fragment arguments and begins loading the
     * event details from Firestore.
     *
     * @param inflater the LayoutInflater used to inflate the fragment layout
     * @param container the parent view that the fragment UI will attach to
     * @param savedInstanceState previously saved instance state, if any
     * @return the root view of the fragment layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_organizer_event_details, container, false);

        posterImageView = view.findViewById(R.id.image_detail_poster);
        titleView       = view.findViewById(R.id.text_detail_title);
        organizerView   = view.findViewById(R.id.text_detail_organizer);
        descriptionView = view.findViewById(R.id.text_detail_description);
        locationView    = view.findViewById(R.id.text_detail_location);
        feeView         = view.findViewById(R.id.text_detail_fee);
        capacityView    = view.findViewById(R.id.text_detail_capacity);
        eventDateView   = view.findViewById(R.id.text_detail_event_date);
        regOpenView     = view.findViewById(R.id.text_detail_reg_open);
        regCloseView    = view.findViewById(R.id.text_detail_reg_close);
        geolocationView = view.findViewById(R.id.text_detail_geolocation);
        waitlistView    = view.findViewById(R.id.text_detail_waitlist);
        lotteryButton    = view.findViewById(R.id.lottery_button);


        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }

        if (eventRepository == null) eventRepository = new EventRepository();
        if (entrantRepository == null) entrantRepository = new EntrantRepository();

        loadEventDetails();
        lotteryButton.setOnClickListener(v -> handleLotteryClick());

        return view;
    }
    /**
     * Retrieves the event information from Firestore using the
     * {@link EventRepository}. Once the event is successfully fetched,
     * the UI fields are populated with the event data.
     */
    private void loadEventDetails() {
        eventRepository.getEventById(eventId, new EventRepository.FirestoreCallback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null || !isAdded()) return;

                populateViews(event);
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load event details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    /**
     * Populates all UI components with data from the given event.
     * This includes the poster image, title, organizer name, description,
     * location, fees, registration dates, event date, and waitlist rules.
     *
     * @param event the event whose details should be displayed
     */
    private void populateViews(Event event) {
        currentEvent = event;

        titleView.setText(event.getTitle());

        organizerView.setText("By " +
                (event.getOrganizerName() != null ? event.getOrganizerName() : "Unknown"));

        descriptionView.setText(event.getDescription());

        locationView.setText("Location: " +
                (event.getLocation() != null ? event.getLocation() : "TBD"));

        feeView.setText(event.getRegistrationFee() == 0.0
                ? "Fee: Free"
                : "Fee: $" + String.format(Locale.getDefault(), "%.2f", event.getRegistrationFee()));

        capacityView.setText("Lottery Capacity: " + event.getLotteryCapacity());

        eventDateView.setText("Event Date: " +
                (event.getEventStartDate() != null
                        ? DATE_FORMAT.format(event.getEventStartDate())
                        : "TBD"));

        regOpenView.setText("Registration Opens: " +
                (event.getRegistrationOpenDate() != null
                        ? DATE_FORMAT.format(event.getRegistrationOpenDate())
                        : "TBD"));

        regCloseView.setText("Registration Closes: " +
                (event.getRegistrationCloseDate() != null
                        ? DATE_FORMAT.format(event.getRegistrationCloseDate())
                        : "TBD"));

        geolocationView.setText("Geolocation Required: "
                + (event.isGeolocationRequired() ? "Yes" : "No"));

        waitlistView.setText(event.isHasWaitlistLimit()
                ? "Waitlist Limit: " + event.getWaitlistLimit()
                : "Waitlist Limit: Unlimited");

        String posterUrl = event.getPosterUrl();

        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this)
                    .load(posterUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(posterImageView);
        }
    }
    /**
     * Handles clicks on the lottery button.
     *
     * <p>This method triggers the lottery selection process that randomly
     * chooses entrants from the waitlist based on the event's lottery
     * capacity.</p>
     */
    private void handleLotteryClick() {
        if (currentEvent == null) return; // event not loaded yet

        if (currentEvent.getRegistrationCloseDate() != null &&
                new Date().after(currentEvent.getRegistrationCloseDate())) {
            selectLottery();
        } else {
            Toast.makeText(getContext(),
                    "Registration period has not ended yet",
                    Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Performs the lottery selection for the event.
     *
     * <p>Retrieves all entrants on the waitlist, randomly shuffles them, and selects
     * up to {@code lotteryCapacity} winners. Winners are updated to
     * {@link Entrant#STATUS_INVITED}. All remaining entrants who were not selected
     * are updated to {@link Entrant#STATUS_NOT_SELECTED}.
     *
     * <p>If the waitlist is empty the button is re-enabled and the organizer is informed.
     */
    private void selectLottery() {
        lotteryButton.setEnabled(false);

        entrantRepository.getEntrantsByStatus(eventId, Entrant.STATUS_WAITLIST, new EntrantRepository.FirestoreCallback<List<Entrant>>() {
            @Override
            public void onSuccess(List<Entrant> waitlist) {
                if (waitlist == null || waitlist.isEmpty()) {
                    Toast.makeText(getContext(), "No entrants on the waitlist", Toast.LENGTH_SHORT).show();
                    lotteryButton.setEnabled(true); // nothing ran — let organizer try again later
                    return;
                }

                Collections.shuffle(waitlist);

                int capacity = Math.min(currentEvent.getLotteryCapacity(), waitlist.size());

                // Invite the selected entrants
                for (int i = 0; i < capacity; i++) {
                    Entrant entrant = waitlist.get(i);
                    entrantRepository.updateStatus(eventId, entrant.getDeviceId(),
                            Entrant.STATUS_INVITED, new EntrantRepository.FirestoreCallback<Void>() {
                                @Override public void onSuccess(Void unused) {}
                                @Override public void onFailure(Exception e) {
                                    if (isAdded()) Toast.makeText(getContext(),
                                            "Failed to invite: " + entrant.getDeviceId(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                }

                // Mark the remaining entrants as not selected
                for (int i = capacity; i < waitlist.size(); i++) {
                    Entrant entrant = waitlist.get(i);
                    entrantRepository.updateStatus(eventId, entrant.getDeviceId(),
                            Entrant.STATUS_NOT_SELECTED, new EntrantRepository.FirestoreCallback<Void>() {
                                @Override public void onSuccess(Void unused) {}
                                @Override public void onFailure(Exception e) {
                                    if (isAdded()) Toast.makeText(getContext(),
                                            "Failed to update: " + entrant.getDeviceId(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                }

                if (isAdded()) {
                    Toast.makeText(getContext(),
                            capacity + " entrant(s) invited. " + (waitlist.size() - capacity) + " not selected.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to fetch waitlist", Toast.LENGTH_SHORT).show();
                }
                lotteryButton.setEnabled(true);
            }
        });
    }
}