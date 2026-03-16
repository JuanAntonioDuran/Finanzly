package com.example.finanzly.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.adapters.UserAdapter;
import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Invitation;
import com.example.finanzly.models.User;
import com.example.finanzly.services.InvitationService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.*;

public class EditBudgetDialog {

    public interface OnBudgetEditedListener {
        void onBudgetEdited(Budget updatedBudget);
    }

    private final Context context;
    private final Budget budget;
    private final boolean isOwner;
    private final String currentUserId;
    private final String currentUserName;
    private final OnBudgetEditedListener listener;

    private AlertDialog dialog;

    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
    private final DatabaseReference invitationsRef = FirebaseDatabase.getInstance().getReference("invitations");
    private final InvitationService invitationService = new InvitationService();

    private List<User> collaboratorList;
    private UserAdapter adapter;

    public EditBudgetDialog(Context context, Budget budget, boolean isOwner,
                            String currentUserId, String currentUserName,
                            OnBudgetEditedListener listener) {
        this.context = context;
        this.budget = budget;
        this.isOwner = isOwner;
        this.currentUserId = currentUserId;
        this.currentUserName = currentUserName;
        this.listener = listener;
    }

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_budget, null);

        EditText etCategory = view.findViewById(R.id.etCategory);
        EditText etLimit = view.findViewById(R.id.etLimit);
        EditText etEmail = view.findViewById(R.id.etEmail);
        ImageButton btnAddEmail = view.findViewById(R.id.btnAddEmail);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        LinearLayout layoutCollaborators = view.findViewById(R.id.layoutCollaborators);
        RecyclerView rvCollaborators = view.findViewById(R.id.rvCollaborators);

        etCategory.setText(budget.getCategory());
        etLimit.setText(String.valueOf(budget.getLimit()));

        if (budget.getSharedUserIds() == null) {
            budget.setSharedUserIds(new ArrayList<>());
        }

        collaboratorList = new ArrayList<>();
        adapter = new UserAdapter(collaboratorList, user -> {
            collaboratorList.remove(user);
            budget.getSharedUserIds().remove(user.getUid());
            adapter.notifyDataSetChanged();
        });

        rvCollaborators.setLayoutManager(new LinearLayoutManager(context));
        rvCollaborators.setAdapter(adapter);

        // Cargar colaboradores existentes
        if (!budget.getSharedUserIds().isEmpty()) {
            usersRef.get().addOnSuccessListener(snapshot -> {
                for (String uid : budget.getSharedUserIds()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        User user = child.getValue(User.class);
                        if (user != null && user.getUid().equals(uid)) {
                            collaboratorList.add(user);
                            break;
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }).addOnFailureListener(e ->
                    Toast.makeText(context, "Error cargando colaboradores: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }

        if (!isOwner) {
            layoutCollaborators.setVisibility(View.GONE);
        }

        // 🔍 Comprobar si ya existe invitación pendiente
        btnAddEmail.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Ingresa un correo válido");
                return;
            }

            usersRef.get().addOnSuccessListener(snapshot -> {
                boolean found = false;

                for (DataSnapshot child : snapshot.getChildren()) {
                    User invitedUser = child.getValue(User.class);

                    if (invitedUser != null && invitedUser.getEmail().equalsIgnoreCase(email)) {
                        found = true;

                        // Ya está en el budget
                        if (budget.getSharedUserIds().contains(invitedUser.getUid())) {
                            Toast.makeText(context, "Usuario ya agregado", Toast.LENGTH_SHORT).show();
                            etEmail.setText("");
                            return;
                        }

                        // 🔍 Comprobar invitación existente
                        checkExistingInvitation(invitedUser.getUid(), budget.getId(), exists -> {

                            if (exists) {
                                Toast.makeText(context, "Ya existe una invitación pendiente", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Añadir colaborador localmente
                            collaboratorList.add(invitedUser);
                            budget.getSharedUserIds().add(invitedUser.getUid());
                            adapter.notifyDataSetChanged();

                            // Crear invitación
                            String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                    .format(new Date());

                            Invitation invitation = new Invitation();
                            invitation.setFromUserId(currentUserId);
                            invitation.setToUserId(invitedUser.getUid());
                            invitation.setResourceType("budget");
                            invitation.setResourceIdBudget(budget.getId());
                            invitation.setCreatedAt(now);
                            invitation.setStatus("pending");
                            invitation.setMessage(currentUserName + " te ha invitado al presupuesto \"" + budget.getCategory() + "\"");

                            invitationService.createInvitation(invitation,
                                    id -> Toast.makeText(context, "Invitación enviada a " + invitedUser.getName(), Toast.LENGTH_SHORT).show(),
                                    e -> Toast.makeText(context, "Error al enviar invitación", Toast.LENGTH_SHORT).show()
                            );

                            etEmail.setText("");
                        });

                        return;
                    }
                }

                if (!found) {
                    Toast.makeText(context, "No existe usuario con email: " + email, Toast.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e ->
                    Toast.makeText(context, "Error cargando usuarios: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });

        btnSave.setOnClickListener(v -> {
            String category = etCategory.getText().toString().trim();
            String limitStr = etLimit.getText().toString().trim();

            if (category.isEmpty()) {
                etCategory.setError("Requerido");
                return;
            }

            if (limitStr.isEmpty()) {
                etLimit.setError("Requerido");
                return;
            }

            double limit;
            try {
                limit = Double.parseDouble(limitStr);
            } catch (NumberFormatException e) {
                etLimit.setError("Número inválido");
                return;
            }

            String currentDate = getCurrentUTCDate();

            budget.setCategory(category);
            budget.setLimit(limit);
            budget.setUpdatedAt(currentDate);

            listener.onBudgetEdited(budget);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();

        dialog.show();
    }

    // 🔍 Método para comprobar invitaciones duplicadas
    private void checkExistingInvitation(String toUserId, String budgetId, OnCheckInvitation callback) {
        invitationsRef.get().addOnSuccessListener(snapshot -> {
            boolean exists = false;

            for (DataSnapshot child : snapshot.getChildren()) {
                Invitation inv = child.getValue(Invitation.class);

                if (inv != null
                        && inv.getToUserId().equals(toUserId)
                        && budgetId.equals(inv.getResourceIdBudget())
                        && "pending".equals(inv.getStatus())) {
                    exists = true;
                    break;
                }
            }

            callback.onResult(exists);

        }).addOnFailureListener(e -> callback.onResult(false));
    }

    interface OnCheckInvitation {
        void onResult(boolean exists);
    }

    private String getCurrentUTCDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}