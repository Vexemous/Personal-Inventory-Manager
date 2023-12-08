package com.example.inventorymanager.gettingstarted;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.inventorymanager.R;
import com.example.inventorymanager.ToastMatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test for testing functionalities of Register Fragment
 */
@RunWith(AndroidJUnit4.class)
public class RegisterInstrumentedTest {
    FragmentScenario<RegisterFragment> scenario = FragmentScenario.launchInContainer(RegisterFragment.class,
            null, R.style.Base_Theme_InventoryManager);
    TestNavHostController navController = new TestNavHostController(
            ApplicationProvider.getApplicationContext());

    @Before
    public void setUp() {
        // Navigate To Register Fragment
        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.gettingstarted_graph);
            // Make the NavController available via the findNavController() APIs
            Navigation.setViewNavController(fragment.requireView(), navController);
            navController.navigate(R.id.registerFragment);
        });
        // Set the initial state
        scenario.moveToState(Lifecycle.State.STARTED);
    }

    // Test Login Fragment Initial State
    @Test
    public void testLoginFragmentInitialState() {
        // Check if UI is visible
        onView(withId(R.id.username_edit_text)).check(matches(isDisplayed()));

        onView(withId(R.id.email_address_edit_text)).check(matches(isDisplayed()));

        onView(withId(R.id.password_edit_text)).check(matches(isDisplayed()));

        onView(withId(R.id.submit)).check(matches(isDisplayed()));
    }

    // Test Login EditText
    @Test
    public void testRegisterEditText() {
        // Perform actions using Espresso
        onView(withId(R.id.username_edit_text))
                .perform(ViewActions.typeText("your_username"))
                .perform(ViewActions.closeSoftKeyboard());

        onView(withId(R.id.email_address_edit_text))
                .perform(ViewActions.typeText("your_email_address"))
                .perform(ViewActions.closeSoftKeyboard());

        onView(withId(R.id.password_edit_text))
                .perform(ViewActions.typeText("your_password"))
                .perform(ViewActions.closeSoftKeyboard());

        onView(withId(R.id.username_edit_text))
                .check(matches(withText("your_username")));

        onView(withId(R.id.email_address_edit_text))
                .check(matches(withText("your_email_address")));

        onView(withId(R.id.password_edit_text))
                .check(matches(withText("your_password")));

        onView(withId(R.id.submit))
                .perform(ViewActions.click());

        // Check for Toast
        onView(withText("Registration failed"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    // Test Empty Field Errors
    @Test
    public void testHandleEmptyFields() {
        // Perform actions to leave email and password fields empty
        onView(withId(R.id.submit)).perform(ViewActions.click());

        // Verify that error messages are displayed
        onView(withId(R.id.username))
                .check(matches(hasDescendant(withText("Username is required"))));
        onView(withId(R.id.email_address))
                .check(matches(hasDescendant(withText("Email is required"))));
        onView(withId(R.id.password))
                .check(matches(hasDescendant(withText("Password is required"))));

        // Check for Toast
        onView(withText("Missing required fields"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }
}
