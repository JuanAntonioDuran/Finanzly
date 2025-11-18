package com.example.finanzly.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.finanzly.models.Budget;
import com.example.finanzly.models.Goal;
import com.example.finanzly.models.Movement;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
     * ➕ Inserta un nuevo movimiento
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
     * ✏️ Actualiza completamente un movimiento
     */
    public void update(@NonNull String id, @NonNull Movement movement) {
        reference.child(id).setValue(movement);
    }

    /**
     * 🔄 Actualiza solo algunos campos del movimiento
     */
    public void updatePartial(@NonNull String id, @NonNull Map<String, Object> updates) {
        reference.child(id).updateChildren(updates);
    }

    /**
     * ❌ Elimina un movimiento por su ID
     */
    public void delete(@NonNull String id) {
        reference.child(id).removeValue();
    }

    /**
     * 📋 Devuelve la referencia principal (todos los movimientos)
     */
    public DatabaseReference getReference() {
        return reference;
    }

    /**
     * 🔍 Devuelve la referencia de un movimiento por ID
     */
    public DatabaseReference getById(@NonNull String id) {
        return reference.child(id);
    }

    /**
     * 🔎 Obtiene movimientos relacionados con un presupuesto específico
     */
    public DatabaseReference getByBudgetId(@NonNull String budgetId) {
        return reference.orderByChild("linkedBudgetId").equalTo(budgetId).getRef();
    }

    /**
     * 🔎 Obtiene movimientos relacionados con una meta específica
     */
    public DatabaseReference getByGoalId(@NonNull String goalId) {
        return reference.orderByChild("linkedGoalId").equalTo(goalId).getRef();
    }

    /**
     * 🔎 Obtiene movimientos de un usuario específico
     */
    public DatabaseReference getByUserId(@NonNull String userId) {
        return reference.orderByChild("userId").equalTo(userId).getRef();
    }

    /**
     * 💰 Calcula el gasto total de un usuario (desde Firebase)
     */
    public void calculateUserTotalSpent(@NonNull String userId, @NonNull OnTotalCalculatedListener listener) {
        reference.orderByChild("userId").equalTo(userId).get()
                .addOnSuccessListener(snapshot -> {
                    double total = 0;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Movement m = child.getValue(Movement.class);
                        if (m != null && "expense".equalsIgnoreCase(m.getType())) {
                            total += m.getAmount();
                        }
                    }
                    listener.onTotalCalculated(total);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }





    /**
     * 🧩 Interfaz callback para cálculos asíncronos
     */
    public interface OnTotalCalculatedListener {
        void onTotalCalculated(double total);
        void onError(String error);
    }
}
