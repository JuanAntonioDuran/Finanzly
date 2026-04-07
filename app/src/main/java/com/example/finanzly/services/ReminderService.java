package com.example.finanzly.services;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.finanzly.models.Reminder;
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

public class ReminderService {

    private final DatabaseReference reference;

    public ReminderService(Context context) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        reference = db.getReference("reminders");
    }



    public void deleteByGoalId(String goalId, Runnable onComplete) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("reminders");

        ref.orderByChild("linkedGoalId").equalTo(goalId)
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


    public void deleteByBudgetId(String budgetId, Runnable onComplete) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("reminders");

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
    /**
     * ️ Actualiza completamente un recordatorio
     */
    public void update(@NonNull String id, @NonNull Reminder reminder) {
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        reminder.setUpdatedAt(now);
        reference.child(id).setValue(reminder);
    }




    /**
     *  Referencia a todos los recordatorios
     */
    public DatabaseReference getReference() {
        return reference;
    }






}
