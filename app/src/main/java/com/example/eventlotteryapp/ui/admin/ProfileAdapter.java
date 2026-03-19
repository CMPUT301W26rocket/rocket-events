package com.example.eventlotteryapp.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.User;

import java.util.List;

/**
 * RecyclerView adapter for displaying user profiles in admin-managed lists.
 * Supports both general profile browsing and organizer browsing screens.
 * @author Mazen
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    /**
     * Click listener interface for profile row selection.
     */
    public interface OnProfileClickListener {
        /**
         * Handles a click on a displayed user profile.
         *
         * @param user selected user
         */
        void onProfileClick(User user);
    }

    private final List<User> userList;
    private final OnProfileClickListener listener;

    /**
     * Creates a new profile adapter for the supplied user list.
     *
     * @param userList list of users to display
     * @param listener click listener for profile selection
     */
    public ProfileAdapter(List<User> userList, OnProfileClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    /**
     * Inflates a new profile row view.
     *
     * @param parent parent view group
     * @param viewType RecyclerView view type
     * @return new profile view holder
     */
    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    /**
     * Binds the user data for one profile row.
     *
     * @param holder row view holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        User user = userList.get(position);

        holder.textProfileName.setText(
                user.getName() == null || user.getName().isEmpty() ? "No name" : user.getName()
        );
        holder.textProfileEmail.setText(
                user.getEmail() == null || user.getEmail().isEmpty() ? "No email" : user.getEmail()
        );
        holder.textProfilePhone.setText(
                user.getPhone() == null || user.getPhone().isEmpty() ? "No phone" : user.getPhone()
        );

        holder.itemView.setOnClickListener(v -> listener.onProfileClick(user));
    }

    /**
     * Returns the total number of displayed profiles.
     *
     * @return profile count
     */
    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * View holder for one profile row in the admin list.
     */
    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView textProfileName;
        TextView textProfileEmail;
        TextView textProfilePhone;

        /**
         * Binds row text views for one profile item.
         *
         * @param itemView inflated row view
         */
        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            textProfileName = itemView.findViewById(R.id.textProfileName);
            textProfileEmail = itemView.findViewById(R.id.textProfileEmail);
            textProfilePhone = itemView.findViewById(R.id.textProfilePhone);
        }
    }
}