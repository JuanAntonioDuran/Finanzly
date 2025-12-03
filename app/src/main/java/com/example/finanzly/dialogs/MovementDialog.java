package com.example.finanzly.dialogs;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.finanzly.R;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.Movement;
import com.example.finanzly.services.MovementService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.Locale;

public class MovementDialog {

    public interface OnMovementSavedListener {
        void onMovementSaved(Movement movement);
    }

    private final Context context;
    private Budget currentBudget;
    private Goal currentGoal;
    private String linkedBudgetId;
    private String linkedGoalId;
    private OnMovementSavedListener listener;
    private MovementService movementService;

    public MovementDialog(Context context, OnMovementSavedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setBudget(Budget budget, String budgetId) {
        this.currentBudget = budget;
        this.linkedBudgetId = budgetId;
    }

    public void setGoal(Goal goal, String goalId) {
        this.currentGoal = goal;
        this.linkedGoalId = goalId;
    }

    public void open(Movement movement) {
        boolean isNew = movement == null;
        Movement m = isNew ? new Movement() : movement;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_movement_form, null);

        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnType = dialogView.findViewById(R.id.btnType); // Botón de tipo

        movementService = new MovementService(context);

        if (!isNew) {
            etDate.setText(m.getDate());
            etAmount.setText(String.valueOf(m.getAmount()));
            etDescription.setText(m.getDescription());
            btnType.setText(m.getType().equals("income") ? "Ingreso" : "Gasto");
        }

        // Mostrar el botón solo si es Goal
        if (currentGoal != null) {
            btnType.setVisibility(View.VISIBLE);
            // Inicializamos tipo si es nuevo
            if (isNew) m.setType("expense");
            btnType.setText(m.getType().equals("income") ? "Ingreso" : "Gasto");

            btnType.setOnClickListener(v -> {
                if (m.getType().equals("income")) {
                    m.setType("expense");
                    btnType.setText("Gasto");
                } else {
                    m.setType("income");
                    btnType.setText("Ingreso");
                }
            });
        } else {
            btnType.setVisibility(View.GONE);
            m.setType("expense"); // presupuestos siempre expense
        }

        // Selector de fecha
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(context, (view, year, month, day) ->
                    etDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String date = etDate.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

            if (date.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(context, "Fecha y cantidad obligatorias", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Cantidad inválida", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validaciones presupuesto/objetivo
            if (currentBudget != null) {
                double spentWithoutThis = isNew ? currentBudget.getSpent() : currentBudget.getSpent() - m.getAmount();
                double newTotal = spentWithoutThis + amount;

                if (newTotal > currentBudget.getLimit()) {
                    Toast.makeText(context, "Advertencia: has superado el límite de este presupuesto.", Toast.LENGTH_LONG).show();
                }
            }

            if (currentGoal != null) {
                double spentWithoutThis = isNew ? currentGoal.getCurrentAmount() : currentGoal.getCurrentAmount() - m.getAmount();
                double newTotal = spentWithoutThis + amount;

                // 🔹 Validación para que el goal no quede por debajo de 0
                if (m.getType().equals("expense") && (spentWithoutThis - amount) < 0) {
                    Toast.makeText(context, "No puedes gastar más de lo que tienes en este objetivo.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Validación límite máximo
                if (newTotal > currentGoal.getTargetAmount()) {
                    Toast.makeText(context, "No puedes superar el objetivo total.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Actualizar datos del movimiento
            m.setDate(date);
            m.setDescription(desc);
            m.setAmount(amount);
            // 🔹 El tipo se obtiene del botón de tipo
            m.setUserId(FirebaseAuth.getInstance().getUid());

            if (currentBudget != null) {
                m.setLinkedBudgetId(linkedBudgetId);
                m.setCategory(currentBudget.getCategory());
            } else if (currentGoal != null) {
                m.setLinkedGoalId(linkedGoalId);
                m.setCategory(currentGoal.getTitle());
            }

            // 🔹 Guardar usando insert o update
            if (isNew) {
                String id = movementService.insert(m);
                m.setId(id); // asegurarnos de asignar el ID
            } else {
                movementService.update(m.getId(), m);
            }

            // Notificar a la Activity
            if (listener != null) listener.onMovementSaved(m);

            dialog.dismiss();
        });


        dialog.show();
    }
}
