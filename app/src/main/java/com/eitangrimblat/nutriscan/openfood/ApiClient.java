package com.eitangrimblat.nutriscan.openfood;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton client for accessing the Open Food Facts API via Retrofit.
 */
public class ApiClient {
    private static OpenFoodFactsApi api;

    /**
     * Provides a configured instance of {@link OpenFoodFactsApi}.
     *
     * @return The Retrofit API service.
     */
    public static OpenFoodFactsApi getApi() {
        if (api == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://world.openfoodfacts.org/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            api = retrofit.create(OpenFoodFactsApi.class);
        }
        return api;
    }
}
