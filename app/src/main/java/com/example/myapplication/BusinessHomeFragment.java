package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BusinessHomeFragment extends Fragment {

    private TextView tvWelcome, tvDate, tvTodayCount, tvPendingCount, tvNextClientName, tvNextClientInfo;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;

    public BusinessHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_home, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvDate = view.findViewById(R.id.tvDate);
        tvTodayCount = view.findViewById(R.id.tvTodayCount);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvNextClientName = view.findViewById(R.id.tvNextClientName);
        tvNextClientInfo = view.findViewById(R.id.tvNextClientInfo);

        // הגדרת תאריך עליון
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("he"));
        tvDate.setText(sdf.format(new Date()));

        fetchBusinessIdAndLoadData();

        return view;
    }

    private void fetchBusinessIdAndLoadData() {
        if (auth.getCurrentUser() == null) return;

        // שלב 1: מציאת ה-ID של העסק
        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        businessId = querySnapshot.getDocuments().get(0).getString("businessId");
                        String businessName = querySnapshot.getDocuments().get(0).getString("businessName");

                        tvWelcome.setText("שלום, " + (businessName != null ? businessName : "בעל עסק"));

                        loadDashboardStats();
                    }
                });
    }

    private void loadDashboardStats() {
        if (businessId == null) return;

        // השגת התאריך של היום בפורמט ששמור בדאטה בייס (d/M/yyyy)
        SimpleDateFormat sdfDate = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        String todayStr = sdfDate.format(new Date());

        // 1. ספירת תורים להיום (APPROVED)
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("date", todayStr)
                .whereEqualTo("status", "APPROVED")
                .get()
                .addOnSuccessListener(snapshots -> {
                    tvTodayCount.setText(String.valueOf(snapshots.size()));
                });

        // 2. ספירת בקשות ממתינות (בכל התאריכים) - PENDING
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(snapshots -> {
                    tvPendingCount.setText(String.valueOf(snapshots.size()));
                });

        // 3. מציאת התור הבא (הכי קרוב לשעה הנוכחית להיום)
        findNextAppointment(todayStr);
    }

    private void findNextAppointment(String todayStr) {
        // משיכת כל התורים המאושרים של היום
        db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("date", todayStr)
                .whereEqualTo("status", "APPROVED")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        tvNextClientName.setText("אין תורים נוספים היום");
                        tvNextClientInfo.setText("יום שקט!");
                        return;
                    }

                    // לוגיקה פשוטה למציאת התור הקרוב ביותר שטרם עבר
                    String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

                    Appointment nextApp = null;
                    String minTimeDiff = "23:59"; // אתחול ערך גבוה

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Appointment app = doc.toObject(Appointment.class);
                        if (app.getTime() != null && app.getTime().compareTo(currentTime) >= 0) {
                            // אם התור הוא בעתיד (יותר גדול מהשעה עכשיו)
                            // נבדוק אם הוא הקרוב ביותר שמצאנו עד כה
                            if (app.getTime().compareTo(minTimeDiff) < 0) {
                                minTimeDiff = app.getTime();
                                nextApp = app;
                            }
                        }
                    }

                    if (nextApp != null) {
                        tvNextClientName.setText(nextApp.getUserName());
                        String desc = nextApp.getDescription() != null ? nextApp.getDescription() : "טיפול";
                        tvNextClientInfo.setText("בשעה " + nextApp.getTime() + " | " + desc);
                    } else {
                        tvNextClientName.setText("סיימת להיום!");
                        tvNextClientInfo.setText("כל התורים עברו");
                    }
                });
    }
}