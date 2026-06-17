package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BusinessScheduleFragment extends Fragment {

    private RecyclerView rvAppointments;
    private AppointmentsAdapter adapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String businessId;

    // אובייקט לניהול הרישום של המאזין הדינמי – משמש לניתוק יזום של החיבור בעת סגירת הפרגמנט
    private ListenerRegistration appointmentsListener;

    public BusinessScheduleFragment() {
        // Required empty public constructor
    }

    /**
     * מה הפעולה עושה: מנפחת את ממשק המשתמש, מאתחלת את ה-RecyclerView והאדפטר ומפעילה את תהליך שליפת מזהה העסק.
     * קלט: LayoutInflater, ViewGroup container, Bundle savedInstanceState.
     * פלט: View (תצוגת המסך המוכנה).
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_schedule, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvAppointments = view.findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        appointmentList = new ArrayList<>();
        adapter = new AppointmentsAdapter(appointmentList);
        rvAppointments.setAdapter(adapter);

        fetchBusinessIdAndLoad();

        return view;
    }

    /**
     * מה הפעולה עושה: מאתרת את מזהה העסק השייך לבעלים הנוכחי לפי ה-UID של המשתמש המחובר, ומזמנת את טעינת התורים.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void fetchBusinessIdAndLoad() {
        if (auth.getCurrentUser() == null) return;

        db.collection("businesses")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            businessId = queryDocumentSnapshots.getDocuments().get(0).getString("businessId");
                            loadAppointments();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (isAdded() && getContext() != null)
                            Toast.makeText(getContext(), "שגיאה בטעינת נתוני עסק", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * מה הפעולה עושה: פותחת מאזין קבוע בזמן אמת לאוסף התורים של העסק, מסננת וממירה את מסמכי ה-Firestore לאובייקטים ומעדכנת את התצוגה.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadAppointments() {
        if (businessId == null) return;

        // הגנה מפני דליפות זיכרון וכפילויות: ניתוק מאזין קודם אם היה פעיל בפרגמנט
        if (appointmentsListener != null) {
            appointmentsListener.remove();
        }

        // חיבור מאזין SnapshotListener שמסתנכרן אוטומטית מול הענן וממיין את התורים לפי חותם זמן (Timestamp) יורד
        appointmentsListener = db.collection("appointments")
                .whereEqualTo("businessId", businessId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null || !isAdded()) return;

                        appointmentList.clear();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                Appointment app = doc.toObject(Appointment.class);
                                app.setAppointmentId(doc.getId());

                                // סינון מקומי: הוספה לרשימה רק אם התור עומד בחוקי התצוגה (סטטוס ותאריך)
                                if (shouldShowAppointment(app)) {
                                    appointmentList.add(app);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * מה הפעולה עושה: קובעת את חוקי הסינון הכלליים של רשימת התורים (מסתירה תורים שנדחו/נחסמו או תורים ששייכים לימים עברו).
     * קלט: Appointment app.
     * פלט: boolean (אמת אם יש להציג את התור, שקר אם יש להסתירו).
     */
    private boolean shouldShowAppointment(Appointment app) {
        String status = app.getStatus();
        if (status == null) status = "PENDING";

        if (status.equals("REJECTED") || status.equals("BLOCKED")) return false;

        return !isDateInPast(app.getDate());
    }

    /**
     * מה הפעולה עושה: משווה קלנדרית את תאריך התור מול תאריך היום הנוכחי (ללא התחשבות בשעות ודקות) כדי לקבוע האם הוא בעבר.
     * קלט: String dateStr.
     * פלט: boolean (אמת אם התאריך שייך לאתמול ומטה, שקר אחרת).
     */
    private boolean isDateInPast(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        try {
            Date appointmentDate = sdf.parse(dateStr);
            if (appointmentDate == null) return false;

            Calendar calApp = Calendar.getInstance();
            calApp.setTime(appointmentDate);

            // איפוס מוחלט של שדות הזמן בלוח השנה של היום כדי לבצע השוואת ימים נטו (ברמת התאריך בלבד)
            Calendar calToday = Calendar.getInstance();
            calToday.set(Calendar.HOUR_OF_DAY, 0);
            calToday.set(Calendar.MINUTE, 0);
            calToday.set(Calendar.SECOND, 0);
            calToday.set(Calendar.MILLISECOND, 0);

            return calApp.before(calToday);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * מחזור חיים: ניתוק מוחלט של המאזין מה-Firestore ברגע שה-View של הפרגמנט נהרס, למניעת בזבוז סוללה ומשאבים ברקע.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (appointmentsListener != null) {
            appointmentsListener.remove();
        }
    }

    // --- מחלקת אדפטר פנימית: לניהול והצגת פריטי התורים ברשימה ---
    class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.ViewHolder> {
        private List<Appointment> list;

        public AppointmentsAdapter(List<Appointment> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment app = list.get(position);

            holder.tvClientName.setText(app.getUserName());
            holder.tvDateTime.setText(app.getDate() + " | " + app.getTime());

            String desc = (app.getDescription() == null || app.getDescription().isEmpty()) ? "אין הערות" : app.getDescription();
            holder.tvDescription.setText(desc);

            String status = app.getStatus() != null ? app.getStatus() : "PENDING";

            // עיצוב דינמי של השורה (צבעים ונראות כפתורים) על פי הסטטוס הנוכחי של בקשת התור
            if (status.equals("PENDING")) {
                holder.tvStatus.setText("ממתין לאישור");
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // כתום
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnReject.setText("דחה");
            } else if (status.equals("APPROVED")) {
                holder.tvStatus.setText("מאושר");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // ירוק
                holder.btnApprove.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnReject.setText("בטל תור");
            }

            holder.btnApprove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateStatus(app, "APPROVED");
                }
            });

            holder.btnReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateStatus(app, "REJECTED");
                }
            });
        }

        /**
         * מה הפעולה עושה: מעדכנת את שדה הסטטוס במסמך התור ב-Firestore, שולפת את טלפון הלקוח ומכינה מערכת כוונות (Intent) חצי-אוטומטית לשליחת SMS.
         * קלט: Appointment app, String newStatus.
         * פלט: אין (void).
         */
        private void updateStatus(Appointment app, String newStatus) {
            if (app.getAppointmentId() == null) return;

            db.collection("appointments").document(app.getAppointmentId()).update("status", newStatus)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(getContext(), "סטטוס עודכן בבסיס הנתונים", Toast.LENGTH_SHORT).show();

                                if (app.getUserId() != null) {
                                    // שלב ב': מעבר לאוסף המשתמשים הכללי כדי לחלץ את מספר הטלפון העדכני של הלקוח
                                    db.collection("users").document(app.getUserId()).get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot userDoc) {
                                                    String clientPhone = "";
                                                    if (userDoc.exists()) {
                                                        clientPhone = userDoc.getString("phone");
                                                    }

                                                    // הרכבת נוסח ההודעה המילולית בהתאם להחלטת בעל העסק (אישור מול דחייה/ביטול)
                                                    String msg = newStatus.equals("APPROVED") ?
                                                            "איזה יופי! התור שלך עבור " + app.getDescription() + " בתאריך " + app.getDate() + " בשעה " + app.getTime() + " אושר בהצלחה!" :
                                                            "שלום, לצערנו התור שלך עבור " + app.getDescription() + " בתאריך " + app.getDate() + " בשעה " + app.getTime() + " לא אושר או בוטל.";

                                                    // שימוש בכוונת מערכת מפורשת (Intent.ACTION_SENDTO) המעבירה את מספר הטלפון ותוכן ההודעה ישירות לאפליקציית ה-SMS של המכשיר
                                                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                                                    smsIntent.setData(Uri.parse("smsto:" + (clientPhone != null ? clientPhone : "")));
                                                    smsIntent.putExtra("sms_body", msg);

                                                    try {
                                                        startActivity(smsIntent);
                                                    } catch (Exception e) {
                                                        Toast.makeText(getContext(), "לא נמצאה אפליקציית SMS מותקנת במכשיר", Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(getContext(), "שגיאה בעדכון הסטטוס", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvClientName, tvDateTime, tvStatus, tvDescription;
            Button btnApprove, btnReject;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClientName = itemView.findViewById(R.id.tvClientName);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}