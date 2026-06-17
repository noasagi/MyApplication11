package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

// מחלקת רסיבר (Broadcast Receiver) הפועלת ברקע ומאזינה לאותות ושידורים מתוזמנים של מערכת ההפעלה
public class NotificationReceiver extends BroadcastReceiver {

    // מזהה קבוע וייחודי לערוץ ההתראות (Notification Channel ID) - חובה לפי חוקי אנדרואיד החדשים
    private static final String CHANNEL_ID = "appointment_reminders";

    /**
     * מה הפעולה עושה: פונקציית הליבה של הרסיבר. מופעלת אוטומטית על ידי מערכת ההפעלה ברגע ששעון ה-AlarmManager מגיע לזמן המתוזמן, ושולפת את נתוני ההתראה מתוך ה-Intent.
     * קלט: Context context, Intent intent (האינטנט ששודר ומכיל את נתוני ההתראה).
     * פלט: אין (void).
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // שליפת הנתונים הדינמיים שהועברו בתוך ה-Intent מהחלק המערכתי
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        // שליפת מזהה ייחודי (במידה ואין, נשתמש בזמן הנוכחי במילישניות כמזהה ברירת מחדל למניעת דריסה)
        int notificationId = intent.getIntExtra("notificationId", (int) System.currentTimeMillis());

        // שליפת מנהל ההתראות המערכתי של אנדרואיד (NotificationManager) האחראי על שיגור הודעות למסך
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // --- יצירת ערוץ התראות (Notification Channel) - חובה החל מ-Android 8.0 (Oreo) ומעלה ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // יצירת עצם הערוץ עם רמת חשיבות גבוהה (IMPORTANCE_HIGH) המאפשרת השמעת צליל והקפצת בועה (Banner)
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "תזכורות תורים", NotificationManager.IMPORTANCE_HIGH);
            // רישום הערוץ במערכת ההפעלה של המכשיר
            notificationManager.createNotificationChannel(channel);
        }

        // --- בניית אובייקט ההתראה הגרפי באמצעות תבנית העיצוב Builder לתאימות גרסאות רחבה ---
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // הגדרת האייקון הקטן שיופיע בשורת הסטטוס העליונה של הטלפון
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                // הגדרת כותרת ותוכן ההודעה ששלפנו מתוך ה-Intent
                .setContentTitle(title)
                .setContentText(message)
                // תאימות לאחור: הגדרת עדיפות גבוהה למכשירים ישנים (מתחת לגרסה 8)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // הגדרה המעלימה את ההתראה אוטומטית מסרגל ההודעות ברגע שהמשתמש לוחץ עליה
                .setAutoCancel(true);

        // פקודת השיגור הסופית: הזרקת ההתראה המוכנה למסך המשתמש באמצעות המזהה הייחודי שלה
        notificationManager.notify(notificationId, builder.build());
    }
}