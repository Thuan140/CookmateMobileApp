package com.example.cookmate;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONArray;
import org.json.JSONObject;


import java.util.HashMap;
import java.util.Map;


public class IngredientApiService {
    private static final String TAG = "IngredientApiService";
    private static final String BASE_URL = "https://cookm8.vercel.app/api/ingredients";
    private final RequestQueue requestQueue;
    private final Context ctx;


    public IngredientApiService(Context context) {
        this.ctx = context;
        requestQueue = Volley.newRequestQueue(context);
    }


    public interface IngredientCallback {
        void onSuccess(JSONArray ingredients);
        void onError(String errorMessage);
    }


    public interface SimpleCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }public interface DeleteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }


    // ---------- GET ingredients ----------
    public void getIngredients(String token, IngredientCallback callback) {
        Log.d(TAG, "GET " + BASE_URL);
        JsonArrayRequest arrayRequest = new JsonArrayRequest(Request.Method.GET, BASE_URL, null,
                response -> callback.onSuccess(response),
                error -> {
                    Log.w(TAG, "GET JSONArray failed, trying object fallback: " + error);
                    tryObjectRequest(token, callback);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> headers = new HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        requestQueue.add(arrayRequest);
    }private void tryObjectRequest(String token, IngredientCallback callback) {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, BASE_URL, null,
                response -> {
                    if (response.has("ingredients")) {
                        try {
                            JSONArray arr = response.getJSONArray("ingredients");
                            callback.onSuccess(arr);
                        } catch (Exception ex) {
                            callback.onError("Parse error: " + ex.getMessage());
                        }
                    } else {
                        callback.onError("Unexpected response: " + response.toString());
                    }
                },
                error -> new Handler(Looper.getMainLooper()).post(() -> {
                    String msg = extractVolleyError(error.networkResponse, "Network Failure");
                    callback.onError(msg);
                })) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> headers = new HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        requestQueue.add(req);
    }// ---------- CREATE (multipart POST) ----------
    public void createIngredientWithImage(String token,
                                          Map<String, String> fields,
                                          byte[] imageData,
                                          SimpleCallback callback) {
        Log.d(TAG, "POST multipart " + BASE_URL + " fields=" + fields);
        VolleyMultipartRequest vmr = new VolleyMultipartRequest(Request.Method.POST, BASE_URL,
                response -> {
                    try {
                        String json = new String(response.data, "UTF-8");
                        JSONObject obj = new JSONObject(json);
                        callback.onSuccess(obj);
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                },
                error -> {
                    String msg = extractVolleyError(error.networkResponse, "Network failure");
                    callback.onError(msg);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                return fields;
            }


            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> map = new HashMap<>();
                if (imageData != null && imageData.length > 0) {
                    map.put("image", new DataPart("image.jpg", imageData, "image/jpeg"));
                }
                return map;
            }


            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> headers = new HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };


        vmr.setRetryPolicy(new DefaultRetryPolicy(20000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(vmr);
    }// ---------- UPDATE (multipart PUT) ----------
    public void updateIngredientWithImage(String token,
                                          Map<String, String> fields,
                                          byte[] imageData,
                                          SimpleCallback callback) {
        Log.d(TAG, "PUT multipart " + BASE_URL + " fields=" + fields);
        VolleyMultipartRequest vmr = new VolleyMultipartRequest(Request.Method.PUT, BASE_URL,
                response -> {
                    try {
                        String json = new String(response.data, "UTF-8");
                        JSONObject obj = new JSONObject(json);
                        callback.onSuccess(obj);
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                },
                error -> {
                    String msg = extractVolleyError(error.networkResponse, "Network failure");
                    callback.onError(msg);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                return fields;
            }


            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> map = new HashMap<>();
                if (imageData != null && imageData.length > 0) {
                    map.put("image", new DataPart("image.jpg", imageData, "image/jpeg"));
                }
                return map;
            }


            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> headers = new HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };


        vmr.setRetryPolicy(new DefaultRetryPolicy(20000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(vmr);
    }// ---------- UPDATE (JSON no-image) ----------
    public void updateIngredientNoImage(String token, JSONObject body, SimpleCallback callback) {
        Log.d(TAG, "PUT json " + BASE_URL + " body=" + body);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.PUT, BASE_URL, body,
                response -> callback.onSuccess(response),
                error -> new Handler(Looper.getMainLooper()).post(() -> {
                    String msg = extractVolleyError(error.networkResponse, "Network failure");
                    callback.onError(msg);
                })) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> headers = new HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        requestQueue.add(req);
    }


    // ---------- DELETE ----------
// body must be JSON like { "ingredientId": "..." }
    public void deleteIngredient(String token, JSONObject body, DeleteCallback callback) {
        Log.d(TAG, "deleteIngredient - attempt DELETE body=" + (body != null ? body.toString() : "null"));


// 1) Thử gửi DELETE trực tiếp (một số server sẽ chấp nhận)
        JsonObjectRequest deleteReq = new JsonObjectRequest(Request.Method.DELETE, BASE_URL, body,
                response -> {
                    Log.d(TAG, "deleteIngredient - DELETE direct success: " + response.toString());
                    callback.onSuccess();
                },
                error -> {
// Lấy body server trả (nếu có)
                    String serverBody = null;
                    int status = -1;
                    if (error.networkResponse != null) {
                        status = error.networkResponse.statusCode;
                        try { serverBody = new String(error.networkResponse.data, "UTF-8"); }
                        catch (Exception ignored) {}
                    }
                    Log.w(TAG, "deleteIngredient - DELETE direct failed status=" + status + " body=" + serverBody);


// 2) Thử fallback: POST + X-HTTP-Method-Override: DELETE (nhiều backend hỗ trợ)
                    tryFallbackDeleteWithPost(token, body, callback, status, serverBody);
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
        };


        requestQueue.add(deleteReq);
    }private void tryFallbackDeleteWithPost(String token, JSONObject body, DeleteCallback callback, int prevStatus, String prevBody) {
        Log.d(TAG, "tryFallbackDeleteWithPost - sending POST w/ X-HTTP-Method-Override=DELETE body=" + (body != null ? body.toString() : "null") + " prevStatus=" + prevStatus + " prevBody=" + prevBody);


        JsonObjectRequest postOverride = new JsonObjectRequest(Request.Method.POST, BASE_URL, body,
                response -> {
                    Log.d(TAG, "tryFallbackDeleteWithPost - success response: " + response.toString());
                    callback.onSuccess();
                },
                error -> {
                    String finalMsg;
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try { finalMsg = new String(error.networkResponse.data, "UTF-8"); }
                        catch (Exception e) { finalMsg = "HTTP " + error.networkResponse.statusCode; }
                    } else {
                        finalMsg = "Network failure";
                    }
                    Log.w(TAG, "tryFallbackDeleteWithPost - failed: " + finalMsg + " (previous: " + prevStatus + " body=" + prevBody + ")");
                    callback.onError(finalMsg);
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                headers.put("X-HTTP-Method-Override", "DELETE");
                return headers;
            }
        };


        requestQueue.add(postOverride);
    }


    // helper to extract server body from Volley error
    private static String extractVolleyError(com.android.volley.NetworkResponse nr, String fallback) {
        if (nr != null && nr.data != null) {
            try {
                String body = new String(nr.data, "UTF-8");
                return "HTTP " + nr.statusCode + " - " + body;
            } catch (Exception e) {
                return "HTTP " + nr.statusCode;
            }
        }
        return fallback;
    }


}