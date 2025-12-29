package com.example.myapplication;

import java.util.HashMap;
import java.util.Map;

public class BusinessScheduleSettings {
    private boolean isBookingEnabled;
    private int slotDurationMinutes; // לדוגמה: 30, 45, 60
    // מפתח: יום בשבוע (Sunday, Monday...), ערך: "09:00-17:00"
    private Map<String, String> workDays;

    public BusinessScheduleSettings() {
        // בנאי ריק חובה לפיירבייס
        this.workDays = new HashMap<>();
    }

    public BusinessScheduleSettings(boolean isBookingEnabled, int slotDurationMinutes) {
        this.isBookingEnabled = isBookingEnabled;
        this.slotDurationMinutes = slotDurationMinutes;
        this.workDays = new HashMap<>();
    }

    public boolean isBookingEnabled() {
        return isBookingEnabled;
    }

    public void setBookingEnabled(boolean bookingEnabled) {
        isBookingEnabled = bookingEnabled;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public Map<String, String> getWorkDays() {
        return workDays;
    }

    public void setWorkDays(Map<String, String> workDays) {
        this.workDays = workDays;
    }
}