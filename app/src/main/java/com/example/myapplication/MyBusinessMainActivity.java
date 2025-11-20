package com.example.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class MyBusinessMainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private ImageView imgBusinessMain;
    private Button btnChooseImage, btnSaveBusiness;
    private EditText eTBusinessName, eTBusinessPhone, eTBusinessDescription;

    private Uri imageUri;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

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

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        btnChooseImage.setOnClickListener(v -> openImagePicker());
        btnSaveBusiness.setOnClickListener(v -> saveBusiness());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imgBusinessMain.setImageURI(imageUri);
        }
    }

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

        String ownerId = user.getUid();
        String businessId = UUID.randomUUID().toString();

        // Upload the image
        StorageReference imgRef = storageRef.child("business_images/" + ownerId + "/main_" + businessId + ".jpg");

        imgRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();

                            Business business = new Business(
                                    businessId,
                                    ownerId,
                                    name,
                                    description,
                                    phone,
                                    imageUrl
                            );

                            db.collection("businesses")
                                    .document(businessId)
                                    .set(business)
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
