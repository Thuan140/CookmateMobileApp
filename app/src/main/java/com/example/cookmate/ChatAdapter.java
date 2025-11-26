package com.example.cookmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cookmate.models.FavoriteItem;
import com.example.cookmate.models.MealPlanItem;
import com.example.cookmate.models.ShoppingItem;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_MEAL_PLAN = 3;
    private static final int VIEW_TYPE_PROFILE = 4;
    private static final int VIEW_TYPE_SHOPPING = 5;

    private static final int VIEW_TYPE_FAVORITE = 6;


    private Context context;
    private List<ChatMessage> messages;
    public interface OnMealPlanClickListener {
        void onMealPlanClick(MealPlanItem item, View itemView);
        void onProfileClick(User user);
        void onShoppingItemClick(ShoppingItem item);
        void onFavoriteClick(FavoriteItem item);

    }

    private OnMealPlanClickListener listener;

    public ChatAdapter(Context context, List<ChatMessage> list, OnMealPlanClickListener listener) {
        this.context = context;
        this.messages = list;
        this.listener = listener;
    }

    public ChatAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);

        if (msg.isUserProfile()) return VIEW_TYPE_PROFILE;
        if (msg.isMealPlanItem()) return VIEW_TYPE_MEAL_PLAN;
        if (msg.isShoppingItem()) return VIEW_TYPE_SHOPPING;
        if (msg.isFavoriteItem()) return VIEW_TYPE_FAVORITE;

        return msg.getRole().equals("user") ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        }
        if (viewType == VIEW_TYPE_MEAL_PLAN) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_meal_plan_entry, parent, false);
            return new MealPlanHolder(view);
        }
        if (viewType == VIEW_TYPE_PROFILE) {
            View v = LayoutInflater.from(context)
                    .inflate(R.layout.item_profile_entry, parent, false);
            return new ProfileHolder(v);
        }
        if (viewType == VIEW_TYPE_SHOPPING) {
            View v = LayoutInflater.from(context)
                    .inflate(R.layout.activity_item_shoppinglist, parent, false);
            return new ShoppingHolder(v);
        }
        if (viewType == VIEW_TYPE_FAVORITE) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.activity_item_favoritelist, parent, false);
            return new FavoriteHolder(view);
        }
        else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).messageText.setText(msg.getContent());
        }
        else if (holder instanceof MealPlanHolder) {
            MealPlanItem item = messages.get(position).getMealPlanItem();
            ((MealPlanHolder) holder).bind(item);
        }
        else if (holder instanceof ProfileHolder) {
            ((ProfileHolder) holder).bind(msg.getUser());
        }
        else if (holder instanceof ShoppingHolder) {
            ((ShoppingHolder) holder).bind(msg.getShoppingItem());
        }
        else if (holder instanceof FavoriteHolder) {
            ((FavoriteHolder) holder).bind(msg.getFavoriteItem());
        }
        else {
            ((ReceivedViewHolder) holder).messageText.setText(msg.getContent());
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    public static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }

    public static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }
public class MealPlanHolder extends RecyclerView.ViewHolder {

    TextView mealName, recipeName;
    ImageView mealImage;

    public MealPlanHolder(@NonNull View itemView) {
        super(itemView);
        mealName = itemView.findViewById(R.id.mealName);
        recipeName = itemView.findViewById(R.id.recipe_name);
        mealImage = itemView.findViewById(R.id.mealImage);
    }

    public void bind(MealPlanItem item) {
        mealName.setText(item.getName());

        if (item.getRecipeIds() != null && !item.getRecipeIds().isEmpty()) {
            recipeName.setText("ID: " + item.getRecipeIds().get(0));
        } else {
            recipeName.setText("Không có món");
        }

        itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMealPlanClick(item, v);
            }
        });
    }
}

    public class ShoppingHolder extends RecyclerView.ViewHolder {

        CheckBox checkbox;
        TextView textView;
        ImageView editIcon, deleteIcon;

        public ShoppingHolder(@NonNull View itemView) {
            super(itemView);

            checkbox = itemView.findViewById(R.id.checkboxItem1);
            textView = itemView.findViewById(R.id.textViewItem1);
            editIcon = itemView.findViewById(R.id.editIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }

        public void bind(ShoppingItem item) {
            textView.setText(item.getName());
            checkbox.setChecked(item.getStatus().equals("done"));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onShoppingItemClick(item);
            });
        }
    }

    public class ProfileHolder extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView name, email;

        public ProfileHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.profile_avatar);
            name = itemView.findViewById(R.id.profile_name);
            email = itemView.findViewById(R.id.profile_email);
        }

        public void bind(User user) {
            name.setText(user.getName());
            email.setText(user.getEmail());

            Glide.with(itemView.getContext())
                    .load(user.getAvatar())
                    .into(avatar);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProfileClick(user);
            });
        }
    }

    public class FavoriteHolder extends RecyclerView.ViewHolder {

        ImageView recipeImage;
        TextView recipeTitle, recipeReadyIn, recipeServes;
        ImageButton removeBtn;

        public FavoriteHolder(@NonNull View itemView) {
            super(itemView);

            recipeImage     = itemView.findViewById(R.id.recipe_image);
            recipeTitle     = itemView.findViewById(R.id.recipe_title);
            recipeReadyIn   = itemView.findViewById(R.id.recipe_ready_in);
            recipeServes    = itemView.findViewById(R.id.recipe_serves);
            removeBtn       = itemView.findViewById(R.id.btn_remove_favorite);
        }

        public void bind(FavoriteItem item) {
            recipeTitle.setText(item.getTitle());

            recipeReadyIn.setText(
                    "Ready in: " +
                            (item.getReadyInMinutes() != null ? item.getReadyInMinutes() + " mins" : "N/A")
            );

            recipeServes.setText(
                    "Serves: " +
                            (item.getServings() != null ? item.getServings() : "N/A")
            );

            // Load image
            Glide.with(itemView.getContext())
                    .load(item.getImage())
                    .placeholder(R.drawable.food)
                    .into(recipeImage);

            // Click item để mở FavoriteListActivity
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFavoriteClick(item);
            });

            // Nút remove: trong Chat không xóa – để disable
            removeBtn.setVisibility(View.GONE);
        }
    }


}
