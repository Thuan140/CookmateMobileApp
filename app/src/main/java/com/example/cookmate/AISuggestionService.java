package com.example.cookmate;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.cookmate.models.FavoriteItem;
import com.example.cookmate.models.MealPlanItem;
import com.example.cookmate.models.ShoppingItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AISuggestionService {

    private static final String API_URL = "https://cookm8.vercel.app/api/ai";
    private static final String TAG = "AISuggestionService";
    private final Context context;
    private final SessionManager session;

    // Callback tách riêng TEXT vs MEALPLANS
    public interface AICallback {
        void onText(String text);
        void onMealPlans(List<MealPlanItem> mealPlans);
        void onUserProfile(User user);
        void onError(String error);
        void onShoppingList(List<ShoppingItem> list);
        void onFavorites(List<FavoriteItem> list);

    }

    public AISuggestionService(Context context) {
        this.context = context;
        this.session = new SessionManager(context);
    }

    public void sendChat(List<ChatMessage> messages, AICallback callback) {
        try {
            JSONObject body = new JSONObject();
            JSONArray msgArray = new JSONArray();

            for (ChatMessage msg : messages) {
                JSONObject obj = new JSONObject();
                obj.put("role", msg.getRole());
                obj.put("content", msg.getContent());
                msgArray.put(obj);
            }

            body.put("messages", msgArray);
            Log.d(TAG, "SEND_BODY: " + body);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL,
                    body,
                    response -> handleAIResponse(response, callback),
                    error -> {
                        Log.e(TAG, "API Error: " + error);
                        callback.onError("Không thể kết nối tới máy chủ: " + error);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    String token = session.getToken();
                    if (token != null && !token.isEmpty()) {
                        headers.put("Authorization", "Bearer " + token);
                    }
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    20000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            Volley.newRequestQueue(context).add(request);

        } catch (Exception e) {
            Log.e(TAG, "Body creation error", e);
            callback.onError("Lỗi tạo body: " + e.getMessage());
        }
    }

    private void handleAIResponse(JSONObject response, AICallback callback) {
        try {
            Log.d(TAG, "AI_RESPONSE: " + response);

            //--------------------------------------------------------
            // CASE 1: MODEL TRẢ VỀ TOOL-RESULT
            //--------------------------------------------------------
            if (response.has("message")) {
                JSONObject msg = response.getJSONObject("message");

                if (msg.has("toolInvocations")) {

                    JSONObject toolObj = msg.getJSONObject("toolInvocations");
                    JSONArray resultArray = toolObj.optJSONArray("result");

                    if (resultArray != null && resultArray.length() > 0) {

                        JSONObject tool = resultArray.getJSONObject(0);
                        JSONObject output = tool.optJSONObject("output");

                        if (output != null) {

                            // --- CASE: get_profile ---
                            if (output.has("user")) {
                                User user = AIParser.parseUserProfile(output);
                                if (user != null) {
                                    callback.onUserProfile(user);
                                    return;
                                }
                            }

                            // --- CASE: mealPlans ---
                            if (output.has("mealPlans")) {
                                List<MealPlanItem> list = AIParser.parseMealPlanItems(output);
                                callback.onMealPlans(list);
                                return;
                            }
                            // --- CASE: get_shopping_list ---
                            if (output.has("shoppingList")) {
                                List<ShoppingItem> list = AIParser.parseShoppingList(output);
                                callback.onShoppingList(list);
                                return;
                            }
                            // --- CASE: get_favorite_list ---
                            if (output.has("recipes")) {
                                List<FavoriteItem> list = AIParser.parseFavorites(output);
                                callback.onFavorites(list);
                                return;
                            }

                        }

                    }
                }

                // CASE 2: TEXT message
                String content = msg.optString("content", "");
                if (!content.isEmpty()) {
                    callback.onText(content);
                    return;
                }
            }

            //--------------------------------------------------------
            // CASE 3: GPT-4o-mini chuẩn
            //--------------------------------------------------------
            if (response.has("choices")) {
                JSONArray choices = response.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
                    String content = msg.optString("content", "");

                    callback.onText(content);
                    return;
                }
            }

            callback.onError("Không có phản hồi hợp lệ từ AI.");

        } catch (Exception e) {
            Log.e(TAG, "Parse error", e);
            callback.onError("Lỗi parse JSON: " + e.getMessage());
        }
    }
}
