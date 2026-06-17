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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class ClientChatsAdapter extends RecyclerView.Adapter<ClientChatsAdapter.ChatViewHolder> {

    private List<ChatRoomModel> chatRooms;
    private FirebaseFirestore db;

    /**
     * פעולה בונה (Constructor): מקבלת את רשימת חדרי הצ'אט ומאתחלת את המופע של Firestore.
     */
    public ClientChatsAdapter(List<ChatRoomModel> chatRooms) {
        this.chatRooms = chatRooms;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * מה הפעולה עושה: מייצרת ומנפחת (Inflate) את תבנית העיצוב של שורה בודדת ברשימה ומחזירה ViewHolder.
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
        return new ChatViewHolder(view);
    }

    /**
     * מה הפעולה עושה: יוצקת את הנתונים לתוך רכיבי התצוגה של השורה הנוכחית, מבצעת שאילתה אסינכרונית לשליפת פרטי העסק והלוגו שלו, ומגדירה מאזין מעבר למסך השיחה.
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatRoomModel room = chatRooms.get(position);

        // הצגת תוכן ההודעה האחרונה מהמודל
        holder.tvLastMessage.setText(room.getLastMessage());

        // שליפת פרטי העסק ומרה בינארית של התמונה בזמן אמת מתוך ה-Blob שנשמר ב-Firestore
        db.collection("businesses").whereEqualTo("ownerId", room.getBusinessId()).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            BusinessModel business = queryDocumentSnapshots.getDocuments().get(0).toObject(BusinessModel.class);
                            if (business != null) {
                                holder.tvClientName.setText(business.getName());

                                // תהליך המרת תמונה: שליפת ה-Blob הראשי - המרה למערך בייטים (byte[]) - פענוח ל-Bitmap
                                List<Blob> images = business.getImageBlobs();
                                if (images != null && !images.isEmpty()) {
                                    Blob firstImage = images.get(0);
                                    byte[] bytes = firstImage.toBytes();
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    holder.imgChatProfile.setImageBitmap(bitmap);
                                }
                            }
                        } else {
                            holder.tvClientName.setText("עסק לא ידוע");
                        }
                    }
                });

        // הגדרת מאזין לחיצה לפתיחת חלון הצ'אט הספציפי והעברת ה-ID שלו כפרמטר ב-Intent
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("chatRoomId", room.getChatRoomId());
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    // --- מחלקת מחזיק רכיבים פנימית (ViewHolder) ---
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