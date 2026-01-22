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
    private CardView btnEditProfile, btnFavorites;
    private Button btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // חיבור רכיבים
        imgProfileSmall = view.findViewById(R.id.imgProfileSmall);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnFavorites = view.findViewById(R.id.btnFavorites);
        btnLogout = view.findViewById(R.id.btnLogout);

        // --- מאזינים ללחיצות ---

        // מעבר לעריכת פרופיל
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SetProfileActivity.class);
            startActivity(intent);
        });

        // מעבר למועדפים
        // שים לב: כאן הנחתי שיצרת Activity בשם FavoritesActivity
        // אם זה פרגמנט אחר, תצטרך להשתמש ב-FragmentManager
        btnFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FavoritesActivity.class); // וודא שיש לך Activity כזה
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            // 1. ניתוק מ-Firebase
            mAuth.signOut();

            // 2. מעבר למסך הכניסה (תוודא שקוראים לו אצלך LoginActivity)
            Intent intent = new Intent(getActivity(), LoginActivity.class);

            // 3. מחיקת ההיסטוריה - כדי שהמשתמש לא יוכל ללחוץ "Back" ולחזור לאפליקציה בלי להירשם
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
        });

        return view;
    }

    // הפונקציה הזו רצה בכל פעם שהמסך מוצג למשתמש (כולל בחזרה ממסך אחר)
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

                            // טעינת תמונה (אותה לוגיקה כמו ב-SetProfileActivity)
                            Blob imageBlob = documentSnapshot.getBlob("profileImageBlob");
                            if (imageBlob != null) {
                                byte[] bytes = imageBlob.toBytes();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                imgProfileSmall.setImageBitmap(bitmap);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // אפשר להציג הודעה אם רוצים, אבל בדרך כלל מתעלמים מכשל טעינה שקטה ב-Settings
                    });
        }
    }
}