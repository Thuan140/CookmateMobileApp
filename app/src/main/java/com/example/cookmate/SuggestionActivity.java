package com.example.cookmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.models.FavoriteItem;
import com.example.cookmate.models.MealPlanItem;
import com.example.cookmate.models.ShoppingItem;

import java.util.ArrayList;
import java.util.List;

public class SuggestionActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText inputEditText;
    private ImageButton sendButton, backButton;

    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private AISuggestionService aiService;

    private String authToken;

    private SessionManager sessionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestion);

        NavHelper.setupBottomNav(this, R.id.navigation_suggestion);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        inputEditText = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);

        messages = new ArrayList<>();
        adapter = new ChatAdapter(
                this,
                messages,
                new ChatAdapter.OnMealPlanClickListener() {

                    @Override
                    public void onMealPlanClick(MealPlanItem item, View view) {
                        Intent i = new Intent(SuggestionActivity.this, MealPlanActivity.class);
                        i.putExtra("mealPlanId", item.getId());
                        startActivity(i);
                    }

                    @Override
                    public void onProfileClick(User user) {
                        Intent i = new Intent(SuggestionActivity.this, ProfileActivity.class);
                        i.putExtra("userId", user.getId());
                        i.putExtra("email", user.getEmail());
                        i.putExtra("name", user.getName());
                        i.putExtra("avatar", user.getAvatar());
                        startActivity(i);
                    }

                    @Override
                    public void onShoppingItemClick(ShoppingItem item) {
                        Intent i = new Intent(SuggestionActivity.this, ShoppingListActivity.class);
                        i.putExtra("scrollToId", item.getId());
                        startActivity(i);
                    }

                    @Override
                    public void onFavoriteClick(FavoriteItem item) {
                        Intent i = new Intent(SuggestionActivity.this, FavoriteListActivity.class);
                        i.putExtra("scrollToId", item.getId());
                        startActivity(i);
                    }

                }
        );

        sessionManager = new SessionManager(this);
        authToken = sessionManager.getToken();

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);

        aiService = new AISuggestionService(this);

        sendButton.setOnClickListener(v -> sendMessage());

        OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);
        backButton.setOnClickListener(v -> backCallback.handleOnBackPressed());
    }

    private void sendMessage() {
        String text = inputEditText.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage userMsg = new ChatMessage("user", text);
        messages.add(userMsg);
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);
        inputEditText.setText("");

        aiService.sendChat(messages, new AISuggestionService.AICallback() {

            @Override
            public void onText(String reply) {
                ChatMessage ai = new ChatMessage("assistant", reply);
                messages.add(ai);

                runOnUiThread(() -> {
                    adapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onMealPlans(List<MealPlanItem> list) {

                for (MealPlanItem item : list) {
                    messages.add(new ChatMessage(item));
                }

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onUserProfile(User user) {
                messages.add(new ChatMessage(user));

                runOnUiThread(() -> {
                    adapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onShoppingList(List<ShoppingItem> list) {
                for (ShoppingItem item : list) {
                    messages.add(new ChatMessage(item));
                }

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onFavorites(List<FavoriteItem> list) {

                for (FavoriteItem item : list) {
                    messages.add(new ChatMessage(item));
                }

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onError(String error) {
                ChatMessage ai = new ChatMessage("assistant", " " + error);
                messages.add(ai);

                runOnUiThread(() -> {
                    adapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                });
            }
        });
    }

}
