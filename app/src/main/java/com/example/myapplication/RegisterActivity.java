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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText eTEmail, eTPass;
    private TextView tVMsg;
    private RadioGroup radioGroupType;
    private Button btnGoogleRegister;

    private FirebaseAuth refAuth;
    private FirebaseFirestore db;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private String pendingRoleForGoogle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ביטלנו את ה-EdgeToEdge הבעייתי שגרם לקריסה
        setContentView(R.layout.activity_register);

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        radioGroupType = findViewById(R.id.radioGroupType);
        Button createUser = findViewById(R.id.createUser);
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // הגדרת גוגל - תדביקי כאן שוב את ה-ID שלך אם הוא שונה מהקודם
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("784460475101-3si8ujd61vnj3s4nn9b0v9f24cn2jvh0.apps.googleusercontent.com") // כאן להדביק את ה-ID
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseRegisterWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            // כאן הוספנו את ההדפסה של קוד השגיאה!
                            int statusCode = e.getStatusCode();
                            Log.e("GoogleAuthError", "Google sign in failed. Error Code: " + statusCode);
                            tVMsg.setText("שגיאה בהרשמה לגוגל. קוד: " + statusCode);
                            Toast.makeText(this, "שגיאת גוגל קוד: " + statusCode, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e("GoogleAuthError", "Result Code is not OK. It is: " + result.getResultCode());
                        tVMsg.setText("הפעולה בוטלה או נכשלה במסך של גוגל.");
                    }
                }
        );

        createUser.setOnClickListener(this::createUser);

        btnGoogleRegister.setOnClickListener(v -> {
            int selectedId = radioGroupType.getCheckedRadioButtonId();
            if (selectedId == -1) {
                tVMsg.setText("אנא בחר סוג משתמש (עסק או לקוח) לפני ההרשמה עם גוגל.");
                return;
            }
            RadioButton selectedRadio = findViewById(selectedId);
            if (selectedRadio.getId() == R.id.rbBusiness) {
                pendingRoleForGoogle = UserHelper.ROLE_BUSINESS;
            } else {
                pendingRoleForGoogle = UserHelper.ROLE_CLIENT;
            }
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private boolean isPasswordValid(String password) {
        if (password.length() < 8) return false;
        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasNumber = password.matches(".*\\d.*");
        return hasUppercase && hasNumber;
    }

    public void createUser(View view) {
        String email = eTEmail.getText().toString().trim();
        String pass  = eTPass.getText().toString().trim();
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
        String userType = (selectedRadio.getId() == R.id.rbBusiness) ? UserHelper.ROLE_BUSINESS : UserHelper.ROLE_CLIENT;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("יוצר משתמש...");
        pd.show();

        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        saveUserToFirestore(refAuth.getCurrentUser(), userType, email, null);
                    } else {
                        tVMsg.setText("שגיאה: " + task.getException().getMessage());
                    }
                });
    }

    private void firebaseRegisterWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        refAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                checkIfNewGoogleUser(refAuth.getCurrentUser());
            } else {
                tVMsg.setText("התחברות לגוגל נכשלה.");
            }
        });
    }

    private void checkIfNewGoogleUser(FirebaseUser user) {
        if (user == null) return;
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                refAuth.signOut();
                mGoogleSignInClient.signOut();
                tVMsg.setText("חשבון כבר קיים. עברו למסך התחברות.");
            } else {
                saveUserToFirestore(user, pendingRoleForGoogle, user.getEmail(), user.getDisplayName());
            }
        });
    }

    private void saveUserToFirestore(FirebaseUser user, String userType, String email, String name) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("type", userType);
        if (name != null) userData.put("name", name);

        db.collection("users").document(user.getUid()).set(userData).addOnSuccessListener(aVoid -> {
            new UserHelper(this).setRole(userType);
            Intent intent = userType.equals(UserHelper.ROLE_BUSINESS) ?
                    new Intent(this, BusinessMainActivity.class) : new Intent(this, ClientMainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}