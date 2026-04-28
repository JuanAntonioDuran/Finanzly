package com.example.finanzly.dialogs;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import com.example.finanzly.R;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.Movement;
import com.example.finanzly.models.User;
import com.example.finanzly.services.MovementService;
import com.example.finanzly.services.UserService;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.*;

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
    private UserService userService;

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

        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_movement_form, null);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnType = dialogView.findViewById(R.id.btnType);

        Spinner spUserAssign = dialogView.findViewById(R.id.spUserAssign);
        TextView tvUserLabel = dialogView.findViewById(R.id.tvUserLabel);

        movementService = new MovementService(context);
        userService = new UserService(context);

        String currentUserId = FirebaseAuth.getInstance().getUid();
        final String[] selectedUserId = {currentUserId};

        if (isNew) {
            tvDialogTitle.setText("Nuevo movimiento");
            btnSave.setText("Guardar");

            // 🔥 FECHA POR DEFECTO (HOY)
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());
            etDate.setText(today);

        } else {
            tvDialogTitle.setText("Editar movimiento");
            btnSave.setText("Actualizar");

            etDate.setText(m.getDate());
            etAmount.setText(String.valueOf(m.getAmount()));
            etDescription.setText(m.getDescription());

            selectedUserId[0] = m.getUserId();
        }

        //  BOTÓN TIPO (solo Goal)
        if (currentGoal != null) {

            btnType.setVisibility(View.VISIBLE);

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
            m.setType("expense");
        }

        //  SPINNER SOLO PARA OWNER
        String ownerId = currentBudget != null
                ? currentBudget.getUserId()
                : currentGoal != null
                ? currentGoal.getUserId()
                : null;

        boolean isOwner = ownerId != null && ownerId.equals(currentUserId);

        if (isOwner) {

            spUserAssign.setVisibility(View.VISIBLE);
            tvUserLabel.setVisibility(View.VISIBLE);

            loadUsersForSpinner(spUserAssign, selectedUserId);

        } else {

            spUserAssign.setVisibility(View.GONE);
            tvUserLabel.setVisibility(View.GONE);
        }

        //  DATE PICKER
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();

            new DatePickerDialog(
                    context,
                    (view, year, month, day) ->
                            etDate.setText(String.format(
                                    Locale.getDefault(),
                                    "%04d-%02d-%02d",
                                    year,
                                    month + 1,
                                    day
                            )),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
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

            m.setDate(date);
            m.setDescription(desc);
            m.setAmount(amount);
            m.setUserId(selectedUserId[0]);

            if (currentBudget != null) {
                m.setLinkedBudgetId(linkedBudgetId);
                m.setCategory(currentBudget.getCategory());
            } else if (currentGoal != null) {
                m.setLinkedGoalId(linkedGoalId);
                m.setCategory(currentGoal.getTitle());
            }

            if (isNew) {
                String id = movementService.insert(m);
                m.setId(id);
            } else {
                movementService.update(m.getId(), m);
            }

            if (listener != null) listener.onMovementSaved(m);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadUsersForSpinner(
            Spinner spinner,
            String[] selectedUserIdRef
    ) {

        Set<String> userIds = new HashSet<>();

        if (currentBudget != null) {
            userIds.add(currentBudget.getUserId());
            if (currentBudget.getSharedUserIds() != null)
                userIds.addAll(currentBudget.getSharedUserIds());
        }

        if (currentGoal != null) {
            userIds.add(currentGoal.getUserId());
            if (currentGoal.getSharedUserIds() != null)
                userIds.addAll(currentGoal.getSharedUserIds());
        }

        List<String> names = new ArrayList<>();
        Map<String, String> map = new HashMap<>();

        for (String uid : userIds) {

            userService.getById(uid).get()
                    .addOnSuccessListener(snapshot -> {

                        if (!snapshot.exists()) return;

                        User u = snapshot.getValue(User.class);
                        if (u == null) return;

                        names.add(u.getName());
                        map.put(u.getName(), u.getUid());

                        if (names.size() == userIds.size()) {

                            ArrayAdapter<String> adapter =
                                    new ArrayAdapter<>(
                                            context,
                                            android.R.layout.simple_spinner_item,
                                            names
                                    );

                            adapter.setDropDownViewResource(
                                    android.R.layout.simple_spinner_dropdown_item
                            );

                            spinner.setAdapter(adapter);

                            for (int i = 0; i < names.size(); i++) {

                                String name = names.get(i);
                                String uidFromMap = map.get(name);

                                if (uidFromMap != null &&
                                        uidFromMap.equals(selectedUserIdRef[0])) {

                                    spinner.setSelection(i);
                                    break;
                                }
                            }

                            spinner.setOnItemSelectedListener(
                                    new AdapterView.OnItemSelectedListener() {

                                        @Override
                                        public void onItemSelected(
                                                AdapterView<?> parent,
                                                View view,
                                                int position,
                                                long id) {

                                            String name =
                                                    (String) parent.getItemAtPosition(position);

                                            selectedUserIdRef[0] =
                                                    map.get(name);
                                        }

                                        @Override
                                        public void onNothingSelected(
                                                AdapterView<?> parent) {
                                        }
                                    });
                        }
                    });
        }
    }
}