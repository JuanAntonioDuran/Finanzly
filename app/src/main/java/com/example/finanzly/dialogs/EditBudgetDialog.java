package com.example.finanzly.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class EditBudgetDialog {

    public interface OnBudgetEditedListener {
        void onBudgetEdited(Budget updatedBudget);
    }

    private final Context context;
    private final Budget budget;
    private final boolean isOwner;
    private String currentUserId;
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
        this.currentUserName = currentUserName != null ? currentUserName : "Usuario";
        this.listener = listener;
    }

    public void show() {

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        }

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(context, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

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

            // 1. Eliminar de lista local
            collaboratorList.remove(user);
            budget.getSharedUserIds().remove(user.getUid());

            adapter.notifyDataSetChanged();
            updateRecyclerVisibility(rvCollaborators);

            // 2.  ACTUALIZAR FIREBASE AL INSTANTE
            DatabaseReference budgetsRef = FirebaseDatabase.getInstance().getReference("budgets");

            Map<String, Object> updates = new HashMap<>();
            updates.put("sharedUserIds", budget.getSharedUserIds());

            budgetsRef.child(budget.getId())
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(context, "Colaborador eliminado", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error al eliminar colaborador", Toast.LENGTH_SHORT).show();

                        //  Revertir si falla
                        collaboratorList.add(user);
                        budget.getSharedUserIds().add(user.getUid());
                        adapter.notifyDataSetChanged();
                        updateRecyclerVisibility(rvCollaborators);
                    });
        });

        rvCollaborators.setLayoutManager(new LinearLayoutManager(context));
        rvCollaborators.setAdapter(adapter);
        rvCollaborators.setVisibility(View.GONE);

        //  Cargar SOLO colaboradores reales
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
                updateRecyclerVisibility(rvCollaborators);
            });
        }

        if (!isOwner) {
            layoutCollaborators.setVisibility(View.GONE);
        }

        //  INVITAR
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

                        if (invitedUser.getUid().equals(currentUserId)) {
                            Toast.makeText(context, "No puedes invitarte a ti mismo", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (budget.getSharedUserIds().contains(invitedUser.getUid())) {
                            Toast.makeText(context, "Usuario ya es colaborador", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        checkExistingInvitation(invitedUser.getUid(), budget.getId(), exists -> {

                            if (exists) {
                                Toast.makeText(context, "Ya existe una invitación pendiente", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String now = getCurrentUTCDate();

                            Invitation invitation = new Invitation();
                            invitation.setFromUserId(currentUserId);
                            invitation.setToUserId(invitedUser.getUid());
                            invitation.setResourceType("budget");
                            invitation.setResourceIdBudget(budget.getId());
                            invitation.setCreatedAt(now);
                            invitation.setStatus("pending");

                            invitation.setMessage(
                                    currentUserName + " te ha invitado al presupuesto \"" + budget.getCategory() + "\""
                            );

                            invitationService.createInvitation(invitation,
                                    id -> Toast.makeText(context, "Invitación enviada", Toast.LENGTH_SHORT).show(),
                                    e -> Toast.makeText(context, "Error al enviar invitación", Toast.LENGTH_SHORT).show()
                            );

                            etEmail.setText("");
                        });

                        return;
                    }
                }

                if (!found) {
                    Toast.makeText(context, "No existe usuario con ese email", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnSave.setOnClickListener(v -> {
            String category = etCategory.getText().toString().trim();
            String limitStr = etLimit.getText().toString().trim();

            if (TextUtils.isEmpty(category)) {
                etCategory.setError("Requerido");
                return;
            }

            if (TextUtils.isEmpty(limitStr)) {
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

            if (limit < 0) {
                etLimit.setError("No puede ser menor a 0");
                return;
            }

            budget.setCategory(category);
            budget.setLimit(limit);
            budget.setUpdatedAt(getCurrentUTCDate());

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

    private void updateRecyclerVisibility(RecyclerView rv) {
        if (collaboratorList != null && !collaboratorList.isEmpty()) {
            rv.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.GONE);
        }
    }

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