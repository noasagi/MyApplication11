package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class BusinessChatsFragment extends Fragment {

    private RecyclerView rvBusinessChats;
    private BusinessChatsAdapter adapter;
    private List<ChatRoomModel> chatRoomsList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    /**
     * מה הפעולה עושה: מנפחת (טוענת) את קובץ ה-XML של הפרגמנט, מאתחלת את רכיבי הרשימה, החיבור למסד הנתונים ומפעילה את טעינת הצ'אטים.
     * קלט: LayoutInflater, ViewGroup container, Bundle savedInstanceState.
     * פלט: View (מסך הפרגמנט המוכן להצגה).
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_business_chats, container, false);

        rvBusinessChats = view.findViewById(R.id.rvBusinessChats);

        // הגדרת פריסה אנכית (LinearLayoutManager) כך שהצ'אטים יוצגו זה תחת זה כמו בוואטסאפ
        rvBusinessChats.setLayoutManager(new LinearLayoutManager(getContext()));

        chatRoomsList = new ArrayList<>();
        adapter = new BusinessChatsAdapter(chatRoomsList);
        rvBusinessChats.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // הפעלת מאזין הצ'אטים מיד עם יצירת המסך
        loadChats();

        return view;
    }

    /**
     * מה הפעולה עושה: מחברת מאזין בזמן אמת (addSnapshotListener) לחדרי הצ'אט של העסק המחובר, וממיינת אותם מהחדש ביותר לישן ביותר.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void loadChats() {
        if (auth.getCurrentUser() == null) return;

        // במערכת שלנו, מזהה העסק (businessId) זהה לחלוטין ל-UID של בעל העסק שפתח אותו
        String currentBusinessId = auth.getCurrentUser().getUid();

        // שימוש ב-addSnapshotListener מאפשר לנו "להקשיב" לשינויים בזמן אמת.
        // בכל פעם שלקוח ישלח הודעה חדשה, השרת יקפיץ אוטומטית את הקוד שבתוך onEvent ויעדכן את המסך ללא צורך בריענון ידני.
        db.collection("ChatRooms")
                .whereEqualTo("businessId", currentBusinessId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        // חסימת שגיאות: אם יש בעיית קליטה או תקשורת, הפונקציה נעצרת כדי למנוע קריסה של האפליקציה
                        if (error != null || value == null) {
                            return;
                        }

                        // ניקוי הרשימה הישנה חיוני כדי למנוע כפילויות של אותם חדרים בכל פעם שמגיעה הודעה חדשה
                        chatRoomsList.clear();

                        for (QueryDocumentSnapshot doc : value) {
                            ChatRoomModel room = doc.toObject(ChatRoomModel.class);
                            chatRoomsList.add(room);
                        }

                        // לוגיקת מיון: שימוש ב-Comparator כדי למיין את חדרי הצ'אט לפי תאריך העדכון האחרון שלהם.
                        // ההשוואה מתבצעת בסדר הפוך (r2 מול r1) כדי שהצ'אט עם ההודעה הכי עדכנית יקפוץ תמיד לראש הרשימה.
                        Collections.sort(chatRoomsList, new Comparator<ChatRoomModel>() {
                            @Override
                            public int compare(ChatRoomModel r1, ChatRoomModel r2) {
                                if (r1.getLastUpdate() == null || r2.getLastUpdate() == null) return 0;
                                return r2.getLastUpdate().compareTo(r1.getLastUpdate());
                            }
                        });

                        // פקודה המורה לאדפטר לצייר ולרענן את הרשימה מחדש על המסך בעקבות השינויים והמיון
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}