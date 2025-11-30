package com.example.finanzly.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.finanzly.R;
import com.example.finanzly.adapters.ReminderAdapter;
import com.example.finanzly.models.Reminder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * RemindersFragment completo: replica funcionalidades del componente Angular
 */
public class RemindersFragment extends Fragment implements ReminderAdapter.OnReminderActionListener {

    private RecyclerView recyclerView;
    private ReminderAdapter adapter;
    private final List<Reminder> reminderList = new ArrayList<>();
    private final List<Reminder> filteredList = new ArrayList<>();

    private DatabaseReference remindersRef;
    private DatabaseReference goalsRef;
    private DatabaseReference budgetsRef;
    private DatabaseReference usersRef;

    private String currentUserId;

    // UI (fragment_reminders.xml should contener estas vistas)

    private EditText edtSearch;
    private Spinner spinnerFilterType;
    private Spinner spinnerFilterStatus;
    private Button btnClearFilters;
    private LinearLayout alertPendingContainer;
    private TextView txtPendingAlert;
    private TextView txtPendingCount;
    private Button btnApplyFilters; // declarar



    // Data auxiliares
    private final Map<String, List<String>> sharedUsersMap = new HashMap<>(); // reminderId -> nombres
    private final Map<String, Map<String, Object>> goalsMap = new HashMap<>();   // goalId -> goal object map
    private final Map<String, Map<String, Object>> budgetsMap = new HashMap<>(); // budgetId -> budget map
    private final Map<String, Map<String, Object>> usersMap = new HashMap<>();   // userId -> user map

    // Filtros
    private String filterSearch = "";
    private String filterType = "";
    private String filterStatus = "";

    // Utils
    private final SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeOnly = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public RemindersFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_reminders, container, false);

        // -----------------------------
        // RecyclerView + Adapter
        // -----------------------------
        recyclerView = root.findViewById(R.id.recyclerReminders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReminderAdapter(getContext(), filteredList, sharedUsersMap, this);
        recyclerView.setAdapter(adapter);

        // -----------------------------
        // Firebase refs
        // -----------------------------
        remindersRef = FirebaseDatabase.getInstance().getReference("reminders");
        goalsRef = FirebaseDatabase.getInstance().getReference("goals");
        budgetsRef = FirebaseDatabase.getInstance().getReference("budgets");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        currentUserId = FirebaseAuth.getInstance().getUid();

        // -----------------------------
        // UI bindings
        // -----------------------------
        edtSearch = root.findViewById(R.id.edtFilterSearch);
        spinnerFilterType = root.findViewById(R.id.spinnerFilterType);
        spinnerFilterStatus = root.findViewById(R.id.spinnerFilterStatus);
        btnClearFilters = root.findViewById(R.id.btnClearFilters);
        btnApplyFilters = root.findViewById(R.id.btnApplyFilters);

        alertPendingContainer = root.findViewById(R.id.alertPendingContainer);
        txtPendingAlert = root.findViewById(R.id.txtPendingAlert);
        txtPendingCount = root.findViewById(R.id.txtPendingCount);

        // -----------------------------
        // Botones
        // -----------------------------


        btnApplyFilters.setOnClickListener(v -> applyFilters());

        btnClearFilters.setOnClickListener(v -> {
            filterSearch = "";
            filterType = "";
            filterStatus = "";
            edtSearch.setText("");
            spinnerFilterType.setSelection(0);
            spinnerFilterStatus.setSelection(0);
            filteredList.clear();
            adapter.updateList(filteredList); // vaciar lista
        });

        // -----------------------------
        // Spinners
        // -----------------------------
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"ninguno", "meta", "presupuesto", "otro"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterType.setAdapter(typeAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"ninguno", "pending", "completed", "expired"});
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterStatus.setAdapter(statusAdapter);

        // -----------------------------
        // EditText búsqueda
        // -----------------------------
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearch = s.toString();
            }
            @Override public void afterTextChanged(Editable s) {}
        });



        loadUsers();
        loadReminders();



        return root;
    }






    private void loadUsers() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    usersMap.put(ds.getKey(), (Map<String, Object>) ds.getValue());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void loadReminders() {
        remindersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                reminderList.clear();
                sharedUsersMap.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Reminder r = ds.getValue(Reminder.class);
                    if (r == null) continue;

                    // --- asegurar ID
                    if (r.getId() == null) r.setId(ds.getKey());

                    // --- normalizar sharedUserIds
                    List<String> shared = r.getSharedUserIds();
                    if (shared == null || shared.isEmpty()) {
                        shared = new ArrayList<>();
                        DataSnapshot sharedSnap = ds.child("sharedUserIds");
                        if (sharedSnap.exists()) {
                            Object val = sharedSnap.getValue();
                            if (val instanceof List) {
                                List<?> raw = (List<?>) val;
                                for (Object o : raw) if (o != null) shared.add(o.toString());
                            } else {
                                for (DataSnapshot child : sharedSnap.getChildren()) {
                                    Object v = child.getValue();
                                    if (v != null) shared.add(v.toString());
                                }
                            }
                        }
                        r.setSharedUserIds(shared);
                    }

                    // --- seguridad: solo si es creador o compartido
                    String creator = r.getUserId() != null ? r.getUserId() : "";
                    boolean accepted = currentUserId != null &&
                            (currentUserId.equals(creator) || shared.contains(currentUserId));

                    if (!accepted) continue;

                    // --- marcar expirado
                    r.setExpired(computeExpired(r));

                    // --- añadir a la lista
                    reminderList.add(r);

                    // --- cargar nombres de usuarios compartidos
                    List<String> names = new ArrayList<>();
                    for (String uid : shared) {
                        Map<String, Object> userData = usersMap.get(uid);
                        if (userData != null && userData.get("name") != null)
                            names.add((String) userData.get("name"));
                        else
                            names.add("Usuario");
                    }
                    if (!names.contains(creator) && !creator.isEmpty()) names.add("Tú");
                    sharedUsersMap.put(r.getId(), names);
                }

                // --- actualizar UI UNA SOLA VEZ
                applyFilters();
                updatePendingAlert();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilters() {
        filteredList.clear();

        for (Reminder r : reminderList) {
            // Filtro búsqueda
            if (!TextUtils.isEmpty(filterSearch) &&
                    (r.getTitle() == null || !r.getTitle().toLowerCase().contains(filterSearch.toLowerCase()))) {
                continue;
            }

            // Filtro tipo
            if (!TextUtils.isEmpty(filterType) && !"ninguno".equals(filterType)) {
                if (r.getType() == null || !r.getType().equals(filterType)) continue;
            }

            // Filtro estado
            if (!TextUtils.isEmpty(filterStatus) && !"ninguno".equals(filterStatus)) {
                switch (filterStatus) {
                    case "completed":
                        if (!r.isCompleted()) continue;
                        break;
                    case "pending":
                        if (r.isCompleted() || r.isExpired()) continue;
                        break;
                    case "expired":
                        if (!r.isExpired()) continue;
                        break;
                }
            }

            filteredList.add(r);
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> adapter.updateList(filteredList));
        }
    }







    // Cargar nombres de usuarios compartidos para mostrar badges
    private void loadSharedUserNames(Reminder reminder) {
        String rid = reminder.getId();
        if (rid == null) return;
        List<String> uids = new ArrayList<>();
        if (reminder.getSharedUserIds() != null && !reminder.getSharedUserIds().isEmpty()) {
            uids.addAll(reminder.getSharedUserIds());
        } else {
            // fallback: si tiene linkedGoalId o linkedBudgetId, tomar compartidos desde esas entidades (si están en Firebase)
            if (reminder.getLinkedGoalId() != null && goalsMap.containsKey(reminder.getLinkedGoalId())) {
                Object o = goalsMap.get(reminder.getLinkedGoalId()).get("sharedUserIds");
                if (o instanceof List) uids.addAll((List<String>) o);
            }
            if (reminder.getLinkedBudgetId() != null && budgetsMap.containsKey(reminder.getLinkedBudgetId())) {
                Object o = budgetsMap.get(reminder.getLinkedBudgetId()).get("sharedUserIds");
                if (o instanceof List) uids.addAll((List<String>) o);
            }
        }
        // Asegurar creador
        if (!uids.contains(reminder.getUserId())) uids.add(reminder.getUserId());

        // Mapear ids a nombres (si user info cargada)
        List<String> names = new ArrayList<>();
        for (String uid : uids) {
            Map<String, Object> ud = usersMap.get(uid);
            if (ud != null && ud.get("name") != null) names.add((String) ud.get("name"));
            else names.add("Usuario");
        }
        sharedUsersMap.put(rid, names);
        // Actualizar adapter (para que los badges se muestren)
        applyFilters();
    }

    // --------------------------------------
    // FILTRADO
    // --------------------------------------





    private boolean computeExpired(Reminder r) {
        if (r.getDate() == null || r.getTime() == null) return false;
        String dt = r.getDate() + "T" + r.getTime();
        try {
            Date target = dateIso.parse(dt);
            if (target == null) return false;
            return target.before(new Date()) && !r.isCompleted();
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --------------------------------------
    // ALERTA PENDING
    // --------------------------------------
    private void updatePendingAlert() {
        int pendingCount = 0;
        for (Reminder r : reminderList) {
            if (!r.isCompleted() && r.getDate() != null && r.getTime() != null) {
                if (!computeExpired(r)) {
                    // pending if date/time <= now
                    try {
                        Date target = dateIso.parse(r.getDate() + "T" + r.getTime());
                        if (target != null && !target.after(new Date())) pendingCount++;
                    } catch (ParseException e) { /* ignore */ }
                }
            }
        }
        if (pendingCount > 0) {
            if (alertPendingContainer != null) alertPendingContainer.setVisibility(View.VISIBLE);
            if (txtPendingCount != null) txtPendingCount.setText(String.valueOf(pendingCount));
            if (txtPendingAlert != null) txtPendingAlert.setText("Tienes " + pendingCount + " recordatorio(s) pendiente(s).");
        } else {
            if (alertPendingContainer != null) alertPendingContainer.setVisibility(View.GONE);
        }
    }

    // --------------------------------------
// DIALOG: Crear / Editar (igual al Angular)
// --------------------------------------
    private void openCreateEditDialog(Reminder existing) {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_add_edit_reminder, null);
        builder.setView(view);

        EditText edtTitle = view.findViewById(R.id.edtReminderTitle);
        EditText edtDesc = view.findViewById(R.id.edtReminderDescription);
        EditText edtDate = view.findViewById(R.id.edtReminderDate);
        EditText edtTime = view.findViewById(R.id.edtReminderTime);
        Spinner spinnerGoal = view.findViewById(R.id.spinnerLinkedGoal);
        Spinner spinnerBudget = view.findViewById(R.id.spinnerLinkedBudget);
        Button btnToggleUsers = view.findViewById(R.id.btnToggleUsers);
        LinearLayout usersContainer = view.findViewById(R.id.usersContainer);
        Button btnCancel = view.findViewById(R.id.btnCancelReminder);
        Button btnSave = view.findViewById(R.id.btnSaveReminder);

        // -----------------------------
        // SPINNER GOALS
        // -----------------------------
        final List<String> goalIds = new ArrayList<>();
        final List<String> goalTitles = new ArrayList<>();

        goalIds.add(null);
        goalTitles.add("Ninguna");

        for (Map.Entry<String, Map<String, Object>> e : goalsMap.entrySet()) {
            goalIds.add(e.getKey());
            Object t = e.getValue().get("title");
            goalTitles.add(t != null ? t.toString() : "Meta");
        }

        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                goalTitles
        );
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGoal.setAdapter(goalAdapter);
        spinnerGoal.setTag(goalIds);

        // -----------------------------
        // SPINNER BUDGETS
        // -----------------------------
        final List<String> budgetIds = new ArrayList<>();
        final List<String> budgetTitles = new ArrayList<>();

        budgetIds.add(null);
        budgetTitles.add("Ninguno");

        for (Map.Entry<String, Map<String, Object>> e : budgetsMap.entrySet()) {
            budgetIds.add(e.getKey());
            Object cat = e.getValue().get("category");
            budgetTitles.add(cat != null ? cat.toString() : "Presupuesto");
        }

        ArrayAdapter<String> budgetAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                budgetTitles
        );
        budgetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBudget.setAdapter(budgetAdapter);
        spinnerBudget.setTag(budgetIds);

        // -----------------------------
        // Si EDITAMOS
        // -----------------------------
        final Reminder working = existing != null ? existing : new Reminder();

        if (existing != null) {
            edtTitle.setText(existing.getTitle());
            edtDesc.setText(existing.getDescription());
            edtDate.setText(existing.getDate());
            edtTime.setText(existing.getTime());

            // Seleccionar goal
            if (existing.getLinkedGoalId() != null) {
                int idx = goalIds.indexOf(existing.getLinkedGoalId());
                spinnerGoal.setSelection(Math.max(idx, 0));
            } else spinnerGoal.setSelection(0);

            // Seleccionar budget
            if (existing.getLinkedBudgetId() != null) {
                int idx = budgetIds.indexOf(existing.getLinkedBudgetId());
                spinnerBudget.setSelection(Math.max(idx, 0));
            } else spinnerBudget.setSelection(0);
        }

        // -----------------------------
        // DATEPICKER / TIMEPICKER
        // -----------------------------
        final Calendar c = Calendar.getInstance();

        edtDate.setOnClickListener(v -> {
            DatePickerDialog dp = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        edtDate.setText(String.format(Locale.getDefault(),
                                "%04d-%02d-%02d", year, month + 1, dayOfMonth));
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            );
            dp.show();
        });

        edtTime.setOnClickListener(v -> {
            TimePickerDialog tp = new TimePickerDialog(
                    requireContext(),
                    (view12, hourOfDay, minute) ->
                            edtTime.setText(String.format(Locale.getDefault(),
                                    "%02d:%02d", hourOfDay, minute)),
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true
            );
            tp.show();
        });

        // -----------------------------
        // ACTUALIZAR LISTA DE USUARIOS AL CAMBIAR SPINNER
        // -----------------------------
        AdapterView.OnItemSelectedListener updateUsersListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (usersContainer.getVisibility() == View.VISIBLE) {
                    populateLinkedUsersCheckboxes(usersContainer, spinnerGoal, spinnerBudget, working);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerGoal.setOnItemSelectedListener(updateUsersListener);
        spinnerBudget.setOnItemSelectedListener(updateUsersListener);

        // -----------------------------
        // BOTÓN PARA MOSTRAR/OCULTAR USUARIOS
        // -----------------------------
        btnToggleUsers.setOnClickListener(v -> {
            if (usersContainer.getVisibility() == View.GONE) {
                usersContainer.setVisibility(View.VISIBLE);
                populateLinkedUsersCheckboxes(usersContainer, spinnerGoal, spinnerBudget, working);
                btnToggleUsers.setText("Ocultar usuarios vinculados");
            } else {
                usersContainer.setVisibility(View.GONE);
                btnToggleUsers.setText("Mostrar usuarios vinculados");
            }
        });

        AlertDialog dialog = builder.create();

        // -----------------------------
        // CANCELAR
        // -----------------------------
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
                Toast.makeText(requireContext(),
                        "Completa título, fecha y hora", Toast.LENGTH_SHORT).show();
                return;
            }

            // -----------------------------
            // RECOGER USUARIOS SELECCIONADOS
            // -----------------------------
            List<String> selectedUserIds = new ArrayList<>();
            Map<String, Boolean> statusMap = new HashMap<>();

            if (usersContainer.getVisibility() == View.VISIBLE) {
                int childCount = usersContainer.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View child = usersContainer.getChildAt(i);
                    if (child instanceof LinearLayout) {
                        CheckBox cb = child.findViewById(R.id.checkbox_user_dynamic);
                        if (cb == null) continue; // seguridad extra

                        String uid = (String) cb.getTag();
                        if (cb.isChecked()) selectedUserIds.add(uid);

                        boolean prev = false;
                        if (working.getSharedUsersStatus() != null &&
                                working.getSharedUsersStatus().containsKey(uid))
                            prev = working.getSharedUsersStatus().get(uid);

                        statusMap.put(uid, prev);
                    }
                }
            }

            // 🔥 AÑADIR AL CREADOR SIEMPRE
            if (!selectedUserIds.contains(currentUserId))
                selectedUserIds.add(0, currentUserId);

            for (String uid : selectedUserIds)
                statusMap.putIfAbsent(uid, false);

            // -----------------------------
            // GUARDAR DATOS
            // -----------------------------
            if (existing == null) {
                String newId = remindersRef.push().getKey();
                working.setId(newId);
                working.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm",
                        Locale.getDefault()).format(new Date()));
            }

            working.setUserId(currentUserId);
            working.setTitle(title);
            working.setDescription(desc);
            working.setDate(date);
            working.setTime(time);

            // GOAL/BUDGET SELECCIONADOS
            int gpos = spinnerGoal.getSelectedItemPosition();
            working.setLinkedGoalId(gpos > 0 ? goalIds.get(gpos) : null);

            int bpos = spinnerBudget.getSelectedItemPosition();
            working.setLinkedBudgetId(bpos > 0 ? budgetIds.get(bpos) : null);

            // TIPO
            if (working.getLinkedGoalId() != null) working.setType("meta");
            else if (working.getLinkedBudgetId() != null) working.setType("presupuesto");
            else working.setType("otro");

            working.setSharedUserIds(selectedUserIds);
            working.setSharedUsersStatus(statusMap);
            working.setCompleted(false);
            working.setExpired(false);
            working.setUpdatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm",
                    Locale.getDefault()).format(new Date()));

            // FIREBASE
            remindersRef.child(working.getId()).setValue(working);

            Toast.makeText(requireContext(),
                    existing != null ? "Recordatorio actualizado" : "Recordatorio creado",
                    Toast.LENGTH_SHORT).show();

            dialog.dismiss();
        });

        dialog.show();
    }



    private void populateLinkedUsersCheckboxes(
            LinearLayout usersContainer,
            Spinner spinnerGoal,
            Spinner spinnerBudget,
            Reminder working
    ) {
        usersContainer.removeAllViews();

        // Recuperar listas de IDs asociadas al spinner (puestas en openCreateEditDialog)
        List<String> goalIds = null;
        List<String> budgetIds = null;

        try {
            Object gTag = spinnerGoal.getTag();
            if (gTag instanceof List) goalIds = (List<String>) gTag;

            Object bTag = spinnerBudget.getTag();
            if (bTag instanceof List) budgetIds = (List<String>) bTag;
        } catch (Exception ignored) {}

        List<String> uidCandidates = new ArrayList<>();

        // GOAL seleccionado
        int gpos = spinnerGoal.getSelectedItemPosition();
        if (gpos > 0 && goalIds != null && gpos < goalIds.size()) {
            String goalId = goalIds.get(gpos);
            if (goalId != null && goalsMap.containsKey(goalId)) {
                Object su = goalsMap.get(goalId).get("sharedUserIds");
                if (su instanceof List) uidCandidates.addAll((List<String>) su);
            }
        }

        // BUDGET seleccionado
        int bpos = spinnerBudget.getSelectedItemPosition();
        if (bpos > 0 && budgetIds != null && bpos < budgetIds.size()) {
            String budgetId = budgetIds.get(bpos);
            if (budgetId != null && budgetsMap.containsKey(budgetId)) {
                Object su = budgetsMap.get(budgetId).get("sharedUserIds");
                if (su instanceof List) uidCandidates.addAll((List<String>) su);
            }
        }

        // ELIMINAR DUPLICADOS
        LinkedHashSet<String> uidsSet = new LinkedHashSet<>(uidCandidates);



        // Si working tiene creatorId o userId, tampoco mostrarlo si aparece
        if (working.getUserId() != null) {
            uidsSet.remove(working.getUserId());
        }

        // Construir lista final de UIDs compartidos
        List<String> finalUids = new ArrayList<>(uidsSet);

        // Crear los CheckBox dinámicamente
        for (String uid : finalUids) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(8, 8, 8, 8);

            CheckBox cb = new CheckBox(requireContext());
            cb.setId(R.id.checkbox_user_dynamic);
            cb.setTag(uid);

            boolean checked =
                    working.getSharedUserIds() != null &&
                            working.getSharedUserIds().contains(uid);

            cb.setChecked(checked);

            TextView tv = new TextView(requireContext());
            String name = "Usuario";
            if (usersMap.containsKey(uid) &&
                    usersMap.get(uid).get("name") != null) {
                name = usersMap.get(uid).get("name").toString();
            }

            tv.setText(" " + name);
            tv.setTextSize(15f);

            row.addView(cb);
            row.addView(tv);

            usersContainer.addView(row);
        }

        // Si no hay usuarios compartidos
        if (usersContainer.getChildCount() == 0) {
            TextView tv = new TextView(requireContext());
            tv.setText("No hay usuarios compartidos con esta meta o presupuesto.");
            tv.setPadding(8, 8, 8, 8);
            usersContainer.addView(tv);
        }
    }





    // --------------------------------------
    // Toggle Completed (si eres creator togglea todos, si eres compartido togglea solo tu estado)
    // --------------------------------------
    private void toggleCompleted(Reminder reminder) {
        if (reminder == null) return;
        Map<String, Boolean> status = reminder.getSharedUsersStatus() != null ? reminder.getSharedUsersStatus() : new HashMap<>();
        boolean current = status.containsKey(currentUserId) && status.get(currentUserId);
        status.put(currentUserId, !current);

        // recompute allCompleted
        boolean allCompleted = true;
        for (Boolean val : status.values()) {
            if (!val) { allCompleted = false; break; }
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("sharedUsersStatus", status);
        updates.put("isCompleted", allCompleted);
        updates.put("updatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));

        remindersRef.child(reminder.getId()).updateChildren(updates);

        // actualizar localmente
        reminder.setSharedUsersStatus(status);
        reminder.setCompleted(allCompleted);
        applyFilters();
        updatePendingAlert();
    }

    // --------------------------------------
    // Leave reminder (el usuario sale del shared)
    // --------------------------------------
    private void leaveReminder(Reminder reminder) {
        if (reminder == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Salir del recordatorio?")
                .setMessage("Ya no recibirás notificaciones de este recordatorio.")
                .setPositiveButton("Salir", (d, w) -> {
                    List<String> sids = reminder.getSharedUserIds() != null ? new ArrayList<>(reminder.getSharedUserIds()) : new ArrayList<>();
                    sids.remove(currentUserId);
                    Map<String, Boolean> status = reminder.getSharedUsersStatus() != null ? new HashMap<>(reminder.getSharedUsersStatus()) : new HashMap<>();
                    status.remove(currentUserId);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("sharedUserIds", sids);
                    updates.put("sharedUsersStatus", status);
                    updates.put("updatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));

                    remindersRef.child(reminder.getId()).updateChildren(updates);
                    Toast.makeText(requireContext(), "Has salido del recordatorio", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --------------------------------------
    // Confirm delete
    // --------------------------------------
    private void confirmDelete(Reminder reminder) {
        if (reminder == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar recordatorio")
                .setMessage("¿Estás seguro que quieres eliminar este recordatorio? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    remindersRef.child(reminder.getId()).removeValue();
                    Toast.makeText(requireContext(), "Recordatorio eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }



    // --------------------------------------
    // Implementación de ReminderAdapter.OnReminderActionListener (compatibilidad)
    // Si tu adapter usa otra interfaz, ajusta.
    // --------------------------------------
    @Override
    public void onEdit(Reminder reminder) { openCreateEditDialog(reminder); }

    @Override
    public void onDelete(Reminder reminder) { confirmDelete(reminder); }

    @Override
    public void onToggleComplete(Reminder reminder) { toggleCompleted(reminder); }

    @Override
    public void onAddUsers(Reminder reminder) {  }

    @Override
    public void onLeave(Reminder reminder) { leaveReminder(reminder); }
}
