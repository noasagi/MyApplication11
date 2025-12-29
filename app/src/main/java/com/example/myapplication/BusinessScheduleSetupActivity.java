package com.example.myapplication;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class BusinessScheduleSetupActivity extends AppCompatActivity {

    private EditText etDuration;
    private CheckBox cbSunday, cbMonday; // תוסיפי את שאר הימים
    private EditText etSundayStart, etSundayEnd; // דוגמה ליום ראשון

    // הפניה ל-Realtime Database
    private DatabaseReference mDatabase;
    private String currentBusinessId; // את צריכה להשיג את זה (נניח שזה ה-ID של המשתמש המחובר אם הוא הבעלים)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_schedule_setup);

        // אתחול Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // נניח שה-BusinessID זהה ל-UserID של בעל העסק (או שתשלפי אותו מאיפשהו)
        currentBusinessId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // קישור לרכיבי המסך (דוגמה חלקית)
        etDuration = findViewById(R.id.etSlotDuration);
        cbSunday = findViewById(R.id.cbSunday);
        etSundayStart = findViewById(R.id.etSundayStart);
        etSundayEnd = findViewById(R.id.etSundayEnd);

        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        // 1. יצירת האובייקט
        BusinessScheduleSettings settings = new BusinessScheduleSettings();

        // הגדרת משך תור
        String durationStr = etDuration.getText().toString();
        if (durationStr.isEmpty()) {
            etDuration.setError("חובה להזין זמן");
            return;
        }
        settings.setSlotDurationMinutes(Integer.parseInt(durationStr));
        settings.setBookingEnabled(true);

        // 2. איסוף שעות הפעילות
        Map<String, String> hours = new HashMap<>();

        // בדיקת יום ראשון
        if (cbSunday.isChecked()) {
            String start = etSundayStart.getText().toString(); // וודאי פורמט HH:mm
            String end = etSundayEnd.getText().toString();
            if (!start.isEmpty() && !end.isEmpty()) {
                hours.put("Sunday", start + "-" + end);
            }
        }

        // ... (כאן את משכפלת את הלוגיקה לשאר הימים) ...

        settings.setWorkDays(hours);

        // 3. שמירה לפיירבייס Realtime Database
        // הנתיב: businesses_settings -> business_id
        mDatabase.child("business_settings").child(currentBusinessId)
                .setValue(settings)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "ההגדרות נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}