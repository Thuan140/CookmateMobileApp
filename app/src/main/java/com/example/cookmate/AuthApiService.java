//package com.example.cookmate;
//
//import android.content.Context;
//import android.os.Handler;
//import android.os.Looper;
//
//import com.android.volley.Request;
//import com.android.volley.RequestQueue;
//import com.android.volley.toolbox.JsonObjectRequest;
//import com.android.volley.toolbox.Volley;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//public class AuthApiService {
//    private static final String BASE_URL = "https://cookm8.vercel.app/api/auth/google";
//    private final RequestQueue requestQueue;
//
//    public AuthApiService(Context context) {
//        requestQueue = Volley.newRequestQueue(context);
//    }
//
//    public interface AuthCallback {
//        void onSuccess(AuthResponse response);
//        void onError(String errorMessage);
//    }
//
//    public void loginWithGoogle(String googleUserId, String email, String name, String avatar, AuthCallback callback) {
//        try {
//            JSONObject body = new JSONObject();
//            body.put("googleUserId", googleUserId);
//            body.put("email", email);
//            body.put("name", name);
//            body.put("avatar", avatar);
//
//            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BASE_URL, body,
//                    response -> {
//                        try {
//                            if (response.has("message") && response.getString("message").contains("successful")) {
//                                AuthResponse authResponse = AuthResponse.fromJson(response);
//                                callback.onSuccess(authResponse);
//                            } else {
//                                callback.onError("Unexpected response");
//                            }
//                        } catch (JSONException e) {
//                            callback.onError("Parse error: " + e.getMessage());
//                        }
//                    },
//                    error -> {
//                        new Handler(Looper.getMainLooper()).post(() -> {
//                            if (error.networkResponse != null) {
//                                int status = error.networkResponse.statusCode;
//                                if (status == 400) {
//                                    callback.onError("Bad Request - Invalid input data");
//                                } else if (status == 500) {
//                                    callback.onError("Internal Server Error");
//                                } else {
//                                    callback.onError("HTTP Error " + status);
//                                }
//                            } else {
//                                callback.onError("Network Failure");
//                            }
//                        });
//                    });
//
//            requestQueue.add(request);
//
//        } catch (JSONException e) {
//            callback.onError("JSON Error: " + e.getMessage());
//        }
//    }
//}
package com.example.cookmate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class AuthApiService {
    private static final String BASE_URL = "https://cookm8.vercel.app/api/auth/google";
    private final RequestQueue requestQueue;

    public AuthApiService(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public interface AuthCallback {
        void onSuccess(AuthResponse response);
        void onError(String errorMessage);
    }

    public void loginWithGoogle(String googleUserId, String email, String name, String avatar, AuthCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("googleUserId", googleUserId);
            body.put("email", email);
            body.put("name", name);
            body.put("avatar", avatar);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BASE_URL, body,
                    response -> {
                        try {
                            if (response.has("message")) {
                                AuthResponse authResponse = AuthResponse.fromJson(response);
                                callback.onSuccess(authResponse);
                            } else {
                                callback.onError("Unexpected response");
                            }
                        } catch (JSONException e) {
                            callback.onError("Parse error: " + e.getMessage());
                        }
                    },
                    error -> new Handler(Looper.getMainLooper()).post(() -> {
                        if (error.networkResponse != null)
                            callback.onError("HTTP Error " + error.networkResponse.statusCode);
                        else
                            callback.onError("Network Failure");
                    })
            );

            requestQueue.add(request);

        } catch (JSONException e) {
            callback.onError("JSON Error: " + e.getMessage());
        }
    }
}
