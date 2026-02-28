package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BusinessStatisticsFragment extends Fragment {

    private TextView tvTotalOrders, tvAvgRating, tvReturningCustomers, tvPeakHour, tvPopularService;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_statistics, container, false);

        tvTotalOrders = view.findViewById(R.id.tvTotalOrders);
        tvAvgRating = view.findViewById(R.id.tvAvgRating);
        tvReturningCustomers = view.findViewById(R.id.tvReturningCustomers);
        tvPeakHour = view.findViewById(R.id.tvPeakHour);
        tvPopularService = view.findViewById(R.id.tvPopularService);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        fetchBusinessIdAndLoadStats();

        return view;
    }

    private void fetchBusinessIdAndLoadStats() {
        if (auth.getCurrentUser() == null) return;

        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        businessId = queryDocumentSnapshots.getDocuments().get(0).getString("businessId");
                        if (businessId == null) {
                            businessId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        }

                        // קריאה לפונקציות החישוב
                        calculateAppointmentsStats();
                        calculateAverageRating();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "שגיאה בטעינת הנתונים", Toast.LENGTH_SHORT).show();
                });
    }

    private void calculateAppointmentsStats() {
        if (businessId == null) return;

        // משיכת כל התורים של העסק (לא כולל תורים שנדחו)
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalValidOrders = 0;

                    // מפות (Dictionaries) כדי לספור תדירויות
                    Map<String, Integer> hoursMap = new HashMap<>();
                    Map<String, Integer> servicesMap = new HashMap<>();
                    Map<String, Integer> usersMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if ("REJECTED".equals(status)) continue; // מתעלמים מתורים מבוטלים

                        totalValidOrders++;

                        String time = doc.getString("time");
                        String desc = doc.getString("description");
                        String userId = doc.getString("userId");

                        // ספירת שעות (למשל מ-14:30 לוקחים רק "14:00")
                        if (time != null && time.contains(":")) {
                            String hour = time.split(":")[0] + ":00";
                            hoursMap.put(hour, hoursMap.getOrDefault(hour, 0) + 1);
                        }

                        // ספירת שירותים (מנקים את המחיר מהמחרוזת למשל מ-"תספורת (₪50)")
                        if (desc != null && !desc.isEmpty()) {
                            String serviceName = desc.split(" \\(")[0]; // חותך הכל לפני סוגריים המחיר
                            servicesMap.put(serviceName, servicesMap.getOrDefault(serviceName, 0) + 1);
                        }

                        // ספירת משתמשים (לקוחות)
                        if (userId != null) {
                            usersMap.put(userId, usersMap.getOrDefault(userId, 0) + 1);
                        }
                    }

                    // 1. עדכון סך הכל תורים
                    tvTotalOrders.setText(String.valueOf(totalValidOrders));

                    // 2. חישוב שעת שיא (השעה שהופיעה הכי הרבה)
                    String peakHour = "אין נתונים";
                    int maxHourCount = 0;
                    for (Map.Entry<String, Integer> entry : hoursMap.entrySet()) {
                        if (entry.getValue() > maxHourCount) {
                            maxHourCount = entry.getValue();
                            peakHour = entry.getKey();
                        }
                    }
                    tvPeakHour.setText(peakHour);

                    // 3. חישוב שירות פופולרי
                    String popularService = "אין נתונים";
                    int maxServiceCount = 0;
                    for (Map.Entry<String, Integer> entry : servicesMap.entrySet()) {
                        if (entry.getValue() > maxServiceCount) {
                            maxServiceCount = entry.getValue();
                            popularService = entry.getKey();
                        }
                    }
                    tvPopularService.setText(popularService);

                    // 4. חישוב לקוחות חוזרים (כל מי שהזמין יותר מפעם 1)
                    int returningCustomers = 0;
                    for (int count : usersMap.values()) {
                        if (count > 1) {
                            returningCustomers++;
                        }
                    }
                    tvReturningCustomers.setText(String.valueOf(returningCustomers));

                });
    }

    private void calculateAverageRating() {
        if (businessId == null) return;

        // משיכת כל הביקורות על העסק מהקולקציה reviews
        db.collection("reviews")
                .whereEqualTo("businessId", businessId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvAvgRating.setText("טרם דורג");
                        return;
                    }

                    double sum = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // בהנחה שהשדה של הציון נקרא "rating"
                        Double rating = doc.getDouble("rating");
                        if (rating != null) {
                            sum += rating;
                            count++;
                        }
                    }

                    if (count > 0) {
                        double average = sum / count;
                        // עיגול לספרה עשרונית אחת (למשל 4.5)
                        tvAvgRating.setText(String.format(java.util.Locale.getDefault(), "%.1f", average));
                    } else {
                        tvAvgRating.setText("טרם דורג");
                    }
                });
    }
}