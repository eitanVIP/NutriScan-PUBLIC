package com.eitangrimblat.nutriscan.ui.viewhelpers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.data.ProductItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * RecyclerView adapter for displaying a list of scanned products.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private List<ProductItem> list;
    private OnItemRemoveListener onRemoveListener;
    private OnClickListener onClickListener;

    /** Interface for handling product removal events. */
    public interface OnItemRemoveListener {
        void onRemove(ProductItem item, int position);
    }

    /** Interface for handling product click events. */
    public interface OnClickListener {
        void onClick(ProductItem item);
    }

    public ProductAdapter(List<ProductItem> items, OnItemRemoveListener onRemoveListener, OnClickListener onClickListener) {
        this.list = items;
        this.onRemoveListener = onRemoveListener;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem item = list.get(position);
        holder.tvName.setText(item.getProduct().product_name);
        holder.btnRemove.setOnClickListener(v -> onRemoveListener.onRemove(item, position));
        holder.card.setOnClickListener(v -> onClickListener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Updates the adapter's list and refreshes the UI.
     * @param newList The new list of products.
     */
    public void updateList(List<ProductItem> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    /**
     * Removes a product item from the list and notifies the adapter.
     * @param item The item to remove.
     * @param position The position of the item.
     */
    public void deleteItem(ProductItem item, int position) {
        list.remove(item);
        notifyItemRemoved(position);
    }

    /**
     * ViewHolder class for product cards.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        MaterialButton btnRemove;
        MaterialCardView card;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvName = itemView.findViewById(R.id.tvProductName);
            btnRemove = itemView.findViewById(R.id.bcmProductRemove);
        }
    }
}