package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyBusinessMainActivity extends BaseActivity {

    private LinearLayout previewContainer; // המיכל של התמונות הקטנות
    private TextView tvNoImages;
    private Button btnChooseImage, btnTakePhoto, btnSaveBusiness;
    private EditText eTBusinessName, eTBusinessPhone, eTBusinessDescription;
    private AutoCompleteTextView autoBusinessType;

    private FirebaseAuth auth;
    private FirebaseFirestore firebaseFirestore;

    private final int REQUEST_CAMERA_PERMISSION = 100;
    private final int REQUEST_STORAGE_PERMISSION = 101;

    // רשימות לתמונות
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<Bitmap> selectedCameraBitmaps = new ArrayList<>();

    // --- בחירת תמונות מהגלריה ---
    private final ActivityResultLauncher<String> mGetMultipleContent =
            registerForActivityResult(
                    new ActivityResultContracts.GetMultipleContents(),
                    result -> {
                        if (result != null && !result.isEmpty()) {
                            // אפשר להחליט אם רוצים להוסיף לרשימה הקיימת או לדרוס אותה.
                            // כאן אנחנו מוסיפים לרשימה הקיימת:
                            selectedImageUris.addAll(result);

                            // עדכון התצוגה למשתמש
                            refreshImagePreviews();

                            Log.d("ImagePicker", "Selected " + result.size() + " images");
                        }
                    }
            );

    // --- צילום תמונה ---
    private final ActivityResultLauncher<Void> mTakePhoto =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicturePreview(),
                    result -> {
                        if (result != null) {
                            selectedCameraBitmaps.add(result);
                            refreshImagePreviews();
                            Log.d("Camera", "Captured photo");
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_business_main);

        // קישור לרכיבי ה-XML
        previewContainer = findViewById(R.id.previewContainer);
        tvNoImages = findViewById(R.id.tvNoImages);

        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSaveBusiness = findViewById(R.id.btnSaveBusiness);
        eTBusinessName = findViewById(R.id.eTBusinessName);
        eTBusinessPhone = findViewById(R.id.eTBusinessPhone);
        eTBusinessDescription = findViewById(R.id.eTBusinessDescription);
        autoBusinessType = findViewById(R.id.autoBusinessType);

        // הגדרת AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.business_types)
        );
        autoBusinessType.setAdapter(adapter);
        autoBusinessType.setThreshold(0);
        autoBusinessType.setOnClickListener(v -> autoBusinessType.showDropDown());
        autoBusinessType.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) autoBusinessType.showDropDown();
        });

        auth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // --- לחצנים ---
        btnChooseImage.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                mGetMultipleContent.launch("image/*");
            } else {
                requestStoragePermission();
            }
        });

        btnTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                mTakePhoto.launch(null);
            } else {
                requestCameraPermission();
            }
        });

        btnSaveBusiness.setOnClickListener(v -> saveBusiness());
    }

    /**
     * פונקציה שמרעננת את שורת התמונות בתצוגה.
     * היא מוחקת את כל מה שיש ב-LinearLayout ויוצרת את התמונות מחדש לפי הרשימות.
     */
    private void refreshImagePreviews() {
        previewContainer.removeAllViews(); // ניקוי תצוגה קודמת

        boolean hasImages = !selectedImageUris.isEmpty() || !selectedCameraBitmaps.isEmpty();

        if (!hasImages) {
            previewContainer.addView(tvNoImages);
            return;
        }

        // הצגת תמונות מהגלריה
        for (Uri uri : selectedImageUris) {
            addImageToPreview(uri, null);
        }

        // הצגת תמונות מהמצלמה
        for (Bitmap bitmap : selectedCameraBitmaps) {
            addImageToPreview(null, bitmap);
        }
    }

    /**
     * פונקציית עזר להוספת תמונה בודדת לפס הגלילה
     */
    private void addImageToPreview(Uri uri, Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250); // גודל ריבוע התמונה
        params.setMargins(8, 0, 8, 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (uri != null) {
            imageView.setImageURI(uri);
        } else if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }

        previewContainer.addView(imageView);
    }

    // --- הרשאות ---

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
    }

    // --- שמירה לפיירבייס ---

    private void saveBusiness() {
        String name = eTBusinessName.getText().toString().trim();
        String phone = eTBusinessPhone.getText().toString().trim();
        String description = eTBusinessDescription.getText().toString().trim();
        String businessType = autoBusinessType.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || description.isEmpty() ||
                (selectedImageUris.isEmpty() && selectedCameraBitmaps.isEmpty()) ||
                businessType.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות ולבחור לפחות תמונה אחת", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("שומר את העסק");
        pd.setMessage("מעבד תמונות...");
        pd.setCancelable(false);
        pd.show();

        String ownerId = user.getUid();
        String businessId = UUID.randomUUID().toString();

        new Thread(() -> {
            try {
                List<Blob> imageBlobs = new ArrayList<>();
                long totalBytes = 0;

                // עיבוד תמונות גלריה
                for (Uri uri : selectedImageUris) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    if (bitmap != null) {
                        // הקטנת תמונה כדי לחסוך מקום (אופציונלי אך מומלץ)
                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 800, 800, true);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos); // איכות 70%
                        byte[] data = baos.toByteArray();
                        totalBytes += data.length;
                        imageBlobs.add(Blob.fromBytes(data));
                    }
                }

                // עיבוד תמונות מצלמה
                for (Bitmap cameraBitmap : selectedCameraBitmaps) {
                    if (cameraBitmap != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        cameraBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        byte[] data = baos.toByteArray();
                        totalBytes += data.length;
                        imageBlobs.add(Blob.fromBytes(data));
                    }
                }

                // בדיקת גודל (Firestore מגביל מסמך ל-1MB)
                if (totalBytes > 950 * 1024) {
                    runOnUiThread(() -> {
                        pd.dismiss();
                        Toast.makeText(this, "התמונות גדולות מדי! נסי לבחור פחות תמונות.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                Map<String, Object> businessData = new HashMap<>();
                businessData.put("businessId", businessId);
                businessData.put("ownerId", ownerId);
                businessData.put("name", name);
                businessData.put("description", description);
                businessData.put("phone", phone);
                businessData.put("businessType", businessType);
                businessData.put("imageBlobs", imageBlobs);

                firebaseFirestore.collection("businesses")
                        .document(businessId)
                        .set(businessData)
                        .addOnSuccessListener(aVoid -> {
                            runOnUiThread(() -> {
                                pd.dismiss();
                                Toast.makeText(this, "העסק נשמר בהצלחה!", Toast.LENGTH_SHORT).show();
                                finish(); // סגירת הדף לאחר שמירה
                            });
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                pd.dismiss();
                                Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, "שגיאה בעיבוד: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}