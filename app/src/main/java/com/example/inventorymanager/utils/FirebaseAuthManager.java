package com.example.inventorymanager.utils;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Firebase Auth Manager for getting instance of FirebaseAuth
 */
public class FirebaseAuthManager {
    private static FirebaseAuth mAuth;

    public static FirebaseAuth getInstance() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        return mAuth;
    }
}
