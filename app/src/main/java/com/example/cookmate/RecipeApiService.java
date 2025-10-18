package com.example.cookmate;

import android.content.Context;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeApiService {

    private static final String API_URL = "https://cookm8.vercel.app/api/recipes/today";
    private Context context;

    public interface RecipeCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(String error);
    }

    public RecipeApiService(Context context) {
        this.context = context;
    }

    public void getTodayRecipes(String token, RecipeCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, API_URL, null,
                response -> {
                    try {
                        JSONArray recipesArray = response.getJSONArray("recipes");
                        List<Recipe> list = new ArrayList<>();
                        for (int i = 0; i < recipesArray.length(); i++) {
                            JSONObject obj = recipesArray.getJSONObject(i);
                            Recipe recipe = new Recipe();
                            // Dùng reflection đơn giản
                            recipe = new com.google.gson.Gson().fromJson(obj.toString(), Recipe.class);
                            list.add(recipe);
                        }
                        callback.onSuccess(list);
                    } catch (Exception e) {
                        callback.onError(e.getMessage());
                    }
                },
                error -> {
                    Log.e("API_ERROR", "Error: " + error.toString());
                    callback.onError("API call failed");
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }
}
