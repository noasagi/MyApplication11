package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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

public class BusinessProfileFragment extends Fragment {

    private LinearLayout previewContainer;
    private TextView tvNoImages, tVTitle;
    private Button btnChooseImage, btnTakePhoto, btnSaveBusiness, btnDeleteBusiness;
    private EditText eTBusinessName, eTBusinessPhone, eTBusinessDescription;
    private AutoCompleteTextView autoBusinessType;
    private Button btnManageHours;

    private FirebaseAuth auth;
    private FirebaseFirestore firebaseFirestore;

    // רשימות תמונות
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<Bitmap> selectedCameraBitmaps = new ArrayList<>();

    // ניהול מצב
    private boolean isEditMode = false;
    private String currentBusinessId = null;

    // מפעילים לבחירת תמונה (חייבים להיות מוגדרים לפני onCreate)
    private ActivityResultLauncher<String> mGetMultipleContent;
    private ActivityResultLauncher<Void> mTakePhoto;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // אתחול המפעילים (Launchers)
        mGetMultipleContent = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), result -> {
            if (result != null && !result.isEmpty()) {
                selectedImageUris.addAll(result);
                refreshImagePreviews();
            }
        });

        mTakePhoto = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
            if (result != null) {
                selectedCameraBitmaps.add(result);
                refreshImagePreviews();
            }
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(getContext(), "הרשאה התקבלה, נסה שוב", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "יש לאשר גישה לתמונות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_profile, container, false);

        // חיבור ל-XML
        tVTitle = view.findViewById(R.id.tVTitle);
        previewContainer = view.findViewById(R.id.previewContainer);
        tvNoImages = view.findViewById(R.id.tvNoImages);
        btnChooseImage = view.findViewById(R.id.btnChooseImage);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        btnSaveBusiness = view.findViewById(R.id.btnSaveBusiness);
        btnDeleteBusiness = view.findViewById(R.id.btnDeleteBusiness);

        eTBusinessName = view.findViewById(R.id.eTBusinessName);
        eTBusinessPhone = view.findViewById(R.id.eTBusinessPhone);
        eTBusinessDescription = view.findViewById(R.id.eTBusinessDescription);
        autoBusinessType = view.findViewById(R.id.autoBusinessType);
        btnManageHours = view.findViewById(R.id.btnManageHours);

        auth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        setupUI();
        checkIfUserHasBusiness();

        return view;
    }

    private void setupUI() {
        // כפתור שעות פעילות
        btnManageHours.setOnClickListener(v -> {
            if (currentBusinessId != null) {
                // הערה: יש לוודא שקיים קובץ BusinessHoursActivity
                Intent intent = new Intent(requireContext(), BusinessHoursActivity.class);
                intent.putExtra("BUSINESS_ID", currentBusinessId);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "יש לשמור את העסק קודם", Toast.LENGTH_SHORT).show();
            }
        });

        // סוגי עסקים
        if (getContext() != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.business_types));
            autoBusinessType.setAdapter(adapter);
            autoBusinessType.setOnClickListener(v -> autoBusinessType.showDropDown());
        }

        // בחירת תמונה
        btnChooseImage.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                mGetMultipleContent.launch("image/*");
            } else {
                requestStoragePermission();
            }
        });

        // צילום תמונה
        btnTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                mTakePhoto.launch(null);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnSaveBusiness.setOnClickListener(v -> saveOrUpdateBusiness());
        btnDeleteBusiness.setOnClickListener(v -> showDeleteConfirmation());
    }

    // --- לוגיקה ---

    private void checkIfUserHasBusiness() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        ProgressDialog pd = new ProgressDialog(getContext());
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
                    Toast.makeText(getContext(), "שגיאה בבדיקת נתונים", Toast.LENGTH_SHORT).show();
                });
    }

    private void switchToEditMode(BusinessModel business) {
        isEditMode = true;
        currentBusinessId = business.getBusinessId();

        tVTitle.setText("עריכת העסק שלי");
        btnSaveBusiness.setText("עדכן פרטים");
        btnDeleteBusiness.setVisibility(View.VISIBLE);
        btnManageHours.setVisibility(View.VISIBLE);

        eTBusinessName.setText(business.getName());
        eTBusinessPhone.setText(business.getPhone());
        eTBusinessDescription.setText(business.getDescription());
        autoBusinessType.setText(business.getBusinessType(), false);

        if (business.getImageBlobs() != null) {
            for (Blob blob : business.getImageBlobs()) {
                byte[] bytes = blob.toBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                selectedCameraBitmaps.add(bitmap);
            }
            refreshImagePreviews();
        }
    }

    private void saveOrUpdateBusiness() {
        String name = eTBusinessName.getText().toString().trim();
        String phone = eTBusinessPhone.getText().toString().trim();
        String description = eTBusinessDescription.getText().toString().trim();
        String businessType = autoBusinessType.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || description.isEmpty() ||
                (selectedImageUris.isEmpty() && selectedCameraBitmaps.isEmpty()) || businessType.isEmpty()) {
            Toast.makeText(getContext(), "נא למלא הכל ולהוסיף תמונה", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(getContext());
        pd.setTitle(isEditMode ? "מעדכן..." : "יוצר עסק...");
        pd.setMessage("אנא המתן");
        pd.show();

        String businessIdToSave = (currentBusinessId != null) ? currentBusinessId : UUID.randomUUID().toString();
        String ownerId = auth.getCurrentUser().getUid();

        new Thread(() -> {
            try {
                List<Blob> imageBlobs = processImages();

                if (imageBlobs == null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            pd.dismiss();
                            Toast.makeText(getContext(), "התמונות כבדות מדי", Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                Map<String, Object> businessData = new HashMap<>();
                businessData.put("businessId", businessIdToSave);
                businessData.put("ownerId", ownerId);
                businessData.put("name", name);
                businessData.put("description", description);
                businessData.put("phone", phone);
                businessData.put("businessType", businessType);
                businessData.put("imageBlobs", imageBlobs);

                firebaseFirestore.collection("businesses").document(businessIdToSave)
                        .set(businessData)
                        .addOnSuccessListener(aVoid -> {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                pd.dismiss();
                                Toast.makeText(getContext(), isEditMode ? "עודכן בהצלחה!" : "נוצר בהצלחה!", Toast.LENGTH_SHORT).show();

                                isEditMode = true;
                                currentBusinessId = businessIdToSave;

                                btnManageHours.setVisibility(View.VISIBLE);
                                btnDeleteBusiness.setVisibility(View.VISIBLE);

                                tVTitle.setText("עריכת העסק שלי");
                                btnSaveBusiness.setText("עדכן פרטים");
                            });
                        })
                        .addOnFailureListener(e -> {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                pd.dismiss();
                                Toast.makeText(getContext(), "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                            });
                        });

            } catch (Exception e) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(pd::dismiss);
            }
        }).start();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("מחיקת עסק")
                .setMessage("האם את בטוחה? הפעולה לא ניתנת לביטול.")
                .setPositiveButton("מחק", (dialog, which) -> deleteBusiness())
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteBusiness() {
        if (currentBusinessId == null) return;
        ProgressDialog pd = new ProgressDialog(getContext());
        pd.setMessage("מוחק...");
        pd.show();

        firebaseFirestore.collection("businesses").document(currentBusinessId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    Toast.makeText(getContext(), "העסק נמחק", Toast.LENGTH_SHORT).show();
                    resetForm();
                });
    }

    private void resetForm() {
        isEditMode = false;
        currentBusinessId = null;
        tVTitle.setText("יצירת עסק חדש");
        btnSaveBusiness.setText("שמור עסק");
        btnDeleteBusiness.setVisibility(View.GONE);
        btnManageHours.setVisibility(View.GONE);
        eTBusinessName.setText("");
        eTBusinessPhone.setText("");
        eTBusinessDescription.setText("");
        autoBusinessType.setText("");
        selectedImageUris.clear();
        selectedCameraBitmaps.clear();
        refreshImagePreviews();
    }

    // --- עזרים לתמונות ---

    private List<Blob> processImages() {
        List<Blob> imageBlobs = new ArrayList<>();
        long totalBytes = 0;

        try {
            if (getContext() == null) return new ArrayList<>();

            for (Uri uri : selectedImageUris) {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
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

        if (totalBytes > 950 * 1024) return null; // הגבלת גודל לפיירבייס
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
        ImageView iv = new ImageView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
        params.setMargins(8, 0, 8, 0);
        iv.setLayoutParams(params);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (uri != null) iv.setImageURI(uri);
        else iv.setImageBitmap(bitmap);
        previewContainer.addView(iv);
    }

    // --- הרשאות ---
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        requestPermissionLauncher.launch(permission);
    }
}