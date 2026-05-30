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

// הגדרת מחלקת פרגמנט עבור מסך הבית של הלקוח באפליקציה
public class CustomerHomeFragment extends Fragment {

    // הצהרה על רכיבי הטקסט, כרטיסי המידע ולחצני הפעולה המרכזיים
    private TextView tvWelcome, tvRateBusinessName;
    private CardView cardSearch, cardAppointments, cardRateUs;
    private Button btnRateNow;
    private ImageView imgHomeProfile;

    // רכיבי הגישה הרשמיים לעבודה מול שירותי האימות ומסד הנתונים של פיירבייס
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // רכיבי התצוגה והרשימה עבור בתי העסק האחרונים בהם ביקר הלקוח
    private LinearLayout layoutRecentBusinesses;
    private RecyclerView rvRecentBusinesses;
    private RecentBusinessAdapter recentAdapter;
    private List<Appointment> recentList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // ניפוח וטעינת קובץ עיצוב ה-XML הייעודי של מסך הבית של הלקוח
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        // קישור משתני הרכיבים לרכיבים הויזואליים מתוך קובץ ה-XML המנופח
        tvWelcome = view.findViewById(R.id.tvWelcome);
        cardSearch = view.findViewById(R.id.cardSearch);
        cardAppointments = view.findViewById(R.id.cardAppointments);
        imgHomeProfile = view.findViewById(R.id.imgHomeProfile);

        cardRateUs = view.findViewById(R.id.cardRateUs);
        tvRateBusinessName = view.findViewById(R.id.tvRateBusinessName);
        btnRateNow = view.findViewById(R.id.btnRateNow);

        layoutRecentBusinesses = view.findViewById(R.id.layoutRecentBusinesses);
        rvRecentBusinesses = view.findViewById(R.id.rvRecentBusinesses);

        // אתחול מופעי הגישה של פיירבייס
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // הגדרת מנהל פריסה אנכי לרכיב הרשימה תוך וידאו קיום ה-Context של הפרגמנט
        if (getContext() != null) {
            rvRecentBusinesses.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        // אתחול הרשימה המקומית והצמדת המתאם (Adapter) אל רכיב ה-RecyclerView
        recentList = new ArrayList<>();
        recentAdapter = new RecentBusinessAdapter(recentList);
        rvRecentBusinesses.setAdapter(recentAdapter);

        // קריאה לפעולות הטעינה הראשוניות של נתוני המשתמש והתורים
        loadUserName();
        checkPendingReviews();
        loadRecentBusinesses();

        // הגדרת מאזין לחיצה אנונימי קלאסי לכרטיס החיפוש למעבר למסך החיפוש
        cardSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(R.id.nav_customer_search);
            }
        });

        // הגדרת מאזין לחיצה אנונימי קלאסי לכרטיס התורים למעבר למסך ניהול התורים
        cardAppointments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(R.id.nav_customer_appointments);
            }
        });

        // הגדרת מאזין לחיצה אנונימי קלאסי לתמונת הפרופיל למעבר למסך עריכת הפרופיל
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
        // ריענון ובדיקת נתונים מחודשת בכל פעם שהמשתמש חוזר חזותית למסך הבית
        checkPendingReviews();
        loadRecentBusinesses();
    }

    // פעולה פרטית הבודקת האם קיים תור מאושר בעבר שהלקוח טרם דירג או כתב עליו ביקורת
    private void checkPendingReviews() {
        if (auth.getCurrentUser() == null) return;

        final long currentTime = System.currentTimeMillis();

        // שאילתה השולפת תורים מאושרים של המשתמש ששדה הדירוג שלהם מסומן כשלילי (False)
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

                            // בדיקה האם זמן התור המאושר קטן מהזמן הנוכחי (כלומר, הטיפול כבר הסתיים במציאות)
                            if (app.getTimestamp() < currentTime) {
                                showReviewCard(app); // הצגת כרטיס הצעת הדירוג על המסך
                                found = true;
                                break; // עצירת הלולאה - מציגים רק תור אחד בכל פעם לדירוג
                            }
                        }

                        // במידה ולא נמצא אף תור שממתין לדירוג, נעלים לחלוטין את כרטיס הדירוג מהמסך
                        if (!found) {
                            cardRateUs.setVisibility(View.GONE);
                        }
                    }
                });
    }

    // פעולה פרטית המציגה את כרטיס הדירוג ומצמידה לו מאזין לפתיחת תיבת הדו שיח
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

    // פעולה פרטית המנפחת ומציגה דיאלוג (AlertDialog) מותאם אישית להזנת חוות דעת ודירוג כוכבים
    private void showAddReviewDialog(final Appointment app) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.activity_dialog_add_review, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // קישור רכיבי תיבת הדו-שיח מתוך ה-XML המנופח של הדיאלוג
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

                // הגנה: וידוא שהמשתמש הציב דירוג (לפחות כוכב אחד) בכל אחת משלוש הקטגוריות
                if (ratingProf == 0 || ratingRel == 0 || ratingPrice == 0) {
                    Toast.makeText(getContext(), "אנא דרג את כל הקטגוריות", Toast.LENGTH_SHORT).show();
                    return;
                }

                // יצירת מזהה ייחודי עבור מסמך הביקורת החדש
                final String reviewId = db.collection("reviews").document().getId();

                ReviewModel newReview = new ReviewModel(
                        reviewId, app.getBusinessId(), app.getUserId(), app.getUserName(),
                        comment, app.getAppointmentId(), ratingProf, ratingRel, ratingPrice,
                        com.google.firebase.Timestamp.now()
                );

                // שמירת מסמך הביקורת בענן ועדכון סטטוס התור ל"דורג"
                db.collection("reviews").document(reviewId).set(newReview)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // עדכון שדה הכלת הדירוג בתור המקור על מנת שלא יקפוץ שוב לדירוג במסך הבית
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

    // פעולה פרטית השולפת את שם המשתמש ותמונת הפרופיל הבינארית שלו מתוך אוסף המשתמשים
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

                                // שליפת תמונת הפרופיל המאוחסנת כביטים (Blob) והמרתה לביטמפ חזותי
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

    // פעולה פרטית המממשת אלגוריתם סינון חכם לשליפת 3 בתי העסק האחרונים ללא כפילויות
    private void loadRecentBusinesses() {
        if (auth.getCurrentUser() == null) return;

        // פנייה לאוסף התורים, מיון לפי חותם זמן יורד והגבלת השליפה ל-15 מסמכים ראשונים בלבד
        db.collection("appointments")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(15)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        recentList.clear();
                        // רשימת עזר זמנית לשמירת מזהי העסקים שכבר נתקלנו בהם במהלך הלולאה
                        List<String> addedBusinessIds = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Appointment app = doc.toObject(Appointment.class);
                            String bId = app.getBusinessId();

                            // אלגוריתם מניעת כפילויות: בודק אם מזהה העסק הנוכחי לא נמצא ברשימת העזר
                            if (bId != null && !addedBusinessIds.contains(bId)) {
                                addedBusinessIds.add(bId); // הוספת המזהה לרשימת החסימה
                                recentList.add(app); // הוספת התור לרשימת התצוגה הסופית

                                // עצירת האלגוריתם ברגע שהגענו בדיוק ל-3 בתי עסק שונים וייחודיים
                                if (recentList.size() >= 3) {
                                    break;
                                }
                            }
                        }

                        recentAdapter.notifyDataSetChanged();

                        // ניהול נראות המכולה ב-XML בהתאם לקיום או אי-קיום של עסקים בהיסטוריה
                        if (recentList.isEmpty()) {
                            layoutRecentBusinesses.setVisibility(View.GONE);
                        } else {
                            layoutRecentBusinesses.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    // פעולה פרטית המקשרת ומנווטת בין הפרגמנטים השונים דרך רכיב ה-BottomNavigationView הראשי
    private void navigateTo(int navId) {
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(navId);
            }
        }
    }

    // --- אדפטר פנימי מבוסס מחלקה קלאסית לניהול רשימת בתי העסק האחרונים ---
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
                // שליפת נתוני העסק הספציפי על מנת להציג את שמו ותמונת הלוגו שלו מהענן
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

                                    // שליפת מערך התמונות ופענוח התמונה הראשונה בבלוק לטובת הצגת הלוגו בשורה
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

            // הגדרת מאזין לחיצה אנונימי קלאסי למעבר ישיר למסך קביעת תור חוזר לאותו העסק
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