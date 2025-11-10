package com.example.finanzly.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.finanzly.models.Reminder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReminderService {

    private final DatabaseReference reference;

    public ReminderService(Context context) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        reference = db.getReference("reminders");
    }

    /**
     * ➕ Crea un nuevo recordatorio
     */
    public String insert(@NonNull Reminder reminder) {
        DatabaseReference newRef = reference.push();
        String id = newRef.getKey();

        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        reminder.setId(id);
        reminder.setCreatedAt(now);
        reminder.setUpdatedAt(now);

        newRef.setValue(reminder);
        return id;
    }

    /**
     * ✏️ Actualiza completamente un recordatorio
     */
    public void update(@NonNull String id, @NonNull Reminder reminder) {
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        reminder.setUpdatedAt(now);
        reference.child(id).setValue(reminder);
    }

    /**
     * 🔄 Actualiza solo campos específicos
     */
    public void updatePartial(@NonNull String id, @NonNull Map<String, Object> updates) {
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        updates.put("updatedAt", now);
        reference.child(id).updateChildren(updates);
    }

    /**
     * ❌ Elimina un recordatorio
     */
    public void delete(@NonNull String id) {
        reference.child(id).removeValue();
    }

    /**
     * 📋 Referencia a todos los recordatorios
     */
    public DatabaseReference getReference() {
        return reference;
    }

    /**
     * 🔍 Referencia a un recordatorio por ID
     */
    public DatabaseReference getById(@NonNull String id) {
        return reference.child(id);
    }

    /**
     * ➕ Añade un usuario compartido al recordatorio
     */
    public void addSharedUser(@NonNull String reminderId, @NonNull String userId) {
        reference.child(reminderId)
                .child("sharedUserIds")
                .push()
                .setValue(userId);
    }

    /**
     * ➖ Elimina un usuario compartido del recordatorio
     */
    public void removeSharedUser(@NonNull String reminderId, @NonNull String userId) {
        reference.child(reminderId).child("sharedUserIds")
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

    /**
     * ✅ Marca un recordatorio como completado por un usuario
     */
    public void markCompleted(@NonNull String reminderId, @NonNull String userId) {
        reference.child(reminderId)
                .child("sharedUsersStatus")
                .child(userId)
                .setValue(true);
    }

    /**
     * ❌ Marca un recordatorio como no completado por un usuario
     */
    public void markUncompleted(@NonNull String reminderId, @NonNull String userId) {
        reference.child(reminderId)
                .child("sharedUsersStatus")
                .child(userId)
                .setValue(false);
    }
}
