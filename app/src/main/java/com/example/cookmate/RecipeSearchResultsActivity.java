package com.example.cookmate;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

public class RecipeSearchResultsActivity extends AppCompatActivity {

    private CardView searchCard;
    private EditText searchEditText;
    private ImageView searchIcon;
    private RecyclerView recipeRecyclerView;
    private LinearLayout paginationLayout;
    private ImageView nextPageBtn, prevPageBtn;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList = new ArrayList<>();
    private RequestQueue requestQueue;
    private SessionManager sessionManager;

    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 6;
    private String lastQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_search_results);

        // --- Ánh xạ view ---
        searchCard = findViewById(R.id.searchCard);
        recipeRecyclerView = findViewById(R.id.recipeRecyclerView);
        paginationLayout = findViewById(R.id.paginationLayout);
        requestQueue = Volley.newRequestQueue(this);
        sessionManager = new SessionManager(this);
        searchEditText = findViewById(R.id.editText);
        searchIcon = searchCard.findViewById(R.id.ic_search);

        // --- Thiết lập RecyclerView ---
        recipeAdapter = new RecipeAdapter(this, new ArrayList<>());
        recipeRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recipeRecyclerView.setAdapter(recipeAdapter);

        // --- Sự kiện nhấn Enter để tìm ---
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });

        // --- Khi nhấn vào biểu tượng tìm kiếm ---
        if (searchIcon != null) {
            searchIcon.setOnClickListener(v -> performSearch());
        }

        // --- Gán sự kiện cho phân trang ---
        prevPageBtn = (ImageView) paginationLayout.getChildAt(0);
        nextPageBtn = (ImageView) paginationLayout.getChildAt(paginationLayout.getChildCount() - 1);

        prevPageBtn.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                fetchRecipes(lastQuery, currentPage);
            }
        });

        nextPageBtn.setOnClickListener(v -> {
            currentPage++;
            fetchRecipes(lastQuery, currentPage);
        });
    }

    private void performSearch() {
        Log.d("DEBUG_FLOW", "performSearch() CALLED"); // kiểm tra xem sự kiện có được gọi không
        String query = searchEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(query)) {
            lastQuery = query;
            currentPage = 1;
            hideKeyboard(searchEditText);
            fetchRecipes(query, currentPage);
        } else {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Ẩn bàn phím ---
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // --- Gọi API ---
    private void fetchRecipes(String query, int page) {
        String token = sessionManager.getToken();
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int startIndex = (page - 1) * ITEMS_PER_PAGE;
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = "https://cookm8.vercel.app/api/recipes/search?query=" + encodedQuery +
                    "&number=" + ITEMS_PER_PAGE + "&offset=" + startIndex;

            Log.d("API_REQUEST", "GET: " + url);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET, url, null,
                    response -> {
                        try {
                            if (response.has("recipes")) {
                                Log.d("API_RESPONSE", response.toString()); // kiểm tra dữ liệu từ API
                                JSONArray recipesArray = response.getJSONArray("recipes");
                                recipeList.clear();
                                for (int i = 0; i < recipesArray.length(); i++) {
                                    JSONObject recipeJson = recipesArray.getJSONObject(i);
                                    Recipe recipe = new Gson().fromJson(recipeJson.toString(), Recipe.class);
                                    recipeList.add(recipe);
                                }
                                recipeAdapter.updateData(recipeList);
                                if (recipeList.isEmpty())
                                    Toast.makeText(this, "No recipes found", Toast.LENGTH_SHORT).show();
                            } else if (response.has("error")) {
                                Toast.makeText(this, "Error: " + response.optString("error"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("API_PARSE_ERROR", e.toString());
                        }
                    },
                    error -> {
                        String message = "Network error";
                        if (error.networkResponse != null) {
                            message = "HTTP " + error.networkResponse.statusCode;
                            if (error.networkResponse.data != null)
                                message += ": " + new String(error.networkResponse.data);
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        Log.e("API_ERROR", message);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };

            requestQueue.add(request);

        } catch (Exception e) {
            Toast.makeText(this, "Request error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
