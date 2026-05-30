package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// מחלקת שירות ועזר (Utility/Helper Class) המיועדת לתזמון התראות דחיפה מקומיות במכשיר עבור תורים שנקבעו
public class NotificationHelper {

    // פונקציה סטטית המקבלת את פרטי התור ומחשבת באופן דינמי את זמני ההתראות (יום לפני ושעה לפני)
    public static void scheduleAppointmentNotifications(Context context, String appointmentId, String date, String time, String businessName) {
        // הגדרת פורמט קריאת התאריך והשעה (לדוגמה: 28/5/2026 15:30) בהתאם לשעון המקומי
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());
        try {
            // ניסיון לפענח ולמזג את מחרוזות התאריך והשעה לכדי אובייקט Date יחיד של Java
            Date appointmentDate = sdf.parse(date + " " + time);
            // תנאי הגנה: במידה ופענוח התאריך נכשל, נעצור את הפונקציה למניעת קריסה
            if (appointmentDate == null) return;

            // המרת מועד התור המדויק לערך מספרי במילישניות (מייצג את הזמן שחלף מאז שנת 1970)
            long appointmentMillis = appointmentDate.getTime();
            // חישוב נקודת הזמן של יום לפני התור: הפחתת 24 שעות (מבוטאות במילישניות) מזמן התור
            long oneDayBefore = appointmentMillis - (24 * 60 * 60 * 1000L);
            // חישוב נקודת הזמן של שעה לפני התור: הפחתת שעה אחת (מבוטאת במילישניות) מזמן התור
            long oneHourBefore = appointmentMillis - (60 * 60 * 1000L);

            // שליפת שירות ה-AlarmManager של מערכת ההפעלה אנדרואיד האחראי על תזמון משימות ברקע
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // ==========================================
            // --- תזמון התראה ראשונה: יום אחד לפני התור ---
            // ==========================================
            // תנאי: מוודאים שנקודת הזמן של "יום לפני" עדיין לא עברה ונמצאת בעתיד ביחס לזמן הנוכחי במכשיר
            if (System.currentTimeMillis() < oneDayBefore) {
                // יצירת כוונת (Intent) המכוונת אל מחלקת ה-Receiver שתקלוט את השידור ברקע
                Intent intent1Day = new Intent(context, NotificationReceiver.class);
                // העברת כותרת ההתראה כפרמטר בתוך ה-Intent
                intent1Day.putExtra("title", "תזכורת: תור מחר!");
                // העברת תוכן הודעת התזכורת עם שם בית העסק המדויק והשעה
                intent1Day.putExtra("message", "מחר יש לך תור אצל " + businessName + " בשעה " + time);
                // יצירת מזהה ייחודי להתראה על ידי הפעלת פונקציית ייצור קוד גיבוב (Hash) ממחרוזת זיהוי התור
                intent1Day.putExtra("notificationId", (appointmentId + "day").hashCode());

                // עטיפת ה-Intent באובייקט PendingIntent המאפשר למערכת ההפעלה להריץ את הקוד בשם האפליקציה גם כשהיא סגורה
                // שימוש בדגלים המגדירים שההתראה אינה ניתנת לשינוי (IMMUTABLE) ושתתעדכן במידה וקיימת (UPDATE_CURRENT)
                PendingIntent pendingIntent1Day = PendingIntent.getBroadcast(
                        context, (appointmentId + "day").hashCode(), intent1Day,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                // התאמת קוד לתמיכה בגרסאות אנדרואיד השונות (בדיקה האם גרסת המכשיר היא מרשמלו 6.0 ומעלה)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // תזמון מדויק המאפשר להעיר את המכשיר גם אם הוא נמצא במצב חיסכון בסוללה (Doze Mode)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, oneDayBefore, pendingIntent1Day);
                } else {
                    // תזמון מדויק בגרסאות אנדרואיד ישנות יותר
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, oneDayBefore, pendingIntent1Day);
                }
            }

            // ==========================================
            // --- תזמון התראה שנייה: שעה אחת לפני התור ---
            // ==========================================
            // תנאי: מוודאים שנקודת הזמן של "שעה לפני" עדיין לא עברה ונמצאת בעתיד ביחס לזמן הנוכחי במכשיר
            if (System.currentTimeMillis() < oneHourBefore) {
                // יצירת כוונת (Intent) ממוקדת עבור התראת השעה
                Intent intent1Hour = new Intent(context, NotificationReceiver.class);
                // הגדרת כותרת ותוכן ההודעה הייעודיים להתראת הטווח הקצר
                intent1Hour.putExtra("title", "תזכורת: התור שלך בעוד שעה!");
                intent1Hour.putExtra("message", "התור אצל " + businessName + " מתחיל בשעה " + time);
                intent1Hour.putExtra("notificationId", (appointmentId + "hour").hashCode());

                // עטיפת ה-Intent ב-PendingIntent ייחודי בעל קוד בקשה (Request Code) נפרד המבוסס על קוד הגיבוב
                PendingIntent pendingIntent1Hour = PendingIntent.getBroadcast(
                        context, (appointmentId + "hour").hashCode(), intent1Hour,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                // הפעלת התזמון המדויק מול שירות ה-AlarmManager של המכשיר
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, oneHourBefore, pendingIntent1Hour);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, oneHourBefore, pendingIntent1Hour);
                }
            }

        } catch (Exception e) {
            // הדפסת עקבות השגיאה במערכת הלוגים במידה ופונקציית הפענוח נכשלה
            e.printStackTrace();
        }
    }
}