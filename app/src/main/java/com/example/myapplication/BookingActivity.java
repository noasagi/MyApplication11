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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class BookingActivity extends BaseActivity {

    private TextView tvSelectedDate, tvNoSlots;
    private RecyclerView rvTimeSlots;
    private Button btnPickDate, btnConfirmBooking;
    private Spinner spinnerTreatments;

    private String selectedDate = "";
    private String selectedTime = "";
    private TimeSlotAdapter adapter;
    private List<String> timeSlotsList;
    private Calendar selectedCalendar = null;

    private String currentBusinessId;
    private String currentBusinessName = "";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private List<Treatment> treatmentList = new ArrayList<>();
    private Treatment selectedTreatment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        currentBusinessId = getIntent().getStringExtra("businessId");
        currentBusinessName = getIntent().getStringExtra("businessName");
        if (currentBusinessId == null) {
            currentBusinessId = getIntent().getStringExtra("BUSINESS_ID");
        }

        // מניעת שגיאות מרווחים נסתרים
        if (currentBusinessId != null) {
            currentBusinessId = currentBusinessId.trim();
        }

        if (currentBusinessId == null || currentBusinessId.isEmpty()) {
            Toast.makeText(this, "שגיאה: לא זוהה עסק", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoSlots = findViewById(R.id.tvNoSlots);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        spinnerTreatments = findViewById(R.id.spinnerTreatments);

        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        timeSlotsList = new ArrayList<>();
        adapter = new TimeSlotAdapter(timeSlotsList, 0);
        rvTimeSlots.setAdapter(adapter);

        btnPickDate.setOnClickListener(v -> {
            if (selectedTreatment == null) {
                Toast.makeText(this, "נא לבחור טיפול קודם", Toast.LENGTH_SHORT).show();
                return;
            }
            showDatePicker();
        });

        btnConfirmBooking.setOnClickListener(v -> saveAppointmentRequest());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("הזמנת תור");
        }

        loadTreatments();
    }

    private void loadTreatments() {
        db.collection("businesses").document(currentBusinessId).collection("treatments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    treatmentList.clear();
                    List<String> treatmentNames = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Treatment treatment = doc.toObject(Treatment.class);
                        treatmentList.add(treatment);
                        treatmentNames.add(treatment.getName() + " (" + treatment.getDurationMinutes() + " דקות)");
                    }

                    if (treatmentList.isEmpty()) {
                        treatmentNames.add("לא הוגדרו טיפולים לעסק זה");
                        btnPickDate.setEnabled(false);
                    }

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, treatmentNames);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTreatments.setAdapter(spinnerAdapter);

                    spinnerTreatments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (!treatmentList.isEmpty()) {
                                selectedTreatment = treatmentList.get(position);
                                selectedTime = "";
                                btnConfirmBooking.setEnabled(false);

                                if (selectedCalendar != null) {
                                    loadRealTimeSlots(selectedCalendar);
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            selectedTreatment = null;
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת טיפולים", Toast.LENGTH_SHORT).show());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year1, month1, dayOfMonth);

                    // התיקון הקריטי: פורמט DD/MM/YYYY עם אפסים מובילים
                    selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month1 + 1, year1);

                    tvSelectedDate.setText("תאריך נבחר: " + selectedDate);
                    selectedTime = "";
                    btnConfirmBooking.setEnabled(false);
                    loadRealTimeSlots(selectedCalendar);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
    private void loadRealTimeSlots(Calendar selectedDateCal) {
        if (selectedTreatment == null) return;

        timeSlotsList.clear();
        adapter.notifyDataSetChanged();
        tvNoSlots.setText("בודק זמינות...");
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);

        String dayOfWeekKey = String.valueOf(selectedDateCal.get(Calendar.DAY_OF_WEEK) - 1);

        db.collection("businesses").document(currentBusinessId).get()
                .addOnSuccessListener(businessDoc -> {
                    if (!businessDoc.exists()) {
                        showNoSlots("העסק לא נמצא במערכת");
                        return;
                    }

                    int appointmentDuration = selectedTreatment.getDurationMinutes();
                    Object scheduleObj = businessDoc.get("weeklySchedule");
                    Map<String, Object> dayData = null;

                    // טיפול חכם ב-Firestore שהופך לעיתים מפות למערכים
                    if (scheduleObj instanceof Map) {
                        Map<String, Object> weeklyMap = (Map<String, Object>) scheduleObj;
                        if (weeklyMap.containsKey(dayOfWeekKey)) {
                            dayData = (Map<String, Object>) weeklyMap.get(dayOfWeekKey);
                        } else {
                            // ניסיון לחפש לפי שם היום בעברית
                            String[] dayNamesHebrew = {"יום ראשון", "יום שני", "יום שלישי", "יום רביעי", "יום חמישי", "יום שישי", "יום שבת"};
                            int dayIndex = Integer.parseInt(dayOfWeekKey);
                            if (dayIndex >= 0 && dayIndex < dayNamesHebrew.length) {
                                String dayNameKey = dayNamesHebrew[dayIndex];
                                if (weeklyMap.containsKey(dayNameKey)) {
                                    dayData = (Map<String, Object>) weeklyMap.get(dayNameKey);
                                }
                            }
                        }
                    } else if (scheduleObj instanceof List) {
                        List<Object> weeklyList = (List<Object>) scheduleObj;
                        int dayIndex = Integer.parseInt(dayOfWeekKey);
                        if (dayIndex >= 0 && dayIndex < weeklyList.size()) {
                            Object item = weeklyList.get(dayIndex);
                            if (item instanceof Map) {
                                dayData = (Map<String, Object>) item;
                            }
                        }
                    }

                    if (dayData != null) {
                        boolean isOpenBool = false;
                        Object isOpenObj = dayData.get("isOpen");

                        // המרה בטוחה לבוליאני
                        if (isOpenObj instanceof Boolean) {
                            isOpenBool = (Boolean) isOpenObj;
                        } else if (isOpenObj instanceof String) {
                            isOpenBool = Boolean.parseBoolean((String) isOpenObj);
                        }

                        if (isOpenBool) {
                            String startTime = (String) dayData.get("start");
                            String endTime = (String) dayData.get("end");
                            if (startTime != null && endTime != null) {
                                fetchBookedSlotsAndGenerate(startTime, endTime, appointmentDuration);
                            } else {
                                showNoSlots("חסרות שעות פתיחה ליום זה");
                            }
                        } else {
                            showNoSlots("העסק סגור ביום זה");
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
                    List<int[]> bookedRanges = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String status = doc.getString("status");
                        if (status != null && !status.equals("REJECTED")) {
                            String time = doc.getString("time");
                            Long durationLong = doc.getLong("duration");

                            int bookedDuration = (durationLong != null) ? durationLong.intValue() : 30;

                            if (time != null) {
                                int bookedStart = convertTimeToMinutes(time);
                                int bookedEnd = bookedStart + bookedDuration;
                                bookedRanges.add(new int[]{bookedStart, bookedEnd});
                            }
                        }
                    }
                    generateSlots(start, end, duration, bookedRanges);
                })
                .addOnFailureListener(e -> showNoSlots("שגיאה בבדיקת תורים תפוסים"));
    }

    private void generateSlots(String start, String end, int durationMinutes, List<int[]> bookedRanges) {
        timeSlotsList.clear();
        int startMins = convertTimeToMinutes(start);
        int endMins = convertTimeToMinutes(end);

        int intervalMinutes = 30;

        while (startMins + durationMinutes <= endMins) {
            int proposedStart = startMins;
            int proposedEnd = startMins + durationMinutes;
            boolean isOverlapping = false;

            for (int[] range : bookedRanges) {
                int bookedStart = range[0];
                int bookedEnd = range[1];

                if (proposedStart < bookedEnd && proposedEnd > bookedStart) {
                    isOverlapping = true;
                    break;
                }
            }

            if (!isOverlapping) {
                String timeString = convertMinutesToTime(startMins);
                timeSlotsList.add(timeString);
            }

            startMins += intervalMinutes;
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
        if (auth.getCurrentUser() == null || selectedTreatment == null) {
            Toast.makeText(this, "חסרים נתונים", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmBooking.setEnabled(false);
        String userId = auth.getCurrentUser().getUid();

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
        String description = selectedTreatment.getName() + " (₪" + selectedTreatment.getPrice() + ")";

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
        data.put("description", description);
        data.put("duration", selectedTreatment.getDurationMinutes());

        db.collection("appointments").document(appointmentId).set(data)
                .addOnSuccessListener(aVoid -> {

                    // --- תוספת ההתראות שלנו: שליחה לבעל העסק ---
                    db.collection("businesses").document(currentBusinessId).get()
                            .addOnSuccessListener(doc -> {
                                String ownerId = doc.getString("ownerId");
                                if (ownerId != null) {
                                    String msg = "נקבע תור חדש ל-" + selectedDate + " בשעה " + selectedTime;
                                    PushNotificationHelper.sendNotification(ownerId, "תור חדש ממתין לאישור!", msg);
                                }
                            });
                    // ----------------------------------------

                    // הפעלת ההתראות המקומיות שלנו כאן!
                    NotificationHelper.scheduleAppointmentNotifications(
                            BookingActivity.this,
                            appointmentId,
                            selectedDate,
                            selectedTime,
                            currentBusinessName
                    );

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