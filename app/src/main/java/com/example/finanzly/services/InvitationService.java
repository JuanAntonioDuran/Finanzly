package com.example.finanzly.services;

import androidx.annotation.NonNull;

import com.example.finanzly.models.Invitation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InvitationService {

    private final DatabaseReference invitationsRef;

    public InvitationService() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        invitationsRef = db.getReference("invitations"); // 🔥 Tabla en Realtime DB
    }

    // ✅ CREATE (con ID autogenerado)
    public void createInvitation(Invitation invitation,
                                 OnSuccessListener<String> successListener,
                                 OnFailureListener failureListener) {

        String id = invitationsRef.push().getKey();
        if (id == null) {
            failureListener.onFailure(new Exception("Error generando ID"));
            return;
        }

        invitation.setId(id);

        invitationsRef.child(id)
                .setValue(invitation)
                .addOnSuccessListener(unused -> successListener.onSuccess(id))
                .addOnFailureListener(failureListener);
    }

    // ✅ READ – Obtener invitación por ID
    public void getInvitationById(String id,
                                  OnSuccessListener<Invitation> successListener,
                                  OnFailureListener failureListener) {

        invitationsRef.child(id).get()
                .addOnSuccessListener(dataSnapshot -> {
                    Invitation invitation = dataSnapshot.getValue(Invitation.class);
                    successListener.onSuccess(invitation);
                })
                .addOnFailureListener(failureListener);
    }

    // ✅ READ – Obtener invitaciones recibidas por un usuario
    public void getInvitationsForUser(String userId,
                                      OnSuccessListener<Iterable<Invitation>> successListener,
                                      OnFailureListener failureListener) {

        invitationsRef.orderByChild("toUserId")
                .equalTo(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Invitation> list = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Invitation inv = child.getValue(Invitation.class);
                        list.add(inv);
                    }
                    successListener.onSuccess(list);
                })
                .addOnFailureListener(failureListener);
    }

    public void deleteByBudgetId(String budgetId, Runnable onComplete) {

        invitationsRef.orderByChild("resourceIdBudget").equalTo(budgetId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        if (onComplete != null) onComplete.run();
                        return;
                    }

                    int total = (int) snapshot.getChildrenCount();
                    final int[] counter = {0};

                    for (DataSnapshot child : snapshot.getChildren()) {
                        child.getRef().removeValue().addOnCompleteListener(task -> {
                            counter[0]++;
                            if (counter[0] == total && onComplete != null) {
                                onComplete.run();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (onComplete != null) onComplete.run();
                });
    }


    public void deleteByGoalId(String goalId, Runnable onComplete) {

        final int[] counter = {0};
        final int totalQueries = 2;

        Runnable checkComplete = () -> {
            counter[0]++;
            if (counter[0] == totalQueries && onComplete != null) {
                onComplete.run();
            }
        };

        // 🔴 Query 1
        invitationsRef.orderByChild("resourceIdGoal").equalTo(goalId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        checkComplete.run();
                        return;
                    }

                    int total = (int) snapshot.getChildrenCount();
                    final int[] innerCounter = {0};

                    for (DataSnapshot child : snapshot.getChildren()) {
                        child.getRef().removeValue().addOnCompleteListener(task -> {
                            innerCounter[0]++;
                            if (innerCounter[0] == total) {
                                checkComplete.run();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> checkComplete.run());

        // 🔴 Query 2 (por si usas otro campo)
        invitationsRef.orderByChild("linkedGoalId").equalTo(goalId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        checkComplete.run();
                        return;
                    }

                    int total = (int) snapshot.getChildrenCount();
                    final int[] innerCounter = {0};

                    for (DataSnapshot child : snapshot.getChildren()) {
                        child.getRef().removeValue().addOnCompleteListener(task -> {
                            innerCounter[0]++;
                            if (innerCounter[0] == total) {
                                checkComplete.run();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> checkComplete.run());
    }


    // ✅ UPDATE
    public void updateInvitation(String id,
                                 Invitation invitation,
                                 OnSuccessListener<Void> successListener,
                                 OnFailureListener failureListener) {

        invitationsRef.child(id)
                .setValue(invitation)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // ✅ DELETE
    public void deleteInvitation(String id,
                                 OnSuccessListener<Void> successListener,
                                 OnFailureListener failureListener) {

        invitationsRef.child(id)
                .removeValue()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }


    public void getInvitationsSentByUser(String userId,
                                         OnSuccessListener<Iterable<Invitation>> successListener,
                                         OnFailureListener failureListener) {
        invitationsRef.orderByChild("fromUserId")
                .equalTo(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Invitation> list = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Invitation inv = child.getValue(Invitation.class);
                        list.add(inv);
                    }
                    successListener.onSuccess(list);
                })
                .addOnFailureListener(failureListener);
    }

}
