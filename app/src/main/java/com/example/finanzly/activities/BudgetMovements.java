package com.example.finanzly.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.adapters.MovementAdapter;
import com.example.finanzly.dialogs.AddEditReminderDialog;
import com.example.finanzly.dialogs.EditBudgetDialog;
import com.example.finanzly.dialogs.MovementDialog;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Movement;
import com.example.finanzly.models.User;
import com.example.finanzly.services.MovementService;
import com.example.finanzly.services.UserService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BudgetMovements extends AppCompatActivity {

    // UI
    private TextView tvPercent, tvSpent, tvEmptyState;
    private EditText  etUserFilter, etStartDateFilter, etEndDateFilter;
    private RecyclerView recyclerView;
    private ProgressBar progressTop;
    private FloatingActionButton fabAddMovement, fabEditBudget, fabAddReminder;

    // Firebase references
    private DatabaseReference budgetsRef;
    private DatabaseReference movementsRef;
    private DatabaseReference usersRef;
    private String uid;

    // Data
    private String budgetId;
    private Budget currentBudget;

    private MovementAdapter adapter;
    private List<Movement> movementList = new ArrayList<>();
    private List<Movement> filteredList = new ArrayList<>();

    private MovementService movementService;
    private UserService userService;


    // Maps
    private Map<String, String> userIdToNameMap = new HashMap<>();


    private String filterUser = "";
    private String filterDateFrom = "";
    private String filterDateTo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_movements);

        uid = FirebaseAuth.getInstance().getUid();
        budgetsRef = FirebaseDatabase.getInstance().getReference("budgets");
        movementsRef = FirebaseDatabase.getInstance().getReference("movements");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        budgetId = getIntent().getStringExtra("budgetId");

        // UI BINDING
        tvPercent = findViewById(R.id.tvPercent);
        tvSpent = findViewById(R.id.tvSpent);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        etUserFilter = findViewById(R.id.etUserFilter);
        etStartDateFilter = findViewById(R.id.etStartDateFilter);
        etEndDateFilter = findViewById(R.id.etEndDateFilter);
        recyclerView = findViewById(R.id.recyclerViewMovements);
        progressTop = findViewById(R.id.progressTop);

        fabAddMovement = findViewById(R.id.fabAddMovement);
        fabEditBudget = findViewById(R.id.fabEditBudget);
        fabAddReminder = findViewById(R.id.fabAddReminder);

        // Suponiendo que tienes el budget y el currentUserId
        FloatingActionButton fabEditBudget = findViewById(R.id.fabEditBudget);
        FloatingActionButton fabAddReminder = findViewById(R.id.fabAddReminder);



        movementService = new MovementService(this);
        userService = new UserService(this);


        findViewById(R.id.btnBack).setOnClickListener(v -> finish());


        loadUsers();          // Intentamos precargar usuarios (cache)
        loadBudget();
        setupDatePickers();
        setupFilterButtons();




        // FAB – Añadir movimiento
        fabAddMovement.setOnClickListener(v -> {
            MovementDialog dialog = new MovementDialog(this, m -> {
                String id = m.getId() != null ? m.getId() : movementsRef.child(budgetId).push().getKey();
                m.setId(id);

                movementsRef.child(budgetId).child(id)
                        .setValue(m)
                        .addOnSuccessListener(aVoid -> {
                            // ❌ No añadimos manualmente a movementList, el listener de loadMovements() se encargará
                            Toast.makeText(this, "Movimiento guardado", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Error guardando movimiento", Toast.LENGTH_SHORT).show());
            });

            dialog.setBudget(currentBudget, budgetId);
            dialog.open(null);
        });

// FAB – Editar presupuesto
        fabEditBudget.setOnClickListener(v -> {
            if (currentBudget == null) return;

            EditBudgetDialog dialog = new EditBudgetDialog(
                    BudgetMovements.this,
                    currentBudget,
                    true, // Asumimos que el usuario es owner aquí
                    FirebaseAuth.getInstance().getUid(),
                    FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                    updatedBudget -> {
                        // Callback: actualizamos budget local y en Firebase
                        currentBudget.setCategory(updatedBudget.getCategory());
                        currentBudget.setLimit(updatedBudget.getLimit());

                        // Actualizar en Firebase
                        budgetsRef.child(currentBudget.getId())
                                .setValue(currentBudget)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(BudgetMovements.this, "Presupuesto actualizado", Toast.LENGTH_SHORT).show();
                                    recalculateBudgetSpent();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(BudgetMovements.this, "Error al actualizar presupuesto", Toast.LENGTH_SHORT).show());
                    });

            dialog.show();
        });

        fabAddReminder.setOnClickListener(v -> {

            // ⚠️ Verificar que el presupuesto esté cargado
            if (currentBudget == null) {
                Toast.makeText(this, "Presupuesto no cargado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener usuarios compartidos del BUDGET
            List<String> sharedUsers = currentBudget.getSharedUserIds();
            if (sharedUsers == null) sharedUsers = new ArrayList<>();

            AddEditReminderDialog dialog = new AddEditReminderDialog(
                    BudgetMovements.this,
                    null,                               // crear nuevo
                    uid,                                // usuario actual
                    currentBudget.getId(),              // ID del presupuesto
                    currentBudget.getCategory(),        // nombre del presupuesto
                    false,                              // false = es BUDGET
                    currentBudget.getSharedUserIds(),   // lista de usuarios vinculados
                    userIdToNameMap,                    // mapa UID -> Nombre
                    (reminder, isNew) -> {
                        Toast.makeText(this, "Recordatorio guardado", Toast.LENGTH_SHORT).show();
                    }
            );

            dialog.show();
        });




    }

    // ----------------------------
    // RECYCLER
    // ----------------------------

    private void setupRecycler() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MovementAdapter(
                this,
                filteredList,
                uid,
                currentBudget.getUserId()
        );
        recyclerView.setAdapter(adapter);

        adapter.setOnMovementActionListener(new MovementAdapter.OnMovementActionListener() {
            @Override
            public void onEdit(Movement movement) {
                MovementDialog dialog = new MovementDialog(BudgetMovements.this, m -> {
                    // Actualizar en Firebase correctamente
                    movementsRef.child(m.getLinkedBudgetId()).child(m.getId())
                            .setValue(m)
                            .addOnSuccessListener(aVoid -> {
                                // Actualizar lista en memoria buscando por id
                                int index = findMovementIndexById(m.getId());
                                if (index != -1) {
                                    movementList.set(index, m);
                                    applyFilters();
                                } else {
                                    // Si no está, no forzamos duplicados — la escucha de Firebase sincronizará en breve
                                }
                                recalculateBudgetSpent();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(BudgetMovements.this, "Error actualizando movimiento", Toast.LENGTH_SHORT).show());
                });

                dialog.setBudget(currentBudget, budgetId);
                dialog.open(movement);
            }

            @Override
            public void onDelete(Movement movement) {
                new androidx.appcompat.app.AlertDialog.Builder(BudgetMovements.this)
                        .setTitle("Eliminar movimiento")
                        .setMessage("¿Estás seguro de que quieres eliminar este movimiento?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            try {
                                // Llamamos al servicio para borrar en Firebase.
                                // NO eliminamos localmente de movementList para evitar inconsistencias/duplicados:
                                // el ValueEventListener se encargará de actualizar la lista cuando Firebase confirme el cambio.
                                movementService.delete(movement.getId());
                                // Si movementService tiene callbacks, deberías usarlos para notificar éxito/fracaso.
                                Toast.makeText(BudgetMovements.this, "Eliminando movimiento...", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(BudgetMovements.this, "Error eliminando movimiento", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }



        });
    }

    private int findMovementIndexById(String id) {
        if (id == null) return -1;
        for (int i = 0; i < movementList.size(); i++) {
            Movement m = movementList.get(i);
            if (m != null && id.equals(m.getId())) return i;
        }
        return -1;
    }


    // ----------------------------
    // DATE PICKERS
    // ----------------------------

    private void setupDatePickers() {
        etStartDateFilter.setOnClickListener(v -> showDatePicker(etStartDateFilter));
        etEndDateFilter.setOnClickListener(v -> showDatePicker(etEndDateFilter));
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) ->
                target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ----------------------------
    // FILTER BUTTONS
    // ----------------------------

    private void setupFilterButtons() {
        findViewById(R.id.btnApplyFilter).setOnClickListener(v -> applyFilters());
        findViewById(R.id.btnClearFilter).setOnClickListener(v -> clearFilters());
    }



    // ----------------------------
    // LOAD DATA
    // ----------------------------

    private void loadBudget() {
        budgetsRef.orderByChild("id").equalTo(budgetId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            currentBudget = child.getValue(Budget.class);
                        }

                        if (currentBudget != null) {
                            // Inicializamos el recycler ahora que tenemos el budget
                            setupRecycler();



                            // Cargamos movimientos
                            loadMovements();

                            // Actualizamos UI
                            recalculateBudgetSpent();
                            updateProgressBar();

                            if (currentBudget.getUserId().equals(uid)) {
                                fabEditBudget.setVisibility(View.VISIBLE);
                                fabAddReminder.setVisibility(View.VISIBLE);
                            } else {
                                fabEditBudget.setVisibility(View.GONE);
                                fabAddReminder.setVisibility(View.GONE);
                            }


                        } else {
                            Log.e("BudgetMovements", "No se encontró el presupuesto con id: " + budgetId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("BudgetMovements", "Error cargando presupuesto: " + error.getMessage());
                    }
                });
    }



    private void loadUsers() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userIdToNameMap.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    User u = child.getValue(User.class);
                    if (u != null) userIdToNameMap.put(u.getUid(), u.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadMovements() {
        if (budgetId == null) return;

        // No borramos userIdToNameMap aquí: podemos reutilizar caché si ya cargamos usuarios.
        // Reiniciamos sólo las listas de movimientos para evitar duplicados.
        movementList.clear();
        filteredList.clear();

        movementService.getReference()
                .orderByChild("linkedBudgetId")
                .equalTo(budgetId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        List<Movement> allMovements = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Movement m = ds.getValue(Movement.class);
                            if (m != null) allMovements.add(m);
                        }

                        // Si no hay movimientos
                        if (allMovements.isEmpty()) {
                            movementList.clear();
                            filteredList.clear();
                            if (adapter != null) adapter.notifyDataSetChanged();
                            tvEmptyState.setVisibility(View.VISIBLE);
                            recalculateBudgetSpent();
                            updateProgressBar();
                            return;
                        }

                        // Detectar userIds que faltan en cache
                        Set<String> missingUserIds = new HashSet<>();
                        for (Movement m : allMovements) {
                            if (m.getUserId() != null && !userIdToNameMap.containsKey(m.getUserId())) {
                                missingUserIds.add(m.getUserId());
                            }
                        }

                        if (missingUserIds.isEmpty()) {
                            // Ya tenemos todos los nombres: asignamos de una vez
                            movementList.clear();
                            movementList.addAll(allMovements);

                            // Asignar nombres al adapter (si lo requiere)
                            if (adapter != null) {
                                for (Movement mv : movementList) {
                                    String name = userIdToNameMap.getOrDefault(mv.getUserId(), "Desconocido");
                                    adapter.setUserNameForMovement(mv.getId(), name);
                                }
                            }

                            filteredList.clear();
                            filteredList.addAll(movementList);
                            if (adapter != null) adapter.notifyDataSetChanged();
                            tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);

                            recalculateBudgetSpent();
                            updateProgressBar();
                        } else {
                            // Tenemos que cargar los usuarios faltantes y esperar a que terminen todas las cargas
                            AtomicInteger counter = new AtomicInteger(missingUserIds.size());

                            for (String userId : missingUserIds) {
                                userService.getById(userId).get()
                                        .addOnSuccessListener(userSnap -> {
                                            if (userSnap.exists()) {
                                                User u = userSnap.getValue(User.class);
                                                if (u != null) {
                                                    userIdToNameMap.put(userId, u.getName());
                                                }
                                            }
                                            // Si falla o no existe, dejamos sin nombre y seguiremos
                                            if (counter.decrementAndGet() <= 0) {
                                                // Todas las cargas terminadas: asignar la lista de movimientos de una vez
                                                movementList.clear();
                                                movementList.addAll(allMovements);

                                                if (adapter != null) {
                                                    for (Movement mv : movementList) {
                                                        String name = userIdToNameMap.getOrDefault(mv.getUserId(), "Desconocido");
                                                        adapter.setUserNameForMovement(mv.getId(), name);
                                                    }
                                                }

                                                filteredList.clear();
                                                filteredList.addAll(movementList);
                                                if (adapter != null) adapter.notifyDataSetChanged();
                                                tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);

                                                recalculateBudgetSpent();
                                                updateProgressBar();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            // Incluso si falla una petición, decrementamos el contador para no bloquear
                                            if (counter.decrementAndGet() <= 0) {
                                                movementList.clear();
                                                movementList.addAll(allMovements);

                                                if (adapter != null) {
                                                    for (Movement mv : movementList) {
                                                        String name = userIdToNameMap.getOrDefault(mv.getUserId(), "Desconocido");
                                                        adapter.setUserNameForMovement(mv.getId(), name);
                                                    }
                                                }

                                                filteredList.clear();
                                                filteredList.addAll(movementList);
                                                if (adapter != null) adapter.notifyDataSetChanged();
                                                tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);

                                                recalculateBudgetSpent();
                                                updateProgressBar();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("BudgetMovements", "Error cargando movimientos: " + error.getMessage());
                    }
                });
    }

    // ----------------------------
    // FILTERS
    // ----------------------------

    private void clearFilters() {
        etUserFilter.setText("");
        etStartDateFilter.setText("");
        etEndDateFilter.setText("");


        applyFilters();
    }



    private void applyFilters() {
        filterUser = etUserFilter.getText().toString().trim().toLowerCase();
        filterDateFrom = etStartDateFilter.getText().toString().trim();
        filterDateTo = etEndDateFilter.getText().toString().trim();

        filteredList.clear();

        for (Movement m : movementList) {
            boolean ok = true;

            // Filtrar por fecha desde
            if (!filterDateFrom.isEmpty() && m.getDate().compareTo(filterDateFrom) < 0)
                ok = false;

            // Filtrar por fecha hasta
            if (!filterDateTo.isEmpty() && m.getDate().compareTo(filterDateTo) > 0)
                ok = false;

            // Filtrar por usuario
            if (!filterUser.isEmpty()) {
                String name = userIdToNameMap.get(m.getUserId());
                if (name == null || !name.toLowerCase().contains(filterUser))
                    ok = false;
            }

            if (ok) filteredList.add(m);
        }

        // Asignar nombres al adapter
        for (Movement mv : filteredList) {
            String name = userIdToNameMap.getOrDefault(mv.getUserId(), "Desconocido");
            adapter.setUserNameForMovement(mv.getId(), name);
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }


    // ----------------------------
    // BUDGET LOGIC
    // ----------------------------

    private void recalculateBudgetSpent() {
        if (currentBudget == null) return;

        double spent = 0;
        for (Movement m : movementList) {
            if (budgetId.equals(m.getLinkedBudgetId())) {
                spent += m.getAmount();
            }
        }

        currentBudget.setSpent(spent);

        double remaining = currentBudget.getLimit() - spent;

        tvSpent.setText(String.format(Locale.getDefault(), " — Te quedan %.2f€", remaining));

        if (spent > currentBudget.getLimit()) {
            tvSpent.setTextColor(getColor(R.color.red_error));
            Toast.makeText(this, "⚠ Has superado tu presupuesto", Toast.LENGTH_SHORT).show();
        } else {
            tvSpent.setTextColor(getColor(R.color.gray_dark));
        }

        double percent = (spent / currentBudget.getLimit()) * 100;
        if (percent > 100) percent = 100;

        progressTop.setProgress((int) percent);
        tvPercent.setText(String.format(Locale.getDefault(), "%.0f%%", percent));

        // 🔥 ACTUALIZAR EN FIREBASE
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("budgets")
                .child(currentBudget.getId()); // o budgetId

        ref.setValue(currentBudget)
                .addOnSuccessListener(aVoid -> {
                    // Opcional: Log o Toast silencioso
                    // Log.d("Budget", "Presupuesto actualizado correctamente en Firebase");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar el presupuesto", Toast.LENGTH_SHORT).show()
                );
    }



    private void updateProgressBar() {
        if (currentBudget == null) return;

        double percent = (currentBudget.getSpent() / currentBudget.getLimit()) * 100;
        if (percent > 100) percent = 100;

        progressTop.setProgress((int) percent);
        tvPercent.setText(String.format(Locale.getDefault(), "%.0f%%", percent));
    }
}
