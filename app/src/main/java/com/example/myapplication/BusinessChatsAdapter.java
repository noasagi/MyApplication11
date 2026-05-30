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

// מחלקת אדפטר (Adapter) המקשרת בין רשימת הנתונים של חדרי הצ'אט לבין רכיב התצוגה הממוחזרת (RecyclerView) בצד העסק
public class BusinessChatsAdapter extends RecyclerView.Adapter<BusinessChatsAdapter.ChatViewHolder> {

    // רשימה דינמית המכילה את אובייקטי המודל של חדרי הצ'אט הפעילים במערכת
    private List<ChatRoomModel> chatRooms;
    // מופע הגישה הראשי לבסיס הנתונים בענן Cloud Firestore
    private FirebaseFirestore db;

    // פעולה בונה (Constructor) המקבלת את רשימת חדרי הצ'אט ומאתחלת את מופע הפנייה לפיירבייס
    public BusinessChatsAdapter(List<ChatRoomModel> chatRooms) {
        this.chatRooms = chatRooms;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    // פונקציה המופעלת על ידי המערכת כדי לייצר מבנה גרפי חדש (ViewHolder) עבור שורת חדר צ'אט ברשימה
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינה ואינפלציה (Inflation) של קובץ ה-XML המייצג שורת חדר צ'אט בודדת
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
        // החזרת מופע ViewHolder חדש המקושר לעיצוב שנטען
        return new ChatViewHolder(view);
    }

    @Override
    // פונקציה המופעלת על ידי המערכת כדי לצקת נתונים מתוך אובייקט חדר צ'אט אל תוך רכיבי הממשק
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        // שליפת אובייקט חדר הצ'אט הנוכחי מתוך הרשימה על פי המיקום (Position)
        ChatRoomModel room = chatRooms.get(position);

        // הצגת תוכן ההודעה האחרונה שנשלחה בצ'אט בתוך רכיב הטקסט המתאים
        holder.tvLastMessage.setText(room.getLastMessage());

        // --- פנייה דינמית לענן לשליפת נתוני הלקוח המשויך לצ'אט ---
        // שליפת מסמך המשתמש מתוך אוסף המשתמשים (users) על פי מזהה הלקוח הייחודי השמור בחדר הצ'אט
        db.collection("users").document(room.getClientId()).get()
                // הגדרת מאזין הצלחה אנונימי קלאסי לקבלת תוצאות השליפה מהענן
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // תנאי: מוודאים שהמסמך אכן קיים בענן ומכיל נתונים
                        if (documentSnapshot.exists()) {
                            // שליפת השדה הטקסטואלי של שם הלקוח מתוך המסמך
                            String name = documentSnapshot.getString("name");
                            // הצבת השם ברכיב הטקסט (במידה והשם ריק, יוצג טקסט ברירת המחדל "לקוח")
                            holder.tvClientName.setText(name != null ? name : "לקוח");

                            // שליפת שדה התמונה הבינארי (Blob) מתוך מסמך המשתמש בענן
                            Blob imageBlob = documentSnapshot.getBlob("profileImageBlob");
                            // תנאי: אם קיימת תמונת פרופיל שמורה עבור המשתמש
                            if (imageBlob != null) {
                                // המרת ה-Blob למערך בייטים (byte[]) פשוט
                                byte[] bytes = imageBlob.toBytes();
                                // פיענוח מערך הבייטים והמרתו לאובייקט תמונה מסוג Bitmap של אנדרואיד
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                // הצבת תמונת הפרופיל בתוך רכיב ה-ImageView המיועד ברשימה
                                holder.imgChatProfile.setImageBitmap(bitmap);
                            }
                        }
                    }
                });

        // --- הגדרת האזנה ללחיצה על שורת חדר הצ'אט (כניסה למסך שיחה פעילה) ---
        // שימוש במופע אנונימי קלאסי של View.OnClickListener במקום למדא
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת כוונת (Intent) למעבר מהמסך הנוכחי אל מסך חלון הצ'אט המורחב והפעיל
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                // העברת מזהה חדר הצ'אט הייחודי כפרמטר (Extra) כדי שהמסך הבא ידע אילו הודעות לטעון
                intent.putExtra("chatRoomId", room.getChatRoomId());
                // התחלת האקטיביטי ופתיחת חלון השיחה בפועל באמצעות ה-Context של הרכיב שנלחץ
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    // פונקציה המחזירה למערכת את כמות הפריטים הכוללת הקיימת ברשימת חדרי הצ'אט
    public int getItemCount() {
        return chatRooms.size();
    }

    // תת-מחלקה פנימית וסטטית המייצגת את מחזיק הרכיבים (ViewHolder) של שורת חדר הצ'אט
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        // הצהרה על רכיבי הטקסט והתמונה המרכיבים את עיצוב ה-XML של שורת הצ'אט
        TextView tvClientName, tvLastMessage;
        ImageView imgChatProfile;

        // פעולה בונה המקבלת את תצוגת השורה ומקשרת בין המשתנים לרכיבי ה-XML בפועל
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור משתנה שם הלקוח לרכיב ה-XML המתאים על פי מזהה
            tvClientName = itemView.findViewById(R.id.tvChatClientName);
            // קישור משתנה תוכן ההודעה האחרונה לרכיב ה-XML המתאים על פי מזהה
            tvLastMessage = itemView.findViewById(R.id.tvChatLastMessage);
            // קישור משתנה תמונת הפרופיל של הלקוח לרכיב ה-XML המתאים על פי מזהה
            imgChatProfile = itemView.findViewById(R.id.imgChatProfile);
        }
    }
}