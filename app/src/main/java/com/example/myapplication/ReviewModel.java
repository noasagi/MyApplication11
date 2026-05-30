package com.example.myapplication;

import com.google.firebase.Timestamp;

// מחלקת מודל נתונים (Model / POJO Class) המייצגת ביקורת ודירוג במערכת ומיועדת למיפוי אוטומטי מול מסמכי פיירסטור
public class ReviewModel {
    // מזהה ייחודי של מסמך הביקורת במסד הנתונים בענן
    private String reviewId;
    // מזהה ייחודי של בית העסק שעבורו נכתבה חוות הדעת הזו
    private String businessId;
    // מזהה ייחודי של הלקוח (המשתמש) שביצע את הדירוג וכתב את הביקורת
    private String userId;
    // שמו העדכני של הלקוח כפי שנשלף מאוסף המשתמשים בעת יצירת הביקורת
    private String userName;
    // תוכן חוות הדעת המילולית החופשית שהקליד הלקוח במערכת
    private String comment;
    // מזהה ייחודי של התור שבוצע בפועל (משמש לקישור חד-חד-ערכי בין התור לביקורת)
    private String appointmentId;

    // שדות מספריים מסוג נקודה צפה (Float) לאחסון שלושת מדדי הדירוג המפורטים (בין 1 ל-5 כוכבים)
    private float ratingProfessionalism; // מדד רמת המקצועיות של השירות
    private float ratingReliability;     // מדד רמת האמינות ועמידה בזמנים
    private float ratingPrice;           // מדד רמת הוגנות המחיר

    // אובייקט זמן רשמי של פיירבייס המציין את רגע שליחת ושמירת הביקורת בשרת הענן
    private Timestamp timestamp;

    // פעולה בונה ריקה (Default Constructor) - דרישת חובה טכנולוגית של Cloud Firestore לצורך המרה אוטומטית של מסמכים לאובייקט ג'אווה
    public ReviewModel() {
    }

    // פעולה בונה מלאה (Parameterized Constructor) לאתחול והקמת אובייקט ביקורת שלם בזיכרון המערכת
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

    // --- מערך פונקציות גישה ועדכון (Getters & Setters) סטנדרטיות לשמירה על עקרון הכמוסה ---

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

    // פונקציית עזר חישובית (מתודה פנימית) המחזירה את ממוצע הדירוג המשוקלל של שלושת המדדים עבור ביקורת ספציפית זו
    public float calculateAverage() {
        // חיבור ערכי שלוש הקטגוריות וחלוקתן במספר המדדים (3.0) לקבלת ממוצע מדויק מסוג float
        return (ratingProfessionalism + ratingReliability + ratingPrice) / 3.0f;
    }
}