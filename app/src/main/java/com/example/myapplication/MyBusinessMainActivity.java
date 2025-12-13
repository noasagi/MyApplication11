package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyBusinessMainActivity extends BaseActivity {

    private LinearLayout previewContainer;
    private TextView tvNoImages, tVTitle;
    private Button btnChooseImage, btnTakePhoto, btnSaveBusiness, btnDeleteBusiness;
    private EditText eTBusinessName, eTBusinessPhone, eTBusinessDescription;
    private AutoCompleteTextView autoBusinessType;

    private FirebaseAuth auth;
    private FirebaseFirestore firebaseFirestore;

    // רשימות תמונות
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<Bitmap> selectedCameraBitmaps = new ArrayList<>();

    // ניהול מצב (האם יוצרים או עורכים?)
    private boolean isEditMode = false;
    private String currentBusinessId = null;

    private final int REQUEST_CAMERA_PERMISSION = 100;
    private final int REQUEST_STORAGE_PERMISSION = 101;

    private final ActivityResultLauncher<String> mGetMultipleContent =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), result -> {
                if (result != null && !result.isEmpty()) {
                    selectedImageUris.addAll(result);
                    refreshImagePreviews();
                }
            });

    private final ActivityResultLauncher<Void> mTakePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
                if (result != null) {
                    selectedCameraBitmaps.add(result);
                    refreshImagePreviews();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_business_main);

        // חיבור ל-XML
        tVTitle = findViewById(R.id.tVTitle);
        previewContainer = findViewById(R.id.previewContainer);
        tvNoImages = findViewById(R.id.tvNoImages);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSaveBusiness = findViewById(R.id.btnSaveBusiness);
        btnDeleteBusiness = findViewById(R.id.btnDeleteBusiness);
        eTBusinessName = findViewById(R.id.eTBusinessName);
        eTBusinessPhone = findViewById(R.id.eTBusinessPhone);
        eTBusinessDescription = findViewById(R.id.eTBusinessDescription);
        autoBusinessType = findViewById(R.id.autoBusinessType);

        // סוגי עסקים
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.business_types));
        autoBusinessType.setAdapter(adapter);
        autoBusinessType.setOnClickListener(v -> autoBusinessType.showDropDown());

        auth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // בדיקה אוטומטית: האם למשתמש הזה כבר יש עסק?
        checkIfUserHasBusiness();

        // מאזינים
        btnChooseImage.setOnClickListener(v -> {
            if (checkStoragePermission()) mGetMultipleContent.launch("image/*");
            else requestStoragePermission();
        });

        btnTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) mTakePhoto.launch(null);
            else requestCameraPermission();
        });

        btnSaveBusiness.setOnClickListener(v -> saveOrUpdateBusiness());

        btnDeleteBusiness.setOnClickListener(v -> showDeleteConfirmation());
    }

    // --- לוגיקה לזיהוי עסק קיים ---
    private void checkIfUserHasBusiness() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("בודק נתונים...");
        pd.show();

        // חיפוש עסק לפי ה-ID של בעל העסק (המשתמש המחובר)
        firebaseFirestore.collection("businesses")
                .whereEqualTo("ownerId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pd.dismiss();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // נמצא עסק! עוברים למצב עריכה
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        BusinessModel business = document.toObject(BusinessModel.class);
                        if (business != null) {
                            switchToEditMode(business);
                        }
                    } else {
                        // לא נמצא עסק - נשארים במצב יצירה
                        tVTitle.setText("יצירת עסק חדש");
                        btnDeleteBusiness.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "שגיאה בבדיקת נתונים", Toast.LENGTH_SHORT).show();
                });
    }

    private void switchToEditMode(BusinessModel business) {
        isEditMode = true;
        currentBusinessId = business.getBusinessId();

        // עדכון כותרות
        tVTitle.setText("עריכת העסק שלי");
        btnSaveBusiness.setText("עדכן פרטים");
        btnDeleteBusiness.setVisibility(View.VISIBLE); // מציג את כפתור המחיקה

        // מילוי שדות
        eTBusinessName.setText(business.getName());
        eTBusinessPhone.setText(business.getPhone());
        eTBusinessDescription.setText(business.getDescription());
        autoBusinessType.setText(business.getBusinessType(), false);

        // טעינת תמונות קיימות
        if (business.getImageBlobs() != null) {
            for (Blob blob : business.getImageBlobs()) {
                byte[] bytes = blob.toBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                selectedCameraBitmaps.add(bitmap); // מוסיף לרשימת העריכה
            }
            refreshImagePreviews();
        }
    }

    // --- שמירה / עדכון ---
    private void saveOrUpdateBusiness() {
        String name = eTBusinessName.getText().toString().trim();
        String phone = eTBusinessPhone.getText().toString().trim();
        String description = eTBusinessDescription.getText().toString().trim();
        String businessType = autoBusinessType.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || description.isEmpty() ||
                (selectedImageUris.isEmpty() && selectedCameraBitmaps.isEmpty()) || businessType.isEmpty()) {
            Toast.makeText(this, "נא למלא הכל ולהוסיף תמונה", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(isEditMode ? "מעדכן..." : "יוצר עסק...");
        pd.setMessage("אנא המתן");
        pd.show();

        String businessIdToSave = isEditMode ? currentBusinessId : UUID.randomUUID().toString();
        String ownerId = auth.getCurrentUser().getUid();

        new Thread(() -> {
            try {
                List<Blob> imageBlobs = processImages();

                if (imageBlobs == null) { // תמונות גדולות מדי
                    runOnUiThread(() -> {
                        pd.dismiss();
                        Toast.makeText(this, "התמונות כבדות מדי", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Map<String, Object> businessData = new HashMap<>();
                businessData.put("businessId", businessIdToSave);
                businessData.put("ownerId", ownerId);
                businessData.put("name", name);
                businessData.put("description", description);
                businessData.put("phone", phone);
                businessData.put("businessType", businessType);
                businessData.put("imageBlobs", imageBlobs);

                firebaseFirestore.collection("businesses").document(businessIdToSave)
                        .set(businessData)
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            pd.dismiss();
                            Toast.makeText(this, isEditMode ? "עודכן בהצלחה!" : "נוצר בהצלחה!", Toast.LENGTH_SHORT).show();
                            finish();
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            pd.dismiss();
                            Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }));

            } catch (Exception e) {
                runOnUiThread(pd::dismiss);
            }
        }).start();
    }

    // --- מחיקה ---
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת עסק")
                .setMessage("האם את בטוחה? הפעולה לא ניתנת לביטול.")
                .setPositiveButton("מחק", (dialog, which) -> deleteBusiness())
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteBusiness() {
        if (currentBusinessId == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מוחק...");
        pd.show();

        firebaseFirestore.collection("businesses").document(currentBusinessId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    Toast.makeText(this, "העסק נמחק", Toast.LENGTH_SHORT).show();

                    // איפוס המסך למצב "יצירה"
                    isEditMode = false;
                    currentBusinessId = null;
                    tVTitle.setText("יצירת עסק חדש");
                    btnSaveBusiness.setText("שמור עסק");
                    btnDeleteBusiness.setVisibility(View.GONE);
                    eTBusinessName.setText("");
                    eTBusinessPhone.setText("");
                    eTBusinessDescription.setText("");
                    autoBusinessType.setText("");
                    selectedImageUris.clear();
                    selectedCameraBitmaps.clear();
                    refreshImagePreviews();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
                });
    }

    // --- עזרים ---
    private List<Blob> processImages() {
        List<Blob> imageBlobs = new ArrayList<>();
        long totalBytes = 0;

        try {
            for (Uri uri : selectedImageUris) {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                byte[] data = compressBitmap(bitmap);
                totalBytes += data.length;
                imageBlobs.add(Blob.fromBytes(data));
            }
            for (Bitmap bitmap : selectedCameraBitmaps) {
                byte[] data = compressBitmap(bitmap);
                totalBytes += data.length;
                imageBlobs.add(Blob.fromBytes(data));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }

        if (totalBytes > 950 * 1024) return null; // חריגה מ-1MB
        return imageBlobs;
    }

    private byte[] compressBitmap(Bitmap bitmap) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 800, 800, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return baos.toByteArray();
    }

    private void refreshImagePreviews() {
        previewContainer.removeAllViews();
        if (selectedImageUris.isEmpty() && selectedCameraBitmaps.isEmpty()) {
            previewContainer.addView(tvNoImages);
            return;
        }
        for (Uri uri : selectedImageUris) addPreview(uri, null);
        for (Bitmap bmp : selectedCameraBitmaps) addPreview(null, bmp);
    }

    private void addPreview(Uri uri, Bitmap bitmap) {
        ImageView iv = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
        params.setMargins(8, 0, 8, 0);
        iv.setLayoutParams(params);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (uri != null) iv.setImageURI(uri);
        else iv.setImageBitmap(bitmap);
        previewContainer.addView(iv);
    }

    // הרשאות
    private boolean checkCameraPermission() { return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED; }
    private void requestCameraPermission() { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION); }
    private boolean checkStoragePermission() { return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED; }
    private void requestStoragePermission() { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION); }
}