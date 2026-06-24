package com.eitangrimblat.nutriscan.nonui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.eitangrimblat.nutriscan.data.Nutriments;
import com.eitangrimblat.nutriscan.data.Product;
import com.eitangrimblat.nutriscan.data.RecipeItem;
import com.eitangrimblat.nutriscan.firebase.AI;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.firebase.Database;
import com.eitangrimblat.nutriscan.firebase.OnCompleteRunnable;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.type.PublicPreviewAPI;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker responsible for generating and saving personalized recipes based on user products and preferences.
 */
public class RecipesWorker extends ListenableWorker {
    /**
     * Initializes the worker with context and parameters.
     */
    public RecipesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /**
     * Sends a progress event message to observers.
     *
     * @param message The message to broadcast.
     */
    private void sendEvent(String message) {
        // We wrap the message in a Data object
        Data progress = new Data.Builder()
            .putString("event_message", message)
            .build();

        // This sends the data out to anyone observing this specific worker
        setProgressAsync(progress);
    }

    private void failure(CallbackToFutureAdapter.Completer<Result> completer, String message) {
        Data mes = new Data.Builder()
            .putString("event_message", message)
            .build();

        completer.set(Result.failure(mes));
    }

    /**
     * Initiates the recipe generation background task.
     *
     * @return A future representing the background work.
     */
    @Override
    @org.jspecify.annotations.NonNull
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            generateNewRecipes(completer);
            sendEvent("Started generation 10%");

            return "RecipeGenerationTask";
        });
    }

    /**
     * Orchestrates the retrieval of user data and the request for new recipes.
     *
     * @param completer The completer to signal work completion.
     */
    @OptIn(markerClass = PublicPreviewAPI.class)
    public void generateNewRecipes(CallbackToFutureAdapter.Completer<Result> completer) {
        Database.loadCollection(Database
                .getCollection(Database.COLLECTION_USERS)
                .document(Auth.getCurrentUser().getUid())
                .collection(Database.COLLECTION_PRODUCTS),
            (collection, productsSuccess, productsException) -> {
                if (!productsSuccess) {
                    Log.d("Eitan Debug Recipes", "Error while creating recipes: error loading products: " + productsException);
                    failure(completer, "Error while creating recipes, try again later");
                    return;
                }

                if (collection.getDocuments().isEmpty()) {
                    Log.d("Eitan Debug Recipes", "Error while creating recipes: user has no products");
                    failure(completer, "Scan products before generating new recipes");
                    return;
                }

                Database.loadData(Database.getCollection(Database.COLLECTION_USERS), Auth.getCurrentUser().getUid(),
                (userData, userDataSuccess, userDataException) -> {
                    if (!userDataSuccess) {
                        Log.d("Eitan Debug Recipes", "Error while creating recipes: error loading user data: " + userDataException);
                        failure(completer, "Error while creating recipes, try again later");
                        return;
                    }

                    Map<String, Boolean> allergies = new HashMap<>();
                    if (userData.contains(Database.FIELD_ALLERGIES)) {
                        allergies = (Map<String, Boolean>) userData.get(Database.FIELD_ALLERGIES);
                    }

                    sendEvent("Loaded user data 20%");

                    ArrayList<String> products = new ArrayList<>();
                    String prompt = createPrompt(collection, products, allergies);
                    if (prompt.isEmpty()) {
                        Log.d("Eitan Debug Recipes", "No recipes are available: user has no products");
                        failure(completer, "No recipes are available, try again later");
                        return;
                    }

                    Log.d("Eitan Debug Recipes", "Sending prompt to Gemini:\n" + prompt);
                    AI.sendMessage(prompt, (response, success, exception) -> onAIResponse(response, success, exception, products, completer));
                });
            });
    }

    /**
     * Handles the raw response and triggers parsing or failure.
     *
     * @param response  The raw response string.
     * @param success   Whether the request succeeded.
     * @param exception Error message if failed.
     * @param products  List of product IDs.
     * @param completer The work completer.
     */
    private void onAIResponse(String response, boolean success, String exception, List<String> products, CallbackToFutureAdapter.Completer<Result> completer)  {
        if (!success) {
            Log.d("Eitan Debug Recipes", "Error while creating recipes: failed to send message to AI: " + exception);
            failure(completer, "Error while creating recipes, try again later");
            return;
        }

        Log.d("Eitan Debug Recipes", "Received reply from Gemini:\n" + response);

        try {
            if (new JSONObject(response).getJSONArray("recipes").length() == 0) {
                Log.d("Eitan Debug Recipes", "No recipes are available: AI says there aren't enough products");
                failure(completer, "No recipes are available, try again later");
                return;
            }
        }
        catch (JSONException e) {
            Log.d("Eitan Debug Recipes", "No recipes are available: error while parsing AI JSON response");
            failure(completer, "Error Occurred while generating recipes, try again");
            return;
        }

        sendEvent("Generated JSON 40%");

        parseAIResponse(response, products, completer);
    }

    /**
     * Converts the JSON response into a list of {@link RecipeItem} objects.
     *
     * @param response  The JSON string to parse.
     * @param products  The product ID mapping.
     * @param completer The work completer.
     */
    private void parseAIResponse(String response, List<String> products, CallbackToFutureAdapter.Completer<Result> completer) {
        List<RecipeItem> recipeItems = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(response);
            JSONArray recipesArray = root.getJSONArray("recipes");

            for (int i = 0; i < recipesArray.length(); i++) {
                JSONObject obj = recipesArray.getJSONObject(i);

                String name = obj.getString("name");
                String desc = obj.getString("description");
                String instr = obj.getString("instructions");
                JSONArray indexArray = obj.getJSONArray("product_indexes");

                String[] productIds = new String[indexArray.length()];
                for (int j = 0; j < indexArray.length(); j++) {
                    productIds[j] = products.get(indexArray.getInt(j));
                }

                recipeItems.add(new RecipeItem(name, desc, null, instr, productIds, null, null, null));
            }

            if (recipeItems.isEmpty()) {
                Log.d("Eitan Debug Recipes", "AI returned 0 recipes");
                failure(completer, "No recipes available for these items");
                return;
            }

            sendEvent("Parsed JSON 35%");

            generateAllRecipesImages(recipeItems, 0, completer);
        } catch (JSONException e) {
            Log.d("Eitan Debug Recipes", "JSON Parsing error: " + e.getMessage());
            failure(completer, "Error parsing AI response, try again");
        }
    }

    /**
     * Recursively generates images for all provided recipes.
     *
     * @param recipes   The list of recipes to process.
     * @param i         The current recipe index.
     * @param completer The work completer.
     */
    private void generateAllRecipesImages(List<RecipeItem> recipes, int i, CallbackToFutureAdapter.Completer<Result> completer) {
        if (i >= recipes.size()) {
            saveNewRecipes(recipes, completer);
            return;
        }

        Log.d("Eitan Debug Recipes", "Starting image generation for recipe " + (i + 1) + "/" + recipes.size() + ": " + recipes.get(i).getName());

        generateRecipeImage(recipes.get(i), () -> {
            sendEvent("Generated recipe image" + (i + 1) + " " + (40 + (i + 1) * 10) + "%");

            // Move to the next recipe regardless of success (prevents getting stuck)
            generateAllRecipesImages(recipes, i + 1, completer);
        });
    }

    /**
     * Generates and sets an image for the recipe.
     *
     * @param recipe The recipe to update.
     * @param andThen Callback executed after completion.
     * @see AI#sendRequestImage(String, OnCompleteRunnable)
     */
    @OptIn(markerClass = PublicPreviewAPI.class)
    private void generateRecipeImage(RecipeItem recipe, Runnable andThen) {
        String imagePrompt = "A professional, high-quality food photography shot of " + recipe.getName() +
            ". Descriptive style: " + recipe.getDescription() +
            ". Lighting: Bright, natural. Resolution: 512px.";

        AI.sendRequestImage(imagePrompt, (Bitmap result, boolean success, String exception) -> {
            if (success) {
                if (result != null) {
                    Log.d("Eitan Debug Recipes", "Successfully generated image for: " + recipe.getName());
                    recipe.setImage(result);
                } else {
                    Log.d("Eitan Debug Recipes", "Image AI returned success but Bitmap was null for: " + recipe.getName());
                    sendEvent("Error while creating image");
                }
            } else {
                Log.d("Eitan Debug Recipes", "Error while creating image for " + recipe.getName() + ": " + exception);
                sendEvent("Error while creating image");
            }

            andThen.run();
        });
    }

    /**
     * Saves all generated recipes and completes the worker task.
     *
     * @param recipes   The recipes to save.
     * @param completer The work completer.
     */
    private void saveNewRecipes(List<RecipeItem> recipes, CallbackToFutureAdapter.Completer<Result> completer) {
        AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 0; i < recipes.size(); i++) {
            saveRecipe(recipes.get(i), (unused, success, exception) -> {
                if (completedCount.incrementAndGet() >= recipes.size()) {
                    sendEvent("Saved recipes 100%");

                    String json = new Gson().toJson(recipes);
                    completer.set(Result.success(new Data.Builder().putString("recipes", json).build()));
                }
            });
        }
    }

    /**
     * Saves an individual recipe and its image to Firestore and Storage.
     *
     * @param recipe     The recipe to save.
     * @param onComplete Callback for individual save completion.
     */
    @OptIn(markerClass = PublicPreviewAPI.class)
    private void saveRecipe(RecipeItem recipe, OnCompleteRunnable<Void> onComplete) {
        // Unique name for the image file
        String imageName = "recipe_" + System.currentTimeMillis() + ".jpg";

        recipe.setImageId(imageName);

        File imageFile = new File(getApplicationContext().getCacheDir(), "recipe_img_temp_" + imageName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            recipe.getImage().compress(Bitmap.CompressFormat.JPEG, 80, fos);
            String cachedPath = imageFile.getAbsolutePath();
            recipe.setWorkerTransferTempImagePath(cachedPath);
        } catch (IOException e) {
            Log.d("Eitan Debug Recipes", "Failed to save '" + recipe.getName() + "' local image transfer:\n" + e);
        }

        Database.addDocument(Database.getCollection(Database.COLLECTION_USERS)
                .document(Auth.getCurrentUser().getUid())
                .collection(Database.COLLECTION_RECIPES),
            Map.of(
                Database.RECIPE_FIELD_NAME, recipe.getName(),
                Database.RECIPE_FIELD_DESC, recipe.getDescription(),
                Database.RECIPE_FIELD_IMAGE, imageName,
                Database.RECIPE_FIELD_INSTRUCTIONS, recipe.getInstructions(),
                Database.RECIPE_FIELD_PRODUCTIDS, Arrays.asList(recipe.getProductsIds())
            ), (documentId, documentSuccess, documentException) -> {
                if (!documentSuccess) {
                    Log.d("Eitan Debug Recipes", "Failed to save '" + recipe.getName() + "': " + documentException);
                    sendEvent("Failed to save '" + recipe.getName() + "'");
                }

                if (recipe.getImage() != null) {
                    Database.storeImage(recipe.getImage(), imageName, 80, (unused, success, e) -> {
                        if (!success) {
                            Log.d("Eitan Debug Recipes", "Failed to save '" + recipe.getName() + "' image: " + e);
                            sendEvent("Failed to save '" + recipe.getName() + "' image");
                        }

                        onComplete.onComplete(null, documentSuccess, documentException);
                    });
                } else {
                    onComplete.onComplete(null, documentSuccess, documentException);
                }
            }
        );
    }

    /**
     * Constructs the chef-persona prompt for recipe generation.
     *
     * @param collection The user's products.
     * @param products   List to be populated with product IDs.
     * @param allergies  User's allergy preferences.
     * @return The formatted prompt string.
     */
    private String createPrompt(QuerySnapshot collection, ArrayList<String> products, Map<String, Boolean> allergies) {
        if (collection.isEmpty())
            return "";

        ArrayList<String> products_strings = new ArrayList<>();
        for (int i = 0; i < collection.size(); i++) {
            Product p = new Product();
            p.product_name = collection.getDocuments().get(i).getString(Database.PRODUCT_FIELD_NAME);
            p.brands = collection.getDocuments().get(i).getString(Database.PRODUCT_FIELD_BRANDS);
            p.quantity = collection.getDocuments().get(i).getString(Database.PRODUCT_FIELD_QUANTITY);
            p.nutriments = new Nutriments();
            p.nutriments.energy_kcal = collection.getDocuments().get(i).getDouble(Database.PRODUCT_FIELD_ENERGY);
            p.nutriments.fat = collection.getDocuments().get(i).getDouble(Database.PRODUCT_FIELD_FAT);
            p.nutriments.salt = collection.getDocuments().get(i).getDouble(Database.PRODUCT_FIELD_SALT);
            p.nutriments.sugars = collection.getDocuments().get(i).getDouble(Database.PRODUCT_FIELD_SUGARS);

            products.add(collection.getDocuments().get(i).getId());

            products_strings.add(p.product_name + ";;;"
                + p.brands + ";;;"
                + p.quantity + ";;;"
                + p.nutriments.energy_kcal + ";;;"
                + p.nutriments.fat + ";;;"
                + p.nutriments.salt + ";;;"
                + p.nutriments.sugars);
        }

        ArrayList<String> allergies_strings = new ArrayList<>();
        for (String allergy : allergies.keySet()) {
            if (allergies.getOrDefault(allergy, false))
                allergies_strings.add(allergy);
        }
        String allergyList = allergies_strings.isEmpty() ? "None" : String.join(", ", allergies_strings);

        return """
            You are a professional chef. Based on the products provided, generate 4 recipes.
            Return the response as a JSON object with a key "recipes" containing an array of objects.
            Each object must have:
            - "name": string (1-2 words)
            - "description": string (max 50 chars)
            - "instructions": string (max 500 chars)
            - "product_indexes": array of integers
            
            * The instructions are a full work through including what materials to use, what to do, how much time to do each thing, etc...
            
            * The user has the following allergies:
            """
            + allergyList +
            """
            \n
            * If nothing can be made, return {"recipes": []}.
            
            * The list of products consists of all products available in someone's home.
            Therefore, you don't need to use all products.
            Although, if there are a small count of them, you can.
            
            * You will get a list of products with this format one per line:
            Name;;;Brands;;;Mass;;;Energy;;;Fat;;;Salt;;;Sugar
            For example:
            Nutella;;;Ferrero;;;400 g;;;539;;;30.9;;;0.107;;;56.3
            
            Products:
            """ + String.join("\n", products_strings);
    }
}