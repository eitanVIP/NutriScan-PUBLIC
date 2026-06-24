package com.eitangrimblat.nutriscan.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.data.Product;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.ui.activities.MainActivity;
import com.eitangrimblat.nutriscan.ui.viewhelpers.BottomSheet;
import com.eitangrimblat.nutriscan.ui.viewhelpers.ProductAdapter;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

/**
 * Fragment responsible for displaying the user's scanned products history.
 */
public class HomeFragment extends Fragment {
    private View fragmentView;
    private HomeViewModel viewModel;
    private ProductAdapter adapter;

    private SwipeRefreshLayout srlHome;
    private BottomSheet<MaterialCardView> resultCard;

    private TextView tvScanCardName;
    private TextView tvScanCardBrands;
    private TextView tvScanCardEnergyContent;
    private TextView tvScanCardFatContent;
    private TextView tvScanCardSaltContent;
    private TextView tvScanCardSugarsContent;

    /**
     * Initializes the ViewModel and redirects to MainActivity if the user is not authenticated.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        if (Auth.getCurrentUser() == null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    /**
     * Inflates the fragment layout and initializes UI components and observers.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_home, container, false);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupViews();
        setupRecyclerView();
        setupSearchView();
        observeViewModel();

        viewModel.loadProducts();

        return fragmentView;
    }

    /**
     * Initializes the UI views and the product details bottom sheet.
     */
    private void setupViews() {
        srlHome = fragmentView.findViewById(R.id.srlHome);
        srlHome.setEnabled(false);

        tvScanCardName = fragmentView.findViewById(R.id.tvScanCardName);
        tvScanCardBrands = fragmentView.findViewById(R.id.tvScanCardBrands);
        tvScanCardEnergyContent = fragmentView.findViewById(R.id.tvScanCardEnergyContent);
        tvScanCardFatContent = fragmentView.findViewById(R.id.tvScanCardFatContent);
        tvScanCardSaltContent = fragmentView.findViewById(R.id.tvScanCardSaltContent);
        tvScanCardSugarsContent = fragmentView.findViewById(R.id.tvScanCardSugarsContent);

        fragmentView.findViewById(R.id.tvScanCardTitle).setVisibility(View.GONE);
        fragmentView.findViewById(R.id.bcmScanSave).setVisibility(View.GONE);

        resultCard = new BottomSheet<>(fragmentView.findViewById(R.id.scanCardResult), getResources());
    }

    /**
     * Configures the RecyclerView with a ProductAdapter for displaying the list of products.
     */
    private void setupRecyclerView() {
        RecyclerView rvHome = fragmentView.findViewById(R.id.rvHome);
        rvHome.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ProductAdapter(
            new ArrayList<>(),
            (item, position) -> viewModel.deleteProduct(item.getId()),
            item -> openResultCard(item.getProduct())
        );
        rvHome.setAdapter(adapter);
    }

    /**
     * Sets up the SearchView to allow filtering the product list.
     */
    private void setupSearchView() {
        SearchView searchView = fragmentView.findViewById(R.id.searchViewHome);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.filter(newText);
                return true;
            }
        });
    }

    /**
     * Observes the ViewModel's LiveData for product updates, loading states, and events.
     */
    private void observeViewModel() {
        viewModel.getProducts().observe(getViewLifecycleOwner(), items -> adapter.updateList(items));

        viewModel.isLoading().observe(getViewLifecycleOwner(), state -> {
            srlHome.setRefreshing(state);
        });

        viewModel.getEvent().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null)
                Snackbar.make(fragmentView, msg, Snackbar.LENGTH_LONG).show();
        });
    }

    /**
     * Opens the result card bottom sheet to show detailed information about a product.
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
}