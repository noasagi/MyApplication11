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

import java.util.ArrayList;
import java.util.List;

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

        // בדיקה: האם קיבלנו ID מהעמוד הקודם?
        businessId = getIntent().getStringExtra("BUSINESS_ID");

        if (businessId != null) {
            // יש ID - טען תורים
            loadAppointments();
        } else {
            // אין ID - ננסה למצוא את העסק של המשתמש הנוכחי
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
                        // מצאנו את העסק!
                        businessId = queryDocumentSnapshots.getDocuments().get(0).getString("businessId");
                        loadAppointments();
                    } else {
                        Toast.makeText(this, "לא נמצא עסק מקושר למשתמש זה", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בחיפוש עסק", Toast.LENGTH_SHORT).show());
    }

    private void loadAppointments() {
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
               .orderBy("timestamp", Query.Direction.DESCENDING) // וודא שיש לך אינדקס ב-Firebase אם זה קורס
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // לפעמים קורס אם אין אינדקס, נסה להסיר את ה-orderBy אם יש בעיה
                        Toast.makeText(this, "שגיאה בטעינת תורים: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    appointmentList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId()); // חשוב לשמור את ה-ID של המסמך לעדכון סטטוס
                            appointmentList.add(app);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
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

            // טיפול בערכי NULL כדי למנוע קריסה
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
                case "REJECTED":
                    holder.tvStatus.setText("נדחה/בוטל");
                    holder.tvStatus.setTextColor(android.graphics.Color.RED);
                    holder.btnApprove.setVisibility(View.GONE);
                    holder.btnReject.setVisibility(View.GONE);
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
            TextView tvClientName, tvDateTime, tvStatus;
            Button btnApprove, btnReject;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClientName = itemView.findViewById(R.id.tvClientName);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}