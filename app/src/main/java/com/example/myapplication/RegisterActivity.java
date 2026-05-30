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

// הגדרת המחלקה של מסך ההרשמה שיורשת ממחלקת אקטיביטי בסיסית
public class RegisterActivity extends AppCompatActivity {

    // הצהרה על תיבת טקסט לקליטת כתובת האימייל של המשתמש
    private EditText eTEmail;

    // הצהרה על תיבת טקסט לקליטת הסיסמה של המשתמש
    private EditText eTPass;

    // הצהרה על רכיב טקסט להצגת הודעות שגיאה או הודעות מערכת
    private TextView tVMsg;

    // הצהרה על קבוצת כפתורי רדיו לבחירת תפקיד המשתמש
    private RadioGroup radioGroupType;

    // הצהרה על כפתור ההרשמה באמצעות חשבון גוגל
    private Button btnGoogleRegister;

    // הצהרה על עצם הקישור למערכת אימות המשתמשים של פיירבייס
    private FirebaseAuth refAuth;

    // הצהרה על עצם הקישור לבסיס הנתונים פיירסטור של פיירבייס
    private FirebaseFirestore db;

    // הצהרה על עצם שמנהל את תהליך החיבור והגדרות החשבון של גוגל
    private GoogleSignInClient mGoogleSignInClient;

    // הצהרה על רכיב שמקשיב ומטפל בתוצאה שחוזרת ממסך בחירת החשבונות החיצוני
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // משתנה מחרוזת לשמירה זמנית של סוג המשתמש שנבחר עבור רישום גוגל
    private String pendingRoleForGoogle = "";

    // פעולת המערכת הראשית שמתחילה ברגע יצירת המסך
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת עיצוב ה-XML הייעודי של מסך ההרשמה
        setContentView(R.layout.activity_register);

        // קישור משתנה האימייל לרכיב הויזואלי האמיתי בתוך ה-XML
        eTEmail = findViewById(R.id.eTEmail);
        // קישור משתנה הסיסמה לרכיב הויזואלי האמיתי בתוך ה-XML
        eTPass = findViewById(R.id.eTPass);
        // קישור משתנה הודעות המערכת לרכיב הויזואלי האמיתי בתוך ה-XML
        tVMsg = findViewById(R.id.tVMsg);
        // קישור משתנה קבוצת כפתורי הרדיו לרכיב הויזואלי האמיתי בתוך ה-XML
        radioGroupType = findViewById(R.id.radioGroupType);
        // קישור משתנה כפתור יצירת המשתמש הרגיל לרכיב הויזואלי האמיתי בתוך ה-XML
        Button createUser = findViewById(R.id.createUser);
        // קישור משתנה כפתור ההרשמה עם גוגל לרכיב הויזואלי האמיתי בתוך ה-XML
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);

        // אתחול וקבלת המופע הנוכחי של מערכת האימות פיירבייס לקוד
        refAuth = FirebaseAuth.getInstance();
        // אתחול וקבלת המופע הנוכחי של בסיס הנתונים פיירסטור לקוד
        db = FirebaseFirestore.getInstance();

        // בניית הגדרות בקשת החיבור של גוגל - קבלת אימייל וקוד זיהוי מאובטח
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("784460475101-3si8ujd61vnj3s4nn9b0v9f24cn2jvh0.apps.googleusercontent.com")
                .requestEmail()
                .build();
        // אתחול לקוח גוגל הרשמי עם ההגדרות שנקבעו בשורה הקודמת
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // רישום רכיב מאזין לתוצאה חוזרת ממסך בחירת חשבון גוגל
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    // פעולה המופעלת אוטומטית ברגע שהחלון החיצוני נסגר וחוזרים לאפליקציה
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // בדיקה האם בחירת החשבון במסך גוגל הסתיימה בהצלחה ללא שגיאות
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // שליפת הנתונים והכוונות שחזרו ממסך גוגל
                            Intent data = result.getData();
                            // חילוץ החשבון הספציפי שנבחר מתוך המידע שחזר
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                // קבלת העצם המלא של החשבון בהצלחה
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                // קריאה לפעולת האימות בשרת פיירבייס עם הטוקן המאובטח של החשבון
                                firebaseRegisterWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                // שמירת קוד השגיאה הטכני שחזר במקרה של תקלה
                                int statusCode = e.getStatusCode();
                                // הדפסת הודעת שגיאה מפורטת בלוגים של המחשב לצורכי בדיקה
                                Log.e("GoogleAuthError", "Google sign in failed. Code: " + statusCode);
                                // עדכון טקסט השגיאה במסך עבור המשתמש
                                tVMsg.setText("שגיאה בהרשמה לגוגל. קוד: " + statusCode);
                                // הקפצת הודעה קצרה על המסך עם קוד השגיאה
                                Toast.makeText(RegisterActivity.this, "שגיאת גוגל קוד: " + statusCode, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // הדפסת לוג המודיע כי קוד התוצאה שחזר אינו תקין
                            Log.e("GoogleAuthError", "Result Code is not OK: " + result.getResultCode());
                            // הצגת הודעה למשתמש שהפעולה בוטלה על ידו או נכשלה
                            tVMsg.setText("הפעולה בוטלה או נכשלה במסך של גוגל.");
                        }
                    }
                }
        );

        // הגדרת מאזין ללחיצות על כפתור הרישום הרגיל של המשתמש
        createUser.setOnClickListener(new View.OnClickListener() {
            // פעולה המופעלת בעת לחיצה על כפתור יצירת המשתמש
            @Override
            public void onClick(View v) {
                // קריאה לפעולה שמנהלת את יצירת החשבון הרגיל
                createUser(v);
            }
        });

        // הגדרת מאזין ללחיצות על כפתור הרישום המהיר דרך גוגל
        btnGoogleRegister.setOnClickListener(new View.OnClickListener() {
            // פעולה המופעלת בעת לחיצה על כפתור גוגל
            @Override
            public void onClick(View v) {
                // בדיקה מהו מזהה כפתור הרדיו המסומן כרגע מתוך הקבוצה
                int selectedId = radioGroupType.getCheckedRadioButtonId();
                // תנאי הבודק האם המשתמש שכח לבחור סוג משתמש (הערך מחזיר מינוס 1)
                if (selectedId == -1) {
                    // הצגת הודעת אזהרה למשתמש שעליו לבחור סוג חשבון
                    tVMsg.setText("אנא בחר סוג משתמש (עסק או לקוח) לפני ההרשמה...");
                    // עצירת המשך ביצוע הפעולה ויציאה ממנה
                    return;
                }

                // מציאת עצם כפתור הרדיו הספציפי שסומן בפועל על ידי ה-ID שלו
                RadioButton selectedRadio = findViewById(selectedId);
                // בדיקה האם כפתור הרדיו שסומן הוא של בעל עסק
                if (selectedRadio.getId() == R.id.rbBusiness) {
                    // שמירת סוג המשתמש כבעל עסק במשתנה הזמני
                    pendingRoleForGoogle = UserHelper.ROLE_BUSINESS;
                } else {
                    // שמירת סוג המשתמש כלקוח במשתנה הזמני במידה וזה לא עסק
                    pendingRoleForGoogle = UserHelper.ROLE_CLIENT;
                }

                // יצירת כוונת מעבר לפתיחת חלון בחירת החשבונות הרשמי של גוגל
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                // הפעלת החלון החיצוני והקפצתו על המסך בעזרת הרכיב המאזין
                googleSignInLauncher.launch(signInIntent);
            }
        });
    }

    // פעולת עזר פרטית הבודקת האם הסיסמה עומדת בתנאי החוזק שהגדרנו
    private boolean isPasswordValid(String password) {
        // בדיקה האם אורך הסיסמה קטן מ-8 תווים
        if (password.length() < 8) return false;
        // בדיקה האם קיימת לפחות אות אחת גדולה באנגלית בתוך מחרוזת הסיסמה
        boolean hasUppercase = !password.equals(password.toLowerCase());
        // בדיקה באמצעות ביטוי רגולרי האם הסיסמה מכילה תו מספרי כלשהו
        boolean hasNumber = password.matches(".*\\d.*");
        // החזרת תוצאה חיובית רק במידה וגם האות הגדולה וגם המספר קיימים
        return hasUppercase && hasNumber;
    }

    // פעולה המנהלת את יצירת החשבון הרגיל באמצעות אימייל וסיסמה
    public void createUser(View view) {
        // שליפת הטקסט מתיבת האימייל והסרת רווחים מיותרים מהקצוות
        final String email = eTEmail.getText().toString().trim();
        // שליפת הטקסט מתיבת הסיסמה והסרת רווחים מיותרים מהקצוות
        String pass = eTPass.getText().toString().trim();
        // קבלת המזהה של כפתור הרדיו שנבחר על ידי המשתמש
        int selectedId = radioGroupType.getCheckedRadioButtonId();

        // תנאי הבודק האם אחד השדות הושאר ריק או שלא נבחר כפתור רדיו
        if (email.isEmpty() || pass.isEmpty() || selectedId == -1) {
            // הצגת הודעה מתאימה למשתמש למילוי כל הפרטים הנדרשים
            tVMsg.setText("אנא מלא את כל השדות ובחר סוג משתמש");
            // יציאה מהפעולה ועצירת תהליך הרישום
            return;
        }

        // פנייה לפעולת העזר לבדיקת חוזק הסיסמה שנקלטה בקלט
        if (!isPasswordValid(pass)) {
            // הצגת הודעה למשתמש שהסיסמה חלשה מדי ופירוט התנאים
            tVMsg.setText("סיסמה חלשה: לפחות 8 תווים, אות גדולה ומספר");
            // עצירת תהליך הרישום ויציאה מהפעולה
            return;
        }

        // מציאת רכיב כפתור הרדיו שנבחר מתוך הממשק
        RadioButton selectedRadio = findViewById(selectedId);
        // קביעת מחרוזת סוג המשתמש בהתאם לכפתור הרדיו שסומן בפועל
        final String userType = (selectedRadio.getId() == R.id.rbBusiness) ? UserHelper.ROLE_BUSINESS : UserHelper.ROLE_CLIENT;

        // יצירת חלונית המתנה קופצת להצגת התקדמות התהליך למשתמש
        final ProgressDialog pd = new ProgressDialog(this);
        // הגדרת הטקסט שיוצג בתוך חלונית ההמתנה
        pd.setMessage("יוצר משתמש...");
        // הצגת חלונית ההמתנה על גבי המסך
        pd.show();

        // פנייה רשמית לשרת פיירבייס ליצירת המשתמש עם האימייל והסיסמה
        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    // פעולה המופעלת ברגע שהשרת מחזיר תשובה לגבי בקשת הרישום
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // העלמת חלונית ההמתנה הקופצת מהמסך
                        pd.dismiss();
                        // בדיקה האם בקשת יצירת החשבון הצליחה בשרת פיירבייס
                        if (task.isSuccessful()) {
                            // קריאה לפעולה ששומרת את פרופיל המשתמש בתוך מסד הנתונים פיירסטור
                            saveUserToFirestore(refAuth.getCurrentUser(), userType, email, null);
                        } else {
                            // הצגת הודעת השגיאה שהתקבלה מהשרת על גבי המסך למשתמש
                            tVMsg.setText("שגיאה: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // פעולה המבצעת את החיבור והאימות בפיירבייס באמצעות אישור החשבון מגוגל
    private void firebaseRegisterWithGoogle(String idToken) {
        // יצירת אישור כניסה רשמי לפיירבייס המבוסס על הטוקן שקיבלנו מחשבון הגוגל
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        // ביצוע כניסה לפיירבייס באמצעות אישור הכניסה הדיגיטלי
        refAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            // פעולה המופעלת עם סיום ניסיון החיבור בפיירבייס
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // בדיקה האם החיבור באמצעות חשבון הגוגל עבר בהצלחה
                if (task.isSuccessful()) {
                    // קריאה לפעולה שבודקת האם זהו משתמש חדש לחלוטין או משתמש קיים
                    checkIfNewGoogleUser(refAuth.getCurrentUser());
                } else {
                    // הצגת הודעת כישלון במידה והחיבור מול שרתי פיירבייס נכשל
                    tVMsg.setText("התחברות לגוגל נכשלה.");
                }
            }
        });
    }

    // פעולה פרטית לבדיקה האם למשתמש שנכנס עם גוגל קיים כבר מסמך במסד הנתונים
    private void checkIfNewGoogleUser(final FirebaseUser user) {
        // בדיקת בטיחות לוודא שעצם המשתמש אינו ריק
        if (user == null) return;

        // פנייה לאוסף המשתמשים ובקשת שליפת המסמך לפי ה-UID הייחודי של המשתמש
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            // פעולה המופעלת ברגע ששליפת המידע מהמסד הסתיימה בהצלחה
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                // תנאי הבודק האם המסמך של המשתמש הזה כבר קיים בפועל במסד הנתונים
                if (doc.exists()) {
                    // ניתוק המשתמש ממערכת האימות כדי למנוע כניסה ללא רישום תקין
                    refAuth.signOut();
                    // ניתוק המשתמש גם מרכיב החיבור המקומי של גוגל
                    mGoogleSignInClient.signOut();
                    // הצגת הודעה למשתמש שעליו לעבור למסך התחברות כי החשבון קיים
                    tVMsg.setText("חשבון כבר קיים. עברו למסך התחברות.");
                } else {
                    // במידה והמסמך לא קיים - זהו משתמש חדש, ונשמור את הפרופיל שלו במסד
                    saveUserToFirestore(user, pendingRoleForGoogle, user.getEmail(), user.getDisplayName());
                }
            }
        });
    }

    // פעולה פרטית האחראית על בניית מסמך המשתמש ושמירתו באוסף ב-Firestore
    private void saveUserToFirestore(FirebaseUser user, final String userType, String email, String name) {
        // יצירת מבנה נתונים של מפה לשמירת זוגות של שם שדה וערך עבור המסמך
        Map<String, Object> userData = new HashMap<>();
        // הוספת כתובת האימייל של המשתמש למפת הנתונים
        userData.put("email", email);
        // הוספת סוג המשתמש (בעל עסק או לקוח) למפת הנתונים
        userData.put("type", userType);

        // תנאי הבודק האם התקבל שם מלא (רלוונטי בעיקר בהרשמה דרך גוגל)
        if (name != null) {
            // הוספת השם המלא של המשתמש למפת הנתונים
            userData.put("name", name);
        }

        // פנייה ל-Firestore ויצירת מסמך חדש שהמזהה שלו הוא ה-UID של המשתמש
        db.collection("users").document(user.getUid()).set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    // פעולה המופעלת ברגע ששמירת המסמך בשרת הסתיימה בהצלחה מלאה
                    @Override
                    public void onSuccess(Void aVoid) {
                        // יצירת מופע של מחלקת העזר ועדכון סוג המשתמש בתוכה
                        new UserHelper(RegisterActivity.this).setRole(userType);

                        // הצהרה על כוונת מעבר למסך הבא
                        Intent intent;
                        // בדיקה האם המשתמש שנרשם כעת מוגדר כבעל עסק במערכת
                        if (userType.equals(UserHelper.ROLE_BUSINESS)) {
                            // הגדרת יעד המעבר למסך הראשי של בעלי העסקים
                            intent = new Intent(RegisterActivity.this, BusinessMainActivity.class);
                        } else {
                            // הגדרת יעד המעבר למסך הראשי של הלקוחות הרגילים
                            intent = new Intent(RegisterActivity.this, ClientMainActivity.class);
                        }

                        // הפעלת כוונת המעבר ומעבר בפועל למסך הבית שנבחר
                        startActivity(intent);
                        // סגירת מסך ההרשמה הנוכחי והסרתו מהמחסנית של המכשיר
                        finish();
                    }
                });
    }
}