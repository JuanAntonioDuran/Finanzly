package com.example.finanzly.dialogs;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.finanzly.R;
import com.example.finanzly.models.Goal;
import com.example.finanzly.services.GoalService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class GoalDialog {

    public interface OnGoalCreatedListener {
        void onGoalCreated();
    }

    private Context context;
    private String currentUserId;
    private GoalService goalService;
    private OnGoalCreatedListener listener;

    private Goal existingGoal;

    public GoalDialog(Context context,
                      String currentUserId,
                      GoalService goalService,
                      OnGoalCreatedListener listener) {

        this.context = context;
        this.currentUserId = currentUserId;
        this.goalService = goalService;
        this.listener = listener;
    }

    public void setGoal(Goal goal) {
        this.existingGoal = goal;
    }

    public void show() {

        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_goal, null);

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etTargetAmount = dialogView.findViewById(R.id.etTargetAmount);
        EditText etDeadline = dialogView.findViewById(R.id.etDeadline);

        boolean isEdit = existingGoal != null;

        // -----------------------------
        //  FECHA POR DEFECTO (SOLO CREAR)
        // -----------------------------
        if (!isEdit) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());
            etDeadline.setText(today);
        }

        // -----------------------------
        // MODO EDICIÓN
        // -----------------------------
        if (isEdit) {
            etTitle.setText(existingGoal.getTitle());
            etTargetAmount.setText(String.valueOf(existingGoal.getTargetAmount()));
            etDeadline.setText(existingGoal.getDeadline());
        }

        // -----------------------------
        // DATE PICKER
        // -----------------------------
        etDeadline.setFocusable(false);
        etDeadline.setClickable(true);

        etDeadline.setOnClickListener(v -> {

            java.util.Calendar calendar = java.util.Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) ->
                            etDeadline.setText(
                                    String.format(
                                            Locale.getDefault(),
                                            "%04d-%02d-%02d",
                                            year,
                                            month + 1,
                                            dayOfMonth
                                    )
                            ),
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(isEdit ? "Actualizar" : "Guardar", null)
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String title = etTitle.getText().toString().trim();
            String targetStr = etTargetAmount.getText().toString().trim();
            String deadline = etDeadline.getText().toString().trim();

            if (title.isEmpty() || targetStr.isEmpty() || deadline.isEmpty()) {
                Toast.makeText(context, "Debes llenar todos los campos.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (title.length() < 3) {
                Toast.makeText(context, "El título debe tener al menos 3 caracteres.", Toast.LENGTH_SHORT).show();
                return;
            }

            double target;

            try {
                target = Double.parseDouble(targetStr);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Introduce un número válido.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (target <= 0) {
                Toast.makeText(context, "Debe ser mayor que 0.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEdit) {
                existingGoal.setTitle(title);
                existingGoal.setTargetAmount(target);
                existingGoal.setDeadline(deadline);

                goalService.update(existingGoal.getId(), existingGoal);

                Toast.makeText(context, "Meta actualizada.", Toast.LENGTH_SHORT).show();

            } else {
                String currentDate = getCurrentUTCDate();

                Goal newGoal = new Goal();
                newGoal.setTitle(title);
                newGoal.setTargetAmount(target);
                newGoal.setCurrentAmount(0);
                newGoal.setUserId(currentUserId);
                newGoal.setDeadline(deadline);
                newGoal.setCreatedAt(currentDate);
                newGoal.setSharedUserIds(List.of());

                goalService.insert(newGoal);

                Toast.makeText(context, "Meta creada correctamente.", Toast.LENGTH_SHORT).show();
            }

            if (listener != null) listener.onGoalCreated();

            dialog.dismiss();
        });
    }

    private String getCurrentUTCDate() {

        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.format(new Date());
    }
}