package com.eitangrimblat.nutriscan.ui.recipes;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.data.Product;
import com.eitangrimblat.nutriscan.data.ProductItem;
import com.eitangrimblat.nutriscan.data.RecipeItem;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.firebase.Database;
import com.eitangrimblat.nutriscan.ui.activities.MainAppActivity;
import com.eitangrimblat.nutriscan.ui.viewhelpers.ProductAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays detailed information about a specific recipe, including its ingredients.
 */
public class RecipeFragment extends Fragment {
    private static final String ARG_RECIPE = "arg_recipe";
    private RecipeItem recipeItem;
    private View fragmentView;
    private ProductAdapter adapter;
    private List<ProductItem> currentlyDeleting = new ArrayList<>();

    /**
     * Creates a new instance of RecipeFragment with the given recipe item.
     * @param item The recipe to display.
     * @return A new instance of RecipeFragment.
     */
    public static RecipeFragment newInstance(RecipeItem item) {
        RecipeFragment fragment = new RecipeFragment();
        Bundle args = new Bundle();

        args.putSerializable(ARG_RECIPE, item);

        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Handles the deletion of an ingredient (product) from the user's history.
     * @param item The product item to delete.
     * @param position The position of the item in the list.
     */
    private void onIngredientDelete(ProductItem item, int position) {
        if (currentlyDeleting.contains(item))
            return;

        currentlyDeleting.add(item);

        Database.deleteDocument(Database.getCollection(Database.COLLECTION_USERS)
                .document(Auth.getCurrentUser().getUid())
                .collection(Database.COLLECTION_PRODUCTS),
            item.getId(), (unused, delSuccess, delException) -> {
                currentlyDeleting.remove(item);
                if (!delSuccess) {
                    Log.d("Eitan Debug Recipe", "Failed to delete product: " + delException);
                    Snackbar.make(fragmentView, "Failed to delete product, try again later", Snackbar.LENGTH_LONG).show();
                }
            });
        adapter.deleteItem(item, position);
    }

    /**
     * Configures the RecyclerView for displaying the recipe's ingredients.
     */
    private void setupRecyclerView() {
        RecyclerView rvRecipe = fragmentView.findViewById(R.id.rvRecipe);
        rvRecipe.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ProductAdapter(
            new ArrayList<>(),
            this::onIngredientDelete,
            item -> {}
        );
        rvRecipe.setAdapter(adapter);
    }

    /**
     * Checks if a specific product ID is part of the current recipe.
     * @param productId The ID of the product to check.
     * @return True if the product is part of the recipe, false otherwise.
     */
    private boolean isProductPartOfRecipe(String productId) {
        for (String id : recipeItem.getProductsIds()) {
            if (id.equals(productId))
                return true;
        }

        return false;
    }

    /**
     * Initializes the UI views with recipe details and handles the back button click.
     */
    private void setupViews() {
        ((TextView)fragmentView.findViewById(R.id.tvRecipeTitle)).setText(recipeItem.getName());
        ((TextView)fragmentView.findViewById(R.id.tvRecipeDescription)).setText(recipeItem.getDescription());
        ((TextView)fragmentView.findViewById(R.id.tvRecipeInstructions)).setText(recipeItem.getInstructions());

        if (recipeItem.getImage() != null) {
            ((ImageView)fragmentView.findViewById(R.id.ivRecipeHeader)).setImageBitmap(recipeItem.getImage());
        } else {
            ((ImageView)fragmentView.findViewById(R.id.ivRecipeHeader)).setImageResource(R.drawable.ic_launcher_background);
            Snackbar.make(fragmentView, "Loading recipe image...", Snackbar.LENGTH_LONG).show();

            Database.loadImage(recipeItem.getImageId(), (image, success, exception) -> {
                if (success && isAdded() && fragmentView != null) {
                    recipeItem.setImage(image);
                    ((ImageView)fragmentView.findViewById(R.id.ivRecipeHeader)).setImageBitmap(image);
                } else if (!success && isAdded() && fragmentView != null) {
                    Snackbar.make(fragmentView, "Failed to load recipe image", Snackbar.LENGTH_LONG).show();
                }
            });
        }

        fragmentView.findViewById(R.id.bcmRecipeBack).setOnClickListener(v -> {
            if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity)getActivity()).switchFragment(new RecipesFragment());
            }
        });
    }

    /**
     * Retrieves the recipe item from the fragment arguments.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            recipeItem = (RecipeItem) getArguments().getSerializable(ARG_RECIPE);
        }
    }

    /**
     * Inflates the fragment layout, initializes views, and loads ingredient data from Firestore.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_recipe, container, false);

        if (recipeItem == null) {
            Log.d("Eitan Debug Recipe", "Failed to open recipe fragment: null recipe");
            Snackbar.make(fragmentView, "Failed to open recipe fragment", Snackbar.LENGTH_LONG).show();

            if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity)getActivity()).switchFragment(new RecipesFragment());
            }
        }

        setupViews();
        setupRecyclerView();

        Database.loadCollection(Database.getCollection(Database.COLLECTION_USERS).document(Auth.getCurrentUser().getUid()).collection(Database.COLLECTION_PRODUCTS), (collection, success, exception) -> {
            if (!success) {
                Log.d("Eitan Debug Recipe", "Failed to load products: " + exception);
                Snackbar.make(fragmentView, "Failed to load products, try again later", Snackbar.LENGTH_LONG).show();
                return;
            }

            List<ProductItem> products = new ArrayList<>();
            for (DocumentSnapshot productDocument : collection.getDocuments()) {
                if (!isProductPartOfRecipe(productDocument.getId()))
                    continue;

                Product product = new Product();
                product.product_name = productDocument.getString(Database.PRODUCT_FIELD_NAME);
                products.add(new ProductItem(
                    product,
                    productDocument.getId()
                ));
            }

            adapter.updateList(products);
        });

        return fragmentView;
    }
}