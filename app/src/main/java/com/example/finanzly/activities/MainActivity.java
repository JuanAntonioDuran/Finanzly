package com.example.finanzly.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.finanzly.R;
import com.example.finanzly.fragments.BudgetsFragment;
import com.example.finanzly.fragments.GoalsFragment;
import com.example.finanzly.fragments.InvitationFragment;
import com.example.finanzly.fragments.MovementsFragment;
import com.example.finanzly.fragments.RemindersFragment;
import com.example.finanzly.fragments.UserFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        //  Verificar sesión del usuario
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Si no hay usuario logueado, redirigir a LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Evita que pueda volver con el botón "atrás"
            return;
        }

        //  Si hay sesión activa, cargar la interfaz normal
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_movements) {
                selectedFragment = new MovementsFragment();
            } else if (id == R.id.nav_budgets) {
                selectedFragment = new BudgetsFragment();
            } else if (id == R.id.nav_goals) {
                selectedFragment = new GoalsFragment();
            } else if (id == R.id.nav_reminders) {
                selectedFragment = new RemindersFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new UserFragment();
            }else if (id == R.id.nav_invite) {
            selectedFragment = new InvitationFragment();
        }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MovementsFragment())
                    .commit();
        }
    }
}
