package com.mnpos.distribution.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.mnpos.distribution.R;
import com.mnpos.distribution.data.ApiClient;
import com.mnpos.distribution.data.Catalog;
import com.mnpos.distribution.data.Options;
import com.mnpos.distribution.model.PickerOption;
import com.mnpos.distribution.model.RecordSpec;
import com.mnpos.distribution.ui.adapters.RecordAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic list screen used for every read-oriented distribution section
 * (Branches, Sales Reps, Customers, Sales Orders, Sales, Collections,
 * Returns, Stock Requests, Expenses, Daily Settlement, and both report
 * screens). Configuration comes entirely from the RecordSpec looked up by
 * the "specKey" intent extra - this ONE Activity replaces what would
 * otherwise be ~12 near-duplicate screens.
 */
public class RecordListActivity extends Activity {
    private static final int PAGE_SIZE = 30;

    private RecordSpec spec;
    private RecordAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recycler;
    private TextView emptyView;
    private EditText searchInput;
    private Spinner branchSpinner;
    private Spinner repSpinner;
    private EditText dateInput;

    private int page = 1;
    private boolean loading = false;
    private boolean endReached = false;
    private String searchTerm = "";
    private int selectedBranchId = 0;
    private int selectedRepId = 0;

    private final Handler debounce = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::reload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);

        String key = getIntent().getStringExtra("specKey");
        spec = Catalog.RECORDS.get(key);
        if (spec == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.screenTitle)).setText(spec.title);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipeRefresh);
        recycler = findViewById(R.id.recordRecycler);
        emptyView = findViewById(R.id.emptyView);
        searchInput = findViewById(R.id.searchInput);
        branchSpinner = findViewById(R.id.branchSpinner);
        repSpinner = findViewById(R.id.repSpinner);
        dateInput = findViewById(R.id.dateInput);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordAdapter(spec, this::openDetail);
        recycler.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::reload);
        setupFilters();
        setupPagination();
        setupFab();

        reload();
    }

    private void setupFilters() {
        if (spec.searchable) {
            searchInput.setVisibility(View.VISIBLE);
            searchInput.addTextChangedListener(new SimpleWatcher(text -> {
                searchTerm = text.trim();
                debounce.removeCallbacks(searchRunnable);
                debounce.postDelayed(searchRunnable, 400);
            }));
        }

        if (spec.branchFilter) {
            branchSpinner.setVisibility(View.VISIBLE);
            Options.loadBranches(options -> bindSpinner(branchSpinner, options, id -> {
                selectedBranchId = id;
                reload();
            }));
        }

        if (spec.repFilter) {
            repSpinner.setVisibility(View.VISIBLE);
            Options.loadReps(options -> bindSpinner(repSpinner, options, id -> {
                selectedRepId = id;
                reload();
            }));
        }

        if (spec.dateFilter) {
            dateInput.setVisibility(View.VISIBLE);
            dateInput.setText(today());
            dateInput.setOnEditorActionListener((v, actionId, event) -> {
                reload();
                return true;
            });
        }
    }

    private void bindSpinner(Spinner spinner, List<PickerOption> options, java.util.function.IntConsumer onSelect) {
        ArrayAdapter<PickerOption> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                onSelect.accept(options.get(position).id);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupPagination() {
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || loading || endReached) return;
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int visibleCount = lm.getChildCount();
                int total = lm.getItemCount();
                int firstVisible = lm.findFirstVisibleItemPosition();
                if (visibleCount + firstVisible >= total - 5) {
                    loadPage(false);
                }
            }
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.addFab);
        String specKey = getIntent().getStringExtra("specKey");
        if ("sales_orders".equals(specKey)) {
            // Order-taking needs multi-line product entry, so it gets its
            // own dedicated screen instead of the generic single-field form.
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> startActivity(new Intent(this, OrderActivity.class)));
        } else if (spec.createFields != null) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(this, RecordFormActivity.class);
                intent.putExtra("specKey", specKey);
                startActivity(intent);
            });
        }
    }

    private void reload() {
        page = 1;
        endReached = false;
        loadPage(true);
    }

    private void loadPage(boolean replace) {
        if (loading) return;
        loading = true;
        if (replace) swipeRefresh.setRefreshing(true);

        java.util.Map<String, String> query = ApiClient.query(
            "page", page,
            "query", searchTerm.isEmpty() ? null : searchTerm,
            "location_id", selectedBranchId > 0 ? selectedBranchId : null,
            "sales_rep_id", selectedRepId > 0 ? selectedRepId : null,
            "date", spec.dateFilter ? dateInput.getText().toString().trim() : null
        );

        ApiClient.get(spec.endpoint, query, new ApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                loading = false;
                swipeRefresh.setRefreshing(false);
                JSONArray data = response.optJSONArray("data");
                List<JSONObject> rows = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject row = data.optJSONObject(i);
                        if (row != null) rows.add(row);
                    }
                }
                if (rows.size() < PAGE_SIZE) endReached = true;
                if (replace) adapter.replace(rows); else adapter.append(rows);
                page++;
                emptyView.setVisibility(adapter.size() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception error) {
                loading = false;
                swipeRefresh.setRefreshing(false);
                if (replace) {
                    adapter.replace(new ArrayList<>());
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText(error.getMessage() == null ? "Could not load data." : error.getMessage());
                }
            }
        });
    }

    private void openDetail(JSONObject row) {
        Intent intent = new Intent(this, RecordDetailActivity.class);
        intent.putExtra("title", spec.title);
        intent.putExtra("json", row.toString());
        startActivity(intent);
    }

    private String today() {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        return format.format(new java.util.Date());
    }

    /** Minimal TextWatcher so callers only supply an onChanged(String) lambda. */
    private static class SimpleWatcher implements android.text.TextWatcher {
        interface OnChanged { void changed(String text); }
        private final OnChanged onChanged;
        SimpleWatcher(OnChanged onChanged) { this.onChanged = onChanged; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChanged.changed(s.toString()); }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }
}
