package com.example.eventlotteryapp.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Fragment that displays the details of an event from an entrant's perspective.
 * Shows event information (poster, title, organizer, description, dates, etc.)
 * and a state-based action button that reflects the user's current relationship
 * to the event (e.g. join waitlist, leave waitlist, invited, enrolled).
 *
 * <p>The join button is greyed out if the current date is outside the registration window,
 * or if the event's waitlist is full.
 *
 * <p>Navigate to this fragment from {@link HomeFragment} when a user taps an event.
 * Pass {@code eventId} and {@code deviceId} as fragment arguments.
 */
public class EntrantEventDetailsFragment extends Fragment {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private String eventId;
    private String deviceId;
    private Entrant currentEntrant;
    private Event currentEvent;

    private ImageView posterImageView;
    private TextView titleView, organizerView, descriptionView, locationView;
    private TextView feeView, capacityView, eventDateView, regOpenView, regCloseView;
    private TextView geolocationView, waitlistView, waitlistCountView;
    private Button actionButton;

    private EventRepository eventRepository;
    private EntrantRepository entrantRepository;
    private ListenerRegistration waitlistCountListener;

    /** Required empty public constructor. */
    public EntrantEventDetailsFragment() {}

    /** Allows tests to inject a mock {@link EventRepository} before the fragment loads. */
    public void setEventRepository(EventRepository repo) { this.eventRepository = repo; }

    /** Allows tests to inject a mock {@link EntrantRepository} before the fragment loads. */
    public void setEntrantRepository(EntrantRepository repo) { this.entrantRepository = repo; }

    /**
     * Inflates the fragment layout, binds all views, reads {@code eventId} and
     * {@code deviceId} from arguments, and triggers loading of event details and
     * the user's entrant status.
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

        View view = inflater.inflate(R.layout.fragment_entrant_event_details, container, false);

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
        actionButton    = view.findViewById(R.id.action_button);
        waitlistCountView = view.findViewById(R.id.text_detail_waitlist_count);

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (getArguments() != null) {
            eventId  = getArguments().getString("eventId");
            deviceId = getArguments().getString("deviceId");
        }

        if (eventRepository == null)   eventRepository   = new EventRepository();
        if (entrantRepository == null) entrantRepository = new EntrantRepository();

        loadEventDetails();
        actionButton.setOnClickListener(v -> handleButtonClick());

        return view;
    }

    /**
     * Fetches the event document from Firestore and populates all detail views.
     * Once the event is loaded, triggers {@link #loadEntrantStatus()} to determine
     * the correct button state.
     */
    private void loadEventDetails() {
        eventRepository.getEventById(eventId, new EventRepository.FirestoreCallback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null || !isAdded()) return;
                currentEvent = event;
                populateViews(event);
                loadEntrantStatus();
                attachWaitlistCountListener();
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
     * Populates all detail TextViews and the poster ImageView with data from the given event.
     *
     * @param event the event whose details should be displayed
     */
    private void populateViews(Event event) {
        titleView.setText(event.getTitle());
        organizerView.setText("By " + (event.getOrganizerName() != null ? event.getOrganizerName() : "Unknown"));
        descriptionView.setText(event.getDescription());
        locationView.setText("Location: " + (event.getLocation() != null ? event.getLocation() : "TBD"));

        feeView.setText(event.getRegistrationFee() == 0.0
                ? "Fee: Free"
                : "Fee: $" + String.format(Locale.getDefault(), "%.2f", event.getRegistrationFee()));

        capacityView.setText("Lottery Capacity: " + event.getLotteryCapacity());

        eventDateView.setText("Event Date: " +
                (event.getEventStartDate() != null ? DATE_FORMAT.format(event.getEventStartDate()) : "TBD"));
        regOpenView.setText("Registration Opens: " +
                (event.getRegistrationOpenDate() != null ? DATE_FORMAT.format(event.getRegistrationOpenDate()) : "TBD"));
        regCloseView.setText("Registration Closes: " +
                (event.getRegistrationCloseDate() != null ? DATE_FORMAT.format(event.getRegistrationCloseDate()) : "TBD"));

        geolocationView.setText("Geolocation Required: " + (event.isGeolocationRequired() ? "Yes" : "No"));

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
     * Fetches the user's entrant record for this event via {@link EntrantRepository#getEntrant}
     * and updates the action button to reflect their current status.
     */
    private void loadEntrantStatus() {
        entrantRepository.getEntrant(eventId, deviceId, new EntrantRepository.FirestoreCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant entrant) {
                if (!isAdded()) return;
                currentEntrant = entrant;
                updateButton();
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load your status", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Attaches a real-time Firestore listener that updates the waitlist count
     * automatically whenever someone joins or leaves. Stored in
     * {@code waitlistCountListener} so it can be removed in {@link #onDestroyView()}.
     */
    private void attachWaitlistCountListener() {
        waitlistCountListener = entrantRepository.listenToWaitlistCount(eventId,
                new EntrantRepository.FirestoreCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        if (!isAdded()) return;
                        waitlistCountView.setText("Current Waitlist: " + count);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            waitlistCountView.setText("Current Waitlist: —");
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (waitlistCountListener != null) {
            waitlistCountListener.remove();
        }
    }

    /**
     * Updates the action button's text and enabled state based on {@code currentEntrant}.
     * If the user has no entrant record, checks whether registration is currently open
     * via {@link Event#isRegistrationOpen()} before showing "Join Waitlist".
     * Greyed-out states (enrolled, declined, cancelled) disable the button.
     */
    private void updateButton() {
        if (currentEntrant == null) {
            if (currentEvent != null && currentEvent.isRegistrationNotYetOpen()) {
                actionButton.setText("Registration Not Open Yet");
                actionButton.setEnabled(false);
                actionButton.setAlpha(0.5f);
                return;
            }
            if (currentEvent != null && !currentEvent.isRegistrationOpen()) {
                actionButton.setText("Registration Closed");
                actionButton.setEnabled(false);
                actionButton.setAlpha(0.5f);
                return;
            }
            actionButton.setText("Join Waitlist");
            actionButton.setEnabled(true);
            actionButton.setAlpha(1f);
            return;
        }

        switch (currentEntrant.getStatus()) {
            case Entrant.STATUS_WAITLIST:
                actionButton.setText("Leave Waitlist");
                actionButton.setEnabled(true);
                actionButton.setAlpha(1f);
                break;
            case Entrant.STATUS_INVITED:
                actionButton.setText("Invited");
                actionButton.setEnabled(true);
                actionButton.setAlpha(1f);
                break;
            case Entrant.STATUS_NOT_SELECTED:
                actionButton.setText("Leave Waitlist");
                actionButton.setEnabled(true);
                actionButton.setAlpha(1f);
                break;
            case Entrant.STATUS_ENROLLED:
                actionButton.setText("Enrolled");
                actionButton.setEnabled(false);
                actionButton.setAlpha(0.5f);
                break;
            case Entrant.STATUS_DECLINED:
                actionButton.setText("Declined");
                actionButton.setEnabled(false);
                actionButton.setAlpha(0.5f);
                break;
            case Entrant.STATUS_CANCELLED:
                actionButton.setText("Cancelled");
                actionButton.setEnabled(false);
                actionButton.setAlpha(0.5f);
                break;
        }
    }

    /**
     * Handles the action button tap based on the user's current entrant status.
     * Routes to join, leave, or the invitation dialog as appropriate.
     */
    private void handleButtonClick() {
        if (currentEntrant == null) {
            showTermsDialog();
        } else {
            switch (currentEntrant.getStatus()) {
                case Entrant.STATUS_WAITLIST:
                case Entrant.STATUS_NOT_SELECTED:
                    leaveWaitlist();
                    break;
                case Entrant.STATUS_INVITED:
                    showInvitationDialog();
                    break;
            }
        }
    }

    /**
     * Attempts to add the user to the event's waitlist.
     * If the event has a waitlist limit, first checks via {@link EntrantRepository#isWaitlistFull}
     * and shows a "Waitlist is full" message if the limit has been reached.
     * Otherwise proceeds with {@link #proceedWithJoin()}.
     */
    private void joinWaitlist() {
        actionButton.setEnabled(false);

        if (currentEvent != null && currentEvent.isHasWaitlistLimit()) {
            entrantRepository.isWaitlistFull(eventId, currentEvent.getWaitlistLimit(),
                    new EntrantRepository.FirestoreCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean isFull) {
                            if (!isAdded()) return;
                            if (isFull) {
                                Toast.makeText(getContext(),
                                        "Waitlist is full for this event", Toast.LENGTH_SHORT).show();
                                actionButton.setEnabled(true);
                            } else {
                                proceedWithJoin();
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (isAdded()) {
                                Toast.makeText(getContext(),
                                        "Failed to check waitlist", Toast.LENGTH_SHORT).show();
                                actionButton.setEnabled(true);
                            }
                        }
                    });
        } else {
            proceedWithJoin();
        }
    }

    /**
     * Performs the actual Firestore write to add the user to the waitlist via
     * {@link EntrantRepository#joinWaitlist}. Updates the button to "Leave Waitlist" on success.
     */
    private void proceedWithJoin() {
        entrantRepository.joinWaitlist(eventId, deviceId, new EntrantRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Joined waitlist!", Toast.LENGTH_SHORT).show();
                currentEntrant = new Entrant();
                currentEntrant.setStatus(Entrant.STATUS_WAITLIST);
                updateButton();
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                    actionButton.setEnabled(true);
                }
            }
        });
    }

    /**
     * Removes the user from the event's waitlist via {@link EntrantRepository#leaveWaitlist}.
     * Updates the button to "Join Waitlist" on success.
     */
    private void leaveWaitlist() {
        actionButton.setEnabled(false);
        entrantRepository.leaveWaitlist(eventId, deviceId, new EntrantRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Left waitlist", Toast.LENGTH_SHORT).show();
                currentEntrant = null;
                updateButton();
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to leave waitlist", Toast.LENGTH_SHORT).show();
                    actionButton.setEnabled(true);
                }
            }
        });
    }

    /**
     * Shows a dialog prompting the user to accept or decline their lottery invitation.
     * Accepting updates the status to {@link Entrant#STATUS_ENROLLED};
     * declining updates it to {@link Entrant#STATUS_DECLINED}.
     * Both outcomes grey out the button permanently.
     */
    private void showInvitationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("You're Invited!")
                .setMessage("Would you like to accept or decline this invitation?")
                .setPositiveButton("Accept", (dialog, which) -> respondToInvitation(Entrant.STATUS_ENROLLED))
                .setNegativeButton("Decline", (dialog, which) -> respondToInvitation(Entrant.STATUS_DECLINED))
                .setCancelable(false)
                .show();
    }

    /**
     * Updates the user's entrant status to either enrolled or declined via
     * {@link EntrantRepository#updateStatus} and refreshes the button state.
     *
     * @param newStatus the new status to set — either {@link Entrant#STATUS_ENROLLED}
     *                  or {@link Entrant#STATUS_DECLINED}
     */
    private void respondToInvitation(String newStatus) {
        actionButton.setEnabled(false);
        entrantRepository.updateStatus(eventId, deviceId, newStatus, new EntrantRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                if (!isAdded()) return;
                String msg = newStatus.equals(Entrant.STATUS_ENROLLED) ? "You're enrolled!" : "Invitation declined";
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                currentEntrant.setStatus(newStatus);
                updateButton();
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                    actionButton.setEnabled(true);
                }
            }
        });
    }

    /**
     * Displays a confirmation dialog explaining the event lottery rules
     * before allowing the user to join the waitlist.
     *
     * If the user agrees, they are added to the waitlist.
     */
    private void showTermsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Criteria & Guidelines")
                .setMessage("By joining the waitlist, you understand that selection is random. Enrollment and cancellation policies are fixed.")
                .setPositiveButton("Agree", (dialog, which) -> joinWaitlist())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}
