package com.eitangrimblat.nutriscan.data;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the nutritional information of a product per 100g.
 */
public class Nutriments {
    @SerializedName("energy-kcal_100g")
    public Double energy_kcal;

    @SerializedName("fat_100g")
    public Double fat;

    @SerializedName("sugars_100g")
    public Double sugars;

    @SerializedName("salt_100g")
    public Double salt;
}
