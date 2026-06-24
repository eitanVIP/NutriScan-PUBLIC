package com.eitangrimblat.nutriscan.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.firebase.Database;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for managing user allergy preferences, which are synchronized with Firestore.
 */
public class AllergiesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_allergies);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.allergiesLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Auth.getCurrentUser() == null) {
            Intent intent = new Intent(AllergiesActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        Database.loadData(Database.getCollection(Database.COLLECTION_USERS), Auth.getCurrentUser().getUid(), (DocumentSnapshot result, boolean success, String exception) -> {
            if (success) {
                if (result.contains(Database.FIELD_ALLERGIES)) {
                    MaterialButtonToggleGroup group1 = findViewById(R.id.bcmAllergies);
                    MaterialButtonToggleGroup group2 = findViewById(R.id.bcmAllergies2);
                    Map<String, Boolean> map = (Map<String, Boolean>) result.get(Database.FIELD_ALLERGIES);

                    if(map.get(buttonIdToFirestoreKey(R.id.bcmAllergiesDairy)))
                        group1.check(R.id.bcmAllergiesDairy);

                    if(map.get(buttonIdToFirestoreKey(R.id.bcmAllergiesNuts)))
                        group1.check(R.id.bcmAllergiesNuts);

                    if(map.get(buttonIdToFirestoreKey(R.id.bcmAllergiesEggs)))
                        group1.check(R.id.bcmAllergiesEggs);

                    if(map.get(buttonIdToFirestoreKey(R.id.bcmAllergiesFish)))
                        group2.check(R.id.bcmAllergiesFish);

                    if(map.get(buttonIdToFirestoreKey(R.id.bcmAllergiesSesame)))
                        group2.check(R.id.bcmAllergiesSesame);

                    if(map.get(buttonIdToFirestoreKey(R.id.bcmAllergiesGluten)))
                        group2.check(R.id.bcmAllergiesGluten);
                }
            } else {
                Log.d("Eitan Debug Allergies", "Failed to load allergy data: " + exception);
            }
        });

        MaterialButtonToggleGroup.OnButtonCheckedListener listener = (group, checkedId, isChecked) -> {
            Database.loadData(Database.getCollection(Database.COLLECTION_USERS), Auth.getCurrentUser().getUid(), (DocumentSnapshot result, boolean success, String exception) -> {
                if (success) {
                    Map<String, Boolean> map;
                    if (result.contains(Database.FIELD_ALLERGIES)) {
                        map = (Map<String, Boolean>) result.get(Database.FIELD_ALLERGIES);
                    } else {
                        map = new HashMap<>();
                        map.put("dairy", false);
                        map.put("nuts", false);
                        map.put("fish", false);
                        map.put("gluten", false);
                        map.put("eggs", false);
                        map.put("sesame", false);
                    }
                    map.put(buttonIdToFirestoreKey(checkedId), isChecked);

                    Database.saveData(Database.getCollection(Database.COLLECTION_USERS), Auth.getCurrentUser().getUid(), Map.of(Database.FIELD_ALLERGIES, map), (Void unused, boolean success2, String exception2) -> {
                        if (!success2) {
                            Log.d("Eitan Debug Allergies", "Failed to save allergy data: " + exception2);
                        }
                    });
                } else {
                    Log.d("Eitan Debug Allergies", "Failed to load and save allergy data: " + exception);
                }
            });
        };

        ((MaterialButtonToggleGroup) findViewById(R.id.bcmAllergies)).addOnButtonCheckedListener(listener);
        ((MaterialButtonToggleGroup) findViewById(R.id.bcmAllergies2)).addOnButtonCheckedListener(listener);

        findViewById(R.id.bcmAllergiesBack).setOnClickListener(v -> {
            startActivity(new Intent(AllergiesActivity.this, SettingsActivity.class));
        });
    }

    /**
     * Maps button resource IDs to Firestore allergy keys.
     */
    private String buttonIdToFirestoreKey(int buttonId) {
        if (buttonId == R.id.bcmAllergiesDairy)
            return Database.ALLERGY_KEY_DAIRY;

        if (buttonId == R.id.bcmAllergiesNuts)
            return Database.ALLERGY_KEY_NUTS;

        if (buttonId == R.id.bcmAllergiesFish)
            return Database.ALLERGY_KEY_FISH;

        if (buttonId == R.id.bcmAllergiesEggs)
            return Database.ALLERGY_KEY_EGGS;

        if (buttonId == R.id.bcmAllergiesGluten)
            return Database.ALLERGY_KEY_GLUTEN;

        if (buttonId == R.id.bcmAllergiesSesame)
            return Database.ALLERGY_KEY_SESAME;

        return "error";
    }
}
