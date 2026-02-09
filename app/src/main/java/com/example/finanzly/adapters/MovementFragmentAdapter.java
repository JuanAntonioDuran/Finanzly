package com.example.finanzly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
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

        holder.tvCategory.setText(movement.getDescription() != null ? movement.getDescription() : "-");
        holder.tvDescription.setText(movement.getCategory() != null ? movement.getCategory() : "-");
        holder.tvDate.setText(movement.getDate() != null ? movement.getDate() : "-");

        double amount = movement.getAmount();

        if ("income".equalsIgnoreCase(movement.getType())) {
            holder.tvAmount.setText(String.format("+%.2f €", amount));
            holder.tvAmount.setTextColor(context.getColor(R.color.green_primary)); // verde
        } else {
            holder.tvAmount.setText(String.format("-%.2f €", amount));
            holder.tvAmount.setTextColor(context.getColor(R.color.red_error)); // rojo
        }
    }


    @Override
    public int getItemCount() {
        return movementList.size();
    }

    public static class MovementViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvCategory, tvDate, tvAmount;

        public MovementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvMovementDescription);
            tvCategory = itemView.findViewById(R.id.tvMovementCategory);
            tvDate = itemView.findViewById(R.id.tvMovementDate);
            tvAmount = itemView.findViewById(R.id.tvMovementAmount);
        }
    }
}
