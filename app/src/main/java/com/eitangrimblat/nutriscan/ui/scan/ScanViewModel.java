package com.eitangrimblat.nutriscan.ui.scan;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.camera.core.ImageProxy;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eitangrimblat.nutriscan.data.Nutriments;
import com.eitangrimblat.nutriscan.data.Product;
import com.eitangrimblat.nutriscan.data.ProductResponse;
import com.eitangrimblat.nutriscan.firebase.AI;
import com.eitangrimblat.nutriscan.openfood.ApiClient;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.PublicPreviewAPI;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for ScanFragment, managing barcode and AI scanning logic and results.
 */
public class ScanViewModel extends ViewModel {
    /**
     * Enum defining the types of scanning supported.
     */
    public enum ScanType {
        Barcode,
        AI,
        Receipt
    }

    private boolean allowScanning = false;
    private ScanType scanType = ScanType.Barcode;
    private final AtomicBoolean _isScanning = new AtomicBoolean(false);
    private final MutableLiveData<Boolean> isScanning = new MutableLiveData<>(false);
    private final MutableLiveData<Product> scannedProduct = new MutableLiveData<>();
    private final MutableLiveData<String> uiEvent = new MutableLiveData<>();

    /** @return Whether scanning is currently allowed. */
    public boolean isAllowScanning() {
        return allowScanning;
    }
    /** @param allowScanning Sets whether scanning should be allowed. */
    public void setAllowScanning(boolean allowScanning) {
        this.allowScanning = allowScanning;
    }
    /** @return LiveData indicating if a scan operation is in progress. */
    public LiveData<Boolean> isScanning() {
        return isScanning;
    }
    /** @param scanType Sets the current scan mode (Barcode or AI). */
    public void setScanType(ScanType scanType) {
        this.scanType = scanType;
    }
    /** @return LiveData containing the last successfully scanned product. */
    public LiveData<Product> getScannedProduct() {
        return scannedProduct;
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
     * Triggers a scan operation based on the current scan type.
     * @param imageProxy The CameraX image proxy.
     * @param mediaImage The underlying media image.
     */
    public void scan(ImageProxy imageProxy, Image mediaImage) {
        if (isAllowScanning() && _isScanning.compareAndSet(false, true)) {
            setAllowScanning(false);

            switch (scanType) {
                case Barcode:
                    isScanning.postValue(true);
                    scanBarcode(mediaImage, imageProxy);
                    break;

                case AI:
                    isScanning.postValue(true);
                    scanAI(imageProxy);
                    break;

                default:
                    _isScanning.set(false);
                    isScanning.postValue(false);
                    setAllowScanning(false);
                    imageProxy.close();
                    break;
            }
        } else {
            imageProxy.close();
        }
    }

    /**
     * Performs AI-based image analysis to identify food and its nutritional content.
     * @param imageProxy The image to analyze.
     */
    @OptIn(markerClass = PublicPreviewAPI.class)
    private void scanAI(ImageProxy imageProxy) {
        Bitmap image = imageProxy.toBitmap();
        AI.sendRequest(new Content.Builder()
            .addImage(image)
            .addText("""
            You are an AI that analyzes images of food. Given an image, you must estimate the following information:

            1. Name of the food.
            2. Brands of the food (if identifiable) (separated by comma and a whitespace).
            3. Quantity of the food (mass in grams)
            4. Estimated nutritional values of the full product:
               - energy_kcal (in kcal, double)
               - fat (in grams, double)
               - sugars (in grams, double)
               - salt (in grams, double)

            All values must be estimated based on the image only.

            **Response rules:**
            - If the image contains food, output all the data in this exact format, with ;;; as the separator, no spaces or new lines:
              'FoodName;;;BrandsNames;;;Quantity;;;energy_kcal;;;fat;;;sugars;;;salt'

            - Example:
              Nutella;;;Ferrero, Nutella;;;400 g;;;539;;;30.9;;;56.39;;;0.107

            - If no food can be detected, output exactly:
              'NOFOOD'

            - All numeric values must be in double format (e.g., 123.22).
        """).build(), (response, success, exception) -> {
            try {
                if (success && response != null && response.getText() != null) {
                    Product product = new Product();
                    product.nutriments = new Nutriments();

                    Log.d("Eitan Debug Scan", "AI scan received response: " + response.getText());

                    if (response.getText().contains("NOFOOD")) {
                        Log.d("Eitan Debug Scan", "AI scanned image doesn't contain food");
                        sendEvent("No visible food in image");
                        return;
                    }

                    String[] results = response.getText().replace("\"", "").split(";;;");

                    if (results.length < 7) {
                        Log.d("Eitan Debug Scan", "AI response malformed, got " + results.length + " fields: " + response.getText());
                        sendEvent("Scan failed");
                        return;
                    }

                    product.product_name = results[0];
                    product.brands = results[1];
                    product.quantity = results[2];
                    try {
                        product.nutriments.energy_kcal = Double.valueOf(results[3].trim());
                        product.nutriments.fat = Double.valueOf(results[4].trim());
                        product.nutriments.sugars = Double.valueOf(results[5].trim());
                        product.nutriments.salt = Double.valueOf(results[6].trim());
                    } catch (NumberFormatException e) {
                        Log.d("Eitan Debug Scan", "One or more of ai scan results' nutriments aren't in the correct number format:");
                        sendEvent("Scan failed");
                        e.printStackTrace();
                        return;
                    }

                    scannedProduct.postValue(product);
                } else {
                    Log.d("Eitan Debug Scan", "Error in ai image scan: empty or unsuccessful response " + exception);
                    sendEvent("Scan failed");
                }
            } finally {
                imageProxy.close();
                _isScanning.set(false);
                isScanning.postValue(false);
                setAllowScanning(false);
            }
        });
    }

    /**
     * Performs barcode scanning using ML Kit.
     * @param image The image to scan for barcodes.
     * @param imageProxy The CameraX image proxy.
     */
    private void scanBarcode(Image image, ImageProxy imageProxy) {
        InputImage inputImage = InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees());
        BarcodeScanner scanner = BarcodeScanning.getClient();

        scanner.process(inputImage)
            .addOnSuccessListener(barcodes -> {
                if (barcodes.isEmpty()) {
                    sendEvent("No visible barcode in image");
                    imageProxy.close();
                    _isScanning.set(false);
                    isScanning.postValue(false);
                    setAllowScanning(false);
                    return;
                }

                Barcode b = barcodes.get(0);
                Log.d("Eitan Debug Scan", "Scanned barcode: " + b.getRawValue());

                callFoodAPI(b.getRawValue(), imageProxy);
            })
            .addOnFailureListener(t -> {
                Log.d("Eitan Debug Scan", "Barcode translation failed: " + t.getMessage());
                sendEvent("Scan failed");
                imageProxy.close();
                _isScanning.set(false);
                isScanning.postValue(false);
                setAllowScanning(false);
            });
    }

    /**
     * Fetches product data from the OpenFoodFacts API using the scanned barcode.
     * @param barcode The scanned barcode string.
     * @param imageProxy The CameraX image proxy to be closed.
     */
    private void callFoodAPI(String barcode, ImageProxy imageProxy) {
        ApiClient.getApi().getProduct(barcode).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Product product = response.body().product;
                    if (product != null
                        && product.product_name != null
                        && product.brands != null
                        && product.quantity != null
                        && product.nutriments != null
                        && product.nutriments.energy_kcal != null
                        && product.nutriments.fat != null
                        && product.nutriments.sugars != null
                        && product.nutriments.salt != null) {

                        Log.d("Eitan Debug Scan", "Product: " + product.product_name);
                        scannedProduct.postValue(product);
                    } else {
                        Log.d("Eitan Debug Scan", "No product found");
                        sendEvent("No product found... Try again");
                    }
                } else {
                    Log.d("Eitan Debug Scan", "No response from OpenFoodAPI");
                    sendEvent("Scan failed");
                }

                imageProxy.close();
                _isScanning.set(false);
                isScanning.setValue(false);
                setAllowScanning(false);
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Log.d("Eitan Debug Scan", "Error calling OpenFoodAPI: " + t.getMessage());
                sendEvent("Scan failed");

                imageProxy.close();
                _isScanning.set(false);
                isScanning.setValue(false);
                setAllowScanning(false);
            }
        });
    }
}