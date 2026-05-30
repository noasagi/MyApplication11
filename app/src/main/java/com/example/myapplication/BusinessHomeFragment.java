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

// הגדרת המחלקה המייצגת את מסך הבית של בעל העסק, היורשת ממאפייני Fragment
public class BusinessHomeFragment extends Fragment {

    // הצהרה על רכיבי הטקסט השונים להצגת הנתונים במסך הבית
    private TextView tvWelcome, tvDate, tvTodayCount, tvPendingCount, tvDailyRevenue, tvNextClientName, tvNextClientInfo;

    // הצהרה על עצם הגישה לבסיס הנתונים פיירסטור
    private FirebaseFirestore db;

    // הצהרה על עצם הגישה למערכת אימות המשתמשים של פיירבייס
    private FirebaseAuth auth;

    // הצהרה על משתנה מחרוזת לשמירת מזהה העסק הייחודי שנמצא בטיפול
    private String businessId;

    // בנאי ברירת מחדל ריק הנדרש עבור מחלקות מסוג פרגמנט
    public BusinessHomeFragment() {}

    // פעולת מערכת האחראית על יצירת וניפוח ממשק המשתמש הויזואלי של הפרגמנט
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // טעינת וניפוח קובץ ה-XML של מסך הבית של בעל העסק
        View view = inflater.inflate(R.layout.fragment_business_home, container, false);

        // אתחול וקבלת המופע של בסיס הנתונים פיירסטור
        db = FirebaseFirestore.getInstance();
        // אתחול וקבלת המופע של מערכת האימות פיירבייס
        auth = FirebaseAuth.getInstance();

        // קישור משתני רכיבי הטקסט לרכיבים הויזואליים האמיתיים מתוך קובץ ה-XML
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvDate = view.findViewById(R.id.tvDate);
        tvTodayCount = view.findViewById(R.id.tvTodayCount);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvDailyRevenue = view.findViewById(R.id.tvDailyRevenue);
        tvNextClientName = view.findViewById(R.id.tvNextClientName);
        tvNextClientInfo = view.findViewById(R.id.tvNextClientInfo);

        // יצירת פורמט תאריך מותאם המציג יום בשבוע, יום בחודש, שם חודש ושנה בעברית
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("he"));
        // הגדרת הטקסט של תאריך היום הנוכחי על גבי המסך בפורמט שנקבע
        tvDate.setText(sdf.format(new Date()));

        // קריאה לפעולה פנימית האחראית על שליפת מזהה העסק וטעינת הנתונים הסטטיסטיים
        fetchBusinessIdAndLoadData();

        // החזרת מבט התצוגה המלא של הפרגמנט למערכת
        return view;
    }

    // פעולה לשליפת מזהה העסק של המשתמש המחובר כעת מתוך מסד הנתונים
    private void fetchBusinessIdAndLoadData() {
        // בדיקת בטיחות לוודא שקיים משתמש מחובר כרגע במערכת האימות
        if (auth.getCurrentUser() == null) return;

        // פנייה לאוסף העסקים וחיפוש מסמך שבו שדה מזהה הבעלים שווה ל-UID של המשתמש המחובר
        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    // פעולה המופעלת ברגע ששליפת נתוני העסק מהמסד הצליחה
                    @Override
                    public void onSuccess(QuerySnapshot qs) {
                        // בדיקה האם תוצאת החיפוש אינה ריקה ונמצא עסק מתאים
                        if (!qs.isEmpty()) {
                            // שליפה ושמירה של מזהה העסק מתוך המסמך הראשון שנמצא
                            businessId = qs.getDocuments().get(0).getString("businessId");

                            // שליפת שם העסק מתוך המסמך
                            String bName = qs.getDocuments().get(0).getString("businessName");
                            // במידה ושם העסק ריק, ננסה לשלוף משדה חלופי בשם name
                            if (bName == null) bName = qs.getDocuments().get(0).getString("name");

                            // בדיקה האם גם השדה החלופי ריק או שאינו מכיל תווים
                            if (bName == null || bName.isEmpty()) {
                                // הגדרת שם ברירת מחדל כללי למסך הברכה
                                bName = "בעל עסק";
                            }

                            // עדכון כותרת הברכה האישית על גבי המסך עם שם העסק
                            tvWelcome.setText("שלום, " + bName);
                            // קריאה לפעולה האחראית על טעינת הנתונים הסטטיסטיים של לוח הבקרה
                            loadDashboardStats();
                        }
                    }
                });
    }

    // פעולה האחראית על טעינת ועדכון הנתונים הסטטיסטיים של העסק בזמן אמת
    private void loadDashboardStats() {
        // בדיקת בטיחות לוודא שמזהה העסק נשלף בהצלחה ואינו ריק
        if (businessId == null) return;

        // יצירת פורמט תאריך סטנדרטי לצורך השוואה מול מסד הנתונים
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        // שמירת מחרוזת תאריך היום הנוכחי בפורמט שנקבע
        String todayStr = sdfDate.format(new Date());

        // פנייה לאוסף התורים ושליפת התורים המאושרים של העסק עבור תאריך היום
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("date", todayStr)
                .whereEqualTo("status", "APPROVED")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    // פעולה המופעלת אוטומטית בכל פעם שיש שינוי או עדכון בנתוני התורים בשרת
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                        // בדיקה האם התרחשה שגיאה בתקשורת או שהנתונים שחזרו ריקים
                        if (error != null || snapshots == null) return;

                        // עדכון כמות התורים של היום על גבי המסך לפי כמות המסמכים שחזרו
                        tvTodayCount.setText(String.valueOf(snapshots.size()));

                        // משתנה לצבירת סך ההכנסות היומיות מהתורים של היום
                        double totalRevenue = 0;
                        // לולאה המעוברת על כל מסמך תור בנפרד מתוך רשימת התורים של היום
                        for (QueryDocumentSnapshot doc : snapshots) {
                            // ניסיון שליפת שדה המחיר כסוג נתון של שלם ארוך
                            Long p = doc.getLong("price");
                            // בדיקה האם השדה קיים ותקין
                            if (p != null) {
                                // הוספת הערך המספרי לסך ההכנסות המצטבר
                                totalRevenue += p.doubleValue();
                            } else {
                                // במידה ולא נמצא כשלם, ננסה לשלוף אותו כסוג נתון עשרוני
                                Double d = doc.getDouble("price");
                                // הוספת הערך העשרוני לסך ההכנסות במידה וקיים
                                if (d != null) totalRevenue += d;
                            }
                        }
                        // עדכון רכיב הטקסט של ההכנסה היומית בתוספת סימן המטבע
                        tvDailyRevenue.setText("₪ " + (long)totalRevenue);
                        // קריאה לפעולת עזר לעדכון פרטי התור הבא בתור מתוך הרשימה הנוכחית
                        updateNextAppointmentFromList(snapshots);
                    }
                });

        // פנייה לאוסף התורים ושליפת התורים שנמצאים במצב ממתין לאישור עבור עסק זה
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    // פעולה המופעלת בזמן אמת בעת עדכון תורים הממתינים לאישור בענן
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                        // בדיקת שגיאות או נתונים ריקים שחזרו מהשרת בענן
                        if (error != null || snapshots == null) return;

                        // משתנה מנייה לספירת כמות התורים הממתינים שאינם שייכים לעבר
                        int validPendingCount = 0;
                        // לולאה המעוברת על כל מסמכי התורים הממתינים שחזרו מהמסד
                        for (QueryDocumentSnapshot doc : snapshots) {
                            // שליפת מחרוזת התאריך של התור הנוכחי בלולאה
                            String dateStr = doc.getString("date");
                            // בדיקה באמצעות פעולת עזר האם התאריך של התור אינו נמצא בעבר
                            if (!isDateInPast(dateStr)) {
                                // קידום המונה במידה והתור רלוונטי ועתידי
                                validPendingCount++;
                            }
                        }
                        // הצגת כמות התורים הממתינים הרלוונטיים על גבי המסך
                        tvPendingCount.setText(String.valueOf(validPendingCount));
                    }
                });
    }

    // פעולת עזר לאיתור ועדכון פרטי התור הקרוב ביותר להיום מתוך רשימת התורים המאושרים
    private void updateNextAppointmentFromList(QuerySnapshot snapshots) {
        // שמירת השעה הנוכחית בפורמט של שעות ודקות לצורך השוואת זמנים
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        // משתנה זמני שיחזיק את מסמך התור הקרוב ביותר שנמצא
        QueryDocumentSnapshot nextDoc = null;
        // הגדרת ערך קיצוני עליון לשעה, שיוחלף בכל פעם שיימצא תור מוקדם יותר
        String minT = "23:59";

        // לולאה העוברת על פני כל התורים המאושרים של היום
        for (QueryDocumentSnapshot doc : snapshots) {
            // שליפת מחרוזת השעה של התור הנוכחי בלולאה
            String time = doc.getString("time");
            // תנאי הבודק האם השעה קיימת, גדולה או שווה לשעה הנוכחית, וקטנה מהשעה המינימלית שנמצאה עד כה
            if (time != null && time.compareTo(now) >= 0 && time.compareTo(minT) < 0) {
                // עדכון השעה המינימלית בשעה החדשה והקרובה יותר שנמצאה
                minT = time;
                // שמירת הפניה למסמך התור הספציפי הזה
                nextDoc = doc;
            }
        }

        // תנאי הבודק האם נמצא תור עתידי קרוב להיום
        if (nextDoc != null) {
            // הצגת שם הלקוח של התור הבא על גבי המסך
            tvNextClientName.setText(nextDoc.getString("userName"));
            // הצגת שעת התור ותיאור הטיפול על גבי רכיב המידע במסך
            tvNextClientInfo.setText("בשעה " + nextDoc.getString("time") + " | " + nextDoc.getString("description"));
        } else {
            // עדכון רכיבי הטקסט בהתאם במידה ואין יותר תורים להיום
            tvNextClientName.setText("אין תורים נוספים");
            tvNextClientInfo.setText("סיימת להיום!");
        }
    }

    // פעולת עזר בוליאנית לבדיקה האם תאריך מסוים כבר עבר ביחס ליום הנוכחי
    private boolean isDateInPast(String dateStr) {
        // בדיקה האם מחרוזת התאריך שהתקבלה ריקה או לא קיימת
        if (dateStr == null || dateStr.isEmpty()) return false;
        // יצירת מפרש תאריכים לפי הפורמט של יום/חודש/שנה
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        try {
            // המרת מחרוזת התאריך לעצם מסוג Date
            Date appointmentDate = sdf.parse(dateStr);
            // קבלת עצם תאריך המייצג את זמן הרגע הנוכחי
            Date today = new Date();
            // אתחול שני עצמי לוח שנה לצורך חישובי והשוואת שדות התאריך
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            // הגדרת הזמן של לוח השנה הראשון לפי תאריך התור במידה וקיים
            if (appointmentDate != null) cal1.setTime(appointmentDate);
            // הגדרת הזמן של לוח השנה השני לפי תאריך היום
            cal2.setTime(today);

            // תנאי הבודק האם שנת התור קטנה מהשנה הנוכחית
            if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) return true;
            // תנאי הבודק האם השנה זהה אך היום בשנה של התור קטן מהיום הנוכחי בשנה
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR);
        } catch (ParseException e) {
            // החזרת שקר במקרה של כשל במבנה ופירוש מחרוזת התאריך
            return false;
        }
    }
}