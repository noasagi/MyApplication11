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

public class BusinessBlockSlotsActivity extends BaseActivity {

    private TextView tvSelectedDate, tvStatusMessage;
    private RecyclerView rvSlots;
    private Button btnPickDate;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;
    private String selectedDate;
    private int businessDuration = 30;

    private SlotsAdapter adapter;
    private List<SlotModel> slotsList;

    /**
     * מה הפעולה עושה: מאתחלת את המסך, מחברת את רכיבי הממשק (UI), מגדירה את מבנה הרשת של השעות ויוזמת שליפה של מזהה העסק.
     * קלט: Bundle savedInstanceState (נתוני מערכת לשחזור מצב האקטיביטי).
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_block_slots);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // שליפת מזהה העסק קורה מיד עם עליית המסך כדי שנדע למי לשייך את החסימות בהמשך
        fetchMyBusinessId();

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        btnPickDate = findViewById(R.id.btnPickDate);
        rvSlots = findViewById(R.id.rvSlots);

        // הגדרת ה-RecyclerView כמטריצה של 3 עמודות להצגת השעות בצורה נוחה וקומפקטית
        rvSlots.setLayoutManager(new GridLayoutManager(this, 3));
        slotsList = new ArrayList<>();
        adapter = new SlotsAdapter(slotsList);
        rvSlots.setAdapter(adapter);

        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
    }

    /**
     * מה הפעולה עושה: מוצאת ב-Firestore את מסמך העסק ששייך למשתמשת המחוברת כרגע (ownerId) ושומרת את ה-ID שלו.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void fetchMyBusinessId() {
        if (auth.getCurrentUser() == null) return;

        // שאילתה שמחפשת בתוך אוסף העסקים את העסק שבו שדה בעל העסק שווה ל-UID של מי שמחובר כרגע
        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        if (!querySnapshot.isEmpty()) {
                            businessId = querySnapshot.getDocuments().get(0).getId();
                        } else {
                            Toast.makeText(BusinessBlockSlotsActivity.this, "לא נמצא עסק", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: מציגה לוח שנה קופץ לבחירת תאריך, ולאחר הבחירה שומרת אותו בפורמט טקסט ומפעילה את טעינת השעות.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(year, month, dayOfMonth);

                        // המרה למחרוזת בפורמט אחיד "dd/MM/yyyy" כדי להתאים למבנה החיפוש ב-Firestore
                        selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        tvSelectedDate.setText("תאריך: " + selectedDate);

                        loadSlotsForDate(selectedCal);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    /**
     * מה הפעולה עושה: שולפת מתוך מסמך העסק את לוח הזמנים השבועי ומודדת אם העסק בכלל פתוח ביום שנבחר ובאילו שעות.
     * קלט: Calendar cal (אובייקט התאריך שנבחר).
     * פלט: אין (void).
     */
    private void loadSlotsForDate(Calendar cal) {
        if (businessId == null) return;

        tvStatusMessage.setText("טוען שעות...");
        slotsList.clear();
        adapter.notifyDataSetChanged();

        // המרת יום השבוע לאינדקס (0-6) התואם לשמירה שלנו בבסיס הנתונים (יום ראשון = 0)
        String dayKey = String.valueOf(cal.get(Calendar.DAY_OF_WEEK) - 1);

        db.collection("businesses").document(businessId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (!doc.exists()) return;

                        // שליפת מפת שעות הפעילות ואימות שהעסק אכן מוגדר כ"פתוח" ביום הזה
                        Map<String, Object> schedule = (Map<String, Object>) doc.get("weeklySchedule");
                        if (schedule != null && schedule.containsKey(dayKey)) {
                            Map<String, Object> dayData = (Map<String, Object>) schedule.get(dayKey);

                            Boolean isOpen = (Boolean) dayData.get("isOpen");
                            if (isOpen != null && isOpen) {
                                String start = (String) dayData.get("start");
                                String end = (String) dayData.get("end");

                                // שליפת אורך הטיפול המוגדר לעסק כדי לדעת לפיו איך לחלק את חלונות הזמן
                                Long durationLong = doc.getLong("appointmentDuration");
                                businessDuration = (durationLong != null) ? durationLong.intValue() : 30;

                                // שלב הבא: שליפת התורים הקיימים (והחסימות) לאותו יום כדי להצליב נתונים
                                fetchExistingAppointments(start, end, businessDuration);
                            } else {
                                tvStatusMessage.setText("העסק סגור ביום זה");
                            }
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: שולפת מ-Firestore את כל מסמכי התורים והחסימות השייכים לעסק זה בתאריך שנבחר.
     * קלט: String start (שעת תחילת פעילות), String end (שעת סיום פעילות), int duration (אורך חלון זמן).
     * פלט: אין (void).
     */
    private void fetchExistingAppointments(String start, String end, int duration) {
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        // מיפוי של התורים לתוך HashMap כאשר מפתח החיפוש המהיר הוא שעת התור (למשל "10:00")
                        Map<String, Appointment> bookedMap = new HashMap<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId());

                            // סינון של תורים שבוטלו או נדחו - הם לא אמורים לתפוס מקום ביומן
                            if (!"REJECTED".equals(app.getStatus())) {
                                bookedMap.put(app.getTime(), app);
                            }
                        }
                        generateSlotsList(start, end, duration, bookedMap);
                    }
                });
    }

    /**
     * מה הפעולה עושה: אלגוריתם המייצר חלונות זמן משעת הפתיחה עד שעת הסגירה, ומסווג כל חלון כ"פנוי", "תפוס על ידי לקוח", או "חסום על ידי העסק".
     * קלט: שעת התחלה, שעת סיום, אורך חלון, ומפת התורים הקיימים.
     * פלט: אין (void).
     */
    private void generateSlotsList(String start, String end, int duration, Map<String, Appointment> bookedMap) {
        // המרה של שעות טקסטואליות למספר דקות כולל כדי לאפשר חישוב מתמטי פשוט בלולאה
        int startMins = convertTimeToMinutes(start);
        int endMins = convertTimeToMinutes(end);

        while (startMins <= endMins) {
            String time = convertMinutesToTime(startMins);
            SlotModel slot = new SlotModel();
            slot.time = time;

            // בדיקה האם השעה הזו קיימת במפת התורים התפוסים ששלפנו קודם מהשרת
            if (bookedMap.containsKey(time)) {
                Appointment app = bookedMap.get(time);
                // במערכת שלנו, חסימה של בעל העסק נשמרת גם היא כתור, אך עם סטטוס מיוחד שנקרא "BLOCKED"
                if ("BLOCKED".equals(app.getStatus())) {
                    slot.status = "BLOCKED";
                    slot.appointmentId = app.getAppointmentId(); // שמירת ה-ID כדי לאפשר מחיקה קלה בלחיצה חוזרת
                } else {
                    slot.status = "BOOKED"; // תור אמיתי של לקוח
                }
            } else {
                slot.status = "FREE"; // אין שום דבר בשעה זו - החלון פנוי
            }

            slotsList.add(slot);
            startMins += duration; // קידום הלולאה קדימה לפי אורך הטיפול בעסק
        }
        tvStatusMessage.setText("");
        adapter.notifyDataSetChanged();
    }

    /**
     * מה הפעולה עושה: מנהלת את מנגנון הלחיצה (Toggle) של בעל העסק: חוסמת שעה פנויה (יוצרת מסמך), או משחררת שעה חסומה (מוחקת מסמך).
     * קלט: SlotModel slot (החלון עליו המשתמשת לחצה).
     * פלט: אין (void).
     */
    private void toggleSlotBlock(SlotModel slot) {
        // הגנה עסקית: אם לקוח כבר הזמין תור בשעה הזו, לבעל העסק אסור לחסום את המקום באופן שרירותי מכאן
        if (slot.status.equals("BOOKED")) return;

        // מצב פנוי - חסימה: יצירת אובייקט "תור" חדש בסטטוס BLOCKED והעלאתו לענן
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
                            slot.status = "BLOCKED";
                            slot.appointmentId = docRef.getId(); // הצמדת ה-ID שקיבלנו מהשרת למודל המקומי
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
        // מצב חסום  שחרור: מחיקת מסמך החסימה הספציפי מ-Firestore על פי המזהה שלו
        else if (slot.status.equals("BLOCKED")) {
            db.collection("appointments").document(slot.appointmentId).delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            slot.status = "FREE";
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private int convertTimeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) { return 0; }
    }

    private String convertMinutesToTime(int totalMinutes) {
        return String.format(Locale.getDefault(), "%02d:%02d", totalMinutes / 60, totalMinutes % 60);
    }

    // --- מודל נתונים פנימי לייצוג מצב של חלון זמן בודד ---
    class SlotModel {
        String time, status, appointmentId;
    }

    // --- אדפטר פנימי לניהול פריטי השעות ברשת ה-RecyclerView ---
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

            // לוגיקת צביעה בהתאם למצב: לבן = חופשי, אפור כהה = חסום על ידי העסק, אדום = תפוס על ידי לקוח
            if ("FREE".equals(item.status)) h.cardView.setCardBackgroundColor(Color.WHITE);
            else if ("BLOCKED".equals(item.status)) h.cardView.setCardBackgroundColor(Color.DKGRAY);
            else h.cardView.setCardBackgroundColor(Color.RED);

            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSlotBlock(item);
                }
            });
        }

        @Override public int getItemCount() { return list.size(); }

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