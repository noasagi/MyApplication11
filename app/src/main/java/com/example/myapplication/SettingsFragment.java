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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

// הגדרת מחלקת פרגמנט המנהלת את מסך הגדרות ואפשרויות המשתמשת באפליקציה
public class SettingsFragment extends Fragment {

    // הצהרה על רכיבי הממשק הויזואליים של התפריט והפרופיל
    private ImageView imgProfileSmall;
    private TextView tvProfileName;
    private CardView btnEditProfile, btnFavorites, btnHistory, btnMyChats;
    private Button btnLogout;

    // מופעי הגישה לרכיבי האימות ומסד הנתונים של פיירבייס
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // טעינת וניפוח קובץ ה-XML של מסך ההגדרות
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // אתחול מופעי הגישה של שירותי פיירבייס
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // קישור רכיבי הגרפיקה מה-XML למשתני המחלקה
        imgProfileSmall = view.findViewById(R.id.imgProfileSmall);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnFavorites = view.findViewById(R.id.btnFavorites);
        btnHistory = view.findViewById(R.id.btnHistory);
        btnMyChats = view.findViewById(R.id.btnMyChats);
        btnLogout = view.findViewById(R.id.btnLogout);

        // --- הגדרת מאזיני לחיצה אנונימיים קלאסיים לניווט ותפעול המסך ---

        // מאזין למעבר למסך עריכת הפרופיל האישי
        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SetProfileActivity.class);
                startActivity(intent);
            }
        });

        // מאזין למעבר למסך העסקים המועדפים
        btnFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FavoritesActivity.class);
                startActivity(intent);
            }
        });

        // מאזין למעבר למסך ריכוז ההודעות והצ'אטים של המשתמשת
        btnMyChats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ClientChatsActivity.class);
                startActivity(intent);
            }
        });

        // תיקון: מאזין למעבר למסך היסטוריית תורים באמצעות Intent (מאחר ומדובר באקטיביטי)
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CustomerHistoryActivity.class);
                startActivity(intent);
            }
        });

        // מאזין לביצוע התנתקות מהחשבון וניקוי מחסנית המסכים
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut(); // התנתקות רשמית משירות ה-Auth בענן
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                // הגדרת דגלים לניקוי כל האקטיביטיז הקודמים מהזיכרון למניעת חזרה לאחור
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // קריאה לפעולת סינכרון וטעינת נתוני המשתמשת בכל פעם שהמסך חוזר לקדמת הבמה
        loadUserData();
    }

    // שליפה ועדכון חזותי של פרטי שם ותמונת המשתמשת המחוברת מתוך מסמך הענן
    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                // שליפת והצגת שם המשתמשת המעודכן
                                String name = documentSnapshot.getString("name");
                                if (name != null && !name.isEmpty()) {
                                    tvProfileName.setText(name);
                                } else {
                                    tvProfileName.setText("שלום אורח");
                                }

                                // חילוץ תמונת ה-Blob הבינארית, המרתה למערך ביטים והצגתה כ-Bitmap
                                Blob imageBlob = documentSnapshot.getBlob("profileImageBlob");
                                if (imageBlob != null) {
                                    byte[] bytes = imageBlob.toBytes();
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    imgProfileSmall.setImageBitmap(bitmap);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // ניהול שגיאות שקט במידת הצורך
                        }
                    });
        }
    }
}