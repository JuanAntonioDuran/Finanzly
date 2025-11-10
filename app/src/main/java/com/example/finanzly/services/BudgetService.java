package com.example.finanzly.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.finanzly.models.Budget;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class BudgetService {

    private final DatabaseReference reference;

    public BudgetService(Context context) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        reference = db.getReference("budgets");
    }

    /**
     * ➕ Inserta un nuevo presupuesto
     */
    public String insert(@NonNull Budget budget) {
        DatabaseReference newRef = reference.push();
        String id = newRef.getKey();

        // Fecha actual en formato ISO
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        budget.setId(id);
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);
        if (budget.getSpent() == 0) budget.setSpent(0);

        newRef.setValue(budget);
        return id;
    }

    /**
     * ✏️ Actualiza completamente un presupuesto
     */
    public void update(@NonNull String id, @NonNull Budget budget) {
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        budget.setUpdatedAt(now);
        reference.child(id).setValue(budget);
    }

    /**
     * 🔄 Actualiza solo campos específicos sin sobrescribir todo el objeto
     */
    public void updatePartial(@NonNull String id, @NonNull Map<String, Object> updates) {
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        updates.put("updatedAt", now);
        reference.child(id).updateChildren(updates);
    }

    /**
     * ❌ Elimina un presupuesto por su ID
     */
    public void delete(@NonNull String id) {
        reference.child(id).removeValue();
    }

    /**
     * 📋 Obtiene la referencia principal (todos los presupuestos)
     */
    public DatabaseReference getReference() {
        return reference;
    }

    /**
     * 🔍 Obtiene una referencia a un presupuesto por su ID
     */
    public DatabaseReference getById(@NonNull String id) {
        return reference.child(id);
    }

    /**
     * ➕ Añade un colaborador al presupuesto
     */
    public void addCollaborator(@NonNull String budgetId, @NonNull String userId) {
        reference.child(budgetId)
                .child("sharedUserIds")
                .push()
                .setValue(userId);
    }

    /**
     * ➖ Elimina un colaborador del presupuesto
     */
    public void removeCollaborator(@NonNull String budgetId, @NonNull String userId) {
        // Buscar y eliminar el ID dentro de sharedUserIds
        reference.child(budgetId).child("sharedUserIds")
                .get().addOnSuccessListener(snapshot -> {
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
}
