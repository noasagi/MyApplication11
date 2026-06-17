package com.example.myapplication;

import com.google.firebase.firestore.Blob;
import java.util.List;

public class BusinessModel {

    private String businessId;
    private String ownerId;
    private String name;
    private String description;
    private String phone;
    private String businessType;
    private String address;

    // שמירת התמונות כרשימה של אובייקטי Blob (מבנה בינארי של פיירבייס) מאפשרת אחסון ישיר של קובצי המדיה בתוך מסמך ה-Firestore
    private List<Blob> imageBlobs;

    // שימוש בטיפוס הכללי Double (ולא double פרימיטיבי) מאפשר לשדות המיקום להכיל ערך null במידה והעסק לא הגדיר מיקום
    private Double latitude;
    private Double longitude;

    // מדדי הדירוג השונים של בית העסק המשמשים להצגת כוכבי הביקורות במערכת
    private float avgProfessionalism = 0f;
    private float avgReliability = 0f;
    private float avgPrice = 0f;
    private int totalReviews = 0;

    /**
     * פעולה בונה ריקה (Default Constructor): דרישת חובה מוחלטת של ספריית Firestore.
     * בזמן שליפת נתונים, פיירבייס משתמש בה כדי ליצור אובייקט ריק ואז מזרק אליו את הערכים באמצעות ה-Setters.
     */
    public BusinessModel() {
        // דרוש ל-Firestore
    }

    /**
     * פעולה בונה מלאה (Parameterized Constructor): משמשת ליצירת מופע חדש של עסק בזיכרון המכשיר לפני שמירתו הראשונית במסד.
     */
    public BusinessModel(String businessId, String ownerId, String name,
                         String description, String phone, String businessType,
                         String address, List<Blob> imageBlobs, Double latitude, Double longitude) {
        this.businessId = businessId;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.phone = phone;
        this.businessType = businessType;
        this.address = address;
        this.imageBlobs = imageBlobs;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * מה הפעולה עושה: מחשבת ומשקללת את הציון הממוצע הכולל של העסק מתוך שלושת המדדים הקיימים.
     * קלט: אין.
     * פלט: float (ממוצע הציון המשוקלל).
     */
    public float getOverallRating() {
        // מנגנון הגנה: אם אין עדיין ביקורות, נחזיר 0 כדי למנוע חישוב שגוי או חלוקה באפס
        if (totalReviews == 0) return 0f;
        return (avgProfessionalism + avgReliability + avgPrice) / 3.0f;
    }

    // --- פעולות גישה ועדכון (Getters & Setters) סטנדרטיות ---

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

    public float getAvgProfessionalism() { return avgProfessionalism; }
    public void setAvgProfessionalism(float avgProfessionalism) { this.avgProfessionalism = avgProfessionalism; }

    public float getAvgReliability() { return avgReliability; }
    public void setAvgReliability(float avgReliability) { this.avgReliability = avgReliability; }

    public float getAvgPrice() { return avgPrice; }
    public void setAvgPrice(float avgPrice) { this.avgPrice = avgPrice; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
}