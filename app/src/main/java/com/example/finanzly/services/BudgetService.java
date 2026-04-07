package com.example.finanzly.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.finanzly.models.Budget;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
     *  Inserta un nuevo presupuesto
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
     *  Actualiza completamente un presupuesto
     */
    public void update(@NonNull String id, @NonNull Budget budget) {
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        budget.setUpdatedAt(now);
        reference.child(id).setValue(budget);
    }


    /**
     *  Elimina un presupuesto por su ID
     */
    public void delete(@NonNull String id) {
        reference.child(id).removeValue();
    }

    /**
     *  Obtiene la referencia principal (todos los presupuestos)
     */
    public DatabaseReference getReference() {
        return reference;
    }





}
