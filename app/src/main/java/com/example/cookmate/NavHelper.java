package com.example.cookmate;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.IdRes;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavHelper {

    /**
     * Gọi trong onCreate của mỗi Activity để setup bottom nav.
     *
     * @param activity current activity
     * @param currentItemId menu id (R.id.navigation_home / navigation_pantry / ...)
     */
    public static void setupBottomNav(Activity activity, @IdRes int currentItemId) {
        BottomNavigationView navView = activity.findViewById(R.id.nav_view);
        if (navView == null) {
            Log.w("NavHelper", "nav_view not found in layout for " + activity.getClass().getSimpleName());
            return;
        }

        // mark current item selected
        navView.setSelectedItemId(currentItemId);

        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // nếu bấm item đang active => không làm gì (có thể scroll to top nếu muốn)
            if (id == currentItemId) {
                return true;
            }

            Intent i = null;
            if (id == R.id.navigation_home) {
                i = new Intent(activity, HomeActivity.class);
            } else if (id == R.id.navigation_pantry) {
                i = new Intent(activity, PantryActivity.class);
            } else if (id == R.id.navigation_recipe_search_results) {
                i = new Intent(activity, RecipeSearchResultsActivity.class);
            } else if (id == R.id.navigation_mealplan) {
                i = new Intent(activity, MealPlanActivity.class);
            } else if (id == R.id.navigation_profile) {
                i = new Intent(activity, ProfileActivity.class);
            } else if (id == R.id.navigation_suggestion) {
                i = new Intent(activity, SuggestionActivity.class);
            }



            if (i != null) {
                // nếu Activity đã tồn tại, đưa lên trước thay vì tạo mới
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(i);
            }
            return true;
        });
    }
}
