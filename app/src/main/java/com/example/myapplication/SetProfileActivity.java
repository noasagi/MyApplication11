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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

// הגדרת אקטיביטי לעריכה ועדכון נתוני הפרופיל והתמונה של המשתמשת במערכת
public class SetProfileActivity extends BaseActivity {

    // רכיבי הזנת טקסט, תצוגה וכפתורים בממשק המשתמש
    private EditText eTName, eTBirthDate, eTAddress, eTPhone;
    private TextView tVMsg;
    private Button btnSaveProfile;
    private ImageView imgProfile;

    // מופעי הגישה לרכיבי האימות ומסד הנתונים של פיירבייס
    private FirebaseAuth refAuth;
    private FirebaseFirestore db;

    // אובייקט לשמירת תמונת הביטמאפ שנבחרה בזיכרון המקומי לפני העלאה לענן
    private Bitmap selectedImageBitmap = null;

    // 1. משגר אנונימי קלאסי לבקשת הרשאת גישה למצלמת המכשיר בזמן אמת
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        launchCameraSafely(); // אם אישרה - מפעילים את המצלמה בבטחה
                    } else {
                        Toast.makeText(SetProfileActivity.this, "יש לאשר גישה למצלמה כדי לצלם תמונה", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // 2. משגר אנונימי קלאסי לפתיחת גלריית המכשיר וחילוץ קובץ התמונה שנבחרה
    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<android.net.Uri>() {
                @Override
                public void onActivityResult(android.net.Uri uri) {
                    if (uri != null) {
                        try {
                            // פתיחת זרם קלט אל נתיב התמונה המבוקש והמרתו לאובייקט Bitmap
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
                            imgProfile.setImageBitmap(selectedImageBitmap); // הצגה חזותית על המסך
                        } catch (Exception e) {
                            Toast.makeText(SetProfileActivity.this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    // 3. משגר אנונימי קלאסי להפעלת תצוגה מקדימה של המצלמה וקבלת התמונה שצולמה
    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            new ActivityResultCallback<Bitmap>() {
                @Override
                public void onActivityResult(Bitmap bitmap) {
                    if (bitmap != null) {
                        selectedImageBitmap = bitmap; // שמירת התמונה שצולמה
                        imgProfile.setImageBitmap(selectedImageBitmap); // הצגה חזותית על המסך
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_profile);

        // קישור רכיבי הגרפיקה מה-XML למשתני המחלקה
        eTName = findViewById(R.id.eTName);
        eTBirthDate = findViewById(R.id.eTBirthDate);
        eTAddress = findViewById(R.id.eTAddress);
        eTPhone = findViewById(R.id.eTPhone);
        tVMsg = findViewById(R.id.tVMsg);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        imgProfile = findViewById(R.id.imgProfile);

        // אתחול מופעי הגישה של שירותי פיירבייס
        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // הגדרת מאזין לחיצה אנונימי לפתיחת דיאלוג בחירת תאריך הלידה
        eTBirthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // הגדרת מאזין לחיצה על רכיב התמונה לפתיחת תפריט בחירת המדיה
        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });

        // הגדרת מאזין לחיצה לשמירת ועריכת כלל נתוני הפרופיל בענן
        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        // הגדרת וחיבור סרגל הכלים המשני של המסך
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // העלמת כותרת ברירת המחדל לטובת עיצוב נקי
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // טעינה וסינכרון של נתוני המשתמשת הקיימים מתוך הענן
        loadUserData();
    }

    // בנייה והצגה של תיבת דיאלוג מותאמת אישית לבחירת תמונה (מצלמה, גלריה או אווטארים)
    private void showImagePickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_picker, null);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Button btnCamera = dialogView.findViewById(R.id.btnCamera);
        Button btnGallery = dialogView.findViewById(R.id.btnGallery);

        // הגדרת מאזין לכפתור הפעלת מצלמה (כולל מנגנון בדיקת הרשאות)
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (ContextCompat.checkSelfPermission(SetProfileActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchCameraSafely();
                } else {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            }
        });

        // הגדרת מאזין לכפתור בחירה מגלריה
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageLauncher.launch("image/*");
                dialog.dismiss();
            }
        });

        // פונקציית מאזין לחיצה משותפת עבור מבחר האווטארים המוכנים מראש בדיאלוג
        View.OnClickListener avatarClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int drawableId = 0;
                int id = v.getId();

                if (id == R.id.imgAvatar1) drawableId = R.drawable.avatar_1;
                else if (id == R.id.imgAvatar2) drawableId = R.drawable.avatar_2;
                else if (id == R.id.imgAvatar3) drawableId = R.drawable.avatar_3;
                else if (id == R.id.imgAvatar4) drawableId = R.drawable.avatar_4;
                else if (id == R.id.imgAvatar5) drawableId = R.drawable.avatar_5;
                else if (id == R.id.imgAvatar6) drawableId = R.drawable.avatar_6;
                else if (id == R.id.imgAvatar7) drawableId = R.drawable.avatar_7;
                else if (id == R.id.imgAvatar8) drawableId = R.drawable.avatar_8;
                else if (id == R.id.imgAvatar9) drawableId = R.drawable.avatar_9;

                if (drawableId != 0) {
                    // המרת משאב ה-Drawable המקומי לאובייקט Bitmap להצגה ושמירה
                    selectedImageBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
                    imgProfile.setImageBitmap(selectedImageBitmap);
                }
                dialog.dismiss();
            }
        };

        // הצמדת המאזין המשותף לכל אחד מתשעת רכיבי האווטארים בדיאלוג
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

    // הפעלה מוגנת של המצלמה כדי למנוע קריסה במכשירים ללא חומרת מצלמה זמינה
    private void launchCameraSafely() {
        try {
            takePictureLauncher.launch(null);
        } catch (Exception e) {
            Toast.makeText(this, "לא נמצאה אפליקציית מצלמה זמינה במכשיר", Toast.LENGTH_SHORT).show();
        }
    }

    // שליפה חד פעמית של נתוני המשתמשת הקיימים ממסמך הענן לצורך הצגתם בשדות
    private void loadUserData() {
        FirebaseUser user = refAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                eTName.setText(documentSnapshot.getString("name"));
                                eTBirthDate.setText(documentSnapshot.getString("birthDate"));
                                eTAddress.setText(documentSnapshot.getString("address"));
                                eTPhone.setText(documentSnapshot.getString("phone"));

                                // חילוץ התמונה הבינארית (Blob), המרתה למערך ביטים והצגתה כ-Bitmap
                                Blob imageBlob = documentSnapshot.getBlob("profileImageBlob");
                                if (imageBlob != null) {
                                    byte[] bytes = imageBlob.toBytes();
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    imgProfile.setImageBitmap(bitmap);
                                }
                            }
                        }
                    });
        }
    }

    // יצירת והצגת רכיב לוח שנה לבחירה ויזואלית ונוחה של תאריך הלידה
    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                        eTBirthDate.setText(date);
                    }
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // איסוף, וולידציה ושמירה של כלל נתוני הפרופיל והתמונה המעובדת למסמך המשתמש בענן
    private void saveProfile() {
        String name = eTName.getText().toString().trim();
        String birthDate = eTBirthDate.getText().toString().trim();
        String address = eTAddress.getText().toString().trim();
        String phone = eTPhone.getText().toString().trim();

        // בדיקת תקינות בסיסית: חובת הזנת שם משתמש
        if (name.isEmpty()) {
            tVMsg.setText("חובה למלא שם");
            return;
        }

        // הצגת חלונית המתנה בזמן עיבוד המדיה וההעלאה לשרת
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("שומר פרופיל");
        pd.setMessage("מעבד תמונה ושומר...");
        pd.show();

        // ארזת הנתונים בתוך אובייקט מפה (Map)
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("birthDate", birthDate);
        data.put("address", address);
        data.put("phone", phone);

        // במידה והמשתמשת עדכנה או בחרה תמונה חדשה
        if (selectedImageBitmap != null) {
            // קריאה לפונקציית הליבה לעיבוד, כיווץ והמרת הביטמאפ ל-Blob לענן
            Blob imageBlob = compressBitmapToBlob(selectedImageBitmap);
            if (imageBlob != null) {
                data.put("profileImageBlob", imageBlob);
            } else {
                Toast.makeText(this, "התמונה גדולה מדי או פגומה", Toast.LENGTH_SHORT).show();
            }
        }

        FirebaseUser user = refAuth.getCurrentUser();
        if (user != null) {
            // עדכון ממוקד (update) של שדות מסמך המשתמש לפי ה-UID שלו
            db.collection("users").document(user.getUid())
                    .update(data)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            pd.dismiss();
                            Toast.makeText(SetProfileActivity.this, "הפרופיל נשמר!", Toast.LENGTH_SHORT).show();
                            finish(); // סגירת המסך וחזרה לאקטיביטי הקודם
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pd.dismiss();
                            tVMsg.setText("שגיאה: " + e.getMessage());
                        }
                    });
        }
    }

    // פונקציית הליבה: ניהול מערך עיבוד התמונה והמרתה לפורמט בינארי מותאם לענן
    private Blob compressBitmapToBlob(Bitmap originalBitmap) {
        try {
            // 1. שינוי ממדי התמונה לרדיוס פרופיל מקסימלי של 500 פיקסלים
            Bitmap resizedBitmap = getResizedBitmap(originalBitmap, 500);

            // 2. פתיחת זרם פלט בינארי דחוס
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 3. כיווץ דינמי של איכות התמונה ל-70% וייצואה כפורמט JPEG חסכוני
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);

            // 4. המרת המידע למערך ביתים וממנו לאובייקט מסוג Blob התואם לפיירסטור
            byte[] data = baos.toByteArray();
            return Blob.fromBytes(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // פונקציית חישוב מתמטית לשינוי יחסי של גודל הפיקסלים (Scale) ושמירה על יחס הגובה-רוחב
    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        // חישוב יחס הפרופורציה הקיים בין הרוחב לגובה
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            // אם מדובר בתמונת רוחב (Landscape)
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            // אם מדובר בתמונת אורך (Portrait)
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        // יצירת הביטמאפ החדש והקבוע על בסיס הממדים הפרופורציונליים שחושבו
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}