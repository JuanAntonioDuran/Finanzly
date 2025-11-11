package com.example.finanzly.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finanzly.R;
import com.example.finanzly.adapters.BudgetAdapter;
import com.example.finanzly.models.Budget;
import com.example.finanzly.services.BudgetService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BudgetsFragment extends Fragment {

    private BudgetService budgetService;

    private RecyclerView rvBudgets;
    private TextView tvNoBudgets;
    private EditText etCategoryFilter;
    private Spinner spinnerStatusFilter;
    private Button btnApplyFilter, btnClearFilter;

    private List<Budget> allBudgets = new ArrayList<>();
    private List<Budget> filteredBudgets = new ArrayList<>();
    private BudgetAdapter adapter;

    // UID del usuario logueado
    private String currentUserId;

    public BudgetsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_budgets, container, false);

        // UID del usuario logueado
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // Inicializar vistas
        rvBudgets = root.findViewById(R.id.rvBudgets);
        tvNoBudgets = root.findViewById(R.id.tvNoBudgets);
        etCategoryFilter = root.findViewById(R.id.etCategoryFilter);
        spinnerStatusFilter = root.findViewById(R.id.spinnerStatusFilter);
        btnApplyFilter = root.findViewById(R.id.btnApplyFilter);
        btnClearFilter = root.findViewById(R.id.btnClearFilter);

        // RecyclerView setup
        adapter = new BudgetAdapter(filteredBudgets, getContext());
        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgets.setAdapter(adapter);

        // Inicializar servicio
        budgetService = new BudgetService(getContext());

        // Cargar presupuestos desde Firebase
        loadBudgets();

        // Botón filtrar
        btnApplyFilter.setOnClickListener(v -> applyFilters());

        // Botón limpiar
        btnClearFilter.setOnClickListener(v -> clearFilters());

        return root;
    }

    private void loadBudgets() {
        budgetService.getReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allBudgets.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Budget budget = child.getValue(Budget.class);
                    if (budget != null) {
                        // Mostrar solo si es propietario o colaborador
                        if (budget.getUserId().equals(currentUserId) ||
                                (budget.getSharedUserIds() != null && budget.getSharedUserIds().contains(currentUserId))) {
                            allBudgets.add(budget);
                        }
                    }
                }
                applyFilters(); // Aplica filtros cada vez que cambia la data
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Manejar error de Firebase si es necesario
            }
        });
    }

    private void applyFilters() {
        String categoryFilter = etCategoryFilter.getText().toString().trim();
        String statusFilter = spinnerStatusFilter.getSelectedItem().toString();

        filteredBudgets.clear();
        for (Budget budget : allBudgets) {
            boolean matchesCategory = TextUtils.isEmpty(categoryFilter) ||
                    budget.getCategory().toLowerCase().contains(categoryFilter.toLowerCase());
            boolean matchesStatus = true;

            if ("Activo".equals(statusFilter)) {
                matchesStatus = budget.getSpent() < budget.getLimit();
            } else if ("Excedido".equals(statusFilter)) {
                matchesStatus = budget.getSpent() >= budget.getLimit();
            }

            if (matchesCategory && matchesStatus) {
                filteredBudgets.add(budget);
            }
        }

        adapter.notifyDataSetChanged();
        tvNoBudgets.setVisibility(filteredBudgets.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearFilters() {
        etCategoryFilter.setText("");
        spinnerStatusFilter.setSelection(0);
        applyFilters();
    }
}
