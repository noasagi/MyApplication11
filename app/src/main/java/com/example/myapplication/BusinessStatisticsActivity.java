package com.example.myapplication;

import android.os.Bundle;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BusinessStatisticsActivity extends AppCompatActivity {

    private TextView tvTotalOrders, tvAvgRating, tvReturningCustomers, tvPeakHour, tvPopularService, tvTotalIncome;
    private RatingBar rbStatsProfessionalism, rbStatsReliability, rbStatsPrice;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי הממשק, יוצרת מופעים של רכיבי הפיירבייס ומזמנת את חילוץ הנתונים.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_statistics);

        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvAvgRating = findViewById(R.id.tvAvgRating);
        tvReturningCustomers = findViewById(R.id.tvReturningCustomers);
        tvPeakHour = findViewById(R.id.tvPeakHour);
        tvPopularService = findViewById(R.id.tvPopularService);

        rbStatsProfessionalism = findViewById(R.id.rbStatsProfessionalism);
        rbStatsReliability = findViewById(R.id.rbStatsReliability);
        rbStatsPrice = findViewById(R.id.rbStatsPrice);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        fetchBusinessIdAndLoadStats();
    }

    /**
     * מה הפעולה עושה: מוצאת את מסמך העסק באוסף הראשי, מחלצת את דירוגי הביקורות הממוצעים שלו, מציגה אותם ב-RatingBars וממשיכה לחישוב נתוני התורים.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void fetchBusinessIdAndLoadStats() {
        if (auth.getCurrentUser() == null) return;

        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            businessId = doc.getId();

                            BusinessModel business = doc.toObject(BusinessModel.class);
                            if (business != null) {
                                // הצגת דירוגים ממוצעים: מבוצע רק אם קיימות ביקורות בפועל (מניעת חלוקה באפס)
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

                            // מעבר לשלב ב': הרצת אלגוריתם הניתוח על אוסף התורים של העסק הנוכחי
                            calculateAppointmentsStats();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(BusinessStatisticsActivity.this, "שגיאה בטעינת הנתונים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * מה הפעולה עושה: שולפת את כל התורים של העסק, מפלחת אותם באמצעות מפות (Maps) ומחשבת פדיון כספי, שעת שיא, שירות פופולרי וכמות לקוחות חוזרים.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void calculateAppointmentsStats() {
        if (businessId == null) return;

        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        int totalValidOrders = 0;
                        double totalIncome = 0;

                        // שימוש במפות (Maps) לצורך ספירת שכיחויות וחילול אלגוריתם "השכיח ביותר" (מפתח -> כמות מופעים)
                        Map<String, Integer> hoursMap = new HashMap<>();
                        Map<String, Integer> servicesMap = new HashMap<>();
                        Map<String, Integer> usersMap = new HashMap<>();

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String status = doc.getString("status");

                            // סינון ראשוני: התעלמות מוחלטת מתורים שנדחו או מחלונות זמן שנחסמו
                            if ("REJECTED".equals(status) || "BLOCKED".equals(status)) continue;

                            totalValidOrders++;

                            String date = doc.getString("date");
                            String time = doc.getString("time");
                            String desc = doc.getString("description");
                            String userId = doc.getString("userId");

                            // שקלול הכנסות: מחושב אך ורק על תורים מאושרים (APPROVED) שמועדם כבר עבר כרונולוגית
                            if ("APPROVED".equals(status) && isAppointmentPassed(date, time)) {
                                Double priceVal = doc.getDouble("price");

                                if (priceVal != null) {
                                    totalIncome += priceVal;
                                }
                                // לוגיקת גיבוי: חילוץ מחיר טקסטואלי מתוך סוגריים בתיאור הטיפול במידה ושדה ה-price חסר
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

                            // פילוח שעות: עיגול השעה (למשל: "16:30" הופך ל-"16:00") ועדכון מונה המופעים במפה
                            if (time != null && time.contains(":")) {
                                String hour = time.split(":")[0] + ":00";
                                hoursMap.put(hour, hoursMap.getOrDefault(hour, 0) + 1);
                            }

                            // פילוח שירותים: ניקוי סוגרי המחיר משם הטיפול ועדכון שכיחותו
                            if (desc != null && !desc.isEmpty()) {
                                if (desc.contains(" (")) {
                                    String serviceName = desc.split(" \\(")[0];
                                    servicesMap.put(serviceName, servicesMap.getOrDefault(serviceName, 0) + 1);
                                } else {
                                    servicesMap.put(desc, servicesMap.getOrDefault(desc, 0) + 1);
                                }
                            }

                            // פילוח לקוחות: רישום מספר הביקורים של כל משתמש לצורך זיהוי לקוחות חוזרים
                            if (userId != null && !userId.trim().isEmpty()) {
                                usersMap.put(userId, usersMap.getOrDefault(userId, 0) + 1);
                            }
                        }

                        // עדכון רכיבי התצוגה הכלליים על גבי המסך
                        tvTotalIncome.setText("₪" + String.format(Locale.getDefault(), "%,.0f", totalIncome));
                        tvTotalOrders.setText(String.valueOf(totalValidOrders));

                        // אלגוריתם למציאת שעת השיא (הערך הגבוה ביותר במפת השעות)
                        String peakHour = "אין נתונים";
                        int maxHourCount = 0;
                        for (Map.Entry<String, Integer> entry : hoursMap.entrySet()) {
                            if (entry.getValue() > maxHourCount) {
                                maxHourCount = entry.getValue();
                                peakHour = entry.getKey();
                            }
                        }
                        tvPeakHour.setText(peakHour);

                        // אלגוריתם למציאת הטיפול הנפוץ ביותר (הערך הגבוה ביותר במפת השירותים)
                        String popularService = "אין נתונים";
                        int maxServiceCount = 0;
                        for (Map.Entry<String, Integer> entry : servicesMap.entrySet()) {
                            if (entry.getValue() > maxServiceCount) {
                                maxServiceCount = entry.getValue();
                                popularService = entry.getKey();
                            }
                        }
                        tvPopularService.setText(popularService);

                        // חישוב לקוחות חוזרים: מעבר על ערכי המפה וספירת המשתמשים שביקרו יותר מפעם אחת (count > 1)
                        int returningCustomers = 0;
                        for (int count : usersMap.values()) {
                            if (count > 1) {
                                returningCustomers++;
                            }
                        }
                        tvReturningCustomers.setText(String.valueOf(returningCustomers));
                    }
                });
    }

    /**
     * מה הפעולה עושה: משווה בצורה מלאה את תאריך ושעת התור מול רגע הזמן הנוכחי במכשיר כדי לקבוע האם הוא שייך לעבר.
     * קלט: String dateStr, String timeStr.
     * פלט: boolean (אמת אם התור כבר עבר והסתיים, שקר אחרת).
     */
    private boolean isAppointmentPassed(String dateStr, String timeStr) {
        if (dateStr == null || timeStr == null || dateStr.isEmpty() || timeStr.isEmpty()) return false;

        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());
        try {
            Date appDateTime = sdf.parse(dateStr + " " + timeStr);
            if (appDateTime != null) {
                return appDateTime.before(new Date());
            }
        } catch (ParseException e) {
            return false;
        }
        return false;
    }
}