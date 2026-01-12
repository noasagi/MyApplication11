package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BusinessAppointmentsActivity extends AppCompatActivity {

    private RecyclerView rvAppointments;
    private AppointmentsAdapter adapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_appointments);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvAppointments = findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        appointmentList = new ArrayList<>();
        adapter = new AppointmentsAdapter(appointmentList);
        rvAppointments.setAdapter(adapter);

        businessId = getIntent().getStringExtra("BUSINESS_ID");

        if (businessId != null) {
            loadAppointments();
        } else {
            fetchBusinessIdAndLoad();
        }
    }

    private void fetchBusinessIdAndLoad() {
        if (auth.getCurrentUser() == null) return;

        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        businessId = queryDocumentSnapshots.getDocuments().get(0).getString("businessId");
                        loadAppointments();
                    } else {
                        Toast.makeText(this, "לא נמצא עסק מקושר למשתמש זה", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בחיפוש עסק", Toast.LENGTH_SHORT).show());
    }

    private void loadAppointments() {
        if (businessId == null) return;

        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "שגיאה בטעינת תורים: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    appointmentList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId());

                            // סינון: מציגים רק אם הפונקציה מחזירה true
                            if (shouldShowAppointment(app)) {
                                appointmentList.add(app);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // פונקציה שמחליטה האם להציג את התור ברשימה
    private boolean shouldShowAppointment(Appointment app) {
        String status = app.getStatus();
        if (status == null) status = "PENDING";

        // 1. אם התור נדחה - הסתר אותו מיד
        if (status.equals("REJECTED")) {
            return false;
        }

        // 2. אם התור מאושר - הסתר אותו רק אם התאריך עבר
        if (status.equals("APPROVED")) {
            if (isDateInPast(app.getDate())) {
                return false; // התאריך עבר, אל תציג
            }
        }

        // 3. הצג כל דבר אחר (כולל PENDING)
        return true;
    }

    // פונקציית עזר לבדיקה אם תאריך עבר
    private boolean isDateInPast(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;

        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        try {
            Date appointmentDate = sdf.parse(dateStr);
            Date today = new Date();

            // איפוס שעות כדי להשוות רק תאריכים
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            if (appointmentDate != null) {
                cal1.setTime(appointmentDate);
            }
            cal2.setTime(today);

            // השוואה: האם שנה קודמת? או אותה שנה ויום קודם?
            if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return true;

            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR);

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- Adapter ---
    class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.ViewHolder> {
        private List<Appointment> list;

        public AppointmentsAdapter(List<Appointment> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment app = list.get(position);

            holder.tvClientName.setText(app.getUserName());
            holder.tvDateTime.setText(app.getDate() + " | " + app.getTime());

            // תיאור
            String desc = app.getDescription();
            if (desc == null || desc.isEmpty()) desc = "אין הערות";
            holder.tvDescription.setText(desc);

            String status = app.getStatus() != null ? app.getStatus() : "PENDING";

            switch (status) {
                case "PENDING":
                    holder.tvStatus.setText("ממתין לאישור");
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"));
                    holder.btnApprove.setVisibility(View.VISIBLE);
                    holder.btnReject.setVisibility(View.VISIBLE);
                    holder.btnReject.setText("דחה");
                    break;
                case "APPROVED":
                    holder.tvStatus.setText("מאושר");
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                    holder.btnApprove.setVisibility(View.GONE);
                    holder.btnReject.setVisibility(View.VISIBLE);
                    holder.btnReject.setText("בטל תור");
                    break;
            }

            holder.btnApprove.setOnClickListener(v -> updateStatus(app.getAppointmentId(), "APPROVED"));
            holder.btnReject.setOnClickListener(v -> updateStatus(app.getAppointmentId(), "REJECTED"));
        }

        private void updateStatus(String docId, String newStatus) {
            if (docId == null) return;
            db.collection("appointments").document(docId)
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> Toast.makeText(BusinessAppointmentsActivity.this, "סטטוס עודכן", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(BusinessAppointmentsActivity.this, "שגיאה בעדכון", Toast.LENGTH_SHORT).show());
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvClientName, tvDateTime, tvStatus, tvDescription;
            Button btnApprove, btnReject;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClientName = itemView.findViewById(R.id.tvClientName);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                // וודא שה-ID הזה קיים ב-XML שלך:
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}