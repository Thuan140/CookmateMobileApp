//package com.example.cookmate;
//
//import android.app.Activity;
//import android.content.Context;
//import android.util.Log;
//import android.widget.Toast;
//
//import com.android.volley.AuthFailureError;
//import com.android.volley.Request;
//import com.android.volley.toolbox.JsonObjectRequest;
//import com.android.volley.toolbox.Volley;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class SearchApiService {
//    private static final String TAG = "SearchApiService";
//    private static final String BASE_URL = "https://cookm8.vercel.app/api/recipes/search";
//    private final Context context;
//    private final SessionManager session;
//
//    public interface SearchCallback {
//        void onSuccess(List<Recipe> recipes);
//        void onError(String message);
//    }
//
//    public SearchApiService(Context context) {
//        this.context = context;
//        this.session = new SessionManager(context);
//    }
//
//public void searchRecipes(String query, SearchCallback callback) {
//    try {
//        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
//        String url = "https://cookm8.vercel.app/api/recipes/search?query=" + encodedQuery + "&limit=10";
//        Log.d(TAG, " Request URL: " + url);
//
//        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
//                response -> {
//                    try {
//                        Log.d(TAG, " Response: " + response.toString(2));
//
//                        JSONArray arr = response.optJSONArray("recipes");
//                        if (arr == null) {
//                            callback.onError("API không trả về danh sách recipes.");
//                            return;
//                        }
//
//                        List<Recipe> list = new ArrayList<>();
//                        for (int i = 0; i < arr.length(); i++) {
//                            JSONObject obj = arr.getJSONObject(i);
//                            int id = obj.optInt("id");
//                            String title = obj.optString("title", "Untitled");
//                            String image = obj.optString("image", "");
//                            String summary = obj.optString("summary", "Không có mô tả");
//                            list.add(new Recipe(id, title, image, summary));
//                        }
//
//                        Log.i(TAG, " Parsed " + list.size() + " recipes");
//                        callback.onSuccess(list);
//
//                    } catch (Exception e) {
//                        Log.e(TAG, " Parse error: " + e.getMessage());
//                        callback.onError("Parse error: " + e.getMessage());
//                    }
//                },
//                error -> {
//                    Log.e(TAG, " Network error: " + error);
//                    String msg = "Network error";
//
//                    if (error.networkResponse != null) {
//                        int status = error.networkResponse.statusCode;
//                        msg = "HTTP " + status;
//                        Log.e(TAG, "HTTP Status Code: " + status);
//                        try {
//                            String resp = new String(error.networkResponse.data);
//                            Log.e(TAG, "Response Data: " + resp);
//                        } catch (Exception ignored) {}
//                    }
//
//                    callback.onError(msg);
//                }) {
//
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String> headers = new HashMap<>();
//                headers.put("Content-Type", "application/json");
//                // Không cần Authorization cho endpoint public
//                return headers;
//            }
//        };
//
//        Volley.newRequestQueue(context).add(request);
//        Log.d(TAG, " Request added to Volley queue");
//
//    } catch (Exception e) {
//        callback.onError("URL encode error: " + e.getMessage());
//    }
//}
//    /**
//     * Hiển thị Toast an toàn, đảm bảo chạy trên Main Thread.
//     */
//    private void showToast(String message) {
//        Log.d(TAG, " Toast: " + message);
//        if (context instanceof Activity) {
//            ((Activity) context).runOnUiThread(() ->
//                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
//            );
//        } else {
//            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
//        }
//    }
//}

package com.example.cookmate;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchApiService {
    private static final String TAG = "SearchApiService";
    private static final String BASE_URL = "https://cookm8.vercel.app/api/recipes/search";
    private final Context context;
    private final SessionManager session;

    public interface SearchCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(String message);
    }

    public SearchApiService(Context context) {
        this.context = context;
        this.session = new SessionManager(context);
    }

    public void searchRecipes(String query, Integer limit, String cuisine, String diet, Integer maxReadyTime, SearchCallback callback) {
        try {
            StringBuilder urlBuilder = new StringBuilder(BASE_URL + "?");

            // query là bắt buộc
            if (query != null && !query.trim().isEmpty()) {
                urlBuilder.append("query=").append(URLEncoder.encode(query, "UTF-8"));
            } else {
                callback.onError("Thiếu từ khóa tìm kiếm (query)");
                return;
            }

            if (limit != null && limit > 0) {
                urlBuilder.append("&limit=").append(limit);
            }
            if (cuisine != null && !cuisine.trim().isEmpty()) {
                urlBuilder.append("&cuisine=").append(URLEncoder.encode(cuisine, "UTF-8"));
            }
            if (diet != null && !diet.trim().isEmpty()) {
                urlBuilder.append("&diet=").append(URLEncoder.encode(diet, "UTF-8"));
            }
            if (maxReadyTime != null && maxReadyTime > 0) {
                urlBuilder.append("&maxReadyTime=").append(maxReadyTime);
            }

            String url = urlBuilder.toString();
            Log.d(TAG, "➡ Request URL: " + url);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            Log.d(TAG, " Response: " + response.toString(2));
                            JSONArray arr = response.optJSONArray("recipes");
                            if (arr == null) {
                                callback.onError("API không trả về danh sách recipes.");
                                return;
                            }

                            List<Recipe> list = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                int id = obj.optInt("id");
                                String title = obj.optString("title", "Untitled");
                                String image = obj.optString("image", "");
                                String summary = obj.optString("summary", "Không có mô tả");
                                list.add(new Recipe(id, title, image, summary));
                            }

                            Log.i(TAG, "Parsed " + list.size() + " recipes");
                            callback.onSuccess(list);

                        } catch (Exception e) {
                            Log.e(TAG, " Parse error: " + e.getMessage());
                            callback.onError("Parse error: " + e.getMessage());
                        }
                    },
                    error -> {
                        String msg = "Network error";
                        if (error.networkResponse != null) {
                            int status = error.networkResponse.statusCode;
                            msg = "HTTP " + status;
                            try {
                                String resp = new String(error.networkResponse.data);
                                Log.e(TAG, " Response Data: " + resp);
                            } catch (Exception ignored) {}
                        }
                        Log.e(TAG, " Network error: " + msg);
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
            Log.d(TAG, " Request added to Volley queue");

        } catch (Exception e) {
            callback.onError("URL encode error: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            );
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
}

