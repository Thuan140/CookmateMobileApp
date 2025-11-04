package com.example.cookmate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class RecoverApiService {
    private static final String BASE_URL = "https://cookm8.vercel.app/api/auth/recover";
    private Context context;

    public RecoverApiService(Context context) {
        this.context = context;
    }

    public interface RecoverCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // 🔹 Gửi OTP
    public void sendOtp(String email, RecoverCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BASE_URL, body,
                    response -> {
                        if (response.has("message")) {
                            callback.onSuccess("OTP sent to email");
                        }
                    },
                    error -> new Handler(Looper.getMainLooper()).post(() -> {
                        if (error.networkResponse != null) {
                            int code = error.networkResponse.statusCode;
                            if (code == 400)
                                callback.onError("Missing required fields");
                            else if (code == 404)
                                callback.onError("Resource not found");
                            else if (code == 500)
                                callback.onError("Something went wrong");
                            else
                                callback.onError("Error " + code);
                        } else {
                            callback.onError("Network error");
                        }
                    })
            );
            Volley.newRequestQueue(context).add(request);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    // 🔹 Xác minh OTP
    public void verifyOtp(String email, String otp, RecoverCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("otp", otp);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BASE_URL + "/verify", body,
                    response -> {
                        if (response.has("message")) {
                            callback.onSuccess("Account recovered successfully");
                        }
                    },
                    error -> new Handler(Looper.getMainLooper()).post(() -> {
                        if (error.networkResponse != null) {
                            int code = error.networkResponse.statusCode;
                            if (code == 400)
                                callback.onError("Missing required fields");
                            else if (code == 404)
                                callback.onError("Resource not found");
                            else if (code == 500)
                                callback.onError("Something went wrong");
                            else
                                callback.onError("Error " + code);
                        } else {
                            callback.onError("Network error");
                        }
                    })
            );
            Volley.newRequestQueue(context).add(request);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
        }
    }
}
