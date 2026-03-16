package com.example.finanzly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.models.Invitation;
import com.example.finanzly.models.User;
import com.example.finanzly.services.UserService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder> {

    private List<Invitation> invitations;
    private Context context;
    private boolean received;

    private UserService userService;

    private OnInvitationActionListener listener;

    public interface OnInvitationActionListener {
        void onAccept(Invitation invitation);
        void onDecline(Invitation invitation);
        void onCancel(Invitation invitation);
    }

    public void setOnInvitationActionListener(OnInvitationActionListener listener) {
        this.listener = listener;
    }

    public InvitationAdapter(List<Invitation> invitations, Context context, boolean received) {
        this.invitations = invitations;
        this.context = context;
        this.received = received;
        this.userService = new UserService(context);
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        Invitation inv = invitations.get(position);

        // 🔎 Obtener nombre del usuario
        userService.getById(inv.getFromUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String userName = "usuario";

                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getName() != null) {
                        userName = user.getName();
                    }
                }

                // 🔥 AQUÍ CAMBIA EL TEXTO SEGÚN SI ES RECIBIDA O ENVIADA
                if (received) {
                    holder.tvTitle.setText("Has recibido una invitación de " + userName);
                } else {
                    holder.tvTitle.setText("Has enviado una invitación a " + userName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (received) {
                    holder.tvTitle.setText("Has recibido una invitación");
                } else {
                    holder.tvTitle.setText("Has enviado una invitación");
                }
            }
        });

        // 📌 Tipo de recurso
        String type = inv.getResourceType();

        if ("budget".equals(type)) {
            holder.tvResourceType.setText("Presupuesto");
        } else if ("goal".equals(type)) {
            holder.tvResourceType.setText("Meta");
        } else {
            holder.tvResourceType.setText(type);
        }

        holder.tvDate.setText(inv.getCreatedAt());
        holder.tvMessage.setText(inv.getMessage());

        // 🔄 Reset botones
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnDecline.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);

        if (received) {

            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnDecline.setVisibility(View.VISIBLE);

            holder.btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(inv);
            });

            holder.btnDecline.setOnClickListener(v -> {
                if (listener != null) listener.onDecline(inv);
            });

        } else {

            holder.btnCancel.setVisibility(View.VISIBLE);

            holder.btnCancel.setOnClickListener(v -> {
                if (listener != null) listener.onCancel(inv);
            });
        }
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    static class InvitationViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvResourceType, tvDate, tvStatus, tvMessage;
        Button btnAccept, btnDecline, btnCancel;

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvInvitationTitle);
            tvResourceType = itemView.findViewById(R.id.tvResourceType);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvMessage = itemView.findViewById(R.id.tvMessage);

            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}