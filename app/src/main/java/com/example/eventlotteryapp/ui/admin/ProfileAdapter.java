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

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView textProfileName, textProfileEmail, textProfilePhone;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            textProfileName = itemView.findViewById(R.id.textProfileName);
            textProfileEmail = itemView.findViewById(R.id.textProfileEmail);
            textProfilePhone = itemView.findViewById(R.id.textProfilePhone);
        }
    }
}