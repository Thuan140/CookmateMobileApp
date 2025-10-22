package com.example.cookmate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.adapters.CategoryAdapter;
import com.example.cookmate.models.IngredientCategory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SelectCategoryActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";
    public static final String EXTRA_CATEGORY_NAME = "extra_category_name";
    public static final String EXTRA_CATEGORIES_CHANGED = "categories_changed"; // new
    private static final int REQ_ADD_CATEGORY = 200;
    private static final int REQ_UPDATE_CATEGORY = 201;

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private EditText searchBar;

    private List<IngredientCategory> categoryList = new ArrayList<>();

    // track whether categories were modified during this SelectCategory session
    private boolean categoriesChanged = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_category);

        recyclerView = findViewById(R.id.category_list);
        searchBar = findViewById(R.id.search_bar);
        // replace finish() with onCancelPressed so we return categoriesChanged flag
        findViewById(R.id.btn_cancel).setOnClickListener(v -> onCancelPressed());

        // ✅ “Add New Category” bar
        LinearLayout createNewCategory = findViewById(R.id.create_new_category);
        createNewCategory.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddIngredientCategoryActivity.class);
            startActivityForResult(intent, REQ_ADD_CATEGORY);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(categoryList, new CategoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(IngredientCategory category) {
                onCategorySelected(category);
            }

            @Override
            public void onEditClick(IngredientCategory category) {
                onCategoryEdit(category);
            }
        });
        recyclerView.setAdapter(adapter);

        loadCategories();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCategories() {
        String token = new SessionManager(this).getToken();
        Log.d("SelectCategory", "Token: " + token);
        IngredientCategoryApiService svc = new IngredientCategoryApiService(this);

        svc.getCategories(token, new IngredientCategoryApiService.CategoriesCallback() {
            @Override
            public void onSuccess(JSONArray categoriesArray) {
                categoryList.clear();
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject obj = categoriesArray.optJSONObject(i);
                    if (obj != null) {
                        IngredientCategory cat = new IngredientCategory();
                        String id = obj.optString("_id", obj.optString("id", ""));
                        String name = obj.optString("name", obj.optString("title", "Unnamed"));
                        String icon = obj.optString("icon", obj.optString("emoji", "🍽"));
                        cat.setId(id);
                        cat.setName(name);
                        cat.setIcon(icon);
                        categoryList.add(cat);
                    }
                }
                runOnUiThread(() -> adapter.updateData(categoryList));
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("SelectCategory", "Load failed: " + errorMessage);
                runOnUiThread(() ->
                        Toast.makeText(SelectCategoryActivity.this,
                                "Load failed: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void onCategorySelected(IngredientCategory category) {
        Intent result = new Intent();
        result.putExtra(EXTRA_CATEGORY_ID, category.getId());
        result.putExtra(EXTRA_CATEGORY_NAME, category.getName());
        result.putExtra(EXTRA_CATEGORIES_CHANGED, categoriesChanged); // trả flag về caller
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private void onCategoryEdit(IngredientCategory category) {
        // Gửi id, name, icon sang Activity update
        Intent intent = new Intent(this, UpdateIngredientCategoryActivity.class);
        intent.putExtra(UpdateIngredientCategoryActivity.EXTRA_CATEGORY_ID, category.getId());
        intent.putExtra(UpdateIngredientCategoryActivity.EXTRA_CATEGORY_NAME, category.getName());
        intent.putExtra(UpdateIngredientCategoryActivity.EXTRA_CATEGORY_ICON, category.getIcon());
        startActivityForResult(intent, REQ_UPDATE_CATEGORY);
    }

    private void onCancelPressed() {
        Intent result = new Intent();
        result.putExtra(EXTRA_CATEGORIES_CHANGED, categoriesChanged);
        // if changed -> return RESULT_OK so caller can act, otherwise RESULT_CANCELED
        setResult(categoriesChanged ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADD_CATEGORY && resultCode == RESULT_OK) {
            // danh mục mới đã thêm -> reload local list và đặt flag
            categoriesChanged = true;
            loadCategories();
        } else if (requestCode == REQ_UPDATE_CATEGORY && resultCode == RESULT_OK) {
            // danh mục đã update -> reload local list và đặt flag
            categoriesChanged = true;
            loadCategories();
        }
    }
}
