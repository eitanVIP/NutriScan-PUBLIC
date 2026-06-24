package com.eitangrimblat.nutriscan.ui.recipes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.data.RecipeItem;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.ui.activities.MainActivity;
import com.eitangrimblat.nutriscan.ui.activities.MainAppActivity;
import com.eitangrimblat.nutriscan.ui.viewhelpers.RecipeAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays a list of generated recipes.
 * Handles recipe generation via RecipesWorker and manages the recipe list UI.
 */
public class RecipesFragment extends Fragment {
    private View fragmentView;
    private RecipesViewModel viewModel;
    private RecyclerView rvRecipes;
    private SwipeRefreshLayout srlRecipes;
    private RecipeAdapter recipeAdapter;
    private boolean isGenerationRefreshing = false;

    /**
     * Starts observing the RecipesWorker's progress and results.
     */
    private void startObservingWorker() {
        WorkManager wm = WorkManager.getInstance(requireContext());
        wm.pruneWork();

        wm.getWorkInfosForUniqueWorkLiveData(RecipesViewModel.WORK_NAME + "_" + Auth.getCurrentUser().getUid())
            .observe(getViewLifecycleOwner(), workInfoList -> {
                if (workInfoList == null || workInfoList.isEmpty()) return;

                WorkInfo info = workInfoList.get(0);

                // Handle progress events (snackbar messages from the worker)
                String event = info.getProgress().getString("event_message");
                if (event != null && !event.isEmpty()) {
                    Snackbar.make(fragmentView, event, Snackbar.LENGTH_LONG).show();
                    Log.d("Eitan Debug Recipes", event);
                }

                if (info.getState().isFinished()) {
                    isGenerationRefreshing = false;
                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        String json = info.getOutputData().getString("recipes");
                        List<RecipeItem> recipes = new Gson().fromJson(json, new TypeToken<List<RecipeItem>>(){}.getType());
                        viewModel.addRecipes(recipes);
                    } else {
                        srlRecipes.setRefreshing(false);

                        event = info.getOutputData().getString("event_message");
                        if (event != null && !event.isEmpty())
                            Snackbar.make(fragmentView, event, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
    }

    /**
     * Updates the UI with the list of recipes and stops the refresh animation.
     * @param recipes The list of recipes to display.
     */
    private void showRecipes(List<RecipeItem> recipes) {
        if (recipes.isEmpty())
            return;

        recipeAdapter.updateList(recipes);

        if (!isGenerationRefreshing)
            srlRecipes.setRefreshing(false);
    }

    /**
     * Initializes UI views, pull-to-refresh, and swipe-to-delete functionality.
     */
    private void setupViews() {
        rvRecipes = fragmentView.findViewById(R.id.rvRecipes);
        srlRecipes = fragmentView.findViewById(R.id.srlRecipes);

        rvRecipes.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!srlRecipes.isRefreshing()) {
                    boolean isAtTop = !recyclerView.canScrollVertically(-1);
                    srlRecipes.setEnabled(isAtTop);
                }
            }
        });

        srlRecipes.setRefreshing(true);
        srlRecipes.setOnRefreshListener(() -> {
            isGenerationRefreshing = true;
            viewModel.generateNewRecipes();
        });
        srlRecipes.setDistanceToTriggerSync(600);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                RecipeItem deletedItem = recipeAdapter.getItem(position);

                recipeAdapter.deleteItem(position);

                viewModel.deleteRecipe(deletedItem);
            }
        }).attachToRecyclerView(rvRecipes);
    }

    /**
     * Configures the RecyclerView with a RecipeAdapter.
     */
    private void setupRecyclerView() {
        rvRecipes.setLayoutManager(new LinearLayoutManager(getContext()));

        recipeAdapter = new RecipeAdapter(new ArrayList<>(), recipeItem -> {
            if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity) getActivity()).switchFragment(RecipeFragment.newInstance(recipeItem));
            }
        });

        rvRecipes.setAdapter(recipeAdapter);
    }

    /**
     * Sets up observers for UI events and recipe list updates.
     */
    private void setupObservers() {
        viewModel.getUiEvent().observe(getViewLifecycleOwner(), event -> {
            Snackbar.make(fragmentView, event, Snackbar.LENGTH_LONG).show();
        });

        viewModel.getCurrentRecipes().observe(getViewLifecycleOwner(), this::showRecipes);

        startObservingWorker();
    }

    /**
     * Initializes the RecipesViewModel.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RecipesViewModel.class);
    }

    /**
     * Inflates the fragment layout and triggers UI and observer setup.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_recipes, container, false);

        if (Auth.getCurrentUser() == null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }

        setupViews();
        setupRecyclerView();
        setupObservers();

        return fragmentView;
    }
}