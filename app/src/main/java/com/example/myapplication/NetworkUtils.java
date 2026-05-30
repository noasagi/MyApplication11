package com.example.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

// מחלקת עזר (Utility Class) המכילה פונקציה סטטית לבדיקת מצב חיבור הרשת והאינטרנט במכשיר
public class NetworkUtils {

    // פונקציה סטטית (ניתנת לקריאה מכל מקום ללא יצירת מופע) הבודקת אם יש כרגע אינטרנט פעיל במכשיר
    public static boolean isNetworkAvailable(Context context) {

        // שליפת מנהל קישוריות הרשת של מערכת ההפעלה (ConnectivityManager) באמצעות ה-Context של האפליקציה
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // תנאי הגנה: מוודאים שמערכת ההפעלה הצליחה לספק את מנהל הקישוריות (אינו Null)
        if (connectivityManager != null) {

            // שליפת מאפייני הרשת (Capabilities) עבור הרשת הפעילה כרגע במכשיר (Active Network)
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            // תנאי: מוודאים שקיימת רשת פעילה ושנשלפו המאפיינים שלה בהצלחה
            if (capabilities != null) {

                // בדיקה א': האם הרשת הפעילה הנוכחית מבוססת על חיבור אלחוטי (Wi-Fi)
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true; // יש אינטרנט פעיל - החזרת אמת

                    // בדיקה ב': במידה ואין Wi-Fi, האם הרשת הפעילה מבוססת על תשתית סלולרית (Cellular Data)
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return true; // יש אינטרנט פעיל - החזרת אמת
                }
            }
        }

        // במידה ולא נמצאה אף רשת פעילה, או שהמכשיר במצב טיסה/מנותק - הפונקציה תחזיר שקר
        return false;
    }
}