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

public class FavoritesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private BusinessAdapter adapter;
    private List<BusinessModel> favoritesList;
    private TextView tvEmptyState;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי המסך, מגדירה את סרגל הכלים (Toolbar), ומקשרת את ה-RecyclerView לאדפטר העסקים המרכזי.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        recyclerView = findViewById(R.id.recyclerViewFavorites);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        favoritesList = new ArrayList<>();
        adapter = new BusinessAdapter(this, favoritesList);
        recyclerView.setAdapter(adapter);

        loadFavorites();
    }

    /**
     * מה הפעולה עושה: פונה לתת-האוסף (Sub-collection) של העסקים המועדפים השמור תחת מסמך המשתמש הנוכחי, ומחלצת את מזהי העסקים (businessId).
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadFavorites() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "יש להתחבר כדי לצפות במועדפים", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // גישה מובנית למבנה נתונים מקונן: Users - [userId] -favorites
        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.isEmpty()) {
                            showEmptyState(); // טיפול במצב שבו רשימת המועדפים ריקה
                        } else {
                            favoritesList.clear(); // ניקוי מקומי למניעת כפילויות תצוגה בריענון
                            hideEmptyState();

                            // לולאה על פני מזהי העסקים שחזרו מתת-האוסף
                            for (DocumentSnapshot favDoc : queryDocumentSnapshots) {
                                String businessId = favDoc.getString("businessId");
                                if (businessId != null) {
                                    fetchFullBusinessDetails(businessId); // שלב ב': קריאה לפרטים המלאים
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

    /**
     * מה הפעולה עושה: מבצעת שאילתה אסינכרונית ממוקדת מול אוסף העסקים הראשי (businesses) כדי למשוך את אובייקט הנתונים המלא של העסק ולהציגו ברשימה.
     * קלט: String businessId.
     * פלט: אין (void).
     */
    private void fetchFullBusinessDetails(String businessId) {
        db.collection("businesses").document(businessId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            BusinessModel business = documentSnapshot.toObject(BusinessModel.class);
                            if (business != null) {
                                if (business.getBusinessId() == null) {
                                    business.setBusinessId(documentSnapshot.getId()); // הגנת שלמות נתונים
                                }
                                favoritesList.add(business);
                                adapter.notifyDataSetChanged(); // ריענון חזותי של הרשימה עם הגעת נתוני העסק
                            }
                        }
                    }
                });
    }

    // --- פעולות עזר פרטיות לניהול מצבי נראות הממשק (UI States) ---

    private void showEmptyState() {
        tvEmptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        tvEmptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}