package com.example.cookmate.utils;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import com.example.cookmate.R;
import java.util.List;

public class EmojiPickerDialog {

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(String emoji);
    }

    public static void show(Context context, OnEmojiSelectedListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_emoji_picker);

        EditText searchBox = dialog.findViewById(R.id.searchBox);
        GridView emojiGrid = dialog.findViewById(R.id.emojiGrid);
        ImageButton btnClose = dialog.findViewById(R.id.btnClose);

        // Khởi tạo adapter
        List<String> emojis = EmojiData.getAll();
        EmojiAdapter adapter = new EmojiAdapter(context, emojis);
        emojiGrid.setAdapter(adapter);

        // Khi click chọn emoji
        emojiGrid.setOnItemClickListener((parent, view, position, id) -> {
            String emoji = emojis.get(position);
            listener.onEmojiSelected(emoji);
            dialog.dismiss();
        });

        // Tìm kiếm emoji
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                List<String> filtered = query.isEmpty() ? EmojiData.getAll() : EmojiData.search(query);
                adapter.updateData(filtered);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Nút đóng
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Căn giữa
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.CENTER);
        }

        dialog.show();
    }
}
