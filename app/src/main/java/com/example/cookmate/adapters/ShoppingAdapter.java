package com.example.cookmate.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.R;
import com.example.cookmate.models.ShoppingItem;

import java.util.ArrayList;
import java.util.List;

public class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.VH> {

    public interface Callbacks {
        void onDeleteClicked(ShoppingItem item);
        void onEditClicked(ShoppingItem item);
        void onCheckChanged(ShoppingItem item, boolean checked);
    }

    private final Context ctx;
    private final List<ShoppingItem> items = new ArrayList<>();
    private final Callbacks callbacks;

    public ShoppingAdapter(Context ctx, Callbacks callbacks) {
        this.ctx = ctx;
        this.callbacks = callbacks;
    }

    public void setItems(List<ShoppingItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void addItem(ShoppingItem item) {
        items.add(0, item);
        notifyItemInserted(0);
    }

    public void removeItemById(String id) {
        for (int i = 0; i < items.size(); i++) {
            ShoppingItem it = items.get(i);
            if (it.getId() != null && it.getId().equals(id)) {
                items.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.activity_item_shoppinglist, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ShoppingItem item = items.get(position);
        holder.text.setText(item.getName() == null ? "" : item.getName());
        // check box state: treat status "done" or "purchased" as checked (you can adapt)
        boolean checked = "done".equalsIgnoreCase(item.getStatus()) || "purchased".equalsIgnoreCase(item.getStatus());
        holder.checkBox.setChecked(checked);

        holder.deleteIcon.setOnClickListener(v -> {
            new AlertDialog.Builder(ctx)
                    .setTitle("Delete")
                    .setMessage("Delete \"" + item.getName() + "\"?")
                    .setPositiveButton("Delete", (d, w) -> {
                        if (callbacks != null) callbacks.onDeleteClicked(item);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        holder.editIcon.setOnClickListener(v -> {
            if (callbacks != null) callbacks.onEditClicked(item);
        });

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Avoid firing on recycled views initial set: only notify if callback exists
            if (callbacks != null) callbacks.onCheckChanged(item, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView text;
        ImageView editIcon;
        ImageView deleteIcon;

        public VH(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxItem1);
            text = itemView.findViewById(R.id.textViewItem1);
            editIcon = itemView.findViewById(R.id.editIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }
}
