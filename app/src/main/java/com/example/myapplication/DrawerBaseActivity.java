package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public abstract class DrawerBaseActivity extends BaseActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // חשוב! עדיין קוראים ל-onCreate של BaseActivity/AppCompatActivity
        super.onCreate(savedInstanceState);
    }

    /**
     * API חדש ונקי – זו הפונקציה שנקרא אליה מה-Activities
     */
    protected void initDrawer(Toolbar toolbar,
                              DrawerLayout drawerLayout,
                              NavigationView navigationView) {
        // פשוט מעבירה הלאה לפונקציה הקיימת שלך
        setupDrawer(toolbar, drawerLayout, navigationView);
    }

    /**
     * הפונקציה המקורית שלך – לא נוגעים בלוגיקה בפנים
     */
    protected void setupDrawer(Toolbar toolbar,
                               DrawerLayout drawerLayout,
                               NavigationView navigationView) {

        this.toolbar = toolbar;
        this.drawerLayout = drawerLayout;
        this.navigationView = navigationView;

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // טיפול בכפתור Back כשהמגירה פתוחה
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    onBackPressed();
                    setEnabled(true);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        navigationView.setNavigationItemSelectedListener(item -> {
            boolean handled = handleNavigationItemSelection(item);

            if (handled) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else {
                if (item.getItemId() == R.id.action_set_profile) {
                    startActivity(new Intent(this, SetProfileActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                return false;
            }
        });

        // סינון פריטי מגירה
        filterMenuItems(navigationView.getMenu());
    }
}
