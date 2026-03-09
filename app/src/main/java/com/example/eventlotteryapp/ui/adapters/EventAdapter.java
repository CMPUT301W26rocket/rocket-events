package com.example.eventlotteryapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.titleTextView.setText(event.getTitle() != null ? event.getTitle() : "Untitled Event");

        String subtitle = event.getOrganizerId() != null && !event.getOrganizerId().isEmpty()
                ? event.getOrganizerId()
                : "Organizer";
        holder.subtitleTextView.setText(subtitle);

        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView subtitleTextView;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_event_title);
            subtitleTextView = itemView.findViewById(R.id.text_event_subtitle);
        }
    }
}