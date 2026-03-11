package com.example.eventlotteryapp.ui.fragments;

import android.app.DatePickerDialog;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateEventFragment extends Fragment {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private ImageView posterImageView;
    private EditText titleInput, descriptionInput, locationInput;
    private EditText registrationFeeInput, lotteryCapacityInput, waitlistLimitInput;
    private EditText eventStartDateInput, registrationOpenDateInput, registrationCloseDateInput;
    private CheckBox hasWaitlistLimitCheckBox, geolocationRequiredCheckBox;
    private Button createButton;

    private EventRepository eventRepository;
    private UserRepository userRepository;
    private ImageRepository imageRepository;

    private String deviceId;
    private Uri selectedImageUri = null;

    // Selected dates — null means not yet chosen
    private Date selectedEventStartDate        = null;
    private Date selectedRegistrationOpenDate  = null;
    private Date selectedRegistrationCloseDate = null;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
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

        posterImageView          = view.findViewById(R.id.image_event_poster);
        Button uploadButton      = view.findViewById(R.id.button_upload_poster);
        titleInput               = view.findViewById(R.id.edit_event_title);
        descriptionInput         = view.findViewById(R.id.edit_event_description);
        locationInput            = view.findViewById(R.id.edit_event_location);
        registrationFeeInput     = view.findViewById(R.id.edit_registration_fee);
        lotteryCapacityInput     = view.findViewById(R.id.edit_lottery_capacity);
        waitlistLimitInput       = view.findViewById(R.id.edit_waitlist_limit);
        eventStartDateInput      = view.findViewById(R.id.edit_event_start_date);
        registrationOpenDateInput  = view.findViewById(R.id.edit_registration_open_date);
        registrationCloseDateInput = view.findViewById(R.id.edit_registration_close_date);
        hasWaitlistLimitCheckBox   = view.findViewById(R.id.checkbox_has_waitlist_limit);
        geolocationRequiredCheckBox = view.findViewById(R.id.checkbox_geolocation_required);
        createButton             = view.findViewById(R.id.button_create_event);

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        eventRepository = new EventRepository();
        userRepository  = new UserRepository();
        imageRepository = new ImageRepository();

        uploadButton.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        createButton.setOnClickListener(v -> saveEventToFirestore());

        // Each date field opens a DatePickerDialog on tap
        eventStartDateInput.setOnClickListener(v ->
                showDatePicker(eventStartDateInput, date -> selectedEventStartDate = date));
        registrationOpenDateInput.setOnClickListener(v ->
                showDatePicker(registrationOpenDateInput, date -> selectedRegistrationOpenDate = date));
        registrationCloseDateInput.setOnClickListener(v ->
                showDatePicker(registrationCloseDateInput, date -> selectedRegistrationCloseDate = date));

        return view;
    }

    private void showDatePicker(EditText target, OnDateSelectedListener listener) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (datePicker, year, month, day) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    Date date = selected.getTime();
                    target.setText(DATE_FORMAT.format(date));
                    listener.onDateSelected(date);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private interface OnDateSelectedListener {
        void onDateSelected(Date date);
    }

    private void saveEventToFirestore() {
        String title        = titleInput.getText().toString().trim();
        String description  = descriptionInput.getText().toString().trim();
        String location     = locationInput.getText().toString().trim();
        String feeText      = registrationFeeInput.getText().toString().trim();
        String capacityText = lotteryCapacityInput.getText().toString().trim();
        String waitlistText = waitlistLimitInput.getText().toString().trim();

        boolean hasWaitlistLimit    = hasWaitlistLimitCheckBox.isChecked();
        boolean geolocationRequired = geolocationRequiredCheckBox.isChecked();

        // --- Validation ---
        if (TextUtils.isEmpty(title)) {
            titleInput.setError("Title is required");
            titleInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(location)) {
            locationInput.setError("Location is required");
            locationInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(capacityText)) {
            lotteryCapacityInput.setError("Enrollment capacity is required");
            lotteryCapacityInput.requestFocus();
            return;
        }
        if (selectedEventStartDate == null) {
            eventStartDateInput.setError("Event start date is required");
            eventStartDateInput.requestFocus();
            return;
        }
        if (selectedRegistrationOpenDate == null) {
            registrationOpenDateInput.setError("Registration open date is required");
            registrationOpenDateInput.requestFocus();
            return;
        }
        if (selectedRegistrationCloseDate == null) {
            registrationCloseDateInput.setError("Registration close date is required");
            registrationCloseDateInput.requestFocus();
            return;
        }
        if (!selectedRegistrationCloseDate.after(selectedRegistrationOpenDate)) {
            registrationCloseDateInput.setError("Close date must be after open date");
            registrationCloseDateInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(deviceId)) {
            Toast.makeText(getContext(), "Missing device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse lottery capacity
        int lotteryCapacity;
        try {
            lotteryCapacity = Integer.parseInt(capacityText);
            if (lotteryCapacity <= 0) {
                lotteryCapacityInput.setError("Capacity must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            lotteryCapacityInput.setError("Enter a valid number");
            return;
        }

        // Parse registration fee (defaults to 0.0 if empty)
        double registrationFee = 0.0;
        if (!TextUtils.isEmpty(feeText)) {
            try {
                registrationFee = Double.parseDouble(feeText);
                if (registrationFee < 0) {
                    registrationFeeInput.setError("Fee cannot be negative");
                    return;
                }
            } catch (NumberFormatException e) {
                registrationFeeInput.setError("Enter a valid amount");
                return;
            }
        }

        // Parse optional waitlist limit
        int waitlistLimit = 0;
        if (hasWaitlistLimit) {
            if (TextUtils.isEmpty(waitlistText)) {
                waitlistLimitInput.setError("Waitlist limit is required when enabled");
                return;
            }
            try {
                waitlistLimit = Integer.parseInt(waitlistText);
                if (waitlistLimit <= 0) {
                    waitlistLimitInput.setError("Waitlist limit must be greater than 0");
                    return;
                }
            } catch (NumberFormatException e) {
                waitlistLimitInput.setError("Enter a valid number");
                return;
            }
        }

        createButton.setEnabled(false);

        final int finalLotteryCapacity = lotteryCapacity;
        final int finalWaitlistLimit   = waitlistLimit;
        final double finalFee          = registrationFee;

        // Fetch organizer name then proceed
        userRepository.getUser(deviceId, new UserRepository.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                String organizerName = (user != null && user.getName() != null)
                        ? user.getName() : "Unknown";

                if (selectedImageUri != null) {
                    uploadPosterThenSave(organizerName, title, description, location,
                            finalFee, finalLotteryCapacity, finalWaitlistLimit,
                            hasWaitlistLimit, geolocationRequired);
                } else {
                    buildAndSaveEvent(organizerName, "", title, description, location,
                            finalFee, finalLotteryCapacity, finalWaitlistLimit,
                            hasWaitlistLimit, geolocationRequired);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load your profile", Toast.LENGTH_SHORT).show();
                createButton.setEnabled(true);
            }
        });
    }

    private void uploadPosterThenSave(String organizerName, String title, String description,
                                      String location, double fee, int lotteryCapacity,
                                      int waitlistLimit, boolean hasWaitlistLimit,
                                      boolean geolocationRequired) {
        Toast.makeText(getContext(), "Uploading poster...", Toast.LENGTH_SHORT).show();

        imageRepository.uploadEventPoster(requireContext(), deviceId, selectedImageUri,
                new ImageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        buildAndSaveEvent(organizerName, downloadUrl, title, description,
                                location, fee, lotteryCapacity, waitlistLimit,
                                hasWaitlistLimit, geolocationRequired);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Poster upload failed — saving event without image",
                                    Toast.LENGTH_SHORT).show();
                        }
                        buildAndSaveEvent(organizerName, "", title, description,
                                location, fee, lotteryCapacity, waitlistLimit,
                                hasWaitlistLimit, geolocationRequired);
                    }
                });
    }

    private void buildAndSaveEvent(String organizerName, String posterUrl,
                                   String title, String description, String location,
                                   double fee, int lotteryCapacity, int waitlistLimit,
                                   boolean hasWaitlistLimit, boolean geolocationRequired) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setLocation(location);
        event.setOrganizerId(deviceId);
        event.setOrganizerName(organizerName);
        event.setPosterUrl(posterUrl);
        event.setQrCodeValue("");
        event.setRegistrationFee(fee);
        event.setLotteryCapacity(lotteryCapacity);
        event.setEventStartDate(selectedEventStartDate);
        event.setRegistrationOpenDate(selectedRegistrationOpenDate);
        event.setRegistrationCloseDate(selectedRegistrationCloseDate);
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
        locationInput.setText("");
        registrationFeeInput.setText("");
        lotteryCapacityInput.setText("");
        waitlistLimitInput.setText("");
        eventStartDateInput.setText("");
        registrationOpenDateInput.setText("");
        registrationCloseDateInput.setText("");
        hasWaitlistLimitCheckBox.setChecked(false);
        geolocationRequiredCheckBox.setChecked(false);
        selectedImageUri = null;
        selectedEventStartDate = null;
        selectedRegistrationOpenDate = null;
        selectedRegistrationCloseDate = null;
        posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        posterImageView.setBackgroundResource(R.drawable.poster_placeholder_bg);
        createButton.setEnabled(true);
    }
}
