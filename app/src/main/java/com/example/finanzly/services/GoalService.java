package com.example.finanzly.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.finanzly.models.Goal;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GoalService {

    private final DatabaseReference reference;

    public GoalService(Context context) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        reference = db.getReference("goals");
    }

    /**
     * ➕ Crea una nueva meta
     */
    public String insert(@NonNull Goal goal) {
        DatabaseReference newRef = reference.push();
        String id = newRef.getKey();

        // Fecha actual en formato ISO
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        goal.setId(id);
        goal.setCreatedAt(now);
        newRef.setValue(goal);

        return id;
    }

    /**
     * ✏️ Actualiza toda la meta
     */
    public void update(@NonNull String id, @NonNull Goal goal) {
        reference.child(id).setValue(goal);
    }

    /**
     * 🔄 Actualiza solo campos específicos sin sobrescribir todo el objeto
     */
    public void updatePartial(@NonNull String id, @NonNull Map<String, Object> updates) {
        reference.child(id).updateChildren(updates);
    }

    /**
     * ❌ Elimina una meta por su ID
     */
    public void delete(@NonNull String id) {
        reference.child(id).removeValue();
    }

    /**
     * 📋 Devuelve la referencia principal (todas las metas)
     */
    public DatabaseReference getReference() {
        return reference;
    }

    /**
     * 🔍 Devuelve la referencia a una meta específica
     */
    public DatabaseReference getById(@NonNull String id) {
        return reference.child(id);
    }

    /**
     * ➕ Añade un colaborador (usuario compartido)
     */
    public void addSharedUser(@NonNull String goalId, @NonNull String userId) {
        reference.child(goalId)
                .child("sharedUserIds")
                .push()
                .setValue(userId);
    }

    /**
     * ➖ Elimina un colaborador
     */
    public void removeSharedUser(@NonNull String goalId, @NonNull String userId) {
        reference.child(goalId)
                .child("sharedUserIds")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (userId.equals(child.getValue(String.class))) {
                                child.getRef().removeValue();
                                break;
                            }
                        }
                    }
                });
    }

    /**
     * 📊 Incrementa el progreso de la meta (currentAmount)
     */
    public void addProgress(@NonNull String goalId, double amountToAdd) {
        reference.child(goalId)
                .child("currentAmount")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Double current = snapshot.getValue(Double.class);
                    if (current == null) current = 0.0;
                    double newAmount = current + amountToAdd;
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("currentAmount", newAmount);
                    reference.child(goalId).updateChildren(updates);
                });
    }

    /**
     * 📉 Reduce el progreso (por si se retira dinero del ahorro)
     */
    public void subtractProgress(@NonNull String goalId, double amountToSubtract) {
        reference.child(goalId)
                .child("currentAmount")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Double current = snapshot.getValue(Double.class);
                    if (current == null) current = 0.0;
                    double newAmount = Math.max(0, current - amountToSubtract);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("currentAmount", newAmount);
                    reference.child(goalId).updateChildren(updates);
                });
    }
}
