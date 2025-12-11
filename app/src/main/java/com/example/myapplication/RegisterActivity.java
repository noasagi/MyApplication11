package com.example.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText eTEmail, eTPass;
    private TextView tVMsg;
    private RadioGroup radioGroupType;
    private FirebaseAuth refAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        radioGroupType = findViewById(R.id.radioGroupType);
        Button createUser = findViewById(R.id.createUser);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        createUser.setOnClickListener(this::createUser);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void createUser(View view) {
        String email = eTEmail.getText().toString().trim();
        String pass  = eTPass.getText().toString().trim();

        int selectedId = radioGroupType.getCheckedRadioButtonId();

        // ✅ קודם כל – בדיקת ולידציה
        if (email.isEmpty() || pass.isEmpty() || selectedId == -1) {
            tVMsg.setText("אנא מלא את כל השדות ובחר סוג משתמש");
            return;
        }

        // לוקחים את הרדיובטון שנבחר
        RadioButton selectedRadio = findViewById(selectedId);

        // במקום לשמור טקסט בעברית – נשמור role עקבי
        String userType;
        if (selectedRadio.getId() == R.id.rbBusiness) { // תוודאי שה-id ב-XML ככה
            userType = UserHelper.ROLE_BUSINESS; // "business"
        } else {
            userType = UserHelper.ROLE_CLIENT;   // "client"
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Creating user...");
        pd.show();

        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = refAuth.getCurrentUser();
                        if (user != null) {

                            // נשמור את המשתמש גם ב-Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("type", userType);

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();

                                        // נשמור גם ב-SharedPreferences דרך UserHelper
                                        UserHelper helper = new UserHelper(this);
                                        helper.setRole(userType);

                                        // ניווט למסך מתאים
                                        Intent intent;
                                        if (UserHelper.ROLE_BUSINESS.equals(userType)) {
                                            intent = new Intent(RegisterActivity.this, BusinessMainActivity.class);
                                        } else {
                                            intent = new Intent(RegisterActivity.this, ClientMainActivity.class);
                                        }
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        tVMsg.setText("שגיאה בשמירת הנתונים: " + e.getMessage());
                                    });
                        }
                    } else {
                        Exception exp = task.getException();
                        if (exp instanceof FirebaseAuthWeakPasswordException) {
                            tVMsg.setText("סיסמה חלשה מדי");
                        } else if (exp instanceof FirebaseAuthUserCollisionException) {
                            tVMsg.setText("המשתמש כבר קיים");
                        } else if (exp instanceof FirebaseNetworkException) {
                            tVMsg.setText("שגיאת רשת");
                        } else {
                            tVMsg.setText("שגיאה לא צפויה");
                        }
                        Log.w("RegisterActivity", "createUserWithEmailAndPassword: failure", exp);
                    }
                });
    }
}
