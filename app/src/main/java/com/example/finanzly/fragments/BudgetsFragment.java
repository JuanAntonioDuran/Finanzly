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
            public void onEdit(Budget budget) {
                openBudgetDialog(budget);
            }

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

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_budget_form, null);

        EditText etCategory = dialogView.findViewById(R.id.etCategory);
        EditText etLimit = dialogView.findViewById(R.id.etLimit);
        EditText etAddUserId = dialogView.findViewById(R.id.etAddUserId);
        Button btnAddUser = dialogView.findViewById(R.id.btnAddUser);
        RecyclerView recyclerSharedUsers = dialogView.findViewById(R.id.recyclerSharedUsers);
        TextView tvUsersTitle = dialogView.findViewById(R.id.tvUsersTitle);

        boolean isEditing = (budgetToEdit != null);

        List<User> sharedUsers = new ArrayList<>();
        List<String> sharedUserIds = new ArrayList<>();

        UserService userService = new UserService(getContext());

        UserAdapter[] usersAdapter = new UserAdapter[1];

        usersAdapter[0] = new UserAdapter(sharedUsers, user -> {
            sharedUsers.remove(user);
            sharedUserIds.remove(user.getUid());
            usersAdapter[0].notifyDataSetChanged();
        });

        recyclerSharedUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSharedUsers.setAdapter(usersAdapter[0]);

        if (isEditing) {
            etCategory.setText(budgetToEdit.getCategory());
            etLimit.setText(String.valueOf(budgetToEdit.getLimit()));

            if (budgetToEdit.getSharedUserIds() != null) {
                tvUsersTitle.setVisibility(View.VISIBLE);
                recyclerSharedUsers.setVisibility(View.VISIBLE);

                for (String uid : budgetToEdit.getSharedUserIds()) {
                    userService.getById(uid).get().addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            User u = snapshot.getValue(User.class);
                            if (u != null) {
                                sharedUsers.add(u);
                                sharedUserIds.add(u.getUid());
                                usersAdapter[0].notifyItemInserted(sharedUsers.size() - 1);
                            }
                        }
                    });
                }
            }
        }

        btnAddUser.setOnClickListener(v -> {
            String uid = etAddUserId.getText().toString().trim();

            if (uid.isEmpty()) {
                Toast.makeText(getContext(), "Introduce un UID válido", Toast.LENGTH_SHORT).show();
                return;
            }

            if (sharedUserIds.contains(uid)) {
                Toast.makeText(getContext(), "Ese usuario ya está agregado.", Toast.LENGTH_SHORT).show();
                return;
            }

            userService.getById(uid).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        sharedUsers.add(user);
                        sharedUserIds.add(uid);
                        usersAdapter[0].notifyItemInserted(sharedUsers.size() - 1);

                        etAddUserId.setText("");
                        tvUsersTitle.setVisibility(View.VISIBLE);
                        recyclerSharedUsers.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), "Usuario no encontrado.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(isEditing ? "Editar presupuesto" : "Nuevo presupuesto")
                .setView(dialogView)
                .setPositiveButton(isEditing ? "Actualizar" : "Guardar", (dialog, which) -> {
                    String category = etCategory.getText().toString().trim();
                    String limitStr = etLimit.getText().toString().trim();

                    if (category.isEmpty() || limitStr.isEmpty()) {
                        showAlert("Campos incompletos", "Debes llenar todos los campos.");
                        return;
                    }

                    double limit = Double.parseDouble(limitStr);
                    if (limit <= 0) {
                        showAlert("Límite inválido", "Debe ser mayor que 0.");
                        return;
                    }

                    if (isEditing) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Para guardar en UTC
                        String currentDate = sdf.format(new Date());
                        budgetToEdit.setCategory(category);
                        budgetToEdit.setLimit(limit);
                        budgetToEdit.setSharedUserIds(sharedUserIds);
                        budgetToEdit.setUpdatedAt(currentDate);
                        budgetService.update(budgetToEdit.getId(), budgetToEdit);

                        showAlert("Actualizado", "Presupuesto actualizado correctamente.");
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Para guardar en UTC
                        String currentDate = sdf.format(new Date());
                        Budget newBudget = new Budget();
                        newBudget.setCategory(category);
                        newBudget.setLimit(limit);
                        newBudget.setSpent(0);
                        newBudget.setUserId(currentUserId);
                        newBudget.setSharedUserIds(sharedUserIds);
                        newBudget.setCreatedAt(currentDate);


                        budgetService.insert(newBudget);

                        showAlert("Creado", "Presupuesto creado correctamente.");
                    }

                    loadBudgets();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
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
