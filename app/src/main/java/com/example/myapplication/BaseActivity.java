package com.example.myapplication;

import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public abstract class BaseActivity extends AppCompatActivity {

    protected UserHelper userHelper;

    @Override
    protected void onStart() {
        super.onStart();
        // זה חשוב להשאיר כדי שיהיה לך גישה לנתוני משתמש בכל דף
        if (userHelper == null) {
            userHelper = new UserHelper(this);
        }
    }

    // --- הגדרות עיצוב (Toolbar) ---
    // להשאיר את זה, כי דפים עדיין משתמשים בזה כדי להציג כותרת וחץ אחורה
    protected void setupToolbar(Toolbar toolbar) {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    protected void setupSecondaryToolbar(Toolbar toolbar, boolean showBackButton) {
        setupToolbar(toolbar);

        if (showBackButton && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    // --- טיפול בחץ "אחורה" ---
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // אם לחצו על החץ אחורה בפינה למעלה
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // תחזור אחורה
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // כל שאר הפונקציות של התפריטים (filterMenuItems, handleNavigation...) נמחקו מכאן!
}