package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class BusinessScheduleFragment extends Fragment {

    private RecyclerView rvAppointments;
    private AppointmentsAdapter adapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;

    public BusinessScheduleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_business_schedule, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvAppointments = view.findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        appointmentList = new ArrayList<>();
        adapter = new AppointmentsAdapter(appointmentList);
        rvAppointments.setAdapter(adapter);

        // טעינת העסק והתורים
        fetchBusinessIdAndLoad();

        return view;
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
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "שגיאה בטעינת נתוני עסק", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAppointments() {
        if (businessId == null) return;

        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    appointmentList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId());

                            if (shouldShowAppointment(app)) {
                                appointmentList.add(app);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private boolean shouldShowAppointment(Appointment app) {
        String status = app.getStatus();
        if (status == null) status = "PENDING";

        // אם התור בוטל או שזה חסימה של בעל העסק - אל תציג!
        if (status.equals("REJECTED") || status.equals("BLOCKED")) return false;

        if (status.equals("APPROVED")) {
            if (isDateInPast(app.getDate())) return false;
        }

        return true;
    }
    private boolean isDateInPast(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        try {
            Date appointmentDate = sdf.parse(dateStr);
            Date today = new Date();
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            if (appointmentDate != null) cal1.setTime(appointmentDate);
            cal2.setTime(today);

            if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return true;
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR);
        } catch (ParseException e) {
            return false;
        }
    }

    // --- Adapter Inner Class ---
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

            String desc = (app.getDescription() == null || app.getDescription().isEmpty()) ? "אין הערות" : app.getDescription();
            holder.tvDescription.setText(desc);

            String status = app.getStatus() != null ? app.getStatus() : "PENDING";

            if (status.equals("PENDING")) {
                holder.tvStatus.setText("ממתין לאישור");
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // כתום
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnReject.setText("דחה");
            } else if (status.equals("APPROVED")) {
                holder.tvStatus.setText("מאושר");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // ירוק
                holder.btnApprove.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnReject.setText("בטל תור");
            }

            holder.btnApprove.setOnClickListener(v -> updateStatus(app.getAppointmentId(), "APPROVED"));
            holder.btnReject.setOnClickListener(v -> updateStatus(app.getAppointmentId(), "REJECTED"));
        }

        private void updateStatus(String docId, String newStatus) {
            db.collection("appointments").document(docId).update("status", newStatus)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "סטטוס עודכן", Toast.LENGTH_SHORT).show());
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
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}