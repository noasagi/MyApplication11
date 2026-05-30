package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

// מחלקת עזר (Helper Class) לניהול מקומי של סוגי המשתמשים, ההרשאות ומצב החיבור באפליקציה
public class UserHelper {

    // הגדרת קבועים (Constants) המייצגים את שלושת התפקידים האפשריים במערכת
    public static final String ROLE_BUSINESS = "business"; // בעל עסק
    public static final String ROLE_CLIENT = "client";     // לקוח רגיל
    public static final String ROLE_GUEST = "guest";       // אורח (אינו מחובר)

    // הגדרת קבועי מחרוזת המשמשים כמפתחות זיהוי עבור רכיב ה-SharedPreferences
    private static final String PREF_NAME = "user_prefs"; // שם קובץ ה-XML המקומי שישמר במכשיר
    private static final String KEY_ROLE = "user_role";   // המפתח הספציפי שבו יישמר ערך התפקיד

    // הצהרה על רכיב ה-SharedPreferences של אנדרואיד לאחסון נתונים קלים בצורה מקומית
    private final SharedPreferences prefs;

    // פעולה בונה (Constructor) המקבלת Context ומאתחלת את קובץ הזיכרון המקומי במצב פרטי (MODE_PRIVATE)
    public UserHelper(Context context) {
        // MODE_PRIVATE מבטיח שרק האפליקציה הזו מורשית לקרוא ולכתוב לקובץ נתונים זה
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // פונקציה לעדכון ושמירת תפקיד המשתמש הנוכחי בזיכרון המכשיר
    public void setRole(String role) {
        // שימוש ב-Editor כדי לפתוח עריכה, הזנת המחרוזת תחת המפתח המתאים, והפעלה ברקע באמצעות apply()
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    // פונקציה השולפת את תפקיד המשתמש השמור כרגע בזיכרון המכשיר
    public String getRole() {
        // במידה ועדיין לא נשמר שום תפקיד (למשל בכניסה הראשונה לאפליקציה), ערך ברירת המחדל שיוחזר יהיה "אורח"
        return prefs.getString(KEY_ROLE, ROLE_GUEST);
    }

    // פונקציה בוליאנית הבודקת האם המשתמש הנוכחי הוא בעל עסק
    public boolean isBusinessOwner() {
        // השוואה בטוחה בין קבוע התפקיד לערך השלוף מהזיכרון
        return ROLE_BUSINESS.equals(getRole());
    }

    // פונקציה בוליאנית הבודקת האם המשתמש הנוכחי הוא לקוח רגיל
    public boolean isClient() {
        return ROLE_CLIENT.equals(getRole());
    }

    // פונקציה בוליאנית הבודקת האם המשתמש הנוכחי מוגדר כאורח (לא מחובר לחשבון)
    public boolean isGuest() {
        return ROLE_GUEST.equals(getRole());
    }

    // פונקציית התנתקות (Logout) המאפסת את תפקיד המשתמש בחזרה למצב אורח בזיכרון המקומי
    public void logout() {
        setRole(ROLE_GUEST);
    }
}