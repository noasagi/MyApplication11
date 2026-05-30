package com.example.myapplication;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// הגדרת מחלקת מסך שעות הפעילות של העסק, היורשת מ-BaseActivity
public class BusinessHoursActivity extends BaseActivity {

    // הצהרה על רכיב רשימה ממוחזרת להצגת ימי השבוע
    private RecyclerView rvDays;
    // הצהרה על רכיב תיבת טקסט להזנת משך זמן תור/טיפול בדקות
    private EditText etDuration;
    // הצהרה על לחצן לשמירת כל הנתונים והשעות בשרת
    private Button btnSaveAll;
    // משתנה מחרוזת לשמירת מזהה העסק הייחודי
    private String businessId;
    // הצהרה על המתאם המותאם אישית עבור רשימת ימי העבודה
    private DaysAdapter adapter;
    // הצהרה על רשימה דינמית שמכילה את מודלי לוחות הזמנים היומיים
    private List<DaySchedule> daysList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת וחיבור קובץ ה-XML של עיצוב מסך שעות הפעילות
        setContentView(R.layout.activity_business_hours);

        // חיבור וקישור סרגל הכלים העליון של המסך
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // ביטול כותרת ברירת המחדל של סרגל הכלים במידה והוא קיים
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // ניסיון חילוץ מזהה העסק מתוך הכוונת (Intent) שפתחה את המסך בשני הפורמטים האפשריים
        businessId = getIntent().getStringExtra("BUSINESS_ID");
        if (businessId == null) businessId = getIntent().getStringExtra("businessId");

        // הגנה: אם מזהה העסק לא עבר בכוונת, נשלוף את ה-UID של המשתמשת המחוברת כברירת מחדל
        if (businessId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            businessId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // ניקוי רווחים מיותרים ממחרוזת מזהה העסק במידה והוא לא ריק
        if (businessId != null) {
            businessId = businessId.trim();
        }

        // קישור משתני הרכיבים לרכיבים הויזואליים האמיתיים מתוך קובץ ה-XML
        etDuration = findViewById(R.id.etDuration);
        btnSaveAll = findViewById(R.id.btnSaveAll);
        rvDays = findViewById(R.id.rvDays);

        // קריאה לפעולה הפנימית המאתחלת את רשימת הימים בזיכרון עם ערכי ברירת מחדל
        initDaysList();

        // הגדרת מנהל פריסה אנכי (רשימה רגילה) עבור רכיב הרשימה הממוחזרת
        rvDays.setLayoutManager(new LinearLayoutManager(this));
        // יצירת מופע של המתאם המותאם אישית וחיבורו לרשימה הויזואלית
        adapter = new DaysAdapter(daysList, this);
        rvDays.setAdapter(adapter);

        // קריאה לפעולה הפנימית הטוענת את השעות השמורות של העסק מתוך הענן לקוד
        loadFromFirebase();

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור השמירה הכללי
        btnSaveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // קריאה לפעולה שיוצרת את המבנה ושומרת את הנתונים בפיירסטור
                saveToFirebase();
            }
        });
    }

    // פעולה פרטית הממלאת את הרשימה הדינמית בשבעת ימי השבוע וערכי שעות ברירת מחדל
    private void initDaysList() {
        daysList = new ArrayList<>();
        String[] dayNames = {"יום ראשון", "יום שני", "יום שלישי", "יום רביעי", "יום חמישי", "יום שישי", "יום שבת"};
        for (int i = 0; i < dayNames.length; i++) {
            // הוספת מודל יום חדש: אינדקס, שם היום, שעת התחלה 09:00, שעת סיום 17:00, ומצב פתוח (true)
            daysList.add(new DaySchedule(i, dayNames[i], "09:00", "17:00", true));
        }
    }

    // פעולה פרטית הטוענת את הגדרות שעות הפעילות ואורך הטיפול ממסמך העסק בפיירסטור
    private void loadFromFirebase() {
        if (businessId == null) return;

        // פנייה ישירה למסמך העסק הספציפי באוסף העסקים בענן
        FirebaseFirestore.getInstance()
                .collection("businesses")
                .document(businessId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // בדיקה האם מסמך העסק אכן קיים במסד הנתונים
                        if (documentSnapshot.exists()) {
                            // שליפת שדה משך זמן הטיפול והצגתו בתיבת הטקסט על המסך
                            if (documentSnapshot.contains("appointmentDuration")) {
                                Long duration = documentSnapshot.getLong("appointmentDuration");
                                if (duration != null) etDuration.setText(String.valueOf(duration));
                            }

                            // שליפת אובייקט לוח הזמנים השבועי הכללי מהמסמך
                            Object scheduleObj = documentSnapshot.get("weeklySchedule");

                            // טיפול במצב שבו פיירבייס החזיר את המבנה כמפת נתונים (Map)
                            if (scheduleObj instanceof Map) {
                                Map<String, Object> weeklySchedule = (Map<String, Object>) scheduleObj;
                                for (DaySchedule day : daysList) {
                                    String key = String.valueOf(day.dayIndex);
                                    // עדכון נתוני היום לפי מפתח המספר (0-6) או לפי שם היום הטקסטואלי
                                    if (weeklySchedule.containsKey(key)) {
                                        updateDayFromMap(day, (Map<String, Object>) weeklySchedule.get(key));
                                    } else if (weeklySchedule.containsKey(day.dayName)) {
                                        updateDayFromMap(day, (Map<String, Object>) weeklySchedule.get(day.dayName));
                                    }
                                }
                            }
                            // טיפול במצב שבו פיירבייס החזיר את המבנה כרשימה (List) לצורך תאימות גיבוי
                            else if (scheduleObj instanceof List) {
                                List<Object> weeklySchedule = (List<Object>) scheduleObj;
                                for (int i = 0; i < weeklySchedule.size() && i < daysList.size(); i++) {
                                    Object item = weeklySchedule.get(i);
                                    if (item instanceof Map) {
                                        updateDayFromMap(daysList.get(i), (Map<String, Object>) item);
                                    }
                                }
                            }
                            // ריענון המתאם ועדכון התצוגה הגרפית של הימים על המסך עם הנתונים האמיתיים
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    // פעולת עזר פרטית המפרקת את מפת הנתונים של יום ספציפי ומעדכנת את תכונות המודל שלו
    private void updateDayFromMap(DaySchedule day, Map<String, Object> dayData) {
        if (dayData == null) return;
        if (dayData.containsKey("start") && dayData.get("start") != null) {
            day.startTime = String.valueOf(dayData.get("start"));
        }
        if (dayData.containsKey("end") && dayData.get("end") != null) {
            day.endTime = String.valueOf(dayData.get("end"));
        }
        if (dayData.containsKey("isOpen") && dayData.get("isOpen") != null) {
            Object isOpenObj = dayData.get("isOpen");
            if (isOpenObj instanceof Boolean) {
                day.isOpen = (Boolean) isOpenObj;
            } else if (isOpenObj instanceof String) {
                day.isOpen = Boolean.parseBoolean((String) isOpenObj);
            }
        }
    }

    // פעולה פרטית האורזת את כל נתוני המסך העדכניים ושומרת אותם בחזרה בענן
    private void saveToFirebase() {
        if (businessId == null) {
            Toast.makeText(this, "שגיאה: חסר מזהה עסק", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("BusinessHours", "Saving to businessId: " + businessId);

        String durationStr = etDuration.getText().toString();
        // הגנה: בדיקה שהמשתמשת אכן הזינה ערך מספרי בשדה אורך הטיפול
        if (durationStr.isEmpty()) {
            Toast.makeText(this, "נא להזין משך תור", Toast.LENGTH_SHORT).show();
            return;
        }

        // בניית מפת נתונים ראשית לייצוג לוח הזמנים השבועי
        Map<String, Object> weeklySchedule = new HashMap<>();
        for (DaySchedule day : daysList) {
            // יצירת תת-מפה עבור כל יום בנפרד המכילה את מאפייניו השונים לקוד
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("dayName", day.dayName);
            dayData.put("start", day.startTime);
            dayData.put("end", day.endTime);
            dayData.put("isOpen", day.isOpen);

            // קישור תת-המפה של היום אל תוך מפת לוח הזמנים הכללית לפי אינדקס היום כמפתח
            weeklySchedule.put(String.valueOf(day.dayIndex), dayData);
        }

        // יצירת מפת הנתונים הסופית שתישלח למסמך בפיירסטור
        Map<String, Object> dataToSave = new HashMap<>();
        dataToSave.put("appointmentDuration", Integer.parseInt(durationStr));
        dataToSave.put("weeklySchedule", weeklySchedule);

        // כתיבה ועדכון של נתוני המפה לתוך מסמך העסק, תוך שימוש באופציית מיזוג (Merge) למניעת דריסת שדות אחרים
        FirebaseFirestore.getInstance()
                .collection("businesses")
                .document(businessId)
                .set(dataToSave, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(BusinessHoursActivity.this, "השעות נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                        // סגירת המסך הנוכחי וחזרה למסך ההגדרות הקודם
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(BusinessHoursActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // מחלקת מודל פנימית וסטטית לייצוג מבנה הנתונים של יום פעילות בודד בשבוע
    public static class DaySchedule {
        int dayIndex;
        String dayName;
        String startTime;
        String endTime;
        boolean isOpen;

        public DaySchedule(int dayIndex, String dayName, String startTime, String endTime, boolean isOpen) {
            this.dayIndex = dayIndex;
            this.dayName = dayName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isOpen = isOpen;
        }
    }

    // מחלקת מתאם פנימית (Adapter) האחראית על ניהול, הרכבת והזרקת נתוני הימים לתוך הרשימה הממוחזרת
    class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DayViewHolder> {
        private List<DaySchedule> list;
        private AppCompatActivity context;

        public DaysAdapter(List<DaySchedule> list, AppCompatActivity context) {
            this.list = list;
            this.context = context;
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_schedule, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            DaySchedule day = list.get(position);
            // הצגת שם היום העדכני בשורת הרשימה הנוכחית
            holder.tvDayName.setText(day.dayName);
            // הצגת שעות ההתחלה והסיום על גבי הלחצנים הייעודיים
            holder.btnStartTime.setText(day.startTime);
            holder.btnEndTime.setText(day.endTime);

            // ניתוק זמני של מאזין השינוי של הסוויץ' כדי למנוע קריאות שווא בזמן טעינת ומיחזור השורות
            holder.switchIsOpen.setOnCheckedChangeListener(null);
            holder.switchIsOpen.setChecked(day.isOpen);
            // קריאה לפעולה פנימית המעדכנת את נראות לחצני השעות לפי מצב הסוויץ'
            updateVisibility(holder, day.isOpen);

            // הגדרת מאזין שינוי מצב אנונימי רגיל לסוויץ' הפתיחה/סגירה של יום העבודה
            holder.switchIsOpen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // עדכון המצב במודל הנתונים בהתאם לבחירת המשתמשת
                    day.isOpen = isChecked;
                    // שינוי נראות כפתורי השעות בהתאמה מיידית על גבי השורה
                    updateVisibility(holder, isChecked);
                }
            });

            // הגדרת מאזין לחיצה אנונימי רגיל ללחצן שעת הפתיחה
            holder.btnStartTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // הצגת רכיב בחירת הזמן הקופץ עבור שעת תחילת העבודה
                    showTimePicker(holder.btnStartTime, day, true);
                }
            });

            // הגדרת מאזין לחיצה אנונימי רגיל ללחצן שעת הסגירה
            holder.btnEndTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // הצגת רכיב בחירת הזמן הקופץ עבור שעת סיום העבודה
                    showTimePicker(holder.btnEndTime, day, false);
                }
            });
        }

        // פעולה פרטית המשנה את נראות כפתורי השעות והטקסט של הסוויץ' בהתאם למצב הפתיחה של היום
        private void updateVisibility(DayViewHolder holder, boolean isOpen) {
            holder.btnStartTime.setVisibility(isOpen ? View.VISIBLE : View.INVISIBLE);
            holder.btnEndTime.setVisibility(isOpen ? View.VISIBLE : View.INVISIBLE);
            holder.switchIsOpen.setText(isOpen ? "פתוח" : "סגור");
        }

        // פעולה פרטית המציגה דיאלוג קופץ לבחירת שעה (TimePickerDialog) ומעדכנת את הלחצן והמודל
        private void showTimePicker(Button btn, DaySchedule day, boolean isStart) {
            TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            // עיצוב מחרוזת השעה הנבחרת בפורמט קבוע של שעות עגולות (למשל "09:00") כדי להתאים למערכת התורים
                            String timeFormatted = String.format(Locale.getDefault(), "%02d:00", hourOfDay);
                            // עדכון הטקסט המוצג על גבי הלחצן שלחצו עליו
                            btn.setText(timeFormatted);
                            // עדכון שדה שעת ההתחלה או שעת הסיום במודל הנתונים בהתאמה
                            if (isStart) day.startTime = timeFormatted;
                            else day.endTime = timeFormatted;
                        }
                    }, 12, 0, true); // ברירת מחדל לפתיחת הדיאלוג בשעה 12:00
            timePickerDialog.show();
        }

        @Override
        public int getItemCount() { return list.size(); }

        // מחלקת ViewHolder פנימית המקשרת ומחזיקה את כל רכיבי ממשק המשתמש של שורת יום בודד ברשימה
        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView tvDayName;
            Switch switchIsOpen;
            Button btnStartTime, btnEndTime;
            public DayViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDayName = itemView.findViewById(R.id.tvDayName);
                switchIsOpen = itemView.findViewById(R.id.switchIsOpen);
                btnStartTime = itemView.findViewById(R.id.btnStartTime);
                btnEndTime = itemView.findViewById(R.id.btnEndTime);
            }
        }
    }
}