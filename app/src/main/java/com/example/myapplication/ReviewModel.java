package com.example.myapplication;

import com.google.firebase.Timestamp;

// מחלקת מודל נתונים (Model / POJO) המייצגת ביקורת ודירוג במערכת, ומיועדת למיפוי אוטומטי מול מסמכי Cloud Firestore
public class ReviewModel {

    // הגדרת משתני מחלקה פרטיים (Private) כחלק מיישום עקרון הכמוסה (Encapsulation) להגנה על המידע
    private String reviewId;
    private String businessId;
    private String userId;
    private String userName;
    private String comment;
    private String appointmentId; // מזהה התור לקישור חד-חד-ערכי (מניעת כפל ביקורות לתור בודד)

    // שדות מספריים מסוג נקודה צפה (Float) המייצגים את שלושת מדדי הדירוג המפורטים באפליקציה
    private float ratingProfessionalism;
    private float ratingReliability;
    private float ratingPrice;

    // אובייקט זמן רשמי של פיירבייס המציין את רגע יצירת ושמירת המסמך בענן
    private Timestamp timestamp;

    /**
     * מה הפעולה עושה: פעולה בונה ריקה (Default Constructor).
     * למה היא חובה: דרישה טכנולוגית מוחלטת של ספריית Firestore. בזמן שליפת מסמך מהענן, ה-SDK של פיירבייס מייצר קודם כל מופע ריק של המחלקה בזיכרון, ורק אז מזריק אליו את הנתונים דרך ה-Setters או שיקוף קוד (Reflection). ללא פעולה זו, האפליקציה תקרוס בעת שליפת נתונים!
     */
    public ReviewModel() {
    }

    // פעולה בונה מלאה (Parameterized Constructor) המשמשת ליצירת אובייקט ביקורת שלם בזיכרון המערכת רגע לפני שליחתו לענן
    public ReviewModel(String reviewId, String businessId, String userId, String userName, String comment, String appointmentId, float ratingProfessionalism, float ratingReliability, float ratingPrice, Timestamp timestamp) {
        this.reviewId = reviewId;
        this.businessId = businessId;
        this.userId = userId;
        this.userName = userName;
        this.comment = comment;
        this.appointmentId = appointmentId;
        this.ratingProfessionalism = ratingProfessionalism;
        this.ratingReliability = ratingReliability;
        this.ratingPrice = ratingPrice;
        this.timestamp = timestamp;
    }

    // --- מערך פונקציות גישה ועדכון (Getters & Setters) המאפשרות גישה מבוקרת לשדות הפרטיים מחוץ למחלקה ---

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public float getRatingProfessionalism() { return ratingProfessionalism; }
    public void setRatingProfessionalism(float ratingProfessionalism) { this.ratingProfessionalism = ratingProfessionalism; }

    public float getRatingReliability() { return ratingReliability; }
    public void setRatingReliability(float ratingReliability) { this.ratingReliability = ratingReliability; }

    public float getRatingPrice() { return ratingPrice; }
    public void setRatingPrice(float ratingPrice) { this.ratingPrice = ratingPrice; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    /**
     * מה הפעולה עושה: פונקציית עזר חישובית (מתודה פנימית של האובייקט) המחשבת ומחזירה את ממוצע הדירוג המשוקלל של שלוש הקטגוריות.
     * קלט: אין.
     * פלט: float (ממוצע הציון של המשתמש, למשל 4.6).
     */
    public float calculateAverage() {
        // חיבור ערכי שלוש הקטגוריות וחלוקתן ב-3.0f (הסימון f מגדיר את המספר כ-Float למניעת חלוקת שלמים שגויה)
        return (ratingProfessionalism + ratingReliability + ratingPrice) / 3.0f;
    }
}