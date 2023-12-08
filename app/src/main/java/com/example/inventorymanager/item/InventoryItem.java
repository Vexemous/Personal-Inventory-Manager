package com.example.inventorymanager.item;

/**
 * InventoryItem class to hold the data for an item in the inventory.
 */
public class InventoryItem {
    private String item_name;
    private String item_category;
    private String item_description;
    private String item_location;
    private double item_price;
    private int item_quantity;
    private String image_path;

    // Empty Constructor
    public InventoryItem() {

    }

    public InventoryItem(String item_name, String item_description, String item_category, String item_location, double item_price, int item_quantity) {
        this.item_name = item_name;
        this.item_description = item_description;
        this.item_category = item_category;
        this.item_location = item_location;
        this.item_price = item_price;
        this.item_quantity = item_quantity;
    }

    public InventoryItem(String item_name, String item_description, String item_category, String item_location, double item_price, int item_quantity, String image_path) {
        this.item_name = item_name;
        this.item_description = item_description;
        this.item_category = item_category;
        this.item_location = item_location;
        this.item_price = item_price;
        this.item_quantity = item_quantity;
        this.image_path = image_path;
    }


    public String getItem_name() {
        return item_name;
    }

    public void setItem_name(String item_name) {
        this.item_name = item_name;
    }

    public String getItem_category() {
        return item_category;
    }

    public void setItem_category(String item_category) {
        this.item_category = item_category;
    }

    public String getItem_description() {
        return item_description;
    }

    public void setItem_description(String item_description) {
        this.item_description = item_description;
    }

    public String getItem_location() {
        return item_location;
    }

    public void setItem_location(String item_location) {
        this.item_location = item_location;
    }

    public double getItem_price() {
        return item_price;
    }

    public void setItem_price(double item_price) {
        this.item_price = item_price;
    }

    public int getItem_quantity() {
        return item_quantity;
    }

    public void setItem_quantity(int item_quantity) {
        this.item_quantity = item_quantity;
    }

    public String getImage_path() {
        return image_path;
    }

    public void setImage_path(String image_path) {
        this.image_path = image_path;
    }
}
