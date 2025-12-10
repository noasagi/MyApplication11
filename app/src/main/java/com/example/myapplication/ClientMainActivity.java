package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.widget.Toolbar;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;

// ✅ יורש מ-DrawerBaseActivity ומקבל את כל הלוגיקה של ההמבורגר
public class ClientMainActivity extends DrawerBaseActivity {

    private Button btnSetProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        // 1. קישור לרכיבי ה-XML
        Toolbar toolbar = findViewById(R.id.toolbar);

        // שימוש במשתנים המוגנים של DrawerBaseActivity
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // ✅ 2. קריאה לפונקציית האתחול של המגירה
        setupDrawer(toolbar, drawerLayout, navigationView);

        // 3. לוגיקת כפתורים ספציפיים לדף זה
        btnSetProfile = findViewById(R.id.btnSetProfile);
        btnSetProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ClientMainActivity.this, SetProfileActivity.class);
            startActivity(intent);
        });

        // ... כל לוגיקה ייחודית אחרת לדף הלקוח ...
    }

    // אין צורך ב-onBackPressed() או onCreateOptionsMenu()!
}