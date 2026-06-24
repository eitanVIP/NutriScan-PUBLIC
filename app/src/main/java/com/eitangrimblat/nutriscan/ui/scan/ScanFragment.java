package com.eitangrimblat.nutriscan.ui.scan;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.data.Product;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.firebase.Database;
import com.eitangrimblat.nutriscan.ui.viewhelpers.BottomSheet;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Fragment that handles barcode and AI-based food scanning using CameraX.
 */
public class ScanFragment extends Fragment {
    private View fragmentView;
    private ScanViewModel viewModel;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private BottomSheet<MaterialCardView> resultCard;
    private ActivityResultLauncher<String[]> cameraPermissionActivityResult = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            String permissionName = entry.getKey();
            Boolean isGranted = entry.getValue();

            if (isGranted) {
                startCamera();
            } else {
                Toast.makeText(requireContext(), "No Camera Access", Toast.LENGTH_SHORT).show();
                Log.d("Eitan Debug Scan", "No camera access");
            }
        }
    });

    private TextView tvScanCardName;
    private TextView tvScanCardBrands;
    private TextView tvScanCardEnergyContent;
    private TextView tvScanCardFatContent;
    private TextView tvScanCardSaltContent;
    private TextView tvScanCardSugarsContent;
    private SwipeRefreshLayout srlScan;

    /**
     * Checks for camera permissions and starts the camera if granted.
     */
    private void startCameraWhenPermission() {
        if (checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionActivityResult.launch(new String[] { Manifest.permission.CAMERA });
        }
    }

    /**
     * Initializes the CameraX ProcessCameraProvider.
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * Binds the camera preview and image analysis use cases to the fragment's lifecycle.
     * NOTE: The binding code of the camera to the application was done with the help of AI.
     */
    private void bindCamera() {
        CameraSelector cameraSelector =
                new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis =
                new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        analysis.setAnalyzer(Executors.newSingleThreadExecutor(), imageProxy -> {
            @SuppressLint("UnsafeOptInUsageError")
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null && viewModel.isAllowScanning()) {
                viewModel.scan(imageProxy, mediaImage);
            } else {
                imageProxy.close();
            }
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis);
    }

    /**
     * Opens the result card bottom sheet to show scanned product details.
     * @param product The product to display.
     */
    public void openResultCard(Product product) {
        resultCard.open(() -> {
            tvScanCardName.setText(product.product_name + " " + product.quantity);
            tvScanCardBrands.setText(product.brands);
            tvScanCardEnergyContent.setText(product.nutriments.energy_kcal + " kcal");
            tvScanCardFatContent.setText(product.nutriments.fat + " g");
            tvScanCardSaltContent.setText(product.nutriments.salt + " g");
            tvScanCardSugarsContent.setText(product.nutriments.sugars + " g");
        });
    }

    /**
     * Closes the result card bottom sheet.
     */
    public void closeResultCard() {
        resultCard.close(() -> {});
    }

    /**
     * Callback for when a product is successfully scanned.
     * @param product The scanned product.
     */
    private void onScan(Product product) {
        openResultCard(product);
    }

    /**
     * Initializes buttons and click listeners for scan type selection and saving products.
     */
    private void initButtons() {
        ((MaterialButtonToggleGroup) fragmentView.findViewById(R.id.scanNav)).addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.scanNavBarcode) {
                    viewModel.setScanType(ScanViewModel.ScanType.Barcode);
                } else if (checkedId == R.id.scanNavAI) {
                    viewModel.setScanType(ScanViewModel.ScanType.AI);
                }/* else if (checkedId == R.id.scanNavReceipt) {
                    viewModel.setScanType(ScanViewModel.ScanType.Receipt);
                }*/
            }
        });

        fragmentView.findViewById(R.id.bcmScanShutter).setOnClickListener(v -> {
            if (!viewModel.isScanning().getValue())
                viewModel.setAllowScanning(true);
            else {
                Snackbar.make(fragmentView, "Already scanning", Snackbar.LENGTH_LONG).show();
                Log.d("Eitan Debug Scan", "Already scanning - user clicked shutter button while scanning");
            }
        });

        fragmentView.findViewById(R.id.bcmScanSave).setOnClickListener(v -> {
            Product product = viewModel.getScannedProduct().getValue();
            if (product == null) {
                Log.d("Eitan Debug Scan", "Tried to save a null product");
                return;
            }

            Database.addDocument(Database.getCollection(Database.COLLECTION_USERS).document(Auth.getCurrentUser().getUid()).collection(Database.COLLECTION_PRODUCTS), Map.of(
                    Database.PRODUCT_FIELD_NAME, product.product_name,
                    Database.PRODUCT_FIELD_BRANDS, product.brands,
                    Database.PRODUCT_FIELD_QUANTITY, product.quantity,
                    Database.PRODUCT_FIELD_ENERGY, product.nutriments.energy_kcal,
                    Database.PRODUCT_FIELD_SALT, product.nutriments.salt,
                    Database.PRODUCT_FIELD_SUGARS, product.nutriments.sugars,
                    Database.PRODUCT_FIELD_FAT, product.nutriments.fat
            ), (String id, boolean success, String exception) -> {
                if (success) {
                    Snackbar.make(fragmentView, "Saved product to database", Snackbar.LENGTH_LONG).show();
                    closeResultCard();
                } else {
                    Log.d("Eitan Debug Scan", "Failed to save scanned product in database: " + exception);
                    Snackbar.make(fragmentView, "Failed to save product, try again", Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Initializes the ScanViewModel.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ScanViewModel.class);
    }

    /**
     * Inflates the fragment layout and initializes UI components, observers, and camera.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_scan, container, false);

        tvScanCardName = fragmentView.findViewById(R.id.tvScanCardName);
        tvScanCardBrands = fragmentView.findViewById(R.id.tvScanCardBrands);
        tvScanCardEnergyContent = fragmentView.findViewById(R.id.tvScanCardEnergyContent);
        tvScanCardFatContent = fragmentView.findViewById(R.id.tvScanCardFatContent);
        tvScanCardSaltContent = fragmentView.findViewById(R.id.tvScanCardSaltContent);
        tvScanCardSugarsContent = fragmentView.findViewById(R.id.tvScanCardSugarsContent);
        previewView = fragmentView.findViewById(R.id.surScanCamera);

        srlScan = fragmentView.findViewById(R.id.srlScan);
        srlScan.setEnabled(false);

        viewModel.getScannedProduct().observe(getViewLifecycleOwner(), this::onScan);
        viewModel.getUiEvent().observe(getViewLifecycleOwner(), event -> {
            Snackbar.make(fragmentView, event, Snackbar.LENGTH_LONG).show();
        });
        viewModel.isScanning().observe(getViewLifecycleOwner(), isScanning -> {
            srlScan.setRefreshing(isScanning);
        });

        resultCard = new BottomSheet<>(fragmentView.findViewById(R.id.scanCardResult), getResources());
        initButtons();
        startCameraWhenPermission();

        return fragmentView;
    }
}