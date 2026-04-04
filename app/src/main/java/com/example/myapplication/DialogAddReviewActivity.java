package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class DialogAddReviewActivity extends BaseActivity {

    private RatingBar rbProfessionalism, rbReliability, rbPrice;
    private EditText etComment;
    private Button btnSubmitReview;
    private TextView tvTitle;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String appointmentId, businessId, businessName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_add_review);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // קבלת הנתונים מהפרגמנט
        if (getIntent() != null) {
            appointmentId = getIntent().getStringExtra("appointmentId");
            businessId = getIntent().getStringExtra("businessId");
            businessName = getIntent().getStringExtra("businessName");
        }

        // קישור רכיבים
        rbProfessionalism = findViewById(R.id.rbProfessionalism);
        rbReliability = findViewById(R.id.rbReliability);
        rbPrice = findViewById(R.id.rbPrice);
        etComment = findViewById(R.id.etComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);

        // עדכון כותרת (אופציונלי, צריך להוסיף ID ב-XML אם רוצים)
        // TextView tvTitle = findViewById(R.id.tvTitle);
        // tvTitle.setText("ביקורת על " + businessName);

        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        float ratingProf = rbProfessionalism.getRating();
        float ratingRel = rbReliability.getRating();
        float ratingPrice = rbPrice.getRating();
        String comment = etComment.getText().toString().trim();

        if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
            Toast.makeText(this, "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) return;

        // 1. קודם שולפים את שם המשתמש הנוכחי (לצורך התצוגה בביקורת)
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.getString("name");
            if (userName == null) userName = "משתמש אנונימי";

            // יצירת אובייקט הביקורת
            ReviewModel review = new ReviewModel();
            review.setBusinessId(businessId);
            review.setUserId(userId);
            review.setUserName(userName);
            review.setAppointmentId(appointmentId); // הקישור החשוב!
            review.setComment(comment);
            review.setRatingProfessionalism(ratingProf);
            review.setRatingReliability(ratingRel);
            review.setRatingPrice(ratingPrice);
            review.setTimestamp(new Timestamp(new Date()));

            // 2. שמירת הביקורת ב-Firebase
            db.collection("reviews").add(review)
                    .addOnSuccessListener(documentReference -> {
                        review.setReviewId(documentReference.getId()); // עדכון ID אם צריך

                        // 3. עדכון התור שהוא דורג (כדי שהכרטיס ייעלם מדף הבית)
                        if (appointmentId != null) {
                            db.collection("appointments").document(appointmentId)
                                    .update("isReviewed", true)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(DialogAddReviewActivity.this, "הביקורת פורסמה!", Toast.LENGTH_SHORT).show();
                                        finish(); // סגירת המסך
                                    });
                        } else {
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(DialogAddReviewActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}