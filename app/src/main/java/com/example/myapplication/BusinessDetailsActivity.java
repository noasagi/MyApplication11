package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    private TextView tvName, tvType, tvPhone, tvDescription;
    private LinearLayout galleryContainer;
    private FloatingActionButton btnFavorite; // הכפתור החדש

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String currentBusinessId;
    private BusinessModel currentBusiness; // נשמור את העסק בזיכרון לשמירה במועדפים
    private boolean isFavorite = false; // מעקב אחר המצב הנוכחי

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_details);

        tvName = findViewById(R.id.tvDetailName);
        tvType = findViewById(R.id.tvDetailType);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvDescription = findViewById(R.id.tvDetailDescription);
        galleryContainer = findViewById(R.id.galleryContainer);
        btnFavorite = findViewById(R.id.btnFavorite); // חיבור הכפתור

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        currentBusinessId = getIntent().getStringExtra("BUSINESS_ID");

        if (currentBusinessId != null) {
            loadBusinessData(currentBusinessId);
            checkIfFavorite(); // בדיקה האם כבר במועדפים
        } else {
            Toast.makeText(this, "שגיאה בטעינת העסק", Toast.LENGTH_SHORT).show();
            finish();
        }

        // לחיצה על כפתור המועדפים
        btnFavorite.setOnClickListener(v -> toggleFavorite());
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

        // טלפון לחיץ (חייגן)
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
    }

    // --- לוגיקת מועדפים ---

    private void checkIfFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            btnFavorite.hide(); // אם זה אורח, נסתיר את הכפתור
            return;
        }

        // נתיב: users -> [userID] -> favorites -> [businessID]
        db.collection("users").document(user.getUid())
                .collection("favorites").document(currentBusinessId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        isFavorite = true;
                        updateFavoriteIcon();
                    } else {
                        isFavorite = false;
                        updateFavoriteIcon();
                    }
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
            // הסרה מהמועדפים
            favRef.delete().addOnSuccessListener(aVoid -> {
                isFavorite = false;
                updateFavoriteIcon();
                Toast.makeText(this, "הוסר מהמועדפים", Toast.LENGTH_SHORT).show();
            });
        } else {
            // הוספה למועדפים
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
            // כוכב מלא
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);

            // משנה את הצבע לצהוב-זהב (#FFD700)
            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#FFD700"));
        } else {
            // כוכב ריק (או אפור)
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);

            // מנקה את הצבע הצהוב ומחזיר למקור
            btnFavorite.clearColorFilter();

            // אופציה: אם הכוכב הריק לא נראה טוב, אפשר לצבוע אותו באפור:
            // btnFavorite.setColorFilter(android.graphics.Color.GRAY);
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
}