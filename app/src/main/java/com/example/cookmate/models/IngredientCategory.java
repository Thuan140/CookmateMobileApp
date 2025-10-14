package com.example.cookmate.models;

public class IngredientCategory {
    private String id;
    private String name;

    public IngredientCategory() {}

    public IngredientCategory(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name; // Spinner hiển thị tên
    }
}
