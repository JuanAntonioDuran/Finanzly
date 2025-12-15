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
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemindersFragment extends Fragment implements ReminderAdapter.OnReminderActionListener {

    private RecyclerView recyclerView;
    private ReminderAdapter adapter;
    private final List<Reminder> reminderList = new ArrayList<>();
    private final List<Reminder> allReminders = new ArrayList<>(); // lista completa para filtrar
    private final Map<String, List<String>> sharedUsersMap = new HashMap<>();
    private String currentUserId;
    private DatabaseReference remindersRef;
    private Map<String, Map<String, Object>> usersMap = new HashMap<>();

    private Spinner spinnerReminderStatus;
    private EditText edtFilterSearch;
    private Button btnApplyFilters , btncleanFilters; // botón para aplicar filtros

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_reminders, container, false);

        recyclerView = root.findViewById(R.id.recyclerReminders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        spinnerReminderStatus = root.findViewById(R.id.spinnerReminderStatus);
        edtFilterSearch = root.findViewById(R.id.edtFilterSearch);
        btnApplyFilters = root.findViewById(R.id.btnApplyFilters);
        btncleanFilters = root.findViewById(R.id.btnClearFilters);

        setupFilters(); // configuramos solo el filtrado por botón

        currentUserId = FirebaseAuth.getInstance().getUid();
        adapter = new ReminderAdapter(getContext(), reminderList, sharedUsersMap, this, currentUserId);
        recyclerView.setAdapter(adapter);

        remindersRef = FirebaseDatabase.getInstance().getReference("reminders");

        loadUsers();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Todos", "Pendientes", "Completados", "Expirados"}
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReminderStatus.setAdapter(adapter);


        return root;
    }

    // =============================================
// FILTROS — solo se aplican al pulsar el botón
// =============================================
    private void setupFilters() {

        btnApplyFilters.setOnClickListener(v -> applyFilters());

        btncleanFilters.setOnClickListener(v -> cleanFilters());
    }


    private void applyFilters() {
        String searchText = edtFilterSearch.getText().toString().trim().toLowerCase();
        String selectedStatus = spinnerReminderStatus.getSelectedItem().toString();

        List<Reminder> filtered = new ArrayList<>();

        for (Reminder r : allReminders) {

            // FILTRO POR TÍTULO
            boolean matchesText = r.getTitle() != null &&
                    r.getTitle().toLowerCase().contains(searchText);

            boolean matchesState = true;

            // FILTRO POR ESTADO DEL SPINNER
            switch (selectedStatus) {

                case "Pendientes":
                    matchesState = !r.getIsCompleted() && !r.getIsExpired();
                    break;

                case "Completados":
                    matchesState = r.getIsCompleted();
                    break;

                case "Expirados":
                    matchesState = r.getIsExpired();
                    break;

                case "Todos":
                default:
                    matchesState = true;
                    break;
            }

            if (matchesText && matchesState) {
                filtered.add(r);
            }
        }

        adapter.updateList(filtered);
    }


    // =========================================================
    // El resto del fragment permanece **exactamente igual**
    // =========================================================

    private void cleanFilters() {

        // Limpiar texto
        edtFilterSearch.setText("");

        // Resetear spinner a "Todos"
        spinnerReminderStatus.setSelection(0);

        // Restaurar lista completa
        adapter.updateList(new ArrayList<>(allReminders));
    }

/**
     * Carga TODOS los usuarios una vez y luego llama a loadReminders().
     * De esta forma garantizamos que usersMap ya está poblado cuando armamos los nombres.
     */
    private void loadUsers() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // Guardamos todo el nodo del usuario para poder acceder a name u otros campos
                    usersMap.put(ds.getKey(), (Map<String, Object>) ds.getValue());
                }
                Log.d("RemindersFragment", "Usuarios cargados: " + usersMap.size());

                // ahora que tenemos usuarios, cargamos los reminders
                loadReminders();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RemindersFragment", "Error cargando usuarios: " + error.getMessage());
                // incluso si falla, intentamos cargar reminders para no dejar la UI vacía
                loadReminders();
            }
        });
    }

    /**
     * Escucha los reminders y construye sharedUsersMap usando usersMap (ya poblado).
     */
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
                    reminderList.add(r);
                }

                // 🔥🔥🔥 AÑADE SOLO ESTO 🔥🔥🔥
                allReminders.clear();
                allReminders.addAll(reminderList);
                // ---------------------------------

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
