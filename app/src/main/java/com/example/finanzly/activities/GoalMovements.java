package com.example.finanzly.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.adapters.MovementAdapter;
import com.example.finanzly.dialogs.AddEditReminderDialog;
import com.example.finanzly.dialogs.EditGoalDialog;
import com.example.finanzly.dialogs.MovementDialog;
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.Movement;
import com.example.finanzly.models.Reminder;
import com.example.finanzly.models.User;
import com.example.finanzly.services.GoalService;
import com.example.finanzly.services.MovementService;
import com.example.finanzly.services.UserService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class GoalMovements extends AppCompatActivity {

    private RecyclerView recyclerViewMovements;
    private MovementAdapter adapter;
    private List<Movement> movementList;
    private List<Movement> filteredList;
    private HashMap<String, String> userIdToNameMap = new HashMap<>();

    private MovementService movementService;
    private GoalService goalService;
    private UserService userService;

    private TextView tvEmptyState;
    private TextView tvRemaining;
    private TextView tvPercent;
    private TextView tvTitle;
    private ProgressBar progressTop;

    private EditText etStartDateFilter;
    private EditText etEndDateFilter;
    private EditText etUserFilter;
    private Spinner spUserFilter;
    private String goalId;
    private Goal currentGoal;
    private String uid;
    private String filterUser = "";
    private HashMap<String, String> userNameToIdMap = new HashMap<>();
    private ImageButton btnBack;
    private FloatingActionButton fabAddMovement, fabEditGoal, fabAddReminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_movements);

        recyclerViewMovements = findViewById(R.id.recyclerViewMovements);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvPercent = findViewById(R.id.tvPercent);
        progressTop = findViewById(R.id.progressTop);
        tvTitle = findViewById(R.id.tvTitleMovements);
        uid = FirebaseAuth.getInstance().getUid();

        etStartDateFilter = findViewById(R.id.etStartDateFilter);
        etEndDateFilter = findViewById(R.id.etEndDateFilter);
        spUserFilter = findViewById(R.id.spUserFilter);

        btnBack = findViewById(R.id.btnBack);
        fabAddMovement = findViewById(R.id.fabAddMovement);
        fabEditGoal = findViewById(R.id.fabEditGoal);
        fabAddReminder = findViewById(R.id.fabAddReminder);

        movementService = new MovementService(this);
        goalService = new GoalService(this);
        userService = new UserService(this);

        movementList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MovementAdapter(this, filteredList, uid, null);
        recyclerViewMovements.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMovements.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        // Recibir GoalId
        goalId = getIntent().getStringExtra("goalId");
        if (goalId == null) { Toast.makeText(this, "No se recibió goalId", Toast.LENGTH_SHORT).show(); finish(); return; }

        // Cargar Goal
        goalService.getById(goalId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentGoal = snapshot.getValue(Goal.class);
                    if (currentGoal != null) {
                        tvTitle.setText("Movimientos de \"" + currentGoal.getTitle() + "\"");

                        updateRemainingText();
                        updateProgressBar();
                        loadMovements();

                        // Solo el dueño ve los FABs de editar goal y añadir recordatorio
                        boolean isOwner = uid.equals(currentGoal.getUserId());
                        fabEditGoal.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                        fabAddReminder.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                    }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });

        // FAB Añadir Movimiento
        fabAddMovement.setOnClickListener(v -> openMovementDialog(null));
        fabAddReminder.setOnClickListener(v -> openReminderDialog(null));

        // FAB Editar Goal
        fabEditGoal.setOnClickListener(v -> {
            if (currentGoal == null) return;

            EditGoalDialog editGoalDialog = new EditGoalDialog(
                    this,
                    currentGoal,
                    uid.equals(currentGoal.getUserId()), // Solo el dueño puede editar
                    uid,
                    "", // nombre del usuario actual (si quieres mostrarlo en invitación)
                    updatedGoal -> {
                        // Actualizamos el Goal local
                        currentGoal.setTitle(updatedGoal.getTitle());
                        currentGoal.setTargetAmount(updatedGoal.getTargetAmount());
                        currentGoal.setDeadline(updatedGoal.getDeadline());
                        currentGoal.setSharedUserIds(updatedGoal.getSharedUserIds());

                        // Actualizamos en Firebase
                        new GoalService(this).update(currentGoal.getId(), updatedGoal);

                        // Actualizamos UI
                        tvTitle.setText("Movimientos de \"" + currentGoal.getTitle() + "\"");
                        recalculateGoalCurrentAmount();

                        Toast.makeText(this, "Goal actualizado", Toast.LENGTH_SHORT).show();
                    }
            );

            editGoalDialog.show();
        });




        adapter.setOnMovementActionListener(new MovementAdapter.OnMovementActionListener() {
            @Override public void onEdit(Movement movement) { openMovementDialog(movement); }
            @Override public void onDelete(Movement movement) { deleteMovement(movement); }
        });

        etStartDateFilter.setOnClickListener(v -> showDatePicker(etStartDateFilter));
        etEndDateFilter.setOnClickListener(v -> showDatePicker(etEndDateFilter));

        TextWatcher filterWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { applyFilters(); }
        };
        etStartDateFilter.addTextChangedListener(filterWatcher);
        etEndDateFilter.addTextChangedListener(filterWatcher);

    }


    private void setupUserFilterSpinner() {

        if (currentGoal == null) return;

        userNameToIdMap.clear();
        List<String> userNames = new ArrayList<>();
        userNames.add("Todos");

        HashSet<String> userIds = new HashSet<>();

        // 🔹 Owner
        if (currentGoal.getUserId() != null) {
            userIds.add(currentGoal.getUserId());
        }

        // 🔹 Shared users
        if (currentGoal.getSharedUserIds() != null) {
            userIds.addAll(currentGoal.getSharedUserIds());
        }

        // 🔹 Usuarios que aparecen en movimientos
        for (Movement m : movementList) {
            if (m.getUserId() != null) {
                userIds.add(m.getUserId());
            }
        }

        for (String uid : userIds) {
            String name = userIdToNameMap.get(uid);
            if (name != null) {
                userNames.add(name);
                userNameToIdMap.put(name, uid);
            }
        }

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                userNames
        );

        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUserFilter.setAdapter(adapterSpinner);

        spUserFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selectedName = (String) parent.getItemAtPosition(position);

                if ("Todos".equals(selectedName)) {
                    filterUser = "";
                } else {
                    filterUser = userNameToIdMap.get(selectedName);
                }

                applyFilters(); // si quieres solo con botón, quita esta línea
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterUser = "";
            }
        });
    }










    private void openMovementDialog(Movement movement) {
        MovementDialog dialog = new MovementDialog(this, savedMovement -> {
            if (!movementList.contains(savedMovement)) movementList.add(savedMovement);
            refreshAdapter();
        });

        dialog.setGoal(currentGoal, goalId);
        dialog.open(movement);
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (vw, y, m, d) ->
                target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void loadMovements() {
        movementService.getReference()
                .orderByChild("linkedGoalId")
                .equalTo(goalId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        movementList.clear();
                        userIdToNameMap.clear();

                        List<Movement> allMovements = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Movement m = ds.getValue(Movement.class);
                            if (m != null) allMovements.add(m);
                        }

                        if (allMovements.isEmpty()) {
                            applyFilters();
                            recalculateGoalCurrentAmount();
                            return;
                        }

                        for (Movement m : allMovements) {
                            if (!userIdToNameMap.containsKey(m.getUserId())) {
                                userService.getById(m.getUserId()).get()
                                        .addOnSuccessListener(userSnap -> {
                                            if (userSnap.exists()) {
                                                User u = userSnap.getValue(User.class);
                                                if (u != null) userIdToNameMap.put(m.getUserId(), u.getName());
                                            }
                                            movementList.add(m);
                                            if (movementList.size() == allMovements.size()) refreshAdapter();
                                        });
                            } else {
                                movementList.add(m);
                                if (movementList.size() == allMovements.size()) refreshAdapter();
                            }
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void refreshAdapter() {

        for (Movement mv : movementList) {
            String name = userIdToNameMap.get(mv.getUserId());
            adapter.setUserNameForMovement(mv.getId(), name != null ? name : "Desconocido");
        }

        setupUserFilterSpinner();   // 🔥 IMPORTANTE
        applyFilters();
        recalculateGoalCurrentAmount();
    }

    private void applyFilters() {

        String startDate = etStartDateFilter.getText().toString().trim();
        String endDate = etEndDateFilter.getText().toString().trim();

        filteredList.clear();

        for (Movement m : movementList) {

            boolean matches = true;

            // 🔹 Filtro fecha inicio
            if (!startDate.isEmpty() && m.getDate() != null) {
                if (m.getDate().compareTo(startDate) < 0) {
                    matches = false;
                }
            }

            // 🔹 Filtro fecha fin
            if (!endDate.isEmpty() && m.getDate() != null) {
                if (m.getDate().compareTo(endDate) > 0) {
                    matches = false;
                }
            }

            // 🔹 Filtro usuario por userId
            if (!filterUser.isEmpty()) {
                if (m.getUserId() == null || !m.getUserId().equals(filterUser)) {
                    matches = false;
                }
            }

            if (matches) {
                filteredList.add(m);
            }
        }

        for (Movement mv : filteredList) {
            String name = userIdToNameMap.get(mv.getUserId());
            adapter.setUserNameForMovement(mv.getId(), name != null ? name : "Desconocido");
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void deleteMovement(Movement movement) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar movimiento")
                .setMessage("¿Seguro que quieres eliminar este movimiento?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    movementService.delete(movement.getId());
                    recalculateGoalCurrentAmount();
                    refreshAdapter();
                    Toast.makeText(this, "Movimiento eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    private void recalculateGoalCurrentAmount() {
        movementService.getReference()
                .orderByChild("linkedGoalId")
                .equalTo(goalId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    double total = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Movement m = ds.getValue(Movement.class);
                        if (m != null) total += m.getType().equals("income") ? m.getAmount() : -m.getAmount();
                    }
                    if (total < 0) total = 0;
                    if (total > currentGoal.getTargetAmount()) total = currentGoal.getTargetAmount();

                    currentGoal.setCurrentAmount(total);
                    double finalTotal = total;
                    goalService.updatePartial(goalId, new HashMap<String, Object>() {{
                        put("currentAmount", finalTotal);
                    }});

                    updateRemainingText();
                    updateProgressBar();
                });
    }

    private void updateRemainingText() {
        if (currentGoal != null) {
            double remaining = currentGoal.getTargetAmount() - currentGoal.getCurrentAmount();
            tvRemaining.setText(" — Te faltan " + remaining + "€");
        }
    }

    private void updateProgressBar() {
        if (currentGoal == null) return;
        double percent = (currentGoal.getCurrentAmount() / currentGoal.getTargetAmount()) * 100;
        if (percent > 100) percent = 100;
        progressTop.setProgress((int) percent);
        tvPercent.setText((int) percent + "%");
    }

    private void openReminderDialog(Reminder existingReminder) {
        // Primero cargamos los nombres de todos los usuarios
        loadUsersForGoal(() -> {
            AddEditReminderDialog dialog = new AddEditReminderDialog(
                    this,
                    existingReminder,
                    uid, // Usuario actual
                    goalId, // ID del goal
                    currentGoal != null ? currentGoal.getTitle() : "Meta", // Título del goal
                    true, // isGoal
                    currentGoal != null ? currentGoal.getSharedUserIds() : new ArrayList<>(), // Usuarios compartidos
                    userIdToNameMap, // Map usuario -> nombre
                    (reminder, isNew) -> {
                        // Callback después de crear/editar
                        Toast.makeText(this, isNew ? "Recordatorio creado" : "Recordatorio actualizado", Toast.LENGTH_SHORT).show();
                    }
            );

            dialog.show();
        });
    }

    // Método para cargar nombres de usuarios
    private void loadUsersForGoal(Runnable callback) {
        userService.getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userIdToNameMap.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    User u = child.getValue(User.class);
                    if (u != null) userIdToNameMap.put(u.getUid(), u.getName());
                }
                // Ejecutar callback cuando los nombres estén listos
                if (callback != null) callback.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }




}
