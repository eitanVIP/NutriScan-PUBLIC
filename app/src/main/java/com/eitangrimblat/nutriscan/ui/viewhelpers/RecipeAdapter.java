package com.eitangrimblat.nutriscan.ui.viewhelpers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.data.RecipeItem;

import java.util.List;
import java.util.function.Consumer;

/**
 * RecyclerView adapter for displaying a list of recipes in cards.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private List<RecipeItem> recipeList;
    private Consumer<RecipeItem> onOpenRecipe;

    public RecipeAdapter(List<RecipeItem> recipeList, Consumer<RecipeItem> onOpenRecipe) {
        this.recipeList = recipeList;
        this.onOpenRecipe = onOpenRecipe;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        RecipeItem item = recipeList.get(position);

        holder.name.setText(item.getName());
        holder.description.setText(item.getDescription());
        holder.image.setImageBitmap(item.getImage());

        holder.recipeContainer.setOnClickListener(v -> {
            onOpenRecipe.accept(item);
        });
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    /**
     * Deletes a recipe item from the list and notifies the adapter.
     * @param position The position of the item to remove.
     */
    public void deleteItem(int position) {
        recipeList.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * Returns the recipe item at the specified position.
     * @param position The position of the item.
     * @return The RecipeItem at that position.
     */
    public RecipeItem getItem(int position) {
        return recipeList.get(position);
    }

    /**
     * Updates the adapter's list and refreshes the UI.
     * @param newList The new list of recipes.
     */
    public void updateList(List<RecipeItem> newList) {
        recipeList = newList;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class for recipe cards.
     */
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView name, description;
        ImageView image;
        ConstraintLayout recipeContainer;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            description = itemView.findViewById(R.id.description);
            image = itemView.findViewById(R.id.image);
            recipeContainer = itemView.findViewById(R.id.recipeContainer);
        }
    }
}