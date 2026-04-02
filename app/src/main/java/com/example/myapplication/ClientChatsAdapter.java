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

public class ClientChatsAdapter extends RecyclerView.Adapter<ClientChatsAdapter.ChatViewHolder> {

    private List<ChatRoomModel> chatRooms;
    private FirebaseFirestore db;

    public ClientChatsAdapter(List<ChatRoomModel> chatRooms) {
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

        // מושכים את השם והתמונה של העסק ממסד הנתונים
        db.collection("businesses").whereEqualTo("ownerId", room.getBusinessId()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // ניקח את המסמך הראשון של העסק שמצאנו
                        BusinessModel business = queryDocumentSnapshots.getDocuments().get(0).toObject(BusinessModel.class);
                        if (business != null) {
                            holder.tvClientName.setText(business.getName());

                            // משיכת התמונה הראשונה של העסק (אם יש)
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
                });

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
        ImageView imgChatProfile;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            // משתמשים באותו עיצוב של item_chat_room שיצרנו קודם!
            tvClientName = itemView.findViewById(R.id.tvChatClientName);
            tvLastMessage = itemView.findViewById(R.id.tvChatLastMessage);
            imgChatProfile = itemView.findViewById(R.id.imgChatProfile);
        }
    }
}