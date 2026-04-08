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
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.Invitation;
import com.example.finanzly.models.User;
import com.example.finanzly.services.InvitationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class EditGoalDialog {

    public interface OnGoalEditedListener {
        void onGoalEdited(Goal updatedGoal);
    }

    private final Context context;
    private final Goal goal;
    private final boolean isOwner;
    private String currentUserId;
    private final String currentUserName;
    private final OnGoalEditedListener listener;

    private AlertDialog dialog;

    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
    private final DatabaseReference invitationsRef = FirebaseDatabase.getInstance().getReference("invitations");
    private final InvitationService invitationService = new InvitationService();

    private List<User> collaboratorList;
    private UserAdapter adapter;

    public EditGoalDialog(Context context, Goal goal, boolean isOwner,
                          String currentUserId, String currentUserName,
                          OnGoalEditedListener listener) {
        this.context = context;
        this.goal = goal;
        this.isOwner = isOwner;
        this.currentUserId = currentUserId;
        this.currentUserName = currentUserName != null ? currentUserName : "";
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

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_goal, null);

        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etTargetAmount = view.findViewById(R.id.etTargetAmount);
        EditText etDeadline = view.findViewById(R.id.etDeadline);
        EditText etEmail = view.findViewById(R.id.etEmail);
        ImageButton btnAddEmail = view.findViewById(R.id.btnAddEmail);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        LinearLayout layoutCollaborators = view.findViewById(R.id.layoutCollaborators);
        RecyclerView rvCollaborators = view.findViewById(R.id.rvCollaborators);

        etTitle.setText(goal.getTitle());
        etTargetAmount.setText(String.valueOf(goal.getTargetAmount()));
        etDeadline.setText(goal.getDeadline() != null ? goal.getDeadline() : "");

        if (goal.getSharedUserIds() == null) {
            goal.setSharedUserIds(new ArrayList<>());
        }

        collaboratorList = new ArrayList<>();

        adapter = new UserAdapter(collaboratorList, user -> {

            // 1. Eliminar de lista local
            collaboratorList.remove(user);
            goal.getSharedUserIds().remove(user.getUid());

            adapter.notifyDataSetChanged();
            updateRecyclerVisibility(rvCollaborators);

            // 2. 🔥 ACTUALIZAR FIREBASE AL INSTANTE
            DatabaseReference goalsRef = FirebaseDatabase.getInstance().getReference("goals");

            Map<String, Object> updates = new HashMap<>();
            updates.put("sharedUserIds", goal.getSharedUserIds());

            goalsRef.child(goal.getId())
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(context, "Colaborador eliminado", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error al eliminar colaborador", Toast.LENGTH_SHORT).show();


                        collaboratorList.add(user);
                        goal.getSharedUserIds().add(user.getUid());
                        adapter.notifyDataSetChanged();
                        updateRecyclerVisibility(rvCollaborators);
                    });
        });

        rvCollaborators.setLayoutManager(new LinearLayoutManager(context));
        rvCollaborators.setAdapter(adapter);
        rvCollaborators.setVisibility(View.GONE);

        // 🔹 Cargar SOLO colaboradores reales
        if (!goal.getSharedUserIds().isEmpty()) {
            usersRef.get().addOnSuccessListener(snapshot -> {
                for (String uid : goal.getSharedUserIds()) {
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

        // ➕ Invitar usuario (SIN añadirlo aún como colaborador)
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

                        if (goal.getSharedUserIds().contains(invitedUser.getUid())) {
                            Toast.makeText(context, "Usuario ya es colaborador", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        checkExistingInvitation(invitedUser.getUid(), goal.getId(), exists -> {

                            if (exists) {
                                Toast.makeText(context, "Ya existe una invitación pendiente", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String now = getCurrentUTCDate();

                            Invitation invitation = new Invitation();
                            invitation.setFromUserId(currentUserId);
                            invitation.setToUserId(invitedUser.getUid());
                            invitation.setResourceType("goal");
                            invitation.setResourceIdGoal(goal.getId());
                            invitation.setCreatedAt(now);
                            invitation.setStatus("pending");

                            invitation.setMessage(
                                    currentUserName + " te ha invitado al objetivo \"" + goal.getTitle() + "\""
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
            String title = etTitle.getText().toString().trim();
            String targetStr = etTargetAmount.getText().toString().trim();
            String deadline = etDeadline.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Requerido");
                return;
            }

            if (TextUtils.isEmpty(targetStr)) {
                etTargetAmount.setError("Requerido");
                return;
            }

            double targetAmount;
            try {
                targetAmount = Double.parseDouble(targetStr);
            } catch (NumberFormatException e) {
                etTargetAmount.setError("Número inválido");
                return;
            }

            if (targetAmount < 0) {
                etTargetAmount.setError("No puede ser menor a 0");
                return;
            }

            goal.setTitle(title);
            goal.setTargetAmount(targetAmount);
            goal.setDeadline(!deadline.isEmpty() ? deadline : null);
            goal.setUpdatedAt(getCurrentUTCDate());

            listener.onGoalEdited(goal);
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

    private void checkExistingInvitation(String toUserId, String goalId, OnCheckInvitation callback) {
        invitationsRef.get().addOnSuccessListener(snapshot -> {
            boolean exists = false;

            for (DataSnapshot child : snapshot.getChildren()) {
                Invitation inv = child.getValue(Invitation.class);

                if (inv != null
                        && inv.getToUserId().equals(toUserId)
                        && goalId.equals(inv.getResourceIdGoal())
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