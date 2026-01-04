package com.example.myapplication;

import java.util.Date; // <--- חשוב מאוד: לוודא שהשורה הזו קיימת!

public class Appointment {
    private String appointmentId;
    private String businessId;
    private String userId;
    private String userName;
    private String date;
    private String time;
    private String status;

    // ✅ התיקון: שינוי מ-long ל-Date כדי להתאים ל-Firestore
    private Date timestamp;

    // בנאי ריק (חובה ל-Firebase)
    public Appointment() {}

    public Appointment(String appointmentId, String businessId, String userId, String userName, String date, String time, String status, Date timestamp) {
        this.appointmentId = appointmentId;
        this.businessId = businessId;
        this.userId = userId;
        this.userName = userName;
        this.date = date;
        this.time = time;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters & Setters
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // ✅ גם כאן שינינו ל-Date
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}