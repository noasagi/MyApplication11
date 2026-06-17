package com.example.myapplication;

import com.google.firebase.Timestamp;

public class ChatRoomModel {

    private String chatRoomId;
    private String clientId;

    // שמירת שם הלקוח ישירות במודל חדר הצ'אט (Denormalization) כדי למנוע שאילתות כפולות ומורכבות בעת טעינת רשימת השיחות
    private String clientName;
    private String businessId;

    // שדות המשמשים להצגת תצוגה מקדימה ומיון כרונולוגי של חדר השיחה ברשימת הצ'אטים הכללית
    private String lastMessage;
    private Timestamp lastUpdate;

    /**
     * פעולה בונה ריקה (Default Constructor): דרישת חובה של Firestore לצורך המרה אוטומטית של מסמכים לאובייקט Java.
     */
    public ChatRoomModel() {}

    /**
     * פעולה בונה מלאה (Parameterized Constructor): משמשת ליצירה או עדכון מהיר של אובייקט חדר צ'אט בזיכרון המכשיר לפני שליחתו לענן.
     */
    public ChatRoomModel(String chatRoomId, String clientId, String clientName, String businessId, String lastMessage, Timestamp lastUpdate) {
        this.chatRoomId = chatRoomId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.businessId = businessId;
        this.lastMessage = lastMessage;
        this.lastUpdate = lastUpdate;
    }

    // --- פעולות גישה (Getters) סטנדרטיות עבור שדות המחלקה ---

    public String getChatRoomId() { return chatRoomId; }
    public String getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getBusinessId() { return businessId; }
    public String getLastMessage() { return lastMessage; }
    public Timestamp getLastUpdate() { return lastUpdate; }
}