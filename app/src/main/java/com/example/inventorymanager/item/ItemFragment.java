package com.example.inventorymanager.item;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.inventorymanager.utils.FirebaseAuthManager;
import com.example.inventorymanager.R;
import com.example.inventorymanager.utils.ToastUtils;
import com.example.inventorymanager.databinding.FragmentItemBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Fragment to display the details of an item
 */
public class ItemFragment extends Fragment {
    private FragmentItemBinding binding;
    private FirebaseAuth mAuth;
    public static final String EXTRA_ITEM_DOCUMENT_ID = "extra_item_document_id";
    private NavController navController;
    private String itemImagePath;
    private String itemDocumentId;
    private DocumentReference documentRef;
    private Drawable loadedDrawable;
    private InventoryItem inventoryItem;
    private ListenerRegistration itemListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuthManager.getInstance();

        // Setup Transitions
        MaterialSharedAxis sharedAxis = new MaterialSharedAxis(MaterialSharedAxis.X, true);
        MaterialSharedAxis sharedAxis2 = new MaterialSharedAxis(MaterialSharedAxis.X, false);

        setEnterTransition(sharedAxis);
        setReenterTransition(sharedAxis);
        setReturnTransition(sharedAxis2);

        // Set Custom Back Pressed Callback
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navController.navigate(R.id.action_itemFragment_to_inventoryFragment);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentItemBinding.inflate(inflater, container, false);

        navController = NavHostFragment.findNavController(ItemFragment.this);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupItemDetailsFireStore();
        setupTopAppBar();
    }

    // Method to setup the FireStore listener for the item details
    private void setupItemDetailsFireStore() {
        // Get item document ID from arguments
        Bundle args = getArguments();

        if (args != null && mAuth.getCurrentUser() != null) {
            itemDocumentId = args.getString(EXTRA_ITEM_DOCUMENT_ID, "");

            // Create a reference to the Firestore document
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = mAuth.getCurrentUser().getUid();
            CollectionReference inventoryRef = db.collection("users")
                    .document(userId)
                    .collection("Inventory");
            DocumentReference itemRef = inventoryRef.document(itemDocumentId);

            // Attach a snapshot listener to the document
            itemListener = itemRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w("Error", "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // Document exists, create an InventoryItem object
                        inventoryItem = snapshot.toObject(InventoryItem.class);

                        // Update UI with the data
                        MaterialToolbar topAppBar = binding.topAppBar;
                        topAppBar.setTitle(inventoryItem.getItem_name());
                        binding.itemName.setText(inventoryItem.getItem_name());
                        binding.itemCategory.setText(inventoryItem.getItem_category());
                        binding.itemDescription.setText(inventoryItem.getItem_description());
                        binding.itemLocation.setText(inventoryItem.getItem_location());
                        binding.itemPrice.setText(String.valueOf(inventoryItem.getItem_price()));
                        binding.itemQuantity.setText(String.valueOf(inventoryItem.getItem_quantity()));

                        itemImagePath = inventoryItem.getImage_path();

                        if (itemImagePath != null) {
                            // Load and display the image using Glide
                            Glide.with(ItemFragment.this)
                                    .load(itemImagePath)
                                    .into(new CustomTarget<Drawable>() {
                                        @Override
                                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                            // Set the loaded image to the ImageView in the fragment
                                            binding.itemImageView.setImageDrawable(resource);
                                            binding.itemImageView.setVisibility(View.VISIBLE);
                                            // Save the loaded Drawable to reuse in the fullscreen dialog
                                            loadedDrawable = resource;
                                        }

                                        @Override
                                        public void onLoadCleared(@Nullable Drawable placeholder) {
                                        }
                                    });
                        }
                        binding.itemImageView.setVisibility(View.VISIBLE);

                        // Setup click listener for the ImageView
                        binding.itemImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Show full screen image
                                if (itemImagePath != null) {
                                    showFullScreenImage();
                                }
                            }
                        });
                    } else {
                        Log.d("Error", "Current data: null");
                    }
                }
            });
        }
    }

    // Setup the top app bar menu
    private void setupTopAppBar(){
        binding.topAppBar.setNavigationOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        binding.topAppBar.setOnMenuItemClickListener(new MaterialToolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.edit_item) {
                    // Handle Edit Item
                    navigateToEditItemFragment();
                    return true;
                }
                else if (itemId == R.id.delete_item){
                    // Handle Delete Item
                    // Add Confirmation Dialog
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialAlertDialog);
                    builder.setTitle("Item Deletion Confirmation");
                    // set icon at top of dialog
                    builder.setIcon(R.drawable.baseline_delete_48);
                    builder.setMessage("Do You Want To Delete Item?");
                    builder.setNeutralButton("Cancel", null);
                    // Toast on Click OK button
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            DeleteItem();
                        }
                    });
                    builder.show();
                    return true;
                }
                else return false; // Return false for any other menu item not explicitly handled
            }
        });
    }

    // Method to navigate to Edit Item Fragment
    private void navigateToEditItemFragment() {
        // Navigate to Edit Item Fragment
        Bundle bundle = new Bundle();
        bundle.putString(ItemEditFragment.EXTRA_ITEM_DOCUMENT_ID, itemDocumentId);
        navController.navigate(R.id.action_itemFragment_to_itemEditFragment, bundle);
    }

    // Method to show fullscreen image using a Dialog
    private void showFullScreenImage() {
        Dialog fullscreenDialog = new Dialog(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_Dark);
        fullscreenDialog.setContentView(R.layout.fullscreen_image_layout_item_details);

        ImageView fullscreenImageView = fullscreenDialog.findViewById(R.id.fullscreenImageView);
        fullscreenImageView.setImageDrawable(loadedDrawable);

        MaterialToolbar fullscreentoolbar = fullscreenDialog.findViewById(R.id.topAppBar);
        fullscreentoolbar.setTitle(inventoryItem.getItem_name());
        fullscreentoolbar.setNavigationOnClickListener(v -> {
            fullscreenDialog.dismiss();
        });

        fullscreenDialog.show();
    }

    // Method to delete an item from FireStore
    private void DeleteItem() {
        if (mAuth.getCurrentUser() != null) {
            // User is signed in
            String currentUserId = mAuth.getCurrentUser().getUid();

            // Reference to the specific document in the SubCollection
            documentRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .collection("Inventory")
                    .document(itemDocumentId);  // Use the document ID of the item

            // Get the document data including the image URL
            documentRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get the image URL from the document
                    String imagePath = documentSnapshot.getString("image_path");

                    // Delete the document from Firestore
                    documentRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                // Document successfully deleted

                                // Delete the image from Firebase Storage
                                if (imagePath != null && !imagePath.isEmpty()) {
                                    deleteImageFromStorage(imagePath);
                                }

                                // Handle success, e.g., show a toast or navigate back
                                ToastUtils.showToast(requireContext(), "Item deleted successfully");
                                requireActivity().getOnBackPressedDispatcher().onBackPressed(); // Go back to the previous screen
                            })
                            .addOnFailureListener(e -> {
                                // Handle failure
                                ToastUtils.showToast(requireContext(), "Error deleting item");
                            });
                }
            });
        }
    }

    // Method to delete an image from Firebase Storage
    private void deleteImageFromStorage(String imagePath) {
        // Create a reference to the Firebase Storage image
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imagePath);

        // Delete the image
        storageRef.delete().addOnSuccessListener(aVoid -> {
            // Image deleted successfully
            Log.d("ImageDeletion", "Image deleted successfully");
        }).addOnFailureListener(e -> {
            // Handle the image deletion failure
            Log.e("ImageDeletion", "Failed to delete image: " + e.getMessage());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        itemListener.remove();
        binding = null;
    }
}