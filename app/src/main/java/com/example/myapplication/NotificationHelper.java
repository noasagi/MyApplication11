package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// מחלקת שירות ועזר (Utility Class) המיועדת לתזמון התראות דחיפה מקומיות במכשיר עבור תורים שנקבעו
public class NotificationHelper {

    /**
     * מה הפעולה עושה: מחשבת באופן דינמי את זמני ההתראות (יום לפני ושעה לפני התור) ורושמת אותם במערכת השעונים של אנדרואיד באמצעות AlarmManager.
     * קלט: Context context, String appointmentId, String date, String time, String businessName.
     * פלט: אין (void).
     */
    public static void scheduleAppointmentNotifications(Context context, String appointmentId, String date, String time, String businessName) {
        // הגדרת פורמט קריאת התאריך והשעה בהתאם לשעון ולשפת המכשיר (Locale.getDefault)
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());

        try {
            // ניסיון לפענח ולמזג את מחרוזות הטקסט של התאריך והשעה לכדי אובייקט Date יחיד של Java
            Date appointmentDate = sdf.parse(date + " " + time);
            if (appointmentDate == null) return; // תנאי הגנה מפני שגיאות פענוח

            // המרת מועד התור המדויק לערך מספרי במילישניות (Timestamp מאז 1970) לצורך חישובים מתמטיים
            long appointmentMillis = appointmentDate.getTime();
            // חישוב נקודת הזמן של יום לפני: הפחתת 24 שעות (מבוטאות במילישניות) מזמן התור
            long oneDayBefore = appointmentMillis - (24 * 60 * 60 * 1000L);
            // חישוב נקודת הזמן של שעה לפני: הפחתת שעה אחת (מבוטאת במילישניות) מזמן התור
            long oneHourBefore = appointmentMillis - (60 * 60 * 1000L);

            // שליפת שירות ה-AlarmManager של מערכת ההפעלה האחראי על תזמון משימות ברקע
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // ==========================================
            // --- תזמון התראה ראשונה: יום אחד לפני התור ---
            // ==========================================
            // תנאי לוגי: מוודאים שחלון הזמן של "יום לפני" עדיין נמצא בעתיד ביחס לזמן הנוכחי במכשיר
            if (System.currentTimeMillis() < oneDayBefore) {
                // יצירת כוונת (Intent) מפורשת המכוונת אל מחלקת ה-NotificationReceiver שתקלוט את השידור
                Intent intent1Day = new Intent(context, NotificationReceiver.class);
                intent1Day.putExtra("title", "תזכורת: תור מחר!");
                intent1Day.putExtra("message", "מחר יש לך תור אצל " + businessName + " בשעה " + time);
                // הפקת מזהה מספרי ייחודי להתראה באמצעות קוד גיבוב (hashCode) של מחרוזת ה-ID
                intent1Day.putExtra("notificationId", (appointmentId + "day").hashCode());

                // עטיפת ה-Intent ב-PendingIntent המאפשר למערכת להריץ את הקוד בשמנו גם כשהאפליקציה סגורה
                // FLAG_IMMUTABLE: דרישת אבטחה חובה באנדרואיד חדיש המונעת שינוי של האינטנט על ידי גורם חיצוני
                // FLAG_UPDATE_CURRENT: מעדכן את נתוני ההתראה הקיימת במידה ונוצר תזמון מחדש, במקום ליצור כפל התראות
                PendingIntent pendingIntent1Day = PendingIntent.getBroadcast(
                        context, (appointmentId + "day").hashCode(), intent1Day,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                // התאמת התזמון לגרסאות אנדרואיד השונות (תמיכה ב-Doze Mode החל מגרסה 6.0 ומעלה)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // setExactAndAllowWhileIdle: מבטיח יקיצה וביצוע מדויק של השעון גם במצב חיסכון סוללה עמוק
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, oneDayBefore, pendingIntent1Day);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, oneDayBefore, pendingIntent1Day);
                }
            }

            // ==========================================
            // --- תזמון התראה שנייה: שעה אחת לפני התור ---
            // ==========================================
            // תנאי לוגי: מוודאים שנקודת הזמן של "שעה לפני" עדיין רלוונטית ונמצאת בעתיד
            if (System.currentTimeMillis() < oneHourBefore) {
                // יצירת אינטנט ועטיפת PendingIntent ייחודית ונפרדת עבור התראת הטווח הקצר
                Intent intent1Hour = new Intent(context, NotificationReceiver.class);
                intent1Hour.putExtra("title", "תזכורת: התור שלך בעוד שעה!");
                intent1Hour.putExtra("message", "התור אצל " + businessName + " מתחיל בשעה " + time);
                intent1Hour.putExtra("notificationId", (appointmentId + "hour").hashCode());

                // שימוש בקוד בקשה (Request Code) שונה כדי שמערכת ההפעלה לא תדרוס את ההתראה של היום שלפני
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
            // תפיסת שגיאות פירסור והדפסת עקבות השגיאה בלוגים לצורך ניפוי (Debugging)
            e.printStackTrace();
        }
    }
}