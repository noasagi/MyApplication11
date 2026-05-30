package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.Date;

// מחלקת אקטיביטי המנהלת את ממשק הזנת הביקורת והדירוג על ידי הלקוח, ומעדכנת את מסד הנתונים בענן
public class DialogAddReviewActivity extends BaseActivity {

    // הצהרה על רכיבי ממשק המשתמש (רכיבי כוכבי דירוג, תיבת טקסט וכפתור שליחה)
    private RatingBar rbProfessionalism, rbReliability, rbPrice;
    private EditText etComment;
    private Button btnSubmitReview;

    // מופעי הגישה לרכיבי האימות (Auth) ובסיס הנתונים (Firestore) של פיירבייס
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // משתני מחרוזת לאחסון מזהי התור, בית העסק ושם העסק המועברים למסך זה
    private String appointmentId, businessId, businessName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת עיצוב ה-XML הייעודי של מסך הדיאלוג להוספת ביקורת
        setContentView(R.layout.activity_dialog_add_review);

        // אתחול מופעי העבודה מול פיירבייס
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // שליפת הנתונים והמזהים שהועברו במסגרת ה-Intent שהפתח את המסך הנוכחי
        if (getIntent() != null) {
            appointmentId = getIntent().getStringExtra("appointmentId");
            businessId = getIntent().getStringExtra("businessId");
            businessName = getIntent().getStringExtra("businessName");
        }

        // קישור משתני רכיבי הממשק הגרפיים לקובץ ה-XML על פי המזהים שלהם
        rbProfessionalism = findViewById(R.id.rbProfessionalism);
        rbReliability = findViewById(R.id.rbReliability);
        rbPrice = findViewById(R.id.rbPrice);
        etComment = findViewById(R.id.etComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);

        // הגדרת מאזין לחיצה אנונימי מסורתי לכפתור שליחת הביקורת (במקום למדא)
        btnSubmitReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // הפעלת הפונקציה המרכזית המבצעת את תהליך השמירה והחישובים
                submitReview();
            }
        });
    }

    // פונקציית הליבה המנהלת את איסוף הנתונים, בדיקות התקינות והרצת הטרנזקציה מול הענן
    private void submitReview() {
        // שליפת ערכי הכוכבים שנבחרו על ידי הלקוח בכל אחת משלוש הקטגוריות
        final float ratingProf = rbProfessionalism.getRating();
        final float ratingRel = rbReliability.getRating();
        final float ratingPrice = rbPrice.getRating();
        // שליפת הטקסט המילולי שנכתב בתיבת הביקורת וניקוי רווחים מיותרים מהקצוות
        final String comment = etComment.getText().toString().trim();

        // תנאי הגנה: וידוא שהלקוח העניק דירוג (לפחות כוכב אחד) בכל אחת משלוש הקטגוריות
        if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
            Toast.makeText(this, "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
            return; // עצירת הפונקציה במידה ואחת הקטגוריות לא דורגה
        }

        // תנאי בטיחות: וידוא שיש משתמש מחובר למערכת ושמזהה בית העסק קיים ותקין
        if (auth.getCurrentUser() == null || businessId == null) return;

        // נטרול כפתור השליחה כדי למנוע מצב בו הלקוח לוחץ פעמיים ומייצר כפילויות במסד
        btnSubmitReview.setEnabled(false);
        // שליפת מזהה המשתמש (UID) הייחודי של הלקוח המחובר
        String userId = auth.getCurrentUser().getUid();

        // שלב מקדים: פנייה לאוסף המשתמשים כדי לחלץ את שם הלקוח העדכני שישמר בתוך מסמך הביקורת
        db.collection("users").document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot userDoc) {
                // ניסיון שליפת השם משדה "name"
                String userName = userDoc.getString("name");
                // במידה והשדה ריק, ניסיון שליפת השם משדה הגיבוי "fullName"
                if (userName == null) userName = userDoc.getString("fullName");
                // במידה ושני השדות ריקים, הגדרת שם ברירת מחדל
                if (userName == null) userName = "משתמש אנונימי";

                final String finalUserName = userName;

                // יצירת הפניות (References) למסמכים הרלוונטיים שיעודכנו כחלק בלתי נפרד מהטרנזקציה
                DocumentReference businessRef = db.collection("businesses").document(businessId);
                DocumentReference appointmentRef = db.collection("appointments").document(appointmentId);
                DocumentReference reviewRef = db.collection("reviews").document(); // הפנייה ליצירת מזהה ייחודי חדש לביקורת

                // הרצה של טרנזקציה (Transaction) אטומית המבטיחה עדכון בו זמני ומניעת התנגשויות נתונים בענן
                db.runTransaction(new Transaction.Function<Void>() {
                    @Override
                    public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                        // קריאה ולקיחת תמונת מצב (Snapshot) נוכחית של מסמך בית העסק מתוך הענן
                        DocumentSnapshot businessSnap = transaction.get(businessRef);

                        // 1. קריאת המדדים הקיימים וחישוב הממוצעים המשוקללים החדשים של העסק
                        long totalReviews = 0;
                        if (businessSnap.contains("totalReviews")) {
                            totalReviews = businessSnap.getLong("totalReviews");
                        }

                        float oldProf = 0, oldRel = 0, oldPrice = 0;
                        if (businessSnap.contains("avgProfessionalism")) oldProf = businessSnap.getDouble("avgProfessionalism").floatValue();
                        if (businessSnap.contains("avgReliability")) oldRel = businessSnap.getDouble("avgReliability").floatValue();
                        if (businessSnap.contains("avgPrice")) oldPrice = businessSnap.getDouble("avgPrice").floatValue();

                        // חישוב כמות הביקורות הכוללת החדשה (הוספת הביקורת הנוכחית)
                        long newTotal = totalReviews + 1;

                        // אלגוריתם עדכון ממוצע נע ללא אובדן מידע: מכפילים ממוצע ישן בכמות ישנה, מוסיפים את הציון החדש ומחלקים בכמות החדשה
                        float newProf = ((oldProf * totalReviews) + ratingProf) / newTotal;
                        float newRel = ((oldRel * totalReviews) + ratingRel) / newTotal;
                        float newPrice = ((oldPrice * totalReviews) + ratingPrice) / newTotal;

                        // 2. פקודת העדכון של מסמך בית העסק בתוך הטרנזקציה
                        transaction.update(businessRef,
                                "totalReviews", newTotal,
                                "avgProfessionalism", newProf,
                                "avgReliability", newRel,
                                "avgPrice", newPrice);

                        // 3. יצירת אובייקט מודל הביקורת המלא והזרקתו למסד הנתונים
                        ReviewModel review = new ReviewModel(
                                reviewRef.getId(), businessId, userId, finalUserName,
                                comment, appointmentId, ratingProf, ratingRel, ratingPrice,
                                new Timestamp(new Date())
                        );
                        transaction.set(reviewRef, review);

                        // 4. עדכון שדה החיווי בתור המקורי כדי לסמן שהוא כבר דורג ולא ניתן לדרגו שוב
                        transaction.update(appointmentRef, "isReviewed", true);

                        return null; // סיום מוצלח של פעולות הטרנזקציה
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // חיווי למשתמש על הצלחת התהליך כולו וסגירת חלון האקטיביטי וחזרה למסך הקודם
                        Toast.makeText(DialogAddReviewActivity.this, "הביקורת פורסמה בהצלחה!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // במקרה של כישלון - החזרת הכפתור למצב פעיל והצגת הודעת השגיאה שנתקבלה מהשרת
                        btnSubmitReview.setEnabled(true);
                        Toast.makeText(DialogAddReviewActivity.this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}