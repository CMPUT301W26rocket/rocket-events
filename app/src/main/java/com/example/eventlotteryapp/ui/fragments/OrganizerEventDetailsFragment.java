package com.example.eventlotteryapp.ui.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Comment;
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.CommentRepository;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.repository.ImageRepository;
import com.example.eventlotteryapp.repository.UserRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that displays the details of an event from the organizer's perspective.
 * Shows event information such as poster, title, description, dates, capacity, etc.
 * User Stories Implemented:
 * US 02.04.02 As an organizer I want to update an event poster to provide visual information to entrants.
 * US 02.08.01 As an organizer, I want to view and delete entrant comments on my event.
 * US 02.08.02 As an organizer, I want to comment on my events so that I can share updates, answer questions, or engage with entrants in the event discussion.
 * @author Santiago
 * @author Leyla
 * @co-author Daniel
 */
public class OrganizerEventDetailsFragment extends Fragment {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat COMMENT_DATE_FORMAT =
            new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());

    private String eventId;
    private String deviceId;

    private ImageView posterImageView;
    private TextView titleView, organizerView, descriptionView, locationView;
    private TextView feeView, capacityView, eventDateView, regOpenView, regCloseView;
    private TextView geolocationView, waitlistView;
    private Button lotteryButton;
    private Button entrantsButton;
    private Button inviteWaitlistButton;
    private Button coOrganizerButton;
    private LinearLayout commentsContainer;
    private TextView noCommentsText;
    private EditText commentInput;
    private Button sendCommentButton;
    private Event currentEvent;

    private EventRepository eventRepository;
    private EntrantRepository entrantRepository;
    private CommentRepository commentRepository;
    private UserRepository userRepository;
    private ImageRepository imageRepository;
    private ListenerRegistration commentsListener;
    private String currentUserName;

    private final ActivityResultLauncher<String> posterGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadNewPoster(uri);
            });


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
        lotteryButton        = view.findViewById(R.id.lottery_button);
        entrantsButton       = view.findViewById(R.id.entrants_button);
        inviteWaitlistButton = view.findViewById(R.id.invite_waitlist_button);
        coOrganizerButton    = view.findViewById(R.id.co_organizer_button);
        commentsContainer = view.findViewById(R.id.comments_container);
        noCommentsText    = view.findViewById(R.id.text_no_comments);
        commentInput      = view.findViewById(R.id.edit_comment_input);
        sendCommentButton = view.findViewById(R.id.button_send_comment);

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (getArguments() != null) {
            eventId  = getArguments().getString("eventId");
            deviceId = getArguments().getString("deviceId");
        }

        if (eventRepository == null)   eventRepository   = new EventRepository();
        if (entrantRepository == null) entrantRepository = new EntrantRepository();
        if (commentRepository == null) commentRepository = new CommentRepository();
        if (userRepository == null)    userRepository    = new UserRepository();
        if (imageRepository == null)   imageRepository   = new ImageRepository();

        loadEventDetails();
        loadCurrentUserName();
        attachCommentsListener();
        view.findViewById(R.id.button_update_poster).setOnClickListener(v ->
                posterGalleryLauncher.launch("image/*"));

        lotteryButton.setOnClickListener(v -> handleLotteryClick());
        entrantsButton.setOnClickListener(v -> openEntrantList());
        inviteWaitlistButton.setOnClickListener(v -> openInviteToWaitlist());
        coOrganizerButton.setOnClickListener(v -> openCoOrganizer());
        sendCommentButton.setOnClickListener(v -> handleSendComment());

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
                updateLotteryButton(event);
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

        inviteWaitlistButton.setVisibility(event.isPrivateEvent() ? View.VISIBLE : View.GONE);
    }
    /**
     * Handles clicks on the lottery button.
     *
     * <p>This method triggers the lottery selection process that randomly
     * chooses entrants from the waitlist based on the event's lottery
     * capacity.</p>
     */
    private void updateLotteryButton(Event event) {
        if (event.isLotteryCompleted()) {
            lotteryButton.setText("Lottery Completed");
            lotteryButton.setEnabled(false);
        } else if (event.isRegistrationNotYetOpen()) {
            lotteryButton.setText("Registration Not Open Yet");
            lotteryButton.setEnabled(false);
        } else if (event.isRegistrationOpen()) {
            lotteryButton.setText("Registration Open (Lottery Pending)");
            lotteryButton.setEnabled(false);
        } else {
            lotteryButton.setText("Draw Lottery");
            lotteryButton.setEnabled(true);
        }
    }

    private void handleLotteryClick() {
        if (currentEvent == null) return; // event not loaded yet

        if (currentEvent.isLotteryCompleted()) {
            Toast.makeText(getContext(),
                    "Lottery has already been completed for this event",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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
    private void openEntrantList() {
        EntrantListFragment fragment = new EntrantListFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        args.putString("eventTitle", currentEvent != null ? currentEvent.getTitle() : "Event");
        args.putBoolean("lotteryCompleted", currentEvent != null && currentEvent.isLotteryCompleted());
        args.putLong("registrationCloseDate",
                currentEvent != null && currentEvent.getRegistrationCloseDate() != null
                        ? currentEvent.getRegistrationCloseDate().getTime() : -1);
        args.putInt("lotteryCapacity", currentEvent != null ? currentEvent.getLotteryCapacity() : 0);
        args.putBoolean("geolocationRequired", currentEvent != null && currentEvent.isGeolocationRequired());
        fragment.setArguments(args);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
    /**
     * Opens {@link InviteToWaitlistFragment} so the organizer can search and invite
     * specific users to this private event's waitlist.
     */
    private void openInviteToWaitlist() {
        InviteToWaitlistFragment fragment = new InviteToWaitlistFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        args.putString("eventTitle", currentEvent != null ? currentEvent.getTitle() : "Event");
        args.putString("organizerDeviceId", deviceId);
        fragment.setArguments(args);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Uploads a new poster image to Firebase Storage and updates the event's
     * {@code posterUrl} field in Firestore. Shows the new image immediately on success.
     *
     * @param uri the URI of the image selected from the gallery
     */
    private void uploadNewPoster(Uri uri) {
        if (eventId == null || deviceId == null) return;
        Toast.makeText(getContext(), "Uploading poster...", Toast.LENGTH_SHORT).show();

        imageRepository.uploadEventPoster(requireContext(), deviceId, uri,
                new ImageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        if (!isAdded()) return;
                        eventRepository.updatePosterUrl(eventId, downloadUrl,
                                new EventRepository.FirestoreCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        if (!isAdded()) return;
                                        if (currentEvent != null) currentEvent.setPosterUrl(downloadUrl);
                                        com.bumptech.glide.Glide.with(OrganizerEventDetailsFragment.this)
                                                .load(downloadUrl)
                                                .centerCrop()
                                                .placeholder(R.drawable.ic_image_placeholder)
                                                .into(posterImageView);
                                        Toast.makeText(getContext(), "Poster updated!", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), "Failed to save poster URL.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to upload poster.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Opens {@link CoOrganizerFragment} so the organizer can search and assign a
     * co-organizer for this event.
     */
    private void openCoOrganizer() {
        CoOrganizerFragment fragment = new CoOrganizerFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        args.putString("eventTitle", currentEvent != null ? currentEvent.getTitle() : "Event");
        args.putString("organizerDeviceId", deviceId);
        fragment.setArguments(args);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Executes the lottery selection for the current event.
     *
     * <p>This method retrieves all entrants currently on the waitlist and randomly
     * shuffles them to ensure a fair lottery. A number of entrants equal to the
     * event's lottery capacity (or the size of the waitlist if smaller) are then
     * selected and their status is updated to "pending".</p>
     *
     * <p>After the selection is complete, the event is marked as having its
     * lottery completed in Firestore so that the lottery cannot be run again.</p>
     *
     * <p>If the waitlist is empty or the fetch fails, a message is shown to the
     * organizer via a Toast.</p>
     */
    private void loadCurrentUserName() {
        if (deviceId == null) return;
        userRepository.getUser(deviceId, new UserRepository.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) currentUserName = user.getName();
            }
            @Override
            public void onFailure(Exception e) { /* name stays null; handled in send */ }
        });
    }

    private void attachCommentsListener() {
        commentsListener = commentRepository.listenToComments(eventId,
                new CommentRepository.FirestoreCallback<List<Comment>>() {
                    @Override
                    public void onSuccess(List<Comment> comments) {
                        if (!isAdded()) return;
                        renderComments(comments);
                    }
                    @Override
                    public void onFailure(Exception e) { /* leave placeholder visible */ }
                });
    }

    private void renderComments(List<Comment> comments) {
        commentsContainer.removeAllViews();
        if (comments.isEmpty()) {
            commentsContainer.addView(noCommentsText);
            return;
        }
        for (Comment comment : comments) {
            View item = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_comment, commentsContainer, false);
            ((TextView) item.findViewById(R.id.comment_author)).setText(comment.getAuthorName());
            TextView commentText = item.findViewById(R.id.comment_text);
            TextView showMore = item.findViewById(R.id.comment_show_more);
            commentText.setText(comment.getText());
            String time = comment.getTimestamp() != null
                    ? COMMENT_DATE_FORMAT.format(comment.getTimestamp().toDate())
                    : "";
            ((TextView) item.findViewById(R.id.comment_timestamp)).setText(time);
            commentsContainer.addView(item);
            commentText.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    commentText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    android.text.Layout layout = commentText.getLayout();
                    if (layout != null && layout.getLineCount() >= 4
                            && layout.getEllipsisCount(layout.getLineCount() - 1) > 0) {
                        showMore.setVisibility(View.VISIBLE);
                        showMore.setOnClickListener(v -> {
                            if (commentText.getMaxLines() == 4) {
                                commentText.setMaxLines(Integer.MAX_VALUE);
                                showMore.setText("Show less");
                            } else {
                                commentText.setMaxLines(4);
                                showMore.setText("Show more");
                            }
                        });
                    }
                }
            });

            TextView deleteButton = item.findViewById(R.id.comment_delete);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v ->
                    commentRepository.deleteComment(eventId, comment.getCommentId(),
                            new CommentRepository.FirestoreCallback<Void>() {
                                @Override public void onSuccess(Void unused) {}
                                @Override public void onFailure(Exception e) {
                                    if (isAdded()) Toast.makeText(getContext(),
                                            "Failed to delete comment", Toast.LENGTH_SHORT).show();
                                }
                            }));
        }
    }

    private void handleSendComment() {
        String text = commentInput.getText().toString().trim();
        if (text.isEmpty()) return;

        String authorName = currentUserName != null ? currentUserName : "Unknown";
        Comment comment = new Comment(deviceId, authorName, text, Timestamp.now());

        sendCommentButton.setEnabled(false);
        commentRepository.addComment(eventId, comment, new CommentRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                if (!isAdded()) return;
                commentInput.setText("");
                sendCommentButton.setEnabled(true);
            }
            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to post comment", Toast.LENGTH_SHORT).show();
                    sendCommentButton.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (commentsListener != null) {
            commentsListener.remove();
        }
    }

    private void selectLottery() {
        lotteryButton.setEnabled(false);

        entrantRepository.getEntrantsByStatus(eventId, Entrant.STATUS_WAITLIST, new EntrantRepository.FirestoreCallback<List<Entrant>>() {
            @Override
            public void onSuccess(List<Entrant> waitlist) {
                if (!isAdded()) return;

                if (waitlist == null || waitlist.isEmpty()) {
                    Toast.makeText(getContext(), "No entrants on the waitlist", Toast.LENGTH_SHORT).show();
                    lotteryButton.setEnabled(true);
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

                // Mark lottery as completed so it cannot be run again
                eventRepository.updateLotteryCompleted(eventId, true,
                        new EventRepository.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                currentEvent.setLotteryCompleted(true);
                                if (isAdded()) updateLotteryButton(currentEvent);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                if (isAdded()) Toast.makeText(getContext(),
                                        "Failed to mark lottery completed",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

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