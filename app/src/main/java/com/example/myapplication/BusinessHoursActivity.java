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

public class BusinessHoursActivity extends BaseActivity {

    private RecyclerView rvDays;
    private EditText etDuration;
    private Button btnSaveAll;
    private String businessId;
    private DaysAdapter adapter;
    private List<DaySchedule> daysList;

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי המסך, מחלצת ומאבטחת את מזהה העסק (ID), מייצרת את רשימת הימים הבסיסית וטוענת נתונים קיימים.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_hours);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // חילוץ מזהה העסק עם מנגנון הגנה: אם ה-Intent לא החזיר ערך, ניקח את ה-UID של המשתמש המחובר כברירת מחדל
        businessId = getIntent().getStringExtra("BUSINESS_ID");
        if (businessId == null) businessId = getIntent().getStringExtra("businessId");
        if (businessId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            businessId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        if (businessId != null) {
            businessId = businessId.trim();
        }

        etDuration = findViewById(R.id.etDuration);
        btnSaveAll = findViewById(R.id.btnSaveAll);
        rvDays = findViewById(R.id.rvDays);

        initDaysList();

        rvDays.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DaysAdapter(daysList, this);
        rvDays.setAdapter(adapter);

        // טעינת הגדרות הזמנים השמורות בענן (אם קיימות) מיד עם עליית המסך
        loadFromFirebase();

        btnSaveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToFirebase();
            }
        });
    }

    /**
     * מה הפעולה עושה: ממלאת את רשימת הזיכרון המקומית בשבעת ימי השבוע עם ערכי ברירת מחדל אחידים (פתוח, 09:00 - 17:00).
     * קלט: אין.
     * פלט: אין (void).
     */
    private void initDaysList() {
        daysList = new ArrayList<>();
        String[] dayNames = {"יום ראשון", "יום שני", "יום שלישי", "יום רביעי", "יום חמישי", "יום שישי", "יום שבת"};
        for (int i = 0; i < dayNames.length; i++) {
            daysList.add(new DaySchedule(i, dayNames[i], "09:00", "17:00", true));
        }
    }

    /**
     * מה הפעולה עושה: שולפת ממסמך העסק ב-Firestore את אורך התור ואת אובייקט לוח הזמנים השבועי (תוך תמיכה במבנה של Map או List).
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadFromFirebase() {
        if (businessId == null) return;

        FirebaseFirestore.getInstance()
                .collection("businesses")
                .document(businessId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            if (documentSnapshot.contains("appointmentDuration")) {
                                Long duration = documentSnapshot.getLong("appointmentDuration");
                                if (duration != null) etDuration.setText(String.valueOf(duration));
                            }

                            Object scheduleObj = documentSnapshot.get("weeklySchedule");

                            // פיירבייס עשוי להחזיר את לוח הזמנים כמפה או כרשימה. הקוד בודק את הטיפוס בזמן ריצה (instanceof) כדי למנוע קריסות
                            if (scheduleObj instanceof Map) {
                                Map<String, Object> weeklySchedule = (Map<String, Object>) scheduleObj;
                                for (DaySchedule day : daysList) {
                                    String key = String.valueOf(day.dayIndex);
                                    if (weeklySchedule.containsKey(key)) {
                                        updateDayFromMap(day, (Map<String, Object>) weeklySchedule.get(key));
                                    } else if (weeklySchedule.containsKey(day.dayName)) {
                                        updateDayFromMap(day, (Map<String, Object>) weeklySchedule.get(day.dayName));
                                    }
                                }
                            } else if (scheduleObj instanceof List) {
                                List<Object> weeklySchedule = (List<Object>) scheduleObj;
                                for (int i = 0; i < weeklySchedule.size() && i < daysList.size(); i++) {
                                    Object item = weeklySchedule.get(i);
                                    if (item instanceof Map) {
                                        updateDayFromMap(daysList.get(i), (Map<String, Object>) item);
                                    }
                                }
                            }
                            // עדכון הרשימה הגרפית על גבי המסך בנתונים האמיתיים שנשלפו
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: מפרקת את נתוני היום הגולמיים שחזרו מהענן ומעדכנת את שדות מודל ה-DaySchedule המקומי.
     * קלט: DaySchedule day, Map<String, Object> dayData.
     * פלט: אין (void).
     */
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

    /**
     * מה הפעולה עושה: אורזת את אורך הטיפול ורשימת שעות הימים לתוך מפות נתונים, ושומרת אותם ב-Firestore בשיטת מיזוג (Merge).
     * קלט: אין.
     * פלט: אין (void).
     */
    private void saveToFirebase() {
        if (businessId == null) {
            Toast.makeText(this, "שגיאה: חסר מזהה עסק", Toast.LENGTH_SHORT).show();
            return;
        }

        String durationStr = etDuration.getText().toString();
        if (durationStr.isEmpty()) {
            Toast.makeText(this, "נא להזין משך תור", Toast.LENGTH_SHORT).show();
            return;
        }

        // בניית המבנה השבועי: הפיכת רשימת האובייקטים למפת נתונים מקוננת (Map בתוך Map) שמתאימה לאחסון ב-Firestore
        Map<String, Object> weeklySchedule = new HashMap<>();
        for (DaySchedule day : daysList) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("dayName", day.dayName);
            dayData.put("start", day.startTime);
            dayData.put("end", day.endTime);
            dayData.put("isOpen", day.isOpen);

            weeklySchedule.put(String.valueOf(day.dayIndex), dayData);
        }

        Map<String, Object> dataToSave = new HashMap<>();
        dataToSave.put("appointmentDuration", Integer.parseInt(durationStr));
        dataToSave.put("weeklySchedule", weeklySchedule);

        // שימוש ב-SetOptions.merge() קריטי כדי לעדכן רק את השעות ואורך התור מבלי למחוק שדות אחרים במסמך העסק
        FirebaseFirestore.getInstance()
                .collection("businesses")
                .document(businessId)
                .set(dataToSave, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(BusinessHoursActivity.this, "השעות נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
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

    // --- מחלקת מודל פנימית: מייצגת יום פעילות בודד ---
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

    // --- מחלקת אדפטר פנימית: לניהול שורות ימי השבוע ברשימה הממוחזרת ---
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
            holder.tvDayName.setText(day.dayName);
            holder.btnStartTime.setText(day.startTime);
            holder.btnEndTime.setText(day.endTime);

            // ניתוק זמני של המאזין (setOnCheckedChangeListener(null)) מונע קריאות שווא ובלבול בנתונים בזמן שגלילת הרשימה ממחזרת את השורות
            holder.switchIsOpen.setOnCheckedChangeListener(null);
            holder.switchIsOpen.setChecked(day.isOpen);
            updateVisibility(holder, day.isOpen);

            holder.switchIsOpen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    day.isOpen = isChecked;
                    updateVisibility(holder, isChecked);
                }
            });

            holder.btnStartTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTimePicker(holder.btnStartTime, day, true);
                }
            });

            holder.btnEndTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTimePicker(holder.btnEndTime, day, false);
                }
            });
        }

        /**
         * מה הפעולה עושה: משנה את המראה והנראות (Visibility) של כפתורי השעות והטקסט של הסוויץ' בהתאם למצב פתיחת היום.
         */
        private void updateVisibility(DayViewHolder holder, boolean isOpen) {
            holder.btnStartTime.setVisibility(isOpen ? View.VISIBLE : View.INVISIBLE);
            holder.btnEndTime.setVisibility(isOpen ? View.VISIBLE : View.INVISIBLE);
            holder.switchIsOpen.setText(isOpen ? "פתוח" : "סגור");
        }

        /**
         * מה הפעולה עושה: מציגה שעון קופץ לבחירת זמן, ומעגלת את דקות הבחירה ל-":00" כדי לשמור על פורמט שעות עגולות ויציב במערכת.
         */
        private void showTimePicker(Button btn, DaySchedule day, boolean isStart) {
            TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            // אילוץ פורמט שעות עגולות במערכת (למשל 09:00, 10:00) כדי לפשט את תהליך יצירת התורים
                            String timeFormatted = String.format(Locale.getDefault(), "%02d:00", hourOfDay);
                            btn.setText(timeFormatted);
                            if (isStart) day.startTime = timeFormatted;
                            else day.endTime = timeFormatted;
                        }
                    }, 12, 0, true);
            timePickerDialog.show();
        }

        @Override
        public int getItemCount() { return list.size(); }

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