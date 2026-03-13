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
import com.example.eventlotteryapp.repository.EntrantRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Displays all entrants for an event grouped by status (waitlist, pending, enrolled, etc.).
 * Opened from the organizer event details screen.
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
    private RecyclerView recyclerView;
    private EntrantAdapter adapter;

    public EntrantListFragment() {}

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

        entrantRepository = new EntrantRepository();
        loadEntrants();

        return view;
    }

    private void loadEntrants() {
        entrantRepository.getAllEntrantsForEvent(eventId, new EntrantRepository.FirestoreCallback<List<Entrant>>() {
            @Override
            public void onSuccess(List<Entrant> entrants) {
                if (!isAdded()) return;
                adapter.setEntrants(groupByStatus(entrants));
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** Returns a flat list of items: a header item followed by entrant items for each status group. */
    private List<ListItem> groupByStatus(List<Entrant> entrants) {
        List<ListItem> items = new ArrayList<>();
        for (String status : STATUS_ORDER) {
            List<Entrant> group = new ArrayList<>();
            for (Entrant e : entrants) {
                if (status.equals(e.getStatus())) group.add(e);
            }
            if (!group.isEmpty()) {
                items.add(new ListItem(formatStatus(status), null));
                for (Entrant e : group) {
                    items.add(new ListItem(null, e.getDeviceId()));
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
        final String header;   // non-null = section header row
        final String deviceId; // non-null = entrant row
        ListItem(String header, String deviceId) {
            this.header = header;
            this.deviceId = deviceId;
        }
    }

    // --- Adapter ---

    private static class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.VH> {

        private List<ListItem> items = new ArrayList<>();

        void setEntrants(List<ListItem> items) {
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
            if (item.header != null && item.deviceId == null) {
                // header row
                holder.header.setVisibility(View.VISIBLE);
                holder.header.setText(item.header);
                holder.deviceId.setVisibility(View.GONE);
            } else {
                holder.header.setVisibility(View.GONE);
                holder.deviceId.setVisibility(View.VISIBLE);
                holder.deviceId.setText(item.deviceId);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView header;
            final TextView deviceId;
            VH(View v) {
                super(v);
                header = v.findViewById(R.id.text_section_header);
                deviceId = v.findViewById(R.id.text_device_id);
            }
        }
    }
}
