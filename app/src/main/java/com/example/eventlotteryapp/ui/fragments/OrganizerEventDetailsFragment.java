package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    //private Button lotteryButton;


    private EventRepository eventRepository;

    public OrganizerEventDetailsFragment() {}

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
        //lotteryButton    = view.findViewById(R.id.lottery_button);


        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }

        eventRepository = new EventRepository();

        loadEventDetails();
        //lotteryButton.setOnClickListener(v -> handleLotteryClick());

        return view;
    }

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

    private void populateViews(Event event) {

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
//    private void handleLotteryClick() {
//        // conditions (registration date, etc.)
//        selectLottery();
//    }
//
//    private void selectLottery() {
//        lotteryButton.setEnabled(false);
//
//        entrantRepository.joinWaitlist(eventId, deviceId, new EntrantRepository.FirestoreCallback<Void>() {
//            @Override
//            public void onSuccess(Void unused) {
//                if (!isAdded()) return;
//                Toast.makeText(getContext(), "Joined waitlist!", Toast.LENGTH_SHORT).show();
//                currentEntrant = new Entrant();
//                currentEntrant.setStatus(Entrant.STATUS_WAITLIST);
//                updateButton();
//            }
//
//            @Override
//            public void onFailure(Exception e) {
//                if (isAdded()) {
//                    Toast.makeText(getContext(), "Failed to join waitlist", Toast.LENGTH_SHORT).show();
//                    actionButton.setEnabled(true);
//                }
//            }
//        });
//    }
}