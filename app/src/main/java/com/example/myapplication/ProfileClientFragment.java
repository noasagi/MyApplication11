package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ProfileClientFragment extends Fragment {

    private EditText eTName, eTBirthDate, eTAddress, eTPhone;
    private TextView tVMsg;
    private Button btnSaveProfile;
    private ImageView imgProfile;

    private FirebaseAuth refAuth;
    private FirebaseFirestore db;
    private Uri imageUri = null;

    // משגר לבחירת תמונה
    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    imgProfile.setImageURI(imageUri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // ודאי שיש לך קובץ xml בשם fragment_profile (אם אין, תשתמשי ב-XML ששלחת לי קודם בשם הזה)
        View view = inflater.inflate(R.layout.fragment_client_profile, container, false);

        // אתחול רכיבים
        eTName = view.findViewById(R.id.eTName);
        eTBirthDate = view.findViewById(R.id.eTBirthDate);
        eTAddress = view.findViewById(R.id.eTAddress);
        eTPhone = view.findViewById(R.id.eTPhone);
        tVMsg = view.findViewById(R.id.tVMsg);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        imgProfile = view.findViewById(R.id.imgProfile);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        eTBirthDate.setOnClickListener(v -> showDatePicker());
        imgProfile.setOnClickListener(v -> selectImageLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        loadUserData();

        return view;
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
        if (getContext() == null) return;
        Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
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

        if (getContext() == null) return;
        ProgressDialog pd = new ProgressDialog(getContext());
        pd.setTitle("שומר פרופיל");
        pd.setMessage("אנא המתן...");
        pd.show();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("birthDate", birthDate);
        data.put("address", address);
        data.put("phone", phone);

        new Thread(() -> {
            if (imageUri != null) {
                Blob imageBlob = compressImageToBlob(imageUri);
                if (imageBlob != null) data.put("profileImageBlob", imageBlob);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    FirebaseUser user = refAuth.getCurrentUser();
                    if (user != null) {
                        db.collection("users").document(user.getUid())
                                .update(data)
                                .addOnSuccessListener(aVoid -> {
                                    pd.dismiss();
                                    Toast.makeText(getContext(), "נשמר בהצלחה!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    pd.dismiss();
                                    tVMsg.setText("שגיאה: " + e.getMessage());
                                });
                    }
                });
            }
        }).start();
    }

    private Blob compressImageToBlob(Uri uri) {
        try {
            if (getContext() == null) return null;
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 500, 500, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            return Blob.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }
}