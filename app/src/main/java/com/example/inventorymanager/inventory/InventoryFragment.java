package com.example.inventorymanager.inventory;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.inventorymanager.utils.FirebaseAuthManager;
import com.example.inventorymanager.R;
import com.example.inventorymanager.utils.NetworkUtils;
import com.example.inventorymanager.utils.ToastUtils;
import com.example.inventorymanager.databinding.FragmentInventoryBinding;
import com.example.inventorymanager.gettingstarted.MainActivity;
import com.example.inventorymanager.item.InventoryItem;
import com.example.inventorymanager.item.ItemAddFragment;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchView;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Fragment for the Inventory Screen
 * Displays the user's inventory and username
 * Allows the user to add and view items in their inventory
 */
public class InventoryFragment extends Fragment {
    private FragmentInventoryBinding binding;
    private FirebaseAuth mAuth;
    private FirestoreRecyclerAdapter<InventoryItem, InventoryAdapter.InventoryViewHolder> adapter;
    private FirestoreRecyclerAdapter<InventoryItem, InventoryAdapter.InventoryViewHolder> Search_adapter;
    private CollectionReference inventoryRef;
    private NavController navController;
    private static final int MODE_LIGHT = 0;
    private static final int MODE_DARK = 1;
    private static final int MODE_SYSTEM = 2;
    private static final String CHANGE_THEME = "change_theme";
    private static final String THEME_MODE_PREF = "theme_mode_pref";
    NetworkUtils networkUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuthManager.getInstance();

        // Setup network utils
        networkUtils = new NetworkUtils(requireContext());

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
                if (binding.searchView.isShowing()) {
                    binding.searchView.hide();
                }
                else if(isEnabled()) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInventoryBinding.inflate(inflater, container, false);

        navController = NavHostFragment.findNavController(InventoryFragment.this);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(networkUtils.isNetworkAvailable()) {
            // Setup Inventory Recycler View
            Log.d("Network", "Network is available");
            setupInventoryRecyclerView();
            setTopAppBarOnClick();
            setAddItemOnClick();
        }
        else {
            // Network is not available
            Log.d("Network", "Network is not available");
            ToastUtils.showToast(getContext(), "Network is not available");
        }

        SharedPreferences settings = requireActivity().getSharedPreferences(CHANGE_THEME, Context.MODE_PRIVATE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            int savedThemeMode = settings.getInt(THEME_MODE_PREF, MODE_SYSTEM);

            // Set the theme mode based on the retrieved value
            switch (savedThemeMode) {
                case MODE_LIGHT:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case MODE_DARK:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case MODE_SYSTEM:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
            }
        }, 1000);
    }

    // Setups The Recycler View For Showing Inventory Items
    private void setupInventoryRecyclerView() {
        if (mAuth.getCurrentUser() != null) {
            // User is signed in
            String currentUserId = mAuth.getCurrentUser().getUid();
            setAppBarTitle(currentUserId);

            // Reference to the "Inventory" SubCollection for the current user
            inventoryRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .collection("Inventory");

            Query query = inventoryRef.orderBy("item_name", Query.Direction.ASCENDING);

            FirestoreRecyclerOptions<InventoryItem> options = new FirestoreRecyclerOptions.Builder<InventoryItem>()
                    .setQuery(query, InventoryItem.class)
                    .build();

            adapter = new InventoryAdapter(options, navController);

            RecyclerView recyclerView = binding.inventoryList;
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            recyclerView.setAdapter(adapter);
        }
    }

    // Sets The Title Of The Top App Bar To The User's Username
    private void setAppBarTitle(String userId) {
        // Assuming you have a reference to the current user's document in Firestore
        DocumentReference userDocRef = FirebaseFirestore.getInstance().collection("users").document(userId);

        // Get the user's document data
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Retrieve the "name" field from the document
                String username = documentSnapshot.getString("name");

                // Update the Toolbar Title
                MaterialToolbar topAppBar = binding.topAppBar;
                topAppBar.setTitle(username + "'s Inventory");

                // You can use the username to set any other UI components or perform additional actions.
            }
        }).addOnFailureListener(e -> {
            // Handle failure, e.g., log an error message or show a toast
            ToastUtils.showToast(getContext(), "Error getting user's username");
        });
    }

    // Sets The On Click Listener For The Top App Bar
    private void setTopAppBarOnClick(){
        binding.topAppBar.setOnMenuItemClickListener(new MaterialToolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.settings) {
                    // Handle Settings
                    navController.navigate(R.id.action_inventoryFragment_to_settingsFragment);
                    return true;
                }
                else if (itemId == R.id.logout) {
                    // Handle Logout
                    logoutOnClick();
                    return true;
                }
                else if (itemId == R.id.search){
                    // Handle Search
                    binding.searchView.show();
                    setupSearchView();
                    return true;
                }
                else return false; // Return false for any other menu item not explicitly handled
            }
        });
    }

    // Sets Up The Search View For Searching Items
    private void setupSearchView() {
        RecyclerView SearchrecyclerView = binding.searchList;
        SearchrecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String searchText = binding.searchView.getEditText().getText().toString();

                if (!searchText.isEmpty()) {
                    Query searchQuery = inventoryRef
                            .orderBy("item_name")
                            .startAt(searchText)
                            .endAt(searchText + "\uf8ff");

                    FirestoreRecyclerOptions<InventoryItem> searchOptions = new FirestoreRecyclerOptions.Builder<InventoryItem>()
                            .setQuery(searchQuery, InventoryItem.class)
                            .build();
                    Search_adapter = new InventoryAdapter(searchOptions, navController);
                    Search_adapter.startListening();
                    SearchrecyclerView.setAdapter(Search_adapter);
                }
                else {
                    SearchrecyclerView.setAdapter(null);
                }
            }
        });

        binding.searchView.addTransitionListener(new SearchView.TransitionListener() {
            @Override
            public void onStateChanged(@NonNull SearchView searchView, @NonNull SearchView.TransitionState previousState, @NonNull SearchView.TransitionState newState) {
                if(newState == SearchView.TransitionState.HIDING){
                    if (Search_adapter != null) {
                        Search_adapter.stopListening();
                        searchView.clearText();
                    }
                }
            }
        });
    }

    // Sets The On Click Listener For The Add Item Button
    private void setAddItemOnClick(){
        binding.addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialAlertDialog);
                builder.setTitle("Add Item Image");
                // set icon at top of dialog
                builder.setIcon(R.drawable.baseline_photo_camera_48);
                builder.setMessage("Would You Like To Add Item Image?");
                builder.setNeutralButton("Cancel", null);
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Bundle bundle = new Bundle();
                        bundle.putString(ItemAddFragment.EXTRA_ITEM_OBJECT_NAME, null);
                        bundle.putString(ItemAddFragment.EXTRA_ITEM_IMAGE_PATH, null);
                        navController.navigate(R.id.action_inventoryFragment_to_itemAddFragment, bundle);
                    }
                });
                // Toast on Click OK button
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Handle the click event for the OK button
                        // For example, show a Toast
                        ToastUtils.showToast(getContext(), "Starting Camera...");
                        navController.navigate(R.id.action_inventoryFragment_to_itemCameraFragment);
                    }
                });
                builder.show();
            }
        });
    }

    // Handles Logout
    private void logoutOnClick() {
        // Sign out the user
        mAuth.signOut();

        // Move from this Fragment to the Login Fragment in MainActivity
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("navigateToLoginFragment", true);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null && networkUtils.isNetworkAvailable()) {
            adapter.notifyDataSetChanged();
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}