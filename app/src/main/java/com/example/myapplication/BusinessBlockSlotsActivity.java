package com.example.myapplication;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusinessBlockSlotsActivity extends BaseActivity {

    private TextView tvSelectedDate, tvStatusMessage;
    private RecyclerView rvSlots;
    private Button btnPickDate;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;
    private String selectedDate;

    private SlotsAdapter adapter;
    private List<SlotModel> slotsList; // רשימה שתכיל את השעות והסטטוס שלהן

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_block_slots);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        fetchMyBusinessId();

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        btnPickDate = findViewById(R.id.btnPickDate);
        rvSlots = findViewById(R.id.rvSlots);

        rvSlots.setLayoutManager(new GridLayoutManager(this, 3));
        slotsList = new ArrayList<>();
        adapter = new SlotsAdapter(slotsList);
        rvSlots.setAdapter(adapter);

        btnPickDate.setOnClickListener(v -> showDatePicker());
    }

    private void fetchMyBusinessId() {
        if (auth.getCurrentUser() == null) return;

        // מוצאים את העסק של המשתמש המחובר
        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        businessId = querySnapshot.getDocuments().get(0).getId();
                    } else {
                        Toast.makeText(this, "לא נמצא עסק", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת פרטי העסק", Toast.LENGTH_SHORT).show());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    tvSelectedDate.setText("תאריך: " + selectedDate);

                    loadSlotsForDate(selectedCal);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void loadSlotsForDate(Calendar cal) {
        if (businessId == null) {
            tvStatusMessage.setText("עדיין טוען נתוני עסק, נסה שוב בעוד רגע...");
            return;
        }

        tvStatusMessage.setText("טוען שעות...");
        slotsList.clear();
        adapter.notifyDataSetChanged();

        // התיקון: התאמה למבנה במסד הנתונים (0 עד 6) בדיוק כמו בדף ההזמנות של הלקוח
        String dayKey = String.valueOf(cal.get(Calendar.DAY_OF_WEEK) - 1);

        db.collection("businesses").document(businessId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        tvStatusMessage.setText("העסק לא נמצא");
                        return;
                    }

                    Map<String, Object> schedule = (Map<String, Object>) doc.get("weeklySchedule");
                    if (schedule != null && schedule.containsKey(dayKey)) {
                        Map<String, Object> dayData = (Map<String, Object>) schedule.get(dayKey);

                        Boolean isOpen = (Boolean) dayData.get("isOpen"); // מונע קריסה במקרה של Null
                        if (isOpen != null && isOpen) {
                            String start = (String) dayData.get("start");
                            String end = (String) dayData.get("end");

                            Long durationLong = doc.getLong("appointmentDuration");
                            int duration = (durationLong != null) ? durationLong.intValue() : 30; // ברירת מחדל לביטחון

                            // עכשיו נבדוק איזה תורים כבר קיימים (לקוחות או חסימות)
                            fetchExistingAppointments(start, end, duration);
                        } else {
                            tvStatusMessage.setText("העסק סגור ביום זה");
                        }
                    } else {
                        tvStatusMessage.setText("אין שעות פעילות מוגדרות");
                    }
                })
                .addOnFailureListener(e -> tvStatusMessage.setText("שגיאה בתקשורת מול השרת"));
    }

    private void fetchExistingAppointments(String start, String end, int duration) {
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // מפה שתחזיק: שעה -> האובייקט של התור (כדי שנוכל למחוק אותו אם צריך)
                    Map<String, Appointment> bookedMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Appointment app = doc.toObject(Appointment.class);
                        app.setAppointmentId(doc.getId()); // חשוב למחיקה!
                        if (!"REJECTED".equals(app.getStatus())) {
                            bookedMap.put(app.getTime(), app);
                        }
                    }
                    generateSlotsList(start, end, duration, bookedMap);
                })
                .addOnFailureListener(e -> tvStatusMessage.setText("שגיאה בבדיקת תורים קיימים"));
    }

    private void generateSlotsList(String start, String end, int duration, Map<String, Appointment> bookedMap) {
        int startMins = convertTimeToMinutes(start);
        int endMins = convertTimeToMinutes(end);

        while (startMins + duration <= endMins) {
            String time = convertMinutesToTime(startMins);
            SlotModel slot = new SlotModel();
            slot.time = time;

            if (bookedMap.containsKey(time)) {
                Appointment app = bookedMap.get(time);
                if ("BLOCKED".equals(app.getStatus())) {
                    slot.status = "BLOCKED"; // חסום על ידי בעל העסק
                    slot.appointmentId = app.getAppointmentId(); // נשמור את ה-ID כדי שנוכל לבטל חסימה
                } else {
                    slot.status = "BOOKED"; // תפוס על ידי לקוח אמיתי
                }
            } else {
                slot.status = "FREE";
            }

            slotsList.add(slot);
            startMins += duration;
        }

        tvStatusMessage.setText("");
        adapter.notifyDataSetChanged();
    }

    // --- פעולות חסימה ושחרור ---

    private void toggleSlotBlock(SlotModel slot) {
        if (slot.status.equals("BOOKED")) {
            Toast.makeText(this, "לא ניתן לחסום תור שנקבע ע\"י לקוח. יש לבטל אותו קודם.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (slot.status.equals("FREE")) {
            // חסימת התור -> יצירת תור פיקטיבי
            Appointment blockApp = new Appointment();
            blockApp.setBusinessId(businessId);
            blockApp.setDate(selectedDate);
            blockApp.setTime(slot.time);
            blockApp.setStatus("BLOCKED"); // הסטטוס המיוחד שלנו
            blockApp.setTimestamp(new Date().getTime());

            db.collection("appointments").add(blockApp)
                    .addOnSuccessListener(docRef -> {
                        slot.status = "BLOCKED";
                        slot.appointmentId = docRef.getId();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "השעה נחסמה", Toast.LENGTH_SHORT).show();
                    });
        } else if (slot.status.equals("BLOCKED")) {
            // שחרור חסימה -> מחיקת התור הפיקטיבי
            if (slot.appointmentId != null) {
                db.collection("appointments").document(slot.appointmentId).delete()
                        .addOnSuccessListener(aVoid -> {
                            slot.status = "FREE";
                            slot.appointmentId = null;
                            adapter.notifyDataSetChanged();
                            Toast.makeText(this, "החסימה הוסרה", Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    // --- עזרים ---
    private int convertTimeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) { return 0; }
    }

    private String convertMinutesToTime(int totalMinutes) {
        return String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60);
    }

    // --- מחלקה פנימית לייצוג שעה ברשימה ---
    class SlotModel {
        String time;
        String status; // FREE, BLOCKED, BOOKED
        String appointmentId; // רלוונטי רק אם חסום
    }

    // --- Adapter ---
    class SlotsAdapter extends RecyclerView.Adapter<SlotsAdapter.ViewHolder> {
        List<SlotModel> list;
        public SlotsAdapter(List<SlotModel> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SlotModel item = list.get(position);
            holder.tvTime.setText(item.time);

            // צביעת הכפתורים לפי סטטוס
            switch (item.status) {
                case "FREE":
                    holder.cardView.setCardBackgroundColor(Color.WHITE); // לבן - פנוי
                    holder.tvTime.setTextColor(Color.BLACK);
                    holder.tvTime.setText(item.time + "\n(פנוי)");
                    break;
                case "BLOCKED":
                    holder.cardView.setCardBackgroundColor(Color.DKGRAY); // אפור כהה - חסום ע"י בעל עסק
                    holder.tvTime.setTextColor(Color.WHITE);
                    holder.tvTime.setText(item.time + "\n(חסום)");
                    break;
                case "BOOKED":
                    holder.cardView.setCardBackgroundColor(Color.RED); // אדום - יש לקוח
                    holder.tvTime.setTextColor(Color.WHITE);
                    holder.tvTime.setText(item.time + "\n(לקוח)");
                    break;
            }

            holder.itemView.setOnClickListener(v -> toggleSlotBlock(item));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime;
            CardView cardView;
            public ViewHolder(View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tvTimeSlot);
                cardView = itemView.findViewById(R.id.cardSlot);
            }
        }
    }
}