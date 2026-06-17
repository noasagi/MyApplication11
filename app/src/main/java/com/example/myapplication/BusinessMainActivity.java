package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BusinessMainActivity extends BaseActivity {

    /**
     * מה הפעולה עושה: מאתחלת את סרגל הניווט התחתון (BottomNavigationView), מגדירה מאזין למעבר בין מסכים, וטוענת את מסך הבית כברירת מחדל.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // הגדרת מאזין ללחיצות על כפתורי התפריט התחתון להחלפת הפרגמנטים המוצגים
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            // לוגיקת ניווט: התאמה בין ה-ID של הכפתור שנלחץ לפרגמנט המתאים לו
            if (id == R.id.nav_business_home) {
                selectedFragment = new BusinessHomeFragment();
            } else if (id == R.id.nav_business_schedule) {
                selectedFragment = new BusinessScheduleFragment();
            } else if (id == R.id.nav_business_chats) {
                selectedFragment = new BusinessChatsFragment();
            } else if (id == R.id.nav_business_settings) {
                selectedFragment = new BusinessSettingsFragment();
            }

            // ביצוע החלפת המסכים בפועל בתוך ה-Container (מכולת התצוגה ב-XML) באמצעות FragmentManager
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // מנגנון הגנה: טעינת מסך הבית רק בריצה הראשונית של האקטיביטי (מניעת טעינה כפולה בסיבוב מסך)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new BusinessHomeFragment())
                    .commit();
        }
    }
}