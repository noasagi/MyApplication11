package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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

import com.google.android.material.textfield.TextInputEditText;
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

    // משתנה לתיבת הטקסט (הערות)
    private TextInputEditText etDescription;

    private String selectedDate = "";
    private String selectedTime = "";
    private TimeSlotAdapter adapter;
    private List<String> timeSlotsList;

    private String currentBusinessId;
    private String currentBusinessName = "";
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

        if (currentBusinessName == null || currentBusinessName.isEmpty()) {
            fetchBusinessNameFromDB();
        }

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoSlots = findViewById(R.id.tvNoSlots);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        // חיבור ה-View של ההערות
        etDescription = findViewById(R.id.etDescription);

        updateTitle();

        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3)); // אפשר לשנות ל-2 אם הכפתורים רחבים מדי
        timeSlotsList = new ArrayList<>();

        // *** שינוי 1: אתחול האדפטר עם 0 דקות כברירת מחדל ***
        adapter = new TimeSlotAdapter(timeSlotsList, 0);
        rvTimeSlots.setAdapter(adapter);

        btnPickDate.setOnClickListener(v -> showDatePicker());

        btnConfirmBooking.setOnClickListener(v -> saveAppointmentRequest());
    }

    private void updateTitle() {
        if (getSupportActionBar() != null) {
            String title = (currentBusinessName != null && !currentBusinessName.isEmpty()) ? currentBusinessName : "הזמנת תור";
            getSupportActionBar().setTitle("הזמנת תור ל" + title);
        }
    }

    private void fetchBusinessNameFromDB() {
        db.collection("businesses").document(currentBusinessId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentBusinessName = documentSnapshot.getString("businessName");
                        updateTitle();
                    }
                });
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
                        Appointment app = doc.toObject(Appointment.class);
                        if (app.getStatus() != null && !app.getStatus().equals("REJECTED")) {
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

            // *** שינוי 2: עדכון האדפטר עם משך הזמן החדש ***
            adapter.updateData(timeSlotsList, durationMinutes);
        }
    }

    private void saveAppointmentRequest() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "יש להתחבר כדי לקבוע תור", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "נא לבחור תאריך ושעה", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmBooking.setEnabled(false);
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName = "לקוח";
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.getString("name") != null) {
                            userName = documentSnapshot.getString("name");
                        } else if (documentSnapshot.getString("fullName") != null) {
                            userName = documentSnapshot.getString("fullName");
                        } else {
                            String email = auth.getCurrentUser().getEmail();
                            if (email != null) userName = email;
                        }
                    }
                    finalizeBooking(userId, userName);
                })
                .addOnFailureListener(e -> {
                    String fallbackName = auth.getCurrentUser().getEmail();
                    if (fallbackName == null) fallbackName = "לקוח אורח";
                    finalizeBooking(userId, fallbackName);
                });
    }

    private void finalizeBooking(String userId, String userName) {
        String appointmentId = db.collection("appointments").document().getId();

        // קבלת התיאור מהשדה
        String userDescription = etDescription.getText().toString().trim();
        if (userDescription.isEmpty()) {
            userDescription = "ללא תיאור";
        }

        Appointment newAppointment = new Appointment();
        newAppointment.setAppointmentId(appointmentId);
        newAppointment.setBusinessId(currentBusinessId);
        newAppointment.setBusinessName(currentBusinessName);
        newAppointment.setUserId(userId);
        newAppointment.setUserName(userName);
        newAppointment.setDate(selectedDate);
        newAppointment.setTime(selectedTime); // כאן נשמרת רק שעת ההתחלה
        newAppointment.setStatus("PENDING");
        newAppointment.setTimestamp(new Date().getTime());
        // שמירת התיאור
        newAppointment.setDescription(userDescription);

        db.collection("appointments").document(appointmentId).set(newAppointment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "בקשתך נשלחה לבעל העסק!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשליחת הבקשה", Toast.LENGTH_SHORT).show();
                    btnConfirmBooking.setEnabled(true);
                });
    }

    private int convertTimeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
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

    // --- Adapter המעודכן עם תצוגת טווח שעות ---
    class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
        private List<String> slots;
        private int durationMinutes; // משתנה למשך התור
        private int selectedPosition = -1;

        // בנאי מעודכן
        public TimeSlotAdapter(List<String> slots, int durationMinutes) {
            this.slots = slots;
            this.durationMinutes = durationMinutes;
        }

        // פונקציה לעדכון נתונים מבחוץ
        public void updateData(List<String> newSlots, int newDuration) {
            this.slots = newSlots;
            this.durationMinutes = newDuration;
            this.selectedPosition = -1; // איפוס בחירה כשמחליפים יום
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String startTime = slots.get(position);

            // חישוב שעת הסיום לתצוגה
            String endTime = calculateEndTime(startTime, durationMinutes);
            String displayTime = startTime + " - " + endTime;

            holder.tvTime.setText(displayTime);

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
                selectedTime = slots.get(selectedPosition); // שומרים רק את שעת ההתחלה
                btnConfirmBooking.setEnabled(true);
                notifyItemChanged(prev);
                notifyItemChanged(selectedPosition);
            });
        }

        @Override
        public int getItemCount() { return slots.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime;
            CardView cardView;
            public ViewHolder(View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tvTimeSlot);
                cardView = itemView.findViewById(R.id.cardSlot);
            }
        }

        // פונקציית עזר לחישוב זמן סיום
        private String calculateEndTime(String start, int duration) {
            try {
                String[] parts = start.split(":");
                int totalMin = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                totalMin += duration;
                return String.format("%02d:%02d", totalMin / 60, totalMin % 60);
            } catch (Exception e) {
                return start;
            }
        }
    }
}