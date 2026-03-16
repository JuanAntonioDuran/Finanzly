package com.example.finanzly.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.activities.BudgetMovements;
import com.example.finanzly.activities.GoalMovements;
import com.example.finanzly.models.Movement;


import java.util.List;

public class MovementFragmentAdapter extends RecyclerView.Adapter<MovementFragmentAdapter.MovementViewHolder> {

    private final Context context;
    private final List<Movement> movementList;

    public MovementFragmentAdapter(Context context, List<Movement> movementList) {
        this.context = context;
        this.movementList = movementList;
    }

    @NonNull
    @Override
    public MovementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movement_fragment, parent, false);
        return new MovementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovementViewHolder holder, int position) {
        Movement movement = movementList.get(position);

        // 📝 Descripción
        holder.tvDescription.setText(
                movement.getDescription() != null && !movement.getDescription().isEmpty()
                        ? movement.getDescription()
                        : "Sin descripción"
        );

        // 🏷️ Categoría
        holder.tvCategory.setText(
                movement.getCategory() != null && !movement.getCategory().isEmpty()
                        ? movement.getCategory()
                        : "-"
        );

        // 📅 Fecha
        holder.tvDate.setText(
                movement.getDate() != null && !movement.getDate().isEmpty()
                        ? movement.getDate()
                        : "-"
        );

        double amount = movement.getAmount();

        // 💰 Tipo + color
        if ("income".equalsIgnoreCase(movement.getType())) {

            holder.tvAmount.setText(String.format("+%.2f €", amount));
            holder.tvAmount.setTextColor(context.getColor(R.color.green_primary));

            holder.tvType.setText("INGRESO");
            holder.tvType.setBackgroundResource(R.color.green_primary);
            holder.tvType.setTextColor(Color.WHITE);

        } else {

            holder.tvAmount.setText(String.format("-%.2f €", amount));
            holder.tvAmount.setTextColor(context.getColor(R.color.red_error));

            holder.tvType.setText("GASTO");
            holder.tvType.setBackgroundResource(R.color.red_error);
            holder.tvType.setTextColor(Color.WHITE);
        }

        // 🔗 Vinculación (texto)
        if (movement.getLinkedBudgetId() != null && !movement.getLinkedBudgetId().isEmpty()) {

            holder.tvLink.setText("Presupuesto");
            holder.tvLink.setVisibility(View.VISIBLE);

        } else if (movement.getLinkedGoalId() != null && !movement.getLinkedGoalId().isEmpty()) {

            holder.tvLink.setText("Meta");
            holder.tvLink.setVisibility(View.VISIBLE);

        } else {
            holder.tvLink.setVisibility(View.GONE);
        }

        // 🚀 CLICK EN EL ITEM
        holder.itemView.setOnClickListener(v -> {

            // 👉 Si tiene presupuesto
            if (movement.getLinkedBudgetId() != null && !movement.getLinkedBudgetId().isEmpty()) {

                Intent intent = new Intent(context, BudgetMovements.class);
                intent.putExtra("budgetId", movement.getLinkedBudgetId());
                context.startActivity(intent);

            }
            // 👉 Si tiene meta
            else if (movement.getLinkedGoalId() != null && !movement.getLinkedGoalId().isEmpty()) {

                Intent intent = new Intent(context, GoalMovements.class);
                intent.putExtra("goalId", movement.getLinkedGoalId());
                context.startActivity(intent);

            }
            // 👉 Si no tiene nada
            else {
                Toast.makeText(context, "Este movimiento no está vinculado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return movementList.size();
    }

    public static class MovementViewHolder extends RecyclerView.ViewHolder {

        TextView tvDescription, tvCategory, tvDate, tvAmount, tvType, tvLink;

        public MovementViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDescription = itemView.findViewById(R.id.tvMovementDescription);
            tvCategory = itemView.findViewById(R.id.tvMovementCategory);
            tvDate = itemView.findViewById(R.id.tvMovementDate);
            tvAmount = itemView.findViewById(R.id.tvMovementAmount);

            tvType = itemView.findViewById(R.id.tvMovementType);
            tvLink = itemView.findViewById(R.id.tvMovementLink);
        }
    }
}