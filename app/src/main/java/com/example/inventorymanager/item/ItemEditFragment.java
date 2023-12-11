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
import com.example.inventorymanager.databinding.FragmentItemEditBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Objects;

/**
 * Fragment for editing an item in the inventory
 */
public class ItemEditFragment extends Fragment {

    public FragmentItemEditBinding binding;
    private FirebaseAuth mAuth;
    private SharedViewModel sharedViewModel;
    public static final String EXTRA_ITEM_DOCUMENT_ID = "extra_item_document_id";
    public static final String EXTRA_ITEM_OBJECT_NAME = "extra_item_object_name";
    public static final String EXTRA_ITEM_IMAGE_PATH = "extra_item_image_path";
    private NavController navController;
    public String itemImagePath;
    private String itemDocumentId;
    private Drawable loadedDrawable;
    public InventoryItem inventoryItem;
    DocumentReference itemRef;
    Bundle args;

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
        setExitTransition(sharedAxis2);

        // Set Custom Back Pressed Callback
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Clear the sharedViewModel
                sharedViewModel.clearItemDetails();
                // Get item document ID from ViewModel
                Bundle bundle = new Bundle();
                bundle.putString(ItemFragment.EXTRA_ITEM_DOCUMENT_ID, sharedViewModel.getItemDocumentID().getValue());
                navController.navigate(R.id.action_itemEditFragment_to_itemFragment, bundle);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentItemEditBinding.inflate(inflater, container, false);
        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(ItemEditFragment.this);
        setupItemDetails();
        setupTopAppBar();
        setupUI();
    }

    // Sets item details if already in the shared view model, else get data from FireStore
    public void setupItemDetails(){
        // Get item name and image path from arguments
        args = getArguments();
        // Setup shared view model
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        InventoryItem item = sharedViewModel.getItemDetails().getValue();

        // If SharedViewModel has the item details
        if (item != null) {
            // Update UI with the data
            binding.itemCategory.getEditText().setText(item.getItem_category());
            binding.itemDescription.getEditText().setText(item.getItem_description());
            binding.itemLocation.getEditText().setText(item.getItem_location());
            binding.itemPrice.getEditText().setText(String.valueOf(item.getItem_price()));
            binding.itemQuantity.getEditText().setText(String.valueOf(item.getItem_quantity()));

            // Check and set the item name
            if(args.getString(EXTRA_ITEM_OBJECT_NAME) != null)
                binding.itemName.getEditText().setText(args.getString(EXTRA_ITEM_OBJECT_NAME, ""));
            else {
                binding.itemName.getEditText().setText(item.getItem_name());
            }

            // Check and set the image path
            checkImagePath(args.getString(EXTRA_ITEM_IMAGE_PATH), item);

            if (itemImagePath != null) {
                // Load and display the image using Glide
                Glide.with(ItemEditFragment.this)
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
            // Get item details from FireStore
            setupItemDetailsFireStore();
        }
    }

    // Method to check if the image path is the same as the camera image path
    public void checkImagePath(String CameraImagePath, InventoryItem item){
        // If camera Image path is not null and has an image
        if (CameraImagePath != null && !Objects.equals(CameraImagePath, "No Image")) {
            // Check if camera image path is not the same as the item image path
            if(!Objects.equals(CameraImagePath, item.getImage_path())) {
                // Check if the item image path is not the same as the firebase image path
                if (!Objects.equals(item.getImage_path(), sharedViewModel.getFirebaseImage().getValue())) {
                    // Delete temporary file from shared view model
                    deleteTemporaryFile(item.getImage_path());
                }
            }
            // Replace current image path with camera image path
            itemImagePath = CameraImagePath;
            Log.d("Image Path", "Fetching From Camera");
        }
        else {
            // If camera image path has no image
            if(Objects.equals(CameraImagePath, "No Image")){
                // Check if item image path is not the same as the firebase image path
                if(!item.getImage_path().equals(sharedViewModel.getFirebaseImage().getValue())){
                    // Delete temporary file from shared view model
                    deleteTemporaryFile(item.getImage_path());
                }
                // set item image path as null
                itemImagePath = null;
            }
            else {
                itemImagePath = item.getImage_path();
            }
            Log.d("Image Path", "Fetching From Shared View Model");
        }

        Log.d("Image", "Image Path: " + itemImagePath);
        Log.d("Image", "Firebase Image Path: " + sharedViewModel.getFirebaseImage().getValue());
    }

    // Method to get initial item details from FireStore
    private void setupItemDetailsFireStore() {

        if (args != null && mAuth.getCurrentUser() != null) {
            itemDocumentId = args.getString(EXTRA_ITEM_DOCUMENT_ID, "");
            // Save the document ID
            sharedViewModel.setItemDocumentID(itemDocumentId);

            // Create a reference to the FireStore document
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = mAuth.getCurrentUser().getUid();
            CollectionReference inventoryRef = db.collection("users")
                    .document(userId)
                    .collection("Inventory");
            itemRef = inventoryRef.document(itemDocumentId);

            // Fetch the data from FireStore
            itemRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Document exists, create an InventoryItem object
                            inventoryItem = document.toObject(InventoryItem.class);

                            // Update UI with the data
                            binding.itemName.getEditText().setText(inventoryItem.getItem_name());
                            binding.itemCategory.getEditText().setText(inventoryItem.getItem_category());
                            binding.itemDescription.getEditText().setText(inventoryItem.getItem_description());
                            binding.itemLocation.getEditText().setText(inventoryItem.getItem_location());
                            binding.itemPrice.getEditText().setText(String.valueOf(inventoryItem.getItem_price()));
                            binding.itemQuantity.getEditText().setText(String.valueOf(inventoryItem.getItem_quantity()));
                            itemImagePath = inventoryItem.getImage_path();

                            if (itemImagePath != null) {
                                // Save Image Path
                                sharedViewModel.setFirebaseImage(itemImagePath);
                                // Load and display the image using Glide
                                Glide.with(ItemEditFragment.this)
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
                        } else {
                            Log.d("Error", "No such document");
                        }
                    } else {
                        Log.w("Error", "Get failed with ", task.getException());
                    }
                }
            });
        }
    }

    // Update ViewModel with current Text on EditText fields
    private void updateViewModel() {
        // Update the fields
        InventoryItem currentDetails = new InventoryItem();
        currentDetails.setItem_name(binding.itemName.getEditText().getText().toString());
        currentDetails.setItem_category(binding.itemCategory.getEditText().getText().toString());
        currentDetails.setItem_description(binding.itemDescription.getEditText().getText().toString());
        currentDetails.setItem_location(binding.itemLocation.getEditText().getText().toString());
        currentDetails.setItem_price(Double.parseDouble(binding.itemPrice.getEditText().getText().toString()));
        currentDetails.setItem_quantity(Integer.parseInt(binding.itemQuantity.getEditText().getText().toString()));
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

        // Setup click listener for the update button
        binding.updateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if item name is inputted
                String itemName = binding.itemName.getEditText().getText().toString().trim();
                if (itemName.isEmpty()) {
                    // Display a toast if item name is not provided
                    ToastUtils.showToast(requireContext(), "Please enter an item name");
                } else {
                    // Call the method to update the item in FireStore
                    FinalizeItemUpdateDetails();
                }
            }
        });
    }

    // Sets the updated item details to update in FireStore
    private void FinalizeItemUpdateDetails() {
        FirebaseUser user = mAuth.getCurrentUser();

        String name = setValue(binding.itemName);
        String description = setValue(binding.itemDescription);
        String category = setValue(binding.itemCategory);
        String location = setValue(binding.itemLocation);
        double price = setDoubleValue(binding.itemPrice);
        int quantity = setIntValue(binding.itemQuantity);

        if (user != null) {
            // Get User ID
            String userId = user.getUid();
            Log.d("User", "User ID: " + userId);

            // Create a new InventoryItem object with updated details
            InventoryItem updatedItem = new InventoryItem(name, description, category, location, price, quantity);

            String firebaseImage = sharedViewModel.getFirebaseImage().getValue();

            // Check if there's a new image path
            if (itemImagePath != null && !itemImagePath.equals(firebaseImage)) {
                // Check if there is an existing firebase image
                if (firebaseImage != null) {
                    // Delete the existing image from Firebase Storage
                    deleteImageFromStorage(firebaseImage);
                }
                // Upload the new image to Firebase Storage
                uploadImageToStorage(itemImagePath, new ImageUploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        // Image upload successful, set the new image URL to the InventoryItem
                        updatedItem.setImage_path(imageUrl);
                        // Update the existing item in the Inventory collection
                        updateItemInFireStore(userId, updatedItem);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Handle image upload failure
                        ToastUtils.showToast(requireContext(), "Failed to update image");
                    }
                });
            } else {
                // If current image is null and firebase image is not null, delete the previous image
                if (itemImagePath == null && firebaseImage != null) {
                    // Delete the previous image from Firebase Storage
                    deleteImageFromStorage(firebaseImage);
                }
                // If there's no new image, set the current image path
                updatedItem.setImage_path(itemImagePath);
                // Update the existing item in the Inventory collection
                updateItemInFireStore(userId, updatedItem);
            }
        }
    }

    // Method to upload image to Firebase Storage
    private void uploadImageToStorage(String imagePath, ItemEditFragment.ImageUploadCallback callback) {
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

    // Updates the item in the "Inventory" collection in FireStore
    private void updateItemInFireStore(String userId, InventoryItem updatedItem) {
        // Create a reference to the user's "Inventory" collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference inventoryRef = db.collection("users").document(userId).collection("Inventory");
        itemDocumentId = sharedViewModel.getItemDocumentID().getValue();

        inventoryRef.document(itemDocumentId)
                .set(updatedItem, SetOptions.merge()) // Use SetOptions.merge() to update only the provided fields
                .addOnSuccessListener(aVoid -> {
                    // Item updated successfully
                    ToastUtils.showToast(requireContext(), "Item updated successfully");
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    // Handle the Firestore update error
                    ToastUtils.showToast(requireContext(), "Failed to update item");
                    // Show the error
                    Log.d("Error", e.toString());
                });
    }

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
                    navController.navigate(R.id.action_itemEditFragment_to_itemEditCameraFragment);
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
                    navController.navigate(R.id.action_itemEditFragment_to_itemEditCameraFragment);
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