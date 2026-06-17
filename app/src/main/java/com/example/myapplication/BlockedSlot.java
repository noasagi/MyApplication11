package com.example.myapplication;

// מחלקת מודל המייצגת חלון זמן חסום ביומן של בית העסק (למשל: הפסקת צהריים, סידורים)
public class BlockedSlot {

    // --- משתני המחלקה (תכונות פרטיות) ---
    private String blockId;    // מזהה ייחודי של החסימה בבסיס הנתונים Firestore
    private String date;       // תאריך יום החסימה (למשל: "28/05/2026")
    private String startTime;  // שעת תחילת חלון הזמן החסום (למשל: "13:00")
    private String endTime;    // שעת סיום חלון הזמן החסום (למשל: "14:00")
    private String reason;     // סיבת החסימה (למשל: "הפסקת צהריים")

    /**
     * [פעולה בונה ריקה - Default Constructor]
     * קלט: אין. | פלט: אובייקט חסימה ריק בזיכרון.
     * מה עושה ואיך: פונקציה ריקה לחלוטין שהיא חובה של Firebase Firestore.
     * כשמערכת ה-Firestore שולפת נתונים על חסימות, היא משתמשת בה כדי לייצר את האובייקט הבסיסי בזיכרון.
     */
    public BlockedSlot() {}

    /**
     * [פעולה בונה מלאה - Parameterized Constructor]
     * קלט: מזהה חסימה, תאריך, שעת התחלה, שעת סיום וסיבה.
     * פלט: אובייקט חסימה מאותחל עם כל הנתונים.
     * מה עושה ואיך: משתמשת במילה השמורה 'this' כדי להבדיל בין המשתנים שקיבלנו בסוגריים לבין משתני המחלקה,
     * ומציבה את ערכי הקלט בתוך התכונות הפרטיות של האובייקט שזה עתה נוצר.
     */
    public BlockedSlot(String blockId, String date, String startTime, String endTime, String reason) {
        this.blockId = blockId;         // הצבת מזהה החסימה שנתקבל בתוך משתנה המחלקה blockId
        this.date = date;               // הצבת התאריך
        this.startTime = startTime;     // הצבת שעת התחלה
        this.endTime = endTime;         // הצבת שעת סיום
        this.reason = reason;           // הצבת הסיבה לחסימה
    }

    // --- פעולות גישה (Getters) לקבלת ערכי השדות מתוך האובייקט ---
    // הערה: במחלקה זו יש רק פונקציות לקריאת הנתונים (Getters) ואין פונקציות לשינוי (Setters).

    /**
     * קלט: אין. | פלט: מחרוזת (String) המייצגת את שעת תחילת החסימה.
     * מה עושה: מחזירה את השעה שבה החסימה מתחילה, כדי שנוכל לבדוק אותה מול תורים של לקוחות.
     */
    public String getStartTime() { return startTime; }

    /**
     * קלט: אין. | פלט: מחרוזת (String) המייצגת את שעת סיום החסימה.
     * מה עושה: מחזירה את השעה שבה החסימה מסתיימת.
     */
    public String getEndTime() { return endTime; }
}