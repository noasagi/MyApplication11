package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

public class AppointmentsClientFragment extends Fragment {

    private RecyclerView rvMyAppointments;
    private UserAppointmentsAdapter adapter;
    private List<Appointment> list;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // טעינת ה-XML של הפרגמנט
        View view = inflater.inflate(R.layout.fragment_appointments, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvMyAppointments = view.findViewById(R.id.rvMyAppointments);

        // שימוש ב-getContext() בתוך פרגמנט
        if (getContext() != null) {
            rvMyAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        list = new ArrayList<>();
        adapter = new UserAppointmentsAdapter(list);
        rvMyAppointments.setAdapter(adapter);

        loadUserAppointments();

        return view;
    }

    private void loadUserAppointments() {
        if (auth.getCurrentUser() == null) return;

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid()) // רק תורים שלי
                .orderBy("timestamp", Query.Direction.DESCENDING) // הכי חדש למעלה
                .addSnapshotListener((value, error) -> {
                    // בדיקה שהפרגמנט עדיין מחובר למסך כדי למנוע קריסה
                    if (getContext() == null) return;

                    if (error != null) {
                        Toast.makeText(getContext(), "שגיאה בטעינת תורים", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    list.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Appointment app = doc.toObject(Appointment.class);
                            // נשמור גם את ה-ID של המסמך ליתר ביטחון
                            app.setAppointmentId(doc.getId());
                            list.add(app);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // --- האדפטר הפנימי ---
    class UserAppointmentsAdapter extends RecyclerView.Adapter<UserAppointmentsAdapter.ViewHolder> {
        private List<Appointment> appointments;

        public UserAppointmentsAdapter(List<Appointment> appointments) {
            this.appointments = appointments;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
                holder.tvBusinessName.setText(app.getBusinessName());
            } else {
                holder.tvBusinessName.setText("טוען שם עסק...");
                if (app.getBusinessId() != null) {
                    FirebaseFirestore.getInstance()
                            .collection("businesses")
                            .document(app.getBusinessId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String name = documentSnapshot.getString("businessName");
                                    // עדכון השם בתצוגה
                                    holder.tvBusinessName.setText(name);
                                    // אופציונלי: שמירת השם באובייקט כדי שלא נצטרך לטעון שוב בגלילה
                                    app.setBusinessName(name);
                                } else {
                                    holder.tvBusinessName.setText("עסק לא ידוע");
                                }
                            });
                }
            }

            // --- צבעים וסטטוס ---
            String status = app.getStatus() != null ? app.getStatus() : "PENDING";
            switch (status) {
                case "PENDING":
                    holder.tvStatus.setText("ממתין לאישור");
                    holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // כתום
                    break;
                case "APPROVED":
                    holder.tvStatus.setText("✔ התור אושר!");
                    holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // ירוק
                    break;
                case "REJECTED":
                    holder.tvStatus.setText("❌ התור נדחה/בוטל");
                    holder.tvStatus.setTextColor(Color.RED);
                    break;
                default:
                    holder.tvStatus.setText(status);
                    holder.tvStatus.setTextColor(Color.GRAY);
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