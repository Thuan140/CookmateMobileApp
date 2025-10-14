package com.example.cookmate;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.adapters.PantryAdapter;
import com.example.cookmate.models.Ingredient;
import com.example.cookmate.models.IngredientCategory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PantryActivity extends AppCompatActivity implements PantryAdapter.ActionListener {

    private RecyclerView recyclerView;
    private PantryAdapter adapter;
    private List<Ingredient> ingredientList = new ArrayList<>();
    private List<Ingredient> currentPageList = new ArrayList<>();

    private static final int ITEMS_PER_PAGE = 3;
    private int currentPage = 1;
    private int totalPages = 1;

    private ImageView btnPrev, btnNext;
    private LinearLayout pageContainer;
    private ImageButton buttonAdd;

    // categories cache
    private List<IngredientCategory> cachedCategories = new ArrayList<>();
    private boolean categoriesLoaded = false;

    // image pick
    private static final int REQUEST_PICK_IMAGE = 1501;
    private byte[] pendingImageData = null; // image chosen for currently open dialog
    private String pendingImageName = null;
    private boolean pendingIsEdit = false;
    private Ingredient pendingEditIngredient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantrylist);
        NavHelper.setupBottomNav(this, R.id.navigation_pantry);

        recyclerView = findViewById(R.id.ingredientsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryAdapter(this, currentPageList, this);
        recyclerView.setAdapter(adapter);

        btnPrev = findViewById(R.id.outline_arrow_back_ios_24);
        btnNext = findViewById(R.id.outline_arrow_forward_ios_24);
        pageContainer = findViewById(R.id.pageNumbersContainer);
        buttonAdd = findViewById(R.id.buttonAdd);

        setupPaginationButtons();
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
                out.add(new IngredientCategory(id, name));
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
                totalPages = Math.max(1, (int)Math.ceil((double)ingredientList.size()/ITEMS_PER_PAGE));
                runOnUiThread(() -> { createPageNumbers(); showPage(1); });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Load error: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ========== Pagination ==========
    private void setupPaginationButtons() {
        btnPrev.setOnClickListener(v -> { if (currentPage>1) showPage(currentPage-1); });
        btnNext.setOnClickListener(v -> { if (currentPage<totalPages) showPage(currentPage+1); });
    }

    private void createPageNumbers() {
        pageContainer.removeAllViews();
        for (int i=1;i<=totalPages;i++){
            final int p = i;
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(i));
            tv.setPadding(20,10,20,10);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8,0,8,0);
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
        int end = Math.min(start + ITEMS_PER_PAGE, ingredientList.size());
        currentPageList.clear();
        currentPageList.addAll(ingredientList.subList(start, end));
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

    // ========== Dialogs: Add ==========
    private void showAddDialog() {
        pendingImageData = null; pendingImageName = null; pendingIsEdit = false; pendingEditIngredient = null;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36,18,36,18);

        final EditText nameInput = new EditText(this); nameInput.setHint("Name"); layout.addView(nameInput);
        final Spinner spinnerCategory = new Spinner(this); layout.addView(spinnerCategory);
        final EditText qtyInput = new EditText(this); qtyInput.setHint("Quantity"); qtyInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(qtyInput);
        final EditText unitInput = new EditText(this); unitInput.setHint("Unit"); layout.addView(unitInput);
        final EditText expiryInput = new EditText(this); expiryInput.setHint("Expiry YYYY-MM-DD or ISO"); layout.addView(expiryInput);
        final EditText notesInput = new EditText(this); notesInput.setHint("Notes (optional)"); layout.addView(notesInput);

        final ImageButton pickBtn = new ImageButton(this);
        pickBtn.setImageResource(android.R.drawable.ic_menu_gallery);
        layout.addView(pickBtn);

        ArrayAdapter<IngredientCategory> spAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spAdapter);
        if (categoriesLoaded) { spAdapter.clear(); spAdapter.addAll(cachedCategories); spAdapter.notifyDataSetChanged(); }
        else { loadCategories(); }

        pickBtn.setOnClickListener(v -> {
            pendingIsEdit = false;
            pendingEditIngredient = null;
            pickImageFromGallery();
        });

        new AlertDialog.Builder(this)
                .setTitle("Add Ingredient")
                .setView(layout)
                .setPositiveButton("Create", (d,w) -> {
                    String name = nameInput.getText().toString().trim();
                    IngredientCategory sel = (IngredientCategory) spinnerCategory.getSelectedItem();
                    String categoryId = sel != null ? sel.getId() : "";
                    String qty = qtyInput.getText().toString().trim();
                    if (qty.isEmpty()) qty = "0";
                    String unit = unitInput.getText().toString().trim();
                    String expiry = expiryInput.getText().toString().trim();
                    if (expiry.matches("^\\d{4}-\\d{2}-\\d{2}$")) expiry = expiry + "T00:00:00.000Z";
                    String notes = notesInput.getText().toString().trim();

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
                            runOnUiThread(() -> { Toast.makeText(PantryActivity.this, "Created", Toast.LENGTH_SHORT).show(); loadIngredients(); });
                        }
                        @Override public void onError(String errorMessage) {
                            runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Create error: " + errorMessage, Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ========== Dialogs: Edit ==========
    private void showEditDialog(Ingredient ing) {
        if (ing == null) return;
        pendingImageData = null; pendingImageName = null; pendingIsEdit = true; pendingEditIngredient = ing;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36,18,36,18);

        final EditText nameInput = new EditText(this); nameInput.setText(ing.getName()); layout.addView(nameInput);
        final Spinner spinnerCategory = new Spinner(this); layout.addView(spinnerCategory);
        final EditText qtyInput = new EditText(this); qtyInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        qtyInput.setText(String.valueOf(ing.getQuantity())); layout.addView(qtyInput);
        final EditText unitInput = new EditText(this); unitInput.setText(ing.getUnit()); layout.addView(unitInput);
        final EditText expiryInput = new EditText(this); expiryInput.setText(ing.getExpiryDate()); layout.addView(expiryInput);
        final EditText notesInput = new EditText(this); notesInput.setText(ing.getNotes() != null ? ing.getNotes() : ""); layout.addView(notesInput);

        final ImageButton pickBtn = new ImageButton(this);
        pickBtn.setImageResource(android.R.drawable.ic_menu_gallery);
        layout.addView(pickBtn);

        ArrayAdapter<IngredientCategory> spAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spAdapter);

        if (categoriesLoaded) {
            spAdapter.clear(); spAdapter.addAll(cachedCategories); spAdapter.notifyDataSetChanged();
            // set selection using id
            String cur = ing.getCategoryId();
            if (cur != null && !cur.isEmpty()) {
                for (int i=0;i<cachedCategories.size();i++){
                    if (cur.equals(cachedCategories.get(i).getId())) { spinnerCategory.setSelection(i); break; }
                }
            }
        } else {
            loadCategories();
        }

        pickBtn.setOnClickListener(v -> {
            pendingIsEdit = true;
            pendingEditIngredient = ing;
            pickImageFromGallery();
        });

        new AlertDialog.Builder(this)
                .setTitle("Edit Ingredient")
                .setView(layout)
                .setPositiveButton("Save", (d,w) -> {
                    String name = nameInput.getText().toString().trim();
                    IngredientCategory sel = (IngredientCategory) spinnerCategory.getSelectedItem();
                    String categoryId = sel != null ? sel.getId() : "";
                    String qty = qtyInput.getText().toString().trim();
                    if (qty.isEmpty()) qty = "0";
                    String unit = unitInput.getText().toString().trim();
                    String expiry = expiryInput.getText().toString().trim();
                    if (expiry.matches("^\\d{4}-\\d{2}-\\d{2}$")) expiry = expiry + "T00:00:00.000Z";
                    String notes = notesInput.getText().toString().trim();

                    if (name.isEmpty() || categoryId.isEmpty()) {
                        Toast.makeText(this, "Name and category required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String,String> fields = new HashMap<>();
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
                                runOnUiThread(() -> { Toast.makeText(PantryActivity.this, "Updated", Toast.LENGTH_SHORT).show(); loadIngredients(); });
                            }
                            @Override public void onError(String errorMessage) {
                                runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Update error: " + errorMessage, Toast.LENGTH_LONG).show());
                            }
                        });
                    } else {
                        JSONObject body = new JSONObject();
                        try {
                            for (Map.Entry<String,String> e : fields.entrySet()) body.put(e.getKey(), e.getValue());
                        } catch (JSONException ex) { ex.printStackTrace(); }
                        svc.updateIngredientNoImage(token, body, new IngredientApiService.SimpleCallback() {
                            @Override public void onSuccess(JSONObject response) {
                                runOnUiThread(() -> { Toast.makeText(PantryActivity.this, "Updated", Toast.LENGTH_SHORT).show(); loadIngredients(); });
                            }
                            @Override public void onError(String errorMessage) {
                                runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Update error: " + errorMessage, Toast.LENGTH_LONG).show());
                            }
                        });
                    }
                    pendingImageData = null; pendingImageName = null; pendingIsEdit = false; pendingEditIngredient = null;
                })
                .setNegativeButton("Cancel", (dd,ww) -> {
                    pendingImageData = null; pendingImageName = null; pendingIsEdit = false; pendingEditIngredient = null;
                })
                .show();
    }

    // ========== Delete confirm ==========
    private void confirmDelete(Ingredient ing) {
        if (ing == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete \"" + ing.getName() + "\"?")
                .setPositiveButton("Delete", (d,w) -> {
                    JSONObject body = new JSONObject();
                    try { body.put("ingredientId", ing.getId()); } catch (JSONException e) { e.printStackTrace(); }

                    String token = new SessionManager(PantryActivity.this).getToken();
                    IngredientApiService svc = new IngredientApiService(PantryActivity.this);
                    svc.deleteIngredient(token, body, new IngredientApiService.DeleteCallback() {
                        @Override public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(PantryActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                loadIngredients();
                            });
                        }
                        @Override public void onError(String errorMessage) {
                            runOnUiThread(() -> Toast.makeText(PantryActivity.this, "Delete error: " + errorMessage, Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ========== Image picking ==========
    private void pickImageFromGallery() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Choose image"), REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && data != null && data.getData() != null) {
            Uri uri = data.getData();
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
