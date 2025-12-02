package com.example.finanzly.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
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
    private final InvitationService invitationService = new InvitationService();

    // ✅ A nivel de clase
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

        // Inicializamos la lista y adapter con nombres reales
        collaboratorList = new ArrayList<>();
        adapter = new UserAdapter(collaboratorList, user -> {
            collaboratorList.remove(user);
            budget.getSharedUserIds().remove(user.getUid());
            adapter.notifyDataSetChanged();
        });

        rvCollaborators.setLayoutManager(new LinearLayoutManager(context));
        rvCollaborators.setAdapter(adapter);

        // Cargar los usuarios existentes de Firebase para mostrar nombres reales
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

        // Agregar colaborador por email
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

                        if (budget.getSharedUserIds().contains(invitedUser.getUid())) {
                            Toast.makeText(context, "Usuario ya agregado", Toast.LENGTH_SHORT).show();
                            etEmail.setText("");
                            break;
                        }

                        // Agregar al listado temporal y al presupuesto
                        collaboratorList.add(invitedUser);
                        budget.getSharedUserIds().add(invitedUser.getUid());
                        adapter.notifyDataSetChanged();

                        // Crear invitación en Firebase
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
                        break;
                    }
                }

                if (!found) {
                    Toast.makeText(context, "No existe usuario con email: " + email, Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(context, "Error cargando usuarios: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });

        // Guardar cambios del presupuesto
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

            budget.setCategory(category);
            budget.setLimit(limit);

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

}
