package com.example.myapplication;

public class Business {

    private String id;
    private String ownerId;
    private String name;
    private String description;
    private String phone;
    private String mainImageUrl;

    // חשוב – בנאי ריק לפיירסטור
    public Business() {}

    public Business(String id, String ownerId, String name, String description, String phone, String mainImageUrl) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.phone = phone;
        this.mainImageUrl = mainImageUrl;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPhone() {
        return phone;
    }

    public String getMainImageUrl() {
        return mainImageUrl;
    }
}
