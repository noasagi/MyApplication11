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

// הגדרת מחלקת מסך היסטוריית התורים של הלקוח
public class CustomerHistoryActivity extends AppCompatActivity {

    // הצהרה על רכיב הרשימה הממוחזרת להצגת היסטוריית התורים
    private RecyclerView rvCustomerHistory;
    // הצהרה על רכיב טקסט המוצג רק כאשר אין תורים בהיסטוריה
    private TextView tvNoHistory;
    // הצהרה על המתאם הפנימי ועל הרשימה הדינמית שמחזיקה את מודלי התורים
    private HistoryAdapter adapter;
    private List<Appointment> historyList;

    // רכיבי הגישה המרכזיים של פיירסטור ומערכת ניהול המשתמשים
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת וחיבור קובץ ה-XML של עיצוב מסך היסטוריית התורים
        setContentView(R.layout.activity_customer_history);

        // קישור משתני הרכיבים לרכיבים הויזואליים מתוך קובץ ה-XML
        rvCustomerHistory = findViewById(R.id.rvCustomerHistory);
        tvNoHistory = findViewById(R.id.tvNoHistory);

        // קבלת מופעי הגישה אל בסיס הנתונים ומערכת האימות של פיירבייס
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // הגדרת מנהל פריסה אנכי לרכיב הרשימה תוך שימוש בהקשר המפורש של האקטיביטי
        rvCustomerHistory.setLayoutManager(new LinearLayoutManager(CustomerHistoryActivity.this));

        // אתחול רשימת התורים המקומית ויצירת המתאם המקשר בינה לבין ה-RecyclerView
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        rvCustomerHistory.setAdapter(adapter);

        // זימון הפעולה האחראית על שליפת, סינון וטעינת נתוני ההיסטוריה מהענן
        loadHistory();
    }

    // פעולה פרטית המבצעת שליפת תורים, המרת זמנים וסינון היסטוריית לקוח
    private void loadHistory() {
        // הגנה: בדיקה האם קים משתמש מחובר, במידה ולא נעצור את הפעולה
        if (auth.getCurrentUser() == null) return;

        // קבלת הזמן הנוכחי של המערכת במילישניות לצורך חישובי עבר/עתיד
        final long currentTime = System.currentTimeMillis();

        // פנייה לאוסף התורים הכללי, סינון לפי מזהה המשתמש ומיון לפי חותם זמן יורד
        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // ניקוי הרשימה המקומית בזיכרון כדי למנוע כפילויות של מידע ישן
                        historyList.clear();

                        // מעבר בלולאה על כל מסמכי התורים שחזרו מהשאילתה בענן
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            // המרת מסמך הפיירסטור הגולמי ישירות לאובייקט מסוג מודל תור
                            Appointment app = doc.toObject(Appointment.class);
                            // הזרקת מזהה המסמך הייחודי מתוך פיירסטור אל תוך שדה המודל
                            app.setAppointmentId(doc.getId());

                            long appointmentTimeInMillis = 0;
                            try {
                                // חיבור מחרוזת התאריך ומחרוזת השעה למחרוזת זמן אחת אחידה
                                String dateTimeStr = app.getDate() + " " + app.getTime();
                                // הגדרת פורמט מפנח תואם הכולל ימים, חודשים, שנים, שעות ודקות
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                // פענוח והמרת מחרוזת הטקסט לאובייקט מסוג תאריך (Date)
                                Date date = sdf.parse(dateTimeStr);
                                if (date != null) {
                                    // חילוץ הזמן הריאלי במילישניות מתוך אובייקט התאריך המפוענח
                                    appointmentTimeInMillis = date.getTime();
                                }
                            } catch (Exception e) {
                                // מנגנון הגנה: אם הפענוח נכשל, נשתמש בחותם הזמן הגנרי ששמור במודל לקוד
                                appointmentTimeInMillis = app.getTimestamp();
                            }

                            // בדיקה לוגית מתמטית: האם זמן התור קטן מהזמן הנוכחי (כלומר התור שייך לעבר)
                            boolean isPast = appointmentTimeInMillis < currentTime;
                            // בדיקה לוגית מחרוזתית: האם התור בוטל או נדחה על ידי בעל העסק
                            boolean isRejected = "REJECTED".equals(app.getStatus());

                            // סינון: התור ייכנס להיסטוריה רק אם הוא שייך לעבר או שהוא בסטטוס מבוטל
                            if (isPast || isRejected) {
                                historyList.add(app);
                            }
                        }

                        // הודעה למתאם הרשימה לבצע ריענון חזותי של שורות התורים על המסך
                        adapter.notifyDataSetChanged();

                        // ניהול נראות ויזואלית: הצגת הודעת טקסט מתאימה במידה והיסטוריית התורים ריקה
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

    // --- אדפטר פנימי מבוסס מחלקה קלאסית לניהול שורות הרשימה בהיסטוריה ---
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<Appointment> list;

        // בנאי המקבל את רשימת התורים המסוננת להצגה
        public HistoryAdapter(List<Appointment> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // ניפוח וטעינת קובץ עיצוב השורה הבודדת מתוך ה-XML
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            // שליפת אובייקט התור הספציפי בהתאם למיקום הנוכחי בשורת הרשימה
            final Appointment app = list.get(position);

            // הצגת מחרוזת התאריך והשעה המשולבים על גבי רכיב הטקסט בשורה
            holder.tvHistoryDateTime.setText(app.getDate() + " | " + app.getTime());

            // לוגיקת טעינת שם העסק: בודק אם השם קיים כבר במודל
            if (app.getBusinessName() != null && !app.getBusinessName().isEmpty()) {
                holder.tvHistoryBusinessName.setText(app.getBusinessName());
            } else if (app.getBusinessId() != null) {
                // אם השם לא קיים אך יש מזהה, נציג טקסט זמני ונשלוף את השם מאוסף העסקים בענן
                holder.tvHistoryBusinessName.setText("טוען...");
                db.collection("businesses").document(app.getBusinessId()).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot ds) {
                                if (ds.exists()) {
                                    String name = ds.getString("name");
                                    if (name != null) {
                                        holder.tvHistoryBusinessName.setText(name);
                                        app.setBusinessName(name); // עדכון מקומי במודל למניעת שליפות כפולות
                                    }
                                }
                            }
                        });
            } else {
                holder.tvHistoryBusinessName.setText("עסק לא ידוע");
            }

            // קביעת מראה סטטוס התור וצביעתו הגרפית בהתאם לערך השדה
            String status = app.getStatus();
            if ("REJECTED".equals(status)) {
                holder.tvHistoryStatus.setText("❌ בוטל / נדחה");
                holder.tvHistoryStatus.setTextColor(android.graphics.Color.RED);
            } else {
                holder.tvHistoryStatus.setText("✅ הושלם בהצלחה");
                holder.tvHistoryStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            }

            // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור "הזמן שוב" למעבר למסך קביעת תור חדש
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
            // החזרת הכמות הכוללת של הפריטים הקיימים ברשימה המסוננת לקוד
            return list.size();
        }

        // מחלקת עזר פנימית להחזקה וקישור של רכיבי הממשק הויזואליים של השורה הבודדת
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