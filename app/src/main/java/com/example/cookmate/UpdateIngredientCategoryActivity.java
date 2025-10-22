package com.example.cookmate;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.adapters.EmojiGridAdapter;
import com.example.cookmate.utils.EmojiData;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UpdateIngredientCategoryActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY_ID = "EXTRA_CATEGORY_ID";
    public static final String EXTRA_CATEGORY_NAME = "EXTRA_CATEGORY_NAME";
    public static final String EXTRA_CATEGORY_ICON = "EXTRA_CATEGORY_ICON";

    private static final String TAG = "UpdateIngredientCategory";

    private EditText etName;
    private TextView tvSelectedIcon;
    private TextView tvIconStatus;
    private LinearLayout iconPickerBar;
    private Button btnSave;
    private ImageView btnBack;

    private String categoryId;
    private String chosenIcon = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_ingredient_category);

        etName = findViewById(R.id.input_category_name);
        tvSelectedIcon = findViewById(R.id.selected_icon);
        tvIconStatus = findViewById(R.id.icon_status_text);
        iconPickerBar = findViewById(R.id.icon_picker_bar);
        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.back_button);

        // nhận init từ Intent (nếu có) để UI không trống
        if (getIntent() != null) {
            categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
            String initName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
            String initIcon = getIntent().getStringExtra(EXTRA_CATEGORY_ICON);

            if (initName != null) etName.setText(initName);
            if (initIcon != null && !initIcon.trim().isEmpty()) {
                chosenIcon = initIcon;
                tvSelectedIcon.setText(initIcon);
                tvIconStatus.setText("Selected");
            } else {
                tvSelectedIcon.setText("");
                tvIconStatus.setText("Choose an icon");
            }
        }

        if (categoryId == null || categoryId.trim().isEmpty()) {
            Toast.makeText(this, "Category ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // tải chi tiết từ server (tùy chọn) - nếu có dữ liệu ở Intent thì UI đã có
        loadCategoryDetails(categoryId);

        // bấm vào thanh chọn icon -> mở dialog giống Add (grid + tabs + search)
        iconPickerBar.setOnClickListener(v -> showEmojiPickerDialog());

        btnSave.setOnClickListener(v -> onSaveClicked());
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Reuse the same emoji picker dialog as AddIngredientCategoryActivity:
     * - dialog_emoji_grid layout with RecyclerView, search EditText, and tabs container
     * - EmojiGridAdapter and EmojiData utilities
     */
    private void showEmojiPickerDialog() {
        // Mutable copy of all emojis
        final ArrayList<String> emojiList = new ArrayList<>(EmojiData.getAll());

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_emoji_grid, null);

        final RecyclerView rv = dialogView.findViewById(R.id.emojiRecycler);
        final EditText searchEt = dialogView.findViewById(R.id.emojiSearch);
        final LinearLayout tabsContainer = dialogView.findViewById(R.id.emojiTabs);

        rv.setLayoutManager(new GridLayoutManager(this, 6));

        // create dialog first so we can dismiss it from the adapter callback
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        // Adapter with click listener: set chosenIcon, update UI, dismiss dialog
        com.example.cookmate.adapters.EmojiGridAdapter gridAdapter =
                new com.example.cookmate.adapters.EmojiGridAdapter(emojiList, emoji -> {
                    chosenIcon = emoji;
                    tvSelectedIcon.setText(chosenIcon);
                    tvIconStatus.setText("Icon selected");
                    dialog.dismiss();
                });

        // attach adapter BEFORE filters/search so notifyDataSetChanged works reliably
        rv.setAdapter(gridAdapter);

        // Initialize tabs (same categories as Add)
        String[] tabs = {"All", "Food", "Drink"};
        for (String t : tabs) {
            Button b = new Button(this);
            b.setText(t);
            b.setAllCaps(false);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            p.setMargins(8, 8, 8, 8);
            b.setLayoutParams(p);
            tabsContainer.addView(b);

            b.setOnClickListener(tb -> {
                List<String> filtered;
                if ("All".equalsIgnoreCase(t)) filtered = EmojiData.getAll();
                else filtered = EmojiData.filterByCategory(t);
                emojiList.clear();
                emojiList.addAll(filtered);
                gridAdapter.notifyDataSetChanged();
            });
        }

        // Search box: update emojiList and notify adapter
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim();
                List<String> filtered = q.isEmpty() ? EmojiData.getAll() : EmojiData.search(q);
                emojiList.clear();
                emojiList.addAll(filtered);
                gridAdapter.notifyDataSetChanged();
                // Optional: scroll to top so user sees results
                rv.scrollToPosition(0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // show dialog after setup
        dialog.show();

        // Optional: size tweak
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }



    /**
     * Load category details robustly. If server returns different shapes (wrapped object), handle it.
     * On error we only log — do not show toast so user won't be annoyed since Intent already filled UI.
     */
    private void loadCategoryDetails(String id) {
        String token = new SessionManager(this).getToken();
        if (token == null) {
            Toast.makeText(this, "Please login", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        IngredientCategoryApiService svc = new IngredientCategoryApiService(this);
        svc.getCategoryById(token, id, new IngredientCategoryApiService.CategoryCallback() {
            @Override
            public void onSuccess(JSONObject categoryJson) {
                runOnUiThread(() -> {
                    try {
                        JSONObject inner = null;
                        if (categoryJson.has("ingredientCategory") && categoryJson.opt("ingredientCategory") instanceof JSONObject) {
                            inner = categoryJson.optJSONObject("ingredientCategory");
                        } else if (categoryJson.has("data") && categoryJson.opt("data") instanceof JSONObject) {
                            inner = categoryJson.optJSONObject("data");
                        } else {
                            inner = categoryJson;
                        }

                        String name = inner.optString("name", null);
                        String icon = inner.optString("icon", null);

                        if (name != null && !name.isEmpty()) etName.setText(name);
                        if (icon != null && !icon.isEmpty()) {
                            chosenIcon = icon;
                            tvSelectedIcon.setText(icon);
                            tvIconStatus.setText("Selected");
                        } else {
                            if (tvSelectedIcon.getText() == null || tvSelectedIcon.getText().toString().isEmpty()) {
                                tvIconStatus.setText("Choose an icon");
                            }
                        }
                    } catch (Exception ex) {
                        Log.w(TAG, "Parse detail failed: " + ex.getMessage(), ex);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Detail load failed: " + errorMessage);
            }
        });
    }

    private void onSaveClicked() {
        String newName = etName.getText().toString().trim();
        String newIcon = chosenIcon != null && !chosenIcon.isEmpty() ? chosenIcon :
                (tvSelectedIcon.getText() != null ? tvSelectedIcon.getText().toString().trim() : "");

        if (newName.isEmpty()) {
            etName.setError("Name required");
            return;
        }

        if (newIcon == null) newIcon = "";

        String token = new SessionManager(this).getToken();
        if (token == null) {
            Toast.makeText(this, "Please login", Toast.LENGTH_SHORT).show();
            return;
        }

        IngredientCategoryApiService svc = new IngredientCategoryApiService(this);
        svc.updateCategoryWithFallback(token, categoryId, newName, newIcon, new IngredientCategoryApiService.CategoryApiCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(UpdateIngredientCategoryActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(UpdateIngredientCategoryActivity.this, "Update error: " + errorMessage, Toast.LENGTH_LONG).show());
            }
        });
    }
}
