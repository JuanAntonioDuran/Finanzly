package com.example.finanzly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.models.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnRemoveUserListener {
        void onRemove(User user);
    }

    private final List<User> userList;
    private final OnRemoveUserListener removeListener;

    public UserAdapter(List<User> userList, OnRemoveUserListener removeListener) {
        this.userList = userList;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvEmail;
        ImageButton btnRemove;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName  = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            btnRemove = itemView.findViewById(R.id.btnRemoveUser);
        }

        public void bind(User user) {
            tvName.setText(user.getName());
            tvEmail.setText(user.getEmail());

            // ⭐⭐ FIX IMPORTANTE ⭐⭐
            btnRemove.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    removeListener.onRemove(user);
                }
            });
        }
    }
}
