package com.example.myapplication; // ודאי שזה תואם לשם החבילה שלך

import android.app.Application;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class MyApplication extends Application {

    // ה-APP ID שלך מ-OneSignal
    private static final String ONESIGNAL_APP_ID = "29f0915e-c086-46c3-ab13-15c5d387e90c";

    @Override
    public void onCreate() {
        super.onCreate();

        // מפעיל לוגים כדי שנוכל לראות אם יש שגיאות בחיבור
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

        // אתחול הספריה של OneSignal
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
    }
}