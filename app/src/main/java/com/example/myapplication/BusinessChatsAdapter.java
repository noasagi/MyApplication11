package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BusinessChatsAdapter extends RecyclerView.Adapter<BusinessChatsAdapter.ChatViewHolder> {

    private List<ChatRoomModel> chatRooms;
    private FirebaseFirestore db;

    /**
     * מה הפעולה עושה: פעולה בונה (Constructor) השומרת את רשימת חלונות הצ'אט ומאתחלת את החיבור ל-Firestore.
     * קלט: List<ChatRoomModel> chatRooms (רשימת מודלים של חדרי הצ'אט).
     * פלט: אין.
     */
    public BusinessChatsAdapter(List<ChatRoomModel> chatRooms) {
        this.chatRooms = chatRooms;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * מה הפעולה עושה: מייצרתViewHolder חדש ומנפחת עבורו את קובץ ה-XML של שורת חדר הצ'אט ברשימה.
     * קלט: ViewGroup parent, int viewType.
     * פלט: ChatViewHolder (מחזיק הרכיבים של שורת הצ'אט).
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // המרת קובץ עיצוב ה-XML הסטטי לאובייקט תצוגה חי בזיכרון
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
        return new ChatViewHolder(view);
    }

    /**
     * מה הפעולה עושה: יוצקת את הנתונים של חדר צ'אט ספציפי לרכיבי הממשק, ומבצעת שליפה דינמית נוספת מ-Firestore לקבלת פרטי הלקוח.
     * קלט: ChatViewHolder holder, int position (מיקום הפריט הנוכחי ברשימה).
     * פלט: אין (void).
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatRoomModel room = chatRooms.get(position);

        // הצגת ההודעה האחרונה שנשמרת כחלק ממודל חדר הצ'אט (מגיע ישירות ללא שליפה נוספת)
        holder.tvLastMessage.setText(room.getLastMessage());

        // לוגיקה של שליפה משנית (כפולה): מודל חדר הצ'אט מכיל רק את ה-ID של הלקוח (clientId).
        // כדי להציג את השם והתמונה האמיתיים שלו, המערכת מבצעת קריאה דינמית נוספת לאוסף המשתמשים ("users").
        db.collection("users").document(room.getClientId()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            holder.tvClientName.setText(name != null ? name : "לקוח");

                            // המרת התמונה: פיענוח קובץ ה-Blob הבינארי השמור ב-Firestore והפיכתו ל-Bitmap תצוגתי
                            Blob imageBlob = documentSnapshot.getBlob("profileImageBlob");
                            if (imageBlob != null) {
                                byte[] bytes = imageBlob.toBytes();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                holder.imgChatProfile.setImageBitmap(bitmap);
                            }
                        }
                    }
                });

        // הגדרת מאזין לחיצה על כל שטח השורה. בעת לחיצה, נעבור למסך השיחה המלא (ChatActivity)
        // ונעביר אליו את ה-ID של החדר כדי שהמסך הבא ידע אילו הודעות לטעון בזמן אמת.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("chatRoomId", room.getChatRoomId());
                v.getContext().startActivity(intent);
            }
        });
    }

    /**
     * מה הפעולה עושה: מחזירה את מספר חדרי הצ'אט הכולל הקיים ברשימה.
     * קלט: אין.
     * פלט: int (גודל הרשימה).
     */
    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    // --- תת-מחלקה פנימית: מחזיק הרכיבים של שורת הצ'אט (ViewHolder) ---
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientName, tvLastMessage;
        ImageView imgChatProfile;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClientName = itemView.findViewById(R.id.tvChatClientName);
            tvLastMessage = itemView.findViewById(R.id.tvChatLastMessage);
            imgChatProfile = itemView.findViewById(R.id.imgChatProfile);
        }
    }
}