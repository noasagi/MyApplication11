package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;

// ✅ שינוי: יורש מ-DrawerBaseActivity כדי לקבל את תפריט ההמבורגר (המגירה)
public class BusinessMainActivity extends DrawerBaseActivity {

    // אין צורך להגדיר כאן drawerLayout, navigationView, ו-toolbar
    // כי הם כבר מוגדרים במחלקת האב (DrawerBaseActivity)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_main);

        // 1. קישור לרכיבי ה-XML
        Toolbar toolbar = findViewById(R.id.toolbar);

        // 2. קישור לרכיבי ה-Drawer באמצעות המשתנים המוגנים מהאב
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // ✅ 3. קריאה לפונקציית האתחול של המגירה
        setupDrawer(toolbar, drawerLayout, navigationView);

        // 4. לוגיקה ספציפית למסך העסקי
        // ... כל הקוד הייחודי לדף העסק שלך יופיע כאן ...
    }
}