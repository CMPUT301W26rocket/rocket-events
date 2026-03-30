package com.example.eventlotteryapp.ui.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
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
import com.example.eventlotteryapp.utils.QRCodeUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment that provides a form for creating a new event.
 *
 * <p>The user fills in the event title, description, location, registration fee,
 * lottery capacity, optional waitlist limit, three date fields, and optional geolocation
 * and waitlist-limit flags. An event poster image can be selected from the gallery.
 *
 * <p>On submission the fragment:
 * <ol>
 *   <li>Validates all required fields.</li>
 *   <li>Fetches the organizer's display name from {@link UserRepository}.</li>
 *   <li>If a poster was selected, uploads it via {@link ImageRepository} before saving.</li>
 *   <li>Saves the event to Firestore via {@link EventRepository#addEvent}.</li>
 *   <li>Clears the form and pops the back stack on success.</li>
 * </ol>
 * User Stories Implemented:
 * US 02.01.01 As an organizer I want to create a new event and generate a unique promotional QR code that links to the event description and event poster in the app.
 * US 02.01.04 As an organizer, I want to set a registration period.
 * US 02.02.03 As an organizer I want to enable or disable the geolocation requirement for my event.
 * US 02.03.01 As an organizer I want to OPTIONALLY limit the number of entrants who can join my waiting list.
 * US 02.04.01 As an organizer I want to upload an event poster to the event details page to provide visual information to entrants.
 * US 02.05.02 As an organizer I want to set the system to sample a specified number of attendees to register for the event.
 * @author Daniel
 * @author Leyla
 */
public class CreateEventFragment extends Fragment {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private ImageView posterImageView;
    private EditText titleInput, descriptionInput, locationInput;
    private EditText registrationFeeInput, lotteryCapacityInput, waitlistLimitInput;
    private EditText eventStartDateInput, registrationOpenDateInput, registrationCloseDateInput;
    private CheckBox hasWaitlistLimitCheckBox, geolocationRequiredCheckBox, privateEventCheckBox;
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

    /**
     * Required empty public constructor.
     */
    public CreateEventFragment() {}

    /** Allows tests to inject a mock {@link EventRepository} before the fragment loads. */
    public void setEventRepository(EventRepository repo) { this.eventRepository = repo; }

    /** Allows tests to inject a mock {@link UserRepository} before the fragment loads. */
    public void setUserRepository(UserRepository repo) { this.userRepository = repo; }

    /** Allows tests to pre-set the event start date, bypassing the DatePickerDialog. */
    public void setSelectedEventStartDate(Date date) { this.selectedEventStartDate = date; }

    /** Allows tests to pre-set the registration open date, bypassing the DatePickerDialog. */
    public void setSelectedRegistrationOpenDate(Date date) { this.selectedRegistrationOpenDate = date; }

    /** Allows tests to pre-set the registration close date, bypassing the DatePickerDialog. */
    public void setSelectedRegistrationCloseDate(Date date) { this.selectedRegistrationCloseDate = date; }

    /**
     * Inflates the fragment layout, binds all form fields and buttons, and attaches
     * click listeners for the gallery picker, date pickers, and the create button.
     * Also reads the device ID from the fragment arguments.
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
        hasWaitlistLimitCheckBox    = view.findViewById(R.id.checkbox_has_waitlist_limit);
        geolocationRequiredCheckBox = view.findViewById(R.id.checkbox_geolocation_required);
        privateEventCheckBox        = view.findViewById(R.id.checkbox_private_event);
        createButton             = view.findViewById(R.id.button_create_event);

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        if (eventRepository == null) eventRepository = new EventRepository();
        if (userRepository == null)  userRepository  = new UserRepository();
        if (imageRepository == null) imageRepository = new ImageRepository();

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

    /**
     * Shows a {@link DatePickerDialog} pre-filled to today's date.
     * When the user confirms a date, the formatted string is written into {@code target}
     * and the selected {@link Date} is delivered to {@code listener}.
     *
     * @param target   the {@link EditText} to display the formatted date in
     * @param listener callback that receives the chosen {@link Date}
     */
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

    /**
     * Internal callback interface used by {@link #showDatePicker} to return the selected date.
     */
    private interface OnDateSelectedListener {
        void onDateSelected(Date date);
    }

    /**
     * Validates all form fields, then fetches the organizer name and triggers either
     * {@link #uploadPosterThenSave} (if a poster was selected) or {@link #buildAndSaveEvent}
     * directly.
     */
    private void saveEventToFirestore() {
        String title        = titleInput.getText().toString().trim();
        String description  = descriptionInput.getText().toString().trim();
        String location     = locationInput.getText().toString().trim();
        String feeText      = registrationFeeInput.getText().toString().trim();
        String capacityText = lotteryCapacityInput.getText().toString().trim();
        String waitlistText = waitlistLimitInput.getText().toString().trim();

        boolean hasWaitlistLimit    = hasWaitlistLimitCheckBox.isChecked();
        boolean geolocationRequired = geolocationRequiredCheckBox.isChecked();
        boolean isPrivate           = privateEventCheckBox.isChecked();

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
                            hasWaitlistLimit, geolocationRequired, isPrivate);
                } else {
                    buildAndSaveEvent(organizerName, "", title, description, location,
                            finalFee, finalLotteryCapacity, finalWaitlistLimit,
                            hasWaitlistLimit, geolocationRequired, isPrivate);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load your profile", Toast.LENGTH_SHORT).show();
                createButton.setEnabled(true);
            }
        });
    }

    /**
     * Uploads the selected poster image to Firebase Storage via {@link ImageRepository},
     * then calls {@link #buildAndSaveEvent} with the resulting download URL.
     * If the upload fails, the event is saved without a poster.
     *
     * @param organizerName     the organizer's display name
     * @param title             event title
     * @param description       event description
     * @param location          event location
     * @param fee               registration fee
     * @param lotteryCapacity   number of spots in the lottery draw
     * @param waitlistLimit     maximum waitlist size (only relevant when {@code hasWaitlistLimit} is true)
     * @param hasWaitlistLimit  whether a waitlist size cap is enforced
     * @param geolocationRequired whether geolocation verification is required to join
     */
    private void uploadPosterThenSave(String organizerName, String title, String description,
                                      String location, double fee, int lotteryCapacity,
                                      int waitlistLimit, boolean hasWaitlistLimit,
                                      boolean geolocationRequired, boolean isPrivate) {
        Toast.makeText(getContext(), "Uploading poster...", Toast.LENGTH_SHORT).show();

        imageRepository.uploadEventPoster(requireContext(), deviceId, selectedImageUri,
                new ImageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        buildAndSaveEvent(organizerName, downloadUrl, title, description,
                                location, fee, lotteryCapacity, waitlistLimit,
                                hasWaitlistLimit, geolocationRequired, isPrivate);
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
                                hasWaitlistLimit, geolocationRequired, isPrivate);
                    }
                });
    }

    /**
     * Constructs an {@link Event} object from the provided parameters and saves it to
     * Firestore via {@link EventRepository#addEvent}. On success, clears the form and
     * pops the back stack.
     *
     * @param organizerName     the organizer's display name
     * @param posterUrl         Firebase Storage download URL for the poster, or empty string if none
     * @param title             event title
     * @param description       event description
     * @param location          event location
     * @param fee               registration fee
     * @param lotteryCapacity   number of spots in the lottery draw
     * @param waitlistLimit     maximum waitlist size (only relevant when {@code hasWaitlistLimit} is true)
     * @param hasWaitlistLimit  whether a waitlist size cap is enforced
     * @param geolocationRequired whether geolocation verification is required to join
     */
    private void buildAndSaveEvent(String organizerName, String posterUrl,
                                   String title, String description, String location,
                                   double fee, int lotteryCapacity, int waitlistLimit,
                                   boolean hasWaitlistLimit, boolean geolocationRequired,
                                   boolean isPrivate) {
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
        event.setPrivateEvent(isPrivate);

        eventRepository.addEvent(event, new EventRepository.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String eventId) {
                if (getContext() == null) {
                    clearForm();
                    return;
                }

                if (isPrivate) {
                    clearForm();
                    Toast.makeText(getContext(), "Private event created", Toast.LENGTH_SHORT).show();
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStack();
                    }
                    return;
                }

                // Build the QR content URI and persist it on the event
                String qrContent = "eventlotteryapp://event/" + eventId;
                event.setQrCodeValue(qrContent);
                eventRepository.updateEvent(event, new EventRepository.FirestoreCallback<Void>() {
                    @Override public void onSuccess(Void v) {}
                    @Override public void onFailure(Exception e) {}
                });

                // Generate the QR bitmap and show the success dialog
                Bitmap qrBitmap = QRCodeUtils.generateQrBitmap(qrContent, 600);
                clearForm();
                showEventCreatedDialog(qrBitmap, title);
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

    /**
     * Shows a dialog confirming that the event was created, displaying the generated QR code
     * and offering a button to download it to the device gallery.
     *
     * @param qrBitmap   the generated QR code bitmap, or {@code null} if generation failed
     * @param eventTitle the event title, used as the filename when saving the QR code
     */
    private void showEventCreatedDialog(Bitmap qrBitmap, String eventTitle) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_event_created, null);

        ImageView qrImageView   = dialogView.findViewById(R.id.image_qr_code);
        Button downloadButton   = dialogView.findViewById(R.id.button_download_qr);
        Button doneButton       = dialogView.findViewById(R.id.button_done);

        if (qrBitmap != null) {
            qrImageView.setImageBitmap(qrBitmap);
        } else {
            downloadButton.setEnabled(false);
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        downloadButton.setOnClickListener(v -> {
            if (qrBitmap != null) {
                boolean saved = QRCodeUtils.saveQrToGallery(requireContext(), qrBitmap, eventTitle);
                Toast.makeText(getContext(),
                        saved ? "QR code saved to gallery!" : "Failed to save QR code",
                        Toast.LENGTH_SHORT).show();
            }
        });

        doneButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        dialog.show();
    }

    /**
     * Resets all form fields, checkboxes, date selections, and the poster image back to
     * their default empty state. Re-enables the create button after a successful submission.
     */
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
        privateEventCheckBox.setChecked(false);
        selectedImageUri = null;
        selectedEventStartDate = null;
        selectedRegistrationOpenDate = null;
        selectedRegistrationCloseDate = null;
        posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        posterImageView.setBackgroundResource(R.drawable.poster_placeholder_bg);
        createButton.setEnabled(true);
    }
}
