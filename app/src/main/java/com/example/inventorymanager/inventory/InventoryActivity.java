package com.example.inventorymanager.inventory;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.example.inventorymanager.utils.FirebaseAuthManager;
import com.example.inventorymanager.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Activity for Inventory Interface
 */
public class InventoryActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Initialize Firebase Auth
        mAuth = FirebaseAuthManager.getInstance();
    }
}