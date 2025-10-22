package com.example.cookmate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class IngredientCategoryApiService {
    private static final String TAG = "IngredientCategoryApi";
    private static final String BASE_URL = "https://cookm8.vercel.app/api/ingredient-categories";
    private final RequestQueue requestQueue;

    public IngredientCategoryApiService(Context ctx) {
        requestQueue = Volley.newRequestQueue(ctx);
    }

    // callbacks
    public interface CategoriesCallback {
        void onSuccess(JSONArray categories);
        void onError(String message);
    }

    public interface CategoryCallback {
        void onSuccess(JSONObject category);
        void onError(String message);
    }

    public interface CategoryApiCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    // ---------------- GET list ----------------
    public void getCategories(String token, CategoriesCallback cb) {
        Log.d(TAG, "GET " + BASE_URL);
        JsonArrayRequest reqArray = new JsonArrayRequest(Request.Method.GET, BASE_URL, null,
                response -> {
                    Log.d(TAG, "GET array response: " + response.toString());
                    cb.onSuccess(response);
                },
                error -> {
                    Log.w(TAG, "GET array failed, trying object fallback: " + error.toString());
                    tryObjectFallback(token, cb);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                if (token != null && !token.isEmpty()) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        requestQueue.add(reqArray);
    }

    // fallback when server returns object containing array
    private void tryObjectFallback(String token, CategoriesCallback cb) {
        Log.d(TAG, "tryObjectFallback GET " + BASE_URL);
        JsonObjectRequest objReq = new JsonObjectRequest(Request.Method.GET, BASE_URL, null,
                response -> {
                    Log.d(TAG, "GET object fallback response: " + response.toString());
                    try {
                        if (response.has("ingredientCategories")) {
                            JSONArray arr = response.getJSONArray("ingredientCategories");
                            cb.onSuccess(arr);
                            return;
                        }
                        if (response.has("ingredientCategory")) {
                            JSONArray arr = response.getJSONArray("ingredientCategory");
                            cb.onSuccess(arr);
                            return;
                        }
                        if (response.has("categories")) {
                            JSONArray arr = response.getJSONArray("categories");
                            cb.onSuccess(arr);
                            return;
                        }
                        cb.onError("Unexpected response shape: " + response.toString());
                    } catch (Exception ex) {
                        Log.e(TAG, "Parse error in fallback: " + ex.getMessage(), ex);
                        cb.onError("Parse error: " + ex.getMessage());
                    }
                },
                error -> new Handler(Looper.getMainLooper()).post(() -> {
                    String msg = (error.networkResponse != null) ? "HTTP " + error.networkResponse.statusCode : "Network Failure";
                    Log.e(TAG, "GET object fallback error: " + msg + " | " + error.toString());
                    cb.onError(msg);
                })) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                if (token != null && !token.isEmpty()) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        requestQueue.add(objReq);
    }

    // ---------------- GET single ----------------
    public void getCategoryById(String token, String categoryId, CategoryCallback cb) {
        String url = BASE_URL + "/" + categoryId;
        Log.d(TAG, "GET single " + url);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "GET single category response: " + response.toString());
                    cb.onSuccess(response);
                },
                error -> new Handler(Looper.getMainLooper()).post(() -> {
                    String msg = (error.networkResponse != null) ? "HTTP " + error.networkResponse.statusCode : "Network Failure";
                    Log.e(TAG, "GET single category error: " + msg + " | " + error.toString());
                    cb.onError(msg);
                })) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                if (token != null && !token.isEmpty()) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        requestQueue.add(req);
    }

    // ---------------- UPDATE (primary PUT) ----------------
    /**
     * Primary PUT to plural endpoint BASE_URL/{id}
     */
    public void updateCategory(String token, String categoryId, String name, String icon, CategoryApiCallback cb) {
        // ✅ Gọi đúng endpoint không có /{id}
        String url = BASE_URL;
        Log.d(TAG, "PUT " + url);

        JSONObject payload = new JSONObject();
        try {
            payload.put("ingredientCategoryId", categoryId); // ✅ backend yêu cầu field này
            payload.put("name", name);
            payload.put("icon", icon);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to create JSON payload: " + ex.getMessage(), ex);
            cb.onError("Failed to create request payload.");
            return;
        }

        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PUT, url, payload,
                response -> {
                    Log.d(TAG, "PUT response: " + response.toString());
                    cb.onSuccess();
                },
                error -> new Handler(Looper.getMainLooper()).post(() -> {
                    String msg = (error.networkResponse != null)
                            ? "HTTP " + error.networkResponse.statusCode
                            : "Network Failure";
                    Log.e(TAG, "PUT error: " + msg + " | " + error.toString());
                    cb.onError(msg);
                })) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                h.put("Content-Type", "application/json");
                if (token != null && !token.isEmpty())
                    h.put("Authorization", "Bearer " + token);
                return h;
            }
        };

        requestQueue.add(putRequest);
    }
    public void updateCategoryWithFallback(String token, String categoryId, String name, String icon, CategoryApiCallback cb) {
        // Đầu tiên thử endpoint đúng chuẩn theo Postman
        String url = BASE_URL;
        Log.d(TAG, "PUT (fallback) " + url);

        JSONObject payload = new JSONObject();
        try {
            payload.put("ingredientCategoryId", categoryId); // ✅ backend yêu cầu
            payload.put("name", name);
            payload.put("icon", icon);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to create JSON payload: " + ex.getMessage(), ex);
            cb.onError("Failed to create JSON payload");
            return;
        }

        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PUT, url, payload,
                response -> {
                    Log.d(TAG, "PUT fallback response: " + response.toString());
                    cb.onSuccess();
                },
                error -> {
                    // Nếu lỗi 404 hoặc 405, thử kiểu cũ (/{id})
                    int code = (error.networkResponse != null) ? error.networkResponse.statusCode : -1;
                    Log.w(TAG, "PUT fallback error: HTTP " + code);
                    if (code == 404 || code == 405) {
                        updateCategoryOldStyle(token, categoryId, name, icon, cb);
                    } else {
                        cb.onError("HTTP " + code);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                h.put("Content-Type", "application/json");
                if (token != null && !token.isEmpty())
                    h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        requestQueue.add(putRequest);
    }

    // ✅ fallback kiểu cũ nếu server vẫn hỗ trợ /{id}
    private void updateCategoryOldStyle(String token, String categoryId, String name, String icon, CategoryApiCallback cb) {
        String url = BASE_URL + "/" + categoryId;
        Log.d(TAG, "PUT old style " + url);

        JSONObject payload = new JSONObject();
        try {
            payload.put("name", name);
            payload.put("icon", icon);
        } catch (Exception e) {
            cb.onError("Payload error: " + e.getMessage());
            return;
        }

        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PUT, url, payload,
                response -> {
                    Log.d(TAG, "PUT old style response: " + response.toString());
                    cb.onSuccess();
                },
                error -> {
                    String msg = (error.networkResponse != null)
                            ? "HTTP " + error.networkResponse.statusCode
                            : "Network Failure";
                    cb.onError(msg);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                h.put("Content-Type", "application/json");
                if (token != null && !token.isEmpty())
                    h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        requestQueue.add(putRequest);
    }




}
