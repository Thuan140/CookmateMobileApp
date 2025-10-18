//package com.example.cookmate;
//public class Recipe {
//    private String id;
//    private String title;
//    private String image;
//    private String summary;
//    private int readyInMinutes;
//    private int servings;
//
//    public String getId() { return id; }
//    public String getTitle() { return title; }
//    public String getImage() { return image; }
//    public String getSummary() { return summary; }
//    public int getReadyInMinutes() { return readyInMinutes; }
//    public int getServings() { return servings; }
//}
package com.example.cookmate;

import java.util.List;

public class Recipe {
    private String id;
    private String title;
    private String image;
    private int readyInMinutes;
    private int servings;
    private String summary;
    private List<String> instructions;
    private List<Ingredient> ingredients;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getImage() { return image; }
    public int getReadyInMinutes() { return readyInMinutes; }
    public int getServings() { return servings; }
    public String getSummary() { return summary; }
    public List<String> getInstructions() { return instructions; }
    public List<Ingredient> getIngredients() { return ingredients; }

    public static class Ingredient {
        private String id;
        private String name;
        private double amount;
        private String unit;
        private String image;

        public String getName() { return name; }
        public double getAmount() { return amount; }
        public String getUnit() { return unit; }
        public String getImage() { return image; }
    }
}
