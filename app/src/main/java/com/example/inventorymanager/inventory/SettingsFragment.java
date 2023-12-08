package com.example.inventorymanager.inventory;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.inventorymanager.databinding.FragmentSettingsBinding;
import com.example.inventorymanager.utils.FirebaseAuthManager;
import com.example.inventorymanager.utils.ToastUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

/**
 * Fragment for Settings
 * Allows user to set theme and change username
 */
public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private FirebaseAuth mAuth;
    private NavController navController;
    private DocumentReference userDocRef;
    private String CurrentUsername;
    private int pendingThemeMode;
    private static final int MODE_LIGHT = 0;
    private static final int MODE_DARK = 1;
    private static final int MODE_SYSTEM = 2;
    private static final String CHANGE_THEME = "change_theme";
    private static final String THEME_MODE_PREF = "theme_mode_pref";

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        navController = NavHostFragment.findNavController(SettingsFragment.this);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
    }

    // Sets up the UI
    private void setupUI () {
        setUsername();

        if(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            binding.radioButton1.setChecked(true);
        }
        else if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            binding.radioButton2.setChecked(true);
        }
        else if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            binding.radioButton3.setChecked(true);
        }

        binding.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (binding.radioButton1.isChecked()) {
                setThemeMode(MODE_LIGHT);
            } else if (binding.radioButton2.isChecked()) {
                setThemeMode(MODE_DARK);
            } else if (binding.radioButton3.isChecked()) {
                setThemeMode(MODE_SYSTEM);
            }
        });

        binding.saveButton.setOnClickListener(v -> {
            saveThemeModePreference();
            saveUserName();
            ToastUtils.showToast(requireContext(), "Settings saved ");
        });

        binding.topAppBar.setNavigationOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    // Sets The Edit Text to current user's username
    private void setUsername() {
        if (mAuth.getCurrentUser() != null) {
            String currentUserId = mAuth.getCurrentUser().getUid();
            // Assuming you have a reference to the current user's document in Firestore
            userDocRef = FirebaseFirestore.getInstance().collection("users")
                    .document(currentUserId);

            // Get the user's document data
            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Retrieve the "name" field from the document
                    String username = documentSnapshot.getString("name");

                    // Update the EditText with the retrieved name
                    Objects.requireNonNull(binding.userName.getEditText()).setText(username);
                    CurrentUsername = username;
                }
            }).addOnFailureListener(e -> {
                // Handle failure, e.g., log an error message or show a toast
                ToastUtils.showToast(getContext(), "Error getting user's username");
            });
        }
    }

    // Sets the theme mode for the application
    private void setThemeMode(int themeMode) {
        // Set the theme mode for the application
        if (themeMode == MODE_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themeMode == MODE_DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (themeMode == MODE_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    // Saves the theme mode preference to SharedPreferences
    private void saveThemeModePreference() {
        // Check Theme Mode
        if (binding.radioButton1.isChecked()) {
            pendingThemeMode = MODE_LIGHT;
        } else if (binding.radioButton2.isChecked()) {
            pendingThemeMode = MODE_DARK;
        } else if (binding.radioButton3.isChecked()) {
            pendingThemeMode = MODE_SYSTEM;
        }
        // Save theme mode preference to SharedPreferences
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(CHANGE_THEME, Context.MODE_PRIVATE).edit();
        editor.putInt(THEME_MODE_PREF, pendingThemeMode);
        editor.apply();
    }

    // Saves the username to FireStore
    private void saveUserName(){
        // Get the username from the EditText
        String newUsername = binding.userName.getEditText().getText().toString();

        // Update to FireStore if username is different
        if (!Objects.equals(CurrentUsername, newUsername)) {
            // Update the user's document in Firestore
            userDocRef.update("name", newUsername)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Username", "Username Saved");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Username", "Username Not Saved" + e);
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}