package com.example.inventorymanager.gettingstarted;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.inventorymanager.utils.FirebaseAuthManager;
import com.example.inventorymanager.R;
import com.example.inventorymanager.utils.ToastUtils;
import com.example.inventorymanager.databinding.FragmentRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Fragment for Register
 * Allows user to register with username, email, and password
 */
public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;
    private FirebaseAuth mAuth;
    private String username;
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
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleRegister();
            }
        });
    }

    // Handles the registration process
    private void handleRegister() {
        email = binding.emailAddress.getEditText().getText().toString().trim();
        password = binding.password.getEditText().getText().toString().trim();
        username = binding.username.getEditText().getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username)) {
            handleDetailErrors();
            return;
        }

        // Perform user registration with Firebase Authentication
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // User registration successful
                            linkWithFirestore(); // Link with Firestore
                            NavHostFragment.findNavController(RegisterFragment.this)
                                    .navigate(R.id.action_registerFragment_to_registerSuccessFragment);
                        } else {
                            // If registration fails, handle the error
                            showToast("Registration failed");
                        }
                    }
                });
    }


    // TODO: Create Test Cases for Registration Validation
    private void handleDetailErrors(){
        // Handle the case account details are empty
        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password) && TextUtils.isEmpty(username)) {
            binding.emailAddress.setError("Email is required");
            binding.password.setError("Password is required");
            binding.username.setError("Username is required");
        }
        else if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            binding.emailAddress.setError("Email is required");
            binding.password.setError("Password is required");
        }
        else if (TextUtils.isEmpty(email) && TextUtils.isEmpty(username)) {
            binding.emailAddress.setError("Email is required");
            binding.username.setError("Username is required");
        }
        else if (TextUtils.isEmpty(password) && TextUtils.isEmpty(username)) {
            binding.password.setError("Password is required");
            binding.username.setError("Username is required");
        }
        else if (TextUtils.isEmpty(email)) {
            binding.emailAddress.setError("Email is required");
        }
        else if (TextUtils.isEmpty(password)) {
            binding.password.setError("Password is required");
        }
        else if (TextUtils.isEmpty(username)) {
            binding.username.setError("Username is required");
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
                    binding.username.setError(null);
                }
            }
        }, 2000);
    }

    // Link Firebase Authentication with FireStore
    private void linkWithFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // Get user details
            String uid = user.getUid();
            String email = user.getEmail();

            // Create a user document in Firestore
            DocumentReference userRef = db.collection("users").document(uid);

            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("name", username);
            userDetails.put("email", email);

            userRef.set(userDetails)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // User details successfully stored in Firestore
                            showToast("User details stored successfully.");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle failure
                            showToast("Failed to store user details.");
                        }
                    });
        }
    }

    private void showToast(final String message) {
        ToastUtils.showToast(requireContext(), message);
    }

    @Override
    public void onStop() {
        super.onStop();
        binding.emailAddress.setError(null);
        binding.password.setError(null);
        binding.username.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}