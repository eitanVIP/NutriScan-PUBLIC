package com.eitangrimblat.nutriscan.ui.home;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eitangrimblat.nutriscan.data.Nutriments;
import com.eitangrimblat.nutriscan.data.Product;
import com.eitangrimblat.nutriscan.data.ProductItem;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.firebase.Database;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for HomeFragment, managing the loading, deletion, and filtering of scanned products.
 */
public class HomeViewModel extends ViewModel {
    private final List<ProductItem> allProducts = new ArrayList<>();

    private final MutableLiveData<List<ProductItem>> products = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> event = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    /** @return LiveData containing the list of filtered product items. */
    public LiveData<List<ProductItem>> getProducts() {
        return products;
    }
    /** @return LiveData for notifying UI about success or error messages. */
    public LiveData<String> getEvent() {
        return event;
    }
    /** @return LiveData indicating if a data operation is in progress. */
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    /**
     * Loads the scanned products from Firestore for the current user.
     */
    public void loadProducts() {
        isLoading.postValue(true);

        Database.loadCollection(
            Database.getCollection(Database.COLLECTION_USERS)
                .document(Auth.getCurrentUser().getUid())
                .collection(Database.COLLECTION_PRODUCTS),
            (collection, success, exception) -> {
                if (!success) {
                    Log.d("Eitan Debug Home", "Failed to load products: " + exception);
                    event.postValue("Failed to load products, try again later");
                    isLoading.postValue(false);
                    return;
                }

                allProducts.clear();
                for (DocumentSnapshot doc : collection.getDocuments()) {
                    Product product = new Product();
                    product.nutriments = new Nutriments();
                    product.product_name = doc.getString(Database.PRODUCT_FIELD_NAME);
                    product.brands = doc.getString(Database.PRODUCT_FIELD_BRANDS);
                    product.quantity = doc.getString(Database.PRODUCT_FIELD_QUANTITY);
                    product.nutriments.energy_kcal = doc.getDouble(Database.PRODUCT_FIELD_ENERGY);
                    product.nutriments.fat = doc.getDouble(Database.PRODUCT_FIELD_FAT);
                    product.nutriments.sugars = doc.getDouble(Database.PRODUCT_FIELD_SUGARS);
                    product.nutriments.salt = doc.getDouble(Database.PRODUCT_FIELD_SALT);
                    allProducts.add(new ProductItem(product, doc.getId()));
                }

                products.postValue(new ArrayList<>(allProducts));
                isLoading.postValue(false);
            });
    }

    /**
     * Deletes a specific product from the user's history in Firestore.
     * @param id The Firestore document ID of the product to delete.
     */
    public void deleteProduct(String id) {
        Database.deleteDocument(
            Database.getCollection(Database.COLLECTION_USERS)
                .document(Auth.getCurrentUser().getUid())
                .collection(Database.COLLECTION_PRODUCTS),
            id, (unused, success, exception) -> {
                if (success) {
                    event.postValue("Product removed");
                    loadProducts();
                } else {
                    Log.d("Eitan Debug Home", "Error removing product: " + exception);
                    event.postValue("Error removing product, try again later");
                }
            });
    }

    /**
     * Filters the product list based on the given query string.
     * @param query The search query to filter product names by.
     */
    public void filter(String query) {
        if (query == null || query.isEmpty()) {
            products.postValue(new ArrayList<>(allProducts));
            return;
        }
        String lower = query.toLowerCase();
        List<ProductItem> filtered = new ArrayList<>();
        for (ProductItem item : allProducts) {
            String name = item.getProduct().product_name;
            if (name != null && name.toLowerCase().contains(lower)) {
                filtered.add(item);
            }
        }
        products.postValue(filtered);
    }
}