package com.example.myapplication;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

// מחלקה אבסטרקטית - לא משתמשים בה ישירות אלא יורשים ממנה
public abstract class BaseActivity extends AppCompatActivity {

    protected UserHelper userHelper;

    @Override
    protected void onStart() {
        super.onStart();
        // אתחול ה-Helper בכל עמוד שעולה
        if (userHelper == null) {
            userHelper = new UserHelper(this);
        }
    }

    // 1. יצירת התפריט
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // 2. סינון פריטים לפי סוג משתמש
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (userHelper == null) userHelper = new UserHelper(this);

        boolean isBusiness = userHelper.isBusinessOwner();
        boolean isGuest = userHelper.isGuest();

        MenuItem itemBusiness = menu.findItem(R.id.action_business_page);
        MenuItem itemLogin = menu.findItem(R.id.action_login);
        MenuItem itemLogout = menu.findItem(R.id.action_logout);

        if (itemBusiness != null) {
            itemBusiness.setVisible(isBusiness); // יופיע רק אם הוא בעל עסק
        }

        if (itemLogin != null) {
            itemLogin.setVisible(isGuest); // יופיע רק אם הוא אורח
        }

        if (itemLogout != null) {
            itemLogout.setVisible(!isGuest); // יופיע רק אם הוא מחובר
        }

        return super.onPrepareOptionsMenu(menu);
    }

    // 3. טיפול בלחיצות על התפריט
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_home) {
            // כאן תנווט לדף הבית הכללי שלך
            Intent intent = new Intent(this, ClientMainActivity.class); // שיניתי לדוגמה
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_business_page) {
            Intent intent = new Intent(this, BusinessMainActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_login) {
            // Intent intent = new Intent(this, LoginActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "מעבר להתחברות...", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            userHelper.logout(); // איפוס הזיכרון המקומי
            Toast.makeText(this, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu(); // רענון התפריט

            // אופציונלי: מעבר לדף כניסה
            // Intent intent = new Intent(this, LoginActivity.class);
            // startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}