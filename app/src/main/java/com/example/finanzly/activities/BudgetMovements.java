package com.example.finanzly.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.adapters.MovementAdapter;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Movement;
import com.example.finanzly.models.User;
import com.example.finanzly.services.BudgetService;
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

public class BudgetMovements extends AppCompatActivity {

    private RecyclerView recyclerViewMovements;
    private MovementAdapter adapter;
    private List<Movement> movementList;
    private MovementService movementService;
    private BudgetService budgetService;
    private UserService userService;

    private TextView tvEmptyState;
    private TextView tvSpent;
    private TextView tvPercent;
    private TextView tvTitle;
    private ProgressBar progressTop;

    private String budgetId;
    private Budget currentBudget;

    private ImageButton btnBack;
    private FloatingActionButton fabAddMovement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_budget_movements);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicialización
        recyclerViewMovements = findViewById(R.id.recyclerViewMovements);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvSpent = findViewById(R.id.tvSpent);
        tvPercent = findViewById(R.id.tvPercent);
        progressTop = findViewById(R.id.progressTop);
        tvTitle = findViewById(R.id.tvTitle);

        btnBack = findViewById(R.id.btnBack);
        fabAddMovement = findViewById(R.id.fabAddMovement);

        movementService = new MovementService(this);
        budgetService = new BudgetService(this);
        userService = new UserService(this);

        movementList = new ArrayList<>();
        adapter = new MovementAdapter(this, movementList);
        recyclerViewMovements.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMovements.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        fabAddMovement.setOnClickListener(v -> openMovementDialog(null));

        // Recibir ID del presupuesto
        budgetId = getIntent().getStringExtra("budgetId");
        if (budgetId == null) {
            Toast.makeText(this, "No se recibió budgetId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Cargar presupuesto
        budgetService.getById(budgetId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentBudget = snapshot.getValue(Budget.class);

                    if (currentBudget != null && currentBudget.getCategory() != null) {
                        tvTitle.setText("Movimientos de \"" + currentBudget.getCategory() + "\"");
                    }

                    updateSpentText();
                    updateProgressBar();
                    loadMovements();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        adapter.setOnMovementActionListener(new MovementAdapter.OnMovementActionListener() {
            @Override
            public void onEdit(Movement movement) {
                openMovementDialog(movement);
            }

            @Override
            public void onDelete(Movement movement) {
                deleteMovement(movement);
            }
        });
    }

    private void loadMovements() {
        movementService.getReference()
                .orderByChild("linkedBudgetId")
                .equalTo(budgetId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        movementList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Movement movement = ds.getValue(Movement.class);
                            if (movement != null) {
                                movementList.add(movement);

                                userService.getById(movement.getUserId()).get()
                                        .addOnSuccessListener(userSnap -> {
                                            if (userSnap.exists()) {
                                                User u = userSnap.getValue(User.class);
                                                if (u != null) {
                                                    adapter.setUserNameForMovement(movement.getId(), u.getName());
                                                }
                                            }
                                        });
                            }
                        }
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(movementList.isEmpty() ? View.VISIBLE : View.GONE);

                        recalculateBudgetSpent();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void openMovementDialog(Movement movement) {
        boolean isNew = movement == null;
        Movement m = isNew ? new Movement() : movement;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_movementbudget_form, null);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (!isNew) {
            etDate.setText(m.getDate());
            etAmount.setText(String.valueOf(m.getAmount()));
            etDescription.setText(m.getDescription());
        }

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (view, y, mo, d) -> etDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, mo + 1, d)),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
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

            if (isNew) {
                // Ajustar límite al crear
                double maxAllowed = currentBudget.getLimit() - currentBudget.getSpent();
                if (amount > maxAllowed) {
                    amount = maxAllowed;
                    Toast.makeText(this, "El gasto se ajustó al límite restante del presupuesto", Toast.LENGTH_LONG).show();
                }
            } else {
                // Validar límite al editar
                double currentSpentWithoutThis = currentBudget.getSpent() - m.getAmount();
                double maxAllowed = currentBudget.getLimit() - currentSpentWithoutThis;
                if (amount > maxAllowed) {
                    Toast.makeText(this, "No se puede actualizar: el gasto excede el límite del presupuesto", Toast.LENGTH_LONG).show();
                    return; // No permite guardar
                }
            }

            m.setId(isNew ? UUID.randomUUID().toString() : m.getId());
            m.setType("expense");
            m.setDate(date);
            m.setDescription(desc);
            m.setAmount(amount);
            m.setLinkedBudgetId(budgetId);
            m.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());

            // Asignar categoría del presupuesto al movimiento
            if (currentBudget != null) {
                m.setCategory(currentBudget.getCategory());
            }

            if (isNew) movementService.insert(m);
            else movementService.update(m.getId(), m);

            recalculateBudgetSpent();
            updateProgressBar();

            dialog.dismiss();
            Toast.makeText(this, isNew ? "Gasto creado" : "Gasto actualizado", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void deleteMovement(Movement movement) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar gasto")
                .setMessage("¿Seguro que quieres eliminar este gasto?")
                .setPositiveButton("Sí", (d, w) -> {
                    movementService.delete(movement.getId());
                    recalculateBudgetSpent();
                    updateProgressBar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void recalculateBudgetSpent() {
        movementService.getReference()
                .orderByChild("linkedBudgetId")
                .equalTo(budgetId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    double total = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Movement m = ds.getValue(Movement.class);
                        if (m != null && m.getType().equals("expense")) {
                            total += m.getAmount();
                        }
                    }

                    HashMap<String, Object> updates = new HashMap<>();
                    updates.put("spent", total);
                    budgetService.updatePartial(budgetId, updates);

                    currentBudget.setSpent(total);

                    updateSpentText();
                    updateProgressBar();
                });
    }

    private void updateSpentText() {
        if (currentBudget != null) {
            double remaining = currentBudget.getLimit() - currentBudget.getSpent();
            if (remaining <= 0) remaining = 0;
            tvSpent.setText(" — Te quedan " + remaining + "€");
        }
    }

    private void updateProgressBar() {
        if (currentBudget == null) return;

        double limit = currentBudget.getLimit();
        double spent = currentBudget.getSpent();

        if (limit <= 0) return;

        double percent = (spent / limit) * 100;
        if (percent > 100) percent = 100;

        progressTop.setProgress((int) percent);
        tvPercent.setText((int) percent + "%");
    }
}
