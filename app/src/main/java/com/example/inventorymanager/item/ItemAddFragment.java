package com.example.inventorymanager.item;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
import com.example.inventorymanager.utils.SharedViewModel;
import com.example.inventorymanager.utils.ToastUtils;
import com.example.inventorymanager.databinding.FragmentItemAddBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

/**
 * Fragment for adding an item to the inventory
 */
public class ItemAddFragment extends Fragment {
    private FragmentItemAddBinding binding;
    private FirebaseAuth mAuth;
    private SharedViewModel sharedViewModel;
    public static final String EXTRA_ITEM_OBJECT_NAME = "extra_item_object_name";
    public static final String EXTRA_ITEM_IMAGE_PATH = "extra_item_image_path";
    public String itemImagePath;
    private NavController navController;
    private Drawable loadedDrawable;
    Bundle args;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuthManager.getInstance();

        // Set Custom Back Pressed Callback
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Clear the sharedViewModel
                sharedViewModel.clearItemDetails();
                // Delete the temporary file
                deleteTemporaryFile(itemImagePath);
                // Navigate Back to Inventory Fragment
                navController.navigate(R.id.action_itemAddFragment_to_inventoryFragment);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentItemAddBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);
        setupItemDetails();
        setupTopAppBar();
        setupUI();
    }

    // Method to setup the item details
    public void setupItemDetails(){
        // Setup shared view model to retain data
        args = getArguments();
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        InventoryItem item = sharedViewModel.getItemDetails().getValue();
        if (item != null) {
            // Update UI with the data
            binding.itemDescription.getEditText().setText(item.getItem_description());
            binding.itemCategory.getEditText().setText(item.getItem_location());
            binding.itemLocation.getEditText().setText(item.getItem_location());
            binding.itemPrice.getEditText().setText(String.valueOf(item.getItem_price()));
            binding.itemQuantity.getEditText().setText(String.valueOf(item.getItem_quantity()));

            // Check and set the item name
            if (args.getString(EXTRA_ITEM_OBJECT_NAME) != null)
                binding.itemName.getEditText().setText(args.getString(EXTRA_ITEM_OBJECT_NAME, ""));
            else {
                binding.itemName.getEditText().setText(item.getItem_name());
            }

            // Check and set the image path
            checkImagePath(args.getString(EXTRA_ITEM_IMAGE_PATH), item);

            if (itemImagePath != null) {
                // Load and display the image using Glide
                Glide.with(ItemAddFragment.this)
                        .load(itemImagePath)
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                // Set the loaded image to the ImageView in the fragment
                                binding.imageView.setImageDrawable(resource);
                                // Save the loaded Drawable to reuse in the fullscreen dialog
                                loadedDrawable = resource;
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
            }
            binding.imageView.setVisibility(View.VISIBLE);
        }
        else {
            // set the initial item details
            setupInitialItemDetails();
        }
    }

    // Method to check if the image path is from camera or from shared view model
    public void checkImagePath(String CameraImagePath, InventoryItem item){
        // If camera Image path is not null and has Image
        if (CameraImagePath != null) {
            // Replace with Camera Image path
            itemImagePath = CameraImagePath;

            // Delete the temporary file from shared view model if it doesn't match the camera image path
            if(!CameraImagePath.equals(item.getImage_path())){
                deleteTemporaryFile(item.getImage_path());
            }

        }
        // If camera Image path is null or has No Image
        else {
            // Delete the temporary file from shared view model
            if(item.getImage_path() != null){
                deleteTemporaryFile(item.getImage_path());
            }

            itemImagePath = null;
        }
    }

    // Method to set the initial item details
    private void setupInitialItemDetails(){
        if (args != null) {
            // Set the initial item details if it exists
            if (args.getString(EXTRA_ITEM_OBJECT_NAME) != null) {
                binding.itemName.getEditText().setText(args.getString(EXTRA_ITEM_OBJECT_NAME));
            }

            if (args.getString(EXTRA_ITEM_IMAGE_PATH) != null) {
                // If camera Image path is not null
                itemImagePath = args.getString(EXTRA_ITEM_IMAGE_PATH);
            }

            if (itemImagePath != null) {
                // Load and display the image using Glide
                Glide.with(ItemAddFragment.this)
                        .load(itemImagePath)
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                // Set the loaded image to the ImageView in the fragment
                                binding.imageView.setImageDrawable(resource);
                                // Save the loaded Drawable to reuse in the fullscreen dialog
                                loadedDrawable = resource;
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
            }
            binding.imageView.setVisibility(View.VISIBLE);
        }
    }

    // Update ViewModel with current Text on EditText fields
    private void updateViewModel() {
        // Update the fields
        InventoryItem currentDetails = new InventoryItem();
        currentDetails.setItem_name(setValue(binding.itemName));
        currentDetails.setItem_category(setValue(binding.itemCategory));
        currentDetails.setItem_description(setValue(binding.itemDescription));
        currentDetails.setItem_location(setValue(binding.itemLocation));
        currentDetails.setItem_price(setDoubleValue(binding.itemPrice));
        currentDetails.setItem_quantity(setIntValue(binding.itemQuantity));
        currentDetails.setImage_path(itemImagePath);

        // Update the ViewModel with the modified ItemDetails
        sharedViewModel.setItemDetails(currentDetails);
    }

    // Setups the UI Functionalities
    private void setupUI(){
        // Setup click listener for the ImageView
        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show full screen image
                if (itemImagePath != null) {
                    showFullScreenImage();
                }
            }
        });

        // Setup click listener for the Add Item button
        binding.addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if item name is inputted
                String itemName = binding.itemName.getEditText().getText().toString().trim();
                if (itemName.isEmpty()) {
                    // Display a toast if item name is not provided
                    ToastUtils.showToast(requireContext(), "Please enter an item name");
                } else {
                    // Call the method to update the item in FireStore
                    FinalizeItemDetails();
                }
            }
        });
    }

    // Method to finalize Item Details to be added to FireStore
    private void FinalizeItemDetails(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        String name = setValue(binding.itemName);
        String description = setValue(binding.itemDescription);
        String category = setValue(binding.itemCategory);
        String location = setValue(binding.itemLocation);
        double price = setDoubleValue(binding.itemPrice);
        int quantity = setIntValue(binding.itemQuantity);

        if (user != null) {
            // User is signed in
            String userId = user.getUid();
            Log.d("User", "User ID: " + userId);

            // Create a reference to the user's "Inventory" collection
            CollectionReference inventoryCollection = db.collection("users").document(userId).collection("Inventory");

            // Create a new InventoryItem object
            InventoryItem inventoryItem = new InventoryItem(name, description, category, location, price, quantity);

            // Check if there's an image path
            if (itemImagePath != null) {
                // Upload the image to Firebase Storage
                uploadImageToStorage(itemImagePath, new ImageUploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        // Image upload successful, set the image URL to the InventoryItem
                        inventoryItem.setImage_path(imageUrl);

                        // Add the item to the "Inventory" collection
                        AddItemToFireStore(inventoryCollection, inventoryItem);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Handle image upload failure
                        ToastUtils.showToast(requireContext(), "Failed to add image");
                    }
                });
            } else {
                // If there's no image, add the item to the "Inventory" collection directly
                AddItemToFireStore(inventoryCollection, inventoryItem);
            }
        }
    }

    // Method to upload Item to FireStore
    private void AddItemToFireStore(CollectionReference inventoryCollection, InventoryItem Item) {
        inventoryCollection.add(Item)
                .addOnSuccessListener(documentReference -> {
                    // Item added successfully
                    ToastUtils.showToast(requireContext(), "Item added successfully");
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    // Handle the FireStore write error
                    ToastUtils.showToast(requireContext(), "Failed to add item");
                });
    }

    // Method to upload image to Firebase Storage
    private void uploadImageToStorage(String imagePath, ItemAddFragment.ImageUploadCallback callback) {
        // Create a storage reference
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        // Generate a unique name for the image using the current timestamp
        String imageName = "image_" + System.currentTimeMillis() + ".jpg";

        // Create a reference to the location where you want to store the file in Firebase Storage
        StorageReference imageRef = storageRef.child("images").child(imageName);

        // Upload the file
        UploadTask uploadTask = imageRef.putFile(Uri.fromFile(new File(imagePath)));

        // Register observers to listen for when the upload is successful or fails
        // Handle the image upload error
        uploadTask.addOnSuccessListener(taskSnapshot -> {
                    // Image upload success, get the download URL
                    // Handle the image download URL retrieval error
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                // Uri contains the download URL
                                callback.onSuccess(uri.toString());
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Interface to handle image upload callback
    interface ImageUploadCallback {
        void onSuccess(String imageUrl);

        void onFailure(Exception e);
    }

    // Sets default value for empty fields
    private String setValue(TextInputLayout EditText){
        if(EditText.getEditText().getText().toString().isEmpty()){
            return "";
        }
        else {
            return EditText.getEditText().getText().toString();
        }
    }

    private Double setDoubleValue(TextInputLayout EditText){
        if(EditText.getEditText().getText().toString().isEmpty()){
            return 0.0;
        }
        else {
            return Double.parseDouble(EditText.getEditText().getText().toString());
        }
    }

    // Sets default value for empty fields
    private Integer setIntValue(TextInputLayout EditText){
        if(EditText.getEditText().getText().toString().isEmpty()){
            return 0;
        }
        else {
            return Integer.parseInt(EditText.getEditText().getText().toString());
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

                if (itemId == R.id.edit_item_image) {
                    // Handle Edit Item Image
                    updateViewModel();
                    navController.navigate(R.id.action_itemAddFragment_to_itemCameraFragment);
                    return true;
                }
                else return false; // Return false for any other menu item not explicitly handled
            }
        });
    }

    // Method to show fullscreen image using a Dialog
    private void showFullScreenImage() {
        Dialog fullscreenDialog = new Dialog(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_Dark);
        fullscreenDialog.setContentView(R.layout.fullscreen_image_layout);

        ImageView fullscreenImageView = fullscreenDialog.findViewById(R.id.fullscreenImageView);
        fullscreenImageView.setImageDrawable(loadedDrawable);

        MaterialToolbar fullscreentoolbar = fullscreenDialog.findViewById(R.id.topAppBar);
        fullscreentoolbar.setNavigationOnClickListener(v -> {
            fullscreenDialog.dismiss();
        });

        fullscreentoolbar.setOnMenuItemClickListener(new MaterialToolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.edit_item_image) {
                    // Handle Edit Item Image
                    fullscreenDialog.dismiss();
                    updateViewModel();
                    navController.navigate(R.id.action_itemAddFragment_to_itemCameraFragment);
                    return true;
                }
                else return false;
            }
        });

        fullscreenDialog.show();
    }

    // Release the image from memory if navigating back to inventory list
    private void deleteTemporaryFile(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            File file = new File(filePath);
            boolean deleted = file.delete();
            if (deleted) {
                Log.d("Image Path", "Temporary file deleted successfully");
            } else {
                Log.e("Image Path", "Failed to delete temporary file");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}