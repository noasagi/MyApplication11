package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// מחלקת אדפטר (Adapter) מותאמת אישית המנהלת תצוגה דינמית מרובת סוגים (Multi-View) עבור בועות הטקסט בחלון הצ'אט
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    // הגדרת קבועים מספריים (Constants) המייצגים את סוגי התצוגה הגרפית השונים בתוך חלון השיחה
    private static final int VIEW_TYPE_SENT = 1;     // מזהה עבור הודעה שנשלחה על ידי המשתמש המקומי (יוצאת)
    private static final int VIEW_TYPE_RECEIVED = 2; // מזהה עבור הודעה שהתקבלה מהמשתמש בצד השני (נכנסת)

    // רשימה דינמית המכילה את כל אובייקטי ההודעות (`Message`) השייכים לשיחה הנוכחית
    private List<Message> messageList;
    // מזהה המשתמש הייחודי (UID) של האדם שכרגע אוחז במכשיר ומביט במסך
    private String currentUserId;

    // פעולה בונה (Constructor) המקבלת את רשימת ההודעות ואת מזהה המשתמש הנוכחי לצורך ביצוע ההפרדה הגרפית
    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    // פונקציה קריטית של המערכת הבודקת את מאפייני הפריט במיקום הנוכחי ומחזירה את סוג התצוגה המתאים לו
    @Override
    public int getItemViewType(int position) {
        // שליפת אובייקט ההודעה הספציפי מתוך הרשימה לפי מיקומו ברצף ה-RecyclerView
        Message message = messageList.get(position);

        // תנאי לוגי: אם מזהה שולח ההודעה זהה לחלוטין למזהה המשתמש הנוכחי של המכשיר
        if (message.getSenderId().equals(currentUserId)) {
            // החזרת קבוע המייצג הודעה יוצאת (תנופח בעיצוב ימני/שולח)
            return VIEW_TYPE_SENT;
        } else {
            // החזרת קבוע המייצג הודעה נכנסת (תנופח בעיצוב שמאלי/מקבל)
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    // פונקציה המופעלת על ידי המערכת כדי לייצר מחזיק רכיבים (ViewHolder) חדש, תוך התחשבות בסוג התצוגה (viewType) שחושב
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        // התניית ניפוח הממשק: בדיקה איזה קבוע מספרי התקבל מפונקציית getItemViewType
        if (viewType == VIEW_TYPE_SENT) {
            // טעינה ואינפלציה של קובץ ה-XML המעצב הודעה שנשלחה על ידינו (למשל, בועה ימנית)
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            // טעינה ואינפלציה של קובץ ה-XML המעצב הודעה שהתקבלה מהצד השני (למשל, בועה שמאלית)
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }

        // החזרת מופע ViewHolder חדש המקושר לקובץ העיצוב הספציפי שנבחר ונטען
        return new MessageViewHolder(view);
    }

    @Override
    // פונקציה המופעלת על ידי המערכת כדי לצקת את תוכן הנתונים הגולמי מתוך אובייקט ההודעה אל רכיבי הממשק בשורה
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        // שליפת אובייקט ההודעה הנוכחי מתוך הרשימה על פי המיקום (Position)
        Message message = messageList.get(position);
        // הצבת מחרוזת הטקסט של ההודעה בתוך רכיב ה-TextView המיועד לכך ב-ViewHolder
        holder.tvMessageContent.setText(message.getText());
    }

    @Override
    // פונקציה המחזירה למערכת את כמות הפריטים הכוללת הקיימת ברשימת ההודעות (קובע את גודל הרשימה על המסך)
    public int getItemCount() {
        return messageList.size();
    }

    // תת-מחלקה פנימית וסטטית המייצגת את מחזיק הרכיבים הגרפיים (ViewHolder) של הודעה בודדת בשיחה
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        // הצהרה על רכיב הטקסט המיועד להצגת תוכן ההודעה
        TextView tvMessageContent;

        // פעולה בונה המקבלת את תצוגת הבועה ומקשרת בין משתנה הג'אווה לרכיב ה-XML בפועל
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור משתנה התוכן לרכיב ה-TextView הנמצא בתוך קובץ ה-XML (משותף לשני סוגי ה-Layouts בזכות מזהה זהה)
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
        }
    }
}