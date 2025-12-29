package com.example.myapplication;

public class Appointment {
    private String appointmentId;
    private String businessId;
    private String clientId;
    private String clientName;
    private String date; // Format: "dd-MM-yyyy"
    private String time; // Format: "HH:mm"
    private String status; // "PENDING", "APPROVED", "REJECTED"

    public Appointment() {}

    public Appointment(String appointmentId, String businessId, String clientId, String clientName, String date, String time, String status) {
        this.appointmentId = appointmentId;
        this.businessId = businessId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    // Getters and Setters...
    public String getAppointmentId() { return appointmentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTime() { return time; }
    public String getDate() { return date; }
    // ... תוסיפי את שאר ה-Getters וה-Setters לפי הצורך
}