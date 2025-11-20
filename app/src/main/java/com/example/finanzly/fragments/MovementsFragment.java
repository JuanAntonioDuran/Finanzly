package com.example.finanzly.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.finanzly.R;
import com.example.finanzly.adapters.MovementAdapter;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.Movement;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MovementsFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovementAdapter adapter;
    private Button btnAdd, btnApplyFilter, btnClearFilter;
    private Spinner spinnerTypeFilter;
    private TextView tvEmptyState;
    private EditText etCategoryFilter;
    private EditText etDateFilter;

    private List<Movement> movementList = new ArrayList<>();
    private List<Movement> filteredList = new ArrayList<>();

    private DatabaseReference movementsRef;
    private static DatabaseReference budgetsRef;
    private static DatabaseReference goalsRef;
    private String uid;

    public MovementsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_movements, container, false);

        uid = FirebaseAuth.getInstance().getUid();
        movementsRef = FirebaseDatabase.getInstance().getReference("movements");
        budgetsRef = FirebaseDatabase.getInstance().getReference("budgets");
        goalsRef = FirebaseDatabase.getInstance().getReference("goals");

        // Views
        recyclerView = view.findViewById(R.id.recyclerViewMovements);
        btnAdd = view.findViewById(R.id.btnAddMovement);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnClearFilter = view.findViewById(R.id.btnClearFilter);
        spinnerTypeFilter = view.findViewById(R.id.spinnerTypeFilter);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        etCategoryFilter = view.findViewById(R.id.etCategoryFilter);

        // Spinner de filtro
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Todos", "Ingreso", "Gasto"}
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(filterAdapter);

        // RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MovementAdapter(getContext(), filteredList);
        adapter.setOnMovementActionListener(new MovementAdapter.OnMovementActionListener() {
            @Override
            public void onEdit(Movement movement) {
                openEditDialog(movement);
            }

            @Override
            public void onDelete(Movement movement) {
                deleteMovement(movement);
            }
        });
        recyclerView.setAdapter(adapter);

        // Botón agregar
        btnAdd.setOnClickListener(v -> openCreateDialog());

        // Filtros
        btnApplyFilter.setOnClickListener(v -> applyFilter());
        btnClearFilter.setOnClickListener(v -> clearFilter());

        etDateFilter = view.findViewById(R.id.etDateFilter);

        etDateFilter.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            try {
                String[] parts = etDateFilter.getText().toString().split("-");
                if (parts.length == 3) {
                    calendar.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                }
            } catch (Exception ignored) {}

            DatePickerDialog dialog = new DatePickerDialog(
                    getContext(),
                    (dView, year, month, day) -> {
                        // Formato yyyy-MM-dd
                        String formatted = String.format("%04d-%02d-%02d", year, month + 1, day);
                        etDateFilter.setText(formatted);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });





        loadMovements();

        return view;
    }

    // -------------------------------------------------------------
    // CARGAR MOVIMIENTOS
    // -------------------------------------------------------------
    private void loadMovements() {
        movementsRef.orderByChild("userId").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        movementList.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Movement m = child.getValue(Movement.class);
                            if (m != null) {
                                m.setId(child.getKey());
                                movementList.add(m);
                            }
                        }

                        if (isAdded()) applyFilter();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // -------------------------------------------------------------
    // FILTROS
    // -------------------------------------------------------------
    private void applyFilter() {
        if (!isAdded()) return;

        String categoryFilter = etCategoryFilter.getText().toString().trim().toLowerCase();
        String typeFilter = spinnerTypeFilter.getSelectedItem().toString();
        String dateFilter = etDateFilter.getText().toString().trim();

        filteredList.clear();

        for (Movement m : movementList) {

            boolean matchesCategory =
                    categoryFilter.isEmpty() ||
                            (m.getCategory() != null &&
                                    m.getCategory().toLowerCase().contains(categoryFilter));

            boolean matchesType =
                    typeFilter.equals("Todos") ||
                            (m.getType() != null && m.getType().equals(typeFilter.toLowerCase()));

            boolean matchesDate =
                    dateFilter.isEmpty() ||
                            (m.getDate() != null && m.getDate().equals(dateFilter));

            if (matchesCategory && matchesType && matchesDate) {
                filteredList.add(m);
            }
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }


    private void clearFilter() {
        etCategoryFilter.setText("");
        spinnerTypeFilter.setSelection(0);
        etDateFilter.setText("");

        filteredList.clear();
        filteredList.addAll(movementList);
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }


    // -------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------
    private void openCreateDialog() {
        MovementDialog dialog = new MovementDialog(getContext(), null,
                (movement, isEditing) -> saveNewMovement(movement));
        dialog.show();
    }

    private void saveNewMovement(Movement movement) {
        if (movement.getUserId() == null || movement.getUserId().isEmpty()) {
            movement.setUserId(uid);
        }

        // Validar límites ANTES de escribir en Firebase
        checkBudgetAndGoalLimits(movement, true, success -> {
            if (!success) return;

            String id = movementsRef.push().getKey();
            movement.setId(id);
            if (id != null) {
                movementsRef.child(id).setValue(movement, (error, ref) -> {
                    if (error == null && isAdded()) {
                        // Aplicar efecto (sube spent / actualiza goal)
                        applyEffectOfMovement(movement);
                        applyFilter();
                    }
                });
            }
        });
    }

    private void openEditDialog(Movement movement) {
        // Creamos una copia manual del movimiento para no modificar el original antes de guardar
        Movement copy = copyMovement(movement);

        MovementDialog dialog = new MovementDialog(getContext(), copy,
                (updatedMovement, isEditing) -> saveEditedMovement(updatedMovement));
        dialog.show();
    }

    /**
     * Método auxiliar para clonar un Movement manualmente.
     */
    private Movement copyMovement(Movement original) {
        if (original == null) return null;

        Movement copy = new Movement();
        copy.setId(original.getId());
        copy.setUserId(original.getUserId());
        copy.setType(original.getType());
        copy.setCategory(original.getCategory());
        copy.setAmount(original.getAmount());
        copy.setDate(original.getDate());
        copy.setDescription(original.getDescription());
        copy.setLinkedBudgetId(original.getLinkedBudgetId());
        copy.setLinkedGoalId(original.getLinkedGoalId());

        return copy;
    }


    private void saveEditedMovement(Movement movement) {
        if (movement.getId() == null) return;

        movementsRef.child(movement.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Movement old = snapshot.getValue(Movement.class);
                if (old != null) {
                    old.setId(snapshot.getKey());

                    reverseEffectOfMovement(old, () -> {
                        // Antes de guardar, actualizamos la categoría según el nuevo linkedBudget o linkedGoal
                        if (movement.getLinkedBudgetId() != null) {
                            budgetsRef.child(movement.getLinkedBudgetId())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot budgetSnapshot) {
                                            Budget b = budgetSnapshot.getValue(Budget.class);
                                            if (b != null) {
                                                movement.setCategory(b.getCategory()); // Actualizamos categoría
                                                // Validar límites con la nueva versión del movimiento (aplicar)
                                                checkBudgetAndGoalLimits(movement, true, success -> {
                                                    if (success) {
                                                        saveMovementToFirebase(movement);
                                                    } else {
                                                        // si no válido, re-aplicamos el efecto viejo para no dejar inconsistencias
                                                        applyEffectOfMovement(old);
                                                    }
                                                });
                                            } else {
                                                // si budget desapareció, cancelar y re-aplicar old
                                                applyEffectOfMovement(old);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            applyEffectOfMovement(old);
                                        }
                                    });
                        } else if (movement.getLinkedGoalId() != null) {
                            goalsRef.child(movement.getLinkedGoalId())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot goalSnapshot) {
                                            Goal g = goalSnapshot.getValue(Goal.class);
                                            if (g != null) {
                                                movement.setCategory(g.getTitle()); // Actualizamos categoría
                                                checkBudgetAndGoalLimits(movement, true, success -> {
                                                    if (success) {
                                                        saveMovementToFirebase(movement);
                                                    } else {
                                                        applyEffectOfMovement(old);
                                                    }
                                                });
                                            } else {
                                                applyEffectOfMovement(old);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            applyEffectOfMovement(old);
                                        }
                                    });
                        } else {
                            // No está vinculado a nada, guardamos directamente
                            saveMovementToFirebase(movement);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Método auxiliar para guardar en Firebase
    private void saveMovementToFirebase(Movement movement) {
        movementsRef.child(movement.getId()).setValue(movement, (error, ref) -> {
            if (error == null && isAdded()) {
                applyEffectOfMovement(movement);
                applyFilter();
            }
        });
    }


    // Interfaz callback para comprobaciones asíncronas
    private interface LimitCallback {
        void onResult(boolean success);
    }

    /**
     * Comprueba si se puede aplicar (isApplying==true) o revertir (isApplying==false) el movimiento
     * respetando:
     *  - budgets: solo expenses; spent no puede superar limit ni quedar < 0
     *  - goals: currentAmount no puede quedar < 0 ni superar targetAmount
     */
    private void checkBudgetAndGoalLimits(Movement movement, boolean isApplying, LimitCallback callback) {
        if (movement.getLinkedBudgetId() != null) {
            budgetsRef.child(movement.getLinkedBudgetId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Budget b = snapshot.getValue(Budget.class);
                            if (b == null) {
                                // si no existe el budget, rechazamos
                                Toast.makeText(getContext(), "Presupuesto no encontrado", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                                return;
                            }

                            double currentSpent = b.getSpent();
                            double amt = movement.getAmount();

                            // presupuesto solo acepta expenses
                            if (!"expense".equals(movement.getType())) {
                                Toast.makeText(getContext(), "Solo se pueden registrar gastos en un presupuesto", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                                return;
                            }

                            double newSpent;
                            if (isApplying) {
                                // aplicar movimiento: spent + amount
                                newSpent = currentSpent + amt;
                            } else {
                                // revertir (baja el spent): spent - amount
                                newSpent = currentSpent - amt;
                            }

                            // spent no puede ser negativo
                            if (newSpent < 0) {
                                Toast.makeText(getContext(), "Operación inválida: el presupuesto quedaría negativo", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                                return;
                            }

                            // al aplicar no puede exceder el límite
                            if (isApplying && newSpent > b.getLimit()) {
                                Toast.makeText(getContext(), "No puedes exceder el presupuesto", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                                return;
                            }

                            callback.onResult(true);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onResult(false);
                        }
                    });
        } else if (movement.getLinkedGoalId() != null) {
            goalsRef.child(movement.getLinkedGoalId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Goal g = snapshot.getValue(Goal.class);
                            if (g == null) {
                                Toast.makeText(getContext(), "Meta no encontrada", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                                return;
                            }

                            double current = g.getCurrentAmount();
                            double amt = movement.getAmount();

                            // delta cuando se APLICA este movimiento (aplicar): expense => -amt, income => +amt
                            double applyDelta = "expense".equals(movement.getType()) ? -amt : amt;

                            double newCurrent;
                            if (isApplying) {
                                newCurrent = current + applyDelta;
                            } else {
                                // si revertimos (eliminar/revertir), restamos el delta aplicado anteriormente
                                newCurrent = current - applyDelta;
                            }

                            if (newCurrent < 0) {
                                Toast.makeText(getContext(), "Operación inválida: la meta quedaría en negativo", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                                return;
                            }
                            if (newCurrent > g.getTargetAmount()) {
                                Toast.makeText(getContext(), "Operación inválida: superarías la meta", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                                return;
                            }

                            callback.onResult(true);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onResult(false);
                        }
                    });
        } else {
            // no linked -> siempre permitido
            callback.onResult(true);
        }
    }


    private void deleteMovement(Movement movement) {
        if (movement == null || movement.getId() == null) return;

        // Antes de borrar, comprobamos que revirtiendo el movimiento no rompa límites (p.ej. meta < 0)
        checkBudgetAndGoalLimits(movement, false, canDelete -> {
            if (!canDelete) return;

            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Estás seguro de que quieres eliminar este movimiento?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        movementsRef.child(movement.getId()).removeValue((error, ref) -> {
                            if (error == null) {
                                // Revertimos efectos en budget/goal
                                updateBudgetOrGoalDelete(movement);
                                if (isAdded()) applyFilter();
                                Toast.makeText(getContext(), "Movimiento eliminado", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                    .show();
        });
    }

    // -------------------------------------------------------------
    // ACTUALIZAR BUDGET O GOAL
    // -------------------------------------------------------------
    private void applyEffectOfMovement(Movement movement) {
        if (movement.getLinkedBudgetId() != null) {
            budgetsRef.child(movement.getLinkedBudgetId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Budget b = snapshot.getValue(Budget.class);
                            if (b != null) {
                                b.setId(snapshot.getKey());

                                // Asegurarse de que solo sean gastos
                                if (!"expense".equals(movement.getType())) {
                                    // guard: no aplicar
                                    return;
                                }

                                double newSpent = b.getSpent() + movement.getAmount();
                                if (newSpent < 0) newSpent = 0;
                                if (newSpent > b.getLimit()) {
                                    // guard: no aplicar
                                    return;
                                }

                                b.setSpent(newSpent);
                                movement.setCategory(b.getCategory()); // Actualizamos la categoría al nombre del presupuesto
                                budgetsRef.child(b.getId()).setValue(b);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        if (movement.getLinkedGoalId() != null) {
            goalsRef.child(movement.getLinkedGoalId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Goal g = snapshot.getValue(Goal.class);
                            if (g != null) {
                                g.setId(snapshot.getKey());

                                double newCurrent = g.getCurrentAmount();
                                if ("expense".equals(movement.getType())) {
                                    newCurrent -= movement.getAmount();
                                } else {
                                    newCurrent += movement.getAmount();
                                }

                                if (newCurrent < 0) {
                                    // guard: no aplicar
                                    return;
                                }

                                if (newCurrent > g.getTargetAmount()) {
                                    // guard: no aplicar
                                    return;
                                }

                                g.setCurrentAmount(newCurrent);
                                movement.setCategory(g.getTitle()); // Actualizamos la categoría al título de la meta
                                goalsRef.child(g.getId()).setValue(g);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }


    private void reverseEffectOfMovement(Movement movement, Runnable callback) {
        if (movement.getLinkedBudgetId() != null) {
            budgetsRef.child(movement.getLinkedBudgetId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Budget b = snapshot.getValue(Budget.class);
                            if (b != null) {
                                b.setId(snapshot.getKey());
                                double amount = "expense".equals(movement.getType()) ? movement.getAmount() : -movement.getAmount();
                                b.setSpent(b.getSpent() - amount);
                                // evitar spent < 0
                                if (b.getSpent() < 0) b.setSpent(0);
                                budgetsRef.child(b.getId()).setValue(b)
                                        .addOnCompleteListener(task -> callback.run());
                            } else callback.run();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { callback.run(); }
                    });
        } else callback.run();

        if (movement.getLinkedGoalId() != null) {
            goalsRef.child(movement.getLinkedGoalId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Goal g = snapshot.getValue(Goal.class);
                            if (g != null) {
                                g.setId(snapshot.getKey());
                                double amount = "expense".equals(movement.getType()) ? -movement.getAmount() : movement.getAmount();
                                g.setCurrentAmount(g.getCurrentAmount() - amount);
                                if (g.getCurrentAmount() < 0) g.setCurrentAmount(0);
                                goalsRef.child(g.getId()).setValue(g)
                                        .addOnCompleteListener(task -> callback.run());
                            } else callback.run();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { callback.run(); }
                    });
        }
    }

    private void updateBudgetOrGoalDelete(Movement movement) {
        if (movement.getLinkedBudgetId() != null) {
            budgetsRef.child(movement.getLinkedBudgetId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Budget b = snapshot.getValue(Budget.class);
                            if (b != null) {
                                b.setId(snapshot.getKey());
                                double amount = "expense".equals(movement.getType()) ? movement.getAmount() : -movement.getAmount();
                                b.setSpent(b.getSpent() - amount);
                                if (b.getSpent() < 0) b.setSpent(0);
                                budgetsRef.child(b.getId()).setValue(b);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        if (movement.getLinkedGoalId() != null) {
            goalsRef.child(movement.getLinkedGoalId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Goal g = snapshot.getValue(Goal.class);
                            if (g != null) {
                                g.setId(snapshot.getKey());
                                double amount = "expense".equals(movement.getType()) ? -movement.getAmount() : movement.getAmount();
                                g.setCurrentAmount(g.getCurrentAmount() - amount);
                                if (g.getCurrentAmount() < 0) g.setCurrentAmount(0);
                                goalsRef.child(g.getId()).setValue(g);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    // -------------------------------------------------------------
    // DIALOG MOVEMENT (lo dejo abajo por claridad en la otra parte)
    // -------------------------------------------------------------
    // Coloca esta clase dentro de MovementsFragment (o en su propio fichero si prefieres).
    private static class MovementDialog extends Dialog {

        public interface MovementCallback {
            void onMovementSaved(Movement movement, boolean isEditing);
        }

        private Movement movement;
        private MovementCallback callback;

        private EditText etDate, etAmount, etDescription;
        private Spinner spinnerType, spinnerTargetType, spinnerBudgets, spinnerGoals;
        private LinearLayout budgetRow, goalRow;
        private Button btnSave, btnCancel;

        private List<Budget> budgetList = new ArrayList<>();
        private List<Goal> goalList = new ArrayList<>();

        public MovementDialog(@NonNull Context context, Movement movement, MovementCallback callback) {
            super(context);
            this.movement = movement;
            this.callback = callback;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_movement_complete);

            // Views
            etDate = findViewById(R.id.etDate);

            // --- Abrir calendario al pulsar en la fecha ---
            etDate.setOnClickListener(v -> {
                final Calendar calendar = Calendar.getInstance();

                try {
                    String[] parts = etDate.getText().toString().split("-");
                    if (parts.length == 3) {
                        calendar.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                        calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                    }
                } catch (Exception ignored) {}

                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(getContext(),
                        (view, y, m, d) -> {
                            // Formato yyyy-MM-dd
                            String formatted = String.format("%04d-%02d-%02d", y, (m + 1), d);
                            etDate.setText(formatted);
                        },
                        year, month, day);

                dialog.show();
            });

            etAmount = findViewById(R.id.etAmount);
            etDescription = findViewById(R.id.etDescription);

            spinnerType = findViewById(R.id.spinnerType);
            spinnerTargetType = findViewById(R.id.spinnerTargetType);
            spinnerBudgets = findViewById(R.id.spinnerBudgets);
            spinnerGoals = findViewById(R.id.spinnerGoals);

            budgetRow = findViewById(R.id.budgetRow);
            goalRow = findViewById(R.id.goalRow);

            btnSave = findViewById(R.id.btnSave);
            btnCancel = findViewById(R.id.btnCancel);

            // Spinner tipo
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{"Ingreso", "Gasto"}
            );
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);

            // Spinner destino
            ArrayAdapter<String> targetTypeAdapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{"Presupuesto", "Meta"}
            );
            targetTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTargetType.setAdapter(targetTypeAdapter);

            // --- BLOQUEAR TIPO PARA PRESUPUESTOS ---
            spinnerTargetType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selected = spinnerTargetType.getSelectedItem().toString();

                    if (selected.equals("Presupuesto")) {
                        budgetRow.setVisibility(View.VISIBLE);
                        goalRow.setVisibility(View.GONE);
                        loadBudgets();

                        spinnerType.setSelection(1); // Gasto
                        spinnerType.setEnabled(false);
                        spinnerType.setAlpha(0.5f);

                    } else {
                        goalRow.setVisibility(View.VISIBLE);
                        budgetRow.setVisibility(View.GONE);
                        loadGoals();

                        spinnerType.setEnabled(true);
                        spinnerType.setAlpha(1f);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // Si editando rellenar datos
            if (movement != null) {
                etDate.setText(movement.getDate()); // yyyy-MM-dd
                etAmount.setText(String.valueOf(movement.getAmount()));
                etDescription.setText(movement.getDescription());

                spinnerType.setSelection("income".equals(movement.getType()) ? 0 : 1);

                if (movement.getLinkedBudgetId() != null)
                    spinnerTargetType.setSelection(0);
                else
                    spinnerTargetType.setSelection(1);
            }

            btnCancel.setOnClickListener(v -> dismiss());

            // Guardar
            btnSave.setOnClickListener(v -> {

                String date = etDate.getText().toString().trim(); // yyyy-MM-dd
                String desc = etDescription.getText().toString().trim();
                double amount = 0;

                try {
                    if (!etAmount.getText().toString().trim().isEmpty()) {
                        amount = Double.parseDouble(etAmount.getText().toString().trim());
                    }
                } catch (Exception ignored) {}

                // Tipo
                String type;
                if (spinnerTargetType.getSelectedItem().toString().equals("Presupuesto")) {
                    type = "expense"; // siempre gasto para presupuestos
                } else {
                    type = spinnerType.getSelectedItemPosition() == 0 ? "income" : "expense";
                }

                String linkedBudgetId = null;
                String linkedGoalId = null;
                String category = null;

                // --- PRESUPUESTO ---
                if (budgetRow.getVisibility() == View.VISIBLE &&
                        spinnerBudgets.getSelectedItemPosition() >= 0 &&
                        spinnerBudgets.getSelectedItemPosition() < budgetList.size()) {

                    Budget b = budgetList.get(spinnerBudgets.getSelectedItemPosition());
                    linkedBudgetId = b.getId();
                    category = b.getCategory();
                }

                // --- META ---
                else if (goalRow.getVisibility() == View.VISIBLE &&
                        spinnerGoals.getSelectedItemPosition() >= 0 &&
                        spinnerGoals.getSelectedItemPosition() < goalList.size()) {

                    Goal g = goalList.get(spinnerGoals.getSelectedItemPosition());
                    linkedGoalId = g.getId();
                    category = g.getTitle();
                }

                Movement m = movement != null ? movement : new Movement();
                m.setDate(date); // GUARDAR FECHA EN FORMATO yyyy-MM-dd
                m.setAmount(amount);
                m.setDescription(desc);
                m.setType(type);
                m.setLinkedBudgetId(linkedBudgetId);
                m.setLinkedGoalId(linkedGoalId);
                m.setCategory(category);

                callback.onMovementSaved(m, movement != null && movement.getId() != null);
                dismiss();
            });
        }

        // ------- LOAD BUDGETS -------
        private void loadBudgets() {
            budgetsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    budgetList.clear();
                    List<String> names = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Budget b = child.getValue(Budget.class);
                        if (b != null) {
                            b.setId(child.getKey());
                            boolean isOwner = b.getUserId() != null && b.getUserId().equals(FirebaseAuth.getInstance().getUid());
                            boolean isShared = b.getSharedUserIds() != null && b.getSharedUserIds().contains(FirebaseAuth.getInstance().getUid());
                            if (isOwner || isShared) {
                                budgetList.add(b);
                                names.add(b.getCategory() != null ? b.getCategory() : ("Presupuesto " + b.getId()));
                            }
                        }
                    }

                    spinnerBudgets.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, names));

                    if (movement != null && movement.getLinkedBudgetId() != null) {
                        for (int i = 0; i < budgetList.size(); i++) {
                            if (budgetList.get(i).getId().equals(movement.getLinkedBudgetId())) {
                                spinnerBudgets.setSelection(i);
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // ------- LOAD GOALS -------
        private void loadGoals() {
            goalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    goalList.clear();
                    List<String> names = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Goal g = child.getValue(Goal.class);
                        if (g != null) {
                            g.setId(child.getKey());
                            boolean isOwner = g.getUserId() != null && g.getUserId().equals(FirebaseAuth.getInstance().getUid());
                            boolean isShared = g.getSharedUserIds() != null && g.getSharedUserIds().contains(FirebaseAuth.getInstance().getUid());
                            if (isOwner || isShared) {
                                goalList.add(g);
                                names.add(g.getTitle() != null ? g.getTitle() : ("Meta " + g.getId()));
                            }
                        }
                    }

                    spinnerGoals.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, names));

                    if (movement != null && movement.getLinkedGoalId() != null) {
                        for (int i = 0; i < goalList.size(); i++) {
                            if (goalList.get(i).getId().equals(movement.getLinkedGoalId())) {
                                spinnerGoals.setSelection(i);
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }


}
