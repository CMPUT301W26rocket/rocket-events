package com.example.eventlotteryapp.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;

import java.util.List;

/**
 * RecyclerView adapter for displaying event poster images in the admin image browsing screen.
 * Each row shows one event poster and allows the administrator to open the
 * corresponding image detail screen.
 *
 * @author Mazen
 */
public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.AdminImageViewHolder> {

    /**
     * Listener interface for handling clicks on an image row.
     */
    public interface OnImageClickListener {

        /**
         * Handles selection of an event image from the admin image grid.
         *
         * @param event selected event
         */
        void onImageClick(Event event);
    }

    private final List<Event> events;
    private final OnImageClickListener listener;

    /**
     * Creates a new adapter for displaying admin image rows.
     *
     * @param events list of events whose poster images will be displayed
     * @param listener click listener for image selection
     */
    public AdminImageAdapter(List<Event> events, OnImageClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    /**
     * Inflates one image row view for the RecyclerView.
     *
     * @param parent parent view group
     * @param viewType RecyclerView view type
     * @return new admin image view holder
     */
    @NonNull
    @Override
    public AdminImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new AdminImageViewHolder(view);
    }

    /**
     * Binds one event poster image to a displayed RecyclerView row.
     * If the event has no poster URL, a placeholder image is shown instead.
     *
     * @param holder row view holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull AdminImageViewHolder holder, int position) {
        Event event = events.get(position);

        String posterUrl = event.getPosterUrl();
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(posterUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.imagePoster);
        } else {
            holder.imagePoster.setImageResource(R.drawable.ic_image_placeholder);
        }

        holder.itemView.setOnClickListener(v -> listener.onImageClick(event));
    }

    /**
     * Returns the total number of image items displayed by the adapter.
     *
     * @return image item count
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * View holder for one image row in the admin image grid.
     */
    static class AdminImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePoster;

        /**
         * Binds the views used in one admin image row.
         *
         * @param itemView inflated row view
         */
        public AdminImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePoster = itemView.findViewById(R.id.image_admin_poster);
        }
    }
}