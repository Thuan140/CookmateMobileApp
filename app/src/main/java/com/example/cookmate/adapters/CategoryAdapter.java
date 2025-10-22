package com.example.cookmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.R;
import com.example.cookmate.models.IngredientCategory;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(IngredientCategory category);
        void onEditClick(IngredientCategory category);
    }

    private List<IngredientCategory> originalList = new ArrayList<>();
    private List<IngredientCategory> filteredList = new ArrayList<>();
    private final OnItemClickListener listener;

    public CategoryAdapter(List<IngredientCategory> list, OnItemClickListener l) {
        if (list != null) {
            originalList = new ArrayList<>(list);
            filteredList = new ArrayList<>(list);
        }
        listener = l;
    }

    public void updateData(List<IngredientCategory> list) {
        originalList = new ArrayList<>();
        if (list != null) originalList.addAll(list);
        filteredList = new ArrayList<>(originalList);
        notifyDataSetChanged();
    }

    public void filter(String q) {
        String s = (q == null) ? "" : q.toLowerCase().trim();
        filteredList.clear();
        if (s.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            for (IngredientCategory c : originalList) {
                if (c.getName() != null && c.getName().toLowerCase().contains(s)) {
                    filteredList.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        // Biến danh mục được gọi là 'c'
        final IngredientCategory c = filteredList.get(position); // 'final' (hoặc effectively final) là tốt nhất cho lambda

        holder.name.setText(c.getName());
        String ic = (c.getIcon() != null && !c.getIcon().isEmpty()) ? c.getIcon() : "🍽";
        holder.icon.setText(ic);

        // 1. Xử lý sự kiện click vào toàn bộ item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(c);
        });

        // 2. Xử lý sự kiện click vào nút chỉnh sửa, sử dụng 'holder.editBtn' và biến 'c'
        holder.editBtn.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(c);
        });

        // Dòng code gây lỗi đã được loại bỏ!
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        // Đã khai báo là 'editBtn'
        TextView icon, name;
        ImageButton editBtn;

        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.category_icon);
            name = itemView.findViewById(R.id.category_name);
            // Đã ánh xạ đúng ID trong item_category.xml
            editBtn = itemView.findViewById(R.id.edit_button);
        }
    }
}