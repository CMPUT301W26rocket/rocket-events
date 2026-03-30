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
import androidx.viewpager2.widget.ViewPager2;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.UserRepository;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;


/**
 * Displays all entrants for an event in a tabbed slider view grouped by status.
 * Tabs: Invited, Enrolled, Cancelled, Waitlist (includes Not Selected).
 * Declined entrants are not shown. The Invited tab includes organizer action buttons.
 *
 * User Stories Implemented:
 * US 02.02.01 As an organizer I want to view the list of entrants who joined my event waiting list.
 * US 02.06.01 As an organizer I want to view a list of all chosen entrants who are invited to apply.
 * US 02.06.02 As an organizer I want to see a list of all the cancelled entrants.
 * @author Daniel
 * @author Leyla
 */
public class EntrantListFragment extends Fragment {

    private static final int TAB_INVITED   = 0;
    private static final int TAB_ENROLLED  = 1;
    private static final int TAB_CANCELLED = 2;
    private static final int TAB_WAITLIST  = 3;

    private static final String[] TAB_TITLES = {"Invited", "Enrolled", "Cancelled", "Waitlist"};

    private String eventId;
    private Entrant selectedEntrant = null;
    private EntrantRepository entrantRepository;
    private UserRepository userRepository;

    private final List<List<Entrant>> tabData = new ArrayList<>(); //private final List<List<String>> tabData = new ArrayList<>();


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

        // Initialise empty lists for each tab
        for (int i = 0; i < TAB_TITLES.length; i++) tabData.add(new ArrayList<>());

        if (entrantRepository == null) entrantRepository = new EntrantRepository();
        if (userRepository == null) userRepository = new UserRepository();

        ViewPager2 viewPager = view.findViewById(R.id.view_pager);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        EntrantPagerAdapter pagerAdapter = new EntrantPagerAdapter();
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])).attach();

        loadEntrants(pagerAdapter);

        return view;
    }

    private void loadEntrants(EntrantPagerAdapter pagerAdapter) {
        entrantRepository.getAllEntrantsForEvent(eventId, new EntrantRepository.FirestoreCallback<List<Entrant>>() {
            @Override
            public void onSuccess(List<Entrant> entrants) {
                if (!isAdded()) return;
                if (entrants == null || entrants.isEmpty()) {
                    pagerAdapter.notifyDataSetChanged();
                    return;
                }
                resolveNamesAndDisplay(entrants, pagerAdapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void resolveNamesAndDisplay(List<Entrant> entrants, EntrantPagerAdapter pagerAdapter) {
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
                public void onFailure(Exception e) { checkDone(); }

                private void checkDone() {
                    if (remaining.decrementAndGet() == 0 && isAdded()) {
                        groupIntoTabs(entrants, names, pagerAdapter);
                    }
                }
            });
        }
    }

    private void groupIntoTabs(List<Entrant> entrants, Map<String, String> names,
                                EntrantPagerAdapter pagerAdapter) {
        //for (List<String> list : tabData) list.clear();
        for (List<Entrant> list : tabData) list.clear();

        for (Entrant e : entrants) {
            String display = names.getOrDefault(e.getDeviceId(), e.getDeviceId());
//            switch (e.getStatus()) {
//                case Entrant.STATUS_INVITED:      tabData.get(TAB_INVITED).add(display);   break;
//                case Entrant.STATUS_ENROLLED:     tabData.get(TAB_ENROLLED).add(display);  break;
//                case Entrant.STATUS_CANCELLED:    tabData.get(TAB_CANCELLED).add(display); break;
//                case Entrant.STATUS_WAITLIST:
//                case Entrant.STATUS_NOT_SELECTED: tabData.get(TAB_WAITLIST).add(display);  break;
            switch (e.getStatus()) {
                case Entrant.STATUS_INVITED:
                    tabData.get(TAB_INVITED).add(e);
                    break;
                case Entrant.STATUS_ENROLLED:
                    tabData.get(TAB_ENROLLED).add(e);
                    break;
                case Entrant.STATUS_CANCELLED:
                    tabData.get(TAB_CANCELLED).add(e);
                    break;
                case Entrant.STATUS_WAITLIST:
                case Entrant.STATUS_NOT_SELECTED:
                    tabData.get(TAB_WAITLIST).add(e);
                    break;

                // STATUS_DECLINED intentionally excluded
            }
        }
        pagerAdapter.setNamesMap(names);
        pagerAdapter.notifyDataSetChanged();
    }

    // --- ViewPager2 adapter ---

    private class EntrantPagerAdapter extends RecyclerView.Adapter<EntrantPagerAdapter.PageVH> {

        private Map<String, String> names;
        public void setNamesMap(Map<String, String> names) {
            this.names = names;
        }
        @NonNull
        @Override
        public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.page_entrant_tab, parent, false);
            return new PageVH(page);
        }

        @Override
        public void onBindViewHolder(@NonNull PageVH holder, int position) {
            List<Entrant> entrants = tabData.get(position);

            holder.recycler.setLayoutManager(new LinearLayoutManager(holder.recycler.getContext()));
            holder.recycler.setAdapter(new NamesAdapter(entrants, names, (entrant, pos) -> {
                selectedEntrant = entrant;

                Toast.makeText(holder.recycler.getContext(),
                        "Clicked: " + entrant.getDeviceId(),
                        Toast.LENGTH_SHORT).show();

                holder.layoutButtonsInvited.findViewById(R.id.button_cancel_entrant)
                        .setOnClickListener(v -> {
                            if (selectedEntrant == null) {
                                Toast.makeText(holder.itemView.getContext(), "Select an entrant first", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            entrantRepository.updateStatus(eventId, selectedEntrant.getDeviceId(), Entrant.STATUS_CANCELLED,
                                    new EntrantRepository.FirestoreCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Toast.makeText(holder.itemView.getContext(),
                                                    "Cancelled: " + selectedEntrant.getDeviceId(),
                                                    Toast.LENGTH_SHORT).show();
                                            loadEntrants((EntrantPagerAdapter) ((ViewPager2) requireView().findViewById(R.id.view_pager)).getAdapter());
                                            selectedEntrant = null;
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Toast.makeText(holder.itemView.getContext(), "Failed to cancel entrant", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        });
            }));
            holder.layoutButtonsInvited.findViewById(R.id.button_draw_replacement)
                    .setOnClickListener(v -> {
                                entrantRepository.getAllEntrantsForEvent(eventId, new EntrantRepository.FirestoreCallback<List<Entrant>>() {
                                    @Override
                                    public void onSuccess(List<Entrant> allEntrants) {
                                        if (allEntrants == null || allEntrants.isEmpty()) return;

                                        int openSpots = 0;
                                        for (Entrant e : allEntrants) {
                                            if (Entrant.STATUS_CANCELLED.equals(e.getStatus()) || Entrant.STATUS_DECLINED.equals(e.getStatus())) {
                                                openSpots++;
                                            }
                                        }
                                        if (openSpots == 0) {
                                            Toast.makeText(getContext(), "No open spots for replacements", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        // 2. Collect waitlist / not-selected entrants
                                        List<Entrant> candidates = new ArrayList<>();
                                        for (Entrant e : allEntrants) {
                                            if (Entrant.STATUS_WAITLIST.equals(e.getStatus()) ||
                                                    Entrant.STATUS_NOT_SELECTED.equals(e.getStatus())) {
                                                candidates.add(e);
                                            }
                                        }

                                        if (candidates.isEmpty()) {
                                            Toast.makeText(getContext(), "No candidates available for replacement", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        // 3. Promote candidates to ENROLLED for open spots
                                        int spotsToFill = Math.min(openSpots, candidates.size());
                                        for (int i = 0; i < spotsToFill; i++) {
                                            Entrant replacement = candidates.get(i);

                                            entrantRepository.updateStatus(eventId, replacement.getDeviceId(), Entrant.STATUS_ENROLLED,
                                                    new EntrantRepository.FirestoreCallback<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            Toast.makeText(getContext(),
                                                                    "Replacement enrolled: " + replacement.getDeviceId(),
                                                                    Toast.LENGTH_SHORT).show();
                                                            loadEntrants((EntrantPagerAdapter) ((ViewPager2) requireView().findViewById(R.id.view_pager)).getAdapter());
//
                                                        }

                                                        @Override
                                                        public void onFailure(Exception e) {
                                                            Toast.makeText(getContext(),
                                                                    "Failed to enroll replacement: " + replacement.getDeviceId(),
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });
//                        entrantRepository.getEntrantsByStatus(eventId, Entrant.STATUS_NOT_SELECTED,
//                                new EntrantRepository.FirestoreCallback<List<Entrant>>() {
//                                    @Override
//                                    public void onSuccess(List<Entrant> candidates) {
//                                        if (candidates == null || candidates.isEmpty()) {
//                                            Toast.makeText(holder.itemView.getContext(),
//                                                    "No replacements available", Toast.LENGTH_SHORT).show();
//                                            return;
//                                        }
//                                        Collections.shuffle(candidates);
//                                        // For demonstration, pick the first replacement (you can add your logic)
//                                        Entrant replacement = candidates.get(0);
//                                        Toast.makeText(holder.itemView.getContext(),
//                                                "Replacement drawn: " + replacement.getDeviceId(),
//                                                Toast.LENGTH_SHORT).show();
//                                        // Optionally, update status to INVITED
//                                        int spotsToFill = Math.min(openSpots, candidates.size());
//                                        int k = 0;
//
//                                        while (k < spotsToFill) {
//                                            entrantRepository.updateStatus(eventId, replacement.getDeviceId(), Entrant.STATUS_INVITED,
//                                                    new EntrantRepository.FirestoreCallback<Void>() {
//                                                        @Override
//                                                        public void onSuccess(Void unused) {
//                                                            Toast.makeText(holder.itemView.getContext(),
//                                                                    "Replacements invited: " + replacement.getDeviceId(),
//                                                                    Toast.LENGTH_SHORT).show();
//                                                            // Refresh tabs
//                                                            loadEntrants((EntrantPagerAdapter) ((ViewPager2) requireView().findViewById(R.id.view_pager)).getAdapter());
//                                                        }
//
//                                                        @Override
//                                                        public void onFailure(Exception e) {
//                                                            Toast.makeText(holder.itemView.getContext(),
//                                                                    "Failed to invite replacements", Toast.LENGTH_SHORT).show();
//                                                        }
//                                            });
//                                        }
//                                    }

//                                    @Override
//                                    public void onFailure(Exception e) {
//                                        Toast.makeText(holder.itemView.getContext(),
//                                                "Failed to retrieve replacements", Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                    });

            holder.emptyText.setVisibility(entrants.isEmpty() ? View.VISIBLE : View.GONE);
            //holder.emptyText.setVisibility(names.isEmpty() ? View.VISIBLE : View.GONE);


            // Show the correct button section per tab
            holder.layoutButtonsInvited.setVisibility(  position == TAB_INVITED   ? View.VISIBLE : View.GONE);
            holder.layoutButtonsEnrolled.setVisibility( position == TAB_ENROLLED  ? View.VISIBLE : View.GONE);
            holder.layoutButtonsCancelled.setVisibility(position == TAB_CANCELLED ? View.VISIBLE : View.GONE);
            holder.layoutButtonsWaitlist.setVisibility( position == TAB_WAITLIST  ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() { return TAB_TITLES.length; }

        class PageVH extends RecyclerView.ViewHolder {
            final TextView emptyText;
            final RecyclerView recycler;
            final View layoutButtonsInvited;
            final View layoutButtonsEnrolled;
            final View layoutButtonsCancelled;
            final View layoutButtonsWaitlist;

            PageVH(@NonNull View itemView) {
                super(itemView);
                emptyText              = itemView.findViewById(R.id.text_empty);
                recycler               = itemView.findViewById(R.id.recycler_tab);
                layoutButtonsInvited   = itemView.findViewById(R.id.layout_buttons_invited);
                layoutButtonsEnrolled  = itemView.findViewById(R.id.layout_buttons_enrolled);
                layoutButtonsCancelled = itemView.findViewById(R.id.layout_buttons_cancelled);
                layoutButtonsWaitlist  = itemView.findViewById(R.id.layout_buttons_waitlist);
            }
        }
    }

private static class NamesAdapter extends RecyclerView.Adapter<NamesAdapter.VH> {

    interface OnItemClickListener {
        void onItemClick(Entrant entrant, int position);
    }

    private final List<Entrant> entrants;
    private final Map<String, String> names;
    private final OnItemClickListener listener;
    private int selectedPosition = -1;

    NamesAdapter(List<Entrant> entrants,
                 Map<String, String> names,
                 OnItemClickListener listener) {
        this.entrants = entrants;
        this.names = names;
        this.listener = listener;
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
        Entrant entrant = entrants.get(position);

        String name = names != null
                ? names.getOrDefault(entrant.getDeviceId(), entrant.getDeviceId())
                : entrant.getDeviceId();

        holder.nameView.setText(name);
        holder.nameView.setVisibility(View.VISIBLE);
        holder.headerView.setVisibility(View.GONE);

        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(
                    holder.itemView.getContext().getColor(R.color.color_selected_item)
            );
        } else {
            holder.itemView.setBackgroundColor(
                    holder.itemView.getContext().getColor(android.R.color.transparent)
            );
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = position;

            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onItemClick(entrant, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entrants.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nameView, headerView;

        VH(View v) {
            super(v);
            nameView   = v.findViewById(R.id.text_device_id);
            headerView = v.findViewById(R.id.text_section_header);
        }
    }
}
}



