package com.example.eventlotteryapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.Event;

import java.util.List;

/**
 * RecyclerView adapter that displays a list of events alongside the entrant's status for each.
 * Used by {@link com.example.eventlotteryapp.ui.fragments.EventHistoryFragment} to show
 * an entrant's full event registration history.
 *
 * User Stories Implemented:
 * US 01.02.03 As an entrant, I want to have a history of events I have registered for, whether I was selected or not.
 * @author William
 */
public class EventHistoryAdapter extends RecyclerView.Adapter<EventHistoryAdapter.ViewHolder> {

    /**
     * Callback interface for item click events.
     */
    public interface OnItemClickListener {
        /**
         * Called when the user taps an event row.
         *
         * @param event the {@link Event} that was tapped
         */
        void onItemClick(Event event);
    }

    private final List<Event> events;
    private final List<String> statuses;
    private final OnItemClickListener listener;

    /**
     * Constructs a new adapter.
     *
     * @param events   the list of events to display
     * @param statuses entrant status strings parallel to {@code events}
     * @param listener callback invoked when an item is tapped
     */
    public EventHistoryAdapter(List<Event> events, List<String> statuses, OnItemClickListener listener) {
        this.events = events;
        this.statuses = statuses;
        this.listener = listener;
    }

    /**
     * Inflates {@code item_event} and wraps it in a {@link ViewHolder}.
     *
     * @param parent   the RecyclerView that this ViewHolder will be attached to
     * @param viewType unused — only one view type exists
     * @return a new {@link ViewHolder}
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds event data and entrant status to the given {@link ViewHolder}.
     * Loads the event poster via Glide if a URL is present, otherwise shows a placeholder.
     * Hides registration and waitlist status badges — they are not relevant in history view.
     *
     * @param holder   the ViewHolder to populate
     * @param position the position of the item in the data set
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        String status = statuses.get(position);

        holder.titleTextView.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");
        holder.subtitleTextView.setText(formatStatus(status));

        String posterUrl = event.getPosterUrl();
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(posterUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.posterImageView);
        } else {
            holder.posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        }

        // Hide status badges — not relevant in history view
        holder.itemView.findViewById(R.id.badge_registration).setVisibility(View.GONE);
        holder.itemView.findViewById(R.id.badge_waitlist).setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(event));
    }

    /** @return the number of events in this adapter */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Converts a raw Firestore status string into a human-readable label.
     *
     * @param status the entrant status string (e.g. {@link com.example.eventlotteryapp.models.Entrant#STATUS_ENROLLED})
     * @return a user-facing status label, or {@code "Unknown"} if {@code status} is null
     */
    private String formatStatus(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case Entrant.STATUS_WAITLIST:          return "Waitlisted";
            case Entrant.STATUS_INVITED:           return "Invited to Enroll";
            case Entrant.STATUS_ENROLLED:          return "Enrolled";
            case Entrant.STATUS_DECLINED:          return "Declined";
            case Entrant.STATUS_CANCELLED:         return "Cancelled";
            case Entrant.STATUS_NOT_SELECTED:      return "Not Selected";
            case Entrant.STATUS_WAITLIST_INVITED:  return "Invited to Join Waitlist";
            case Entrant.STATUS_DECLINED_WAITLIST: return "Waitlist Invite Declined";
            default:                               return status;
        }
    }

    /**
     * Holds references to the views within a single {@code item_event} row.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, subtitleTextView;
        ImageView posterImageView;

        /**
         * Binds view references from the inflated {@code item_event} layout.
         *
         * @param itemView the root view of the event row
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView   = itemView.findViewById(R.id.text_event_title);
            subtitleTextView = itemView.findViewById(R.id.text_event_subtitle);
            posterImageView  = itemView.findViewById(R.id.image_event_poster);
        }
    }
}
