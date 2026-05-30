package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    // עצם עזר לבדיקת סוג המשתמש המחובר (בעל עסק או לקוח)
    private UserHelper userHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // אתחול מחלקת העזר
        userHelper = new UserHelper(this);

        // השהיית המסך למשך 3 שניות (3000 מילישניות) לצורך הצגת הלוגו, ואז מעבר לפעולה הבאה
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserAndNavigate();
        }, 3000);
    }

    // פעולה הבודקת את מצב החיבור של המשתמש ומנווטת למסך המתאים
    private void checkUserAndNavigate() {
        // קבלת המשתמש הנוכחי ממערכת האימות של פיירבייס
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // בדיקה האם קיים משתמש מחובר במערכת
        if (currentUser == null) {
            // אם אין משתמש מחובר, ניווט למסך ההתחברות
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        } else {
            // אם יש משתמש מחובר, נבדוק את סוג המשתמש בעזרת מחלקת העזר
            if (userHelper.isBusinessOwner()) {
                // ניווט למסך הראשי של בעל העסק
                Intent intent = new Intent(SplashActivity.this, BusinessMainActivity.class);
                startActivity(intent);
            } else {
                // ניווט למסך הראשי של הלקוח
                Intent intent = new Intent(SplashActivity.this, ClientMainActivity.class);
                startActivity(intent);
            }
        }

        // סגירת מסך הפתיחה כדי למנוע חזרה אליו בלחיצה על כפתור החזור במכשיר
        finish();
    }
}