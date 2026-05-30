package com.example.myapplication;

import java.util.HashMap;
import java.util.Map;

// מחלקת מודל (Model Class) המייצגת את הגדרות מערכת הזמנת התורים ושעות הפעילות של בית העסק
public class BusinessScheduleSettings {

    // משתנה בוליאני הקובע האם אפשרות הזמנת התורים בעסק פעילה ופתוחה לקהל הלקוחות כרגע
    private boolean isBookingEnabled;
    // משתנה מספרי המגדיר את משך הזמן של תור בודד בדקות (לדוגמה: 30, 45, 60 דקות)
    private int slotDurationMinutes;
    // מפת נתונים (Map) המייצגת את ימי ושעות העבודה - מפתח: שם היום באנגלית, ערך: מחרוזת טווח השעות (למשל: "09:00-17:00")
    private Map<String, String> workDays;

    // פעולה בונה ריקה (Default Constructor) - דרישת חובה של Firestore לצורך המרת מסמכים אוטומטית לאובייקט Java
    public BusinessScheduleSettings() {
        // אתחול מפת ימי העבודה כאובייקט HashMap ריק למניעת שגיאות הצבעה לערך ריק (NullPointerException)
        this.workDays = new HashMap<>();
    }

    // פעולה בונה חלקית (Parameterized Constructor) המאפשרת לאתחל את הגדרות היסוד של יומן העסק בזיכרון
    public BusinessScheduleSettings(boolean isBookingEnabled, int slotDurationMinutes) {
        this.isBookingEnabled = isBookingEnabled;       // השמת מצב פעילות מערכת הזמנת התורים
        this.slotDurationMinutes = slotDurationMinutes; // השמת משך זמן התור בדקות
        this.workDays = new HashMap<>();                 // אתחול מפת ימי העבודה כ-HashMap ריק מוכן לשימוש
    }

    // --- פעולות גישה ועדכון (Getters & Setters) סטנדרטיות עבור שדות המחלקה ---

    // פונקציה לקבלת מצב זמינות הזמנת התורים (האם המערכת פתוחה ללקוחות)
    public boolean isBookingEnabled() {
        return isBookingEnabled;
    }

    // פונקציה לעדכון מצב זמינות הזמנת התורים
    public void setBookingEnabled(boolean bookingEnabled) {
        isBookingEnabled = bookingEnabled;
    }

    // פונקציה לקבלת משך זמן תור בודד בדקות
    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    // פונקציה לעדכון משך זמן תור בודד בדקות
    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    // פונקציה לקבלת מפת ימי ושעות העבודה של העסק
    public Map<String, String> getWorkDays() {
        return workDays;
    }

    // פונקציה לעדכון מפת ימי ושעות העבודה של העסק
    public void setWorkDays(Map<String, String> workDays) {
        this.workDays = workDays;
    }
}