package com.example.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

// הגדרת מחלקה מופשטת (Abstract) המשמשת בסיס ואב-טיפוס לשאר המסכים באפליקציה
public abstract class BaseActivity extends AppCompatActivity {

    // משתנה מוגן (protected) שיהיה נגיש בכל המסכים שיורשים ממחלקה זו
    protected UserHelper userHelper;

    // רכיב של אנדרואיד שמנהל ומנטר את מצב החיבוריות והרשת במכשיר
    private ConnectivityManager connectivityManager;

    // ממשק מאזין (Callback) שמקבל עדכונים בזמן אמת כשהאינטרנט מתנתק או חוזר
    private ConnectivityManager.NetworkCallback networkCallback;

    // משתנה בוליאני (דגל) שמונע הקפצת הודעות כפולות על ניתוק האינטרנט
    private boolean isNetworkLostAlready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // שליפת שירות המערכת של אנדרואיד שאחראי על האינטרנט והרשתות במכשיר
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // אם אובייקט העזר למשתמש עדיין לא נוצר, ניצור אותו כעת עבור המסך הנוכחי
        if (userHelper == null) {
            userHelper = new UserHelper(this);
        }

        // בדיקה ידנית ראשונית של מצב האינטרנט מיד כשהמסך עולה
        checkCurrentNetworkState();

        // הפעלת מאזין קבוע שישים לב אם האינטרנט מתנתק בזמן שהמשתמש במסך
        registerNetworkCallback();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // עצירת המאזין כשהמסך נעלם מהעין כדי לחסוך בסוללה ובמשאבי מעבד של המכשיר
        unregisterNetworkCallback();
    }

    // --- מערכת בדיקת וניטור מצב האינטרנט בזמן אמת ---

    /**
     * קלט: אין. | פלט: אין (void).
     * מה עושה ואיך: מבצעת בדיקה חד-פעמית ומהירה. אם משתמשת במחלקת עזר חיצונית (NetworkUtils)
     * ומגלה שאין אינטרנט, היא מדליקה את הדגל (isNetworkLostAlready = true) ומקפיצה הודעת אזהרה.
     */
    private void checkCurrentNetworkState() {
        // קריאה לפונקציה סטטית הבודקת האם יש כרגע חיבור רשת זמין
        if (!NetworkUtils.isNetworkAvailable(this)) {

            // סימון שהאינטרנט אבד כדי שהמאזין ידע ולא יקפיץ הודעה כפולה בהמשך
            isNetworkLostAlready = true;

            // הקפצת הודעת טקסט קצרה (Toast) למשתמש על גבי המסך
            Toast.makeText(this, "אין חיבור לאינטרנט. פעולות מסוימות עלולות לא לעבוד 😕", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * קלט: אין. | פלט: אין (void).
     * מה עושה ואיך: מגדירה דרישת רשת ספציפית (שרשת האינטרנט תהיה עם גישה אמיתית לעולם), מייצרת
     * אובייקט מאזין אנונימי (NetworkCallback) שמכיל שתי פונקציות: onAvailable (כשחוזר) ו-onLost (כשמתנתק).
     */
    private void registerNetworkCallback() {
        try {
            // הגדרת קריטריון: אנו מעוניינים להאזין רק לרשתות שיש להן יכולת גישה לאינטרנט
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            // יצירת המאזין האנונימי שיקבל את האירועים ממערכת ההפעלה אנדרואיד
            networkCallback = new ConnectivityManager.NetworkCallback() {

                @Override
                // פונקציה זו רצה אוטומטית כשאנדרואיד מזהה שהמכשיר התחבר מחדש לאינטרנט
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);

                    // נציג הודעה שהאינטרנט חזר רק אם המכשיר באמת היה מנותק קודם לכן
                    if (isNetworkLostAlready) {

                        // שימוש ב-runOnUiThread חובה! אירוע הרשת מגיע מתהליך רקע, ואסור לעדכן גרפיקה (Toast) משם.
                        // פקודה זו מעבירה את ביצוע הקוד לתהליך הראשי (UI Thread) שאחראי על המסך.
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(BaseActivity.this, "החיבור לאינטרנט חזר 😊", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // איפוס הדגל בחזרה לשקר, כי כעת המצב תקין ומחובר
                        isNetworkLostAlready = false;
                    }
                }

                @Override
                // פונקציה זו רצה אוטומטית כשהאינטרנט מתנתק (למשל, עבר למצב טיסה או יצא מטווח ה-Wi-Fi)
                public void onLost(@NonNull Network network) {
                    super.onLost(network);

                    // סימון שהאינטרנט נעלם
                    isNetworkLostAlready = true;

                    // מעבר לתהליך ה-UI הראשי כדי להציג הודעת אזהרה למשתמש על גבי המסך כשהוא מנסה לעבוד
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(BaseActivity.this, "אין חיבור לאינטרנט. פעולות מסוימות עלולות לא לעבוד 😕", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            };

            // רישום המאזין שיצרנו בתוך מנהל הרשתות של אנדרואיד כדי שיתחיל לעבוד בפועל
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

        } catch (Exception e) {
            // הדפסת שגיאה במקלדת (Log) במידה ומשהו נכשל בתהליך הרישום של המאזין
            e.printStackTrace();
        }
    }

    /**
     * קלט: אין. | פלט: אין (void).
     * מה עושה ואיך: פונה למנהל הרשת ומבטלת את ההאזנה של ה-NetworkCallback.
     * זה קורה ב-onStop כדי למנוע "זליגת זיכרון" (Memory Leak) כשהמשתמש עוזב את המסך.
     */
    private void unregisterNetworkCallback() {
        try {
            // בדיקת בטיחות: מוודאים שמנהל הרשת והמאזין בכלל קיימים לפני שמנסים לבטל אותם
            if (connectivityManager != null && networkCallback != null) {

                // ניתוק רשמי של המאזין ממערכת ההפעלה
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- מערכת ניהול סרגלי כלים (Toolbar) ---

    /**
     * קלט: אובייקט Toolbar מה-XML. | פלט: אין (void).
     * מה עושה ואיך: מקבלת סרגל כלים שעיצבנו ב-XML ומגדירה אותו כסרגל הראשי הרשמי של המסך הנוכחי.
     */
    protected void setupToolbar(Toolbar toolbar) {
        if (toolbar != null) {
            // הגדרת ה-Toolbar שישמש כ-ActionBar של המסך לצורך הצגת כותרות ותפריטים
            setSupportActionBar(toolbar);
        }
    }

    /**
     * קלט: אובייקט Toolbar, ומשתנה בוליאני showBackButton (האם להציג חץ חזרה). | פלט: אין (void).
     * מה עושה ואיך: מגדירה את הסרגל, ואם showBackButton הוא true, היא מוסיפה באופן מובנה חץ קטן שפונה שמאלה/ימינה לחזרה למסך הקודם.
     */
    protected void setupSecondaryToolbar(Toolbar toolbar, boolean showBackButton) {
        // שימוש בפונקציה הקודמת כדי להגדיר את הסרגל הבסיסי במערכת
        setupToolbar(toolbar);

        // אם המשתמש ביקש חץ חזרה, וסרגל הכלים אכן קיים ופועל בהצלחה
        if (showBackButton && getSupportActionBar() != null) {

            // פקודה המציגה את כפתור ה"בית" המובנה של הסרגל
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            // הפיכת הכפתור הזה ללחיץ ונראה כחץ ניווט לחזרה
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    // --- טיפול בלחיצה על חץ החזרה שבסרגל הכלים ---
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // בדיקה: האם הפריט שנלחץ בסרגל הוא כפתור החזרה המובנה של אנדרואיד (שמזהה ה-ID שלו הוא קבוע במערכת)
        if (item.getItemId() == android.R.id.home) {

            // קריאה לפונקציה המובנית שמדמה לחיצה על כפתור החזור הפיזי של הטלפון וסוגרת את המסך הנוכחי
            onBackPressed();

            // החזרת true כדי לסמן למערכת שטיפלנו בלחיצה הזו ואין צורך להעביר אותה הלאה
            return true;
        }

        // אם נלחץ משהו אחר (כמו כפתור תפריט אחר), תן למחלקת האב לטפל בזה כרגיל
        return super.onOptionsItemSelected(item);
    }
}