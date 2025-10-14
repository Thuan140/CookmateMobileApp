package com.example.cookmate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class IngredientCategoryApiService {
    private static final String TAG = "IngredientCategoryApi";
    private static final String BASE_URL = "https://cookm8.vercel.app/api/ingredient-categories";
    private final RequestQueue requestQueue;

    public IngredientCategoryApiService(Context ctx) {
        requestQueue = Volley.newRequestQueue(ctx);
    }

    public interface CategoriesCallback {
        void onSuccess(JSONArray categories);
        void onError(String message);
    }

    public void getCategories(String token, CategoriesCallback cb) {
        Log.d(TAG, "GET " + BASE_URL);
        JsonArrayRequest reqArray = new JsonArrayRequest(Request.Method.GET, BASE_URL, null,
                response -> cb.onSuccess(response),
                error -> {
                    // fallback: maybe server returns an object like { "ingredientCategory": [...] }
                    tryObjectFallback(token, cb);
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                if (token != null) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        requestQueue.add(reqArray);
    }

    private void tryObjectFallback(String token, CategoriesCallback cb) {
        JsonObjectRequest objReq = new JsonObjectRequest(Request.Method.GET, BASE_URL, null,
                response -> {
                    // try to find array under known keys
                    if (response.has("ingredientCategory")) {
                        try {
                            JSONArray arr = response.getJSONArray("ingredientCategory");
                            cb.onSuccess(arr);
                            return;
                        } catch (Exception ex) { /* fallthrough */ }
                    }
                    if (response.has("categories")) {
                        try {
                            JSONArray arr = response.getJSONArray("categories");
                            cb.onSuccess(arr);
                            return;
                        } catch (Exception ex) { /* fallthrough */ }
                    }
                    // else not understood
                    cb.onError("Unexpected response shape: " + response.toString());
                },
                error -> new Handler(Looper.getMainLooper()).post(() -> {
                    String msg = (error.networkResponse != null) ? "HTTP " + error.networkResponse.statusCode : "Network Failure";
                    cb.onError(msg);
                })) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                if (token != null) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        requestQueue.add(objReq);
    }
}
