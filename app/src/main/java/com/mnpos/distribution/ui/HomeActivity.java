package com.mnpos.distribution.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mnpos.distribution.R;
import com.mnpos.distribution.data.ApiClient;
import com.mnpos.distribution.data.Catalog;
import com.mnpos.distribution.data.Session;
import com.mnpos.distribution.model.MenuItem;
import com.mnpos.distribution.ui.adapters.MenuAdapter;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeActivity extends Activity {
    private GridLayout metricsGrid;
    private RecyclerView menuRecycler;
    private SwipeRefreshLayout swipeRefresh;
    private TextView businessName;
    private TextView userLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Session session = Session.get();
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        metricsGrid = findViewById(R.id.metricsGrid);
        menuRecycler = findViewById(R.id.menuRecycler);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        businessName = findViewById(R.id.businessName);
        userLine = findViewById(R.id.userLine);

        businessName.setText(session.businessName.isEmpty() ? "Distribution" : session.businessName);
        userLine.setText(session.userName + "  •  " + (session.roleName.isEmpty() ? "User" : session.roleName)
            + (session.locationName.isEmpty() ? "" : "  •  " + session.locationName));

        menuRecycler.setLayoutManager(new LinearLayoutManager(this));
        menuRecycler.setAdapter(new MenuAdapter(Catalog.menuForCurrentUser(), this::openMenuItem));

        swipeRefresh.setOnRefreshListener(this::loadDashboard);
        loadDashboard();
    }

    private void openMenuItem(MenuItem item) {
        if ("stock_transfer".equals(item.activityTag)) {
            startActivity(new Intent(this, StockTransferActivity.class));
            return;
        }
        Intent intent = new Intent(this, RecordListActivity.class);
        intent.putExtra("specKey", item.specKey);
        startActivity(intent);
    }

    private void loadDashboard() {
        swipeRefresh.setRefreshing(true);
        ApiClient.get("/api/mobile/distribution/dashboard", new ApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                swipeRefresh.setRefreshing(false);
                renderMetrics(response.optJSONObject("data"));
            }

            @Override
            public void onError(Exception error) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void renderMetrics(JSONObject data) {
        metricsGrid.removeAllViews();
        if (data == null) return;
        addMetric("Today Sales", money(data.optDouble("today_sales", 0)));
        addMetric("Collections", money(data.optDouble("today_collections", 0)));
        addMetric("Outstanding", money(data.optDouble("customer_outstanding", 0)));
        addMetric("Stock Value", money(data.optDouble("branch_stock_value", 0)));
        addMetric("Pending Requests", String.valueOf(data.optInt("pending_stock_requests", 0)));
        addMetric("Low Stock", String.valueOf(data.optInt("low_stock", 0)));
    }

    private void addMetric(String label, String value) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_metric, metricsGrid, false);
        ((TextView) card.findViewById(R.id.metricLabel)).setText(label);
        ((TextView) card.findViewById(R.id.metricValue)).setText(value);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        card.setLayoutParams(params);
        metricsGrid.addView(card);
    }

    private String money(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "Rs " + format.format(value);
    }
}
