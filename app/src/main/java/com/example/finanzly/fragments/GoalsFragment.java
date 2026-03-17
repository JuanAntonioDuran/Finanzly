package com.example.finanzly.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.activities.GoalMovements;
import com.example.finanzly.adapters.GoalAdapter;
import com.example.finanzly.dialogs.GoalDialog;
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.Reminder;
import com.example.finanzly.services.GoalService;
import com.example.finanzly.services.MovementService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

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

    // 🔥 IMPORTANTE: inicializado
    private Map<String, List<Reminder>> remindersByGoal = new HashMap<>();

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

        adapter = new GoalAdapter(filteredGoals, getContext(), remindersByGoal);

        adapter.setOnGoalClickListener(new GoalAdapter.OnGoalClickListener() {

            @Override
            public void onAddProgress(Goal goal) {
                goToGoalMovements(goal);
            }

            @Override
            public void onDelete(Goal goal) {
                onDeleteGoal(goal);
            }

            @Override
            public void onLeave(Goal goal) {
                leaveGoal(goal);
            }

            @Override
            public void onViewMovements(Goal goal) {
                goToGoalMovements(goal);
            }

            @Override
            public void onReminder(Goal goal) {
                openRemindersFragment(goal.getId());
            }
        });

        rvGoals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGoals.setAdapter(adapter);

        loadGoals();
        loadReminders(); // 🔥 CLAVE

        btnApplyFilter.setOnClickListener(v -> applyFilters());
        btnClearFilter.setOnClickListener(v -> clearFilters());

        root.findViewById(R.id.btnAddGoal).setOnClickListener(v -> {

            if (currentUserId == null) return;

            GoalDialog dialog = new GoalDialog(
                    getContext(),
                    currentUserId,
                    goalService,
                    () -> loadGoals()
            );

            dialog.show();
        });

        return root;
    }

    // 🔥 COPIADO DE BUDGET PERO PARA GOALS
    private void loadReminders() {

        FirebaseDatabase.getInstance()
                .getReference("reminders")
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        remindersByGoal.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {

                            Reminder reminder =
                                    child.getValue(Reminder.class);

                            if (reminder == null) continue;

                            // 🔴 IMPORTANTE
                            if (reminder.getLinkedGoalId() == null)
                                continue;

                            String goalId =
                                    reminder.getLinkedGoalId();

                            if (!remindersByGoal.containsKey(goalId)) {
                                remindersByGoal.put(
                                        goalId,
                                        new ArrayList<>()
                                );
                            }

                            remindersByGoal
                                    .get(goalId)
                                    .add(reminder);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
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
                                    (goal.getSharedUserIds() != null &&
                                            goal.getSharedUserIds().contains(currentUserId)))) {

                        allGoals.add(goal);
                    }
                }

                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showAlert("Error",
                        "No se pudieron cargar las metas: " + error.getMessage());
            }
        });
    }

    private void applyFilters() {

        String titleFilter =
                etTitleFilter.getText().toString().trim().toLowerCase();

        String statusFilter =
                spinnerGoalStatusFilter.getSelectedItem().toString();

        filteredGoals.clear();

        for (Goal goal : allGoals) {

            boolean matchesTitle =
                    titleFilter.isEmpty() ||
                            goal.getTitle().toLowerCase().contains(titleFilter);

            boolean matchesStatus = true;

            String goalStatus = getGoalStatus(goal);

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
        tvNoGoals.setVisibility(
                filteredGoals.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String getGoalStatus(Goal goal) {

        String deadline = goal.getDeadline();
        String currentDate = getCurrentDate();

        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            return "completado";
        }

        if (currentDate.compareTo(deadline) > 0) {
            return "fallida";
        }

        return "en_progreso";
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void clearFilters() {
        etTitleFilter.setText("");
        applyFilters();
    }

    private void leaveGoal(Goal goal) {

        List<String> updated =
                new ArrayList<>(goal.getSharedUserIds());

        updated.remove(currentUserId);

        FirebaseDatabase.getInstance()
                .getReference("goals")
                .child(goal.getId())
                .child("sharedUserIds")
                .setValue(updated)
                .addOnSuccessListener(aVoid -> loadGoals());
    }

    private void onDeleteGoal(Goal goal) {

        if (goal == null || goal.getId() == null) return;

        MovementService movementService =
                new MovementService(getContext());

        movementService.deleteByGoalId(goal.getId());
        goalService.delete(goal.getId());

        loadGoals();
    }

    private void goToGoalMovements(Goal goal) {

        Intent intent =
                new Intent(getContext(), GoalMovements.class);

        intent.putExtra("goalId", goal.getId());
        startActivity(intent);
    }

    private void openRemindersFragment(String goalId) {

        RemindersFragment fragment =
                new RemindersFragment();

        Bundle bundle = new Bundle();
        bundle.putString("goalId", goalId);
        fragment.setArguments(bundle);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showAlert(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}