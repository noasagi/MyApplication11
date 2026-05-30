package com.example.myapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

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

// הגדרת מחלקת מסך רשימת הצ'אטים של הלקוח, היורשת מ-BaseActivity
public class ClientChatsActivity extends BaseActivity {

    // הצהרה על רכיב הרשימה הממוחזרת להצגת חדרי הצ'אט השונים
    private RecyclerView rvClientChats;
    // הצהרה על המתאם המותאם אישית עבור רשימת חדרים זו
    private ClientChatsAdapter adapter;
    // רשימה דינמית המחזיקה את אובייקטי מודל חדר הצ'אט בזיכרון האפליקציה
    private List<ChatRoomModel> chatRoomsList;

    // רכיבי הגישה המרכזיים לבסיס הנתונים ומערכת ניהול המשתמשים
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת וחיבור קובץ ה-XML של עיצוב מסך רשימת הצ'אטים
        setContentView(R.layout.activity_client_chats);

        // חיבור וקישור סרגל הכלים העליון של המסך
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupSecondaryToolbar(toolbar, true);

        // ביטול כותרת ברירת המחדל של סרגל הכלים במידה והוא קיים
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // קישור רכיב הרשימה מה-XML והגדרת מנהל פריסה אנכי סטנדרטי
        rvClientChats = findViewById(R.id.rvClientChats);
        rvClientChats.setLayoutManager(new LinearLayoutManager(this));

        // אתחול רשימת החדרים ויצירת המתאם המקשר בינה לבין רכיב ה-RecyclerView
        chatRoomsList = new ArrayList<>();
        adapter = new ClientChatsAdapter(chatRoomsList);
        rvClientChats.setAdapter(adapter);

        // קבלת מופעי הגישה אל פיירסטור ומערכת האימות
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // קריאה לפעולה האחראית על טעינת והזרמת חדרים אלו מהענן
        loadClientChats();
    }

    // פעולה פרטית הטוענת ומאזינה בזמן אמת לכל חדרי השיחה המשויכים ללקוח הנוכחי
    private void loadClientChats() {
        // הגנה: בדיקה האם קיים משתמש מחובר למערכת, במידה ולא - המסך נסגר
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "יש להתחבר כדי לראות הודעות", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // שליפת מזהה ה-UID הייחודי של המשתמש (הלקוח) המחובר כעת
        String currentClientId = auth.getCurrentUser().getUid();

        // פנייה לאוסף החדרים הראשי, סינון לפי מזהה הלקוח והפעלת מאזין רציף לשינויים
        db.collection("ChatRooms")
                .whereEqualTo("clientId", currentClientId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        // הגנה: במידה ונוצרה שגיאה או שלא חזרו נתונים מהשרת - נעצור את הביצוע
                        if (error != null || value == null) {
                            return;
                        }

                        // ניקוי הרשימה המקומית בזיכרון כדי למנוע כפילויות של חדרים בעת עדכון
                        chatRoomsList.clear();

                        // מעבר בלולאה על כל מסמכי חדר הצ'אט שחזרו מהסינון בענן
                        for (QueryDocumentSnapshot doc : value) {
                            // המרת מסמך הנתונים הגולמי מהפיירסטור לאובייקט מסוג מודל חדר צ'אט
                            ChatRoomModel room = doc.toObject(ChatRoomModel.class);
                            chatRoomsList.add(room);
                        }

                        // מיון ידני של הרשימה באמצעות מחלקה אנונימית של Comparator (ללא למדא)
                        // המטרה: להציג בראש הרשימה את החדרים שבהם התקבלה ההודעה האחרונה (סדר כרונולוגי יורד)
                        Collections.sort(chatRoomsList, new Comparator<ChatRoomModel>() {
                            @Override
                            public int compare(ChatRoomModel r1, ChatRoomModel r2) {
                                // הגנה מפני ערכי Null בחותמי הזמן
                                if (r1.getLastUpdate() == null || r2.getLastUpdate() == null) {
                                    return 0;
                                }
                                // השוואה בין חותמי הזמן בצורה הפוכה (r2 מול r1) לקבלת סדר יורד
                                return r2.getLastUpdate().compareTo(r1.getLastUpdate());
                            }
                        });

                        // הודעה למתאם הרשימה לבצע ריענון חזותי של שורות החדרים על המסך
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}