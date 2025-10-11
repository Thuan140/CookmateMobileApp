package com.example.cookmate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AuthResponse {
    private String message;
    private User user;
    private String token;

    public AuthResponse(String message, User user, String token) {
        this.message = message;
        this.user = user;
        this.token = token;
    }

    public static AuthResponse fromJson(JSONObject json) throws JSONException {
        String message = json.getString("message");
        JSONObject userJson = json.getJSONObject("user");
        String token = json.getString("token");

        User user = new User(
                userJson.getString("id"),
                userJson.getString("email"),
                userJson.getString("name"),
                userJson.getString("avatar"),
                jsonArrayToList(userJson.getJSONArray("dietaryPreferences"))
        );

        return new AuthResponse(message, user, token);
    }

    private static ArrayList<String> jsonArrayToList(JSONArray array) throws JSONException {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }

    public String getMessage() { return message; }
    public User getUser() { return user; }
    public String getToken() { return token; }
}
