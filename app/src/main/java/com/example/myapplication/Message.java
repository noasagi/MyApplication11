package com.example.myapplication;

import com.google.firebase.Timestamp;

// מחלקת מודל (Model Class) פסיבית המייצגת הודעה בודדת בתוך שיחת צ'אט ומיועדת למיפוי אוטומטי מול פיירסטור
public class Message {

    // מזהה ייחודי (UID) של שולח ההודעה - יכול להיות מזהה של לקוח או מזהה של בעל עסק
    private String senderId;
    // תוכן הטקסט המילולי של ההודעה שנשלחה בחלון השיחה
    private String text;
    // אובייקט זמן מובנה של פיירבייס (Timestamp) המתעד את רגע משלוח ההודעה המדויק בענן
    private Timestamp timestamp;

    // פעולה בונה ריקה (Default Constructor) - דרישת חובה של פרוטוקול פיירסטור לצורך המרת מסמכים אוטומטית לאובייקט ג'אווה
    public Message() {
    }

    // פעולה בונה מלאה (Parameterized Constructor) לאתחול אובייקט הודעה חדש בזיכרון עם כל שדותיו
    public Message(String senderId, String text, Timestamp timestamp) {
        this.senderId = senderId;       // השמת מזהה שולח ההודעה
        this.text = text;               // השמת תוכן הטקסט של ההודעה
        this.timestamp = timestamp;     // השמת חותמת הזמן של שליחת ההודעה
    }

    // --- פעולות גישה ועדכון (Getters & Setters) סטנדרטיות עבור שדות המחלקה (שמירה על עקרון הכמוסה) ---

    // פונקציה לקבלת מזהה שולח ההודעה
    public String getSenderId() {
        return senderId;
    }

    // פונקציה לעדכון מזהה שולח ההודעה
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    // פונקציה לקבלת תוכן ההודעה
    public String getText() {
        return text;
    }

    // פונקציה לעדכון תוכן ההודעה
    public void setText(String text) {
        this.text = text;
    }

    // פונקציה לקבלת חותמת הזמן של ההודעה
    public Timestamp getTimestamp() {
        return timestamp;
    }

    // פונקציה לעדכון חותמת הזמן של ההודעה
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}