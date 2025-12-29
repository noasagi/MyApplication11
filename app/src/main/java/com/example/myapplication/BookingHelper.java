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
     * פונקציה שמקבלת את כל הנתונים ומחזירה רשימת שעות פנויות (Strings)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static List<String> calculateAvailableSlots(
            String dateString,
            BusinessScheduleSettings settings,
            List<Appointment> existingAppointments,
            List<BlockedSlot> blockedSlots
    ) {
        List<String> availableSlots = new ArrayList<>();

        if (settings == null || settings.getWorkDays() == null) {
            return availableSlots;
        }

        try {
            // 1. זיהוי היום בשבוע
            LocalDate date = LocalDate.parse(dateString, DATE_FORMATTER);
            String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            // 2. בדיקה אם העסק עובד ביום הזה
            Map<String, String> workDays = settings.getWorkDays();
            if (!workDays.containsKey(dayOfWeek)) {
                return availableSlots; // העסק סגור
            }

            // 3. חילוץ שעות פתיחה וסגירה
            String hoursRange = workDays.get(dayOfWeek);
            if (hoursRange == null || !hoursRange.contains("-")) return availableSlots;

            String[] parts = hoursRange.split("-");
            LocalTime openTime = LocalTime.parse(parts[0].trim(), TIME_FORMATTER);
            LocalTime closeTime = LocalTime.parse(parts[1].trim(), TIME_FORMATTER);
            int slotDuration = settings.getSlotDurationMinutes();

            // 4. לולאה על כל הסלוטים האפשריים
            LocalTime currentSlotStart = openTime;

            while (currentSlotStart.plusMinutes(slotDuration).isBefore(closeTime) ||
                    currentSlotStart.plusMinutes(slotDuration).equals(closeTime)) {

                LocalTime currentSlotEnd = currentSlotStart.plusMinutes(slotDuration);
                String slotTimeStr = currentSlotStart.format(TIME_FORMATTER);

                boolean isBooked = false;
                boolean isBlocked = false;

                // בדיקת תורים קיימים
                if (existingAppointments != null) {
                    for (Appointment app : existingAppointments) {
                        if (app.getTime().equals(slotTimeStr) && ! "REJECTED".equals(app.getStatus())) {
                            isBooked = true;
                            break;
                        }
                    }
                }

                // בדיקת חסימות (Blocked Slots)
                if (!isBooked && blockedSlots != null) {
                    for (BlockedSlot block : blockedSlots) {
                        LocalTime blockStart = LocalTime.parse(block.getStartTime(), TIME_FORMATTER);
                        LocalTime blockEnd = LocalTime.parse(block.getEndTime(), TIME_FORMATTER);

                        // אם הסלוט חופף לחסימה
                        if (currentSlotStart.isBefore(blockEnd) && currentSlotEnd.isAfter(blockStart)) {
                            isBlocked = true;
                            break;
                        }
                    }
                }

                if (!isBooked && !isBlocked) {
                    availableSlots.add(slotTimeStr);
                }

                currentSlotStart = currentSlotStart.plusMinutes(slotDuration);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return availableSlots;
    }
}