package com.example.inventorymanager.gettingstarted;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.inventorymanager.utils.FirebaseAuthManager;
import com.example.inventorymanager.utils.ToastUtils;
import com.example.inventorymanager.inventory.InventoryActivity;
import com.example.inventorymanager.R;
import com.example.inventorymanager.databinding.FragmentLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

/**
 * Fragment for Login
 * Allows user to login with email and password
 */
public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;
    private String email;
    private String password;
    private final Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuthManager.getInstance();

        MaterialSharedAxis sharedAxis = new MaterialSharedAxis(MaterialSharedAxis.X, true);
        MaterialSharedAxis sharedAxis2 = new MaterialSharedAxis(MaterialSharedAxis.X, false);

        setEnterTransition(sharedAxis);
        setReenterTransition(sharedAxis);
        setReturnTransition(sharedAxis2);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_loginFragment_to_registerFragment);
            }
        });

        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Call your method to handle login
                handleLogin();
            }
        });
    }

    // Handles the login process
    private void handleLogin() {
        email = Objects.requireNonNull(binding.emailAddress.getEditText()).getText().toString().trim();
        password = Objects.requireNonNull(binding.password.getEditText()).getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            handleDetailErrors();
            return;
        }

        // Authenticate user with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Login success, navigate to the next screen or perform other actions
                            showToast("Login successful");
                            // Start Inventory Activity
                            startInventoryActivity();
                        } else {
                            // If sign in fails, display a message to the user.
                            showToast("Authentication failed");
                        }
                    }
                });
    }

    // Handles the cases where details are empty
    private void handleDetailErrors(){
        // Handle the cases where details are empty
        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            binding.emailAddress.setError("Email is required");
            binding.password.setError("Password is required");
        }
        else if (TextUtils.isEmpty(email)) {
            binding.emailAddress.setError("Email is required");
        }
        else if (TextUtils.isEmpty(password)) {
            binding.password.setError("Password is required");
        }
        showToast("Missing required fields");

        // Use a Handler to clear the errors after a delay (e.g., 2 seconds)
        handler.removeCallbacksAndMessages(null); // Remove any existing callbacks and messages
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    // Clear the error messages after the delay
                    binding.emailAddress.setError(null);
                    binding.password.setError(null);
                }
            }
        }, 2000);
    }

    private void showToast(final String message) {
        ToastUtils.showToast(requireContext(), message);
    }

    // Starts the InventoryActivity
    private void startInventoryActivity() {
        // Create an Intent to start the new activity
        Intent intent = new Intent(requireActivity(), InventoryActivity.class);
        // Start the new activity
        startActivity(intent);
        requireActivity().finish();
    }


    @Override
    public void onStop() {
        super.onStop();
        binding.emailAddress.setError(null);
        binding.password.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}