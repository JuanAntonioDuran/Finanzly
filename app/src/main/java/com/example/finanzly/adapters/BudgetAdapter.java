package com.example.finanzly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Reminder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<Budget> budgets;
    private Context context;
    private OnBudgetClickListener listener;
    private Map<String, List<Reminder>> remindersByBudget;

    public interface OnBudgetClickListener {
        void onDelete(Budget budget);
        void onViewMovements(Budget budget);
        void onLeave(Budget budget);
        void onReminder(Budget budget);
    }

    public BudgetAdapter(List<Budget> budgets,
                         Context context,
                         Map<String, List<Reminder>> remindersByBudget) {

        this.budgets = budgets;
        this.context = context;
        this.remindersByBudget = remindersByBudget != null
                ? remindersByBudget
                : new HashMap<>();
    }

    public void setOnBudgetClickListener(OnBudgetClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {

        Budget budget = budgets.get(position);

        holder.tvCategory.setText(budget.getCategory());
        holder.tvLimit.setText("Límite: €" + budget.getLimit());
        holder.tvSpent.setText("Gastado: €" + budget.getSpent());

        // Estado presupuesto
        if (budget.getSpent() < budget.getLimit()) {
            holder.tvStatus.setText("Activo");
            holder.tvStatus.setBackgroundResource(R.color.green_primary);
        } else {
            holder.tvStatus.setText("Excedido");
            holder.tvStatus.setBackgroundResource(R.color.red_error);
        }

        // Barra progreso
        int progress = (int) ((budget.getSpent() / budget.getLimit()) * 100);
        holder.progressBar.setProgress(Math.min(progress, 100));

        String currentUserId =
                FirebaseAuth.getInstance().getCurrentUser().getUid();

        boolean isOwner =
                budget.getUserId().equals(currentUserId);

        //  Botones owner / shared
        holder.btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        holder.btnLeave.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        holder.btnViewMovements.setVisibility(View.VISIBLE);

        //  LÓGICA REMINDERS
        List<Reminder> reminders =
                remindersByBudget.get(budget.getId());

        if (reminders != null && !reminders.isEmpty()) {

            boolean hasRelevantReminder = false;
            boolean hasExpired = false;
            boolean hasPending = false;

            for (Reminder r : reminders) {

                boolean belongsToUser =
                        currentUserId.equals(r.getUserId()) ||
                                (r.getSharedUserIds() != null &&
                                        r.getSharedUserIds().contains(currentUserId));

                if (!belongsToUser) continue;

                hasRelevantReminder = true;

                if (r.getIsExpired()) {
                    hasExpired = true;
                } else if (!r.getIsCompleted()) {
                    hasPending = true;
                }
            }

            if (hasRelevantReminder) {

                holder.btnReminder.setVisibility(View.VISIBLE);

                //  PRIORIDAD COLORES
                if (hasExpired) {
                    holder.btnReminder.setBackgroundColor(
                            context.getColor(R.color.red_error)
                    );
                } else if (hasPending) {
                    holder.btnReminder.setBackgroundColor(
                            context.getColor(R.color.yellow)
                    );
                } else {
                    holder.btnReminder.setBackgroundColor(
                            context.getColor(R.color.green_primary)
                    );
                }

            } else {
                holder.btnReminder.setVisibility(View.GONE);
            }

        } else {
            holder.btnReminder.setVisibility(View.GONE);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onViewMovements(budget);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null)
                listener.onDelete(budget);
        });

        holder.btnViewMovements.setOnClickListener(v -> {
            if (listener != null)
                listener.onViewMovements(budget);
        });

        holder.btnLeave.setOnClickListener(v -> {
            if (listener != null)
                listener.onLeave(budget);
        });

        holder.btnReminder.setOnClickListener(v -> {
            if (listener != null)
                listener.onReminder(budget);
        });
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {

        TextView tvCategory, tvLimit, tvSpent, tvStatus;
        ProgressBar progressBar;
        Button btnDelete, btnViewMovements, btnLeave, btnReminder;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvLimit = itemView.findViewById(R.id.tvItemLimit);
            tvSpent = itemView.findViewById(R.id.tvItemSpent);
            tvStatus = itemView.findViewById(R.id.tvItemStatus);

            progressBar = itemView.findViewById(R.id.progressBarBudget);

            btnDelete = itemView.findViewById(R.id.btnItemDelete);
            btnViewMovements = itemView.findViewById(R.id.btnItemViewMovements);
            btnLeave = itemView.findViewById(R.id.btnItemLeft);
            btnReminder = itemView.findViewById(R.id.btnItemReminder);
        }
    }
}