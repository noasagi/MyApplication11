package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// מחלקת אקטיביטי עבור מסך הפתיחה (Splash Screen) האחראית על הצגת לוגו האפליקציה וניתוב אוטומטי של המשתמש לפי מצב החיבור שלו
public class SplashActivity extends AppCompatActivity {

    // רכיב עזר מותאם אישית (Helper Class) לבדיקת תפקיד המשתמש (לקוח או בעל עסק) שנשמר ב-SharedPreferences או במסד הנתונים
    private UserHelper userHelper;

    /**
     * מה הפעולה עושה: מציגה את עיצוב מסך הפתיחה, ומפעילה טיימר אסינכרוני המשהה את המסך למשך 3 שניות לפני קבלת החלטת הניווט.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        userHelper = new UserHelper(this);

        /**
         * הסבר ארכיטקטוני לבוחן:
         * אנו משתמשים ב-Handler המחובר ל-Looper.getMainLooper() כדי לתזמן משימה עתידית על חוט המערכת הראשי (UI Thread),
         * מבלי לחסום או לתקוע את המסך (Non-blocking delay).
         * המערכת תציג את הלוגו, תמתין 3000 מילישניות (3 שניות) ברקע, ואז תפעיל את לוגיקת הניתוב.
         */
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserAndNavigate();
        }, 3000);
    }

    /**
     * מה הפעולה עושה: פונקציית הנתב (Router). בודקת מול Firebase Auth האם יש משתמש מחובר:
     * - אם אין: מנווטת למסך ההתחברות (LoginActivity).
     * - אם יש: בודקת דרך ה-UserHelper האם מדובר בבעל עסק או לקוח, ומנווטת למסך הבית המתאים.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void checkUserAndNavigate() {
        // שליפת ה-Token הנוכחי של המשתמש מרכיב ה-Authentication בענן
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            // מקרה 1: המשתמש אורח / לא מחובר -> מעבר למסך כניסה ורישום
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        } else {
            // מקרה 2: המשתמש כבר מחובר בעבר -> פיצול ארכיטקטוני לפי סוג המשתמש (Role-Based Routing)
            if (userHelper.isBusinessOwner()) {
                // ניווט למסך הניהול של בעל העסק
                Intent intent = new Intent(SplashActivity.this, BusinessMainActivity.class);
                startActivity(intent);
            } else {
                // ניווט למסך הראשי של הלקוח המזמין
                Intent intent = new Intent(SplashActivity.this, ClientMainActivity.class);
                startActivity(intent);
            }
        }

        /**
         * למה פקודה זו קריטית:
         * הפקודה finish() סוגרת ומחסלת את האקטיביטי הנוכחי (SplashActivity) ומוציאה אותו ממחסנית המסכים (Backstack).
         * הדבר מונע מצב שבו המשתמש ילחץ על כפתור ה-"Back" במסך הבית ויחזור בטעות למסך הלוגו התקוע.
         */
        finish();
    }
}