package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// מסך המציג ללקוח את התורים שלו, ומאפשר ביטול תור או הוספת ביקורת
public class AppointmentsClientFragment extends Fragment {

    // רכיבי ממשק המשתמש ומבני הנתונים לרשימה
    private RecyclerView rvMyAppointments;
    private UserAppointmentsAdapter adapter;
    private List<Appointment> list; // רשימה דינמית שתחזיק את כל אובייקטי התורים שנשלפו

    // רכיבי קישור ל-Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // טעינת תצוגת ה-XML של המסך והפיכתו לאובייקט תצוגה בקוד
        View view = inflater.inflate(R.layout.fragment_appointments, container, false);

        // אתחול מופעי הגישה למסד הנתונים (Firestore) ולמערכת האימות (Auth)
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // קישור רכיב הרשימה מה-XML לקוד הג'אווה
        rvMyAppointments = view.findViewById(R.id.rvAppointments);

        if (rvMyAppointments != null) {
            if (getContext() != null) {
                // הגדרת מנהל פריסה אנכי (שורות מלמעלה למטה) עבור ה-RecyclerView
                rvMyAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
            }

            list = new ArrayList<>(); // יצירת רשימה ריקה בזיכרון
            adapter = new UserAppointmentsAdapter(list); // יצירת האדפטר וקישורו לרשימה הריקה
            rvMyAppointments.setAdapter(adapter); // חיבור האדפטר ל-RecyclerView במסך
        } else {
            android.util.Log.e("AppointmentsFragment", "Error: RecyclerView rvAppointments not found in XML!");
        }

        // קריאה לפונקציית טעינת הנתונים מהענן
        loadUserAppointments();

        return view;
    }

    /**
     * קלט: אין קלט ישיר (משתמשת ב-UID של המשתמש המחובר).
     * פלט: אין פלט (void).
     * מה עושה ואיך: מאזינה בזמן אמת לאוסף התורים ב-Firestore, מסננת לפי המשתמש הנוכחי וממיינת לפי הזמן.
     */
    private void loadUserAppointments() {
        // הגנה: אם אין משתמש מחובר כרגע במערכת, עצור את הפעולה מיד כדי למנוע קריסה
        if (auth.getCurrentUser() == null) return;

        // בניית השילוף מ-Firestore: פנייה לאוסף "appointments"
        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid()) // סינון: רק תורים שה-userId שלהם שווה ל-ID של המשתמש המחובר
                .orderBy("timestamp", Query.Direction.DESCENDING) // מיון: מהחדש ביותר לישן ביותר על בסיס חותם הזמן
                .addSnapshotListener(new EventListener<QuerySnapshot>() { // פתיחת מאזין קבוע לשינויים בזמן אמת
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        // בדיקת בטיחות: אם יש שגיאה, או שאין נתונים, או שהמסך כבר לא קיים - אל תמשיך
                        if (getContext() == null || error != null || value == null) return;

                        list.clear(); // ניקוי הרשימה המקומית לפני הכנסת הנתונים החדשים כדי למנוע כפילויות במסך
                        long currentTime = System.currentTimeMillis(); // קבלת הזמן הנוכחי של המכשיר במילישניות
                        long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000; // חישוב מתמטי של 3 ימים במילישניות עבור חלון הדירוג

                        // לולאה שעוברת מסמך-מסמך (תור-תור) מתוך כל התוצאות שחזרו מהענן
                        for (QueryDocumentSnapshot doc : value) {
                            // המרה אוטומטית של מסמך ה-Firestore לאובייקט ג'אווה מסוג Appointment
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId()); // שמירת ה-ID של המסמך מ-Firestore בתוך האובייקט

                            long appointmentTimeInMillis = calculateMillis(app); // המרת תאריך ושעה של התור למילישניות מספריות
                            boolean isPast = appointmentTimeInMillis < currentTime; // בדיקה: האם זמן התור כבר עבר? (תור מהעבר)
                            boolean isRejected = "REJECTED".equals(app.getStatus()); // בדיקה: האם התור נדחה על ידי בית העסק?
                            boolean needsReview = "APPROVED".equals(app.getStatus()) && !app.getIsReviewed(); // בדיקה: האם התור אושר ועדיין לא דורג?
                            boolean withinGracePeriod = (currentTime - appointmentTimeInMillis) < threeDaysInMillis; // בדיקה: האם עברו פחות מ-3 ימים מאז התור?

                            // סינון לוגי: הוספת התור לרשימה רק אם הוא לא נדחה, והוא או עתידי, או עבר וצריך דירוג בדחיפות
                            if (!isRejected) {
                                if (!isPast) {
                                    list.add(app); // תור עתידי - תמיד מציגים ללקוח
                                } else if (needsReview && withinGracePeriod) {
                                    list.add(app); // תור מהעבר שזכאי לדירוג - מציגים ללקוח כדי שילחץ על כפתור דירוג
                                }
                            }
                        }
                        // עדכון האדפטר שהרשימה השתנתה, שייקח את הנתונים החדשים ויצייר מחדש את המסך
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * קלט: אובייקט מסוג Appointment (תור).
     * פלט: מספר מסוג long המייצג את הזמן במילישניות.
     * מה עושה ואיך: לוקחת את מחרוזות התאריך והשעה של התור, מחברת אותן וממירה אותן לזמן מספרי כדי שנוכל להשוות לזמן הנוכחי של המערכת.
     */
    private long calculateMillis(Appointment app) {
        try {
            String dateTimeStr = app.getDate() + " " + app.getTime(); // חיבור המחרוזות, למשל: "28/05/2026 14:30"
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()); // הגדרת פורמט הקריאה של הזמן
            Date date = sdf.parse(dateTimeStr); // ניתוח והמרת המחרוזת לאובייקט מסוג Date של ג'אווה
            return (date != null) ? date.getTime() : app.getTimestamp(); // החזרת הזמן במילישניות, או גיבוי מחותם הזמן המקורי
        } catch (Exception e) {
            return app.getTimestamp(); // במקרה של שגיאה (למשל טקסט לא תקין), החזר את חותם הזמן הבסיסי של האובייקט
        }
    }

    // אדפטר פנימי לניהול התצוגה של כל שורה ברשימה
    class UserAppointmentsAdapter extends RecyclerView.Adapter<UserAppointmentsAdapter.ViewHolder> {
        private List<Appointment> appointments;

        public UserAppointmentsAdapter(List<Appointment> appointments) {
            this.appointments = appointments;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // ניפוח וטעינה של קובץ ה-XML שמייצג שורה בודדת ברשימה
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_appointment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            // שליפת אובייקט התור הספציפי שנמצא במיקום (position) הנוכחי ברשימה
            final Appointment app = appointments.get(position);

            // הצגת התאריך והשעה של התור בתוך רכיב הטקסט המתאים בשורה
            holder.tvDateTime.setText(app.getDate() + " | " + app.getTime());

            // ניהול הצגת שם העסק (אופטימיזציה: אם השם כבר שמור באובייקט נציג אותו, אם לא - נשלוף אותו מהענן)
            if (app.getBusinessName() != null && !app.getBusinessName().isEmpty()) {
                holder.tvBusinessName.setText(app.getBusinessName());
            } else {
                holder.tvBusinessName.setText("טוען..."); // הצגת טקסט זמני בזמן השליפה
                if (app.getBusinessId() != null) {
                    // פנייה חד פעמית ל-Firestore כדי להביא את מסמך המידע של העסק הספציפי הזה
                    db.collection("businesses").document(app.getBusinessId()).get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() { // מאזין להצלחת השליפה
                                @Override
                                public void onSuccess(DocumentSnapshot ds) {
                                    if (ds.exists()) {
                                        String name = ds.getString("businessName"); // שליפת מחרוזת השם מתוך המסמך
                                        holder.tvBusinessName.setText(name); // עדכון הטקסט בשורה בזמן אמת
                                        app.setBusinessName(name); // שמירת השם באובייקט המקומי כדי שלא נצטרך לשלוף שוב בגלילה הבאה
                                    }
                                }
                            });
                }
            }

            // קביעת עיצוב, טקסט וצבע לרכיב הסטטוס לפי מצב התור (PENDING, APPROVED, REJECTED)
            String status = app.getStatus() != null ? app.getStatus() : "PENDING";
            switch (status) {
                case "PENDING":
                    holder.tvStatus.setText("ממתין לאישור");
                    holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // צבע כתום לממתין
                    break;
                case "APPROVED":
                    holder.tvStatus.setText("✔ התור אושר!");
                    holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // צבע ירוק למאושר
                    break;
                case "REJECTED":
                    holder.tvStatus.setText("❌ נדחה");
                    holder.tvStatus.setTextColor(Color.RED); // צבע אדום לנדחה
                    break;
                default:
                    holder.tvStatus.setText(status);
                    holder.tvStatus.setTextColor(Color.GRAY);
            }

            long appointmentTimeInMillis = calculateMillis(app);
            boolean isPast = appointmentTimeInMillis < System.currentTimeMillis(); // בדיקה חוזרת: האם התור כבר עבר ביחס לרגע זה?

            // לוגיקת נראות כפתור דירוג: יוצג אך ורק אם התור אושר, עבר זמנו, והמשתמש טרם דירג אותו
            if ("APPROVED".equals(status) && !app.getIsReviewed() && isPast) {
                holder.btnRate.setVisibility(View.VISIBLE); // הפיכת הכפתור לנראה על המסך
                holder.btnRate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddReviewDialog(app); // פתיחת דיאלוג הדירוג בלחיצה
                    }
                });
            } else {
                holder.btnRate.setVisibility(View.GONE); // הסתרת הכפתור מהמסך לחלוטין אם התנאים לא מתקיימים
            }

            // לוגיקת נראות כפתור ביטול: לקוח יכול לבטל תור רק אם מדובר בתור עתידי
            if (isPast) {
                holder.btnDelete.setVisibility(View.GONE); // הסתרת כפתור הביטול אם התור כבר עבר
            } else {
                holder.btnDelete.setVisibility(View.VISIBLE); // הצגת כפתור ביטול לתור עתידי
                holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // בנייה והצגה של חלונית התראה (AlertDialog) כדי לוודא שהמשתמש באמת רוצה לבטל
                        new AlertDialog.Builder(getContext())
                                .setTitle("ביטול תור")
                                .setMessage("האם אתה בטוח שברצונך לבטל את התור הזה?")
                                .setPositiveButton("כן, בטל", new DialogInterface.OnClickListener() { // כפתור אישור
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteAppointment(app); // קריאה לפונקציית המחיקה מ-Firestore
                                    }
                                })
                                .setNegativeButton("לא", null) // כפתור ביטול (סוגר את הדיאלוג בלי לעשות כלום)
                                .show(); // פקודה חיונית שמציגה את הדיאלוג בפועל על המסך
                    }
                });
            }
        }

        @Override
        public int getItemCount() { return appointments.size(); } // מחזירה ל-RecyclerView כמה שורות סך הכל יש לצייר

        // מחלקת עזר פנימית שמחזיקה ומקשרת את רכיבי ה-XML של שורה בודדת
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvBusinessName, tvDateTime, tvStatus;
            CardView btnDelete;
            Button btnRate;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                // ביצוע הקישורים לרכיבים הגרפיים מתוך ה-layout של השורה בודדת
                tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                btnRate = itemView.findViewById(R.id.btnRate);
            }
        }
    }

    /**
     * קלט: אובייקט מסוג Appointment (התור שרוצים למחוק).
     * פלט: אין פלט (void).
     * מה עושה ואיך: מוחקת את מסמך התור מ-Firestore ומפעילה כוונת (Intent) לשליחת SMS אוטומטי לעסק.
     */
    private void deleteAppointment(final Appointment app) {
        if (app == null || app.getAppointmentId() == null) return;

        // פנייה למסמך התור הספציפי באוסף ומחיקתו מ-Firestore
        db.collection("appointments").document(app.getAppointmentId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() { // הפעלת קוד זה במידה והמחיקה הצליחה בענן
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getContext(), "התור בוטל בהצלחה", Toast.LENGTH_SHORT).show();

                        if (app.getBusinessId() != null) {
                            // שליפת מספר הטלפון של העסק כדי שנוכל לשלוח לו הודעה על הביטול
                            db.collection("businesses").document(app.getBusinessId()).get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot doc) {
                                            String businessPhone = doc.getString("phone"); // קבלת מספר הטלפון מהמסמך
                                            String msg = "הלקוח " + app.getUserName() + " ביטל את התור שנקבע ל-" + app.getDate() + " בשעה " + app.getTime();

                                            // יצירת מערכת כוונות (Intent) לפתיחת אפליקציית ה-SMS החיצונית של המכשיר
                                            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                                            smsIntent.setData(Uri.parse("smsto:" + (businessPhone != null ? businessPhone : ""))); // הגדרת היעד (טלפון)
                                            smsIntent.putExtra("sms_body", msg); // השתלת תוכן ההודעה המוכנה מראש

                                            try {
                                                startActivity(smsIntent); // מעבר פיזי מאפליקציית הנדסת התוכנה שלנו אל אפליקציית ה-SMS
                                            } catch (Exception e) {
                                                if (isAdded() && getContext() != null) {
                                                    Toast.makeText(getContext(), "לא נמצאה אפליקציית SMS במכשיר", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() { // הפעלת קוד זה במידה והייתה שגיאת תקשורת במחיקה
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "שגיאה בביטול: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * קלט: אובייקט מסוג Appointment (התור שעבורו משאירים ביקורת).
     * פלט: אין פלט (void).
     * מה עושה ואיך: מציגה חלונית דיאלוג מותאמת אישית (AlertDialog) עם שלושה רכיבי דירוג (RatingBar) ושדה טקסט.
     */
    private void showAddReviewDialog(final Appointment app) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // ניפוח קובץ ה-XML המותאם אישית שעיצבנו עבור חלונית הדירוג
        View view = LayoutInflater.from(getContext()).inflate(R.layout.activity_dialog_add_review, null);
        builder.setView(view); // השתלת התצוגה המנופחת לתוך הדיאלוג

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            // הפיכת רקע חלון הדיאלוג לשקוף כדי שהעיצוב המעוגל שלנו ב-XML יעבוד יפה
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // קישור רכיבי הדירוג ושדות הטקסט מתוך ה-View המנופח של הדיאלוג
        final RatingBar rbProfessionalism = view.findViewById(R.id.rbProfessionalism);
        final RatingBar rbReliability = view.findViewById(R.id.rbReliability);
        final RatingBar rbPrice = view.findViewById(R.id.rbPrice);
        final EditText etComment = view.findViewById(R.id.etComment);
        Button btnSubmit = view.findViewById(R.id.btnSubmitReview);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // שליפת הערכים המספריים (מספר הכוכבים שנבחרו) מרכיבי הדירוג
                final float ratingProf = rbProfessionalism.getRating();
                final float ratingRel = rbReliability.getRating();
                final float ratingPrice = rbPrice.getRating();
                String comment = etComment.getText().toString().trim(); // שליפת הביקורת הכתובה וניקוי רווחים מיותרים

                // וולידציה (בדיקת תקינות): חובה על המשתמש לבחור לפחות כוכב אחד בכל קטגוריה
                if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
                    Toast.makeText(getContext(), "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
                    return; // עצירת הפונקציה כדי שהטופס הלא תקין לא יישלח לענן
                }

                // יצירת מזהה (ID) ייחודי וחדש עבור מסמך הביקורת שעומד להיווצר ב-Firestore
                final String reviewId = db.collection("reviews").document().getId();

                // יצירת אובייקט מודל חדש של ביקורת ומילוי כל הנתונים השלופים
                ReviewModel newReview = new ReviewModel(
                        reviewId, app.getBusinessId(), app.getUserId(), app.getUserName(),
                        comment, app.getAppointmentId(), ratingProf, ratingRel, ratingPrice,
                        com.google.firebase.Timestamp.now() // הזנת זמן יצירת הביקורת הנוכחי מהשרת
                );

                // שמירת אובייקט הביקורת בתוך האוסף "reviews" בשרת תחת ה-ID שייצרנו
                db.collection("reviews").document(reviewId).set(newReview)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // 1. עדכון שדה הבוליאני במסמך התור ל-true, כדי שלא יוצג לדירוג שוב בטעות
                                db.collection("appointments").document(app.getAppointmentId())
                                        .update("isReviewed", true);

                                // 2. תחילת אלגוריתם שקלול ממוצע הדירוגים במסמך העסק בענן
                                if (app.getBusinessId() != null) {
                                    db.collection("businesses").document(app.getBusinessId()).get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot doc) {
                                                    if (doc.exists()) {
                                                        BusinessModel b = doc.toObject(BusinessModel.class);
                                                        if (b != null) {
                                                            int oldTotal = b.getTotalReviews(); // מספר המדרגים שהיו עד עכשיו
                                                            int newTotal = oldTotal + 1; // הוספת המדרג הנוכחי לסך הכל

                                                            // חישוב מתמטי משוקלל: (ממוצע קודם כפול כמות מדרגים קודמת + הציון החדש) חלקי כמות המדרגים החדשה
                                                            float newProf = ((b.getAvgProfessionalism() * oldTotal) + ratingProf) / newTotal;
                                                            float newRel = ((b.getAvgReliability() * oldTotal) + ratingRel) / newTotal;
                                                            float newPrice = ((b.getAvgPrice() * oldTotal) + ratingPrice) / newTotal;

                                                            // עדכון ערכי הדירוג החדשים ומספר המדרגים המעודכן ישירות בתוך מסמך העסק ב-Firestore
                                                            db.collection("businesses").document(app.getBusinessId())
                                                                    .update(
                                                                            "avgProfessionalism", newProf,
                                                                            "avgReliability", newRel,
                                                                            "avgPrice", newPrice,
                                                                            "totalReviews", newTotal
                                                                    );
                                                        }
                                                    }
                                                }
                                            });
                                }

                                Toast.makeText(getContext(), "תודה על הדירוג!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss(); // סגירת חלונית הדיאלוג והעלמתה מהמסך בסיום מוצלח
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getContext(), "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        dialog.show(); // פקודה המציגה את חלון הדיאלוג המובנה ללקוח על גבי המסך
    }
}