package com.example.myapplication;

import android.os.Build;
import androidx.annotation.RequiresApi;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// מחלקת עזר ושירות (Helper Class) המכילה את האלגוריתם המרכזי לחישוב והצגת שעות פנויות לתורים
public class BookingHelper {

    // הגדרת פורמטר קבוע לעבודה עם שעות במבנה של שעה:דקות (למשל "14:30")
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    // הגדרת פורמטר קבוע לעבודה עם תאריכים במבנה של יום-חודש-שנה (למשל "28-05-2026")
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * פונקציה סטטית המקבלת תאריך, הגדרות עסק, תורים קיימים וחסימות, ומחשבת את חלונות הזמן הפנויים
     */
    @RequiresApi(api = Build.VERSION_CODES.O) // דרישת חובה של אנדרואיד לשימוש ב-Java Time API (LocalDate/LocalTime)
    public static List<String> calculateAvailableSlots(
            String dateString,                           // מחרוזת התאריך המבוקש לבדיקה
            BusinessScheduleSettings settings,           // אובייקט הגדרות שעות הפעילות של העסק
            List<Appointment> existingAppointments,      // רשימת התורים שכבר תפוסים ומוזמנים לאותו יום
            List<BlockedSlot> blockedSlots               // רשימת חלונות הזמן שבעל העסק חסם (כגון הפסקות)
    ) {
        // יצירת רשימה דינמית חדשה מסוג ArrayList שתחזיק את מחרוזות השעות הפנויות שנמצאו
        List<String> availableSlots = new ArrayList<>();

        // תנאי הגנה ראשוני: במידה ואין הגדרות עסק או שאין ימי עבודה מוגדרים, נחזיר מיד רשימה ריקה
        if (settings == null || settings.getWorkDays() == null) {
            return availableSlots;
        }

        try {
            // 1. זיהוי היום בשבוע
            // פיענוח מחרוזת הטקסט של התאריך והמרתה לאובייקט LocalDate רשמי
            LocalDate date = LocalDate.parse(dateString, DATE_FORMATTER);
            // חילוץ שם היום בשבוע באנגלית מלאה מתוך התאריך (למשל: "Monday", "Thursday")
            String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            // 2. בדיקה אם העסק עובד ביום הזה
            // שליפת מפת ימי העבודה מתוך הגדרות העסק (מפתח: שם היום, ערך: טווח השעות)
            Map<String, String> workDays = settings.getWorkDays();
            // תנאי: אם שם היום הנוכחי לא קיים במפת ימי העבודה, משמע שהעסק סגור ביום זה ונחזיר רשימה ריקה
            if (!workDays.containsKey(dayOfWeek)) {
                return availableSlots;
            }

            // 3. חילוץ שעות פתיחה וסגירה
            // שליפת מחרוזת טווח השעות של אותו יום (למשל: "09:00 - 17:00")
            String hoursRange = workDays.get(dayOfWeek);
            // תנאי הגנה: מוודא שהמחרוזת תקינה ומכילה את התו מקף המפריד בין שעת ההתחלה לשעת הסיום
            if (hoursRange == null || !hoursRange.contains("-")) return availableSlots;

            // פיצול המחרוזת לשני חלקים על בסיס התו מקף
            String[] parts = hoursRange.split("-");
            // פיענוח החלק הראשון והמרתו לאובייקט זמן של שעת פתיחת העסק (ניקוי רווחים בקצוות)
            LocalTime openTime = LocalTime.parse(parts[0].trim(), TIME_FORMATTER);
            // פיענוח החלק השני והמרתו לאובייקט זמן של שעת סגירת העסק (ניקוי רווחים בקצוות)
            LocalTime closeTime = LocalTime.parse(parts[1].trim(), TIME_FORMATTER);
            // שליפת משך זמן מוגדר עבור תור בודד בדקות מתוך הגדרות העסק (למשל: 30 דקות)
            int slotDuration = settings.getSlotDurationMinutes();

            // 4. לולאה הסורקת את כל חלונות הזמן הפוטנציאליים לאורך יום העבודה
            // אתחול משתנה הרצה שמחזיק את שעת תחילת הסלוט הנוכחי, שמתחיל משעת פתיחת העסק
            LocalTime currentSlotStart = openTime;

            // תנאי הלולאה: כל עוד שעת תחילת הסלוט בתוספת משך התור קטנה או שווה לשעת סגירת העסק
            while (currentSlotStart.plusMinutes(slotDuration).isBefore(closeTime) ||
                    currentSlotStart.plusMinutes(slotDuration).equals(closeTime)) {

                // חישוב וקביעת שעת סיום חלון הזמן הנוכחי (שעת התחלה + משך זמן התור)
                LocalTime currentSlotEnd = currentSlotStart.plusMinutes(slotDuration);
                // המרת שעת תחילת החלון למחרוזת טקסט מפורמטת (למשל "10:30") לצורך השוואות והצגה
                String slotTimeStr = currentSlotStart.format(TIME_FORMATTER);

                // אתחול משתנים בוליאניים כדגלים לבדיקת זמינות חלון הזמן הספציפי
                boolean isBooked = false;  // מסמן האם החלון כבר תפוס על ידי תור קיים
                boolean isBlocked = false; // מסמן האם החלון נמצא בתוך טווח שעות חסום

                // --- בדיקת תורים קיימים (Existing Appointments) ---
                // תנאי: מוודא שרשימת התורים הקיימים שהתקבלה מהשרת אינה ריקה
                if (existingAppointments != null) {
                    // לולאה הסורקת את כל התורים שכבר נקבעו לאותו יום
                    for (Appointment app : existingAppointments) {
                        // תנאי: אם שעת התור הקבוע שווה לשעת תחילת הסלוט, והתור לא בסטטוס "נדחה"
                        if (app.getTime().equals(slotTimeStr) && ! "REJECTED".equals(app.getStatus())) {
                            isBooked = true; // סימון חלון הזמן כתפוס
                            break;           // יציאה מיידית מהלולאה הנוכחית למניעת ריצות מיותרות
                        }
                    }
                }

                // --- בדיקת חסימות יומן (Blocked Slots) ---
                // תנאי: נבצע את בדיקת החסימות רק אם החלון לא נמצא כבר כתפוס על ידי תור מוקדם יותר
                if (!isBooked && blockedSlots != null) {
                    // לולאה הסורקת את כל חלונות הזמן החסומים של בעל העסק לאותו יום
                    for (BlockedSlot block : blockedSlots) {
                        // פיענוח והמרת שעת תחילת החסימה לאובייקט LocalTime
                        LocalTime blockStart = LocalTime.parse(block.getStartTime(), TIME_FORMATTER);
                        // פיענוח והמרת שעת סיום החסימה לאובייקט LocalTime
                        LocalTime blockEnd = LocalTime.parse(block.getEndTime(), TIME_FORMATTER);

                        // תנאי מתמטי לבדיקת חפיפה בין טווחים: אם תחילת הסלוט לפני סיום החסימה וגם סיום הסלוט אחרי תחילת החסימה
                        if (currentSlotStart.isBefore(blockEnd) && currentSlotEnd.isAfter(blockStart)) {
                            isBlocked = true; // סימון חלון הזמן כחסום
                            break;            // יציאה מיידית מהלולאה הנוכחית
                        }
                    }
                }

                // --- החלטה סופית והוספת הסלוט הפנוי ---
                // תנאי: אם חלון הזמן אינו תפוס על ידי תור ואינו נמצא בתוך חלון חסום
                if (!isBooked && !isBlocked) {
                    // הוספת מחרוזת השעה המוכחת כפנויה אל תוך רשימת הסלוטים הזמינים
                    availableSlots.add(slotTimeStr);
                }

                // קידום המשתנה והזזת שעת תחילת הסלוט הבא קדימה לפי משך הדקות של התור
                currentSlotStart = currentSlotStart.plusMinutes(slotDuration);
            }

        } catch (Exception e) {
            // הדפסת עקבות השגיאה במידה וארעה חריגה בזמן הפיענוח או חישוב הזמנים
            e.printStackTrace();
        }

        // החזרת הרשימה הסופית המכילה את כל השעות הפנויות והזמינות להזמנה
        return availableSlots;
    }
}