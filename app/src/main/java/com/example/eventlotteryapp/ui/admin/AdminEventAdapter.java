package com.example.eventlotteryapp.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;

import java.util.List;

/**
 * RecyclerView adapter for displaying events in the admin event browsing screen.
 * Each row shows the event title, organizer name, and description, and supports
 * opening the selected event in the admin detail screen.
 *
 * @author Mazen
 */
public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    /**
     * Listener interface for handling clicks on an event row.
     */
    public interface OnEventClickListener {

        /**
         * Handles selection of an event from the admin event list.
         *
         * @param event selected event
         */
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

    /**
     * Creates a new adapter for displaying admin event rows.
     *
     * @param events list of events to display
     * @param listener click listener for event selection
     */
    public AdminEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    /**
     * Inflates one event row view for the RecyclerView.
     *
     * @param parent parent view group
     * @param viewType RecyclerView view type
     * @return new admin event view holder
     */
    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(view);
    }

    /**
     * Binds one event's data to a displayed RecyclerView row.
     * Fallback text is used when title, organizer name, or description is missing.
     *
     * @param holder row view holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        Event event = events.get(position);

        String title = event.getTitle() != null && !event.getTitle().trim().isEmpty()
                ? event.getTitle()
                : "Untitled Event";

        String organizer = event.getOrganizerName() != null && !event.getOrganizerName().trim().isEmpty()
                ? event.getOrganizerName()
                : "Unknown organizer";

        String description = event.getDescription() != null && !event.getDescription().trim().isEmpty()
                ? event.getDescription()
                : "No description";

        holder.textEventTitle.setText(title);
        holder.textEventOrganizer.setText(organizer);
        holder.textEventDescription.setText(description);

        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    /**
     * Returns the total number of events displayed by the adapter.
     *
     * @return event count
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * View holder for one event row in the admin event list.
     */
    static class AdminEventViewHolder extends RecyclerView.ViewHolder {
        TextView textEventTitle;
        TextView textEventOrganizer;
        TextView textEventDescription;
        ImageView imageArrow;

        /**
         * Binds the views used in one admin event row.
         *
         * @param itemView inflated row view
         */
        public AdminEventViewHolder(@NonNull View itemView) {
            super(itemView);
            textEventTitle = itemView.findViewById(R.id.text_admin_event_title);
            textEventOrganizer = itemView.findViewById(R.id.text_admin_event_organizer);
            textEventDescription = itemView.findViewById(R.id.text_admin_event_description);
            imageArrow = itemView.findViewById(R.id.image_admin_event_arrow);
        }
    }
}