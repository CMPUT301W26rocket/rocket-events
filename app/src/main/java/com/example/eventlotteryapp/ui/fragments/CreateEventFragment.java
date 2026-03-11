package com.example.eventlotteryapp.ui.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.repository.ImageRepository;
import com.example.eventlotteryapp.repository.UserRepository;

import java.util.Date;

public class CreateEventFragment extends Fragment {

    private ImageView posterImageView;
    private EditText titleInput, descriptionInput, waitlistLimitInput;
    private CheckBox hasWaitlistLimitCheckBox, geolocationRequiredCheckBox;
    private Button createButton;

    private EventRepository eventRepository;
    private UserRepository userRepository;
    private ImageRepository imageRepository;

    private String deviceId;
    private Uri selectedImageUri = null; // null means no image selected

    // Registers the gallery picker — must be set up before onCreateView
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    // Show the selected image immediately in the preview
                    Glide.with(this).load(uri).centerCrop().into(posterImageView);
                }
            });

    public CreateEventFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        posterImageView       = view.findViewById(R.id.image_event_poster);
        Button uploadButton   = view.findViewById(R.id.button_upload_poster);
        titleInput            = view.findViewById(R.id.edit_event_title);
        descriptionInput      = view.findViewById(R.id.edit_event_description);
        waitlistLimitInput    = view.findViewById(R.id.edit_waitlist_limit);
        hasWaitlistLimitCheckBox    = view.findViewById(R.id.checkbox_has_waitlist_limit);
        geolocationRequiredCheckBox = view.findViewById(R.id.checkbox_geolocation_required);
        createButton          = view.findViewById(R.id.button_create_event);

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        eventRepository = new EventRepository();
        userRepository  = new UserRepository();
        imageRepository = new ImageRepository();

        uploadButton.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        createButton.setOnClickListener(v -> saveEventToFirestore());

        return view;
    }

    private void saveEventToFirestore() {
        String title       = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String waitlistText = waitlistLimitInput.getText().toString().trim();

        boolean hasWaitlistLimit    = hasWaitlistLimitCheckBox.isChecked();
        boolean geolocationRequired = geolocationRequiredCheckBox.isChecked();

        if (TextUtils.isEmpty(title)) {
            titleInput.setError("Title is required");
            return;
        }

        if (TextUtils.isEmpty(deviceId)) {
            Toast.makeText(getContext(), "Missing device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        int waitlistLimit = 0;
        if (hasWaitlistLimit) {
            if (TextUtils.isEmpty(waitlistText)) {
                waitlistLimitInput.setError("Waitlist limit is required");
                return;
            }
            try {
                waitlistLimit = Integer.parseInt(waitlistText);
            } catch (NumberFormatException e) {
                waitlistLimitInput.setError("Enter a valid number");
                return;
            }
        }

        createButton.setEnabled(false);

        final int finalWaitlistLimit = waitlistLimit;

        // Step 1: fetch organizer name
        userRepository.getUser(deviceId, new UserRepository.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                String organizerName = (user != null && user.getName() != null)
                        ? user.getName() : "Unknown";

                if (selectedImageUri != null) {
                    // Step 2a: upload poster first, then save event with URL
                    uploadPosterThenSave(organizerName, finalWaitlistLimit,
                            hasWaitlistLimit, geolocationRequired, title, description);
                } else {
                    // Step 2b: no image — save event immediately with empty posterUrl
                    buildAndSaveEvent(organizerName, "", title, description,
                            finalWaitlistLimit, hasWaitlistLimit, geolocationRequired);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load your profile", Toast.LENGTH_SHORT).show();
                createButton.setEnabled(true);
            }
        });
    }

    private void uploadPosterThenSave(String organizerName, int waitlistLimit,
                                      boolean hasWaitlistLimit, boolean geolocationRequired,
                                      String title, String description) {
        Toast.makeText(getContext(), "Uploading poster...", Toast.LENGTH_SHORT).show();

        imageRepository.uploadEventPoster(requireContext(), deviceId, selectedImageUri, new ImageRepository.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                buildAndSaveEvent(organizerName, downloadUrl, title, description,
                        waitlistLimit, hasWaitlistLimit, geolocationRequired);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Poster upload failed — saving event without image",
                            Toast.LENGTH_SHORT).show();
                }
                // Still save the event, just without a poster
                buildAndSaveEvent(organizerName, "", title, description,
                        waitlistLimit, hasWaitlistLimit, geolocationRequired);
            }
        });
    }

    private void buildAndSaveEvent(String organizerName, String posterUrl,
                                   String title, String description,
                                   int waitlistLimit, boolean hasWaitlistLimit,
                                   boolean geolocationRequired) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setOrganizerId(deviceId);
        event.setOrganizerName(organizerName);
        event.setPosterUrl(posterUrl);
        event.setQrCodeValue("");
        event.setRegistrationOpenDate(new Date());
        event.setRegistrationCloseDate(new Date());
        event.setGeolocationRequired(geolocationRequired);
        event.setHasWaitlistLimit(hasWaitlistLimit);
        event.setWaitlistLimit(waitlistLimit);

        eventRepository.addEvent(event, new EventRepository.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Event created!", Toast.LENGTH_SHORT).show();
                }
                clearForm();
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to save event", Toast.LENGTH_SHORT).show();
                }
                createButton.setEnabled(true);
            }
        });
    }

    private void clearForm() {
        titleInput.setText("");
        descriptionInput.setText("");
        waitlistLimitInput.setText("");
        hasWaitlistLimitCheckBox.setChecked(true);
        geolocationRequiredCheckBox.setChecked(false);
        selectedImageUri = null;
        posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        posterImageView.setBackgroundResource(R.drawable.poster_placeholder_bg);
        createButton.setEnabled(true);
    }
}
