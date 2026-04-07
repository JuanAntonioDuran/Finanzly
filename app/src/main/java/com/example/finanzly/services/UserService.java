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
     *  Actualiza completamente un usuario
     */
    public void update(@NonNull String uid, @NonNull User user) {
        reference.child(uid).setValue(user);
    }


    /**
     *  Devuelve la referencia principal de usuarios
     */
    public DatabaseReference getReference() {
        return reference;
    }

    /**
     *  Devuelve la referencia a un usuario específico
     */
    public DatabaseReference getById(@NonNull String uid) {
        return reference.child(uid);
    }
}
