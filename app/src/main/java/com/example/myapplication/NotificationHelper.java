package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationHelper {

    public static void scheduleAppointmentNotifications(Context context, String appointmentId, String date, String time, String businessName) {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());
        try {
            Date appointmentDate = sdf.parse(date + " " + time);
            if (appointmentDate == null) return;

            long appointmentMillis = appointmentDate.getTime();
            long oneDayBefore = appointmentMillis - (24 * 60 * 60 * 1000L); // פחות 24 שעות
            long oneHourBefore = appointmentMillis - (60 * 60 * 1000L);      // פחות שעה

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // --- התראה ליום לפני ---
            if (System.currentTimeMillis() < oneDayBefore) {
                Intent intent1Day = new Intent(context, NotificationReceiver.class);
                intent1Day.putExtra("title", "תזכורת: תור מחר!");
                intent1Day.putExtra("message", "מחר יש לך תור אצל " + businessName + " בשעה " + time);
                intent1Day.putExtra("notificationId", (appointmentId + "day").hashCode());

                PendingIntent pendingIntent1Day = PendingIntent.getBroadcast(
                        context, (appointmentId + "day").hashCode(), intent1Day,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, oneDayBefore, pendingIntent1Day);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, oneDayBefore, pendingIntent1Day);
                }
            }

            // --- התראה לשעה לפני ---
            if (System.currentTimeMillis() < oneHourBefore) {
                Intent intent1Hour = new Intent(context, NotificationReceiver.class);
                intent1Hour.putExtra("title", "תזכורת: התור שלך בעוד שעה!");
                intent1Hour.putExtra("message", "התור אצל " + businessName + " מתחיל בשעה " + time);
                intent1Hour.putExtra("notificationId", (appointmentId + "hour").hashCode());

                PendingIntent pendingIntent1Hour = PendingIntent.getBroadcast(
                        context, (appointmentId + "hour").hashCode(), intent1Hour,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, oneHourBefore, pendingIntent1Hour);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, oneHourBefore, pendingIntent1Hour);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}