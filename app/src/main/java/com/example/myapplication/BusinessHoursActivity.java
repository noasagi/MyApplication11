package com.example.myapplication;

import android.app.TimePickerDialog;
import android.os.Bundle;
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

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BusinessHoursActivity extends AppCompatActivity {

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

        businessId = getIntent().getStringExtra("BUSINESS_ID");
        etDuration = findViewById(R.id.etDuration);
        btnSaveAll = findViewById(R.id.btnSaveAll);
        rvDays = findViewById(R.id.rvDays);

        // אתחול רשימת הימים
        initDaysList();

        // הגדרת ה-RecyclerView
        rvDays.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DaysAdapter(daysList, this);
        rvDays.setAdapter(adapter);

        btnSaveAll.setOnClickListener(v -> saveToFirebase());
    }

    private void initDaysList() {
        daysList = new ArrayList<>();
        String[] dayNames = {"יום ראשון", "יום שני", "יום שלישי", "יום רביעי", "יום חמישי", "יום שישי"};

        for (String name : dayNames) {
            // ברירת מחדל: 09:00 עד 17:00, יום פתוח
            daysList.add(new DaySchedule(name, "09:00", "17:00", true));
        }
    }

    private void saveToFirebase() {
        String durationStr = etDuration.getText().toString();
        if (durationStr.isEmpty()) {
            Toast.makeText(this, "נא להזין משך תור", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration = Integer.parseInt(durationStr);

        // הכנת המידע לשמירה
        Map<String, Object> dataToSave = new HashMap<>();
        dataToSave.put("appointmentDuration", duration); // שמירת משך התור

        Map<String, Object> weeklySchedule = new HashMap<>();
        for (DaySchedule day : daysList) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("start", day.startTime);
            dayData.put("end", day.endTime);
            dayData.put("isOpen", day.isOpen);

            // המפתח הוא שם היום
            weeklySchedule.put(day.dayName, dayData);
        }

        dataToSave.put("weeklySchedule", weeklySchedule);

        FirebaseFirestore.getInstance()
                .collection("businesses")
                .document(businessId)
                .update(dataToSave)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "השעות נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show());
    }

    // --- מחלקות עזר (מודל ואדפטר) ---

    // מודל לייצוג יום אחד
    public static class DaySchedule {
        String dayName;
        String startTime;
        String endTime;
        boolean isOpen;

        public DaySchedule(String dayName, String startTime, String endTime, boolean isOpen) {
            this.dayName = dayName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isOpen = isOpen;
        }
    }

    // אדפטר לרשימה
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
            holder.switchIsOpen.setChecked(day.isOpen);

            // הסתרה/הצגה של השעות לפי המתג
            updateVisibility(holder, day.isOpen);

            holder.switchIsOpen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                day.isOpen = isChecked;
                updateVisibility(holder, isChecked);
            });

            holder.btnStartTime.setOnClickListener(v -> {
                showTimePicker(holder.btnStartTime, day, true);
            });

            holder.btnEndTime.setOnClickListener(v -> {
                showTimePicker(holder.btnEndTime, day, false);
            });
        }

        private void updateVisibility(DayViewHolder holder, boolean isOpen) {
            if (isOpen) {
                holder.btnStartTime.setVisibility(View.VISIBLE);
                holder.btnEndTime.setVisibility(View.VISIBLE);
                holder.switchIsOpen.setText("פתוח");
            } else {
                holder.btnStartTime.setVisibility(View.INVISIBLE);
                holder.btnEndTime.setVisibility(View.INVISIBLE);
                holder.switchIsOpen.setText("סגור");
            }
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
        public int getItemCount() {
            return list.size();
        }

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