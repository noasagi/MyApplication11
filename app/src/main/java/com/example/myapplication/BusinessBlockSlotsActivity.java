package com.example.myapplication;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

// הגדרת מחלקת אקטיביטי לחסימת שעות וחלונות זמן על ידי בעל העסק, היורשת מ-BaseActivity
public class BusinessBlockSlotsActivity extends BaseActivity {

    // הצהרה על רכיבי טקסט להצגת התאריך הנבחר והודעות הסטטוס של המערכת
    private TextView tvSelectedDate, tvStatusMessage;
    // הצהרה על רכיב רשימה ממוחזרת להצגת חלונות הזמן במסך
    private RecyclerView rvSlots;
    // הצהרה על כפתור לפתיחת חלונית בחירת התאריך
    private Button btnPickDate;

    // הצהרה על עצם הגישה לבסיס הנתונים פיירסטור של פיירבייס
    private FirebaseFirestore db;
    // הצהרה על עצם הגישה למערכת אימות המשתמשים של פיירבייס
    private FirebaseAuth auth;
    // משתני מחרוזת לשמירת מזהה העסק והתאריך שנבחר על ידי המשתמשת
    private String businessId;
    private String selectedDate;
    // משתנה מספרי לשמירת משך זמן טיפול ברירת מחדל בדקות
    private int businessDuration = 30;

    // הצהרה על המתאם המותאם אישית של רשימת חלונות הזמן
    private SlotsAdapter adapter;
    // הצהרה על רשימה דינמית המכילה את מודלי חלונות הזמן
    private List<SlotModel> slotsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת וחיבור קובץ ה-XML של עיצוב מסך חסימת השעות
        setContentView(R.layout.activity_business_block_slots);

        // חיבור וקישור סרגל הכלים העליון של המסך
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // ביטול כותרת ברירת המחדל של סרגל הכלים במידה והוא קיים
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // אתחול וקבלת המופע הנוכחי של פיירסטור ואימות פיירבייס לקוד
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // קריאה לפעולה הפנימית לשליפת מזהה העסק של המשתמשת המחוברת
        fetchMyBusinessId();

        // קישור משתני הרכיבים לרכיבים הגרפיים האמיתיים מתוך קובץ ה-XML
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        btnPickDate = findViewById(R.id.btnPickDate);
        rvSlots = findViewById(R.id.rvSlots);

        // הגדרת מנהל פריסת רשת (גריד) עם 3 עמודות עבור רשימת השעות
        rvSlots.setLayoutManager(new GridLayoutManager(this, 3));
        // אתחול הרשימה הדינמית בזיכרון
        slotsList = new ArrayList<>();
        // יצירת מופע של המתאם וחיבורו לרכיב הרשימה הויזואלי
        adapter = new SlotsAdapter(slotsList);
        rvSlots.setAdapter(adapter);

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור בחירת התאריך
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // קריאה לפעולה שמציגה את חלונית בחירת התאריך הקופצת
                showDatePicker();
            }
        });
    }

    // פעולה פרטית לשליפת מזהה המסמך של העסק לפי ה-UID של המשתמשת המחוברת
    private void fetchMyBusinessId() {
        if (auth.getCurrentUser() == null) return;

        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        // אם נמצא מסמך עסק, נשלוף ונשמור את מזהה המסמך הייחודי שלו
                        if (!querySnapshot.isEmpty()) {
                            businessId = querySnapshot.getDocuments().get(0).getId();
                        } else {
                            Toast.makeText(BusinessBlockSlotsActivity.this, "לא נמצא עסק", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // פעולה פרטית המציגה דיאלוג קופץ לבחירת תאריך מתוך לוח שנה
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(year, month, dayOfMonth);

                        // המרת התאריך הנבחר לפורמט מחרוזת קבוע המסונכרן מול בסיס הנתונים
                        selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);

                        // הצגת התאריך הנבחר על גבי המסך למשתמשת
                        tvSelectedDate.setText("תאריך: " + selectedDate);
                        // קריאה לפעולה שטוענת את חלונות הזמן הפנויים והתפוסים עבור יום זה
                        loadSlotsForDate(selectedCal);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    // פעולה פרטית הטוענת את הגדרות שעות הפעילות של העסק עבור היום הנבחר בשבוע
    private void loadSlotsForDate(Calendar cal) {
        if (businessId == null) return;

        tvStatusMessage.setText("טוען שעות...");
        slotsList.clear();
        adapter.notifyDataSetChanged();

        // חישוב מפתח היום בשבוע (מ-0 עד 6) כדי להתאים למבנה הנתונים השמור בעסק
        String dayKey = String.valueOf(cal.get(Calendar.DAY_OF_WEEK) - 1);

        // שליפת מסמך העסק הספציפי מתוך Firestore
        db.collection("businesses").document(businessId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (!doc.exists()) return;

                        // שליפת מפת לוח הזמנים השבועי מתוך מסמך העסק
                        Map<String, Object> schedule = (Map<String, Object>) doc.get("weeklySchedule");
                        if (schedule != null && schedule.containsKey(dayKey)) {
                            Map<String, Object> dayData = (Map<String, Object>) schedule.get(dayKey);

                            // בדיקה האם העסק בכלל פתוח לקבלת קהל ביום זה בשבוע
                            Boolean isOpen = (Boolean) dayData.get("isOpen");
                            if (isOpen != null && isOpen) {
                                // שליפת שעות הפתיחה והסגירה של אותו היום
                                String start = (String) dayData.get("start");
                                String end = (String) dayData.get("end");

                                // שליפת משך זמן הטיפול המוגדר לעסק זה במסד
                                Long durationLong = doc.getLong("appointmentDuration");
                                businessDuration = (durationLong != null) ? durationLong.intValue() : 30;

                                // מעבר לשלב הבא: שליפת התורים הקיימים שכבר נקבעו לתאריך זה
                                fetchExistingAppointments(start, end, businessDuration);
                            } else {
                                tvStatusMessage.setText("העסק סגור ביום זה");
                            }
                        }
                    }
                });
    }

    // פעולה פרטית השולפת את כל התורים הקיימים בענן עבור התאריך הספציפי שנבחר
    private void fetchExistingAppointments(String start, String end, int duration) {
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        // יצירת מפת נתונים מקומית למיפוי תורים קיימים לפי שעת ההתחלה שלהם
                        Map<String, Appointment> bookedMap = new HashMap<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId());
                            // הוספת התור למפה רק אם הסטטוס שלו לא נדחה/בוטל
                            if (!"REJECTED".equals(app.getStatus())) {
                                bookedMap.put(app.getTime(), app);
                            }
                        }
                        // קריאה לפעולה האלגוריתמית שמייצרת ומציגה את רשימת חלונות הזמן הויזואלית
                        generateSlotsList(start, end, duration, bookedMap);
                    }
                });
    }

    // פעולה פרטית המייצרת את רשימת חלונות הזמן הדינמית בין שעת הפתיחה לשעת הסגירה
    private void generateSlotsList(String start, String end, int duration, Map<String, Appointment> bookedMap) {
        // המרת שעות הטקסט (HH:mm) למספר דקות כולל מתחילת היום לצורך חישוב מתמטי
        int startMins = convertTimeToMinutes(start);
        int endMins = convertTimeToMinutes(end);

        // לולאה הרצה ומתקדמת בכל פעם לפי אורך משך הטיפול כל עוד לא הגענו לשעת הסגירה
        while (startMins <= endMins) {
            // המרה חזרה ממספר דקות כולל למחרוזת שעה מובנית (HH:mm)
            String time = convertMinutesToTime(startMins);
            SlotModel slot = new SlotModel();
            slot.time = time;

            // בדיקה האם חלון הזמן הנוכחי כבר תפוס על ידי תור או חסימה קיימת במפה
            if (bookedMap.containsKey(time)) {
                Appointment app = bookedMap.get(time);
                // אם קיים מסמך חסימה בסטטוס BLOCKED, נסמן את חלון הזמן כחסום
                if ("BLOCKED".equals(app.getStatus())) {
                    slot.status = "BLOCKED";
                    slot.appointmentId = app.getAppointmentId();
                } else {
                    // אחרת, חלון הזמן תפוס על ידי לקוח אמיתי שקבע תור
                    slot.status = "BOOKED";
                }
            } else {
                // אם השעה לא קיימת במפה, חלון הזמן הזה פנוי לחלוטין
                slot.status = "FREE";
            }

            // הוספת חלון הזמן המאופיין אל תוך הרשימה הדינמית
            slotsList.add(slot);
            // קידום דקות ההתחלה לפי משך זמן הטיפול שנקבע לעסק
            startMins += duration;
        }
        tvStatusMessage.setText("");
        // עדכון המתאם לצורך ריענון וציור מחדש של רשת חלונות הזמן על המסך
        adapter.notifyDataSetChanged();
    }

    // פעולה פרטית המשנה את סטטוס חלון הזמן (חסימה או שחרור מחסימה) בעת לחיצה
    private void toggleSlotBlock(SlotModel slot) {
        // הגנה: אם חלון הזמן תפוס על ידי תור של לקוח, לא ניתן לחסום או לשנות אותו מכאן
        if (slot.status.equals("BOOKED")) return;

        // אם חלון הזמן פנוי, ניצור מסמך חדש באוסף התורים המייצג חסימה של שעה
        if (slot.status.equals("FREE")) {
            Appointment blockApp = new Appointment();
            blockApp.setBusinessId(businessId);
            blockApp.setDate(selectedDate);
            blockApp.setTime(slot.time);
            blockApp.setDuration(businessDuration);
            blockApp.setStatus("BLOCKED");
            blockApp.setTimestamp(new Date().getTime());

            db.collection("appointments").add(blockApp)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference docRef) {
                            // עדכון הסטטוס המקומי לחסום ושמירת מזהה המסמך שנוצר בענן לקטגוריית המודל
                            slot.status = "BLOCKED";
                            slot.appointmentId = docRef.getId();
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
        // אם חלון הזמן כבר חסום, לחיצה נוספת תמחק את מסמך החסימה מ-Firestore ותשחרר אותו
        else if (slot.status.equals("BLOCKED")) {
            db.collection("appointments").document(slot.appointmentId).delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // החזרת הסטטוס המקומי למצב פנוי ועדכון התצוגה הויזואלית
                            slot.status = "FREE";
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    // פעולת עזר מתמטית המקבלת מחרוזת שעה (למשל "10:30") ומחזירה את סך הדקות הכולל מתחילת היום (630)
    private int convertTimeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) { return 0; }
    }

    // פעולת עזר מתמטית המקבלת מספר דקות כולל ומחזירה מחרוזת זמן מעוצבת בפורמט שעה קבוע
    private String convertMinutesToTime(int totalMinutes) {
        return String.format(Locale.getDefault(), "%02d:%02d", totalMinutes / 60, totalMinutes % 60);
    }

    // מחלקת מודל פנימית לייצוג המאפיינים של חלון זמן בודד ברשת הויזואלית
    class SlotModel {
        String time, status, appointmentId;
    }

    // מחלקת מתאם פנימית (Adapter) לניהול, הזרקת וחיבור נתוני חלונות הזמן לרכיבי ה-RecyclerView
    class SlotsAdapter extends RecyclerView.Adapter<SlotsAdapter.ViewHolder> {
        List<SlotModel> list;
        public SlotsAdapter(List<SlotModel> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_time_slot, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            SlotModel item = list.get(p);
            h.tvTime.setText(item.time);

            // צביעת רקע הכרטיסייה בהתאם לסטטוס חלון הזמן: לבן לפנוי, אפור כהה לחסום, אדום לתפוס על ידי לקוח
            if ("FREE".equals(item.status)) h.cardView.setCardBackgroundColor(Color.WHITE);
            else if ("BLOCKED".equals(item.status)) h.cardView.setCardBackgroundColor(Color.DKGRAY);
            else h.cardView.setCardBackgroundColor(Color.RED);

            // הגדרת מאזין לחיצה אנונימי רגיל על פריט השעה לצורך ביצוע חסימה או שחרור
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSlotBlock(item);
                }
            });
        }

        @Override public int getItemCount() { return list.size(); }

        // מחלקת ViewHolder פנימית להחזקה וקישור הרכיבים הויזואליים של שורת פריט זמן בודד
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime; CardView cardView;
            public ViewHolder(View v) {
                super(v);
                tvTime = v.findViewById(R.id.tvTimeSlot);
                cardView = v.findViewById(R.id.cardSlot);
            }
        }
    }
}