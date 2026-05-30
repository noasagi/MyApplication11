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

// הגדרת מחלקה לניהול מסך רשימת הצ'אטים של העסק, היורשת ממאפייני Fragment
public class BusinessChatsFragment extends Fragment {

    // הצהרה על רכיב הרשימה הממוחזרת להצגת חלוניות הצ'אט במסך
    private RecyclerView rvBusinessChats;

    // הצהרה על המתאם המותאם אישית שמקשר בין נתוני חדר הצ'אט לרכיבי התצוגה
    private BusinessChatsAdapter adapter;

    // הצהרה על רשימה דינמית שתכיל את עצמי מודל חדר הצ'אט
    private List<ChatRoomModel> chatRoomsList;

    // הצהרה על עצם הגישה לבסיס הנתונים פיירסטור של פיירבייס
    private FirebaseFirestore db;

    // הצהרה על עצם הגישה למערכת אימות המשתמשים של פיירבייס
    private FirebaseAuth auth;

    // פעולת מערכת האחראית על יצירת וניפוח ממשק המשתמש הויזואלי של הפרגמנט
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // מנפחים ומטעינים את קובץ ה-XML של עיצוב מסך הצ'אטים של העסק
        View view = inflater.inflate(R.layout.fragment_business_chats, container, false);

        // קישור משתנה הרשימה לרכיב הויזואלי הממוחזר הנמצא בתוך ה-XML
        rvBusinessChats = view.findViewById(R.id.rvBusinessChats);
        // הגדרת מנהל פריסה אנכי עבור הרשימה הממוחזרת להצגת הפריטים אחד מתחת לשני
        rvBusinessChats.setLayoutManager(new LinearLayoutManager(getContext()));

        // אתחול הרשימה הדינמית בזיכרון לשמירת חדרים ומודלים של צ'אט
        chatRoomsList = new ArrayList<>();
        // יצירת מופע חדש של המתאם והעברת רשימת החדרים הריקה אליו
        adapter = new BusinessChatsAdapter(chatRoomsList);
        // חיבור רשמי של המתאם המתוכנת אל רכיב הרשימה הממוחזרת בממשק הויזואלי
        rvBusinessChats.setAdapter(adapter);

        // אתחול וקבלת המופע הנוכחי של בסיס הנתונים פיירסטור
        db = FirebaseFirestore.getInstance();
        // אתחול וקבלת המופע הנוכחי של מערכת האימות פיירבייס לקוד
        auth = FirebaseAuth.getInstance();

        // קריאה לפעולה הפנימית האחראית על טעינת והאזנה לחדרי הצ'אט הפעילים
        loadChats();

        // החזרת מבט התצוגה המלא והמוכן של הפרגמנט
        return view;
    }

    // פעולה פרטית הטוענת ומאזינה לכל חדרי הצ'אט המשויכים לבעל עסק זה בזמן אמת
    private void loadChats() {
        // בדיקת הגנה לוודא שקיים משתמש מחובר כרגע במערכת האימות
        if (auth.getCurrentUser() == null) return;

        // שמירת ה-UID הייחודי של בעל העסק המחובר כרגע למערכת
        String currentBusinessId = auth.getCurrentUser().getUid();

        // פנייה לאוסף חדרי הצ'אט והצבת מאזין דינמי שמסנן חדרים שבהם מזהה העסק תואם לבעלים
        db.collection("ChatRooms")
                .whereEqualTo("businessId", currentBusinessId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    // פעולה המופעלת אוטומטית בכל פעם שנוצר חדר, נשלחת הודעה חדשה, או חל שינוי בענן
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        // בדיקה האם התרחשה שגיאה בתקשורת או שהנתונים שחזרו מהשרת ריקים
                        if (error != null || value == null) {
                            // יציאה ועצירת התהליך ללא קריסת האפליקציה במקרה של שגיאה
                            return;
                        }

                        // ניקוי רשימת החדרים המקומית בזיכרון המכשיר לפני קבלת הנתונים המעודכנים
                        chatRoomsList.clear();

                        // לולאה העוברת על כל מסמך חדר צ'אט בנפרד מתוך אוסף התוצאות שחזרו מהמסד
                        for (QueryDocumentSnapshot doc : value) {
                            // המרת נתוני המסמך ישירות לעצם מובנה מסוג מודל חדר הצ'אט
                            ChatRoomModel room = doc.toObject(ChatRoomModel.class);
                            // הוספת עצם החדר שפוענח אל תוך רשימת החדרים המקומית
                            chatRoomsList.add(room);
                        }

                        // שימוש במחלקת עזר למיון הרשימה הדינמית בזיכרון המקומי באמצעות בנאי אנונימי של השוואה
                        Collections.sort(chatRoomsList, new Comparator<ChatRoomModel>() {
                            // פעולת ההשוואה המגדירה את חוקי המיון של שני חדרים ברשימה
                            @Override
                            public int compare(ChatRoomModel r1, ChatRoomModel r2) {
                                // בדיקת הגנה במידה ואחד מחותמי הזמן של העדכון האחרון ריק
                                if (r1.getLastUpdate() == null || r2.getLastUpdate() == null) return 0;
                                // השוואת חותמי הזמן בסדר הפוך על מנת למיין מהחדש ביותר לישן ביותר
                                return r2.getLastUpdate().compareTo(r1.getLastUpdate());
                            }
                        });

                        // עדכון המתאם כי חל שינוי בנתוני הרשימה כדי שיצייר מחדש את המסך למשתמש
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}