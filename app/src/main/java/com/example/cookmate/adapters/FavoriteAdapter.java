package com.example.cookmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.R;
import com.example.cookmate.models.FavoriteItem;

import java.util.ArrayList;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.VH> {

    public interface Callbacks {
        void onRemoveClicked(FavoriteItem item);
        void onItemClicked(FavoriteItem item);
    }

    private final Context ctx;
    private final List<FavoriteItem> items = new ArrayList<>();
    private final Callbacks callbacks;

    public FavoriteAdapter(Context ctx, Callbacks callbacks) {
        this.ctx = ctx;
        this.callbacks = callbacks;
    }

    public void setItems(List<FavoriteItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void removeById(String id) {
        for (int i = 0; i < items.size(); i++) {
            FavoriteItem it = items.get(i);
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
        View v = LayoutInflater.from(ctx).inflate(R.layout.activity_item_favoritelist, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FavoriteItem it = items.get(position);
        holder.title.setText(it.getTitle() != null ? it.getTitle() : "");
        holder.desc.setText(it.getCreator() != null && !it.getCreator().isEmpty() ? it.getCreator() : "Unknown");


        // TODO: load image with Glide/Picasso if imageUrl present. For now use default src in layout.
        holder.removeBtn.setOnClickListener(v -> {
            if (callbacks != null) callbacks.onRemoveClicked(it);
        });

        holder.itemView.setOnClickListener(v -> {
            if (callbacks != null) callbacks.onItemClicked(it);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, desc;
        ImageButton removeBtn;
        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.recipe_image);
            title = itemView.findViewById(R.id.recipe_title);
            desc = itemView.findViewById(R.id.recipe_description);
            removeBtn = itemView.findViewById(R.id.btn_remove_favorite);
        }
    }
}
