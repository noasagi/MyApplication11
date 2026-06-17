package com.example.myapplication;

// מחלקת מודל נתונים (Model / POJO Class) המייצגת שירות או טיפול המוצע על ידי בית עסק במערכת
// המחלקה מיועדת למיפוי והמרה אוטומטית מול מסמכי בסיס הנתונים Cloud Firestore
public class Treatment {

    // הגדרת משתני מחלקה פרטיים (Private) כחלק מיישום עקרון הכמוסה (Encapsulation) להגנה על המידע
    private String treatmentId;
    private String name;           // שם השירות (למשל: "תספורת גברים", "טיפול פנים")
    private double price;          // מחיר השירות בשקלים (double מאפשר דיוק עשרוני במידת הצורך)
    private int durationMinutes;   // משך הטיפול בדקות - קריטי לחישובי זמנים ומשבצות פנויות ביומן האפליקציה

    /**
     * מה הפעולה עושה: פעולה בונה ריקה (Default Constructor).
     * למה היא חובה: דרישה טכנולוגית מוחלטת של Cloud Firestore לצורך דה-סריאליזציה (Deserialization) - תהליך שבו פיירבייס לוקח מסמך NoSQL מהענן, מייצר מופע ריק של האובייקט בזיכרון, ומזריק אליו את הנתונים באופן אוטומטי. ללא פעולה זו, השליפה תיכשל והאפליקציה תקרוס!
     */
    public Treatment() {}

    // פעולה בונה מלאה (Parameterized Constructor) לאתחול והקמת אובייקט טיפול שלם בזיכרון המערכת רגע לפני יצירה או עדכון
    public Treatment(String treatmentId, String name, double price, int durationMinutes) {
        this.treatmentId = treatmentId;
        this.name = name;
        this.price = price;
        this.durationMinutes = durationMinutes;
    }

    // --- מערך פונקציות גישה ועדכון (Getters & Setters) המאפשרות גישה מבוקרת לשדות הפרטיים מחוץ למחלקה ---

    public String getTreatmentId() { return treatmentId; }
    public void setTreatmentId(String treatmentId) { this.treatmentId = treatmentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}