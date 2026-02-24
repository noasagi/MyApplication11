package com.example.myapplication;

public class Treatment {
    private String treatmentId;
    private String name;        // שם הטיפול (לדוגמה: תספורת גברים)
    private double price;       // מחיר הטיפול
    private int durationMinutes; // משך הטיפול בדקות (לדוגמה: 30)

    // בנאי ריק - חובה בשביל Firebase Firestore
    public Treatment() {}

    public Treatment(String treatmentId, String name, double price, int durationMinutes) {
        this.treatmentId = treatmentId;
        this.name = name;
        this.price = price;
        this.durationMinutes = durationMinutes;
    }

    // Getters
    public String getTreatmentId() { return treatmentId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getDurationMinutes() { return durationMinutes; }

    // Setters
    public void setTreatmentId(String treatmentId) { this.treatmentId = treatmentId; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}