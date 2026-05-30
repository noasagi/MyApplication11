package com.example.myapplication;

import com.google.firebase.Timestamp;

// מחלקת מודל (Model Class) המייצגת ישות של חדר צ'אט במערכת ומיועדת למיפוי אוטומטי מול פיירסטור
public class ChatRoom {

    // מזהה ייחודי עבור חדר הצ'אט (נוצר לרוב משילוב של מזהה הלקוח ומזהה בית העסק למניעת כפילויות)
    private String id;
    // המזהה הייחודי (UID) של הלקוח המשתתף בשיחה זו
    private String clientId;
    // המזהה הייחודי (ID) של בית העסק המשתתף בשיחה זו
    private String businessId;
    // מחרוזת טקסט המכילה את תוכן ההודעה האחרונה שנשלחה (משמשת להצגת תצוגה מקדימה במסך הרשימה)
    private String lastMessage;
    // אובייקט זמן מובנה של פיירבייס (Timestamp) השומר את נקודת הזמן המדויקת בה נשלחה ההודעה האחרונה
    private Timestamp lastUpdated;

    // פעולה בונה ריקה (Default Constructor) - דרישת חובה של פרוטוקול פיירסטור לצורך המרת מסמכים אוטומטית לאובייקט ג'אווה
    public ChatRoom() {
    }

    // פעולה בונה מלאה (Parameterized Constructor) לאתחול אובייקט חדר צ'אט חדש בזיכרון עם כלל נתוני התשתית שלו
    public ChatRoom(String id, String clientId, String businessId, String lastMessage, Timestamp lastUpdated) {
        this.id = id;                     // השמת מזהה חדר הצ'אט
        this.clientId = clientId;         // השמת מזהה הלקוח
        this.businessId = businessId;     // השמת מזהה בית העסק
        this.lastMessage = lastMessage;   // השמת תוכן ההודעה האחרונה
        this.lastUpdated = lastUpdated;   // השמת חותמת הזמן של העדכון האחרון
    }

    // --- פעולות גישה ועדכון (Getters & Setters) סטנדרטיות עבור שדות המחלקה ---

    // פונקציה לקבלת מזהה חדר הצ'אט
    public String getId() {
        return id;
    }

    // פונקציה לעדכון מזהה חדר הצ'אט
    public void setId(String id) {
        this.id = id;
    }

    // פונקציה לקבלת מזהה הלקוח בשיחה
    public String getClientId() {
        return clientId;
    }

    // פונקציה לעדכון מזהה הלקוח בשיחה
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    // פונקציה לקבלת מזהה בית העסק בשיחה
    public String getBusinessId() {
        return businessId;
    }

    // פונקציה לעדכון מזהה בית העסק בשיחה
    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    // פונקציה לקבלת תוכן ההודעה האחרונה בשיחה
    public String getLastMessage() {
        return lastMessage;
    }

    // פונקציה לעדכון תוכן ההודעה האחרונה בשיחה
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    // פונקציה לקבלת חותמת הזמן של ההודעה האחרונה
    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    // פונקציה לעדכון חותמת הזמן של ההודעה האחרונה
    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}