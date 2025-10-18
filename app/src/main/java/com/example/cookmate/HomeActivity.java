//package com.example.cookmate;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.Handler;
//import android.view.View;
//
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.viewpager2.widget.CompositePageTransformer;
//import androidx.viewpager2.widget.MarginPageTransformer;
//import androidx.viewpager2.widget.ViewPager2;
//
//import com.google.android.material.tabs.TabLayout;
//import com.google.android.material.tabs.TabLayoutMediator;
//
//import java.util.Arrays;
//import java.util.List;
//
//public class HomeActivity extends AppCompatActivity {
//    private ViewPager2 viewPager;
//    private CarouselAdapter adapter;
//    private Handler autoScrollHandler = new Handler();
//    private Runnable autoScrollRunnable;
//    private final long AUTO_SCROLL_DELAY = 3000; // 3s
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_home);
//        NavHelper.setupBottomNav(this, R.id.navigation_home);
//        viewPager = findViewById(R.id.viewPager);
//        TabLayout tabLayout = findViewById(R.id.tabLayout);
//
//        // sample images (drawable ids)
//        List<Integer> images = Arrays.asList(
//                R.drawable.food,
//                R.drawable.food,
//                R.drawable.food,
//                R.drawable.food
//        );
//
//        adapter = new CarouselAdapter(this, images);
//        viewPager.setAdapter(adapter);
//
//        // show part of next/prev item
//        viewPager.setOffscreenPageLimit(3);
//        // disable clipping so you can see neighboring cards
//        viewPager.setClipToPadding(false);
//        viewPager.setClipChildren(false);
//
//        // PageTransformer: margin + scale + alpha
//        CompositePageTransformer transformer = new CompositePageTransformer();
//        transformer.addTransformer(new MarginPageTransformer(24)); // space between pages
//
//        transformer.addTransformer((page, position) -> {
//            float r = 1 - Math.abs(position);
//            // scale between 0.85 and 1
//            page.setScaleY(0.85f + r * 0.15f);
//            page.setAlpha(0.6f + r * 0.4f);
//        });
//        viewPager.setPageTransformer(transformer);
//
//        // Optional: indicator using TabLayoutMediator
//        new TabLayoutMediator(tabLayout, viewPager,
//                (tab, pos) -> { /* tabs are just indicators, no text */ }
//        ).attach();
//
//        // set custom view for each tab so it stays a fixed small dot
//        for (int i = 0; i < tabLayout.getTabCount(); i++) {
//            TabLayout.Tab t = tabLayout.getTabAt(i);
//            if (t != null) {
//                t.setCustomView(R.layout.custom_tab);
//            }
//        }
//
//        // Auto-scroll runnable
//        autoScrollRunnable = new Runnable() {
//            @Override
//            public void run() {
//                int itemCount = adapter.getItemCount();
//                if (itemCount == 0) return;
//                int next = (viewPager.getCurrentItem() + 1) % itemCount;
//                viewPager.setCurrentItem(next, true);
//                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY);
//            }
//        };
//
//        // start auto-scroll
//        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
//
//        // stop/reset auto-scroll on user interaction & update tab selection visuals
//        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
//            @Override
//            public void onPageSelected(int position) {
//                // reset timer so it waits full delay after manual swipe
//                autoScrollHandler.removeCallbacks(autoScrollRunnable);
//                autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
//
//                // update selected state on custom views
//                for (int i = 0; i < tabLayout.getTabCount(); i++) {
//                    TabLayout.Tab tab = tabLayout.getTabAt(i);
//                    if (tab != null && tab.getCustomView() != null) {
//                        tab.getCustomView().setSelected(i == position);
//                    }
//                }
//            }
//        });
//
//        // ensure initial selected state (in case default page is not 0)
//        int initial = viewPager.getCurrentItem();
//        for (int i = 0; i < tabLayout.getTabCount(); i++) {
//            TabLayout.Tab t = tabLayout.getTabAt(i);
//            if (t != null && t.getCustomView() != null) {
//                t.getCustomView().setSelected(i == initial);
//            }
//        }
//
//        // ===========================
//        // <-- Phần thêm: mở Activity -->
//        // ===========================
//        View btnHub = findViewById(R.id.btn_Hub);     // Hub
//        View btnList = findViewById(R.id.btn_list);    // ♥ List
//        View btnPantry = findViewById(R.id.btn_Pantry);  // Pantry
//
//        // Mở HubActivity khi bấm Hub
//        btnHub.setOnClickListener(v -> {
//            Intent i = new Intent(HomeActivity.this, HubActivity.class);
//            startActivity(i);
//        });
//
//        // Mở FavoritesActivity khi bấm ♥ List
//        btnList.setOnClickListener(v -> {
//            Intent i = new Intent(HomeActivity.this, FavoritesActivity.class);
//            startActivity(i);
//        });
//
//        // Mở PantryActivity khi bấm Pantry
//        btnPantry.setOnClickListener(v -> {
//            Intent i = new Intent(HomeActivity.this, PantryActivity.class);
//            startActivity(i);
//        });
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        autoScrollHandler.removeCallbacks(autoScrollRunnable);
//    }
//}
package com.example.cookmate;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private RecipePagerAdapter adapter;
    private Handler autoScrollHandler = new Handler();
    private Runnable autoScrollRunnable;
    private int currentPage = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // chọn đúng item tương ứng với activity này
        NavHelper.setupBottomNav(this, R.id.navigation_home);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        SessionManager session = new SessionManager(this);
        String token = session.getToken();

        if (token == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        RecipeApiService apiService = new RecipeApiService(this);
        apiService.getTodayRecipes(token, new RecipeApiService.RecipeCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                adapter = new RecipePagerAdapter(HomeActivity.this, recipes);
                viewPager.setAdapter(adapter);
                new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

                startAutoScroll(recipes.size());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startAutoScroll(int size) {
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (adapter != null && size > 0) {
                    currentPage = (currentPage + 1) % size;
                    viewPager.setCurrentItem(currentPage, true);
                    autoScrollHandler.postDelayed(this, 8000); // 8 giây
                }
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 8000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }
}
