package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyBusinessMainActivity extends BaseActivity {

    private LinearLayout previewContainer;
    private TextView tvNoImages, tVTitle;
    private Button btnChooseImage, btnTakePhoto, btnSaveBusiness, btnDeleteBusiness, btnUpdateLocation;

    private EditText eTBusinessName, eTBusinessPhone, eTBusinessDescription, eTBusinessAddress;
    private AutoCompleteTextView autoBusinessType;

    private FirebaseAuth auth;
    private Button btnManageHours;
    private FirebaseFirestore firebaseFirestore;

    // רשימות תמונות
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<Bitmap> selectedCameraBitmaps = new ArrayList<>();

    // ניהול מצב (האם יוצרים או עורכים?)
    private boolean isEditMode = false;
    private String currentBusinessId = null;

    private final int REQUEST_CAMERA_PERMISSION = 100;
    private final int REQUEST_STORAGE_PERMISSION = 101;
    private final int REQUEST_LOCATION_PERMISSION = 102;

    // --- משתני מיקום ---
    private FusedLocationProviderClient fusedLocationClient;
    private Double deviceLat = null;
    private Double deviceLon = null;
    private Double businessLat = null;
    private Double businessLon = null;

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
        btnUpdateLocation = findViewById(R.id.btnUpdateLocation);

        eTBusinessName = findViewById(R.id.eTBusinessName);
        eTBusinessPhone = findViewById(R.id.eTBusinessPhone);
        eTBusinessDescription = findViewById(R.id.eTBusinessDescription);
        eTBusinessAddress = findViewById(R.id.eTBusinessAddress);

        autoBusinessType = findViewById(R.id.autoBusinessType);
        btnManageHours = findViewById(R.id.btnManageHours);

        auth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // אתחול רכיב מיקום ושליפת המיקום הנוכחי כבר בעליית המסך
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fetchDeviceLocation();

        // הגדרת כפתור שעות פעילות
        btnManageHours.setOnClickListener(v -> {
            if (currentBusinessId != null) {
                Intent intent = new Intent(MyBusinessMainActivity.this, BusinessHoursActivity.class);
                intent.putExtra("BUSINESS_ID", currentBusinessId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "יש לשמור את העסק קודם", Toast.LENGTH_SHORT).show();
            }
        });

        // כפתור עדכון מיקום ידני
        btnUpdateLocation.setOnClickListener(v -> {
            if (deviceLat != null && deviceLon != null) {
                businessLat = deviceLat;
                businessLon = deviceLon;
                Toast.makeText(this, "מיקום ה-GPS נדגם! אל תשכח ללחוץ על 'שמור שינויים'", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "מנסה לאתר מיקום... ודא שה-GPS דלוק ונסה שוב", Toast.LENGTH_SHORT).show();
                fetchDeviceLocation();
            }
        });

        // סוגי עסקים
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.business_types));
        autoBusinessType.setAdapter(adapter);
        autoBusinessType.setOnClickListener(v -> autoBusinessType.showDropDown());

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

    // --- טיפול במיקום (GPS) ---
    private void fetchDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            getLocationNow();
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocationNow() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                deviceLat = location.getLatitude();
                deviceLon = location.getLongitude();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationNow();
        }
    }

    // --- לוגיקה לזיהוי עסק קיים ---
    private void checkIfUserHasBusiness() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("בודק נתונים...");
        pd.show();

        firebaseFirestore.collection("businesses")
                .whereEqualTo("ownerId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pd.dismiss();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        BusinessModel business = document.toObject(BusinessModel.class);
                        if (business != null) {
                            switchToEditMode(business);
                        }
                    } else {
                        // מצב יצירה
                        tVTitle.setText("יצירת עסק חדש");
                        btnDeleteBusiness.setVisibility(View.GONE);
                        btnManageHours.setVisibility(View.GONE);
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

        // שמירת המיקום הקיים של העסק (כדי שלא נדרוס אותו בטעות)
        businessLat = business.getLatitude();
        businessLon = business.getLongitude();

        // עדכון כותרות וכפתורים
        tVTitle.setText("עריכת העסק שלי");
        btnSaveBusiness.setText("עדכן פרטים");
        btnDeleteBusiness.setVisibility(View.VISIBLE);
        btnManageHours.setVisibility(View.VISIBLE);

        // מילוי שדות כולל הכתובת המילולית!
        eTBusinessName.setText(business.getName());
        eTBusinessPhone.setText(business.getPhone());
        eTBusinessDescription.setText(business.getDescription());
        eTBusinessAddress.setText(business.getAddress() != null ? business.getAddress() : "");
        autoBusinessType.setText(business.getBusinessType(), false);

        // טעינת תמונות קיימות
        if (business.getImageBlobs() != null) {
            for (Blob blob : business.getImageBlobs()) {
                byte[] bytes = blob.toBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                selectedCameraBitmaps.add(bitmap);
            }
            refreshImagePreviews();
        }
    }

    // --- שמירה / עדכון ---
    private void saveOrUpdateBusiness() {
        String name = eTBusinessName.getText().toString().trim();
        String phone = eTBusinessPhone.getText().toString().trim();
        String description = eTBusinessDescription.getText().toString().trim();
        String address = eTBusinessAddress.getText().toString().trim();
        String businessType = autoBusinessType.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || description.isEmpty() || address.isEmpty() ||
                (selectedImageUris.isEmpty() && selectedCameraBitmaps.isEmpty()) || businessType.isEmpty()) {
            Toast.makeText(this, "נא למלא הכל (כולל כתובת) ולהוסיף תמונה", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(isEditMode ? "מעדכן..." : "יוצר עסק...");
        pd.setMessage("אנא המתן");
        pd.show();

        String businessIdToSave = (currentBusinessId != null) ? currentBusinessId : UUID.randomUUID().toString();
        String ownerId = auth.getCurrentUser().getUid();

        new Thread(() -> {
            try {
                List<Blob> imageBlobs = processImages();

                if (imageBlobs == null) {
                    runOnUiThread(() -> {
                        pd.dismiss();
                        Toast.makeText(this, "התמונות כבדות מדי", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
// אם לעסק עדיין אין מיקום שמור - ניקח אוטומטית את המיקום שדגמנו עכשיו
                if (businessLat == null || businessLon == null) {
                    businessLat = deviceLat;
                    businessLon = deviceLon;
                }

                Map<String, Object> businessData = new HashMap<>();
                businessData.put("businessId", businessIdToSave);
                businessData.put("ownerId", ownerId);
                businessData.put("name", name);
                businessData.put("description", description);
                businessData.put("phone", phone);
                businessData.put("address", address);
                businessData.put("businessType", businessType);
                businessData.put("imageBlobs", imageBlobs);
                businessData.put("latitude", businessLat);
                businessData.put("longitude", businessLon);

                firebaseFirestore.collection("businesses").document(businessIdToSave)
                        .set(businessData)
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            pd.dismiss();
                            Toast.makeText(this, isEditMode ? "עודכן בהצלחה!" : "נוצר בהצלחה!", Toast.LENGTH_SHORT).show();

                            isEditMode = true;
                            currentBusinessId = businessIdToSave;

                            btnManageHours.setVisibility(View.VISIBLE);
                            btnDeleteBusiness.setVisibility(View.VISIBLE);
                            tVTitle.setText("עריכת העסק שלי");
                            btnSaveBusiness.setText("עדכן פרטים");
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            pd.dismiss();
                            Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

                    isEditMode = false;
                    currentBusinessId = null;
                    businessLat = null;
                    businessLon = null;
                    tVTitle.setText("יצירת עסק חדש");
                    btnSaveBusiness.setText("שמור עסק");

                    btnDeleteBusiness.setVisibility(View.GONE);
                    btnManageHours.setVisibility(View.GONE);

                    eTBusinessName.setText("");
                    eTBusinessPhone.setText("");
                    eTBusinessDescription.setText("");
                    eTBusinessAddress.setText("");
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

        if (totalBytes > 950 * 1024) return null;
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
            tvNoImages.setVisibility(View.VISIBLE);
        } else {
            tvNoImages.setVisibility(View.GONE);

            for (Uri uri : selectedImageUris) addPreview(uri, null);
            for (Bitmap bmp : selectedCameraBitmaps) addPreview(null, bmp);
        }
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

    // --- הרשאות למצלמה ותמונות ---
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }
}