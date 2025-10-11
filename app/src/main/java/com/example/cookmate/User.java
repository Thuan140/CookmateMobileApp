package com.example.cookmate;

import java.util.List;

public class User {
    private String id;
    private String email;
    private String name;
    private String avatar;
    private List<String> dietaryPreferences;

    public User(String id, String email, String name, String avatar, List<String> dietaryPreferences) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.avatar = avatar;
        this.dietaryPreferences = dietaryPreferences;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public List<String> getDietaryPreferences() { return dietaryPreferences; }

    // Setter
    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setDietaryPreferences(List<String> dietaryPreferences) { this.dietaryPreferences = dietaryPreferences; }
}
