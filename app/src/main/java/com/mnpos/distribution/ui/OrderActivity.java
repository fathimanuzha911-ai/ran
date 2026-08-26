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
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mnpos.distribution.R;
import com.mnpos.distribution.data.ApiClient;
import com.mnpos.distribution.data.Options;
import com.mnpos.distribution.data.Session;
import com.mnpos.distribution.model.CustomerLite;
import com.mnpos.distribution.model.OrderProduct;
import com.mnpos.distribution.model.PickerOption;
import com.mnpos.distribution.ui.adapters.CustomerResultAdapter;
import com.mnpos.distribution.ui.adapters.OrderLineAdapter;
import com.mnpos.distribution.ui.adapters.OrderProductResultAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Order-taking screen for sales reps. Posts to the same
 * /api/mobile/sales-orders endpoint and body shape the original POS
 * activity used for "save order" (status "ordered"), so no backend changes
 * are needed:
 *
 * { client_transaction_date, status: "ordered", total, location_id,
 *   customer_id, customer_name, customer_mobile,
 *   items: [{ product_id, variation_id, quantity, unit_price }] }
 */
public class OrderActivity extends Activity {
    private Spinner branchSpinner;
    private EditText customerSearchInput;
    private RecyclerView customerResults;
    private View selectedCustomerBox;
    private TextView selectedCustomerText;
    private EditText productSearchInput;
    private RecyclerView productResults;
    private RecyclerView lineRecycler;
    private TextView emptyLinesText;
    private TextView orderTotal;

    private CustomerResultAdapter customerAdapter;
    private OrderProductResultAdapter productAdapter;
    private OrderLineAdapter lineAdapter;
    private CustomerLite selectedCustomer;

    private final Handler debounce = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        branchSpinner = findViewById(R.id.branchSpinner);
        customerSearchInput = findViewById(R.id.customerSearchInput);
        customerResults = findViewById(R.id.customerResults);
        selectedCustomerBox = findViewById(R.id.selectedCustomerBox);
        selectedCustomerText = findViewById(R.id.selectedCustomerText);
        productSearchInput = findViewById(R.id.productSearchInput);
        productResults = findViewById(R.id.productResults);
        lineRecycler = findViewById(R.id.lineRecycler);
        emptyLinesText = findViewById(R.id.emptyLinesText);
        orderTotal = findViewById(R.id.orderTotal);

        setupBranch();
        setupCustomerSearch();
        setupProductSearch();
        setupLines();

        findViewById(R.id.changeCustomerButton).setOnClickListener(v -> {
            selectedCustomer = null;
            selectedCustomerBox.setVisibility(View.GONE);
            customerSearchInput.setVisibility(View.VISIBLE);
            customerSearchInput.setText("");
        });

        findViewById(R.id.submitButton).setOnClickListener(v -> submit());
    }

    private void setupBranch() {
        Options.loadBranches(options -> {
            ArrayAdapter<PickerOption> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            branchSpinner.setAdapter(adapter);
            // Default to the logged-in user's own branch, if it's in the list.
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).id == Session.get().locationId) {
                    branchSpinner.setSelection(i);
                    break;
                }
            }
        });
    }

    private void setupCustomerSearch() {
        customerAdapter = new CustomerResultAdapter(customer -> {
            selectedCustomer = customer;
            selectedCustomerText.setText(customer.name + (customer.mobile.isEmpty() ? "" : "  •  " + customer.mobile));
            selectedCustomerBox.setVisibility(View.VISIBLE);
            customerSearchInput.setVisibility(View.GONE);
            customerResults.setVisibility(View.GONE);
        });
        customerResults.setLayoutManager(new LinearLayoutManager(this));
        customerResults.setAdapter(customerAdapter);

        customerSearchInput.addTextChangedListener(watcher(query -> {
            debounce.removeCallbacksAndMessages(null);
            if (query.trim().length() < 2) {
                customerResults.setVisibility(View.GONE);
                return;
            }
            debounce.postDelayed(() -> searchCustomers(query.trim()), 350);
        }));
    }

    private void searchCustomers(String query) {
        ApiClient.get("/api/mobile/customers", ApiClient.query("query", query), new ApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray data = response.optJSONArray("data");
                List<CustomerLite> results = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject row = data.optJSONObject(i);
                        if (row != null) results.add(CustomerLite.fromJson(row));
                    }
                }
                customerAdapter.replace(results);
                customerResults.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(Exception error) {
                customerResults.setVisibility(View.GONE);
            }
        });
    }

    private void setupProductSearch() {
        productAdapter = new OrderProductResultAdapter(product -> {
            lineAdapter.addProduct(product);
            productSearchInput.setText("");
            productAdapter.replace(new ArrayList<>());
            productResults.setVisibility(View.GONE);
        });
        productResults.setLayoutManager(new LinearLayoutManager(this));
        productResults.setAdapter(productAdapter);

        productSearchInput.addTextChangedListener(watcher(query -> {
            debounce.removeCallbacksAndMessages(null);
            if (query.trim().length() < 2) {
                productResults.setVisibility(View.GONE);
                return;
            }
            debounce.postDelayed(() -> searchProducts(query.trim()), 350);
        }));
    }

    private void searchProducts(String query) {
        PickerOption branch = selectedBranch();
        ApiClient.get("/api/mobile/products",
            ApiClient.query("query", query, "location_id", branch != null && branch.id > 0 ? branch.id : null),
            new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    JSONArray data = response.optJSONArray("data");
                    List<OrderProduct> results = new ArrayList<>();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject row = data.optJSONObject(i);
                            if (row != null) results.add(OrderProduct.fromJson(row));
                        }
                    }
                    productAdapter.replace(results);
                    productResults.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onError(Exception error) {
                    productResults.setVisibility(View.GONE);
                }
            });
    }

    private void setupLines() {
        lineAdapter = new OrderLineAdapter(this::refreshTotal);
        lineRecycler.setLayoutManager(new LinearLayoutManager(this));
        lineRecycler.setAdapter(lineAdapter);
        refreshTotal();
    }

    private void refreshTotal() {
        orderTotal.setText("Rs " + String.format(Locale.US, "%.2f", lineAdapter.total()));
        emptyLinesText.setVisibility(lineAdapter.lines().isEmpty() ? View.VISIBLE : View.GONE);
        lineRecycler.setVisibility(lineAdapter.lines().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void submit() {
        if (selectedCustomer == null) {
            toast("Select a customer first.");
            return;
        }
        if (lineAdapter.lines().isEmpty()) {
            toast("Add at least one product.");
            return;
        }
        PickerOption branch = selectedBranch();
        if (branch == null || branch.id <= 0) {
            toast("Choose a branch.");
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("client_transaction_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            body.put("status", "ordered");
            body.put("total", lineAdapter.total());
            body.put("location_id", branch.id);
            body.put("customer_id", selectedCustomer.id);
            body.put("customer_name", selectedCustomer.name);
            body.put("customer_mobile", selectedCustomer.mobile);

            JSONArray items = new JSONArray();
            for (OrderLineAdapter.Line line : lineAdapter.lines()) {
                JSONObject item = new JSONObject();
                item.put("product_id", line.product.productId);
                item.put("variation_id", line.product.variationId);
                item.put("quantity", line.qty);
                item.put("unit_price", Math.max(0, line.unitPrice));
                items.put(item);
            }
            body.put("items", items);

            ApiClient.post("/api/mobile/sales-orders", body, new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    boolean success = response.optBoolean("success", !response.has("http_status"));
                    if (success) {
                        toast("Order saved.");
                        finish();
                    } else {
                        toast(response.optString("message", "Order save failed."));
                    }
                }

                @Override
                public void onError(Exception error) {
                    toast(error.getMessage() == null ? "Order save failed." : error.getMessage());
                }
            });
        } catch (Exception e) {
            toast(e.getMessage() == null ? "Something went wrong." : e.getMessage());
        }
    }

    private PickerOption selectedBranch() {
        Object item = branchSpinner.getSelectedItem();
        return item instanceof PickerOption ? (PickerOption) item : null;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private TextWatcher watcher(java.util.function.Consumer<String> onChanged) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChanged.accept(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        };
    }
}
