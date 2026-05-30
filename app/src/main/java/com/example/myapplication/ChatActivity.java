package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// הגדרת מחלקת אקטיביטי לניהול ותצוגת מסך הצ'אט בזמן אמת
public class ChatActivity extends AppCompatActivity {

    // הצהרה על רכיב הרשימה הממוחזרת להצגת שרשור ההודעות
    private RecyclerView recyclerViewChat;
    // הצהרה על רכיב תיבת קלט הטקסט להקלדת ההודעה החדשה
    private EditText etMessageInput;
    // הצהרה על רכיב לחצן שליחת ההודעה
    private Button btnSendMessage;

    // הצהרה על המתאם המותאם אישית (Adapter) ועל רשימת אובייקטי ההודעות
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    // הצהרה על רכיבי הגישה של פיירסטור ומערכת ניהול המשתמשים
    private FirebaseFirestore db;
    private FirebaseAuth refAuth;
    // משתני מחרוזת לשמירת מזהה המשתמש הנוכחי ומזהה חדר הצ'אט הספציפי
    private String currentUserId;
    private String chatRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת וחיבור קובץ ה-XML של עיצוב מסך הצ'אט
        setContentView(R.layout.activity_chat);

        // אתחול וקבלת המופע הנוכחי של פיירסטור ואימות פיירבייס
        db = FirebaseFirestore.getInstance();
        refAuth = FirebaseAuth.getInstance();

        // הגנה: בדיקה האם קיים משתמש מחובר למערכת, אם לא - המסך נסגר מיידית
        if (refAuth.getCurrentUser() != null) {
            currentUserId = refAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "שגיאה: משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // חילוץ מזהה חדר הצ'אט שהועבר בתוך ה-Intent מהמסך הקודם
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        // ברירת מחדל לבדיקות מקומיות במקרה שלא הועבר מזהה חדר
        if (chatRoomId == null) {
            chatRoomId = "test_room_123";
        }

        // קישור משתני הרכיבים לרכיבים הגרפיים האמיתיים מתוך קובץ ה-XML
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        // אתחול רשימת ההודעות הדינמית ויצירת מופע של המתאם המותאם אישית
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);

        // הגדרת מנהל פריסה אנכי (LinearLayoutManager) עבור רכיב הרשימה
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        // הגדרת תכונה הגורמת להודעות להיערם ולהתחיל מתחתית המסך כלפי מעלה (מתאים למסכי שיחה)
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(messageAdapter);

        // הגדרת מאזין לחיצה אנונימי קלאסי לכפתור שליחת ההודעה
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        // הפעלת פעולת ההאזנה הרציפה לקבלת הודעות חדשות בזמן אמת מהענן
        listenForMessages();
    }

    // פעולה פרטית האחראית על בניית אובייקט ההודעה, יצירת חדר הצ'אט ושמירת הנתונים בענן
    private void sendMessage() {
        // קריאת הטקסט מתיבת הקלט וניקוי רווחים מיותרים מהקצוות
        String text = etMessageInput.getText().toString().trim();
        // הגנה: מניעת שליחה של הודעה ריקה
        if (text.isEmpty()) {
            return;
        }

        // יצירת חותם זמן עדכני של פיירבייס המבוסס על תאריך ושעת המכשיר הנוכחיים
        Timestamp now = new Timestamp(new Date());
        // יצירת מופע חדש של מודל הודעה עם מזהה השולח, הטקסט וזמן השליחה
        Message message = new Message(currentUserId, text, now);

        // פיצול מזהה חדר הצ'אט באמצעות קו תחתון כדי לחלץ את מזהי המשתתפים בשיחה
        String[] ids = chatRoomId.split("_");
        String clientId = ids.length > 0 ? ids[0] : "";
        String businessId = ids.length > 1 ? ids[1] : "";

        // יצירת אובייקט מודל עבור חדר הצ'אט המרכז את פרטי החדר העדכניים וההודעה האחרונה
        ChatRoomModel roomModel = new ChatRoomModel(
                chatRoomId, clientId, "לקוח/ה", businessId, text, now
        );

        // שלב א': כתיבה ועדכון של מסמך חדר הצ'אט הראשי באוסף הכללי של פיירסטור
        db.collection("ChatRooms").document(chatRoomId).set(roomModel)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // שלב ב': רק לאחר הצלחת יצירת/עדכון החדר הראשי, נכנסים לתת-אוסף פנימי ומוסיפים את מסמך ההודעה
                        db.collection("ChatRooms").document(chatRoomId)
                                .collection("Messages")
                                .add(message)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        // ניקוי תיבת קלט הטקסט על המסך עם סיום השליחה המוצלחת
                                        etMessageInput.setText("");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(ChatActivity.this, "החדר נוצר אך הייתה שגיאה בשליחת ההודעה", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, "שגיאה ביצירת החדר הראשי: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // פעולה פרטית הפותחת ערוץ האזנה רציף (צינור נתונים) לקבלת הודעות המשויכות לחדר זה
    private void listenForMessages() {
        db.collection("ChatRooms").document(chatRoomId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING) // מיון כרונולוגי מהישן ביותר לחדיש ביותר
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        // הגנה: במידה ונוצרה שגיאה בתקשורת או בהרשאות מול הענן - נעצור את הפעולה
                        if (error != null) {
                            return;
                        }

                        // בדיקה שאכן התקבלו נתונים תקינים מתוך הענן
                        if (value != null) {
                            // ניקוי רשימת ההודעות המקומית בזיכרון כדי למנוע כפילויות של מידע ישן וחדש במסך
                            messageList.clear();
                            // מעבר בלולאה מובנית על כל מסמכי ההודעות שהתקבלו מהשאילתה בענן
                            for (QueryDocumentSnapshot doc : value) {
                                // המרת מסמך הנתונים הגולמי מהפיירסטור ישירות לאובייקט מסוג מחלקת ההודעה
                                Message message = doc.toObject(Message.class);
                                messageList.add(message);
                            }

                            // עדכון המתאם (Adapter) על כך שחלו שינויים במקור הנתונים כדי שיצייר את רכיבי השיחה מחדש
                            messageAdapter.notifyDataSetChanged();

                            // מנגנון גלילה אוטומטי: אם קיימות הודעות בשיחה, נבצע גלילה של הרשימה ישירות למיקום ההודעה האחרונה
                            if (messageList.size() > 0) {
                                recyclerViewChat.scrollToPosition(messageList.size() - 1);
                            }
                        }
                    }
                });
    }
}