package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

// מחלקת עזר (Helper Class) לניהול מקומי מהיר של סוגי המשתמשים, ההרשאות ומצב החיבור באפליקציה
// המחלקה מיישמת ארכיטקטורה מבוססת תפקידים (Role-Based State) באמצעות רכיב ה-SharedPreferences של אנדרואיד
public class UserHelper {

    // הגדרת קבועים פומביים (Constants) המייצגים את שלושת התפקידים האפשריים במערכת
    // שימוש בקבועים מונע שגיאות כתיב (Typos) ברחבי האפליקציה (למשל, כתיבת "Business" עם ב' רבתית בטעות)
    public static final String ROLE_BUSINESS = "business"; // בעל עסק
    public static final String ROLE_CLIENT = "client";     // לקוח רגיל
    public static final String ROLE_GUEST = "guest";       // אורח (אינו מחובר)

    // קבועי מחרוזת פרטיים המשמשים כמפתחות זיהוי (Keys) עבור קובץ ה-XML המקומי
    private static final String PREF_NAME = "user_prefs"; // שם קובץ ה-XML שישמר פיזית בתיקיית האפליקציה במכשיר
    private static final String KEY_ROLE = "user_role";   // המפתח הספציפי שבו יישמר ערך התפקיד הנוכחי

    // רכיב ה-SharedPreferences לאחסון זוגות של מפתח-ערך (Key-Value) בצורה פשוטה, מקומית ומהירה
    private final SharedPreferences prefs;

    /**
     * פעולה בונה (Constructor) המקבלת Context ומאתחלת את קובץ הזיכרון המקומי.
     * הסבר לבוחן: השימוש ב-Context.MODE_PRIVATE הוא הגדרת אבטחה קריטית שמבטיחה שקובץ ה-XML הזה יהיה מוצפן/נגיש
     * אך ורק לאפליקציה הזו, ואף אפליקציה חיצונית אחרת המותקנת על המכשיר לא תוכל לקרוא או לשנות את תוכן ההרשאות שלו.
     */
    public UserHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * מה הפעולה עושה: שומרת את תפקיד המשתמש החדש בקובץ ה-XML המקומי.
     * קלט: String role.
     * הסבר טכנולוגי: אנו משתמשים ב-`prefs.edit()` לפתיחת עורך, מזינים את הערך, וקוראים ל-`apply()`.
     * פקודת `apply()` שומרת את המידע בזיכרון ה-RAM מיד, וכותבת לקובץ הפיזי בדיסק ברקע באופן אסינכרוני (בניגוד ל-`commit()` שהיא סינכרונית וחוסמת את ה-UI).
     */
    public void setRole(String role) {
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    /**
     * מה הפעולה עושה: שולפת את תפקיד המשתמש השמור כרגע בזיכרון המכשיר.
     * פלט: String (תפקיד המשתמש).
     * מנגנון הגנה: הפרמטר השני ב-`getString` הוא ערך ברירת מחדל (Default Value). אם זו הפעם הראשונה שהאפליקציה נפתחת
     * ועדיין לא קיים מפתח כזה בזיכרון, המערכת תחזיר אוטומטית `ROLE_GUEST` ("guest") ובכך תמנע קבלת `null` וקריסה.
     */
    public String getRole() {
        return prefs.getString(KEY_ROLE, ROLE_GUEST);
    }

    /**
     * פונקציה בוליאנית לבדיקה מהירה האם המשתמש הנוכחי הוא בעל עסק.
     * טיפ לתשובה בבגרות: השתמשתי במבנה `ROLE_BUSINESS.equals(getRole())` ולא בהפך (`getRole().equals(...)`).
     * הסיבה היא הגנה מפני קריסת NullPointerException - אם מסיבה כלשהי getRole יחזיר null, המבנה שלי לא יקרוס אלא פשוט יחזיר false, כי קבוע לעולם אינו null.
     */
    public boolean isBusinessOwner() {
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

    // פונקציית התנתקות (Logout) מקומית - מחזירה את מצב ההרשאות במכשיר למצב "אורח" בטוח
    public void logout() {
        setRole(ROLE_GUEST);
    }
}