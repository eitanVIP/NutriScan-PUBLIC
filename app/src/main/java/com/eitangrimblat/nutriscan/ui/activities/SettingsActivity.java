package com.eitangrimblat.nutriscan.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.nonui.RecipesCache;
import com.eitangrimblat.nutriscan.ui.recipes.RecipesViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;

/**
 * Activity for managing application settings, including profile, theme, allergies, and account actions.
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutSettings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Auth.getCurrentUser() != null) {
            ((TextView)findViewById(R.id.tvSettingsEmail)).setText(Auth.getCurrentUser().getEmail());
            ((TextView)findViewById(R.id.tvSettingsName)).setText(Auth.getCurrentUser().getDisplayName());
        } else {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
            ((MaterialButtonToggleGroup) findViewById(R.id.bcmSettingsTheme)).check(R.id.bcmSettingsDark);
        else if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO)
            ((MaterialButtonToggleGroup) findViewById(R.id.bcmSettingsTheme)).check(R.id.bcmSettingsLight);
        else if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            ((MaterialButtonToggleGroup) findViewById(R.id.bcmSettingsTheme)).check(R.id.bcmSettingsSystem);
        
        findViewById(R.id.bcmSettingsAllergies).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, AllergiesActivity.class));
        });

        findViewById(R.id.bcmSettingsProfile).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, EditProfileActivity.class));
        });

        findViewById(R.id.bcmSettingsSignout).setOnClickListener(v -> {
            RecipesCache.clearCache(SettingsActivity.this);

            String uniqueWorkName = RecipesViewModel.WORK_NAME + "_" + Auth.getCurrentUser().getUid();
            WorkManager.getInstance(SettingsActivity.this).cancelUniqueWork(uniqueWorkName);
            Log.d("Eitan Debug Settings", "Cancelled work: " + uniqueWorkName);

            Auth.signOut();

            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.bcmSettingsBack).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, MainAppActivity.class));
        });

        if (Auth.getCurrentUser().getPhotoUrl() != null) {
            Glide.with(this)
                .load(Auth.getCurrentUser().getPhotoUrl())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into((ImageView) findViewById(R.id.imgSettingsProfile));
        }

        findViewById(R.id.bcmSettingsPassword).setOnClickListener(v -> {
            Auth.sendPasswordResetEmail((unused, success, exception) -> {
                if (success) {
                    Snackbar.make(findViewById(R.id.layoutSettings), "Password reset email sent", Snackbar.LENGTH_LONG).show();
                    Log.d("Eitan Debug Settings", "Password reset email sent");
                } else {
                    Snackbar.make(findViewById(R.id.layoutSettings), "Failed to send password reset email, try again later", Snackbar.LENGTH_LONG).show();
                    Log.d("Eitan Debug Settings", "Failed to send password reset email: " + exception);
                }
            });
        });
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ((MaterialButtonToggleGroup) findViewById(R.id.bcmSettingsTheme)).clearOnButtonCheckedListeners();
        ((MaterialButtonToggleGroup) findViewById(R.id.bcmSettingsTheme)).addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.bcmSettingsDark && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    recreate();
                } else if (checkedId == R.id.bcmSettingsLight && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    recreate();
                } else if (checkedId == R.id.bcmSettingsSystem && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    recreate();
                }
            }
        });
    }
}
