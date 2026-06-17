package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusinessServicesActivity extends AppCompatActivity {

    private EditText etTreatmentName, etTreatmentDuration, etTreatmentPrice;
    private Button btnAddTreatment;
    private RecyclerView rvTreatments;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId = null;

    private TreatmentsAdapter adapter;
    private List<Treatment> treatmentList = new ArrayList<>();

    // רשימה מקבילה השומרת את מזהי המסמכים (IDs) מה-Firestore כדי לאפשר מחיקה מדויקת של טיפול לפי מיקומו ברשימה
    private List<String> treatmentIds = new ArrayList<>();

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי הממשק, מגדירה את ה-RecyclerView ומפעילה את השלב הראשון של זיהוי העסק וטעינת הנתונים.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_services);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etTreatmentName = findViewById(R.id.etTreatmentName);
        etTreatmentDuration = findViewById(R.id.etTreatmentDuration);
        etTreatmentPrice = findViewById(R.id.etTreatmentPrice);
        btnAddTreatment = findViewById(R.id.btnAddTreatment);
        rvTreatments = findViewById(R.id.rvTreatments);

        rvTreatments.setLayoutManager(new LinearLayoutManager(BusinessServicesActivity.this));
        adapter = new TreatmentsAdapter();
        rvTreatments.setAdapter(adapter);

        // שלב א': מציאת ה-ID של העסק השייך למשתמש המחובר, ורק אז טעינת הטיפולים שלו
        findBusinessIdAndLoadTreatments();

        btnAddTreatment.setOnClickListener(v -> addTreatment());
    }

    /**
     * מה הפעולה עושה: מאתרת את מסמך העסק באוסף הראשי לפי ה-UID של בעל העסק ומחלצת את ה-ID שלו.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void findBusinessIdAndLoadTreatments() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("businesses").whereEqualTo("ownerId", uid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        businessId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        // שלב ב': טעינת הטיפולים מתוך תת-האוסף הפנימי של העסק שנמצא
                        loadTreatments();
                    } else {
                        Toast.makeText(BusinessServicesActivity.this, "לא נמצא עסק", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(BusinessServicesActivity.this, "שגיאה בטעינת העסק", Toast.LENGTH_SHORT).show());
    }

    /**
     * מה הפעולה עושה: שולפת את כל מסמכי הטיפולים מתוך תת-האוסף (Sub-collection) שנקרא "treatments" ומעדכנת את הרשימה הגרפית.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadTreatments() {
        if (businessId == null) return;

        // גישה למבנה מקונן ב-Firestore: businesses -> {businessId} -> treatments
        db.collection("businesses").document(businessId).collection("treatments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    treatmentList.clear();
                    treatmentIds.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Treatment treatment = doc.toObject(Treatment.class);
                        treatmentList.add(treatment);
                        treatmentIds.add(doc.getId()); // שמירת ה-ID הייחודי של מסמך הטיפול לצורכי מחיקה
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * מה הפעולה עושה: קוראת את שדות הקלט, מבצעת המרות סוגים (ל-int ו-double), ואורזת אותם למפה שנשמרת כתת-מסמך חדש ב-Firestore.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void addTreatment() {
        if (businessId == null) {
            Toast.makeText(BusinessServicesActivity.this, "אנא המתן לטעינת העסק", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etTreatmentName.getText().toString().trim();
        String durationStr = etTreatmentDuration.getText().toString().trim();
        String priceStr = etTreatmentPrice.getText().toString().trim();

        // הגנה: מניעת שליחת ערכים ריקים למסד הנתונים
        if (name.isEmpty() || durationStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(BusinessServicesActivity.this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration = Integer.parseInt(durationStr);
        double price = Double.parseDouble(priceStr);

        Map<String, Object> treatmentData = new HashMap<>();
        treatmentData.put("name", name);
        treatmentData.put("durationMinutes", duration);
        treatmentData.put("price", price);

        // שימוש בפעולת add() המייצרת אוטומטית מסמך חדש עם מזהה אקראי (ID) בתוך תת-האוסף
        db.collection("businesses").document(businessId).collection("treatments")
                .add(treatmentData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(BusinessServicesActivity.this, "הטיפול נוסף בהצלחה!", Toast.LENGTH_SHORT).show();
                    etTreatmentName.setText("");
                    etTreatmentDuration.setText("");
                    etTreatmentPrice.setText("");
                    loadTreatments(); // ריענון יזום של הרשימה כדי להציג את הטיפול החדש
                })
                .addOnFailureListener(e -> Toast.makeText(BusinessServicesActivity.this, "שגיאה בהוספת הטיפול", Toast.LENGTH_SHORT).show());
    }

    // --- מחלקת אדפטר פנימית: לניהול שורות הטיפולים ברשימה ---
    class TreatmentsAdapter extends RecyclerView.Adapter<TreatmentsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // שימוש בעיצוב מובנה של מערכת אנדרואיד (simple_list_item_2) הכולל שתי שורות טקסט מוכנות לעבודה
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Treatment treatment = treatmentList.get(position);

            holder.text1.setText(treatment.getName());
            holder.text2.setText("⏱ " + treatment.getDurationMinutes() + " דקות | ₪" + treatment.getPrice() + " (לחיצה ארוכה למחיקה)");

            // הגדרת מאזין ללחיצה ארוכה (OnLongClickListener) המציג דיאלוג אזהרה לפני מחיקה מהענן
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(BusinessServicesActivity.this)
                        .setTitle("מחיקת טיפול")
                        .setMessage("האם את/ה בטוח/ה שברצונך למחוק את הטיפול '" + treatment.getName() + "'?")
                        .setPositiveButton("כן, מחק", (dialog, which) -> {
                            // שליפת ה-ID המדויק של המסמך לפי מיקום הלחיצה ברשימה וביצוע פעולת מחיקה (delete)
                            String docId = treatmentIds.get(position);
                            db.collection("businesses").document(businessId).collection("treatments").document(docId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(BusinessServicesActivity.this, "הטיפול נמחק", Toast.LENGTH_SHORT).show();
                                        loadTreatments(); // טעינה מחדש של הרשימה העדכנית
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(BusinessServicesActivity.this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("ביטול", null)
                        .show();
                return true; // החזרת true מציינת שהאירוע טופל במלואו ולא יפעיל לחיצות רגילות בטעות
            });
        }

        @Override
        public int getItemCount() {
            return treatmentList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(View itemView) {
                super(itemView);
                // קישור רכיבי הטקסט המובנים של תבנית ה-simple_list_item_2 של אנדרואיד
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}