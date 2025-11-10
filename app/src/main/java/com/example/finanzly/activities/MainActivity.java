package com.example.finanzly.activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.finanzly.R;
import com.example.finanzly.fragments.BudgetsFragment;
import com.example.finanzly.fragments.GoalsFragment;
import com.example.finanzly.fragments.MovementsFragment;
import com.example.finanzly.fragments.RemindersFragment;
import com.example.finanzly.fragments.UserFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
