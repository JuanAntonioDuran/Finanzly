package com.example.finanzly.fragments;

import android.app.AlertDialog;
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
import java.util.List;

public class MovementsFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovementAdapter adapter;
    private Button btnAdd, btnApplyFilter, btnClearFilter;
    private Spinner spinnerTypeFilter;
    private TextView tvEmptyState;
    private EditText etCategoryFilter;

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

        String typeSelected = spinnerTypeFilter.getSelectedItem().toString();
        String categoryFilter = etCategoryFilter != null ? etCategoryFilter.getText().toString().trim().toLowerCase() : "";

        filteredList.clear();

        for (Movement m : movementList) {
            String typeInSpanish = m.getType() != null && m.getType().equals("income") ? "Ingreso" : "Gasto";

            boolean matchesType = typeSelected.equals("Todos") || typeInSpanish.equals(typeSelected);
            boolean matchesCategory = categoryFilter.isEmpty() ||
                    (m.getCategory() != null && m.getCategory().toLowerCase().contains(categoryFilter));

            if (matchesType && matchesCategory) filteredList.add(m);
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearFilter() {
        spinnerTypeFilter.setSelection(0);
        if (etCategoryFilter != null) etCategoryFilter.setText("");
        applyFilter();
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

        String id = movementsRef.push().getKey();
        movement.setId(id);
        if (id != null) {
            movementsRef.child(id).setValue(movement);
            updateBudgetOrGoalDelete(movement);

        }
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
                        // Antes de guardar, validar restricciones
                        checkBudgetAndGoalLimits(movement, success -> {
                            if (success) {
                                movementsRef.child(movement.getId()).setValue(movement, (error, ref) -> {
                                    if (error == null && isAdded()) {
                                        applyEffectOfMovement(movement);
                                        applyFilter();
                                    }
                                });
                            }
                        });
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private interface LimitCallback {
        void onResult(boolean success);
    }

    private void checkBudgetAndGoalLimits(Movement movement, LimitCallback callback) {
        if (movement.getLinkedBudgetId() != null) {
            budgetsRef.child(movement.getLinkedBudgetId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Budget b = snapshot.getValue(Budget.class);
                            if (b != null) {
                                double newSpent = b.getSpent() + movement.getAmount();
                                if (!"expense".equals(movement.getType())) {
                                    Toast.makeText(getContext(), "Solo se pueden registrar gastos en un presupuesto", Toast.LENGTH_SHORT).show();
                                    callback.onResult(false);
                                    return;
                                }
                                if (newSpent > b.getLimit()) {
                                    Toast.makeText(getContext(), "No puedes exceder el presupuesto", Toast.LENGTH_SHORT).show();
                                    callback.onResult(false);
                                    return;
                                }
                            }
                            callback.onResult(true);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { callback.onResult(false); }
                    });
        } else if (movement.getLinkedGoalId() != null) {
            goalsRef.child(movement.getLinkedGoalId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Goal g = snapshot.getValue(Goal.class);
                            if (g != null) {
                                double newCurrent = "expense".equals(movement.getType()) ? -movement.getAmount() : movement.getAmount();
                                newCurrent += g.getCurrentAmount();
                                if (newCurrent < 0) {
                                    Toast.makeText(getContext(), "No puedes dejar la meta en negativo", Toast.LENGTH_SHORT).show();
                                    callback.onResult(false);
                                    return;
                                }
                                if (newCurrent > g.getTargetAmount()) {
                                    Toast.makeText(getContext(), "No puedes superar la meta", Toast.LENGTH_SHORT).show();
                                    callback.onResult(false);
                                    return;
                                }
                            }
                            callback.onResult(true);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { callback.onResult(false); }
                    });
        } else {
            callback.onResult(true);
        }
    }



    private void deleteMovement(Movement movement) {
        if (movement == null || movement.getId() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar este movimiento?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    movementsRef.child(movement.getId()).removeValue((error, ref) -> {
                        if (error == null) {
                            updateBudgetOrGoalDelete(movement);
                            if (isAdded()) applyFilter();
                            Toast.makeText(getContext(), "Movimiento eliminado", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
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
                                    Toast.makeText(getContext(), "Solo se pueden registrar gastos en un presupuesto", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                double newSpent = b.getSpent() + movement.getAmount();
                                if (newSpent > b.getLimit()) {
                                    Toast.makeText(getContext(), "No puedes exceder el presupuesto", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(getContext(), "No puedes dejar la meta en negativo", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (newCurrent > g.getTargetAmount()) {
                                    Toast.makeText(getContext(), "No puedes superar la meta", Toast.LENGTH_SHORT).show();
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
                                goalsRef.child(g.getId()).setValue(g);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }





// -------------------------------------------------------------
// DIALOG MOVEMENT (adaptado a spinner en lugar de lista)
// -------------------------------------------------------------
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

            // Spinner tipo movimiento
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{"Ingreso", "Gasto"}
            );
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);

            // Spinner destino (Presupuesto / Meta)
            ArrayAdapter<String> targetTypeAdapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{"Presupuesto", "Meta"}
            );
            targetTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTargetType.setAdapter(targetTypeAdapter);

            spinnerTargetType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selected = spinnerTargetType.getSelectedItem().toString();
                    if (selected.equals("Presupuesto")) {
                        budgetRow.setVisibility(View.VISIBLE);
                        goalRow.setVisibility(View.GONE);
                        loadBudgets();
                    } else {
                        goalRow.setVisibility(View.VISIBLE);
                        budgetRow.setVisibility(View.GONE);
                        loadGoals();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // Rellenar campos si editando
            if (movement != null) {
                etDate.setText(movement.getDate());
                etAmount.setText(String.valueOf(movement.getAmount()));
                etDescription.setText(movement.getDescription());
                spinnerType.setSelection("income".equals(movement.getType()) ? 0 : 1);
                spinnerTargetType.setSelection(movement.getLinkedBudgetId() != null ? 0 : 1);
            }

            btnCancel.setOnClickListener(v -> dismiss());

            btnSave.setOnClickListener(v -> {
                String date = etDate.getText().toString().trim();
                String desc = etDescription.getText().toString().trim();
                double amount = 0;
                try {
                    String amtStr = etAmount.getText().toString().trim();
                    if (!amtStr.isEmpty()) amount = Double.parseDouble(amtStr);
                } catch (Exception ignored) {}

                String type = spinnerType.getSelectedItemPosition() == 0 ? "income" : "expense";
                String linkedBudgetId = null;
                String linkedGoalId = null;

                if (budgetRow.getVisibility() == View.VISIBLE && spinnerBudgets.getSelectedItemPosition() >= 0 && spinnerBudgets.getSelectedItemPosition() < budgetList.size()) {
                    linkedBudgetId = budgetList.get(spinnerBudgets.getSelectedItemPosition()).getId();
                } else if (goalRow.getVisibility() == View.VISIBLE && spinnerGoals.getSelectedItemPosition() >= 0 && spinnerGoals.getSelectedItemPosition() < goalList.size()) {
                    linkedGoalId = goalList.get(spinnerGoals.getSelectedItemPosition()).getId();
                }

                Movement m = movement != null ? movement : new Movement();
                m.setDate(date);
                m.setAmount(amount);
                m.setDescription(desc);
                m.setType(type);
                m.setLinkedBudgetId(linkedBudgetId);
                m.setLinkedGoalId(linkedGoalId);

                callback.onMovementSaved(m, movement != null && movement.getId() != null);
                dismiss();
            });
        }

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

                    // Seleccionar el presupuesto vinculado si editando
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

                    // Seleccionar la meta vinculada si editando
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
