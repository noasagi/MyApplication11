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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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

    /**
     * מה הפעולה עושה: מנפחת את עיצוב ה-XML, מחברת את הרכיבים הגרפיים, ומאתחלת את ה-RecyclerView והאדפטר המקומי.
     * קלט: LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState.
     * פלט: View (תצוגת הפרגמנט המוכנה).
     */
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

        cardSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(R.id.nav_customer_search);
            }
        });

        cardAppointments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(R.id.nav_customer_appointments);
            }
        });

        imgHomeProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(R.id.nav_customer_profile);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPendingReviews();
        loadRecentBusinesses();
    }

    /**
     * מה הפעולה עושה: מחפשת תורים מאושרים (APPROVED) מהעבר שטרם דורגו (isReviewed == false) כדי להציג למשתמש כרטיס תזכורת לדירוג.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void checkPendingReviews() {
        if (auth.getCurrentUser() == null) return;

        final long currentTime = System.currentTimeMillis();

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .whereEqualTo("status", "APPROVED")
                .whereEqualTo("isReviewed", false)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        boolean found = false;
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Appointment app = doc.toObject(Appointment.class);
                            app.setAppointmentId(doc.getId());

                            // בדיקה כרונולוגית: האם התור התרחש בעבר (timestamp < currentTime)
                            if (app.getTimestamp() < currentTime) {
                                showReviewCard(app);
                                found = true;
                                break; // מציגים תור אחד בלבד בכל פעם כדי לא להעמיס על הממשק
                            }
                        }

                        if (!found) {
                            cardRateUs.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void showReviewCard(final Appointment app) {
        cardRateUs.setVisibility(View.VISIBLE);
        tvRateBusinessName.setText("איך היה אצל " + app.getBusinessName() + "?");

        btnRateNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddReviewDialog(app);
            }
        });
    }

    /**
     * מה הפעולה עושה: מנפחת ומציגה תיבת דו-שיח (AlertDialog) מותאמת אישית, קולטת דירוג תלת-קטגורי, ושומרת מסמך ביקורת חדש ב-Firestore.
     * קלט: final Appointment app.
     * פלט: אין (void).
     */
    private void showAddReviewDialog(final Appointment app) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.activity_dialog_add_review, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final RatingBar rbProfessionalism = view.findViewById(R.id.rbProfessionalism);
        final RatingBar rbReliability = view.findViewById(R.id.rbReliability);
        final RatingBar rbPrice = view.findViewById(R.id.rbPrice);
        final EditText etComment = view.findViewById(R.id.etComment);
        Button btnSubmit = view.findViewById(R.id.btnSubmitReview);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float ratingProf = rbProfessionalism.getRating();
                float ratingRel = rbReliability.getRating();
                float ratingPrice = rbPrice.getRating();
                String comment = etComment.getText().toString().trim();

                // הגנת קלט: וידוא שבוצע דירוג חיובי (לפחות כוכב אחד) בכל המדדים
                if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
                    Toast.makeText(getContext(), "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
                    return;
                }

                final String reviewId = db.collection("reviews").document().getId();

                ReviewModel newReview = new ReviewModel(
                        reviewId, app.getBusinessId(), app.getUserId(), app.getUserName(),
                        comment, app.getAppointmentId(), ratingProf, ratingRel, ratingPrice,
                        com.google.firebase.Timestamp.now()
                );

                // שמירת הביקורת ועדכון דגל הסימון בתור המקורי למניעת הצגה כפולה
                db.collection("reviews").document(reviewId).set(newReview)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                db.collection("appointments").document(app.getAppointmentId())
                                        .update("isReviewed", true);

                                Toast.makeText(getContext(), "תודה על הדירוג!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                cardRateUs.setVisibility(View.GONE);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getContext(), "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        dialog.show();
    }

    /**
     * מה הפעולה עושה: שולפת את פרטי הלקוח ומבצעת המרה בינארית של ה-Blob השמור ב-Firestore להצגת תמונת הפרופיל.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadUserName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
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
                        }
                    });
        }
    }

    /**
     * מה הפעולה עושה: מממשת אלגוריתם סינון ייחודי המאחזר את 15 התורים האחרונים, ומחלץ מתוכם בדיוק 3 בתי עסק שונים ללא כפילויות תצוגה.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadRecentBusinesses() {
        if (auth.getCurrentUser() == null) return;

        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                // התיקון כאן: שינוי ל-DESCENDING באותיות גדולות
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(15)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        recentList.clear();
                        List<String> addedBusinessIds = new ArrayList<>(); // רשימת מעקב זמנית למניעת כפילויות

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Appointment app = doc.toObject(Appointment.class);
                            String bId = app.getBusinessId();

                            // אלגוריתם מניעת כפילויות: הוספה לרשימה רק אם מזהה העסק מופיע בפעם הראשונה בריצה
                            if (bId != null && !addedBusinessIds.contains(bId)) {
                                addedBusinessIds.add(bId);
                                recentList.add(app);

                                // עצירה מוחלטת של הלולאה ברגע שהגענו למכסה של 3 עסקים שונים
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
                    }
                });
    }

    /**
     * מה הפעולה עושה: מתממשקת מול ה-BottomNavigationView של האקטיביטי המארח ומדמה לחיצה חזותית להחלפת מסכים תקינה.
     * קלט: int navId.
     * פלט: אין (void).
     */
    private void navigateTo(int navId) {
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(navId);
            }
        }
    }

    // --- אדפטר פנימי (Adapter) לניהול רשימת בתי העסק האחרונים ---
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
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            final Appointment app = recentAppointments.get(position);

            if (app.getBusinessId() != null) {
                db.collection("businesses").document(app.getBusinessId()).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot ds) {
                                if (ds.exists()) {
                                    String name = ds.getString("name");
                                    if (name != null) {
                                        holder.tvRecentBusinessName.setText(name);
                                        app.setBusinessName(name);
                                    }

                                    // פיענוח בינארי של הלוגו מתוך מערך הבלובים השמור בענן
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
                            }
                        });
            } else if (app.getBusinessName() != null) {
                holder.tvRecentBusinessName.setText(app.getBusinessName());
            } else {
                holder.tvRecentBusinessName.setText("עסק לא ידוע");
            }

            holder.btnBookAgain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), BookingActivity.class);
                    intent.putExtra("businessId", app.getBusinessId());
                    intent.putExtra("businessName", app.getBusinessName());
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return recentAppointments.size();
        }

        // --- מחזיק רכיבים (ViewHolder) לשורת עסק אחרון בודד ---
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