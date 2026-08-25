package com.mnpos.distribution.ui.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mnpos.distribution.R;
import com.mnpos.distribution.model.ProductLite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TransferLineAdapter extends RecyclerView.Adapter<TransferLineAdapter.VH> {

    public static class Line {
        public final ProductLite product;
        public double qty;
        public Line(ProductLite product, double qty) {
            this.product = product;
            this.qty = qty;
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private final Consumer<Integer> onRemove;

    public TransferLineAdapter(Consumer<Integer> onRemove) {
        this.onRemove = onRemove;
    }

    public void addLine(ProductLite product) {
        for (Line line : lines) {
            if (line.product.variationId == product.variationId) {
                line.qty += 1;
                notifyDataSetChanged();
                return;
            }
        }
        lines.add(new Line(product, 1));
        notifyItemInserted(lines.size() - 1);
    }

    public void removeAt(int position) {
        if (position < 0 || position >= lines.size()) return;
        lines.remove(position);
        notifyItemRemoved(position);
    }

    public List<Line> lines() {
        return lines;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer_line, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Line line = lines.get(position);
        holder.name.setText(line.product.name);

        holder.qty.setTag(null);
        holder.qty.setText(formatQty(line.qty));
        holder.qty.removeTextChangedListener(holder.watcher);
        holder.watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    line.qty = Double.parseDouble(s.toString());
                } catch (NumberFormatException ignored) {}
            }
        };
        holder.qty.addTextChangedListener(holder.watcher);

        holder.remove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) onRemove.accept(pos);
        });
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    private String formatQty(double qty) {
        if (qty == Math.floor(qty)) return String.valueOf((long) qty);
        return String.valueOf(qty);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final EditText qty;
        final ImageButton remove;
        TextWatcher watcher;

        VH(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.lineName);
            qty = itemView.findViewById(R.id.lineQty);
            remove = itemView.findViewById(R.id.removeLine);
        }
    }
}
