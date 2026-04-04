package com.example.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public abstract class BaseActivity extends AppCompatActivity {

    protected UserHelper userHelper;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkLostAlready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // אתחול מנהל הרשתות של אנדרואיד
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // זה חשוב להשאיר כדי שיהיה לך גישה לנתוני משתמש בכל דף
        if (userHelper == null) {
            userHelper = new UserHelper(this);
        }

        // בדיקה ידנית ראשונית: למקרה שהאינטרנט כבר היה כבוי כשהמסך נפתח
        checkCurrentNetworkState();

        // מתחילים להאזין לשינויים עתידיים ברשת
        registerNetworkCallback();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // מפסיקים להאזין כשהמסך יורד לרקע (חוסך סוללה!)
        unregisterNetworkCallback();
    }

    // --- מערכת בדיקת אינטרנט גלובלית ---

    private void checkCurrentNetworkState() {
        // אנחנו משתמשים במחלקה שיצרנו קודם כדי לבדוק את המצב הנוכחי
        if (!NetworkUtils.isNetworkAvailable(this)) {
            isNetworkLostAlready = true;
            Toast.makeText(this, "אין חיבור לאינטרנט. פעולות מסוימות עלולות לא לעבוד 😕", Toast.LENGTH_LONG).show();
        }
    }

    private void registerNetworkCallback() {
        try {
            // הגדרה מפורשת: אנחנו מאזינים רק לרשתות שיש להן יכולת אינטרנט ממשית
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            networkCallback = new ConnectivityManager.NetworkCallback() {

                // כשהאינטרנט חוזר
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    if (isNetworkLostAlready) {
                        runOnUiThread(() -> Toast.makeText(BaseActivity.this, "החיבור לאינטרנט חזר 😊", Toast.LENGTH_SHORT).show());
                        isNetworkLostAlready = false;
                    }
                }

                // כשהאינטרנט מתנתק (זמן אמת)
                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    isNetworkLostAlready = true;
                    runOnUiThread(() -> Toast.makeText(BaseActivity.this, "אין חיבור לאינטרנט. פעולות מסוימות עלולות לא לעבוד 😕", Toast.LENGTH_LONG).show());
                }
            };
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterNetworkCallback() {
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- הגדרות עיצוב (Toolbar) ---
    protected void setupToolbar(Toolbar toolbar) {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    protected void setupSecondaryToolbar(Toolbar toolbar, boolean showBackButton) {
        setupToolbar(toolbar);

        if (showBackButton && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    // --- טיפול בחץ "אחורה" ---
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}