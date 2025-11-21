package com.example.finanzly.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.finanzly.R;
import com.example.finanzly.models.User;
import com.example.finanzly.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserFragment extends Fragment {

    private TextView tvUid, tvEmail, tvCreatedAt;
    private EditText etName;
    private Button btnUpdateName, btnChangePassword, btnLogout;

    private UserService userService;
    private DatabaseReference usersRef;
    private String uid;

    public UserFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Inicializar views
        tvUid = view.findViewById(R.id.tvUid);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt);
        etName = view.findViewById(R.id.etName);

        btnUpdateName = view.findViewById(R.id.btnUpdateName);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Firebase
        uid = FirebaseAuth.getInstance().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Servicio de usuarios
        userService = new UserService(requireContext());

        // Cargar datos del usuario
        loadUserData();

        // Botón actualizar nombre
        btnUpdateName.setOnClickListener(v -> updateName());

        // Botón cambiar contraseña
        btnChangePassword.setOnClickListener(v -> sendPasswordResetEmail());

        // Botón cerrar sesión
        btnLogout.setOnClickListener(v -> logout());

        return view;
    }

    private void loadUserData() {
        if (TextUtils.isEmpty(uid)) return;

        usersRef.child(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvUid.setText(user.getUid());
                    etName.setText(user.getName());
                    tvEmail.setText(user.getEmail());
                    tvCreatedAt.setText(user.getCreatedAt());
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(), "Error cargando usuario", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateName() {
        String newName = etName.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        usersRef.child(uid).child("name").setValue(newName)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error al actualizar nombre", Toast.LENGTH_SHORT).show());
    }

    private void sendPasswordResetEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(user.getEmail())
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(requireContext(), "Correo de cambio de contraseña enviado", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error enviando correo", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(requireContext(), "No se pudo obtener el correo del usuario", Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();
                    requireActivity().finish();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
