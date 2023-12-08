package com.example.inventorymanager.inventory;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.inventorymanager.R;
import com.example.inventorymanager.item.InventoryItem;
import com.example.inventorymanager.item.ItemFragment;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;

/**
 * Adapter for Inventory RecyclerView
 */
public class InventoryAdapter extends FirestoreRecyclerAdapter<InventoryItem, InventoryAdapter.InventoryViewHolder> {

    private NavController navController;

    public InventoryAdapter(@NonNull FirestoreRecyclerOptions<InventoryItem> options, NavController navController) {
        super(options);
        this.navController = navController;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position, @NonNull InventoryItem model) {
        // Bind the data to views
        holder.bind(model, position);
        // Get the document id of the item at the current position
        String documentId = getSnapshots().getSnapshot(position).getId();
        holder.itemView.setOnClickListener(v -> {
            // Handle item click
            openItemDetailsFragment(model, documentId);
        });
    }

    private void openItemDetailsFragment(InventoryItem item, String documentId) {
        // Create a bundle to pass item details as arguments
        Bundle bundle = new Bundle();
        bundle.putString(ItemFragment.EXTRA_ITEM_DOCUMENT_ID, documentId);

        navController.navigate(R.id.action_inventoryFragment_to_itemFragment, bundle);
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView itemTextView;
        ImageView  itemImageView;
        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            cardView = itemView.findViewById(R.id.item_card_view);
            itemTextView = itemView.findViewById(R.id.item_text_view);
            itemImageView = itemView.findViewById(R.id.item_image_view);
        }

        public void bind(InventoryItem item, int position) {
            itemTextView.setText(item.getItem_name());
            // Load and display the image using Glide
            if (item.getImage_path() != null && !item.getImage_path().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImage_path())
                        .placeholder(R.drawable.placeholder_image)
                        .into(itemImageView);
            } else {
                // Handle the case when there is no image path
                itemImageView.setImageResource(R.drawable.placeholder_image);  // Placeholder image resource
            }
        }
    }
}
