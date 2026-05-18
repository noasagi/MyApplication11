package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerHomeFragment extends Fragment {

    private TextView tvWelcome, tvRateBusinessName;
    private CardView cardSearch, cardAppointments, cardRateUs;
    private Button btnRateNow;
    private ImageView imgHomeProfile;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout layoutRecentBusinesses;
    private RecyclerView rvRecentBusinesses;
    private RecentBusinessAdapter recentAdapter;
    private List<Appointment> recentList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        cardSearch = view.findViewById(R.id.cardSearch);
        cardAppointments = view.findViewById(R.id.cardAppointments);
        imgHomeProfile = view.findViewById(R.id.imgHomeProfile);

        cardRateUs = view.findViewById(R.id.cardRateUs);
        tvRateBusinessName = view.findViewById(R.id.tvRateBusinessName);
        btnRateNow = view.findViewById(R.id.btnRateNow);

        layoutRecentBusinesses = view.findViewById(R.id.layoutRecentBusinesses);
        rvRecentBusinesses = view.findViewById(R.id.rvRecentBusinesses);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        if (getContext() != null) {
            rvRecentBusinesses.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        recentList = new ArrayList<>();
        recentAdapter = new RecentBusinessAdapter(recentList);
        rvRecentBusinesses.setAdapter(recentAdapter);

        loadUserName();
        checkPendingReviews();
        loadRecentBusinesses();

        cardSearch.setOnClickListener(v -> navigateTo(R.id.nav_customer_search));
        cardAppointments.setOnClickListener(v -> navigateTo(R.id.nav_customer_appointments));
        imgHomeProfile.setOnClickListener(v -> navigateTo(R.id.nav_customer_profile));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPendingReviews();
        loadRecentBusinesses();
    }

    private void checkPendingReviews() {
        if (auth.getCurrentUser() == null) return;

        long currentTime = System.currentTimeMillis();

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .whereEqualTo("status", "APPROVED")
                .whereEqualTo("isReviewed", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean found = false;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Appointment app = doc.toObject(Appointment.class);
                        app.setAppointmentId(doc.getId());

                        if (app.getTimestamp() < currentTime) {
                            showReviewCard(app);
                            found = true;
                            break;
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
            showAddReviewDialog(app);
        });
    }

    private void showAddReviewDialog(Appointment app) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.activity_dialog_add_review, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RatingBar rbProfessionalism = view.findViewById(R.id.rbProfessionalism);
        RatingBar rbReliability = view.findViewById(R.id.rbReliability);
        RatingBar rbPrice = view.findViewById(R.id.rbPrice);
        EditText etComment = view.findViewById(R.id.etComment);
        Button btnSubmit = view.findViewById(R.id.btnSubmitReview);

        btnSubmit.setOnClickListener(v -> {
            float ratingProf = rbProfessionalism.getRating();
            float ratingRel = rbReliability.getRating();
            float ratingPrice = rbPrice.getRating();
            String comment = etComment.getText().toString().trim();

            if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
                Toast.makeText(getContext(), "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
                return;
            }

            String reviewId = db.collection("reviews").document().getId();

            ReviewModel newReview = new ReviewModel(
                    reviewId,
                    app.getBusinessId(),
                    app.getUserId(),
                    app.getUserName(),
                    comment,
                    app.getAppointmentId(),
                    ratingProf,
                    ratingRel,
                    ratingPrice,
                    com.google.firebase.Timestamp.now()
            );

            db.collection("reviews").document(reviewId).set(newReview)
                    .addOnSuccessListener(aVoid -> {
                        db.collection("appointments").document(app.getAppointmentId())
                                .update("isReviewed", true);

                        Toast.makeText(getContext(), "תודה על הדירוג!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cardRateUs.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
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

    private void loadRecentBusinesses() {
        if (auth.getCurrentUser() == null) return;

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(15)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recentList.clear();
                    List<String> addedBusinessIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Appointment app = doc.toObject(Appointment.class);
                        String bId = app.getBusinessId();

                        if (bId != null && !addedBusinessIds.contains(bId)) {
                            addedBusinessIds.add(bId);
                            recentList.add(app);

                            if (recentList.size() >= 3) {
                                break;
                            }
                        }
                    }
                    recentAdapter.notifyDataSetChanged();

                    if (recentList.isEmpty()) {
                        layoutRecentBusinesses.setVisibility(View.GONE);
                    } else {
                        layoutRecentBusinesses.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void navigateTo(int navId) {
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(navId);
            }
        }
    }

    class RecentBusinessAdapter extends RecyclerView.Adapter<RecentBusinessAdapter.ViewHolder> {
        private List<Appointment> recentAppointments;

        public RecentBusinessAdapter(List<Appointment> recentAppointments) {
            this.recentAppointments = recentAppointments;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_business, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment app = recentAppointments.get(position);

            if (app.getBusinessId() != null) {
                db.collection("businesses").document(app.getBusinessId()).get()
                        .addOnSuccessListener(ds -> {
                            if (ds.exists()) {
                                String name = ds.getString("name");
                                if (name != null) {
                                    holder.tvRecentBusinessName.setText(name);
                                    app.setBusinessName(name);
                                }

                                List<Blob> imageBlobs = (List<Blob>) ds.get("imageBlobs");
                                if (imageBlobs != null && !imageBlobs.isEmpty()) {
                                    Blob firstImage = imageBlobs.get(0);
                                    if (firstImage != null) {
                                        byte[] bytes = firstImage.toBytes();
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        holder.imgBusinessLogo.setImageBitmap(bitmap);
                                    }
                                }
                            }
                        });
            } else if (app.getBusinessName() != null) {
                holder.tvRecentBusinessName.setText(app.getBusinessName());
            } else {
                holder.tvRecentBusinessName.setText("עסק לא ידוע");
            }

            holder.btnBookAgain.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), BookingActivity.class);
                intent.putExtra("businessId", app.getBusinessId());
                intent.putExtra("businessName", app.getBusinessName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return recentAppointments.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRecentBusinessName;
            ImageView imgBusinessLogo;
            Button btnBookAgain;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRecentBusinessName = itemView.findViewById(R.id.tvRecentBusinessName);
                imgBusinessLogo = itemView.findViewById(R.id.imgBusinessLogo);
                btnBookAgain = itemView.findViewById(R.id.btnBookAgain);
            }
        }
    }
}