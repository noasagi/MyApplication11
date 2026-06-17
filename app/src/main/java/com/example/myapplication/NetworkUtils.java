package com.example.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class NetworkUtils {

    /**
     * מה הפעולה עושה: פונקציה סטטית (Static Utility) השולפת את מנהל הקישוריות של מערכת ההפעלה, ובודקת האם המכשיר מחובר כעת לרשת אינטרנט פעילה מסוג Wi-Fi או נתונים סלולריים.
     * קלט: Context context (הקשר המערכת הנדרש לגישה לשירותי מערכת ההפעלה).
     * פלט: boolean (true אם יש אינטרנט זמין, false אם המכשיר מנותק).
     */
    public static boolean isNetworkAvailable(Context context) {

        // שליפת מנהל קישוריות הרשת הרשמי של מערכת ההפעלה אנדרואיד
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            // שליפת מאפייני הרשת (Capabilities) עבור הרשת שנמצאת בשימוש פעיל כרגע
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (capabilities != null) {
                // בדיקת פולימורפיזם של סוגי המדיה: האם החיבור מבוסס Wi-Fi או תשתית סלולרית (Cellular)
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return true;
                }
            }
        }
        return false; // אין חיבור רשת זמין במכשיר
    }
}