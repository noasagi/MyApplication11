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

// מחלקת פרגמנט המנהלת את מסך חיפוש בתי העסק, ומבצעת סינון רב-שכבתי דינמי ומיון גיאוגרפי לפי מיקום המשתמש
public class SearchFragment extends Fragment {

    private RecyclerView rvBusinesses;
    private BusinessAdapter businessAdapter;
    private TextView tvEmptyState;
    private SearchView searchView;
    private MaterialButton btnFilter;

    private FirebaseFirestore db;
    private ListenerRegistration businessListener; // אובייקט לרישום וניתוק המאזין של פיירבייס בזמן אמת

    // ניהול שתי רשימות נפרדות בזיכרון לשמירה על יעילות וביצועים (הרשימה המקורית מול הרשימה המסוננת המוצגת)
    private List<BusinessModel> originalList = new ArrayList<>();
    private List<BusinessModel> displayedList = new ArrayList<>();

    // משתני מצב השומרים את הקריטריונים הנוכחיים של הסינון בכל רגע נתון
    private String currentSearchText = "";
    private String currentCategoryFilter = "הכל";
    private float maxDistanceKmFilter = 50f;
    private float minRatingFilter = 0f;

    // רכיבי Google Play Services לצורך דגימת ה-GPS של המכשיר
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    /**
     * מה הפעולה עושה: מנפחת את ממשק ה-XML, מאתחלת את ה-RecyclerView והאדפטר, ומפעילה את תהליכי קליטת המידע והמיקום.
     * קלט: LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState.
     * פלט: View (תצוגת הפרגמנט המוכנה).
     */
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

        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterDialog();
            }
        });

        startListeningForBusinesses();
        checkLocationPermissionAndFetch();

        return view;
    }

    /**
     * מה הפעולה עושה: פותחת צינור האזנה קבוע (addSnapshotListener) מול Firestore. כל שינוי בנתוני העסקים בענן מעדכן מיד את הרשימה המקומית ללא צורך בריענון ידני.
     */
    private void startListeningForBusinesses() {
        businessListener = db.collection("businesses")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) return; // תנאי הגנה במקרה של שגיאת תקשורת
                        if (querySnapshot != null) {
                            originalList.clear(); // מניעת כפל נתונים בריענון
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                BusinessModel business = doc.toObject(BusinessModel.class);
                                if (business != null) {
                                    business.setBusinessId(doc.getId());
                                    originalList.add(business);
                                }
                            }
                            applyFilters(); // הפעלת הלוגיקה על המידע החדש שנחת מהענן
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: מחזור חיים של פרגמנט - מנתקת את המאזין הרישמי של פיירבייס בעת סגירת התצוגה.
     * למה זה קריטי: מונע זליגת זיכרון (Memory Leak) ובזבוז קריאות מיותרות ומשאבי סוללה ברקע של המכשיר.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (businessListener != null) businessListener.remove();
    }

    // בדיקה האם המשתמש אישר לאפליקציה גישה לרכיב המיקום של הטלפון ברמת ה-Runtime
    private void checkLocationPermissionAndFetch() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // בקשת הרשאה מפורשת מהמשתמש באמצעות תיבה קופצת של המערכת
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchUserLocationNow(); // ההרשאה כבר קיימת - ניגש ישר לדגום מיקום
        }
    }

    // דגימת נקודת המיקום הגאוגרפית המדויקת והעדכנית ביותר של הטלפון באמצעות הלוויין
    @SuppressLint("MissingPermission")
    private void fetchUserLocationNow() {
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            userLocation = location; // שמירת קואורדינטות המשתמש (Latitude & Longitude)
                            applyFilters(); // ריצה מחדש על אלגוריתם הסינונים והמיון הגיאוגרפי
                        }
                    }
                });
    }

    // הגדרת מאזין להקלדות בתוך רכיב החיפוש שמריץ את הסינון בזמן אמת (על כל תו שנכתב או נמחק)
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

    /**
     * מה הפעולה עושה: מייצרת ומציגה דיאלוג (AlertDialog) מותאם אישית המכיל סליידרים (Sliders) וספינר לבחירת קריטריוני סינון מתקדמים.
     */
    private void showFilterDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter, null);

        final Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerDialogCategory);
        final Slider sliderDistance = dialogView.findViewById(R.id.sliderDistance);
        final Slider sliderRating = dialogView.findViewById(R.id.sliderRating);
        final TextView tvDistanceLabel = dialogView.findViewById(R.id.tvDistanceLabel);
        final TextView tvRatingLabel = dialogView.findViewById(R.id.tvRatingLabel);
        Button btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);

        // טעינת מערך הקטגוריות מקובץ ה-strings.xml והלבשתו על ה-Spinner באמצעות ArrayAdapter
        List<String> categories = new ArrayList<>();
        categories.add("הכל");
        categories.addAll(Arrays.asList(getResources().getStringArray(R.array.business_types)));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);
        spinnerCategory.setSelection(categories.indexOf(currentCategoryFilter));

        sliderDistance.setValue(maxDistanceKmFilter);
        tvDistanceLabel.setText("עד " + (int)maxDistanceKmFilter + " ק\"מ");

        sliderRating.setValue(minRatingFilter);
        tvRatingLabel.setText("מדירוג " + minRatingFilter + " ומעלה");

        sliderDistance.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                tvDistanceLabel.setText("עד " + (int)value + " ק\"מ");
            }
        });

        sliderRating.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                tvRatingLabel.setText("מדירוג " + value + " ומעלה");
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        btnApplyFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCategoryFilter = spinnerCategory.getSelectedItem().toString();
                maxDistanceKmFilter = sliderDistance.getValue();
                minRatingFilter = sliderRating.getValue();

                if (maxDistanceKmFilter < 50f && userLocation == null) {
                    checkLocationPermissionAndFetch();
                    Toast.makeText(getContext(), "מאתר מיקום...", Toast.LENGTH_SHORT).show();
                }

                applyFilters();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * מה הפעולה עושה: אלגוריתם הליבה המשולב. חלק א' - מסנן את העסקים בזיכרון המקומי לפי 4 שכבות (טקסט, קטגוריה, מרחק ודירוג). חלק ב' - ממיין גיאוגרפית את התוצאות מהקרוב לרחוק באמצעות ה-GPS.
     */
    private void applyFilters() {
        displayedList.clear(); // ניקוי רשימת התצוגה לפני בנייה מחדש

        for (BusinessModel business : originalList) {

            // שכבה 1: התאמת טקסט חופשי (חיפוש ללא רגישות לאותיות גדולות/קטנות)
            String bName = business.getName() != null ? business.getName() : "";
            boolean matchesSearch = bName.toLowerCase().contains(currentSearchText.toLowerCase());

            // שכבה 2: סינון קטגוריה
            String bType = business.getBusinessType() != null ? business.getBusinessType() : "";
            boolean matchesCategory = currentCategoryFilter.equals("הכל") || bType.equals(currentCategoryFilter);

            // שכבה 3: חישוב מתמטי וסינון גיאוגרפי מבוסס רדיוס קילומטרים
            boolean matchesDistance = true;
            if (maxDistanceKmFilter < 50f) { // 50 ק"מ ומעלה נחשב בקוד כאי-הגבלה של מרחק
                if (userLocation == null || business.getLatitude() == null || business.getLongitude() == null) {
                    matchesDistance = false; // אם אין נתוני מיקום זמינים, לא נציג את העסק תחת סינון מרחק
                } else {
                    float[] results = new float[1];
                    // פונקציה רשמית של אנדרואיד המחשבת מרחק אווירי במטרים בין שתי נקודות על פני כדור הארץ
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                            business.getLatitude(), business.getLongitude(), results);

                    float distanceInKm = results[0] / 1000f; // המרת מטרים לקילומטרים
                    if (distanceInKm > maxDistanceKmFilter) matchesDistance = false;
                }
            }

            // שכבה 4: סינון לפי ציון כוכבים מינימלי
            boolean matchesRating = true;
            float bRating = business.getOverallRating();
            if (bRating < minRatingFilter) {
                matchesRating = false;
            }

            // אינטגרציה: רק עסק שעמד בהצלחה בכל 4 השכבות, ייכנס לרשימת התצוגה
            if (matchesSearch && matchesCategory && matchesDistance && matchesRating) {
                displayedList.add(business);
            }
        }

        // חלק ב': במידה וקיים מיקום GPS למכשיר, נמיין את הרשימה המסוננת באמצעות ממשק Comparator מהקרוב לרחוק
        if (userLocation != null) {
            Collections.sort(displayedList, new Comparator<BusinessModel>() {
                @Override
                public int compare(BusinessModel b1, BusinessModel b2) {
                    if (b1.getLatitude() == null) return 1;
                    if (b2.getLatitude() == null) return -1;
                    float[] r1 = new float[1], r2 = new float[1];

                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), b1.getLatitude(), b1.getLongitude(), r1);
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), b2.getLatitude(), b2.getLongitude(), r2);

                    return Float.compare(r1[0], r2[0]); // החזרת תוצאת ההשוואה הגיאוגרפית
                }
            });
        }

        // עדכון גרפי של ה-RecyclerView (מפעיל את onBindViewHolder של האדפטר מחדש)
        if (businessAdapter != null) {
            businessAdapter.notifyDataSetChanged();
        }

        // ניהול מצב מסך ריק (Empty State) במידה ואף עסק לא תאם לקריטריוני החיפוש
        tvEmptyState.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        rvBusinesses.setVisibility(displayedList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}