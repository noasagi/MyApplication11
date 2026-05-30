package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Locale;

// הגדרת מחלקת אקטיביטי להזמנת תור על ידי הלקוח, היורשת מ-BaseActivity
public class BookingActivity extends BaseActivity {

    // הצהרה על רכיבי הטקסט להצגת התאריך הנבחר והודעות מחסור בתורים פנויים
    private TextView tvSelectedDate, tvNoSlots;
    // הצהרה על רכיב הרשימה הממוחזרת להצגת חלונות הזמן הפנויים
    private RecyclerView rvTimeSlots;
    // הצהרה על לחצני בחירת התאריך ואישור ההזמנה הסופי
    private Button btnPickDate, btnConfirmBooking;
    // הצהרה על רכיב תיבת בחירה נפתחת (Spinner) לבחירת סוג הטיפול
    private Spinner spinnerTreatments;

    // משתני מחרוזת לשמירת התאריך והשעה הספציפיים שהלקוח בחר לתור
    private String selectedDate = "";
    private String selectedTime = "";
    // הצהרה על המתאם המותאם אישית של רשימת חלונות השעות
    private TimeSlotAdapter adapter;
    // הצהרה על רשימה דינמית המכילה את מחרוזות השעות הפנויות (HH:mm)
    private List<String> timeSlotsList;
    // הצהרה על עצם לוח שנה לשמירת הנתונים הקלנדריים של היום הנבחר
    private Calendar selectedCalendar = null;

    // משתני מחרוזת לשמירת מזהה העסק הנוכחי ושם העסק שאליו נקבע התור
    private String currentBusinessId;
    private String currentBusinessName = "";
    // הצהרה על עצמי הגישה לבסיס הנתונים פיירסטור ומערכת האימות של פיירבייס
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // הצהרה על רשימה דינמית המכילה את אובייקטי הטיפולים הזמינים בעסק
    private List<Treatment> treatmentList = new ArrayList<>();
    // משתנה מסוג מודל הטיפול לשמירת הטיפול הספציפי שנבחר על ידי המשתמש
    private Treatment selectedTreatment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת וחיבור קובץ ה-XML של עיצוב מסך הזמנת תור
        setContentView(R.layout.activity_booking);

        // חיבור וקישור סרגל הכלים העליון של המסך
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // ביטול כותרת ברירת המחדל של סרגל הכלים במידה והוא קיים לקוד
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // אתחול וקבלת המופע הנוכחי של פיירסטור ואימות פיירבייס
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // חילוץ מזהה ושם העסק שהועברו בתוך ה-Intent מהמסך הקודם
        currentBusinessId = getIntent().getStringExtra("businessId");
        currentBusinessName = getIntent().getStringExtra("businessName");
        if (currentBusinessId == null) {
            currentBusinessId = getIntent().getStringExtra("BUSINESS_ID");
        }

        // ניקוי רווחים מיותרים ממחרוזת מזהה העסק במידה והוא התקבל
        if (currentBusinessId != null) {
            currentBusinessId = currentBusinessId.trim();
        }

        // הגנה: אם מזהה העסק חסר או ריק, נציג הודעה ונסגור את המסך מיידית
        if (currentBusinessId == null || currentBusinessId.isEmpty()) {
            Toast.makeText(this, "שגיאה: לא זוהה עסק", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // קישור משתני הרכיבים לרכיבים הגרפיים האמיתיים מתוך קובץ ה-XML
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoSlots = findViewById(R.id.tvNoSlots);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        spinnerTreatments = findViewById(R.id.spinnerTreatments);

        // הגדרת מנהל פריסת רשת (גריד) בעל 3 עמודות להצגת השעות הפנויות
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        timeSlotsList = new ArrayList<>();
        // יצירת מופע של המתאם המותאם אישית וחיבורו לרכיב הרשימה הויזואלי
        adapter = new TimeSlotAdapter(timeSlotsList, 0);
        rvTimeSlots.setAdapter(adapter);

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור פתיחת לוח השנה
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // הגנה: חיוב המשתמש לבחור קודם כל סוג טיפול לפני בחירת תאריך
                if (selectedTreatment == null) {
                    Toast.makeText(BookingActivity.this, "נא לבחור טיפול קודם", Toast.LENGTH_SHORT).show();
                    return;
                }
                showDatePicker();
            }
        });

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור אישור ההזמנה ושמירת התור
        btnConfirmBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // קריאה לפעולה שמתחילה את תהליך רישום ושמירת התור
                saveAppointmentRequest();
            }
        });

        // קריאה לפעולה הפנימית הטוענת את רשימת הטיפולים הזמינים של העסק הנוכחי
        loadTreatments();
    }

    // פעולה פרטית השולפת את תת-אוסף הטיפולים של העסק ומציגה אותם בתוך ה-Spinner
    private void loadTreatments() {
        db.collection("businesses").document(currentBusinessId).collection("treatments")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        treatmentList.clear();
                        List<String> treatmentNames = new ArrayList<>();

                        // מעבר בלולאה על כל מסמכי הטיפולים שנשלפו מהענן
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Treatment treatment = doc.toObject(Treatment.class);
                            treatmentList.add(treatment);
                            // בניית מחרוזת טקסט להצגה המשלבת את שם הטיפול ומשך הזמן שלו
                            treatmentNames.add(treatment.getName() + " (" + treatment.getDurationMinutes() + " דקות)");
                        }

                        // אם לא נמצאו טיפולים מוגדרים לעסק זה בענן
                        if (treatmentList.isEmpty()) {
                            treatmentNames.add("לא הוגדרו טיפולים לעסק זה");
                            btnPickDate.setEnabled(false);
                        }

                        // יצירת מתאם פשוט עבור רכיב הספינר וחיבור רשימת השמות אליו
                        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(BookingActivity.this, android.R.layout.simple_spinner_item, treatmentNames);
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerTreatments.setAdapter(spinnerAdapter);

                        // הגדרת מאזין בחירת פריט אנונימי קלאסי עבור רכיב הספינר
                        spinnerTreatments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (!treatmentList.isEmpty()) {
                                    // שמירת הטיפול שנבחר במשתנה הגלובלי ואיפוס בחירת השעה הקודמת
                                    selectedTreatment = treatmentList.get(position);
                                    selectedTime = "";
                                    btnConfirmBooking.setEnabled(false);
                                    // אם כבר נבחר תאריך, נחשב ונרענן מחדש את חלונות הזמן לפי אורך הטיפול החדש
                                    if (selectedCalendar != null) loadRealTimeSlots(selectedCalendar);
                                }
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) { selectedTreatment = null; }
                        });
                    }
                });
    }

    // פעולה פרטית המציגה דיאלוג קופץ לבחירת תאריך מתוך רכיב לוח שנה מובנה
    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(year, month, dayOfMonth);
                        // המרת התאריך הנבחר לפורמט מחרוזת קבוע (dd/MM/yyyy) לסינכרון מול השרת
                        selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        tvSelectedDate.setText("תאריך נבחר: " + selectedDate);
                        selectedTime = "";
                        btnConfirmBooking.setEnabled(false);
                        // מעבר לשלב הבא: בדיקת שעות הפעילות וזמינות התורים לתאריך זה
                        loadRealTimeSlots(selectedCalendar);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // הגבלת לוח השנה כך שלא יהיה ניתן לבחור תאריכים שעברו (מינימום מהזמן הנוכחי)
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    // פעולה פרטית השולפת את הגדרות שעות הפעילות של העסק עבור היום בשבוע שנבחר
    private void loadRealTimeSlots(Calendar selectedDateCal) {
        if (selectedTreatment == null) return;
        timeSlotsList.clear();
        adapter.notifyDataSetChanged();
        tvNoSlots.setText("בודק זמינות...");
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);

        // חישוב אינדקס היום בשבוע (0-6) התואם למפתח השמור במבנה הנתונים בענן
        String dayOfWeekKey = String.valueOf(selectedDateCal.get(Calendar.DAY_OF_WEEK) - 1);

        // שליפת מסמך הגדרות העסק מתוך Firestore
        db.collection("businesses").document(currentBusinessId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot businessDoc) {
                        if (!businessDoc.exists()) { showNoSlots("העסק לא נמצא"); return; }

                        // קבלת אורך משך זמן הטיפול הספציפי בדקות שנבחר מהספינר
                        int appointmentDuration = selectedTreatment.getDurationMinutes();
                        // שליפת מפת לוח הזמנים השבועי מתוך מסמך העסק
                        Map<String, Object> weeklySchedule = (Map<String, Object>) businessDoc.get("weeklySchedule");

                        if (weeklySchedule != null && weeklySchedule.containsKey(dayOfWeekKey)) {
                            Map<String, Object> dayData = (Map<String, Object>) weeklySchedule.get(dayOfWeekKey);
                            // בדיקה האם העסק אכן פתוח לקבלת קהל ביום זה בשבוע
                            if (dayData != null && Boolean.TRUE.equals(dayData.get("isOpen"))) {
                                // מעבר לשלב הבא: שליפת התורים הקיימים שתפסו שעות באותו יום בענן
                                fetchBookedSlotsAndGenerate((String)dayData.get("start"), (String)dayData.get("end"), appointmentDuration);
                            } else { showNoSlots("העסק סגור ביום זה"); }
                        } else { showNoSlots("לא הוגדרו שעות פעילות"); }
                    }
                });
    }

    // פעולה פרטית השולפת את כל התורים התפוסים של העסק בתאריך שנבחר לצורך סינון חפיפות
    private void fetchBookedSlotsAndGenerate(String start, String end, int duration) {
        db.collection("appointments")
                .whereEqualTo("businessId", currentBusinessId)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        // יצירת רשימה של מערכים מספריים לייצוג טווחי הזמן התפוסים בדקות [התחלה, סיום]
                        List<int[]> bookedRangesList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            // סינון והתעלמות מתורים שסטטוס העבודה שלהם נדחה או בוטל
                            if (!"REJECTED".equals(doc.getString("status"))) {
                                int bStart = convertTimeToMinutes(doc.getString("time"));
                                Long bDur = doc.getLong("duration");
                                // הוספת טווח הזמן: שעת ההתחלה ועד שעת הסיום (התחלה + משך הטיפול)
                                bookedRangesList.add(new int[]{bStart, bStart + (bDur != null ? bDur.intValue() : 30)});
                            }
                        }
                        // קריאה לפעולה האלגוריתמית שמייצרת ומסננת את חלונות השעות הפנויות
                        generateSlots(start, end, duration, bookedRangesList);
                    }
                });
    }

    // פעולה פרטית המייצרת חלונות זמן קפיצה ובודקת חפיפות מול רשימת הטווחים התפוסים
    private void generateSlots(String start, String end, int duration, List<int[]> bookedRanges) {
        timeSlotsList.clear();
        int current = convertTimeToMinutes(start);
        int stop = convertTimeToMinutes(end);

        // לולאה הרצה משעת הפתיחה ומתקדמת במרווחים קבועים של 30 דקות כל עוד יש מקום לטיפול
        while (current + duration <= stop) {
            boolean overlap = false;
            // לולאה פנימית הבודקת חפיפת זמנים מתמטית מול כל אחד מהתורים הקיימים באותו יום
            for (int[] r : bookedRanges) {
                if (current < r[1] && (current + duration) > r[0]) {
                    overlap = true;
                    break;
                }
            }
            // אם חלון הזמן הנוכחי נמצא פנוי ואינו חופף לאף תור קיים, נוסיף אותו לרשימה הויזואלית
            if (!overlap) {
                timeSlotsList.add(convertMinutesToTime(current));
            }
            // התקדמות בקפיצות של 30 דקות כדי לאפשר גמישות בתחילת תורים
            current += 30;
        }

        // בדיקה האם נוצרו חלונות זמן פנויים כלשהם והצגת הודעה מתאימה
        if (timeSlotsList.isEmpty()) showNoSlots("אין תורים פנויים");
        else {
            tvNoSlots.setVisibility(View.GONE);
            rvTimeSlots.setVisibility(View.VISIBLE);
            // עדכון המתאם והזרקת רשימת השעות ואורך הטיפול לרכיב הויזואלי
            adapter.updateData(timeSlotsList, duration);
        }
    }

    // פעולה פרטית לשליפת שם הלקוח המחובר כעת מהמסד לפני השלמת הרישום
    private void saveAppointmentRequest() {
        if (auth.getCurrentUser() == null || selectedTreatment == null) return;
        btnConfirmBooking.setEnabled(false);
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                // מעבר לפעולת הרישום הסופית עם מזהה ושם הלקוח האמיתי
                finalizeBooking(userId, doc.getString("name") != null ? doc.getString("name") : "לקוח");
            }
        });
    }

    // פעולה פרטית היוצרת את מסמך התור החדש בענן, ושולחת הודעת SMS מתאימה לבעל העסק
    private void finalizeBooking(String userId, String userName) {
        // יצירת מזהה מסמך ייחודי ואקראי עבור התור החדש ב-Firestore
        String appointmentId = db.collection("appointments").document().getId();

        // אריזת כל שדות הנתונים של התור החדש לתוך מפת נתונים מובנית
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("appointmentId", appointmentId);
        data.put("businessId", currentBusinessId);
        data.put("businessName", currentBusinessName);
        data.put("userId", userId);
        data.put("userName", userName);
        data.put("date", selectedDate);
        data.put("time", selectedTime);
        data.put("status", "PENDING"); // קביעת סטטוס ראשוני כממתין לאישור של בעל העסק
        data.put("timestamp", System.currentTimeMillis());
        data.put("description", selectedTreatment.getName());
        data.put("duration", selectedTreatment.getDurationMinutes());
        data.put("price", selectedTreatment.getPrice());

        // שמירה וכתיבה של מפת הנתונים לתוך מסמך התור החדש בענן
        db.collection("appointments").document(appointmentId).set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // שליפת מספר הטלפון של העסק כדי להכין הודעת טקסט לבעליו
                        db.collection("businesses").document(currentBusinessId).get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot businessDoc) {
                                        String businessPhone = "";
                                        if (businessDoc.exists()) {
                                            businessPhone = businessDoc.getString("phone");
                                        }

                                        // ניסוח מחרוזת הודעת ה-SMS המובנית עם פרטי ההזמנה המלאים
                                        String message = "היי! ביקשתי לקבוע תור ל" + selectedTreatment.getName() + " בתאריך " + selectedDate + " בשעה " + selectedTime + ". אשמח לאישור המערכת. תודה, " + userName;

                                        // יצירת כוונת (Intent) מפורשת לפתיחת אפליקציית ה-SMS החיצונית של המכשיר
                                        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                                        smsIntent.setData(Uri.parse("smsto:" + (businessPhone != null ? businessPhone : "")));
                                        smsIntent.putExtra("sms_body", message);

                                        try {
                                            // הפעלת האקטיביטי החיצונית של שליחת הודעת ה-SMS
                                            startActivity(smsIntent);
                                        } catch (Exception e) {
                                            Toast.makeText(BookingActivity.this, "לא נמצאה אפליקציית SMS מותקנת במכשיר", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                        Toast.makeText(BookingActivity.this, "התור נשמר! אנא שלח את ה-SMS לאישור סופי.", Toast.LENGTH_LONG).show();
                        // סגירת מסך ההזמנה וחזרה למסך הראשי
                        finish();
                    }
                });
    }

    // פעולת עזר מתמטית המקבלת מחרוזת שעה (HH:mm) ומחזירה את סך הדקות הכולל מתחילת היום
    private int convertTimeToMinutes(String t) {
        try { String[] p = t.split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); }
        catch (Exception e) { return 0; }
    }

    // פעולת עזר מתמטית המקבלת מספר דקות כולל ומחזירה מחרוזת טקסט בפורמט שעה קבוע
    private String convertMinutesToTime(int m) {
        return String.format(Locale.getDefault(), "%02d:%02d", m / 60, m % 60);
    }

    // פעולה פרטית המעדכנת את רכיבי הממשק במקרה שבו אין תורים זמינים להצגה
    private void showNoSlots(String msg) {
        tvNoSlots.setText(msg);
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);
    }

    // מחלקת מתאם פנימית (Adapter) לניהול, הזרקת וצביעת רשימת חלונות השעות ברכיב ה-RecyclerView
    class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
        private List<String> slots;
        private int duration;
        private int selectedPos = -1; // משתנה לשמירת המיקום שנבחר ברשת, ברירת מחדל 1- (לא נבחר)

        public TimeSlotAdapter(List<String> s, int d) { this.slots = s; this.duration = d; }

        public void updateData(List<String> s, int d) { this.slots = s; this.duration = d; this.selectedPos = -1; notifyDataSetChanged(); }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_time_slot, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            h.tvTime.setText(slots.get(p));

            // שינוי צבעי הכרטיסייה והטקסט במידה וזו השעה הספציפית שהלקוח לחץ עליה
            h.cardView.setCardBackgroundColor(selectedPos == p ? Color.parseColor("#6200EE") : Color.WHITE);
            h.tvTime.setTextColor(selectedPos == p ? Color.WHITE : Color.BLACK);

            // הגדרת מאזין לחיצה אנונימי רגיל על פריט השעה ברשת
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // שמירת המיקום והשעה הנבחרת, הפעלת כפתור האישור ורענון צבעי הרשימה
                    selectedPos = h.getAdapterPosition();
                    selectedTime = slots.get(selectedPos);
                    btnConfirmBooking.setEnabled(true);
                    notifyDataSetChanged();
                }
            });
        }

        @Override public int getItemCount() { return slots.size(); }

        // מחלקת ViewHolder פנימית לקישור והחזקת הרכיבים הויזואליים של פריט שעה בודד
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime; CardView cardView;
            public ViewHolder(View v) { super(v); tvTime = v.findViewById(R.id.tvTimeSlot); cardView = v.findViewById(R.id.cardSlot); }
        }
    }
}