package com.example.cookmate.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
        holder.quantity.setText(qText + (item.getUnit() != null && !item.getUnit().isEmpty() ? " " + item.getUnit() : ""));

        // expiry display and color logic
        String expiryIso = item.getExpiryDate();
        if (expiryIso != null && !expiryIso.isEmpty()) {
            String displayDate = expiryIso.contains("T") ? expiryIso.split("T")[0] : expiryIso;
            holder.expiry.setText("Expires: " + displayDate);

            long days = daysUntilExpiry(expiryIso);
            // debug log (optional)
            // Log.d("PantryAdapter", "Ingredient " + item.getName() + " expiry=" + expiryIso + " days=" + days);

            if (days < 0) {
                // expired -> red
                holder.expiry.setTextColor(Color.parseColor("#D32F2F"));
            } else if (days <= 2) {
                // within 2 days -> orange
                holder.expiry.setTextColor(Color.parseColor("#F57C00"));
            } else {
                // normal -> dark text
                holder.expiry.setTextColor(Color.parseColor("#222222"));
            }
        } else {
            holder.expiry.setText("No expiry date");
            holder.expiry.setTextColor(Color.parseColor("#222222"));
        }

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
            ingredientImage = itemView.findViewById(R.id.ingredientImage);
        }
    }

    private long daysUntilExpiry(String expiryIso) {
        if (expiryIso == null || expiryIso.trim().isEmpty()) return Long.MAX_VALUE;
        try {
            // expecting ISO like "2025-10-21T00:00:00.000Z"
            SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date expDate = null;
            try {
                expDate = isoSdf.parse(expiryIso);
            } catch (Exception e) {
                // fallback: if server gave only date "yyyy-MM-dd"
                try {
                    SimpleDateFormat sdfDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    sdfDateOnly.setTimeZone(TimeZone.getTimeZone("UTC"));
                    expDate = sdfDateOnly.parse(expiryIso);
                } catch (Exception ex) {
                    // give up
                    expDate = null;
                }
            }
            if (expDate == null) return Long.MAX_VALUE;

            // convert to local calendar, normalize to local midnight of that date
            Calendar expLocal = Calendar.getInstance();
            expLocal.setTime(expDate);
            expLocal.set(Calendar.HOUR_OF_DAY, 0);
            expLocal.set(Calendar.MINUTE, 0);
            expLocal.set(Calendar.SECOND, 0);
            expLocal.set(Calendar.MILLISECOND, 0);

            Calendar todayLocal = Calendar.getInstance();
            todayLocal.set(Calendar.HOUR_OF_DAY, 0);
            todayLocal.set(Calendar.MINUTE, 0);
            todayLocal.set(Calendar.SECOND, 0);
            todayLocal.set(Calendar.MILLISECOND, 0);

            long diffMillis = expLocal.getTimeInMillis() - todayLocal.getTimeInMillis();
            return diffMillis / (24L * 60L * 60L * 1000L);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Long.MAX_VALUE;
        }
    }
}
