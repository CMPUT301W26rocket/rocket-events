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
import com.example.eventlotteryapp.models.Notification;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.example.eventlotteryapp.repository.NotificationRepository;
import com.example.eventlotteryapp.repository.UserRepository;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays all entrants for an event in a tabbed slider view grouped by status.
 * Tabs: Invited, Enrolled, Cancelled, Waitlist (includes Not Selected).
 * Declined entrants are not shown. The Invited tab includes organizer action buttons.
 *
 * User Stories Implemented:
 * US 01.04.01 As an entrant I want to receive notification when I am chosen (win notification).
 * US 01.04.02 As an entrant I want to receive notification when I am not chosen (loss notification).
 * US 01.05.01 As an entrant I want another chance to be chosen if a selected user declines.
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
    private String eventTitle;
    private boolean lotteryCompleted;
    private long registrationCloseDate = -1;
    private EntrantRepository entrantRepository;
    private UserRepository userRepository;
    private NotificationRepository notificationRepository;

    /** Display names per tab (shown in list). */
    private final List<List<String>> tabData = new ArrayList<>();
    /** Entrant objects per tab (used for notification sending and replacement drawing). */
    private final List<List<Entrant>> tabEntrants = new ArrayList<>();

    public EntrantListFragment() {}

    /** Injects a mock {@link EntrantRepository} for testing. */
    public void setEntrantRepository(EntrantRepository repo) { this.entrantRepository = repo; }

    /** Injects a mock {@link UserRepository} for testing. */
    public void setUserRepository(UserRepository repo) { this.userRepository = repo; }

    /** Injects a mock {@link NotificationRepository} for testing. */
    public void setNotificationRepository(NotificationRepository repo) { this.notificationRepository = repo; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_entrant_list, container, false);

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (getArguments() != null) {
            eventId              = getArguments().getString("eventId");
            eventTitle           = getArguments().getString("eventTitle", "Event");
            lotteryCompleted     = getArguments().getBoolean("lotteryCompleted", false);
            registrationCloseDate = getArguments().getLong("registrationCloseDate", -1);
        }

        for (int i = 0; i < TAB_TITLES.length; i++) {
            tabData.add(new ArrayList<>());
            tabEntrants.add(new ArrayList<>());
        }

        if (entrantRepository == null)    entrantRepository    = new EntrantRepository();
        if (userRepository == null)       userRepository       = new UserRepository();
        if (notificationRepository == null) notificationRepository = new NotificationRepository();

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
        for (List<String> list : tabData) list.clear();
        for (List<Entrant> list : tabEntrants) list.clear();

        for (Entrant e : entrants) {
            String display = names.getOrDefault(e.getDeviceId(), e.getDeviceId());
            switch (e.getStatus()) {
                case Entrant.STATUS_INVITED:
                    tabData.get(TAB_INVITED).add(display);
                    tabEntrants.get(TAB_INVITED).add(e);
                    break;
                case Entrant.STATUS_ENROLLED:
                    tabData.get(TAB_ENROLLED).add(display);
                    tabEntrants.get(TAB_ENROLLED).add(e);
                    break;
                case Entrant.STATUS_CANCELLED:
                    tabData.get(TAB_CANCELLED).add(display);
                    tabEntrants.get(TAB_CANCELLED).add(e);
                    break;
                case Entrant.STATUS_WAITLIST:
                case Entrant.STATUS_NOT_SELECTED:
                    tabData.get(TAB_WAITLIST).add(display);
                    tabEntrants.get(TAB_WAITLIST).add(e);
                    break;
                // STATUS_DECLINED intentionally excluded
            }
        }
        pagerAdapter.notifyDataSetChanged();
    }

    // --- Notification helpers ---

    /**
     * Sends a notification to every entrant in the given list.
     *
     * @param entrants  the recipients
     * @param type      one of {@link Notification} TYPE_* constants
     * @param message   the notification body
     */
    private void sendNotificationsToAll(List<Entrant> entrants, String type, String message) {
        if (entrants == null || entrants.isEmpty()) {
            Toast.makeText(getContext(), "No entrants to notify", Toast.LENGTH_SHORT).show();
            return;
        }
        for (Entrant entrant : entrants) {
            Notification notification = new Notification(eventId, eventTitle, type, message);
            notificationRepository.addNotification(
                    entrant.getDeviceId(), notification,
                    new NotificationRepository.FirestoreCallback<String>() {
                        @Override public void onSuccess(String id) {}
                        @Override public void onFailure(Exception e) {}
                    });
        }
        Toast.makeText(getContext(),
                entrants.size() + " notification(s) sent", Toast.LENGTH_SHORT).show();
    }

    /**
     * Draws one random replacement from the waitlist tab (STATUS_NOT_SELECTED or STATUS_WAITLIST),
     * updates their status to STATUS_INVITED, and sends a replacement notification.
     * Implements US 01.05.01.
     *
     * @param pagerAdapter the adapter to refresh after the draw
     */
    private void drawReplacement(EntrantPagerAdapter pagerAdapter) {
        List<Entrant> pool = tabEntrants.get(TAB_WAITLIST);
        if (pool == null || pool.isEmpty()) {
            Toast.makeText(getContext(), "No entrants available for replacement", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make a mutable copy and shuffle to pick randomly
        List<Entrant> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        Entrant replacement = shuffled.get(0);

        entrantRepository.updateStatus(eventId, replacement.getDeviceId(),
                Entrant.STATUS_INVITED, new EntrantRepository.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (!isAdded()) return;

                        Notification notification = new Notification(
                                eventId, eventTitle,
                                Notification.TYPE_REPLACEMENT,
                                "You have been given another chance! A spot opened up for \""
                                        + eventTitle + "\". Open the app to accept or decline.");
                        notificationRepository.addNotification(
                                replacement.getDeviceId(), notification,
                                new NotificationRepository.FirestoreCallback<String>() {
                                    @Override public void onSuccess(String id) {}
                                    @Override public void onFailure(Exception e) {}
                                });

                        Toast.makeText(getContext(), "Replacement drawn and notified", Toast.LENGTH_SHORT).show();
                        // Reload the list to reflect the status change
                        for (List<String> list : tabData) list.clear();
                        for (List<Entrant> list : tabEntrants) list.clear();
                        loadEntrants(pagerAdapter);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to draw replacement", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // --- ViewPager2 adapter ---

    private class EntrantPagerAdapter extends RecyclerView.Adapter<EntrantPagerAdapter.PageVH> {

        @NonNull
        @Override
        public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.page_entrant_tab, parent, false);
            return new PageVH(page);
        }

        @Override
        public void onBindViewHolder(@NonNull PageVH holder, int position) {
            List<String> names = tabData.get(position);

            holder.recycler.setLayoutManager(new LinearLayoutManager(holder.recycler.getContext()));
            holder.recycler.setAdapter(new NamesAdapter(names));
            holder.emptyText.setVisibility(names.isEmpty() ? View.VISIBLE : View.GONE);

            holder.layoutButtonsInvited.setVisibility(  position == TAB_INVITED   ? View.VISIBLE : View.GONE);
            holder.layoutButtonsEnrolled.setVisibility( position == TAB_ENROLLED  ? View.VISIBLE : View.GONE);
            holder.layoutButtonsCancelled.setVisibility(position == TAB_CANCELLED ? View.VISIBLE : View.GONE);
            holder.layoutButtonsWaitlist.setVisibility( position == TAB_WAITLIST  ? View.VISIBLE : View.GONE);

            if (position == TAB_INVITED) {
                boolean hasInvited  = !tabEntrants.get(TAB_INVITED).isEmpty();
                boolean hasWaitlist = !tabEntrants.get(TAB_WAITLIST).isEmpty();
                boolean registrationClosed = registrationCloseDate > 0
                        && System.currentTimeMillis() > registrationCloseDate;

                // US 02.05.01 US 01.04.01: notify chosen entrants they won
                holder.btnSendWin.setEnabled(hasInvited);
                holder.btnSendWin.setText(hasInvited
                        ? "Send Win Notification (" + tabEntrants.get(TAB_INVITED).size() + ")"
                        : "No Invited Entrants");

                // US 02.07.02: send general notification to all selected/invited
                holder.btnSendNotificationInvited.setEnabled(hasInvited);
                holder.btnSendNotificationInvited.setText(hasInvited
                        ? "Send Notification (" + tabEntrants.get(TAB_INVITED).size() + ")"
                        : "No Invited Entrants");

                // US 02.05.03 US 01.05.01: draw replacement from waitlist
                boolean canDraw = hasWaitlist && lotteryCompleted && registrationClosed;
                holder.btnDrawReplacement.setEnabled(canDraw);
                if (!lotteryCompleted) {
                    holder.btnDrawReplacement.setText("Lottery Not Run Yet");
                } else if (!registrationClosed) {
                    holder.btnDrawReplacement.setText("Registration Still Open");
                } else if (!hasWaitlist) {
                    holder.btnDrawReplacement.setText("No Waitlist Entrants");
                } else {
                    holder.btnDrawReplacement.setText("Draw Replacement (" + tabEntrants.get(TAB_WAITLIST).size() + " available)");
                }

                holder.btnSendWin.setOnClickListener(v ->
                        sendNotificationsToAll(
                                tabEntrants.get(TAB_INVITED),
                                Notification.TYPE_WON,
                                "Congratulations! You have been selected for \"" + eventTitle
                                        + "\". Open the app to accept or decline your invitation."));

                holder.btnSendNotificationInvited.setOnClickListener(v ->
                        sendNotificationsToAll(
                                tabEntrants.get(TAB_INVITED),
                                Notification.TYPE_GENERAL,
                                "You have a pending invitation for \"" + eventTitle + "\"."));

                holder.btnDrawReplacement.setOnClickListener(v -> drawReplacement(this));
            }

            if (position == TAB_CANCELLED) {
                // US 02.07.03: send notification to all cancelled entrants
                boolean hasCancelled = !tabEntrants.get(TAB_CANCELLED).isEmpty();
                holder.btnSendNotificationCancelled.setEnabled(hasCancelled);
                holder.btnSendNotificationCancelled.setText(hasCancelled
                        ? "Send Notification (" + tabEntrants.get(TAB_CANCELLED).size() + ")"
                        : "No Cancelled Entrants");
                holder.btnSendNotificationCancelled.setOnClickListener(v ->
                        sendNotificationsToAll(
                                tabEntrants.get(TAB_CANCELLED),
                                Notification.TYPE_GENERAL,
                                "An update is available for the event \"" + eventTitle + "\"."));
            }

            if (position == TAB_WAITLIST) {
                long notSelectedCount = 0;
                for (Entrant e : tabEntrants.get(TAB_WAITLIST)) {
                    if (Entrant.STATUS_NOT_SELECTED.equals(e.getStatus())) notSelectedCount++;
                }
                boolean hasNotSelected = notSelectedCount > 0;

                // US 01.04.02 US 02.07.01: notify not-selected entrants they lost
                holder.btnSendNotificationWaitlist.setEnabled(hasNotSelected);
                holder.btnSendNotificationWaitlist.setText(hasNotSelected
                        ? "Send Loss Notification (" + notSelectedCount + ")"
                        : "No Not-Selected Entrants");

                holder.btnSendNotificationWaitlist.setOnClickListener(v -> {
                    List<Entrant> notSelected = new ArrayList<>();
                    for (Entrant e : tabEntrants.get(TAB_WAITLIST)) {
                        if (Entrant.STATUS_NOT_SELECTED.equals(e.getStatus())) {
                            notSelected.add(e);
                        }
                    }
                    sendNotificationsToAll(
                            notSelected,
                            Notification.TYPE_LOST,
                            "Unfortunately you were not selected for \"" + eventTitle
                                    + "\" this time. You may be drawn as a replacement if a spot opens up.");
                });
            }
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
            // Invited tab buttons
            final android.widget.Button btnSendWin;
            final android.widget.Button btnSendNotificationInvited;
            final android.widget.Button btnDrawReplacement;
            // Cancelled tab buttons
            final android.widget.Button btnSendNotificationCancelled;
            // Waitlist tab buttons
            final android.widget.Button btnSendNotificationWaitlist;

            PageVH(@NonNull View itemView) {
                super(itemView);
                emptyText              = itemView.findViewById(R.id.text_empty);
                recycler               = itemView.findViewById(R.id.recycler_tab);
                layoutButtonsInvited   = itemView.findViewById(R.id.layout_buttons_invited);
                layoutButtonsEnrolled  = itemView.findViewById(R.id.layout_buttons_enrolled);
                layoutButtonsCancelled = itemView.findViewById(R.id.layout_buttons_cancelled);
                layoutButtonsWaitlist  = itemView.findViewById(R.id.layout_buttons_waitlist);
                btnSendWin                   = itemView.findViewById(R.id.button_send_win_notification);
                btnSendNotificationInvited   = itemView.findViewById(R.id.button_send_notification_invited);
                btnDrawReplacement           = itemView.findViewById(R.id.button_draw_replacement);
                btnSendNotificationCancelled = itemView.findViewById(R.id.button_send_notification_cancelled);
                btnSendNotificationWaitlist  = itemView.findViewById(R.id.button_send_notification_waitlist);
            }
        }
    }

    // --- Simple names list adapter ---

    private static class NamesAdapter extends RecyclerView.Adapter<NamesAdapter.VH> {
        private final List<String> names;

        NamesAdapter(List<String> names) { this.names = names; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_entrant, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.nameView.setText(names.get(position));
            holder.nameView.setVisibility(View.VISIBLE);
            holder.headerView.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() { return names.size(); }

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
