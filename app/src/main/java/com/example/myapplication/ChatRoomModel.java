package com.example.myapplication;

import com.google.firebase.Timestamp;

// מחלקת מודל (Model Class) המייצגת אובייקט של חדר צ'אט, המשמשת כתוכנית עבודה (POJO) להמרת נתונים מול Firestore
public class ChatRoomModel {
    // מזהה ייחודי עבור חדר הצ'אט במערכת (תואם ל-ID של המסמך בבסיס הנתונים)
    private String chatRoomId;
    // המזהה הייחודי (UID) של הלקוח המשתתף בשיחת הצ'אט
    private String clientId;
    // שמירת שם הלקוח באופן ישיר במודל לצורך ייעול הגישה והצגה מהירה בממשק
    private String clientName;
    // המזהה הייחודי של בית העסק המשתתף בשיחת הצ'אט
    private String businessId;
    // מחרוזת טקסט השומרת את תוכן ההודעה האחרונה שנשלחה (עבור תצוגה מקדימה ברשימת הצ'אטים)
    private String lastMessage;
    // אובייקט זמן של פיירבייס (Timestamp) המתעד את רגע שליחת ההודעה האחרונה לצורך מיון כרונולוגי
    private Timestamp lastUpdate;

    // פעולה בונה ריקה (Default Constructor) - דרישת חובה של פרוטוקול פיירסטור לצורך המרת מסמכים אוטומטית לאובייקט ג'אווה
    public ChatRoomModel() {}

    // פעולה בונה מלאה (Parameterized Constructor) המאפשרת לאתחל אובייקט חדר צ'אט חדש בזיכרון עם כל שדותיו
    public ChatRoomModel(String chatRoomId, String clientId, String clientName, String businessId, String lastMessage, Timestamp lastUpdate) {
        this.chatRoomId = chatRoomId;     // השמת מזהה חדר הצ'אט
        this.clientId = clientId;         // השמת מזהה הלקוח
        this.clientName = clientName;     // השמת שם הלקוח
        this.businessId = businessId;     // השמת מזהה בית העסק
        this.lastMessage = lastMessage;   // השמת תוכן ההודעה האחרונה
        this.lastUpdate = lastUpdate;     // השמת חותמת הזמן לעדכון האחרון
    }

    // --- פעולות גישה (Getters) לקבלת ערכי השדות מתוך האובייקט (שמירה על עקרון הכמוסה) ---

    // פונקציה לקבלת מזהה חדר הצ'אט
    public String getChatRoomId() { return chatRoomId; }

    // פונקציה לקבלת מזהה הלקוח
    public String getClientId() { return clientId; }

    // פונקציה לקבלת שם הלקוח
    public String getClientName() { return clientName; }

    // פונקציה לקבלת מזהה בית העסק
    public String getBusinessId() { return businessId; }

    // פונקציה לקבלת תוכן ההודעה האחרונה
    public String getLastMessage() { return lastMessage; }

    // פונקציה לקבלת חותמת הזמן של העדכון האחרון
    public Timestamp getLastUpdate() { return lastUpdate; }
}