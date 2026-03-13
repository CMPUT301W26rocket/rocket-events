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
import com.example.eventlotteryapp.models.Event;

import java.util.List;

/**
 * RecyclerView adapter that displays a list of {@link Event} objects.
 * Each item shows the event's poster image, title, and organizer name.
 * Poster images are loaded with Glide; a placeholder is shown when no URL is available.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    /**
     * Callback interface for item click events.
     */
    public interface OnEventClickListener {
        /**
         * Called when the user taps on an event item.
         *
         * @param event the {@link Event} that was clicked
         */
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

    /**
     * Creates an adapter for the given list of events.
     *
     * @param events   the list of events to display
     * @param listener callback invoked when an event item is tapped
     */
    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    /**
     * Inflates the {@code item_event} layout and wraps it in a new {@link EventViewHolder}.
     *
     * @param parent   the RecyclerView that this view holder will be attached to
     * @param viewType the view type of the new view (unused — only one type exists)
     * @return a new {@link EventViewHolder} holding the inflated item view
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds the event at the given position to the view holder.
     * Sets the title, organizer name, and loads the poster image via Glide.
     * Falls back to a placeholder if the poster URL is null or empty.
     *
     * @param holder   the view holder to bind data into
     * @param position the index of the event in the list
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.titleTextView.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");

        String subtitle = event.getOrganizerName() != null && !event.getOrganizerName().isEmpty()
                ? event.getOrganizerName()
                : "Unknown organizer";
        holder.subtitleTextView.setText(subtitle);

        // Load poster — shows placeholder if posterUrl is null or empty
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

        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    /**
     * Returns the total number of events in the list.
     *
     * @return number of events
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder that holds references to the views for a single event list item.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView subtitleTextView;
        ImageView posterImageView;

        /**
         * Binds view references from the given item view.
         *
         * @param itemView the root view of the inflated event list item
         */
        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView   = itemView.findViewById(R.id.text_event_title);
            subtitleTextView = itemView.findViewById(R.id.text_event_subtitle);
            posterImageView  = itemView.findViewById(R.id.image_event_poster);
        }
    }
}
