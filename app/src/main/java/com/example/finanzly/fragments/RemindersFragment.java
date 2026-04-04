package com.example.finanzly.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.finanzly.R;
import com.example.finanzly.adapters.ReminderAdapter;
import com.example.finanzly.dialogs.AddEditReminderDialog;
import com.example.finanzly.models.Reminder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RemindersFragment extends Fragment implements ReminderAdapter.OnReminderActionListener {

    private RecyclerView recyclerView;
    private ReminderAdapter adapter;


    private Spinner spinnerLinkedResource;

    private final List<String> linkedResourceNames = new ArrayList<>();
    private final Map<String, String> linkedResourceMap = new HashMap<>();
    private final List<Reminder> reminderList = new ArrayList<>();
    private final List<Reminder> allReminders = new ArrayList<>();

    private String filterGoalId = null;
    private String filterBudgetId = null;
    private final Map<String, List<String>> sharedUsersMap = new HashMap<>();

    private String currentUserId;
    private DatabaseReference remindersRef;
    private Map<String, Map<String, Object>> usersMap = new HashMap<>();

    private Spinner spinnerReminderStatus;
    private Spinner spinnerReminderType; // NUEVO FILTRO
    private EditText edtFilterSearch;

    private Button btnApplyFilters , btncleanFilters;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_reminders, container, false);

        recyclerView = root.findViewById(R.id.recyclerReminders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        spinnerReminderStatus = root.findViewById(R.id.spinnerReminderStatus);
        spinnerReminderType = root.findViewById(R.id.spinnerReminderType); // NUEVO
        edtFilterSearch = root.findViewById(R.id.edtFilterSearch);

        btnApplyFilters = root.findViewById(R.id.btnApplyFilters);
        btncleanFilters = root.findViewById(R.id.btnClearFilters);

        spinnerLinkedResource = root.findViewById(R.id.spinnerLinkedResource);
        loadLinkedResources();

        setupFilters();

        currentUserId = FirebaseAuth.getInstance().getUid();

        adapter = new ReminderAdapter(getContext(), reminderList, sharedUsersMap, this, currentUserId);
        recyclerView.setAdapter(adapter);

        remindersRef = FirebaseDatabase.getInstance().getReference("reminders");

        loadUsers();

        // SPINNER ESTADO
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Todos", "Pendientes", "Completados", "Expirados"}
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReminderStatus.setAdapter(statusAdapter);

        // SPINNER TIPO (META / PRESUPUESTO)
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Todos", "Meta", "Presupuesto"}
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReminderType.setAdapter(typeAdapter);

        if (getArguments() != null) {
            filterGoalId = getArguments().getString("goalId");
            filterBudgetId = getArguments().getString("budgetId");
        }


        return root;
    }

    // =============================================
    // FILTROS
    // =============================================
    private void setupFilters() {

        btnApplyFilters.setOnClickListener(v -> applyFilters());

        btncleanFilters.setOnClickListener(v -> cleanFilters());
    }


    private void loadLinkedResources() {

        linkedResourceNames.clear();
        linkedResourceMap.clear();

        linkedResourceNames.add("Todos");

        DatabaseReference remindersRef = FirebaseDatabase.getInstance().getReference("reminders");
        DatabaseReference goalsRef = FirebaseDatabase.getInstance().getReference("goals");
        DatabaseReference budgetsRef = FirebaseDatabase.getInstance().getReference("budgets");

        // 👇 Sets para evitar duplicados
        Set<String> goalIds = new HashSet<>();
        Set<String> budgetIds = new HashSet<>();

        // 1️⃣ PRIMERO: sacar IDs usados en reminders del usuario
        remindersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot ds : snapshot.getChildren()) {

                    String userId = ds.child("userId").getValue(String.class);

                    // ⚠️ Filtrar solo los del usuario actual
                    if (userId == null || !userId.equals(currentUserId)) continue;

                    String goalId = ds.child("linkedGoalId").getValue(String.class);
                    String budgetId = ds.child("linkedBudgetId").getValue(String.class);

                    if (goalId != null && !goalId.isEmpty()) {
                        goalIds.add(goalId);
                    }

                    if (budgetId != null && !budgetId.isEmpty()) {
                        budgetIds.add(budgetId);
                    }
                }

                // 2️⃣ Cargar SOLO esos goals
                goalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String id = ds.getKey();
                            String title = ds.child("title").getValue(String.class);

                            if (id != null && title != null && goalIds.contains(id)) {
                                String name = "Meta: " + title;
                                linkedResourceNames.add(name);
                                linkedResourceMap.put(name, id);
                            }
                        }

                        // 3️⃣ Cargar SOLO esos budgets
                        budgetsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    String id = ds.getKey();
                                    String category = ds.child("category").getValue(String.class);

                                    if (id != null && category != null && budgetIds.contains(id)) {
                                        String name = "Presupuesto: " + category;
                                        linkedResourceNames.add(name);
                                        linkedResourceMap.put(name, id);
                                    }
                                }

                                // 4️⃣ Set adapter
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        requireContext(),
                                        android.R.layout.simple_spinner_item,
                                        linkedResourceNames
                                );

                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerLinkedResource.setAdapter(adapter);
                                if (filterGoalId != null || filterBudgetId != null) {

                                    spinnerLinkedResource.post(() -> {
                                        for (int i = 0; i < linkedResourceNames.size(); i++) {

                                            String name = linkedResourceNames.get(i);
                                            String id = linkedResourceMap.get(name);

                                            if ((filterGoalId != null && filterGoalId.equals(id)) ||
                                                    (filterBudgetId != null && filterBudgetId.equals(id))) {

                                                spinnerLinkedResource.setSelection(i);
                                                applyFilters();
                                                break;
                                            }
                                        }
                                    });
                                }
                            }

                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void applyFilters() {

        String searchText = edtFilterSearch.getText().toString().trim().toLowerCase();
        String selectedStatus = spinnerReminderStatus.getSelectedItem().toString();
        String selectedType = spinnerReminderType.getSelectedItem().toString();
        String selectedResource = spinnerLinkedResource.getSelectedItem() != null
                ? spinnerLinkedResource.getSelectedItem().toString()
                : "Todos";
        String selectedResourceId = linkedResourceMap.get(selectedResource);


        List<Reminder> filtered = new ArrayList<>();

        for (Reminder r : allReminders) {

            // ---- FILTRO TEXTO ----
            boolean matchesText = r.getTitle() != null &&
                    r.getTitle().toLowerCase().contains(searchText);

            boolean matchesResource = true;
            boolean isExpired = isReminderExpired(r);
            boolean matchesState = true;

            // ---- FILTRO ESTADO ----
            switch (selectedStatus) {

                case "Pendientes":
                    matchesState = !r.getIsCompleted() && !isExpired;
                    break;

                case "Completados":
                    matchesState = r.getIsCompleted();
                    break;

                case "Expirados":
                    matchesState = isExpired;
                    break;

                case "Todos":
                default:
                    matchesState = true;
                    break;
            }

            // ---- FILTRO TIPO (META / PRESUPUESTO) ----
            boolean matchesType = true;

            switch (selectedType) {

                case "Meta":
                    matchesType = r.getLinkedGoalId() != null;
                    break;

                case "Presupuesto":
                    matchesType = r.getLinkedBudgetId() != null;
                    break;

                case "Todos":
                default:
                    matchesType = true;
                    break;
            }

            if (!selectedResource.equals("Todos") && selectedResourceId != null) {
                matchesResource =
                        (r.getLinkedGoalId() != null && r.getLinkedGoalId().equals(selectedResourceId)) ||
                                (r.getLinkedBudgetId() != null && r.getLinkedBudgetId().equals(selectedResourceId));
            }

            if (matchesText && matchesState && matchesType && matchesResource) {
                filtered.add(r);
            }
        }

        adapter.updateList(filtered);
    }

    private boolean isReminderExpired(Reminder r) {

        if (r.getDate() == null || r.getTime() == null) return false;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
            Date reminderDate = sdf.parse(r.getDate() + "T" + r.getTime());
            return new Date().after(reminderDate);
        } catch (ParseException e) {
            return false;
        }
    }

    private void cleanFilters() {

        edtFilterSearch.setText("");
        spinnerLinkedResource.setSelection(0);
        spinnerReminderStatus.setSelection(0);
        spinnerReminderType.setSelection(0);

        adapter.updateList(new ArrayList<>(allReminders));
    }

    // =========================================================
    // CARGA DE USUARIOS
    // =========================================================

    private void loadUsers() {

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                usersMap.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    usersMap.put(ds.getKey(), (Map<String, Object>) ds.getValue());
                }

                loadReminders();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadReminders();
            }
        });
    }

    // =========================================================
    // CARGA RECORDATORIOS
    // =========================================================

    private void loadReminders() {

        remindersRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                reminderList.clear();
                sharedUsersMap.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {

                    Reminder r = ds.getValue(Reminder.class);
                    if (r == null) continue;

                    if (r.getId() == null) r.setId(ds.getKey());
                    if (r.getSharedUserIds() == null) r.setSharedUserIds(new ArrayList<>());

                    boolean accepted = currentUserId != null &&
                            (currentUserId.equals(r.getUserId()) ||
                                    r.getSharedUserIds().contains(currentUserId));

                    if (!accepted) continue;

                    List<String> names = new ArrayList<>();

                    for (String uid : r.getSharedUserIds()) {

                        Map<String, Object> userData = usersMap.get(uid);

                        if (userData != null && userData.get("name") != null) {
                            names.add((String) userData.get("name"));
                        } else {
                            names.add("Usuario");
                        }
                    }

                    sharedUsersMap.put(r.getId(), names);
                    // FILTRO AUTOMÁTICO SI VIENE DESDE GOAL
                    if (filterGoalId != null) {
                        if (r.getLinkedGoalId() == null || !r.getLinkedGoalId().equals(filterGoalId)) {
                            continue;
                        }
                    }

                    reminderList.add(r);
                }

                allReminders.clear();
                allReminders.addAll(reminderList);
//  Si viene filtrado por goal → aplicar filtros directamente
                if (filterGoalId != null) {
                    applyFilters();
                } else {
                    adapter.notifyDataSetChanged();
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Log.e("RemindersFragment", "Error cargando reminders: " + error.getMessage());
            }
        });
    }

    // =====================================================================================
    //  ✔ BOTÓN EDITAR → ahora espera a tener usersMap cargado (loadUsers fue llamado antes)
    // =====================================================================================
    @Override
    public void onEdit(Reminder reminder) {

        boolean isGoal = reminder.getLinkedGoalId() != null;
        String linkedId = isGoal ? reminder.getLinkedGoalId() : reminder.getLinkedBudgetId();

        if (linkedId == null) {
            Log.e("RemindersFragment", "No tiene linkedId, no se puede editar correctamente.");
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(
                isGoal ? "goals" : "budgets"
        ).child(linkedId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    Log.e("RemindersFragment", "No se encontró la meta/presupuesto vinculado");
                    return;
                }

                // Obtener título correcto según sea meta o presupuesto
                String linkedTitle = isGoal
                        ? snapshot.child("title").getValue(String.class)
                        : snapshot.child("category").getValue(String.class);
                if (linkedTitle == null) linkedTitle = "(sin nombre)";

                // Obtener lista de usuarios vinculados al goal/budget desde Firebase
                List<String> linkedUsers = new ArrayList<>();
                if (snapshot.hasChild("sharedUserIds")) {
                    for (DataSnapshot userSnapshot : snapshot.child("sharedUserIds").getChildren()) {
                        String uid = userSnapshot.getValue(String.class);
                        if (uid != null) linkedUsers.add(uid);
                    }
                }

                // Crear mapa userId → nombre usando usersMap ya cargado
                Map<String, String> userIdToName = new HashMap<>();
                for (String uid : usersMap.keySet()) {
                    Map<String, Object> udata = usersMap.get(uid);
                    if (udata != null && udata.get("name") != null) {
                        userIdToName.put(uid, udata.get("name").toString());
                    } else {
                        userIdToName.put(uid, "Usuario");
                    }
                }

                // Mostrar diálogo con datos desde Firebase
                AddEditReminderDialog dialog = new AddEditReminderDialog(
                        getContext(),
                        reminder,                    // modo edición
                        currentUserId,
                        linkedId,                    // ID real
                        linkedTitle,                 // Título consultado desde Firebase
                        isGoal,
                        linkedUsers,                 // usuarios vinculados desde Firebase
                        userIdToName,                // mapa id → nombre
                        (updatedReminder, isNew) -> {
                            Log.d("RemindersFragment", "Reminder actualizado correctamente.");
                        }
                );

                dialog.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RemindersFragment", "Error buscando título: " + error.getMessage());
            }
        });
    }


    // =====================================================================================
    //  ✔ BOTÓN BORRAR — AHORA CON CONFIRMACIÓN
    // =====================================================================================
    @Override
    public void onDelete(Reminder reminder) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar recordatorio")
                .setMessage("¿Seguro que quieres eliminar este recordatorio? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    remindersRef.child(reminder.getId()).removeValue()
                            .addOnSuccessListener(aVoid ->
                                    Log.d("RemindersFragment", "Reminder eliminado"))
                            .addOnFailureListener(e ->
                                    Log.e("RemindersFragment", "Error eliminando reminder", e));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =====================================================================================
    //  ✔ BOTÓN COMPLETE / PENDING → TOGGLE
    // =====================================================================================
    @Override
    public void onToggleComplete(Reminder reminder) {
        if (reminder.getSharedUsersStatus() == null)
            reminder.setSharedUsersStatus(new HashMap<>());

        Map<String, Boolean> statusMap = reminder.getSharedUsersStatus();
        boolean currentState = statusMap.getOrDefault(currentUserId, false);

        // Cambiar el estado del usuario actual
        boolean newState = !currentState;
        statusMap.put(currentUserId, newState);

        // Guardar en Firebase el estado del usuario
        remindersRef.child(reminder.getId())
                .child("sharedUsersStatus")
                .child(currentUserId)
                .setValue(newState);

        // Revisar si todos los usuarios han completado
        boolean allCompleted = true;
        List<String> users = reminder.getSharedUserIds();
        if (users != null && !users.isEmpty()) {
            for (String uid : users) {
                Boolean val = statusMap.get(uid);
                if (val == null || !val) {
                    allCompleted = false;
                    break;
                }
            }
        } else {
            // Si no hay usuarios compartidos, solo el currentUser cuenta
            allCompleted = newState;
        }

        // Actualizar el campo isCompleted en Firebase
        // Si alguien descompleta, isCompleted será false automáticamente
        remindersRef.child(reminder.getId())
                .child("isCompleted")
                .setValue(allCompleted);

        Log.d("RemindersFragment", "Toggle -> Usuario " + currentUserId +
                " estado: " + newState + " | isCompleted: " + allCompleted);
    }




    // =====================================================================================
    //  ✔ BOTÓN LEAVE — AHORA CON CONFIRMACIÓN
    // =====================================================================================
    @Override
    public void onLeave(Reminder reminder) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Salir del recordatorio")
                .setMessage("¿Seguro que deseas salir de este recordatorio compartido?")
                .setPositiveButton("Salir", (dialog, which) -> {

                    if (!reminder.getSharedUserIds().contains(currentUserId)) return;

                    reminder.getSharedUserIds().remove(currentUserId);

                    remindersRef.child(reminder.getId())
                            .child("sharedUserIds")
                            .setValue(reminder.getSharedUserIds());

                    if (reminder.getSharedUsersStatus() != null) {
                        remindersRef.child(reminder.getId())
                                .child("sharedUsersStatus")
                                .child(currentUserId)
                                .removeValue();
                    }

                })
                .setNegativeButton("Cancelar", null)
                .show();
    }







}
