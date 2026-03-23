package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText eTEmail, eTPass;
    private TextView tVMsg, tvForgotPassword;
    private CheckBox cBStayConnect;
    private Button btnGoogleLogin;

    private FirebaseAuth refAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPref;
    private UserHelper userHelper;

    // משתנים עבור התחברות עם גוגל
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        userHelper = new UserHelper(this);
        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPref = getSharedPreferences("MyPref", MODE_PRIVATE);

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cBStayConnect = findViewById(R.id.cBStayConnect);
        Button loginUser = findViewById(R.id.loginUser);
        Button btnGoToRegister = findViewById(R.id.btnGoToRegister);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        // --- הגדרת גוגל Sign-In ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("        784460475101-3si8ujd61vnj3s4nn9b0v9f24cn2jvh0.apps.googleusercontent.com\n")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // מאזין לתוצאה של חלונית גוגל
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            tVMsg.setText("שגיאה בהתחברות לגוגל.");
                            Log.w("LoginActivity", "Google sign in failed", e);
                        }
                    }
                }
        );

        // לחיצות כפתורים
        loginUser.setOnClickListener(this::loginUser);
        btnGoToRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> showRecoverPasswordDialog());
        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
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

    // --- התחברות רגילה (אימייל וסיסמה) ---
    public void loginUser(View view) {
        String email = eTEmail.getText().toString().trim();
        String pass = eTPass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("אנא מלא את כל השדות");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("מתחבר");
        pd.setMessage("מבצע התחברות...");
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
                            tVMsg.setText("כתובת אימייל שגויה.");
                        } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                            tVMsg.setText("אימייל או סיסמה שגויים.");
                        } else if (exp instanceof FirebaseNetworkException) {
                            tVMsg.setText("שגיאת רשת. אנא בדוק את החיבור שלך.");
                        } else {
                            tVMsg.setText("אירעה שגיאה. אנא נסה שוב מאוחר יותר.");
                        }
                    }
                });
    }

    // --- התחברות עם גוגל בפיירבייס ---
    private void firebaseAuthWithGoogle(String idToken) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מתחבר עם גוגל...");
        pd.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        refAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = refAuth.getCurrentUser();
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("stayConnect", cBStayConnect.isChecked());
                        editor.apply();

                        checkAndCreateGoogleUser(user);
                    } else {
                        tVMsg.setText("ההתחברות דרך גוגל נכשלה.");
                    }
                });
    }

    // בודק אם המשתמש כבר במסד הנתונים, ואם לא - יוצר אותו כלקוח
// בודק אם המשתמש כבר במסד הנתונים. אם לא - חוסם את ההתחברות ומפנה להרשמה.
    private void checkAndCreateGoogleUser(FirebaseUser user) {
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // מעולה, המשתמש קיים במסד הנתונים - נכניס אותו פנימה
                        redirectUser(user.getUid());
                    } else {
                        // המשתמש לא קיים!
                        // ננתק אותו מפיירבייס ומגוגל, כדי שלא יישאר מחובר "באוויר"
                        refAuth.signOut();
                        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                            tVMsg.setText("אין לך חשבון קיים. אנא עבור למסך ההרשמה קודם.");
                        });
                    }
                })
                .addOnFailureListener(e -> tVMsg.setText("שגיאה בבדיקת משתמש: " + e.getMessage()));
    }

    // --- שחזור סיסמה ---
    private void showRecoverPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("שחזור סיסמה");
        builder.setMessage("הכנס את כתובת האימייל שלך כדי לקבל קישור לאיפוס סיסמה:");

        LinearLayout linearLayout = new LinearLayout(this);
        final EditText etEmail = new EditText(this);
        etEmail.setHint("כתובת אימייל");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setMinEms(16);

        linearLayout.addView(etEmail);
        linearLayout.setPadding(40, 20, 40, 20);
        builder.setView(linearLayout);

        builder.setPositiveButton("שלח", (dialog, which) -> {
            String email = etEmail.getText().toString().trim();
            if (!email.isEmpty()) {
                beginRecovery(email);
            } else {
                Toast.makeText(this, "אנא הכנס כתובת אימייל", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void beginRecovery(String email) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("שולח אימייל שחזור...");
        pd.show();

        refAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "אימייל שחזור נשלח בהצלחה", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "שגיאה: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- ניתוב משתמשים ---
    private void redirectUser(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("type");

                        if (userType != null) {
                            userHelper.setRole(userType);
                        } else {
                            userHelper.setRole(UserHelper.ROLE_CLIENT);
                        }

                        Intent intent;
                        if (UserHelper.ROLE_BUSINESS.equals(userType)) {
                            intent = new Intent(LoginActivity.this, BusinessMainActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, ClientMainActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        tVMsg.setText("נתוני משתמש לא נמצאו.");
                    }
                })
                .addOnFailureListener(e -> tVMsg.setText("שגיאה בשליפת נתוני משתמש: " + e.getMessage()));
    }
}