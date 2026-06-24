package com.eitangrimblat.nutriscan.ui.statistics;

import android.util.Log;

import androidx.annotation.OptIn;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eitangrimblat.nutriscan.data.Nutriments;
import com.eitangrimblat.nutriscan.data.Product;
import com.eitangrimblat.nutriscan.data.StatisticsData;
import com.eitangrimblat.nutriscan.firebase.AI;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.firebase.Database;
import com.google.firebase.ai.type.PublicPreviewAPI;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for StatisticsFragment, managing the generation and caching of nutritional insights using AI.
 */
public class StatisticsViewModel extends ViewModel {
    private final MutableLiveData<StatisticsData> statsData = new MutableLiveData<>(new StatisticsData());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> uiEvent = new MutableLiveData<>();

    /** @return LiveData containing the processed statistics data. */
    public LiveData<StatisticsData> getStatsData() {
        return statsData;
    }
    /** @return LiveData indicating if statistics generation is in progress. */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    /** @return LiveData for notifying UI about success or error messages. */
    public LiveData<String> getUiEvent() {
        return uiEvent;
    }

    /**
     * Posts a message to the UI event LiveData.
     * @param message The message to send.
     */
    private void sendEvent(String message) {
        uiEvent.postValue(message);
    }

    /**
     * Initializes the ViewModel and triggers statistics generation.
     */
    @OptIn(markerClass = PublicPreviewAPI.class)
    public StatisticsViewModel() {
        generateStats();
    }

    /**
     * Orchestrates the statistics generation process: checks Firestore cache, or requests new data from AI.
     */
    @OptIn(markerClass = PublicPreviewAPI.class)
    private void generateStats() {
        isLoading.postValue(true);

        if (Auth.getCurrentUser() == null)
            return;

        String uid = Auth.getCurrentUser().getUid();
        CollectionReference productsRef = Database
            .getCollection(Database.COLLECTION_USERS)
            .document(uid)
            .collection(Database.COLLECTION_PRODUCTS);

        Database.loadCollection(productsRef, (collection, productsSuccess, productsException) -> {
            if (!productsSuccess) {
                Log.d("Eitan Debug Stats", "Error loading products: " + productsException);
                sendEvent("Error while creating stats, try again later");
                isLoading.postValue(false);
                return;
            }

            if (collection.isEmpty()) {
                Log.d("Eitan Debug Stats", "No products found");
                sendEvent("No stats available, add products first");
                isLoading.postValue(false);
                return;
            }

            List<String> currentIds = new ArrayList<>();
            for (DocumentSnapshot doc : collection.getDocuments()) {
                currentIds.add(doc.getId());
            }

            CollectionReference statsRef = Database
                .getCollection(Database.COLLECTION_USERS)
                .document(uid)
                .collection(Database.COLLECTION_STATS);

            Database.loadData(statsRef, Database.DOCUMENT_CACHE, (cacheDoc, cacheSuccess, cacheException) -> {
                if (cacheSuccess && cacheDoc != null && cacheDoc.exists()) {
                    List<String> cachedIds = (List<String>) cacheDoc.get(Database.DOCUMENT_CACHE_PRODUCT_IDS);
                    if (cachedIds != null && new HashSet<>(cachedIds).equals(new HashSet<>(currentIds))) {
                        StatisticsData cached = parseCacheDocument(cacheDoc);
                        if (cached != null) {
                            Log.d("Eitan Debug Stats", "Using cached stats");
                            statsData.postValue(cached);
                            isLoading.postValue(false);
                            return;
                        }
                    }
                }

                String prompt = createPrompt(collection);
                Log.d("Eitan Debug Stats", "Sending prompt to Gemini:\n" + prompt);

                AI.sendMessage(prompt, (response, aiSuccess, aiException) -> {
                    if (!aiSuccess) {
                        Log.d("Eitan Debug Stats", "Failed to generate stats: " + aiException);
                        sendEvent("Failed to generate stats, try again later");
                        isLoading.postValue(false);
                        return;
                    }

                    Log.d("Eitan Debug Stats", "Received response from AI:\n" + response);

                    StatisticsData data = parseAIResponse(response);
                    if (data == null) {
                        isLoading.postValue(false);
                        return;
                    }

                    statsData.postValue(data);
                    isLoading.postValue(false);
                    saveStatsCache(data, currentIds);
                });
            });
        });
    }

    /**
     * Parses the JSON response from the AI into a StatisticsData object.
     * @param response The raw JSON string from the AI.
     * @return A StatisticsData object, or null if parsing fails.
     */
    private StatisticsData parseAIResponse(String response) {
        try {
            JSONObject root = new JSONObject(response);
            StatisticsData data = new StatisticsData();

            data.score = root.getInt("overall_score");
            data.nutrient1Value = root.getInt("nutrient_protein") + "%";
            data.nutrient2Value = root.getInt("nutrient_carbs") + "%";
            data.nutrient3Value = root.getInt("nutrient_fats") + "%";

            JSONArray insightsArray = root.getJSONArray("insights");
            data.insight1Title   = insightsArray.getJSONObject(0).getString("title");
            data.insight1Content = insightsArray.getJSONObject(0).getString("content");
            data.insight2Title   = insightsArray.getJSONObject(1).getString("title");
            data.insight2Content = insightsArray.getJSONObject(1).getString("content");
            data.insight3Title   = insightsArray.getJSONObject(2).getString("title");
            data.insight3Content = insightsArray.getJSONObject(2).getString("content");

            data.specialScoreTitle = root.getJSONObject("special_score").getString("title");
            data.specialScore      = root.getJSONObject("special_score").getInt("value");

            return data;
        } catch (JSONException e) {
            Log.d("Eitan Debug Stats", "JSON parsing error: " + e.getMessage());
            sendEvent("Error parsing AI response, try again");
            return null;
        }
    }

    /**
     * Parses a Firestore document snapshot into a StatisticsData object.
     * @param doc The document snapshot to parse.
     * @return A StatisticsData object, or null if parsing fails.
     */
    private StatisticsData parseCacheDocument(DocumentSnapshot doc) {
        try {
            StatisticsData data = new StatisticsData();
            data.score           = ((Long) doc.get(Database.DOCUMENT_CACHE_OVERALL_SCORE)).intValue();
            data.nutrient1Value  = doc.getString(  Database.DOCUMENT_CACHE_NUTRIENT1);
            data.nutrient2Value  = doc.getString(  Database.DOCUMENT_CACHE_NUTRIENT2);
            data.nutrient3Value  = doc.getString(  Database.DOCUMENT_CACHE_NUTRIENT3);
            data.insight1Title   = doc.getString(  Database.DOCUMENT_CACHE_INSIGHT1_TITLE);
            data.insight1Content = doc.getString(  Database.DOCUMENT_CACHE_INSIGHT1_CONTENT);
            data.insight2Title   = doc.getString(  Database.DOCUMENT_CACHE_INSIGHT2_TITLE);
            data.insight2Content = doc.getString(  Database.DOCUMENT_CACHE_INSIGHT2_CONTENT);
            data.insight3Title   = doc.getString(  Database.DOCUMENT_CACHE_INSIGHT3_TITLE);
            data.insight3Content = doc.getString(  Database.DOCUMENT_CACHE_INSIGHT3_CONTENT);
            data.specialScore    = ((Long) doc.get(Database.DOCUMENT_CACHE_SPECIAL_SCORE)).intValue();
            data.specialScoreTitle = doc.getString(Database.DOCUMENT_CACHE_SPECIAL_TITLE);
            return data;
        } catch (Exception e) {
            Log.d("Eitan Debug Stats", "Failed to parse cache document: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves the generated statistics and the corresponding product IDs to Firestore as a cache.
     * @param data The statistics data to cache.
     * @param productIds The list of product IDs used to generate these stats.
     */
    private void saveStatsCache(StatisticsData data, List<String> productIds) {
        String uid = Auth.getCurrentUser().getUid();
        Map<String, Object> cache = new HashMap<>();
        cache.put(Database.DOCUMENT_CACHE_PRODUCT_IDS,      productIds);
        cache.put(Database.DOCUMENT_CACHE_OVERALL_SCORE,    data.score);
        cache.put(Database.DOCUMENT_CACHE_NUTRIENT1,        data.nutrient1Value);
        cache.put(Database.DOCUMENT_CACHE_NUTRIENT2,        data.nutrient2Value);
        cache.put(Database.DOCUMENT_CACHE_NUTRIENT3,        data.nutrient3Value);
        cache.put(Database.DOCUMENT_CACHE_INSIGHT1_TITLE,   data.insight1Title);
        cache.put(Database.DOCUMENT_CACHE_INSIGHT1_CONTENT, data.insight1Content);
        cache.put(Database.DOCUMENT_CACHE_INSIGHT2_TITLE,   data.insight2Title);
        cache.put(Database.DOCUMENT_CACHE_INSIGHT2_CONTENT, data.insight2Content);
        cache.put(Database.DOCUMENT_CACHE_INSIGHT3_TITLE,   data.insight3Title);
        cache.put(Database.DOCUMENT_CACHE_INSIGHT3_CONTENT, data.insight3Content);
        cache.put(Database.DOCUMENT_CACHE_SPECIAL_SCORE,    data.specialScore);
        cache.put(Database.DOCUMENT_CACHE_SPECIAL_TITLE,    data.specialScoreTitle);

        Database.saveData(
            Database.getCollection(Database.COLLECTION_USERS)
                .document(uid)
                .collection(Database.COLLECTION_STATS),
            Database.DOCUMENT_CACHE,
            cache,
            (v, success, exception) -> {
                if (!success) {
                    Log.d("Eitan Debug Stats", "Failed to save stats cache: " + exception);
                }
            }
        );
    }

    /**
     * Creates a detailed prompt for the AI based on the user's scanned products.
     * @param collection The Firestore query snapshot of the user's products.
     * @return A formatted prompt string.
     */
    private String createPrompt(com.google.firebase.firestore.QuerySnapshot collection) {
        ArrayList<String> products_strings = new ArrayList<>();
        for (int i = 0; i < collection.size(); i++) {
            Product p = new Product();
            p.product_name = collection.getDocuments().get(i).getString(Database.PRODUCT_FIELD_NAME);
            p.brands       = collection.getDocuments().get(i).getString(Database.PRODUCT_FIELD_BRANDS);
            p.quantity     = collection.getDocuments().get(i).getString(Database.PRODUCT_FIELD_QUANTITY);
            p.nutriments   = new Nutriments();
            p.nutriments.energy_kcal = collection.getDocuments().get(i).getDouble(Database.PRODUCT_FIELD_ENERGY);
            p.nutriments.fat         = collection.getDocuments().get(i).getDouble(Database.PRODUCT_FIELD_FAT);
            p.nutriments.salt        = collection.getDocuments().get(i).getDouble(Database.PRODUCT_FIELD_SALT);
            p.nutriments.sugars      = collection.getDocuments().get(i).getDouble(Database.PRODUCT_FIELD_SUGARS);

            products_strings.add(p.product_name + ";;;"
                + p.brands           + ";;;"
                + p.quantity         + ";;;"
                + p.nutriments.energy_kcal + ";;;"
                + p.nutriments.fat   + ";;;"
                + p.nutriments.salt  + ";;;"
                + p.nutriments.sugars);
        }

        return """
            You are a professional nutritionist and kitchen analyst.
            Analyze the user's kitchen inventory provided below and return a JSON object with kitchen statistics.
            
            The JSON must follow this exact structure:
            {
              "overall_score": integer (0-100 based on nutritional balance),
              "nutrient_protein": integer (0-100),
              "nutrient_carbs": integer (0-100),
              "nutrient_fats": integer (0-100),
              "insights": [
                { "title": "string (max 30 chars)", "content": "string (max 60 chars)" },
                { "title": "string (max 30 chars)", "content": "string (max 60 chars)" },
                { "title": "string (max 30 chars)", "content": "string (max 60 chars)" }
              ],
              "special_score": {
                "title": "string (A creative metric name based on the inventory)",
                "value": integer (0-100)
              }
            }
        
            Rules:
            1. The "overall_score" should reflect how healthy the total inventory is.
            2. The nutrients should represent the estimated macro-nutrient calorie distribution of the whole kitchen. CRITICAL: The values of "nutrient_protein", "nutrient_carbs", and "nutrient_fats" MUST add up to exactly 100.
            3. Insights should be actionable.
            4. The "special_score" is a random creative metric about the kitchen's current state.
        
            The list of products consists of:
            Name;;;Brands;;;Mass;;;Energy;;;Fat;;;Salt;;;Sugar
            
            Products:
            """ + String.join("\n", products_strings);
    }
}