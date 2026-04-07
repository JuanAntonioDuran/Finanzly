package com.example.finanzly.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.Movement;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MovementService {

    private final DatabaseReference reference;

    public MovementService(Context context) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        reference = db.getReference("movements");
    }

    /**
     * Inserta un nuevo movimiento
     */
    public String insert(@NonNull Movement movement) {
        DatabaseReference newRef = reference.push();
        String id = newRef.getKey();

        // Fecha actual en formato ISO si no se proporciona
        if (movement.getDate() == null || movement.getDate().isEmpty()) {
            String now = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            movement.setDate(now);
        }

        movement.setId(id);
        newRef.setValue(movement);
        return id;
    }

    /**
     *  Actualiza completamente un movimiento
     */
    public void update(@NonNull String id, @NonNull Movement movement) {
        reference.child(id).setValue(movement);
    }



    /**
     *  Elimina un movimiento por su ID
     */
    public void delete(@NonNull String id) {
        reference.child(id).removeValue();
    }

    /**
     *  Devuelve la referencia principal (todos los movimientos)
     */
    public DatabaseReference getReference() {
        return reference;
    }



    public void deleteByGoalId(String goalId, Runnable onComplete) {

        reference.orderByChild("linkedGoalId").equalTo(goalId)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().removeValue();
                        }

                        if (onComplete != null) onComplete.run();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public void deleteByBudgetId(String budgetId, Runnable onComplete) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("movements");

        ref.orderByChild("linkedBudgetId").equalTo(budgetId)
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






}
