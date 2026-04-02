package com.example.myapplication;

import com.google.firebase.Timestamp;

public class Message {

    private String senderId; // ה-ID של מי ששלח את ההודעה (לקוח או עסק)
    private String text; // תוכן ההודעה
    private Timestamp timestamp; // מתי ההודעה נשלחה (כדי שנוכל לסדר אותן)

    // בנאי ריק - חובה עבור Firebase Firestore!
    public Message() {
    }

    public Message(String senderId, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}