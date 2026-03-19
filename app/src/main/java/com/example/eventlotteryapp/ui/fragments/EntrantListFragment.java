package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays all entrants for an event grouped by status (waitlist, invited, enrolled, etc.).
 * Opened from the organizer event details screen.
 *
 * User Stories Implemented:
 * US 02.02.01 As an organizer I want to view the list of entrants who joined my event waiting list.
 * US 02.06.01 As an organizer I want to view a list of all chosen entrants who are invited to apply.
 * US 02.06.02 As an organizer I want to see a list of all the cancelled entrants.
 * US 02.06.02 As an organizer I want to see a list of all the cancelled entrants.
 * User Stories Left:
 * US 01.05.01 As an entrant I want another chance to be chosen from the waiting list if a selected user declines an invitation to sign up.
 * US 02.02.02 As an organizer I want to see on a map where entrants joined my event waiting list from.
 * US 02.06.04 As an organizer I want to cancel entrants that did not sign up for the event.
 * US 02.05.01 As an organizer I want to send a notification to chosen entrants to sign up for events.
 * US 02.06.05 As an organizer I want to export a final list of entrants who enrolled for the event in CSV format.
 * US 02.07.01 As an organizer I want to send notifications to all entrants on the waiting list.
 * US 02.07.02 As an organizer I want to send notifications to all selected entrants.
 * US 02.07.03 As an organizer I want to send a notification to all cancelled entrants.
 * @author Daniel
 * @author Leyla
 */
public class EntrantListFragment extends Fragment {

    private static final List<String> STATUS_ORDER = Arrays.asList(
            Entrant.STATUS_INVITED,
            Entrant.STATUS_ENROLLED,
            Entrant.STATUS_WAITLIST,
            Entrant.STATUS_NOT_SELECTED,
            Entrant.STATUS_DECLINED,
            Entrant.STATUS_CANCELLED
    );

    private String eventId;
    private EntrantRepository entrantRepository;
    private UserRepository userRepository;
    private RecyclerView recyclerView;
    private EntrantAdapter adapter;

    public EntrantListFragment() {}

    /** Injects a mock {@link EntrantRepository} for testing. */
    public void setEntrantRepository(EntrantRepository repo) { this.entrantRepository = repo; }

    /** Injects a mock {@link UserRepository} for testing. */
    public void setUserRepository(UserRepository repo) { this.userRepository = repo; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_entrant_list, container, false);

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }

        recyclerView = view.findViewById(R.id.recycler_entrants);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EntrantAdapter();
        recyclerView.setAdapter(adapter);

        if (entrantRepository == null) entrantRepository = new EntrantRepository();
        if (userRepository == null) userRepository = new UserRepository();
        loadEntrants();

        return view;
    }

    private void loadEntrants() {
        entrantRepository.getAllEntrantsForEvent(eventId, new EntrantRepository.FirestoreCallback<List<Entrant>>() {
            @Override
            public void onSuccess(List<Entrant> entrants) {
                if (!isAdded()) return;
                if (entrants == null || entrants.isEmpty()) {
                    List<ListItem> empty = new ArrayList<>();
                    empty.add(new ListItem("No entrants yet", null));
                    adapter.setItems(empty);
                    return;
                }
                resolveNamesAndDisplay(entrants);
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Fetches a display name for each entrant in parallel from the users collection,
     * then builds and shows the grouped list once all lookups have completed.
     */
    private void resolveNamesAndDisplay(List<Entrant> entrants) {
        Map<String, String> names = new ConcurrentHashMap<>();
        AtomicInteger remaining = new AtomicInteger(entrants.size());

        for (Entrant entrant : entrants) {
            userRepository.getUser(entrant.getDeviceId(), new UserRepository.FirestoreCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                        names.put(entrant.getDeviceId(), user.getName());
                    }
                    checkDone();
                }

                @Override
                public void onFailure(Exception e) {
                    checkDone();
                }

                private void checkDone() {
                    if (remaining.decrementAndGet() == 0 && isAdded()) {
                        adapter.setItems(groupByStatus(entrants, names));
                    }
                }
            });
        }
    }

    /** Returns a flat list: a section header followed by entrant rows for each status group. */
    private List<ListItem> groupByStatus(List<Entrant> entrants, Map<String, String> names) {
        List<ListItem> items = new ArrayList<>();
        for (String status : STATUS_ORDER) {
            List<Entrant> group = new ArrayList<>();
            for (Entrant e : entrants) {
                if (status.equals(e.getStatus())) group.add(e);
            }
            if (!group.isEmpty()) {
                items.add(new ListItem(formatStatus(status), null));
                for (Entrant e : group) {
                    String display = names.getOrDefault(e.getDeviceId(), e.getDeviceId());
                    items.add(new ListItem(null, display));
                }
            }
        }
        if (items.isEmpty()) {
            items.add(new ListItem("No entrants yet", null));
        }
        return items;
    }

    private String formatStatus(String status) {
        switch (status) {
            case Entrant.STATUS_WAITLIST:      return "Waitlist";
            case Entrant.STATUS_INVITED:       return "Invited (Pending Response)";
            case Entrant.STATUS_ENROLLED:      return "Enrolled";
            case Entrant.STATUS_DECLINED:      return "Declined";
            case Entrant.STATUS_CANCELLED:     return "Cancelled";
            case Entrant.STATUS_NOT_SELECTED:  return "Not Selected";
            default:                           return status;
        }
    }

    // --- Simple data class for list rows ---

    private static class ListItem {
        final String header;      // non-null = section header row
        final String displayName; // non-null = entrant row (user name or deviceId fallback)
        ListItem(String header, String displayName) {
            this.header = header;
            this.displayName = displayName;
        }
    }

    // --- Adapter ---

    private static class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.VH> {

        private List<ListItem> items = new ArrayList<>();

        void setItems(List<ListItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_entrant, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ListItem item = items.get(position);
            if (item.header != null) {
                holder.headerView.setVisibility(View.VISIBLE);
                holder.headerView.setText(item.header);
                holder.nameView.setVisibility(View.GONE);
            } else {
                holder.headerView.setVisibility(View.GONE);
                holder.nameView.setVisibility(View.VISIBLE);
                holder.nameView.setText(item.displayName);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView headerView;
            final TextView nameView;
            VH(View v) {
                super(v);
                headerView = v.findViewById(R.id.text_section_header);
                nameView   = v.findViewById(R.id.text_device_id);
            }
        }
    }
}
