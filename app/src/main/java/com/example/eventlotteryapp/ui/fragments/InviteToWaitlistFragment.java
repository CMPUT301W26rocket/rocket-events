package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.Notification;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.NotificationRepository;
import com.example.eventlotteryapp.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that allows an organizer to invite specific users to a private event's waitlist.
 * Provides a search bar to find users by name, email, or phone number. Inviting a user
 * creates an entrant document with status {@link Entrant#STATUS_WAITLIST_INVITED} and sends
 * a {@link Notification#TYPE_WAITLIST_INVITE} notification to the invited user.
 *
 * <p>Navigate here from {@link OrganizerEventDetailsFragment} for private events.
 * Pass {@code eventId}, {@code eventTitle}, and {@code organizerDeviceId} as fragment arguments.
 *
 * User Stories Implemented:
 * US 02.01.03 As an organizer, I want to invite specific entrants to a private event's waiting list by searching via name, phone number and/or email.
 * US 01.05.06 As an entrant, I want to receive a notification that I've been invited to join the waiting list for a private event.
 * @author Leyla
 */
public class InviteToWaitlistFragment extends Fragment {

    private String eventId;
    private String eventTitle;
    private String organizerDeviceId;

    private EditText editSearch;
    private TextView textStatus;
    private RecyclerView recyclerResults;
    private SearchResultsAdapter adapter;

    private UserRepository userRepository;
    private EntrantRepository entrantRepository;
    private NotificationRepository notificationRepository;

    /** Maps deviceId → status for all existing entrants of this event. Populated on load. */
    private final java.util.Map<String, String> existingStatuses = new java.util.HashMap<>();

    /** Required empty public constructor. */
    public InviteToWaitlistFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_invite_to_waitlist, container, false);

        if (getArguments() != null) {
            eventId           = getArguments().getString("eventId");
            eventTitle        = getArguments().getString("eventTitle");
            organizerDeviceId = getArguments().getString("organizerDeviceId");
        }

        userRepository        = new UserRepository();
        entrantRepository     = new EntrantRepository();
        notificationRepository = new NotificationRepository();

        editSearch    = view.findViewById(R.id.edit_search);
        textStatus    = view.findViewById(R.id.text_status);
        recyclerResults = view.findViewById(R.id.recycler_search_results);

        adapter = new SearchResultsAdapter();
        recyclerResults.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerResults.setAdapter(adapter);

        loadExistingEntrants();

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.button_search).setOnClickListener(v -> runSearch());

        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch();
                return true;
            }
            return false;
        });

        return view;
    }

    /**
     * Fetches all existing entrant docs for this event and caches their statuses in
     * {@code existingStatuses}. Called once on fragment load so search results can
     * immediately reflect who has already been invited.
     */
    private void loadExistingEntrants() {
        entrantRepository.getAllEntrantsForEvent(eventId,
                new EntrantRepository.FirestoreCallback<java.util.List<Entrant>>() {
                    @Override
                    public void onSuccess(java.util.List<Entrant> entrants) {
                        existingStatuses.clear();
                        for (Entrant e : entrants) {
                            existingStatuses.put(e.getDeviceId(), e.getStatus());
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onFailure(Exception e) { /* statuses stay empty; invite buttons enabled */ }
                });
    }

    /**
     * Reads the search query from the input field and searches users via
     * {@link UserRepository#searchUsers}. Excludes the organizer from results.
     * Updates the RecyclerView with matching users.
     */
    private void runSearch() {
        String query = editSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Enter a name, email, or phone to search", Toast.LENGTH_SHORT).show();
            return;
        }

        textStatus.setVisibility(View.GONE);
        adapter.setResults(new ArrayList<>());

        userRepository.searchUsers(query, new UserRepository.FirestoreCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (!isAdded()) return;
                // Exclude the organizer themselves
                List<User> filtered = new ArrayList<>();
                for (User u : users) {
                    if (!u.getDeviceId().equals(organizerDeviceId)) {
                        filtered.add(u);
                    }
                }
                adapter.setResults(filtered);
                if (filtered.isEmpty()) {
                    textStatus.setText("No users found for \"" + query + "\"");
                    textStatus.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Search failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Invites the given user to the private event's waitlist.
     * Checks if the user is already an entrant first; if so, shows an appropriate message.
     * On success, creates the entrant doc with {@link Entrant#STATUS_WAITLIST_INVITED}
     * and sends a {@link Notification#TYPE_WAITLIST_INVITE} notification.
     *
     * @param user   the user to invite
     * @param button the "Invite" button in the row (disabled during the operation)
     */
    private void inviteUser(User user, Button button) {
        button.setEnabled(false);
        button.setText("...");

        entrantRepository.getEntrant(eventId, user.getDeviceId(),
                new EntrantRepository.FirestoreCallback<Entrant>() {
                    @Override
                    public void onSuccess(Entrant existing) {
                        if (!isAdded()) return;
                        if (existing != null) {
                            String status = existing.getStatus();
                            if (Entrant.STATUS_WAITLIST_INVITED.equals(status)) {
                                Toast.makeText(getContext(),
                                        user.getName() + " is already invited.",
                                        Toast.LENGTH_SHORT).show();
                            } else if (Entrant.STATUS_WAITLIST.equals(status)
                                    || Entrant.STATUS_INVITED.equals(status)
                                    || Entrant.STATUS_ENROLLED.equals(status)) {
                                Toast.makeText(getContext(),
                                        user.getName() + " is already on this event.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // Declined waitlist or other terminal state — re-invite
                                proceedWithInvite(user, button);
                                return;
                            }
                            button.setEnabled(true);
                            button.setText("Invite");
                        } else {
                            proceedWithInvite(user, button);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to check status.", Toast.LENGTH_SHORT).show();
                            button.setEnabled(true);
                            button.setText("Invite");
                        }
                    }
                });
    }

    /**
     * Performs the actual invite: creates the entrant doc and sends the notification.
     *
     * @param user   the user to invite
     * @param button the row's invite button (re-enabled on completion)
     */
    private void proceedWithInvite(User user, Button button) {
        entrantRepository.inviteToPrivateWaitlist(eventId, user.getDeviceId(),
                new EntrantRepository.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (!isAdded()) return;
                        existingStatuses.put(user.getDeviceId(), Entrant.STATUS_WAITLIST_INVITED);
                        sendInviteNotification(user.getDeviceId());
                        Toast.makeText(getContext(),
                                user.getName() + " invited!", Toast.LENGTH_SHORT).show();
                        button.setEnabled(false);
                        button.setText("Invited");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to invite. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                            button.setEnabled(true);
                            button.setText("Invite");
                        }
                    }
                });
    }

    /**
     * Sends a waitlist invitation notification to the specified user.
     *
     * @param recipientDeviceId the device ID of the invited user
     */
    private void sendInviteNotification(String recipientDeviceId) {
        String title = eventTitle != null ? eventTitle : "an event";
        Notification notification = new Notification(
                eventId,
                title,
                Notification.TYPE_WAITLIST_INVITE,
                "You've been personally invited to join the waitlist for the private event \""
                        + title + "\". Click to accept or decline your invitation."
        );
        notificationRepository.addNotification(recipientDeviceId, notification,
                new NotificationRepository.FirestoreCallback<String>() {
                    @Override public void onSuccess(String id) {}
                    @Override public void onFailure(Exception e) {}
                });
    }

    // -----------------------------------------------------------------------------------------
    // Adapter
    // -----------------------------------------------------------------------------------------

    /**
     * RecyclerView adapter that displays a list of {@link User} search results,
     * each with an "Invite" button.
     */
    private class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

        private List<User> results = new ArrayList<>();

        void setResults(List<User> users) {
            results = users;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_search, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = results.get(position);
            holder.textName.setText(user.getName() != null ? user.getName() : "");
            holder.textEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            String phone = user.getPhone();
            if (phone != null && !phone.isEmpty()) {
                holder.textPhone.setText(phone);
                holder.textPhone.setVisibility(View.VISIBLE);
            } else {
                holder.textPhone.setVisibility(View.GONE);
            }
            String status = existingStatuses.get(user.getDeviceId());
            if (Entrant.STATUS_WAITLIST_INVITED.equals(status)) {
                holder.buttonInvite.setEnabled(false);
                holder.buttonInvite.setText("Invited");
            } else if (status != null
                    && (Entrant.STATUS_WAITLIST.equals(status)
                    || Entrant.STATUS_INVITED.equals(status)
                    || Entrant.STATUS_ENROLLED.equals(status))) {
                holder.buttonInvite.setEnabled(false);
                holder.buttonInvite.setText("On Event");
            } else {
                holder.buttonInvite.setEnabled(true);
                holder.buttonInvite.setText("Invite");
                holder.buttonInvite.setOnClickListener(v -> inviteUser(user, holder.buttonInvite));
            }
        }

        @Override
        public int getItemCount() { return results.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textEmail, textPhone;
            Button buttonInvite;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textName    = itemView.findViewById(R.id.text_user_name);
                textEmail   = itemView.findViewById(R.id.text_user_email);
                textPhone   = itemView.findViewById(R.id.text_user_phone);
                buttonInvite = itemView.findViewById(R.id.button_invite);
            }
        }
    }
}
