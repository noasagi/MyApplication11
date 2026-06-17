package com.example.myapplication;

import android.app.Activity;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// מחלקת אקטיביטי המנהלת את מסך רישום המשתמשים (הן רגיל והן באמצעות צד שלישי - Google)
public class RegisterActivity extends AppCompatActivity {

    private EditText eTEmail;
    private EditText eTPass;
    private TextView tVMsg;
    private RadioGroup radioGroupType;
    private Button btnGoogleRegister;

    // רכיבי ליבה של פיירבייס: אימות משתמשים (Auth) ומסד נתונים NoSQL (Firestore)
    private FirebaseAuth refAuth;
    private FirebaseFirestore db;

    // רכיבי התשתית של גוגל לניהול חלון בחירת החשבונות וקבלת האסימון הדיגיטלי (Token)
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private String pendingRoleForGoogle = ""; // משתנה עזר לשמירת סוג המשתמש שנבחר לפני היציאה לחלון של גוגל

    /**
     * מה הפעולה עושה: מאתחלת את רכיבי המסך, מגדירה את לקוח ה-Google Sign-In עם טוקן שרת ייחודי, ומכינה את ה-Launcher לקליטת התוצאה מחלון הבחירה של גוגל.
     * קלט: Bundle savedInstanceState.
     * פלט: אין.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        radioGroupType = findViewById(R.id.radioGroupType);
        Button createUser = findViewById(R.id.createUser);
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // הגדרת אפשרויות ההתחברות של גוגל: מבקשים גישה לאימייל וכן מקבלים מפתח אבטחה (ID Token) לשרת הפיירבייס
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("784460475101-3si8ujd61vnj3s4nn9b0v9f24cn2jvh0.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // אתחול ה-Launcher שממתין לחזרת המשתמש מחלון בחירת החשבונות החיצוני של גוגל
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                // חילוץ מוצלח של חשבון הגוגל שנבחר והעברת ה-Token שלו להמשך אימות בפיירבייס
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                firebaseRegisterWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                int statusCode = e.getStatusCode();
                                Log.e("GoogleAuthError", "Google sign in failed. Code: " + statusCode);
                                tVMsg.setText("שגיאה בהרשמה לגוגל. קוד: " + statusCode);
                            }
                        } else {
                            tVMsg.setText("הפעולה בבוטלה או נכשלה במסך של גוגל.");
                        }
                    }
                }
        );

        createUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser(v);
            }
        });

        btnGoogleRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // בדיקת תנאי הגנה: המשתמש חייב לבחור סוג חשבון (Radio Button) לפני שנאפשר לו להירשם עם גוגל
                int selectedId = radioGroupType.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    tVMsg.setText("אנא בחר סוג משתמש (עסק או לקוח) לפני ההרשמה...");
                    return;
                }

                RadioButton selectedRadio = findViewById(selectedId);
                if (selectedRadio.getId() == R.id.rbBusiness) {
                    pendingRoleForGoogle = UserHelper.ROLE_BUSINESS;
                } else {
                    pendingRoleForGoogle = UserHelper.ROLE_CLIENT;
                }

                // הפעלת חלון הבחירה המאובטח של גוגל
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            }
        });
    }

    /**
     * מה הפעולה עושה: בדיקת תקינות (Validation) וחוזק סיסמה מקומית בקוד הג'אווה לפני פנייה לשרת (חוסך תעבורת רשת מיותרת).
     * קלט: String password.
     * פלט: boolean (true אם עומדת בתנאים: אורך 8, אות גדולה ומספר, false אחרת).
     */
    private boolean isPasswordValid(String password) {
        if (password.length() < 8) return false;
        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasNumber = password.matches(".*\\d.*"); // שימוש בביטוי רגולרי (Regex) לבדיקת נוכחות ספרה
        return hasUppercase && hasNumber;
    }

    /**
     * מה הפעולה עושה: אוספת את נתוני הקלט (אימייל וסיסמה), מוודאת תקינות, ומבצעת רישום אסינכרוני מול שרת ה-Authentication של פיירבייס.
     * קלט: View view.
     * פלט: אין (void).
     */
    public void createUser(View view) {
        final String email = eTEmail.getText().toString().trim();
        String pass = eTPass.getText().toString().trim();
        int selectedId = radioGroupType.getCheckedRadioButtonId();

        if (email.isEmpty() || pass.isEmpty() || selectedId == -1) {
            tVMsg.setText("אנא מלא את כל השדות ובחר סוג משתמש");
            return;
        }

        if (!isPasswordValid(pass)) {
            tVMsg.setText("סיסמה חלשה: לפחות 8 תווים, אות גדולה ומספר");
            return;
        }

        RadioButton selectedRadio = findViewById(selectedId);
        final String userType = (selectedRadio.getId() == R.id.rbBusiness) ? UserHelper.ROLE_BUSINESS : UserHelper.ROLE_CLIENT;

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("יוצר משתמש...");
        pd.show();

        // קריאה אסינכרונית ליצירת המשתמש בפיירבייס
        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            // שלב ב': ברגע שהמשתמש נוצר ב-Auth, ניגש לשמור את הפרופיל שלו ב-Firestore
                            saveUserToFirestore(refAuth.getCurrentUser(), userType, email, null);
                        } else {
                            tVMsg.setText("שגיאה: " + task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * מה הפעולה עושה: ממירה את ה-ID Token שהתקבל מחשבון הגוגל החיצוני לכדי אישור דיגיטלי (AuthCredential) המוכר על ידי שרתי פיירבייס.
     * קלט: String idToken.
     * פלט: אין (void).
     */
    private void firebaseRegisterWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        refAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // שלב קריטי: בדיקה האם המשתמש שבחר בגוגל הוא חדש לגמרי או שהוא כבר קיים במערכת
                    checkIfNewGoogleUser(refAuth.getCurrentUser());
                } else {
                    tVMsg.setText("התחברות לגוגל נכשלה.");
                }
            }
        });
    }

    /**
     * מה הפעולה עושה: ניגשת לאוסף המשתמשים ב-Firestore ובודקת האם קיים כבר מסמך עבור ה-UID הזה. מונעת יצירה כפולה ודריסת נתונים למשתמשי גוגל קיימים.
     * קלט: final FirebaseUser user.
     * פלט: אין (void).
     */
    private void checkIfNewGoogleUser(final FirebaseUser user) {
        if (user == null) return;

        db.collection("users").document(user.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                if (doc.exists()) {
                    // מנגנון הגנה: החשבון כבר קיים במסד, לכן ננתק אותו ונבקש ממנו לעבור למסך לוגין הרגיל
                    refAuth.signOut();
                    mGoogleSignInClient.signOut();
                    tVMsg.setText("חשבון כבר קיים. עברו למסך התחברות.");
                } else {
                    // המשתמש אכן חדש לחלוטין - נמשיך לשלב השמירה והגדרת התפקיד שלו
                    saveUserToFirestore(user, pendingRoleForGoogle, user.getEmail(), user.getDisplayName());
                }
            }
        });
    }

    /**
     * מה הפעולה עושה: מייצרת מסמך פרופיל משתמש באוסף "users" ב-Firestore, מעדכנת את ה-SharedPreferences המקומי ומנתבת את המשתמש למסך הבית המתאים לתפקידו.
     * קלט: FirebaseUser user, final String userType, String email, String name.
     * פלט: אין (void).
     */
    private void saveUserToFirestore(FirebaseUser user, final String userType, String email, String name) {
        // בניית מפת נתונים (Key-Value) המייצגת את שדות המסמך בבסיס הנתונים NoSQL
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("type", userType);

        if (name != null) {
            userData.put("name", name);
        }

        // שימוש ב-UID הייחודי של פיירבייס כמפתח הראשי (Document ID) של המסמך ב-Firestore לאחידות מלאה
        db.collection("users").document(user.getUid()).set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // שמירת תפקיד המשתמש מקומית במכשיר באמצעות מחלקת עזר (SharedPreferences)
                        new UserHelper(RegisterActivity.this).setRole(userType);

                        // ניתוב ארכיטקטוני (Intent) בהתאם לסוג החשבון שנרשם
                        Intent intent;
                        if (userType.equals(UserHelper.ROLE_BUSINESS)) {
                            intent = new Intent(RegisterActivity.this, BusinessMainActivity.class);
                        } else {
                            intent = new Intent(RegisterActivity.this, ClientMainActivity.class);
                        }

                        startActivity(intent);
                        finish(); // סגירת מסך הרישום הנוכחי כדי שלא יוכל לחזור אליו בלחיצה על כפתור "חזור" במכשיר
                    }
                });
    }
}