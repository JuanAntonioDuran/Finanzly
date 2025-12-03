package com.example.finanzly.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.*;

public class EditGoalDialog {

    public interface OnGoalEditedListener {
        void onGoalEdited(Goal updatedGoal);
    }

    private final Context context;
    private final Goal goal;
    private final boolean isOwner;
    private final String currentUserId;
    private final String currentUserName;
    private final OnGoalEditedListener listener;

    private AlertDialog dialog;

    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
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
        this.currentUserName = currentUserName;
        this.listener = listener;
    }

    public void show() {
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

        // Cargar datos existentes
        etTitle.setText(goal.getTitle());
        etTargetAmount.setText(String.valueOf(goal.getTargetAmount()));
        etDeadline.setText(goal.getDeadline() != null ? goal.getDeadline() : "");

        if (goal.getSharedUserIds() == null) {
            goal.setSharedUserIds(new ArrayList<>());
        }

        // Inicializar lista y adapter de colaboradores
        collaboratorList = new ArrayList<>();
        adapter = new UserAdapter(collaboratorList, user -> {
            collaboratorList.remove(user);
            goal.getSharedUserIds().remove(user.getUid());
            adapter.notifyDataSetChanged();
        });

        rvCollaborators.setLayoutManager(new LinearLayoutManager(context));
        rvCollaborators.setAdapter(adapter);

        // Cargar usuarios existentes de Firebase para mostrar nombres reales
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
            }).addOnFailureListener(e ->
                    Toast.makeText(context, "Error cargando colaboradores: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }

        if (!isOwner) {
            layoutCollaborators.setVisibility(View.GONE);
        }

        // Agregar colaborador por email y crear invitación
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

                        // Solo enviamos invitación, no añadimos todavía al Goal
                        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                .format(new Date());
                        Invitation invitation = new Invitation();
                        invitation.setFromUserId(currentUserId);
                        invitation.setToUserId(invitedUser.getUid());
                        invitation.setResourceType("goal");
                        invitation.setResourceIdGoal(goal.getId());
                        invitation.setCreatedAt(now);
                        invitation.setStatus("pending");
                        invitation.setMessage(currentUserName + " te ha invitado al objetivo \"" + goal.getTitle() + "\"");

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


        // Guardar cambios del goal
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
}
