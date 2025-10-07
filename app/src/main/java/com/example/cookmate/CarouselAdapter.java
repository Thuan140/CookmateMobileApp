package com.example.cookmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.ViewHolder> {
    private List<Integer> items; // list of drawable resource ids
    private Context context;

    public CarouselAdapter(Context ctx, List<Integer> items) {
        this.context = ctx;
        this.items = items;
    }

    @NonNull
    @Override
    public CarouselAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_carousel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselAdapter.ViewHolder holder, int position) {
        int resId = items.get(position);
        holder.image.setImageResource(resId);
        // nếu cần set click listener: holder.itemView.setOnClickListener(...)
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
        }
    }
}
