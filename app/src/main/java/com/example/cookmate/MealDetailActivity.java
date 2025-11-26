package com.example.cookmate;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MealDetailActivity extends AppCompatActivity {

    private ImageView ivFood, ivBack, ivFavorite;
    private TextView tvTitle, tvIngredientList, tvInstructionContent, tvNutritionDetail;
    private PieChart pieChart;

    private FavoriteApiService favoriteService;
    private SessionManager sessionManager;
    private String token;

    private String recipeId;
    private boolean isFavorite = false;   // flag trạng thái

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mealdetail);

        // Init session
        sessionManager = new SessionManager(this);
        token = sessionManager.getToken();

        // Init views
        ivFood = findViewById(R.id.ivFood);
        tvTitle = findViewById(R.id.tvTitle);
        tvIngredientList = findViewById(R.id.tvIngredientList);
        tvInstructionContent = findViewById(R.id.tvInstructionContent);
        tvNutritionDetail = findViewById(R.id.tvNutritionDetail);
        pieChart = findViewById(R.id.pieNutritionChart);
        ivBack = findViewById(R.id.ivBack);
        ivFavorite = findViewById(R.id.ivFavorite);

        favoriteService = new FavoriteApiService(this);

        loadRecipeData();
        checkFavoriteStatus();   // 🔥 kiểm tra GET Favorites

        ivFavorite.setOnClickListener(v -> {
            if (!isFavorite) {
                addFavorite();
            }
        });

        ivBack.setOnClickListener(v -> finish());
    }

    // ====================== LOAD DATA NHƯ CŨ ======================
    private void loadRecipeData() {
        try {
            String jsonStr = getIntent().getStringExtra("recipeData");
            if (jsonStr != null) {
                JSONObject recipe = new JSONObject(jsonStr);
                recipeId = recipe.optString("id");   // 🔥 lấy ID món ăn

                tvTitle.setText(recipe.optString("title"));
                Glide.with(this).load(recipe.optString("image")).into(ivFood);

                // Ingredients
                StringBuilder ingredients = new StringBuilder();
                JSONArray ingArr = recipe.optJSONArray("extendedIngredients");
                if (ingArr != null) {
                    for (int i = 0; i < ingArr.length(); i++) {
                        JSONObject ing = ingArr.getJSONObject(i);
                        ingredients.append("• ")
                                .append(ing.optString("original"))
                                .append("\n");
                    }
                }
                tvIngredientList.setText(ingredients.toString().trim());

                // Instructions
                tvInstructionContent.setText(recipe.optString("instructions", "No instructions provided."));

                // Nutrition
                JSONObject nutrition = recipe.optJSONObject("nutrition");
                if (nutrition != null) {
                    JSONArray nutrients = nutrition.optJSONArray("nutrients");
                    if (nutrients != null) {
                        List<PieEntry> entries = new ArrayList<>();
                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; i < nutrients.length(); i++) {
                            JSONObject nut = nutrients.getJSONObject(i);
                            String name = nut.optString("name");
                            double amount = nut.optDouble("amount");
                            String unit = nut.optString("unit");

                            // chỉ hiển thị 3 nhóm chính
                            if (name.equalsIgnoreCase("Fat") ||
                                    name.equalsIgnoreCase("Protein") ||
                                    name.equalsIgnoreCase("Carbohydrates")) {
                                entries.add(new PieEntry((float) amount, name));
                            }

                            sb.append(name)
                                    .append(": ")
                                    .append(amount)
                                    .append(" ")
                                    .append(unit)
                                    .append("\n");
                        }

                        tvNutritionDetail.setText(sb.toString().trim());

                        // Cấu hình PieChart
                        PieDataSet dataSet = new PieDataSet(entries, "");
                        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                        dataSet.setValueTextSize(14f);
                        PieData pieData = new PieData(dataSet);

                        pieChart.setData(pieData);
                        pieChart.setUsePercentValues(true);
                        pieChart.getDescription().setEnabled(false);
                        pieChart.getLegend().setEnabled(false);
                        pieChart.invalidate(); // refresh
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================== GET /favorites ======================
    private void checkFavoriteStatus() {

        favoriteService.getFavorites(token, new FavoriteApiService.FavoriteCallback() {
            @Override
            public void onSuccess(JSONArray favorites) {
                if (favorites != null) {
                    for (int i = 0; i < favorites.length(); i++) {
                        JSONObject item = favorites.optJSONObject(i);
                        if (item != null && recipeId.equals(item.optString("id"))) {

                            isFavorite = true;
                            ivFavorite.setImageResource(R.drawable.ic_heart_filled);  // 🔥 đổi icon
                            return;
                        }
                    }
                }
                isFavorite = false;
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MealDetailActivity.this, "Favorite load error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ====================== POST /favorites ======================
    private void addFavorite() {

        favoriteService.addFavorite(token, recipeId, new FavoriteApiService.FavoriteAddCallback() {

            @Override
            public void onSuccess(String message) {
                isFavorite = true;
                ivFavorite.setImageResource(R.drawable.ic_heart_filled);
                Toast.makeText(MealDetailActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MealDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
