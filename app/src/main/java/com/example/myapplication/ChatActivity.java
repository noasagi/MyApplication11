package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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
    private String chatRoomId; // מזהה ייחודי לחדר הצ'אט

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // אתחול פיירבייס
        db = FirebaseFirestore.getInstance();
        refAuth = FirebaseAuth.getInstance();

        if (refAuth.getCurrentUser() != null) {
            currentUserId = refAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "שגיאה: משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // קבלת מזהה החדר מהמסך הקודם (כרגע נשים מזהה זמני לבדיקות כדי שתוכלי לראות שזה עובד)
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        if (chatRoomId == null) {
            chatRoomId = "test_room_123";
        }

        // חיבור ה-Views
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        // הגדרת ה-RecyclerView וה-Adapter
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // גורם להודעות להתחיל מלמטה למעלה
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(messageAdapter);

        // לחיצה על כפתור השליחה
        btnSendMessage.setOnClickListener(v -> sendMessage());

        // הפעלת ההאזנה להודעות בזמן אמת
        listenForMessages();
    }

    private void sendMessage() {
        String text = etMessageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        Timestamp now = new Timestamp(new Date());
        Message message = new Message(currentUserId, text, now);

        // חילוק המזהה של החדר. שימי לב - אם יצרת את המזהה כך שקודם מופיע העסק ואז הלקוח,
        // צריך להפוך פה בין businessId ל-clientId!
        String[] ids = chatRoomId.split("_");
        String clientId = ids.length > 0 ? ids[0] : "";
        String businessId = ids.length > 1 ? ids[1] : "";

        ChatRoomModel roomModel = new ChatRoomModel(
                chatRoomId, clientId, "לקוח/ה", businessId, text, now
        );

        // הפעם נשמור קודם כל את מסמך החדר הראשי!
        db.collection("ChatRooms").document(chatRoomId).set(roomModel)
                .addOnSuccessListener(aVoid -> {
                    // רק אם המסמך הראשי נוצר בהצלחה (והפסיק להיות נטוי), נשמור את ההודעה
                    db.collection("ChatRooms").document(chatRoomId)
                            .collection("Messages")
                            .add(message)
                            .addOnSuccessListener(documentReference -> {
                                etMessageInput.setText(""); // ניקוי שורת הטקסט
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ChatActivity.this, "החדר נוצר אך הייתה שגיאה בשליחת ההודעה", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "שגיאה ביצירת החדר הראשי: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void listenForMessages() {
        db.collection("ChatRooms").document(chatRoomId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING) // סידור כרונולוגי
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            return; // אם יש שגיאה, מתעלמים
                        }

                        if (value != null) {
                            messageList.clear(); // מנקים את הרשימה כדי למנוע כפילויות
                            for (QueryDocumentSnapshot doc : value) {
                                Message message = doc.toObject(Message.class);
                                messageList.add(message);
                            }

                            // מודיעים ל-Adapter שיש נתונים חדשים והוא מצייר את המסך מחדש
                            messageAdapter.notifyDataSetChanged();

                            // גוללים להודעה האחרונה שנשלחה (לתחתית המסך)
                            if (messageList.size() > 0) {
                                recyclerViewChat.scrollToPosition(messageList.size() - 1);
                            }
                        }
                    }
                });
    }
}