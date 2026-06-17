package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.Nullable;

public class BusinessHomeFragment extends Fragment {

    private TextView tvWelcome, tvDate, tvTodayCount, tvPendingCount, tvDailyRevenue, tvNextClientName, tvNextClientInfo;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;

    public BusinessHomeFragment() {}

    /**
     * מה הפעולה עושה: מנפחת את קובץ הממשק (UI), מקשרת את הרכיבים, מציגה את תאריך היום ומפעילה את תהליך זיהוי העסק וטעינת הנתונים.
     * קלט: LayoutInflater, ViewGroup container, Bundle savedInstanceState.
     * פלט: View (תצוגת הפרגמנט המוכנה).
     */
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

        // יצירת מחרוזת תאריך מילולית וברורה בעברית להצגה בכותרת (למשל: "יום שלישי, 16 ביוני 2026")
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("he"));
        tvDate.setText(sdf.format(new Date()));

        fetchBusinessIdAndLoadData();

        return view;
    }

    /**
     * מה הפעולה עושה: מאתרת את מסמך העסק השייך לבעלים הנוכחי לפי מזהה המשתמש שלו (UID), מעדכנת את כותרת הברכה וממשיכה לטעינת הסטטיסטיקות.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void fetchBusinessIdAndLoadData() {
        if (auth.getCurrentUser() == null) return;

        // שליפת מסמך העסק שבו ה-ownerId תואם למשתמש המחובר כרגע
        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qs) {
                        if (!qs.isEmpty()) {
                            DocumentSnapshot doc = qs.getDocuments().get(0);
                            businessId = doc.getString("businessId");

                            // שליפת שם העסק מתוך השדות האפשריים במסמך לצורך הצגת הודעת ברכה מותאמת אישית
                            String bName = doc.getString("businessName");
                            if (bName == null) bName = doc.getString("name");
                            if (bName == null || bName.isEmpty()) bName = "בעל עסק";

                            tvWelcome.setText("שלום, " + bName);

                            // מעבר לשלב הבא: הצבת מאזינים בזמן אמת עבור לוח הבקרה (Dashboard)
                            loadDashboardStats();
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: מחברת מאזינים בזמן אמת (addSnapshotListener) לחישוב אוטומטי של כמות התורים המאושרים להיום, סך ההכנסות היומי, איתור התור הקרוב ביותר וספירת התורים הממתינים העתידיים.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadDashboardStats() {
        if (businessId == null) return;

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String todayStr = sdfDate.format(new Date());

        // מאזין 1: שליפת התורים המאושרים של היום לצורך סיכום כמותי, כספי ואיתור הלקוח הבא
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("date", todayStr)
                .whereEqualTo("status", "APPROVED")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                        if (error != null || snapshots == null) return;

                        // עדכון כמות התורים שנקבעו להיום
                        tvTodayCount.setText(String.valueOf(snapshots.size()));

                        // אלגוריתם סכימת הכנסות: ריצה על פריטי היום וטיפול גמיש בשדה המחיר (הן כ-Long והן כ-Double) למניעת שגיאות טיפוס
                        double totalRevenue = 0;
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Long p = doc.getLong("price");
                            if (p != null) {
                                totalRevenue += p.doubleValue();
                            } else {
                                Double d = doc.getDouble("price");
                                if (d != null) totalRevenue += d;
                            }
                        }
                        tvDailyRevenue.setText("₪ " + (long)totalRevenue);

                        // קריאה לפונקציה פנימית שתעבד מתוך הרשימה הנוכחית את התור הקרוב ביותר לשעה הנוכחית
                        updateNextAppointmentFromList(snapshots);
                    }
                });

        // מאזין 2: שליפת תורים במצב PENDING (ממתין לאישור) תוך סינון תורים ישנים שנשארו בעבר ולא רלוונטיים
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                        if (error != null || snapshots == null) return;

                        int validPendingCount = 0;
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String dateStr = doc.getString("date");
                            if (!isDateInPast(dateStr)) {
                                validPendingCount++;
                            }
                        }
                        tvPendingCount.setText(String.valueOf(validPendingCount));
                    }
                });
    }

    /**
     * מה הפעולה עושה: סורקת את רשימת התורים המאושרים של היום, משווה את שעותיהם לשעה הנוכחית ומציגה את הלקוח הקרוב ביותר.
     * קלט: QuerySnapshot snapshots (אוסף מסמכי התורים של היום).
     * פלט: אין (void).
     */
    private void updateNextAppointmentFromList(QuerySnapshot snapshots) {
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        QueryDocumentSnapshot nextDoc = null;
        String minT = "23:59";

        // לולאה המוצאת את התור המוקדם ביותר ששעתו עדיין גדולה או שווה לשעה הנוכחית במכשיר
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

    /**
     * מה הפעולה עושה: בודקת האם תאריך שהתקבל במחרוזת נמצא כרונולוגית לפני היום הנוכחי (ברמת ימים ושנים בלבד).
     * קלט: String dateStr (מחרוזת התאריך לבדיקה).
     * פלט: boolean (אמת אם התאריך עבר, שקר אם היום או בעתיד).
     */
    private boolean isDateInPast(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        try {
            Date appointmentDate = sdf.parse(dateStr);
            Date today = new Date();

            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            if (appointmentDate != null) cal1.setTime(appointmentDate);
            cal2.setTime(today);

            // השוואה לוגית ברמת השנה והיום בשנה כדי לקבוע אם התאריך שייך לאתמול ומטה
            if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return true;
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR);
        } catch (ParseException e) {
            return false;
        }
    }
}