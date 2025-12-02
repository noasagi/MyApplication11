package com.example.myapplication;

import android.app.ProgressDialog;
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
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MyBusinessMainActivity extends AppCompatActivity {

    private ImageView imgBusinessMain;
    private Button btnChooseImage, btnSaveBusiness;
    private EditText eTBusinessName, eTBusinessPhone, eTBusinessDescription;
    private AutoCompleteTextView autoBusinessType;

    private Uri imageUri;

    private FirebaseAuth auth;
    private FirebaseFirestore firebaseFirestore;

    // בחירת תמונה
    private ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null) {
                        imgBusinessMain.setImageURI(result);
                        imageUri = result;
                    } else {
                        Log.d("ImagePicker", "Selection cancelled");
                    }
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

        // להציע גם בלי להקליד כלום
        autoBusinessType.setThreshold(0);

        // כשלוחצים על השדה – לפתוח מיד את הרשימה
        autoBusinessType.setOnClickListener(v -> autoBusinessType.showDropDown());

        // כשמקבל פוקוס (למשל מעבר עם טאץ' או טאב) – גם לפתוח
        autoBusinessType.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                autoBusinessType.showDropDown();
            }
        });

        auth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        btnChooseImage.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnSaveBusiness.setOnClickListener(v -> saveBusiness());
    }

    private void saveBusiness() {

        String name = eTBusinessName.getText().toString().trim();
        String phone = eTBusinessPhone.getText().toString().trim();
        String description = eTBusinessDescription.getText().toString().trim();
        String businessType = autoBusinessType.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || description.isEmpty() ||
                imageUri == null || businessType.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות, לבחור תמונה וסוג עסק", Toast.LENGTH_SHORT).show();
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
            // --- המרת URI ל-Bitmap ---
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // --- כיווץ התמונה ל-JPEG 60% ---
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();

            // בדיקת גודל למסמך Firestore (מקסימום ~1MB)
            if (imageBytes.length > 900 * 1024) { // 900 KB
                pd.dismiss();
                Toast.makeText(this, "התמונה גדולה מדי. נסי לבחור תמונה קטנה יותר.", Toast.LENGTH_LONG).show();
                return;
            }

            // --- המרה ל-Blob ---
            Blob imageBlob = Blob.fromBytes(imageBytes);

            // --- יצירת המסמך ---
            Map<String, Object> businessData = new HashMap<>();
            businessData.put("businessId", businessId);
            businessData.put("ownerId", ownerId);
            businessData.put("name", name);
            businessData.put("description", description);
            businessData.put("phone", phone);
            businessData.put("businessType", businessType); // שמירת סוג העסק
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
