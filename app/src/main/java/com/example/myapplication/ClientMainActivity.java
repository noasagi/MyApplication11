package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ClientMainActivity extends BaseActivity {

    /**
     * מה הפעולה עושה: מאתחלת את ממשק המשתמש, מקשרת את תפריט הניווט התחתון (BottomNavigationView), ומגדירה את החלפת הפרגמנטים בזמן אמת.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // הגדרת מאזין להחלפת מסכים (Fragments) בלחיצה על תפריט הניווט התחתון
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            // התאמה בין מזהה הכפתור שנלחץ ב-XML לבין הפרגמנט המתאים לו בזיכרון
            if (id == R.id.nav_customer_home) {
                selectedFragment = new CustomerHomeFragment();
            } else if (id == R.id.nav_customer_search) {
                selectedFragment = new SearchFragment();
            } else if (id == R.id.nav_customer_appointments) {
                selectedFragment = new AppointmentsClientFragment();
            } else if (id == R.id.nav_customer_profile) {
                selectedFragment = new SettingsFragment();
            }

            // ביצוע מנגנון החלפת התצוגות (Fragment Transaction) בתוך מכולת התוכן (Fragment Container)
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true; // החזרת true מאשרת למערכת לסמן חזותית את הכפתור שנבחר
        });

        // מנגנון הגנה וחוויית משתמש (UX): טעינה ראשונית של מסך הבית רק בריצה הראשונה של האקטיביטי
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CustomerHomeFragment())
                    .commit();
        }
    }
}