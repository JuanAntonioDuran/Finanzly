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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.adapters.BudgetAdapter;
import com.example.finanzly.adapters.UserAdapter;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.User;
import com.example.finanzly.services.BudgetService;
import com.example.finanzly.services.UserService;
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

        // Inicializar servicio
        budgetService = new BudgetService(getContext());

        // Configurar RecyclerView y adaptador con acciones
        adapter = new BudgetAdapter(filteredBudgets, getContext());
        adapter.setOnBudgetClickListener(new BudgetAdapter.OnBudgetClickListener() {
            @Override
            public void onEdit(Budget budget) {
                openBudgetDialog(budget); // Abrir diálogo para editar
            }

            @Override
            public void onDelete(Budget budget) {
                onDeleteBudget(budget); // Manejar eliminación
            }

            @Override
            public void onViewMovements(Budget budget) {
                goToMovements(budget); // Ir a movimientos
            }
        });


        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgets.setAdapter(adapter);

        // Cargar presupuestos desde Firebase
        loadBudgets();

        // Botones de filtros
        btnApplyFilter.setOnClickListener(v -> applyFilters());
        btnClearFilter.setOnClickListener(v -> clearFilters());

        // Botón de agregar presupuesto
        Button btnAddBudget = root.findViewById(R.id.btnAddBudget);
        btnAddBudget.setOnClickListener(v -> openBudgetDialog(null));

        return root;
    }

    // 📦 Cargar presupuestos desde Firebase
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
                showAlert("Error", "No se pudieron cargar los presupuestos: " + error.getMessage());
            }
        });
    }

    // 📊 Aplicar filtros
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

    // 🧹 Limpiar filtros
    private void clearFilters() {
        etCategoryFilter.setText("");
        spinnerStatusFilter.setSelection(0);
        applyFilters();
    }

    // 🧾 Diálogo para crear o editar presupuesto
    private void openBudgetDialog(Budget budgetToEdit) {
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
        UserAdapter usersAdapter = new UserAdapter(sharedUsers);

        recyclerSharedUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSharedUsers.setAdapter(usersAdapter);

        UserService userService = new UserService(getContext());

        // 🔁 Si estamos editando, cargamos los datos del presupuesto existente
        if (isEditing) {
            etCategory.setText(budgetToEdit.getCategory());
            etLimit.setText(String.valueOf(budgetToEdit.getLimit()));

            if (budgetToEdit.getSharedUserIds() != null && !budgetToEdit.getSharedUserIds().isEmpty()) {
                tvUsersTitle.setVisibility(View.VISIBLE);
                recyclerSharedUsers.setVisibility(View.VISIBLE);

                for (String uid : budgetToEdit.getSharedUserIds()) {
                    userService.getById(uid).get().addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            User u = snapshot.getValue(User.class);
                            if (u != null) {
                                sharedUsers.add(u);
                                sharedUserIds.add(u.getUid());
                                usersAdapter.notifyItemInserted(sharedUsers.size() - 1);
                            }
                        }
                    });
                }
            }
        }

        // ➕ Agregar usuario
        btnAddUser.setOnClickListener(v -> {
            String uid = etAddUserId.getText().toString().trim();
            if (uid.isEmpty()) {
                Toast.makeText(getContext(), "Introduce un UID válido", Toast.LENGTH_SHORT).show();
                return;
            }

            if (sharedUserIds.contains(uid)) {
                Toast.makeText(getContext(), "Ese usuario ya está agregado", Toast.LENGTH_SHORT).show();
                return;
            }

            userService.getById(uid).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        sharedUsers.add(user);
                        sharedUserIds.add(user.getUid());
                        usersAdapter.notifyItemInserted(sharedUsers.size() - 1);

                        etAddUserId.setText("");
                        tvUsersTitle.setVisibility(View.VISIBLE);
                        recyclerSharedUsers.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Error al buscar el usuario", Toast.LENGTH_SHORT).show()
            );
        });

        // 💾 Crear o actualizar presupuesto
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(isEditing ? "Editar presupuesto" : "Nuevo presupuesto")
                .setView(dialogView)
                .setPositiveButton(isEditing ? "Actualizar" : "Guardar", (dialog, which) -> {
                    String category = etCategory.getText().toString().trim();
                    String limitStr = etLimit.getText().toString().trim();

                    if (TextUtils.isEmpty(category) || TextUtils.isEmpty(limitStr)) {
                        showAlert("Campos incompletos", "Introduce una categoría y un límite válido.");
                        return;
                    }

                    double limit = Double.parseDouble(limitStr);
                    if (limit <= 0) {
                        showAlert("Límite inválido", "Debe ser mayor que 0.");
                        return;
                    }

                    if (isEditing) {
                        budgetToEdit.setCategory(category);
                        budgetToEdit.setLimit(limit);
                        budgetToEdit.setSharedUserIds(sharedUserIds);
                        budgetService.update(budgetToEdit.getId(), budgetToEdit);
                        showAlert("Actualizado", "Presupuesto actualizado correctamente.");
                    } else {
                        Budget newBudget = new Budget();
                        newBudget.setCategory(category);
                        newBudget.setLimit(limit);
                        newBudget.setSpent(0);
                        newBudget.setUserId(currentUserId);
                        newBudget.setSharedUserIds(sharedUserIds);
                        budgetService.insert(newBudget);
                        showAlert("Creado", "Presupuesto creado correctamente.");
                    }

                    loadBudgets();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }


    // 📍 Mostrar alerta simple
    private void showAlert(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void onDeleteBudget(Budget budget) {
        if (budget.getUserId().equals(currentUserId)) {
            // Propietario elimina
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("¿Eliminar presupuesto?")
                    .setMessage("Esta acción no se puede deshacer")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        budgetService.delete(budget.getId());
                        loadBudgets();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            // Colaborador sale
            budgetService.removeCollaborator(budget.getId(), currentUserId);
            loadBudgets();
        }
    }

    private void goToMovements(Budget budget) {
        // Aquí puedes abrir la Activity o Fragment de movimientos
        showAlert("Movimientos", "Aquí se mostrarán los movimientos de: " + budget.getCategory());
    }


}
