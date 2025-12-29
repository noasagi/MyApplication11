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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();

        // קליטת פרטי העסק
        currentBusinessId = getIntent().getStringExtra("businessId");
        currentBusinessName = getIntent().getStringExtra("businessName");

        if (currentBusinessId == null) {
            Toast.makeText(this, "שגיאה: לא זוהה עסק", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // חיבור רכיבים
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

        btnConfirmBooking.setOnClickListener(v -> {
            if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "נא לבחור תאריך ושעה", Toast.LENGTH_SHORT).show();
            } else {
                // כאן יהיה הקוד לשמירת ההזמנה הסופית
                Toast.makeText(this, "התור נקבע ל: " + selectedDate + " שעה " + selectedTime, Toast.LENGTH_LONG).show();
                finish();
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
                    // יצירת אובייקט לוח שנה כדי להבין איזה יום בשבוע נבחר
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year1, month1, dayOfMonth);

                    selectedDate = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
                    tvSelectedDate.setText("תאריך נבחר: " + selectedDate);

                    // איפוס בחירה קודמת
                    selectedTime = "";
                    btnConfirmBooking.setEnabled(false);

                    // קריאה לפונקציה האמיתית שטוענת מפיירבייס
                    loadRealTimeSlots(selectedCal);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    // --- הפונקציה החדשה שטוענת נתונים אמיתיים ---
    private void loadRealTimeSlots(Calendar selectedDateCal) {
        timeSlotsList.clear();
        adapter.notifyDataSetChanged();
        tvNoSlots.setText("טוען שעות...");
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);

        // המרת היום בשבוע לשם בעברית (כמו ששמרנו בפיירבייס)
        String dayOfWeekKey = getHebrewDayName(selectedDateCal.get(Calendar.DAY_OF_WEEK));

        db.collection("businesses").document(currentBusinessId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;

                    // קריאת משך התור
                    Long durationLong = documentSnapshot.getLong("appointmentDuration");
                    int appointmentDuration = (durationLong != null) ? durationLong.intValue() : 30; // ברירת מחדל 30

                    // קריאת הטבלה השבועית
                    Map<String, Object> weeklySchedule = (Map<String, Object>) documentSnapshot.get("weeklySchedule");

                    if (weeklySchedule != null && weeklySchedule.containsKey(dayOfWeekKey)) {
                        Map<String, Object> dayData = (Map<String, Object>) weeklySchedule.get(dayOfWeekKey);

                        boolean isOpen = (boolean) dayData.get("isOpen");

                        if (isOpen) {
                            String startTime = (String) dayData.get("start");
                            String endTime = (String) dayData.get("end");

                            // חישוב התורים
                            generateSlots(startTime, endTime, appointmentDuration);
                        } else {
                            showNoSlots("העסק סגור ביום זה");
                        }
                    } else {
                        showNoSlots("לא הוגדרו שעות ליום זה");
                    }
                })
                .addOnFailureListener(e -> showNoSlots("שגיאה בטעינת נתונים"));
    }

    // פונקציה שמחשבת את השעות (לוגיקה מתמטית)
    private void generateSlots(String start, String end, int durationMinutes) {
        timeSlotsList.clear();

        int startMins = convertTimeToMinutes(start);
        int endMins = convertTimeToMinutes(end);

        // לולאה שיוצרת תורים כל עוד נכנסים בטווח השעות
        while (startMins + durationMinutes <= endMins) {
            timeSlotsList.add(convertMinutesToTime(startMins));
            startMins += durationMinutes;
        }

        if (timeSlotsList.isEmpty()) {
            showNoSlots("אין תורים פנויים");
        } else {
            tvNoSlots.setVisibility(View.GONE);
            rvTimeSlots.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    // פונקציות עזר לחישובי זמן
    private int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int min = Integer.parseInt(parts[1]);
        return hour * 60 + min;
    }

    private String convertMinutesToTime(int totalMinutes) {
        int hour = totalMinutes / 60;
        int min = totalMinutes % 60;
        return String.format("%02d:%02d", hour, min);
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
            case Calendar.SATURDAY: return "יום שבת"; // למקרה שתרצי להוסיף
            default: return "יום ראשון";
        }
    }

    // --- ה-Adapter נשאר זהה ---
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
                int previousPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                selectedTime = slots.get(selectedPosition);
                btnConfirmBooking.setEnabled(true);
                notifyItemChanged(previousPosition);
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