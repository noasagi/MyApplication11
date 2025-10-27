package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private TextView tVWelcome;
    private Button btnLogout;
    private FirebaseAuth refAuth;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        tVWelcome = findViewById(R.id.tVWelcome);
        btnLogout = findViewById(R.id.btnLogout);

        refAuth = FirebaseAuth.getInstance();
        sharedPref = getSharedPreferences("MyPref", MODE_PRIVATE);

        FirebaseUser user = refAuth.getCurrentUser();
        if (user != null) {
            tVWelcome.setText("ברוך הבא " + user.getEmail());
        }

        btnLogout.setOnClickListener(v -> logoutUser());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void logoutUser() {
        // התנתקות מ-Firebase
        refAuth.signOut();

        // איפוס SharedPreferences
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("stayConnect", false);
        editor.apply();

        // חזרה ל-LoginActivity
        Intent si = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(si);
        finish();
    }
}
