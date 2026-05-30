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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// הגדרת מחלקת פרגמנט לחיפוש, סינון ומיון גיאוגרפי של בתי עסק במערכת
public class SearchFragment extends Fragment {

    // הצהרה על רכיבי ממשק המשתמש הויזואליים
    private RecyclerView rvBusinesses;
    private BusinessAdapter businessAdapter;
    private TextView tvEmptyState;
    private SearchView searchView;
    private MaterialButton btnFilter;

    // מופע בסיס הנתונים ורכיב ההאזנה הרציפה לשינויים בענן של פיירסטור
    private FirebaseFirestore db;
    private ListenerRegistration businessListener;

    // רשימות עזר: רשימה מקורית של כל העסקים מול רשימה מסוננת המוצגת בפועל
    private List<BusinessModel> originalList = new ArrayList<>();
    private List<BusinessModel> displayedList = new ArrayList<>();

    // --- פרמטרים ומצבים לניהול הסינונים ---
    private String currentSearchText = "";
    private String currentCategoryFilter = "הכל";
    private float maxDistanceKmFilter = 50f; // רדיוס מקסימלי בשימוש גיאוגרפי
    private float minRatingFilter = 0f;      // ציון דירוג מינימלי לסינון

    // רכיבי הגישה לקבלת נתוני מיקום ה-GPS של המכשיר
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // טעינת וניפוח קובץ ה-XML של מסך החיפוש והסינון
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // קישור רכיבי הגרפיקה מתוך ה-XML אל משתני המחלקה
        rvBusinesses = view.findViewById(R.id.rvBusinesses);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        searchView = view.findViewById(R.id.searchView);
        btnFilter = view.findViewById(R.id.btnFilter);

        // אתחול רכיבי הגישה של פיירבייס ושירותי המיקום של גוגל
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // הגדרת רכיב הרשימה והצמדת האדפטר המרכזי של העסקים
        if (getContext() != null) {
            rvBusinesses.setLayoutManager(new LinearLayoutManager(getContext()));
            businessAdapter = new BusinessAdapter(getContext(), displayedList);
            rvBusinesses.setAdapter(businessAdapter);
        }

        // הגדרת רכיב תיבת החיפוש הטקסטואלי
        setupSearch();

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור פתיחת חלון מסנן הפרמטרים
        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterDialog();
            }
        });

        // הפעלת מאזין רציני לעדכוני מידע בזמן אמת מהענן
        startListeningForBusinesses();

        // בדיקת הרשאות ודגימת מיקום ה-GPS של המשתמש לצורך חישובי מרחקים
        checkLocationPermissionAndFetch();

        return view;
    }

    // פעולה לפתיחת ערוץ האזנה קבוע (Real-time Snapshot) מול אוסף העסקים בענן
    private void startListeningForBusinesses() {
        businessListener = db.collection("businesses")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) return;
                        if (querySnapshot != null) {
                            originalList.clear(); // ניקוי הרשימה המקורית למניעת כפילויות
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                BusinessModel business = doc.toObject(BusinessModel.class);
                                if (business != null) {
                                    business.setBusinessId(doc.getId());
                                    originalList.add(business); // הוספת העסק העדכני לארכיון המקומי
                                }
                            }
                            applyFilters(); // הרצת אלגוריתם הסינון והמיון על המידע החדש
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // הגנה מפני זליגת זיכרון: ניתוק והסרת מאזין הפיירסטור בעת סגירת התצוגה
        if (businessListener != null) businessListener.remove();
    }

    // בדיקת סטטוס הרשאת גישה למיקום המכשיר ובקשת הרשאה במידת הצורך
    private void checkLocationPermissionAndFetch() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchUserLocationNow(); // אם קיימת הרשאה, ניגש ישירות לדגימת המיקום
        }
    }

    // דגימת מיקום המכשיר הנוכחי והמדויק ברמת דיוק גבוהה (High Accuracy)
    @SuppressLint("MissingPermission")
    private void fetchUserLocationNow() {
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            userLocation = location; // שמירת אובייקט המיקום שנתקבל מהלוויין
                            applyFilters(); // עדכון ומיון הרשימה מחדש על בסיס הנקודה הגיאוגרפית
                        }
                    }
                });
    }

    // הגדרת מאזין להקלדות טקסט בתוך רכיב ה-SearchView
    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                currentSearchText = newText; // עדכון מחרוזת הטקסט לחיפוש
                applyFilters(); // הרצת הסינון מחדש בכל הקלדה של המשתמש
                return true;
            }
        });
    }

    // ניפוח והצגת חלון דיאלוג (AlertDialog) מותאם אישית להגדרת מסננים מתקדמים
    private void showFilterDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter, null);

        // קישור רכיבי הבחירה והסליידרים מתוך קובץ ה-XML של הדיאלוג
        final Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerDialogCategory);
        final Slider sliderDistance = dialogView.findViewById(R.id.sliderDistance);
        final Slider sliderRating = dialogView.findViewById(R.id.sliderRating);
        final TextView tvDistanceLabel = dialogView.findViewById(R.id.tvDistanceLabel);
        final TextView tvRatingLabel = dialogView.findViewById(R.id.tvRatingLabel);
        Button btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);

        // בניית וניהול רשימת הקטגוריות בתוך רכיב הספינר
        List<String> categories = new ArrayList<>();
        categories.add("הכל");
        categories.addAll(Arrays.asList(getResources().getStringArray(R.array.business_types)));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);
        spinnerCategory.setSelection(categories.indexOf(currentCategoryFilter));

        // קביעת ערכים נוכחיים בסליידרים
        sliderDistance.setValue(maxDistanceKmFilter);
        tvDistanceLabel.setText("עד " + (int)maxDistanceKmFilter + " ק\"מ");

        sliderRating.setValue(minRatingFilter);
        tvRatingLabel.setText("מדירוג " + minRatingFilter + " ומעלה");

        // הגדרת מאזין שינוי אנונימי קלאסי לסליידר המרחק לעדכון הטקסט בזמן גרירה
        sliderDistance.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                tvDistanceLabel.setText("עד " + (int)value + " ק\"מ");
            }
        });

        // הגדרת מאזין שינוי אנונימי קלאסי לסליידר הדירוג לעדכון הטקסט בזמן גרירה
        sliderRating.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                tvRatingLabel.setText("מדירוג " + value + " ומעלה");
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        // מאזין לחיצה אנונימי קלאסי לכפתור אישור והחלת המסננים על הרשימה
        btnApplyFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCategoryFilter = spinnerCategory.getSelectedItem().toString();
                maxDistanceKmFilter = sliderDistance.getValue();
                minRatingFilter = sliderRating.getValue();

                // במידה ונדרש סינון מרחק אך נתוני ה-GPS טרם נקלטו במכשיר
                if (maxDistanceKmFilter < 50f && userLocation == null) {
                    checkLocationPermissionAndFetch();
                    Toast.makeText(getContext(), "מאתר מיקום...", Toast.LENGTH_SHORT).show();
                }

                applyFilters(); // החלת מערכת הסינונים המעודכנת
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // אלגוריתם מרכזי: סינון משולב רב-שכבתי ומיון גיאוגרפי לפי מרחק משתמש
    private void applyFilters() {
        displayedList.clear(); // ניקוי רשימת התצוגה הנוכחית

        for (BusinessModel business : originalList) {

            // שכבה 1: סינון טקסט חופשי (התאמה של שם העסק למחרוזת שהוקלדה)
            String bName = business.getName() != null ? business.getName() : "";
            boolean matchesSearch = bName.toLowerCase().contains(currentSearchText.toLowerCase());

            // שכבה 2: סינון קטגוריית העסק (התאמה לסוג העסק שנבחר בספינר)
            String bType = business.getBusinessType() != null ? business.getBusinessType() : "";
            boolean matchesCategory = currentCategoryFilter.equals("הכל") || bType.equals(currentCategoryFilter);

            // שכבה 3: חישוב וסינון גיאוגרפי מבוסס קואורדינטות ורדיוס
            boolean matchesDistance = true;
            if (maxDistanceKmFilter < 50f) { // הערך 50 מסמל אי-הגבלה של מרחק
                if (userLocation == null || business.getLatitude() == null || business.getLongitude() == null) {
                    matchesDistance = false;
                } else {
                    float[] results = new float[1];
                    // שימוש בפונקציה מתמטית לחישוב המרחק הריאלי במטרים בין שתי נקודות גיאוגרפיות
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                            business.getLatitude(), business.getLongitude(), results);

                    float distanceInKm = results[0] / 1000f; // המרה של המרחק ממטרים לקילומטרים
                    if (distanceInKm > maxDistanceKmFilter) matchesDistance = false;
                }
            }

            // שכבה 4: סינון לפי רמת דירוג כוכבים מספרית
            boolean matchesRating = true;
            float bRating = business.getOverallRating();
            if (bRating < minRatingFilter) {
                matchesRating = false;
            }

            // הצבה ובדיקה משולבת של כלל תנאי הסינון
            if (matchesSearch && matchesCategory && matchesDistance && matchesRating) {
                displayedList.add(business); // הוספת העסק לרשימה המסוננת רק אם עבר את כל השלבים
            }
        }

        // חלק ב' של האלגוריתם: מיון הרשימה המסוננת מהקרוב ביותר אל הרחוק ביותר
        if (userLocation != null) {
            Collections.sort(displayedList, new Comparator<BusinessModel>() {
                @Override
                public int compare(BusinessModel b1, BusinessModel b2) {
                    if (b1.getLatitude() == null) return 1;
                    if (b2.getLatitude() == null) return -1;
                    float[] r1 = new float[1], r2 = new float[1];

                    // חישוב מרחק גיאוגרפי עבור העסק הראשון והשני בהשוואה למיקום ה-GPS של המשתמש
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), b1.getLatitude(), b1.getLongitude(), r1);
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), b2.getLatitude(), b2.getLongitude(), r2);

                    // השוואה מתמטית בין שני ערכי המרחקים שנתקבלו
                    return Float.compare(r1[0], r2[0]);
                }
            });
        }

        // עדכון גרפי של הרשימה על גבי מסך המכשיר
        if (businessAdapter != null) {
            businessAdapter.notifyDataSetChanged();
        }

        // ניהול מצבי נראות חזותיים בהתאם לקיומם של עסקים העונים על תנאי הסינון
        tvEmptyState.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        rvBusinesses.setVisibility(displayedList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}