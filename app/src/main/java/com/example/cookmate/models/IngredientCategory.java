package com.example.cookmate.models;

import java.io.Serializable;

// ✅ THÊM: implements Serializable
public class IngredientCategory implements Serializable {

    // Khuyến nghị thêm serialVersionUID để kiểm soát phiên bản
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String icon; // emoji or icon string

    public IngredientCategory() {}

    public IngredientCategory(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public IngredientCategory(String id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    @Override
    public String toString() { return name; }
}