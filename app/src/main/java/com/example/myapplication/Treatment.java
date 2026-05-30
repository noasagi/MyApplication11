package com.example.myapplication;

// מחלקת מודל נתונים (Model / POJO Class) המייצגת שירות או טיפול המוצע על ידי בית עסק במערכת
// המחלקה מיועדת למיפוי והמרה אוטומטית מול מסמכי בסיס הנתונים Cloud Firestore
public class Treatment {
    // מזהה ייחודי של מסמך הטיפול במסד הנתונים בענן
    private String treatmentId;
    // שם הטיפול או השירות המוצע ללקוחות (לדוגמה: "תספורת גברים", "לק ג'ל")
    private String name;
    // מחיר השירות/הטיפול בשקלים (משתנה מסוג double לתמיכה בערכים עשרוניים במידת הצורך)
    private double price;
    // משך הזמן הנדרש לביצוע הטיפול, מבוטא ביחידות של דקות (לדוגמה: 30, 45, 60)
    private int durationMinutes;

    // פעולה בונה ריקה (Default Constructor) - דרישת חובה טכנולוגית של פיירבייס לצורך המרה אוטומטית של מסמכים לאובייקט ג'אווה
    public Treatment() {}

    // פעולה בונה מלאה (Parameterized Constructor) לאתחול והקמת אובייקט טיפול שלם בזיכרון המערכת
    public Treatment(String treatmentId, String name, double price, int durationMinutes) {
        this.treatmentId = treatmentId;
        this.name = name;
        this.price = price;
        this.durationMinutes = durationMinutes;
    }

    // --- מערך פונקציות גישה (Getters) לקריאת ערכי המשתנים הפרטיים מחוץ למחלקה ---

    public String getTreatmentId() { return treatmentId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getDurationMinutes() { return durationMinutes; }

    // --- מערך פונקציות עדכון (Setters) לשינוי ערכי המשתנים הפרטיים תוך שמירה על עקרון הכמוסה ---

    public void setTreatmentId(String treatmentId) { this.treatmentId = treatmentId; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}