package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ClientMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_customer_home) {
                selectedFragment = new CustomerHomeFragment();
            } else if (id == R.id.nav_customer_search) {
                selectedFragment = new SearchFragment();
            } else if (id == R.id.nav_customer_appointments) {
                selectedFragment = new AppointmentsClientFragment();
            } else if (id == R.id.nav_customer_profile) {
                selectedFragment = new ProfileClientFragment();
            } else if (id == R.id.nav_customer_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // טעינה ראשונית של מסך הבית
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new CustomerHomeFragment()).commit();
        }
    }
}