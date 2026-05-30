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

// הגדרת מחלקת מסך פרטי העסק, היורשת מ-BaseActivity
public class BusinessDetailsActivity extends BaseActivity {

    // הצהרה על רכיבי תצוגת הטקסט עבור פרטי המידע של העסק
    private TextView tvName, tvType, tvPhone, tvDescription, tvAddress;
    // הצהרה על שלושת רכיבי מדדי הדירוג הגרפיים (RatingBar) המציגים ממוצעי ביקורות
    private RatingBar rbAvgProfessionalism, rbAvgReliability, rbAvgPrice;
    // הצהרה על מכולה אנכית (LinearLayout) המשמשת כגלריית תמונות דינמית
    private LinearLayout galleryContainer;
    // הצהרה על לחצן צף להוספה או הסרה של העסק מרשימת המועדפים
    private FloatingActionButton btnFavorite;
    // הצהרה על לחצני הפעולה המקשרים לאפליקציות חיצוניות ומסכים פנימיים
    private Button btnWhatsApp, btnWaze, btnBookAppointment, btnAppChat;

    // רכיבי הגישה המרכזיים של פיירסטור ומערכת ניהול המשתמשים
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    // משתני מחרוזת ומודל להחזקת המזהה והנתונים של העסק הנוכחי המציג במסך
    private String currentBusinessId;
    private BusinessModel currentBusiness;
    // משתנה בוליאני המציין האם העסק הנוכחי מסומן כמועדף אצל המשתמש המחובר
    private boolean isFavorite = false;

    // רכיבי רשימת הביקורות והמתאם המותאם אישית המחובר אליה
    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private List<ReviewModel> reviewsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת וחיבור קובץ ה-XML של עיצוב מסך פרטי העסק
        setContentView(R.layout.activity_business_details);

        // חיבור וקישור סרגל הכלים העליון של המסך
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // ביטול כותרת ברירת המחדל של סרגל הכלים במידה והוא קיים לקוד
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // קישור משתני הרכיבים לרכיבים הויזואליים מתוך קובץ ה-XML
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

        // קישור והגדרת מנהל פריסה אנכי לרשימת חוות הדעת והביקורות
        rvReviews = findViewById(R.id.rvReviewsList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewsList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewsList);
        rvReviews.setAdapter(reviewAdapter);

        // קבלת מופעי הגישה אל בסיס הנתונים ומערכת האימות
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        // שליפת מזהה העסק הספציפי שהועבר באמצעות ה-Intent מהמסך הקודם
        currentBusinessId = getIntent().getStringExtra("BUSINESS_ID");

        if (currentBusinessId != null) {
            // האזנה אקטיבית ורציפה לשינויים במסמך העסק בענן
            listenToBusinessData(currentBusinessId);
            // בדיקה ראשונית האם העסק מסומן כמועדף בתיקיית המשתמש המחובר
            checkIfFavorite();
            // טעינה והאזנה לרשימת הביקורות שנכתבו על עסק זה
            loadReviews(currentBusinessId);
        }

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור המועדפים
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavorite();
            }
        });

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור מעבר למסך קביעת תור
        btnBookAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BusinessDetailsActivity.this, BookingActivity.class);
                intent.putExtra("businessId", currentBusinessId);
                intent.putExtra("businessName", tvName.getText().toString());
                startActivity(intent);
            }
        });

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור פתיחת הצ'אט הפנימי באפליקציה
        btnAppChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChat();
            }
        });

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור פתיחת אפליקציית WhatsApp
        btnWhatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWhatsApp();
            }
        });

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור הניווט באמצעות אפליקציית Waze
        btnWaze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWaze();
            }
        });
    }

    // פעולה פרטית המגדירה מאזין קבוע (נוטיפייר) המעדכן את המסך מיידית בכל שינוי דאטה בענן
    private void listenToBusinessData(String businessId) {
        db.collection("businesses").document(businessId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot doc, @Nullable FirebaseFirestoreException e) {
                        if (doc != null && doc.exists()) {
                            // המרת מסמך הפיירסטור ישירות לאובייקט מסוג מודל עסק
                            currentBusiness = doc.toObject(BusinessModel.class);
                            if (currentBusiness != null) {
                                // קריאה לפעולת עדכון הרכיבים הויזואליים על המסך
                                updateUI(currentBusiness);
                            }
                        }
                    }
                });
    }

    // פעולה פרטית המקבלת את מודל העסק ומזריקה את ערכיו לתוך רכיבי ממשק המשתמש
    private void updateUI(BusinessModel business) {
        tvName.setText(business.getName());
        tvType.setText(business.getBusinessType());
        tvPhone.setText(business.getPhone());
        tvDescription.setText(business.getDescription());
        tvAddress.setText(business.getAddress() != null ? business.getAddress() : "לא צוינה כתובת");

        // עדכון גרפי של שלושת מדדי הדירוג השונים (מקצועיות, אמינות ומחיר)
        rbAvgProfessionalism.setRating(business.getAvgProfessionalism());
        rbAvgReliability.setRating(business.getAvgReliability());
        rbAvgPrice.setRating(business.getAvgPrice());

        // ניקוי הגלריה הישנה לפני טעינת והוספת התמונות המעודכנות
        galleryContainer.removeAllViews();
        if (business.getImageBlobs() != null) {
            // מעבר בלולאה על רשימת הביטים של התמונות המאוחסנות במסמך העסק
            for (Blob blob : business.getImageBlobs()) {
                addImageToGallery(blob);
            }
        }
    }

    // פעולה פרטית המעבדת אובייקט Blob של תמונה, הופכת אותו לביטמפ ומזריקה אותו לתצוגה
    private void addImageToGallery(Blob blob) {
        // המרת המבנה של פיירבייס למערך של בייטים גולמיים
        byte[] bytes = blob.toBytes();
        // פענוח והמרת מערך הבייטים לאובייקט תמונה מסוג Bitmap הניתן להצגה בנייטיב
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        ImageView imageView = new ImageView(this);
        // הגדרת מאפייני פריסה דינמיים לתמונה: רוחב מלא וגובה קבוע של 600 פיקסלים
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600);
        params.setMargins(0, 0, 0, 30); // הוספת מרווח תחתון בין תמונה לתמונה
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // חיתוך ומרכוז אופטימלי של התמונה
        imageView.setImageBitmap(bitmap);
        // הוספה פיזית של רכיב התמונה החדש אל תוך מכולת הגלריה ב-XML
        galleryContainer.addView(imageView);
    }

    // פעולה פרטית הטוענת ומאזינה בזמן אמת לכל הביקורות המשויכות לעסק זה בסדר יורד
    private void loadReviews(String businessId) {
        db.collection("reviews")
                .whereEqualTo("businessId", businessId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {
                        if (querySnapshot != null) {
                            reviewsList.clear();
                            // ריצה בלולאה על כל מסמכי הביקורות שנמצאו תחת הסינון
                            for (DocumentSnapshot doc : querySnapshot) {
                                ReviewModel review = doc.toObject(ReviewModel.class);
                                if (review != null) reviewsList.add(review);
                            }
                            // הודעה למתאם הרשימה לבצע ריענון ויזואלי של שורות הביקורות במסך
                            reviewAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    // פעולה פרטית המחשבת את מזהה חדר הצ'אט הייחודי ופותחת את מסך הדו-שיח הפנימי
    private void openChat() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || currentBusiness == null) return;
        // יצירת מזהה חדר צ'אט ייחודי המורכב מ-UID הלקוח ומ-UID בעל העסק מופרדים בקו תחתון
        String chatRoomId = user.getUid() + "_" + currentBusiness.getOwnerId();
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatRoomId", chatRoomId);
        startActivity(intent);
    }

    // פעולה פרטית הבודקת האם קיים מסמך לעסק הנוכחי תחת אוסף המועדפים של המשתמש בענן
    private void checkIfFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).collection("favorites").document(currentBusinessId)
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        // אם המסמך קיים – העסק מועדף (true), אחרת לא (false)
                        isFavorite = doc.exists();
                        // קריאה לפעולה המשנה את מראה האייקון של הכוכב בהתאם
                        updateFavoriteIcon();
                    }
                });
    }

    // פעולה פרטית המוסיפה או מסירה את העסק מאוסף המועדפים האישי של המשתמש
    private void toggleFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        // יצירת הפנייה מדויקת למיקום מסמך המועדף הספציפי בתוך אוסף המשתמש
        DocumentReference favRef = db.collection("users").document(user.getUid()).collection("favorites").document(currentBusinessId);

        if (isFavorite) {
            // אם העסק כבר היה מועדף, נמחק את המסמך שלו מהענן
            favRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    isFavorite = false;
                    updateFavoriteIcon();
                }
            });
        } else {
            // אם העסק לא היה מועדף, ניצור מפת נתונים ונשמור מסמך חדש בענן עבורו
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

    // פעולה פרטית האחראית על החלפת המראה החזותי של כפתור הכוכב (מלא / ריק)
    private void updateFavoriteIcon() {
        btnFavorite.setImageTintList(null); // ביטול גוון אוטומטי לטובת הצבע המקורי של המשאב
        if (isFavorite) {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_on); // הגדרת תמונת כוכב מלא וצהוב
        } else {
            btnFavorite.setImageResource(android.R.drawable.star_off); // הגדרת תמונת כוכב ריק ואפור
        }
    }

    // פעולה פרטית המכינה ומקשרת את המשתמש ישירות לשיחת WhatsApp מול בעל העסק
    private void openWhatsApp() {
        if (currentBusiness == null || currentBusiness.getPhone() == null || currentBusiness.getPhone().isEmpty()) {
            Toast.makeText(this, "מספר טלפון לא זמין", Toast.LENGTH_SHORT).show();
            return;
        }

        // ניקוי מחרוזת הטלפון מכל תו שאינו ספרה (רווחים, מקפים וכו') בעזרת ביטוי רגולרי
        String phone = currentBusiness.getPhone().replaceAll("\\D", "");

        // התאמת הקידומת לפורמט בינלאומי: החלפת ה-0 הראשון בקידומת המדינה של ישראל (972)
        if (phone.startsWith("0")) {
            phone = "972" + phone.substring(1);
        }

        // ניסוח הודעת הפתיחה המוכנת מראש שתופיע בתיבת הטקסט של המשתמשת בוואטסאפ
        String message = "שלום, הגעתי דרך אפליקציית JOBSY. אשמח לקבל פרטים נוספים!";
        // קידוד מחרוזת ההודעה לפורמט URL תקני כדי למנוע קריסות או בעיות עם תווים מיוחדים ורווחים
        String encodedMessage = Uri.encode(message);

        try {
            // יצירת כוונת (Intent) להצגת תוכן חזותי חיצוני
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // הגדרת כתובת ה-API הרשמית של וואטסאפ המשלבת את מספר הטלפון והטקסט המקודד
            intent.setData(Uri.parse("https://wa.me/" + phone + "?text=" + encodedMessage));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "ווצאפ לא מותקן על המכשיר", Toast.LENGTH_SHORT).show();
        }
    }

    // פעולה פרטית המייצרת כוונת ניווט ייעודית ומפעילה את אפליקציית Waze במכשיר
    private void openWaze() {
        if (currentBusiness == null) return;

        String uriString;

        // עדיפות ראשונה: ניווט מדויק ומבוסס נקודות ציון (קו אורך ורוחב מספריים) מתוך המודל
        if (currentBusiness.getLatitude() != null && currentBusiness.getLongitude() != null) {
            uriString = "waze://?ll=" + currentBusiness.getLatitude() + "," + currentBusiness.getLongitude() + "&navigate=yes";
        }
        // עדיפות שנייה: ניווט מבוסס חיפוש טקסטואלי של כתובת המגורים/עסק של בעל המקצוע
        else if (currentBusiness.getAddress() != null && !currentBusiness.getAddress().isEmpty()) {
            uriString = "waze://?q=" + Uri.encode(currentBusiness.getAddress()) + "&navigate=yes";
        }
        else {
            Toast.makeText(this, "לא הוגדר מיקום לעסק זה", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // הפעלת אפליקציית Waze החיצונית באמצעות שליחת הקישור הפרוטוקולי המיוחד
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
            startActivity(intent);
        } catch (Exception e) {
            try {
                // מנגנון הגנה: אם וויז לא מותקנת, נקפיץ למשתמש ישירות את עמוד ההורדה שלה בחנות האפליקציות Play Store
                Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.waze"));
                startActivity(playStoreIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "וויז לא מותקן על המכשיר", Toast.LENGTH_SHORT).show();
            }
        }
    }
}