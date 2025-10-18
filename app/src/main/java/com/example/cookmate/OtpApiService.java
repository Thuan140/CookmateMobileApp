package com.example.cookmate;

import android.os.AsyncTask;
import org.json.JSONObject;

public class OtpApiService {

    public interface OtpCallback {
        void onResponse(JSONObject response);
    }

    private static final String SEND_OTP_URL = "https://cookm8.vercel.app/api/auth/otp";
    private static final String VERIFY_OTP_URL = "https://cookm8.vercel.app/api/auth/otp/verify";

    public static void sendOtp(String email, OtpCallback callback) {
        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {
                try {
                    JSONObject body = new JSONObject();
                    body.put("email", email);
                    return ApiService.postJson(SEND_OTP_URL, body);
                } catch (Exception e) {
                    JSONObject err = new JSONObject();
                    try { err.put("error", e.getMessage()); } catch (Exception ignored) {}
                    return err;
                }
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                callback.onResponse(result);
            }
        }.execute();
    }

    public static void verifyOtp(String email, String otp, OtpCallback callback) {
        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {
                try {
                    JSONObject body = new JSONObject();
                    body.put("email", email);
                    body.put("otp", otp);
                    return ApiService.postJson(VERIFY_OTP_URL, body);
                } catch (Exception e) {
                    JSONObject err = new JSONObject();
                    try { err.put("error", e.getMessage()); } catch (Exception ignored) {}
                    return err;
                }
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                callback.onResponse(result);
            }
        }.execute();
    }
}
