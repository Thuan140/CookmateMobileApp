package com.example.cookmate;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.cookmate.adapters.EmojiGridAdapter;
import com.example.cookmate.utils.EmojiData;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddIngredientCategoryActivity extends AppCompatActivity {

    private EditText inputCategoryName;
    private LinearLayout iconPickerBar;
    private TextView selectedIcon, iconStatusText;
    private Button btnSave;
    private ImageView backButton;

    private String chosenIcon = "";

    private static final String BASE_URL = "https://cookm8.vercel.app/api/ingredient-categories";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_ingredient_category);

        inputCategoryName = findViewById(R.id.input_category_name);
        iconPickerBar = findViewById(R.id.icon_picker_bar);
        selectedIcon = findViewById(R.id.selected_icon);
        iconStatusText = findViewById(R.id.icon_status_text);
        btnSave = findViewById(R.id.btn_save);
        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> finish());
        iconPickerBar.setOnClickListener(v -> showEmojiPickerDialog());
        btnSave.setOnClickListener(v -> saveCategory());
    }

    private void showEmojiPickerDialog() {
        List<String> emojiList = EmojiData.getAll();

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_emoji_grid, null);

        RecyclerView rv = dialogView.findViewById(R.id.emojiRecycler);
        EditText searchEt = dialogView.findViewById(R.id.emojiSearch);
        LinearLayout tabsContainer = dialogView.findViewById(R.id.emojiTabs);

        rv.setLayoutManager(new GridLayoutManager(this, 6));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss());
        AlertDialog dialog = builder.create();

        EmojiGridAdapter gridAdapter = new EmojiGridAdapter(emojiList, emoji -> {
            chosenIcon = emoji;
            selectedIcon.setText(chosenIcon);
            iconStatusText.setText("Icon selected");
            dialog.dismiss();
        });
        rv.setAdapter(gridAdapter);

        // Tabs filter
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
                List<String> filtered = EmojiData.filterByCategory(t);
                emojiList.clear();
                emojiList.addAll(filtered);
                gridAdapter.notifyDataSetChanged();
            });
        }

        // 🔍 Search theo tên emoji
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim();
                List<String> filtered = q.isEmpty() ? EmojiData.getAll() : EmojiData.search(q);
                emojiList.clear();
                emojiList.addAll(filtered);
                gridAdapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private void saveCategory() {
        String name = inputCategoryName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter category name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chosenIcon.isEmpty()) {
            Toast.makeText(this, "Please choose an icon", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("icon", chosenIcon);

            String token = new SessionManager(this).getToken();

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_URL,
                    body,
                    response -> {
                        Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    },
                    error -> {
                        String msg = (error.networkResponse != null)
                                ? "HTTP " + error.networkResponse.statusCode
                                : error.toString();
                        Toast.makeText(this, "Add failed: " + msg, Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    if (token != null) h.put("Authorization", "Bearer " + token);
                    h.put("Content-Type", "application/json");
                    return h;
                }
            };

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(req);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
