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

public class EventHistoryAdapter extends RecyclerView.Adapter<EventHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Event event);
    }

    private final List<Event> events;
    private final List<String> statuses;
    private final OnItemClickListener listener;

    public EventHistoryAdapter(List<Event> events, List<String> statuses, OnItemClickListener listener) {
        this.events = events;
        this.statuses = statuses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

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

    @Override
    public int getItemCount() {
        return events.size();
    }

    private String formatStatus(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case Entrant.STATUS_WAITLIST:      return "Waitlisted";
            case Entrant.STATUS_INVITED:       return "Invited — Awaiting Response";
            case Entrant.STATUS_ENROLLED:      return "Enrolled";
            case Entrant.STATUS_DECLINED:      return "Declined";
            case Entrant.STATUS_CANCELLED:     return "Cancelled";
            case Entrant.STATUS_NOT_SELECTED:  return "Not Selected";
            default:                           return status;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, subtitleTextView;
        ImageView posterImageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView   = itemView.findViewById(R.id.text_event_title);
            subtitleTextView = itemView.findViewById(R.id.text_event_subtitle);
            posterImageView  = itemView.findViewById(R.id.image_event_poster);
        }
    }
}
