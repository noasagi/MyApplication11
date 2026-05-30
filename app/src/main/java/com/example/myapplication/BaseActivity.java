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

// הגדרת מחלקה מופשטת (Abstract Class) המשמשת כאב טיפוס לכל האקטיביטיז באפליקציה למניעת שכפול קוד
public abstract class BaseActivity extends AppCompatActivity {

    // הצהרה על משתנה מוגן (Protected) המאפשר גישה לעדכון ושליפת נתוני משתמש בכל אקטיביטי יורשת
    protected UserHelper userHelper;
    // הצהרה על רכיב מערכת של אנדרואיד המנהל ומנטר את מצב החיבוריות והרשתות במכשיר
    private ConnectivityManager connectivityManager;
    // הצהרה על ממשק מאזין (Callback) שתפקידו לקבל אירועים בזמן אמת על שינויים במצב הרשת
    private ConnectivityManager.NetworkCallback networkCallback;
    // משתנה בוליאני המשמש כדגל (Flag) כדי לדעת האם כבר זיהינו שהאינטרנט התנתק בעבר (למניעת כפל הודעות)
    private boolean isNetworkLostAlready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // שליפת שירות המערכת של אנדרואיד לניהול חיבוריות והצבתו במשתנה connectivityManager
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // תנאי: אתחול של אובייקט העזר למשתמש במידה והוא עדיין ריק ולא אותחל קודם לכן
        if (userHelper == null) {
            userHelper = new UserHelper(this);
        }

        // הפעלת בדיקה ידנית ראשונית של מצב האינטרנט מיד עם פתיחת המסך
        checkCurrentNetworkState();

        // קריאה לפונקציה המבצעת רישום והפעלה של המאזין הדינמי לשינויי רשת
        registerNetworkCallback();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // הסרת הרישום של המאזין לרשת כאשר המסך יוצא מטווח הראייה של המשתמש (חוסך משאבים וסוללה)
        unregisterNetworkCallback();
    }

    // --- מערכת בדיקת וניטור מצב האינטרנט הגלובלית ---

    // פונקציה המבצעת בדיקה נקודתית חד-פעמית של מצב הרשת הנוכחי במכשיר
    private void checkCurrentNetworkState() {
        // שימוש במחלקת העזר הסטטית NetworkUtils לבדיקה האם האינטרנט זמין כרגע
        if (!NetworkUtils.isNetworkAvailable(this)) {
            // עדכון הדגל למצב אמת המציין כי החיבור לרשת אבד
            isNetworkLostAlready = true;
            // הצגת הודעה קופצת (Toast) למשתמש המזהירה אותו שאין חיבור תקין לרשת
            Toast.makeText(this, "אין חיבור לאינטרנט. פעולות מסוימות עלולות לא לעבוד 😕", Toast.LENGTH_LONG).show();
        }
    }

    // פונקציה האחראית להגדיר ולרשום את המאזין הדינמי שמנטר את רשת האינטרנט בזמן אמת
    private void registerNetworkCallback() {
        try {
            // בניית דרישת רשת מוגדרת: אנו מבקשים להאזין אך ורק לרשתות שיש להן יכולת גישה ממשית לאינטרנט
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            // יצירת מופע אנונימי קלאסי של מחלקת ה-NetworkCallback
            networkCallback = new ConnectivityManager.NetworkCallback() {

                @Override
                // פונקציה המופעלת אוטומטית על ידי המערכת ברגע שרשת אינטרנט הופכת לזמינה ותקינה
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    // תנאי: נציג הודעה שהאינטרנט חזר אך ורק אם קודם לכן זיהינו מצב של נתק
                    if (isNetworkLostAlready) {
                        // הרצת קוד בתוך תהליך הממשק הראשי (UI Thread) מכיוון שאירוע הרשת מגיע מתהליך רקע
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // הצגת הודעת חיווי מהירה למשתמש שהחיבור לרשת האינטרנט חזר בהצלחה
                                Toast.makeText(BaseActivity.this, "החיבור לאינטרנט חזר 😊", Toast.LENGTH_SHORT).show();
                            }
                        });
                        // איפוס הדגל בחזרה למצב שקר מכיוון שהאינטרנט כעת מחובר ותקין
                        isNetworkLostAlready = false;
                    }
                }

                @Override
                // פונקציה המופעלת אוטומטית על ידי המערכת ברגע שרשת האינטרנט מתנתקת לחלוטין
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    // עדכון הדגל למצב אמת המסמן שהחיבור לאינטרנט אבד כעת
                    isNetworkLostAlready = true;
                    // מעבר לתהליך ה-UI הראשי לצורך ביצוע שינויים גרפיים והצגת הודעה למשתמש
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // הצגת הודעת אזהרה למשתמש שאין חיבור לרשת ופעולות באפליקציה עלולות להיכשל
                            Toast.makeText(BaseActivity.this, "אין חיבור לאינטרנט. פעולות מסוימות עלולות לא לעבוד 😕", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            };
            // רישום רשמי של המאזין שיצרנו בתוך מנהל הרשתות של מערכת ההפעלה אנדרואיד
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        } catch (Exception e) {
            // הדפסת עקבות השגיאה במידה ונוצרה חריגה בזמן הרישום של המאזין
            e.printStackTrace();
        }
    }

    // פונקציה המבצעת ניתוק מבוקר ובטוח של המאזין ממערכת ההפעלה
    private void unregisterNetworkCallback() {
        try {
            // תנאי בטיחות: מוודאים שגם מנהל הרשת וגם אובייקט המאזין קיימים ואינם מצביעים לערך ריק
            if (connectivityManager != null && networkCallback != null) {
                // ביטול הרישום של המאזין הספציפי בתוך מנהל הרשתות
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            // הדפסת עקבות השגיאה במידה ונוצרה חריגה בזמן ביטול הרישום
            e.printStackTrace();
        }
    }

    // --- הגדרות וניהול סרגלי כלים (Toolbar) ---

    // פונקציה מוגנת המאפשרת לאקטיביטיז הבנות להגדיר סרגל כלים ראשי במסך
    protected void setupToolbar(Toolbar toolbar) {
        // תנאי: אם רכיב ה-Toolbar שהועבר אינו ריק, נגדיר אותו כסרגל הכלים הרשמי של האקטיביטי
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    // פונקציה מוגנת להגדרת סרגל כלים משני הכולל אפשרות להצגת לחצן חץ חזרה מובנה
    protected void setupSecondaryToolbar(Toolbar toolbar, boolean showBackButton) {
        // קריאה לפונקציה הבסיסית להגדרת הסרגל במערכת
        setupToolbar(toolbar);

        // תנאי: אם המשתמש ביקש להציג כפתור חזרה וסרגל הכלים אכן מאותחל ותקין
        if (showBackButton && getSupportActionBar() != null) {
            // הפעלת אפשרות התצוגה של כפתור הבית (Home) כחץ חזרה מובנה
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // הגדרה המאפשרת לכפתור הבית לתפקד ולהיראות כחצן ניווט חזרה ברור
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    // --- טיפול וניהול אירועי הלחיצה על חץ הניווט "אחורה" שבסרגל הכלים ---
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // בדיקה: האם המזהה של הפריט שנלחץ בסרגל תואם למזהה המובנה של חץ החזרה (android.R.id.home)
        if (item.getItemId() == android.R.id.home) {
            // הפעלת פונקציית החזרה המובנית של המכשיר המדמה לחיצה על כפתור החזרה הפיזי
            onBackPressed();
            // החזרת ערך אמת (true) המציין כי טיפלנו באירוע הלחיצה בהצלחה ואין צורך להמשיך הלאה
            return true;
        }
        // במידה ונלחץ רכיב אחר, נעביר את הטיפול להמשך המימוש של מחלקת האב
        return super.onOptionsItemSelected(item);
    }
}