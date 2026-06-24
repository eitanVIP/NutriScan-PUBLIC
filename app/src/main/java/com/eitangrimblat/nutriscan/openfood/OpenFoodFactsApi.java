package com.eitangrimblat.nutriscan.openfood;

import com.eitangrimblat.nutriscan.data.ProductResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit interface defining the endpoints for the Open Food Facts API.
 */
public interface OpenFoodFactsApi {
    /**
     * Retrieves product information for a given barcode.
     *
     * @param barcode The product barcode.
     * @return A call returning the {@link ProductResponse}.
     */
    @GET("api/v0/product/{barcode}.json")
    Call<ProductResponse> getProduct(@Path("barcode") String barcode);
}
