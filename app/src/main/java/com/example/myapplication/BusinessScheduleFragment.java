package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// הגדרת מחלקה לניהול מסך תורנים ויומן עבודה של עסק, היורשת מפרגמנט
public class BusinessScheduleFragment extends Fragment {

    // הצהרה על רכיב הרשימה הממוחזרת להצגת התורים במסך
    private RecyclerView rvAppointments;

    // הצהרה על מתאם מותאם אישית לקישור נתוני התורים לרכיבי התצוגה ברשימה
    private AppointmentsAdapter adapter;

    // הצהרה על רשימה דינמית שתכיל את עצמי התורים שיישלפו מהמסד
    private List<Appointment> appointmentList;

    // הצהרה על עצם הגישה לבסיס הנתונים פיירסטור של פיירבייס
    private FirebaseFirestore db;

    // הצהרה על עצם הגישה למערכת אימות המשתמשים של פיירבייס
    private FirebaseAuth auth;

    // הצהרה על משתנה מחרוזת לשמירת מזהה העסק הייחודי שבו אנו מטפלים
    private String businessId;

    // הצהרה על רכיב רישום המאזין בזמן אמת, המשמש לניתוק המאזין בסגירת המסך
    private ListenerRegistration appointmentsListener;

    // בנאי ברירת מחדל ריק המחויב על פי כללי המערכת ביצירת פרגמנט
    public BusinessScheduleFragment() {
        // Required empty public constructor
    }

    // פעולת המערכת המרכזית לבנייה וניפוח של קובץ העיצוב הויזואלי בפרגמנט
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // טעינת וניפוח קובץ ה-XML של מסך ניהול התורים של העסק
        View view = inflater.inflate(R.layout.fragment_business_schedule, container, false);

        // קבלת מופע ואתחול של בסיס הנתונים פיירסטור
        db = FirebaseFirestore.getInstance();
        // קבלת מופע ואתחול של מערכת אימות המשתמשים
        auth = FirebaseAuth.getInstance();

        // קישור משתנה הרשימה לרכיב הויזואלי הממוחזר ב-XML
        rvAppointments = view.findViewById(R.id.rvAppointments);
        // הגדרת מנהל פריסה אנכי עבור הרשימה הממוחזרת להצגת הפריטים בזה אחר זה
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        // אתחול הרשימה הדינמית לשמירת התורים בזיכרון המכשיר
        appointmentList = new ArrayList<>();
        // יצירת מופע חדש של המתאם והעברת רשימת התורים הריקה אליו
        adapter = new AppointmentsAdapter(appointmentList);
        // חיבור המתאם המתוכנת אל הרשימה הממוחזרת בממשק הויזואלי
        rvAppointments.setAdapter(adapter);

        // קריאה לפעולה פנימית לשליפת מזהה העסק וטעינת התורים בהתאם
        fetchBusinessIdAndLoad();

        // החזרת התצוגה המלאה והמנופחת של המסך
        return view;
    }

    // פעולה פרטית לשליפת מזהה העסק על פי קוד ה-UID של בעל העסק המחובר
    private void fetchBusinessIdAndLoad() {
        // בדיקת הגנה לוודא שישנו משתמש המחובר כעת בשרת האימות
        if (auth.getCurrentUser() == null) return;

        // ביצוע שאילתה באוסף העסקים לאיתור עסק ששדה מזהה הבעלים שלו תואם למשתמש המחובר
        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    // פעולה המופעלת ברגע ששליפת נתוני העסק הסתיימה בהצלחה
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // בדיקה האם נמצא מסמך עסק התואם לתנאי השאילתה במסד
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // שליפה ושמירה של מזהה העסק מתוך מסמך העסק הראשון שחזר
                            businessId = queryDocumentSnapshots.getDocuments().get(0).getString("businessId");
                            // זימון הפעולה האחראית על טעינת רשימת התורים המשויכים לעסק זה
                            loadAppointments();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    // פעולה המופעלת במקרה של כשל או תקלת תקשורת מול השרת
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // בדיקה שהפרגמנט עדיין מחובר למסך ושההקשר הויזואלי קיים
                        if (isAdded() && getContext() != null)
                            // הקפצת הודעה קצרה על המסך המתריעה על שגיאה בטעינת נתוני העסק
                            Toast.makeText(getContext(), "שגיאה בטעינת נתוני עסק", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // פעולה הטוענת ומאזינה לתורים המשויכים לעסק זה בזמן אמת מהענן
    private void loadAppointments() {
        // בדיקת בטיחות לוודא שמזהה העסק קיים ותקין לפני הרצת השאילתה
        if (businessId == null) return;

        // בדיקה האם קיים מאזין פעיל קודם, ובמידה וכן - ננתק אותו למניעת כפילויות של מידע
        if (appointmentsListener != null) {
            appointmentsListener.remove();
        }

        // הגדרת מאזין קבוע בזמן אמת על אוסף התורים המסונן לפי מזהה העסק וממוין לפי חותם זמן יורד
        appointmentsListener = db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    // פעולה המופעלת אוטומטית בכל פעם שמתבצע שינוי, מחיקה או הוספת תור בשרת
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        // בדיקה האם אירעה שגיאה בקבלת המידע או שהפרגמנט נסגר תוך כדי התהליך
                        if (error != null || !isAdded()) return;

                        // ניקוי רשימת התורים המקומית בזיכרון כדי להכין אותה לנתונים המעודכנים
                        appointmentList.clear();
                        // בדיקה שהמידע המוחזר מהשרת אינו ריק ומכיל מסמכים
                        if (value != null) {
                            // לולאה העוברת על כל מסמך תור בנפרד מתוך אוסף התוצאות שחזר
                            for (QueryDocumentSnapshot doc : value) {
                                // המרת נתוני המסמך מהמסד ישירות לעצם מובנה מסוג מחלקת התור
                                Appointment app = doc.toObject(Appointment.class);
                                // שמירת מזהה המסמך הייחודי מתוך פיירסטור בתוך שדה ייעודי בעצם התור
                                app.setAppointmentId(doc.getId());

                                // בדיקה באמצעות פעולת סינון האם יש להציג את התור הזה בממשק
                                if (shouldShowAppointment(app)) {
                                    // הוספת עצם התור התקין אל רשימת התורים המקומית
                                    appointmentList.add(app);
                                }
                            }
                        }
                        // עדכון המתאם כי חל שינוי ברשימה, על מנת שיצייר מחדש את הרכיבים במסך
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // פעולת עזר המגדירה את חוקי הסינון וההצגה של התורים במסך היומן
    private boolean shouldShowAppointment(Appointment app) {
        // שליפת מחרוזת הסטטוס של התור הנוכחי
        String status = app.getStatus();
        // במידה ושדה הסטטוס ריק, נקבע לו ערך ברירת מחדל כממתין לאישור
        if (status == null) status = "PENDING";

        // במידה והתור נדחה או נחסם על ידי בעל העסק, נחזיר שקר כדי שלא יוצג ברשימה
        if (status.equals("REJECTED") || status.equals("BLOCKED")) return false;

        // החזרת ערך בוליאני הפוך הבודק האם תאריך התור כבר שייך לימים עברו
        return !isDateInPast(app.getDate());
    }

    // פעולת עזר בוליאנית לבדיקה קלנדרית האם תאריך מסוים נמצא בעבר
    private boolean isDateInPast(String dateStr) {
        // בדיקת בטיחות לוודא שמחרוזת התאריך שהתקבלה אינה ריקה
        if (dateStr == null || dateStr.isEmpty()) return false;
        // הגדרת פורמט פירוש תאריכים לפי יום/חודש/שנה במערכת
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        try {
            // המרת מחרוזת הטקסט של התור לעצם תאריך רשמי של שפת ג'אווה
            Date appointmentDate = sdf.parse(dateStr);
            // בדיקה האם פעולת המרת התאריך נכשלה והחזירה ערך ריק
            if (appointmentDate == null) return false;

            // יצירת לוח שנה עבור תאריך התור והזנת הזמן של התור בתוכו
            Calendar calApp = Calendar.getInstance();
            calApp.setTime(appointmentDate);

            // יצירת לוח שנה עבור היום הנוכחי ואיפוס שדות השעה, הדקות והשניות לצורך השוואה נקייה של ימים
            Calendar calToday = Calendar.getInstance();
            calToday.set(Calendar.HOUR_OF_DAY, 0);
            calToday.set(Calendar.MINUTE, 0);
            calToday.set(Calendar.SECOND, 0);
            calToday.set(Calendar.MILLISECOND, 0);

            // החזרת ערך אמת במידה ולוח שנת התור נמצא כרונולוגית לפני לוח שנת היום הנוכחי
            return calApp.before(calToday);
        } catch (ParseException e) {
            // החזרת ערך שקר במקרה של שגיאה או כשל בניתוח מחרוזת התאריך
            return false;
        }
    }

    // פעולת מחזור חיים המופעלת כאשר ממשק הפרגמנט נהרס ונסגר
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // ניתוק והסרת מאזין פיירסטור הפעיל כדי למנוע זליגות זיכרון ברקע של המכשיר
        if (appointmentsListener != null) {
            appointmentsListener.remove();
        }
    }

    // הגדרת מחלקת מתאם פנימית לרשימה הממוחזרת, היורשת מ-RecyclerView.Adapter
    class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.ViewHolder> {
        // הצהרה על רשימת המידע המקומית של המתאם
        private List<Appointment> list;

        // בנאי של מחלקת המתאם המקבל את רשימת התורים כפרמטר
        public AppointmentsAdapter(List<Appointment> list) { this.list = list; }

        // יצירת מחזיק תצוגה חדש עבור פריט ברשימה בעת הצורך על ידי המערכת
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // ניפוח וטעינת קובץ עיצוב ה-XML הייעודי של שורת בקשת תור בודדת
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment_request, parent, false);
            // החזרת עצם מחזיק התצוגה החדש המכיל את העיצוב המנופח
            return new ViewHolder(view);
        }

        // חיבור ויציקת נתוני תור ספציפי מתוך הרשימה אל תוך רכיבי הממשק הויזואליים בשורה
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // שליפת עצם התור המתאים לפי מיקומו הנוכחי ברשימה
            Appointment app = list.get(position);

            // הגדרת שם הלקוח על גבי רכיב הטקסט המתאים במחזיק התצוגה
            holder.tvClientName.setText(app.getUserName());
            // שרשור והגדרת מחרוזת התאריך והשעה על גבי רכיב הטקסט בשורה
            holder.tvDateTime.setText(app.getDate() + " | " + app.getTime());

            // בדיקה האם שדה תיאור הטיפול ריק או לא קיים, וקביעת טקסט חלופי במידת הצורך
            String desc = (app.getDescription() == null || app.getDescription().isEmpty()) ? "אין הערות" : app.getDescription();
            // עדכון רכיב טקסט תיאור הטיפול בשורה
            holder.tvDescription.setText(desc);

            // שליפת מצב התור, ובמידה והוא ריק נקבע אותו כמצב ממתין כברירת מחדל
            String status = app.getStatus() != null ? app.getStatus() : "PENDING";

            // תנאי הבודק האם התור נמצא כעת במצב ממתין לאישור
            if (status.equals("PENDING")) {
                // עדכון טקסט מצב התור לעברית בשורה
                holder.tvStatus.setText("ממתין לאישור");
                // שינוי צבע הטקסט של הסטטוס לצבע כתום המציין המתנה
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
                // הצגה והפיכת כפתור האישור לגלוי בממשק המשתמש
                holder.btnApprove.setVisibility(View.VISIBLE);
                // הצגה והפיכת כפתור הדחייה לגלוי בממשק המשתמש
                holder.btnReject.setVisibility(View.VISIBLE);
                // הגדרת כותרת כפתור הדחייה למילה "דחה"
                holder.btnReject.setText("דחה");
            } // תנאי חלופי הבודק האם התור כבר מאושר במערכת
            else if (status.equals("APPROVED")) {
                // עדכון טקסט מצב התור למילה "מאושר"
                holder.tvStatus.setText("מאושר");
                // שינוי צבע הטקסט של הסטטוס לצבע ירוק המציין הצלחה ואישור
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                // הסתרת כפתור האישור מהמסך מכיוון שהתור כבר אושר בעבר
                holder.btnApprove.setVisibility(View.GONE);
                // השארת כפתור הדחייה גלוי לצורך ביטול עתידי של התור
                holder.btnReject.setVisibility(View.VISIBLE);
                // שינוי כותרת כפתור הביטול למילים "בטל תור"
                holder.btnReject.setText("בטל תור");
            }

            // הגדרת מאזין לחיצה אנונימי רגיל עבור כפתור אישור התור
            holder.btnApprove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // קריאה לפעולת עדכון הסטטוס של התור למצב APPROVED
                    updateStatus(app, "APPROVED");
                }
            });

            // הגדרת מאזין לחיצה אנונימי רגיל עבור כפתור דחיית או ביטול התור
            holder.btnReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // קריאה לפעולת עדכון הסטטוס של התור למצב REJECTED
                    updateStatus(app, "REJECTED");
                }
            });
        }

        // פעולה פנימית במתאם המעדכנת את סטטוס התור ב-Firestore ושולחת הודעת SMS ללקוח
        private void updateStatus(Appointment app, String newStatus) {
            // בדיקת בטיחות לוודא שלתור הנוכחי קיים מזהה מסמך תקין לפיירסטור
            if (app.getAppointmentId() == null) return;

            // פנייה לאוסף התורים, בחירת המסמך הספציפי ועדכון שדה הסטטוס לערך החדש שנתקבל
            db.collection("appointments").document(app.getAppointmentId()).update("status", newStatus)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        // פעולה המופעלת ברגע שעדכון הסטטוס בוצע בהצלחה מלאה בשרת הענן
                        @Override
                        public void onSuccess(Void aVoid) {
                            // בדיקה שהמסך עדיין פעיל ומחובר להקשר של האפליקציה
                            if (isAdded() && getContext() != null) {
                                // הקפצת הודעת חיווי קצרה על המסך המאשרת את עדכון בסיס הנתונים
                                Toast.makeText(getContext(), "סטטוס עודכן בבסיס הנתונים", Toast.LENGTH_SHORT).show();

                                // תנאי הבודק האם קיים מזהה משתמש לקוח המשויך לתור זה
                                if (app.getUserId() != null) {
                                    // שליפת מסמך הפרופיל של הלקוח מאוסף המשתמשים כדי לקבל את מספר הטלפון שלו
                                    db.collection("users").document(app.getUserId()).get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                // פעולה המופעלת עם קבלת מסמך המשתמש בהצלחה מהשרת
                                                @Override
                                                public void onSuccess(DocumentSnapshot userDoc) {
                                                    // משתנה מחרוזת שיכיל את מספר הטלפון של הלקוח
                                                    String clientPhone = "";
                                                    // בדיקה האם מסמך המשתמש אכן קיים במסד הנתונים
                                                    if (userDoc.exists()) {
                                                        // שליפת ערך מספר הטלפון מתוך שדה הטקסט phone במסמך
                                                        clientPhone = userDoc.getString("phone");
                                                    }

                                                    // בניית הודעת הטקסט שתישלח ללקוח בהתאם להחלטת בעל העסק (אישור או ביטול)
                                                    String msg = newStatus.equals("APPROVED") ?
                                                            "איזה יופי! התור שלך עבור " + app.getDescription() + " בתאריך " + app.getDate() + " בשעה " + app.getTime() + " אושר בהצלחה!" :
                                                            "שלום, לצערנו התור שלך עבור " + app.getDescription() + " בתאריך " + app.getDate() + " בשעה " + app.getTime() + " לא אושר או בוטל.";

                                                    // יצירת כוונת מערכת (Intent) לפתיחת אפליקציית שליחת הודעות ה-SMS במכשיר
                                                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                                                    // הגדרת פרוטוקול הכתובת ומספר הטלפון של נמען ההודעה
                                                    smsIntent.setData(Uri.parse("smsto:" + (clientPhone != null ? clientPhone : "")));
                                                    // הוספת תוכן ההודעה המילולית המובנית לתוך כוונת המערכת
                                                    smsIntent.putExtra("sms_body", msg);

                                                    try {
                                                        // הפעלת אפליקציית ה-SMS החיצונית והעברת המידע אליה
                                                        startActivity(smsIntent);
                                                    } catch (Exception e) {
                                                        // תפיסת שגיאה והצגת הודעה במידה ובמכשיר אין רכיב או אפליקציית SMS מותקנת
                                                        Toast.makeText(getContext(), "לא נמצאה אפליקציית SMS מותקנת במכשיר", Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        // פעולה המופעלת במידה ועדכון שדה הסטטוס נכשל בפיירסטור
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // בדיקה שההקשר הויזואלי של המסך עדיין קיים ותקף
                            if (isAdded() && getContext() != null) {
                                // הקפצת הודעה למשתמש המתריעה על כשל טכני בעדכון
                                Toast.makeText(getContext(), "שגיאה בעדכון הסטטוס", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

        // החזרת מספר הפריטים הכולל הקיים ברשימת התורים המקומית
        @Override
        public int getItemCount() { return list.size(); }

        // הגדרת מחלקת עזר פנימית לניהול ואחזקת רכיבי הממשק הויזואליים של השורה ברשימה
        class ViewHolder extends RecyclerView.ViewHolder {
            // הצהרה על רכיבי הטקסט והכפתורים הנמצאים בכל שורה ברשימה
            TextView tvClientName, tvDateTime, tvStatus, tvDescription;
            Button btnApprove, btnReject;

            // בנאי המקבל את מבט השורה ומקשר את משתני המחלקה לרכיבים האמיתיים בעיצוב
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClientName = itemView.findViewById(R.id.tvClientName);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}