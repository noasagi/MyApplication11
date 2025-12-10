package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MyBusinessMainActivity extends BaseActivity {

    private ImageView imgBusinessMain;
    private Button btnChooseImage, btnTakePhoto, btnSaveBusiness;
    private EditText eTBusinessName, eTBusinessPhone, eTBusinessDescription;
    private AutoCompleteTextView autoBusinessType;

    private Uri imageUri;
    private Bitmap currentBitmap;

    private FirebaseAuth auth;
    private FirebaseFirestore firebaseFirestore;

    private final int REQUEST_CAMERA_PERMISSION = 100;
    private final int REQUEST_STORAGE_PERMISSION = 101;

    // --- בחירת תמונה מהגלריה ---
    private ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
                if (result != null) {
                    imgBusinessMain.setImageURI(result);
                    imageUri = result;
                    currentBitmap = null; // ביטול Bitmap קודם
                } else {
                    Log.d("ImagePicker", "Selection cancelled");
                }
            }
    );

    // --- צילום תמונה מהמצלמה ---
    private ActivityResultLauncher<Void> mTakePhoto = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            result -> {
                if (result != null) {
                    imgBusinessMain.setImageBitmap(result);
                    currentBitmap = result;
                    imageUri = null; // ביטול Uri קודם
                } else {
                    Log.d("Camera", "צילום בוטל");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_business_main);

        imgBusinessMain = findViewById(R.id.imgBusinessMain);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSaveBusiness = findViewById(R.id.btnSaveBusiness);
        eTBusinessName = findViewById(R.id.eTBusinessName);
        eTBusinessPhone = findViewById(R.id.eTBusinessPhone);
        eTBusinessDescription = findViewById(R.id.eTBusinessDescription);
        autoBusinessType = findViewById(R.id.autoBusinessType);

        // הגדרת AutoCompleteTextView עם רשימת סוגי העסקים
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
                mGetContent.launch("image/*");
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

    // בדיקה של הרשאת מצלמה
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    // בדיקה של הרשאת אחסון
    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_STORAGE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mTakePhoto.launch(null);
            } else {
                Toast.makeText(this, "הרשאת מצלמה נחוצה לצילום תמונה", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mGetContent.launch("image/*");
            } else {
                Toast.makeText(this, "הרשאת אחסון נחוצה לבחירת תמונה מהגלריה", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveBusiness() {
        String name = eTBusinessName.getText().toString().trim();
        String phone = eTBusinessPhone.getText().toString().trim();
        String description = eTBusinessDescription.getText().toString().trim();
        String businessType = autoBusinessType.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || description.isEmpty() ||
                (imageUri == null && currentBitmap == null) || businessType.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות, לבחור תמונה או לצלם תמונה וסוג עסק", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("שומר את העסק");
        pd.setMessage("אנא המתן...");
        pd.setCancelable(false);
        pd.show();

        String ownerId = user.getUid();
        String businessId = UUID.randomUUID().toString();

        try {
            Bitmap bitmapToSave;

            if (currentBitmap != null) {
                bitmapToSave = currentBitmap;
            } else {
                bitmapToSave = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            }

            // כיווץ התמונה ל-JPEG 60%
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();

            if (imageBytes.length > 900 * 1024) {
                pd.dismiss();
                Toast.makeText(this, "התמונה גדולה מדי. נסי לבחור תמונה קטנה יותר.", Toast.LENGTH_LONG).show();
                return;
            }

            Blob imageBlob = Blob.fromBytes(imageBytes);

            Map<String, Object> businessData = new HashMap<>();
            businessData.put("businessId", businessId);
            businessData.put("ownerId", ownerId);
            businessData.put("name", name);
            businessData.put("description", description);
            businessData.put("phone", phone);
            businessData.put("businessType", businessType);
            businessData.put("imageBlob", imageBlob);

            firebaseFirestore.collection("businesses")
                    .document(businessId)
                    .set(businessData)
                    .addOnSuccessListener(aVoid -> {
                        pd.dismiss();
                        Toast.makeText(this, "העסק נשמר בהצלחה!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(this, "שגיאה בשמירת העסק: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            pd.dismiss();
            Toast.makeText(this, "שגיאה בעיבוד התמונה: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
