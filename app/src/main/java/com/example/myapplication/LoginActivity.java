package com.example.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText eTEmail, eTPass;
    private TextView tVMsg;
    private CheckBox cBStayConnect;
    private FirebaseAuth refAuth;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        cBStayConnect = findViewById(R.id.cBStayConnect);
        Button loginUser = findViewById(R.id.loginUser);
        Button btnGoToRegister = findViewById(R.id.btnGoToRegister); // כפתור הרשמה חדש

        refAuth = FirebaseAuth.getInstance();
        sharedPref = getSharedPreferences("MyPref", MODE_PRIVATE);

        loginUser.setOnClickListener(this::loginUser);

        btnGoToRegister.setOnClickListener(v -> {
            Intent si = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(si);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean isChecked = sharedPref.getBoolean("stayConnect", false);
        FirebaseUser user = refAuth.getCurrentUser();
        if (user != null && isChecked) {
            Intent si = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(si);
            finish();
        }
    }

    public void loginUser(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();

        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("Please fill all fields");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Logging in user...");
        pd.show();

        refAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = refAuth.getCurrentUser();
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("stayConnect", cBStayConnect.isChecked());
                        editor.apply();

                        Intent si = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(si);
                        finish();
                    } else {
                        Exception exp = task.getException();
                        if (exp instanceof FirebaseAuthInvalidUserException) {
                            tVMsg.setText("Invalid email address.");
                        } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                            tVMsg.setText("Invalid email or password.");
                        } else if (exp instanceof FirebaseNetworkException) {
                            tVMsg.setText("Network error. Please check your connection.");
                        } else {
                            tVMsg.setText("An error occurred. Please try again later.");
                        }
                        Log.w("LoginActivity", "signInWithEmailAndPassword: failure", exp);
                    }
                });
    }
}
