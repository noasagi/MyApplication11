package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText eTEmail, eTPass;
    private TextView tVMsg, tvForgotPassword;
    private android.widget.CheckBox cBStayConnect;
    private Button btnGoogleLogin;

    private FirebaseAuth refAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPref;
    private UserHelper userHelper;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי הממשק, מגדירה את מנגנוני הגישה לפיירבייס, ומגדירה את ה-Launcher לקבלת תוצאת התחברות חיצונית מחשבון Google.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
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

        // הגדרת תצורת אימות מול Google API (קבלת מזהה מאובטח וכתובת מייל)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("784460475101-3si8ujd61vnj3s4nn9b0v9f24cn2jvh0.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // שימוש ב-ActivityResultLauncher המודרני במקום בשיטה המיושנת של onActivityResult
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                firebaseAuthWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                Log.e("GoogleLoginError", "Google sign in failed: " + e.getStatusCode());
                                tVMsg.setText("שגיאה בהתחברות לגוגל.");
                            }
                        }
                    }
                }
        );

        loginUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser(v);
            }
        });

        btnGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });

        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            }
        });
    }

    /**
     * מה הפעולה עושה: מנגנון אוטומטי (Persistence) הבודק עם עליית המסך האם המשתמש כבר מחובר, והאם דגל השמירה המקומי קיים. במידה וכן - מדלג ישירות למסך הבית.
     * קלט: אין.
     * פלט: אין (void).
     */
    @Override
    protected void onStart() {
        super.onStart();
        boolean isChecked = sharedPref.getBoolean("stayConnect", false);
        FirebaseUser user = refAuth.getCurrentUser();
        if (user != null && isChecked) {
            redirectUser(user.getUid()); // מעבר מהיר מבוסס מזהה משתמש ייחודי
        }
    }

    /**
     * מה הפעולה עושה: מאמתת את פרטי הקלט, מבצעת התחברות אסינכרונית מול Firebase Authentication, ומנהלת טיפול בשגיאות קצה נפוצות (רשת, סיסמה שגויה).
     * קלט: View view.
     * פלט: אין (void).
     */
    public void loginUser(View view) {
        String email = eTEmail.getText().toString().trim();
        String pass = eTPass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("אנא מלא את כל השדות");
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מבצע התחברות...");
        pd.show();

        refAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            FirebaseUser user = refAuth.getCurrentUser();

                            // שמירת מצב ההתחברות הקבוע בזיכרון קטן לטווח ארוך (SharedPreferences)
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean("stayConnect", cBStayConnect.isChecked());
                            editor.apply();

                            if (user != null) {
                                redirectUser(user.getUid());
                            }
                        } else {
                            // פולימורפיזם וניהול חריגות מדויק לפי סוג השגיאה שחזרה מהשרת
                            Exception exp = task.getException();
                            if (exp instanceof FirebaseAuthInvalidUserException) {
                                tVMsg.setText("כתובת אימייל שגויה או לא קיימת.");
                            } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                                tVMsg.setText("אימייל או סיסמה שגויים.");
                            } else if (exp instanceof FirebaseNetworkException) {
                                tVMsg.setText("שגיאת רשת. אנא בדוק את החיבור שלך.");
                            } else {
                                tVMsg.setText("אירעה שגיאה. נסה שוב מאוחר יותר.");
                            }
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: מקבלת את ה-Token הדיגיטלי המאובטח מחשבון ה-Google, וממירה אותו ל-AuthCredential לטובת חיבור מול שרתי ה-Firebase.
     * קלט: String idToken.
     * פלט: אין (void).
     */
    private void firebaseAuthWithGoogle(String idToken) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מתחבר עם גוגל...");
        pd.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        refAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
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
                    }
                });
    }

    /**
     * מה הפעולה עושה: מנגנון הגנה הבודק האם המשתמש שהתחבר דרך גוגל כבר ביצע רישום פרופיל מלא ב-Firestore (קיום מסמך). במידה ולא - מנתק אותו מיידית.
     * קלט: final FirebaseUser user.
     * פלט: אין (void).
     */
    private void checkAndCreateGoogleUser(final FirebaseUser user) {
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (doc.exists()) {
                            redirectUser(user.getUid()); // משתמש קיים ורשום כחוק
                        } else {
                            // הגנה: מניעת כניסה למשתמש מחובר ללא מסמך מודל תואם במסד הנתונים
                            refAuth.signOut();
                            mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    tVMsg.setText("אין לך חשבון קיים. אנא עבור למסך ההרשמה.");
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        tVMsg.setText("שגיאה בבדיקת משתמש: " + e.getMessage());
                    }
                });
    }

    /**
     * מה הפעולה עושה: מייצרת דיאלוג קופץ (AlertDialog) דינמי הכולל תיבת הזנת טקסט עצמאית, לטובת איסוף כתובת מייל לשחזור.
     * קלט: אין.
     * פלט: אין (void).
     */
    private void showRecoverPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("שחזור סיסמה");
        builder.setMessage("הכנס את כתובת האימייל שלך לקבלת קישור לאיפוס:");

        LinearLayout linearLayout = new LinearLayout(this);
        final EditText etEmail = new EditText(this);
        etEmail.setHint("כתובת אימייל");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setMinEms(16);

        linearLayout.addView(etEmail);
        linearLayout.setPadding(40, 20, 40, 20);
        builder.setView(linearLayout);

        builder.setPositiveButton("שלח", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = etEmail.getText().toString().trim();
                if (!email.isEmpty()) {
                    beginRecovery(email);
                } else {
                    Toast.makeText(LoginActivity.this, "אנא הכנס כתובת אימייל", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("ביטול", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void beginRecovery(String email) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("שולח אימייל שחזור...");
        pd.show();

        refAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "אימייל שחזור נשלח בהצלחה", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "שגיאה: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: שולפת את שדה התפקיד (type) מתוך מסמך המשתמש ב-Firestore, מעדכנת את ה-UserHelper המקומי, ומנתבת את המשתמש באמצעות Intent לאקטיביטי המתאים (ClientMainActivity או BusinessMainActivity).
     * קלט: String uid.
     * פלט: אין (void).
     */
    private void redirectUser(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String userType = documentSnapshot.getString("type");

                            if (userType != null) {
                                userHelper.setRole(userType);
                            } else {
                                userHelper.setRole(UserHelper.ROLE_CLIENT); // ברירת מחדל בטוחה
                            }

                            // מנגנון הניתוח והחיווט הלוגי (Routing) של משתמשי המערכת
                            Intent intent;
                            if (UserHelper.ROLE_BUSINESS.equals(userType)) {
                                intent = new Intent(LoginActivity.this, BusinessMainActivity.class);
                            } else {
                                intent = new Intent(LoginActivity.this, ClientMainActivity.class);
                            }
                            startActivity(intent);
                            finish(); // סגירת אקטיביטי הנוכחי כדי להוציאו ממחסנית המסכים (Backstack)
                        } else {
                            tVMsg.setText("נתוני משתמש לא נמצאו במסד.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        tVMsg.setText("שגיאה בשליפת נתוני משתמש: " + e.getMessage());
                    }
                });
    }
}