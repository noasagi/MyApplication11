package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    // מגדירים שני סוגי תצוגות (אחד לשולח ואחד למקבל)
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private String currentUserId;

    // בנאי שמקבל את רשימת ההודעות ואת ה-ID של המשתמש שכרגע מחזיק את הטלפון
    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    // הפונקציה הזו בודקת מי שלח את ההודעה ומחזירה את סוג התצוגה המתאים
    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT; // ההודעה נשלחה על ידינו
        } else {
            return VIEW_TYPE_RECEIVED; // ההודעה התקבלה מהצד השני
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        // כאן אנחנו מנפחים (Inflate) את ה-XML המתאים לפי התשובה של getItemViewType
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        // כאן אנחנו לוקחים את הטקסט מההודעה ושמים אותו בתוך ה-TextView
        Message message = messageList.get(position);
        holder.tvMessageContent.setText(message.getText());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // מחלקה פנימית שתופסת את רכיבי ה-UI (במקרה שלנו רק ה-TextView של התוכן)
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
        }
    }
}