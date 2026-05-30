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
    // מזהה קבוע וייחודי לערוץ ההתראות (נדרש על פי פרוטוקול מערכת ההפעלה אנדרואיד)
    private static final String CHANNEL_ID = "appointment_reminders";

    @Override
    // פונקציית הליבה המופעלת אוטומטית על ידי מערכת ההפעלה ברגע שמגיע זמן התזכורת המיועד
    public void onReceive(Context context, Intent intent) {
        // שליפת כותרת התזכורת מתוך ה-Intent שהתקבל מהשירות המערכתי
        String title = intent.getStringExtra("title");
        // שליפת תוכן הודעת התזכורת מתוך ה-Intent (שם העסק והשעה)
        String message = intent.getStringExtra("message");
        // שליפת מזהה ההתראה הייחודי, במידה ולא נמצא - ייווצר מזהה המבוסס על חותמת הזמן הנוכחית במילישניות
        int notificationId = intent.getIntExtra("notificationId", (int) System.currentTimeMillis());

        // שליפת מנהל ההתראות המערכתי של אנדרואיד (NotificationManager) האחראי על הצגת הודעות למשתמש
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // --- יצירת ערוץ התראות (Notification Channel) - חובת מימוש ארכיטקטונית החל מאנדרואיד 8.0 ומעלה ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // הגדרת ערוץ חדש הכולל את מזהה הערוץ, השם שיוצג בהגדרות הטלפון ורמת חשיבות גבוהה (IMPORTANCE_HIGH) להשמעת צליל
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "תזכורות תורים", NotificationManager.IMPORTANCE_HIGH);
            // רישום ויצירת הערוץ בפועל בתוך מנהל ההתראות של המכשיר
            notificationManager.createNotificationChannel(channel);
        }

        // --- בניית אובייקט ההתראה הגרפי באמצעות תבנית ה-Builder לתאימות גרסאות רחבה ---
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // הגדרת האייקון הקטן שיופיע בשורת הסטטוס העליונה של המכשיר (משתמש באייקון מידע מובנה של המערכת)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                // הצבת כותרת התזכורת שנשלפה בראש בועת ההתראה
                .setContentTitle(title)
                // הצבת תוכן הטקסט הפירוטי בתוך גוף ההתראה
                .setContentText(message)
                // הגדרת עדיפות גבוהה עבור מכשירים ישנים (תואם לרמת החשיבות של הערוץ)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // הגדרה שגורמת להתראה להיעלם אוטומטית מסרגל ההתראות ברגע שהמשתמש לוחץ עליה
                .setAutoCancel(true);

        // פקודה רשמית המזריקה ומציגה את ההתראה הבנויה על גבי מסך המכשיר באמצעות המזהה הייחודי שלה
        notificationManager.notify(notificationId, builder.build());
    }
}