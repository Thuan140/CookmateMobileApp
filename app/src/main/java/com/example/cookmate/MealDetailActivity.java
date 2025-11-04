//package com.example.cookmate;
//
//import android.os.Bundle;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.bumptech.glide.Glide;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//public class MealDetailActivity extends AppCompatActivity {
//
//    private ImageView ivFood;
//    private TextView tvTitle, tvIngredientList, tvInstructionContent, tvNutritionDetail;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_mealdetail);
//
//        ivFood = findViewById(R.id.ivFood);
//        tvTitle = findViewById(R.id.tvTitle);
//        tvIngredientList = findViewById(R.id.tvIngredientList);
//        tvInstructionContent = findViewById(R.id.tvInstructionContent);
//        tvNutritionDetail = findViewById(R.id.tvNutritionDetail);
//
//        try {
//            String jsonStr = getIntent().getStringExtra("recipeData");
//            if (jsonStr != null) {
//                JSONObject recipe = new JSONObject(jsonStr);
//
//                tvTitle.setText(recipe.optString("title", "No Title"));
//                Glide.with(this).load(recipe.optString("image", "")).into(ivFood);
//
//                // Parse ingredients
//                StringBuilder ingredients = new StringBuilder();
//                JSONArray ingArr = recipe.optJSONArray("extendedIngredients");
//                if (ingArr != null) {
//                    for (int i = 0; i < ingArr.length(); i++) {
//                        JSONObject ing = ingArr.getJSONObject(i);
//                        ingredients.append("• ")
//                                .append(ing.optString("original"))
//                                .append("\n");
//                    }
//                }
//                tvIngredientList.setText(ingredients.toString().trim());
//
//                // Parse instructions (nếu có)
//                String instructions = recipe.optString("instructions", "No instructions provided.");
//                tvInstructionContent.setText(instructions);
//
//                // Nutrition info
//                JSONObject nutrition = recipe.optJSONObject("nutrition");
//                if (nutrition != null) {
//                    JSONArray nutrients = nutrition.optJSONArray("nutrients");
//                    if (nutrients != null) {
//                        StringBuilder sb = new StringBuilder();
//                        for (int i = 0; i < nutrients.length(); i++) {
//                            JSONObject nut = nutrients.getJSONObject(i);
//                            sb.append(nut.optString("name"))
//                                    .append(": ")
//                                    .append(nut.optDouble("amount"))
//                                    .append(" ")
//                                    .append(nut.optString("unit"))
//                                    .append("\n");
//                        }
//                        tvNutritionDetail.setText(sb.toString().trim());
//                    }
//                }
//
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
package com.example.cookmate;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

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

    private ImageView ivFood;
    private TextView tvTitle, tvIngredientList, tvInstructionContent, tvNutritionDetail;
    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mealdetail);

        ivFood = findViewById(R.id.ivFood);
        tvTitle = findViewById(R.id.tvTitle);
        tvIngredientList = findViewById(R.id.tvIngredientList);
        tvInstructionContent = findViewById(R.id.tvInstructionContent);
        tvNutritionDetail = findViewById(R.id.tvNutritionDetail);
        pieChart = findViewById(R.id.pieNutritionChart);

        try {
            String jsonStr = getIntent().getStringExtra("recipeData");
            if (jsonStr != null) {
                JSONObject recipe = new JSONObject(jsonStr);

                tvTitle.setText(recipe.optString("title", "No Title"));
                Glide.with(this).load(recipe.optString("image", "")).into(ivFood);

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
}
