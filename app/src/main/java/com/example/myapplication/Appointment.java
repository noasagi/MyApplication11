package com.example.myapplication;

// מחלקת מודל המייצגת תור במערכת ומיועדת לעבודה מול Firestore
public class Appointment {

    // משתני המחלקה לשמירת נתוני התור
    private String appointmentId; // מזהה התור בבסיס הנתונים
    private String businessName;  // שם בית העסק
    private String businessId;    // מזהה בית העסק
    private String userId;        // מזהה הלקוח
    private String userName;      // שם הלקוח
    private boolean isReviewed;   // האם בוצע דירוג לתור זה
    private String date;          // תאריך התור
    private String time;          // שעת התור
    private String status;        // סטטוס התור (למשל: PENDING, APPROVED)
    private long timestamp;       // חותם זמן במילישניות לצורך מיון
    private String description;   // תיאור השירות המבוקש
    private int duration;         // משך התור בדקות
    private double price;         // מחיר השירות

    // פעולה בונה ריקה - נדרשת עבור המרה אוטומטית של Firestore לאובייקט
    public Appointment() {}

    // פעולה בונה מלאה לאתחול אובייקט תור עם כל הנתונים
    public Appointment(String appointmentId, String businessId, String userId, String userName, String date, String time, String status, long timestamp, String description, int duration, double price) {
        this.appointmentId = appointmentId;
        this.businessId = businessId;
        this.userId = userId;
        this.userName = userName;
        this.date = date;
        this.time = time;
        this.status = status;
        this.timestamp = timestamp;
        this.description = description;
        this.duration = duration;
        this.price = price;
    }

    // --- פעולות גישה ועדכון (Getters & Setters) ---

    // קבלת מזהה התור ועדכונו
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    // קבלת שם העסק ועדכונו
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    // בדיקה ועדכון של מצב הביקורת/דירוג
    public boolean getIsReviewed() { return isReviewed; }
    public void setIsReviewed(boolean isReviewed) { this.isReviewed = isReviewed; }

    // קבלת מזהה העסק ועדכונו
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    // קבלת מזהה המשתמש ועדכונו
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // קבלת שם המשתמש ועדכונו
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    // קבלת תאריך התור ועדכונו
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    // קבלת שעת התור ועדכונו
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    // קבלת סטטוס התור ועדכונו
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // קבלת חותם הזמן ועדכונו
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // קבלת תיאור השירות ועדכונו
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // קבלת משך התור ועדכונו
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    // קבלת מחיר התור ועדכונו
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}