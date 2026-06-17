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

public class BookingHelper {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * מה הפעולה עושה: מחשבת ומחזירה את כל חלונות הזמן הפנויים לקביעת תור ביום מסוים,
     * על ידי הצלבת שעות הפעילות של העסק עם התורים הקיימים והחסימות ביומן.
     * קלט: string תאריך, אובייקט הגדרות עסק, רשימת תורים קיימים, רשימת חסימות (הפסקות).
     * פלט: List<String> - רשימה של שעות פנויות בפורמט "HH:mm".
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static List<String> calculateAvailableSlots(
            String dateString,
            BusinessScheduleSettings settings,
            List<Appointment> existingAppointments,
            List<BlockedSlot> blockedSlots
    ) {
        List<String> availableSlots = new ArrayList<>();

        // תנאי הגנה למקרה שלא הוגדרו שעות פעילות לעסק
        if (settings == null || settings.getWorkDays() == null) {
            return availableSlots;
        }

        try {
            // 1. זיהוי היום בשבוע מתוך התאריך שהתקבל
            LocalDate date = LocalDate.parse(dateString, DATE_FORMATTER);
            String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            // 2. בדיקה האם העסק פתוח ביום זה בשבוע
            Map<String, String> workDays = settings.getWorkDays();
            if (!workDays.containsKey(dayOfWeek)) {
                return availableSlots;
            }

            // 3. חילוץ ופירוק שעות הפתיחה והסגירה של העסק
            String hoursRange = workDays.get(dayOfWeek);
            if (hoursRange == null || !hoursRange.contains("-")) return availableSlots;

            String[] parts = hoursRange.split("-");
            LocalTime openTime = LocalTime.parse(parts[0].trim(), TIME_FORMATTER);
            LocalTime closeTime = LocalTime.parse(parts[1].trim(), TIME_FORMATTER);
            int slotDuration = settings.getSlotDurationMinutes();

            // 4. לולאה הרצה משעת הפתיחה עד שעת הסגירה ובודקת זמינות לכל חלון זמן
            LocalTime currentSlotStart = openTime;

            while (currentSlotStart.plusMinutes(slotDuration).isBefore(closeTime) ||
                    currentSlotStart.plusMinutes(slotDuration).equals(closeTime)) {

                LocalTime currentSlotEnd = currentSlotStart.plusMinutes(slotDuration);
                String slotTimeStr = currentSlotStart.format(TIME_FORMATTER);

                boolean isBooked = false;
                boolean isBlocked = false;

                // סינון חלון הזמן מול תורים שכבר נקבעו ותפוסים
                if (existingAppointments != null) {
                    for (Appointment app : existingAppointments) {
                        if (app.getTime().equals(slotTimeStr) && !"REJECTED".equals(app.getStatus())) {
                            isBooked = true;
                            break;
                        }
                    }
                }

                // סינון חלון הזמן מול חסימות יומן של בעל העסק (הפסקות)
                if (!isBooked && blockedSlots != null) {
                    for (BlockedSlot block : blockedSlots) {
                        LocalTime blockStart = LocalTime.parse(block.getStartTime(), TIME_FORMATTER);
                        LocalTime blockEnd = LocalTime.parse(block.getEndTime(), TIME_FORMATTER);

                        // נוסחה מתמטית לבדיקת התנגשות וחלוקת זמנים
                        if (currentSlotStart.isBefore(blockEnd) && currentSlotEnd.isAfter(blockStart)) {
                            isBlocked = true;
                            break;
                        }
                    }
                }

                // אם החלון נמצא פנוי לחלוטין, הוא מתווסף לרשימה הסופית
                if (!isBooked && !isBlocked) {
                    availableSlots.add(slotTimeStr);
                }

                // קידום השעה לחלון הבא לפי אורך הטיפול
                currentSlotStart = currentSlotStart.plusMinutes(slotDuration);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return availableSlots;
    }
}