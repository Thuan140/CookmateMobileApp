package com.example.cookmate;

import java.util.List;

public class Recipe {
    private int id;
    private String title;
    private String image;
    private String summary;
    private List<Ingredient> ingredients; // thêm nếu bạn muốn hiển thị chi tiết

    public Recipe(int id, String title, String image, String summary) {
        this.id = id;
        this.title = title;
        this.image = image;
        this.summary = summary;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getImage() { return image; }
    public String getSummary() { return summary; }
    public List<Ingredient> getIngredients() { return ingredients; }

    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setImage(String image) { this.image = image; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }

    // 🔹 Inner class cho nguyên liệu
    public static class Ingredient {
        private String id;
        private String name;
        private double amount;
        private String unit;
        private String image;

        public Ingredient(String id, String name, double amount, String unit, String image) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.unit = unit;
            this.image = image;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public double getAmount() { return amount; }
        public String getUnit() { return unit; }
        public String getImage() { return image; }

        public void setId(String id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setAmount(double amount) { this.amount = amount; }
        public void setUnit(String unit) { this.unit = unit; }
        public void setImage(String image) { this.image = image; }
    }
}
