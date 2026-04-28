package com.example.finanzly.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.finanzly.R;
import com.example.finanzly.dialogs.PasswordResetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink , tvForgotPassword;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Ajustar el padding del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        // Referencias UI
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        tvForgotPassword.setOnClickListener(v -> {
            PasswordResetDialog dialog = new PasswordResetDialog();
            dialog.show(getSupportFragmentManager(), "password_reset_dialog");
        });

        //  Acción de inicio de sesión
        btnLogin.setOnClickListener(v -> loginUser());

        //  Enlace al registro
        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    //Metodo para loguear
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu correo");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa tu contraseña");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar sesión");

                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Inicio de sesión exitoso",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();

                    } else {
                        String mensajeError = "Error al iniciar sesión";

                        Exception exception = task.getException();

                        if (exception instanceof FirebaseAuthInvalidUserException) {
                            mensajeError = "El usuario no existe";
                        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            mensajeError = "Correo o contraseña incorrectos";
                        } else if (exception instanceof FirebaseAuthUserCollisionException) {
                            mensajeError = "Este correo ya está en uso";
                        } else if (exception != null && exception.getMessage() != null) {
                            mensajeError = "Error: " + exception.getMessage();
                        }

                        Toast.makeText(LoginActivity.this,
                                mensajeError,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
