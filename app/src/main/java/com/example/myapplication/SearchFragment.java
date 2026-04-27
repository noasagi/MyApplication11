package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView rvBusinesses;
    private BusinessAdapter businessAdapter;
    private TextView tvEmptyState;
    private SearchView searchView;
    private MaterialButton btnFilter;

    private FirebaseFirestore db;
    private ListenerRegistration businessListener;

    private List<BusinessModel> originalList = new ArrayList<>();
    private List<BusinessModel> displayedList = new ArrayList<>();

    // --- פרמטרים לסינון ---
    private String currentSearchText = "";
    private String currentCategoryFilter = "הכל";
    private float maxDistanceKmFilter = 50f; // ברירת מחדל: 50 ק"מ
    private float minRatingFilter = 0f;      // ברירת מחדל: 0 כוכבים

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
        btnFilter = view.findViewById(R.id.btnFilter);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (getContext() != null) {
            rvBusinesses.setLayoutManager(new LinearLayoutManager(getContext()));
            businessAdapter = new BusinessAdapter(getContext(), displayedList);
            rvBusinesses.setAdapter(businessAdapter);
        }

        setupSearch();
        btnFilter.setOnClickListener(v -> showFilterDialog());

        startListeningForBusinesses();

        // מבקשים מיקום מיד כדי שיהיה מוכן לחישובי מרחק
        checkLocationPermissionAndFetch();

        return view;
    }

    private void startListeningForBusinesses() {
        businessListener = db.collection("businesses")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) return;
                    if (querySnapshot != null) {
                        originalList.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            BusinessModel business = doc.toObject(BusinessModel.class);
                            if (business != null) {
                                business.setBusinessId(doc.getId());
                                originalList.add(business);
                            }
                        }
                        applyFilters();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (businessListener != null) businessListener.remove();
    }

    // --- תיקון: דגימת מיקום מדויקת וחדשה ---
    private void checkLocationPermissionAndFetch() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchUserLocationNow();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchUserLocationNow() {
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLocation = location;
                        applyFilters(); // מעדכן רשימה עכשיו כשיש מיקום
                    }
                });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                currentSearchText = newText;
                applyFilters();
                return true;
            }
        });
    }

    // --- חלון הסינון ---
    private void showFilterDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter, null);

        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerDialogCategory);
        Slider sliderDistance = dialogView.findViewById(R.id.sliderDistance);
        Slider sliderRating = dialogView.findViewById(R.id.sliderRating);
        TextView tvDistanceLabel = dialogView.findViewById(R.id.tvDistanceLabel);
        TextView tvRatingLabel = dialogView.findViewById(R.id.tvRatingLabel);
        Button btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);

        // הגדרת רשימת הקטגוריות בספינר
        List<String> categories = new ArrayList<>();
        categories.add("הכל");
        categories.addAll(Arrays.asList(getResources().getStringArray(R.array.business_types)));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);
        spinnerCategory.setSelection(categories.indexOf(currentCategoryFilter));

        // הגדרת ערכים נוכחיים בסליידרים
        sliderDistance.setValue(maxDistanceKmFilter);
        tvDistanceLabel.setText("עד " + (int)maxDistanceKmFilter + " ק\"מ");

        sliderRating.setValue(minRatingFilter);
        tvRatingLabel.setText("מדירוג " + minRatingFilter + " ומעלה");

        // עדכון טקסט תוך כדי גרירה
        sliderDistance.addOnChangeListener((slider, value, fromUser) -> tvDistanceLabel.setText("עד " + (int)value + " ק\"מ"));
        sliderRating.addOnChangeListener((slider, value, fromUser) -> tvRatingLabel.setText("מדירוג " + value + " ומעלה"));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        // שמירת הסינונים
        btnApplyFilters.setOnClickListener(v -> {
            currentCategoryFilter = spinnerCategory.getSelectedItem().toString();
            maxDistanceKmFilter = sliderDistance.getValue();
            minRatingFilter = sliderRating.getValue();

            // אם המשתמש רוצה לסנן לפי מרחק אבל אין לנו מיקום עדיין, נבקש שוב
            if (maxDistanceKmFilter < 50f && userLocation == null) {
                checkLocationPermissionAndFetch();
                Toast.makeText(getContext(), "מאתר מיקום...", Toast.LENGTH_SHORT).show();
            }

            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void applyFilters() {
        displayedList.clear();
        for (BusinessModel business : originalList) {

            // 1. סינון טקסט
            String bName = business.getName() != null ? business.getName() : "";
            boolean matchesSearch = bName.toLowerCase().contains(currentSearchText.toLowerCase());

            // 2. סינון קטגוריה
            String bType = business.getBusinessType() != null ? business.getBusinessType() : "";
            boolean matchesCategory = currentCategoryFilter.equals("הכל") || bType.equals(currentCategoryFilter);

            // 3. סינון מרחק
            boolean matchesDistance = true;
            // אם הסליידר על 50, נתייחס לזה כאל "הראה הכל בלי הגבלת מרחק"
            if (maxDistanceKmFilter < 50f) {
                if (userLocation == null || business.getLatitude() == null || business.getLongitude() == null) {
                    matchesDistance = false; // מסתירים אם אין נתוני מיקום
                } else {
                    float[] results = new float[1];
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                            business.getLatitude(), business.getLongitude(), results);

                    float distanceInKm = results[0] / 1000f;
                    if (distanceInKm > maxDistanceKmFilter) matchesDistance = false;
                }
            }

            // 4. סינון דירוג - קריאה לפונקציה הנכונה שיש במודל
            boolean matchesRating = true;
            float bRating = business.getOverallRating();

            if (bRating < minRatingFilter) {
                matchesRating = false;
            }

            if (matchesSearch && matchesCategory && matchesDistance && matchesRating) {
                displayedList.add(business);
            }
        }

        // מיון לפי מרחק (הקרוב ביותר קודם) אם יש מיקום משתמש
        if (userLocation != null) {
            Collections.sort(displayedList, (b1, b2) -> {
                if (b1.getLatitude() == null) return 1;
                if (b2.getLatitude() == null) return -1;
                float[] r1 = new float[1], r2 = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), b1.getLatitude(), b1.getLongitude(), r1);
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), b2.getLatitude(), b2.getLongitude(), r2);
                return Float.compare(r1[0], r2[0]);
            });
        }

        if (businessAdapter != null) {
            businessAdapter.notifyDataSetChanged();
        }

        tvEmptyState.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        rvBusinesses.setVisibility(displayedList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}