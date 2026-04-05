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
import com.example.eventlotteryapp.models.Notification;
import com.example.eventlotteryapp.repository.NotificationRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Displays all notifications for the current user.
 * Tapping a notification navigates to the event details so the user can accept or decline.
 *
 * User Stories Implemented:
 * US 01.04.01 As an entrant I want to receive notification when I am chosen (win notification).
 * US 01.04.02 As an entrant I want to receive notification when I am not chosen (loss notification).
 * US 01.05.01 As an entrant I want another chance when a selected user declines (replacement notification).
 * US 01.05.02 As an entrant I want to accept the invitation (tap notification → event details).
 * US 01.05.06 As an entrant, I want to receive a notification that I've been invited to join the waiting list for a private event.
 * US 01.09.01 As an entrant, I want to receive a notification if I have been invited to be a co-organizer for an event.
 * @author William
 */
public class NotificationsFragment extends Fragment {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    private String deviceId;
    private NotificationRepository notificationRepository;
    private final List<Notification> notifications = new ArrayList<>();
    private NotificationAdapter adapter;

    public NotificationsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        if (getArguments() != null) {
            deviceId = getArguments().getString("deviceId");
        }

        if (notificationRepository == null) {
            notificationRepository = new NotificationRepository();
        }

        TextView emptyText = view.findViewById(R.id.text_notifications_empty);
        RecyclerView recycler = view.findViewById(R.id.recycler_notifications);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationAdapter();
        recycler.setAdapter(adapter);

        loadNotifications(emptyText);

        return view;
    }

    private void loadNotifications(TextView emptyText) {
        notificationRepository.getNotificationsForUser(deviceId,
                new NotificationRepository.FirestoreCallback<List<Notification>>() {
                    @Override
                    public void onSuccess(List<Notification> result) {
                        if (!isAdded()) return;
                        notifications.clear();
                        if (result != null) notifications.addAll(result);
                        notifications.sort((a, b) -> {
    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
    if (a.getCreatedAt() == null) return 1;
    if (b.getCreatedAt() == null) return -1;
    return b.getCreatedAt().compareTo(a.getCreatedAt());
});
                        adapter.notifyDataSetChanged();
                        emptyText.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Notification n = notifications.get(position);
            holder.titleView.setText(n.getEventTitle() != null ? n.getEventTitle() : "Event");
            holder.messageView.setText(n.getMessage());
            holder.timeView.setText(n.getCreatedAt() != null
                    ? DATE_FORMAT.format(n.getCreatedAt().toDate())
                    : "");
            holder.cardView.setBackgroundResource(n.isRead()
                    ? R.drawable.notification_card_read
                    : R.drawable.notification_card_unread);
            holder.unreadDot.setVisibility(n.isRead() ? View.INVISIBLE : View.VISIBLE);

            holder.itemView.setOnClickListener(v -> {
                // Mark as read
                if (!n.isRead() && n.getNotificationId() != null) {
                    notificationRepository.markAsRead(deviceId, n.getNotificationId(),
                            new NotificationRepository.FirestoreCallback<Void>() {
                                @Override public void onSuccess(Void unused) {
                                    n.setRead(true);
                                    notifications.sort(Comparator.comparingInt(x -> (x.isRead() ? 1 : 0)));
                                    notifyDataSetChanged();
                                }
                                @Override public void onFailure(Exception e) {}
                            });
                }

                // Navigate to event details if we have an eventId
                if (n.getEventId() != null && !n.getEventId().isEmpty()) {
                    EntrantEventDetailsFragment detailsFragment = new EntrantEventDetailsFragment();
                    Bundle args = new Bundle();
                    args.putString("eventId", n.getEventId());
                    args.putString("deviceId", deviceId);
                    detailsFragment.setArguments(args);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                    R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.fragment_container, detailsFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        @Override
        public int getItemCount() { return notifications.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView titleView, messageView, timeView;
            final View cardView, unreadDot;

            VH(@NonNull View itemView) {
                super(itemView);
                titleView   = itemView.findViewById(R.id.text_notification_title);
                messageView = itemView.findViewById(R.id.text_notification_message);
                timeView    = itemView.findViewById(R.id.text_notification_time);
                cardView    = itemView.findViewById(R.id.notification_card);
                unreadDot   = itemView.findViewById(R.id.view_unread_dot);
            }
        }
    }
}
