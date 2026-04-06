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
 * RecyclerView adapter for displaying events in the admin event browser.
 * Each row shows the event title, organizer name, and a short description.
 *
 * @author Mazen
 */
public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    /** Callback interface for event row tap events. */
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

    /**
     * Creates a new adapter.
     *
     * @param events   the list of events to display
     * @param listener callback invoked when an event row is tapped
     */
    public AdminEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(view);
    }

    /**
     * Binds event data to the given view holder. Falls back to placeholder strings
     * when title, organizer, or description are null or empty.
     *
     * @param holder   the view holder to bind data into
     * @param position the position of the item in the list
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
     * Returns the total number of events in the list.
     *
     * @return the size of the events list
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for a single event row in the admin event list.
     */
    static class AdminEventViewHolder extends RecyclerView.ViewHolder {
        TextView textEventTitle;
        TextView textEventOrganizer;
        TextView textEventDescription;
        ImageView imageArrow;

        /**
         * Binds all views from the event row item layout.
         *
         * @param itemView the inflated item view
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