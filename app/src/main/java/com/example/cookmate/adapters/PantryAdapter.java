package com.example.cookmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cookmate.R;
import com.example.cookmate.models.Ingredient;

import java.util.List;

public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.VH> {

    public interface ActionListener {
        void onEdit(Ingredient ingredient);
        void onDelete(Ingredient ingredient);
    }

    private final Context ctx;
    private List<Ingredient> list;
    private final ActionListener listener;

    public PantryAdapter(Context ctx, List<Ingredient> list, ActionListener listener) {
        this.ctx = ctx;
        this.list = list;
        this.listener = listener;
    }

    public void updateList(List<Ingredient> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.activity_item_pantrylist, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Ingredient item = list.get(position);

        holder.name.setText(item.getName() != null ? item.getName() : "No name");

        double q = item.getQuantity();
        String qText = (q == (long) q) ? String.valueOf((long) q) : String.valueOf(q);
        holder.quantity.setText(qText + (item.getUnit() != null ? " " + item.getUnit() : ""));

        holder.expiry.setText(item.getExpiryDate() != null && !item.getExpiryDate().isEmpty()
                ? "Expires: " + item.getExpiryDate().split("T")[0]
                : "No expiry date");

        // load image (if any)
        if (item.getImage() != null && !item.getImage().isEmpty()) {
            Glide.with(ctx)
                    .load(item.getImage())
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(holder.ingredientImage);
        } else {
            holder.ingredientImage.setImageResource(R.drawable.ic_placeholder);
        }

        // edit button
        holder.editIcon.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });

        // delete button
        holder.deleteIcon.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });

        // radio button optional
        holder.radioButton.setOnClickListener(v -> { /* selection */ });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        RadioButton radioButton;
        TextView name, quantity, expiry;
        ImageView editIcon, deleteIcon, ingredientImage;

        VH(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.ingredientRadioButton);
            name = itemView.findViewById(R.id.ingredientName);
            quantity = itemView.findViewById(R.id.ingredientQuantity);
            expiry = itemView.findViewById(R.id.ingredientExpiryDate);
            editIcon = itemView.findViewById(R.id.editIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
            ingredientImage = itemView.findViewById(R.id.ingredientImage); // ensure this id exists in layout
        }
    }
}
