package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
        // וודאי ששם הקובץ fragment_appointments תואם לקובץ ה-XML ששלחת לי
        View view = inflater.inflate(R.layout.fragment_appointments, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // התיקון: השם שונה מ-rvMyAppointments ל-rvAppointments כדי להתאים ל-XML
        rvMyAppointments = view.findViewById(R.id.rvAppointments);

        // בדיקת בטיחות: אם ה-RecyclerView לא נמצא, נדפיס לוג ולא נקרוס
        if (rvMyAppointments != null) {
            if (getContext() != null) {
                rvMyAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
            }

            list = new ArrayList<>();
            adapter = new UserAppointmentsAdapter(list);
            rvMyAppointments.setAdapter(adapter);
        } else {
            // אם את רואה את זה בלוג, סימן שה-ID ב-XML עדיין לא תואם
            android.util.Log.e("AppointmentsFragment", "Error: RecyclerView rvAppointments not found in XML!");
        }

        loadUserAppointments();

        return view;
    }

    private void loadUserAppointments() {
        if (auth.getCurrentUser() == null) return;

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (getContext() == null) return;

                    if (error != null) {
                        // Toast.makeText(getContext(), "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    list.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId()); // חובה לשמור ID בשביל המחיקה!
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
            // כאן אנחנו טוענים את ה-XML החדש שיצרנו למעלה
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_appointment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment app = appointments.get(position);

            holder.tvDateTime.setText(app.getDate() + " | " + app.getTime());

            // --- שם העסק ---
            if (app.getBusinessName() != null && !app.getBusinessName().isEmpty()) {
                holder.tvBusinessName.setText(app.getBusinessName());
            } else {
                holder.tvBusinessName.setText("טוען...");
                if (app.getBusinessId() != null) {
                    db.collection("businesses").document(app.getBusinessId()).get()
                            .addOnSuccessListener(ds -> {
                                if (ds.exists()) {
                                    String name = ds.getString("businessName");
                                    holder.tvBusinessName.setText(name);
                                    app.setBusinessName(name);
                                }
                            });
                }
            }

            // --- סטטוס ---
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
                    holder.tvStatus.setText("❌ נדחה");
                    holder.tvStatus.setTextColor(Color.RED);
                    break;
                default:
                    holder.tvStatus.setText(status);
                    holder.tvStatus.setTextColor(Color.GRAY);
            }

            // --- לוגיקה של כפתור המחיקה ---
            holder.btnDelete.setOnClickListener(v -> {
                // הצגת דיאלוג "האם אתה בטוח?"
                new AlertDialog.Builder(getContext())
                        .setTitle("ביטול תור")
                        .setMessage("האם אתה בטוח שברצונך למחוק את התור הזה?")
                        .setPositiveButton("כן, מחק", (dialog, which) -> {
                            deleteAppointment(app.getAppointmentId());
                        })
                        .setNegativeButton("ביטול", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() { return appointments.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvBusinessName, tvDateTime, tvStatus;
            ImageView btnDelete; // הוספנו את המשתנה הזה

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnDelete = itemView.findViewById(R.id.btnDelete); // חיבור ל-XML
            }
        }
    }

    // פונקציה למחיקת התור מ-Firestore
    private void deleteAppointment(String docId) {
        if (docId == null) return;

        db.collection("appointments").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "התור נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                    // הרשימה תתעדכן אוטומטית בגלל ה-SnapshotListener ב-loadUserAppointments
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}