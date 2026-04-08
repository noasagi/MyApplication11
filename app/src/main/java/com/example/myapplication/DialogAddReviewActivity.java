package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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

        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        final float ratingProf = rbProfessionalism.getRating();
        final float ratingRel = rbReliability.getRating();
        final float ratingPrice = rbPrice.getRating();
        final String comment = etComment.getText().toString().trim();

        if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
            Toast.makeText(this, "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null || businessId == null) return;

        btnSubmitReview.setEnabled(false); // מניעת לחיצות כפולות
        String userId = auth.getCurrentUser().getUid();

        // שליפת שם המשתמש לפני תחילת הטרנזקציה
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.getString("name");
            if (userName == null) userName = userDoc.getString("fullName");
            if (userName == null) userName = "משתמש אנונימי";

            final String finalUserName = userName;

            // התחלת טרנזקציה לעדכון הסטטיסטיקה של העסק
            DocumentReference businessRef = db.collection("businesses").document(businessId);
            DocumentReference appointmentRef = db.collection("appointments").document(appointmentId);
            DocumentReference reviewRef = db.collection("reviews").document(); // ID חדש לביקורת

            db.runTransaction(transaction -> {
                DocumentSnapshot businessSnap = transaction.get(businessRef);

                // 1. חישוב ממוצעים חדשים
                long totalReviews = 0;
                if (businessSnap.contains("totalReviews")) {
                    totalReviews = businessSnap.getLong("totalReviews");
                }

                float oldProf = 0, oldRel = 0, oldPrice = 0;
                if (businessSnap.contains("avgProfessionalism")) oldProf = businessSnap.getDouble("avgProfessionalism").floatValue();
                if (businessSnap.contains("avgReliability")) oldRel = businessSnap.getDouble("avgReliability").floatValue();
                if (businessSnap.contains("avgPrice")) oldPrice = businessSnap.getDouble("avgPrice").floatValue();

                long newTotal = totalReviews + 1;
                float newProf = ((oldProf * totalReviews) + ratingProf) / newTotal;
                float newRel = ((oldRel * totalReviews) + ratingRel) / newTotal;
                float newPrice = ((oldPrice * totalReviews) + ratingPrice) / newTotal;

                // 2. עדכון העסק
                transaction.update(businessRef,
                        "totalReviews", newTotal,
                        "avgProfessionalism", newProf,
                        "avgReliability", newRel,
                        "avgPrice", newPrice);

                // 3. יצירת אובייקט הביקורת
                ReviewModel review = new ReviewModel(
                        reviewRef.getId(), businessId, userId, finalUserName,
                        comment, appointmentId, ratingProf, ratingRel, ratingPrice,
                        new Timestamp(new Date())
                );
                transaction.set(reviewRef, review);

                // 4. עדכון התור שבוצע
                transaction.update(appointmentRef, "isReviewed", true);

                return null;
            }).addOnSuccessListener(result -> {
                Toast.makeText(this, "הביקורת פורסמה בהצלחה!", Toast.LENGTH_SHORT).show();
                finish();
            }).addOnFailureListener(e -> {
                btnSubmitReview.setEnabled(true);
                Toast.makeText(this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });
    }
}