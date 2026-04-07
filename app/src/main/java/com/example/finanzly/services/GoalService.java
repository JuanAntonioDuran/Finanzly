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
     *  Crea una nueva meta
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
     * ✏ Actualiza toda la meta
     */
    public void update(@NonNull String id, @NonNull Goal goal) {
        reference.child(id).setValue(goal);
    }

    /**
     *  Actualiza solo campos específicos sin sobrescribir todo el objeto
     */
    public void updatePartial(@NonNull String id, @NonNull Map<String, Object> updates) {
        reference.child(id).updateChildren(updates);
    }

    /**
     *  Elimina una meta por su ID
     */
    public void delete(@NonNull String id) {
        reference.child(id).removeValue();
    }

    /**
     *  Devuelve la referencia principal (todas las metas)
     */
    public DatabaseReference getReference() {
        return reference;
    }

    /**
     *  Devuelve la referencia a una meta específica
     */
    public DatabaseReference getById(@NonNull String id) {
        return reference.child(id);
    }














}
