package com.mnpos.distribution.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mnpos.distribution.R;
import com.mnpos.distribution.model.CustomerLite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CustomerResultAdapter extends RecyclerView.Adapter<CustomerResultAdapter.VH> {
    private final List<CustomerLite> items = new ArrayList<>();
    private final Consumer<CustomerLite> onPick;

    public CustomerResultAdapter(Consumer<CustomerLite> onPick) {
        this.onPick = onPick;
    }

    public void replace(List<CustomerLite> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_result, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CustomerLite item = items.get(position);
        holder.name.setText(item.name);
        holder.mobile.setText(item.mobile);
        holder.itemView.setOnClickListener(v -> onPick.accept(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView mobile;
        VH(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.customerName);
            mobile = itemView.findViewById(R.id.customerMobile);
        }
    }
}
