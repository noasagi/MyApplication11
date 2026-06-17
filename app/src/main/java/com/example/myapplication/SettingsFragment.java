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

// מחלקת פרגמנט המנהלת את מסך הגדרות המשתמש, ניווט לאזורים שונים באפליקציה וביצוע התנתקות מאובטחת
public class SettingsFragment extends Fragment {

    private ImageView imgProfileSmall;
    private TextView tvProfileName;
    private CardView btnEditProfile, btnFavorites, btnHistory, btnMyChats;
    private Button btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /**
     * מה הפעולה עושה: מנפחת (Inflate) את קובץ ה-XML של המסך, מקשרת את הרכיבים החזותיים, ומגדירה את מאזיני הלחיצה לניווט בין המסכים השונים.
     * קלט: LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState.
     * פלט: View (תצוגת הפרגמנט המוכנה).
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        imgProfileSmall = view.findViewById(R.id.imgProfileSmall);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnFavorites = view.findViewById(R.id.btnFavorites);
        btnHistory = view.findViewById(R.id.btnHistory);
        btnMyChats = view.findViewById(R.id.btnMyChats);
        btnLogout = view.findViewById(R.id.btnLogout);

        // --- ניהול מערך הניווט (Intent Navigation) באפליקציה ---

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SetProfileActivity.class);
                startActivity(intent);
            }
        });

        btnFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FavoritesActivity.class);
                startActivity(intent);
            }
        });

        btnMyChats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ClientChatsActivity.class);
                startActivity(intent);
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CustomerHistoryActivity.class);
                startActivity(intent);
            }
        });

        /**
         * לוגיקת התנתקות מאובטחת:
         * 1. ניתוק רשמי של ה-Session מול שרת ה-Authentication של פיירבייס.
         * 2. שימוש בFlags לניקוי ה-Backstack (מחסנית המסכים). הדבר מבטיח שאחרי ההתנתקות, לחיצה על כפתור החזור של הטלפון לא תאפשר כניסה חוזרת למסכים הרגישים.
         */
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                // ניקוי המחסנית והגדרת משימה חדשה (קריטי להגנת פרטיות!)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        return view;
    }

    /**
     * מה הפעולה עושה: שלב במחזור החיים (Lifecycle) של הפרגמנט. מופעלת בכל פעם שהמסך חוזר לקדמת הבמה.
     * למה זה קריטי כאן: כאשר המשתמש עובר למסך "עריכת פרופיל", משנה שם או תמונה, ואז חוזר אחורה - מסך ההגדרות לא נוצר מחדש (onCreateView לא רץ), אלא רק חוזר למצב רצה (onResume). קריאה ל-loadUserData כאן מבטיחה שהפרטים החדשים יתעדכנו וישתקפו חזותית מיד!
     */
    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    /**
     * מה הפעולה עושה: שליפה אסינכרונית חד-פעמית של מסמך המשתמש הנוכחי מ-Firestore לצורך הצגת השם המעודכן והמרת ה-Blob הבינארי לתמונת פרופיל מעוגלת/קטנה.
     */
    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("name");
                                if (name != null && !name.isEmpty()) {
                                    tvProfileName.setText(name);
                                } else {
                                    tvProfileName.setText("שלום אורח");
                                }

                                // חילוץ תמונת ה-Blob, המרתה למערך ביתים ופענוחה ל-Bitmap חזותי
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
                            // טיפול שקט בשגיאות תקשורת למניעת הפרעה לחוויית המשתמש
                        }
                    });
        }
    }
}