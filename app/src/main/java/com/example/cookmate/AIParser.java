package com.example.cookmate;

import com.example.cookmate.models.FavoriteItem;
import com.example.cookmate.models.MealPlanItem;
import com.example.cookmate.models.ShoppingItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AIParser {

    // Trả về true nếu JSON chứa kết quả mealPlans từ tool
    public static boolean isMealPlanToolResult(JSONObject obj) {
        return obj.has("mealPlans"); // ĐÚNG theo API thật
    }

    public static boolean isUserProfile(JSONObject obj) {
        return obj.has("user");
    }

    public static User parseUserProfile(JSONObject obj) {
        try {
            if (!obj.has("user")) return null;

            JSONObject u = obj.getJSONObject("user");

            String id = u.optString("_id");
            String email = u.optString("email");
            String name = u.optString("name");
            String avatar = u.optString("avatar");

            List<String> prefs = new ArrayList<>();
            JSONArray prefArr = u.optJSONArray("dietaryPreferences");

            if (prefArr != null) {
                for (int i = 0; i < prefArr.length(); i++) {
                    prefs.add(prefArr.getString(i));
                }
            }

            return new User(id, email, name, avatar, prefs);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isShoppingList(JSONObject obj) {
        return obj.has("shoppingList");
    }

    public static List<ShoppingItem> parseShoppingList(JSONObject obj) {
        List<ShoppingItem> list = new ArrayList<>();

        try {
            if (!obj.has("shoppingList")) return list;

            JSONArray arr = obj.getJSONArray("shoppingList");

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                ShoppingItem item = new ShoppingItem();

                item.setId(o.optString("_id"));
                item.setUserId(o.optString("userId"));
                item.setName(o.optString("name"));
                item.setStatus(o.optString("status"));
                item.setNotes(o.optString("notes"));
                item.setCreatedAt(o.optString("createdAt"));
                item.setUpdatedAt(o.optString("updatedAt"));
                item.set__v(o.optInt("__v"));

                list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // Parse danh sách mealPlanItems từ JSON tool output
    public static List<MealPlanItem> parseMealPlanItems(JSONObject obj) {

        List<MealPlanItem> list = new ArrayList<>();

        try {
            if (!obj.has("mealPlans")) return list;

            JSONArray arr = obj.getJSONArray("mealPlans");

            for (int i = 0; i < arr.length(); i++) {

                JSONObject o = arr.getJSONObject(i);

                String id = o.optString("_id");
                String name = o.optString("name");
                String notes = o.optString("notes");
                String date = o.optString("date");

                List<String> recipeIds = new ArrayList<>();
                JSONArray recipeArray = o.optJSONArray("recipeIds");

                if (recipeArray != null) {
                    for (int j = 0; j < recipeArray.length(); j++) {
                        recipeIds.add(recipeArray.getString(j));
                    }
                }

                list.add(new MealPlanItem(id, name, recipeIds, notes, date));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static List<FavoriteItem> parseFavorites(JSONObject obj) {

        List<FavoriteItem> list = new ArrayList<>();

        try {
            if (!obj.has("recipes")) return list;

            JSONArray arr = obj.getJSONArray("recipes");

            for (int i = 0; i < arr.length(); i++) {

                JSONObject r = arr.getJSONObject(i);

                String id = r.optString("id");
                String image = r.optString("image");
                String title = r.optString("title");

                Integer ready = r.has("readyInMinutes") ? r.getInt("readyInMinutes") : null;
                Integer servings = r.has("servings") ? r.getInt("servings") : null;

                list.add(new FavoriteItem(id, image, title, ready, servings));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

}