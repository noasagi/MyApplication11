package com.example.myapplication;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;

public abstract class BaseActivity extends AppCompatActivity {

    protected UserHelper userHelper;

    // האם מופעל חץ חזרה (Up Button) – כדי לא להציג תפריט עם 3 נקודות
    private boolean isUpButtonEnabled = false;

    @Override
    protected void onStart() {
        super.onStart();
        if (userHelper == null) {
            userHelper = new UserHelper(this);
        }
    }

    /**
     * הגדרת Toolbar רגיל (ללא חץ חזרה)
     */
    protected void setupToolbar(Toolbar toolbar) {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    /**
     * Toolbar לדפים משניים עם חץ חזרה (ללא תפריט 3 נקודות)
     */
    protected void setupSecondaryToolbar(Toolbar toolbar, boolean showBackButton) {
        setupToolbar(toolbar);

        if (showBackButton && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            isUpButtonEnabled = true;
        }
    }

    /**
     * סינון פריטי תפריט (גם לתפריט העליון וגם ל-Drawer)
     */
    public void filterMenuItems(Menu menu) {
        if (userHelper == null) userHelper = new UserHelper(this);
        boolean isBusiness = userHelper.isBusinessOwner();
        boolean isGuest = userHelper.isGuest();

        MenuItem itemHome = menu.findItem(R.id.action_home);
        MenuItem itemLogin = menu.findItem(R.id.action_login);
        MenuItem itemLogout = menu.findItem(R.id.action_logout);
        MenuItem itemSetProfile = menu.findItem(R.id.action_set_profile);
        MenuItem itemMyBusiness = menu.findItem(R.id.action_my_business);
        MenuItem itemFavorites = menu.findItem(R.id.action_favorites);
        MenuItem itemAppointments = menu.findItem(R.id.action_appointments);


        if (itemHome != null) {
            itemHome.setVisible(true); // דף הבית תמיד קיים
        }
        if (itemLogin != null) {
            itemLogin.setVisible(isGuest);
        }
        if (itemLogout != null) {
            itemLogout.setVisible(!isGuest);
        }
        if (itemSetProfile != null) {
            itemSetProfile.setVisible(!isGuest);
        }
        if (itemMyBusiness != null) {
            // "העסק שלי" יוצג רק לבעלי עסקים
            itemMyBusiness.setVisible(isBusiness);
        }

        if (itemMyBusiness != null) {
            itemAppointments.setVisible(isBusiness);
        }

        // מציג את הכפתור רק אם המשתמש הוא לקוח (ולא בעל עסק או אורח)
        if (itemFavorites != null) {
            itemFavorites.setVisible(userHelper.isClient());
        }
    }

    /**
     * ניווט כללי בפריטי תפריט (גם Drawer וגם תפריט עליון)
     */
    public boolean handleNavigationItemSelection(MenuItem item) {
        int id = item.getItemId();
        if (userHelper == null) userHelper = new UserHelper(this);
        boolean isBusiness = userHelper.isBusinessOwner();

        if (id == R.id.action_home) {
            // דף הבית – לבעל עסק: BusinessMainActivity, ללקוח: ClientMainActivity
            if (isBusiness) {
                Intent intent = new Intent(this, BusinessMainActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, ClientMainActivity.class);
                startActivity(intent);
            }
            return true;
        }

        else if (id == R.id.action_my_business) {
            // ניהול העסק שלי – מגיע תמיד ל-MyBusinessMainActivity
            Toast.makeText(this, "פותחת את ניהול העסק שלי", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MyBusinessMainActivity.class);
            startActivity(intent);
            return true;
        }

        else if (id == R.id.action_appointments) {
            if (isBusiness) {
                // משיכת ה-ID של המשתמש הנוכחי
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // חיפוש העסק ששייך למשתמש הזה בפיירבייס
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("businesses")
                        // ✅ תיקון: שינינו מ-userId ל-ownerId
                        .whereEqualTo("ownerId", currentUserId)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                // מצאנו את העסק!
                                // אנחנו לוקחים את השדה businessId מתוך המסמך ליתר ביטחון
                                String businessId = queryDocumentSnapshots.getDocuments().get(0).getString("businessId");

                                Intent intent = new Intent(this, BusinessAppointmentsActivity.class);
                                intent.putExtra("BUSINESS_ID", businessId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "לא נמצא עסק מקושר", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "שגיאה בחיבור: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
            return true;
        }

        else if (id == R.id.action_logout) {
            // התנתקות
            FirebaseAuth.getInstance().signOut();
            userHelper.logout();
            Toast.makeText(this, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();

            // ריענון תפריט רק אם זה לא מסך עם Drawer
            if (!(this instanceof DrawerBaseActivity)) {
                invalidateOptionsMenu();
            }

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }

        else if (id == R.id.action_login) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        else if (id == R.id.action_favorites) {
            Intent intent = new Intent(this, FavoritesActivity.class);
            startActivity(intent);
            return true;
        }

        // פעולות שלא טופלו כאן
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // למסכים עם Drawer אין תפריט עליון (3 נקודות)
        if (this instanceof DrawerBaseActivity) {
            return false;
        }

        // למסכים עם חץ חזרה – לא מציגים 3 נקודות
        if (isUpButtonEnabled) {
            return false;
        }

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        filterMenuItems(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // טיפול בלחיצה על חץ חזרה ב-Toolbar
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (handleNavigationItemSelection(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
