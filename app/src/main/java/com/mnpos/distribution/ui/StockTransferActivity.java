package com.mnpos.distribution.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mnpos.distribution.R;
import com.mnpos.distribution.data.ApiClient;
import com.mnpos.distribution.data.Options;
import com.mnpos.distribution.model.PickerOption;
import com.mnpos.distribution.model.ProductLite;
import com.mnpos.distribution.ui.adapters.SearchResultAdapter;
import com.mnpos.distribution.ui.adapters.TransferLineAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StockTransferActivity extends Activity {
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private EditText searchInput;
    private RecyclerView searchResults;
    private RecyclerView lineRecycler;

    private SearchResultAdapter searchAdapter;
    private TransferLineAdapter lineAdapter;
    private List<PickerOption> branches = new ArrayList<>();

    private final Handler debounce = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_transfer);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        fromSpinner = findViewById(R.id.fromSpinner);
        toSpinner = findViewById(R.id.toSpinner);
        searchInput = findViewById(R.id.searchInput);
        searchResults = findViewById(R.id.searchResults);
        lineRecycler = findViewById(R.id.lineRecycler);

        searchAdapter = new SearchResultAdapter(product -> {
            lineAdapter.addLine(product);
            searchInput.setText("");
            searchAdapter.replace(new ArrayList<>());
        });
        searchResults.setLayoutManager(new LinearLayoutManager(this));
        searchResults.setAdapter(searchAdapter);

        lineAdapter = new TransferLineAdapter(position -> lineAdapter.removeAt(position));
        lineRecycler.setLayoutManager(new LinearLayoutManager(this));
        lineRecycler.setAdapter(lineAdapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                debounce.removeCallbacksAndMessages(null);
                if (query.length() < 2) {
                    searchAdapter.replace(new ArrayList<>());
                    return;
                }
                debounce.postDelayed(() -> searchProducts(query), 350);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.submitButton).setOnClickListener(v -> submit());

        Options.loadBranches(options -> {
            branches = options;
            ArrayAdapter<PickerOption> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            fromSpinner.setAdapter(adapter);
            toSpinner.setAdapter(adapter);
        });
    }

    private void searchProducts(String query) {
        PickerOption from = selected(fromSpinner);
        java.util.Map<String, String> params = ApiClient.query(
            "query", query,
            "location_id", from != null && from.id > 0 ? from.id : null
        );
        ApiClient.get("/api/mobile/products", params, new ApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray data = response.optJSONArray("data");
                List<ProductLite> results = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject row = data.optJSONObject(i);
                        if (row != null) results.add(ProductLite.fromJson(row));
                    }
                }
                searchAdapter.replace(results);
            }

            @Override
            public void onError(Exception error) {
                searchAdapter.replace(new ArrayList<>());
            }
        });
    }

    private void submit() {
        PickerOption from = selected(fromSpinner);
        PickerOption to = selected(toSpinner);

        if (from == null || from.id <= 0) {
            toast("Choose the branch to transfer from.");
            return;
        }
        if (to == null || to.id <= 0) {
            toast("Choose the branch to transfer to.");
            return;
        }
        if (from.id == to.id) {
            toast("From and to branches must be different.");
            return;
        }
        if (lineAdapter.lines().isEmpty()) {
            toast("Add at least one product.");
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("from_location_id", from.id);
            body.put("to_location_id", to.id);
            JSONArray items = new JSONArray();
            for (TransferLineAdapter.Line line : lineAdapter.lines()) {
                JSONObject item = new JSONObject();
                item.put("variation_id", line.product.variationId);
                item.put("quantity", line.qty);
                items.put(item);
            }
            body.put("items", items);

            ApiClient.post("/api/mobile/stock-transfers", body, new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    boolean success = response.optBoolean("success", !response.has("http_status"));
                    if (success) {
                        toast("Stock transfer submitted.");
                        finish();
                    } else {
                        toast(response.optString("message", "Transfer failed."));
                    }
                }

                @Override
                public void onError(Exception error) {
                    toast(error.getMessage() == null ? "Transfer failed." : error.getMessage());
                }
            });
        } catch (Exception e) {
            toast(e.getMessage() == null ? "Something went wrong." : e.getMessage());
        }
    }

    private PickerOption selected(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        return item instanceof PickerOption ? (PickerOption) item : null;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
