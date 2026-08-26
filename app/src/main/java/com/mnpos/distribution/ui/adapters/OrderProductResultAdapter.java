package com.mnpos.distribution.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mnpos.distribution.R;
import com.mnpos.distribution.model.OrderProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OrderProductResultAdapter extends RecyclerView.Adapter<OrderProductResultAdapter.VH> {
    private final List<OrderProduct> items = new ArrayList<>();
    private final Consumer<OrderProduct> onPick;

    public OrderProductResultAdapter(Consumer<OrderProduct> onPick) {
        this.onPick = onPick;
    }

    public void replace(List<OrderProduct> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        OrderProduct item = items.get(position);
        holder.name.setText(item.name);
        holder.sku.setText(item.sku + "  •  Rs " + String.format(java.util.Locale.US, "%.2f", item.price));
        holder.itemView.setOnClickListener(v -> onPick.accept(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView sku;
        VH(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.resultName);
            sku = itemView.findViewById(R.id.resultSku);
        }
    }
}
