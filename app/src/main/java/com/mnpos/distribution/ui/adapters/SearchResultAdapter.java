package com.mnpos.distribution.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mnpos.distribution.R;
import com.mnpos.distribution.model.ProductLite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.VH> {
    private final List<ProductLite> items = new ArrayList<>();
    private final Consumer<ProductLite> onPick;

    public SearchResultAdapter(Consumer<ProductLite> onPick) {
        this.onPick = onPick;
    }

    public void replace(List<ProductLite> newItems) {
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
        ProductLite item = items.get(position);
        holder.name.setText(item.name);
        holder.sku.setText(item.sku);
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
