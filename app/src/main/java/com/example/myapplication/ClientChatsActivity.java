package com.example.myapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ClientChatsActivity extends BaseActivity {

    private RecyclerView rvClientChats;
    private ClientChatsAdapter adapter;
    private List<ChatRoomModel> chatRoomsList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי המסך, מגדירה את סרגל הכלים (Toolbar) ומקשרת את ה-RecyclerView לאדפטר.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_chats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        rvClientChats = findViewById(R.id.rvClientChats);
        rvClientChats.setLayoutManager(new LinearLayoutManager(this));

        chatRoomsList = new ArrayList<>();
        adapter = new ClientChatsAdapter(chatRoomsList);
        rvClientChats.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadClientChats();
    }

    /**
     * מה הפעולה עושה: מאזינה בזמן אמת לכל חדרי הצ'אט שבהם המשתמש הנוכחי מוגדר כ-clientId, וממיינת אותם כרונולוגית לפי מועד ההודעה האחרונה.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadClientChats() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "יש להתחבר כדי לראות הודעות", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentClientId = auth.getCurrentUser().getUid();

        // שאילתה מול אוסף ChatRooms לסינון השיחות המשויכות ללקוח המחובר
        db.collection("ChatRooms")
                .whereEqualTo("clientId", currentClientId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null || value == null) return;

                        chatRoomsList.clear(); // ניקוי הרשימה המקומית למניעת כפילויות תצוגה בריענון

                        for (QueryDocumentSnapshot doc : value) {
                            ChatRoomModel room = doc.toObject(ChatRoomModel.class);
                            chatRoomsList.add(room);
                        }

                        // אלגוריתם מיון: סידור החדרים בסדר כרונולוגי יורד (הודעות חדשות ביותר יופיעו בראש הרשימה)
                        Collections.sort(chatRoomsList, new Comparator<ChatRoomModel>() {
                            @Override
                            public int compare(ChatRoomModel r1, ChatRoomModel r2) {
                                if (r1.getLastUpdate() == null || r2.getLastUpdate() == null) {
                                    return 0; // הגנה מפני ערכי Null בחותמי הזמן בענן
                                }
                                // השוואה הפוכה (r2 מול r1) כדי ליצור סדר יורד (Descending Order)
                                return r2.getLastUpdate().compareTo(r1.getLastUpdate());
                            }
                        });

                        adapter.notifyDataSetChanged(); // עדכון גרפי של הרשימה על המסך
                    }
                });
    }
}