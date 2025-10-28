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

/**
 * Adapter cho MealPlanActivity
 * - click item => onEdit
 * - long click item => onDelete
 */
public class MealPlanAdapter extends RecyclerView.Adapter<MealPlanAdapter.VH> {

    public interface Callbacks {
        void onEdit(MealPlanItem item);
        void onDelete(MealPlanItem item);
    }

    private final Context ctx;
    private final Callbacks callbacks;
    private final List<MealPlanItem> items = new ArrayList<>();

    public MealPlanAdapter(Context ctx, Callbacks callbacks) {
        this.ctx = ctx;
        this.callbacks = callbacks;
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

        // Click = edit
        holder.itemView.setOnClickListener(v -> {
            if (callbacks != null) callbacks.onEdit(it);
        });

        // Long click = delete
        holder.itemView.setOnLongClickListener(v -> {
            if (callbacks != null) callbacks.onDelete(it);
            return true;
        });

        // (optional) you can display date/notes if you add views in item xml
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

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

    static class VH extends RecyclerView.ViewHolder {
        TextView mealName;
        ImageView mealImage;
        VH(@NonNull View itemView) {
            super(itemView);
            mealName = itemView.findViewById(R.id.mealName);
            mealImage = itemView.findViewById(R.id.mealImage);
        }
    }
}
