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

    // --- השדה החדש שהוספנו! ---
    private String address;

    private List<Blob> imageBlobs;
    private Double latitude;
    private Double longitude;

    public BusinessModel() {
        // דרוש ל-Firestore
    }

    // בנאי עם כל השדות כולל כתובת ומיקום
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

    // Getters & Setters לכתובת
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<Blob> getImageBlobs() { return imageBlobs; }
    public void setImageBlobs(List<Blob> imageBlobs) { this.imageBlobs = imageBlobs; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}