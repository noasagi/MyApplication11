package com.example.myapplication;

// מחלקת מודל (Model Class) המייצגת אובייקט של תור במערכת ומיועדת למיפוי הנתונים מול Firestore
public class Appointment {

    // מזהה ייחודי עבור התור (תואם ל-ID של המסמך בבסיס הנתונים)
    private String appointmentId;
    // שם בית העסק שבו נקבע התור
    private String businessName;
    // המזהה הייחודי (UID) של בית העסק שאליו משויך התור
    private String businessId;
    // המזהה הייחודי (UID) של הלקוח שהזמין את התור
    private String userId;
    // השם המלא של הלקוח שהזמין את התור
    private String userName;
    // משתנה בוליאני המציין האם הלקוח כבר דירג וכתב ביקורת על תור זה
    private boolean isReviewed;
    // מחרוזת טקסט המייצגת את תאריך התור (למשל: "28/05/2026")
    private String date;
    // מחרוזת טקסט המייצגת את שעת התור (למשל: "14:30")
    private String time;
    // סטטוס התור הנוכחי במערכת (למשל: "PENDING", "APPROVED", "REJECTED")
    private String status;
    // חותם זמן מספרי (במילישניות) המייצג את מועד יצירת או קביעת התור
    private long timestamp;
    // פירוט או תיאור חופשי של סוג השירות המבוקש בתור
    private String description;
    // משך זמן התור המתוכנן בדקות (למשל: 30, 45, 60)
    private int duration;
    // שדה מספרי מסוג נקודה צפה המייצג את מחיר השירות עבור התור הנוכחי
    private double price;

    // פעולה בונה ריקה (Default Constructor) - חובה עבור המרת מסמכי Firestore לאובייקט Java אוטומטי
    public Appointment() {}

    // פעולה בונה מלאה (Parameterized Constructor) לאתחול אובייקט תור עם כלל הנתונים בבת אחת
    public Appointment(String appointmentId, String businessId, String userId, String userName, String date, String time, String status, long timestamp, String description, int duration, double price) {
        this.appointmentId = appointmentId; // השמת מזהה התור
        this.businessId = businessId;       // השמת מזהה העסק
        this.userId = userId;               // השמת מזהה המשתמש
        this.userName = userName;           // השמת שם המשתמש
        this.date = date;                   // השמת תאריך התור
        this.time = time;                   // השמת שעת התור
        this.status = status;               // השמת סטטוס התור
        this.timestamp = timestamp;         // השמת חותם הזמן
        this.description = description;     // השמת תיאור השירות
        this.duration = duration;           // השמת משך הזמן
        this.price = price;                 // השמת מחיר השירות
    }

    // --- פעולות גישה ועדכון (Getters & Setters) עבור שדות המחלקה ---

    // פונקציה לקבלת מזהה התור
    public String getAppointmentId() { return appointmentId; }
    // פונקציה לעדכון מזהה התור
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    // פונקציה לקבלת שם בית העסק
    public String getBusinessName() { return businessName; }
    // פונקציה לעדכון שם בית העסק
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    // פונקציה לקבלת מצב הדירוג של התור (האם נבדק/דורג)
    public boolean getIsReviewed() { return isReviewed; }
    // פונקציה לעדכון מצב הדירוג של התור
    public void setIsReviewed(boolean isReviewed) { this.isReviewed = isReviewed; }

    // פונקציה לקבלת מזהה בית העסק
    public String getBusinessId() { return businessId; }
    // פונקציה לעדכון מזהה בית העסק
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    // פונקציה לקבלת מזהה הלקוח
    public String getUserId() { return userId; }
    // פונקציה לעדכון מזהה הלקוח
    public void setUserId(String userId) { this.userId = userId; }

    // פונקציה לקבלת שם הלקוח
    public String getUserName() { return userName; }
    // פונקציה לעדכון שם הלקוח
    public void setUserName(String userName) { this.userName = userName; }

    // פונקציה לקבלת תאריך התור
    public String getDate() { return date; }
    // פונקציה לעדכון תאריך התור
    public void setDate(String date) { this.date = date; }

    // פונקציה לקבלת שעת התור
    public String getTime() { return time; }
    // פונקציה לעדכון שעת התור
    public void setTime(String time) { this.time = time; }

    // פונקציה לקבלת סטטוס התור
    public String getStatus() { return status; }
    // פונקציה לעדכון סטטוס התור
    public void setStatus(String status) { this.status = status; }

    // פונקציה לקבלת חותם הזמן של התור
    public long getTimestamp() { return timestamp; }
    // פונקציה לעדכון חותם הזמן של התור
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // פונקציה לקבלת תיאור השירות
    public String getDescription() { return description; }
    // פונקציה לעדכון תיאור השירות
    public void setDescription(String description) { this.description = description; }

    // פונקציה לקבלת משך זמן התור
    public int getDuration() { return duration; }
    // פונקציה לעדכון משך זמן התור
    public void setDuration(int duration) { this.duration = duration; }

    // פונקציה לקבלת מחיר התור
    public double getPrice() { return price; }
    // פונקציה לעדכון מחיר התור
    public void setPrice(double price) { this.price = price; }
}