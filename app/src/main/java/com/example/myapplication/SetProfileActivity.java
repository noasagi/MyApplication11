package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SetProfileActivity extends AppCompatActivity {

    private EditText eTName, eTBirthDate, eTAddress, eTPhone;
    private TextView tVMsg;
    private Button btnSaveProfile;

    private FirebaseAuth refAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_profile);

        eTName = findViewById(R.id.eTName);
        eTBirthDate = findViewById(R.id.eTBirthDate);
        eTAddress = findViewById(R.id.eTAddress);
        eTPhone = findViewById(R.id.eTPhone);
        tVMsg = findViewById(R.id.tVMsg);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        eTBirthDate.setOnClickListener(v -> showDatePicker());

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    eTBirthDate.setText(date);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void saveProfile() {
        String name = eTName.getText().toString();
        String birthDate = eTBirthDate.getText().toString();
        String address = eTAddress.getText().toString();
        String phone = eTPhone.getText().toString();

        if(name.isEmpty() || birthDate.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            tVMsg.setText("אנא מלא את כל השדות");
            return;
        }

        FirebaseUser user = refAuth.getCurrentUser();
        if(user == null) {
            tVMsg.setText("שגיאה, המשתמש לא מחובר");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Saving profile");
        pd.setMessage("Please wait...");
        pd.show();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("birthDate", birthDate);
        data.put("address", address);
        data.put("phone", phone);

        db.collection("users")
                .document(user.getUid())
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    Toast.makeText(this, "פרופיל נשמר בהצלחה!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    tVMsg.setText("שגיאה בשמירת הנתונים: " + e.getMessage());
                });
    }
}
