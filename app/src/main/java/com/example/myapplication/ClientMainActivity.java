package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class ClientMainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Button btnSetProfile;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        // קישור ל-XML
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnSetProfile = findViewById(R.id.btnSetProfile);
        toolbar = findViewById(R.id.toolbar);

        // הגדרת Toolbar כאקשן בר
        setSupportActionBar(toolbar);

        // Toggle לפתיחה וסגירה של התפריט
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState(); // קריטי! אם אין זה, סמל ההמבורגר לא פועל

        // כפתור שמוביל ל-SetProfileActivity
        btnSetProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ClientMainActivity.this, SetProfileActivity.class);
            startActivity(intent);
        });

        // מאזין לתפריט הצדדי
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_set_profile) {
                Intent intent = new Intent(ClientMainActivity.this, SetProfileActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_logout) {
                Toast.makeText(ClientMainActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
                // כאן אפשר להוסיף לוגיקה ליציאה
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    // טיפול בכפתור חזרה כאשר Drawer פתוח
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
