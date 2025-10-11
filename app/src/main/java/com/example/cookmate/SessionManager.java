package com.example.cookmate;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.cookmate.User;
import com.google.gson.Gson;

public class SessionManager {
    private static final String PREF_NAME = "cookmate_session";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER = "user_data";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveAuthData(String token, User user) {
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USER, new Gson().toJson(user));
        editor.apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json != null) {
            return new Gson().fromJson(json, User.class);
        }
        return null;
    }

    public void clear() {
        editor.clear().apply();
    }
}