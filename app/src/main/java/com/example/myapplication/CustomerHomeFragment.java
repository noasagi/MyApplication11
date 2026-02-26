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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class CustomerHomeFragment extends Fragment {

    private TextView tvWelcome, tvRateBusinessName;
    private CardView cardSearch, cardAppointments, cardRateUs;
    private Button btnRateNow;
    private ImageView imgHomeProfile;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        cardSearch = view.findViewById(R.id.cardSearch);
        cardAppointments = view.findViewById(R.id.cardAppointments);
        imgHomeProfile = view.findViewById(R.id.imgHomeProfile); // הוספנו את קישור התמונה

        // אלמנטים חדשים לדירוג
        cardRateUs = view.findViewById(R.id.cardRateUs);
        tvRateBusinessName = view.findViewById(R.id.tvRateBusinessName);
        btnRateNow = view.findViewById(R.id.btnRateNow);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserName();

        // בדיקה האם יש תור לדירוג
        checkPendingReviews();

        cardSearch.setOnClickListener(v -> navigateTo(R.id.nav_customer_search));
        cardAppointments.setOnClickListener(v -> navigateTo(R.id.nav_customer_appointments));

        // לחיצה על תמונת הפרופיל - ודא ש-ID הניווט תואם לשלך!
        imgHomeProfile.setOnClickListener(v -> navigateTo(R.id.nav_customer_profile));

        return view;
    }

    // בכל פעם שחוזרים למסך הזה, נבדוק שוב (אולי הוא כבר דירג)
    @Override
    public void onResume() {
        super.onResume();
        checkPendingReviews();
    }

    private void checkPendingReviews() {
        if (auth.getCurrentUser() == null) return;

        // שליפת תורים של המשתמש שהם בסטטוס APPROVED
        long currentTime = System.currentTimeMillis();

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .whereEqualTo("status", "APPROVED") // רק תורים שאושרו
                .whereEqualTo("isReviewed", false) // ועדיין לא דורגו
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean found = false;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Appointment app = doc.toObject(Appointment.class);

                        // בדיקה אם זמן התור עבר
                        if (app.getTimestamp() < currentTime) {
                            showReviewCard(app);
                            found = true;
                            break; // מציגים רק אחד בכל פעם
                        }
                    }

                    if (!found) {
                        cardRateUs.setVisibility(View.GONE);
                    }
                });
    }

    private void showReviewCard(Appointment app) {
        cardRateUs.setVisibility(View.VISIBLE);
        tvRateBusinessName.setText("איך היה אצל " + app.getBusinessName() + "?");

        btnRateNow.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DialogAddReviewActivity.class);
            // העברת נתונים לדיאלוג כדי שנדע על מה הביקורת
            intent.putExtra("appointmentId", app.getAppointmentId());
            intent.putExtra("businessId", app.getBusinessId());
            intent.putExtra("businessName", app.getBusinessName());
            startActivity(intent);
        });
    }

    private void loadUserName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                tvWelcome.setText("שלום, " + name);
                            }

                            // משיכת תמונת הפרופיל והצגתה
                            Blob imageBlob = documentSnapshot.getBlob("profileImageBlob");
                            if (imageBlob != null) {
                                byte[] bytes = imageBlob.toBytes();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                imgHomeProfile.setImageBitmap(bitmap);
                            }
                        }
                    });
        }
    }

    private void navigateTo(int navId) {
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(navId);
            }
        }
    }
}