package com.example.cookmate;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileDeleteApiService {

    private static final String DELETE_URL = "https://cookm8.vercel.app/api/profile/delete";
    private final Context context;

    public ProfileDeleteApiService(Context context) {
        this.context = context;
    }

    public void deleteAccount(Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {
        SessionManager session = new SessionManager(context);
        String token = session.getToken();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, DELETE_URL, null,
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
}
