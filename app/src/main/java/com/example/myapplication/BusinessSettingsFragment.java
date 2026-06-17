package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class BusinessSettingsFragment extends Fragment {

    public BusinessSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * מה הפעולה עושה: מנפחת את ממשק הגדרות העסק ומגדירה מאזיני לחיצה לרכיבי הניווט השונים (העברת פרמטרים או ניקוי מחסנית האקטיביטיז).
     * קלט: LayoutInflater, ViewGroup container, Bundle savedInstanceState.
     * פלט: View (תצוגת התפריט המוכנה).
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_settings, container, false);

        CardView cardEditProfile = view.findViewById(R.id.cardEditProfile);
        CardView cardTreatments = view.findViewById(R.id.cardTreatments);
        CardView cardBusinessHours = view.findViewById(R.id.cardBusinessHours);
        CardView cardBlockHours = view.findViewById(R.id.cardBlockHours);
        CardView cardStatistics = view.findViewById(R.id.cardStatistics);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // 1. ניווט פשוט: מעבר לעדכון פרטי הפרופיל והעסק
        cardEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), MyBusinessMainActivity.class);
                startActivity(intent);
            }
        });

        // 2. ניווט לניהול תפריט השירותים, הטיפולים והמחירים של העסק
        cardTreatments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), BusinessServicesActivity.class);
                startActivity(intent);
            }
        });

        // 3. ניווט מורכב: שליפת מזהה העסק (businessId) מ-Firestore לפני פתיחת מסך שעות הפעילות
        cardBusinessHours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    // הרצת שאילתה באוסף העסקים לאיתור העסק ששייך ל-ownerId הנוכחי
                    FirebaseFirestore.getInstance().collection("businesses")
                            .whereEqualTo("ownerId", uid)
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        String realBusinessId = queryDocumentSnapshots.getDocuments().get(0).getId();

                                        Intent intent = new Intent(getContext(), BusinessHoursActivity.class);
                                        // העברת ה-ID כפרמטר (Intent Extra) כדי שהמסך הבא ידע אילו שעות לטעון ולעדכן
                                        intent.putExtra("BUSINESS_ID", realBusinessId);
                                        intent.putExtra("businessId", realBusinessId);
                                        startActivity(intent);
                                    } else {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "שגיאה: לא נמצא עסק מקושר", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "שגיאה בתקשורת מול השרת", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        // 4. ניווט למסך חסימת חלונות זמן מיוחדים (חופשות, אילוצים או הפסקות)
        cardBlockHours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), BusinessBlockSlotsActivity.class);
                startActivity(intent);
            }
        });

        // 5. ניווט למסך הסטטיסטיקות, הגרפים והדוחות הפיננסיים
        cardStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), BusinessStatisticsActivity.class);
                startActivity(intent);
            }
        });

        // 6. תהליך התנתקות (Logout): ניתוק החשבון וניקוי מוחלט של היסטוריית המסכים
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);

                // שימוש בדגלים (Flags) קריטי כדי לנקות את מחסנית המסכים (Stack) ולמנוע מהמשתמש לחזור אחורה בלחיצת 'Back'
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        return view;
    }
}