package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchFragment extends Fragment {

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // טעינת העיצוב (XML)
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // חיבור רכיבים (משתמשים ב-view.)
        rvBusinesses = view.findViewById(R.id.rvBusinesses);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        searchView = view.findViewById(R.id.searchView);
        spinnerCategories = view.findViewById(R.id.spinnerCategories);

        db = FirebaseFirestore.getInstance();

        // הגדרת RecyclerView
        // שימי לב: אנחנו משתמשים ב-getContext() במקום ב-this
        if (getContext() != null) {
            rvBusinesses.setLayoutManager(new LinearLayoutManager(getContext()));
            businessAdapter = new BusinessAdapter(getContext(), displayedList);
            rvBusinesses.setAdapter(businessAdapter);
        }

        setupFilters();
        loadBusinesses();

        return view;
    }

    private void setupFilters() {
        if (getContext() == null) return;

        // 1. הגדרת הספינר (קטגוריות)
        List<String> categories = new ArrayList<>();
        categories.add("הכל");

        // טעינת המערך מ-strings.xml
        String[] typesArray = getResources().getStringArray(R.array.business_types);
        categories.addAll(Arrays.asList(typesArray));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
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

    private void loadBusinesses() {
        db.collection("businesses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    originalList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BusinessModel business = doc.toObject(BusinessModel.class);
                        if (business != null) {
                            // אנחנו שומרים גם את ה-ID של המסמך במודל אם צריך
                            // business.setId(doc.getId());
                            originalList.add(business);
                        }
                    }
                    applyFilters(); // הצגת הנתונים אחרי הטעינה
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "שגיאה בטעינת עסקים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilters() {
        displayedList.clear();

        for (BusinessModel business : originalList) {
            // 1. בדיקת שם (מגן מפני קריסה אם השם ריק)
            String bName = business.getName() != null ? business.getName() : "";
            boolean matchesSearch = bName.toLowerCase().contains(currentSearchText.toLowerCase());

            // 2. בדיקת קטגוריה
            String bType = business.getBusinessType() != null ? business.getBusinessType() : "";
            boolean matchesCategory = currentCategory.equals("הכל") ||
                    bType.equals(currentCategory);

            if (matchesSearch && matchesCategory) {
                displayedList.add(business);
            }
        }

        // עדכון האדפטר
        if (businessAdapter != null) {
            businessAdapter.setBusinesses(displayedList);
            businessAdapter.notifyDataSetChanged(); // רענון חזותי
        }

        // ניהול מצב "ריק"
        if (displayedList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvBusinesses.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvBusinesses.setVisibility(View.VISIBLE);
        }
    }
}