package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class UserHelper {

    public static final String ROLE_BUSINESS = "business";
    public static final String ROLE_CLIENT = "client";
    public static final String ROLE_GUEST = "guest";

    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_ROLE = "user_role";

    private final SharedPreferences prefs;

    public UserHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setRole(String role) {
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, ROLE_GUEST);
    }

    public boolean isBusinessOwner() {
        return ROLE_BUSINESS.equals(getRole());
    }

    public boolean isClient() {
        return ROLE_CLIENT.equals(getRole());
    }

    public boolean isGuest() {
        return ROLE_GUEST.equals(getRole());
    }

    public void logout() {
        setRole(ROLE_GUEST);
    }
}
