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
 *
 * @author Mazen
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    /** Callback interface for profile row tap events. */
    public interface OnProfileClickListener {
        void onProfileClick(User user);
    }

    private final List<User> userList;
    private final OnProfileClickListener listener;

    /**
     * Creates a new adapter.
     *
     * @param userList the list of users to display
     * @param listener callback invoked when a profile row is tapped
     */
    public ProfileAdapter(List<User> userList, OnProfileClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    /**
     * Binds user profile data to the given view holder. Falls back to placeholder
     * strings when name, email, or phone are null or empty.
     *
     * @param holder   the view holder to bind data into
     * @param position the position of the item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        User user = userList.get(position);

        String name = user.getName() == null || user.getName().trim().isEmpty()
                ? "No name"
                : user.getName();

        String email = user.getEmail() == null || user.getEmail().trim().isEmpty()
                ? "No email"
                : user.getEmail();

        String phone = user.getPhone() == null || user.getPhone().trim().isEmpty()
                ? "No phone"
                : user.getPhone();

        holder.textProfileName.setText(name);
        holder.textProfileEmail.setText(email);
        holder.textProfilePhone.setText(phone);

        holder.itemView.setOnClickListener(v -> listener.onProfileClick(user));
    }

    /**
     * Returns the total number of profiles in the list.
     *
     * @return the size of the user list
     */
    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * ViewHolder for a single profile row in the admin profile list.
     */
    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView textProfileName;
        TextView textProfileEmail;
        TextView textProfilePhone;

        /**
         * Binds the name, email, and phone TextViews from the profile item layout.
         *
         * @param itemView the inflated item view
         */
        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            textProfileName = itemView.findViewById(R.id.textProfileName);
            textProfileEmail = itemView.findViewById(R.id.textProfileEmail);
            textProfilePhone = itemView.findViewById(R.id.textProfilePhone);
        }
    }
}