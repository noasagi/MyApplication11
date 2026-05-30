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

// מחלקת אדפטר (Adapter) המקשרת בין רשימת הנתונים של חדרי הצ'אט לבין רכיב התצוגה הממוחזרת (RecyclerView) בצד הלקוח
public class ClientChatsAdapter extends RecyclerView.Adapter<ClientChatsAdapter.ChatViewHolder> {

    // רשימה דינמית המכילה את אובייקטי המודל של חדרי הצ'אט הפעילים עבור הלקוח
    private List<ChatRoomModel> chatRooms;
    // מופע הגישה הראשי לבסיס הנתונים בענן Cloud Firestore
    private FirebaseFirestore db;

    // פעולה בונה (Constructor) המקבלת את רשימת חדרי הצ'אט ומאתחלת את מופע הפנייה לפיירבייס
    public ClientChatsAdapter(List<ChatRoomModel> chatRooms) {
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

        // --- פנייה דינמית לענן לשליפת נתוני בית העסק המשויך לצ'אט ---
        // ביצוע שאילתת סינון באוסף העסקים (businesses) בהתאם למזהה הבעלים השמור בחדר הצ'אט
        db.collection("businesses").whereEqualTo("ownerId", room.getBusinessId()).get()
                // הגדרת מאזין הצלחה אנונימי קלאסי לקבלת תוצאות השאילתה מהענן
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // תנאי: מוודאים שתוצאת השאילתה אינה ריקה ונמצא לפחות מסמך עסק תואם אחד
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // שליפת המסמך הראשון שנמצא והמרתו האוטומטית לאובייקט מסוג BusinessModel
                            BusinessModel business = queryDocumentSnapshots.getDocuments().get(0).toObject(BusinessModel.class);
                            // תנאי בטיחות: מוודאים שהמרת האובייקט עברה בהצלחה
                            if (business != null) {
                                // הצבת שם בית העסק ברכיב הטקסט המיועד של כותרת השורה
                                holder.tvClientName.setText(business.getName());

                                // משיכת רשימת התמונות (imageBlobs) המשויכת לבית העסק
                                List<Blob> images = business.getImageBlobs();
                                // תנאי: אם קיימות תמונות שמורות עבור בית העסק
                                if (images != null && !images.isEmpty()) {
                                    // שליפת אובייקט ה-Blob הבינארי הראשון מתוך רשימת התמונות
                                    Blob firstImage = images.get(0);
                                    // המרת ה-Blob למערך בייטים (byte[]) פשוט
                                    byte[] bytes = firstImage.toBytes();
                                    // פיענוח מערך הבייטים והמרתו לאובייקט תמונה מסוג Bitmap של אנדרואיד
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    // הצבת תמונת הלוגו/הפרופיל של העסק בתוך רכיב ה-ImageView בשורה
                                    holder.imgChatProfile.setImageBitmap(bitmap);
                                }
                            }
                        } else {
                            // במידה ולא נמצא מסמך עסק תואם באוסף, יוצג חיווי ברירת מחדל
                            holder.tvClientName.setText("עסק לא ידוע");
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
            // קישור משתנה שם העסק לרכיב ה-XML המתאים על פי מזהה (שימוש בעיצוב הממוחזר של שורת צ'אט)
            tvClientName = itemView.findViewById(R.id.tvChatClientName);
            // קישור משתנה תוכן ההודעה האחרונה לרכיב ה-XML המתאים על פי מזהה
            tvLastMessage = itemView.findViewById(R.id.tvChatLastMessage);
            // קישור משתנה תמונת הפרופיל של העסק לרכיב ה-XML המתאים על פי מזהה
            imgChatProfile = itemView.findViewById(R.id.imgChatProfile);
        }
    }
}