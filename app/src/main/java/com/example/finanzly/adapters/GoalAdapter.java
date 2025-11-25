package com.example.finanzly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.models.Goal;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    private final List<Goal> goals;
    private final Context context;
    private OnGoalClickListener listener;

    public interface OnGoalClickListener {
        void onAddProgress(Goal goal);
        void onEdit(Goal goal);
        void onDelete(Goal goal);
        void onLeave(Goal goal);
    }

    public GoalAdapter(List<Goal> goals, Context context) {
        this.goals = goals;
        this.context = context;
    }

    public void setOnGoalClickListener(OnGoalClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        Goal goal = goals.get(position);

        holder.tvTitle.setText(goal.getTitle());
        holder.tvTarget.setText("Meta: €" + goal.getTargetAmount());
        holder.tvCurrent.setText("Progreso: €" + goal.getCurrentAmount());

        // Estado
        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            holder.tvStatus.setText("Completada");
            holder.tvStatus.setBackgroundResource(R.color.green_primary);
        } else {
            holder.tvStatus.setText("Activo");
            holder.tvStatus.setBackgroundResource(R.color.blueAccent);
        }

        // Barra de progreso
        int progress = (int) ((goal.getCurrentAmount() / goal.getTargetAmount()) * 100);
        holder.progressBar.setProgress(Math.min(progress, 100));

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isOwner = goal.getUserId().equals(currentUserId);

        // --- Lógica de visibilidad según propietario ---
        if (isOwner) {
            holder.btnAddProgress.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnLeave.setVisibility(View.GONE);
        } else {
            holder.btnAddProgress.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnLeave.setVisibility(View.VISIBLE);
        }

        // Click listeners
        holder.btnAddProgress.setOnClickListener(v -> {
            if (listener != null) listener.onAddProgress(goal);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(goal);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(goal);
        });

        holder.btnLeave.setOnClickListener(v -> {
            if (listener != null) listener.onLeave(goal);
        });
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvTarget, tvCurrent, tvStatus;
        ProgressBar progressBar;
        Button btnAddProgress, btnEdit, btnDelete, btnLeave;

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvGoalTitle);
            tvTarget = itemView.findViewById(R.id.tvGoalTarget);
            tvCurrent = itemView.findViewById(R.id.tvGoalCurrent);
            tvStatus = itemView.findViewById(R.id.tvGoalStatus);
            progressBar = itemView.findViewById(R.id.progressBarGoal);
            btnAddProgress = itemView.findViewById(R.id.btnGoalAddProgress);
            btnEdit = itemView.findViewById(R.id.btnGoalEdit);
            btnDelete = itemView.findViewById(R.id.btnGoalDelete);
            btnLeave = itemView.findViewById(R.id.btnItemLeft);
        }
    }
}
