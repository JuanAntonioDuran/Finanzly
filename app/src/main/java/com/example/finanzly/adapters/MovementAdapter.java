package com.example.finanzly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.models.Movement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovementAdapter extends RecyclerView.Adapter<MovementAdapter.MovementViewHolder> {

    private final Context context;
    private final List<Movement> movementList;

    // movementId -> userName
    private final Map<String, String> movementIdToUserName = new HashMap<>();

    private OnMovementActionListener listener;

    private final String currentUserId;
    private final String budgetOwnerId;

    public MovementAdapter(Context context,
                           List<Movement> movementList,
                           String currentUserId,
                           String budgetOwnerId) {
        this.context = context;
        this.movementList = movementList;
        this.currentUserId = currentUserId;
        this.budgetOwnerId = budgetOwnerId;
    }

    @NonNull
    @Override
    public MovementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_movement, parent, false);
        return new MovementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovementViewHolder holder, int position) {

        Movement movement = movementList.get(position);

        holder.tvDescription.setText(
                movement.getDescription() != null ? movement.getDescription() : "-"
        );

        holder.tvCategory.setText(
                movement.getCategory() != null ? movement.getCategory() : "-"
        );

        holder.tvDate.setText(
                movement.getDate() != null ? movement.getDate() : "-"
        );

        double amount = movement.getAmount();

        if ("income".equalsIgnoreCase(movement.getType())) {
            holder.tvAmount.setText(String.format("+%.2f €", amount));
            holder.tvAmount.setTextColor(context.getColor(R.color.green_primary));
        } else {
            holder.tvAmount.setText(String.format("-%.2f €", amount));
            holder.tvAmount.setTextColor(context.getColor(R.color.red_error));
        }

        // 🔥 CORREGIDO: usar userId en vez de movementId
        String userName = movementIdToUserName.get(movement.getUserId());
        holder.tvUserName.setText(userName != null ? userName : movement.getUserId());

        boolean isBudgetOwner = currentUserId.equals(budgetOwnerId);
        boolean isMovementOwner = currentUserId.equals(movement.getUserId());

        boolean canEdit = isBudgetOwner || isMovementOwner;

        // 🔹 botones visibles solo si tiene permisos
        holder.btnEdit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(canEdit ? View.VISIBLE : View.GONE);

        // 🔹 click editar botón
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(movement);
        });

        // 🔹 click eliminar botón
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(movement);
        });

        // 🔥 CLICK EN TODA LA TARJETA (NUEVO)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && canEdit) {
                listener.onEdit(movement);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movementList.size();
    }

    public void setOnMovementActionListener(OnMovementActionListener listener) {
        this.listener = listener;
    }

    /**
     * Asigna nombre de usuario a un movimiento
     */
    public void setUserNameForMovement(String userId, String userName) {
        movementIdToUserName.put(userId, userName);
        notifyDataSetChanged();
    }

    public static class MovementViewHolder extends RecyclerView.ViewHolder {

        TextView tvDescription, tvCategory, tvDate, tvAmount, tvUserName;
        ImageButton btnEdit, btnDelete;

        public MovementViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDescription = itemView.findViewById(R.id.tvMovementDescription);
            tvCategory = itemView.findViewById(R.id.tvMovementCategory);
            tvDate = itemView.findViewById(R.id.tvMovementDate);
            tvAmount = itemView.findViewById(R.id.tvMovementAmount);
            tvUserName = itemView.findViewById(R.id.tvMovementUser);

            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    public interface OnMovementActionListener {
        void onEdit(Movement movement);
        void onDelete(Movement movement);
    }
}