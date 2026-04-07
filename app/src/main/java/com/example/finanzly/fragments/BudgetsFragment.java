package com.example.finanzly.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.activities.BudgetMovements;
import com.example.finanzly.adapters.BudgetAdapter;
import com.example.finanzly.dialogs.BudgetDialog;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Reminder;
import com.example.finanzly.services.BudgetService;
import com.example.finanzly.services.InvitationService;
import com.example.finanzly.services.MovementService;
import com.example.finanzly.services.ReminderService;
import com.example.finanzly.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class BudgetsFragment extends Fragment {

    private BudgetService budgetService;

    private RecyclerView rvBudgets;
    private TextView tvNoBudgets;
    private EditText etCategoryFilter;
    private Spinner spinnerStatusFilter;
    private Button btnApplyFilter, btnClearFilter;

    private List<Budget> allBudgets = new ArrayList<>();
    private List<Budget> filteredBudgets = new ArrayList<>();

    private Map<String, List<Reminder>> remindersByBudget = new HashMap<>();

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

        adapter = new BudgetAdapter(
                filteredBudgets,
                requireContext(),
                remindersByBudget
        );

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
                leaveBudget(budget);
            }

            @Override
            public void onReminder(Budget budget) {
                openRemindersFragment(budget.getId());
            }
        });

        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgets.setAdapter(adapter);

        btnApplyFilter.setOnClickListener(v -> applyFilters());
        btnClearFilter.setOnClickListener(v -> clearFilters());

        root.findViewById(R.id.btnAddBudget)
                .setOnClickListener(v -> openBudgetDialog(null));

        loadBudgets();
        loadReminders(); // 🔥 IMPORTANTE

        return root;
    }

    // 🔥 CARGAR REMINDERS
    private void loadReminders() {

        FirebaseDatabase.getInstance()
                .getReference("reminders")
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        remindersByBudget.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {

                            Reminder reminder =
                                    child.getValue(Reminder.class);

                            if (reminder == null) continue;

                            if (reminder.getLinkedBudgetId() == null)
                                continue;

                            String budgetId =
                                    reminder.getLinkedBudgetId();

                            if (!remindersByBudget.containsKey(budgetId)) {
                                remindersByBudget.put(
                                        budgetId,
                                        new ArrayList<>()
                                );
                            }

                            remindersByBudget
                                    .get(budgetId)
                                    .add(reminder);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadBudgets() {

        budgetService.getReference()
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        allBudgets.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {

                            Budget budget =
                                    child.getValue(Budget.class);

                            if (budget == null) continue;

                            if ((budget.getUserId() != null &&
                                    budget.getUserId().equals(currentUserId)) ||

                                    (budget.getSharedUserIds() != null &&
                                            budget.getSharedUserIds()
                                                    .contains(currentUserId))) {

                                allBudgets.add(budget);
                            }
                        }

                        applyFilters();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void applyFilters() {

        String categoryFilter =
                etCategoryFilter.getText().toString().trim();

        String statusFilter =
                spinnerStatusFilter.getSelectedItem() != null
                        ? spinnerStatusFilter.getSelectedItem().toString()
                        : "";

        filteredBudgets.clear();

        for (Budget budget : allBudgets) {

            boolean matchesCategory =
                    TextUtils.isEmpty(categoryFilter)
                            || (budget.getCategory() != null
                            && budget.getCategory()
                            .toLowerCase()
                            .contains(categoryFilter.toLowerCase()));

            boolean matchesStatus = true;

            if ("Activo".equals(statusFilter)) {
                matchesStatus =
                        budget.getSpent() < budget.getLimit();
            } else if ("Excedido".equals(statusFilter)) {
                matchesStatus =
                        budget.getSpent() >= budget.getLimit();
            }

            if (matchesCategory && matchesStatus) {
                filteredBudgets.add(budget);
            }
        }

        adapter.notifyDataSetChanged();

        tvNoBudgets.setVisibility(
                filteredBudgets.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void clearFilters() {
        etCategoryFilter.setText("");
        spinnerStatusFilter.setSelection(0);
        applyFilters();
    }

    private void openBudgetDialog(Budget budgetToEdit) {

        BudgetDialog dialog = new BudgetDialog(
                requireContext(),
                currentUserId,
                budgetService,
                new UserService(getContext()),
                () -> {
                    // 🔥 Esto se ejecuta al guardar
                    loadBudgets();
                }
        );

        dialog.show(budgetToEdit);
    }

    private void onDeleteBudget(Budget budget) {

        if (budget == null || budget.getId() == null)
            return;

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("¿Eliminar presupuesto?")
                .setMessage("Se eliminarán también los movimientos, invitaciones y recordatorios asociados.")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> {

                    MovementService movementService = new MovementService(getContext());
                    InvitationService invitationService = new InvitationService();
                    ReminderService reminderService = new ReminderService(getContext());

                    String budgetId = budget.getId();

                    //
                    reminderService.deleteByBudgetId(budgetId, () -> {

                        invitationService.deleteByBudgetId(budgetId, () -> {

                            movementService.deleteByBudgetId(budgetId, () -> {


                                budgetService.delete(budgetId);

                                Toast.makeText(getContext(), "Presupuesto eliminado correctamente", Toast.LENGTH_SHORT).show();

                                loadBudgets();
                            });

                        });

                    });

                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void leaveBudget(Budget budget) {

        List<String> updated =
                new ArrayList<>(budget.getSharedUserIds());

        updated.remove(currentUserId);

        FirebaseDatabase.getInstance()
                .getReference("budgets")
                .child(budget.getId())
                .child("sharedUserIds")
                .setValue(updated)
                .addOnSuccessListener(aVoid -> loadBudgets());
    }

    private void goToMovements(Budget budget) {

        Intent intent =
                new Intent(getContext(), BudgetMovements.class);

        intent.putExtra("budgetId", budget.getId());

        startActivity(intent);
    }

    // 🔥 NAVEGACIÓN A REMINDERS
    private void openRemindersFragment(String budgetId) {

        RemindersFragment fragment =
                new RemindersFragment();

        Bundle bundle = new Bundle();
        bundle.putString("budgetId", budgetId);
        fragment.setArguments(bundle);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}