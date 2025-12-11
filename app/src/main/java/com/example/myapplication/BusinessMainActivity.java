package com.example.myapplication;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;

public class BusinessMainActivity extends DrawerBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        setupDrawer(toolbar, drawerLayout, navigationView);

        // לוגיקה נוספת לבעל עסק...
    }
}
