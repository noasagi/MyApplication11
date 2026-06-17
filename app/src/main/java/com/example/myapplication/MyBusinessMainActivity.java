package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.location.Address;
import android.location.Geocoder;
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
import androidx.appcompat.widget.Toolbar;
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
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MyBusinessMainActivity extends BaseActivity {

    private LinearLayout previewContainer;
    private TextView tvNoImages;
    private Button btnSaveBusiness, btnDeleteBusiness;

    private EditText eTBusinessName, eTBusinessPhone, eTBusinessDescription, eTBusinessAddress;
    private AutoCompleteTextView autoBusinessType;

    private FirebaseAuth auth;
    private Button btnManageHours;
    private FirebaseFirestore firebaseFirestore;

    private final List<Uri> selectedImageUris = new ArrayList<>();
    private final List<Bitmap> selectedCameraBitmaps = new ArrayList<>();

    private boolean isEditMode = false;
    private String currentBusinessId = null;

    private final int REQUEST_CAMERA_PERMISSION = 100;
    private final int REQUEST_STORAGE_PERMISSION = 101;

    private Double businessLat = null;
    private Double businessLon = null;

    // שימוש ב-ActivityResultLauncher מודרני לבחירת מספר תמונות מהגלריה במקביל
    private final ActivityResultLauncher<String> mGetMultipleContent =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), result -> {
                if (result != null && !result.isEmpty()) {
                    selectedImageUris.addAll(result);
                    refreshImagePreviews();
                }
            });

    // שימוש ב-ActivityResultLauncher מודרני לצילום תמונה ישירות מהמצלמה
    private final ActivityResultLauncher<Void> mTakePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
                if (result != null) {
                    selectedCameraBitmaps.add(result);
                    refreshImagePreviews();
                }
            });

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי המסך, מקשרת את סרגל הכלים, מגדירה את תפריט הבחירה הנגלל (Spinner-like AutoCompleteTextView) ומחברת מאזינים לכפתורים.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_business_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        previewContainer = findViewById(R.id.previewContainer);
        tvNoImages = findViewById(R.id.tvNoImages);
        Button btnChooseImage = findViewById(R.id.btnChooseImage);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSaveBusiness = findViewById(R.id.btnSaveBusiness);
        btnDeleteBusiness = findViewById(R.id.btnDeleteBusiness);

        eTBusinessName = findViewById(R.id.eTBusinessName);
        eTBusinessPhone = findViewById(R.id.eTBusinessPhone);
        eTBusinessDescription = findViewById(R.id.eTBusinessDescription);
        eTBusinessAddress = findViewById(R.id.eTBusinessAddress);

        autoBusinessType = findViewById(R.id.autoBusinessType);
        btnManageHours = findViewById(R.id.btnManageHours);

        auth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        btnManageHours.setOnClickListener(v -> {
            if (currentBusinessId != null) {
                Intent intent = new Intent(MyBusinessMainActivity.this, BusinessHoursActivity.class);
                intent.putExtra("BUSINESS_ID", currentBusinessId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "יש לשמור את העסק קודם", Toast.LENGTH_SHORT).show();
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.business_types));
        autoBusinessType.setAdapter(adapter);
        autoBusinessType.setOnClickListener(v -> autoBusinessType.showDropDown());

        checkIfUserHasBusiness(); // שלב א': בדיקה אסינכרונית אם למשתמש המחובר כבר יש עסק קיים במסד

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

    /**
     * מה הפעולה עושה: פונה ל-Firestore ובודקת האם קיים מסמך באוסף businesses שבו שדה ה-ownerId שווה ל-UID של המשתמש המחובר.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void checkIfUserHasBusiness() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("בודק נתונים...");
        pd.setCancelable(false);
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
                            switchToEditMode(business); // העברת המסך למצב עריכת נתונים קיימים
                        }
                    } else {
                        btnDeleteBusiness.setVisibility(View.GONE);
                        btnManageHours.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "שגיאה בבדיקת נתונים", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * מה הפעולה עושה: משנה את מצב המסך למצב עריכה (Edit Mode), מאכלסת את שדות הקלט בנתוני העסק הקיים, ומפענחת את מערך הבלובים (Blob) חזרה לתמונות Bitmap לתצוגה.
     * קלט: BusinessModel business.
     * פלט: אין (void).
     */
    private void switchToEditMode(BusinessModel business) {
        isEditMode = true;
        currentBusinessId = business.getBusinessId();

        businessLat = business.getLatitude();
        businessLon = business.getLongitude();

        btnSaveBusiness.setText("עדכן פרטים");
        btnDeleteBusiness.setVisibility(View.VISIBLE);
        btnManageHours.setVisibility(View.VISIBLE);

        eTBusinessName.setText(business.getName());
        eTBusinessPhone.setText(business.getPhone());
        eTBusinessDescription.setText(business.getDescription());
        eTBusinessAddress.setText(business.getAddress() != null ? business.getAddress() : "");
        autoBusinessType.setText(business.getBusinessType(), false);

        if (business.getImageBlobs() != null) {
            selectedCameraBitmaps.clear();
            for (Blob blob : business.getImageBlobs()) {
                byte[] bytes = blob.toBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                selectedCameraBitmaps.add(bitmap);
            }
            refreshImagePreviews();
        }
    }

    /**
     * מה הפעולה עושה: אוספת את נתוני הממשק, מעבדת ומקטינה את התמונות, מפעילה מנגנון Geocoder ברקע להמרת הכתובת לקואורדינטות, ושומרת/מעדכנת את המסמך ב-Firestore באמצעות SetOptions.merge().
     * קלט: אין.
     * פלט: אין (void).
     */
    private void saveOrUpdateBusiness() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "עליך להיות מחובר כדי לשמור עסק", Toast.LENGTH_SHORT).show();
            return;
        }

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

        btnSaveBusiness.setEnabled(false);
        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(isEditMode ? "מעדכן..." : "יוצר עסק...");
        pd.setMessage("אנא המתן");
        pd.setCancelable(false);
        pd.show();

        String businessIdToSave = (currentBusinessId != null) ? currentBusinessId : UUID.randomUUID().toString();
        String ownerId = user.getUid();

        // הרצת עיבוד תמונות וגיאוקודינג בשרשור נפרד (Thread) למניעת תקיעת ה-UI (ANR - Application Not Responding)
        new Thread(() -> {
            try {
                List<Blob> imageBlobs = processImages();

                if (imageBlobs == null) {
                    runOnUiThread(() -> {
                        pd.dismiss();
                        btnSaveBusiness.setEnabled(true);
                        Toast.makeText(this, "התמונות כבדות מדי", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // שימוש ברכיב Geocoder להמרת מחרוזת הכתובת למיקומים גיאוגרפיים (מבוסס רשת)
                Geocoder geocoder = new Geocoder(MyBusinessMainActivity.this, new Locale("he", "IL"));
                try {
                    List<Address> addresses = geocoder.getFromLocationName(address + ", ישראל", 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        businessLat = addresses.get(0).getLatitude();
                        businessLon = addresses.get(0).getLongitude();
                    } else {
                        runOnUiThread(() -> {
                            pd.dismiss();
                            btnSaveBusiness.setEnabled(true);
                            Toast.makeText(this, "לא מצאנו את הכתובת. אנא ודא שהיא מדויקת (עיר ורחוב).", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        pd.dismiss();
                        btnSaveBusiness.setEnabled(true);
                        Toast.makeText(this, "שגיאה בחיפוש הכתובת, נסה שוב מאוחר יותר.", Toast.LENGTH_SHORT).show();
                    });
                    return;
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
                        .set(businessData, SetOptions.merge()) // מיזוג חכם המונע דריסת שדות אחרים שלא צוינו (כמו דירוגי ממוצעים)
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            pd.dismiss();
                            btnSaveBusiness.setEnabled(true);
                            Toast.makeText(this, isEditMode ? "עודכן בהצלחה!" : "נוצר בהצלחה!", Toast.LENGTH_SHORT).show();

                            isEditMode = true;
                            currentBusinessId = businessIdToSave;

                            btnManageHours.setVisibility(View.VISIBLE);
                            btnDeleteBusiness.setVisibility(View.VISIBLE);
                            btnSaveBusiness.setText("עדכן פרטים");
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            pd.dismiss();
                            btnSaveBusiness.setEnabled(true);
                            Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    btnSaveBusiness.setEnabled(true);
                });
            }
        }).start();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת עסק")
                .setMessage("האם את בטוחה? הפעולה לא ניתנת לביטול.")
                .setPositiveButton("מחק", (dialog, which) -> deleteBusiness())
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * מה הפעולה עושה: מוחקת לחלוטין את מסמך בית העסק מ-Firestore ומאפסת את כל רכיבי הממשק למצב התחלתי נקי.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void deleteBusiness() {
        if (currentBusinessId == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מוחק...");
        pd.setCancelable(false);
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

    /**
     * מה הפעולה עושה: עוברת על כל התמונות שנבחרו (מצלמה וגלריה), קוראת אותן מהמכשיר, דוחסת אותן והופכת אותן לרשימה של אובייקטי Blob הניתנים לשמירה בתוך מסמך פיירסטור.
     * קלט: אין.
     * פלט: List<Blob> (רשימת התמונות המכווצות כמערכי בתים) או null במידה והנפח חורג מהמגבלה הכללית.
     */
    private List<Blob> processImages() {
        List<Blob> imageBlobs = new ArrayList<>();
        long totalBytes = 0;

        try {
            for (Uri uri : selectedImageUris) {
                Bitmap bitmap;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                    bitmap = ImageDecoder.decodeBitmap(source);
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                }

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

        // הגנת חומת אש של פיירסטור: מסמך שלם אינו יכול לעלות על 1MB (1024KB), לכן נגביל את התמונות ל-950KB מקסימום
        if (totalBytes > 950 * 1024) return null;
        return imageBlobs;
    }

    /**
     * מה הפעולה עושה: משנה את גודל התמונה למימדים של 800x800 פיקסלים ודוחסת אותה בפורמט JPEG לאיכות של 70% כדי להקטין דרסטית את משקלה ברשת.
     * קלט: Bitmap bitmap.
     * פלט: byte[] (מערך בתים דחוס).
     */
    private byte[] compressBitmap(Bitmap bitmap) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 800, 800, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return baos.toByteArray();
    }

    // --- פעולות עזר פרטיות לריענון ויצירת תצוגה מקדימה דינמית לתמונות (UI Previews) ---

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

    // --- ניהול בדיקה ובקשת הרשאות זמן ריצה (Runtime Permissions) מול מערכת ההפעלה אנדרואיד ---

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