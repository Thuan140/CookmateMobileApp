package com.example.cookmate;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MealPlanActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_plan);

        // chọn đúng item tương ứng với activity này
        NavHelper.setupBottomNav(this, R.id.navigation_mealplan);
    }
}
