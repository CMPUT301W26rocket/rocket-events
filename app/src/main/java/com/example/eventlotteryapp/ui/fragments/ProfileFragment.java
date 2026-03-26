package com.example.eventlotteryapp.ui.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.FirebaseConnector;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.admin.AdminActivity;
import com.example.eventlotteryapp.ui.auth.ProfileSetupActivity;

/**
 * Fragment that displays and allows editing of the currently logged-in user's profile.
 * Shown on the Profile tab of {@link com.example.eventlotteryapp.ui.main.MainActivity}.
 *
 * <p>Loads the user's name, email, and phone from Firestore on launch. Provides buttons to
 * save changes, toggle notification preferences, and permanently delete the profile.
 * User Stories Implemented:
 * US 01.02.02 As an entrant I want to update information such as name, email and contact information on my profile
 * US 01.02.04 As an entrant, I want to delete my profile if I no longer wish to use the app.
 * US 01.04.03 As an entrant I want to opt out of receiving notifications from organizers and admins.
 * @author William
 *
 */
public class ProfileFragment extends Fragment {

    private EditText editTextName, editTextEmail, editTextPhone;
    private Button buttonSave, buttonNotifications;
    private UserRepository userRepository;
    private FirebaseConnector firebaseConnector;
    private String deviceId;
    private boolean notificationsEnabled = true;

    /** Required empty public constructor. */
    public ProfileFragment() {}

    /**
     * Injects a mock {@link UserRepository} for testing. Must be called inside a
     * {@link androidx.fragment.app.FragmentFactory} before the fragment attaches.
     *
     * @param repo the repository to use instead of creating a new one
     */
    public void setUserRepository(UserRepository repo) {
        this.userRepository = repo;
    }

    /**
     * Inflates the fragment layout, binds all input fields and buttons, reads the device ID
     * from fragment arguments, and triggers the initial profile load from Firestore.
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
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        if (userRepository == null) userRepository = new UserRepository();
        if (firebaseConnector == null) firebaseConnector = new FirebaseConnector();

        editTextName = view.findViewById(R.id.editTextName);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPhone = view.findViewById(R.id.editTextPhone);
        buttonSave = view.findViewById(R.id.buttonSaveProfile);
        buttonNotifications = view.findViewById(R.id.buttonOptOutNotifications);
        Button buttonAdminPanel = view.findViewById(R.id.buttonAdminPanel);

        loadProfile();
        checkAdminEligibility(buttonAdminPanel);

        buttonSave.setOnClickListener(v -> saveProfile());
        buttonNotifications.setOnClickListener(v -> showNotificationsDialog());
        view.findViewById(R.id.buttonEventHistory).setOnClickListener(v -> openEventHistory());
        view.findViewById(R.id.buttonDeleteProfile).setOnClickListener(v -> showDeleteProfileDialog());
        buttonAdminPanel.setOnClickListener(v -> openAdminPanel());

        return view;
    }

    /**
     * Fetches the user's profile from Firestore via {@link UserRepository#getUser} and
     * populates the name, email, and phone fields. Also syncs the notifications toggle
     * button state to match the stored preference.
     */
    private void loadProfile() {
        userRepository.getUser(deviceId, new UserRepository.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && isAdded()) {
                    editTextName.setText(user.getName());
                    editTextEmail.setText(user.getEmail());
                    editTextPhone.setText(user.getPhone());
                    notificationsEnabled = user.isNotificationsEnabled();
                    updateNotificationsButton();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Checks whether this device is in the {@code adminDevices} collection.
     * If so, makes the Admin Panel button visible.
     *
     * @param buttonAdminPanel the button to show if this device is admin-eligible
     */
    private void checkAdminEligibility(Button buttonAdminPanel) {
        firebaseConnector.isAdminDevice(deviceId, isAdmin -> {
            if (isAdmin && isAdded()) {
                buttonAdminPanel.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Launches {@link AdminActivity}, passing the device ID as an extra.
     */
    private void openAdminPanel() {
        Intent intent = new Intent(requireActivity(), AdminActivity.class);
        intent.putExtra("deviceId", deviceId);
        startActivity(intent);
    }

    /**
     * Updates the notifications button label and tint to reflect the current
     * {@code notificationsEnabled} state. Shows "Opt Out" when enabled and
     * "Opt In" when disabled.
     */
    private void updateNotificationsButton() {
        if (notificationsEnabled) {
            buttonNotifications.setText("Opt Out of Notifications");
            buttonNotifications.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.color_primary, null)));
        } else {
            buttonNotifications.setText("Opt In to Notifications");
            buttonNotifications.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#616161")));
        }
    }

    /**
     * Navigates to {@link EventHistoryFragment}, passing the device ID as an argument.
     */
    private void openEventHistory() {
        EventHistoryFragment historyFragment = new EventHistoryFragment();
        Bundle args = new Bundle();
        args.putString("deviceId", deviceId);
        historyFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, historyFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Shows a confirmation dialog before toggling the user's notification preference.
     * The dialog title and message adapt based on whether the user is opting in or out.
     * On confirmation, updates the preference in Firestore via {@link UserRepository#setNotificationsEnabled}
     * and refreshes the button state.
     */
    private void showNotificationsDialog() {
        boolean willEnable = !notificationsEnabled;
        String title = willEnable ? "Opt In to Notifications" : "Opt Out of Notifications";
        String message = willEnable
                ? "You will start receiving updates about events you've joined."
                : "You will no longer receive updates about events you've joined.";
        String buttonLabel = willEnable ? "Opt In" : "Opt Out";

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonLabel, (dialog, which) -> {
                    userRepository.setNotificationsEnabled(deviceId, willEnable, new UserRepository.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            if (isAdded()) {
                                notificationsEnabled = willEnable;
                                updateNotificationsButton();
                                String toast = willEnable ? "Notifications enabled." : "Notifications disabled.";
                                Toast.makeText(getContext(), toast, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Failed to update notifications.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a confirmation dialog before permanently deleting the user's profile.
     * On confirmation, deletes the user's document from Firestore via {@link UserRepository#deleteUser}
     * and navigates to {@link ProfileSetupActivity}. This action cannot be undone.
     */
    private void showDeleteProfileDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    userRepository.deleteUser(deviceId, new UserRepository.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            goToLogin();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Failed to delete profile. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Navigates to {@link ProfileSetupActivity}, passing the device ID as an extra.
     * Clears the back stack so the user cannot navigate back to the main app after deletion.
     */
    private void goToLogin() {
        Intent intent = new Intent(requireActivity(), ProfileSetupActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Validates the name and email fields, then saves the updated profile to Firestore
     * via {@link UserRepository#saveUserProfile}. Disables the save button during the
     * operation to prevent duplicate submissions. Re-enables it on both success and failure.
     */
    private void saveProfile() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Name is required");
            editTextName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        buttonSave.setEnabled(false);
        buttonSave.setText("Saving...");

        userRepository.saveUserProfile(deviceId, name, email, phone,
                new UserRepository.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                            buttonSave.setEnabled(true);
                            buttonSave.setText("Save Changes");
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show();
                            buttonSave.setEnabled(true);
                            buttonSave.setText("Save Changes");
                        }
                    }
                });
    }
}
