package com.example.inventorymanager.gettingstarted;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.content.Intent;
import android.os.Bundle;

import com.example.inventorymanager.utils.FirebaseAuthManager;
import com.example.inventorymanager.inventory.InventoryActivity;
import com.example.inventorymanager.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity for Getting Started
 * Allows user to login or register an account
 */
public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuthManager.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // Start Inventory Activity
            startInventoryActivity();
        }
        MoveToLogin();
    }

    // Move To Login Fragment if User Signed Out From Inventory Activity
    private void MoveToLogin(){
        // Check if the intent has the flag to navigate to LoginFragment
        if (getIntent().getBooleanExtra("navigateToLoginFragment", false)) {
            // Use Navigation Component to navigate to LoginFragment
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_getting_started);
            navController.navigate(R.id.loginFragment);

            // Clear the flag to avoid unnecessary navigation on configuration changes
            getIntent().removeExtra("navigateToLoginFragment");
        }
    }

    private void startInventoryActivity() {
        Intent intent = new Intent(this, InventoryActivity.class);
        startActivity(intent);
        finish();
    }

}