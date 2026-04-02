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

import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BusinessChatsAdapter extends RecyclerView.Adapter<BusinessChatsAdapter.ChatViewHolder> {

    private List<ChatRoomModel> chatRooms;
    private FirebaseFirestore db;

    public BusinessChatsAdapter(List<ChatRoomModel> chatRooms) {
        this.chatRooms = chatRooms;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatRoomModel room = chatRooms.get(position);

        holder.tvLastMessage.setText(room.getLastMessage());

        // כאן אנחנו מושכים את השם והתמונה המעודכנים של הלקוח ישירות מהמסד!
        db.collection("users").document(room.getClientId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        holder.tvClientName.setText(name != null ? name : "לקוח");

                        Blob imageBlob = documentSnapshot.getBlob("profileImageBlob");
                        if (imageBlob != null) {
                            byte[] bytes = imageBlob.toBytes();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            holder.imgChatProfile.setImageBitmap(bitmap);
                        }
                    }
                });

        // לחיצה פותחת את הצ'אט
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatActivity.class);
            intent.putExtra("chatRoomId", room.getChatRoomId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientName, tvLastMessage;
        ImageView imgChatProfile; // הוספנו את התמונה

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClientName = itemView.findViewById(R.id.tvChatClientName);
            tvLastMessage = itemView.findViewById(R.id.tvChatLastMessage);
            imgChatProfile = itemView.findViewById(R.id.imgChatProfile);
        }
    }
}