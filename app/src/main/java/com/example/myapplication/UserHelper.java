package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class UserHelper {

    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_USER_ROLE = "userRole";

    // הקבועים חייבים להיות זהים למה שכתוב ב-RadioButtons שלך בדף ההרשמה
    public static final String ROLE_REGULAR = "משתמש רגיל";
    public static final String ROLE_BUSINESS = "בעל עסק";
    public static final String ROLE_GUEST = "guest";

    private SharedPreferences sharedPreferences;

    public UserHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // שמירת תפקיד המשתמש
    public void setRole(String role) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    // קבלת תפקיד המשתמש
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, ROLE_GUEST);
    }

    // האם המשתמש הוא בעל עסק?
    public boolean isBusinessOwner() {
        return getUserRole().equals(ROLE_BUSINESS);
    }

    // האם המשתמש הוא אורח (לא מחובר)?
    public boolean isGuest() {
        return getUserRole().equals(ROLE_GUEST);
    }

    // התנתקות
    public void logout() {
        setRole(ROLE_GUEST);
    }
}