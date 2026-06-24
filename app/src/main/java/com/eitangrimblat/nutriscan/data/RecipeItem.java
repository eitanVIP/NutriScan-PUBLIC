package com.eitangrimblat.nutriscan.data;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Represents a generated recipe, including its ingredients, instructions, and visual assets.
 */
public class RecipeItem implements Serializable {
    private String name;
    private String description;
    private String instructions;
    private String[] productsIds;
    private String recipeId;
    private transient Bitmap image;
    private String imageId;
    private String workerTransferTempImagePath;

    /**
     * Initializes a recipe item with all required fields.
     */
    public RecipeItem(String name, String description, Bitmap image, String instructions, String[] productsIds, String recipeId, String imageId, String workerTransferTempImagePath) {
        this.name = name;
        this.description = description;
        this.image = image;
        this.instructions = instructions;
        this.productsIds = productsIds;
        this.recipeId = recipeId;
        this.imageId = imageId;
        this.workerTransferTempImagePath = workerTransferTempImagePath;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getInstructions() {
        return instructions;
    }

    public String[] getProductsIds() {
        return productsIds;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getImageId() {
        return imageId;
    }

    public String getWorkerTransferTempImagePath() {
        return workerTransferTempImagePath;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public void setWorkerTransferTempImagePath(String workerTransferTempImagePath) {
        this.workerTransferTempImagePath = workerTransferTempImagePath;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setProductsIds(String[] productsIds) {
        this.productsIds = productsIds;
    }
}