package com.mnpos.distribution.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mnpos.distribution.R;
import com.mnpos.distribution.model.RecordSpec;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * One adapter renders every distribution list screen. Which fields it pulls
 * out of a row's JSON is controlled entirely by the RecordSpec passed in, so
 * adding a new screen means adding a RecordSpec to Catalog - not writing a
 * new adapter/layout/activity.
 */
public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.VH> {
    private final RecordSpec spec;
    private final List<JSONObject> rows = new ArrayList<>();
    private final Consumer<JSONObject> onClick;

    public RecordAdapter(RecordSpec spec, Consumer<JSONObject> onClick) {
        this.spec = spec;
        this.onClick = onClick;
    }

    public void replace(List<JSONObject> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    public void append(List<JSONObject> more) {
        int start = rows.size();
        rows.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public int size() {
        return rows.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JSONObject row = rows.get(position);
        holder.title.setText(firstString(row, spec.titleKeys, "—"));

        String subtitle = firstString(row, spec.subtitleKeys, "");
        holder.subtitle.setText(subtitle);
        holder.subtitle.setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);

        String rawValue = firstString(row, spec.valueKeys, "");
        if (rawValue.isEmpty()) {
            holder.value.setVisibility(View.GONE);
        } else {
            holder.value.setVisibility(View.VISIBLE);
            holder.value.setText(spec.valueIsCurrency ? formatMoney(rawValue) : rawValue);
        }

        String status = firstString(row, spec.statusKeys, "");
        if (status.isEmpty()) {
            holder.status.setVisibility(View.GONE);
        } else {
            holder.status.setVisibility(View.VISIBLE);
            holder.status.setText(status.toUpperCase(Locale.US));
        }

        holder.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.accept(row);
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static String firstString(JSONObject row, String[] keys, String fallback) {
        if (keys != null) {
            for (String key : keys) {
                if (row.has(key) && !row.isNull(key)) {
                    String value = row.optString(key, "");
                    if (!value.trim().isEmpty()) return value;
                }
            }
        }
        return fallback;
    }

    private static String formatMoney(String raw) {
        try {
            double value = Double.parseDouble(raw);
            NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            return "Rs " + format.format(value);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView value;
        final TextView status;

        VH(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.recordTitle);
            subtitle = itemView.findViewById(R.id.recordSubtitle);
            value = itemView.findViewById(R.id.recordValue);
            status = itemView.findViewById(R.id.recordStatus);
        }
    }
}
