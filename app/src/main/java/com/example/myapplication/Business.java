package com.example.myapplication;

// מחלקת מודל (Model Class) המייצגת אובייקט של בית עסק במערכת ומיועדת למיפוי אוטומטי מול Firestore
public class Business {

    // מזהה ייחודי עבור בית העסק (תואם ל-ID של המסמך באוסף העסקים בבסיס הנתונים)
    private String id;
    // המזהה הייחודי (UID) של המשתמש שהוא בעל העסק (היוצר/המנהל של העסק)
    private String ownerId;
    // שם בית העסק (למשל: "מספרת אקספרס", "קליניקת יופי")
    private String name;
    // תיאור קצר, פירוט או אודות על השירותים שבית העסק מציע
    private String description;
    // מספר טלפון ליצירת קשר עם בית העסק (משמש גם למנגנון שליחת הודעות ה-SMS בביטולים)
    private String phone;
    // כתובת אינטרנט (URL) המובילה לתמונה הראשית של בית העסק המאוחסנת ב-Storage
    private String mainImageUrl;

    // פעולה בונה ריקה (Default Constructor) - חובה על פי הפרוטוקול של פיירסטור לצורך המרת מסמכים אוטומטית
    public Business() {}

    // פעולה בונה מלאה (Parameterized Constructor) לאתחול אובייקט עסק חדש בזיכרון עם כלל נתוניו בבת אחת
    public Business(String id, String ownerId, String name, String description, String phone, String mainImageUrl) {
        this.id = id;                     // השמת מזהה בית העסק
        this.ownerId = ownerId;           // השמת מזהה בעל העסק
        this.name = name;                 // השמת שם העסק
        this.description = description;   // השמת תיאור העסק
        this.phone = phone;               // השמת מספר הטלפון
        this.mainImageUrl = mainImageUrl; // השמת קישור התמונה הראשית
    }

    // --- פעולות גישה (Getters) לקבלת ערכי השדות מתוך האובייקט ---

    // פונקציה לקבלת מזהה בית העסק
    public String getId() {
        return id;
    }

    // פונקציה לקבלת מזהה בעל העסק
    public String getOwnerId() {
        return ownerId;
    }

    // פונקציה לקבלת שם בית העסק
    public String getName() {
        return name;
    }

    // פונקציה לקבלת תיאור בית העסק
    public String getDescription() {
        return description;
    }

    // פונקציה לקבלת מספר הטלפון של העסק
    public String getPhone() {
        return phone;
    }

    // פונקציה לקבלת קישור התמונה הראשית של העסק
    public String getMainImageUrl() {
        return mainImageUrl;
    }
}