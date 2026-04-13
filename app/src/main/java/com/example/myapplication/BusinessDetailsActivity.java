package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusinessDetailsActivity extends BaseActivity {

    private TextView tvName, tvType, tvPhone, tvDescription, tvAddress;
    // רכיבי הדירוג מה-XML שלך
    private RatingBar rbAvgProfessionalism, rbAvgReliability, rbAvgPrice;
    private LinearLayout galleryContainer;
    private FloatingActionButton btnFavorite;
    private Button btnWhatsApp, btnWaze, btnBookAppointment, btnAppChat;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentBusinessId;
    private BusinessModel currentBusiness;
    private boolean isFavorite = false;

    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private List<ReviewModel> reviewsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_details);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // קישור רכיבים לפי ה-XML ששלחת
        tvName = findViewById(R.id.tvDetailName);
        tvType = findViewById(R.id.tvDetailType);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvAddress = findViewById(R.id.tvDetailAddress);

        // קישור שלושת הדירוגים
        rbAvgProfessionalism = findViewById(R.id.rbAvgProfessionalism);
        rbAvgReliability = findViewById(R.id.rbAvgReliability);
        rbAvgPrice = findViewById(R.id.rbAvgPrice);

        galleryContainer = findViewById(R.id.galleryContainer);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnWhatsApp = findViewById(R.id.btnWhatsApp);
        btnWaze = findViewById(R.id.btnWaze);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnAppChat = findViewById(R.id.btnAppChat);

        rvReviews = findViewById(R.id.rvReviewsList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewsList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewsList);
        rvReviews.setAdapter(reviewAdapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentBusinessId = getIntent().getStringExtra("BUSINESS_ID");

        if (currentBusinessId != null) {
            listenToBusinessData(currentBusinessId);
            checkIfFavorite();
            loadReviews(currentBusinessId);
        }

        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnBookAppointment.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingActivity.class);
            intent.putExtra("businessId", currentBusinessId);
            intent.putExtra("businessName", tvName.getText().toString());
            startActivity(intent);
        });

        btnAppChat.setOnClickListener(v -> openChat());

        // --- התוספת החסרה: מאזינים לכפתורי וויז וווצאפ ---
        btnWhatsApp.setOnClickListener(v -> openWhatsApp());
        btnWaze.setOnClickListener(v -> openWaze());
    }

    private void listenToBusinessData(String businessId) {
        db.collection("businesses").document(businessId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        currentBusiness = doc.toObject(BusinessModel.class);
                        if (currentBusiness != null) {
                            updateUI(currentBusiness);
                        }
                    }
                });
    }

    private void updateUI(BusinessModel business) {
        tvName.setText(business.getName());
        tvType.setText(business.getBusinessType());
        tvPhone.setText(business.getPhone());
        tvDescription.setText(business.getDescription());
        tvAddress.setText(business.getAddress() != null ? business.getAddress() : "לא צוינה כתובת");

        // עדכון שלושת הדירוגים הספציפיים מהדאטה-בייס
        rbAvgProfessionalism.setRating(business.getAvgProfessionalism());
        rbAvgReliability.setRating(business.getAvgReliability());
        rbAvgPrice.setRating(business.getAvgPrice());

        // גלריה
        galleryContainer.removeAllViews();
        if (business.getImageBlobs() != null) {
            for (Blob blob : business.getImageBlobs()) {
                addImageToGallery(blob);
            }
        }
    }

    private void addImageToGallery(Blob blob) {
        byte[] bytes = blob.toBytes();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600);
        params.setMargins(0, 0, 0, 30);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bitmap);
        galleryContainer.addView(imageView);
    }

    private void loadReviews(String businessId) {
        db.collection("reviews")
                .whereEqualTo("businessId", businessId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (querySnapshot != null) {
                        reviewsList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                            ReviewModel review = doc.toObject(ReviewModel.class);
                            if (review != null) reviewsList.add(review);
                        }
                        reviewAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void openChat() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || currentBusiness == null) return;
        String chatRoomId = user.getUid() + "_" + currentBusiness.getOwnerId();
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatRoomId", chatRoomId);
        startActivity(intent);
    }

    private void checkIfFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).collection("favorites").document(currentBusinessId)
                .get().addOnSuccessListener(doc -> {
                    isFavorite = doc.exists();
                    updateFavoriteIcon();
                });
    }

    private void toggleFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        DocumentReference favRef = db.collection("users").document(user.getUid()).collection("favorites").document(currentBusinessId);
        if (isFavorite) {
            favRef.delete().addOnSuccessListener(aVoid -> { isFavorite = false; updateFavoriteIcon(); });
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("businessId", currentBusinessId);
            data.put("name", currentBusiness.getName());
            favRef.set(data).addOnSuccessListener(aVoid -> { isFavorite = true; updateFavoriteIcon(); });
        }
    }

    private void updateFavoriteIcon() {
        // ביטול הצביעה האוטומטית כדי שנראה את הצבע המקורי של הכוכב (צהוב/אפור)
        btnFavorite.setImageTintList(null);

        if (isFavorite) {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_on); // כוכב מלא
        } else {
            btnFavorite.setImageResource(android.R.drawable.star_off); // כוכב אפור/ריק
        }
    }

    // --- לוגיקת פתיחת ווצאפ ---
// --- לוגיקת פתיחת ווצאפ ---
// --- לוגיקת פתיחת ווצאפ ---
    private void openWhatsApp() {
        if (currentBusiness == null || currentBusiness.getPhone() == null || currentBusiness.getPhone().isEmpty()) {
            Toast.makeText(this, "מספר טלפון לא זמין", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. מנקים את המספר מכל תו שהוא לא ספרה
        String phone = currentBusiness.getPhone().replaceAll("\\D", "");

        // 2. קידומת ישראל
        if (phone.startsWith("0")) {
            phone = "972" + phone.substring(1);
        }

        // 3. ההודעה המוכנה מראש (את יכולה לשנות את הטקסט כאן למה שבא לך)
        String message = "שלום, הגעתי דרך אפליקציית JOBSY. אשמח לקבל פרטים נוספים!";

        // 4. קידוד הטקסט כדי שיעבוד תקין בתוך הקישור
        String encodedMessage = Uri.encode(message);

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // מוסיפים את ההודעה לקישור בעזרת ?text=
            intent.setData(Uri.parse("https://wa.me/" + phone + "?text=" + encodedMessage));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "ווצאפ לא מותקן על המכשיר", Toast.LENGTH_SHORT).show();
        }
    }

    // --- לוגיקת פתיחת וויז ---
    private void openWaze() {
        if (currentBusiness == null) return;

        String uriString;

        // קודם מנסים לנווט לפי קואורדינטות מדויקות, אם קיימות
        if (currentBusiness.getLatitude() != null && currentBusiness.getLongitude() != null) {
            uriString = "waze://?ll=" + currentBusiness.getLatitude() + "," + currentBusiness.getLongitude() + "&navigate=yes";
        }
        // אם אין קואורדינטות, ננסה לנווט לפי הכתובת הכתובה
        else if (currentBusiness.getAddress() != null && !currentBusiness.getAddress().isEmpty()) {
            uriString = "waze://?q=" + Uri.encode(currentBusiness.getAddress()) + "&navigate=yes";
        }
        else {
            Toast.makeText(this, "לא הוגדר מיקום לעסק זה", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
            startActivity(intent);
        } catch (Exception e) {
            // אם וויז לא מותקן, נפתח חנות אפליקציות
            try {
                Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.waze"));
                startActivity(playStoreIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "וויז לא מותקן על המכשיר", Toast.LENGTH_SHORT).show();
            }
        }
    }
}