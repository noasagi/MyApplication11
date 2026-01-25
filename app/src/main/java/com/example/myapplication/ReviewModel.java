package com.example.myapplication;

import com.google.firebase.Timestamp;

public class ReviewModel {
    private String reviewId;
    private String businessId;
    private String userId;
    private String userName;
    private String comment;
    private String appointmentId; // שדה חדש!

    // שלושת הדירוגים שלנו
    private float ratingProfessionalism;
    private float ratingReliability;
    private float ratingPrice;

    // מתי נכתבה הביקורת
    private Timestamp timestamp;

    public ReviewModel() {
        // בנאי ריק חובה לפיירבייס
    }

    public ReviewModel(String reviewId, String businessId, String userId, String userName, String comment, float ratingProfessionalism, float ratingReliability, float ratingPrice, Timestamp timestamp) {
        this.reviewId = reviewId;
        this.businessId = businessId;
        this.userId = userId;
        this.userName = userName;
        this.comment = comment;
        this.ratingProfessionalism = ratingProfessionalism;
        this.ratingReliability = ratingReliability;
        this.ratingPrice = ratingPrice;
        this.timestamp = timestamp;
    }

    // Getters and Setters
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

    // פונקציית עזר לחישוב ממוצע של הביקורת הספציפית הזו
    public float calculateAverage() {
        return (ratingProfessionalism + ratingReliability + ratingPrice) / 3.0f;
    }
}