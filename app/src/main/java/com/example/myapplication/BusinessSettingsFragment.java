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

public class BusinessSettingsFragment extends Fragment {

    public BusinessSettingsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_settings, container, false);

        CardView cardTreatments = view.findViewById(R.id.cardTreatments);
        CardView cardBusinessHours = view.findViewById(R.id.cardBusinessHours);
        CardView cardBlockHours = view.findViewById(R.id.cardBlockHours);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // ניהול טיפולים (כרגע עדיין טרם בנינו את הדף הזה)
        cardTreatments.setOnClickListener(v ->
                Toast.makeText(getContext(), "בקרוב...", Toast.LENGTH_SHORT).show()
        );

        // שעות פעילות קבועות -> מעבר לדף האמיתי
        cardBusinessHours.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BusinessHoursActivity.class);
            startActivity(intent);
        });

        // חסימת שעות / חופשה -> מעבר לדף האמיתי
        cardBlockHours.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BusinessBlockSlotsActivity.class);
            startActivity(intent);
        });

        // התנתקות
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}