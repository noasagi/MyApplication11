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
    private boolean isUpButtonEnabled = false;

    @Override
    protected void onStart() {
        super.onStart();
        if (userHelper == null) {
            userHelper = new UserHelper(this);
        }
    }

    protected void setupToolbar(Toolbar toolbar) {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    protected void setupSecondaryToolbar(Toolbar toolbar, boolean showBackButton) {
        setupToolbar(toolbar);

        if (showBackButton && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            isUpButtonEnabled = true;
        }
    }

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

        // --- חדש: כפתור לניהול יומן/חסימות ---
        MenuItem itemBlockSlots = menu.findItem(R.id.action_block_slots);

        if (itemHome != null) itemHome.setVisible(true);
        if (itemLogin != null) itemLogin.setVisible(isGuest);
        if (itemLogout != null) itemLogout.setVisible(!isGuest);
        if (itemSetProfile != null) itemSetProfile.setVisible(!isGuest);

        if (itemMyBusiness != null) {
            itemMyBusiness.setVisible(isBusiness);
        }

        if (itemAppointments != null) {
            itemAppointments.setVisible(!isGuest);
        }

        // --- לוגיקה חדשה: הצגת ניהול יומן רק לבעל עסק ---
        if (itemBlockSlots != null) {
            itemBlockSlots.setVisible(isBusiness);
        }

        if (itemFavorites != null) {
            itemFavorites.setVisible(userHelper.isClient());
        }
    }

    public boolean handleNavigationItemSelection(MenuItem item) {
        int id = item.getItemId();
        if (userHelper == null) userHelper = new UserHelper(this);
        boolean isBusiness = userHelper.isBusinessOwner();

        if (id == R.id.action_home) {
            if (isBusiness) {
                startActivity(new Intent(this, BusinessMainActivity.class));
            } else {
                startActivity(new Intent(this, ClientMainActivity.class));
            }
            return true;
        }

        else if (id == R.id.action_my_business) {
            Toast.makeText(this, "פותחת את ניהול העסק שלי", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MyBusinessMainActivity.class));
            return true;
        }

        // --- חדש: ניווט לדף חסימת שעות ---
        else if (id == R.id.action_block_slots) {
            startActivity(new Intent(this, BusinessBlockSlotsActivity.class));
            return true;
        }

        else if (id == R.id.action_appointments) {
            if (isBusiness) {
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("businesses")
                        .whereEqualTo("ownerId", currentUserId)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                String businessId = queryDocumentSnapshots.getDocuments().get(0).getString("businessId");
                                Intent intent = new Intent(this, BusinessAppointmentsActivity.class);
                                intent.putExtra("BUSINESS_ID", businessId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "לא נמצא עסק מקושר", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בחיבור", Toast.LENGTH_SHORT).show());
            } else {
                Intent intent = new Intent(this, MyAppointmentsActivity.class);
                startActivity(intent);
            }
            return true;
        }

        else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            userHelper.logout();
            Toast.makeText(this, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();

            if (!(this instanceof DrawerBaseActivity)) {
                invalidateOptionsMenu();
            }

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }

        else if (id == R.id.action_login) {
            startActivity(new Intent(this, LoginActivity.class));
            return true;
        }

        else if (id == R.id.action_favorites) {
            startActivity(new Intent(this, FavoritesActivity.class));
            return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this instanceof DrawerBaseActivity) return false;
        if (isUpButtonEnabled) return false;
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