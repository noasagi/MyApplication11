package com.example.myapplication;

import java.util.HashMap;
import java.util.Map;

public class BusinessScheduleSettings {

    private boolean isBookingEnabled;
    private int slotDurationMinutes;

    // מפת נתונים (Map) השומרת זוגות של מפתח-ערך (שם היום  טווח השעות, למשל: "Sunday" "09:00-17:00")
    private Map<String, String> workDays;

    /**
     * פעולה בונה ריקה (Default Constructor): דרישת חובה של Firestore לצורך המרה אוטומטית של מסמכים לאובייקט Java.
     * אתחול המפה כאן מונע באופן מוחלט שגיאות קריסה מסוג NullPointerException בעת ניסיון גישה ראשוני.
     */
    public BusinessScheduleSettings() {
        this.workDays = new HashMap<>();
    }

    /**
     * פעולה בונה חלקית (Parameterized Constructor): משמשת לאתחול מהיר של הגדרות היסוד של יומן העסק בזיכרון המכשיר.
     */
    public BusinessScheduleSettings(boolean isBookingEnabled, int slotDurationMinutes) {
        this.isBookingEnabled = isBookingEnabled;
        this.slotDurationMinutes = slotDurationMinutes;
        this.workDays = new HashMap<>();
    }

    // --- פעולות גישה ועדכון (Getters & Setters) סטנדרטיות ---

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