package com.mnpos.distribution.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mnpos.distribution.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

/**
 * Generic detail screen: dumps every field of the tapped record as a
 * label/value row. Works for any RecordSpec without needing a bespoke
 * detail layout per screen. Field names are prettified (snake_case -> Title
 * Case) for readability.
 */
public class RecordDetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_detail);

        String title = getIntent().getStringExtra("title");
        String json = getIntent().getStringExtra("json");

        ((TextView) findViewById(R.id.screenTitle)).setText(title == null ? "Details" : title);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        LinearLayout container = findViewById(R.id.detailContainer);
        try {
            JSONObject data = new JSONObject(json == null ? "{}" : json);
            Iterator<String> keys = data.keys();
            boolean any = false;
            while (keys.hasNext()) {
                String key = keys.next();
                if (data.isNull(key)) continue;
                Object value = data.opt(key);
                if (value instanceof JSONObject || value instanceof JSONArray) continue; // keep it flat/readable
                String text = data.optString(key, "");
                if (text.trim().isEmpty()) continue;
                addRow(container, prettify(key), text);
                any = true;
            }
            if (!any) addRow(container, "Info", "No details available.");
        } catch (Exception e) {
            addRow(container, "Error", "Could not read record details.");
        }
    }

    private void addRow(LinearLayout container, String key, String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_detail_row, container, false);
        ((TextView) row.findViewById(R.id.rowKey)).setText(key);
        ((TextView) row.findViewById(R.id.rowValue)).setText(value);
        container.addView(row);
    }

    private String prettify(String key) {
        String spaced = key.replace('_', ' ');
        String[] words = spaced.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.US));
        }
        return builder.toString();
    }
}
