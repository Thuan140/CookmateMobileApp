package com.example.cookmate;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileApiService {

    private static final String PROFILE_URL = "https://cookm8.vercel.app/api/profile";
    private Context context;

    public ProfileApiService(Context context) {
        this.context = context;
    }

    // 🔹 GET profile
    public void getProfile(Response.Listener<JSONObject> listener,
                           Response.ErrorListener errorListener) {
        SessionManager session = new SessionManager(context);
        String token = session.getToken();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, PROFILE_URL, null,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }

    // 🔹 PUT update profile
    public void updateProfile(JSONObject body,
                              Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {
        SessionManager session = new SessionManager(context);
        String token = session.getToken();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, PROFILE_URL, body,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }
}
