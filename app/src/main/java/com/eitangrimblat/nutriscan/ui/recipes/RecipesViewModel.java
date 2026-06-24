package com.eitangrimblat.nutriscan.ui.recipes;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.eitangrimblat.nutriscan.data.RecipeItem;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.firebase.Database;
import com.eitangrimblat.nutriscan.firebase.OnCompleteRunnable;
import com.eitangrimblat.nutriscan.nonui.RecipesCache;
import com.eitangrimblat.nutriscan.nonui.RecipesWorker;
import com.google.firebase.ai.type.PublicPreviewAPI;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ViewModel for managing recipes, including loading from cache/database, generation, and deletion.
 */
public class RecipesViewModel extends AndroidViewModel {
    public static final String WORK_NAME = "generate_recipes_task";

    private final MutableLiveData<List<RecipeItem>> currentRecipes = new MutableLiveData<>();
    private final MutableLiveData<String> uiEvent = new MutableLiveData<>();

    /** @return LiveData containing the current list of recipes. */
    public MutableLiveData<List<RecipeItem>> getCurrentRecipes() {
        return currentRecipes;
    }
    /** @return LiveData for notifying UI about success or error events. */
    public MutableLiveData<String> getUiEvent() {
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
     * Initializes the ViewModel and triggers an initial recipe load.
     * @param application The application context.
     */
    public RecipesViewModel(Application application) {
        super(application);
        loadRecipes();
    }

    /**
     * Enqueues a WorkManager task to generate new recipes using AI.
     */
    public void generateNewRecipes() {
        if (Auth.getCurrentUser() == null)
            return;

        String uniqueWorkName = RecipesViewModel.WORK_NAME + "_" + Auth.getCurrentUser().getUid();
        Log.d("Eitan Debug Settings", "Started work: " + uniqueWorkName);

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RecipesWorker.class).build();
        WorkManager.getInstance(getApplication())
            .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.KEEP, workRequest);
    }

    /**
     * Deletes a recipe and its associated image from Firestore and Storage, and updates the cache.
     * @param recipe The recipe item to delete.
     */
    public void deleteRecipe(RecipeItem recipe) {
        Database.deleteDocument(
            Database.getCollection(Database.COLLECTION_USERS)
                .document(Auth.getCurrentUser().getUid())
                .collection(Database.COLLECTION_RECIPES),
            recipe.getRecipeId(),
            (_1, _2, _3) -> {});

        Database.deleteFile(recipe.getImageId(), (_1, _2, _3) -> {});

        List<RecipeItem> updated = new ArrayList<>(currentRecipes.getValue());
        updated.remove(recipe);
        currentRecipes.postValue(updated);
        saveCacheAsync(updated);
    }

    /**
     * Adds a list of newly generated recipes to the current list, loading their temporary images.
     * @param newRecipes The list of recipes to add.
     */
    public void addRecipes(List<RecipeItem> newRecipes) {
        // Load all new recipes' images from local temp storage
        for (RecipeItem newRecipe : newRecipes) {
            if (newRecipe.getImage() != null || newRecipe.getWorkerTransferTempImagePath() == null)
                continue;

            Bitmap bitmap = BitmapFactory.decodeFile(newRecipe.getWorkerTransferTempImagePath());
            if (bitmap != null) {
                newRecipe.setImage(bitmap);
            } else {
                Log.d("Eitan Debug Recipes", "Failed to load recipe image from local temp storage after generation: " + newRecipe.getName());
                sendEvent("Failed to load a recipe's image");
            }
        }

        // Delete all recipes' local temp storage images
        File[] images = getApplication().getCacheDir().listFiles((dir, name) -> name.startsWith("recipe_img_temp_"));
        if (images != null) {
            for (File f : images) {
                if (!f.delete())
                    Log.d("Eitan Debug Recipes", "Could not delete temp image after generation: " + f.getName());
            }
        }

        // Update list
        List<RecipeItem> updated = new ArrayList<>(newRecipes);
        updated.addAll(Objects.requireNonNull(currentRecipes.getValue()));
        currentRecipes.postValue(updated);

        saveCacheAsync(updated);
    }

    /**
     * Loads recipes from the local cache and then refreshes from the Firestore database.
     */
    public void loadRecipes() {
        new Thread(() -> {
            List<RecipeItem> cached = RecipesCache.loadCache(getApplication());
            currentRecipes.postValue(cached);

            loadFromDatabase();
        }).start();
    }

    /**
     * Fetches the user's recipes from the Firestore database.
     */
    private void loadFromDatabase() {
        Database.loadCollection(
            Database.getCollection(Database.COLLECTION_USERS)
                .document(Auth.getCurrentUser().getUid())
                .collection(Database.COLLECTION_RECIPES),
            (result, success, exception) -> {
                if (!success) {
                    Log.d("Eitan Debug Recipes", "Failed to load recipe: collection didn't load");
                    sendEvent("Failed to load recipes from database, try again");
                    return;
                }

                if (result.isEmpty()) {
                    generateNewRecipes();
                } else {
                    ArrayList<RecipeItem> recipes = new ArrayList<>();
                    AtomicInteger completed = new AtomicInteger(0);
                    int total = result.getDocuments().size();

                    for (DocumentSnapshot doc : result.getDocuments()) {
                        loadRecipe(doc, (recipe, success2, exception2) -> {
                            if (!success2) {
                                Log.d("Eitan Debug Recipes", "Failed to load recipe image: " + exception2);
                                sendEvent("Failed to load recipe image, try again");
                            }

                            recipes.add(recipe);

                            if (completed.incrementAndGet() >= total) {
                                currentRecipes.postValue(new ArrayList<>(recipes));
                                sendEvent("Loaded database");
                                saveCacheAsync(recipes);
                            }
                        });
                    }
                }
            });
    }

    /**
     * Loads a single recipe's details and image from Firestore.
     * @param document The Firestore document snapshot of the recipe.
     * @param onComplete Callback to execute when loading is finished.
     */
    @OptIn(markerClass = PublicPreviewAPI.class)
    private void loadRecipe(DocumentSnapshot document, OnCompleteRunnable<RecipeItem> onComplete) {
        List<String> productIdsList = (List<String>) document.get(Database.RECIPE_FIELD_PRODUCTIDS);
        if (productIdsList == null) {
            Log.d("Eitan Debug Recipes", "Failed to load recipe: productIds = null");
            sendEvent("Failed to load recipe");
        }
        String[] productIds = productIdsList != null
            ? productIdsList.toArray(new String[0])
            : new String[0];

        String imageName = document.getString(Database.RECIPE_FIELD_IMAGE);

        RecipeItem item = new RecipeItem(
            document.getString(Database.RECIPE_FIELD_NAME),
            document.getString(Database.RECIPE_FIELD_DESC),
            null,
            document.getString(Database.RECIPE_FIELD_INSTRUCTIONS),
            productIds,
            document.getId(),
            imageName,
            null
        );

        if (imageName != null && !imageName.isEmpty()) {
            Database.loadImage(imageName, (image, success, e) -> {
                if (success) item.setImage(image);
                onComplete.onComplete(item, success, e);
            });
        } else {
            onComplete.onComplete(item, true, null);
        }
    }

    /**
     * Saves the current list of recipes to the local cache asynchronously.
     * @param recipes The list of recipes to cache.
     */
    private void saveCacheAsync(List<RecipeItem> recipes) {
        List<RecipeItem> snapshot = new ArrayList<>(recipes);
        new Thread(() -> RecipesCache.saveCache(getApplication(), snapshot)).start();
    }
}