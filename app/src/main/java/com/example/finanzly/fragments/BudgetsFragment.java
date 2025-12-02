package com.example.finanzly.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.activities.BudgetMovements;
import com.example.finanzly.adapters.BudgetAdapter;
import com.example.finanzly.adapters.UserAdapter;
import com.example.finanzly.dialogs.BudgetDialog;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.User;
import com.example.finanzly.services.BudgetService;
import com.example.finanzly.services.MovementService;
import com.example.finanzly.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

    private String currentUserId;

    public BudgetsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_budgets, container, false);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        rvBudgets = root.findViewById(R.id.rvBudgets);
        tvNoBudgets = root.findViewById(R.id.tvNoBudgets);
        etCategoryFilter = root.findViewById(R.id.etCategoryFilter);
        spinnerStatusFilter = root.findViewById(R.id.spinnerStatusFilter);
        btnApplyFilter = root.findViewById(R.id.btnApplyFilter);
        btnClearFilter = root.findViewById(R.id.btnClearFilter);

        budgetService = new BudgetService(getContext());

        adapter = new BudgetAdapter(filteredBudgets, getContext());
        adapter.setOnBudgetClickListener(new BudgetAdapter.OnBudgetClickListener() {


            @Override
            public void onDelete(Budget budget) {
                onDeleteBudget(budget);
            }

            @Override
            public void onViewMovements(Budget budget) {
                goToMovements(budget);
            }
            @Override
            public void onLeave(Budget budget) {
                if (getContext() == null) return;

                new AlertDialog.Builder(getContext())
                        .setTitle("Salir del presupuesto")
                        .setMessage("¿Estás seguro que deseas salir del presupuesto \"" + budget.getCategory() + "\"? Ya no podrás acceder a él.")
                        .setPositiveButton("Sí, salir", (dialog, which) -> {

                            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            if (budget.getSharedUserIds() == null || !budget.getSharedUserIds().contains(currentUserId)) {
                                return;
                            }

                            // Quitar usuario de la lista
                            List<String> updatedList = new ArrayList<>(budget.getSharedUserIds());
                            updatedList.remove(currentUserId);

                            // Actualizar en Realtime Database
                            FirebaseDatabase.getInstance().getReference("budgets")
                                    .child(budget.getId())
                                    .child("sharedUserIds")
                                    .setValue(updatedList)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Has salido del presupuesto", Toast.LENGTH_SHORT).show();
                                        loadBudgets(); // refrescar lista
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al salir del presupuesto", Toast.LENGTH_SHORT).show());

                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }



        });

        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgets.setAdapter(adapter);

        loadBudgets();

        btnApplyFilter.setOnClickListener(v -> applyFilters());
        btnClearFilter.setOnClickListener(v -> clearFilters());

        root.findViewById(R.id.btnAddBudget).setOnClickListener(v -> openBudgetDialog(null));

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
                        if ((budget.getUserId() != null && budget.getUserId().equals(currentUserId)) ||
                                (budget.getSharedUserIds() != null && budget.getSharedUserIds().contains(currentUserId))) {
                            allBudgets.add(budget);
                        }
                    }
                }
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showAlert("Error", "No se pudieron cargar los presupuestos: " + error.getMessage());
            }
        });
    }

    private void applyFilters() {
        String categoryFilter = etCategoryFilter.getText().toString().trim();
        String statusFilter = spinnerStatusFilter.getSelectedItem() != null
                ? spinnerStatusFilter.getSelectedItem().toString()
                : "";

        filteredBudgets.clear();

        for (Budget budget : allBudgets) {
            boolean matchesCategory = TextUtils.isEmpty(categoryFilter)
                    || (budget.getCategory() != null && budget.getCategory().toLowerCase().contains(categoryFilter.toLowerCase()));

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

    private void openBudgetDialog(Budget budgetToEdit) {
        if (currentUserId == null) return;

        BudgetDialog dialog = new BudgetDialog(
                getContext(),
                currentUserId,
                budgetService,
                new UserService(getContext()),
                () -> loadBudgets() // Este código se ejecuta al guardar o actualizar
        );

        dialog.show(budgetToEdit);
    }


    private void onDeleteBudget(Budget budget) {
        if (budget == null || budget.getId() == null || currentUserId == null) return;

        if (currentUserId.equals(budget.getUserId())) {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("¿Eliminar presupuesto?")
                    .setMessage("Esta acción no se puede deshacer. También se eliminarán todos los movimientos asociados.")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        // Eliminar todos los movimientos asociados
                        MovementService movementService = new MovementService(getContext());
                        movementService.deleteByBudgetId(budget.getId());

                        // Eliminar presupuesto
                        budgetService.delete(budget.getId());

                        showAlert("Eliminado", "Presupuesto y movimientos asociados eliminados correctamente.");
                        loadBudgets();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            budgetService.removeCollaborator(budget.getId(), currentUserId);
            showAlert("Actualizado", "Se ha dejado de compartir el presupuesto.");
            loadBudgets();
        }
    }

    private void goToMovements(Budget budget) {
        if (budget == null || budget.getId() == null) {
            showAlert("Error", "No se pudo obtener el ID del presupuesto.");
            return;
        }

        Intent intent = new Intent(getContext(), BudgetMovements.class);
        intent.putExtra("budgetId", budget.getId());
        startActivity(intent);
    }

    private void showAlert(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
