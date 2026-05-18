package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;


public class SettingsFragment extends Fragment {

    private ImageView imgProfileSmall;
    private TextView tvProfileName;
    private CardView btnEditProfile, btnFavorites, btnHistory, btnMyChats;
    private Button btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // חיבור רכיבים
        imgProfileSmall = view.findViewById(R.id.imgProfileSmall);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnFavorites = view.findViewById(R.id.btnFavorites);
        btnHistory = view.findViewById(R.id.btnHistory);
        btnMyChats = view.findViewById(R.id.btnMyChats);
        btnLogout = view.findViewById(R.id.btnLogout);

        // --- מאזינים ללחיצות ---

        // מעבר לעריכת פרופיל
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SetProfileActivity.class);
            startActivity(intent);
        });

        // מעבר למועדפים
        btnFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FavoritesActivity.class);
            startActivity(intent);
        });

        // מעבר להודעות שלי
        btnMyChats.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ClientChatsActivity.class);
            startActivity(intent);
        });

        // מעבר להיסטורית תורים
        btnHistory.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CustomerHistoryFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnLogout.setOnClickListener(v -> {


            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // טעינת שם
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                tvProfileName.setText(name);
                            } else {
                                tvProfileName.setText("שלום אורח");
                            }

                            // טעינת תמונה
                            Blob imageBlob = documentSnapshot.getBlob("profileImageBlob");
                            if (imageBlob != null) {
                                byte[] bytes = imageBlob.toBytes();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                imgProfileSmall.setImageBitmap(bitmap);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                    });
        }
    }
}