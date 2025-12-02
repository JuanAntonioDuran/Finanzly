package com.example.finanzly.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.finanzly.R;
import com.example.finanzly.models.Budget;
import com.example.finanzly.services.BudgetService;
import com.example.finanzly.services.UserService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class BudgetDialog {

    public interface OnBudgetSavedListener {
        void onBudgetSaved();
    }

    private final Context context;
    private final String currentUserId;
    private final BudgetService budgetService;
    private final UserService userService;
    private final OnBudgetSavedListener listener;

    public BudgetDialog(Context context, String currentUserId,
                        BudgetService budgetService, UserService userService,
                        OnBudgetSavedListener listener) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.budgetService = budgetService;
        this.userService = userService;
        this.listener = listener;
    }

    public void show(Budget budgetToEdit) {
        boolean isEditing = budgetToEdit != null;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_budget_form, null);

        EditText etCategory = dialogView.findViewById(R.id.etCategory);
        EditText etLimit = dialogView.findViewById(R.id.etLimit);

        // Si estamos editando, cargar datos existentes
        if (isEditing) {
            etCategory.setText(budgetToEdit.getCategory());
            etLimit.setText(String.valueOf(budgetToEdit.getLimit()));
        }

        new AlertDialog.Builder(context)
                .setTitle(isEditing ? "Editar presupuesto" : "Nuevo presupuesto")
                .setView(dialogView)
                .setPositiveButton(isEditing ? "Actualizar" : "Guardar", (dialog, which) -> {
                    String category = etCategory.getText().toString().trim();
                    String limitStr = etLimit.getText().toString().trim();

                    if (category.isEmpty() || limitStr.isEmpty()) {
                        Toast.makeText(context, "Debes llenar todos los campos.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double limit;
                    try {
                        limit = Double.parseDouble(limitStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Límite inválido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (limit <= 0) {
                        Toast.makeText(context, "Debe ser mayor que 0.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String currentDate = getCurrentUTCDate();

                    if (isEditing) {
                        budgetToEdit.setCategory(category);
                        budgetToEdit.setLimit(limit);
                        budgetToEdit.setUpdatedAt(currentDate);
                        budgetService.update(budgetToEdit.getId(), budgetToEdit);

                        Toast.makeText(context, "Presupuesto actualizado correctamente.", Toast.LENGTH_SHORT).show();
                    } else {
                        Budget newBudget = new Budget();
                        newBudget.setCategory(category);
                        newBudget.setLimit(limit);
                        newBudget.setSpent(0);
                        newBudget.setUserId(currentUserId);
                        newBudget.setCreatedAt(currentDate);

                        budgetService.insert(newBudget);
                        Toast.makeText(context, "Presupuesto creado correctamente.", Toast.LENGTH_SHORT).show();
                    }

                    if (listener != null) listener.onBudgetSaved();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String getCurrentUTCDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}
