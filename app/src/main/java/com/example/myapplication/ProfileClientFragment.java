package com.example.myapplication;

import android.app.AlertDialog;
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

    // שומרים את התמונה כ-Bitmap כדי שנתמוך בכל המקורות (מצלמה, גלריה, אווטאר)
    private Bitmap selectedImageBitmap = null;

    // משגר לבחירת תמונה מהגלריה
    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                        selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
                        imgProfile.setImageBitmap(selectedImageBitmap);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // משגר לצילום תמונה מהמצלמה (מחזיר Thumbnail שמספיק מעולה לפרופיל)
    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    selectedImageBitmap = bitmap;
                    imgProfile.setImageBitmap(selectedImageBitmap);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        // עכשיו לחיצה על התמונה פותחת את הדיאלוג במקום ישר את הגלריה
        imgProfile.setOnClickListener(v -> showImagePickerDialog());

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        loadUserData();

        return view;
    }

    private void showImagePickerDialog() {
        if (getContext() == null) return;

        // טעינת העיצוב שיצרנו לדיאלוג
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_image_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        Button btnCamera = dialogView.findViewById(R.id.btnCamera);
        Button btnGallery = dialogView.findViewById(R.id.btnGallery);

        // כפתור מצלמה
        btnCamera.setOnClickListener(v -> {
            takePictureLauncher.launch(null);
            dialog.dismiss();
        });

        // כפתור גלריה
        btnGallery.setOnClickListener(v -> {
            selectImageLauncher.launch("image/*");
            dialog.dismiss();
        });

        // פונקציה משותפת ללחיצה על אווטארים
        View.OnClickListener avatarClickListener = v -> {
            int drawableId = 0;

            // מזהים על איזה אווטאר המשתמש לחץ
            if (v.getId() == R.id.imgAvatar1) drawableId = R.drawable.avatar_1;
            else if (v.getId() == R.id.imgAvatar2) drawableId = R.drawable.avatar_2;
            else if (v.getId() == R.id.imgAvatar3) drawableId = R.drawable.avatar_3;
            else if (v.getId() == R.id.imgAvatar4) drawableId = R.drawable.avatar_4;
            else if (v.getId() == R.id.imgAvatar5) drawableId = R.drawable.avatar_5;
            else if (v.getId() == R.id.imgAvatar6) drawableId = R.drawable.avatar_6;

            if (drawableId != 0) {
                // הופכים את האווטאר ל-Bitmap ושומרים
                selectedImageBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
                imgProfile.setImageBitmap(selectedImageBitmap);
            }
            dialog.dismiss();
        };

        // חיבור הפונקציה לכל התמונות
        dialogView.findViewById(R.id.imgAvatar1).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar2).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar3).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar4).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar5).setOnClickListener(avatarClickListener);
        dialogView.findViewById(R.id.imgAvatar6).setOnClickListener(avatarClickListener);

        dialog.show();
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
            // כעת נשתמש ב-Bitmap השמור במקום ב-Uri
            if (selectedImageBitmap != null) {
                Blob imageBlob = compressBitmapToBlob(selectedImageBitmap);
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

    // הפונקציה עודכנה לקבל Bitmap במקום Uri
    private Blob compressBitmapToBlob(Bitmap originalBitmap) {
        try {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 500, 500, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            return Blob.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }
}