package com.mnpos.distribution.data;

import com.mnpos.distribution.model.FieldSpec;
import com.mnpos.distribution.model.MenuItem;
import com.mnpos.distribution.model.RecordSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mnpos.distribution.data.Session.TIER_ADMIN;
import static com.mnpos.distribution.data.Session.TIER_MANAGER;
import static com.mnpos.distribution.data.Session.TIER_SALES_REP;
import static com.mnpos.distribution.model.FieldSpec.Type.BRANCH_PICKER;
import static com.mnpos.distribution.model.FieldSpec.Type.DATE;
import static com.mnpos.distribution.model.FieldSpec.Type.NOTES;
import static com.mnpos.distribution.model.FieldSpec.Type.NUMBER;
import static com.mnpos.distribution.model.FieldSpec.Type.REP_PICKER;
import static com.mnpos.distribution.model.FieldSpec.Type.TEXT;

/**
 * ONE place that defines every distribution screen: its endpoint, how a row
 * renders, its filters, and (if applicable) its create form. HomeActivity
 * reads MENU to build the role-filtered menu; RecordListActivity reads
 * RECORDS by key to know how to render/filter whichever screen was opened.
 *
 * All endpoint paths match the existing backend (/api/mobile/...) used by
 * the original app - no server changes needed.
 */
public final class Catalog {
    private Catalog() {}

    public static final Map<String, RecordSpec> RECORDS = new LinkedHashMap<>();
    public static final List<MenuItem> MENU = new ArrayList<>();

    static {
        RecordSpec branches = new RecordSpec("Branches", "/api/mobile/branches", true, false, false);
        branches.titleKeys = new String[]{"name"};
        branches.subtitleKeys = new String[]{"address", "code"};
        branches.valueKeys = new String[]{};
        RECORDS.put("branches", branches);

        RecordSpec reps = new RecordSpec("Sales Reps", "/api/mobile/sales-reps", true, true, false);
        reps.titleKeys = new String[]{"name"};
        reps.subtitleKeys = new String[]{"mobile", "location_name"};
        reps.valueKeys = new String[]{};
        RECORDS.put("reps", reps);

        RecordSpec customers = new RecordSpec("Customers", "/api/mobile/distribution/customers", true, true, false);
        customers.titleKeys = new String[]{"name"};
        customers.subtitleKeys = new String[]{"mobile", "address"};
        customers.valueKeys = new String[]{"balance", "due_amount", "opening_balance"};
        RECORDS.put("customers", customers);

        RecordSpec routes = new RecordSpec("Routes", "/api/mobile/routes", true, true, false);
        routes.titleKeys = new String[]{"name"};
        routes.subtitleKeys = new String[]{"branch_name", "description"};
        routes.valueKeys = new String[]{};
        RECORDS.put("routes", routes);

        RecordSpec stockRequests = new RecordSpec("Stock Requests", "/api/mobile/stock-requests", false, true, true);
        stockRequests.titleKeys = new String[]{"reference_no", "id"};
        stockRequests.subtitleKeys = new String[]{"branch_name", "created_at", "requested_by"};
        stockRequests.valueKeys = new String[]{"quantity", "total_quantity"};
        stockRequests.valueIsCurrency = false;
        RECORDS.put("stock_requests", stockRequests);

        RecordSpec salesOrders = new RecordSpec("Sales Orders", "/api/mobile/sales-orders", true, true, true);
        salesOrders.titleKeys = new String[]{"customer_name", "invoice_no"};
        salesOrders.subtitleKeys = new String[]{"created_at", "sales_rep_name"};
        RECORDS.put("sales_orders", salesOrders);

        RecordSpec sales = new RecordSpec("Sales", "/api/mobile/distribution-sales", true, true, true);
        sales.titleKeys = new String[]{"invoice_no", "customer_name"};
        sales.subtitleKeys = new String[]{"transaction_date", "created_at"};
        RECORDS.put("sales", sales);

        RecordSpec collections = new RecordSpec("Collections", "/api/mobile/collections", true, true, true);
        collections.titleKeys = new String[]{"customer_name", "reference_no"};
        collections.subtitleKeys = new String[]{"payment_method", "created_at"};
        collections.createFields = new FieldSpec[]{
            new FieldSpec("Branch", "location_id", BRANCH_PICKER, true),
            new FieldSpec("Sales Rep", "sales_rep_id", REP_PICKER, false),
            new FieldSpec("Amount", "amount", NUMBER, true),
            new FieldSpec("Payment method", "payment_method", TEXT, false),
            new FieldSpec("Reference no.", "reference_no", TEXT, false),
            new FieldSpec("Note", "note", NOTES, false),
        };
        RECORDS.put("collections", collections);

        RecordSpec returns = new RecordSpec("Returns", "/api/mobile/distribution-returns", true, true, true);
        returns.titleKeys = new String[]{"invoice_no", "customer_name"};
        returns.subtitleKeys = new String[]{"created_at"};
        returns.createFields = new FieldSpec[]{
            new FieldSpec("Branch", "location_id", BRANCH_PICKER, true),
            new FieldSpec("Sales Rep", "sales_rep_id", REP_PICKER, false),
            new FieldSpec("Amount", "amount", NUMBER, true),
            new FieldSpec("Reason", "reason", NOTES, false),
        };
        RECORDS.put("returns", returns);

        RecordSpec expenses = new RecordSpec("Expenses", "/api/mobile/distribution-expenses", true, true, false);
        expenses.titleKeys = new String[]{"category", "title"};
        expenses.subtitleKeys = new String[]{"created_at", "branch_name"};
        expenses.createFields = new FieldSpec[]{
            new FieldSpec("Branch", "location_id", BRANCH_PICKER, true),
            new FieldSpec("Category", "category", TEXT, true),
            new FieldSpec("Amount", "amount", NUMBER, true),
            new FieldSpec("Date", "date", DATE, false),
            new FieldSpec("Note", "note", NOTES, false),
        };
        RECORDS.put("expenses", expenses);

        RecordSpec settlements = new RecordSpec("Daily Settlement", "/api/mobile/daily-settlements", false, true, true);
        settlements.titleKeys = new String[]{"date", "created_at"};
        settlements.subtitleKeys = new String[]{"branch_name", "sales_rep_name"};
        settlements.valueKeys = new String[]{"cash_amount", "amount"};
        settlements.createFields = new FieldSpec[]{
            new FieldSpec("Branch", "location_id", BRANCH_PICKER, true),
            new FieldSpec("Sales Rep", "sales_rep_id", REP_PICKER, true),
            new FieldSpec("Cash amount", "cash_amount", NUMBER, true),
            new FieldSpec("Date", "date", DATE, false),
            new FieldSpec("Note", "note", NOTES, false),
        };
        RECORDS.put("settlements", settlements);

        RecordSpec orderSummary = new RecordSpec("Order Summary", "/api/mobile/distribution-reports/order-summary", false, true, true);
        orderSummary.dateFilter = true;
        orderSummary.titleKeys = new String[]{"product_name", "name"};
        orderSummary.subtitleKeys = new String[]{"sku"};
        orderSummary.valueKeys = new String[]{"total_qty", "quantity"};
        orderSummary.valueIsCurrency = false;
        RECORDS.put("order_summary", orderSummary);

        RecordSpec dailyProducts = new RecordSpec("Daily Products", "/api/mobile/distribution-reports/branch-product-sales", false, true, false);
        dailyProducts.dateFilter = true;
        dailyProducts.titleKeys = new String[]{"product_name", "name"};
        dailyProducts.subtitleKeys = new String[]{"branch_name"};
        dailyProducts.valueKeys = new String[]{"total_qty", "quantity"};
        dailyProducts.valueIsCurrency = false;
        RECORDS.put("daily_products", dailyProducts);

        // ---- Role-gated menu (checked top to bottom, tier is a floor) ----
        MENU.add(new MenuItem("Take Order", null, "take_order", TIER_SALES_REP, "sell.create"));
        MENU.add(new MenuItem("Customers", "customers", null, TIER_SALES_REP, "customer.view", "customer.view_own"));
        MENU.add(new MenuItem("Sales Orders", "sales_orders", null, TIER_SALES_REP));
        MENU.add(new MenuItem("Sales", "sales", null, TIER_SALES_REP));
        MENU.add(new MenuItem("Collections", "collections", null, TIER_SALES_REP));
        MENU.add(new MenuItem("Returns", "returns", null, TIER_SALES_REP));
        MENU.add(new MenuItem("Stock Requests", "stock_requests", null, TIER_SALES_REP));

        MENU.add(new MenuItem("Branches", "branches", null, TIER_MANAGER));
        MENU.add(new MenuItem("Sales Reps", "reps", null, TIER_MANAGER));
        MENU.add(new MenuItem("Routes", "routes", null, TIER_MANAGER));
        MENU.add(new MenuItem("Stock Transfer", null, "stock_transfer", TIER_MANAGER, "stock.transfer"));
        MENU.add(new MenuItem("Expenses", "expenses", null, TIER_MANAGER, "expense.view"));
        MENU.add(new MenuItem("Daily Settlement", "settlements", null, TIER_MANAGER));
        MENU.add(new MenuItem("Order Summary", "order_summary", null, TIER_MANAGER));
        MENU.add(new MenuItem("Daily Products", "daily_products", null, TIER_MANAGER));

        // Admin-only items would go here (e.g. User Management) - intentionally
        // left out of this build since it wasn't in the requested scope.
    }

    public static List<MenuItem> menuForCurrentUser() {
        Session session = Session.get();
        int tier = session.roleTier();
        List<MenuItem> visible = new ArrayList<>();
        for (MenuItem item : MENU) {
            if (tier < item.minTier) continue;
            if (item.anyPermission != null && item.anyPermission.length > 0 && !session.hasAnyPermission(item.anyPermission)) continue;
            visible.add(item);
        }
        return visible;
    }
}
