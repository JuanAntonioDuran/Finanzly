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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
        adapter = new MovementAdapter(this, filteredList, uid, currentGoal != null ? currentGoal.getUserId() : uid);
        recyclerViewMovements.setAdapter(adapter);
        recyclerViewMovements.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMovements.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        // Recibir GoalId
        goalId = getIntent().getStringExtra("goalId");
        if (goalId == null) { Toast.makeText(this, "No se recibió goalId", Toast.LENGTH_SHORT).show(); finish(); return; }

        goalService.getById(goalId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentGoal = snapshot.getValue(Goal.class);

                    if (currentGoal != null) {

                        adapter = new MovementAdapter(
                                GoalMovements.this,
                                filteredList,
                                uid,
                                currentGoal.getUserId()
                        );

                        recyclerViewMovements.setLayoutManager(new LinearLayoutManager(GoalMovements.this));
                        recyclerViewMovements.setAdapter(adapter);

                        adapter.setOnMovementActionListener(new MovementAdapter.OnMovementActionListener() {
                            @Override public void onEdit(Movement movement) { openMovementDialog(movement); }
                            @Override public void onDelete(Movement movement) { deleteMovement(movement); }
                        });

                        tvTitle.setText("Movimientos de \"" + currentGoal.getTitle() + "\"");

                        updateRemainingText();
                        updateProgressBar();

                        loadMovements();

                        // 🔥 FIX IMPORTANTE
                        setupUserFilterSpinner();

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

        fabEditGoal.setOnClickListener(v -> {
            if (currentGoal == null) return;

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

            if (firebaseUser == null) return;

            String uid = firebaseUser.getUid();

            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

            usersRef.child(uid).get().addOnSuccessListener(snapshot -> {
                User user = snapshot.getValue(User.class);

                // ✅ Variable final dentro de la lambda
                final String currentUserName = (user != null && user.getName() != null)
                        ? user.getName()
                        : "Usuario";

                EditGoalDialog editGoalDialog = new EditGoalDialog(
                        this,
                        currentGoal,
                        uid.equals(currentGoal.getUserId()),
                        uid,
                        currentUserName,
                        updatedGoal -> {

                            currentGoal.setTitle(updatedGoal.getTitle());
                            currentGoal.setTargetAmount(updatedGoal.getTargetAmount());
                            currentGoal.setDeadline(updatedGoal.getDeadline());
                            currentGoal.setSharedUserIds(updatedGoal.getSharedUserIds());

                            new GoalService(this).update(currentGoal.getId(), updatedGoal);

                            tvTitle.setText("Movimientos de \"" + currentGoal.getTitle() + "\"");
                            recalculateGoalCurrentAmount();

                            Toast.makeText(this, "Goal actualizado", Toast.LENGTH_SHORT).show();
                        }
                );

                editGoalDialog.show();
            });
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

        if (userNameToIdMap == null) {
            userNameToIdMap = new HashMap<>();
        } else {
            userNameToIdMap.clear();
        }

        final LinkedHashSet<String> userIdsToInclude = new LinkedHashSet<>();

        // Owner
        if (currentGoal.getUserId() != null) {
            userIdsToInclude.add(currentGoal.getUserId());
        }

        // Shared users
        if (currentGoal.getSharedUserIds() != null) {
            for (String uid : currentGoal.getSharedUserIds()) {
                if (uid != null) userIdsToInclude.add(uid);
            }
        }

        // Users from movements
        for (Movement m : movementList) {
            if (m.getUserId() != null) {
                userIdsToInclude.add(m.getUserId());
            }
        }

        // 🔥 CASO: vacío → solo "Todos"
        if (userIdsToInclude.isEmpty()) {

            List<String> userNames = new ArrayList<>();
            userNames.add("Todos");

            userNameToIdMap.put("Todos", "");

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    userNames
            );

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spUserFilter.setAdapter(adapter);
            spUserFilter.setOnItemSelectedListener(spinnerListener());
            return;
        }

        final Set<String> missingUserIds = new HashSet<>();

        for (String uid : userIdsToInclude) {
            if (!userIdToNameMap.containsKey(uid)) {
                missingUserIds.add(uid);
            }
        }

        if (missingUserIds.isEmpty()) {
            populateSpinnerFromUserIds(userIdsToInclude);
        } else {

            final AtomicInteger counter = new AtomicInteger(missingUserIds.size());

            for (String missingId : missingUserIds) {

                userService.getById(missingId).get()
                        .addOnSuccessListener(userSnap -> {

                            if (userSnap.exists()) {
                                User u = userSnap.getValue(User.class);
                                if (u != null && u.getName() != null) {
                                    userIdToNameMap.put(missingId, u.getName());
                                }
                            }

                            if (counter.decrementAndGet() <= 0) {
                                populateSpinnerFromUserIds(userIdsToInclude);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (counter.decrementAndGet() <= 0) {
                                populateSpinnerFromUserIds(userIdsToInclude);
                            }
                        });
            }
        }
    }

    private void populateSpinnerFromUserIds(Set<String> userIds) {

        List<String> userNames = new ArrayList<>();
        userNames.add("Todos");

        for (String uid : userIds) {
            String name = userIdToNameMap.get(uid);
            if (name != null) {
                userNames.add(name);
                userNameToIdMap.put(name, uid);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                userNames
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUserFilter.setAdapter(adapter);
        spUserFilter.setOnItemSelectedListener(spinnerListener());
    }

    private AdapterView.OnItemSelectedListener spinnerListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selectedName = (String) parent.getItemAtPosition(position);

                if ("Todos".equals(selectedName)) {
                    filterUser = "";
                } else {
                    filterUser = userNameToIdMap.get(selectedName);
                }

                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterUser = "";
            }
        };
    }

    // Abre un diálogo para crear o editar un movimiento y actualiza la lista al guardarlo
    private void openMovementDialog(Movement movement) {
        MovementDialog dialog = new MovementDialog(this, savedMovement -> {
            if (!movementList.contains(savedMovement)) movementList.add(savedMovement);
            refreshAdapter();
        });

        dialog.setGoal(currentGoal, goalId);
        dialog.open(movement);
    }


    // Muestra un selector de fecha y coloca la fecha seleccionada en el campo de texto
    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (vw, y, m, d) ->
                target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // Carga los movimientos asociados a un objetivo desde Firebase, obtiene los nombres de usuario y actualiza la lista
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

                        // 🔥 CASO: no hay movimientos
                        if (allMovements.isEmpty()) {
                            movementList.clear();

                            setupUserFilterSpinner(); // 🔥 IMPORTANTE

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
                                                if (u != null) {
                                                    userIdToNameMap.put(m.getUserId(), u.getName());
                                                }
                                            }

                                            movementList.add(m);
                                            if (movementList.size() == allMovements.size()) {
                                                refreshAdapter();
                                            }
                                        });
                            } else {
                                movementList.add(m);
                                if (movementList.size() == allMovements.size()) {
                                    refreshAdapter();
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }
    // Actualiza el adaptador con los nombres de usuario, aplica filtros y recalcula el total del objetivo
    private void refreshAdapter() {

        for (Movement mv : movementList) {
            String name = userIdToNameMap.get(mv.getUserId());
            adapter.setUserNameForMovement(mv.getId(), name != null ? name : "Desconocido");
        }

        setupUserFilterSpinner();
        applyFilters();
        recalculateGoalCurrentAmount();
    }

    // Aplica los filtros de fecha y usuario a la lista de movimientos y actualiza la vista
    private void applyFilters() {

        String startDate = etStartDateFilter.getText().toString().trim();
        String endDate = etEndDateFilter.getText().toString().trim();

        filteredList.clear();

        for (Movement m : movementList) {

            boolean matches = true;

            //  Filtro fecha inicio
            if (!startDate.isEmpty() && m.getDate() != null) {
                if (m.getDate().compareTo(startDate) < 0) {
                    matches = false;
                }
            }

            //  Filtro fecha fin
            if (!endDate.isEmpty() && m.getDate() != null) {
                if (m.getDate().compareTo(endDate) > 0) {
                    matches = false;
                }
            }

            // Filtro usuario por userId
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



    // Muestra un diálogo de confirmación y elimina el movimiento si el usuario acepta
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

//Metodo para recalcular la cantidad total del goal
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

    //Metodo auxiliar para actualizar el texto de la ui
    private void updateRemainingText() {
        if (currentGoal != null) {
            double remaining = currentGoal.getTargetAmount() - currentGoal.getCurrentAmount();
            tvRemaining.setText(" — Te faltan " + remaining + "€");
        }
    }
// Metodo auxiliar para actualizar la barra de progreso de la ui
    private void updateProgressBar() {
        if (currentGoal == null) return;
        double percent = (currentGoal.getCurrentAmount() / currentGoal.getTargetAmount()) * 100;
        if (percent > 100) percent = 100;
        progressTop.setProgress((int) percent);
        tvPercent.setText((int) percent + "%");
    }

    //Metodo apra abrir el dialog de creacion de Recordatorios
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
