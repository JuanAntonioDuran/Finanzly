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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.activities.GoalMovements;
import com.example.finanzly.adapters.GoalAdapter;
import com.example.finanzly.adapters.UserAdapter;
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.User;
import com.example.finanzly.services.GoalService;
import com.example.finanzly.services.MovementService;
import com.example.finanzly.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GoalsFragment extends Fragment {

    private GoalService goalService;

    private RecyclerView rvGoals;
    private TextView tvNoGoals;
    private EditText etTitleFilter;
    private Button btnApplyFilter, btnClearFilter;

    private List<Goal> allGoals = new ArrayList<>();
    private List<Goal> filteredGoals = new ArrayList<>();
    private GoalAdapter adapter;

    private String currentUserId;

    public GoalsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_goals, container, false);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        rvGoals = root.findViewById(R.id.rvGoals);
        tvNoGoals = root.findViewById(R.id.tvNoGoals);
        etTitleFilter = root.findViewById(R.id.etTitleFilter);
        btnApplyFilter = root.findViewById(R.id.btnApplyGoalFilter);
        btnClearFilter = root.findViewById(R.id.btnClearGoalFilter);

        goalService = new GoalService(getContext());

        adapter = new GoalAdapter(filteredGoals, getContext());
        adapter.setOnGoalClickListener(new GoalAdapter.OnGoalClickListener() {
            @Override
            public void onAddProgress(Goal goal) {
                goToGoalMovements(goal);
            }

            @Override
            public void onEdit(Goal goal) {
                openGoalDialog(goal);
            }

            @Override
            public void onDelete(Goal goal) {
                onDeleteGoal(goal);
            }

            @Override
            public void onLeave(Goal goal) {
                if (getContext() == null) return;

                new AlertDialog.Builder(getContext())
                        .setTitle("Salir de la meta")
                        .setMessage("¿Estás seguro que deseas salir de la meta \"" + goal.getTitle() + "\"? Ya no podrás acceder a ella.")
                        .setPositiveButton("Sí, salir", (dialog, which) -> {

                            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            if (goal.getSharedUserIds() == null || !goal.getSharedUserIds().contains(currentUserId)) {
                                return;
                            }

                            // Quitar usuario de la lista
                            List<String> updatedList = new ArrayList<>(goal.getSharedUserIds());
                            updatedList.remove(currentUserId);

                            // Actualizar en Realtime Database
                            FirebaseDatabase.getInstance().getReference("goals")
                                    .child(goal.getId())
                                    .child("sharedUserIds")
                                    .setValue(updatedList)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Has salido de la meta", Toast.LENGTH_SHORT).show();
                                        loadGoals(); // refrescar lista
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al salir de la meta", Toast.LENGTH_SHORT).show());

                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }

        });

        rvGoals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGoals.setAdapter(adapter);

        loadGoals();

        btnApplyFilter.setOnClickListener(v -> applyFilters());
        btnClearFilter.setOnClickListener(v -> clearFilters());

        root.findViewById(R.id.btnAddGoal).setOnClickListener(v -> openGoalDialog(null));

        return root;
    }

    private void loadGoals() {
        goalService.getReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allGoals.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Goal goal = child.getValue(Goal.class);
                    if (goal != null && currentUserId != null &&
                            (currentUserId.equals(goal.getUserId()) ||
                                    (goal.getSharedUserIds() != null && goal.getSharedUserIds().contains(currentUserId)))) {
                        allGoals.add(goal);
                    }
                }
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showAlert("Error", "No se pudieron cargar las metas: " + error.getMessage());
            }
        });
    }


    private void applyFilters() {
        String titleFilter = etTitleFilter.getText().toString().trim();

        filteredGoals.clear();
        for (Goal goal : allGoals) {
            boolean matchesTitle = TextUtils.isEmpty(titleFilter) ||
                    goal.getTitle().toLowerCase().contains(titleFilter.toLowerCase());
            if (matchesTitle) filteredGoals.add(goal);
        }

        adapter.notifyDataSetChanged();
        tvNoGoals.setVisibility(filteredGoals.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearFilters() {
        etTitleFilter.setText("");
        applyFilters();
    }

    private void openGoalDialog(Goal goalToEdit) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_goal_form, null);

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etTargetAmount = dialogView.findViewById(R.id.etTargetAmount);
        EditText etDeadline = dialogView.findViewById(R.id.etDeadline);
        EditText etAddUserId = dialogView.findViewById(R.id.etAddUserId);
        Button btnAddUser = dialogView.findViewById(R.id.btnAddUser);
        RecyclerView recyclerSharedUsers = dialogView.findViewById(R.id.recyclerSharedUsers);
        TextView tvUsersTitle = dialogView.findViewById(R.id.tvUsersTitle);

        boolean isEditing = (goalToEdit != null);

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
            etTitle.setText(goalToEdit.getTitle());
            etTargetAmount.setText(String.valueOf(goalToEdit.getTargetAmount()));
            etDeadline.setText(goalToEdit.getDeadline());

            if (goalToEdit.getSharedUserIds() != null) {
                tvUsersTitle.setVisibility(View.VISIBLE);
                recyclerSharedUsers.setVisibility(View.VISIBLE);

                for (String uid : goalToEdit.getSharedUserIds()) {
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

        // -------------------------- DatePicker para la fecha --------------------------
        etDeadline.setFocusable(false);
        etDeadline.setClickable(true);
        etDeadline.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();

            try {
                String[] parts = etDeadline.getText().toString().split("-");
                if (parts.length == 3) {
                    calendar.set(java.util.Calendar.YEAR, Integer.parseInt(parts[0]));
                    calendar.set(java.util.Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                }
            } catch (Exception ignored) {}

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    getContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                        etDeadline.setText(formattedDate);
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        // ------------------------------------------------------------------------------

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(isEditing ? "Editar meta" : "Nueva meta")
                .setView(dialogView)
                .setPositiveButton(isEditing ? "Actualizar" : "Guardar", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String targetStr = etTargetAmount.getText().toString().trim();
                    String deadline = etDeadline.getText().toString().trim();

                    if (title.isEmpty() || targetStr.isEmpty() || deadline.isEmpty()) {
                        showAlert("Campos incompletos", "Debes llenar todos los campos.");
                        return;
                    }

                    if (title.length() < 3) {
                        showAlert("Título inválido", "El título debe tener al menos 3 caracteres.");
                        return;
                    }

                    double target;
                    try {
                        target = Double.parseDouble(targetStr);
                    } catch (NumberFormatException e) {
                        showAlert("Cantidad inválida", "Introduce un número válido.");
                        return;
                    }

                    if (target <= 0) {
                        showAlert("Cantidad inválida", "Debe ser mayor que 0.");
                        return;
                    }

                    // Validación de fecha
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                        sdf.setLenient(false);
                        java.util.Date selectedDate = sdf.parse(deadline);
                        java.util.Date today = new java.util.Date();

                        // Si la fecha es anterior a hoy
                        if (selectedDate.before(today)) {
                            showAlert("Fecha inválida", "La fecha de la meta no puede ser anterior a hoy.");
                            return;
                        }
                    } catch (Exception e) {
                        showAlert("Fecha inválida", "Formato de fecha incorrecto. Use yyyy-MM-dd.");
                        return;
                    }

                    if (isEditing) {
                        // Confirmación antes de actualizar
                        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                                .setTitle("Confirmar actualización")
                                .setMessage("¿Estás seguro de actualizar esta meta?")
                                .setPositiveButton("Sí", (confirmDialog, confirmWhich) -> {
                                    goalToEdit.setTitle(title);
                                    goalToEdit.setTargetAmount(target);
                                    goalToEdit.setDeadline(deadline);
                                    goalToEdit.setSharedUserIds(sharedUserIds);
                                    goalService.update(goalToEdit.getId(), goalToEdit);
                                    showAlert("Actualizado", "Meta actualizada correctamente.");
                                    loadGoals();
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    } else {
                        Goal newGoal = new Goal();
                        newGoal.setTitle(title);
                        newGoal.setTargetAmount(target);
                        newGoal.setCurrentAmount(0);
                        newGoal.setUserId(currentUserId);
                        newGoal.setDeadline(deadline);
                        newGoal.setSharedUserIds(sharedUserIds);
                        goalService.insert(newGoal);
                        showAlert("Creado", "Meta creada correctamente.");
                        loadGoals();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void onDeleteGoal(Goal goal) {
        if (goal == null || goal.getId() == null) return; // ⚠ Validación extra

        if (goal.getUserId().equals(currentUserId)) {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("¿Eliminar meta?")
                    .setMessage("Esta acción no se puede deshacer. ¿Deseas continuar?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        // Eliminar todos los movimientos asociados
                        MovementService movementService = new MovementService(getContext());
                        movementService.deleteByGoalId(goal.getId());

                        // Eliminar la meta
                        goalService.delete(goal.getId());

                        // Limpiar listas locales
                        allGoals.remove(goal);
                        filteredGoals.remove(goal);

                        adapter.notifyDataSetChanged(); // ⚡ refrescar RecyclerView

                        showAlert("Eliminado", "Meta y movimientos asociados eliminados correctamente.");
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Quitar acceso")
                    .setMessage("¿Deseas dejar de compartir esta meta?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        goalService.removeSharedUser(goal.getId(), currentUserId);

                        allGoals.remove(goal);
                        filteredGoals.remove(goal);
                        adapter.notifyDataSetChanged(); // ⚡ refrescar RecyclerView

                        showAlert("Actualizado", "Se ha dejado de compartir la meta.");
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }



    private void showAlert(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void goToGoalMovements(Goal goal) {
        if (goal == null || goal.getId() == null) {
            showAlert("Error", "No se pudo obtener el ID de la meta.");
            return;
        }

        Intent intent = new Intent(getContext(), GoalMovements.class);
        intent.putExtra("goalId", goal.getId());
        startActivity(intent);
    }
}
