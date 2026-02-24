package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusinessServicesFragment extends Fragment {

    private EditText etTreatmentName, etTreatmentPrice, etTreatmentDuration;
    private Button btnAddTreatment;
    private RecyclerView rvTreatments;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentBusinessId = null;

    private TreatmentsAdapter adapter;
    private List<Treatment> treatmentList;

    public BusinessServicesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_services, container, false);

        // 1. חיבור רכיבי ה-UI מה-XML
        etTreatmentName = view.findViewById(R.id.etTreatmentName);
        etTreatmentPrice = view.findViewById(R.id.etTreatmentPrice);
        etTreatmentDuration = view.findViewById(R.id.etTreatmentDuration);
        btnAddTreatment = view.findViewById(R.id.btnAddTreatment);
        rvTreatments = view.findViewById(R.id.rvTreatments);

        // 2. אתחול Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 3. הגדרת הרשימה (RecyclerView)
        rvTreatments.setLayoutManager(new LinearLayoutManager(getContext()));
        treatmentList = new ArrayList<>();
        adapter = new TreatmentsAdapter(treatmentList);
        rvTreatments.setAdapter(adapter);

        // 4. טעינת מזהה העסק של המשתמש המחובר והצגת הטיפולים שלו
        loadBusinessData();

        // 5. לחיצה על כפתור "הוסף טיפול"
        btnAddTreatment.setOnClickListener(v -> addTreatment());

        return view;
    }

    private void loadBusinessData() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        // חיפוש העסק ששייך למשתמש הנוכחי (לפי התמונה ששלחת, יש שדה ownerId)
        db.collection("businesses")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // מצאנו את העסק! נשמור את ה-ID שלו
                        currentBusinessId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        // עכשיו נטען את הטיפולים שכבר קיימים
                        loadTreatments();
                    } else {
                        Toast.makeText(getContext(), "לא נמצא עסק מקושר למשתמש זה", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בטעינת נתוני עסק", Toast.LENGTH_SHORT).show());
    }

    private void loadTreatments() {
        if (currentBusinessId == null) return;

        db.collection("businesses").document(currentBusinessId).collection("treatments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    treatmentList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Treatment treatment = doc.toObject(Treatment.class);
                        treatmentList.add(treatment);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void addTreatment() {
        if (currentBusinessId == null) {
            Toast.makeText(getContext(), "אנא המתן לטעינת נתוני העסק", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etTreatmentName.getText().toString().trim();
        String priceStr = etTreatmentPrice.getText().toString().trim();
        String durationStr = etTreatmentDuration.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(durationStr)) {
            Toast.makeText(getContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int duration = Integer.parseInt(durationStr);

        // יצירת מזהה ייחודי לטיפול
        String treatmentId = db.collection("businesses").document(currentBusinessId).collection("treatments").document().getId();

        Treatment newTreatment = new Treatment(treatmentId, name, price, duration);

        // שמירה ב-Firestore
        db.collection("businesses").document(currentBusinessId).collection("treatments").document(treatmentId)
                .set(newTreatment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "הטיפול נוסף בהצלחה!", Toast.LENGTH_SHORT).show();
                    // ניקוי השדות
                    etTreatmentName.setText("");
                    etTreatmentPrice.setText("");
                    etTreatmentDuration.setText("");
                    // רענון הרשימה
                    loadTreatments();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בשמירת הטיפול", Toast.LENGTH_SHORT).show());
    }

    // --- אדפטר פנימי להצגת הטיפולים ברשימה ---
    class TreatmentsAdapter extends RecyclerView.Adapter<TreatmentsAdapter.ViewHolder> {
        private List<Treatment> treatments;

        public TreatmentsAdapter(List<Treatment> treatments) {
            this.treatments = treatments;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // ניצור עיצוב זמני דרך קוד כדי לחסוך לך קובץ XML נוסף כרגע (אפשר לשנות בהמשך)
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Treatment treatment = treatments.get(position);
            holder.text1.setText(treatment.getName() + " (" + treatment.getDurationMinutes() + " דקות)");
            holder.text2.setText("מחיר: ₪" + treatment.getPrice());
        }

        @Override
        public int getItemCount() {
            return treatments.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}