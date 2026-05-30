package com.example.myapplication;

// מחלקת מודל (Model Class) המייצגת חלון זמן חסום ביומן של בית העסק (למשל: הפסקת צהריים, סידורים)
public class BlockedSlot {

    // מזהה ייחודי עבור החסימה (תואם ל-ID של המסמך בבסיס הנתונים Firestore)
    private String blockId;
    // מחרוזת טקסט המייצגת את תאריך החסימה בפורמט קבוע (למשל: "28-05-2026")
    private String date;
    // מחרוזת טקסט המייצגת את שעת תחילת חלון הזמן החסום (למשל: "12:00")
    private String startTime;
    // מחרוזת טקסט המייצגת את שעת סיום חלון הזמן החסום (למשל: "14:00")
    private String endTime;
    // מחרוזת טקסט המפרטת את סיבת חסימת התור (למשל: "הפסקת צהריים", "סידורים אישיים")
    private String reason;

    // פעולה בונה ריקה (Default Constructor) - דרישת חובה של Firestore לצורך המרת מסמכים אוטומטית לאובייקט Java
    public BlockedSlot() {}

    // פעולה בונה מלאה (Parameterized Constructor) המאפשרת לאתחל אובייקט חסימה חדש בזיכרון עם כלל נתוניו
    public BlockedSlot(String blockId, String date, String startTime, String endTime, String reason) {
        this.blockId = blockId;         // השמת מזהה החסימה הייחודי
        this.date = date;               // השמת תאריך יום החסימה
        this.startTime = startTime;     // השמת שעת תחילת החסימה
        this.endTime = endTime;         // השמת שעת סיום החסימה
        this.reason = reason;           // השמת סיבת החסימה
    }

    // --- פעולות גישה (Getters) לקבלת ערכי השדות מתוך האובייקט ---

    // פונקציה לקבלת שעת תחילת החסימה
    public String getStartTime() { return startTime; }

    // פונקציה לקבלת שעת סיום החסימה
    public String getEndTime() { return endTime; }
}