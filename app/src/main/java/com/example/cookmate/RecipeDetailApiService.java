package com.example.cookmate;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RecipeDetailApiService {

    private static final String TAG = "RecipeDetailApiService";
    private static final String URL = "https://cookm8.vercel.app/api/recipes/bulk";
    private final Context context;

    public interface RecipeDetailCallback {
        void onSuccess(JSONObject recipe);
        void onError(String message);
    }

    public RecipeDetailApiService(Context context) {
        this.context = context;
    }

    public void fetchRecipeDetail(int recipeId, RecipeDetailCallback callback) {
        try {
            JSONObject body = new JSONObject();
            JSONArray idArray = new JSONArray();
            idArray.put(recipeId);
            body.put("recipeIds", idArray);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL, body,
                    response -> {
                        try {
                            if ("Recipes retrieved successfully".equals(response.optString("message"))) {
                                JSONArray arr = response.optJSONArray("recipes");
                                if (arr != null && arr.length() > 0) {
                                    JSONObject recipe = arr.getJSONObject(0);
                                    callback.onSuccess(recipe);
                                } else {
                                    callback.onError("Không tìm thấy công thức.");
                                }
                            } else {
                                callback.onError("Phản hồi không hợp lệ.");
                            }
                        } catch (Exception e) {
                            callback.onError("Parse error: " + e.getMessage());
                        }
                    },
                    error -> {
                        String msg = "Network error";
                        if (error.networkResponse != null)
                            msg += " (HTTP " + error.networkResponse.statusCode + ")";
                        Log.e(TAG, msg);
                        callback.onError(msg);
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            Volley.newRequestQueue(context).add(request);
        } catch (Exception e) {
            callback.onError("JSON error: " + e.getMessage());
        }
    }
}
