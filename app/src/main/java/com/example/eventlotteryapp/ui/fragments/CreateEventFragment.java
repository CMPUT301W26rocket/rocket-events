package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;

import java.util.Date;

public class CreateEventFragment extends Fragment {

    private EditText titleInput;
    private EditText descriptionInput;
    private EditText waitlistLimitInput;
    private CheckBox hasWaitlistLimitCheckBox;
    private CheckBox geolocationRequiredCheckBox;
    private Button createButton;

    private EventRepository eventRepository;
    private String deviceId;

    public CreateEventFragment() {
        // required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        titleInput = view.findViewById(R.id.edit_event_title);
        descriptionInput = view.findViewById(R.id.edit_event_description);
        waitlistLimitInput = view.findViewById(R.id.edit_waitlist_limit);
        hasWaitlistLimitCheckBox = view.findViewById(R.id.checkbox_has_waitlist_limit);
        geolocationRequiredCheckBox = view.findViewById(R.id.checkbox_geolocation_required);
        createButton = view.findViewById(R.id.button_create_event);

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        eventRepository = new EventRepository();

        createButton.setOnClickListener(v -> saveEventToFirestore());

        return view;
    }

    private void saveEventToFirestore() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String waitlistText = waitlistLimitInput.getText().toString().trim();

        boolean hasWaitlistLimit = hasWaitlistLimitCheckBox.isChecked();
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

        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setOrganizerId(deviceId);
        event.setPosterUrl("");
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
                    Toast.makeText(getContext(), "Event saved to Firestore", Toast.LENGTH_SHORT).show();
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
            }
        });
    }

    private void clearForm() {
        titleInput.setText("");
        descriptionInput.setText("");
        waitlistLimitInput.setText("");
        hasWaitlistLimitCheckBox.setChecked(true);
        geolocationRequiredCheckBox.setChecked(false);
    }
}