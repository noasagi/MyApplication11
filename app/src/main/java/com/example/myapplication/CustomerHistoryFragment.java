package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerHistoryFragment extends Fragment {

    private RecyclerView rvCustomerHistory;
    private TextView tvNoHistory;
    private HistoryAdapter adapter;
    private List<Appointment> historyList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_history, container, false);

        rvCustomerHistory = view.findViewById(R.id.rvCustomerHistory);
        tvNoHistory = view.findViewById(R.id.tvNoHistory);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getContext() != null) {
            rvCustomerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        rvCustomerHistory.setAdapter(adapter);

        loadHistory();

        return view;
    }

    private void loadHistory() {
        if (auth.getCurrentUser() == null) return;

        long currentTime = System.currentTimeMillis();

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Appointment app = doc.toObject(Appointment.class);
                        app.setAppointmentId(doc.getId());

                        // --- חישוב הזמן האמיתי של התור ---
                        long appointmentTimeInMillis = 0;
                        try {
                            String dateTimeStr = app.getDate() + " " + app.getTime();
                            // שימי לב: בהנחה שהתאריך נשמר בפורמט DD/MM/YYYY (למשל 15/04/2024)
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                            Date date = sdf.parse(dateTimeStr);
                            if (date != null) {
                                appointmentTimeInMillis = date.getTime();
                            }
                        } catch (Exception e) {
                            appointmentTimeInMillis = app.getTimestamp(); // גיבוי למקרה של שגיאה בטקסט
                        }

                        // מסננים: מציגים רק תורים שהזמן שלהם באמת עבר או שנדחו
                        boolean isPast = appointmentTimeInMillis < currentTime;
                        boolean isRejected = "REJECTED".equals(app.getStatus());

                        if (isPast || isRejected) {
                            historyList.add(app);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (historyList.isEmpty()) {
                        tvNoHistory.setVisibility(View.VISIBLE);
                        rvCustomerHistory.setVisibility(View.GONE);
                    } else {
                        tvNoHistory.setVisibility(View.GONE);
                        rvCustomerHistory.setVisibility(View.VISIBLE);
                    }
                });
    }

    // --- אדפטר פנימי ---
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
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment app = list.get(position);

            holder.tvHistoryDateTime.setText(app.getDate() + " | " + app.getTime());

            // טעינת שם העסק
            if (app.getBusinessName() != null && !app.getBusinessName().isEmpty()) {
                holder.tvHistoryBusinessName.setText(app.getBusinessName());
            } else if (app.getBusinessId() != null) {
                holder.tvHistoryBusinessName.setText("טוען...");
                db.collection("businesses").document(app.getBusinessId()).get()
                        .addOnSuccessListener(ds -> {
                            if (ds.exists()) {
                                String name = ds.getString("name");
                                if (name != null) {
                                    holder.tvHistoryBusinessName.setText(name);
                                    app.setBusinessName(name);
                                }
                            }
                        });
            } else {
                holder.tvHistoryBusinessName.setText("עסק לא ידוע");
            }

            // סטטוס התור
            String status = app.getStatus();
            if ("REJECTED".equals(status)) {
                holder.tvHistoryStatus.setText("❌ בוטל / נדחה");
                holder.tvHistoryStatus.setTextColor(android.graphics.Color.RED);
            } else {
                holder.tvHistoryStatus.setText("✅ הושלם בהצלחה");
                holder.tvHistoryStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            }

            // כפתור הזמן שוב - מעביר למסך בחירת שעות
            holder.btnHistoryBookAgain.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), BookingActivity.class);
                intent.putExtra("businessId", app.getBusinessId());
                intent.putExtra("businessName", app.getBusinessName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

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