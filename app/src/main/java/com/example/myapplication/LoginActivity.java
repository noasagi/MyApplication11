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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity { // כאן לא צריך BaseActivity כי אין תפריט בלוגין

    private EditText eTEmail, eTPass;
    private TextView tVMsg;
    private CheckBox cBStayConnect;
    private FirebaseAuth refAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPref;

    // 1. הוספת משתנה לקלאס העזר שלנו
    private UserHelper userHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // 2. אתחול UserHelper
        userHelper = new UserHelper(this);

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        cBStayConnect = findViewById(R.id.cBStayConnect);
        Button loginUser = findViewById(R.id.loginUser);
        Button btnGoToRegister = findViewById(R.id.btnGoToRegister);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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
            redirectUser(user.getUid());
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

                        if (user != null) {
                            redirectUser(user.getUid());
                        }
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

    private void redirectUser(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("type");

                        // *** 3. החלק החשוב: עדכון התפריט! ***
                        // אנחנו שומרים בזיכרון המקומי את הסוג שהגיע מ-Firebase
                        if (userType != null) {
                            userHelper.setRole(userType);
                        }
                        // *************************************

                        Intent intent;
                        // שימוש בקבועים מתוך UserHelper למניעת טעויות כתיב
                        if (UserHelper.ROLE_BUSINESS.equals(userType)) {
                            intent = new Intent(LoginActivity.this, BusinessMainActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, ClientMainActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        tVMsg.setText("User data not found.");
                    }
                })
                .addOnFailureListener(e -> tVMsg.setText("Error fetching user data: " + e.getMessage()));
    }
}