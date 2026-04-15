package com.example.finanzly.dialogs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.finanzly.R;
import com.example.finanzly.models.Reminder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.*;

public class AddEditReminderDialog {

    public interface OnReminderSavedListener {
        void onReminderSaved(Reminder reminder, boolean isNew);
    }

    private final Context context;
    private final OnReminderSavedListener listener;

    private final String currentUserId;
    private final String linkedId;
    private final String linkedTitle;
    private final boolean isGoal;
    private final Reminder existing;

    private final List<String> linkedSharedUsers;
    private final Map<String, String> userIdToNameMap;

    private final DatabaseReference remindersRef =
            FirebaseDatabase.getInstance().getReference("reminders");

    private final List<String> selectedUserIds = new ArrayList<>();
    private final Map<String, Boolean> selectedStatus = new HashMap<>();

    public AddEditReminderDialog(Context ctx,
                                 Reminder existingReminder,
                                 String currentUserId,
                                 String linkedId,
                                 String linkedTitle,
                                 boolean isGoal,
                                 List<String> linkedSharedUsers,
                                 Map<String, String> userIdToNameMap,
                                 OnReminderSavedListener listener) {

        this.context = ctx;
        this.listener = listener;
        this.currentUserId = currentUserId;
        this.linkedId = linkedId;
        this.linkedTitle = linkedTitle;
        this.isGoal = isGoal;
        this.existing = existingReminder;
        this.linkedSharedUsers = linkedSharedUsers;
        this.userIdToNameMap = userIdToNameMap;
    }

    public void show() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_add_edit_reminder, null);
        builder.setView(view);

        EditText edtTitle = view.findViewById(R.id.edtReminderTitle);
        EditText edtDesc = view.findViewById(R.id.edtReminderDescription);
        EditText edtDate = view.findViewById(R.id.edtReminderDate);
        EditText edtTime = view.findViewById(R.id.edtReminderTime);

        Button btnToggleUsers = view.findViewById(R.id.btnToggleUsers);
        LinearLayout usersContainer = view.findViewById(R.id.usersContainer);

        TextView tvLinked = view.findViewById(R.id.tvLinkedResource);

        Button btnCancel = view.findViewById(R.id.btnCancelReminder);
        Button btnSave = view.findViewById(R.id.btnSaveReminder);

        tvLinked.setText(isGoal ? "Meta: " + linkedTitle : "Presupuesto: " + linkedTitle);

        Reminder working = existing != null ? existing : new Reminder();

        // 🔥 Estado persistente
        selectedUserIds.clear();
        selectedStatus.clear();

        if (existing != null && existing.getSharedUserIds() != null) {
            selectedUserIds.addAll(existing.getSharedUserIds());
        }

        if (existing != null && existing.getSharedUsersStatus() != null) {
            selectedStatus.putAll(existing.getSharedUsersStatus());
        }

        // -----------------------------
        // Modo edición / creación
        // -----------------------------
        if (existing != null) {
            edtTitle.setText(existing.getTitle());
            edtDesc.setText(existing.getDescription());
            edtDate.setText(existing.getDate());
            edtTime.setText(existing.getTime());
        } else {
            Calendar now = Calendar.getInstance();

            String todayDate = String.format(Locale.getDefault(),
                    "%04d-%02d-%02d",
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH)
            );

            String currentTime = String.format(Locale.getDefault(),
                    "%02d:%02d",
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE)
            );

            edtDate.setText(todayDate);
            edtTime.setText(currentTime);
        }

        Calendar c = Calendar.getInstance();

        edtDate.setOnClickListener(v -> {
            new DatePickerDialog(
                    context,
                    (view1, year, month, dayOfMonth) ->
                            edtDate.setText(String.format(Locale.getDefault(),
                                    "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        edtTime.setOnClickListener(v -> {
            new TimePickerDialog(
                    context,
                    (view12, hourOfDay, minute) ->
                            edtTime.setText(String.format(Locale.getDefault(),
                                    "%02d:%02d", hourOfDay, minute)),
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE),
                    true
            ).show();
        });

        btnToggleUsers.setOnClickListener(v -> {
            if (usersContainer.getVisibility() == View.GONE) {
                usersContainer.setVisibility(View.VISIBLE);
                populateLinkedUsers(usersContainer);
                btnToggleUsers.setText("Ocultar usuarios vinculados");
            } else {
                usersContainer.setVisibility(View.GONE);
                btnToggleUsers.setText("Mostrar usuarios vinculados");
            }
        });

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // -----------------------------
        // GUARDAR
        // -----------------------------
        btnSave.setOnClickListener(v -> {

            String title = edtTitle.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();
            String date = edtDate.getText().toString().trim();
            String time = edtTime.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
                Toast.makeText(context, "Completa título, fecha y hora", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 VALIDACIÓN DE FECHA/HORA
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                Date selectedDateTime = sdf.parse(date + " " + time);
                Date now = new Date();

                if (selectedDateTime != null && selectedDateTime.before(now)) {
                    Toast.makeText(context, "No puedes seleccionar una fecha/hora pasada", Toast.LENGTH_SHORT).show();
                    return;
                }

            } catch (Exception e) {
                Toast.makeText(context, "Fecha u hora inválida", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedUserIds.isEmpty()) {
                Toast.makeText(context, "Debes seleccionar al menos un usuario", Toast.LENGTH_SHORT).show();
                return;
            }

            if (existing == null) {
                String newId = remindersRef.push().getKey();
                working.setId(newId);
                working.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
                working.setIsCompleted(false);
                working.setIsExpired(false);
            }

            working.setUserId(currentUserId);
            working.setTitle(title);
            working.setDescription(desc);
            working.setDate(date);
            working.setTime(time);

            working.setSharedUserIds(new ArrayList<>(selectedUserIds));
            working.setSharedUsersStatus(new HashMap<>(selectedStatus));

            working.setLinkedGoalId(isGoal ? linkedId : null);
            working.setLinkedBudgetId(!isGoal ? linkedId : null);

            working.setType(isGoal ? "meta" : "presupuesto");

            working.setUpdatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));

            remindersRef.child(working.getId()).setValue(working);

            Toast.makeText(context,
                    existing != null ? "Recordatorio actualizado" : "Recordatorio creado",
                    Toast.LENGTH_SHORT).show();

            listener.onReminderSaved(working, existing == null);
            dialog.dismiss();
        });

        dialog.show();

        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    private void populateLinkedUsers(LinearLayout usersContainer) {

        usersContainer.removeAllViews();

        List<String> sourceUsers = linkedSharedUsers != null
                ? new ArrayList<>(linkedSharedUsers)
                : new ArrayList<>();

        if (!sourceUsers.contains(currentUserId)) {
            sourceUsers.add(0, currentUserId);
        }

        for (String uid : sourceUsers) {

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(8, 8, 8, 8);

            CheckBox cb = new CheckBox(context);
            cb.setId(View.generateViewId());
            cb.setTag(uid);

            String name = userIdToNameMap != null
                    ? userIdToNameMap.getOrDefault(uid, uid)
                    : uid;

            cb.setText(name);
            cb.setChecked(selectedUserIds.contains(uid));

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {

                if (isChecked) {
                    if (!selectedUserIds.contains(uid)) {
                        selectedUserIds.add(uid);
                    }
                } else {
                    selectedUserIds.remove(uid);
                }

                boolean prev = existing != null &&
                        existing.getSharedUsersStatus() != null &&
                        existing.getSharedUsersStatus().getOrDefault(uid, false);

                selectedStatus.put(uid, prev);
            });

            row.addView(cb);
            usersContainer.addView(row);
        }
    }
}