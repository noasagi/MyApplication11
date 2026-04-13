package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BusinessStatisticsFragment extends Fragment {

    private TextView tvTotalOrders, tvAvgRating, tvReturningCustomers, tvPeakHour, tvPopularService, tvTotalIncome;
    private RatingBar rbStatsProfessionalism, rbStatsReliability, rbStatsPrice;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_statistics, container, false);

        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvTotalOrders = view.findViewById(R.id.tvTotalOrders);
        tvAvgRating = view.findViewById(R.id.tvAvgRating);
        tvReturningCustomers = view.findViewById(R.id.tvReturningCustomers);
        tvPeakHour = view.findViewById(R.id.tvPeakHour);
        tvPopularService = view.findViewById(R.id.tvPopularService);

        rbStatsProfessionalism = view.findViewById(R.id.rbStatsProfessionalism);
        rbStatsReliability = view.findViewById(R.id.rbStatsReliability);
        rbStatsPrice = view.findViewById(R.id.rbStatsPrice);

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
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        businessId = doc.getId();

                        BusinessModel business = doc.toObject(BusinessModel.class);
                        if (business != null) {
                            if (business.getTotalReviews() > 0) {
                                tvAvgRating.setText(String.format(Locale.getDefault(), "%.1f", business.getOverallRating()));
                                rbStatsProfessionalism.setRating(business.getAvgProfessionalism());
                                rbStatsReliability.setRating(business.getAvgReliability());
                                rbStatsPrice.setRating(business.getAvgPrice());
                            } else {
                                tvAvgRating.setText("טרם");
                                rbStatsProfessionalism.setRating(0);
                                rbStatsReliability.setRating(0);
                                rbStatsPrice.setRating(0);
                            }
                        }

                        calculateAppointmentsStats();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "שגיאה בטעינת הנתונים", Toast.LENGTH_SHORT).show();
                });
    }

    private void calculateAppointmentsStats() {
        if (businessId == null) return;

        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalValidOrders = 0;
                    double totalIncome = 0;

                    Map<String, Integer> hoursMap = new HashMap<>();
                    Map<String, Integer> servicesMap = new HashMap<>();
                    Map<String, Integer> usersMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");

                        // מתעלמים מתורים שנדחו או נחסמו
                        if ("REJECTED".equals(status) || "BLOCKED".equals(status)) continue;

                        totalValidOrders++; // סופר את כל התורים התקינים (כולל עתידיים) בשביל הסטטיסטיקה של כמות התורים

                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String desc = doc.getString("description");
                        String userId = doc.getString("userId");

                        // --- חישוב הכנסות: רק לתורים מאושרים שהזמן שלהם עבר ---
                        if ("APPROVED".equals(status) && isAppointmentPassed(date, time)) {
                            Double priceVal = doc.getDouble("price");

                            // עדיפות ראשונה: שליפה ישירה מהשדה price כמו שעשית בעמוד הבית
                            if (priceVal != null) {
                                totalIncome += priceVal;
                            }
                            // עדיפות שנייה (גיבוי): חילוץ מתוך התיאור (למשל: "תספורת (50₪)")
                            else if (desc != null && desc.contains("(") && desc.contains(")")) {
                                int start = desc.lastIndexOf("(");
                                int end = desc.lastIndexOf(")");
                                if (start < end) {
                                    String priceStr = desc.substring(start + 1, end).replaceAll("[^\\d.]", "");
                                    if (!priceStr.isEmpty()) {
                                        try {
                                            totalIncome += Double.parseDouble(priceStr);
                                        } catch (NumberFormatException ignored) {}
                                    }
                                }
                            }
                        }

                        // --- איסוף נתונים לשאר הסטטיסטיקות ---
                        if (time != null && time.contains(":")) {
                            String hour = time.split(":")[0] + ":00";
                            hoursMap.put(hour, hoursMap.getOrDefault(hour, 0) + 1);
                        }

                        if (desc != null && !desc.isEmpty()) {
                            if (desc.contains(" (")) {
                                String serviceName = desc.split(" \\(")[0];
                                servicesMap.put(serviceName, servicesMap.getOrDefault(serviceName, 0) + 1);
                            } else {
                                servicesMap.put(desc, servicesMap.getOrDefault(desc, 0) + 1);
                            }
                        }

                        if (userId != null && !userId.trim().isEmpty()) {
                            usersMap.put(userId, usersMap.getOrDefault(userId, 0) + 1);
                        }
                    }

                    // 1. הצגת הכנסות
                    tvTotalIncome.setText("₪" + String.format(Locale.getDefault(), "%,.0f", totalIncome));

                    // 2. עדכון סך הכל תורים
                    tvTotalOrders.setText(String.valueOf(totalValidOrders));

                    // 3. חישוב שעת שיא
                    String peakHour = "אין נתונים";
                    int maxHourCount = 0;
                    for (Map.Entry<String, Integer> entry : hoursMap.entrySet()) {
                        if (entry.getValue() > maxHourCount) {
                            maxHourCount = entry.getValue();
                            peakHour = entry.getKey();
                        }
                    }
                    tvPeakHour.setText(peakHour);

                    // 4. חישוב שירות פופולרי
                    String popularService = "אין נתונים";
                    int maxServiceCount = 0;
                    for (Map.Entry<String, Integer> entry : servicesMap.entrySet()) {
                        if (entry.getValue() > maxServiceCount) {
                            maxServiceCount = entry.getValue();
                            popularService = entry.getKey();
                        }
                    }
                    tvPopularService.setText(popularService);

                    // 5. חישוב לקוחות חוזרים
                    int returningCustomers = 0;
                    for (int count : usersMap.values()) {
                        if (count > 1) {
                            returningCustomers++;
                        }
                    }
                    tvReturningCustomers.setText(String.valueOf(returningCustomers));

                });
    }

    // פונקציית עזר: בודקת אם התור (תאריך ושעה) כבר נמצא בעבר
    private boolean isAppointmentPassed(String dateStr, String timeStr) {
        if (dateStr == null || timeStr == null || dateStr.isEmpty() || timeStr.isEmpty()) return false;

        // התבנית חייבת להתאים לאיך שאת שומרת ב-DB (למשל: "14/05/2026 10:30")
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());
        try {
            Date appDateTime = sdf.parse(dateStr + " " + timeStr);
            if (appDateTime != null) {
                return appDateTime.before(new Date()); // מחזיר true אם התור בעבר
            }
        } catch (ParseException e) {
            return false;
        }
        return false;
    }
}