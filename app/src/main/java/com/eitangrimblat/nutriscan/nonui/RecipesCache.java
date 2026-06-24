package com.eitangrimblat.nutriscan.nonui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.eitangrimblat.nutriscan.data.RecipeItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages local caching of recipe metadata and images using SharedPreferences and internal storage.
 */
public class RecipesCache {
    private static final String TAG = "Eitan Debug Recipes Cache";
    private static final String PREFS_NAME  = "recipe_cache";
    private static final String KEY_RECIPES = "recipes_json";
    private static final String IMG_PREFIX  = "recipe_img_";
    private static final String IMG_SUFFIX  = ".jpg";

    /**
     * Saves a list of recipes and their associated images to the local cache.
     *
     * @param context The application context.
     * @param recipes The list of recipes to cache.
     */
    public static void saveCache(Context context, List<RecipeItem> recipes) {
        clearCache(context);
        saveMetadata(context, recipes);
        for (RecipeItem recipe : recipes) {
            if (recipe.getImage() != null
                && recipe.getImageId() != null
                && !recipe.getImageId().isEmpty()) {
                saveBitmap(context, recipe.getImageId(), recipe.getImage());
            }
        }
        Log.d(TAG, "Cache saved - " + recipes.size() + " recipes");
    }

    /**
     * Loads the cached recipes and their images.
     *
     * @param context The application context.
     * @return A list of cached {@link RecipeItem} objects.
     */
    public static List<RecipeItem> loadCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECIPES, null);
        if (json == null || json.isEmpty())
            return new ArrayList<>();

        List<RecipeItem> recipes = new Gson().fromJson(json, new TypeToken<List<RecipeItem>>() {}.getType());
        if (recipes == null)
            return new ArrayList<>();

        for (RecipeItem recipe : recipes) {
            if (recipe.getImageId() != null && !recipe.getImageId().isEmpty()) {
                Bitmap bmp = loadBitmap(context, recipe.getImageId());
                if (bmp != null)
                    recipe.setImage(bmp);
            }
        }

        Log.d(TAG, "Cache loaded - " + recipes.size() + " recipes");
        return recipes;
    }

    /**
     * Clears all cached recipe metadata and image files.
     *
     * @param context The application context.
     */
    public static void clearCache(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply();

        File[] images = context.getFilesDir()
            .listFiles((dir, name) -> name.startsWith(IMG_PREFIX) && name.endsWith(IMG_SUFFIX));
        if (images != null) {
            for (File f : images) {
                if (!f.delete()) Log.w(TAG, "Could not delete: " + f.getName());
            }
        }
        Log.d(TAG, "Cache cleared");
    }

    /**
     * Serializes and saves recipe metadata to SharedPreferences.
     */
    private static void saveMetadata(Context context, List<RecipeItem> recipes) {
        String json = new Gson().toJson(recipes);   // Bitmap fields are transient- skipped
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECIPES, json)
            .apply();
    }

    /**
     * Saves a Bitmap image to internal storage.
     */
    private static void saveBitmap(Context context, String imageId, Bitmap bitmap) {
        File file = imageFile(context, imageId);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save bitmap – imageId=" + imageId, e);
        }
    }

    /**
     * Loads a Bitmap image from internal storage.
     */
    private static Bitmap loadBitmap(Context context, String imageId) {
        File file = imageFile(context, imageId);
        return file.exists() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
    }

    /**
     * Generates a File object for a specific cached image ID.
     */
    private static File imageFile(Context context, String imageId) {
        return new File(context.getFilesDir(), IMG_PREFIX + imageId + IMG_SUFFIX);
    }
}
