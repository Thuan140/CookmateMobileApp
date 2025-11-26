package com.example.cookmate;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FavoriteApiService {

    private static final String BASE_URL = "https://cookm8.vercel.app/api/favorites";
    private final Context context;

    public interface FavoriteCallback {
        void onSuccess(JSONArray favorites);
        void onError(String message);
    }

    public interface FavoriteAddCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public FavoriteApiService(Context context) {
        this.context = context;
    }

    // ===================== GET /favorites =====================
    public void getFavorites(String token, FavoriteCallback callback) {

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, BASE_URL, null,
                response -> {
                    try {
                        if ("Favorites retrieved successfully".equals(response.optString("message"))) {
                            callback.onSuccess(response.optJSONArray("recipes"));
                        } else {
                            callback.onError("Invalid response format");
                        }
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        callback.onError("HTTP " + error.networkResponse.statusCode);
                    } else {
                        callback.onError("Network connection error");
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    // ===================== POST /favorites =====================
    public void addFavorite(String token, String recipeId, FavoriteAddCallback callback) {

        JSONObject body = new JSONObject();
        try {
            body.put("recipeId", recipeId);
        } catch (Exception ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, BASE_URL, body,
                response -> {
                    if ("Favorite added successfully".equals(response.optString("message"))) {
                        callback.onSuccess("Favorite added successfully");
                    } else {
                        callback.onError(response.optString("error", "Unknown error"));
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        callback.onError("HTTP " + error.networkResponse.statusCode);
                    } else {
                        callback.onError("Network error");
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }
}
