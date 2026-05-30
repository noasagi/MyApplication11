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

// הגדרת מחלקת מסך הסטטיסטיקות של העסק היורשת מאקטיביטי
public class BusinessStatisticsActivity extends AppCompatActivity {

    // הצהרה על רכיבי טקסט להצגת הנתונים המספריים והטקסטואליים של הדוחות
    private TextView tvTotalOrders, tvAvgRating, tvReturningCustomers, tvPeakHour, tvPopularService, tvTotalIncome;

    // הצהרה על רכיבי כוכבי דירוג להצגת ממוצעי הביקורות של העסק
    private RatingBar rbStatsProfessionalism, rbStatsReliability, rbStatsPrice;

    // הצהרה על עצם הגישה לבסיס הנתונים פיירסטור של פיירבייס
    private FirebaseFirestore db;

    // הצהרה על עצם הגישה למערכת אימות המשתמשים של פיירבייס
    private FirebaseAuth auth;

    // משתנה מחרוזת לשמירת מזהה העסק הייחודי שנשלוף מהמסד
    private String businessId;

    // פעולת המערכת הראשית שמופעלת אוטומטית בעת יצירת המסך
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת וחיבור קובץ ה-XML של עיצוב מסך הסטטיסטיקות
        setContentView(R.layout.activity_business_statistics);

        // קישור משתני רכיבי הטקסט לרכיבים הויזואליים מתוך קובץ ה-XML
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvAvgRating = findViewById(R.id.tvAvgRating);
        tvReturningCustomers = findViewById(R.id.tvReturningCustomers);
        tvPeakHour = findViewById(R.id.tvPeakHour);
        tvPopularService = findViewById(R.id.tvPopularService);

        // קישור משתני רכיבי כוכבי הדירוג לרכיבים הויזואליים מתוך קובץ ה-XML
        rbStatsProfessionalism = findViewById(R.id.rbStatsProfessionalism);
        rbStatsReliability = findViewById(R.id.rbStatsReliability);
        rbStatsPrice = findViewById(R.id.rbStatsPrice);

        // אתחול וקבלת המופע הנוכחי של בסיס הנתונים פיירסטור לקוד
        db = FirebaseFirestore.getInstance();
        // אתחול וקבלת המופע הנוכחי של מערכת האימות פיירבייס לקוד
        auth = FirebaseAuth.getInstance();

        // קריאה לפעולה הפנימית ששולפת את מזהה העסק וטוענת את הנתונים שלו
        fetchBusinessIdAndLoadStats();
    }

    // פעולה פרטית לשליפת מזהה העסק של המשתמשת המחוברת וטעינת דירוגי הביקורות שלו
    private void fetchBusinessIdAndLoadStats() {
        // בדיקת הגנה לוודא שקיים משתמש מחובר כרגע במערכת האימות
        if (auth.getCurrentUser() == null) return;

        // פנייה לאוסף העסקים לשליפת המסמך שבו שדה מזהה הבעלים שווה ל-UID המחובר
        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    // פעולה המופעלת ברגע ששליפת נתוני העסק הסתיימה בהצלחה מהשרת
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // בדיקה האם נמצא מסמך עסק התואם לתנאי השאילתה במסד
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // חילוץ מסמך העסק הראשון שחזר מהתוצאות
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            // שמירת מזהה המסמך הייחודי של העסק במשתנה הגלובלי
                            businessId = doc.getId();

                            // המרת נתוני המסמך לעצם מובנה מסוג מחלקת מודל העסק
                            BusinessModel business = doc.toObject(BusinessModel.class);
                            // בדיקה שהעצם שפוענח אינו ריק
                            if (business != null) {
                                // תנאי הבודק האם קיימות ביקורות ודירוגים לעסק זה
                                if (business.getTotalReviews() > 0) {
                                    // הצגת הציון הכללי בטקסט עם דיוק של ספרה אחת לאחר הנקודה
                                    tvAvgRating.setText(String.format(Locale.getDefault(), "%.1f", business.getOverallRating()));
                                    // השמת כוכבי הדירוג עבור מדד המקצועיות מתוך נתוני המודל
                                    rbStatsProfessionalism.setRating(business.getAvgProfessionalism());
                                    // השמת כוכבי הדירוג עבור מדד האמינות מתוך נתוני המודל
                                    rbStatsReliability.setRating(business.getAvgReliability());
                                    // השמת כוכבי הדירוג עבור מדד המחיר מתוך נתוני המודל
                                    rbStatsPrice.setRating(business.getAvgPrice());
                                } else {
                                    // הצגת טקסט ברירת מחדל במידה ואין עדיין ביקורות לעסק
                                    tvAvgRating.setText("טרם");
                                    // איפוס כל רכיבי כוכבי הדירוג על המסך ל-0
                                    rbStatsProfessionalism.setRating(0);
                                    rbStatsReliability.setRating(0);
                                    rbStatsPrice.setRating(0);
                                }
                            }

                            // קריאה לפעולה הפנימית שמחשבת את הסטטיסטיקות של התורים שלו
                            calculateAppointmentsStats();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    // פעולה המופעלת במקרה של כשל או תקלת תקשורת מול השרת בענן
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // הצגת הודעת שגיאה על גבי המסך באמצעות קונטקסט מפורש של האקטיביטי
                        Toast.makeText(BusinessStatisticsActivity.this, "שגיאה בטעינת הנתונים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // פעולה פרטית המחשבת ומנתחת את כל נתוני התורים וההכנסות של העסק מתוך מסד הנתונים
    private void calculateAppointmentsStats() {
        // בדיקת הגנה לוודא שמזהה העסק חולץ והוא קיים לפני הרצת השאילתה
        if (businessId == null) return;

        // פנייה לאוסף התורים ושליפת כל התורים המשויכים לעסק הנוכחי
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    // פעולה המופעלת ברגע שרשימת התורים נשלפה בהצלחה מהענן
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // משתנה מונה לספירת כמות התורים התקפים והרלוונטיים
                        int totalValidOrders = 0;
                        // משתנה מספרי לסיכום סך כל הפדיון וההכנסות של העסק
                        double totalIncome = 0;

                        // יצירת מפות נתונים (HashMap) לצורך ספירה ושקלול שכיחויות של שעות, שירותים ולקוחות
                        Map<String, Integer> hoursMap = new HashMap<>();
                        Map<String, Integer> servicesMap = new HashMap<>();
                        Map<String, Integer> usersMap = new HashMap<>();

                        // לולאה העוברת על כל מסמך תור בנפרד מתוך אוסף התוצאות שחזרו מהמסד
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            // שליפת הסטטוס הנוכחי של התור מהמסמך
                            String status = doc.getString("status");

                            // דילוג והתעלמות מתורים שבוטלו על ידי העסק או חלונות זמן שנחסמו על ידו
                            if ("REJECTED".equals(status) || "BLOCKED".equals(status)) continue;

                            // קידום מונה התורים התקפים ב-1 עבור תור רלוונטי
                            totalValidOrders++;

                            // שליפת פרטי התאריך, השעה, התיאור ומזהה הלקוח מתוך מסמך התור
                            String date = doc.getString("date");
                            String time = doc.getString("time");
                            String desc = doc.getString("description");
                            String userId = doc.getString("userId");

                            // תנאי הבודק האם התור מאושר והאם מועדו כבר עבר בפועל, לצורך חישוב הכנסות קופה
                            if ("APPROVED".equals(status) && isAppointmentPassed(date, time)) {
                                // שליפת ערך המחיר הישיר מתוך השדה הייעודי שלו במסמך
                                Double priceVal = doc.getDouble("price");

                                // בדיקה האם קיים ערך מספרי תקין בשדה המחיר
                                if (priceVal != null) {
                                    // הוספת המחיר ישירות אל סך כל ההכנסות שנצברו
                                    totalIncome += priceVal;
                                }
                                // לוגיקת גיבוי מיוחדת במידה והמחיר שמור כחלק מטקסט התיאור בתוך סוגריים
                                else if (desc != null && desc.contains("(") && desc.contains(")")) {
                                    // מציאת המיקום של הסוגר הפותח האחרון בטקסט
                                    int start = desc.lastIndexOf("(");
                                    // מציאת המיקום של הסוגר הסוגר האחרון בטקסט
                                    int end = desc.lastIndexOf(")");
                                    // בדיקה שהסוגריים ממוקמים בצורה הגיונית ותקינה
                                    if (start < end) {
                                        // חילוץ מחרוזת המחיר מתוך הסוגריים וניקוי כל תו שאינו ספרה או נקודה
                                        String priceStr = desc.substring(start + 1, end).replaceAll("[^\\d.]", "");
                                        // בדיקה שהמחרוזת שחולצה אינה ריקה
                                        if (!priceStr.isEmpty()) {
                                            try {
                                                // המרת המחרוזת המנוקה למספר עשרוני והוספתו לסך ההכנסות
                                                totalIncome += Double.parseDouble(priceStr);
                                            } catch (NumberFormatException ignored) {} // התעלמות משגיאות המרה
                                        }
                                    }
                                }
                            }

                            // לוגיקה לבדיקת שעת התור וספירת השכיחות שלה במפת השעות
                            if (time != null && time.contains(":")) {
                                // חיתוך מחרוזת השעה והפיכתה לפורמט עגול
                                String hour = time.split(":")[0] + ":00";
                                // עדכון המפה: הוספת 1 לשכיחות הקיימת של השעה הזו, או קביעת 1 כברירת מחדל
                                hoursMap.put(hour, hoursMap.getOrDefault(hour, 0) + 1);
                            }

                            // לוגיקה לבדיקת שם השירות שבוצע וספירת השכיחות שלו במפת השירותים
                            if (desc != null && !desc.isEmpty()) {
                                // בדיקה האם שם השירות מכיל פירוט מחיר בסוגריים לצורך ניקוי והפרדה
                                if (desc.contains(" (")) {
                                    // חיתוך המחרוזת ולקיחת החלק הראשון בלבד המייצג את שם הטיפול הנקי
                                    String serviceName = desc.split(" \\(")[0];
                                    servicesMap.put(serviceName, servicesMap.getOrDefault(serviceName, 0) + 1);
                                } else {
                                    // שמירת התיאור כולו כשם השירות במידה ואין בו סוגריים
                                    servicesMap.put(desc, servicesMap.getOrDefault(desc, 0) + 1);
                                }
                            }

                            // לוגיקה לבדיקת מזהה המשתמש ורישום כמות הביקורים שלו במפת הלקוחות
                            if (userId != null && !userId.trim().isEmpty()) {
                                usersMap.put(userId, usersMap.getOrDefault(userId, 0) + 1);
                            }
                        }

                        // הצגת סך ההכנסות שחושבו על גבי המסך בפורמט כספי עם סימן שקל וללא שברים
                        tvTotalIncome.setText("₪" + String.format(Locale.getDefault(), "%,.0f", totalIncome));
                        // הצגת כמות התורים התקפים הכוללת בתיבת הטקסט המתאימה
                        tvTotalOrders.setText(String.valueOf(totalValidOrders));

                        // אלגוריתם למציאת שעת העומס הפופולרית ביותר מתוך מפת השעות
                        String peakHour = "אין נתונים";
                        int maxHourCount = 0;
                        for (Map.Entry<String, Integer> entry : hoursMap.entrySet()) {
                            if (entry.getValue() > maxHourCount) {
                                maxHourCount = entry.getValue();
                                peakHour = entry.getKey();
                            }
                        }
                        // הצגת שעת השיא שנמצאה על גבי המסך
                        tvPeakHour.setText(peakHour);

                        // אלגוריתם למציאת הטיפול המבוקש ביותר מתוך מפת השירותים
                        String popularService = "אין נתונים";
                        int maxServiceCount = 0;
                        for (Map.Entry<String, Integer> entry : servicesMap.entrySet()) {
                            if (entry.getValue() > maxServiceCount) {
                                maxServiceCount = entry.getValue();
                                popularService = entry.getKey();
                            }
                        }
                        // הצגת שם הטיפול הפופולרי ביותר על גבי המסך
                        tvPopularService.setText(popularService);

                        // אלגוריתם לספירת כמות הלקוחות החוזרים מתוך מפת המשתמשים
                        int returningCustomers = 0;
                        for (int count : usersMap.values()) {
                            // לקוח נחשב כחוזר אם מזהה המשתמש שלו הופיע יותר מפעם אחת ברשימת התורים
                            if (count > 1) {
                                returningCustomers++;
                            }
                        }
                        // הצגת כמות הלקוחות החוזרים שחושבה על גבי המסך
                        tvReturningCustomers.setText(String.valueOf(returningCustomers));
                    }
                });
    }

    // פעולה פרטית ובודקת המקבלת תאריך ושעה ומחזירה האם התור כבר עבר ביחס לזמן הנוכחי
    private boolean isAppointmentPassed(String dateStr, String timeStr) {
        // בדיקת הגנה לוודא שהפרמטרים של הזמן שהתקבלו אינם ריקים או חסרים
        if (dateStr == null || timeStr == null || dateStr.isEmpty() || timeStr.isEmpty()) return false;

        // יצירת תבנית עיצוב קבועה לניתוח והמרת מחרוזות של תאריך ושעה מלאים
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());
        try {
            // המרת שרשור מחרוזות התאריך והשעה לעצם מסוג Date של Java
            Date appDateTime = sdf.parse(dateStr + " " + timeStr);
            // בדיקה שהעצם נוצר ותורגם בהצלחה
            if (appDateTime != null) {
                // החזרת ערך אמת אם זמן התור נמצא כרונולוגית לפני הזמן הנוכחי של המכשיר
                return appDateTime.before(new Date());
            }
        } catch (ParseException e) {
            // החזרת שקר במקרה של כשל במבנה או בעיצוב מחרוזת הזמן שנתקבלו
            return false;
        }
        return false;
    }
}