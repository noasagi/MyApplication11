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
                        // התיקון: לוקחים ישירות את מזהה המסמך האמיתי, בלי להסתמך על שדות פנימיים
                        businessId = queryDocumentSnapshots.getDocuments().get(0).getId();

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

        // משיכת כל התורים של העסק
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

                        // כאן התיקון: דילוג על תורים מבוטלים או חסימות פיקטיביות
                        if ("REJECTED".equals(status) || "BLOCKED".equals(status)) continue;

                        totalValidOrders++;

                        String time = doc.getString("time");
                        String desc = doc.getString("description");
                        String userId = doc.getString("userId");

                        // ספירת שעות (למשל מ-14:30 לוקחים רק "14:00")
                        if (time != null && time.contains(":")) {
                            String hour = time.split(":")[0] + ":00";
                            hoursMap.put(hour, hoursMap.getOrDefault(hour, 0) + 1);
                        }

                        // ספירת שירותים
                        if (desc != null && !desc.isEmpty()) {
                            // הגנה נוספת: בודקים אם יש סוגריים לפני שחותכים
                            if (desc.contains(" (")) {
                                String serviceName = desc.split(" \\(")[0]; // חותך הכל לפני סוגריים המחיר
                                servicesMap.put(serviceName, servicesMap.getOrDefault(serviceName, 0) + 1);
                            } else {
                                servicesMap.put(desc, servicesMap.getOrDefault(desc, 0) + 1);
                            }
                        }

                        // ספירת משתמשים (לקוחות)
                        // חסימות פיקטיביות לרוב אין להן userId, אבל ליתר ביטחון נוודא שהוא לא ריק
                        if (userId != null && !userId.trim().isEmpty()) {
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

                    double sumTotalReviews = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Double prof = doc.getDouble("ratingProfessionalism");
                        Double rel = doc.getDouble("ratingReliability");
                        Double price = doc.getDouble("ratingPrice");

                        // מוודאים שכל הציונים קיימים בביקורת הזו
                        if (prof != null && rel != null && price != null) {
                            // ממוצע של הביקורת הספציפית הזו
                            double reviewAverage = (prof + rel + price) / 3.0;

                            sumTotalReviews += reviewAverage;
                            count++;
                        }
                    }

                    if (count > 0) {
                        // ממוצע של כל הביקורות יחד
                        double finalAverage = sumTotalReviews / count;
                        // עיגול לספרה עשרונית אחת (למשל 4.5)
                        tvAvgRating.setText(String.format(java.util.Locale.getDefault(), "%.1f", finalAverage));
                    } else {
                        tvAvgRating.setText("טרם דורג");
                    }
                })
                .addOnFailureListener(e -> {
                    tvAvgRating.setText("שגיאה");
                });
    }
}