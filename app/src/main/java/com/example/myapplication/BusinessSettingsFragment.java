package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;

public class BusinessSettingsFragment extends Fragment {

    public BusinessSettingsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_settings, container, false);

        // חיבור לכפתורים
        CardView cardEditProfile = view.findViewById(R.id.cardEditProfile); // הכפתור החדש
        CardView cardTreatments = view.findViewById(R.id.cardTreatments);
        CardView cardBusinessHours = view.findViewById(R.id.cardBusinessHours);
        CardView cardBlockHours = view.findViewById(R.id.cardBlockHours);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // 1. כפתור לעריכת פרטי עסק (הדף ששלחת עכשיו)
        cardEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MyBusinessMainActivity.class);
            startActivity(intent);
        });

        // 2. ניהול טיפולים (מוביל לרשימת הטיפולים שבנינו קודם)
        // אם שם הקובץ שלך אחר, תשני את BusinessServicesActivity לשם הנכון
        /*cardTreatments.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BusinessServicesActivity.class);
            startActivity(intent);
        }); */

        // 3. שעות פעילות קבועות
        cardBusinessHours.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BusinessHoursActivity.class);
            startActivity(intent);
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