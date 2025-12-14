package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BusinessAdapter adapter; // משתמשים באדפטר הקיים והמושקע
    private List<BusinessModel> favoritesList;
    private TextView tvEmptyState;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites); // וודאי שיש לך קובץ layout לזה (ראי למטה)

        recyclerView = findViewById(R.id.recyclerViewFavorites);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // אתחול Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // הגדרת ה-RecyclerView וה-Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesList = new ArrayList<>();
        adapter = new BusinessAdapter(this, favoritesList); // שימוש ב-BusinessAdapter שלך
        recyclerView.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "יש להתחבר כדי לצפות במועדפים", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // 1. קודם כל ניגשים לרשימת המועדפים של המשתמש כדי להשיג את ה-IDs
        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState();
                    } else {
                        favoritesList.clear();
                        hideEmptyState();

                        // 2. לולאה שעוברת על כל מסמך במועדפים
                        for (DocumentSnapshot favDoc : queryDocumentSnapshots) {
                            String businessId = favDoc.getString("businessId");

                            if (businessId != null) {
                                fetchFullBusinessDetails(businessId);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת מועדפים", Toast.LENGTH_SHORT).show());
    }

    // פונקציה שטוענת את הפרטים המלאים (כולל תמונות) מאוסף העסקים הראשי
    private void fetchFullBusinessDetails(String businessId) {
        db.collection("businesses").document(businessId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        BusinessModel business = documentSnapshot.toObject(BusinessModel.class);
                        if (business != null) {
                            // אם אין ID בתוך האובייקט, נכניס אותו ידנית
                            if (business.getBusinessId() == null) {
                                business.setBusinessId(documentSnapshot.getId());
                            }

                            favoritesList.add(business);
                            // מעדכנים את האדפטר שנוספה רשומה חדשה
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void showEmptyState() {
        tvEmptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        tvEmptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}