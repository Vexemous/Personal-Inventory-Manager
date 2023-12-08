package com.example.inventorymanager;

import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.inventorymanager.gettingstarted.MainActivity;
import com.example.inventorymanager.utils.ToastUtils;

@RunWith(AndroidJUnit4.class)
public class ToastUtilsInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testShowToast() {
        mActivityScenarioRule.getScenario().onActivity(activity -> {
            String message = "Test message";
            ToastUtils.showToast(activity, message);

            // Assert that the toast is not null
            assertNotNull(ToastUtils.getToast());
        });
    }

    @Test
    public void testCancelToast() {
        mActivityScenarioRule.getScenario().onActivity(activity -> {
            ToastUtils.showToast(activity, "Test message");
            Toast originalToast = ToastUtils.getToast();

            // Assert that the toast is not null
            assertNotNull(originalToast);

            // Cancel the toast
            originalToast.cancel();

            // Assert that the toast is canceled
            assertEquals(originalToast.getView().getWindowVisibility(), View.GONE);
        });
    }
}

