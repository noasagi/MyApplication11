package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerHistoryActivity extends AppCompatActivity {

    private RecyclerView rvCustomerHistory;
    private TextView tvNoHistory;
    private HistoryAdapter adapter;
    private List<Appointment> historyList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי הממשק, מגדירה את מנהל הפריסה (LayoutManager) ל-RecyclerView, ומזמנת את טעינת הנתונים.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_history);

        rvCustomerHistory = findViewById(R.id.rvCustomerHistory);
        tvNoHistory = findViewById(R.id.tvNoHistory);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvCustomerHistory.setLayoutManager(new LinearLayoutManager(CustomerHistoryActivity.this));

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        rvCustomerHistory.setAdapter(adapter);

        loadHistory();
    }

    /**
     * מה הפעולה עושה: שולפת את תורי המשתמש מ-Firestore, משווה את מועדם מול הזמן הנוכחי במילישניות, ומסננת לרשימה רק תורים מהעבר (הושלמו) או כאלו שבוטלו.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadHistory() {
        if (auth.getCurrentUser() == null) return;

        // קבלת הזמן הנוכחי במכשיר לצורך השוואה כרונולוגית
        final long currentTime = System.currentTimeMillis();

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        historyList.clear(); // ניקוי הרשימה המקומית למניעת כפילויות תצוגה

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId());

                            long appointmentTimeInMillis = 0;
                            try {
                                // המרת מחרוזות התאריך והשעה לאובייקט זמן אחיד לצורך חישוב
                                String dateTimeStr = app.getDate() + " " + app.getTime();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                Date date = sdf.parse(dateTimeStr);
                                if (date != null) {
                                    appointmentTimeInMillis = date.getTime();
                                }
                            } catch (Exception e) {
                                appointmentTimeInMillis = app.getTimestamp(); // הגנת קריסה: שימוש בחותם ברירת מחדל
                            }

                            boolean isPast = appointmentTimeInMillis < currentTime;
                            boolean isRejected = "REJECTED".equals(app.getStatus());

                            // סינון היסטוריה: התור נכנס רק אם מועדו כבר עבר או שהוא סומן כמבוטל/נדחה
                            if (isPast || isRejected) {
                                historyList.add(app);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        // עדכון ויזואלי של הממשק במידה והרשימה ריקה
                        if (historyList.isEmpty()) {
                            tvNoHistory.setVisibility(View.VISIBLE);
                            rvCustomerHistory.setVisibility(View.GONE);
                        } else {
                            tvNoHistory.setVisibility(View.GONE);
                            rvCustomerHistory.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    // --- אדפטר פנימי (Adapter) לניהול רשימת התורים הממוחזרת ---
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<Appointment> list;

        public HistoryAdapter(List<Appointment> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            final Appointment app = list.get(position);

            holder.tvHistoryDateTime.setText(app.getDate() + " | " + app.getTime());

            // טעינת שם העסק: מניעת כפילויות קריאה על ידי שמירה מקומית במודל (Denormalization)
            if (app.getBusinessName() != null && !app.getBusinessName().isEmpty()) {
                holder.tvHistoryBusinessName.setText(app.getBusinessName());
            } else if (app.getBusinessId() != null) {
                holder.tvHistoryBusinessName.setText("טוען...");
                db.collection("businesses").document(app.getBusinessId()).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot ds) {
                                if (ds.exists()) {
                                    String name = ds.getString("name");
                                    if (name != null) {
                                        holder.tvHistoryBusinessName.setText(name);
                                        app.setBusinessName(name); // שמירה במודל כדי למנוע שליפה חוזרת בגלילה
                                    }
                                }
                            }
                        });
            } else {
                holder.tvHistoryBusinessName.setText("עסק לא ידוע");
            }

            // צביעה ועדכון חזותי של כותרת הסטטוס בשורה
            String status = app.getStatus();
            if ("REJECTED".equals(status)) {
                holder.tvHistoryStatus.setText("❌ בוטל / נדחה");
                holder.tvHistoryStatus.setTextColor(android.graphics.Color.RED);
            } else {
                holder.tvHistoryStatus.setText("✅ הושלם בהצלחה");
                holder.tvHistoryStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            }

            // כפתור פעולה: מעבר מהיר למסך זימון תור חדש עבור אותו בית עסק
            holder.btnHistoryBookAgain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CustomerHistoryActivity.this, BookingActivity.class);
                    intent.putExtra("businessId", app.getBusinessId());
                    intent.putExtra("businessName", app.getBusinessName());
                    CustomerHistoryActivity.this.startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        // --- מחזיק רכיבים (ViewHolder) לשורת היסטוריה בודדת ---
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvHistoryBusinessName, tvHistoryDateTime, tvHistoryStatus;
            Button btnHistoryBookAgain;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvHistoryBusinessName = itemView.findViewById(R.id.tvHistoryBusinessName);
                tvHistoryDateTime = itemView.findViewById(R.id.tvHistoryDateTime);
                tvHistoryStatus = itemView.findViewById(R.id.tvHistoryStatus);
                btnHistoryBookAgain = itemView.findViewById(R.id.btnHistoryBookAgain);
            }
        }
    }
}