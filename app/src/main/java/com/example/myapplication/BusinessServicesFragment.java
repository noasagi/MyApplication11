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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusinessServicesFragment extends Fragment {

    private EditText etTreatmentName, etTreatmentDuration, etTreatmentPrice;
    private Button btnAddTreatment;
    private RecyclerView rvTreatments;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId = null;

    private TreatmentsAdapter adapter;
    private List<Treatment> treatmentList = new ArrayList<>();
    // רשימה שתשמור את ה-ID של כל מסמך כדי שנוכל למחוק אותו
    private List<String> treatmentIds = new ArrayList<>();

    public BusinessServicesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_services, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etTreatmentName = view.findViewById(R.id.etTreatmentName);
        etTreatmentDuration = view.findViewById(R.id.etTreatmentDuration);
        etTreatmentPrice = view.findViewById(R.id.etTreatmentPrice);
        btnAddTreatment = view.findViewById(R.id.btnAddTreatment);
        rvTreatments = view.findViewById(R.id.rvTreatments);

        rvTreatments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TreatmentsAdapter();
        rvTreatments.setAdapter(adapter);

        findBusinessIdAndLoadTreatments();

        btnAddTreatment.setOnClickListener(v -> addTreatment());

        return view;
    }

    private void findBusinessIdAndLoadTreatments() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("businesses").whereEqualTo("ownerId", uid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        businessId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        loadTreatments();
                    } else {
                        Toast.makeText(getContext(), "לא נמצא עסק", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בטעינת העסק", Toast.LENGTH_SHORT).show());
    }

    private void loadTreatments() {
        if (businessId == null) return;

        db.collection("businesses").document(businessId).collection("treatments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    treatmentList.clear();
                    treatmentIds.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Treatment treatment = doc.toObject(Treatment.class);
                        treatmentList.add(treatment);
                        treatmentIds.add(doc.getId()); // שומרים את ה-ID למחיקה
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void addTreatment() {
        if (businessId == null) {
            Toast.makeText(getContext(), "אנא המתן לטעינת העסק", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etTreatmentName.getText().toString().trim();
        String durationStr = etTreatmentDuration.getText().toString().trim();
        String priceStr = etTreatmentPrice.getText().toString().trim();

        if (name.isEmpty() || durationStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(getContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration = Integer.parseInt(durationStr);
        double price = Double.parseDouble(priceStr);

        Map<String, Object> treatmentData = new HashMap<>();
        treatmentData.put("name", name);
        treatmentData.put("durationMinutes", duration);
        treatmentData.put("price", price);

        db.collection("businesses").document(businessId).collection("treatments")
                .add(treatmentData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "הטיפול נוסף בהצלחה!", Toast.LENGTH_SHORT).show();
                    etTreatmentName.setText("");
                    etTreatmentDuration.setText("");
                    etTreatmentPrice.setText("");
                    loadTreatments(); // רענון הרשימה
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה בהוספת הטיפול", Toast.LENGTH_SHORT).show());
    }

    // --- Adapter פנימי לרשימת הטיפולים ---
    class TreatmentsAdapter extends RecyclerView.Adapter<TreatmentsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // משתמשים בעיצוב מובנה של אנדרואיד לשתי שורות טקסט
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Treatment treatment = treatmentList.get(position);

            // שורה ראשונה: שם הטיפול
            holder.text1.setText(treatment.getName());
            // שורה שנייה: זמן ומחיר + הדרכה קטנה למחיקה
            holder.text2.setText("⏱ " + treatment.getDurationMinutes() + " דקות | ₪" + treatment.getPrice() + " (לחיצה ארוכה למחיקה)");

            // *** מנגנון המחיקה בלחיצה ארוכה ***
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("מחיקת טיפול")
                        .setMessage("האם את/ה בטוח/ה שברצונך למחוק את הטיפול '" + treatment.getName() + "'?")
                        .setPositiveButton("כן, מחק", (dialog, which) -> {
                            String docId = treatmentIds.get(position);
                            db.collection("businesses").document(businessId).collection("treatments").document(docId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "הטיפול נמחק", Toast.LENGTH_SHORT).show();
                                        loadTreatments(); // רענון הרשימה
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("ביטול", null)
                        .show();
                return true; // אומר לאנדרואיד שטפלנו בלחיצה הארוכה
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
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}