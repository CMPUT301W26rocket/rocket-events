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

    public interface OnProfileClickListener {
        void onProfileClick(User user);
    }

    private final List<User> userList;
    private final OnProfileClickListener listener;

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

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView textProfileName;
        TextView textProfileEmail;
        TextView textProfilePhone;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            textProfileName = itemView.findViewById(R.id.textProfileName);
            textProfileEmail = itemView.findViewById(R.id.textProfileEmail);
            textProfilePhone = itemView.findViewById(R.id.textProfilePhone);
        }
    }
}