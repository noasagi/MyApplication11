package com.example.myapplication;

import com.google.firebase.firestore.Blob;
import java.util.List;

// מחלקת מודל מורחבת (Model Class) המייצגת אובייקט של בית עסק, כולל נתוני מיקום גאוגרפי ומדדי דירוג משוקללים
public class BusinessModel {

    // מזהה ייחודי עבור בית העסק (תואם ל-ID של המסמך באוסף בפיירסטור)
    private String businessId;
    // המזהה הייחודי (UID) של המשתמש שהוא בעל העסק ומנהל אותו
    private String ownerId;
    // שם בית העסק (למשל: "סטודיו ליופי", "מרכז קוסמטיקה")
    private String name;
    // פירוט, תיאור קצר או אודות על השירותים שבית העסק מציע ללקוחותיו
    private String description;
    // מספר טלפון ליצירת קשר עם בית העסק (משמש לחיוג או למשלוח הודעות SMS)
    private String phone;
    // סיווג, סוג או קטגוריית העסק (למשל: "קוסמטיקאית", "ספר", "טכנאי")
    private String businessType;
    // כתובת פיזית טקסטואלית של בית העסק (למשל: "הרצל 15, תל אביב")
    private String address;
    // רשימה דינמית של אובייקטי Blob המכילים את נתוני התמונות של העסק בצורה בינארית ישירות מהמסד
    private List<Blob> imageBlobs;
    // קואורדינטת קו רוחב גאוגרפי לצורך הצגת העסק על גבי מפה או חישוב מרחקים
    private Double latitude;
    // קואורדינטת קו אורך גאוגרפי לצורך הצגת העסק על גבי מפה או חישוב מרחקים
    private Double longitude;

    // --- שדות מספריים דינמיים המנהלים את ממוצעי הדירוגים של העסק ---
    // ציון ממוצע עבור קטגוריית "מקצועיות" (ערך התחלתי: 0)
    private float avgProfessionalism = 0f;
    // ציון ממוצע עבור קטגוריית "אמינות ועמידה בזמנים" (ערך התחלתי: 0)
    private float avgReliability = 0f;
    // ציון ממוצע עבור קטגוריית "מחיר ושביעות רצון" (ערך התחלתי: 0)
    private float avgPrice = 0f;
    // מספר המדרגים הכולל שכתבו ביקורת והעניקו דירוג לבית העסק (ערך התחלתי: 0)
    private int totalReviews = 0;

    // פעולה בונה ריקה (Default Constructor) - דרישת חובה של פרוטוקול פיירסטור לצורך המרת מסמכים אוטומטית לאובייקט ג'אווה
    public BusinessModel() {
        // דרוש ל-Firestore
    }

    // פעולה בונה מלאה (Parameterized Constructor) לאתחול אובייקט עסק חדש בזיכרון עם כלל נתוני התשתית שלו
    public BusinessModel(String businessId, String ownerId, String name,
                         String description, String phone, String businessType,
                         String address, List<Blob> imageBlobs, Double latitude, Double longitude) {
        this.businessId = businessId;         // השמת מזהה בית העסק
        this.ownerId = ownerId;               // השמת מזהה בעל העסק
        this.name = name;                     // השמת שם העסק
        this.description = description;       // השמת תיאור העסק
        this.phone = phone;                   // השמת מספר הטלפון
        this.businessType = businessType;     // השמת קטגוריית העסק
        this.address = address;               // השמת הכתובת הפיזית
        this.imageBlobs = imageBlobs;         // השמת רשימת תמונות ה-Blob
        this.latitude = latitude;             // השמת קו הרוחב הגאוגרפי
        this.longitude = longitude;           // השמת קו האורך הגאוגרפי
    }

    // --- פונקציה לחישוב הציון הכללי הממוצע של בית העסק ---
    // פונקציה המחזירה ערך מספרי מסוג נקודה צפה המייצג את שקלול הציון הכללי מתוך שלושת המדדים
    public float getOverallRating() {
        // תנאי הגנה: אם אף לקוח עדיין לא דירג את העסק, נחזיר מיד ציון 0 כדי למנוע חלוקה באפס
        if (totalReviews == 0) return 0f;
        // חישוב ממוצע חשבוני פשוט: חיבור שלושת מדדי הציונים וחלוקתם במספר המדדים (3)
        return (avgProfessionalism + avgReliability + avgPrice) / 3.0f;
    }

    // --- פעולות גישה ועדכון (Getters & Setters) סטנדרטיות עבור שדות המחלקה ---

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<Blob> getImageBlobs() { return imageBlobs; }
    public void setImageBlobs(List<Blob> imageBlobs) { this.imageBlobs = imageBlobs; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    // --- פעולות גישה ועדכון עבור שדות הביקורות והמדדים ---

    public float getAvgProfessionalism() { return avgProfessionalism; }
    public void setAvgProfessionalism(float avgProfessionalism) { this.avgProfessionalism = avgProfessionalism; }

    public float getAvgReliability() { return avgReliability; }
    public void setAvgReliability(float avgReliability) { this.avgReliability = avgReliability; }

    public float getAvgPrice() { return avgPrice; }
    public void setAvgPrice(float avgPrice) { this.avgPrice = avgPrice; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
}