package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
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
        View view = inflater.inflate(R.layout.fragment_appointments, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvMyAppointments = view.findViewById(R.id.rvAppointments);

        if (rvMyAppointments != null) {
            if (getContext() != null) {
                rvMyAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
            }

            list = new ArrayList<>();
            adapter = new UserAppointmentsAdapter(list);
            rvMyAppointments.setAdapter(adapter);
        } else {
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
                        return;
                    }

                    list.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Appointment app = doc.toObject(Appointment.class);
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

            // --- לוגיקת הצגת כפתור הדירוג ---
            if ("APPROVED".equals(status) && !app.getIsReviewed()) {
                holder.btnRate.setVisibility(View.VISIBLE);
                holder.btnRate.setOnClickListener(v -> {
                    showAddReviewDialog(app);
                });
            } else {
                holder.btnRate.setVisibility(View.GONE);
            }

            // --- לוגיקה של כפתור המחיקה (העברנו פה את כל האובייקט app) ---
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("ביטול תור")
                        .setMessage("האם אתה בטוח שברצונך למחוק את התור הזה?")
                        .setPositiveButton("כן, מחק", (dialog, which) -> {
                            deleteAppointment(app);
                        })
                        .setNegativeButton("ביטול", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() { return appointments.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvBusinessName, tvDateTime, tvStatus;
            ImageView btnDelete;
            Button btnRate;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                btnRate = itemView.findViewById(R.id.btnRate);
            }
        }
    }

    // --- הפונקציה המעודכנת שמטפלת במחיקה ושולחת התראה לעסק ---
    private void deleteAppointment(Appointment app) {
        if (app == null || app.getAppointmentId() == null) return;

        db.collection("appointments").document(app.getAppointmentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "התור נמחק בהצלחה", Toast.LENGTH_SHORT).show();

                    // --- תוספת ההתראות: שליחה לבעל העסק שהלקוח ביטל ---
                    if (app.getBusinessId() != null) {
                        db.collection("businesses").document(app.getBusinessId()).get()
                                .addOnSuccessListener(doc -> {
                                    String ownerId = doc.getString("ownerId");
                                    if (ownerId != null) {
                                        String title = "לקוח ביטל תור";
                                        String msg = "הלקוח " + app.getUserName() + " ביטל את התור שנקבע ל-" + app.getDate() + " בשעה " + app.getTime();
                                        PushNotificationHelper.sendNotification(ownerId, title, msg);
                                    }
                                });
                    }
                    // ------------------------------------------------
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddReviewDialog(Appointment app) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.activity_dialog_add_review, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RatingBar rbProfessionalism = view.findViewById(R.id.rbProfessionalism);
        RatingBar rbReliability = view.findViewById(R.id.rbReliability);
        RatingBar rbPrice = view.findViewById(R.id.rbPrice);
        EditText etComment = view.findViewById(R.id.etComment);
        Button btnSubmit = view.findViewById(R.id.btnSubmitReview);

        btnSubmit.setOnClickListener(v -> {
            float ratingProf = rbProfessionalism.getRating();
            float ratingRel = rbReliability.getRating();
            float ratingPrice = rbPrice.getRating();
            String comment = etComment.getText().toString().trim();

            if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
                Toast.makeText(getContext(), "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
                return;
            }

            String reviewId = db.collection("reviews").document().getId();

            ReviewModel newReview = new ReviewModel(
                    reviewId,
                    app.getBusinessId(),
                    app.getUserId(),
                    app.getUserName(),
                    comment,
                    app.getAppointmentId(),
                    ratingProf,
                    ratingRel,
                    ratingPrice,
                    com.google.firebase.Timestamp.now()
            );

            db.collection("reviews").document(reviewId).set(newReview)
                    .addOnSuccessListener(aVoid -> {
                        // עדכון התור שהוא כבר דורג
                        db.collection("appointments").document(app.getAppointmentId())
                                .update("isReviewed", true);

                        Toast.makeText(getContext(), "תודה על הדירוג!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}