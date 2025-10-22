package com.example.cookmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmojiGridAdapter extends RecyclerView.Adapter<EmojiGridAdapter.VH> {
    private final List<String> items;
    private final java.util.function.Consumer<String> onClick;

    public EmojiGridAdapter(List<String> items, java.util.function.Consumer<String> onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        tv.setTextSize(28);
        tv.setGravity(android.view.Gravity.CENTER);
        int pad = (int)(8 * parent.getContext().getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        return new VH(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String emoji = items.get(position);
        holder.tv.setText(emoji);
        holder.tv.setOnClickListener(v -> onClick.accept(emoji));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(@NonNull View itemView) {
            super(itemView);
            tv = (TextView) itemView;
        }
    }
}
