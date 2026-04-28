package com.example.finanzly.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.finanzly.R;
import com.google.firebase.auth.FirebaseAuth;

public class PasswordResetDialog extends DialogFragment {

    private FirebaseAuth auth;

    private View rootView;
    private TextView tvError;
    private Button btnSend, btnCancel;
    private ProgressBar progressBar;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        auth = FirebaseAuth.getInstance();

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        rootView = inflater.inflate(R.layout.dialog_password_reset, null);

        tvError = rootView.findViewById(R.id.tvError);
        btnSend = rootView.findViewById(R.id.btnSend);
        btnCancel = rootView.findViewById(R.id.btnCancel);
        progressBar = rootView.findViewById(R.id.progressBar);
        final TextView etEmail = rootView.findViewById(R.id.etEmail);

        btnSend.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                tvError.setText("Introduce un correo válido");
                tvError.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            tvError.setText("");

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            tvError.setText(" Correo enviado correctamente");
                            tvError.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        } else {
                            String msg;
                            if (task.getException() != null) {
                                String exMsg = task.getException().getMessage();
                                if (exMsg.contains("There is no user record")) {
                                    msg = "No existe ninguna cuenta con ese correo";
                                } else if (exMsg.contains("badly formatted")) {
                                    msg = "Correo electrónico no válido";
                                } else {
                                    msg = "Error al enviar el correo de recuperación";
                                }
                            } else {
                                msg = "Error al enviar el correo de recuperación";
                            }

                            tvError.setText(msg);
                            tvError.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        }
                    });
        });

        btnCancel.setOnClickListener(v -> dismiss());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(rootView);
        builder.setCancelable(true);

        return builder.create();
    }
}
