package com.example.cookmate;

import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeleteIngredientHelper {
    private static final String BASE_URL = "https://cookm8.vercel.app/api/ingredients";

    public static void deleteIngredientWithOkHttp(String token, String ingredientId, IngredientApiService.DeleteCallback callback) {
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

        Request request = new Request.Builder()
                .url(BASE_URL)
                .delete(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        Log.d("OkHttpDelete", "DELETE " + BASE_URL + " body=" + json.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OkHttpDelete", "onFailure: " + e.getMessage(), e);
                callback.onError("Network failure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body() != null ? response.body().string() : "";
                Log.d("OkHttpDelete", "code=" + response.code() + " body=" + respBody);

                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("HTTP " + response.code() + ": " + respBody);
                }
            }
        });
    }
}
