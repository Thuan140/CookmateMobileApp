package com.example.cookmate;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.adapters.FavoriteAdapter;
import com.example.cookmate.models.FavoriteItem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoriteListActivity extends AppCompatActivity implements FavoriteAdapter.Callbacks {
    private static final String TAG = "FavoriteList";
    private static final String BASE_URL = "https://cookm8.vercel.app/api/favorites";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private SessionManager sessionManager;
    private String authToken;

    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();

    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private ImageView backBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoritelist);

        // init views
        recyclerView = findViewById(R.id.favoritesRecyclerView);
        backBtn = findViewById(R.id.imageView2); // or use a proper back id if layout has one

        backBtn.setOnClickListener(v -> {
            onBackPressed();
            overridePendingTransition(0,0);
        });

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

        fetchFavorites();
    }

    // Try GET; if server expects POST, try POST fallback (no body)
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
                // try POST fallback
                fetchFavoritesPostFallback();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401) {
                    runOnUiThread(() -> Toast.makeText(FavoriteListActivity.this, "Phiên đăng nhập hết hạn", Toast.LENGTH_LONG).show());
                    return;
                }
                if (!response.isSuccessful()) {
                    Log.w(TAG, "GET not successful: " + response.code());
                    // try POST fallback
                    fetchFavoritesPostFallback();
                    return;
                }
                String body = response.body().string();
                parseFavoritesResponse(body);
            }
        });
    }

    // POST fallback (some endpoints return list on POST with empty body)
    private void fetchFavoritesPostFallback() {
        RequestBody rb = RequestBody.create("{}", JSON);
        Request req = new Request.Builder()
                .url(BASE_URL)
                .post(rb)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
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

    // Parse JSON response: looks for array fields like "recipes" or "favorites" or top-level array
    private void parseFavoritesResponse(String body) {
        try {
            JsonObject root = gson.fromJson(body, JsonObject.class);
            List<FavoriteItem> list = new ArrayList<>();

            if (root != null && root.has("recipes") && root.get("recipes").isJsonArray()) {
                for (var el : root.getAsJsonArray("recipes")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject r = el.getAsJsonObject();

                    // id (may be number or string)
                    String id = "";
                    if (r.has("id")) {
                        // convert any primitive to string
                        try { id = r.get("id").getAsString(); }
                        catch (Exception ex) { id = String.valueOf(r.get("id").getAsLong()); }
                    }

                    String image = r.has("image") && !r.get("image").isJsonNull() ? r.get("image").getAsString() : "";
                    String title = r.has("title") && !r.get("title").isJsonNull() ? r.get("title").getAsString() : "";

                    // Find creator/author from several possible fields
                    String creator = "";
                    String[] candidateKeys = new String[] {
                            "creditsText", "creditText", "sourceName", "author", "creator", "createdBy", "source"
                    };
                    for (String k : candidateKeys) {
                        if (r.has(k) && !r.get(k).isJsonNull()) {
                            creator = r.get(k).getAsString();
                            if (creator != null && !creator.trim().isEmpty()) break;
                        }
                    }
                    // fallback: try nested object like "source" : { "name": "..." }
                    if ((creator == null || creator.isEmpty()) && r.has("source") && r.get("source").isJsonObject()) {
                        JsonObject src = r.getAsJsonObject("source");
                        if (src.has("name") && !src.get("name").isJsonNull()) {
                            creator = src.get("name").getAsString();
                        }
                    }

                    // final fallback: use sourceUrl domain (extract host) if present
                    if ((creator == null || creator.isEmpty()) && r.has("sourceUrl") && !r.get("sourceUrl").isJsonNull()) {
                        try {
                            String url = r.get("sourceUrl").getAsString();
                            java.net.URI uri = new java.net.URI(url);
                            String host = uri.getHost();
                            if (host != null) {
                                creator = host.replaceFirst("^www\\.", "");
                            } else {
                                creator = url;
                            }
                        } catch (Exception ignored) { }
                    }

                    if (creator == null) creator = "";

                    FavoriteItem fi = new FavoriteItem(id, image, title, creator);
                    list.add(fi);
                }
            } else {
                // fallback: try parse as array of recipes directly
                Type listType = new TypeToken<List<JsonObject>>(){}.getType();
                List<JsonObject> rawList = gson.fromJson(body, listType);
                if (rawList != null) {
                    for (JsonObject r : rawList) {
                        String id = r.has("id") ? r.get("id").getAsString() : "";
                        String image = r.has("image") ? r.get("image").getAsString() : "";
                        String title = r.has("title") ? r.get("title").getAsString() : "";
                        String creator = "";
                        if (r.has("creditsText")) creator = r.get("creditsText").getAsString();
                        list.add(new FavoriteItem(id, image, title, creator));
                    }
                }
            }

            final List<FavoriteItem> finalList = list;
            runOnUiThread(() -> {
                adapter.setItems(finalList);
                if (finalList.isEmpty()) {
                    Toast.makeText(FavoriteListActivity.this, "Không có mục yêu thích", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ex) {
            Log.e("FavoriteParse", "Parse error: " + ex.getMessage(), ex);
            runOnUiThread(() -> Toast.makeText(FavoriteListActivity.this, "Lỗi phân tích dữ liệu", Toast.LENGTH_SHORT).show());
        }
    }


    // Remove favorite: assume DELETE /api/favorites with body { "recipeId": "<id>" }
    private void removeFavorite(FavoriteItem item) {
        if (item == null || item.getId() == null) return;

        JsonObject body = new JsonObject();
        // try common keys: "recipeId" or "favoriteId"
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
                String resp = response.body().string();
                // If success, remove locally
                runOnUiThread(() -> {
                    adapter.removeById(item.getId());
                    Toast.makeText(FavoriteListActivity.this, "Đã xoá khỏi yêu thích", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Adapter callbacks
    @Override
    public void onRemoveClicked(FavoriteItem item) {
        removeFavorite(item);
    }

    @Override
    public void onItemClicked(FavoriteItem item) {
        // open recipe detail (if you have), otherwise toast
        Toast.makeText(this, item.getTitle() != null ? item.getTitle() : "Mở chi tiết", Toast.LENGTH_SHORT).show();
    }
}
