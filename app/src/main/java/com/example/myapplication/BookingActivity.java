package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private TextView tvSelectedDate, tvNoSlots;
    private RecyclerView rvTimeSlots;
    private Button btnPickDate, btnConfirmBooking;
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

        // קבלת ה-ID שהגיע מהאדפטר
        currentBusinessId = getIntent().getStringExtra("businessId");
        currentBusinessName = getIntent().getStringExtra("businessName");

        if (currentBusinessId == null) {
            // גיבוי למקרה שהמפתח נשלח באותיות גדולות
            currentBusinessId = getIntent().getStringExtra("BUSINESS_ID");
        }

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
        etDescription = findViewById(R.id.etDescription);

        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        timeSlotsList = new ArrayList<>();
        adapter = new TimeSlotAdapter(timeSlotsList, 0);
        rvTimeSlots.setAdapter(adapter);

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnConfirmBooking.setOnClickListener(v -> saveAppointmentRequest());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("הזמנת תור");
        }
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

                    // שמירת התאריך בפורמט אחיד
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

        // שימוש באינדקס (0-6) כדי להתאים לקוד החדש של BusinessHoursActivity
        String dayOfWeekKey = String.valueOf(selectedDateCal.get(Calendar.DAY_OF_WEEK) - 1);
        Log.d("BookingDebug", "Searching for day index: " + dayOfWeekKey + " for business: " + currentBusinessId);

        db.collection("businesses").document(currentBusinessId).get()
                .addOnSuccessListener(businessDoc -> {
                    if (!businessDoc.exists()) {
                        showNoSlots("העסק לא נמצא במערכת");
                        return;
                    }

                    Long durationLong = businessDoc.getLong("appointmentDuration");
                    int appointmentDuration = (durationLong != null) ? durationLong.intValue() : 30;

                    Map<String, Object> weeklySchedule = (Map<String, Object>) businessDoc.get("weeklySchedule");

                    if (weeklySchedule != null && weeklySchedule.containsKey(dayOfWeekKey)) {
                        Map<String, Object> dayData = (Map<String, Object>) weeklySchedule.get(dayOfWeekKey);

                        if (dayData != null) {
                            Boolean isOpen = (Boolean) dayData.get("isOpen");
                            if (isOpen != null && isOpen) {
                                String startTime = (String) dayData.get("start");
                                String endTime = (String) dayData.get("end");
                                fetchBookedSlotsAndGenerate(startTime, endTime, appointmentDuration);
                            } else {
                                showNoSlots("העסק סגור ביום זה");
                            }
                        }
                    } else {
                        showNoSlots("לא הוגדרו שעות פעילות ליום זה");
                    }
                })
                .addOnFailureListener(e -> showNoSlots("שגיאה בתקשורת עם השרת"));
    }

    private void fetchBookedSlotsAndGenerate(String start, String end, int duration) {
        db.collection("appointments")
                .whereEqualTo("businessId", currentBusinessId)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> bookedTimes = new HashSet<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String status = doc.getString("status");
                        if (status != null && !status.equals("REJECTED")) {
                            String time = doc.getString("time");
                            if (time != null) bookedTimes.add(time);
                        }
                    }
                    generateSlots(start, end, duration, bookedTimes);
                })
                .addOnFailureListener(e -> showNoSlots("שגיאה בבדיקת תורים תפוסים"));
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
            showNoSlots("אין תורים פנויים לשעות אלו");
        } else {
            tvNoSlots.setVisibility(View.GONE);
            rvTimeSlots.setVisibility(View.VISIBLE);
            adapter.updateData(timeSlotsList, durationMinutes);
        }
    }

    private void saveAppointmentRequest() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "עליך להתחבר", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmBooking.setEnabled(false);
        String userId = auth.getCurrentUser().getUid();

        // שליפת שם המשתמש מהפרופיל שלו
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            String userName = "לקוח";
            if (doc.exists()) {
                userName = doc.getString("name");
                if (userName == null) userName = doc.getString("fullName");
            }
            finalizeBooking(userId, userName != null ? userName : "לקוח");
        }).addOnFailureListener(e -> finalizeBooking(userId, "לקוח"));
    }

    private void finalizeBooking(String userId, String userName) {
        String appointmentId = db.collection("appointments").document().getId();
        String description = etDescription.getText().toString().trim();

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("appointmentId", appointmentId);
        data.put("businessId", currentBusinessId);
        data.put("businessName", currentBusinessName);
        data.put("userId", userId);
        data.put("userName", userName);
        data.put("date", selectedDate);
        data.put("time", selectedTime);
        data.put("status", "PENDING");
        data.put("timestamp", System.currentTimeMillis());
        data.put("description", description.isEmpty() ? "ללא הערות" : description);

        db.collection("appointments").document(appointmentId).set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "התור נשלח לאישור בעל העסק!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmBooking.setEnabled(true);
                    Toast.makeText(this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                });
    }

    private int convertTimeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) { return 0; }
    }

    private String convertMinutesToTime(int mins) {
        return String.format(Locale.getDefault(), "%02d:%02d", mins / 60, mins % 60);
    }

    private void showNoSlots(String message) {
        tvNoSlots.setText(message);
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);
    }

    // --- TimeSlotAdapter ---
    class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
        private List<String> slots;
        private int duration;
        private int selectedPos = -1;

        public TimeSlotAdapter(List<String> slots, int duration) {
            this.slots = slots;
            this.duration = duration;
        }

        public void updateData(List<String> newSlots, int newDuration) {
            this.slots = newSlots;
            this.duration = newDuration;
            this.selectedPos = -1;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String time = slots.get(position);
            holder.tvTime.setText(time);

            if (selectedPos == position) {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#6200EE"));
                holder.tvTime.setTextColor(Color.WHITE);
            } else {
                holder.cardView.setCardBackgroundColor(Color.WHITE);
                holder.tvTime.setTextColor(Color.BLACK);
            }

            holder.itemView.setOnClickListener(v -> {
                selectedPos = holder.getAdapterPosition();
                selectedTime = slots.get(selectedPos);
                btnConfirmBooking.setEnabled(true);
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() { return slots.size(); }

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