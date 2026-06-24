package com.eitangrimblat.nutriscan.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.ui.home.HomeFragment;
import com.eitangrimblat.nutriscan.ui.recipes.RecipesFragment;
import com.eitangrimblat.nutriscan.ui.scan.ScanFragment;
import com.eitangrimblat.nutriscan.ui.statistics.StatisticsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * The main container activity that hosts the bottom navigation and handles fragment switching.
 */
public class MainAppActivity extends AppCompatActivity {
    /**
     * Replaces the current fragment in the container with the specified fragment.
     *
     * @param selectedFragment The new fragment to display.
     */
    public void switchFragment(Fragment selectedFragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, selectedFragment)
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_app);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        findViewById(R.id.bcmSettings).setOnClickListener(v -> {
            startActivity(new Intent(MainAppActivity.this, SettingsActivity.class));
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new ScanFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_scan) {
                selectedFragment = new ScanFragment();
            } else if (itemId == R.id.nav_recipes) {
                selectedFragment = new RecipesFragment();
            } else if (itemId == R.id.nav_stats) {
                selectedFragment = new StatisticsFragment();
            } else if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            }

            if (selectedFragment != null) {
                switchFragment(selectedFragment);
            }

            return true;
        });
    }
}
