package com.example.eventlotteryapp.ui.admin;

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

    private final List<Event> eventList;
    private final OnEventClickListener listener;

    public EventAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.textEventTitle.setText(
                event.getTitle() == null || event.getTitle().isEmpty() ? "No title" : event.getTitle()
        );
        holder.textEventDescription.setText(
                event.getDescription() == null || event.getDescription().isEmpty() ? "No description" : event.getDescription()
        );
        holder.textEventOrganizer.setText(
                event.getOrganizerId() == null || event.getOrganizerId().isEmpty()
                        ? "Organizer ID: none"
                        : "Organizer ID: " + event.getOrganizerId()
        );

        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView textEventTitle, textEventDescription, textEventOrganizer;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textEventTitle = itemView.findViewById(R.id.textEventTitle);
            textEventDescription = itemView.findViewById(R.id.textEventDescription);
            textEventOrganizer = itemView.findViewById(R.id.textEventOrganizer);
        }
    }
}