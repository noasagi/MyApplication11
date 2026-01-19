package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BusinessMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            // 1. הוספנו את המקרה הזה:
            if (id == R.id.nav_business_home) {
                selectedFragment = new BusinessHomeFragment();
            }
            // שאר המקרים הרגילים:
            else if (id == R.id.nav_business_schedule) {
                selectedFragment = new BusinessScheduleFragment();

            } else if (id == R.id.nav_business_settings) {
                // כאן עמוד ההגדרות והניהול החדש
                selectedFragment = new BusinessSettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // 2. שינינו את זה כך שיפתח את דף הבית בהתחלה (במקום היומן)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new BusinessHomeFragment())
                    .commit();
        }
    }
    }