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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusinessDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvType, tvPhone, tvDescription, tvAddress;
    private LinearLayout galleryContainer;
    private FloatingActionButton btnFavorite;
    private Button btnWhatsApp, btnWaze, btnBookAppointment;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String currentBusinessId;
    private BusinessModel currentBusiness;
    private boolean isFavorite = false;

    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private List<ReviewModel> reviewsList;

    private String currentUserName = "אורח";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_details);

        tvName = findViewById(R.id.tvDetailName);
        tvType = findViewById(R.id.tvDetailType);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvAddress = findViewById(R.id.tvDetailAddress);
        galleryContainer = findViewById(R.id.galleryContainer);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnWhatsApp = findViewById(R.id.btnWhatsApp);
        btnWaze = findViewById(R.id.btnWaze);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        rvReviews = findViewById(R.id.rvReviewsList);

        rvReviews.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        reviewsList = new java.util.ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewsList);
        rvReviews.setAdapter(reviewAdapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        currentBusinessId = getIntent().getStringExtra("BUSINESS_ID");

        if (currentBusinessId != null) {
            loadBusinessData(currentBusinessId);
            checkIfFavorite();
            loadReviews(currentBusinessId);
        } else {
            Toast.makeText(this, "שגיאה בטעינת העסק", Toast.LENGTH_SHORT).show();
            finish();
        }

        fetchCurrentUserName();

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        btnBookAppointment.setOnClickListener(v -> {
            if (currentBusinessId == null || currentBusinessId.isEmpty()) {
                Toast.makeText(this, "אנא המתן לטעינת הנתונים...", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(BusinessDetailsActivity.this, BookingActivity.class);
            intent.putExtra("businessId", currentBusinessId);
            intent.putExtra("businessName", tvName.getText().toString());

            startActivity(intent);
        });
    }

    private void fetchCurrentUserName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                currentUserName = name;
                            }
                        }
                    });
        }
    }

    private void loadBusinessData(String businessId) {
        db.collection("businesses").document(businessId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentBusiness = documentSnapshot.toObject(BusinessModel.class);
                        if (currentBusiness != null) {
                            updateUI(currentBusiness);
                        }
                    } else {
                        Toast.makeText(this, "העסק לא נמצא", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(BusinessModel business) {
        tvName.setText(business.getName());
        tvType.setText(business.getBusinessType());
        tvPhone.setText(business.getPhone());
        tvDescription.setText(business.getDescription());

        String address = business.getAddress();
        if (address != null && !address.trim().isEmpty()) {
            tvAddress.setText(address);

            if (address.contains("מגיע") || address.contains("לקוח")) {
                btnWaze.setVisibility(View.GONE);
            } else {
                btnWaze.setVisibility(View.VISIBLE);
                btnWaze.setOnClickListener(v -> {
                    try {
                        String url = "waze://?q=" + Uri.encode(address);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception ex) {
                        String url = "https://waze.com/ul?q=" + Uri.encode(address);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
                });
            }
        } else {
            tvAddress.setText("לא צוינה כתובת");
            btnWaze.setVisibility(View.GONE);
        }

        tvPhone.setOnClickListener(v -> {
            String phoneNumber = business.getPhone();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "לא ניתן לפתוח חייגן", Toast.LENGTH_SHORT).show();
                }
            }
        });

        galleryContainer.removeAllViews();
        List<Blob> blobs = business.getImageBlobs();
        if (blobs != null && !blobs.isEmpty()) {
            for (Blob blob : blobs) {
                addImageToGallery(blob);
            }
        }

        btnWhatsApp.setOnClickListener(v -> {
            String phone = business.getPhone();

            if (phone != null && !phone.isEmpty()) {
                String cleanPhone = phone.replaceAll("[^0-9]", "");
                if (cleanPhone.startsWith("0")) {
                    cleanPhone = "972" + cleanPhone.substring(1);
                }
                String message = "היי, הגעתי דרך האפליקציה JOBSY ואשמח לשמוע פרטים!";
                try {
                    String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + message;
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "שגיאה בפתיחת WhatsApp", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "לא קיים מספר טלפון לעסק זה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            btnFavorite.hide();
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("favorites").document(currentBusinessId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isFavorite = documentSnapshot.exists();
                    updateFavoriteIcon();
                });
    }

    private void toggleFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "יש להתחבר כדי לשמור מועדפים", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference favRef = db.collection("users").document(user.getUid())
                .collection("favorites").document(currentBusinessId);

        if (isFavorite) {
            favRef.delete().addOnSuccessListener(aVoid -> {
                isFavorite = false;
                updateFavoriteIcon();
                Toast.makeText(this, "הוסר מהמועדפים", Toast.LENGTH_SHORT).show();
            });
        } else {
            Map<String, Object> favData = new HashMap<>();
            favData.put("businessId", currentBusinessId);
            favData.put("name", currentBusiness.getName());
            favData.put("type", currentBusiness.getBusinessType());

            favRef.set(favData).addOnSuccessListener(aVoid -> {
                isFavorite = true;
                updateFavoriteIcon();
                Toast.makeText(this, "נוסף למועדפים!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#FFD700"));
        } else {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
            btnFavorite.clearColorFilter();
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
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewsList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        ReviewModel review = doc.toObject(ReviewModel.class);
                        if (review != null) {
                            reviewsList.add(review);
                        }
                    }
                    reviewAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת ביקורות", Toast.LENGTH_SHORT).show();
                });
    }
}