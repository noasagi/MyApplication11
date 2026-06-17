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

// מחלקת אקטיביטי המנהלת את מסך עריכת פרופיל המשתמש, כולל קלט טקסטואלי וניהול תמונת פרופיל (מצלמה/גלריה/אווטאר)
public class SetProfileActivity extends BaseActivity {

    private EditText eTName, eTBirthDate, eTAddress, eTPhone;
    private TextView tVMsg;
    private Button btnSaveProfile;
    private ImageView imgProfile;

    private FirebaseAuth refAuth;
    private FirebaseFirestore db;

    // אובייקט לשמירת תמונת הביטמאפ (מערך פיקסלים בזיכרון) שנבחרה או צולמה, רגע לפני העיבוד וההעלאה לענן
    private Bitmap selectedImageBitmap = null;

    // 1. משגר (Launcher) לבקשת הרשאת גישה למצלמת המכשיר בזמן אמת (Runtime Permission)
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        launchCameraSafely(); // המשתמש אישר - נפתח את המצלמה
                    } else {
                        Toast.makeText(SetProfileActivity.this, "יש לאשר גישה למצלמה כדי לצלם תמונה", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // 2. משגר לפתיחת אפליקציית הגלריה המובנית של המכשיר וחילוץ נתיב הקובץ (URI) שנבחר
    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<android.net.Uri>() {
                @Override
                public void onActivityResult(android.net.Uri uri) {
                    if (uri != null) {
                        try {
                            // פתיחת זרם קלט (InputStream) אל כתובת ה-URI ופענוחו לאובייקט Bitmap מקומי בזיכרון
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
                            imgProfile.setImageBitmap(selectedImageBitmap); // הצגת התמונה שנבחרה בתוך ה-ImageView
                        } catch (Exception e) {
                            Toast.makeText(SetProfileActivity.this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    // 3. משגר להפעלת תצוגת המצלמה לקבלת תמונה מהירה (חוזרת כביטמאפ מוקטן)
    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            new ActivityResultCallback<Bitmap>() {
                @Override
                public void onActivityResult(Bitmap bitmap) {
                    if (bitmap != null) {
                        selectedImageBitmap = bitmap;
                        imgProfile.setImageBitmap(selectedImageBitmap);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_profile);

        eTName = findViewById(R.id.eTName);
        eTBirthDate = findViewById(R.id.eTBirthDate);
        eTAddress = findViewById(R.id.eTAddress);
        eTPhone = findViewById(R.id.eTPhone);
        tVMsg = findViewById(R.id.tVMsg);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        imgProfile = findViewById(R.id.imgProfile);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        eTBirthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });

        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        loadUserData(); // טעינת נתונים קיימים כדי שהמשתמש יראה את המידע הנוכחי שלו בעת פתיחת המסך
    }

    /**
     * מה הפעולה עושה: מייצרת ומנפחת דיאלוג בחירה (Custom AlertDialog) המאפשר למשתמש לבחור בין צילום, גלריה, או בחירה מתוך 9 אווטארים (איורים) מוכנים מראש.
     */
    private void showImagePickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_picker, null);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Button btnCamera = dialogView.findViewById(R.id.btnCamera);
        Button btnGallery = dialogView.findViewById(R.id.btnGallery);

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                // בדיקת אבטחה: האם ההרשאה למצלמה כבר אושרה בעבר בטלפון זה
                if (ContextCompat.checkSelfPermission(SetProfileActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchCameraSafely();
                } else {
                    // אם לא, נפעיל את משגר בקשת ההרשאה הדינמי
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            }
        });

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageLauncher.launch("image/*"); // הגדרת פילטר MIME-Type לקבלת קבצי תמונה בלבד
                dialog.dismiss();
            }
        });

        // פונקציית מאזין לחיצה משותפת עבור כל האווטארים המקומיים בדיאלוג
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
                    // המרת משאב מקומי (Resource ID) לאובייקט Bitmap כדי שנוכל להתייחס אליו בדיוק כמו תמונה מהמצלמה/גלריה
                    selectedImageBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
                    imgProfile.setImageBitmap(selectedImageBitmap);
                }
                dialog.dismiss();
            }
        };

        // הצמדת המאזין המשותף לכל אחד מרכיבי האווטארים
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

    // תנאי הגנה מפני קריסה (Crash Guard) במקרה שהקוד רץ על אמולטור או מכשיר ללא אפליקציית מצלמה מותקנת
    private void launchCameraSafely() {
        try {
            takePictureLauncher.launch(null);
        } catch (Exception e) {
            Toast.makeText(this, "לא נמצאה אפליקציית מצלמה זמינה במכשיר", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * מה הפעולה עושה: שולפת באופן אסינכרוני את מסמך המשתמש מ-Firestore ומציגה את הנתונים הקיימים, כולל המרת שדה ה-Blob הבינארי בחזרה ל-Bitmap חזותי.
     */
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

                                // שליפת התמונה הבינארית (Blob) והמרתה ההפוכה למערך ביתים וממנו ל-Bitmap
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

    // פתיחת רכיב לוח שנה מובנה של מערכת ההפעלה (DatePickerDialog) ועדכון השדה בטקסט שנבחר
    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // חודשים באנדרואיד מתחילים מ-0 (ינואר הוא 0), לכן מוסיפים 1
                        String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                        eTBirthDate.setText(date);
                    }
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * מה הפעולה עושה: אוספת את הנתונים, מפעילה וולידציה, אורזת אותם ל-Map, ואם קיימת תמונה חדשה – דוחסת אותה ל-Blob ומעדכנת (update) את מסמך המשתמש בענן.
     */
    private void saveProfile() {
        String name = eTName.getText().toString().trim();
        String birthDate = eTBirthDate.getText().toString().trim();
        String address = eTAddress.getText().toString().trim();
        String phone = eTPhone.getText().toString().trim();

        if (name.isEmpty()) {
            tVMsg.setText("חובה למלא שם");
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("שומר פרופיל");
        pd.setMessage("מעבד תמונה ושומר...");
        pd.show();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("birthDate", birthDate);
        data.put("address", address);
        data.put("phone", phone);

        // בדיקה: האם המשתמש שינה/הוסיף תמונה במהלך העבודה במסך?
        if (selectedImageBitmap != null) {
            // הפעלת אלגוריתם הדחיסה והפיכת הפיקסלים לפורמט מותאם לענן
            Blob imageBlob = compressBitmapToBlob(selectedImageBitmap);
            if (imageBlob != null) {
                data.put("profileImageBlob", imageBlob);
            } else {
                Toast.makeText(this, "התמונה גדולה מדי או פגומה", Toast.LENGTH_SHORT).show();
            }
        }

        FirebaseUser user = refAuth.getCurrentUser();
        if (user != null) {
            // שימוש בפקודת update כדי לעדכן רק את השדות הספציפיים הללו מבלי לדרוס שדות אחרים (כמו שדה ה-type של המשתמש!)
            db.collection("users").document(user.getUid())
                    .update(data)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            pd.dismiss();
                            Toast.makeText(SetProfileActivity.this, "הפרופיל נשמר!", Toast.LENGTH_SHORT).show();
                            finish();
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

    /**
     * מה הפעולה עושה: אלגוריתם דחיסת מדיה. משנה תחילה את רזולוציית התמונה באופן פרופורציונלי למקסימום של 500 פיקסלים, ואז דוחסת את הקובץ לפורמט JPEG באיכות של 70%.
     * למה זה חשוב: מסמך ב-Firestore מוגבל בגודלו לעד $1\text{ MB}$. ללא כיווץ רזולוציה ואיכות, תמונות מהמצלמות המודרניות יגרמו לחריגה מהמגבלה ולקריסת האפליקציה.
     * קלט: Bitmap originalBitmap.
     * פלט: Blob (ייצוג בינארי הדחוס של המידע, מוכן לשמירה ב-NoSQL).
     */
    private Blob compressBitmapToBlob(Bitmap originalBitmap) {
        try {
            // שלב א': שינוי גודל פיזי של הפיקסלים (מניעת תמונות ענק של 4K)
            Bitmap resizedBitmap = getResizedBitmap(originalBitmap, 500);

            // שלב ב': פתיחת זרם פלט בינארי וכיווץ איכות הקובץ ל-70% בפורמט JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);

            byte[] data = baos.toByteArray();
            return Blob.fromBytes(data); // יצירת עצם ה-Blob הרשמי של פיירבייס מתוך מערך הביטים
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * מה הפעולה עושה: פונקציה מתמטית לשינוי קנה מידה (Scaling) של תמונה תוך שמירה קפדנית על יחס הגובה-רוחב המקורי (Aspect Ratio) שלה.
     * קלט: Bitmap image, int maxSize.
     * פלט: Bitmap מוקטן ופרופורציונלי.
     */
    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            // תמונת רוחב (Landscape) - הרוחב יוגבל והגובה יחושב בהתאם ליחס
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            // תמונת אורך או ריבוע (Portrait) - הגובה יוגבל והרוחב יחושב בהתאם ליחס
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}