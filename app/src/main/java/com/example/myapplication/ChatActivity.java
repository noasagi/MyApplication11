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

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private EditText etMessageInput;
    private Button btnSendMessage;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private FirebaseFirestore db;
    private FirebaseAuth refAuth;
    private String currentUserId;
    private String chatRoomId;

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי המערכת, מחלצת את מזהה החדר מה-Intent, ומגדירה LayoutManager ייעודי לצ'אט (הערמה מלמטה למעלה).
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        refAuth = FirebaseAuth.getInstance();

        if (refAuth.getCurrentUser() != null) {
            currentUserId = refAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "שגיאה: משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // חילוץ מזהה חדר השיחה הייחודי שהועבר במסך הקודם
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        if (chatRoomId == null) {
            chatRoomId = "test_room_123"; // הגנת ברירת מחדל
        }

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        // תכונה קריטית למסכי צ'אט: גורמת לרשימה להיערך ולהתחיל מתחתית המסך כלפי מעלה
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(messageAdapter);

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        // הפעלת צינור ההאזנה להודעות נכנסות/יוצאות בזמן אמת
        listenForMessages();
    }

    /**
     * מה הפעולה עושה: קוראת את קלט הטקסט, מייצרת אובייקטים של הודעה וחדר, ומבצעת כתיבה דו-שלבית מול Firestore (עדכון החדר הראשית- הוספת ההודעה לתת-האוסף).
     * קלט: אין.
     * פלט: אין (void).
     */
    private void sendMessage() {
        String text = etMessageInput.getText().toString().trim();
        if (text.isEmpty()) return; // הגנה מפני שליחת מחרוזת ריקה

        Timestamp now = new Timestamp(new Date());
        Message message = new Message(currentUserId, text, now);

        // לוגיקת חילוץ מזהי המשתתפים מתוך מחרוזת מזהה החדר (למשל: "clientUID_businessUID")
        String[] ids = chatRoomId.split("_");
        String clientId = ids.length > 0 ? ids[0] : "";
        String businessId = ids.length > 1 ? ids[1] : "";

        ChatRoomModel roomModel = new ChatRoomModel(
                chatRoomId, clientId, "לקוח/ה", businessId, text, now
        );

        // שלב א': כתיבה/עדכון של מסמך חדר הצ'אט הראשי באוסף הכללי לצורך הצגת הודעה אחרונה בתפריט השיחות
        db.collection("ChatRooms").document(chatRoomId).set(roomModel)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // שלב ב': רק לאחר שהחדר הראשי מעודכן, מוסיפים את מסמך ההודעה לתוך תת-האוסף המקונן (Messages)
                        db.collection("ChatRooms").document(chatRoomId)
                                .collection("Messages")
                                .add(message)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        etMessageInput.setText(""); // ניקוי התיבה מיד עם השליחה
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
    /**
     * מה הפעולה עושה: פותחת מאזין SnapshotListener לתת-האוסף של ההודעות, ממיינת אותן כרונולוגית, ומבצעת גלילה אוטומטית להודעה האחרונה בכל עדכון.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void listenForMessages() {
        // פנייה לתת-האוסף הפנימי ומיון ההודעות מהישנה ביותר (למעלה) לחדישה ביותר (למטה)
        db.collection("ChatRooms").document(chatRoomId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) return;

                        if (value != null) {
                            messageList.clear(); // ניקוי הרשימה המקומית בזיכרון למניעת כפילויות תצוגה
                            for (QueryDocumentSnapshot doc : value) {
                                Message message = doc.toObject(Message.class);
                                messageList.add(message);
                            }

                            // התיקון כאן: שימוש בשם המשתנה הנכון שהוגדר בראש המחלקה!
                            messageAdapter.notifyDataSetChanged();

                            // מנגנון חווית משתמש (UX): גלילה אוטומטית ממוקדת של ה-RecyclerView אל ההודעה האחרונה שהתקבלה בשרשרת
                            if (messageList.size() > 0) {
                                recyclerViewChat.scrollToPosition(messageList.size() - 1);
                            }
                        }
                    }
                });
    }
}