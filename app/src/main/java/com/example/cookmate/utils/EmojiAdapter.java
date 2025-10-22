package com.example.cookmate.utils;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.cookmate.R;
import java.util.List;

public class EmojiAdapter extends BaseAdapter {

    private Context context;
    private List<String> emojiList;

    public EmojiAdapter(Context context, List<String> emojiList) {
        this.context = context;
        this.emojiList = emojiList;
    }

    public void updateData(List<String> newList) {
        emojiList.clear();
        emojiList.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return emojiList.size();
    }

    @Override
    public Object getItem(int position) {
        return emojiList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_emoji, parent, false);
        }

        TextView emojiText = view.findViewById(R.id.emojiText);
        emojiText.setText(emojiList.get(position));

        return view;
    }
}
