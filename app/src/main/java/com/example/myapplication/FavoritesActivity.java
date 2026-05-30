package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

// הגדרת מחלקת מסך העסקים המועדפים של הלקוח, היורשת מ-BaseActivity
public class FavoritesActivity extends BaseActivity {

    // הצהרה על רכיב הרשימה הממוחזרת להצגת כרטיסי העסקים המועדפים
    private RecyclerView recyclerView;
    // הצהרה על המתאם (Adapter) הקיים במערכת להצגת בתי עסק
    private BusinessAdapter adapter;
    // רשימה דינמית מסוג ArrayList לשמירת אובייקטי מודל העסקים בזיכרון
    private List<BusinessModel> favoritesList;
    // רכיב טקסט המוצג למשתמש רק כאשר רשימת המועדפים ריקה
    private TextView tvEmptyState;

    // רכיבי הגישה הרשמיים לעבודה מול שירותי האימות ומסד הנתונים של פיירבייס
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת וחיבור קובץ ה-XML של עיצוב מסך המועדפים
        setContentView(R.layout.activity_favorites);

        // חיבור וקישור סרגל הכלים העליון של המסך
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // ביטול כותרת ברירת המחדל של סרגל הכלים במידה והוא קיים
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // קישור רכיבי הממשק מה-XML למשתני המחלקה
        recyclerView = findViewById(R.id.recyclerViewFavorites);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // אתחול מופעי הגישה אל פיירסטור ומערכת האימות
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // הגדרת מנהל פריסה אנכי סטנדרטי עבור רכיב הרשימה
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // אתחול הרשימה המקומית ויצירת המתאם תוך שימוש במתאם העסקים המרכזי
        favoritesList = new ArrayList<>();
        adapter = new BusinessAdapter(this, favoritesList);
        recyclerView.setAdapter(adapter);

        // זימון הפעולה האחראית על טעינת וסינכרון בתי העסק המועדפים מהענן
        loadFavorites();
    }

    // פעולה פרטית הניגשת לתת-אוסף המועדפים של המשתמש ושולפת את מזהי העסקים
    private void loadFavorites() {
        // הגנה: בדיקה האם קיים משתמש מחובר למערכת, במידה ולא - המסך נסגר
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "יש להתחבר כדי לצפות במועדפים", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // שליפת מזהה ה-UID הייחודי של המשתמש (הלקוח) המחובר כעת
        String userId = auth.getCurrentUser().getUid();

        // שלב א': פנייה לתת-אוסף הפנימי "favorites" הנמצא בתוך מסמך המשתמש באוסף "users"
        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // במידה ולא חזרו מסמכים (רשימת המועדפים ריקה במערכת)
                        if (queryDocumentSnapshots.isEmpty()) {
                            showEmptyState(); // קריאה לפעולת הצגת מסך ריק
                        } else {
                            // ניקוי הרשימה המקומית בזיכרון למניעת כפילויות של מידע ישן
                            favoritesList.clear();
                            hideEmptyState(); // הסתרת הודעת המסך הריק

                            // שלב ב: מעבר בלולאה מובנית על כל מסמכי קישורי המועדפים שחזרו מהענן
                            for (DocumentSnapshot favDoc : queryDocumentSnapshots) {
                                // חילוץ מחרוזת מזהה העסק מתוך שדה המסמך הנוכחי בלולאה
                                String businessId = favDoc.getString("businessId");

                                // הגנה: הפעלת פונקציית שליפת המידע המלא רק במידה והמזהה תקין ואינו Null
                                if (businessId != null) {
                                    fetchFullBusinessDetails(businessId);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(FavoritesActivity.this, "שגיאה בטעינת מועדפים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // פעולה פרטית המבצעת שליפה אסינכרונית וממוקדת של פרטי העסק המלאים מתוך האוסף הראשי
    private void fetchFullBusinessDetails(String businessId) {
        // פנייה ישירה לאוסף העסקים הכללי בענן עבור מסמך העסק הספציפי
        db.collection("businesses").document(businessId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // בדיקה שאכן קיים מסמך פיזי עבור עסק זה בבסיס הנתונים
                        if (documentSnapshot.exists()) {
                            // המרת מסמך הנתונים הגולמי מהפיירסטור ישירות לאובייקט מסוג מודל העסק
                            BusinessModel business = documentSnapshot.toObject(BusinessModel.class);
                            if (business != null) {
                                // במידה והמזהה אינו מעודכן בתוך שדות האובייקט, נזריק אותו ידנית מתוך מזהה המסמך בענן
                                if (business.getBusinessId() == null) {
                                    business.setBusinessId(documentSnapshot.getId());
                                }

                                // הוספת אובייקט העסק המלא אל רשימת התצוגה המקומית בזיכרון
                                favoritesList.add(business);
                                // עדכון מתאם הרשימה על כך שנוספה רשומה חדשה כדי שיצייר אותה חזותית על המסך
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    // פעולה פרטית המנהלת את נראות הממשק ומציגה את הודעת ה"אין נתונים" על המסך
    private void showEmptyState() {
        tvEmptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    // פעולה פרטית המנהלת את נראות הממשק ומציגה את רכיב הרשימה הממוחזרת על המסך
    private void hideEmptyState() {
        tvEmptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}