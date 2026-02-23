package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private UserHelper userHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        userHelper = new UserHelper(this);

        // מפעיל טיימר ל-2 שניות (2000 מילישניות)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserAndNavigate();
        }, 4000);
    }

    private void checkUserAndNavigate() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            // המשתמש לא מחובר -> מעבירים למסך כניסה
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        } else {
            // המשתמש מחובר -> בודקים איזה סוג הוא
            if (userHelper.isBusinessOwner()) {
                startActivity(new Intent(SplashActivity.this, BusinessMainActivity.class));
            } else {
                // אם הוא לקוח (או אורח שנרשם כלקוח)
                startActivity(new Intent(SplashActivity.this, ClientMainActivity.class));
            }
        }

        // חשוב! סוגרים את ה-Splash כדי שלא יחזרו אליו בלחיצה על "אחורה"
        finish();
    }
}