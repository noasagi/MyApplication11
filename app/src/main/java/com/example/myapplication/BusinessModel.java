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

    // ✅ כמה תמונות – רשימת Blob
    private List<Blob> imageBlobs;

    public BusinessModel() {
        // דרוש ל-Firestore
    }

    public BusinessModel(String businessId, String ownerId, String name,
                         String description, String phone, String businessType,
                         List<Blob> imageBlobs) {
        this.businessId = businessId;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.phone = phone;
        this.businessType = businessType;
        this.imageBlobs = imageBlobs;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public List<Blob> getImageBlobs() {
        return imageBlobs;
    }

    public void setImageBlobs(List<Blob> imageBlobs) {
        this.imageBlobs = imageBlobs;
    }
}
