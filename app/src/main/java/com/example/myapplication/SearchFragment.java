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
import com.google.firebase.firestore.ListenerRegistration;

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
    private SwitchMaterial switchNearMe;

    private FirebaseFirestore db;
    private ListenerRegistration businessListener; // מאזין לשינויים בזמן אמת

    private List<BusinessModel> originalList = new ArrayList<>();
    private List<BusinessModel> displayedList = new ArrayList<>();

    private String currentSearchText = "";
    private String currentCategory = "הכל";
    private boolean isNearMeOnly = false;
    private static final float MAX_DISTANCE_METERS = 15000f;

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
        switchNearMe = view.findViewById(R.id.switchNearMe);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (getContext() != null) {
            rvBusinesses.setLayoutManager(new LinearLayoutManager(getContext()));
            businessAdapter = new BusinessAdapter(getContext(), displayedList);
            rvBusinesses.setAdapter(businessAdapter);
        }

        setupFilters();
        startListeningForBusinesses(); // שינוי לטעינה חיה
        checkLocationPermissionAndFetch();

        return view;
    }

    // טעינת עסקים בזמן אמת - אם דירוג משתנה, זה יתעדכן מיד ברשימה
    private void startListeningForBusinesses() {
        businessListener = db.collection("businesses")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        return;
                    }
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
        if (businessListener != null) {
            businessListener.remove(); // הסרת המאזין כשהפרגמנט נסגר
        }
    }

    private void checkLocationPermissionAndFetch() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchUserLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchUserLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation = location;
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
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                currentSearchText = newText;
                applyFilters();
                return true;
            }
        });

        switchNearMe.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isNearMeOnly = isChecked;
            if (isNearMeOnly && userLocation == null) {
                checkLocationPermissionAndFetch();
            }
            applyFilters();
        });
    }

    private void applyFilters() {
        displayedList.clear();
        for (BusinessModel business : originalList) {
            String bName = business.getName() != null ? business.getName() : "";
            boolean matchesSearch = bName.toLowerCase().contains(currentSearchText.toLowerCase());
            String bType = business.getBusinessType() != null ? business.getBusinessType() : "";
            boolean matchesCategory = currentCategory.equals("הכל") || bType.equals(currentCategory);

            boolean matchesDistance = true;
            if (isNearMeOnly) {
                if (userLocation == null || business.getLatitude() == null || business.getLongitude() == null) {
                    matchesDistance = false;
                } else {
                    float[] results = new float[1];
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                            business.getLatitude(), business.getLongitude(), results);
                    if (results[0] > MAX_DISTANCE_METERS) matchesDistance = false;
                }
            }

            if (matchesSearch && matchesCategory && matchesDistance) {
                displayedList.add(business);
            }
        }

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