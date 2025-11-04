package com.example.cookmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;

import com.example.cookmate.adapters.PantryAdapter;
import com.example.cookmate.models.Ingredient;
import com.example.cookmate.models.IngredientCategory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PantryActivity extends AppCompatActivity implements PantryAdapter.ActionListener {
    // --- constants ---
    private static final int ITEMS_PER_PAGE = 3;

    // --- UI & adapters ---
    private RecyclerView recyclerView;
    private PantryAdapter adapter;
    private List<Ingredient> ingredientList = new ArrayList<>();
    private List<Ingredient> filteredList = new ArrayList<>();
    private List<Ingredient> currentPageList = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;

    private ImageView btnPrev, btnNext, buttonShopping;
    private LinearLayout pageContainer;
    private ImageButton buttonAdd;

    // filter / sort UI
    private EditText searchInput;
    private Button buttonAll, buttonCategories;
    private TextView sortValue, emptyText;

    // --- categories cache ---
    private List<IngredientCategory> cachedCategories = new ArrayList<>();
    private boolean categoriesLoaded = false;

    // --- dialog / pending state ---
    private byte[] pendingImageData = null;
    private String pendingImageName = null;
    private boolean pendingIsEdit = false;
    private Ingredient pendingEditIngredient = null;

    // For category selection state in dialog
    private String pendingSelectedCategoryId = null;
    private String pendingSelectedCategoryName = null;
    private Button pendingCategoryBtnRef = null;

    // filtering state
    private String activeCategoryFilterId = null; // null means show all
    private String currentSearchQuery = "";
    private String currentSort = "name"; // "name" or "expiry"

    // --- ActivityResultLaunchers ---
    private ActivityResultLauncher<Intent> selectCategoryLauncher;
    private ActivityResultLauncher<String> pickImageLauncher; // GetContent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantrylist);
        NavHelper.setupBottomNav(this, R.id.navigation_pantry);

        // bind filter UI
        searchInput = findViewById(R.id.search_input);
        buttonAll = findViewById(R.id.buttonAll);
        buttonCategories = findViewById(R.id.buttonCategories);
        sortValue = findViewById(R.id.sort_value);
        sortValue.setOnClickListener(v -> {
            // giả sử sort_toggle là id của ImageView/Icon bạn đã bind trước đó
            View sortToggle = findViewById(R.id.sort_toggle);
            if (sortToggle != null) sortToggle.performClick();
        });
        emptyText = findViewById(R.id.empty_text);
        buttonShopping = findViewById(R.id.buttonShopping);
        buttonShopping.setOnClickListener(v -> {
            try {
                Intent it = new Intent(PantryActivity.this, ShoppingListActivity.class);
                startActivity(it);
            } catch (Exception e) {
                // phòng trường hợp bạn đặt tên khác cho Activity
                Toast.makeText(PantryActivity.this, "Cannot open shopping list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
        // Register launcher for selecting category (SelectCategoryActivity)
        selectCategoryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null) {
                        int rc = result.getResultCode();
                        Intent data = result.getData();
                        if (rc == Activity.RESULT_OK && data != null) {
                            String catId = data.getStringExtra(SelectCategoryActivity.EXTRA_CATEGORY_ID);
                            String catName = data.getStringExtra(SelectCategoryActivity.EXTRA_CATEGORY_NAME);
                            boolean categoriesChanged = data.getBooleanExtra(SelectCategoryActivity.EXTRA_CATEGORIES_CHANGED, false);

                            // Lưu tạm (dành cho dialog add/edit)
                            pendingSelectedCategoryId = catId;
                            pendingSelectedCategoryName = catName;

                            // If caller requested reload of categories, do it
                            if (categoriesChanged) {
                                loadCategories();
                            }

                            // also update pending button text (if any)
                            if (pendingCategoryBtnRef != null) {
                                String icon = "";
                                for (IngredientCategory c : cachedCategories) {
                                    if (catId != null && c.getId().equals(catId)) {
                                        icon = c.getIcon();
                                        break;
                                    }
                                }
                                if (icon != null && !icon.isEmpty())
                                    pendingCategoryBtnRef.setText(icon + "  " + (catName != null ? catName : "Selected"));
                                else
                                    pendingCategoryBtnRef.setText(catName != null && !catName.isEmpty() ? catName : "Selected");
                            }
                        } else if (result != null && result.getResultCode() == Activity.RESULT_CANCELED && result.getData() != null) {
                            boolean categoriesChanged = result.getData().getBooleanExtra(SelectCategoryActivity.EXTRA_CATEGORIES_CHANGED, false);
                            if (categoriesChanged) loadCategories();
                        }
                    }
                }
        );

        // Register launcher for picking image using GetContent
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (ActivityResultCallback<Uri>) uri -> {
                    if (uri != null) {
                        try {
                            InputStream is = getContentResolver().openInputStream(uri);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte[] buf = new byte[4096];
                            int r;
                            while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
                            is.close();
                            pendingImageData = bos.toByteArray();
                            pendingImageName = queryName(uri);
                            Toast.makeText(this, "Image selected: " + (pendingImageName != null ? pendingImageName : "ok"), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed read image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // --- normal init ---
        recyclerView = findViewById(R.id.ingredientsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryAdapter(this, currentPageList, this);
        recyclerView.setAdapter(adapter);

        btnPrev = findViewById(R.id.outline_arrow_back_ios_24);
        btnNext = findViewById(R.id.outline_arrow_forward_ios_24);
        pageContainer = findViewById(R.id.pageNumbersContainer);
        buttonAdd = findViewById(R.id.buttonAdd);

        setupPaginationButtons();

        // wire filter UI
        buttonAll.setOnClickListener(v -> {
            activeCategoryFilterId = null;
            currentSearchQuery = "";
            searchInput.setText("");
            applyFiltersAndShow();
        });

        buttonCategories.setOnClickListener(v -> showCategoryFilterDialog());

        // search
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                applyFiltersAndShow();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // sort toggle
        findViewById(R.id.sort_toggle).setOnClickListener(v -> {
            if ("name".equals(currentSort)) currentSort = "expiry";
            else currentSort = "name";
            sortValue.setText(currentSort);
            applyFiltersAndShow();
        });

        // basic load
        loadCategories();
        loadIngredients();

        buttonAdd.setOnClickListener(v -> showAddDialog());
    }

    // ========== Load categories (cache) ==========
    private void loadCategories() {
        String token = new SessionManager(this).getToken();
        if (token == null) return;
        IngredientCategoryApiService svc = new IngredientCategoryApiService(this);
        svc.getCategories(token, new IngredientCategoryApiService.CategoriesCallback() {
            @Override
            public void onSuccess(JSONArray categories) {
                runOnUiThread(() -> {
                    cachedCategories = mapCategories(categories);
                    categoriesLoaded = true;
                    Log.d("PANTRY", "categories loaded: " + cachedCategories.size());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Load categories failed: " + message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private List<IngredientCategory> mapCategories(JSONArray arr) {
        List<IngredientCategory> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                String id = o.optString("_id", o.optString("id", ""));
                String name = o.optString("name", "Unknown");
                String icon = o.optString("icon", "");
                IngredientCategory c = new IngredientCategory(id, name);
                c.setIcon(icon);
                out.add(c);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return out;
    }

    // ========== Load ingredients ==========
    private void loadIngredients() {
        String token = new SessionManager(this).getToken();
        if (token == null) { Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show(); return; }
        IngredientApiService svc = new IngredientApiService(this);
        svc.getIngredients(token, new IngredientApiService.IngredientCallback() {
            @Override
            public void onSuccess(JSONArray ingredients) {
                ingredientList.clear();
                for (int i=0;i<ingredients.length();i++){
                    try {
                        JSONObject o = ingredients.getJSONObject(i);
                        Ingredient ing = new Ingredient();
                        ing.setId(o.optString("_id", o.optString("id","")));
                        ing.setUserId(o.optString("userId", o.optString("user","")));
                        ing.setName(o.optString("name",""));
                        if (o.has("quantity")) {
                            try { ing.setQuantity(o.optDouble("quantity", 0)); } catch (Exception ex){ ing.setQuantity(0); }
                        }
                        ing.setUnit(o.optString("unit",""));
                        ing.setExpiryDate(o.optString("expiryDate", o.optString("expiry","")));
                        ing.setImage(o.optString("image",""));
                        ing.setNotes(o.optString("notes",""));
                        // category mapping
                        String catId = "";
                        if (o.has("categoryId") && !o.isNull("categoryId")) {
                            Object cat = o.opt("categoryId");
                            if (cat instanceof JSONObject) {
                                catId = ((JSONObject)cat).optString("_id", ((JSONObject)cat).optString("id",""));
                            } else catId = String.valueOf(cat);
                        } else if (o.has("category") && !o.isNull("category")) {
                            Object cat = o.opt("category");
                            if (cat instanceof JSONObject) catId = ((JSONObject)cat).optString("_id", ((JSONObject)cat).optString("id",""));
                            else catId = String.valueOf(cat);
                        } else {
                            catId = o.optString("category_id", o.optString("categoryId",""));
                        }
                        ing.setCategoryId(catId);
                        ingredientList.add(ing);
                    } catch (JSONException e) { e.printStackTrace(); }
                }
                runOnUiThread(() -> {
                    applyFiltersAndShow();
                    updateExpirationWarning(); // cập nhật card cảnh báo mỗi lần load
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Load error: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ========== Filtering / Sorting / Pagination ==========
    private void applyFiltersAndShow() {
        // 1) filter by category
        filteredList.clear();
        for (Ingredient ing : ingredientList) {
            // category filter
            if (activeCategoryFilterId != null && !activeCategoryFilterId.equals(ing.getCategoryId()))
                continue;

            // search match (name or category name)
            boolean matches = false;
            if (currentSearchQuery == null || currentSearchQuery.isEmpty()) {
                matches = true;
            } else {
                String q = currentSearchQuery.toLowerCase(Locale.getDefault());
                if (ing.getName() != null && ing.getName().toLowerCase(Locale.getDefault()).contains(q))
                    matches = true;
                else {
                    // check category name from cache
                    for (IngredientCategory c : cachedCategories) {
                        if (c.getId().equals(ing.getCategoryId())) {
                            if (c.getName() != null && c.getName().toLowerCase(Locale.getDefault()).contains(q)) {
                                matches = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (matches) filteredList.add(ing);
        }

        // 2) sort
        if ("expiry".equals(currentSort)) {
            Collections.sort(filteredList, (a, b) -> {
                String ea = a.getExpiryDate() != null ? a.getExpiryDate() : "";
                String eb = b.getExpiryDate() != null ? b.getExpiryDate() : "";
                return ea.compareTo(eb);
            });
        } else {
            Collections.sort(filteredList, (a, b) -> {
                String na = a.getName() != null ? a.getName() : "";
                String nb = b.getName() != null ? b.getName() : "";
                return na.compareToIgnoreCase(nb);
            });
        }

        // 3) pagination
        totalPages = Math.max(1, (int) Math.ceil((double) filteredList.size() / ITEMS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages);

    // tạo/hiện các nút số trang trước khi gọi showPage
        createPageNumbers();

    // nếu không có dữ liệu thì ẩn cả vùng pagination
        if (filteredList.isEmpty()) {
            pageContainer.setVisibility(View.GONE);
        } else {
            pageContainer.setVisibility(View.VISIBLE);
        }

    // hiển thị trang hiện tại
        showPage(currentPage);
    }

    private void setupPaginationButtons() {
        btnPrev.setOnClickListener(v -> { if (currentPage>1) showPage(currentPage-1); });
        btnNext.setOnClickListener(v -> { if (currentPage<totalPages) showPage(currentPage+1); });
    }

    private void createPageNumbers() {
        pageContainer.removeAllViews();

        // nếu chỉ có 1 trang hoặc không có item thì ẩn luôn (tuỳ bạn)
        if (totalPages <= 1) {
            pageContainer.setVisibility(View.GONE);
            return;
        } else {
            pageContainer.setVisibility(View.VISIBLE);
        }

        for (int i = 1; i <= totalPages; i++) {
            final int p = i;
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(i));
            tv.setPadding(20, 10, 20, 10);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            tv.setLayoutParams(params);
            tv.setBackgroundResource(R.drawable.bg_view_border);
            tv.setOnClickListener(v -> showPage(p));
            pageContainer.addView(tv);
        }
        updatePageColors();
    }


    private void showPage(int page) {
        currentPage = page;
        int start = (page-1)*ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredList.size());
        currentPageList.clear();
        if (start < end) currentPageList.addAll(filteredList.subList(start, end));
        adapter.updateList(currentPageList);
        updatePageColors();
    }

    private void updatePageColors() {
        for (int i=0;i<pageContainer.getChildCount();i++){
            TextView tv = (TextView) pageContainer.getChildAt(i);
            if (i+1 == currentPage) {
                tv.setTextColor(getResources().getColor(android.R.color.white));
                tv.setBackgroundColor(getResources().getColor(android.R.color.black));
            } else {
                tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
                tv.setBackgroundResource(R.drawable.bg_view_border);
            }
        }
    }

    // ========== Category filter dialog ==========
    private void showCategoryFilterDialog() {
        if (!categoriesLoaded || cachedCategories.isEmpty()) {
            Toast.makeText(this, "Categories are not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[cachedCategories.size()+1];
        names[0] = "All categories";
        for (int i=0;i<cachedCategories.size();i++) names[i+1] = cachedCategories.get(i).getName();

        new AlertDialog.Builder(this)
                .setTitle("Filter by category")
                .setItems(names, (dialog, which) -> {
                    if (which == 0) {
                        activeCategoryFilterId = null;
                    } else {
                        IngredientCategory sel = cachedCategories.get(which-1);
                        activeCategoryFilterId = sel.getId();
                    }
                    applyFiltersAndShow();
                })
                .show();
    }

    private void updateExpirationWarning() {
        int soonCount = 0;
        int expiredCount = 0;

        for (Ingredient ing : ingredientList) {
            String expiryIso = ing.getExpiryDate();
            if (expiryIso == null || expiryIso.trim().isEmpty()) continue;

            long days = daysUntilExpiry(expiryIso);
            if (days < 0) expiredCount++;
            else if (days <= 2) soonCount++;
        }

        CardView card = findViewById(R.id.expirationWarningCard);
        TextView txt = card != null ? card.findViewById(R.id.expirationWarningText) : null;
        ImageView icon = card != null ? card.findViewById(R.id.expirationWarningIcon) : null; // 👈 thêm dòng này

        if (card == null || txt == null || icon == null) return;

        if (expiredCount > 0) {
            txt.setText(expiredCount + " item(s) already expired");
            card.setCardBackgroundColor(Color.parseColor("#FFF2F2"));
            txt.setTextColor(Color.parseColor("#D32F2F"));
            icon.setImageResource(R.drawable.ic_error);     // 🔴 biểu tượng lỗi
            icon.setColorFilter(Color.parseColor("#D32F2F"));
            card.setVisibility(View.VISIBLE);
        } else if (soonCount > 0) {
            txt.setText(soonCount + " item(s) expiring within 2 days");
            card.setCardBackgroundColor(Color.parseColor("#FFF8E1"));
            txt.setTextColor(Color.parseColor("#F57C00"));
            icon.setImageResource(R.drawable.ic_warning);   // 🟠 biểu tượng cảnh báo
            icon.setColorFilter(Color.parseColor("#F57C00"));
            card.setVisibility(View.VISIBLE);
        } else {
            txt.setText("All ingredients are fresh ✅");
            card.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            txt.setTextColor(Color.parseColor("#388E3C"));
            icon.setImageResource(R.drawable.ic_check_circle); // 🟢 biểu tượng thành công
            icon.setColorFilter(Color.parseColor("#388E3C"));
            card.setVisibility(View.VISIBLE);
        }
    }



    // ================= Add Dialog =================
    private void showAddDialog() {
        pendingImageData = null;
        pendingImageName = null;
        pendingIsEdit = false;
        pendingEditIngredient = null;
        // reset category temp state when opening add
        pendingSelectedCategoryId = null;
        pendingSelectedCategoryName = null;
        pendingCategoryBtnRef = null;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36,18,36,18);

        // --- Name ---
        final EditText nameInput = new EditText(this);
        nameInput.setHint("Name");
        layout.addView(nameInput);

        // --- Category (Button thay Spinner) ---
        final Button categoryButton = new Button(this);
        categoryButton.setAllCaps(false);
        categoryButton.setTextSize(16f);
        categoryButton.setPadding(16, 16, 16, 16);
        categoryButton.setBackgroundResource(android.R.drawable.btn_default);
        categoryButton.setText("Select category");
        layout.addView(categoryButton);

        // lưu ref cho launcher callback
        pendingCategoryBtnRef = categoryButton;

        // bấm để mở SelectCategoryActivity
        categoryButton.setOnClickListener(v -> {
            Intent it = new Intent(PantryActivity.this, SelectCategoryActivity.class);
            it.putExtra("currentCategoryId", pendingSelectedCategoryId);
            selectCategoryLauncher.launch(it);
        });

        // --- Quantity ---
        final EditText qtyInput = new EditText(this);
        qtyInput.setHint("Quantity");
        qtyInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(qtyInput);

        // --- Unit (Spinner) ---
        final Spinner unitSpinner = new Spinner(this);
        String[] unitOptions = new String[]{"g","kg","ml","l","piece","cup","tbsp","tsp"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitOptions);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);
        layout.addView(unitSpinner);

        // --- Expiry (kept as before) ---
        final EditText expiryInput = new EditText(this);
        expiryInput.setFocusable(false);
        expiryInput.setClickable(true);
        expiryInput.setHint("Expiry date (tap to pick)");
        layout.addView(expiryInput);

        final Calendar expiryCal = Calendar.getInstance();
        expiryInput.setOnClickListener(v -> {
            int year = expiryCal.get(Calendar.YEAR);
            int month = expiryCal.get(Calendar.MONTH);
            int dayOfMonth = expiryCal.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dpd = new DatePickerDialog(PantryActivity.this,
                    (view, y, m, dayOfMonthParam) -> {
                        expiryCal.set(y, m, dayOfMonthParam);
                        expiryInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(expiryCal.getTime()));
                    }, year, month, dayOfMonth);
            dpd.show();
        });
        expiryInput.setOnLongClickListener(v -> { expiryInput.setText(""); expiryCal.setTimeInMillis(0); return true; });

        final EditText notesInput = new EditText(this);
        notesInput.setHint("Notes (optional)");
        layout.addView(notesInput);

        final ImageButton pickBtn = new ImageButton(this);
        pickBtn.setImageResource(android.R.drawable.ic_menu_gallery);
        layout.addView(pickBtn);
        pickBtn.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        new AlertDialog.Builder(this)
                .setTitle("Add Ingredient")
                .setView(layout)
                .setPositiveButton("Create", (dialogInterface, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String categoryId = pendingSelectedCategoryId != null ? pendingSelectedCategoryId : "";
                    String qty = qtyInput.getText().toString().trim();
                    if (qty.isEmpty()) qty = "0";
                    String unit = (String) unitSpinner.getSelectedItem(); // <- lấy từ spinner
                    String notes = notesInput.getText().toString().trim();

                    String expiry = expiryInput.getText().toString().trim();
                    if (!expiry.isEmpty()) {
                        try {
                            String[] parts = expiry.split("-");
                            int y = Integer.parseInt(parts[0]);
                            int m = Integer.parseInt(parts[1]); // 1..12
                            int d = Integer.parseInt(parts[2]);

                            Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            utcCal.clear();
                            utcCal.set(y, m - 1, d, 0, 0, 0);
                            utcCal.set(Calendar.MILLISECOND, 0);

                            SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                            isoSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                            expiry = isoSdf.format(utcCal.getTime());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    if (name.isEmpty() || categoryId.isEmpty()) {
                        Toast.makeText(this, "Name and category required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String,String> fields = new HashMap<>();
                    fields.put("name", name);
                    fields.put("categoryId", categoryId);
                    fields.put("quantity", qty);
                    fields.put("unit", unit);
                    if (!expiry.isEmpty()) fields.put("expiryDate", expiry);
                    fields.put("notes", notes);

                    String token = new SessionManager(PantryActivity.this).getToken();
                    IngredientApiService svc = new IngredientApiService(PantryActivity.this);

                    svc.createIngredientWithImage(token, fields, pendingImageData, new IngredientApiService.SimpleCallback() {
                        @Override public void onSuccess(JSONObject response) {
                            runOnUiThread(() -> {
                                Toast.makeText(PantryActivity.this, "Created", Toast.LENGTH_SHORT).show();
                                loadIngredients();
                            });
                        }
                        @Override public void onError(String errorMessage) {
                            runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Create error: " + errorMessage, Toast.LENGTH_LONG).show());
                        }
                    });

                    // reset temp state after create
                    pendingImageData = null;
                    pendingImageName = null;
                    pendingSelectedCategoryId = null;
                    pendingSelectedCategoryName = null;
                    pendingCategoryBtnRef = null;
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> {
                    // reset when cancel
                    pendingImageData = null;
                    pendingImageName = null;
                    pendingSelectedCategoryId = null;
                    pendingSelectedCategoryName = null;
                    pendingCategoryBtnRef = null;
                })
                .show();
    }

    // ================= Edit Dialog =================
    private void showEditDialog(Ingredient ing) {
        if (ing == null) return;

        // --- giữ trạng thái dialog ---
        pendingIsEdit = true;
        pendingEditIngredient = ing;

        // IMPORTANT: reset category temp state and initialize from current ingredient
        pendingSelectedCategoryId = ing.getCategoryId() != null ? ing.getCategoryId() : null;
        pendingSelectedCategoryName = null; // force lấy tên từ cachedCategories nếu có
        pendingCategoryBtnRef = null; // sẽ set lại bên dưới

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36,18,36,18);

        // --- Name ---
        final EditText nameInput = new EditText(this);
        nameInput.setHint("Name");
        nameInput.setText(ing.getName());
        layout.addView(nameInput);

        // --- Category (thay spinner bằng Button dài) ---
        final Button categoryButton = new Button(this);
        categoryButton.setAllCaps(false);
        categoryButton.setTextSize(16f);
        categoryButton.setPadding(16, 16, 16, 16);
        categoryButton.setBackgroundResource(android.R.drawable.btn_default);
        layout.addView(categoryButton);
        String displayText = "Select category";
        pendingCategoryBtnRef = categoryButton;

        // initialize display
        if (pendingSelectedCategoryId != null && categoriesLoaded) {
            for (IngredientCategory c : cachedCategories) {
                if (pendingSelectedCategoryId.equals(c.getId())) {
                    String icon = c.getIcon();
                    displayText = (icon != null && !icon.isEmpty()) ? icon + "  " + c.getName() : c.getName();
                    break;
                }
            }
        }
        categoryButton.setText(displayText);

        // open selector
        categoryButton.setOnClickListener(v -> {
            Intent it = new Intent(PantryActivity.this, SelectCategoryActivity.class);
            it.putExtra("currentCategoryId", pendingSelectedCategoryId);
            selectCategoryLauncher.launch(it);
        });

        // --- Quantity ---
        final EditText qtyInput = new EditText(this);
        qtyInput.setHint("Quantity");
        qtyInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        qtyInput.setText(String.valueOf(ing.getQuantity()));
        layout.addView(qtyInput);

        // --- Unit (Spinner instead of EditText) ---
        final Spinner unitSpinner = new Spinner(this);
        String[] unitOptions = new String[]{"g","kg","ml","l","piece","cup","tbsp","tsp"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitOptions);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);
        // set current selection if matches
        String currentUnit = ing.getUnit() != null ? ing.getUnit() : "";
        int selPos = 0;
        for (int i=0;i<unitOptions.length;i++) {
            if (unitOptions[i].equalsIgnoreCase(currentUnit)) { selPos = i; break; }
        }
        unitSpinner.setSelection(selPos);
        layout.addView(unitSpinner);

        // --- Expiry Date ---
        final EditText expiryInput = new EditText(this);
        expiryInput.setFocusable(false);
        expiryInput.setClickable(true);
        expiryInput.setHint("Expiry date (tap to pick)");
        layout.addView(expiryInput);

        final Calendar expiryCal = Calendar.getInstance();
        String existingExpiry = ing.getExpiryDate();
        if (existingExpiry != null && !existingExpiry.trim().isEmpty()) {
            try {
                SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                isoSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date parsed = isoSdf.parse(existingExpiry);
                if (parsed != null) {
                    Calendar localCal = Calendar.getInstance();
                    localCal.setTime(parsed);
                    expiryCal.setTime(localCal.getTime());
                    SimpleDateFormat displaySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    expiryInput.setText(displaySdf.format(localCal.getTime()));
                }
            } catch (Exception ignored) {}
        }

        expiryInput.setOnClickListener(v -> {
            int year = expiryCal.get(Calendar.YEAR);
            int month = expiryCal.get(Calendar.MONTH);
            int dayOfMonth = expiryCal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dpd = new DatePickerDialog(PantryActivity.this,
                    (view, y, m, dayOfMonthParam) -> {
                        expiryCal.set(y, m, dayOfMonthParam);
                        expiryInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(expiryCal.getTime()));
                    }, year, month, dayOfMonth);
            dpd.show();
        });

        expiryInput.setOnLongClickListener(v -> {
            expiryInput.setText("");
            expiryCal.setTimeInMillis(0);
            return true;
        });

        // --- Notes ---
        final EditText notesInput = new EditText(this);
        notesInput.setHint("Notes (optional)");
        notesInput.setText(ing.getNotes() != null ? ing.getNotes() : "");
        layout.addView(notesInput);

        // --- Image pick ---
        final ImageButton pickBtn = new ImageButton(this);
        pickBtn.setImageResource(android.R.drawable.ic_menu_gallery);
        layout.addView(pickBtn);

        pendingImageData = null;
        pendingImageName = null;

        if (ing.getImage() != null && !ing.getImage().isEmpty()) {
            loadImageFromServer(ing.getImage(), data -> {
                if (data != null) pendingImageData = data;
            });
        }

        pickBtn.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // --- Dialog builder ---
        new AlertDialog.Builder(this)
                .setTitle("Edit Ingredient")
                .setView(layout)
                .setPositiveButton("Save", (dialogInterface, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String categoryId = pendingSelectedCategoryId != null ? pendingSelectedCategoryId : "";

                    String qty = qtyInput.getText().toString().trim();
                    if (qty.isEmpty()) qty = "0";
                    String unit = (String) unitSpinner.getSelectedItem(); // <-- lấy unit từ spinner
                    String notes = notesInput.getText().toString().trim();

                    String expiry = expiryInput.getText().toString().trim();
                    if (!expiry.isEmpty()) {
                        try {
                            String[] parts = expiry.split("-");
                            int y = Integer.parseInt(parts[0]);
                            int m = Integer.parseInt(parts[1]);
                            int day = Integer.parseInt(parts[2]);

                            Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            utcCal.clear();
                            utcCal.set(y, m - 1, day, 0, 0, 0);
                            utcCal.set(Calendar.MILLISECOND, 0);

                            SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                            isoSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                            expiry = isoSdf.format(utcCal.getTime());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    if (name.isEmpty() || categoryId.isEmpty()) {
                        Toast.makeText(this, "Name and category required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, String> fields = new HashMap<>();
                    fields.put("ingredientId", ing.getId());
                    fields.put("name", name);
                    fields.put("categoryId", categoryId);
                    fields.put("quantity", qty);
                    fields.put("unit", unit);
                    if (!expiry.isEmpty()) fields.put("expiryDate", expiry);
                    fields.put("notes", notes);

                    String token = new SessionManager(PantryActivity.this).getToken();
                    IngredientApiService svc = new IngredientApiService(PantryActivity.this);

                    if (pendingImageData != null && pendingImageData.length > 0) {
                        svc.updateIngredientWithImage(token, fields, pendingImageData, new IngredientApiService.SimpleCallback() {
                            @Override public void onSuccess(JSONObject response) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PantryActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                                    loadIngredients();
                                });
                            }
                            @Override public void onError(String errorMessage) {
                                runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Update error: " + errorMessage, Toast.LENGTH_LONG).show());
                            }
                        });
                    } else {
                        // nếu muốn cho phép update không hình, bạn nên gọi api updateWithoutImage.
                        runOnUiThread(() -> Toast.makeText(PantryActivity.this, "No image selected!", Toast.LENGTH_SHORT).show());
                    }

                    // reset tạm
                    pendingImageData = null;
                    pendingIsEdit = false;
                    pendingEditIngredient = null;
                    pendingCategoryBtnRef = null;
                    pendingSelectedCategoryId = null;
                    pendingSelectedCategoryName = null;
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> {
                    pendingImageData = null;
                    pendingIsEdit = false;
                    pendingEditIngredient = null;
                    pendingCategoryBtnRef = null;
                    pendingSelectedCategoryId = null;
                    pendingSelectedCategoryName = null;
                })
                .show();
    }


    private long daysUntilExpiry(String expiryIso) {
        if (expiryIso == null || expiryIso.trim().isEmpty()) return Long.MAX_VALUE;
        try {
            // Parse ISO (stored as UTC instant)
            SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date expDate = isoSdf.parse(expiryIso);
            if (expDate == null) return Long.MAX_VALUE;

            // expiry in local calendar
            Calendar expLocal = Calendar.getInstance(); // default = local timezone
            expLocal.setTime(expDate);
            // normalize to local midnight of expiry date
            expLocal.set(Calendar.HOUR_OF_DAY, 0);
            expLocal.set(Calendar.MINUTE, 0);
            expLocal.set(Calendar.SECOND, 0);
            expLocal.set(Calendar.MILLISECOND, 0);

            // today local midnight
            Calendar todayLocal = Calendar.getInstance();
            todayLocal.set(Calendar.HOUR_OF_DAY, 0);
            todayLocal.set(Calendar.MINUTE, 0);
            todayLocal.set(Calendar.SECOND, 0);
            todayLocal.set(Calendar.MILLISECOND, 0);

            long diffMillis = expLocal.getTimeInMillis() - todayLocal.getTimeInMillis();
            return diffMillis / (24L * 60L * 60L * 1000L); // positive => days left; 0 => today; negative => expired
        } catch (Exception ex) {
            ex.printStackTrace();
            return Long.MAX_VALUE;
        }
    }


    // --- Hàm tải ảnh về byte[] từ server ---
    private void loadImageFromServer(String imageUrl, Consumer<byte[]> callback) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                okhttp3.Request request = new okhttp3.Request.Builder().url(imageUrl).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                byte[] data = response.body().bytes();
                runOnUiThread(() -> callback.accept(data));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> callback.accept(null));
            }
        }).start();
    }

    // ========== Delete confirm (ingredient) ==========
    private void confirmDelete(Ingredient ing) {
        if (ing == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete \"" + ing.getName() + "\"?")
                .setPositiveButton("Delete", (dialogInterface, which) -> {
                    String token = new SessionManager(PantryActivity.this).getToken();
                    IngredientApiService svc = new IngredientApiService(PantryActivity.this);

                    svc.deleteIngredient(token, ing.getId(), new IngredientApiService.DeleteCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(PantryActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                loadIngredients();
                            });
                        }
                        @Override
                        public void onError(String errorMessage) {
                            runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Delete error: " + errorMessage, Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> {})
                .show();
    }

    private String queryName(Uri uri) {
        String result = null;
        Cursor cursor = null;
        try {
            String[] projection = new String[]{OpenableColumns.DISPLAY_NAME};
            cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx != -1) {
                    result = cursor.getString(idx);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                try { cursor.close(); } catch (Exception ignored) {}
            }
        }
        return result;
    }

    // Adapter callbacks
    @Override
    public void onEdit(Ingredient ingredient) { showEditDialog(ingredient); }

    @Override
    public void onDelete(Ingredient ingredient) { confirmDelete(ingredient); }
}
