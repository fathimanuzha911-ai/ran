package com.mnpos.distribution.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mnpos.distribution.R;
import com.mnpos.distribution.model.MenuItem;

import java.util.List;
import java.util.function.Consumer;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.VH> {
    private final List<MenuItem> items;
    private final Consumer<MenuItem> onClick;

    public MenuAdapter(List<MenuItem> items, Consumer<MenuItem> onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MenuItem item = items.get(position);
        holder.label.setText(item.label);
        holder.itemView.setOnClickListener(v -> onClick.accept(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView label;
        VH(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.menuLabel);
        }
    }
}
