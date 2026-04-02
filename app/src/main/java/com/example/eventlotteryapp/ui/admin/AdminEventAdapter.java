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

public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

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

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class AdminEventViewHolder extends RecyclerView.ViewHolder {
        TextView textEventTitle;
        TextView textEventOrganizer;
        TextView textEventDescription;
        ImageView imageArrow;

        public AdminEventViewHolder(@NonNull View itemView) {
            super(itemView);
            textEventTitle = itemView.findViewById(R.id.text_admin_event_title);
            textEventOrganizer = itemView.findViewById(R.id.text_admin_event_organizer);
            textEventDescription = itemView.findViewById(R.id.text_admin_event_description);
            imageArrow = itemView.findViewById(R.id.image_admin_event_arrow);
        }
    }
}