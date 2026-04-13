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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private int businessDuration = 30; // ברירת מחדל

    private SlotsAdapter adapter;
    private List<SlotModel> slotsList;

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

        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        businessId = querySnapshot.getDocuments().get(0).getId();
                    } else {
                        Toast.makeText(this, "לא נמצא עסק", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);

                    // תיקון פורמט תאריך לסנכרון מול ה-Database
                    selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);

                    tvSelectedDate.setText("תאריך: " + selectedDate);
                    loadSlotsForDate(selectedCal);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void loadSlotsForDate(Calendar cal) {
        if (businessId == null) return;

        tvStatusMessage.setText("טוען שעות...");
        slotsList.clear();
        adapter.notifyDataSetChanged();

        String dayKey = String.valueOf(cal.get(Calendar.DAY_OF_WEEK) - 1);

        db.collection("businesses").document(businessId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Map<String, Object> schedule = (Map<String, Object>) doc.get("weeklySchedule");
                    if (schedule != null && schedule.containsKey(dayKey)) {
                        Map<String, Object> dayData = (Map<String, Object>) schedule.get(dayKey);

                        Boolean isOpen = (Boolean) dayData.get("isOpen");
                        if (isOpen != null && isOpen) {
                            String start = (String) dayData.get("start");
                            String end = (String) dayData.get("end");

                            Long durationLong = doc.getLong("appointmentDuration");
                            businessDuration = (durationLong != null) ? durationLong.intValue() : 30;

                            fetchExistingAppointments(start, end, businessDuration);
                        } else {
                            tvStatusMessage.setText("העסק סגור ביום זה");
                        }
                    }
                });
    }

    private void fetchExistingAppointments(String start, String end, int duration) {
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Appointment> bookedMap = new HashMap<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Appointment app = doc.toObject(Appointment.class);
                        app.setAppointmentId(doc.getId());
                        if (!"REJECTED".equals(app.getStatus())) {
                            bookedMap.put(app.getTime(), app);
                        }
                    }
                    generateSlotsList(start, end, duration, bookedMap);
                });
    }

    private void generateSlotsList(String start, String end, int duration, Map<String, Appointment> bookedMap) {
        int startMins = convertTimeToMinutes(start);
        int endMins = convertTimeToMinutes(end);

        // שימוש ב- <= כדי לכלול את שעת הסיום
        while (startMins <= endMins) {
            String time = convertMinutesToTime(startMins);
            SlotModel slot = new SlotModel();
            slot.time = time;

            if (bookedMap.containsKey(time)) {
                Appointment app = bookedMap.get(time);
                if ("BLOCKED".equals(app.getStatus())) {
                    slot.status = "BLOCKED";
                    slot.appointmentId = app.getAppointmentId();
                } else {
                    slot.status = "BOOKED";
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

    private void toggleSlotBlock(SlotModel slot) {
        if (slot.status.equals("BOOKED")) return;

        if (slot.status.equals("FREE")) {
            Appointment blockApp = new Appointment();
            blockApp.setBusinessId(businessId);
            blockApp.setDate(selectedDate);
            blockApp.setTime(slot.time);
            blockApp.setDuration(businessDuration); // חשוב כדי שהלקוח יראה את החסימה
            blockApp.setStatus("BLOCKED");
            blockApp.setTimestamp(new Date().getTime());

            db.collection("appointments").add(blockApp)
                    .addOnSuccessListener(docRef -> {
                        slot.status = "BLOCKED";
                        slot.appointmentId = docRef.getId();
                        adapter.notifyDataSetChanged();
                    });
        } else if (slot.status.equals("BLOCKED")) {
            db.collection("appointments").document(slot.appointmentId).delete()
                    .addOnSuccessListener(aVoid -> {
                        slot.status = "FREE";
                        adapter.notifyDataSetChanged();
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

    class SlotModel {
        String time, status, appointmentId;
    }

    class SlotsAdapter extends RecyclerView.Adapter<SlotsAdapter.ViewHolder> {
        List<SlotModel> list;
        public SlotsAdapter(List<SlotModel> list) { this.list = list; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_time_slot, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            SlotModel item = list.get(p);
            h.tvTime.setText(item.time);
            if ("FREE".equals(item.status)) h.cardView.setCardBackgroundColor(Color.WHITE);
            else if ("BLOCKED".equals(item.status)) h.cardView.setCardBackgroundColor(Color.DKGRAY);
            else h.cardView.setCardBackgroundColor(Color.RED);
            h.itemView.setOnClickListener(v -> toggleSlotBlock(item));
        }
        @Override public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime; CardView cardView;
            public ViewHolder(View v) { super(v); tvTime = v.findViewById(R.id.tvTimeSlot); cardView = v.findViewById(R.id.cardSlot); }
        }
    }
}