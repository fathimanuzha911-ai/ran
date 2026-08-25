package com.mnpos.distribution.model;

/**
 * Describes ONE distribution list screen (Branches, Sales Orders, Collections, ...)
 * generically enough that a single RecordListActivity + single RecordAdapter can
 * render all of them, instead of writing a near-duplicate Activity per screen.
 *
 * titleKeys / subtitleKeys / valueKeys / statusKeys are ordered fallback chains:
 * the renderer uses the first key present in a row's JSON. This makes the
 * screen resilient to minor differences in exactly which fields a given
 * endpoint returns, without needing the backend source to hard-code it.
 */
public class RecordSpec {
    public final String title;
    public final String endpoint;
    public final boolean searchable;
    public final boolean branchFilter;
    public final boolean repFilter;
    public boolean dateFilter = false;

    public String[] titleKeys = {"name", "customer_name", "invoice_no", "reference_no", "title"};
    public String[] subtitleKeys = {"date", "created_at", "mobile", "branch_name", "location_name", "status"};
    public String[] valueKeys = {"total", "amount", "due_amount", "balance", "grand_total"};
    public boolean valueIsCurrency = true;
    public String[] statusKeys = {"status", "payment_status"};

    public FieldSpec[] createFields; // null = read-only screen
    public String createEndpoint;    // defaults to `endpoint` if null

    public RecordSpec(String title, String endpoint, boolean searchable, boolean branchFilter, boolean repFilter) {
        this.title = title;
        this.endpoint = endpoint;
        this.searchable = searchable;
        this.branchFilter = branchFilter;
        this.repFilter = repFilter;
    }
}
