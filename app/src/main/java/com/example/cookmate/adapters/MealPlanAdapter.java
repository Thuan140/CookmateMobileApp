package com.example.cookmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.R;
import com.example.cookmate.models.MealPlanItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

/**
 * Adapter cho MealPlanActivity — có hỗ trợ moveItem để kéo-thả.
 */
public class MealPlanAdapter extends RecyclerView.Adapter<MealPlanAdapter.VH> {

    public interface Callbacks {
        void onEdit(MealPlanItem item);
        void onDelete(MealPlanItem item);
    }

    private final Context ctx;
    private final Callbacks callbacks;
    private final List<MealPlanItem> items = new ArrayList<>();

    // map recipeId -> recipeTitle
    private Map<String, String> idToTitle;

    // map recipeId -> imageUrl (thumbnail)
    private Map<String, String> idToImage = new HashMap<>();

    public MealPlanAdapter(Context ctx, Callbacks callbacks, Map<String, String> idToTitle) {
        this.ctx = ctx;
        this.callbacks = callbacks;
        this.idToTitle = idToTitle;
    }

    public void setIdToTitleMap(Map<String, String> map) {
        this.idToTitle = map;
        notifyDataSetChanged();
    }

    // setter mới cho ảnh
    public void setIdToImageMap(Map<String, String> map) {
        if (map == null) map = new HashMap<>();
        this.idToImage = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_meal_plan_entry, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MealPlanItem it = items.get(position);
        holder.mealName.setText(it.getName() != null ? it.getName() : "No name");

        // Build recipe names string using id->title map if available
        if (it.getRecipeIds() != null && !it.getRecipeIds().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String rid : it.getRecipeIds()) {
                String t = (idToTitle != null && idToTitle.containsKey(rid)) ? idToTitle.get(rid) : null;
                if (t == null || t.isEmpty()) t = rid; // fallback show id
                if (count > 0) sb.append(", ");
                sb.append(t);
                count++;
                if (count >= 5) { // avoid too long string
                    if (it.getRecipeIds().size() > 5) sb.append("...");
                    break;
                }
            }
            holder.recipeName.setText(sb.toString());
        } else {
            holder.recipeName.setText("No recipes selected");
        }

        // Load thumbnail from first recipeId (if available)
        String thumbUrl = null;
        if (it.getRecipeIds() != null && !it.getRecipeIds().isEmpty()) {
            for (String rid : it.getRecipeIds()) {
                if (rid != null && idToImage != null) {
                    String maybe = idToImage.get(rid);
                    if (maybe != null && !maybe.isEmpty()) {
                        thumbUrl = maybe;
                        break; // ✅ dừng ngay khi gặp ảnh hợp lệ
                    }
                }
            }
        }

        // Use Glide to load image with rounded corners and placeholder
        RequestOptions opts = new RequestOptions()
                .transform(new RoundedCorners((int) (6 * ctx.getResources().getDisplayMetrics().density)))
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder);

        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            Glide.with(holder.mealImage.getContext())
                    .load(thumbUrl)
                    .apply(opts)
                    .centerCrop()
                    .into(holder.mealImage);
        } else {
            // fallback placeholder
            holder.mealImage.setImageResource(R.drawable.ic_placeholder);
        }

        // Click = edit
        holder.itemView.setOnClickListener(v -> {
            if (callbacks != null) callbacks.onEdit(it);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // set toàn bộ danh sách (thay thế)
    public void setItems(List<MealPlanItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void addItem(MealPlanItem it) {
        if (it == null) return;
        items.add(0, it);
        notifyItemInserted(0);
    }

    public void removeById(String id) {
        if (id == null) return;
        for (int i = 0; i < items.size(); i++) {
            if (id.equals(items.get(i).getId())) {
                items.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    // ===== move item (kéo-thả) =====
    public void moveItem(int fromPos, int toPos) {
        if (fromPos < 0 || toPos < 0 || fromPos >= items.size() || toPos >= items.size()) return;
        MealPlanItem it = items.remove(fromPos);
        items.add(toPos, it);
        notifyItemMoved(fromPos, toPos);
    }

    // trả về copy của danh sách hiện tại (dùng để lưu order)
    public List<MealPlanItem> getItemsCopy() {
        return new ArrayList<>(items);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView mealName;
        TextView recipeName;
        ImageView mealImage;
        VH(@NonNull View itemView) {
            super(itemView);
            mealName = itemView.findViewById(R.id.mealName);
            recipeName = itemView.findViewById(R.id.recipe_name);
            mealImage = itemView.findViewById(R.id.mealImage);
        }
    }
}
