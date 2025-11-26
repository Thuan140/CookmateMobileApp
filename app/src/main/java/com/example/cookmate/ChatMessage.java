package com.example.cookmate;

import com.example.cookmate.models.FavoriteItem;
import com.example.cookmate.models.MealPlanItem;
import com.example.cookmate.models.ShoppingItem;

public class ChatMessage {

    private String role;
    private String content;

    private MealPlanItem mealPlanItem;
    private boolean isMealPlanItem = false;

    private User user;
    private boolean isUserProfile = false;

    private ShoppingItem shoppingItem;
    private boolean isShoppingItem = false;

    private FavoriteItem favoriteItem;
    private boolean isFavoriteItem = false;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // constructor dành cho MealPlanItem
    public ChatMessage(MealPlanItem mealPlanItem) {
        this.role = "assistant";
        this.content = "";
        this.mealPlanItem = mealPlanItem;
        this.isMealPlanItem = true;
    }

    public boolean isMealPlanItem() { return isMealPlanItem; }
    public MealPlanItem getMealPlanItem() { return mealPlanItem; }

    public ChatMessage(ShoppingItem item) {
        this.role = "assistant";
        this.shoppingItem = item;
        this.isShoppingItem = true;
    }

    public boolean isShoppingItem() { return isShoppingItem; }
    public ShoppingItem getShoppingItem() { return shoppingItem; }

    public ChatMessage(User user) {
        this.role = "assistant";
        this.content = "";
        this.user = user;
        this.isUserProfile = true;
    }

    public boolean isUserProfile() { return isUserProfile; }
    public User getUser() { return user; }

    public ChatMessage(FavoriteItem item) {
        this.role = "assistant";
        this.favoriteItem = item;
        this.isFavoriteItem = true;
    }

    public boolean isFavoriteItem() { return isFavoriteItem; }
    public FavoriteItem getFavoriteItem() { return favoriteItem; }

    public String getRole() { return role; }
    public String getContent() { return content; }



}
