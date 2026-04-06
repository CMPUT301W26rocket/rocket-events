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
 * RecyclerView adapter for displaying event poster thumbnails in a grid
 * on the admin image browser screen.
 *
 * @author Mazen
 */
public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.AdminImageViewHolder> {

    /** Callback interface for image tap events. */
    public interface OnImageClickListener {
        void onImageClick(Event event);
    }

    private final List<Event> events;
    private final OnImageClickListener listener;

    /**
     * Creates a new adapter.
     *
     * @param events   the list of events whose posters should be displayed
     * @param listener callback invoked when a poster thumbnail is tapped
     */
    public AdminImageAdapter(List<Event> events, OnImageClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new AdminImageViewHolder(view);
    }

    /**
     * Loads the event's poster URL into the thumbnail using Glide.
     * Falls back to a placeholder image if no poster URL is set.
     *
     * @param holder   the view holder to bind data into
     * @param position the position of the item in the list
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
     * Returns the total number of poster items in the list.
     *
     * @return the size of the events list
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for a single poster thumbnail in the image grid.
     */
    static class AdminImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePoster;

        /**
         * Binds the poster ImageView from the item layout.
         *
         * @param itemView the inflated item view
         */
        public AdminImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePoster = itemView.findViewById(R.id.image_admin_poster);
        }
    }
}