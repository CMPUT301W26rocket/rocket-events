package com.example.eventlotteryapp.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
 * US 02.06.04 As an organizer I want to cancel entrants that did not sign up for the event.
 * @author Daniel
 * @author Leyla
 * @author Santiago
 * @author William
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
    private int lotteryCapacity = 0;
    private Entrant selectedEntrant = null;

    private EntrantRepository entrantRepository;
    private UserRepository userRepository;
    private NotificationRepository notificationRepository;

    private final List<List<Entrant>> tabData = new ArrayList<>();

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
            eventId               = getArguments().getString("eventId");
            eventTitle            = getArguments().getString("eventTitle", "Event");
            lotteryCompleted      = getArguments().getBoolean("lotteryCompleted", false);
            registrationCloseDate = getArguments().getLong("registrationCloseDate", -1);
            lotteryCapacity       = getArguments().getInt("lotteryCapacity", 0);
        }

        // Initialise empty lists for each tab
        for (int i = 0; i < TAB_TITLES.length; i++) tabData.add(new ArrayList<>());

        if (entrantRepository == null) entrantRepository = new EntrantRepository();
        if (userRepository == null) userRepository = new UserRepository();
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
                case Entrant.STATUS_DECLINED:
                    tabData.get(TAB_CANCELLED).add(e);
                    break;
                case Entrant.STATUS_WAITLIST:
                case Entrant.STATUS_NOT_SELECTED:
                    tabData.get(TAB_WAITLIST).add(e);
                    break;

                // STATUS_DECLINED shown in Cancelled tab, annotated as "Declined"
            }
        }
        pagerAdapter.setNamesMap(names);
        pagerAdapter.notifyDataSetChanged();
    }

    /**
     * Sends a notification of {@code type} to every entrant in {@code targets}.
     * US 01.04.01 US 01.04.02 US 02.07.01 US 02.07.02 US 02.07.03
     */
    private void sendNotificationsToAll(List<Entrant> targets, String type,
                                        String message, android.content.Context ctx) {
        if (targets == null || targets.isEmpty()) {
            Toast.makeText(ctx, "No entrants in this group", Toast.LENGTH_SHORT).show();
            return;
        }
        for (Entrant e : targets) {
            Notification n = new Notification(eventId, eventTitle, type, message);
            notificationRepository.addNotification(e.getDeviceId(), n,
                    new NotificationRepository.FirestoreCallback<String>() {
                        @Override public void onSuccess(String id) {}
                        @Override public void onFailure(Exception ex) {
                            if (isAdded()) Toast.makeText(ctx,
                                    "Failed to notify: " + e.getDeviceId(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        Toast.makeText(ctx,
                "Notifications sent to " + targets.size() + " entrant(s)",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a dialog for the organizer to type a custom message,
     * then sends it to every entrant in {@code targets}.
     */
    private void showCustomNotificationDialog(List<Entrant> targets, String type, Context ctx) {
        if (targets.isEmpty()) {
            Toast.makeText(ctx, "No entrants in this category.", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(ctx);
        input.setHint("Enter your message...");
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(ctx)
                .setTitle("Send Notification")
                .setMessage("Your message will be sent to " + targets.size() + " entrant(s).")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String message = input.getText().toString().trim();
                    if (message.isEmpty()) {
                        Toast.makeText(ctx, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendNotificationsToAll(targets, type, message, ctx);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Fills all open spots by drawing replacements from the waitlist in one go.
     * Open spots = lotteryCapacity - invited - enrolled.
     * Draws up to that many entrants randomly; draws fewer if the waitlist is smaller.
     * US 01.05.01 US 02.05.03
     */
    private void drawReplacement(android.content.Context ctx) {
        int currentInvited = tabData.get(TAB_INVITED).size();
        int currentEnrolled = tabData.get(TAB_ENROLLED).size();
        int spotsToFill = lotteryCapacity - currentInvited - currentEnrolled;

        if (spotsToFill <= 0) {
            Toast.makeText(ctx, "No open spots left to draw a replacement.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Entrant> eligible = new ArrayList<>();
        for (Entrant e : tabData.get(TAB_WAITLIST)) {
            if (Entrant.STATUS_WAITLIST.equals(e.getStatus())
                    || Entrant.STATUS_NOT_SELECTED.equals(e.getStatus())) {
                eligible.add(e);
            }
        }

        if (eligible.isEmpty()) {
            Toast.makeText(ctx, "No one on the waitlist to draw from.", Toast.LENGTH_SHORT).show();
            return;
        }

        Collections.shuffle(eligible);
        List<Entrant> toDraw = eligible.subList(0, Math.min(spotsToFill, eligible.size()));
        int drawCount = toDraw.size();

        AtomicInteger remaining = new AtomicInteger(drawCount);
        AtomicInteger failures = new AtomicInteger(0);

        for (Entrant chosen : toDraw) {
            entrantRepository.updateStatus(eventId, chosen.getDeviceId(), Entrant.STATUS_INVITED,
                    new EntrantRepository.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Notification n = new Notification(eventId, eventTitle,
                                    Notification.TYPE_REPLACEMENT,
                                    "You have been given a replacement invitation for " + eventTitle + "!");
                            notificationRepository.addNotification(chosen.getDeviceId(), n,
                                    new NotificationRepository.FirestoreCallback<String>() {
                                        @Override public void onSuccess(String id) {}
                                        @Override public void onFailure(Exception ex) {}
                                    });
                            checkDone();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            failures.incrementAndGet();
                            checkDone();
                        }

                        private void checkDone() {
                            if (remaining.decrementAndGet() == 0 && isAdded()) {
                                int succeeded = drawCount - failures.get();
                                if (succeeded > 0) {
                                    Toast.makeText(ctx,
                                            "Drew " + succeeded + " replacement(s) from the waitlist.",
                                            Toast.LENGTH_SHORT).show();
                                    loadEntrants((EntrantPagerAdapter) ((ViewPager2) requireView()
                                            .findViewById(R.id.view_pager)).getAdapter());
                                } else {
                                    Toast.makeText(ctx, "Failed to draw replacements.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        }
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
            final int tabIndex = holder.getBindingAdapterPosition();
            if (tabIndex == RecyclerView.NO_ID) return;

            List<Entrant> entrants = tabData.get(tabIndex);

            holder.recycler.setLayoutManager(new LinearLayoutManager(holder.recycler.getContext()));

            NamesAdapter.OnItemClickListener clickListener = null;
            if (tabIndex == TAB_INVITED) clickListener = (entrant, pos) -> {
                selectedEntrant = entrant;

                String displayName = names != null
                        ? names.getOrDefault(entrant.getDeviceId(), entrant.getDeviceId())
                        : entrant.getDeviceId();
                Toast.makeText(holder.recycler.getContext(),
                        "Selected: " + displayName,
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
                                                    "Cancelled: " + displayName,
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
            };

            holder.recycler.setAdapter(new NamesAdapter(entrants, names, clickListener));

            // Send Win Notification to all invited entrants — US 01.04.01
            holder.btnSendWin.setOnClickListener(v ->
                    sendNotificationsToAll(tabData.get(TAB_INVITED), Notification.TYPE_WON,
                            "You have been selected for " + eventTitle + "! Please accept or decline.",
                            holder.itemView.getContext()));

            // Custom notification to invited entrants — US 02.07.02
            holder.btnSendNotificationInvited.setOnClickListener(v ->
                    showCustomNotificationDialog(new ArrayList<>(tabData.get(TAB_INVITED)),
                            Notification.TYPE_GENERAL, holder.itemView.getContext()));

            // Draw replacement — US 01.05.01 US 02.05.03; only enabled when lottery done + reg closed
            boolean regClosed = registrationCloseDate > 0
                    && new java.util.Date().after(new java.util.Date(registrationCloseDate));
            boolean canDraw = lotteryCompleted && regClosed;
            holder.btnDrawReplacement.setEnabled(canDraw);
            holder.btnDrawReplacement.setAlpha(canDraw ? 1f : 0.4f);
            if (!lotteryCompleted) {
                holder.btnDrawReplacement.setText("Draw Replacement (Lottery Not Run)");
            } else if (!regClosed) {
                holder.btnDrawReplacement.setText("Draw Replacement (Registration Open)");
            } else {
                holder.btnDrawReplacement.setText("Draw Replacement");
            }
            holder.btnDrawReplacement.setOnClickListener(v ->
                    drawReplacement(holder.itemView.getContext()));

            // Custom notification to cancelled entrants — US 02.07.03
            holder.btnSendNotificationCancelled.setOnClickListener(v ->
                    showCustomNotificationDialog(new ArrayList<>(tabData.get(TAB_CANCELLED)),
                            Notification.TYPE_GENERAL, holder.itemView.getContext()));

            // Custom notification to all waitlist entrants — US 02.07.01
            holder.btnSendCustomNotificationWaitlist.setOnClickListener(v ->
                    showCustomNotificationDialog(new ArrayList<>(tabData.get(TAB_WAITLIST)),
                            Notification.TYPE_GENERAL, holder.itemView.getContext()));

            // Send Loss Notification to not-selected entrants only — US 01.04.02
            holder.btnSendNotificationWaitlist.setOnClickListener(v -> {
                List<Entrant> notSelected = new ArrayList<>();
                for (Entrant e : tabData.get(TAB_WAITLIST)) {
                    if (Entrant.STATUS_NOT_SELECTED.equals(e.getStatus())) notSelected.add(e);
                }
                sendNotificationsToAll(notSelected, Notification.TYPE_LOST,
                        "Unfortunately you were not selected for " + eventTitle + ".",
                        holder.itemView.getContext());
            });

            holder.emptyText.setVisibility(entrants.isEmpty() ? View.VISIBLE : View.GONE);

            // Show the correct button section per tab
            holder.layoutButtonsInvited.setVisibility(  tabIndex == TAB_INVITED   ? View.VISIBLE : View.GONE);
            holder.layoutButtonsEnrolled.setVisibility( tabIndex == TAB_ENROLLED  ? View.VISIBLE : View.GONE);
            holder.layoutButtonsCancelled.setVisibility(tabIndex == TAB_CANCELLED ? View.VISIBLE : View.GONE);
            holder.layoutButtonsWaitlist.setVisibility( tabIndex == TAB_WAITLIST  ? View.VISIBLE : View.GONE);
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
            final android.widget.Button btnSendWin;
            final android.widget.Button btnSendNotificationInvited;
            final android.widget.Button btnDrawReplacement;
            final android.widget.Button btnSendNotificationCancelled;
            final android.widget.Button btnSendCustomNotificationWaitlist;
            final android.widget.Button btnSendNotificationWaitlist;

            PageVH(@NonNull View itemView) {
                super(itemView);
                emptyText                    = itemView.findViewById(R.id.text_empty);
                recycler                     = itemView.findViewById(R.id.recycler_tab);
                layoutButtonsInvited         = itemView.findViewById(R.id.layout_buttons_invited);
                layoutButtonsEnrolled        = itemView.findViewById(R.id.layout_buttons_enrolled);
                layoutButtonsCancelled       = itemView.findViewById(R.id.layout_buttons_cancelled);
                layoutButtonsWaitlist        = itemView.findViewById(R.id.layout_buttons_waitlist);
                btnSendWin                   = itemView.findViewById(R.id.button_send_win_notification);
                btnSendNotificationInvited   = itemView.findViewById(R.id.button_send_notification_invited);
                btnDrawReplacement           = itemView.findViewById(R.id.button_draw_replacement);
                btnSendNotificationCancelled        = itemView.findViewById(R.id.button_send_notification_cancelled);
                btnSendCustomNotificationWaitlist   = itemView.findViewById(R.id.button_send_custom_notification_waitlist);
                btnSendNotificationWaitlist         = itemView.findViewById(R.id.button_send_notification_waitlist);
            }
        }
    }

    // --- Names list adapter ---

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

        String status = entrant.getStatus();
        if (Entrant.STATUS_DECLINED.equals(status)) {
            name = name + " (Declined)";
        } else if (Entrant.STATUS_CANCELLED.equals(status)) {
            name = name + " (Cancelled)";
        }

        holder.nameView.setText(name);
        holder.nameView.setVisibility(View.VISIBLE);
        holder.headerView.setVisibility(View.GONE);

        if (listener != null && position == selectedPosition) {
            holder.itemView.setBackgroundColor(
                    holder.itemView.getContext().getColor(R.color.color_selected_item));
        } else {
            holder.itemView.setBackgroundColor(
                    holder.itemView.getContext().getColor(android.R.color.transparent));
        }

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos == RecyclerView.NO_ID) return;
                int oldPos = selectedPosition;
                selectedPosition = adapterPos;
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
                listener.onItemClick(entrant, adapterPos);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
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
