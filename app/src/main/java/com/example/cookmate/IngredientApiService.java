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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * IngredientApiService
 * - GET/CREATE/UPDATE still use Volley (existing implementation)
 * - DELETE uses OkHttp to guarantee DELETE with JSON body (works like Postman)
 */
public class IngredientApiService {
    private static final String TAG = "IngredientApiService";
    private static final String BASE_URL = "https://cookm8.vercel.app/api/ingredients";
    private final RequestQueue requestQueue;
    private final Context ctx;

    public IngredientApiService(Context context) {
        this.ctx = context;
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public interface IngredientCallback {
        void onSuccess(JSONArray ingredients);
        void onError(String errorMessage);
    }

    public interface SimpleCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public interface DeleteCallback {
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
    }

    private void tryObjectRequest(String token, IngredientCallback callback) {
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
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String,String> headers = new java.util.HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        requestQueue.add(req);
    }

    // ---------- CREATE (multipart POST) ----------
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
    }

    // ---------- UPDATE (multipart PUT) ----------
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
    }

    // ---------- UPDATE (JSON no-image) ----------
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

    // ---------- DELETE (OkHttp) ----------
    /**
     * Public delete wrapper — uses OkHttp under the hood to guarantee DELETE with JSON body.
     * Call this from your Activity like:
     *   new IngredientApiService(context).deleteIngredient(token, id, callback);
     */
    public void deleteIngredient(String token, String ingredientId, DeleteCallback callback) {
        deleteIngredientWithOkHttp(token, ingredientId, callback);
    }

    // Actual OkHttp implementation
    private void deleteIngredientWithOkHttp(String token, String ingredientId, DeleteCallback callback) {
        OkHttpClient client = new OkHttpClient();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            json.put("ingredientId", ingredientId);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("JSON error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(BASE_URL)
                .delete(body) // DELETE with body
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                // optional: add Postman-like headers if server is picky
                .addHeader("User-Agent", "PostmanRuntime/7.48.0")
                .addHeader("Accept", "*/*")
                .addHeader("Connection", "keep-alive")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .build();

        Log.d(TAG, "OkHttp DELETE " + BASE_URL + " body=" + json.toString());
        Log.d(TAG, "OkHttp Headers: Authorization=Bearer [REDACTED], Content-Type=application/json");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "OkHttp DELETE failure: " + e.getMessage(), e);
                // run callback on main thread
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Network failure: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = null;
                try {
                    if (response.body() != null) respBody = response.body().string();
                } catch (Exception ex) {
                    Log.e(TAG, "Read body error", ex);
                }

                Log.d(TAG, "OkHttp DELETE response code=" + response.code() + " body=" + respBody);

                final String finalResp = respBody;
                if (response.isSuccessful()) {
                    new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                } else {
                    String msg = "HTTP " + response.code() + (finalResp != null ? " - " + finalResp : "");
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(msg));
                }
            }
        });
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
