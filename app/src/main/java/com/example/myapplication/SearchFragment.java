package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView rvBusinesses;
    private BusinessAdapter businessAdapter;
    private TextView tvEmptyState;
    private SearchView searchView;
    private Spinner spinnerCategories;

    // המתג החדש שלנו
    private SwitchMaterial switchNearMe;

    private FirebaseFirestore db;

    // רשימה 1: כל העסקים (מסד הנתונים המלא)
    private List<BusinessModel> originalList = new ArrayList<>();
    // רשימה 2: מה שמוצג כרגע על המסך (אחרי סינון)
    private List<BusinessModel> displayedList = new ArrayList<>();

    private String currentSearchText = "";
    private String currentCategory = "הכל"; // ברירת מחדל

    // מצב המתג "קרוב אליי"
    private boolean isNearMeOnly = false;
    private static final float MAX_DISTANCE_METERS = 15000f; // 15 ק"מ במטרים

    // --- משתני מיקום ---
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        rvBusinesses = view.findViewById(R.id.rvBusinesses);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        searchView = view.findViewById(R.id.searchView);
        spinnerCategories = view.findViewById(R.id.spinnerCategories);
        switchNearMe = view.findViewById(R.id.switchNearMe); // חיבור המתג

        db = FirebaseFirestore.getInstance();

        // אתחול רכיב המיקום של גוגל
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (getContext() != null) {
            rvBusinesses.setLayoutManager(new LinearLayoutManager(getContext()));
            businessAdapter = new BusinessAdapter(getContext(), displayedList);
            rvBusinesses.setAdapter(businessAdapter);
        }

        setupFilters();
        loadBusinesses();

        // קריאה לבדיקת הרשאות וקבלת מיקום
        checkLocationPermissionAndFetch();

        return view;
    }

    // פונקציה לבדיקת הרשאות
    private void checkLocationPermissionAndFetch() {
        if (getContext() == null) return;

        // האם יש לנו כבר הרשאה?
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // אם אין, מבקשים מהמשתמש (מקפיץ חלון)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // אם יש, שולפים את המיקום
            fetchUserLocation();
        }
    }

    // תפיסת התשובה של המשתמש מהחלון הקופץ (אישר או דחה)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // המשתמש אישר! נשלוף את המיקום
                fetchUserLocation();
            } else {
                Toast.makeText(getContext(), "ללא הרשאת מיקום, לא נוכל לסנן לפי מרחק", Toast.LENGTH_SHORT).show();
                switchNearMe.setChecked(false); // מכבים את המתג אם אין הרשאה
            }
        }
    }

    // קבלת המיקום בפועל
    @SuppressLint("MissingPermission")
    private void fetchUserLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation = location;
                // אם מצאנו מיקום, נרענן את הסינון
                applyFilters();
            }
        });
    }

    private void setupFilters() {
        if (getContext() == null) return;

        List<String> categories = new ArrayList<>();
        categories.add("הכל");

        String[] typesArray = getResources().getStringArray(R.array.business_types);
        categories.addAll(Arrays.asList(typesArray));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategories.setAdapter(spinnerAdapter);

        spinnerCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchText = newText;
                applyFilters();
                return true;
            }
        });

        // --- מאזין למתג "קרוב אליי" ---
        switchNearMe.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isNearMeOnly = isChecked;

            // אם הדליקו את המתג אבל עדיין אין לנו מיקום, ננסה לבקש שוב
            if (isNearMeOnly && userLocation == null) {
                Toast.makeText(getContext(), "מנסה לאתר את מיקומך... ודא ששירותי המיקום (GPS) דולקים", Toast.LENGTH_SHORT).show();
                checkLocationPermissionAndFetch();
            }

            applyFilters();
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
                            originalList.add(business);
                        }
                    }
                    applyFilters();
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
            // 1. בדיקת טקסט חיפוש
            String bName = business.getName() != null ? business.getName() : "";
            boolean matchesSearch = bName.toLowerCase().contains(currentSearchText.toLowerCase());

            // 2. בדיקת קטגוריה
            String bType = business.getBusinessType() != null ? business.getBusinessType() : "";
            boolean matchesCategory = currentCategory.equals("הכל") || bType.equals(currentCategory);

            // 3. בדיקת מרחק (החדש!)
            boolean matchesDistance = true; // נניח שכן, אלא אם יוכח אחרת

            if (isNearMeOnly) {
                // אם המתג דלוק אבל אין מיקום לקוח או לעסק, אנחנו לא יכולים להוכיח שהוא קרוב, אז נסנן אותו
                if (userLocation == null || business.getLatitude() == null || business.getLongitude() == null) {
                    matchesDistance = false;
                } else {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            userLocation.getLatitude(), userLocation.getLongitude(),
                            business.getLatitude(), business.getLongitude(),
                            results
                    );

                    // אם המרחק גדול מ-15 ק"מ (15,000 מטר)
                    if (results[0] > MAX_DISTANCE_METERS) {
                        matchesDistance = false;
                    }
                }
            }

            // אם עבר את כל הסינונים - נוסיף לרשימה
            if (matchesSearch && matchesCategory && matchesDistance) {
                displayedList.add(business);
            }
        }

        // --- כאן קורה קסם המיון לפי מיקום (מי הכי קרוב יופיע ראשון) ---
        if (userLocation != null) {
            Collections.sort(displayedList, new Comparator<BusinessModel>() {
                @Override
                public int compare(BusinessModel b1, BusinessModel b2) {
                    Double lat1 = b1.getLatitude();
                    Double lon1 = b1.getLongitude();
                    Double lat2 = b2.getLatitude();
                    Double lon2 = b2.getLongitude();

                    // אם לעסק מסוים אין מיקום במערכת, נזרוק אותו לסוף הרשימה
                    if (lat1 == null || lon1 == null) return 1;
                    if (lat2 == null || lon2 == null) return -1;

                    // חישוב מרחק לעסק הראשון
                    float[] results1 = new float[1];
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), lat1, lon1, results1);

                    // חישוב מרחק לעסק השני
                    float[] results2 = new float[1];
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), lat2, lon2, results2);

                    // השוואה מי יותר קרוב
                    return Float.compare(results1[0], results2[0]);
                }
            });
        }

        if (businessAdapter != null) {
            businessAdapter.setBusinesses(displayedList);
            businessAdapter.notifyDataSetChanged();
        }

        if (displayedList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvBusinesses.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvBusinesses.setVisibility(View.VISIBLE);
        }
    }
}