package com.example.inventorymanager.inventory;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;

import com.example.inventorymanager.R;
import com.example.inventorymanager.ToastMatcher;
import com.example.inventorymanager.item.InventoryItem;
import com.example.inventorymanager.item.ItemAddFragment;

import org.checkerframework.checker.units.qual.C;
import org.junit.Before;
import org.junit.Test;

/**
 * Test Class for ItemAddFragment
 */
public class ItemAddInstrumentedTest {
    FragmentScenario<ItemAddFragment> scenario = FragmentScenario.launchInContainer(ItemAddFragment.class,
            null, R.style.Base_Theme_InventoryManager, new FragmentFactory() {
                @NonNull
                @Override
                public Fragment instantiate(@NonNull ClassLoader classLoader,
                        @NonNull String className) {
                    ItemAddFragment fragment = new ItemAddFragment();
                    // Make the NavController available via the findNavController() APIs
                    fragment.getViewLifecycleOwnerLiveData().observeForever(new Observer<LifecycleOwner>() {
                        @Override
                        public void onChanged(LifecycleOwner viewLifecycleOwner){
                            NavController navController = new TestNavHostController(
                                    ApplicationProvider.getApplicationContext());
                            if (viewLifecycleOwner != null) {
                                navController.setGraph(R.navigation.inventory_graph);
                                Navigation.setViewNavController(fragment.requireView(), navController);
                            }
                        }
                    });
                    return fragment;
                }
            });

    @Before
    public void setUp() {
        // Set the initial state
        scenario.moveToState(Lifecycle.State.STARTED);
    }

    @Test
    public void testItemAddFragmentInitialState() {
        // Check if UI is visible
        onView(withId(R.id.topAppBar)).check(matches(isDisplayed()));

        onView(withId(R.id.imageView)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)));

        onView(withId(R.id.item_name)).check(matches(isDisplayed()));

        onView(withId(R.id.item_description)).check(matches(isDisplayed()));

        onView(withId(R.id.item_category)).check(matches(isDisplayed()));

        onView(withId(R.id.item_price)).check(matches(isDisplayed()));

        onView(withId(R.id.item_quantity))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        onView(withId(R.id.add_item)).check(matches(isDisplayed()));
    }

    @Test
    public void testItemAddEditText(){
        // Perform actions using Espresso
        onView(withId(R.id.item_name_edit_text))
                .perform(scrollTo())
                .perform(ViewActions.replaceText("Item Name Test"))
                .perform(ViewActions.closeSoftKeyboard());

        onView(withId(R.id.item_description_edit_text))
                .perform(scrollTo())
                .perform(ViewActions.replaceText("Item Description Test"))
                .perform(ViewActions.closeSoftKeyboard());

        onView(withId(R.id.item_category_edit_text))
                .perform(scrollTo())
                .perform(ViewActions.replaceText("Item Category Test"))
                .perform(ViewActions.closeSoftKeyboard());

        onView(withId(R.id.item_location_edit_text))
                .perform(scrollTo())
                .perform(ViewActions.replaceText("Item Location Test"))
                .perform(ViewActions.closeSoftKeyboard());

        onView(withId(R.id.item_price_edit_text))
                .perform(scrollTo())
                .perform(ViewActions.replaceText("100"))
                .perform(ViewActions.closeSoftKeyboard());

        onView(withId(R.id.item_quantity_edit_text))
                .perform(scrollTo())
                .perform(ViewActions.replaceText("100"))
                .perform(ViewActions.closeSoftKeyboard());

        onView(withId(R.id.add_item))
                .perform(ViewActions.click());
    }

    @Test
    public void testImagePath(){
        // Test if Item Image is set as Camera Image Path
        scenario.onFragment(fragment -> {
            String CameraImagePath = "CameraImage";
            InventoryItem item = new InventoryItem();
            item.setImage_path("PreviousImage");

            fragment.checkImagePath(CameraImagePath, item);
            assertNotNull(fragment.itemImagePath);
            assertEquals(CameraImagePath, fragment.itemImagePath);
        });

        // Test if Item Image is null if Camera Image Path is null
        scenario.onFragment(fragment -> {
            InventoryItem item = new InventoryItem();
            item.setImage_path(null);

            fragment.checkImagePath(null, item);
            assertNull(fragment.itemImagePath);
        });
    }

    @Test
    public void testHandleEmptyFields() {
        // Test when Item name field is empty
        onView(withId(R.id.add_item)).perform(ViewActions.click());

        String toastMessage = "Please enter an item name";
        onView(withText(toastMessage))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }
}
