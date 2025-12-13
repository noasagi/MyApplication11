package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BusinessDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvType, tvPhone, tvDescription;
    private LinearLayout galleryContainer;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_details);

        // חיבור לרכיבים ב-XML
        tvName = findViewById(R.id.tvDetailName);
        tvType = findViewById(R.id.tvDetailType);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvDescription = findViewById(R.id.tvDetailDescription);
        galleryContainer = findViewById(R.id.galleryContainer);

        db = FirebaseFirestore.getInstance();

        // קבלת ה-ID מהדף הקודם
        String businessId = getIntent().getStringExtra("BUSINESS_ID");

        if (businessId != null) {
            loadBusinessData(businessId);
        } else {
            Toast.makeText(this, "שגיאה בטעינת העסק", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadBusinessData(String businessId) {
        db.collection("businesses").document(businessId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        BusinessModel business = documentSnapshot.toObject(BusinessModel.class);
                        if (business != null) {
                            updateUI(business);
                        }
                    } else {
                        Toast.makeText(this, "העסק לא נמצא", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בחיבור לשרת", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(BusinessModel business) {
        tvName.setText(business.getName());
        tvType.setText(business.getBusinessType());
        tvPhone.setText(business.getPhone());
        tvDescription.setText(business.getDescription());

        // --- הופכים את הטלפון ללחיץ (חייגן) ---
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

        // איפוס הגלריה
        galleryContainer.removeAllViews();

        // טעינת תמונות
        List<Blob> blobs = business.getImageBlobs();
        if (blobs != null && !blobs.isEmpty()) {
            for (Blob blob : blobs) {
                addImageToGallery(blob);
            }
        }
    }

    private void addImageToGallery(Blob blob) {
        byte[] bytes = blob.toBytes();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                600 // גובה התמונה בפיקסלים
        );
        params.setMargins(0, 0, 0, 30); // רווח בין תמונות
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bitmap);
        galleryContainer.addView(imageView);
    }
}