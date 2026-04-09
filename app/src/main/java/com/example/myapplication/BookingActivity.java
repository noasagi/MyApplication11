package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
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
                                if (selectedCalendar != null) loadRealTimeSlots(selectedCalendar);
                            }
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) { selectedTreatment = null; }
                    });
                });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    tvSelectedDate.setText("תאריך נבחר: " + selectedDate);
                    selectedTime = "";
                    btnConfirmBooking.setEnabled(false);
                    loadRealTimeSlots(selectedCalendar);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
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
                    if (!businessDoc.exists()) { showNoSlots("העסק לא נמצא"); return; }

                    int appointmentDuration = selectedTreatment.getDurationMinutes();
                    Map<String, Object> weeklySchedule = (Map<String, Object>) businessDoc.get("weeklySchedule");

                    if (weeklySchedule != null && weeklySchedule.containsKey(dayOfWeekKey)) {
                        Map<String, Object> dayData = (Map<String, Object>) weeklySchedule.get(dayOfWeekKey);
                        if (dayData != null && Boolean.TRUE.equals(dayData.get("isOpen"))) {
                            fetchBookedSlotsAndGenerate((String)dayData.get("start"), (String)dayData.get("end"), appointmentDuration);
                        } else { showNoSlots("העסק סגור ביום זה"); }
                    } else { showNoSlots("לא הוגדרו שעות פעילות"); }
                });
    }

    private void fetchBookedSlotsAndGenerate(String start, String end, int duration) {
        db.collection("appointments")
                .whereEqualTo("businessId", currentBusinessId)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<int[]> bookedRanges = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        if (!"REJECTED".equals(doc.getString("status"))) {
                            int bStart = convertTimeToMinutes(doc.getString("time"));
                            Long bDur = doc.getLong("duration");
                            bookedRanges.add(new int[]{bStart, bStart + (bDur != null ? bDur.intValue() : 30)});
                        }
                    }
                    generateSlots(start, end, duration, bookedRanges);
                });
    }

    private void generateSlots(String start, String end, int duration, List<int[]> bookedRanges) {
        timeSlotsList.clear();
        int current = convertTimeToMinutes(start);
        int stop = convertTimeToMinutes(end);
        while (current + duration <= stop) {
            boolean overlap = false;
            for (int[] r : bookedRanges) {
                if (current < r[1] && (current + duration) > r[0]) { overlap = true; break; }
            }
            if (!overlap) timeSlotsList.add(convertMinutesToTime(current));
            current += 30;
        }
        if (timeSlotsList.isEmpty()) showNoSlots("אין תורים פנויים");
        else {
            tvNoSlots.setVisibility(View.GONE);
            rvTimeSlots.setVisibility(View.VISIBLE);
            adapter.updateData(timeSlotsList, duration);
        }
    }

    private void saveAppointmentRequest() {
        if (auth.getCurrentUser() == null || selectedTreatment == null) return;
        btnConfirmBooking.setEnabled(false);
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            finalizeBooking(userId, doc.getString("name") != null ? doc.getString("name") : "לקוח");
        });
    }

    private void finalizeBooking(String userId, String userName) {
        String appointmentId = db.collection("appointments").document().getId();

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
        data.put("description", selectedTreatment.getName());
        data.put("duration", selectedTreatment.getDurationMinutes());

        // השורה הקריטית לעדכון ההכנסה היומית!
        data.put("price", selectedTreatment.getPrice());

        db.collection("appointments").document(appointmentId).set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "התור נשלח לאישור!", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private int convertTimeToMinutes(String t) {
        try { String[] p = t.split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); }
        catch (Exception e) { return 0; }
    }

    private String convertMinutesToTime(int m) {
        return String.format(Locale.getDefault(), "%02d:%02d", m / 60, m % 60);
    }

    private void showNoSlots(String msg) {
        tvNoSlots.setText(msg);
        tvNoSlots.setVisibility(View.VISIBLE);
        rvTimeSlots.setVisibility(View.GONE);
    }

    class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
        private List<String> slots;
        private int duration;
        private int selectedPos = -1;
        public TimeSlotAdapter(List<String> s, int d) { this.slots = s; this.duration = d; }
        public void updateData(List<String> s, int d) { this.slots = s; this.duration = d; this.selectedPos = -1; notifyDataSetChanged(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_time_slot, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            h.tvTime.setText(slots.get(p));
            h.cardView.setCardBackgroundColor(selectedPos == p ? Color.parseColor("#6200EE") : Color.WHITE);
            h.tvTime.setTextColor(selectedPos == p ? Color.WHITE : Color.BLACK);
            h.itemView.setOnClickListener(v -> { selectedPos = h.getAdapterPosition(); selectedTime = slots.get(selectedPos); btnConfirmBooking.setEnabled(true); notifyDataSetChanged(); });
        }
        @Override public int getItemCount() { return slots.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime; CardView cardView;
            public ViewHolder(View v) { super(v); tvTime = v.findViewById(R.id.tvTimeSlot); cardView = v.findViewById(R.id.cardSlot); }
        }
    }
}