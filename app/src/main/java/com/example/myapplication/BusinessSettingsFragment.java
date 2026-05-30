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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

// הגדרת מחלקה לניהול מסך הגדרות העסק, היורשת ממאפייני Fragment
public class BusinessSettingsFragment extends Fragment {

    // בנאי ברירת מחדל ריק הנדרש על פי כללי המערכת ביצירת פרגמנט
    public BusinessSettingsFragment() { }

    // פעולת המערכת המרכזית לבנייה וניפוח של קובץ העיצוב הויזואלי בפרגמנט
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // טעינת וניפוח קובץ ה-XML של עיצוב מסך הגדרות בעל העסק
        View view = inflater.inflate(R.layout.fragment_business_settings, container, false);

        // קישור משתני כרטיסיות הניווט לרכיבים הגרפיים האמיתיים מתוך קובץ ה-XML
        CardView cardEditProfile = view.findViewById(R.id.cardEditProfile);
        CardView cardTreatments = view.findViewById(R.id.cardTreatments);
        CardView cardBusinessHours = view.findViewById(R.id.cardBusinessHours);
        CardView cardBlockHours = view.findViewById(R.id.cardBlockHours);
        CardView cardStatistics = view.findViewById(R.id.cardStatistics);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // 1. הגדרת מאזין לחיצה אנונימי רגיל עבור כפתור עריכת פרופיל העסק
        cardEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת כוונת מעבר לאקטיביטי עריכת פרטי העסק
                Intent intent = new Intent(getContext(), MyBusinessMainActivity.class);
                // הפעלת הכוונה ומעבר למסך המתאים
                startActivity(intent);
            }
        });

        // 2. הגדרת מאזין לחיצה אנונימי רגיל עבור כפתור ניהול סוגי טיפולים ומחירים
        cardTreatments.setOnClickListener(new View.OnClickListener() { // תוקן מ-cardEditProfile ל-cardTreatments
            @Override
            public void onClick(View v) {
                // יצירת כוונת מעבר לאקטיביטי ניהול השירותים והטיפולים של העסק
                Intent intent = new Intent(getContext(), BusinessServicesActivity.class);
                // הפעלת הכוונה ומעבר למסך המתאים
                startActivity(intent);
            }
        });

        // 3. הגדרת מאזין לחיצה אנונימי רגיל עבור כפתור הגדרת שעות פעילות קבועות
        cardBusinessHours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // בדיקת הגנה לוודא שישנו משתמש מחובר כעת במערכת האימות
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    // שליפת מזהה ה-UID הייחודי של בעל העסק הנוכחי
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    // פנייה לאוסף העסקים לשליפת המסמך שבו שדה מזהה הבעלים שווה ל-UID המחובר
                    FirebaseFirestore.getInstance().collection("businesses")
                            .whereEqualTo("ownerId", uid)
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                // פעולה המופעלת ברגע ששליפת נתוני העסק הסתיימה בהצלחה מהשרת
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    // בדיקה האם נמצא מסמך עסק התואם לתנאי השאילתה במסד
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        // חילוץ מזהה המסמך האמיתי והייחודי של העסק מתוך פיירסטור
                                        String realBusinessId = queryDocumentSnapshots.getDocuments().get(0).getId();

                                        // יצירת כוונת מעבר למסך הגדרת שעות הפעילות
                                        Intent intent = new Intent(getContext(), BusinessHoursActivity.class);
                                        // העברת מזהה העסק כפרמטר (Extra) כדי שהמסך הבא ידע באיזה עסק מדובר
                                        intent.putExtra("BUSINESS_ID", realBusinessId);
                                        intent.putExtra("businessId", realBusinessId);
                                        // הפעלת המעבר למסך שעות הפעילות
                                        startActivity(intent);
                                    } else {
                                        // הקפצת הודעה במידה ולא נמצא מסמך עסק שמקושר לחשבון זה
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "שגיאה: לא נמצא עסק מקושר למשתמש זה", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                // פעולה המופעלת במקרה של כשל או תקלת תקשורת מול השרת בענן
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // הצגת הודעת שגיאה כללית למשתמשת על כשל בתקשורת
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "שגיאה בתקשורת מול השרת", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        // 4. הגדרת מאזין לחיצה אנונימי רגיל עבור כפתור חסימת שעות קבלת קהל / חופשות
        cardBlockHours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת כוונת מעבר לאקטיביטי לניהול וחסימת חלונות זמן
                Intent intent = new Intent(getContext(), BusinessBlockSlotsActivity.class);
                // הפעלת הכוונה ומעבר למסך המתאים
                startActivity(intent);
            }
        });

        // 5. הגדרת מאזין לחיצה אנונימי רגיל עבור כפתור צפייה בסטטיסטיקות ודוחות העסק
        cardStatistics.setOnClickListener(new View.OnClickListener() { // תוקן מ-cardBlockHours ל-cardStatistics
            @Override
            public void onClick(View v) {
                // יצירת כוונת מעבר לאקטיביטי המציג גרפים וסטטיסטיקות פיננסיות של העסק
                Intent intent = new Intent(getContext(), BusinessStatisticsActivity.class);
                // הפעלת הכוונה ומעבר למסך המתאים
                startActivity(intent);
            }
        });

        // 6. הגדרת מאזין לחיצה אנונימי רגיל עבור כפתור התנתקות מהחשבון
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ביצוע התנתקות רשמית של המשתמש ממערכת האימות של פיירבייס
                FirebaseAuth.getInstance().signOut();
                // יצירת כוונת מעבר חזרה למסך ההתחברות (LoginActivity)
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                // הגדרת דגלים (Flags) המנקים את מחסנית המסכים כדי שלא יהיה ניתן לחזור אחורה בלחיצת כפתור 'חזור'
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                // הפעלת המעבר למסך ההתחברות
                startActivity(intent);
            }
        });

        // החזרת מבט התצוגה המלא והמוכן של הפרגמנט
        return view;
    }
}