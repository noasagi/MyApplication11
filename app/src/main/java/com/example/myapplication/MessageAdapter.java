package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    // הגדרת קבועים נומריים (Constants) לניהול סוגי התצוגה הגרפית השונים בצ'אט
    private static final int VIEW_TYPE_SENT = 1;     // הודעה יוצאת (שנשלחה על ידי המשתמש המקומי)
    private static final int VIEW_TYPE_RECEIVED = 2; // הודעה נכנסת (התקבלה מהצד השני)

    private List<Message> messageList;
    private String currentUserId;

    /**
     * מה הפעולה עושה: פעולה בונה (Constructor) המקבלת את רשימת ההודעות ואת מזהה המשתמש המחובר, לצורך סינון והפרדה חזותית בין סוגי ההודעות.
     * קלט: List<Message> messageList, String currentUserId.
     * פלט: מופע מאותחל של האדפטר.
     */
    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    /**
     * מה הפעולה עושה: קובעת ומחזירה את סוג התצוגה (View Type) של הפריט בהתאם לזהות השולח.
     * קלט: int position (מיקום הפריט הנוכחי ברשימה).
     * פלט: int (הקבוע המספרי המייצג את סוג העיצוב - SENT או RECEIVED).
     */
    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);

        // תנאי לוגי: אם מזהה השולח שווה למשתמש האוחז במכשיר, נסמן את ההודעה כיוצאת
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    /**
     * מה הפעולה עושה: מנפחת (Inflate) את קובץ ה-XML המתאים (בועה ימנית או שמאלית) על בסיס ה-viewType שחושב בשלב הקודם.
     * קלט: ViewGroup parent, int viewType.
     * פלט: MessageViewHolder (מחזיק הרכיבים עם תצוגת הבועה המנופחת).
     */
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        // יישום דינמי של Multi-View Type: בחירת קובץ ה-Layout המתאים ביותר להודעה
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }

        return new MessageViewHolder(view);
    }

    /**
     * מה הפעולה עושה: יוצקת את הנתונים הגולמיים (תוכן הטקסט של ההודעה) מתוך מודל הנתונים אל רכיב ה-TextView שבמסך.
     * קלט: MessageViewHolder holder, int position.
     * פלט: אין (void).
     */
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.tvMessageContent.setText(message.getText());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // --- מחלקה פנימית וסטטית (ViewHolder) להחזקת רכיבי הבועה הבודדת ---
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            // מזהה הרכיב זהה בשני קובצי ה-XML (נכנסת/יוצאת) כדי לאפשר שימוש חוזר אלגנטי בקוד הג'אווה
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
        }
    }
}