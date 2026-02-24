package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class BusinessSettingsFragment extends Fragment {

    public BusinessSettingsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_settings, container, false);

        // חיבור לכפתורים
        CardView cardEditProfile = view.findViewById(R.id.cardEditProfile);
        CardView cardTreatments = view.findViewById(R.id.cardTreatments);
        CardView cardBusinessHours = view.findViewById(R.id.cardBusinessHours);
        CardView cardBlockHours = view.findViewById(R.id.cardBlockHours);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // 1. כפתור לעריכת פרטי עסק
        cardEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MyBusinessMainActivity.class);
            startActivity(intent);
        });
        cardTreatments.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new BusinessServicesFragment()) // שימי לב להערה למטה!
                    .addToBackStack(null) // מאפשר למשתמש ללחוץ על כפתור ה"חזור" של הטלפון ולחזור להגדרות
                    .commit();
        });

        // 3. שעות פעילות קבועות (התיקון כאן - מושכים את ה-ID האמיתי של העסק)
        cardBusinessHours.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // פונים לפיירבייס כדי למצוא את המסמך של העסק לפי ה-UID של המנהל
                FirebaseFirestore.getInstance().collection("businesses")
                        .whereEqualTo("ownerId", uid) // ⚠️ שימי לב: ודאי שככה קוראים לשדה של מנהל העסק אצלך במסד הנתונים
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                // מצאנו את העסק! לוקחים את ה-ID האמיתי שלו
                                String realBusinessId = queryDocumentSnapshots.getDocuments().get(0).getId();

                                Intent intent = new Intent(getContext(), BusinessHoursActivity.class);
                                intent.putExtra("BUSINESS_ID", realBusinessId);
                                intent.putExtra("businessId", realBusinessId); // ליתר ביטחון, שולחים גם באותיות קטנות
                                startActivity(intent);
                            } else {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "שגיאה: לא נמצא עסק מקושר למשתמש זה", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "שגיאה בתקשורת מול השרת", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        // 4. חסימת שעות / חופשה
        cardBlockHours.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BusinessBlockSlotsActivity.class);
            startActivity(intent);
        });

        // 5. התנתקות
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}