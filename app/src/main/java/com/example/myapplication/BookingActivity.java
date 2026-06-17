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

// מסך הזמנת תור הכולל אלגוריתם לחישוב חלונות זמן פנויים וסינון חפיפות תורים
public class BookingActivity extends BaseActivity {

    // רכיבי ממשק המשתמש (UI)
    private TextView tvSelectedDate, tvNoSlots;
    private RecyclerView rvTimeSlots;
    private Button btnPickDate, btnConfirmBooking;
    private Spinner spinnerTreatments; // תיבה נפתחת לבחירת סוג הטיפול

    // משתני מצב לניהול התאריך, השעה והאדפטר
    private String selectedDate = "";
    private String selectedTime = "";
    private TimeSlotAdapter adapter;
    private List<String> timeSlotsList; // רשימה שתחזיק את השעות הפנויות שנמצאו (למשל: "10:30")
    private Calendar selectedCalendar = null; // אובייקט לניהול תאריך נבחר במערכת

    // משתני זיהוי העסק וחיבור ל-Firebase
    private String currentBusinessId;
    private String currentBusinessName = "";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // רשימות ומודלים לניהול סוגי הטיפולים
    private List<Treatment> treatmentList = new ArrayList<>();
    private Treatment selectedTreatment = null; // ישמור את אובייקט הטיפול שנבחר כרגע

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // הגדרת סרגל הכלים העליון עם חץ חזרה מובנה (הודות לירושה מ-BaseActivity)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // הסרת כותרת ברירת המחדל הסטטית של הסרגל כדי לעצב אותו בצורה נקייה יותר
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // אתחול רכיבי הגישה ל-Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // שליפת מזהה ושם העסק שהועברו אלינו מהמסך הקודם דרך ה-Intent
        currentBusinessId = getIntent().getStringExtra("businessId");
        currentBusinessName = getIntent().getStringExtra("businessName");
        if (currentBusinessId == null) {
            currentBusinessId = getIntent().getStringExtra("BUSINESS_ID");
        }

        // ניקוי רווחי קצוות ממחרוזת ה-ID כדי למנוע שגיאות בשליפה מ-Firestore
        if (currentBusinessId != null) {
            currentBusinessId = currentBusinessId.trim();
        }

        // הגנת בטיחות: אם משום מה לא הגיע מזהה עסק, נסגור את המסך מיד כדי למנוע קריסה
        if (currentBusinessId == null || currentBusinessId.isEmpty()) {
            Toast.makeText(this, "שגיאה: לא זוהה עסק", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // קישור כל רכיבי ה-XML אל משתני ה-Java
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoSlots = findViewById(R.id.tvNoSlots);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        spinnerTreatments = findViewById(R.id.spinnerTreatments);

        // אתחול ה-RecyclerView עם פריסת רשת (Grid) של 3 עמודות להצגת ריבועי השעות
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        timeSlotsList = new ArrayList<>();
        adapter = new TimeSlotAdapter(timeSlotsList, 0);
        rvTimeSlots.setAdapter(adapter);

        // מאזין ללחיצה על כפתור בחירת תאריך
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // וולידציה: המשתמש חייב קודם כל לבחור סוג טיפול, כי אורך הטיפול משפיע על חישוב השעות
                if (selectedTreatment == null) {
                    Toast.makeText(BookingActivity.this, "נא לבחור טיפול קודם", Toast.LENGTH_SHORT).show();
                    return;
                }
                showDatePicker(); // פתיחת חלונית לוח השנה
            }
        });

        // מאזין ללחיצה על כפתור אישור התור הסופי
        btnConfirmBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAppointmentRequest(); // התחלת תהליך השמירה ב-Firestore
            }
        });

        // טעינה ראשונית של סוגי הטיפולים שהעסק מציע
        loadTreatments();
    }

    /**
     * קלט: אין. | פלט: אין (void).
     * מה עושה: שולפת מתוך תת-אוסף (Collection) בשם "treatments" שנמצא בתוך מסמך העסק הספציפי,
     * את כל סוגי הטיפולים הזמינים ומכניסה אותם לתוך רכיב ה-Spinner הנפתח.
     */
    private void loadTreatments() {
        db.collection("businesses").document(currentBusinessId).collection("treatments")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        treatmentList.clear();
                        List<String> treatmentNames = new ArrayList<>();

                        // מעבר בלולאה על כל מסמכי הטיפולים שהתקבלו מהענן
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Treatment treatment = doc.toObject(Treatment.class); // המרה לאובייקט Java
                            treatmentList.add(treatment);

                            // בניית מחרוזת להצגה ויזואלית בספינר (שם הטיפול + משך הזמן שלו)
                            treatmentNames.add(treatment.getName() + " (" + treatment.getDurationMinutes() + " דקות)");
                        }

                        // טיפול במצב שבו לבעל העסק אין עדיין טיפולים רשומים במערכת
                        if (treatmentList.isEmpty()) {
                            treatmentNames.add("לא הוגדרו טיפולים לעסק זה");
                            btnPickDate.setEnabled(false); // חסימת האפשרות להתקדם לבחירת תאריך
                        }

                        // יצירת מתאם (ArrayAdapter) פשוט של אנדרואיד שמחבר את רשימת הטקסטים לספינר
                        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(BookingActivity.this, android.R.layout.simple_spinner_item, treatmentNames);
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerTreatments.setAdapter(spinnerAdapter);

                        // הגדרת מאזין שישים לב איזה פריט (טיפול) המשתמש בחר מתוך הרשימה הנפתחת
                        spinnerTreatments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (!treatmentList.isEmpty()) {
                                    // שמירת אובייקט הטיפול שנבחר במשתנה הגלובלי לפי המיקום (position) שלו
                                    selectedTreatment = treatmentList.get(position);
                                    selectedTime = ""; // איפוס השעה שנבחרה קודם לכן
                                    btnConfirmBooking.setEnabled(false); // חסימת כפתור האישור עד שתיבחר שעה חדשה

                                    // אופטימיזציה: אם המשתמש כבר בחר תאריך ואז שינה סוג טיפול, נחשב מחדש את השעות הפנויות
                                    if (selectedCalendar != null) loadRealTimeSlots(selectedCalendar);
                                }
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) { selectedTreatment = null; }
                        });
                    }
                });
    }

    /**
     * קלט: אין. | פלט: אין (void).
     * מה עושה: מייצרת ומציגה את חלונית לוח השנה המובנית של אנדרואיד (DatePickerDialog).
     * היא מגבילה את הבחירה החל מהיום הנוכחי, ובעת בחירה היא שומרת את התאריך ומפעילה את חישוב השעות.
     */
    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(year, month, dayOfMonth); // עדכון אובייקט לוח הזמנים הגלובלי

                        // המרת התאריך לפורמט מחרוזת קבוע איתו נעבוד מול מסד הנתונים: dd/MM/yyyy
                        selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        tvSelectedDate.setText("תאריך נבחר: " + selectedDate);
                        selectedTime = ""; // איפוס שעה קודמת
                        btnConfirmBooking.setEnabled(false);

                        // מעבר לשלב הבא: בדיקת שעות הפעילות של העסק עבור התאריך הזה
                        loadRealTimeSlots(selectedCalendar);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // הגבלת לוח השנה: מונע מהמשתמש לבצע בחירה של תאריכים מהעבר (מינימום מהרגע הנוכחי מינוס שנייה)
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    /**
     * קלט: אובייקט Calendar של התאריך הנבחר. | פלט: אין (void).
     * מה עושה: מחשבת את היום בשבוע (אינדקס 0 עד 6, כאשר 0 זה יום ראשון). היא שולפת מתוך מסמך העסק
     * ב-Firestore את מפת שעות הפעילות ("weeklySchedule"). אם העסק פתוח באותו יום, היא שולחת את שעות הפתיחה והסגירה לשלב הבא.
     */
    private void loadRealTimeSlots(Calendar selectedDateCal) {
        if (selectedTreatment == null) return;
        timeSlotsList.clear(); // ניקוי הרשימה הגלובלית לקראת החישוב החדש
        adapter.notifyDataSetChanged();

        // עדכון זמני של המסך למצב טעינה
        tvNoSlots.setText("בודק זמינות...");
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);

        // המרה: ג'אווה נותנת ליום ראשון את הערך 1, אנו מורידים 1 כדי להתאים למפתח שלנו בענן (0 = יום ראשון)
        String dayOfWeekKey = String.valueOf(selectedDateCal.get(Calendar.DAY_OF_WEEK) - 1);

        // קבלת מסמך העסק מתוך אוסף המערכת
        db.collection("businesses").document(currentBusinessId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot businessDoc) {
                        if (!businessDoc.exists()) { showNoSlots("העסק לא נמצא"); return; }

                        int appointmentDuration = selectedTreatment.getDurationMinutes(); // אורך הטיפול הנוכחי בדקות

                        // שליפת מפת לוח הזמנים השבועי מתוך מסמך העסק ב-Firestore
                        Map<String, Object> weeklySchedule = (Map<String, Object>) businessDoc.get("weeklySchedule");

                        if (weeklySchedule != null && weeklySchedule.containsKey(dayOfWeekKey)) {
                            Map<String, Object> dayData = (Map<String, Object>) weeklySchedule.get(dayOfWeekKey);

                            // בדיקה האם העסק הגדיר שהוא פתוח (isOpen = true) ביום הספציפי הזה בשבוע
                            if (dayData != null && Boolean.TRUE.equals(dayData.get("isOpen"))) {
                                // העסק פתוח! שולפים את שעות הפתיחה והסגירה (למשל "09:00", "17:00") ועוברים לשלב הבא
                                fetchBookedSlotsAndGenerate((String)dayData.get("start"), (String)dayData.get("end"), appointmentDuration);
                            } else { showNoSlots("העסק סגור ביום זה"); }
                        } else { showNoSlots("לא הוגדרו שעות פעילות"); }
                    }
                });
    }

    /**
     * קלט: שעת פתיחה (String), שעת סגירה (String), אורך הטיפול בדקות (int). | פלט: אין.
     * מה עושה: פונה לאוסף "appointments" הכללי בענן ושולפת את כל התורים שכבר נקבעו ותפוסים עבור העסק הזה *בתאריך הספציפי הזה*.
     * היא מתרגמת את זמני התורים התפוסים לטווחי דקות מספריים [התחלה, סיום] כדי שיהיה קל לחשב חפיפות מתמטיות.
     */
    private void fetchBookedSlotsAndGenerate(String start, String end, int duration) {
        db.collection("appointments")
                .whereEqualTo("businessId", currentBusinessId) // סינון: רק של העסק הנוכחי
                .whereEqualTo("date", selectedDate)            // סינון: רק לתאריך הנבחר
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        // רשימה של מערכי אינטג'ר באורך 2: אינדקס 0 מייצג דקת התחלה, אינדקס 1 מייצג דקת סיום תור תפוס
                        List<int[]> bookedRangesList = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            // אנו לוקחים בחשבון תורים תפוסים רק אם הסטטוס שלהם לא נדחה (REJECTED) על ידי העסק
                            if (!"REJECTED".equals(doc.getString("status"))) {
                                int bStart = convertTimeToMinutes(doc.getString("time")); // המרת שעת התור (למשל "10:00") לדקות מספריות מתחילת היום
                                Long bDur = doc.getLong("duration"); // משך זמן התור התפוס כפי שנשמר בענן

                                int appointmentDurationMinutes = (bDur != null) ? bDur.intValue() : 30; // ברירת מחדל של 30 דקות לגיבוי

                                // הוספת טווח התור התפוס: [זמן תחילת התור, זמן סיום התור]
                                bookedRangesList.add(new int[]{bStart, bStart + appointmentDurationMinutes});
                            }
                        }
                        // מעבר לחלק האלגוריתמי המרכזי: יצירת חלונות הזמן הריקים וסינונם
                        generateSlots(start, end, duration, bookedRangesList);
                    }
                });
    }

    /**
     * [האלגוריתם המרכזי של האפליקציה - חישוב חלונות זמן פנויים]
     * קלט: שעת פתיחה, שעת סגירה, אורך הטיפול המבוקש, ורשימת טווחי הדקות של התורים התפוסים. | פלט: אין.
     * איך הוא עובד: הוא מתחיל משעת הפתיחה ומריץ לולאה שקופצת כל 30 דקות קדימה. בכל קפיצה, הוא בודק מתמטית
     * האם חלון הזמן הנוכחי (משעה X ועד שעה X פלוס אורך הטיפול) מתנגש או חופף לאחד מהתורים התפוסים ביום זה.
     * אם אין שום חפיפה, השעה מומרת חזרה לטקסט (String) ונוספת לרשימה שמוצגת ללקוח.
     */
    private void generateSlots(String start, String end, int duration, List<int[]> bookedRanges) {
        timeSlotsList.clear();
        int current = convertTimeToMinutes(start); // המרת שעת הפתיחה (למשל "09:00" הופך ל-540 דקות)
        int stop = convertTimeToMinutes(end);      // המרת שעת הסגירה (למשל "18:00" הופך ל-1080 דקות)

        // תנאי הלולאה: המשך לרוץ כל עוד חלון הזמן הנוכחי בתוספת אורך הטיפול אינו חורג משעת סגירת העסק
        while (current + duration <= stop) {
            boolean overlap = false; // דגל (Flag) שמסמן האם מצאנו התנגשות עם תור קיים

            // לולאה פנימית שעוברת על כל אחד מהתורים שכבר תפוסים באותו יום
            for (int[] r : bookedRanges) {
                // נוסחת בדיקת חפיפה מתמטית בין שני טווחים [A1, A2] ו-[B1, B2]:
                // אם זמן ההתחלה של התור הנוכחי קטן מזמן הסיום של התור התפוס,
                // ובמקביל זמן הסיום של התור הנוכחי גדול מזמן ההתחלה של התור התפוס -> יש חפיפה והתנגשות!
                if (current < r[1] && (current + duration) > r[0]) {
                    overlap = true; // סימון שיש חפיפה
                    break;          // שבירת הלולאה הפנימית, אין טעם להמשיך לבדוק את שאר התורים עבור שעה זו
                }
            }

            // אם הלולאה הפנימית הסתיימה והדגל נשאר false (כלומר, השעה הזו פנויה לחלוטין ואין התנגשות)
            if (!overlap) {
                // המרת הדקות המספריות בחזרה לפורמט טקסט של שעה (למשל 540 הופך ל-"09:00") והוספתה לרשימה
                timeSlotsList.add(convertMinutesToTime(current));
            }

            // התקדמות בקפיצות של 30 דקות קדימה (מרווח קפיצה קבוע המאפשר גמישות בבחירת תחילת תורים)
            current += 30;
        }

        // בדיקה סופית האם נוצרו שעות פנויות להצגה
        if (timeSlotsList.isEmpty()) {
            showNoSlots("אין תורים פנויים ביום זה");
        } else {
            tvNoSlots.setVisibility(View.GONE);
            rvTimeSlots.setVisibility(View.VISIBLE);

            // עדכון האדפטר עם השעות החדשות שנמצאו ואורך הטיפול, וריענון התצוגה הגרפית ברשת
            adapter.updateData(timeSlotsList, duration);
        }
    }

    /**
     * קלט: אין. | פלט: אין (void).
     * מה עושה: לפני רישום התור, הפונקציה שולפת מ-Firestore את השם האמיתי של הלקוח המחובר כרגע (מתוך אוסף "users" לפי ה-UID שלו)
     * כדי שנוכל לרשום את שמו בתוך מסמך התור, בשביל שבעל העסק ידע מי הלקוח שמגיע אליו.
     */
    private void saveAppointmentRequest() {
        if (auth.getCurrentUser() == null || selectedTreatment == null) return;
        btnConfirmBooking.setEnabled(false); // חסימת כפתור הלחיצה למניעת שליחה כפולה של הטופס

        String userId = auth.getCurrentUser().getUid(); // קבלת ה-UID הייחודי של המשתמש המחובר

        // פנייה למסמך המשתמש באוסף "users"
        db.collection("users").document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                // קבלת שם המשתמש מהמסמך (או שימוש בברירת מחדל "לקוח") ומעבר לשלב הרישום הסופי
                finalizeBooking(userId, doc.getString("name") != null ? doc.getString("name") : "לקוח");
            }
        });
    }

    /**
     * קלט: מזהה משתמש (String), שם משתמש (String). | פלט: אין (void).
     * מה עושה: מייצרת מזהה אקראי ייחודי (ID) עבור התור החדש, אורזת את כל נתוני התור (תאריך, שעה, טיפול, מחיר, משך זמן, סטטוס ראשוני PENDING)
     * לתוך HashMap, ושומרת אותו ב-Firestore באוסף "appointments". בהצלחה, היא שולפת את מספר הטלפון של העסק ופותחת כוונת (Intent) ל-SMS.
     */
    private void finalizeBooking(String userId, String userName) {
        // יצירת מזהה ייחודי חדש ואקראי עבור מסמך התור ב-Firestore
        String appointmentId = db.collection("appointments").document().getId();

        // יצירת מפת נתונים (Key-Value) שבה נשמור את כל המידע על התור החדש
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("appointmentId", appointmentId);
        data.put("businessId", currentBusinessId);
        data.put("businessName", currentBusinessName);
        data.put("userId", userId);
        data.put("userName", userName);
        data.put("date", selectedDate);
        data.put("time", selectedTime);
        data.put("status", "PENDING"); // התחלה במצב "ממתין" - דורש אישור ידני של בעל העסק במסך שלו
        data.put("timestamp", System.currentTimeMillis()); // חותם זמן של רגע יצירת הבקשה
        data.put("description", selectedTreatment.getName()); // שם הטיפול הנבחר
        data.put("duration", selectedTreatment.getDurationMinutes());
        data.put("price", selectedTreatment.getPrice());

        // כתיבת מפת הנתונים בפועל לתוך מסמך חדש ב-Firestore תחת ה-ID שייצרנו
        db.collection("appointments").document(appointmentId).set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // הצעד הבא: שליפת מספר הטלפון של העסק כדי להכין למשתמש הודעת SMS מוכנה מראש
                        db.collection("businesses").document(currentBusinessId).get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot businessDoc) {
                                        String businessPhone = "";
                                        if (businessDoc.exists()) {
                                            businessPhone = businessDoc.getString("phone");
                                        }

                                        // ניסוח מחרוזת טקסט מובנית הכוללת את כל פרטי התור
                                        String message = "היי! ביקשתי לקבוע תור ל" + selectedTreatment.getName() + " בתאריך " + selectedDate + " בשעה " + selectedTime + ". אשמח לאישור המערכת. תודה, " + userName;

                                        // יצירת כוונת (Intent) לפתיחת אפליקציית ה-SMS החיצונית של הטלפון
                                        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                                        smsIntent.setData(Uri.parse("smsto:" + (businessPhone != null ? businessPhone : ""))); // הגדרת היעד לשליחה
                                        smsIntent.putExtra("sms_body", message); // השתלת הטקסט המנוסח בתוך ההודעה

                                        try {
                                            startActivity(smsIntent); // מעבר פיזי של המשתמש אל אפליקציית ה-SMS של המכשיר שלו
                                        } catch (Exception e) {
                                            Toast.makeText(BookingActivity.this, "לא נמצאה אפליקציית SMS מותקנת במכשיר", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                        Toast.makeText(BookingActivity.this, "התור נשמר! אנא שלח את ה-SMS לאישור סופי.", Toast.LENGTH_LONG).show();
                        finish(); // סגירת אקטיביטי ההזמנה וחזרה אוטומטית למסך הקודם
                    }
                });
    }

    /**
     * פונקציית עזר מתמטית.
     * קלט: מחרוזת זמן בפורמט "HH:mm" (למשל "02:30").
     * פלט: מספר הדקות הכולל מתחילת היום (int). למשל עבור "02:30" יוחזר (2 * 60) + 30 = 150 דקות.
     */
    private int convertTimeToMinutes(String t) {
        try {
            String[] p = t.split(":"); // פיצול המחרוזת לפי סימן הנקודתיים (אינדקס 0 זה השעה, אינדקס 1 זה הדקות)
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); // חישוב והחזרת התוצאה
        } catch (Exception e) { return 0; }
    }

    /**
     * פונקציית עזר מתמטית הפוכה.
     * קלט: מספר דקות כולל מתחילת היום (int). למשל: 150 דקות.
     * פלט: מחרוזת טקסט מעוצבת בפורמט זמן קבוע "HH:mm". עבור 150 יוחזר "02:30".
     */
    private String convertMinutesToTime(int m) {
        // m / 60 נותן את השעות (חלוקה שלמים), m % 60 נותן את דקות השארית (מודולו)
        return String.format(Locale.getDefault(), "%02d:%02d", m / 60, m % 60);
    }

    // פונקציית עזר המעדכנת את תצוגת המסך במצב שבו אין שעות פנויות להצגה
    private void showNoSlots(String msg) {
        tvNoSlots.setText(msg);
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);
    }

    // --- אדפטר פנימי מותאם אישית להצגה וניהול של פריטי השעות ברשת (RecyclerView Grid) ---
    class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
        private List<String> slots;
        private int duration;
        private int selectedPos = -1; // משתנה לשמירת אינדקס השעה שהמשתמש לחץ עליה (ברירת מחדל 1- פירושו שום דבר לא נבחר)

        public TimeSlotAdapter(List<String> s, int d) { this.slots = s; this.duration = d; }

        // פונקציה לעדכון הנתונים מתוך האקטיביטי ואיפוס הבחירה הקודמת בעת החלפת ימים או טיפולים
        public void updateData(List<String> s, int d) {
            this.slots = s;
            this.duration = d;
            this.selectedPos = -1; // איפוס הבחירה הגרפית
            notifyDataSetChanged(); // הוראה ל-RecyclerView לצייר את עצמו מחדש עם הנתונים המעודכנים
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            // ניפוח קובץ ה-XML שמייצג כרטיסיית שעה בודדת ברשת
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_time_slot, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            h.tvTime.setText(slots.get(p)); // הזרקת מחרוזת השעה (למשל "11:30") לתוך הטקסט בכרטיסייה

            // לוגיקה גרפית דינמית: אם מיקום השורה הנוכחית (p) שווה למיקום שהמשתמש לחץ עליו (selectedPos)
            // נצבע את הרקע של הכרטיסייה בצבע סגול כהה והטקסט בלבן. אם לא, הרקע יהיה לבן והטקסט שחור (מצב רגיל).
            h.cardView.setCardBackgroundColor(selectedPos == p ? Color.parseColor("#6200EE") : Color.WHITE);
            h.tvTime.setTextColor(selectedPos == p ? Color.WHITE : Color.BLACK);

            // הגדרת מאזין לחיצה על כרטיסיית שעה ספציפית ברשת
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // שמירת המיקום שנבחר לתוך המשתנה
                    selectedPos = h.getAdapterPosition();

                    // שמירת מחרוזת השעה הנבחרת מתוך רשימת השעות לתוך המשתנה הגלובלי של האקטיביטי
                    selectedTime = slots.get(selectedPos);

                    // הפעלת כפתור האישור הסופי במסך (כי כעת יש לנו גם טיפול, גם תאריך וגם שעה)
                    btnConfirmBooking.setEnabled(true);

                    // קריאה לרענון הציור של הרשימה, כדי שהכרטיסייה שנלחצה תיצבע מיד בסגול ושאר הכרטיסיות יחזרו ללבן
                    notifyDataSetChanged();
                }
            });
        }

        @Override public int getItemCount() { return slots.size(); } // מחזיר כמה ריבועי שעות סך הכל יש לצייר ברשת

        // מחלקת ViewHolder פנימית המחזיקה את רכיבי ה-XML המקומיים של שעה בודדת ברשת
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime;
            CardView cardView;
            public ViewHolder(View v) {
                super(v);
                tvTime = v.findViewById(R.id.tvTimeSlot);
                cardView = v.findViewById(R.id.cardSlot);
            }
        }
    }
}