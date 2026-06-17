package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusinessDetailsActivity extends BaseActivity {

    private TextView tvName, tvType, tvPhone, tvDescription, tvAddress;
    private RatingBar rbAvgProfessionalism, rbAvgReliability, rbAvgPrice;
    private LinearLayout galleryContainer;
    private FloatingActionButton btnFavorite;
    private Button btnWhatsApp, btnWaze, btnBookAppointment, btnAppChat;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentBusinessId;
    private BusinessModel currentBusiness;
    private boolean isFavorite = false;

    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private List<ReviewModel> reviewsList;

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי המסך, מחברת את רשימת הביקורות, שולפת את מזהה העסק שקיבלה ומפעילה מאזיני נתונים ולחיצות.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tvName = findViewById(R.id.tvDetailName);
        tvType = findViewById(R.id.tvDetailType);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvAddress = findViewById(R.id.tvDetailAddress);

        rbAvgProfessionalism = findViewById(R.id.rbAvgProfessionalism);
        rbAvgReliability = findViewById(R.id.rbAvgReliability);
        rbAvgPrice = findViewById(R.id.rbAvgPrice);

        galleryContainer = findViewById(R.id.galleryContainer);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnWhatsApp = findViewById(R.id.btnWhatsApp);
        btnWaze = findViewById(R.id.btnWaze);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnAppChat = findViewById(R.id.btnAppChat);

        // אתחול והגדרת רשימת הביקורות בצורה אנכית
        rvReviews = findViewById(R.id.rvReviewsList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewsList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewsList);
        rvReviews.setAdapter(reviewAdapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // שליפת מזהה העסק שהועבר במסך הקודם על מנת לדעת איזה מידע לטעון
        currentBusinessId = getIntent().getStringExtra("BUSINESS_ID");

        if (currentBusinessId != null) {
            listenToBusinessData(currentBusinessId);
            checkIfFavorite();
            loadReviews(currentBusinessId);
        }

        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavorite();
            }
        });

        btnBookAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BusinessDetailsActivity.this, BookingActivity.class);
                intent.putExtra("businessId", currentBusinessId);
                intent.putExtra("businessName", tvName.getText().toString());
                startActivity(intent);
            }
        });

        btnAppChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChat();
            }
        });

        btnWhatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWhatsApp();
            }
        });

        btnWaze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWaze();
            }
        });
    }

    /**
     * מה הפעולה עושה: מאזינה בזמן אמת לשינויים במסמך העסק הספציפי ומעדכנת את התצוגה בכל שינוי בבסיס הנתונים.
     * קלט: String businessId.
     * פלט: אין (void).
     */
    private void listenToBusinessData(String businessId) {
        db.collection("businesses").document(businessId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot doc, @Nullable FirebaseFirestoreException e) {
                        if (doc != null && doc.exists()) {
                            currentBusiness = doc.toObject(BusinessModel.class);
                            if (currentBusiness != null) {
                                updateUI(currentBusiness);
                            }
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: מזריקה את פרטי העסק וממוצעי הדירוגים לתוך רכיבי התצוגה ובונה מחדש את גלריית התמונות.
     * קלט: BusinessModel business.
     * פלט: אין (void).
     */
    private void updateUI(BusinessModel business) {
        tvName.setText(business.getName());
        tvType.setText(business.getBusinessType());
        tvPhone.setText(business.getPhone());
        tvDescription.setText(business.getDescription());
        tvAddress.setText(business.getAddress() != null ? business.getAddress() : "לא צוינה כתובת");

        rbAvgProfessionalism.setRating(business.getAvgProfessionalism());
        rbAvgReliability.setRating(business.getAvgReliability());
        rbAvgPrice.setRating(business.getAvgPrice());

        // ניקוי הגלריה הישנה קריטי כדי למנוע שכפול תמונות בכל פעם שהנתונים מתעדכנים
        galleryContainer.removeAllViews();
        if (business.getImageBlobs() != null) {
            for (Blob blob : business.getImageBlobs()) {
                addImageToGallery(blob);
            }
        }
    }

    /**
     * מה הפעולה עושה: מקבלת אובייקט תמונה בינארי (Blob), הופכת אותו ל-Bitmap ומייצרת ImageView באופן דינמי לתוך הגלריה.
     * קלט: Blob blob.
     * פלט: אין (void).
     */
    private void addImageToGallery(Blob blob) {
        byte[] bytes = blob.toBytes();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // יצירת רכיב תמונה בצורה תכנותית (ולא דרך XML) כדי לאפשר כמות תמונות דינמית ומשתנה
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600);
        params.setMargins(0, 0, 0, 30);

        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bitmap);

        galleryContainer.addView(imageView);
    }

    /**
     * מה הפעולה עושה: מאזינה בזמן אמת לאוסף הביקורות של העסק ומסדרת אותן לפי הזמן שלהן מהחדש לישן.
     * קלט: String businessId.
     * פלט: אין (void).
     */
    private void loadReviews(String businessId) {
        db.collection("reviews")
                .whereEqualTo("businessId", businessId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {
                        if (querySnapshot != null) {
                            reviewsList.clear();
                            for (DocumentSnapshot doc : querySnapshot) {
                                ReviewModel review = doc.toObject(ReviewModel.class);
                                if (review != null) reviewsList.add(review);
                            }
                            reviewAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: מייצרת מזהה חדר ייחודי המשלב את הלקוח ובעל העסק, ופותחת את מסך השיחה (ChatActivity).
     * קלט: אין.
     * פלט: אין (void).
     */
    private void openChat() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || currentBusiness == null) return;

        // בניית מזהה חדר ייחודי קבוע (UID_לקוח + UID_בעלים) כדי ששני הצדדים יגיעו תמיד לאותו החדר בדיוק
        String chatRoomId = user.getUid() + "_" + currentBusiness.getOwnerId();
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatRoomId", chatRoomId);
        startActivity(intent);
    }

    /**
     * מה הפעולה עושה: בודקת ב-Firestore האם העסק הנוכחי נמצא תחת תת-אוסף המועדפים (favorites) של המשתמש.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void checkIfFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).collection("favorites").document(currentBusinessId)
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        isFavorite = doc.exists();
                        updateFavoriteIcon();
                    }
                });
    }

    /**
     * מה הפעולה עושה: משנה את מצב המועדף (Toggle): מוחקת את העסק מהמועדפים אם היה קיים, או יוצרת מסמך חדש אם לא.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void toggleFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        DocumentReference favRef = db.collection("users").document(user.getUid()).collection("favorites").document(currentBusinessId);

        if (isFavorite) {
            favRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    isFavorite = false;
                    updateFavoriteIcon();
                }
            });
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("businessId", currentBusinessId);
            data.put("name", currentBusiness.getName());

            favRef.set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    isFavorite = true;
                    updateFavoriteIcon();
                }
            });
        }
    }

    /**
     * מה הפעולה עושה: מעדכנת את האייקון הגרפי של לחצן המועדפים (כוכב מלא או ריק) בהתאם למצב הנוכחי בזיכרון.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void updateFavoriteIcon() {
        btnFavorite.setImageTintList(null);
        if (isFavorite) {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            btnFavorite.setImageResource(android.R.drawable.star_off);
        }
    }

    /**
     * מה הפעולה עושה: מנקה ומסדרת את מספר הטלפון לפורמט בינלאומי, ומפעילה כוונת (Intent) לפתיחת שיחה מול העסק ב-WhatsApp.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void openWhatsApp() {
        if (currentBusiness == null || currentBusiness.getPhone() == null || currentBusiness.getPhone().isEmpty()) {
            Toast.makeText(this, "מספר טלפון לא זמין", Toast.LENGTH_SHORT).show();
            return;
        }

        // שימוש בביטוי רגולרי (\\D) כדי למחוק את כל התווים שאינם ספרות ולהימנע משגיאות חיוג
        String phone = currentBusiness.getPhone().replaceAll("\\D", "");

        if (phone.startsWith("0")) {
            phone = "972" + phone.substring(1);
        }

        String message = "שלום, הגעתי דרך אפליקציית JOBSY. אשמח לקבל פרטים נוספים!";
        String encodedMessage = Uri.encode(message);

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/" + phone + "?text=" + encodedMessage));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "ווצאפ לא מותקן על המכשיר", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * מה הפעולה עושה: בונה כתובת URI מבוססת קואורדינטות או כתובת טקסטואלית, ומפעילה כוונת חיצונית לפתיחת אפליקציית Waze.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void openWaze() {
        if (currentBusiness == null) return;

        String uriString;

        // עדיפות עליונה לניווט לפי קואורדינטות גיאוגרפיות מדויקות, ובמידה ואין - ניווט לפי כתובת טקסט
        if (currentBusiness.getLatitude() != null && currentBusiness.getLongitude() != null) {
            uriString = "waze://?ll=" + currentBusiness.getLatitude() + "," + currentBusiness.getLongitude() + "&navigate=yes";
        } else if (currentBusiness.getAddress() != null && !currentBusiness.getAddress().isEmpty()) {
            uriString = "waze://?q=" + Uri.encode(currentBusiness.getAddress()) + "&navigate=yes";
        } else {
            Toast.makeText(this, "לא הוגדר מיקום לעסק זה", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
            startActivity(intent);
        } catch (Exception e) {
            try {
                // מנגנון גיבוי: אם האפליקציה לא מותקנת, המשתמש מועבר ישירות לחנות להורדת Waze
                Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.waze"));
                startActivity(playStoreIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "וויז לא מותקן על המכשיר", Toast.LENGTH_SHORT).show();
            }
        }
    }
}