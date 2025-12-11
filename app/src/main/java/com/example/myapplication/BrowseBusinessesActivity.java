package com.example.myapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BrowseBusinessesActivity extends BaseActivity {

    private RecyclerView rvBusinesses;
    private BusinessAdapter businessAdapter;
    private List<BusinessModel> businessesList = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_businesses);

        // Toolbar עם חץ חזרה
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("עסקים זמינים");
        }

        // RecyclerView
        rvBusinesses = findViewById(R.id.rvBusinesses);
        rvBusinesses.setLayoutManager(new LinearLayoutManager(this));
        businessAdapter = new BusinessAdapter(this, businessesList);
        rvBusinesses.setAdapter(businessAdapter);

        // Firestore
        db = FirebaseFirestore.getInstance();

        loadBusinesses();
    }

    private void loadBusinesses() {
        db.collection("businesses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    businessesList.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BusinessModel business = doc.toObject(BusinessModel.class);
                        if (business != null) {
                            businessesList.add(business);
                        }
                    }

                    businessAdapter.notifyDataSetChanged();

                    if (businessesList.isEmpty()) {
                        Toast.makeText(this, "אין עדיין עסקים להצגה", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת עסקים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
