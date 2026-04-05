package com.example.eventlotteryapp.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying notification log entries in the
 * admin notification logs screen.
 * Each row shows the event title, recipient device ID, message preview,
 * and metadata such as type and timestamp.
 *
 * @author Mazen
 */
public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.LogViewHolder> {
    /**
     * Listener interface for handling clicks on a notification log row.
     */
    public interface OnLogClickListener {
        /**
         * Handles selection of a notification log item.
         *
         * @param item selected log item
         */
        void onLogClick(AdminNotificationLogItem item);
    }

    private final List<AdminNotificationLogItem> logItems;
    private final OnLogClickListener listener;
    /**
     * Creates a new adapter for displaying notification log items.
     *
     * @param logItems list of log items to display
     * @param listener click listener for row selection
     */
    public AdminNotificationLogAdapter(List<AdminNotificationLogItem> logItems,
                                       OnLogClickListener listener) {
        this.logItems = logItems;
        this.listener = listener;
    }
    /**
     * Inflates one notification log row view.
     *
     * @param parent parent view group
     * @param viewType RecyclerView view type
     * @return new log view holder
     */
    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification, parent, false);
        return new LogViewHolder(view);
    }
    /**
     * Binds the notification log data for one displayed row.
     *
     * @param holder row view holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        AdminNotificationLogItem item = logItems.get(position);

        holder.textNotificationTitle.setText(
                item.getEventTitle() == null || item.getEventTitle().trim().isEmpty()
                        ? "Notification"
                        : item.getEventTitle()
        );

        String recipientId = item.getRecipientDeviceId() == null || item.getRecipientDeviceId().trim().isEmpty()
                ? "(unknown)" : item.getRecipientDeviceId();
        String recipientDisplay = item.getRecipientName() != null && !item.getRecipientName().trim().isEmpty()
                ? item.getRecipientName() + " (ID: " + recipientId + ")"
                : recipientId;
        holder.textNotificationRecipient.setText("Recipient: " + recipientDisplay);

        holder.textNotificationMessage.setText(
                item.getMessage() == null || item.getMessage().trim().isEmpty()
                        ? "No message"
                        : item.getMessage()
        );

        holder.textNotificationMeta.setText(
                "Type: " + safeText(item.getType(), "general") + " • " + formatTimestamp(item.getCreatedAt())
        );

        holder.itemView.setOnClickListener(v -> listener.onLogClick(item));
    }
    /**
     * Returns the number of notification log items displayed by the adapter.
     *
     * @return log item count
     */
    @Override
    public int getItemCount() {
        return logItems.size();
    }
    /**
     * Returns the supplied text unless it is null or blank, in which case a fallback
     * value is returned instead.
     *
     * @param value value to check
     * @param fallback fallback value
     * @return safe display text
     */

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
    /**
     * Formats a Firebase timestamp for display in the log list.
     *
     * @param timestamp timestamp to format
     * @return formatted time string, or a fallback label if unavailable
     */
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null || timestamp.toDate() == null) {
            return "Unknown time";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return formatter.format(timestamp.toDate());
    }
    /**
     * View holder for one notification log row.
     */
    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView textNotificationTitle;
        TextView textNotificationRecipient;
        TextView textNotificationMessage;
        TextView textNotificationMeta;
        /**
         * Binds the row views used for one log item.
         *
         * @param itemView inflated row view
         */
        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            textNotificationTitle = itemView.findViewById(R.id.textNotificationTitle);
            textNotificationRecipient = itemView.findViewById(R.id.textNotificationRecipient);
            textNotificationMessage = itemView.findViewById(R.id.textNotificationMessage);
            textNotificationMeta = itemView.findViewById(R.id.textNotificationMeta);
        }
    }
}