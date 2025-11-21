package com.example.finanzly.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.adapters.MovementAdapter;
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.Movement;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
    private TextView tvSpent;
    private TextView tvPercent;
    private TextView tvTitle;
    private ProgressBar progressTop;

    private EditText etDateFilter;
    private EditText etUserFilter;

    private String goalId;
    private Goal currentGoal;

    private ImageButton btnBack;
    private FloatingActionButton fabAddMovement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_movements);

        // Inicialización de vistas
        recyclerViewMovements = findViewById(R.id.recyclerViewMovements);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvSpent = findViewById(R.id.tvRemaining);
        tvPercent = findViewById(R.id.tvPercent);
        progressTop = findViewById(R.id.progressTop);
        tvTitle = findViewById(R.id.tvTitle);

        etDateFilter = findViewById(R.id.etDateFilter);
        etUserFilter = findViewById(R.id.etUserFilter);

        btnBack = findViewById(R.id.btnBack);
        fabAddMovement = findViewById(R.id.fabAddMovement);

        // Servicios
        movementService = new MovementService(this);
        goalService = new GoalService(this);
        userService = new UserService(this);

        movementList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MovementAdapter(this, filteredList);
        recyclerViewMovements.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMovements.setAdapter(adapter);

        // Botón de volver
        btnBack.setOnClickListener(v -> finish());
        fabAddMovement.setOnClickListener(v -> openMovementDialog(null));

        // Recibir ID del Goal
        goalId = getIntent().getStringExtra("goalId");
        if (goalId == null) {
            Toast.makeText(this, "No se recibió goalId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Cargar goal
        goalService.getById(goalId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentGoal = snapshot.getValue(Goal.class);
                    if (currentGoal != null) {
                        tvTitle.setText("Movimientos de \"" + currentGoal.getTitle() + "\"");
                        updateSpentText();
                        updateProgressBar();
                        loadMovements();
                    }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });

        adapter.setOnMovementActionListener(new MovementAdapter.OnMovementActionListener() {
            @Override public void onEdit(Movement movement) { openMovementDialog(movement); }
            @Override public void onDelete(Movement movement) { deleteMovement(movement); }
        });

        // Filtros: DatePicker y TextWatcher
        etDateFilter.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (vw, y, m, d) ->
                    etDateFilter.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        TextWatcher filterWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { applyFilters(); }
        };
        etDateFilter.addTextChangedListener(filterWatcher);
        etUserFilter.addTextChangedListener(filterWatcher);
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

                        // Cargar todos los nombres de usuarios
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
                                                // Asignar nombres al adapter
                                                for (Movement mv : movementList) {
                                                    String name = userIdToNameMap.get(mv.getUserId());
                                                    adapter.setUserNameForMovement(mv.getId(), name != null ? name : "Desconocido");
                                                }
                                                applyFilters();
                                                recalculateGoalCurrentAmount();
                                            }
                                        });
                            } else {
                                movementList.add(m);
                                if (movementList.size() == allMovements.size()) {
                                    for (Movement mv : movementList) {
                                        String name = userIdToNameMap.get(mv.getUserId());
                                        adapter.setUserNameForMovement(mv.getId(), name != null ? name : "Desconocido");
                                    }
                                    applyFilters();
                                    recalculateGoalCurrentAmount();
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void applyFilters() {
        String filterDate = etDateFilter.getText().toString().trim();
        String filterUser = etUserFilter.getText().toString().trim().toLowerCase();

        filteredList.clear();

        for (Movement m : movementList) {
            boolean matches = true;

            // Filtrar por fecha
            if (!filterDate.isEmpty() && !m.getDate().equals(filterDate)) matches = false;

            // Filtrar por usuario
            if (!filterUser.isEmpty()) {
                String name = userIdToNameMap.get(m.getUserId());
                if (name == null || !name.toLowerCase().contains(filterUser)) matches = false;
            }

            if (matches) filteredList.add(m);
        }

        // Actualizar nombres en el adapter antes de mostrar
        for (Movement mv : filteredList) {
            String name = userIdToNameMap.get(mv.getUserId());
            adapter.setUserNameForMovement(mv.getId(), name != null ? name : "Desconocido");
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ================= CREATE / EDIT =================

    private void openMovementDialog(Movement movement) {
        boolean isNew = movement == null;
        Movement m = isNew ? new Movement() : movement;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_movement_form, null);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnType = dialogView.findViewById(R.id.btnType);

        btnType.setVisibility(View.VISIBLE);
        final String[] type = {isNew ? "income" : m.getType()};
        btnType.setText(type[0].equals("income") ? "Ingreso" : "Gasto");
        btnType.setOnClickListener(v -> {
            type[0] = type[0].equals("income") ? "expense" : "income";
            btnType.setText(type[0].equals("income") ? "Ingreso" : "Gasto");
        });

        if (!isNew) {
            etDate.setText(m.getDate());
            etAmount.setText(String.valueOf(m.getAmount()));
            etDescription.setText(m.getDescription());
        }

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, mo, d) ->
                    etDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, mo + 1, d)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String date = etDate.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

            if (date.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Fecha y cantidad obligatorias", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            double currentAmountWithoutThis = isNew ? currentGoal.getCurrentAmount() :
                    currentGoal.getCurrentAmount() - (m.getType().equals("income") ? m.getAmount() : -m.getAmount());
            double projected = currentAmountWithoutThis + (type[0].equals("income") ? amount : -amount);

            if (projected < 0 || projected > currentGoal.getTargetAmount()) {
                Toast.makeText(this, projected < 0 ? "No se puede disminuir más de lo acumulado" :
                        "No se puede superar el objetivo", Toast.LENGTH_LONG).show();
                return;
            }

            m.setId(isNew ? UUID.randomUUID().toString() : m.getId());
            m.setType(type[0]);
            m.setCategory(currentGoal.getTitle());
            m.setDate(date);
            m.setDescription(desc);
            m.setAmount(amount);
            m.setLinkedGoalId(goalId);
            m.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());

            if (isNew) movementService.insert(m);
            else movementService.update(m.getId(), m);

            recalculateGoalCurrentAmount();
            updateProgressBar();
            dialog.dismiss();
            Toast.makeText(this, isNew ? "Movimiento creado" : "Movimiento actualizado", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void deleteMovement(Movement movement) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar movimiento")
                .setMessage("¿Seguro que quieres eliminar este movimiento?")
                .setPositiveButton("Sí", (d, w) -> {
                    movementService.delete(movement.getId());
                    recalculateGoalCurrentAmount();
                    updateProgressBar();
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

                    HashMap<String, Object> updates = new HashMap<>();
                    updates.put("currentAmount", total);
                    goalService.updatePartial(goalId, updates);

                    currentGoal.setCurrentAmount(total);
                    updateSpentText();
                    updateProgressBar();
                });
    }

    private void updateSpentText() {
        if (currentGoal != null) {
            double remaining = currentGoal.getTargetAmount() - currentGoal.getCurrentAmount();
            tvSpent.setText(" — Te faltan " + remaining + "€");
        }
    }

    private void updateProgressBar() {
        if (currentGoal == null) return;

        double percent = (currentGoal.getCurrentAmount() / currentGoal.getTargetAmount()) * 100;
        if (percent > 100) percent = 100;

        progressTop.setProgress((int) percent);
        tvPercent.setText((int) percent + "%");
    }
}
