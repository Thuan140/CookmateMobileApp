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

public class RecipeTodayApiService {

    private static final String API_URL = "https://cookm8.vercel.app/api/recipes/today";
    private final Context context;

    public interface RecipeCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(String error);
    }

    public RecipeTodayApiService(Context context) {
        this.context = context;
    }

    public void getTodayRecipes(String token, RecipeCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, API_URL, null,
                response -> {
                    try {
                        if (!response.has("recipes")) {
                            callback.onError("Invalid response structure");
                            return;
                        }

                        JSONArray recipesArray = response.getJSONArray("recipes");
                        List<Recipe> list = new ArrayList<>();

                        for (int i = 0; i < recipesArray.length(); i++) {
                            JSONObject obj = recipesArray.getJSONObject(i);

                            int id = obj.optInt("id", 0);
                            String title = obj.optString("title", "Untitled");
                            String image = obj.optString("image", "");
                            String summary = obj.optString("summary", "No description available");

                            Recipe recipe = new Recipe(id, title, image, summary);

                            // ✅ Parse ingredients nếu có
                            if (obj.has("ingredients")) {
                                JSONArray ingredientsArray = obj.getJSONArray("ingredients");
                                List<Recipe.Ingredient> ingredients = new ArrayList<>();

                                for (int j = 0; j < ingredientsArray.length(); j++) {
                                    JSONObject ingObj = ingredientsArray.getJSONObject(j);
                                    String ingId = ingObj.optString("id", "");
                                    String name = ingObj.optString("name", "");
                                    double amount = ingObj.optDouble("amount", 0);
                                    String unit = ingObj.optString("unit", "");
                                    String ingImage = ingObj.optString("image", "");

                                    ingredients.add(new Recipe.Ingredient(ingId, name, amount, unit, ingImage));
                                }

                                recipe.setIngredients(ingredients);
                            }

                            list.add(recipe);
                        }

                        callback.onSuccess(list);

                    } catch (Exception e) {
                        Log.e("API_PARSE_ERROR", "Error parsing recipes: " + e.getMessage());
                        callback.onError("Parse error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e("API_ERROR", "Error: " + error.toString());
                    callback.onError("API call failed: " + error.getMessage());
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
