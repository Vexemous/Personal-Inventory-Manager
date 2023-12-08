package com.example.inventorymanager.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.inventorymanager.item.InventoryItem;

/**
 * Shared View Model for passing data between fragments
 */
public class SharedViewModel extends ViewModel {
    private MutableLiveData<InventoryItem> inventoryItem = new MutableLiveData<>();
    private MutableLiveData<String> itemDocumentID = new MutableLiveData<>();
    private MutableLiveData<String> FirebaseImage = new MutableLiveData<>();

    public LiveData<InventoryItem> getItemDetails() {
        return inventoryItem;
    }

    public LiveData<String> getItemDocumentID() {
        return itemDocumentID;
    }

    public LiveData<String> getFirebaseImage() {
        return FirebaseImage;
    }

    public void setItemDetails(InventoryItem item) {
        inventoryItem.setValue(item);
    }

    public void setItemDocumentID(String documentID) {
        itemDocumentID.setValue(documentID);
    }

    public void setFirebaseImage(String image) {
        FirebaseImage.setValue(image);
    }

    public void clearItemDetails() {
        inventoryItem.setValue(null);
    }
}