package com.example.cookmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import android.widget.NumberPicker;
import android.widget.Button;
import android.widget.FrameLayout;

import com.example.cookmate.adapters.MealPlanAdapter;
import com.example.cookmate.models.FavoriteItem;
import com.example.cookmate.models.MealPlanItem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * MealPlanActivity với kéo-thả và lưu thứ tự tạm bằng SharedPreferences.
 */
public class MealPlanActivity extends AppCompatActivity implements MealPlanAdapter.Callbacks {
    private static final String TAG = "MealPlanActivity";
    private static final String BASE_URL = "https://cookm8.vercel.app/api/meal-plans";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final List<FavoriteItem> cachedFavorites = new ArrayList<>();

    private SessionManager sessionManager;
    private String authToken;

    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();

    private RecyclerView recyclerView;
    private MealPlanAdapter adapter;
    private ImageView backBtn;

    // Calendar UI
    private GridLayout calendarGrid;
    private TextView currentMonthYear;
    private TextView currentDateInfo;
    private ImageView btnPreviousMonth, btnNextMonth, btnResetMonth;

    // Data
    private final List<MealPlanItem> mealPlans = new ArrayList<>();
    private final Calendar calendar = Calendar.getInstance(); // used to build shown month
    private Calendar selectedCalendar = null; // day selected by user

    // Drag state
    private ItemTouchHelper itemTouchHelper;
    private String openMealPlanId;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_plan);

        // bottom nav helper (if available)
        try { NavHelper.setupBottomNav(this, R.id.navigation_mealplan); } catch (Exception ignored){}

        // views
        calendarGrid = findViewById(R.id.calendarGrid);
        currentMonthYear = findViewById(R.id.currentMonthYear);
        currentDateInfo = findViewById(R.id.currentDateInfo);

        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnResetMonth = findViewById(R.id.btnResetMonth);

        recyclerView = findViewById(R.id.mealPlanRecyclerView);
        backBtn = findViewById(R.id.back_button);
        openMealPlanId = getIntent().getStringExtra("mealPlanId");

        ImageView btnAdd = findViewById(R.id.btnAddMealPlan);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showAddDialog());
        }

        // back
        if (backBtn != null) backBtn.setOnClickListener(v -> { onBackPressed(); overridePendingTransition(0,0); });

        sessionManager = new SessionManager(this);
        authToken = sessionManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // adapter với id->title map (bắt đầu rỗng)
        adapter = new MealPlanAdapter(this, this, buildIdTitleMap());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // thiết lập ItemTouchHelper (kéo-thả)
        ItemTouchHelper.Callback cb = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            // xử lý di chuyển
            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
                int fromPos = from.getAdapterPosition();
                int toPos = to.getAdapterPosition();
                adapter.moveItem(fromPos, toPos);
                return true;
            }

            // không support swipe -> noop
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }

            // highlight khi kéo (tuỳ chỉnh)
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && selectedCalendar != null) {
                    // khi thao tác kết thúc -> lưu thứ tự hiện tại cho ngày đang chọn
                    saveOrderForDate(selectedCalendar, adapter.getItemsCopy());
                }
            }

            @Override
            public void clearView(RecyclerView rv, RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                // sau khi thả xong cũng lưu để chắc chắn
                if (selectedCalendar != null) saveOrderForDate(selectedCalendar, adapter.getItemsCopy());
            }
        };
        itemTouchHelper = new ItemTouchHelper(cb);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // date navigation clicks
        if (btnPreviousMonth != null) btnPreviousMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            buildCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
        });
        if (btnNextMonth != null) btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, +1);
            buildCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
        });
        if (btnResetMonth != null) btnResetMonth.setOnClickListener(v -> {
            calendar.setTimeInMillis(System.currentTimeMillis());
            buildCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
        });

        // initially selected day = today (set midday to avoid timezone shift)
        selectedCalendar = Calendar.getInstance();
        selectedCalendar.set(Calendar.HOUR_OF_DAY, 12);
        selectedCalendar.set(Calendar.MINUTE, 0);
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);

        // build initial calendar for current month
        buildCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));

        // show selected date info initially
        if (selectedCalendar != null && currentDateInfo != null) {
            SimpleDateFormat display = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            currentDateInfo.setText("Ngày: " + display.format(selectedCalendar.getTime()));
        }

        // load favorites into cache once (để hiển thị tên recipe)
        loadFavoritesCache();

        // fetch meal plans từ server
        fetchMealPlans();
    }

    // fetch meal plans
    private void fetchMealPlans() {
        Request req = new Request.Builder()
                .url(BASE_URL)
                .get()
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Không thể tải meal plans: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401) {
                    runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Phiên đăng nhập hết hạn", Toast.LENGTH_LONG).show());
                    return;
                }
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }
                String body = response.body() != null ? response.body().string() : null;
                parseMealPlansResponse(body);
            }
        });
    }

    private void parseMealPlansResponse(String body) {
        try {
            mealPlans.clear();
            if (body != null) {
                JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                if (root != null && root.has("mealPlans") && root.get("mealPlans").isJsonArray()) {
                    com.google.gson.JsonArray arr = root.getAsJsonArray("mealPlans");
                    for (com.google.gson.JsonElement el : arr) {
                        if (!el.isJsonObject()) continue;
                        com.google.gson.JsonObject o = el.getAsJsonObject();
                        String id = o.has("_id") ? o.get("_id").getAsString() : (o.has("id") ? o.get("id").getAsString() : "");
                        String name = o.has("name") ? o.get("name").getAsString() : "";
                        String notes = o.has("notes") ? o.get("notes").getAsString() : "";
                        String date = o.has("date") ? o.get("date").getAsString() : "";
                        List<String> recipeIds = new ArrayList<>();
                        if (o.has("recipeIds") && o.get("recipeIds").isJsonArray()) {
                            for (com.google.gson.JsonElement r : o.getAsJsonArray("recipeIds")) {
                                if (r.isJsonPrimitive()) recipeIds.add(r.getAsString());
                            }
                        }
                        MealPlanItem item = new MealPlanItem(id, name, recipeIds, notes, date);
                        mealPlans.add(item);
                    }
                }
            }

            runOnUiThread(() -> {
                buildCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
                if (selectedCalendar == null) selectedCalendar = Calendar.getInstance();
                showMealsForDate(selectedCalendar);
                if (openMealPlanId != null && !mealPlans.isEmpty()) {

                    MealPlanItem target = null;

                    for (MealPlanItem mp : mealPlans) {
                        if (mp.getId().equals(openMealPlanId)) {
                            target = mp;
                            break;
                        }
                    }

                    if (target != null && target.getDate() != null) {

                        try {

                            // Chuyển ngày từ ISO sang Calendar
                            SimpleDateFormat parseIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                            parseIso.setTimeZone(TimeZone.getTimeZone("UTC"));

                            Date parsed = parseIso.parse(target.getDate());

                            Calendar c = Calendar.getInstance();
                            c.setTime(parsed);

                            selectedCalendar = c;

                            // Rebuild calendar UI
                            buildCalendar(c.get(Calendar.YEAR), c.get(Calendar.MONTH));

                            // Load đúng danh sách theo ngày
                            showMealsForDate(c);

                            // Scroll đúng item trong adapter
                            recyclerView.post(() -> {

                                List<MealPlanItem> items = adapter.getItems();

                                for (int i = 0; i < items.size(); i++) {
                                    if (items.get(i).getId().equals(openMealPlanId)) {
                                        recyclerView.scrollToPosition(i);
                                        break;
                                    }
                                }
                            });

                        } catch (Exception ignored) {}
                    }
                }

            });
        } catch (Exception ex) {
            ex.printStackTrace();
            runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Lỗi phân tích dữ liệu", Toast.LENGTH_SHORT).show());
        }
    }

    // Build calendar (giữ nguyên logic hiển thị ô vuông)
    private void buildCalendar(int year, int month) {
        if (calendarGrid == null) return;
        calendarGrid.removeAllViews();

        int gridWidth = calendarGrid.getWidth();
        float density = getResources().getDisplayMetrics().density;
        int cellSizePx = gridWidth > 0 ? gridWidth / 7 : (int) (48 * density);

        // weekday headers
        String[] headers = new String[]{"CN","T2","T3","T4","T5","T6","T7"};
        for (String h : headers) {
            TextView hv = new TextView(this);
            hv.setText(h);
            hv.setGravity(Gravity.CENTER);
            hv.setPadding(0, (int)(8 * density), 0, (int)(8 * density));
            hv.setTextSize(14f);
            hv.setTypeface(hv.getTypeface(), android.graphics.Typeface.BOLD);
            hv.setTextColor(getResources().getColor(android.R.color.darker_gray));
            GridLayout.LayoutParams hlp = new GridLayout.LayoutParams();
            hlp.width = 0;
            hlp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            hlp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            hv.setLayoutParams(hlp);
            calendarGrid.addView(hv);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        Calendar tmp = Calendar.getInstance();
        tmp.set(year, month, 1);
        if (currentMonthYear != null) {
            currentMonthYear.setText(sdf.format(tmp.getTime()));
            currentMonthYear.setOnClickListener(v -> showDateWheelBottomSheet());
        }

        Calendar calFirst = Calendar.getInstance();
        calFirst.set(year, month, 1);
        int firstWeekday = calFirst.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = calFirst.getActualMaximum(Calendar.DAY_OF_MONTH);

        int blanks = firstWeekday - Calendar.SUNDAY;
        if (blanks < 0) blanks = 0;
        for (int i = 0; i < blanks; i++) {
            View placeholder = new View(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = cellSizePx;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            placeholder.setLayoutParams(lp);
            calendarGrid.addView(placeholder);
        }

        for (int d = 1; d <= daysInMonth; d++) {
            Calendar dayCal = Calendar.getInstance();
            dayCal.set(year, month, d, 0, 0, 0);
            dayCal.set(Calendar.MILLISECOND, 0);
            boolean hasPlan = hasMealPlanOn(dayCal);
            View dayCell = makeDayCell(d, hasPlan, dayCal, cellSizePx);
            calendarGrid.addView(dayCell);
        }

        if (selectedCalendar != null && currentDateInfo != null) {
            SimpleDateFormat display = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            currentDateInfo.setText("Ngày: " + display.format(selectedCalendar.getTime()));
        }
    }

    private boolean hasMealPlanOn(Calendar dayCal) {
        for (MealPlanItem mp : mealPlans) {
            if (mp == null || mp.getDate() == null) continue;
            try {
                SimpleDateFormat parseIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                parseIso.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date parsed = parseIso.parse(mp.getDate());
                if (parsed == null) continue;
                Calendar c = Calendar.getInstance();
                c.setTime(parsed);
                if (c.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
                        && c.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH)
                        && c.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private View makeDayCell(int dayNumber, boolean hasPlan, Calendar dayCal, int cellHeightPx) {
        TextView tv = new TextView(this);
        tv.setText(String.valueOf(dayNumber));
        tv.setGravity(Gravity.CENTER);
        int pad = (int) (6 * getResources().getDisplayMetrics().density);
        tv.setPadding(0, pad, 0, pad);
        tv.setClickable(true);
        tv.setFocusable(true);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = cellHeightPx > 0 ? cellHeightPx : (int) (64 * getResources().getDisplayMetrics().density);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(4, 4, 4, 4);
        tv.setLayoutParams(lp);

        Calendar today = Calendar.getInstance();
        if (today.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
                && today.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH)
                && today.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)) {
            tv.setBackgroundResource(R.drawable.bg_view_border);
        } else {
            tv.setBackgroundResource(android.R.color.transparent);
        }

        if (selectedCalendar != null
                && selectedCalendar.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
                && selectedCalendar.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH)
                && selectedCalendar.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)) {
            tv.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            tv.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            tv.setTextColor(getResources().getColor(android.R.color.black));
        }

        if (hasPlan) {
            tv.setText(tv.getText() + " •");
            tv.setCompoundDrawablePadding(6);
        }

        tv.setOnClickListener(v -> {
            selectedCalendar = (Calendar) dayCal.clone();
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 12);
            selectedCalendar.set(Calendar.MINUTE, 0);
            selectedCalendar.set(Calendar.SECOND, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);

            buildCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
            SimpleDateFormat display = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            currentDateInfo.setText("Ngày: " + display.format(selectedCalendar.getTime()));
            showMealsForDate(selectedCalendar);
        });

        return tv;
    }

    // show meals for a given date
    private void showMealsForDate(Calendar dayCal) {
        List<MealPlanItem> matches = new ArrayList<>();
        for (MealPlanItem mp : mealPlans) {
            if (mp == null || mp.getDate() == null) continue;
            try {
                SimpleDateFormat parseIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                parseIso.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date parsed = parseIso.parse(mp.getDate());
                if (parsed == null) continue;
                Calendar c = Calendar.getInstance();
                c.setTime(parsed);
                if (c.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
                        && c.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH)
                        && c.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)) {
                    matches.add(mp);
                }
            } catch (Exception ignored) {}
        }

        // apply saved order (if any) BEFORE setItems
        applySavedOrder(dayCal, matches);

        // set items on adapter
        adapter.setItems(matches);
    }

    // ========== Add / Edit dialogs (giữ nguyên logic trước đó; không hiển thị thời gian) ==========
    private void showAddDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_mealplan, null);
        final EditText etName = v.findViewById(R.id.etName);
        final Button btnSelectRecipes = v.findViewById(R.id.btnSelectRecipes);
        final TextView tvSelectedRecipes = v.findViewById(R.id.tvSelectedRecipes);
        final EditText etNotes = v.findViewById(R.id.etNotes);

        final List<String> selectedRecipeIds = new ArrayList<>();
        final List<String> selectedRecipeNames = new ArrayList<>();

        btnSelectRecipes.setOnClickListener(view -> {
            if (!cachedFavorites.isEmpty()) {
                showMultiSelectRecipesDialog(cachedFavorites, selectedRecipeIds, selectedRecipeNames, tvSelectedRecipes);
            } else {
                fetchFavoritesForSelection((favItems) -> {
                    if (favItems == null || favItems.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Không có favorites để chọn", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    runOnUiThread(() -> {
                        cachedFavorites.clear();
                        cachedFavorites.addAll(favItems);
                        adapter.setIdToTitleMap(buildIdTitleMap());
                        showMultiSelectRecipesDialog(favItems, selectedRecipeIds, selectedRecipeNames, tvSelectedRecipes);
                    });
                });
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Create meal plan")
                .setView(v)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String notes = etNotes.getText().toString().trim();

                    if (name.isEmpty()) { Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show(); return; }

                    Calendar useLocal = (selectedCalendar != null) ? (Calendar) selectedCalendar.clone() : Calendar.getInstance();
                    useLocal.set(Calendar.HOUR_OF_DAY, 12);
                    useLocal.set(Calendar.MINUTE, 0);
                    useLocal.set(Calendar.SECOND, 0);
                    useLocal.set(Calendar.MILLISECOND, 0);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    String date = sdf.format(useLocal.getTime());

                    JsonObject body = new JsonObject();
                    body.addProperty("name", name);
                    if (!notes.isEmpty()) body.addProperty("notes", notes);
                    body.addProperty("date", date);

                    com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                    for (String rid : selectedRecipeIds) arr.add(rid);
                    body.add("recipeIds", arr);

                    RequestBody rb = RequestBody.create(body.toString(), JSON);
                    Request req = new Request.Builder()
                            .url(BASE_URL)
                            .post(rb)
                            .addHeader("Authorization", "Bearer " + authToken)
                            .addHeader("Accept", "application/json")
                            .build();

                    client.newCall(req).enqueue(new Callback() {
                        @Override public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Thêm thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                        @Override public void onResponse(Call call, Response response) throws IOException {
                            if (!response.isSuccessful()) {
                                runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show());
                                return;
                            }
                            runOnUiThread(() -> {
                                Toast.makeText(MealPlanActivity.this, "Đã tạo meal plan", Toast.LENGTH_SHORT).show();
                                fetchMealPlans();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEdit(MealPlanItem item) {
        // duy trì logic trước — hiển thị dialog edit, thêm nút Delete (giữ nguyên như code trước)
        // để gọn, gọi lại phương thức cũ (bạn có code edit ở phiên bản trước để copy)
        // Nhưng ở đây ta chỉ gọi lại showAddDialog-like với prefilled values — implement theo file cũ
        // (để ngắn, mình giữ logic edit như bạn đã có; nếu cần, mình paste lại toàn bộ edit dialog)
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_mealplan, null);
        final EditText etName = v.findViewById(R.id.etName);
        final Button btnSelectRecipes = v.findViewById(R.id.btnSelectRecipes);
        final TextView tvSelectedRecipes = v.findViewById(R.id.tvSelectedRecipes);
        final EditText etNotes = v.findViewById(R.id.etNotes);

        final List<String> selectedRecipeIds = new ArrayList<>();
        final List<String> selectedRecipeNames = new ArrayList<>();

        etName.setText(item.getName());
        etNotes.setText(item.getNotes());
        if (item.getRecipeIds() != null) selectedRecipeIds.addAll(item.getRecipeIds());

        // map names
        if (!cachedFavorites.isEmpty()) {
            for (String rid : selectedRecipeIds) {
                for (FavoriteItem f : cachedFavorites) {
                    if (f.getId() != null && f.getId().equals(rid)) {
                        selectedRecipeNames.add(f.getTitle());
                        break;
                    }
                }
            }
            tvSelectedRecipes.setText(formatSelectedNames(selectedRecipeNames));
        } else {
            fetchFavoritesForSelection((favItems) -> {
                if (favItems == null) return;
                runOnUiThread(() -> {
                    cachedFavorites.clear();
                    cachedFavorites.addAll(favItems);
                    adapter.setIdToTitleMap(buildIdTitleMap());
                    selectedRecipeNames.clear();
                    for (String rid : selectedRecipeIds) {
                        for (FavoriteItem f : favItems) {
                            if (f.getId() != null && f.getId().equals(rid)) {
                                selectedRecipeNames.add(f.getTitle());
                                break;
                            }
                        }
                    }
                    tvSelectedRecipes.setText(formatSelectedNames(selectedRecipeNames));
                });
            });
        }

        btnSelectRecipes.setOnClickListener(view -> {
            if (!cachedFavorites.isEmpty()) {
                showMultiSelectRecipesDialog(cachedFavorites, selectedRecipeIds, selectedRecipeNames, tvSelectedRecipes);
            } else {
                fetchFavoritesForSelection((favItems) -> {
                    if (favItems == null || favItems.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Không có favorites để chọn", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    runOnUiThread(() -> {
                        cachedFavorites.clear();
                        cachedFavorites.addAll(favItems);
                        adapter.setIdToTitleMap(buildIdTitleMap());
                        showMultiSelectRecipesDialog(favItems, selectedRecipeIds, selectedRecipeNames, tvSelectedRecipes);
                    });
                });
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Edit meal plan")
                .setView(v)
                .setPositiveButton("Save", (d,w) -> {
                    String name = etName.getText().toString().trim();
                    String notes = etNotes.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show(); return; }

                    Calendar useLocal = (selectedCalendar != null) ? (Calendar) selectedCalendar.clone() : Calendar.getInstance();
                    useLocal.set(Calendar.HOUR_OF_DAY, 12);
                    useLocal.set(Calendar.MINUTE, 0);
                    useLocal.set(Calendar.SECOND, 0);
                    useLocal.set(Calendar.MILLISECOND, 0);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    String date = sdf.format(useLocal.getTime());

                    JsonObject body = new JsonObject();
                    body.addProperty("mealPlanId", item.getId());
                    body.addProperty("name", name);
                    if (!notes.isEmpty()) body.addProperty("notes", notes);
                    body.addProperty("date", date);

                    com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                    for (String rid : selectedRecipeIds) arr.add(rid);
                    body.add("recipeIds", arr);

                    RequestBody rb = RequestBody.create(body.toString(), JSON);
                    Request req = new Request.Builder()
                            .url(BASE_URL)
                            .put(rb)
                            .addHeader("Authorization", "Bearer " + authToken)
                            .addHeader("Accept", "application/json")
                            .build();
                    client.newCall(req).enqueue(new Callback() {
                        @Override public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                        @Override public void onResponse(Call call, Response response) throws IOException {
                            if (!response.isSuccessful()) {
                                runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show());
                                return;
                            }
                            runOnUiThread(() -> {
                                Toast.makeText(MealPlanActivity.this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                                fetchMealPlans();
                            });
                        }
                    });
                })
                .setNeutralButton("Delete", (d, w) -> {
                    new AlertDialog.Builder(MealPlanActivity.this)
                            .setTitle("Confirm delete")
                            .setMessage("Bạn có chắc muốn xoá meal plan này?")
                            .setPositiveButton("Delete", (dd, ww) -> {
                                JsonObject body = new JsonObject();
                                body.addProperty("mealPlanId", item.getId());
                                RequestBody rb = RequestBody.create(body.toString(), JSON);
                                Request req = new Request.Builder()
                                        .url(BASE_URL)
                                        .delete(rb)
                                        .addHeader("Authorization", "Bearer " + authToken)
                                        .addHeader("Accept", "application/json")
                                        .build();
                                client.newCall(req).enqueue(new Callback() {
                                    @Override public void onFailure(Call call, IOException e) {
                                        runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Xoá thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                    }
                                    @Override public void onResponse(Call call, Response response) throws IOException {
                                        if (!response.isSuccessful()) {
                                            runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show());
                                            return;
                                        }
                                        runOnUiThread(() -> {
                                            Toast.makeText(MealPlanActivity.this, "Đã xoá", Toast.LENGTH_SHORT).show();
                                            fetchMealPlans();
                                        });
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDelete(MealPlanItem item) {
        if (item == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete \"" + item.getName() + "\"?")
                .setPositiveButton("Delete", (d,w) -> {
                    JsonObject body = new JsonObject();
                    body.addProperty("mealPlanId", item.getId());
                    RequestBody rb = RequestBody.create(body.toString(), JSON);
                    Request req = new Request.Builder()
                            .url(BASE_URL)
                            .delete(rb)
                            .addHeader("Authorization", "Bearer " + authToken)
                            .addHeader("Accept", "application/json")
                            .build();
                    client.newCall(req).enqueue(new Callback() {
                        @Override public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Xoá thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                        @Override public void onResponse(Call call, Response response) throws IOException {
                            if (!response.isSuccessful()) {
                                runOnUiThread(() -> Toast.makeText(MealPlanActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show());
                                return;
                            }
                            runOnUiThread(() -> {
                                Toast.makeText(MealPlanActivity.this, "Đã xoá", Toast.LENGTH_SHORT).show();
                                fetchMealPlans();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ========== Favorites fetch (giữ nguyên) ==========
    private interface FavCallback { void onResult(List<FavoriteItem> items); }

    private void fetchFavoritesForSelection(FavCallback cb) {
        if (authToken == null || authToken.isEmpty()) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Bạn chưa đăng nhập (token missing)", Toast.LENGTH_SHORT).show();
                if (cb != null) cb.onResult(null);
            });
            return;
        }

        Request req = new Request.Builder()
                .url("https://cookm8.vercel.app/api/favorites")
                .get()
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.w(TAG, "GET favorites failed: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(MealPlanActivity.this, "Không thể tải favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (cb != null) cb.onResult(null);
                });
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String respBody = null;
                try { respBody = response.body() != null ? response.body().string() : null; }
                catch (Exception ex) { Log.e(TAG, "Read body error: " + ex.getMessage(), ex); }

                Log.i(TAG, "Favorites response code=" + response.code() + " body=" + respBody);

                if (response.code() == 401) {
                    runOnUiThread(() -> {
                        Toast.makeText(MealPlanActivity.this, "Phiên đăng nhập hết hạn (401) - hãy đăng nhập lại", Toast.LENGTH_LONG).show();
                        if (cb != null) cb.onResult(null);
                    });
                    return;
                }

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(MealPlanActivity.this, "Lỗi tải favorites: " + response.code(), Toast.LENGTH_SHORT).show();
                        if (cb != null) cb.onResult(null);
                    });
                    return;
                }

                try {
                    List<FavoriteItem> list = new ArrayList<>();
                    if (respBody != null) {
                        com.google.gson.JsonElement rootEl = JsonParser.parseString(respBody);
                        if (rootEl.isJsonObject()) {
                            JsonObject root = rootEl.getAsJsonObject();
                            if (root.has("recipes") && root.get("recipes").isJsonArray()) {
                                for (com.google.gson.JsonElement el : root.getAsJsonArray("recipes")) {
                                    if (!el.isJsonObject()) continue;
                                    com.google.gson.JsonObject r = el.getAsJsonObject();
                                    String id = r.has("id") ? r.get("id").getAsString() : "";
                                    String image = r.has("image") && !r.get("image").isJsonNull() ? r.get("image").getAsString() : "";
                                    String title = r.has("title") && !r.get("title").isJsonNull() ? r.get("title").getAsString() : "";
                                    Integer ready = r.has("readyInMinutes") && !r.get("readyInMinutes").isJsonNull() ? r.get("readyInMinutes").getAsInt() : null;
                                    Integer serves = r.has("servings") && !r.get("servings").isJsonNull() ? r.get("servings").getAsInt() : null;
                                    FavoriteItem it = new FavoriteItem(id, image, title, ready, serves);
                                    list.add(it);
                                }
                            } else if (root.has("favorites") && root.get("favorites").isJsonArray()) {
                                for (com.google.gson.JsonElement el : root.getAsJsonArray("favorites")) {
                                    if (!el.isJsonObject()) continue;
                                    com.google.gson.JsonObject r = el.getAsJsonObject();
                                    String id = r.has("id") ? r.get("id").getAsString() : "";
                                    String title = r.has("title") ? r.get("title").getAsString() : "";
                                    FavoriteItem it = new FavoriteItem(id, "", title, null, null);
                                    list.add(it);
                                }
                            } else if (root.has("data") && root.get("data").isJsonArray()) {
                                for (com.google.gson.JsonElement el : root.getAsJsonArray("data")) {
                                    if (!el.isJsonObject()) continue;
                                    com.google.gson.JsonObject r = el.getAsJsonObject();
                                    String id = r.has("id") ? r.get("id").getAsString() : "";
                                    String title = r.has("title") ? r.get("title").getAsString() : "";
                                    FavoriteItem it = new FavoriteItem(id, "", title, null, null);
                                    list.add(it);
                                }
                            }
                        } else if (rootEl.isJsonArray()) {
                            for (com.google.gson.JsonElement el : rootEl.getAsJsonArray()) {
                                if (!el.isJsonObject()) continue;
                                com.google.gson.JsonObject r = el.getAsJsonObject();
                                String id = r.has("id") ? r.get("id").getAsString() : "";
                                String title = r.has("title") ? r.get("title").getAsString() : "";
                                FavoriteItem it = new FavoriteItem(id, "", title, null, null);
                                list.add(it);
                            }
                        }
                    }
                    final List<FavoriteItem> finalList = list;
                    runOnUiThread(() -> {
                        if (cb != null) cb.onResult(finalList);
                    });
                } catch (Exception ex) {
                    Log.e(TAG, "Parse favs error: " + ex.getMessage(), ex);
                    runOnUiThread(() -> {
                        if (cb != null) cb.onResult(null);
                    });
                }
            }
        });
    }

    private void loadFavoritesCache() {
        fetchFavoritesForSelection(new FavCallback() {
            @Override
            public void onResult(List<FavoriteItem> items) {
                runOnUiThread(() -> {
                    if (items != null) {
                        cachedFavorites.clear();
                        cachedFavorites.addAll(items);
                        Log.i(TAG, "Cached favorites: " + items.size());
                        adapter.setIdToTitleMap(buildIdTitleMap());
                        adapter.setIdToImageMap(buildIdImageMap()); // ✅ thêm dòng này
                    } else {
                        Log.i(TAG, "No favorites cached (null).");
                    }
                });
            }
        });
    }


    // helper multi-select
    private void showMultiSelectRecipesDialog(java.util.List<FavoriteItem> favItems,
                                              java.util.List<String> selectedRecipeIds,
                                              java.util.List<String> selectedRecipeNames,
                                              TextView tvSelectedRecipes) {
        if (favItems == null || selectedRecipeIds == null || selectedRecipeNames == null || tvSelectedRecipes == null) return;

        selectedRecipeNames.clear();
        for (String id : selectedRecipeIds) {
            for (FavoriteItem f : favItems) {
                if (f.getId() != null && f.getId().equals(id)) {
                    selectedRecipeNames.add(f.getTitle());
                    break;
                }
            }
        }

        CharSequence[] names = new CharSequence[favItems.size()];
        boolean[] checked = new boolean[favItems.size()];
        for (int i = 0; i < favItems.size(); i++) {
            names[i] = favItems.get(i).getTitle() != null ? favItems.get(i).getTitle() : ("Recipe " + i);
            checked[i] = selectedRecipeIds.contains(favItems.get(i).getId());
        }

        runOnUiThread(() -> {
            new AlertDialog.Builder(MealPlanActivity.this)
                    .setTitle("Select recipes")
                    .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                        FavoriteItem chosen = favItems.get(which);
                        if (chosen == null) return;
                        String rid = chosen.getId();
                        String rname = chosen.getTitle();
                        if (isChecked) {
                            if (!selectedRecipeIds.contains(rid)) selectedRecipeIds.add(rid);
                            if (!selectedRecipeNames.contains(rname)) selectedRecipeNames.add(rname);
                        } else {
                            selectedRecipeIds.remove(rid);
                            selectedRecipeNames.remove(rname);
                        }
                    })
                    .setPositiveButton("OK", (d, w) -> tvSelectedRecipes.setText(formatSelectedNames(selectedRecipeNames)))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private String formatSelectedNames(java.util.List<String> names) {
        if (names == null || names.isEmpty()) return "No recipes selected";
        if (names.size() <= 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(names.get(i));
            }
            return sb.toString();
        } else {
            return names.size() + " items selected";
        }
    }

    // id->title map
    private Map<String,String> buildIdTitleMap() {
        Map<String,String> m = new HashMap<>();
        for (FavoriteItem f : cachedFavorites) {
            if (f != null && f.getId() != null) {
                m.put(f.getId(), f.getTitle() != null ? f.getTitle() : "");
            }
        }
        return m;
    }
    private Map<String,String> buildIdImageMap() {
        Map<String,String> m = new HashMap<>();
        for (FavoriteItem f : cachedFavorites) {
            if (f != null && f.getId() != null) {
                m.put(f.getId(), f.getImage() != null ? f.getImage() : "");
            }
        }
        return m;
    }

    // ===== LƯU & KHÔI PHỤC THỨ TỰ (SharedPreferences) =====
    private void saveOrderForDate(Calendar dayCal, List<MealPlanItem> ordered) {
        if (dayCal == null || ordered == null) return;
        String key = "mealplan_order_" + String.format(Locale.US, "%1$tY%1$tm%1$td", dayCal);
        ArrayList<String> ids = new ArrayList<>();
        for (MealPlanItem m : ordered) if (m != null && m.getId() != null) ids.add(m.getId());
        String json = new com.google.gson.Gson().toJson(ids);
        getSharedPreferences("mealplan_prefs", MODE_PRIVATE)
                .edit()
                .putString(key, json)
                .apply();
    }

    private void applySavedOrder(Calendar dayCal, List<MealPlanItem> list) {
        if (dayCal == null || list == null) return;
        String key = "mealplan_order_" + String.format(Locale.US, "%1$tY%1$tm%1$td", dayCal);
        String json = getSharedPreferences("mealplan_prefs", MODE_PRIVATE)
                .getString(key, null);
        if (json == null) return;

        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<String>>(){}.getType();
        List<String> idOrder = new com.google.gson.Gson().fromJson(json, type);
        if (idOrder == null || idOrder.isEmpty()) return;

        java.util.Map<String, MealPlanItem> map = new java.util.HashMap<>();
        for (MealPlanItem m : list) map.put(m.getId(), m);

        List<MealPlanItem> reordered = new ArrayList<>();
        for (String id : idOrder) {
            MealPlanItem m = map.remove(id);
            if (m != null) reordered.add(m);
        }
        // add remaining (new) items to end
        reordered.addAll(map.values());

        list.clear();
        list.addAll(reordered);
    }
    private int getDaysInMonth(int month0, int year) {
        Calendar tmp = Calendar.getInstance();
        tmp.set(Calendar.YEAR, year);
        tmp.set(Calendar.MONTH, month0);
        // đảm bảo đặt ngày vào 1 tránh edge-case khi ngày hiện tại > max của tháng mới
        tmp.set(Calendar.DAY_OF_MONTH, 1);
        return tmp.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    private void showDateWheelBottomSheet() {
        // Reuse same name so bạn không phải đổi chỗ gọi
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_date_wheel, null);
        final NumberPicker npDay = dialogView.findViewById(R.id.npDay);
        final NumberPicker npMonth = dialogView.findViewById(R.id.npMonth);
        final NumberPicker npYear = dialogView.findViewById(R.id.npYear);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelDate);
        Button btnOk = dialogView.findViewById(R.id.btnOkDate);

        // months display
        final String[] months = new String[12];
        for (int i = 0; i < 12; i++) months[i] = "TH" + (i + 1);

        int curYear = calendar.get(Calendar.YEAR);
        int minYear = Math.max(1900, curYear - 50);
        int maxYear = curYear + 50;

        int initDay = calendar.get(Calendar.DAY_OF_MONTH);
        int initMonth = calendar.get(Calendar.MONTH); // 0-based
        int initYear = calendar.get(Calendar.YEAR);

        // Year
        npYear.setMinValue(minYear);
        npYear.setMaxValue(maxYear);
        npYear.setValue(initYear);
        npYear.setWrapSelectorWheel(false);

        // Month
        npMonth.setMinValue(0);
        npMonth.setMaxValue(months.length - 1);
        npMonth.setDisplayedValues(months);
        npMonth.setValue(initMonth);
        npMonth.setWrapSelectorWheel(true);

        // Day
        int maxDay = getDaysInMonth(initMonth, initYear);
        npDay.setMinValue(1);
        npDay.setMaxValue(maxDay);
        npDay.setValue(Math.min(initDay, maxDay));
        npDay.setWrapSelectorWheel(true);

        NumberPicker.OnValueChangeListener onMonthYearChanged = (picker, oldVal, newVal) -> {
            int selectedMonth = npMonth.getValue();
            int selectedYear = npYear.getValue();
            int days = getDaysInMonth(selectedMonth, selectedYear);
            int cur = npDay.getValue();
            // cập nhật max; nếu cur > days thì set về days
            npDay.setMaxValue(days);
            if (cur > days) npDay.setValue(days);
        };
        npMonth.setOnValueChangedListener(onMonthYearChanged);
        npYear.setOnValueChangedListener(onMonthYearChanged);

        // Build AlertDialog (centered small dialog with dim)
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        // buttons
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            int chosenDay = npDay.getValue();
            int chosenMonth = npMonth.getValue(); // 0-based
            int chosenYear = npYear.getValue();

            if (selectedCalendar == null) selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(Calendar.YEAR, chosenYear);
            selectedCalendar.set(Calendar.MONTH, chosenMonth);
            int daysInChosen = getDaysInMonth(chosenMonth, chosenYear);
            if (chosenDay > daysInChosen) chosenDay = daysInChosen;
            selectedCalendar.set(Calendar.DAY_OF_MONTH, chosenDay);
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 12);
            selectedCalendar.set(Calendar.MINUTE, 0);
            selectedCalendar.set(Calendar.SECOND, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);

            calendar.set(Calendar.YEAR, chosenYear);
            calendar.set(Calendar.MONTH, chosenMonth);

            buildCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
            SimpleDateFormat display = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            if (currentDateInfo != null) currentDateInfo.setText("Ngày: " + display.format(selectedCalendar.getTime()));

            showMealsForDate(selectedCalendar);
            dialog.dismiss();
        });

        // Show then adjust window size & dim
        dialog.show();

        // Make dialog width smaller than full width and keep height wrap_content
        int widthPx = (int) (getResources().getDisplayMetrics().widthPixels * 0.86); // khoảng 86% width; thay đổi nếu muốn
        dialog.getWindow().setLayout(widthPx, FrameLayout.LayoutParams.WRAP_CONTENT);

        // Bo góc và nền của dialog (nếu dialog_date_wheel có background tròn thì ok)
        // Thêm dim amount (mặc định AlertDialog đã dim; nếu muốn khác, chỉnh như sau)
        android.view.WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.dimAmount = 0.6f; // làm mờ nền xung quanh
        dialog.getWindow().setAttributes(lp);
    }

}
