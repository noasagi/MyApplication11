package com.example.myapplication;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;

public abstract class BaseActivity extends AppCompatActivity {

    protected UserHelper userHelper;

    // ✅ 1. משתנה חדש שיעקוב אם חץ החזרה מופעל
    private boolean isUpButtonEnabled = false;

    @Override
    protected void onStart() {
        super.onStart();
        if (userHelper == null) {
            userHelper = new UserHelper(this);
        }
    }

    protected void setupToolbar(Toolbar toolbar) {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    // ✅ 2. עדכון: מגדירה Toolbar עבור דפים משניים, עם אפשרות לחץ חזרה
    protected void setupSecondaryToolbar(Toolbar toolbar, boolean showBackButton) {
        setupToolbar(toolbar);

        if (showBackButton && getSupportActionBar() != null) {
            // מציג חץ חזרה (Back/Up button)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            // ✅ הפעלת המשתנה החדש
            isUpButtonEnabled = true;
        }
    }

    // פונקציית סינון הפריטים
    public void filterMenuItems(Menu menu) {
        if (userHelper == null) userHelper = new UserHelper(this);
        // ... (השאר את הלוגיקה של הסינון כפי שהיא) ...
        boolean isBusiness = userHelper.isBusinessOwner();
        boolean isGuest = userHelper.isGuest();

        MenuItem itemBusiness = menu.findItem(R.id.action_business_page);
        MenuItem itemLogin = menu.findItem(R.id.action_login);
        MenuItem itemLogout = menu.findItem(R.id.action_logout);
        MenuItem itemSetProfile = menu.findItem(R.id.action_set_profile);

        if (itemBusiness != null) {
            itemBusiness.setVisible(isBusiness);
        }
        if (itemLogin != null) {
            itemLogin.setVisible(isGuest);
        }
        if (itemLogout != null) {
            itemLogout.setVisible(!isGuest);
        }
        if (itemSetProfile != null) {
            itemSetProfile.setVisible(!isGuest);
        }
    }

    // הניווט הכללי
    public boolean handleNavigationItemSelection(MenuItem item) {
        int id = item.getItemId();
        // ... (השאר את הלוגיקה של הניווט כפי שהיא) ...
        if (userHelper == null) userHelper = new UserHelper(this);
        boolean isBusiness = userHelper.isBusinessOwner();

        if (id == R.id.action_home) {
            if (isBusiness) {
                Intent intent = new Intent(this, BusinessMainActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, ClientMainActivity.class);
                startActivity(intent);
            }
            return true;
        }

        else if (id == R.id.action_business_page) {
            Intent intent = new Intent(this, BusinessMainActivity.class);
            startActivity(intent);
            return true;
        }

        else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            userHelper.logout();
            Toast.makeText(this, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();

            if (!(this instanceof DrawerBaseActivity)) {
                invalidateOptionsMenu();
            }

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }

        else if (id == R.id.action_login) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        return false;
    }

    // ✅ 3. עדכון: משתמש במשתנה החדש כדי למנוע הצגת 3 נקודות
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this instanceof DrawerBaseActivity) {
            return false;
        }

        // 🔴 התיקון לשגיאת הקומפילציה: בדיקה אם המשתנה הופעל במקום השיטה החסרה
        if (isUpButtonEnabled) {
            return false;
        }

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        filterMenuItems(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // טיפול בלחיצה על חץ החזרה בדפים משניים
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (handleNavigationItemSelection(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}