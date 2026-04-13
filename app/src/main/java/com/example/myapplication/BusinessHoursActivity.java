package com.example.myapplication;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BusinessHoursActivity extends BaseActivity {

    private RecyclerView rvDays;
    private EditText etDuration;
    private Button btnSaveAll;
    private String businessId;
    private DaysAdapter adapter;
    private List<DaySchedule> daysList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_hours);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        businessId = getIntent().getStringExtra("BUSINESS_ID");
        if (businessId == null) businessId = getIntent().getStringExtra("businessId");
        if (businessId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            businessId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (businessId != null) {
            businessId = businessId.trim();
        }

        etDuration = findViewById(R.id.etDuration);
        btnSaveAll = findViewById(R.id.btnSaveAll);
        rvDays = findViewById(R.id.rvDays);

        initDaysList();

        rvDays.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DaysAdapter(daysList, this);
        rvDays.setAdapter(adapter);

        loadFromFirebase();

        btnSaveAll.setOnClickListener(v -> saveToFirebase());
    }

    private void initDaysList() {
        daysList = new ArrayList<>();
        String[] dayNames = {"יום ראשון", "יום שני", "יום שלישי", "יום רביעי", "יום חמישי", "יום שישי", "יום שבת"};
        for (int i = 0; i < dayNames.length; i++) {
            daysList.add(new DaySchedule(i, dayNames[i], "09:00", "17:00", true));
        }
    }

    private void loadFromFirebase() {
        if (businessId == null) return;

        FirebaseFirestore.getInstance()
                .collection("businesses")
                .document(businessId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("appointmentDuration")) {
                            Long duration = documentSnapshot.getLong("appointmentDuration");
                            if (duration != null) etDuration.setText(String.valueOf(duration));
                        }

                        Object scheduleObj = documentSnapshot.get("weeklySchedule");

                        // טיפול במצב שפיירבייס החזיר Map
                        if (scheduleObj instanceof Map) {
                            Map<String, Object> weeklySchedule = (Map<String, Object>) scheduleObj;
                            for (DaySchedule day : daysList) {
                                String key = String.valueOf(day.dayIndex);
                                if (weeklySchedule.containsKey(key)) {
                                    updateDayFromMap(day, (Map<String, Object>) weeklySchedule.get(key));
                                } else if (weeklySchedule.containsKey(day.dayName)) {
                                    updateDayFromMap(day, (Map<String, Object>) weeklySchedule.get(day.dayName));
                                }
                            }
                        }
                        // טיפול במצב שפיירבייס החזיר List
                        else if (scheduleObj instanceof List) {
                            List<Object> weeklySchedule = (List<Object>) scheduleObj;
                            for (int i = 0; i < weeklySchedule.size() && i < daysList.size(); i++) {
                                Object item = weeklySchedule.get(i);
                                if (item instanceof Map) {
                                    updateDayFromMap(daysList.get(i), (Map<String, Object>) item);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void updateDayFromMap(DaySchedule day, Map<String, Object> dayData) {
        if (dayData == null) return;
        if (dayData.containsKey("start") && dayData.get("start") != null) {
            day.startTime = String.valueOf(dayData.get("start"));
        }
        if (dayData.containsKey("end") && dayData.get("end") != null) {
            day.endTime = String.valueOf(dayData.get("end"));
        }
        if (dayData.containsKey("isOpen") && dayData.get("isOpen") != null) {
            Object isOpenObj = dayData.get("isOpen");
            if (isOpenObj instanceof Boolean) {
                day.isOpen = (Boolean) isOpenObj;
            } else if (isOpenObj instanceof String) {
                day.isOpen = Boolean.parseBoolean((String) isOpenObj);
            }
        }
    }

    private void saveToFirebase() {
        if (businessId == null) {
            Toast.makeText(this, "שגיאה: חסר מזהה עסק", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("BusinessHours", "Saving to businessId: " + businessId);

        String durationStr = etDuration.getText().toString();
        if (durationStr.isEmpty()) {
            Toast.makeText(this, "נא להזין משך תור", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> weeklySchedule = new HashMap<>();
        for (DaySchedule day : daysList) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("dayName", day.dayName);
            dayData.put("start", day.startTime);
            dayData.put("end", day.endTime);
            dayData.put("isOpen", day.isOpen);

            weeklySchedule.put(String.valueOf(day.dayIndex), dayData);
        }

        Map<String, Object> dataToSave = new HashMap<>();
        dataToSave.put("appointmentDuration", Integer.parseInt(durationStr));
        dataToSave.put("weeklySchedule", weeklySchedule);

        FirebaseFirestore.getInstance()
                .collection("businesses")
                .document(businessId)
                .set(dataToSave, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "השעות נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public static class DaySchedule {
        int dayIndex;
        String dayName;
        String startTime;
        String endTime;
        boolean isOpen;

        public DaySchedule(int dayIndex, String dayName, String startTime, String endTime, boolean isOpen) {
            this.dayIndex = dayIndex;
            this.dayName = dayName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isOpen = isOpen;
        }
    }

    class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DayViewHolder> {
        private List<DaySchedule> list;
        private AppCompatActivity context;

        public DaysAdapter(List<DaySchedule> list, AppCompatActivity context) {
            this.list = list;
            this.context = context;
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_schedule, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            DaySchedule day = list.get(position);
            holder.tvDayName.setText(day.dayName);
            holder.btnStartTime.setText(day.startTime);
            holder.btnEndTime.setText(day.endTime);

            holder.switchIsOpen.setOnCheckedChangeListener(null);
            holder.switchIsOpen.setChecked(day.isOpen);
            updateVisibility(holder, day.isOpen);

            holder.switchIsOpen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                day.isOpen = isChecked;
                updateVisibility(holder, isChecked);
            });

            holder.btnStartTime.setOnClickListener(v -> showTimePicker(holder.btnStartTime, day, true));
            holder.btnEndTime.setOnClickListener(v -> showTimePicker(holder.btnEndTime, day, false));
        }

        private void updateVisibility(DayViewHolder holder, boolean isOpen) {
            holder.btnStartTime.setVisibility(isOpen ? View.VISIBLE : View.INVISIBLE);
            holder.btnEndTime.setVisibility(isOpen ? View.VISIBLE : View.INVISIBLE);
            holder.switchIsOpen.setText(isOpen ? "פתוח" : "סגור");
        }

        private void showTimePicker(Button btn, DaySchedule day, boolean isStart) {
            TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                    (view, hourOfDay, minute) -> {
                        String timeFormatted = String.format(Locale.getDefault(), "%02d:00", hourOfDay);
                        btn.setText(timeFormatted);
                        if (isStart) day.startTime = timeFormatted;
                        else day.endTime = timeFormatted;
                    }, 12, 0, true);
            timePickerDialog.show();
        }

        @Override
        public int getItemCount() { return list.size(); }

        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView tvDayName;
            Switch switchIsOpen;
            Button btnStartTime, btnEndTime;
            public DayViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDayName = itemView.findViewById(R.id.tvDayName);
                switchIsOpen = itemView.findViewById(R.id.switchIsOpen);
                btnStartTime = itemView.findViewById(R.id.btnStartTime);
                btnEndTime = itemView.findViewById(R.id.btnEndTime);
            }
        }
    }
}