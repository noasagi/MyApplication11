package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// מחלקת פרגמנט המנהלת את רשימת התורים של הלקוח, ביטולם ודירוג בית העסק לאחר קבלת השירות
public class AppointmentsClientFragment extends Fragment {

    // רכיבי רשימה ומבני נתונים לאחסון התורים
    private RecyclerView rvMyAppointments;
    private UserAppointmentsAdapter adapter;
    private List<Appointment> list;

    // מופעי הגישה של שירותי פיירבייס
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // טעינת וניפוח קובץ ה-XML של מסך התורים
        View view = inflater.inflate(R.layout.fragment_appointments, container, false);

        // אתחול מופעי הגישה של פיירבייס
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvMyAppointments = view.findViewById(R.id.rvAppointments);

        if (rvMyAppointments != null) {
            if (getContext() != null) {
                // קביעת מנהל פריסה אנכי לרשימה הממוחזרת
                rvMyAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
            }

            list = new ArrayList<>();
            adapter = new UserAppointmentsAdapter(list);
            rvMyAppointments.setAdapter(adapter);
        } else {
            android.util.Log.e("AppointmentsFragment", "Error: RecyclerView rvAppointments not found in XML!");
        }

        // טעינה וסינכרון של התורים השייכים למשתמש
        loadUserAppointments();

        return view;
    }

    // פתיחת ערוץ האזנה רציף לשליפה וסינון דינמי של תורי המשתמש הנוכחי
    private void loadUserAppointments() {
        if (auth.getCurrentUser() == null) return;

        // האזנה לתורים של המשתמש הממוינים לפי חותם זמן מהחדש לישן
        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (getContext() == null || error != null || value == null) return;

                        list.clear();
                        long currentTime = System.currentTimeMillis();

                        // הגדרת חלון זמן חסד של 3 ימים לביצוע דירוג (במילישניות)
                        long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000;

                        for (QueryDocumentSnapshot doc : value) {
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId());

                            long appointmentTimeInMillis = calculateMillis(app);
                            boolean isPast = appointmentTimeInMillis < currentTime;
                            boolean isRejected = "REJECTED".equals(app.getStatus());
                            boolean needsReview = "APPROVED".equals(app.getStatus()) && !app.getIsReviewed();

                            // חישוב האם התור עומד בתוך חלון שלושת הימים שחלפו
                            boolean withinGracePeriod = (currentTime - appointmentTimeInMillis) < threeDaysInMillis;

                            if (!isRejected) {
                                if (!isPast) {
                                    // תור עתידי מאושר או ממתין - תמיד מוצג ברשימה
                                    list.add(app);
                                } else if (needsReview && withinGracePeriod) {
                                    // תור שעבר ולא דורג, אך עדיין בתוך חלון הדירוג המותר
                                    list.add(app);
                                }
                            }
                        }
                        // עדכון האדפטר על שינוי בנתוני הרשימה
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // פונקציית עזר להמרת מחרוזות תאריך ושעה לערך מספרי במילישניות
    private long calculateMillis(Appointment app) {
        try {
            String dateTimeStr = app.getDate() + " " + app.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(dateTimeStr);
            return (date != null) ? date.getTime() : app.getTimestamp();
        } catch (Exception e) {
            return app.getTimestamp();
        }
    }

    // --- מחלקת האדפטר הפנימית לניהול והצגת פריטי הרשימה הממוחזרת ---
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
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            final Appointment app = appointments.get(position);

            holder.tvDateTime.setText(app.getDate() + " | " + app.getTime());

            // ניהול הצגת שם בית העסק (מטמון מקומי או שליפה משלימה מהענן)
            if (app.getBusinessName() != null && !app.getBusinessName().isEmpty()) {
                holder.tvBusinessName.setText(app.getBusinessName());
            } else {
                holder.tvBusinessName.setText("טוען...");
                if (app.getBusinessId() != null) {
                    db.collection("businesses").document(app.getBusinessId()).get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot ds) {
                                    if (ds.exists()) {
                                        String name = ds.getString("businessName");
                                        holder.tvBusinessName.setText(name);
                                        app.setBusinessName(name);
                                    }
                                }
                            });
                }
            }

            // קביעת נראות וצבע רכיב הטקסט בהתאם לסטטוס הנוכחי של התור
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

            // חישוב מועד התור לבחינת רלוונטיות של כפתורי ביטול ודירוג לקוח
            long appointmentTimeInMillis = 0;
            try {
                String dateTimeStr = app.getDate() + " " + app.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date date = sdf.parse(dateTimeStr);
                if (date != null) appointmentTimeInMillis = date.getTime();
            } catch (Exception e) {
                appointmentTimeInMillis = app.getTimestamp();
            }
            boolean isPast = appointmentTimeInMillis < System.currentTimeMillis();

            // הצגת כפתור דירוג אך ורק עבור תורים שאושרו, עבר זמנם וטרם דורגו
            if ("APPROVED".equals(status) && !app.getIsReviewed() && isPast) {
                holder.btnRate.setVisibility(View.VISIBLE);
                holder.btnRate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddReviewDialog(app);
                    }
                });
            } else {
                holder.btnRate.setVisibility(View.GONE);
            }

            // ניהול הצגת כפתור ביטול תור: מסתתר אוטומטית אם מועד התור כבר עבר
            if (isPast) {
                holder.btnDelete.setVisibility(View.GONE);
            } else {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("ביטול תור")
                                .setMessage("האם אתה בטוח שברצונך לבטל את התור הזה?")
                                .setPositiveButton("כן, בטל", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteAppointment(app);
                                    }
                                })
                                .setNegativeButton("לא", null)
                                .show();
                    }
                });
            }
        }

        @Override
        public int getItemCount() { return appointments.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvBusinessName, tvDateTime, tvStatus;
            CardView btnDelete;
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

    // מחיקת מסמך התור מהענן ויצירת כוונת (Intent) לשליחת הודעת ביטול לבעל העסק ב-SMS
    private void deleteAppointment(final Appointment app) {
        if (app == null || app.getAppointmentId() == null) return;

        db.collection("appointments").document(app.getAppointmentId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getContext(), "התור בוטל בהצלחה", Toast.LENGTH_SHORT).show();

                        if (app.getBusinessId() != null) {
                            db.collection("businesses").document(app.getBusinessId()).get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot doc) {
                                            String businessPhone = doc.getString("phone");
                                            String msg = "הלקוח " + app.getUserName() + " ביטל את התור שנקבע ל-" + app.getDate() + " בשעה " + app.getTime();

                                            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                                            smsIntent.setData(Uri.parse("smsto:" + (businessPhone != null ? businessPhone : "")));
                                            smsIntent.putExtra("sms_body", msg);

                                            try {
                                                startActivity(smsIntent);
                                            } catch (Exception e) {
                                                if (isAdded() && getContext() != null) {
                                                    Toast.makeText(getContext(), "לא נמצאה אפליקציית SMS מותקנת במכשיר", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "שגיאה בביטול: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // בניית דיאלוג ביקורת ודירוג רב-קטגוריאלי והפעלת אלגוריתם שקלול ממוצעים דינמי
    private void showAddReviewDialog(final Appointment app) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.activity_dialog_add_review, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final RatingBar rbProfessionalism = view.findViewById(R.id.rbProfessionalism);
        final RatingBar rbReliability = view.findViewById(R.id.rbReliability);
        final RatingBar rbPrice = view.findViewById(R.id.rbPrice);
        final EditText etComment = view.findViewById(R.id.etComment);
        Button btnSubmit = view.findViewById(R.id.btnSubmitReview);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final float ratingProf = rbProfessionalism.getRating();
                final float ratingRel = rbReliability.getRating();
                final float ratingPrice = rbPrice.getRating();
                String comment = etComment.getText().toString().trim();

                if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
                    Toast.makeText(getContext(), "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
                    return;
                }

                final String reviewId = db.collection("reviews").document().getId();

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

                // כתיבת מסמך הביקורת החדש אל תוך בסיס הנתונים בענן
                db.collection("reviews").document(reviewId).set(newReview)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // 1. עדכון מצב התור הנוכחי לנדגם ונבדק על מנת שלא יוצג לדירוג שוב
                                db.collection("appointments").document(app.getAppointmentId())
                                        .update("isReviewed", true);

                                // 2. הפעלת אלגוריתם שקלול ממוצע הדירוגים במסמך העסק בענן
                                if (app.getBusinessId() != null) {
                                    db.collection("businesses").document(app.getBusinessId()).get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot doc) {
                                                    if (doc.exists()) {
                                                        BusinessModel b = doc.toObject(BusinessModel.class);
                                                        if (b != null) {
                                                            int oldTotal = b.getTotalReviews();
                                                            int newTotal = oldTotal + 1;

                                                            // חישוב מתמטי משוקלל המשלב את הממוצע הישן עם הציון החדש
                                                            float newProf = ((b.getAvgProfessionalism() * oldTotal) + ratingProf) / newTotal;
                                                            float newRel = ((b.getAvgReliability() * oldTotal) + ratingRel) / newTotal;
                                                            float newPrice = ((b.getAvgPrice() * oldTotal) + ratingPrice) / newTotal;

                                                            // עדכון ערכי הממוצעים המשוקללים החדשים ומספר המדרגים במסמך העסק
                                                            db.collection("businesses").document(app.getBusinessId())
                                                                    .update(
                                                                            "avgProfessionalism", newProf,
                                                                            "avgReliability", newRel,
                                                                            "avgPrice", newPrice,
                                                                            "totalReviews", newTotal
                                                                    );
                                                        }
                                                    }
                                                }
                                            });
                                }

                                Toast.makeText(getContext(), "תודה על הדירוג!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getContext(), "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        dialog.show();
    }
}