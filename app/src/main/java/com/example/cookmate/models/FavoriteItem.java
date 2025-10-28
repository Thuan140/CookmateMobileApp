package com.example.cookmate.models;

public class FavoriteItem {
    private String id;
    private String image;
    private String title;
    private Integer readyInMinutes;
    private Integer servings;

    public FavoriteItem() {}

    public FavoriteItem(String id, String image, String title, Integer readyInMinutes, Integer servings) {
        this.id = id;
        this.image = image;
        this.title = title;
        this.readyInMinutes = readyInMinutes;
        this.servings = servings;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getReadyInMinutes() { return readyInMinutes; }
    public void setReadyInMinutes(Integer readyInMinutes) { this.readyInMinutes = readyInMinutes; }

    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
}
