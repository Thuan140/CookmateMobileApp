//package com.example.cookmate;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.cardview.widget.CardView;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//
//import java.util.List;
//
//public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
//
//    private final Context context;
//    private List<Recipe> recipes;
//
//    public RecipeAdapter(Context context, List<Recipe> recipes) {
//        this.context = context;
//        this.recipes = recipes;
//    }
//
//    public void updateData(List<Recipe> newRecipes) {
//        this.recipes = newRecipes;
//        notifyDataSetChanged();
//    }
//
//    @Override
//    public RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe_list, parent, false);
//        return new RecipeViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(RecipeViewHolder holder, int position) {
//        Recipe recipe = recipes.get(position);
//        holder.recipeTitle.setText(recipe.getTitle());
//        holder.recipeDescription.setText(recipe.getSummary() != null ? recipe.getSummary() : "No description available");
//
//        Glide.with(context)
//                .load(recipe.getImage())
//                .placeholder(R.drawable.food)
//                .into(holder.recipeImage);
//    }
//
//    @Override
//    public int getItemCount() {
//        return recipes.size();
//    }
//
//    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
//        TextView recipeTitle, recipeDescription;
//        ImageView recipeImage;
//        CardView cardView;
//
//        public RecipeViewHolder(View itemView) {
//            super(itemView);
//            recipeTitle = itemView.findViewById(R.id.recipeTitle);
//            recipeDescription = itemView.findViewById(R.id.recipeDescription);
//            recipeImage = itemView.findViewById(R.id.imageView3);
//            cardView = (CardView) itemView;
//        }
//    }
//}
package com.example.cookmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private Context context;
    private List<Recipe> recipeList;

    public RecipeAdapter(Context context, List<Recipe> recipeList) {
        this.context = context;
        this.recipeList = recipeList;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe_list, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);

        holder.recipeName.setText(recipe.getTitle() != null ? recipe.getTitle() : "Unknown Recipe");

        // ✅ Xử lý ảnh rỗng/null: hiển thị ảnh mặc định food.png
        String imageUrl = recipe.getImage();
        if (imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.equals("[]")) {
            Glide.with(context)
                    .load(R.drawable.food) // ảnh mặc định
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.recipeImage);
        } else {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.food) // hiển thị trong khi tải
                    .error(R.drawable.food) // lỗi thì vẫn hiển thị mặc định
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.recipeImage);
        }
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    // ✅ Hàm cập nhật dữ liệu
    public void updateData(List<Recipe> newList) {
        this.recipeList.clear();
        this.recipeList.addAll(newList);
        notifyDataSetChanged();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView recipeImage;
        TextView recipeName;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeName = itemView.findViewById(R.id.recipeTitle);
        }
    }
}
