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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment that allows an organizer to assign a user as a co-organizer for their event.
 * Provides a search bar to find users by name, email, or phone number. Assigning a user
 * creates an entrant document with status {@link Entrant#STATUS_CO_ORGANIZER}, which
 * prevents them from joining the event's waitlist pool, and sends a
 * {@link Notification#TYPE_CO_ORGANIZER} notification to inform them of the assignment.
 *
 * <p>Navigate here from {@link OrganizerEventDetailsFragment} via the "Assign Co-Organizer"
 * button. Pass {@code eventId}, {@code eventTitle}, and {@code organizerDeviceId} as
 * fragment arguments.
 *
 * User Stories Implemented:
 * US 02.09.01 As an organizer, I want to assign an entrant as a co-organizer for my event, which prevents them from joining the entrant pool for that event.
 * US 01.09.01 As an entrant, I want to receive a notification if I have been invited to be a co-organizer for an event.
 * @author Leyla
 */
public class CoOrganizerFragment extends Fragment {

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
    private final Map<String, String> existingStatuses = new HashMap<>();

    /** Required empty public constructor. */
    public CoOrganizerFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_co_organizer, container, false);

        if (getArguments() != null) {
            eventId           = getArguments().getString("eventId");
            eventTitle        = getArguments().getString("eventTitle");
            organizerDeviceId = getArguments().getString("organizerDeviceId");
        }

        userRepository         = new UserRepository();
        entrantRepository      = new EntrantRepository();
        notificationRepository = new NotificationRepository();

        editSearch      = view.findViewById(R.id.edit_search);
        textStatus      = view.findViewById(R.id.text_status);
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
     * Fetches all existing entrant docs for this event and caches their statuses so
     * search results immediately reflect who is already assigned or on the event.
     */
    private void loadExistingEntrants() {
        entrantRepository.getAllEntrantsForEvent(eventId,
                new EntrantRepository.FirestoreCallback<List<Entrant>>() {
                    @Override
                    public void onSuccess(List<Entrant> entrants) {
                        existingStatuses.clear();
                        for (Entrant e : entrants) {
                            existingStatuses.put(e.getDeviceId(), e.getStatus());
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onFailure(Exception e) { /* buttons default to enabled */ }
                });
    }

    /**
     * Searches users by name, email, or phone and displays matching results.
     * Excludes the organizer from results.
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
                List<User> filtered = new ArrayList<>();
                for (User u : users) {
                    if (!u.getDeviceId().equals(organizerDeviceId)) filtered.add(u);
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
     * Assigns the given user as co-organizer. Checks first if they already have a status
     * on this event — if they are already a co-organizer or an active entrant, shows an
     * appropriate message instead of reassigning.
     *
     * @param user   the user to assign
     * @param button the "Assign" button in the row (updated on completion)
     */
    private void assignCoOrganizer(User user, Button button) {
        button.setEnabled(false);
        button.setText("...");

        entrantRepository.getEntrant(eventId, user.getDeviceId(),
                new EntrantRepository.FirestoreCallback<Entrant>() {
                    @Override
                    public void onSuccess(Entrant existing) {
                        if (!isAdded()) return;
                        if (existing != null
                                && Entrant.STATUS_CO_ORGANIZER.equals(existing.getStatus())) {
                            Toast.makeText(getContext(),
                                    user.getName() + " is already a co-organizer.",
                                    Toast.LENGTH_SHORT).show();
                            button.setEnabled(false);
                            button.setText("Assigned");
                        } else {
                            proceedWithAssignment(user, button);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to check status.", Toast.LENGTH_SHORT).show();
                            button.setEnabled(true);
                            button.setText("Assign");
                        }
                    }
                });
    }

    /**
     * Creates the co-organizer entrant doc and sends the assignment notification.
     *
     * @param user   the user to assign
     * @param button the row's assign button
     */
    private void proceedWithAssignment(User user, Button button) {
        entrantRepository.assignCoOrganizer(eventId, user.getDeviceId(),
                new EntrantRepository.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (!isAdded()) return;
                        existingStatuses.put(user.getDeviceId(), Entrant.STATUS_CO_ORGANIZER);
                        sendCoOrganizerNotification(user.getDeviceId());
                        Toast.makeText(getContext(),
                                user.getName() + " assigned as co-organizer!", Toast.LENGTH_SHORT).show();
                        button.setEnabled(false);
                        button.setText("Assigned");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to assign. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                            button.setEnabled(true);
                            button.setText("Assign");
                        }
                    }
                });
    }

    /**
     * Sends a co-organizer assignment notification to the specified user.
     *
     * @param recipientDeviceId the device ID of the assigned co-organizer
     */
    private void sendCoOrganizerNotification(String recipientDeviceId) {
        String title = eventTitle != null ? eventTitle : "an event";
        Notification notification = new Notification(
                eventId,
                title,
                Notification.TYPE_CO_ORGANIZER,
                "You've been assigned as a co-organizer for the event \"" + title + "\". "
                        + "You can view the event in your event history."
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
            if (Entrant.STATUS_CO_ORGANIZER.equals(status)) {
                holder.buttonAssign.setEnabled(false);
                holder.buttonAssign.setText("Assigned");
            } else {
                holder.buttonAssign.setEnabled(true);
                holder.buttonAssign.setText("Assign");
                holder.buttonAssign.setOnClickListener(v -> assignCoOrganizer(user, holder.buttonAssign));
            }
        }

        @Override
        public int getItemCount() { return results.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textEmail, textPhone;
            Button buttonAssign;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textName     = itemView.findViewById(R.id.text_user_name);
                textEmail    = itemView.findViewById(R.id.text_user_email);
                textPhone    = itemView.findViewById(R.id.text_user_phone);
                buttonAssign = itemView.findViewById(R.id.button_invite); // reuses item_user_search layout
            }
        }
    }
}
