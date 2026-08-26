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
import com.mnpos.distribution.model.OrderProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class OrderLineAdapter extends RecyclerView.Adapter<OrderLineAdapter.VH> {

    public static class Line {
        public final OrderProduct product;
        public double qty;
        public double unitPrice;
        public Line(OrderProduct product, double qty) {
            this.product = product;
            this.qty = qty;
            this.unitPrice = product.price;
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private final Runnable onChanged;

    public OrderLineAdapter(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    public void addProduct(OrderProduct product) {
        for (Line line : lines) {
            if (line.product.variationId == product.variationId) {
                line.qty += 1;
                notifyDataSetChanged();
                onChanged.run();
                return;
            }
        }
        lines.add(new Line(product, 1));
        notifyItemInserted(lines.size() - 1);
        onChanged.run();
    }

    public void removeAt(int position) {
        if (position < 0 || position >= lines.size()) return;
        lines.remove(position);
        notifyItemRemoved(position);
        onChanged.run();
    }

    public List<Line> lines() {
        return lines;
    }

    public double total() {
        double sum = 0;
        for (Line line : lines) sum += line.qty * line.unitPrice;
        return sum;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_line, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Line line = lines.get(position);
        holder.name.setText(line.product.name);

        holder.qty.removeTextChangedListener(holder.qtyWatcher);
        holder.qty.setText(formatNumber(line.qty));
        holder.qtyWatcher = simpleWatcher(text -> {
            try {
                line.qty = Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                line.qty = 0;
            }
            updateLineTotal(holder, line);
            onChanged.run();
        });
        holder.qty.addTextChangedListener(holder.qtyWatcher);

        holder.price.removeTextChangedListener(holder.priceWatcher);
        holder.price.setText(formatNumber(line.unitPrice));
        holder.priceWatcher = simpleWatcher(text -> {
            try {
                line.unitPrice = Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                line.unitPrice = 0;
            }
            updateLineTotal(holder, line);
            onChanged.run();
        });
        holder.price.addTextChangedListener(holder.priceWatcher);

        updateLineTotal(holder, line);

        holder.remove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) removeAt(pos);
        });
    }

    private void updateLineTotal(VH holder, Line line) {
        holder.total.setText("Rs " + String.format(Locale.US, "%.2f", line.qty * line.unitPrice));
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    private TextWatcher simpleWatcher(Consumer<String> onText) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { onText.accept(s.toString()); }
        };
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final EditText qty;
        final EditText price;
        final TextView total;
        final ImageButton remove;
        TextWatcher qtyWatcher;
        TextWatcher priceWatcher;

        VH(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.lineName);
            qty = itemView.findViewById(R.id.lineQty);
            price = itemView.findViewById(R.id.linePrice);
            total = itemView.findViewById(R.id.lineTotal);
            remove = itemView.findViewById(R.id.removeLine);
        }
    }
}
