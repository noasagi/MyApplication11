package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BrowseBusinessesActivity extends BaseActivity {

    private RecyclerView rvBusinesses;
    private BusinessAdapter businessAdapter;
    private TextView tvEmptyState;
    private SearchView searchView;
    private Spinner spinnerCategories;

    private FirebaseFirestore db;

    // רשימה 1: כל העסקים (מסד הנתונים המלא)
    private List<BusinessModel> originalList = new ArrayList<>();
    // רשימה 2: מה שמוצג כרגע על המסך (אחרי סינון)
    private List<BusinessModel> displayedList = new ArrayList<>();

    private String currentSearchText = "";
    private String currentCategory = "הכל"; // ברירת מחדל

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_businesses);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // הגדרת Toolbar
        Toolbar toolbar1 = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar1, true);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("חיפוש עסקים");
        }

        // חיבור רכיבים
        rvBusinesses = findViewById(R.id.rvBusinesses);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        searchView = findViewById(R.id.searchView);
        spinnerCategories = findViewById(R.id.spinnerCategories);

        // הגדרת RecyclerView
        rvBusinesses.setLayoutManager(new LinearLayoutManager(this));
        businessAdapter = new BusinessAdapter(this, displayedList); // האדפטר עובד מול הרשימה המוצגת
        rvBusinesses.setAdapter(businessAdapter);

        db = FirebaseFirestore.getInstance();

        setupFilters();
        loadBusinesses();
    }

    private void setupFilters() {
        // 1. הגדרת הספינר (קטגוריות)
        // אנחנו לוקחים את רשימת הקטגוריות מה-strings.xml ומוסיפים לה "הכל" בהתחלה
        List<String> categories = new ArrayList<>();
        categories.add("הכל"); // אפשרות לראות את כולם
        String[] typesArray = getResources().getStringArray(R.array.business_types);
        categories.addAll(Arrays.asList(typesArray));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategories.setAdapter(spinnerAdapter);

        // מאזין לבחירת קטגוריה
        spinnerCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories.get(position);
                applyFilters(); // הפעלת הסינון
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 2. הגדרת החיפוש
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchText = newText;
                applyFilters(); // הפעלת הסינון בכל הקלדת אות
                return true;
            }
        });
    }

    // פונקציה ראשית שטוענת את כל המידע פעם אחת
    private void loadBusinesses() {
        db.collection("businesses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    originalList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BusinessModel business = doc.toObject(BusinessModel.class);
                        if (business != null) {
                            originalList.add(business);
                        }
                    }

                    // בהתחלה מציגים את הכל
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת עסקים", Toast.LENGTH_SHORT).show();
                });
    }

    // המוח של הסינון: בודק גם טקסט וגם קטגוריה
    private void applyFilters() {
        displayedList.clear();

        for (BusinessModel business : originalList) {
            // 1. בדיקת שם (האם השם מכיל את הטקסט שהוקלד?)
            boolean matchesSearch = business.getName().toLowerCase().contains(currentSearchText.toLowerCase());

            // 2. בדיקת קטגוריה (האם הקטגוריה תואמת או שנבחר "הכל"?)
            boolean matchesCategory = currentCategory.equals("הכל") ||
                    business.getBusinessType().equals(currentCategory);

            // אם שני התנאים מתקיימים - מוסיפים לרשימה המוצגת
            if (matchesSearch && matchesCategory) {
                displayedList.add(business);
            }
        }

        // עדכון האדפטר
        businessAdapter.setBusinesses(displayedList);

        // הצגת הודעה "לא נמצאו תוצאות" אם צריך
        if (displayedList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvBusinesses.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvBusinesses.setVisibility(View.VISIBLE);
        }
    }
}