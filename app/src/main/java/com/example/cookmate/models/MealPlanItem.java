package com.example.cookmate.models;

import java.util.List;

public class MealPlanItem {
    private String id;
    private String name;
    private List<String> recipeIds;
    private String notes;
    private String date; // ISO string from server

    public MealPlanItem() {}

    public MealPlanItem(String id, String name, List<String> recipeIds, String notes, String date) {
        this.id = id;
        this.name = name;
        this.recipeIds = recipeIds;
        this.notes = notes;
        this.date = date;
    }

    // getters / setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getRecipeIds() { return recipeIds; }
    public void setRecipeIds(List<String> recipeIds) { this.recipeIds = recipeIds; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
