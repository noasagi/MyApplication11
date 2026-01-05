package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;

public class BookingActivity extends AppCompatActivity {

    private TextView tvSelectedDate, tvNoSlots;
    private RecyclerView rvTimeSlots;
    private Button btnPickDate, btnConfirmBooking;

    private String selectedDate = "";
    private String selectedTime = "";
    private TimeSlotAdapter adapter;
    private List<String> timeSlotsList;

    private String currentBusinessId;
    private String currentBusinessName;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        currentBusinessId = getIntent().getStringExtra("businessId");
        currentBusinessName = getIntent().getStringExtra("businessName");

        if (currentBusinessId == null) {
            Toast.makeText(this, "שגיאה: לא זוהה עסק", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoSlots = findViewById(R.id.tvNoSlots);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        if (getSupportActionBar() != null) {
            String title = (currentBusinessName != null) ? currentBusinessName : "הזמנת תור";
            getSupportActionBar().setTitle("הזמנת תור ל" + title);
        }

        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        timeSlotsList = new ArrayList<>();
        adapter = new TimeSlotAdapter(timeSlotsList);
        rvTimeSlots.setAdapter(adapter);

        btnPickDate.setOnClickListener(v -> showDatePicker());

        btnConfirmBooking.setOnClickListener(v -> saveAppointmentRequest());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year1, month1, dayOfMonth);

                    selectedDate = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
                    tvSelectedDate.setText("תאריך נבחר: " + selectedDate);

                    selectedTime = "";
                    btnConfirmBooking.setEnabled(false);

                    loadRealTimeSlots(selectedCal);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void loadRealTimeSlots(Calendar selectedDateCal) {
        timeSlotsList.clear();
        adapter.notifyDataSetChanged();
        tvNoSlots.setText("בודק זמינות...");
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);

        String dayOfWeekKey = getHebrewDayName(selectedDateCal.get(Calendar.DAY_OF_WEEK));

        db.collection("businesses").document(currentBusinessId).get()
                .addOnSuccessListener(businessDoc -> {
                    if (!businessDoc.exists()) return;

                    Long durationLong = businessDoc.getLong("appointmentDuration");
                    int appointmentDuration = (durationLong != null) ? durationLong.intValue() : 30;

                    Map<String, Object> weeklySchedule = (Map<String, Object>) businessDoc.get("weeklySchedule");

                    if (weeklySchedule != null && weeklySchedule.containsKey(dayOfWeekKey)) {
                        Map<String, Object> dayData = (Map<String, Object>) weeklySchedule.get(dayOfWeekKey);
                        boolean isOpen = (boolean) dayData.get("isOpen");

                        if (isOpen) {
                            String startTime = (String) dayData.get("start");
                            String endTime = (String) dayData.get("end");

                            fetchBookedSlotsAndGenerate(startTime, endTime, appointmentDuration);
                        } else {
                            showNoSlots("העסק סגור ביום זה");
                        }
                    } else {
                        showNoSlots("לא הוגדרו שעות");
                    }
                })
                .addOnFailureListener(e -> showNoSlots("שגיאה בטעינת נתונים"));
    }

    private void fetchBookedSlotsAndGenerate(String start, String end, int duration) {
        db.collection("appointments")
                .whereEqualTo("businessId", currentBusinessId)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> bookedTimes = new HashSet<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // תיקון: שימוש ב-Appointment
                        Appointment app = doc.toObject(Appointment.class);

                        if (!app.getStatus().equals("REJECTED")) {
                            bookedTimes.add(app.getTime());
                        }
                    }
                    generateSlots(start, end, duration, bookedTimes);
                })
                .addOnFailureListener(e -> showNoSlots("שגיאה בבדיקת זמינות"));
    }

    private void generateSlots(String start, String end, int durationMinutes, Set<String> bookedTimes) {
        timeSlotsList.clear();
        int startMins = convertTimeToMinutes(start);
        int endMins = convertTimeToMinutes(end);

        while (startMins + durationMinutes <= endMins) {
            String timeString = convertMinutesToTime(startMins);

            if (!bookedTimes.contains(timeString)) {
                timeSlotsList.add(timeString);
            }

            startMins += durationMinutes;
        }

        if (timeSlotsList.isEmpty()) {
            showNoSlots("אין תורים פנויים ביום זה");
        } else {
            tvNoSlots.setVisibility(View.GONE);
            rvTimeSlots.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void saveAppointmentRequest() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "יש להתחבר כדי לקבוע תור", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה שהמשתמש אכן בחר שעה ותאריך
        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "נא לבחור תאריך ושעה", Toast.LENGTH_SHORT).show();
            return;
        }

        // הכנה לשמירה
        btnConfirmBooking.setEnabled(false); // למנוע לחיצה כפולה
        String userId = auth.getCurrentUser().getUid();

        // שלב 1: משיכת השם של המשתמש מתוך טבלת users
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName = "לקוח"; // ברירת מחדל

                    if (documentSnapshot.exists()) {
                        // נסי למשוך את השם לפי איך שקראת לו ב-users (name, fullName, username...)
                        if (documentSnapshot.getString("name") != null) {
                            userName = documentSnapshot.getString("name");
                        } else if (documentSnapshot.getString("fullName") != null) {
                            userName = documentSnapshot.getString("fullName");
                        } else {
                            // אם אין שם, נשתמש במייל
                            String email = auth.getCurrentUser().getEmail();
                            if (email != null) userName = email;
                        }
                    }

                    // שלב 2: יצירת התור ושמירתו (עכשיו שיש לנו שם)
                    finalizeBooking(userId, userName);
                })
                .addOnFailureListener(e -> {
                    // במקרה של שגיאה במשיכת השם, נשתמש במייל או שם כללי
                    String fallbackName = auth.getCurrentUser().getEmail();
                    if (fallbackName == null) fallbackName = "לקוח אורח";
                    finalizeBooking(userId, fallbackName);
                });
    }

    // פונקציית עזר לשמירה הסופית (כדי לא לכתוב את הקוד פעמיים)
    private void finalizeBooking(String userId, String userName) {
        String appointmentId = db.collection("appointments").document().getId();
        String businessId = currentBusinessId; // משתמש במשתנה הגלובלי שהגדרנו ב-onCreate
        String date = selectedDate;
        String time = selectedTime;

        Appointment newAppointment = new Appointment(
                appointmentId,
                businessId,
                userId,
                userName, // <--- עכשיו השם הזה יהיה מלא
                date,
                time,
                "PENDING",
                new Date()
        );

        db.collection("appointments").document(appointmentId).set(newAppointment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "בקשתך נשלחה לבעל העסק!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשליחת הבקשה", Toast.LENGTH_SHORT).show();
                    btnConfirmBooking.setEnabled(true); // נאפשר לנסות שוב
                });
    }

    private int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private String convertMinutesToTime(int totalMinutes) {
        return String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60);
    }

    private void showNoSlots(String message) {
        tvNoSlots.setText(message);
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);
    }

    private String getHebrewDayName(int dayOfWeek) {
        // התיקון: חזרנו לעברית כדי להתאים למה ששמור בפיירבייס (כפי שראינו בתמונה)
        switch (dayOfWeek) {
            case Calendar.SUNDAY: return "יום ראשון";
            case Calendar.MONDAY: return "יום שני";
            case Calendar.TUESDAY: return "יום שלישי";
            case Calendar.WEDNESDAY: return "יום רביעי";
            case Calendar.THURSDAY: return "יום חמישי";
            case Calendar.FRIDAY: return "יום שישי";
            case Calendar.SATURDAY: return "יום שבת";
            default: return "יום ראשון";
        }
    }

    class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
        private List<String> slots;
        private int selectedPosition = -1;

        public TimeSlotAdapter(List<String> slots) { this.slots = slots; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String time = slots.get(position);
            holder.tvTime.setText(time);
            if (selectedPosition == position) {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#6200EE"));
                holder.tvTime.setTextColor(Color.WHITE);
            } else {
                holder.cardView.setCardBackgroundColor(Color.WHITE);
                holder.tvTime.setTextColor(Color.BLACK);
            }
            holder.itemView.setOnClickListener(v -> {
                int prev = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                selectedTime = slots.get(selectedPosition);
                btnConfirmBooking.setEnabled(true);
                notifyItemChanged(prev);
                notifyItemChanged(selectedPosition);
            });
        }

        @Override
        public int getItemCount() { return slots.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime;
            androidx.cardview.widget.CardView cardView;
            public ViewHolder(View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tvTimeSlot);
                cardView = itemView.findViewById(R.id.cardSlot);
            }
        }
    }
}