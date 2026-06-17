package com.example.myapplication;

import android.app.Application;

public class MyApplication extends Application {

    /**
     * מה הפעולה עושה: נקודת הכניסה והאתחול הגלובלית הראשונה של האפליקציה כולה. מופעלת פעם אחת בלבד בעת טעינת התהליך (Process) לזיכרון המכשיר, עוד לפני שנוצר או מונפש האקטיביטי (המסך) הראשון.
     * קלט: אין.
     * פלט: אין (void).
     */
    @Override
    public void onCreate() {
        // אתחול תשתיות ה-Context הגלובליות של מערכת ההפעלה אנדרואיד
        super.onCreate();

        // מיקום אסטרטגי להגדרת הגדרות רוחביות (כגון ספריות צד-שלישי, הגדרת ערוצי התראות או רישום SDKs)
    }
}