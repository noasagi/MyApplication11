package com.example.myapplication;

import com.google.firebase.Timestamp;

public class Message {

    private String senderId;
    private String text;
    private Timestamp timestamp;

    /**
     * מה הפעולה עושה: פעולה בונה ריקה (Default Constructor). דרישת חובה (חוק בל יעבור) של Firestore לצורך פענוח והמרה אוטומטית (Deserialization) של מסמכי הנתונים לאובייקטי Java באמצעות מתודות כמו toObject.
     * קלט: אין.
     * פלט: מופע ריק של המחלקה.
     */
    public Message() {
    }

    /**
     * מה הפעולה עושה: פעולה בונה מלאה לאתחול מהיר של אובייקט הודעה חדש בזיכרון המערכת רגע לפני שליחתו והזרקתו לבסיס הנתונים בענן.
     * קלט: String senderId, String text, Timestamp timestamp.
     * פלט: מופע מאותחל של מחלקת Message.
     */
    public Message(String senderId, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    // --- פעולות גישה ועדכון (Getters & Setters) ליישום עקרון הכמוסה (Encapsulation) ---

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