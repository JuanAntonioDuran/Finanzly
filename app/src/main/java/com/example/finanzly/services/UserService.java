package com.example.finanzly.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.finanzly.models.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserService {

    private final DatabaseReference reference;

    public UserService(Context context) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        reference = db.getReference("users");
    }

    /**
     * ➕ Crea un nuevo usuario
     */
    public String insert(@NonNull User user) {
        DatabaseReference newRef = reference.push();
        String id = newRef.getKey();

        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        user.setUid(id);
        user.setCreatedAt(now);

        newRef.setValue(user);
        return id;
    }

    /**
     * ✏️ Actualiza completamente un usuario
     */
    public void update(@NonNull String uid, @NonNull User user) {
        reference.child(uid).setValue(user);
    }

    /**
     * 🔄 Actualiza solo campos específicos del usuario
     */
    public void updatePartial(@NonNull String uid, @NonNull java.util.Map<String, Object> updates) {
        reference.child(uid).updateChildren(updates);
    }

    /**
     * ❌ Elimina un usuario
     */
    public void delete(@NonNull String uid) {
        reference.child(uid).removeValue();
    }

    /**
     * 📋 Devuelve la referencia principal de usuarios
     */
    public DatabaseReference getReference() {
        return reference;
    }

    /**
     * 🔍 Devuelve la referencia a un usuario específico
     */
    public DatabaseReference getById(@NonNull String uid) {
        return reference.child(uid);
    }
}
