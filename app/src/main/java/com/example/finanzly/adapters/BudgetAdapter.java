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

import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<Budget> budgets;
    private Context context;

    // Click listeners opcionales
    private OnBudgetClickListener listener;

    public interface OnBudgetClickListener {
        void onEdit(Budget budget);
        void onDelete(Budget budget);
        void onViewMovements(Budget budget);
    }

    public BudgetAdapter(List<Budget> budgets, Context context) {
        this.budgets = budgets;
        this.context = context;
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

        // Estado
        if (budget.getSpent() < budget.getLimit()) {
            holder.tvStatus.setText("Activo");
            holder.tvStatus.setBackgroundResource(R.color.green_primary);
        } else {
            holder.tvStatus.setText("Excedido");
            holder.tvStatus.setBackgroundResource(R.color.red_error);
        }

        // Barra de progreso
        int progress = (int) ((budget.getSpent() / budget.getLimit()) * 100);
        holder.progressBar.setProgress(Math.min(progress, 100));

        // Click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(budget);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(budget);
        });
        holder.btnViewMovements.setOnClickListener(v -> {
            if (listener != null) listener.onViewMovements(budget);
        });
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {

        TextView tvCategory, tvLimit, tvSpent, tvStatus;
        ProgressBar progressBar;
        Button btnEdit, btnDelete, btnViewMovements;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvLimit = itemView.findViewById(R.id.tvItemLimit);
            tvSpent = itemView.findViewById(R.id.tvItemSpent);
            tvStatus = itemView.findViewById(R.id.tvItemStatus);
            progressBar = itemView.findViewById(R.id.progressBarBudget);
            btnEdit = itemView.findViewById(R.id.btnItemEdit);
            btnDelete = itemView.findViewById(R.id.btnItemDelete);
            btnViewMovements = itemView.findViewById(R.id.btnItemViewMovements);
        }
    }
}
