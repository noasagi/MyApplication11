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

public class DialogAddReviewActivity extends BaseActivity {

    private RatingBar rbProfessionalism, rbReliability, rbPrice;
    private EditText etComment;
    private Button btnSubmitReview;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String appointmentId, businessId, businessName;

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי המסך, שולפת את נתוני ה-Intent (מזהה תור, מזהה עסק) ומחברת את מאזין הלחיצה לכפתור השליחה.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_add_review);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getIntent() != null) {
            appointmentId = getIntent().getStringExtra("appointmentId");
            businessId = getIntent().getStringExtra("businessId");
            businessName = getIntent().getStringExtra("businessName");
        }

        rbProfessionalism = findViewById(R.id.rbProfessionalism);
        rbReliability = findViewById(R.id.rbReliability);
        rbPrice = findViewById(R.id.rbPrice);
        etComment = findViewById(R.id.etComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);

        btnSubmitReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReview();
            }
        });
    }

    /**
     * מה הפעולה עושה: אוספת את הנתונים, ומריצה טרנזקציה אטומית (Transaction) מול Firestore המבצעת 3 פעולות בו-זמנית: חישוב הממוצע הנע של העסק, שמירת הביקורת, ונעילת התור מפני דירוג כפול.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void submitReview() {
        final float ratingProf = rbProfessionalism.getRating();
        final float ratingRel = rbReliability.getRating();
        final float ratingPrice = rbPrice.getRating();
        final String comment = etComment.getText().toString().trim();

        // הגנת קלט: וידוא שהמשתמש הציב דירוג חיובי בכל אחת משלוש הקטגוריות
        if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
            Toast.makeText(this, "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null || businessId == null) return;

        // הגנת UX: נטרול הכפתור מייד למניעת לחיצות כפולות ויצירת כפילויות במסד בזמן הריצה
        btnSubmitReview.setEnabled(false);
        String userId = auth.getCurrentUser().getUid();

        // שלב א': שליפת שם המשתמש העדכני לצורך הטמעתו הישירה (Denormalization) במסמך הביקורת
        db.collection("users").document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot userDoc) {
                String userName = userDoc.getString("name");
                if (userName == null) userName = userDoc.getString("fullName");
                if (userName == null) userName = "משתמש אנונימי";

                final String finalUserName = userName;

                DocumentReference businessRef = db.collection("businesses").document(businessId);
                DocumentReference appointmentRef = db.collection("appointments").document(appointmentId);
                DocumentReference reviewRef = db.collection("reviews").document();

                // שלב ב': הרצת טרנזקציה (קריטי למניעת מצבי מירוץ - Race Conditions בעת עדכון הממוצעים על ידי מספר משתמשים במקביל)
                db.runTransaction(new Transaction.Function<Void>() {
                    @Override
                    public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                        DocumentSnapshot businessSnap = transaction.get(businessRef);

                        long totalReviews = 0;
                        if (businessSnap.contains("totalReviews")) {
                            totalReviews = businessSnap.getLong("totalReviews");
                        }

                        float oldProf = 0, oldRel = 0, oldPrice = 0;
                        if (businessSnap.contains("avgProfessionalism")) oldProf = businessSnap.getDouble("avgProfessionalism").floatValue();
                        if (businessSnap.contains("avgReliability")) oldRel = businessSnap.getDouble("avgReliability").floatValue();
                        if (businessSnap.contains("avgPrice")) oldPrice = businessSnap.getDouble("avgPrice").floatValue();

                        long newTotal = totalReviews + 1;

                        // אלגוריתם עדכון ממוצע נע בזמן אמת ללא אובדן נתונים
                        float newProf = ((oldProf * totalReviews) + ratingProf) / newTotal;
                        float newRel = ((oldRel * totalReviews) + ratingRel) / newTotal;
                        float newPrice = ((oldPrice * totalReviews) + ratingPrice) / newTotal;

                        // 1. עדכון מסמך בית העסק עם הסטטיסטיקות והממוצעים החדשים
                        transaction.update(businessRef,
                                "totalReviews", newTotal,
                                "avgProfessionalism", newProf,
                                "avgReliability", newRel,
                                "avgPrice", newPrice);

                        // 2. יצירת מסמך הביקורת המפורט החדש
                        ReviewModel review = new ReviewModel(
                                reviewRef.getId(), businessId, userId, finalUserName,
                                comment, appointmentId, ratingProf, ratingRel, ratingPrice,
                                new Timestamp(new Date())
                        );
                        transaction.set(reviewRef, review);

                        // 3. עדכון סטטוס התור ל-isReviewed = true כדי לחסום אפשרות לדרגו פעם נוספת
                        transaction.update(appointmentRef, "isReviewed", true);

                        return null;
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(DialogAddReviewActivity.this, "הביקורת פורסמה בהצלחה!", Toast.LENGTH_SHORT).show();
                        finish(); // סגירת המסך וחזרה אוטומטית אחורה
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnSubmitReview.setEnabled(true); // שחרור הכפתור במקרה של שגיאת רשת
                        Toast.makeText(DialogAddReviewActivity.this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}