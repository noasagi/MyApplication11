package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;

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

    private Uri imageUri = null; // לשמירת ה-URI הזמני לבחירה

    // משגר לבחירת תמונה מהגלריה
    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    imgProfile.setImageURI(imageUri); // תצוגה מקדימה למשתמש
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

        // לחיצה על התמונה -> פתיחת גלריה
        imgProfile.setOnClickListener(v -> selectImageLauncher.launch("image/*"));

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // טעינת נתונים קיימים (כולל תמונה אם יש)
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser user = refAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // טעינת טקסטים
                            eTName.setText(documentSnapshot.getString("name"));
                            eTBirthDate.setText(documentSnapshot.getString("birthDate"));
                            eTAddress.setText(documentSnapshot.getString("address"));
                            eTPhone.setText(documentSnapshot.getString("phone"));

                            // טעינת תמונה מ-Blob
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

        // אם המשתמש בחר תמונה חדשה, אנחנו ממירים אותה ל-Blob
        if (imageUri != null) {
            Blob imageBlob = compressImageToBlob(imageUri);
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

    // --- פונקציות עזר לכיווץ התמונה ---

    private Blob compressImageToBlob(Uri uri) {
        try {
            // 1. המרה מ-URI ל-Bitmap
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

            // 2. הקטנת גודל (Resize)
            Bitmap resizedBitmap = getResizedBitmap(originalBitmap, 500);

            // 3. דחיסה ל-JPEG והמרה ל-Bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);

            // --- התיקון כאן: ---
            byte[] data = baos.toByteArray();
            // -------------------

            // 4. החזרה כ-Blob של Firestore
            return Blob.fromBytes(data);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // פונקציה שמקטינה את הרזולוציה תוך שמירה על פרופורציות
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