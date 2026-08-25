package com.mnpos.distribution.model;

import org.json.JSONObject;

public class ProductLite {
    public int variationId;
    public String name;
    public String sku;

    public static ProductLite fromJson(JSONObject json) {
        ProductLite p = new ProductLite();
        p.variationId = json.optInt("variation_id", json.optInt("id"));
        p.name = json.optString("name", "Product");
        p.sku = json.optString("sub_sku", json.optString("sku", ""));
        return p;
    }
}
