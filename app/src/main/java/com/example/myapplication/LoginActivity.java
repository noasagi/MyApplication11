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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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

// הגדרת המחלקה של מסך ההתחברות שיורשת מתכונות של מסך רגיל באנדרואיד
public class LoginActivity extends AppCompatActivity {

    // הצהרה על תיבות הטקסט שבהן המשתמש מקליד אימייל וסיסמה
    private EditText eTEmail, eTPass;

    // הצהרה על רכיבי טקסט להצגת הודעות שגיאה ולכפתור "שכחתי סיסמה"
    private TextView tVMsg, tvForgotPassword;

    // הצהרה על תיבת הסימון (וי) שמאפשרת למשתמש לבחור אם להישאר מחובר באפליקציה
    private CheckBox cBStayConnect;

    // הצהרה על כפתור ההתחברות דרך חשבון גוגל
    private Button btnGoogleLogin;

    // הצהרה על משתנה שמתקשר עם מערכת רישום ואימות המשתמשים של פיירבייס
    private FirebaseAuth refAuth;

    // הצהרה על משתנה שמתקשר עם בסיס הנתונים בענן (פיירסטור)
    private FirebaseFirestore db;

    // הצהרה על משתנה לשמירת נתונים קטנים בזיכרון המקומי של הטלפון
    private SharedPreferences sharedPref;

    // הצהרה על מחלקת העזר שלנו שמנהלת ומחזיקה את סוג המשתמש הנוכחי
    private UserHelper userHelper;

    // הצהרה על רכיב של גוגל שמנהל את תהליך בקשת החיבור לחשבון הגוגל
    private GoogleSignInClient mGoogleSignInClient;

    // הצהרה על רכיב שמקשיב ומקבל את התשובה שמסך בחירת החשבונות של גוגל מחזיר
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // פעולת המערכת הראשית שמופעלת ברגע שהמסך נוצר לראשונה
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // הפעלת תצוגה שמנצלת את כל שטח המסך של הטלפון (כולל אזור הסוללה והקליטה)
        EdgeToEdge.enable(this);
        // קישור המחלקה הזו לקובץ העיצוב הויזואלי שלה ב-XML
        setContentView(R.layout.activity_login);

        // יצירת מופע חדש של מחלקת העזר והעברת המסך הנוכחי אליה
        userHelper = new UserHelper(this);
        // קבלת הקישור והגישה למערכת האימות של פיירבייס
        refAuth = FirebaseAuth.getInstance();
        // קבלת הקישור והגישה לבסיס הנתונים פיירסטור
        db = FirebaseFirestore.getInstance();
        // הגדרת קובץ זיכרון מקומי בשם "MyPref" שיהיה פרטי ונגיש רק לאפליקציה הזו
        sharedPref = getSharedPreferences("MyPref", MODE_PRIVATE);

        // קישור משתני תיבות הטקסט לרכיבים הויזואליים האמיתיים שנמצאים בעיצוב ה-XML
        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        // קישור משתני הטקסט והכפתורים לרכיבים הויזואליים שנמצאים בעיצוב ה-XML
        tVMsg = findViewById(R.id.tVMsg);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cBStayConnect = findViewById(R.id.cBStayConnect);
        Button loginUser = findViewById(R.id.loginUser);
        Button btnGoToRegister = findViewById(R.id.btnGoToRegister);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        // הגדרת הגדרות החיבור של גוגל - מבקשים לקבל אישור מאובטח (Token) ואת כתובת המייל של המשתמש
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("784460475101-3si8ujd61vnj3s4nn9b0v9f24cn2jvh0.apps.googleusercontent.com")
                .requestEmail()
                .build();
        // אתחול רכיב החיבור של גוגל באמצעות ההגדרות שקבענו בשורה הקודמת
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // יצירת מאזין שמחכה לקבל את התשובה אחרי שהמשתמש בוחר חשבון גוגל במסך החיצוני
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // בדיקה האם המשתמש אכן אישר ובחר חשבון בהצלחה (ולא סגר את החלון באמצע)
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // לקיחת הנתונים שחזרו ממסך בחירת החשבונות
                            Intent data = result.getData();
                            // חילוץ החשבון שנבחר מתוך הנתונים שחזרו
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                // קבלת פרטי החשבון וקבלת קוד האישור המאובטח (IdToken)
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                // שליחת קוד האישור לפעולה שמחברת את המשתמש הזה לתוך פיירבייס
                                firebaseAuthWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                // במקרה של תקלה טכנית, נדפיס אותה בלוג של המחשב ונציג הודעה למשתמש
                                Log.e("GoogleLoginError", "Google sign in failed. Error Code: " + e.getStatusCode());
                                tVMsg.setText("שגיאה בהתחברות לגוגל. קוד: " + e.getStatusCode());
                            }
                        } else {
                            // במקרה שהמשתמש ביטל או יצא מהחלון של גוגל בלי לבחור
                            Log.e("GoogleLoginError", "Result Code is not OK. It is: " + result.getResultCode());
                            tVMsg.setText("הפעולה בוטלה או נכשלה במסך של גוגל.");
                        }
                    }
                }
        );

        // הגדרת האזנה ללחיצה על כפתור ההתחברות הרגיל
        loginUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // קריאה לפעולת ההתחברות שנמצאת בהמשך המחלקה
                loginUser(v);
            }
        });

        // הגדרת האזנה ללחיצה על כפתור המעבר למסך ההרשמה
        btnGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת כוונה (Intent) לעבור למסך ההרשמה (RegisterActivity) והפעלתו
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // הגדרת האזנה ללחיצה על כפתור "שכחתי סיסמה"
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // קריאה לפעולה שמציגה את חלון הזנת המייל לשחזור סיסמה
                showRecoverPasswordDialog();
            }
        });

        // הגדרת האזנה ללחיצה על כפתור ההתחברות המהירה של גוגל
        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת חלון בחירת החשבונות של גוגל והקפצתו על המסך למשתמש
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            }
        });

        // קוד מערכת שדואג שרכיבי המסך לא יתחבאו מאחורי פסי המערכת של הטלפון (כמו השעון או כפתורי הניווט)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new androidx.core.view.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            }
        });
    }

    // פעולת מערכת שמופעלת בכל פעם שהמסך הופך להיות גלוי למשתמש (אחרי onCreate)
    @Override
    protected void onStart() {
        super.onStart();
        // בדיקה בזיכרון המקומי האם המשתמש סימן בעבר את האפשרות "הישאר מחובר" (ברירת המחדל היא לא - false)
        boolean isChecked = sharedPref.getBoolean("stayConnect", false);
        // בדיקה מול פיירבייס האם יש משתמש שמחובר כרגע באפליקציה
        FirebaseUser user = refAuth.getCurrentUser();
        // אם נמצא משתמש מחובר וגם הוא סימן שהוא רוצה להישאר מחובר קבוע
        if (user != null && isChecked) {
            // נעביר אותו ישירות למסך הבית שלו בלי שיצטרך להקליד כלום מחדש
            redirectUser(user.getUid());
        }
    }

    // פעולה שמבצעת את תהליך ההתחברות הרגיל עם אימייל וסיסמה
    public void loginUser(View view) {
        // לקיחת הטקסט שהמשתמש הקליד בתיבת האימייל, והורדת רווחים מיותרים מהקצוות
        String email = eTEmail.getText().toString().trim();
        // לקיחת הטקסט שהמשתמש הקליד בתיבת הסיסמה, והורדת רווחים מיותרים מהקצוות
        String pass = eTPass.getText().toString().trim();

        // בדיקה האם אחד מהשדות הושאר ריק
        if (email.isEmpty() || pass.isEmpty()) {
            // הצגת הודעת אזהרה על המסך ועצירת הפעולה (לא ממשיכים להתחברות)
            tVMsg.setText("אנא מלא את כל השדות");
            return;
        }

        // יצירת חלונית המתנה קטנה שמודיעה למשתמש שהתהליך מתבצע
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("מתחבר");
        pd.setMessage("מבצע התחברות...");
        pd.show(); // הצגת החלונית על המסך

        // פנייה לפיירבייס לביצוע התחברות בעזרת האימייל והסיסמה שהוזנו
        refAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // העלמת חלונית ההמתנה ברגע שהתקבלה תשובה מהשרת
                        pd.dismiss();
                        // בדיקה האם תהליך ההתחברות הצליח בשרת
                        if (task.isSuccessful()) {
                            // קבלת המשתמש הנוכחי שהתחבר בהצלחה
                            FirebaseUser user = refAuth.getCurrentUser();
                            // פתיחת עורך לקובץ הזיכרון המקומי כדי לשמור את הבחירה של המשתמש
                            SharedPreferences.Editor editor = sharedPref.edit();
                            // שמירה האם תיבת הסימון "הישאר מחובר" מסומנת באותו רגע או לא
                            editor.putBoolean("stayConnect", cBStayConnect.isChecked());
                            editor.apply(); // אישור ושמירת הנתונים סופית בזיכרון המכשיר

                            // אם המשתמש קיים ותקין
                            if (user != null) {
                                // קריאה לפעולה שבודקת את סוג המשתמש ומעבירה אותו למסך הנכון
                                redirectUser(user.getUid());
                            }
                        } else {
                            // במקרה שההתחברות נכשלה, נבדוק מהי סיבת השגיאה כדי להציג הודעה מדויקת
                            Exception exp = task.getException();
                            if (exp instanceof FirebaseAuthInvalidUserException) {
                                // אם המייל לא רשום במערכת בכלל
                                tVMsg.setText("כתובת אימייל שגויה.");
                            } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                                // אם הסיסמה לא נכונה או לא תואמת למייל
                                tVMsg.setText("אימייל או סיסמה שגויים.");
                            } else if (exp instanceof FirebaseNetworkException) {
                                // אם יש בעיית קליטה או אין חיבור לאינטרנט בטלפון
                                tVMsg.setText("שגיאת רשת. אנא בדוק את החיבור שלך.");
                            } else {
                                // לכל תקלה לא צפויה אחרת
                                tVMsg.setText("אירעה שגיאה. אנא נסה שוב מאוחר יותר.");
                            }
                        }
                    }
                });
    }

    // פעולה שמקבלת את קוד האישור של גוגל ומבצעת איתו התחברות רשמית בתוך פיירבייס
    private void firebaseAuthWithGoogle(String idToken) {
        // יצירת חלונית המתנה בזמן שהחיבור מול גוגל מתבצע
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מתחבר עם גוגל...");
        pd.show();

        // יצירת אישור כניסה דיגיטלי לפיירבייס על בסיס קוד האישור שקיבלנו מחשבון הגוגל
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        // חיבור המשתמש לפיירבייס באמצעות האישור הדיגיטלי של גוגל
        refAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // סגירת חלונית ההמתנה
                        pd.dismiss();
                        // בדיקה האם החיבור הצליח
                        if (task.isSuccessful()) {
                            FirebaseUser user = refAuth.getCurrentUser();
                            // שמירת הבחירה לגבי "הישאר מחובר" גם עבור כניסה דרך גוגל
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean("stayConnect", cBStayConnect.isChecked());
                            editor.apply();

                            // קריאה לפעולה שבודקת אם המשתמש הזה כבר עשה הרשמה לאפליקציה בעבר
                            checkAndCreateGoogleUser(user);
                        } else {
                            // אם החיבור של פיירבייס מול גוגל נכשל
                            tVMsg.setText("ההתחברות דרך גוגל נכשלה.");
                        }
                    }
                });
    }

    // פעולה שבודקת האם למשתמש שנכנס עם גוגל יש כבר פרופיל קיים ומסמך בבסיס הנתונים פיירסטור
    private void checkAndCreateGoogleUser(final FirebaseUser user) {
        // אם המשתמש ריק, נעצור מיד ולא נמשיך
        if (user == null) return;

        // פנייה לאוסף המשתמשים "users" בבסיס הנתונים ובקשת המסמך שנושא את קוד הזיהוי (Uid) של המשתמש הזה
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        // בדיקה האם המסמך של המשתמש הזה באמת קיים בבסיס הנתונים
                        if (doc.exists()) {
                            // המשתמש רשום! נעביר אותו למסך הבית המתאים לו
                            redirectUser(user.getUid());
                        } else {
                            // המשתמש עשה כניסה עם גוגל אבל הוא מעולם לא נרשם לאפליקציה!
                            // ננתק אותו מיד ממערכת האימות כדי שלא ייכנס ללא פרופיל
                            refAuth.signOut();
                            // ננתק אותו גם מהרכיב של גוגל ונציג לו הודעה שעליו לעבור קודם למסך ההרשמה
                            mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    tVMsg.setText("אין לך חשבון קיים. אנא עבור למסך ההרשמה קודם.");
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // במקרה שיש שגיאה טכנית בשליפת המידע מהשרת
                        tVMsg.setText("שגיאה בבדיקת משתמש: " + e.getMessage());
                    }
                });
    }

    // פעולה שמקפיצה חלון דיאלוג (חלון קטן מעל המסך) המאפשר להקליד מייל לשחזור סיסמה
    private void showRecoverPasswordDialog() {
        // בניית חלון הדיאלוג והגדרת הכותרת וההסבר שלו
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("שחזור סיסמה");
        builder.setMessage("הכנס את כתובת האימייל שלך כדי לקבל קישור לאיפוס סיסמה:");

        // יצירת רכיב פריסה (לייאאוט) שיחזיק את תיבת ההקלדה בתוך החלון
        LinearLayout linearLayout = new LinearLayout(this);
        // יצירת תיבת הקלדה חדשה באופן ידני דרך הקוד עבור הדיאלוג
        final EditText etEmail = new EditText(this);
        etEmail.setHint("כתובת אימייל"); // טקסט רקע חלש בתוך התיבה
        // הגדרת המקלדת שתיפתח כמקלדת המיועדת להקלדת כתובות אימייל
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setMinEms(16); // הגדרת רוחב מינימלי לתיבת ההקלדה

        // הוספת תיבת ההקלדה לתוך הפריסה שיצרנו
        linearLayout.addView(etEmail);
        // הגדרת רווחים (שוליים פנימיים) מסביב לתיבה בתוך החלון
        linearLayout.setPadding(40, 20, 40, 20);
        // הגדרת הפריסה הזו כתוכן המרכזי של חלון הדיאלוג
        builder.setView(linearLayout);

        // הגדרת כפתור אישור ("שלח") לחלון הדיאלוג והגדרת מה יקרה בלחיצה עליו
        builder.setPositiveButton("שלח", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // לקיחת המייל שהמשתמש הקליד בחלון הקטן
                String email = etEmail.getText().toString().trim();
                // בדיקה שהתיבה לא ריקה
                if (!email.isEmpty()) {
                    // הפעלת הפעולה ששולחת את מייל השחזור בפועל
                    beginRecovery(email);
                } else {
                    // הצגת הודעה קופצת קצרה (Toast) שהשדה ריק
                    Toast.makeText(LoginActivity.this, "אנא הכנס כתובת אימייל", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // הגדרת כפתור ביטול לחלון הדיאלוג שפשוט סוגר ומעלים את החלון
        builder.setNegativeButton("ביטול", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // סגירת החלון
            }
        });

        // יצירת חלון הדיאלוג סופית והצגתו על המסך למשתמש
        builder.create().show();
    }

    // פעולה שפונה לפיירבייס ומבקשת לשלוח מייל לאיפוס סיסמה לכתובת שהתקבלה
    private void beginRecovery(String email) {
        // חלונית המתנה קטנה בזמן השליחה
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("שולח אימייל שחזור...");
        pd.show();

        // בקשה מפיירבייס לשלוח מייל איפוס סיסמה לכתובת המבוקשת
        refAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // סגירת חלונית ההמתנה
                        pd.dismiss();
                        // בדיקה האם השליחה הצליחה בשרת
                        if (task.isSuccessful()) {
                            // הצגת הודעה משמחת למשתמש שהמייל בדרך אליו
                            Toast.makeText(LoginActivity.this, "אימייל שחזור נשלח בהצלחה", Toast.LENGTH_LONG).show();
                        } else {
                            // במקרה של שגיאה, נציג את הודעת השגיאה שהשרת החזיר
                            Toast.makeText(LoginActivity.this, "שגיאה: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // פעולה קריטית שבודקת מה סוג המשתמש (type) בתוך בסיס הנתונים ומעבירה אותו למסך המתאים לו
    private void redirectUser(String uid) {
        // פנייה לאוסף המשתמשים ב-Firestore ושליפת המסמך של המשתמש הספציפי לפי ה-Uid שלו
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // בדיקה האם המסמך של המשתמש אכן קיים בבסיס הנתונים
                        if (documentSnapshot.exists()) {
                            // שליפת מחרוזת הטקסט שנמצאת תחת השדה בשם "type" (למשל "business" או "client")
                            String userType = documentSnapshot.getString("type");

                            // עדכון מחלקת העזר שלנו בסוג המשתמש שנמצא בשרת
                            if (userType != null) {
                                userHelper.setRole(userType);
                            } else {
                                // הגדרת ברירת מחדל כלקוח במקרה שהשדה משום מה ריק
                                userHelper.setRole(UserHelper.ROLE_CLIENT);
                            }

                            // יצירת משתנה מסוג כוונה (Intent) שיחזיק את מסך היעד שנפתח
                            Intent intent;
                            // בדיקה האם ערך השדה שווה לערך שמייצג בעל עסק במערכת
                            if (UserHelper.ROLE_BUSINESS.equals(userType)) {
                                // הגדרת מסך היעד כמסך הראשי של בעלי העסקים
                                intent = new Intent(LoginActivity.this, BusinessMainActivity.class);
                            } else {
                                // הגדרת מסך היעד כמסך הראשי של הלקוחות
                                intent = new Intent(LoginActivity.this, ClientMainActivity.class);
                            }
                            // הפעלת מסך היעד שנבחר ומעבר אליו בפועל
                            startActivity(intent);
                            // סגירת מסך ההתחברות הנוכחי כדי שלא יהיה ניתן לחזור אליו בלחיצה על כפתור "חזור" בטלפון
                            finish();
                        } else {
                            // אם המשתמש מחובר ב-Auth אבל משום מה אין לו מסמך תואם בבסיס הנתונים
                            tVMsg.setText("נתוני משתמש לא נמצאו.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // הצגת הודעת שגיאה במקרה של בעיית תקשורת מול שרתי בסיס הנתונים
                        tVMsg.setText("שגיאה בשליפת נתוני משתמש: " + e.getMessage());
                    }
                });
    }
}