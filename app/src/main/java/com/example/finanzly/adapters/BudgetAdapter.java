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

    // 🔔 Mapa de reminders asociados a cada budget
    private Map<String, List<Reminder>> remindersByBudget;

    public interface OnBudgetClickListener {
        void onDelete(Budget budget);
        void onViewMovements(Budget budget);
        void onLeave(Budget budget);
        void onReminder(Budget budget); // 🔔 NUEVO
    }

    public BudgetAdapter(List<Budget> budgets, Context context) {
        this.budgets = budgets;
        this.context = context;
        this.remindersByBudget = remindersByBudget != null ? remindersByBudget : new HashMap<>();
    }

    public void setOnBudgetClickListener(OnBudgetClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgets.get(position);

        holder.tvCategory.setText(budget.getCategory());
        holder.tvLimit.setText("Límite: €" + budget.getLimit());
        holder.tvSpent.setText("Gastado: €" + budget.getSpent());

        if (budget.getSpent() < budget.getLimit()) {
            holder.tvStatus.setText("Activo");
            holder.tvStatus.setBackgroundResource(R.color.green_primary);
        } else {
            holder.tvStatus.setText("Excedido");
            holder.tvStatus.setBackgroundResource(R.color.red_error);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onViewMovements(budget);
        });

        int progress = (int) ((budget.getSpent() / budget.getLimit()) * 100);
        holder.progressBar.setProgress(Math.min(progress, 100));

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isOwner = budget.getUserId().equals(currentUserId);

        // 🔐 Visibilidad de botones
        holder.btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        holder.btnLeave.setVisibility(isOwner ? View.GONE : View.VISIBLE);

        holder.btnViewMovements.setVisibility(View.VISIBLE);

        // 🔔 Mostrar Reminder solo si existen reminders para este budget
        if (remindersByBudget != null
                && remindersByBudget.containsKey(budget.getId())
                && remindersByBudget.get(budget.getId()) != null
                && !remindersByBudget.get(budget.getId()).isEmpty()) {
            holder.btnReminder.setVisibility(View.VISIBLE);
        } else {
            holder.btnReminder.setVisibility(View.GONE);
        }

        // Clicks
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(budget);
        });

        holder.btnViewMovements.setOnClickListener(v -> {
            if (listener != null) listener.onViewMovements(budget);
        });

        holder.btnLeave.setOnClickListener(v -> {
            if (listener != null) listener.onLeave(budget);
        });

        holder.btnReminder.setOnClickListener(v -> {
            if (listener != null) listener.onReminder(budget); // 🔔 Lanza RemindersFragment
        });
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {

        TextView tvCategory, tvLimit, tvSpent, tvStatus;
        ProgressBar progressBar;
        Button btnDelete, btnViewMovements, btnLeave, btnReminder; // 🔔 NUEVO

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

            btnReminder = itemView.findViewById(R.id.btnItemReminder); // 🔔 NUEVO
        }
    }
}