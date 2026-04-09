package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BusinessHomeFragment extends Fragment {

    private TextView tvWelcome, tvDate, tvTodayCount, tvPendingCount, tvDailyRevenue, tvNextClientName, tvNextClientInfo;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;

    public BusinessHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_home, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvDate = view.findViewById(R.id.tvDate);
        tvTodayCount = view.findViewById(R.id.tvTodayCount);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvDailyRevenue = view.findViewById(R.id.tvDailyRevenue);
        tvNextClientName = view.findViewById(R.id.tvNextClientName);
        tvNextClientInfo = view.findViewById(R.id.tvNextClientInfo);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("he"));
        tvDate.setText(sdf.format(new Date()));

        fetchBusinessIdAndLoadData();
        return view;
    }

    private void fetchBusinessIdAndLoadData() {
        if (auth.getCurrentUser() == null) return;

        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        // שליפה דינמית של ה-ID מה-Firebase
                        businessId = qs.getDocuments().get(0).getString("businessId");

                        // שליפה דינמית של שם העסק
                        String bName = qs.getDocuments().get(0).getString("businessName");
                        if (bName == null) bName = qs.getDocuments().get(0).getString("name");

                        // אם אין שם עסק, נציג את שם המשתמש או טקסט כללי
                        if (bName == null || bName.isEmpty()) {
                            bName = "בעל עסק";
                        }

                        tvWelcome.setText("שלום, " + bName);
                        loadDashboardStats();
                    }
                });
    }

    private void loadDashboardStats() {
        if (businessId == null) return;

        // לפי התמונה שלך, התאריך שמור עם אפסים: 09/04/2026
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String todayStr = sdfDate.format(new Date());

        // האזנה לתורים מאושרים להיום
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("date", todayStr)
                .whereEqualTo("status", "APPROVED")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    tvTodayCount.setText(String.valueOf(snapshots.size()));

                    double totalRevenue = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        // שליפה ישירה של המחיר כדי למנוע בעיות המרה
                        Long p = doc.getLong("price");
                        if (p != null) {
                            totalRevenue += p.doubleValue();
                        } else {
                            // ניסיון שליפה כ-Double למקרה שזה נשמר עם נקודה עשרונית
                            Double d = doc.getDouble("price");
                            if (d != null) totalRevenue += d;
                        }
                    }
                    tvDailyRevenue.setText("₪ " + (long)totalRevenue);
                    updateNextAppointmentFromList(snapshots);
                });

        // האזנה לתורים ממתינים (בלי קשר לתאריך)
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    tvPendingCount.setText(String.valueOf(snapshots.size()));
                });
    }

    private void updateNextAppointmentFromList(QuerySnapshot snapshots) {
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        QueryDocumentSnapshot nextDoc = null;
        String minT = "23:59";

        for (QueryDocumentSnapshot doc : snapshots) {
            String time = doc.getString("time");
            if (time != null && time.compareTo(now) >= 0 && time.compareTo(minT) < 0) {
                minT = time;
                nextDoc = doc;
            }
        }

        if (nextDoc != null) {
            tvNextClientName.setText(nextDoc.getString("userName"));
            tvNextClientInfo.setText("בשעה " + nextDoc.getString("time") + " | " + nextDoc.getString("description"));
        } else {
            tvNextClientName.setText("אין תורים נוספים");
            tvNextClientInfo.setText("סיימת להיום!");
        }
    }
}