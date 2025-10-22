package com.example.cookmate.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmojiData {

    // Map chứa emoji và tên mô tả
    private static final Map<String, String> emojiMap = new HashMap<>();

    static {
        // 🍞 Food
        emojiMap.put("🍞", "bread");
        emojiMap.put("🥐", "croissant");
        emojiMap.put("🥖", "baguette");
        emojiMap.put("🧀", "cheese");
        emojiMap.put("🍔", "burger");
        emojiMap.put("🍕", "pizza");
        emojiMap.put("🍟", "fries");
        emojiMap.put("🌭", "hotdog");
        emojiMap.put("🍗", "chicken");
        emojiMap.put("🥩", "steak");
        emojiMap.put("🍳", "fried egg");
        emojiMap.put("🥗", "salad");
        emojiMap.put("🍝", "pasta");
        emojiMap.put("🍜", "noodle soup");
        emojiMap.put("🍚", "rice");
        emojiMap.put("🍱", "bento");
        emojiMap.put("🥪", "sandwich");
        emojiMap.put("🍰", "cake");
        emojiMap.put("🧁", "cupcake");
        emojiMap.put("🍩", "donut");
        emojiMap.put("🍪", "cookie");
        emojiMap.put("🍫", "chocolate");
        emojiMap.put("🍎", "apple");
        emojiMap.put("🍊", "orange");
        emojiMap.put("🍉", "watermelon");
        emojiMap.put("🍇", "grapes");
        emojiMap.put("🍌", "banana");
        emojiMap.put("🍓", "strawberry");

        // 🍹 Drink
        emojiMap.put("☕", "coffee");
        emojiMap.put("🍵", "tea");
        emojiMap.put("🧃", "juice");
        emojiMap.put("🥛", "milk");
        emojiMap.put("🍺", "beer");
        emojiMap.put("🍷", "wine");
        emojiMap.put("🍸", "cocktail");
        emojiMap.put("🍹", "tropical drink");
        emojiMap.put("🥤", "soft drink");
        emojiMap.put("🧋", "bubble tea");
    }

    // 🔹 Trả về toàn bộ emoji
    public static List<String> getAll() {
        return new ArrayList<>(emojiMap.keySet());
    }

    // 🔹 Tìm kiếm emoji theo tên (ví dụ "pizza", "fruit", ...)
    public static List<String> search(String query) {
        List<String> result = new ArrayList<>();
        String lower = query.toLowerCase();
        for (Map.Entry<String, String> e : emojiMap.entrySet()) {
            if (e.getValue().toLowerCase().contains(lower)) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    // 🔹 Lọc emoji theo loại (Food, Drink hoặc All)
    public static List<String> filterByCategory(String category) {
        if (category.equalsIgnoreCase("All")) {
            return getAll();
        }

        List<String> filtered = new ArrayList<>();
        for (Map.Entry<String, String> e : emojiMap.entrySet()) {
            String name = e.getValue().toLowerCase();
            if (category.equalsIgnoreCase("Food")) {
                if (!isDrink(name)) filtered.add(e.getKey());
            } else if (category.equalsIgnoreCase("Drink")) {
                if (isDrink(name)) filtered.add(e.getKey());
            }
        }
        return filtered;
    }

    // 🔸 Hàm phụ để phân biệt Drink/Food
    private static boolean isDrink(String name) {
        return name.contains("tea") ||
                name.contains("coffee") ||
                name.contains("beer") ||
                name.contains("wine") ||
                name.contains("drink") ||
                name.contains("milk") ||
                name.contains("juice") ||
                name.contains("cocktail") ||
                name.contains("bubble");
    }
}
