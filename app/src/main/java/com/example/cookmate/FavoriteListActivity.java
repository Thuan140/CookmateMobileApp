package com.example.cookmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.adapters.FavoriteAdapter;
import com.example.cookmate.models.FavoriteItem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavoriteListActivity extends AppCompatActivity implements FavoriteAdapter.Callbacks {
    private static final String TAG = "FavoriteList";
    private static final String BASE_URL = "https://cookm8.vercel.app/api/favorites";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // pagination page size
    private static final int ITEMS_PER_PAGE = 4;

    private SessionManager sessionManager;
    private String authToken;

    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();

    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private ImageView backBtn;

    // data lists
    private final List<FavoriteItem> allItems = new ArrayList<>();      // all items from server
    private final List<FavoriteItem> filteredList = new ArrayList<>(); // after search + sort
    private int currentPage = 1;
    private int totalPages = 1;

    // UI controls
    private LinearLayout paginationLayout;
    private EditText searchInput;
    private TextView sortTextView; // textView7 in your xml
    private ImageView sortToggle;
    private boolean sortAscending = true; // true = tăng (asc), false = giảm (desc)
    // current sort: "name", "time", "servings"
    private String currentSort = "name";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoritelist);

        recyclerView = findViewById(R.id.favoritesRecyclerView);
        backBtn = findViewById(R.id.imageView2);
        paginationLayout = findViewById(R.id.paginationLayout);
        searchInput = findViewById(R.id.search_input);
        sortTextView = findViewById(R.id.textView7); // "Newest" in your xml

        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                onBackPressed(); // trở về màn trước
                overridePendingTransition(0, 0); // không hiệu ứng chuyển
            });
        }


        // make sortTextView clickable to toggle sort
        if (sortTextView != null) {
            sortTextView.setOnClickListener(v -> {
                toggleSort();
            });
            // show initial label
            updateSortLabel();
            // find sort toggle image and set click to flip ascending/descending
            sortToggle = findViewById(R.id.sort_toggle);
            if (sortToggle != null) {
                // cập nhật hình ban đầu theo sortAscending
                sortToggle.setRotation(sortAscending ? 0f : 180f);

                sortToggle.setOnClickListener(v -> {
                    // đổi chiều sắp xếp (tăng/giam)
                    sortAscending = !sortAscending;
                    // rotate icon để biểu thị hướng
                    sortToggle.animate().rotation(sortAscending ? 0f : 180f).setDuration(180).start();
                    // apply lại bộ lọc & sắp xếp
                    applyFiltersAndShow();
                });
            }
        }

        sessionManager = new SessionManager(this);
        authToken = sessionManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        adapter = new FavoriteAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // search input realtime
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFiltersAndShow();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // initial load
        fetchFavorites();

    }

    private void toggleSort() {
        // rotate through sorts: name -> time -> servings -> name
        if ("name".equals(currentSort)) currentSort = "time";
        else if ("time".equals(currentSort)) currentSort = "servings";
        else currentSort = "name";
        updateSortLabel();
        applyFiltersAndShow();
    }

    private void updateSortLabel() {
        if (sortTextView == null) return;
        switch (currentSort) {
            case "time": sortTextView.setText("Time"); break;
            case "servings": sortTextView.setText("Servings"); break;
            default: sortTextView.setText("Name"); break;
        }

        // cập nhật biểu tượng hướng
        if (sortToggle != null) {
            // rotation 0 = tăng (mũi tên hướng lên), 180 = giảm (hướng xuống) - tuỳ icon của bạn
            sortToggle.setRotation(sortAscending ? 0f : 180f);
        }
    }


    // -----------------------------------------
    private void fetchFavorites() {
        Request req = new Request.Builder()
                .url(BASE_URL)
                .get()
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.w(TAG, "GET favorites failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(FavoriteListActivity.this, "Không thể tải favorites: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401) {
                    runOnUiThread(() -> Toast.makeText(FavoriteListActivity.this, "Phiên đăng nhập hết hạn", Toast.LENGTH_LONG).show());
                    return;
                }
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(FavoriteListActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }
                String body = response.body().string();
                parseFavoritesResponse(body);
            }
        });
    }

    private void parseFavoritesResponse(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            allItems.clear();

            if (root != null && root.has("recipes") && root.get("recipes").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("recipes")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject r = el.getAsJsonObject();

                    String id = r.has("id") ? r.get("id").getAsString() : "";
                    String image = r.has("image") && !r.get("image").isJsonNull() ? r.get("image").getAsString() : "";
                    String title = r.has("title") && !r.get("title").isJsonNull() ? r.get("title").getAsString() : "";
                    Integer ready = r.has("readyInMinutes") && !r.get("readyInMinutes").isJsonNull() ? r.get("readyInMinutes").getAsInt() : null;
                    Integer serves = r.has("servings") && !r.get("servings").isJsonNull() ? r.get("servings").getAsInt() : null;

                    FavoriteItem it = new FavoriteItem(id, image, title, ready, serves);
                    allItems.add(it);
                }
            }

            runOnUiThread(() -> {
                currentPage = 1;
                applyFiltersAndShow();
//                String scrollId = getIntent().getStringExtra("scrollToId");
//
//                if (scrollId != null) {
//
//                    recyclerView.post(() -> {
//
//                        for (int i = 0; i < allItems.size(); i++) {
//
//                            if (allItems.get(i).getId().equals(scrollId)) {
//                                recyclerView.scrollToPosition(i);
//                                break;
//                            }
//                        }
//                    });
//                }
                String scrollId = getIntent().getStringExtra("scrollToId");

                if (scrollId != null) {

                    recyclerView.post(() -> {

                        for (int i = 0; i < filteredList.size(); i++) {

                            if (filteredList.get(i).getId().equals(scrollId)) {

                                // Tìm page chứa item
                                int page = (i / ITEMS_PER_PAGE) + 1;

                                // Chuyển sang đúng trang
                                showPage(page);

                                // Vị trí trong adapter sau khi phân trang
                                int indexInPage = i % ITEMS_PER_PAGE;

                                recyclerView.post(() ->
                                        recyclerView.scrollToPosition(indexInPage)
                                );

                                break;
                            }
                        }
                    });
                }
                if (allItems.isEmpty()) {
                    Toast.makeText(FavoriteListActivity.this, "Không có mục yêu thích", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ex) {
            Log.e(TAG, "Parse favorites error: " + ex.getMessage(), ex);
            runOnUiThread(() -> Toast.makeText(FavoriteListActivity.this, "Lỗi phân tích dữ liệu", Toast.LENGTH_SHORT).show());
        }
    }

    // ========== Filter / Sort / Pagination ==========
    private void applyFiltersAndShow() {
        // 1) filter by search
        String q = (searchInput != null) ? searchInput.getText().toString().trim().toLowerCase() : "";
        filteredList.clear();
        for (FavoriteItem it : allItems) {
            if (q.isEmpty()) {
                filteredList.add(it);
            } else {
                boolean match = false;
                if (it.getTitle() != null && it.getTitle().toLowerCase().contains(q)) match = true;
                // try matching other fields optionally
                if (!match && it.getId() != null && it.getId().toLowerCase().contains(q)) match = true;
                if (match) filteredList.add(it);
            }
        }

        // 2) sort
        Comparator<FavoriteItem> comparator;
        if ("time".equals(currentSort)) {
            comparator = (a, b) -> {
                Integer ra = a.getReadyInMinutes() != null ? a.getReadyInMinutes() : Integer.MAX_VALUE;
                Integer rb = b.getReadyInMinutes() != null ? b.getReadyInMinutes() : Integer.MAX_VALUE;
                return Integer.compare(ra, rb); // nhỏ trước (asc)
            };
        } else if ("servings".equals(currentSort)) {
            comparator = (a, b) -> {
                Integer sa = a.getServings() != null ? a.getServings() : Integer.MIN_VALUE;
                Integer sb = b.getServings() != null ? b.getServings() : Integer.MIN_VALUE;
                return Integer.compare(sb, sa); // lớn trước (desc) by default in your previous logic
            };
            // note: we'll invert if sortAscending == false
        } else { // name
            comparator = (a, b) -> {
                String na = a.getTitle() != null ? a.getTitle() : "";
                String nb = b.getTitle() != null ? b.getTitle() : "";
                return na.compareToIgnoreCase(nb);
            };
        }

        // apply comparator and respect sortAscending
        if (comparator != null) {
            if (sortAscending) {
                Collections.sort(filteredList, comparator);
            } else {
                Collections.sort(filteredList, comparator.reversed());
            }
        }

        // 3) pagination
        totalPages = Math.max(1, (int) Math.ceil((double) filteredList.size() / ITEMS_PER_PAGE));
        if (currentPage > totalPages) currentPage = totalPages;

        createPageNumbers(); // update page buttons

        showPage(currentPage);
    }

    private void createPageNumbers() {
        if (paginationLayout == null) return;
        paginationLayout.removeAllViews();

        // prev arrow
        ImageView prev = new ImageView(this);
        prev.setImageResource(R.drawable.outline_arrow_back_ios_24);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
        imgLp.setMargins(dpToPx(6),0,dpToPx(6),0);
        prev.setLayoutParams(imgLp);
        prev.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        prev.setOnClickListener(v -> {
            if (currentPage > 1) {
                showPage(currentPage - 1);
            }
        });
        paginationLayout.addView(prev);

        // page numbers (create up to totalPages)
        for (int i = 1; i <= totalPages; i++) {
            final int p = i;
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(i));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            tv.setLayoutParams(lp);
            tv.setBackgroundResource(R.drawable.bg_view_border);
            tv.setOnClickListener(v -> showPage(p));
            paginationLayout.addView(tv);
        }

        // next arrow
        ImageView next = new ImageView(this);
        next.setImageResource(R.drawable.outline_arrow_forward_ios_24);
        next.setLayoutParams(imgLp);
        next.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        next.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                showPage(currentPage + 1);
            }
        });
        paginationLayout.addView(next);

        updatePageColors();
    }

    private void showPage(int page) {
        currentPage = page;
        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredList.size());
        List<FavoriteItem> pageItems = new ArrayList<>();
        if (start < end) pageItems.addAll(filteredList.subList(start, end));
        adapter.setItems(pageItems);
        updatePageColors();
    }

    private void updatePageColors() {
        if (paginationLayout == null) return;
        // children: prev, pages..., next
        int childCount = paginationLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = paginationLayout.getChildAt(i);
            if (v instanceof TextView) {
                int pageNum = Integer.parseInt(((TextView) v).getText().toString());
                if (pageNum == currentPage) {
                    ((TextView) v).setTextColor(getResources().getColor(android.R.color.white));
                    v.setBackgroundColor(getResources().getColor(android.R.color.black));
                } else {
                    ((TextView) v).setTextColor(getResources().getColor(android.R.color.darker_gray));
                    v.setBackgroundResource(R.drawable.bg_view_border);
                }
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ========== Confirm & Delete (unchanged) ==========
    private void confirmRemoveFavorite(FavoriteItem item) {
        if (item == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Favorite")
                .setMessage("Do you want to delete \"" + (item.getTitle() == null ? "" : item.getTitle()) + "\"?")
                .setPositiveButton("Delete", (d, w) -> removeFavorite(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeFavorite(FavoriteItem item) {
        if (item == null || item.getId() == null) return;

        JsonObject body = new JsonObject();
        body.addProperty("recipeId", item.getId());

        RequestBody rb = RequestBody.create(body.toString(), JSON);
        Request req = new Request.Builder()
                .url(BASE_URL)
                .delete(rb)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(FavoriteListActivity.this, "Xoá thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(FavoriteListActivity.this, "Lỗi xoá: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }
                // on success: refetch so UI + pagination sync
                runOnUiThread(() -> {
                    Toast.makeText(FavoriteListActivity.this, "Đã xoá khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    fetchFavorites();
                });
            }
        });
    }

    // Adapter callbacks
    @Override
    public void onRemoveClicked(FavoriteItem item) { confirmRemoveFavorite(item); }

    @Override
    public void onItemClicked(FavoriteItem item) {
        // open detail later; for now show toast
        Toast.makeText(this, item.getTitle() != null ? item.getTitle() : "Open", Toast.LENGTH_SHORT).show();
    }
}
