package com.example.myapplication;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map; // הוספת ייבוא עבור Map אם עדיין חסר

public class MyBusinessMainActivity extends AppCompatActivity {

    // הקבוע PICK_IMAGE_REQUEST נמחק

    private ImageView imgBusinessMain;
    private Button btnChooseImage, btnSaveBusiness;
    private EditText eTBusinessName, eTBusinessPhone, eTBusinessDescription;

    private Uri imageUri;

    private FirebaseAuth auth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageRef;

    // המשתנים ownerId ו-businessId יוגדרו בתוך saveBusiness()

    // הגדרת ה-Launcher כמשתנה חבר (Field) כדי שיהיה נגיש ב-onCreate
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

        // אתחול רכיבי UI
        imgBusinessMain = findViewById(R.id.imgBusinessMain);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnSaveBusiness = findViewById(R.id.btnSaveBusiness);
        eTBusinessName = findViewById(R.id.eTBusinessName);
        eTBusinessPhone = findViewById(R.id.eTBusinessPhone);
        eTBusinessDescription = findViewById(R.id.eTBusinessDescription);

        // אתחול Firebase
        auth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageRef = firebaseStorage.getReference();

        // -------------------------------------------------------------
        // תיקון: הגדרת ה-Listener בצורה נקייה וקריאה ל-Launcher המודרני
        btnChooseImage.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnSaveBusiness.setOnClickListener(v -> saveBusiness());
        // -------------------------------------------------------------

        // כל בלוק הקוד שהיה כאן (יצירת מסמך/UploadTask) נמחק - הוא נמצא כעת ב-saveBusiness!
    }


    // המתודה openImagePicker() נמחקה
    // המתודה onActivityResult() נמחקה


    private void saveBusiness() {
        String name = eTBusinessName.getText().toString().trim();
        String phone = eTBusinessPhone.getText().toString().trim();
        String description = eTBusinessDescription.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || description.isEmpty() || imageUri == null) {
            Toast.makeText(this, "נא למלא את כל השדות ולבחור תמונה", Toast.LENGTH_SHORT).show();
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

        // הגדרת המשתנים בתוך המתודה, כדי למנוע בעיות טווח
        String ownerId = user.getUid();
        String businessId = UUID.randomUUID().toString(); // יצירת ID ייחודי לעסק

        // ----------------------------------------------------------------------------------
        // הלוגיקה של העלאת התמונה ושמירת הנתונים ב-Firestore (הלוגיקה הנכונה)
        // ----------------------------------------------------------------------------------
        StorageReference imgRef = storageRef.child("business_images/" + ownerId + "/main_" + businessId + ".jpg");

        imgRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();

                            // יצירת מפת נתונים לשמירה ב-Firestore
                            Map<String, Object> businessData = new HashMap<>();
                            businessData.put("businessId", businessId);
                            businessData.put("ownerId", ownerId);
                            businessData.put("name", name);
                            businessData.put("description", description);
                            businessData.put("phone", phone);
                            businessData.put("imageUrl", imageUrl);

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
                        })
                )
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "שגיאה בהעלאת תמונה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}