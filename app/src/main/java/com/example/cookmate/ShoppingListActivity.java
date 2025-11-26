package com.example.cookmate;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.adapters.ShoppingAdapter;
import com.example.cookmate.models.ShoppingItem;
import com.google.gson.Gson;
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
import java.util.Collections;
import java.util.List;

public class ShoppingListActivity extends AppCompatActivity implements ShoppingAdapter.Callbacks {
    private SessionManager sessionManager;
    private String authToken;

    private static final String BASE_URL = "https://cookm8.vercel.app/api/shopping";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private ShoppingAdapter adapter;
    private RecyclerView recyclerView;
    private ImageButton btnAdd;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoppinglist);

        // --- INIT VIEWS first ---
        // thêm import: import android.widget.ImageView;
        ImageView backBtn = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.recyclerViewShoppingList);
        btnAdd = findViewById(R.id.buttonAdd);

        // BACK button
        backBtn.setOnClickListener(v -> {
            // gọi onBackPressed() để giữ hành vi hệ thống, hoặc finish()
            onBackPressed();
            overridePendingTransition(0, 0);
        });

        // Session (lấy token) — nếu không có token thì finish sớm
        sessionManager = new SessionManager(this);
        authToken = sessionManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Bạn chưa đăng nhập hoặc token đã hết hạn", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Recycler + adapter
        adapter = new ShoppingAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Add button
        btnAdd.setOnClickListener(v -> showAddDialog());

        // cuối cùng: tải dữ liệu
        fetchShoppingList();
    }


    private void fetchShoppingList() {
        Request req = new Request.Builder()
                .url(BASE_URL)
                .get()
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                        "Không thể tải danh sách: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401) {
                    runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                            "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show());
                    return;
                }

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                            "Lỗi máy chủ: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                String body = response.body().string();
                JsonObject json = gson.fromJson(body, JsonObject.class);
                if (json != null && json.has("shoppingList")) {
                    Type listType = new TypeToken<List<ShoppingItem>>(){}.getType();
                    List<ShoppingItem> list = gson.fromJson(json.get("shoppingList"), listType);
                    runOnUiThread(() -> adapter.setItems(list));
                    String targetId = getIntent().getStringExtra("scrollToId");
                    if (targetId != null) {
                        int index = adapter.getIndexById(targetId);
                        if (index >= 0) recyclerView.scrollToPosition(index);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                            "Phản hồi không hợp lệ từ server", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }


    private void showAddDialog() {
        // simple dialog with one EditText for name (and notes optional)
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Add shopping item");

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_shopping_item, null);
        final EditText etName = v.findViewById(R.id.etName);
        final EditText etNotes = v.findViewById(R.id.etNotes);

        b.setView(v);
        b.setPositiveButton("Add", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
                return;
            }
            addShoppingItem(name, notes);
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void addShoppingItem(String name, String notes) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("status", "draft");
        if (notes != null) body.addProperty("notes", notes);

        RequestBody rb = RequestBody.create(body.toString(), JSON);

        Request req = new Request.Builder()
                .url(BASE_URL)
                .post(rb)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                        "Thêm thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401) {
                    runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                            "Phiên đăng nhập hết hạn. Đăng nhập lại.", Toast.LENGTH_LONG).show());
                    return;
                }

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                            "Lỗi thêm item: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                String resp = response.body().string();
                JsonObject json = gson.fromJson(resp, JsonObject.class);
                if (json != null && json.has("shoppingItem")) {
                    ShoppingItem item = gson.fromJson(json.get("shoppingItem"), ShoppingItem.class);
                    runOnUiThread(() -> {
                        adapter.addItem(item);
                        recyclerView.scrollToPosition(0);
                        Toast.makeText(ShoppingListActivity.this, "Đã thêm vào danh sách", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    private void editShoppingItem(ShoppingItem item) {
        if (item == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chỉnh sửa mục mua sắm");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_shopping_item, null);
        final EditText etName = view.findViewById(R.id.etName);
        final EditText etNotes = view.findViewById(R.id.etNotes);

        // Gán giá trị cũ
        etName.setText(item.getName());
        etNotes.setText(item.getNotes());

        builder.setView(view);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newNotes = etNotes.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }
            updateShoppingItem(item, newName, newNotes);
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    private void updateShoppingItem(ShoppingItem item, String newName, String newNotes) {
        JsonObject body = new JsonObject();
        body.addProperty("shoppingItemId", item.getId());
        body.addProperty("name", newName);
        body.addProperty("status", item.getStatus() != null ? item.getStatus() : "draft");
        if (newNotes != null) body.addProperty("notes", newNotes);

        RequestBody rb = RequestBody.create(body.toString(), JSON);

        Request req = new Request.Builder()
                .url(BASE_URL)
                .put(rb)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                        "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401) {
                    runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                            "Phiên đăng nhập hết hạn. Đăng nhập lại.", Toast.LENGTH_LONG).show());
                    return;
                }

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                            "Lỗi cập nhật: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                String resp = response.body().string();
                JsonObject json = gson.fromJson(resp, JsonObject.class);
                if (json != null && json.has("shoppingItem")) {
                    ShoppingItem updated = gson.fromJson(json.get("shoppingItem"), ShoppingItem.class);

                    runOnUiThread(() -> {
                        // Cập nhật lại danh sách hiển thị
                        adapter.removeItemById(item.getId());
                        adapter.addItem(updated);
                        Toast.makeText(ShoppingListActivity.this, "Đã cập nhật thành công", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }


    // DELETE endpoint expects body: { "shoppingItemId": "..." }
    private void deleteShoppingItem(ShoppingItem item) {
        if (item == null || item.getId() == null) return;

        JsonObject body = new JsonObject();
        body.addProperty("shoppingItemId", item.getId());

        RequestBody rb = RequestBody.create(body.toString(), JSON);

        Request req = new Request.Builder()
                .url(BASE_URL)
                .delete(rb)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                        "Xoá thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401) {
                    runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                            "Phiên đăng nhập hết hạn. Đăng nhập lại.", Toast.LENGTH_LONG).show());
                    return;
                }

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ShoppingListActivity.this,
                            "Lỗi xoá: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                String resp = response.body().string();
                JsonObject json = gson.fromJson(resp, JsonObject.class);
                if (json != null && json.has("shoppingItem")) {
                    JsonObject deleted = json.getAsJsonObject("shoppingItem");
                    String deletedId = deleted.has("_id") ? deleted.get("_id").getAsString() : null;
                    final String idToRemove = deletedId != null ? deletedId : item.getId();
                    runOnUiThread(() -> {
                        adapter.removeItemById(idToRemove);
                        Toast.makeText(ShoppingListActivity.this, "Đã xoá thành công", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }


    // If you want to update status when checked/unchecked you can implement a PATCH/PUT.
    // Here we only change locally and optionally call an update endpoint (not provided in screenshots).
    private void updateItemStatusLocal(ShoppingItem item, boolean checked) {
        String newStatus = checked ? "done" : "draft";
        item.setStatus(newStatus);
        // Optionally: call a PUT/PATCH if backend supports it.
    }

    // Adapter callbacks:
    @Override
    public void onDeleteClicked(ShoppingItem item) {
        deleteShoppingItem(item);
    }

    @Override
    public void onEditClicked(ShoppingItem item) {
        editShoppingItem(item);
    }


    @Override
    public void onCheckChanged(ShoppingItem item, boolean checked) {
        updateItemStatusLocal(item, checked);
    }
}
