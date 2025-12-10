package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

// שימי לב: מסך הפתיחה לא חייב לרשת מ-BaseActivity
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ודאי שיש לך קובץ layout מתאים, למשל activity_main.xml
        setContentView(R.layout.activity_main);

        // נניח שיש כפתור "התחל" או "התחבר" ב-activity_main.xml
        Button btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> {
            navigateToLogin();
        });

        // --- אפשרות נוספת: ניווט אוטומטי לאחר X שניות (כמו Splash Screen) ---
        // new android.os.Handler().postDelayed(this::navigateToLogin, 3000); // 3 שניות
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // סגירת MainActivity כדי שלא יהיה ניתן לחזור אליו אחורה
    }
}