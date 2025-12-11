package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class ClientMainActivity extends DrawerBaseActivity {

    private Button btnSetProfile;
    private Button btnBrowseBusinesses;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        initDrawer(toolbar, drawerLayout, navView);

        btnSetProfile = findViewById(R.id.btnSetProfile);
        btnSetProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ClientMainActivity.this, SetProfileActivity.class);
            startActivity(intent);
        });

        btnBrowseBusinesses = findViewById(R.id.btnBrowseBusinesses);
        btnBrowseBusinesses.setOnClickListener(v -> {
            Intent intent = new Intent(ClientMainActivity.this, BrowseBusinessesActivity.class);
            startActivity(intent);
        });
    }
}
