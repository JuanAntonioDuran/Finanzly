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
import com.example.finanzly.activities.GoalMovements;
import com.example.finanzly.adapters.GoalAdapter;
import com.example.finanzly.adapters.UserAdapter;
import com.example.finanzly.dialogs.GoalDialog;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GoalsFragment extends Fragment {

    private GoalService goalService;

    private RecyclerView rvGoals;
    private TextView tvNoGoals;
    private EditText etTitleFilter;
    private Button btnApplyFilter, btnClearFilter;
    private Spinner spinnerGoalStatusFilter;

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
        spinnerGoalStatusFilter = root.findViewById(R.id.spinnerGoalStatusFilter);

        goalService = new GoalService(getContext());

        adapter = new GoalAdapter(filteredGoals, getContext());
        adapter.setOnGoalClickListener(new GoalAdapter.OnGoalClickListener() {
            @Override
            public void onAddProgress(Goal goal) {
                // El propietario puede añadir progreso
                goToGoalMovements(goal);
            }



            @Override
            public void onDelete(Goal goal) {
                // Solo propietario puede eliminar
                onDeleteGoal(goal);
            }

            @Override
            public void onLeave(Goal goal) {
                if (getContext() == null) return;

                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // Verifica que el usuario esté en la lista de colaboradores
                if (goal.getSharedUserIds() == null || !goal.getSharedUserIds().contains(currentUserId)) {
                    Toast.makeText(getContext(), "No puedes salir de esta meta.", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(getContext())
                        .setTitle("Salir de la meta")
                        .setMessage("¿Estás seguro que deseas salir de la meta \"" + goal.getTitle() + "\"? Ya no podrás acceder a ella.")
                        .setPositiveButton("Sí, salir", (dialog, which) -> {
                            // Quitar usuario de la lista de colaboradores
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

            @Override
            public void onViewMovements(Goal goal) {
                // Todos los usuarios pueden acceder a los movimientos
                goToGoalMovements(goal);
            }
        });


        rvGoals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGoals.setAdapter(adapter);

        loadGoals();

        btnApplyFilter.setOnClickListener(v -> applyFilters());
        btnClearFilter.setOnClickListener(v -> clearFilters());

        // Botón de crear nueva meta
        root.findViewById(R.id.btnAddGoal).setOnClickListener(v -> {
            if (currentUserId == null) return;

            GoalDialog dialog = new GoalDialog(
                    getContext(),
                    currentUserId,
                    goalService, // tu instancia de GoalService
                    () -> loadGoals() // refresca la lista al crear la meta
            );
            dialog.show();
        });


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
        String titleFilter = etTitleFilter.getText().toString().trim().toLowerCase();
        String statusFilter = spinnerGoalStatusFilter.getSelectedItem().toString();

        filteredGoals.clear();

        for (Goal goal : allGoals) {

            // 🔎 Filtro por título
            boolean matchesTitle = titleFilter.isEmpty() ||
                    goal.getTitle().toLowerCase().contains(titleFilter);

            // 🏷️ Filtro por estado calculado
            boolean matchesStatus = true;

            // Calcular el estado de la meta en tiempo real
            String goalStatus = getGoalStatus(goal);  // Método para obtener el estado calculado de la meta

            if (!statusFilter.equals("Todos")) {
                switch (statusFilter) {
                    case "Completada":
                        matchesStatus = "completado".equalsIgnoreCase(goalStatus);
                        break;
                    case "En progreso":
                        matchesStatus = "en_progreso".equalsIgnoreCase(goalStatus);
                        break;
                    case "Fallida":
                        matchesStatus = "fallida".equalsIgnoreCase(goalStatus);
                        break;
                }
            }

            if (matchesTitle && matchesStatus) {
                filteredGoals.add(goal);
            }
        }

        adapter.notifyDataSetChanged();
        tvNoGoals.setVisibility(filteredGoals.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Método para calcular el estado de la meta
    private String getGoalStatus(Goal goal) {
        // Lógica para calcular el estado de la meta en tiempo real

        // Obtener la fecha límite
        String deadline = goal.getDeadline(); // Por ejemplo, "2025-12-31"
        String currentDate = getCurrentDate(); // Método que obtiene la fecha actual (ej. "2025-12-16")

        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            return "completado";  // Si se alcanzó la cantidad objetivo
        }

        if (currentDate.compareTo(deadline) > 0) {
            return "fallida";  // Si la fecha actual es después de la fecha límite
        }

        return "en_progreso";  // Si aún no se ha completado y no ha pasado la fecha límite
    }

    // Método para obtener la fecha actual en formato "YYYY-MM-DD"
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }



    private void clearFilters() {
        etTitleFilter.setText("");
        applyFilters();
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
