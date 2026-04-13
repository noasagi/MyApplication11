package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SetProfileActivity extends BaseActivity {

    private EditText eTName, eTBirthDate, eTAddress, eTPhone;
    private TextView tVMsg;
    private Button btnSaveProfile;
    private ImageView imgProfile;

    private FirebaseAuth refAuth;
    private FirebaseFirestore db;

    private Bitmap selectedImageBitmap = null;

    // 1. משגר לבקשת הרשאת מצלמה
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCameraSafely(); // ההרשאה ניתנה - פותחים מצלמה
                } else {
                    Toast.makeText(this, "יש לאשר גישה למצלמה כדי לצלם תמונה", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // 2. משגר לבחירת תמונה מהגלריה
    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
                        imgProfile.setImageBitmap(selectedImageBitmap);
                    } catch (Exception e) {
                        Toast.makeText(this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // 3. משגר לצילום תמונה מהמצלמה
    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    selectedImageBitmap = bitmap;
                    imgProfile.setImageBitmap(selectedImageBitmap);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_profile);

        // חיבור רכיבים
        eTName = findViewById(R.id.eTName);
        eTBirthDate = findViewById(R.id.eTBirthDate);
        eTAddress = findViewById(R.id.eTAddress);
        eTPhone = findViewById(R.id.eTPhone);
        tVMsg = findViewById(R.id.tVMsg);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        imgProfile = findViewById(R.id.imgProfile);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        eTBirthDate.setOnClickListener(v -> showDatePicker());

        // לחיצה על התמונה פותחת את הדיאלוג
        imgProfile.setOnClickListener(v -> showImagePickerDialog());

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // --- התוספת שלנו להעלמת "My Application" ---
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // ----------------------------------------

        // טעינת נתונים קיימים
        loadUserData();
    }

    private void showImagePickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Button btnCamera = dialogView.findViewById(R.id.btnCamera);
        Button btnGallery = dialogView.findViewById(R.id.btnGallery);

        // כפתור מצלמה - בודק הרשאות קודם!
        btnCamera.setOnClickListener(v -> {
            dialog.dismiss(); // סוגרים את החלון הקופץ קודם

            // בדיקה האם כבר יש לנו הרשאה למצלמה
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCameraSafely();
            } else {
                // אם אין הרשאה, מבקשים אותה
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // כפתור גלריה
        btnGallery.setOnClickListener(v -> {
            selectImageLauncher.launch("image/*");
            dialog.dismiss();
        });

        // פונקציה משותפת ללחיצה על אווטארים
        View.OnClickListener avatarClickListener = v -> {
            int drawableId = 0;

            if (v.getId() == R.id.imgAvatar1) drawableId = R.drawable.avatar_1;
            else if (v.getId() == R.id.imgAvatar2) drawableId = R.drawable.avatar_2;
            else if (v.getId() == R.id.imgAvatar3) drawableId = R.drawable.avatar_3;
            else if (v.getId() == R.id.imgAvatar4) drawableId = R.drawable.avatar_4;
            else if (v.getId() == R.id.imgAvatar5) drawableId = R.drawable.avatar_5;
            else if (v.getId() == R.id.imgAvatar6) drawableId = R.drawable.avatar_6;
            else if (v.getId() == R.id.imgAvatar7) drawableId = R.drawable.avatar_7;
            else if (v.getId() == R.id.imgAvatar8) drawableId = R.drawable.avatar_8;
            else if (v.getId() == R.id.imgAvatar9) drawableId = R.drawable.avatar_9;

            if (drawableId != 0) {
                selectedImageBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
                imgProfile.setImageBitmap(selectedImageBitmap);
            }
            dialog.dismiss();
        };

        // חיבור הפונקציה לכל התמונות (1 עד 9)
        dialogView.findViewById(R.id.imgAvatar1).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar2).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar3).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar4).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar5).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar6).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar7).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar8).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar9).setOnClickListener(avatarClickListener);

        dialog.show();
    }

    // פונקציית עזר לפתיחת המצלמה בצורה בטוחה שלא תקריס את האפליקציה
    private void launchCameraSafely() {
        try {
            takePictureLauncher.launch(null);
        } catch (Exception e) {
            Toast.makeText(this, "לא נמצאה אפליקציית מצלמה זמינה במכשיר", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserData() {
        FirebaseUser user = refAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            eTName.setText(documentSnapshot.getString("name"));
                            eTBirthDate.setText(documentSnapshot.getString("birthDate"));
                            eTAddress.setText(documentSnapshot.getString("address"));
                            eTPhone.setText(documentSnapshot.getString("phone"));

                            Blob imageBlob = documentSnapshot.getBlob("profileImageBlob");
                            if (imageBlob != null) {
                                byte[] bytes = imageBlob.toBytes();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                imgProfile.setImageBitmap(bitmap);
                            }
                        }
                    });
        }
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                    eTBirthDate.setText(date);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveProfile() {
        String name = eTName.getText().toString().trim();
        String birthDate = eTBirthDate.getText().toString().trim();
        String address = eTAddress.getText().toString().trim();
        String phone = eTPhone.getText().toString().trim();

        if (name.isEmpty()) {
            tVMsg.setText("חובה למלא שם");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("שומר פרופיל");
        pd.setMessage("מעבד תמונה ושומר...");
        pd.show();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("birthDate", birthDate);
        data.put("address", address);
        data.put("phone", phone);

        if (selectedImageBitmap != null) {
            Blob imageBlob = compressBitmapToBlob(selectedImageBitmap);
            if (imageBlob != null) {
                data.put("profileImageBlob", imageBlob);
            } else {
                Toast.makeText(this, "התמונה גדולה מדי או פגומה", Toast.LENGTH_SHORT).show();
            }
        }

        FirebaseUser user = refAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        pd.dismiss();
                        Toast.makeText(this, "הפרופיל נשמר!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        tVMsg.setText("שגיאה: " + e.getMessage());
                    });
        }
    }

    private Blob compressBitmapToBlob(Bitmap originalBitmap) {
        try {
            Bitmap resizedBitmap = getResizedBitmap(originalBitmap, 500);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] data = baos.toByteArray();
            return Blob.fromBytes(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}