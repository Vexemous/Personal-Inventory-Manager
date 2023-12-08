package com.example.inventorymanager.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Utility class for showing Toasts.
 */
public class ToastUtils {

    private static Toast toast;

    public static void showToast(Context context, String message) {
        if (toast != null) {
            toast.cancel();
        }

        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static Toast getToast() {
        return toast;
    }
}