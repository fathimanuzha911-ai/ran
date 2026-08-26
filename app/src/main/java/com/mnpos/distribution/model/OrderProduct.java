package com.mnpos.distribution.model;

import org.json.JSONObject;

public class OrderProduct {
    public int productId;
    public int variationId;
    public String name;
    public String sku;
    public double price;

    public static OrderProduct fromJson(JSONObject json) {
        OrderProduct p = new OrderProduct();
        p.productId = json.optInt("id");
        p.variationId = json.optInt("variation_id", p.productId);
        p.name = json.optString("name", "Product");
        p.sku = json.optString("sub_sku", json.optString("sku", ""));
        p.price = json.optDouble("price", json.optDouble("selling_price", 0));
        return p;
    }
}
