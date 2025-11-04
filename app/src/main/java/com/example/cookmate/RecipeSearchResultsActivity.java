//package com.example.cookmate;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.view.KeyEvent;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class RecipeSearchResultsActivity extends AppCompatActivity {
//
//    private static final String TAG = "RecipeSearchActivity";
//    private EditText editText;
//    private ImageView searchIcon;
//    private RecyclerView recyclerView;
//    private RecipeAdapter adapter;
//    private List<Recipe> recipeList;
//    private SearchApiService searchService;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_recipe_search_results);
//        NavHelper.setupBottomNav(this, R.id.navigation_recipe_search_results);
//
//        editText = findViewById(R.id.editText);
//        searchIcon = findViewById(R.id.ic_search);
//        recyclerView = findViewById(R.id.recipeRecyclerView);
//
//        recipeList = new ArrayList<>();
//        adapter = new RecipeAdapter(this, recipeList);
//        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
//        recyclerView.setAdapter(adapter);
//
//        searchService = new SearchApiService(this);
//
//        editText.setOnKeyListener((v, keyCode, event) -> {
//            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
//                performSearch();
//                return true;
//            }
//            return false;
//        });
//
//        searchIcon.setOnClickListener(v -> performSearch());
//    }
//
//    private void performSearch() {
//        String query = editText.getText().toString().trim();
//        if (query.isEmpty()) {
//            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
//            Log.w(TAG, " Search aborted: empty query");
//            return;
//        }
//
//        Toast.makeText(this, "Searching for " + query + "...", Toast.LENGTH_SHORT).show();
//        Log.d(TAG, " Performing search for query: " + query);
//
//        searchService.searchRecipes(query, new SearchApiService.SearchCallback() {
//            @Override
//            public void onSuccess(List<Recipe> recipes) {
//                if (recipes.isEmpty()) {
//                    Toast.makeText(RecipeSearchResultsActivity.this, " Không tìm thấy công thức nào", Toast.LENGTH_LONG).show();
//                } else {
//                    recipeList.clear();
//                    recipeList.addAll(recipes);
//                    adapter.notifyDataSetChanged();
//                    Toast.makeText(RecipeSearchResultsActivity.this, " Tìm thấy " + recipes.size() + " công thức", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onError(String message) {
//                Toast.makeText(RecipeSearchResultsActivity.this, " Lỗi: " + message, Toast.LENGTH_LONG).show();
//                Log.e("RecipeSearchActivity", " Search failed: " + message);
//            }
//        });
//
//    }
//}
package com.example.cookmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RecipeSearchResultsActivity extends AppCompatActivity {

    private static final String TAG = "RecipeSearchActivity";
    private EditText editText;
    private ImageView searchIcon;
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<Recipe> recipeList;
    private SearchApiService searchService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_search_results);
        NavHelper.setupBottomNav(this, R.id.navigation_recipe_search_results);

        // Ánh xạ view
        editText = findViewById(R.id.editText);
        searchIcon = findViewById(R.id.ic_search);
        recyclerView = findViewById(R.id.recipeRecyclerView);

        // Khởi tạo danh sách và adapter
        recipeList = new ArrayList<>();
        adapter = new RecipeAdapter(this, recipeList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        searchService = new SearchApiService(this);

        // Khi người dùng nhấn Enter trong ô tìm kiếm
        editText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                performSearch();
                return true;
            }
            return false;
        });

        // Khi nhấn icon tìm kiếm
        searchIcon.setOnClickListener(v -> performSearch());

        // 🟢 Xử lý khi nhấn vào 1 món trong danh sách
        adapter.setOnItemClickListener(recipe -> {
            int recipeId = recipe.getId();
            Log.d(TAG, "Clicked recipe id = " + recipeId);

            RecipeDetailApiService detailService = new RecipeDetailApiService(this);
            detailService.fetchRecipeDetail(recipeId, new RecipeDetailApiService.RecipeDetailCallback() {
                @Override
                public void onSuccess(JSONObject recipeJson) {
                    try {
                        Intent intent = new Intent(RecipeSearchResultsActivity.this, MealDetailActivity.class);
                        intent.putExtra("recipeData", recipeJson.toString());
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting MealDetailActivity: " + e.getMessage());
                        Toast.makeText(RecipeSearchResultsActivity.this, "Lỗi khi mở chi tiết món ăn", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(RecipeSearchResultsActivity.this, "Lỗi tải chi tiết: " + message, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Fetch detail failed: " + message);
                }
            });
        });
    }

    // 🔹 Giữ nguyên logic tìm kiếm cũ
    private void performSearch() {
        String input = editText.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Search aborted: empty query");
            return;
        }

        Log.d(TAG, "Raw input: " + input);

        // Giá trị mặc định
        String query = "";
        Integer limit = 10;
        String cuisine = "";
        String diet = "";
        Integer maxReadyTime = null;

        // Tách các từ
        String[] tokens = input.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i].toLowerCase();

            if (word.matches("\\d+") && i > 0) {
                try {
                    limit = Integer.parseInt(word);
                    continue;
                } catch (Exception ignored) {}
            }

            if (word.matches("time\\d+")) {
                maxReadyTime = Integer.parseInt(word.replace("time", ""));
                continue;
            }

            if (word.equals("cook") && i + 2 < tokens.length && tokens[i + 1].equals("in")) {
                try {
                    int minutes = Integer.parseInt(tokens[i + 2].replaceAll("\\D+", ""));
                    maxReadyTime = minutes;
                    i += 2;
                    continue;
                } catch (Exception ignored) {}
            }

            if (word.contains("vegetarian")) { diet = "vegetarian"; continue; }
            if (word.contains("vegan")) { diet = "vegan"; continue; }
            if (word.contains("keto")) { diet = "ketogenic"; continue; }
            if (word.contains("paleo")) { diet = "paleo"; continue; }

            if (word.contains("italian")) { cuisine = "italian"; continue; }
            if (word.contains("asian")) { cuisine = "asian"; continue; }
            if (word.contains("thai")) { cuisine = "thai"; continue; }
            if (word.contains("french")) { cuisine = "french"; continue; }
            if (word.contains("japanese")) { cuisine = "japanese"; continue; }

            if (word.matches("for|in|of|and|a|an|the|cook")) {
                continue;
            }

            query += (query.isEmpty() ? word : " " + word);
        }

        if (query.isEmpty()) {
            Toast.makeText(this, "Không xác định được món cần tìm", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, String.format("Parsed -> query=%s, limit=%d, cuisine=%s, diet=%s, maxReadyTime=%s",
                query, limit, cuisine, diet, maxReadyTime));

        Toast.makeText(this, "Searching for: " + query, Toast.LENGTH_SHORT).show();

        searchService.searchRecipes(
                query,
                limit,
                cuisine,
                diet,
                maxReadyTime,
                new SearchApiService.SearchCallback() {
                    @Override
                    public void onSuccess(List<Recipe> recipes) {
                        if (recipes.isEmpty()) {
                            Toast.makeText(RecipeSearchResultsActivity.this,
                                    "Không tìm thấy công thức nào",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            recipeList.clear();
                            recipeList.addAll(recipes);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(RecipeSearchResultsActivity.this,
                                    "Tìm thấy " + recipes.size() + " công thức",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(RecipeSearchResultsActivity.this,
                                "Lỗi: " + message,
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Search failed: " + message);
                    }
                }
        );
    }
}


