package com.example.inventorymanager;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.inventorymanager.gettingstarted.MainActivity;
import com.example.inventorymanager.utils.NetworkUtils;

@RunWith(AndroidJUnit4.class)
public class NetworkUtilsInstrumentedTest {
    private NetworkUtils networkUtils;

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        mActivityScenarioRule.getScenario().onActivity(activity -> {
            // Create an instance of NetworkUtils with the context
            networkUtils = new NetworkUtils(activity);
        });
    }

    @Test
    public void testIsNetworkAvailable() {
        // Test network availability
        assertTrue(networkUtils.isNetworkAvailable());
    }

    @Test
    public void testNetworkType() {
        // Test network type
        assertNotNull(networkUtils.getNetworkType());
    }
}