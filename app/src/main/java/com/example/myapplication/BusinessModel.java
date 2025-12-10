package com.example.myapplication;

import com.google.firebase.firestore.Blob;

// מודל לייצוג נתוני עסק מ-Firestore
public class BusinessModel {
    private String businessId;
    private String ownerId;
    private  String name;
    private String description;
    private String phone;
    private String businessType;
    private Blob imageBlob;

    // Constructor ריק נחוץ ל-Firestore
    public BusinessModel() {}

    // Getters and Setters (חובה כדי ש-Firestore ידע לקרוא ולכתוב את השדות)

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public Blob getImageBlob() { return imageBlob; }
    public void setImageBlob(Blob imageBlob) { this.imageBlob = imageBlob; }

    // הוסיפי את שאר ה-Getters וה-Setters עבור ownerId, description, ו-phone...
    // לדוגמה:
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}