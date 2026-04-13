package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.appcompat.widget.Toolbar;


import java.util.ArrayList;
import java.util.List;

public class MyAppointmentsActivity extends BaseActivity {

    private RecyclerView rvMyAppointments;
    private UserAppointmentsAdapter adapter;
    private List<Appointment> list;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_appointments); // לוודא שיצרת layout למסך הזה

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvMyAppointments = findViewById(R.id.rvMyAppointments);
        rvMyAppointments.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        adapter = new UserAppointmentsAdapter(list);
        rvMyAppointments.setAdapter(adapter);

        loadUserAppointments();
    }

    private void loadUserAppointments() {
        if (auth.getCurrentUser() == null) return;

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid()) // רק תורים שלי
                .orderBy("timestamp", Query.Direction.DESCENDING) // הכי חדש למעלה
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "שגיאה בטעינת תורים", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    list.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            list.add(doc.toObject(Appointment.class));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // --- האדפטר הפנימי (Internal Adapter) ---
    class UserAppointmentsAdapter extends RecyclerView.Adapter<UserAppointmentsAdapter.ViewHolder> {
        private List<Appointment> appointments;

        public UserAppointmentsAdapter(List<Appointment> appointments) {
            this.appointments = appointments;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // משתמשים ב-Layout שיצרנו בשלב 1
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_appointment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment app = appointments.get(position);

            // הגדרת תאריך ושעה
            holder.tvDateTime.setText(app.getDate() + " | " + app.getTime());

            // --- טיפול בשם העסק ---
            if (app.getBusinessName() != null && !app.getBusinessName().isEmpty()) {
                // אופציה א': השם כבר שמור בתוך התור (הכי מהיר)
                holder.tvBusinessName.setText(app.getBusinessName());
            } else {
                // אופציה ב': השם לא שמור (תור ישן), נשלוף אותו לפי ה-ID
                holder.tvBusinessName.setText("טוען שם עסק..."); // טקסט זמני
                FirebaseFirestore.getInstance()
                        .collection("businesses")
                        .document(app.getBusinessId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("businessName"); // וודאי שזה שם השדה ב-businesses
                                holder.tvBusinessName.setText(name);
                            } else {
                                holder.tvBusinessName.setText("עסק לא ידוע");
                            }
                        });
            }

            // --- צבעים וסטטוס (כמו קודם) ---
            String status = app.getStatus() != null ? app.getStatus() : "PENDING";
            switch (status) {
                case "PENDING":
                    holder.tvStatus.setText("ממתין לאישור");
                    holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
                    break;
                case "APPROVED":
                    holder.tvStatus.setText("✔ התור אושר!");
                    holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                    break;
                case "REJECTED":
                    holder.tvStatus.setText("❌ התור נדחה/בוטל");
                    holder.tvStatus.setTextColor(Color.RED);
                    break;
            }
        }

        @Override
        public int getItemCount() { return appointments.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvBusinessName, tvDateTime, tvStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }
        }
    }
}